---

name: deprecated-api-remediation
description: Use this skill whenever modifying Java, JSP, JSPF, FreeMarker, or SCSS source files that may contain Liferay API calls deprecated or removed between the workspace's source version and target version. Trigger especially during Phase 2 (Fix Compile) of the upgrade playbook — invoked per module by `upgrade-compile` — when the user asks about deprecated APIs, removed methods, or `com.liferay.*.kernel.*` imports, or when a build failure mentions a symbol that cannot be resolved in a Liferay package. This skill is the PRIMARY applier of Liferay's source-formatter mappings: the agent does not run `formatSource`, so this skill reads pre-generated indexes (index_simple.md and index_complex.md) derived from Liferay's own `replacements.json`, applies import replacements from `imports.txt`, and applies the structural check-class transformations in `check-classes-reference.md` directly (not as a fallback).
---

# Deprecated API Remediation

Applies Liferay's source-formatter mapping patterns to the workspace's custom code, using indexes
generated from the authoritative `replacements.json` by `/upgrade-refresh-references`.

After any reference-data refresh, re-check previously processed modules in case new mappings apply.

---

## Data sources

Before processing any file, resolve the active `<source-id>` from `upgrade-state.md`
("Reference data source" section). Then read:

| File | Purpose |
|------|---------|
| `.claude/cache/reference/<source-id>/index_simple.md` | Compact one-line index for simple_rename, param_change, param_reorder, static_method_rename |
| `.claude/cache/reference/<source-id>/index_complex.md` | Detailed blocks for class_swap, class_structure, regex_replace, message_only |
| `.claude/cache/reference/<source-id>/imports.txt` | FQN import replacements (`old=new` format) |
| `.claude/reference/custom-replacements.json` | Team-curated overrides — same schema as upstream, applied after indexes, team wins on conflicts |
| `.claude/reference/skipped-replacements.json` | Entries to skip by `issueKey` with reason — read at start of every run |
| `.claude/reference/check-classes-reference.md` | Source-formatter check-class transformations — applied in §3f |

If the cache is empty or the indexes are missing, tell the user to run
`/upgrade-refresh-references` first. Do not proceed with guessed replacements.

For full schema details on how to interpret each pattern type, see
[references/schema.md](references/schema.md).

---

## Gotchas

- **Class not in workspace** → skip silently
- **Same `from` with different `classNames`** → apply both independently
- **Match inside comment or string literal** → skip. Conservative default for all file types:
  - Java: skip matches inside `//`, `/* */`, string literals
  - JSP: skip matches inside `<%-- --%>`
  - FreeMarker: skip matches inside `<#-- -->`
  - Exception: entries with `skipParametersValidation: true` still skip comment/string matches but apply everywhere else
- **Multiple entries apply to the same line** → apply in `replacements.json` order; re-scan line after each application
- **File has pre-existing syntax errors** → log and skip; do not rewrite a file that doesn't parse cleanly

## Workflow

### Step 1 — Establish scope
- If invoked without a module path → iterate every module under `modules.root` (from `CLAUDE.md`)
- If invoked with a path → scope to that module only
- Skip: `node_modules/`, `build/`, `bin/`, and any path matching `.gitignore`

### Step 2 — Apply import replacements (imports.txt)

For each `.java`, `.jsp`, `.jspf` file in scope — **do this before pattern matching**:

1. Read `imports.txt` — each line is `old.fully.qualified.Class=new.fully.qualified.Class`

1. For `.java`: replace matching `import old.Class;` → `import new.Class;` and update all type declarations, method parameters, return types, and variable declarations referencing the old class name across the entire module

1. For `.jsp`/`.jspf`: replace `<%@ page import="old.Class" %>` and type references inside `<% %>` blocks

> Import replacements must run before pattern matching because some `replacements.json` entries assume imports have already been rewritten.

### Step 3 — Match and apply patterns (indexes)

For each `.java`, `.jsp`, `.jspf`, `.ftl`, `.scss` file in scope:

#### 3a — Consult `index_simple.md` first

Each line follows: `` `from` → `to` [ticket, type, extensions] ``

- **simple_rename** — match method or class name → rename directly
- **static_method_rename** — match `ClassName.method` → rename directly
- **param_change** — match method signature including parameter types → rewrite call with `param#N#` substitution
- **param_reorder** — same as param_change; check autonomy rules before applying (see [references/autonomy.md](references/autonomy.md))

