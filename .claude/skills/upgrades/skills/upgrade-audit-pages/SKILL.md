---

name: upgrade-audit-pages
description: Compare pages between two Liferay portal versions side by side and produce a bug report with evidence. Use when the user invokes `/upgrade-audit-pages`, asks to audit pages, or asks to compare visual regressions between versions.

---

# upgrade-audit-pages

Compares pages between two Liferay portal versions — a **source version** (older, in production) and a **target version** (the upgrade being validated) — and produces:

- `report.md` — Markdown report with each bug formatted for local review.
- `bugs.csv` — the same bugs as CSV, one row per bug, for import into whatever tracker the team uses.

The skill is version-agnostic. Source and target versions are read from `CLAUDE.md` (`upgrade.source.version` / `upgrade.target.version`) and used as labels in the report — never hardcoded.

## Prerequisites

- The upgraded portal is deployed and running (Phase 3, Fix Startup, complete) so its pages can be compared against the source version.
- Both portals (source and target) are reachable from the machine running Claude Code.
- Chrome DevTools MCP and Playwright MCP are configured. These are pre-configured automatically by `install.sh` in the workspace's `.mcp.json` (Chrome DevTools for computed styles/console/network/axe-core, Playwright for navigation/screenshots/storage state). If `/mcp` shows them as `failed`, the most common cause is missing Node.js or Playwright browsers — instruct the user to run `npx playwright install chromium`. If `.mcp.json` is missing the entries entirely (workspace had a pre-existing `.mcp.json` that `install.sh` did not overwrite), point the user at `.mcp.json.template` in the agent repo and ask them to merge the `chrome-devtools` and `playwright` entries.
- Working directory is the workspace root.

## Reading CLAUDE.md fields

| Field | Used for | Fallback |
|---|---|---|
| `upgrade.source.version` | Label for the source column in the report (e.g. `7.3`) | Ask the user |
| `upgrade.target.version` | Label for the target column in the report (e.g. `2026.q1.0`) | Ask the user |
| `audit.source.base.url` | Base URL of the source portal — used when CSV rows give relative paths | Ask the user |
| `audit.target.base.url` | Base URL of the target portal — used when CSV rows give relative paths | Ask the user |
| `audit.user` | Login email/screen name for pages that require authentication | Ask the user; required only if any page has `requires_auth=true` |
| `audit.password` | Login password — read from referenced file/env, **never inline** | Required only if any page has `requires_auth=true` |
| `audit.viewports` | Default viewport list, e.g. `desktop` or `desktop,tablet,mobile` | `desktop` (1440x900) |
| `audit.pages.file` | Default path to the CSV with the page list | `.claude/audits/pages.csv` |

---

## Gotchas

- **Liferay-generated portlet instance IDs change between versions** (`#_com_liferay_..._INSTANCE_x`),
  so they are never valid selectors — use semantic ones. This is the most common source of false
  "bugs" in a cross-version diff.
- **A CSV import cannot carry local files.** Screenshots and diffs are referenced by path, so they
  must be attached by hand after any import — a ticket that references them by path arrives with
  nothing.
- **A difference is not automatically a bug.** A divergence that appears consistently portal-wide is
  a design-system change: demote it to informational and exclude it from the bug count.
- **Report only what is new in the target.** Console errors, failed requests and accessibility
  violations that also occur on the source portal are out of scope — pre-existing, not regressions.
- **The session cookie changed name** across versions (`JSESSIONID` → `LFR_SESSION_STATE_*`), so
  auth-state handling that keys on the old name silently fails to carry a session.
- **An MCP server reporting `failed` usually means missing browsers**, not a config error — run
  `npx playwright install chromium`. And `install.sh` does not overwrite a pre-existing `.mcp.json`,
  so a workspace can be missing the `chrome-devtools` / `playwright` entries entirely.

## Step 0 — Setup and input detection

The input CSV format is `assets/pages.example.csv.template`, which `install.sh` also seeds into each
workspace as `.claude/audits/pages.example.csv`.

1. Generate a run timestamp: `<YYYYMMDD-HHMMSS>`.

1. Create the audit directory: `.claude/audits/<timestamp>/` plus subdirectories `screenshots/`, `styles/`, `console/`.

1. Detect the input mode from the arguments passed to `/upgrade-audit-pages`:

### Mode 1 — Single URL pair

Two arguments, both URLs. The **first** URL is the **target**, the **second** is the **source**.

```
/upgrade-audit-pages https://portal-target.example.com/web/home https://portal-source.example.com/web/home
```

### Mode 2 — CSV file

A single argument ending in `.csv` (defaults to `audit.pages.file` from CLAUDE.md if no argument is given but the file exists).

CSV format (header required):

```csv
name,target_url,source_url,requires_auth,notes
Home,https://portal-target.example.com/,https://portal-source.example.com/,false,
Dashboard,/group/intranet,/group/intranet,true,Private page
```

