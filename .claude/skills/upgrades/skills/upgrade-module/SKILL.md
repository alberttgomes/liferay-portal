---

name: upgrade-module
description: Run this skill when the user invokes `/upgrade-module <path>` to apply the full upgrade pipeline to a single OSGi module, theme, or fragment — useful for trying the agent on a single artifact before running a full phase, or for catching up one module that was skipped. Trigger also when the user says "upgrade this module", "migrate just this portlet", or points at a single module directory and asks the agent to update it.

---

# /upgrade-module <path>

Apply the relevant domain skills to a single module. Useful for incremental work, debugging a flagged item, or validating the agent on a representative artifact before running full phases.

## Prerequisites

- Workspace is initialized (`/upgrade-init` has run).
- Reference data is fetched (`/upgrade-refresh-references` has run).
- The target path exists and is a single module (a directory containing `bnd.bnd` or `build.gradle` under the workspace's `modules.root` / `themes.root` / `client-extensions.root`).

## Gotchas

- **Some `release.dxp.api` redundancies cannot be decided statically.** Its POM does not list
  `org.osgi:org.osgi.annotation.versioning` as a transitive dependency, but the runtime **exports its
  packages** — so whether a module still needs the explicit declaration depends on its bytecode, and
  only remove-recompile can tell. That is why the prune is compile-verified here rather than in Phase 1.
- **Locate a prune candidate by its exact content, never by a cached line number.** Each
  comment/restore shifts the lines below it, so a line-number-driven loop edits the wrong line and
  corrupts the file (observed: a false `kept` on `org.osgi.service.component.annotations`).
- **`cssBuilder` / `parentThemes` / `portalCommonCSS` / `themeBuilder` are not on the compile
  classpath**, so `compileJava` cannot probe them — they need the full `:build`.

## Workflow

1. **Classify the module** into one of the types the Phase 2 flow actually handles:
   - **Regular module** — any Java OSGi bundle, including Service Builder projects (`service.xml`), REST Builder projects (`rest-config.yaml`), and JSP hooks / fragment-host bundles (`bnd.bnd` has `Fragment-Host:`).
   - **Theme** — under `themes.root`.
   - **Client extension** — under `client-extensions.root` (`client-extension.yaml`).

   The classification selects which `upgrade-compile` flow runs.

2. **Run the matching `upgrade-compile` per-module flow**, scoped to this single module. `upgrade-compile` is the single source of truth for the per-module steps — this skill does **not** duplicate them, and the only domain skill in the loop is `deprecated-api-remediation` (invoked by `upgrade-compile` Step 4).
   - **Regular module** → `upgrade-compile` §"Regular module steps": Step 1 Service Builder regen → Step 2 REST Builder regen → Step 3 Jakarta migration (`javax.*`→`jakarta.*`) → Step 3b dependency cleanup → Step 4 build-and-remediate loop (delegates to `deprecated-api-remediation`) → Step 5 JSP compilation, including the hook/fragment-host 5b reconciliation.
   - **Theme** → `upgrade-compile` §"Theme module steps": clean generated files → node version (fallback only) → metadata + `dart-sass` migration + build.
   - **Client extension** → validate against the target and bump versions; no Java/Jakarta flow applies.

   Skip any step whose trigger file is absent (no `service.xml` → no SB regen, no `.jsp` → no JSP step), exactly as `upgrade-compile` does. Each step applies its own autonomy rules and confirmation prompts.

3. **Frontend / JS work is out of scope here.** `upgrade-module` covers what Phase 2 covers — making the module *compile*. JS-heavy concerns (React / `metal`→React, Clay, AlloyUI, FreeMarker templates, inline JS) belong to **Phase 5 (Fix Frontend)**, which is deferred. For a JS-heavy module this run gets it compiling against the target but does **not** migrate its frontend — record any frontend follow-up in the summary (step 6) and move on.

4. **Verification loop.** Inherits `upgrade-compile` Step 4 — build, remediate, re-build until clean.
   - Java-touching: `./gradlew :<module-gradle-path>:build`.
   - JS-touching: `node --check` + `eslint` + `npm run build` in the module.
   - Theme: the workspace's theme build task.

4b. **Compile-verified dependency prune.** After the module reaches a clean compile, treat **every declared dependency** as a removal candidate and prune the redundant ones (remove → rebuild → drop if it still builds). See `Compile-verified prune` below for the algorithm and skip conditions.

5. **Capture artifacts.** Under `.claude/upgrade-artifacts/<run-id>/module-<sanitized-path>/` for this single-module run. `<run-id>` is the current upgrade run-id if one exists; otherwise generate `single-module-<timestamp>`.

6. **Summarize.** Under `upgrade-notes/<run-id>/module-<sanitized-path>/summary.md`, committed.

7. **Commit.** Per the `commit` skill and `.claude/rules/commit.md`, prefixed `NOISSUE` when no ticket applies.

## Compile-verified prune (step 4b)

**Why this lives here, not in `upgrade-setup-version`:** see `## Gotchas` — the check needs a known-good baseline (the clean compile from step 4), which only exists during Phase 2, and the static consolidation in `upgrade-compile` Step 3b cannot decide package-level cases.

**Trigger:** the module has any top-level declared dependency. **Every declared dependency is a candidate** — there is no fixed allowlist. (`references/candidate-prune.md` still lists known-removable patterns to *prioritize* and to apply target conditions, but it no longer gates which deps are considered.)

**Opt-out:** read `upgrade.module.dependency.prune` from `CLAUDE.md`. Default `true`. When set to `false`, skip step 4b entirely and produce no `dependency-prune.txt` artifact.

**Algorithm (per module):**

1. Snapshot the current `build.gradle` (in-memory copy is enough; the file is already committed clean from earlier steps).
2. Take **all** top-level declared deps in the `dependencies { … }` block as candidates —
   **including the build-config configs** (`cssBuilder`, `parentThemes`, `portalCommonCSS`,
   `themeBuilder`). These are **version pins for tooling the workspace plugin provides by default**,
   so the explicit declaration is usually redundant — and **is** a candidate, since excluding it leaves
   a redundant pin behind in every CSS/theme-bearing module. Two candidate
   classes:
   - **Classpath deps** — tiered via the dep-tree below, probed with the compile build.
   - **Build-config configs** (`cssBuilder`/`parentThemes`/`portalCommonCSS`/`themeBuilder`) — the
     tiering doesn't apply (see `## Gotchas`). Probe each by **dropping the WHOLE line** (not just the
     `version:`) → run the **full module build** `<gw> :<path>:build` → keep it removed if the build
     still passes, restore the line if it breaks.

   Use `references/candidate-prune.md` to *order* known-removable patterns first and apply target
   conditions — not to gate.