#### 3b — Consult `index_complex.md` for remaining patterns
- **class_swap** — method moved to a different class; also apply `newImports` and `newReference`
- **class_structure** — structural rewrite; check autonomy rules before applying
- **regex_replace** — keyed under a class name (e.g. `### BasePanelApp`) with a **multi-line** `from`
  pattern. Reading the index and eyeballing does **not** work — match them **mechanically**, per
  entry:
  1. **Extract the literal identifiers** from the entry's `from` regex — the class names, method
     names, annotations it references (e.g. for `BasePanelApp`: `BasePanelApp`, `setPortlet`,
     `Portlet`). Strip the regex metacharacters; keep the plain tokens.
  2. **`grep` the in-scope files** for those identifiers (e.g.
     `gg -l 'setPortlet' <module>`). This is the cheap pre-filter — it is exactly how you *find* the
     candidate file (the same grep that locates `### BasePanelApp` at a line in `index_complex.md`).
  3. **For each file that hits**, run the entry's `from` regex against its content (multi-line /
     DOTALL) and apply `to` with capture-group substitution — **from the catalog**, verbatim.

  Do this for **every** `regex_replace` entry, honoring `validExtensions`. **Never** conclude "pattern
  not in the catalog" or label `#automation` for a removed-symbol fix without first running the §3g
  guard.
- **message_only** — do NOT rewrite; log as a warning for human review with the `issueKey`

#### 3c — Apply era filtering (if enabled in CLAUDE.md)

When `issue-key-era-filter = on`:
- Source version < 7.4 → apply both `LPS-*` and `LPD-*` entries
- Source version ≥ 7.4 → apply only `LPD-*` entries

Era filtering is a performance optimization. The idempotence check catches already-applied entries regardless.

#### 3d — Check custom overrides

After applying indexes, check `.claude/reference/custom-replacements.json`:
- Same schema as upstream
- Apply after indexes — custom wins on conflicts
- Log any conflict to the decision log in `upgrade-state.md`

#### 3e — Check skipped entries

Read `.claude/reference/skipped-replacements.json` at the start of every run.
Skip any entry whose `issueKey` appears in this file. Log the skip reason.

#### 3f — Apply `check-classes-reference.md` structural transforms
These are the structural transformations source-formatter would apply via its check classes. Since the agent does **not** run `formatSource`, this skill applies them **directly** — nothing else pre-applies them.

1. Read `.claude/reference/check-classes-reference.md`
2. For each check entry, scan the current file for the **INPUT** pattern described
3. If the INPUT pattern is present, apply the **EXPECTED OUTPUT** transformation
4. Log each application as `check-class transform applied: <check-name>`
5. These are **catalog-sourced** structural transforms — apply them **autonomously** and **flag for human review** in `upgrade-state.md` (per [references/autonomy.md](references/autonomy.md)); they do **not** stop-and-ask. Only a genuinely *hand-derived* structural change — in neither the catalog nor `known-manual-changes.md` — confirms first.

#### 3g — Catalog-coverage guard (before any "manual" / `#automation` fix)

An API change is a **catalog hit** if it is matched by **any** source above — `imports.txt`,
`index_simple.md`, `index_complex.md` (**including the `regex_replace` entries**), or
`check-classes-reference.md`. Before you hand-write a fix for a compile error or removed symbol —
and **before** you label anything `#automation` (in the `upgrade-compile` build loop) — run this
**concrete grep** (do not decide by memory or by skimming):

> **The two indexes partition the catalog by type — neither is a superset.** `index_simple.md` holds
> **only** `simple_rename` / `param_change` / `param_reorder` / `static_method_rename`;
> `index_complex.md` holds **only** `class_swap` / `class_structure` / `regex_replace` / `message_only`.
> A structural or removed-symbol fix (like `BasePanelApp`/`setPortlet`, `LPD-7870`) is a
> `class_structure` entry, so it is **expected to be absent from `index_simple.md`** — its entry lives
> in `index_complex.md`. **"Not in `index_simple.md`" is never evidence of a catalog miss**, and the
> indexes are **not** incomplete relative to `replacements.json` for these types. Always grep **both**
> (the command below does); never conclude "manual" from `index_simple.md` alone.

```bash
# <symbol> = the removed/changed class or method (e.g. BasePanelApp, setPortlet)
grep -rin '<symbol>' .claude/cache/reference/<source-id>/index_simple.md \
  .claude/cache/reference/<source-id>/index_complex.md \
  .claude/cache/reference/<source-id>/imports.txt \
  .claude/reference/check-classes-reference.md \
  .claude/skills/deprecated-api-remediation/references/known-manual-changes.md
```

Any hit in the first four sources → it is a **catalog hit** (the entry exists even if its `from` is a
big multi-line regex). A hit **only** in `known-manual-changes.md` → it is a **documented
known-manual-change** (a change we already know and have a documented fix for, but SF cannot automate).

- If it **is a catalog hit** → apply the catalog `to` / EXPECTED OUTPUT (not your own version), commit it
  as a catalog hit (`Replace X by Y`, **no `#automation` label**), and do **not** route it to the
  manual catalog-miss path.
- If it **is a documented known-manual-change** → apply per `known-manual-changes.md` (`flag` class:
  autonomous + `[applied — verify]`, `stub` class: stub + `[not applied — resolve]`), **no `#automation`
  label** — it is documented, so there is nothing to harvest.
- Only when **none** of the sources match (catalog **and** `known-manual-changes.md`) is it a genuine
  miss (then `upgrade-compile` handles it with the `#automation` label).