Columns:
- `name` — short identifier used in the report.
- `target_url` / `source_url` — full URLs **or** paths relative to `audit.target.base.url` / `audit.source.base.url`.
- `requires_auth` — `true` if login is required.
- `notes` — free-form annotations (e.g. "ignore carousel").

### Mode 3 — Inline list (no arguments)

Reply with:

> Paste the list of pages to audit. Accepted formats:
> - Full CSV (with header)
> - Simple list, one line per page: `<target-url> | <source-url>`
> - Markdown table
>
> Indicate whether the pages require login.

Then wait for the user's next message and parse it.

### Pre-flight confirmation (batch mode)

Before starting in CSV or inline mode, show:

```
About to audit N pages:
  - Viewports: <list>
  - Source: <upgrade.source.version> @ <audit.source.base.url>
  - Target: <upgrade.target.version> @ <audit.target.base.url>
  - Estimated time: ~45s/page (desktop) — total ~M minutes
  - Output directory: .claude/audits/<timestamp>/

Proceed? [y/N]
```

Wait for user confirmation before launching MCP work.

---

## Step 1 — Authentication

If any page has `requires_auth=true`:

1. Read `audit.user` and `audit.password` (resolve by-reference values).

1. For each portal (source and target):
   - Navigate to `<base_url>/c/portal/login` via Playwright MCP.
   - Fill the login form using **semantic selectors** (`getByLabel`, `getByRole`), not generated IDs.
   - Save the resulting cookies/session as Playwright `storageState` to `.claude/audits/<timestamp>/state-source.json` and `.claude/audits/<timestamp>/state-target.json`.

1. Reuse the stored state for every page that requires auth — never log in twice.

If login fails on either portal, stop and report. Do not proceed with partial authentication.

---

## Step 2 — Per-page audit methodology

For each page, repeat all six stages **in order**. Save each artifact under `.claude/audits/<timestamp>/<page-id>/`. The page ID is a slugified version of the `name` column.

### Stage 1 — Load and stabilize

1. Open both URLs in parallel (one Playwright context per portal).

1. Wait for `networkidle`.

1. Mask dynamic regions before any capture:
   - Timestamps and relative dates (`time` elements, `[data-timestamp]`).
   - Liferay `p_auth` tokens in URLs.
   - Carousels — pause via CSS injection: `* { animation-play-state: paused !important; }`.
   - Real-time avatars and counters (`[class*="counter"]`, `[class*="badge"]`).
   - Any selectors listed in the row's `notes` column.

### Stage 2 — Structural inventory

On each portal, extract:

- Buttons (`role=button`) — text and `aria-label`.
- Navigation links — visible text and `href`.
- Form fields — labels and types.
- Headings `h1`–`h6` — text and DOM order.
- Images — `alt` text and dimensions.

Compare and report:

- Elements present in source but missing in target.
- Elements present in target but new (not in source).
- Elements with changed visible text or `aria-label`.
- Heading hierarchy reorderings.

> ⚠️ **Never use Liferay-generated portlet IDs** (`_com_liferay_..._INSTANCE_xxxx_`) as selectors. They change between versions. Use semantic selectors only — role + accessible name, label + type, or visible text.

### Stage 3 — Visual comparison

For each viewport in `audit.viewports`:

1. Full-page screenshot of source → `.claude/audits/<timestamp>/screenshots/<page-id>/source-<viewport>.png`.

1. Full-page screenshot of target → `target-<viewport>.png`.

1. Pixel diff via `pixelmatch` (threshold `0.1`) → `diff-<viewport>.png`.

Heuristics:
- Diff > 10% of pixels: probable visual regression.
- Diff 2–10%: candidate; confirm with computed styles in Stage 4.
- Diff < 2%: usually noise (font hinting, anti-aliasing); ignore unless Stage 4 corroborates.

### Stage 4 — Computed styles

For each **action button** (Save, Cancel, Publish, Submit, Confirm, Delete, etc.) and **primary link** on the page:

1. **Default state** — capture: `background-color`, `color`, `border`, `border-radius`, `box-shadow`, `padding`, `font-size`, `font-weight`, `cursor`, `opacity`.

1. **Hover state** — `page.hover()`, wait 300ms, recapture.

1. **Focus state** — `page.focus()`, recapture.

Save all to `.claude/audits/<timestamp>/styles/<page-id>.json`. Compare property-by-property between source and target. Any divergence is a candidate bug.

> Computed styles catch what pixel diffs miss: color shifts under animation, `:hover` states never triggered in screenshots, `:focus` rings altered for accessibility.

### Stage 5 — Console and network

While the page loads and during Stage 4 interactions, capture:

- All console messages (`error`, `warning`, `info`).
- All network requests with status `4xx` or `5xx`.