3. **Generate the parsed dep-tree** for this module: `blade gw :<gradle-project>:dependencies --configuration compileClasspath --no-daemon`, cached at `.claude/cache/upgrade/gw-deps-<target>/<sanitized-gradle-project>.txt`. Use the parser and three sets (`RESOLVED_BY_RELEASE_DXP_API`, `OVERRIDDEN_FROM_RELEASE_DXP_API`, `CONSTRAINT_ONLY_ROOTS`) from `upgrade-setup-version/references/release-dxp-api-redundant.md` to assign the tiers below.
4. **Classify each candidate** against the parsed tree (see `references/candidate-prune.md` "Tier evidence"):
   - **Tier 1 — auto-remove** (no compile probe): GA ∈ `CONSTRAINT_ONLY_ROOTS` ∧ also ∈ `RESOLVED_BY_RELEASE_DXP_API`. The line contributes nothing classpath-wise; removing it cannot affect compilation. Justification: `auto-removed (constraint-only + provided)`.
   - **Tier 2 — probe-first**: GA ∈ `RESOLVED_BY_RELEASE_DXP_API` (provided transitively) but not constraint-only. High-confidence removable; probe to confirm. Order first within the probe loop.
   - **Tier 3 — probe-with-skepticism**: GA does NOT appear under `release.dxp.api`'s subtree at all but is on the `CANDIDATE_PRUNE` list because the runtime exports its packages (the canonical case is `org.osgi.annotation.versioning`). Probe; order after Tier 2.
   - **Tier 4 — probe last**: GA is a root contributor not under `release.dxp.api`'s subtree (e.g. a genuine third-party lib). Likely kept, but **still probed** (every declared dep is a candidate — a dep can be on the classpath yet provided by another dep or transitively). Order after Tiers 2–3.
