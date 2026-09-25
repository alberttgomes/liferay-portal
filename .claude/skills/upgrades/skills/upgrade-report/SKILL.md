---

name: upgrade-report
description: Consolidate every phase's summary into one final upgrade report suitable for a pull request description or handoff. Use when the user invokes `/upgrade-report`, asks to summarize what changed, or wants a handoff document after the phases complete.

---

# /upgrade-report

Consolidate per-phase summaries into one final report suitable for PR descriptions, handoff, or compliance archival.

## Prerequisites

- At least one phase has been run (ideally all of Phases 1–4).
- `upgrade-state.md` has entries populated in the phase tracker and artifact manifest.
- `upgrade-notes/<run-id>/` contains per-phase summary files.

## Gotchas

- **There is no `tasks.csv`.** Every stat comes from files under `upgrade-notes/`, which makes a
  local task file the wrong instinct.

## Workflow

1. **Identify the run-id** from `upgrade-state.md` (current upgrade branch's most recent).

1. **Gather per-phase summaries.** For each phase with status `complete`, `skipped`, or `blocked`, read `upgrade-notes/<run-id>/phase-<N>-<slug>/summary.md`.

1. **Gather phase stats.** Derive per-phase done/total and outcomes from the per-phase summary files under `upgrade-notes/<run-id>/`. Use these in the "Executive summary" and as the basis of the "Phase outcomes" section.

1. **Extract key metrics.** Per phase: files touched, replacements applied, items flagged. Overall: total modules processed, total build runs, total smoke-test outcomes, total human-review items outstanding.

1. **Generate a PR description stub.** `upgrade-notes/<run-id>/pr-description.md` — a trimmed version of the final report suitable for pasting into a PR description. Same sections, shorter, with markdown checkboxes for follow-up items.

1. **Summarize in chat.** Overall outcome, total items flagged, and a pointer to the generated files. If any phase is still `in-progress` or `blocked`, call it out as an incomplete report.

## Autonomy

Fully autonomous. No destructive operations.

## What this is not

- Not automatic PR creation — it produces the description, not the PR itself.

## Output

- `upgrade-notes/<run-id>/final-report.md` — committed.
- `upgrade-notes/<run-id>/pr-description.md` — committed.
- Chat summary with links to all files.