Save to `.claude/audits/<timestamp>/console/<page-id>.log`. Report only entries that exist **in target but not in source** — these are regressions caused by the upgrade.

### Stage 6 — Accessibility (axe-core)

Inject `axe-core` into both portals and run a full scan. Save the raw output to `.claude/audits/<timestamp>/console/<page-id>-axe.json`. Report only violations **new in target** (regressions). Do not report pre-existing violations from the source portal — they are out of scope for an upgrade audit.

### Checkpoint

After completing all six stages for a page, append a checkpoint to `.claude/audits/<timestamp>/progress.json`, per `assets/progress.json.template`.

If interrupted, re-running `/upgrade-audit-pages` pointing to the same timestamp resumes from the first non-completed page.

---

## Step 3 — Generate `report.md`

Write the report to `.claude/audits/<timestamp>/report.md`, rendering `assets/report.md.template`.

Markdown image links use **relative paths** so the report renders correctly in VS Code, GitHub, and any Markdown viewer that opens the audit folder.

---

## Step 4 — Generate `bugs.csv`

Write `.claude/audits/<timestamp>/bugs.csv` with this exact column order. Any column can be remapped when the team imports it:

```csv
"Bug ID","Summary","Issue Type","Priority","Labels","Components","Environment","Description","Attachments"
```

| Column | Content | Notes |
|---|---|---|
| `Bug ID` | `#001`, `#002`, … (matches the `report.md` IDs) | Custom field "External ID" or simply included in summary |
| `Summary` | ≤ 120 chars, action-oriented (e.g. "Save button has no hover state on Dashboard") | Required |
| `Issue Type` | Always `Bug` | |
| `Priority` | `Highest` (Critical) / `High` / `Medium` / `Low` | |
| `Labels` | Space-separated: `liferay-upgrade audit-<timestamp> visual-regression` (or `functional`, `a11y`, `console`, `network`, `structural`) | |
| `Components` | `UI` / `Functional` / `Accessibility` / `Console` / `Network` / `Structural` | |
| `Environment` | `<upgrade.target.version> @ <viewport>` | |
| `Description` | Full bug body in Markdown — see template below | Multi-line; CSV-quote and escape internal `"` as `""` |
| `Attachments` | Semicolon-separated **local relative paths** to PNGs and logs | Local files must be attached manually after any import |

### Template for the `Description` column

Generate the Description per `assets/bug-description.txt.template`.

Wrap multi-line content in proper CSV quoting. Use `""` to escape internal double quotes.

## Step 6 — Update `upgrade-state.md` and report

Append to `upgrade-state.md`:

```markdown
## Visual & Functional audit — <timestamp>

- Pages audited: <N>
- Bugs found: <total> (Critical: X, High: Y, Medium: Z, Low: W)
- Recurring patterns: <count>
- Report: `.claude/audits/<timestamp>/report.md`
- Bug CSV: `.claude/audits/<timestamp>/bugs.csv`
- Tickets opened via MCP: <N> / not used
```

Final chat output:

```
Audit complete.

Pages audited: <N>
Bugs: <total> (Critical: X, High: Y, Medium: Z, Low: W)
Recurring patterns suggested for grouped tickets: <count>

Report:    .claude/audits/<timestamp>/report.md
Bug CSV:   .claude/audits/<timestamp>/bugs.csv
Artifacts: .claude/audits/<timestamp>/

Top 3 critical bugs:
  1. <link to bug #X in report.md>
  2. <link to bug #Y in report.md>
  3. <link to bug #Z in report.md>

Next: review the report, then run /upgrade-report.
```

---

## Severity rubric

- **Critical** — broken functionality: page does not load, JS error blocks interaction, login fails, data does not save, primary user flow broken.
- **High** — primary action button changed unexpectedly, hover/focus states removed, broken primary link, new WCAG-A violation.
- **Medium** — visible but non-blocking visual difference, altered spacing, off-palette hover, new WCAG-AA violation.
- **Low** — subtle pixel difference, minor microcopy change, new WCAG-AAA violation.

If a divergence is consistent across the entire portal (every page on the target shows the same change), cross-reference `references/known-design-system-changes.md`. If it matches a documented intentional change, demote to **informational** and do not include in the bug count. If it is consistent but not yet documented, include as **informational** with a note recommending the user add it to the calibration log.

---

## Verification loop

This skill captures and reports; it never repairs, so there is no fix/re-verify loop. The one loop is
**calibration**: audit 3–5 pages, review what came back, add the design-system entries that explain the
noise, then re-run those same pages and confirm the noise dropped before scaling to the full list — see
`references/known-design-system-changes.md`.

For Liferay-specific component behavior, version-known visual changes, selector pitfalls, and the
dynamic-content mask list, read `references/liferay-component-notes.md` before classifying bugs in components like Clay/Lexicon, Fragments, Page Editor, asset routes, and the login portlet.

---