**Flagged-for-review ≠ manual.** A `class_structure` entry is applied **autonomously** but flagged for
human review — that flag is about *risk visibility at PR time*, not how you label it. It stays a
catalog hit.

**The decisive test:** if the code you committed equals the catalog `to:` / EXPECTED OUTPUT — which it
must, for any cataloged entry — then **by definition it is a catalog hit**: cite the `issueKey`, no
`#automation`. Writing a hand-derived rationale for a fix whose output matches a catalog entry *is* the
mislabel. Mislabeling pollutes the `#automation` harvest with patterns Liferay already covers.

### Step 4 — Apply newImports

After rewriting a call site, if the entry lists `newImports`:

1. Check if the import is already present — if yes, skip

1. If a conflicting import exists (same simple name, different package) → flag for human review, do not silently shadow

1. Insert in Liferay import order: `java`, `javax`, `jakarta`, `com.liferay`, `org`, then third-party

### Step 5 — Idempotence check

Before applying any entry, check if the file already contains the expected
post-state. If yes, skip and log at debug level. Do not re-apply.

For full schema details and annotated examples, see [references/schema.md](references/schema.md).
For autonomy rules, see [references/autonomy.md](references/autonomy.md).

### Step 7 — Verification loop

After all applicable entries for a file have been applied:

| File type | Verification |
|-----------|-------------|
| `.java` | Compile the module: `blade gw clean compileJava` |
| `.jsp` / `.jspf` / `.ftl` | Syntax check only; full compile fires in the module build |
| `.scss` | Syntax check via workspace SCSS linter if present |

On compile failure, loop — but the loop is **bounded at two attempts**, deliberately:

1. Read the error

1. Attempt one targeted fix, then re-verify

1. If the second attempt also fails → **revert the file**, log the failure, flag for human review,
   and stop. Do not keep iterating: a file that resists two targeted fixes needs a human, and
   further attempts risk compounding damage.

See [references/autonomy.md](references/autonomy.md) for the full retry policy.

### Step 8 — Commit per (module × pattern)

Message format and labels follow the `commit` skill and `.claude/rules/commit.md`; with no ticket, the title is prefixed `NOISSUE`
per `upgrade-phase` §9. **One commit per pattern** within the module — all files touched by the same
pattern sweep into one commit.

Neither catalog entries nor documented known-manual-changes carry an `#automation` label: both already
have a documented fix, so there is nothing to harvest. `#automation` is **only** for a fix in neither
source.

**A catalog hit gets its own commit — never bundle it into another change.** Do not fold a catalog
application into a Jakarta-migration, Service-Builder-regen, or scheduler-migration commit (observed:
the cataloged `IndexWriterHelperUtil.updateDocument` param_change, LPD-8003, was buried inside a
`Migrate scheduler job…` commit and the whole commit was mislabeled `#automation`).

Record per module (in the summary, not in each commit): N files modified, M entries applied (by
type), K entries flagged for human review.

### Step 9 — Summarize

Append a `summary.md` entry under `upgrade-notes/<run-id>/phase-2-compile/` with:
- Modules processed
- Total entries applied vs. flagged
- Table of flagged items: `file:line`, `issueKey`, one-line reason
- message_only warnings list
- Link to raw log under `.claude/upgrade-artifacts/<run-id>/phase-2-compile/`

---

## Metrics per module

Track and report for each module per `assets/module-metrics.txt.template`.

---

## Related skills

- **`upgrade-compile`** — the Phase 2 orchestrator. It invokes this skill per module for API remediation, and itself owns the `javax.*` → `jakarta.*` migration and the Service Builder / REST Builder regen. SB-generated files are regenerated **before** this skill runs on a module, so remediation is never wasted on output SB will overwrite.
- **`/upgrade-refresh-references`** — fetches `replacements.json` / `imports.txt` and generates the `index_simple.md` / `index_complex.md` this skill reads. Run before Phase 2.

---

## Known manual changes — Claude-automated patterns

In addition to the indexes generated from `replacements.json`, Claude also
applies a set of known code changes that SF cannot automate. These are
documented in [references/known-manual-changes.md](references/known-manual-changes.md).

### How to process these cases

1. After applying all patterns from the indexes (Steps 2–3), scan the module for the detection patterns listed in `known-manual-changes.md`

1. For each match found:
   - **autonomous** → apply directly, log the change
   - **flag** → apply the documented transformation directly (use the documented default for any value or variable name that must be chosen), record it under "Flagged for human review" in `upgrade-state.md` tagged `[applied — verify]`, **no `#automation`** — do **not** stop and ask. The fix is documented and deterministic, so the flag is the safety net (a human verifies at PR time). This is symmetric to a cataloged structural rewrite (see [references/autonomy.md](references/autonomy.md)).
   - **stub** → add the stub implementation, leave a `// TODO: implement business logic` comment, flag `[not applied — resolve]` for human review

1. Include all applied cases in the module metrics and PR description under a dedicated "Known manual changes applied" section