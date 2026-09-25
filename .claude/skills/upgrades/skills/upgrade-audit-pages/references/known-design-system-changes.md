# Known design system changes (calibration log)

This file is **meant to grow over time**. When a difference between the source and target versions is consistent across the entire portal, it is almost certainly an intentional design system change introduced by Liferay (or by the customer's theme), not a bug. Documenting those patterns here stops the audit skill from flagging them as bugs on every run.

The audit skill (`upgrade-audit-pages`) reads this file when classifying bugs. If a divergence matches an entry here, it is demoted to **informational** and excluded from the bug count.

## How to use this file

Each entry has four fields:

- **Pattern** — short description of the difference (one line).
- **Affected component / selector** — what to match against (semantic selector or component family).
- **Introduced in** — Liferay version where the change was first observed.
- **Action** — what the audit skill should do (`informational`, `ignore`, or `flag-anyway-because-X`).

Format:

```markdown
### <short title>

- **Pattern:** <one-line description>
- **Affected:** <component family or semantic selector>
- **Introduced in:** <version>
- **Action:** informational | ignore | flag (with reason)
- **Notes:** <optional — link to Liferay release notes, ticket, or design doc>
```

## Seeded examples (replace with patterns observed in your specific upgrade path)

### Clay button hover transition timing

- **Pattern:** Hover transition timing changed from `0.15s ease` to `0.2s ease-in-out` on all `.btn` variants.
- **Affected:** All Clay button components (`.btn-primary`, `.btn-secondary`, `.btn-link`).
- **Introduced in:** 7.4
- **Action:** informational
- **Notes:** Replace this example with patterns the team actually observes during the first audit pass.

### Form input focus ring color

- **Pattern:** Focus ring color updated to match the new brand palette (`#0B5FFF` → `#0747A6`).
- **Affected:** All form inputs, selects, textareas inheriting the Clay focus style.
- **Introduced in:** 2026.q1
- **Action:** informational
- **Notes:** Replace with real observations.

## Recommended workflow for the first week

1. Run the audit on **3–5 representative pages** (login, home, one form-heavy page, one dashboard, one admin page).

1. Review the report carefully. Many "bugs" on the first run will be intentional design system changes that recur across pages.

1. For every false positive that recurs, add an entry to this file.

1. Re-run the same pages — noise should drop significantly.

1. Then scale to the full page list.

## Anti-patterns

Do **not** add entries here for:

- One-off issues that affect a single page or component (those are real bugs, not patterns).
- Customer-specific theme overrides — those should live in the customer's workspace `CLAUDE.md` under "Coding conventions" or a dedicated section, not in this shared agent file.
- Differences the team wants to fix — if it is a bug worth fixing, do not silence it; address it instead.