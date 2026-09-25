# Autonomy boundaries — deprecated-api-remediation

This file defines when the skill acts autonomously and when it requires human
confirmation. Read from `SKILL.md` before processing any entry.

---

## Autonomous — no confirmation needed

- **Simple renames** — method or class renamed with no parameter changes
- **Parameter reorders with type-distinct arguments** — e.g. `(long, String)` → `(String, long)`: the types are different so there is no risk of silent semantic swap
- **Import rewrites** — adding `newImports` listed by an applied entry
- **Symbol-only replacements** — plain text renames in JSP/FTL/SCSS with no structural impact
- **Catalog-hit structural rewrites** — a `classStructurePattern: true` entry, or a
  `check-classes-reference.md` transform, applied **verbatim from the catalog**: deterministic and
  Liferay-sanctioned, so apply it **without stopping to ask** — but **record it under "Flagged for human
  review" in `upgrade-state.md`, tagged `[applied — verify]`** (the customer's code may extend/override
  the class, so a human eyeballs it at PR time). The flag replaces the stop-and-ask; it does not change
  the labeling (still a catalog hit, `Replace X by Y`, no `#automation`).
- **Documented known-manual-changes** — an entry in `references/known-manual-changes.md` classified
  `flag`: apply the documented transformation **without stopping to ask** (use the documented default for
  any value or variable name that must be chosen), then **record it under "Flagged for human review"
  tagged `[applied — verify]`**, **no `#automation`** (it is a change we already know cannot be
  SF-automated but for which we have a documented, deterministic fix — there is nothing to harvest). This
  is symmetric to a catalog-hit structural rewrite. The `stub` class is the exception: add the stub and
  flag `[not applied — resolve]`, since the business logic is not documented.
- **Committing changes** to the upgrade branch after a module is processed and verified

---

## Requires confirmation — stop and ask

- **Same-type parameter reorders** — e.g. `(long, long)` → `(long, long)` with swapped positions: impossible to tell if the call site intended the original order
- **Hand-derived structural changes** — rewriting method signatures or class structure for a change in **neither** the catalog (no `classStructurePattern`/check-class entry) **nor** `references/known-manual-changes.md`: there is no Liferay-sanctioned or documented template, so the rewrite could be wrong — confirm before applying. (A *cataloged* structural rewrite **or** a *documented known-manual-change* is **autonomous-but-flagged** — see the Autonomous list above; neither stops-and-asks.)
- **`removeImplements`** — removing an interface implementation is a behavior change
- **Complex regex rewrites** — when the diff is hard to read or the match is ambiguous
- **`newMethods` insertion** — adding method declarations to a class; positioning is error-prone
- **Unresolvable `to`-side symbol** — if the replacement references a symbol that cannot be found in the cached imports, the reference data may be ahead of the runtime; warn the user and check the fetch source in `upgrade-state.md`

---

## Never autonomous — hard stops

- Push to remote
- Merge the upgrade branch
- Modify files outside the scoped module
- Delete code

---

## Retry policy

After applying an entry, if compilation fails:

1. Read the compiler error

1. Attempt one targeted fix

1. If the second attempt also fails → **revert the file**, log the failure, flag for human review

Hard-stop after one retry. Do not loop.