5. Soft cap: probe up to **20** candidates per module (Tiers 2–4); Tier-1 auto-removes don't count. If a module exceeds that, probe the higher-tier (more-likely-redundant) ones first and record the rest as `skipped (cap)`. Tune per project — each probe is a recompile.
6. **Tier 1 — apply auto-removes** in one pass: comment all Tier-1 candidate lines out simultaneously and write decisions straight to `dependency-prune.txt`. No compile is run between Tier-1 removals (they are constraint-only by definition).
7. **Tiers 2 + 3 — probe loop, one candidate at a time** in tier order:
   1. Comment the candidate line out by **matching its exact content** (the dependency declaration
      text), **never by a cached line number**. Commenting/restoring shifts the line numbers of every
      line below, so a line-number-driven loop edits the wrong line on the next candidate and corrupts
      the file (observed: a false `kept` on `org.osgi.service.component.annotations` after an earlier
      removal shifted indices). Re-locate by content on every comment/restore. Don't delete yet — keep
      it commented for reattribution.
   2. Run `./gradlew :<module-gradle-path>:compileJava :<module-gradle-path>:compileJsp --no-daemon` (skip `compileJsp` if the module has no JSPs). Capture exit code and the first compiler error symbol if any.
   3. If compile **succeeds** → mark `removed (probed; provided transitively)` for Tier 2, `removed (probed; package-level provision)` for Tier 3. The line stays commented for now; final write-out collapses commented blocks.
   4. If compile **fails** → restore (uncomment) the line; mark `kept (used by <symbol>)` with the failing symbol from the compiler error.
8. After processing all tiers, write the surviving `build.gradle` (drop commented-out `removed`/`auto-removed` lines, including any trailing blank line they leave behind).
9. Run a **final** `./gradlew :<module>:build` to confirm the module is still green with all approved removals applied. If this final compile fails, restore the original `build.gradle` from the snapshot and record the entire run as `aborted (final compile failed)` — never leave a half-pruned file behind.

**Ancestor build.gradle are in scope.** A module's effective config = its own `build.gradle` **plus** what its ancestors inject via `subprojects{}`/`allprojects{}`/`configurations.all{}` (forces, pins, dep injections). Walk the ancestor chain (`<group>/build.gradle` → … → `<root>/build.gradle`) for stale `resolutionStrategy.force`/pins affecting this module and remove them too — a leaf-only prune misses them. When a phase runs many modules, this is tracked with a shared manifest + completion gate so each ancestor is cleaned once and none is skipped; see `upgrade-compile` Step 3b "Hierarchical `build.gradle` cleanup".

**Output artifact:** `.claude/upgrade-artifacts/<run-id>/module-<sanitized-path>/dependency-prune.txt` — one line per candidate, per
`assets/dependency-prune.txt.template`.

When step 4b runs but no candidates apply (empty filtered set), write the single line `(no candidates found)` so a reader can distinguish "step ran clean" from "step never ran".

**The artifact must match the committed `build.gradle` — regenerate it on any re-run.** The
`dependency-prune.txt` is the audit record, so its decisions have to agree with what actually shipped:
every `removed`/`auto-removed` dep absent from the final `build.gradle`, every `kept` dep present. If
you re-prune a module — e.g. after correcting a tooling error (a line-number slip, a spurious probe) —
**overwrite the artifact from the final state**; never leave a stale `kept`/`removed` line that the
commit contradicts (observed: a re-pruned module's artifact still said `kept` for a dep the commit had
removed). Before committing the cleanup, **diff the artifact against the committed deps** and reconcile
any mismatch.

**Commit message addendum:** when at least one candidate is `removed`, append a body line to the module's commit message: `Pruned N redundant dependencies after compile verification.` The title (already produced by step 7) is unchanged.

## Autonomy

Inherits from the invoked domain skills. The orchestrator does not add its own confirmations.

Step 4b is autonomous when `upgrade.module.dependency.prune=true` (default): no per-line user prompts, since every removal is gated by a successful compile and the final whole-module build. The user can opt out workspace-wide by setting the key to `false` in `CLAUDE.md`.

## Output

Same artifact and summary layout as phase runs, scoped to one module.