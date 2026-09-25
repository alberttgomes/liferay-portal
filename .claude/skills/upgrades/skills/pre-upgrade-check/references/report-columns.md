# The artifacts-feedback report

`pre-upgrade-check` produces the artifacts-feedback report in three forms:

- `report.csv` — the raw data (one row per issue).
- `report.xlsx` — a **styled** workbook for import into Google Sheets (a CSV carries no
  formatting; see "Deliver a styled spreadsheet" below).
- `report.md` — the same rows as a GitHub table for quick reading in the run dir.

One row per issue found across Steps 1–9.

## Columns (exact, in order)

| Column | Filled by | Content |
|---|---|---|
| `#` | agent | monotonic row number, starting at 1 |
| `Description` | agent | the problem found, stated plainly (what's wrong, where) |
| `Page` | agent | the portal page / URL where a functional or visual issue shows; **blank** for build/config issues that have no page |
| `Evidence` | agent | log path + line / screenshot ref / command output snippet |
| `Analysis` | agent | the diagnosis, the impact, and **what was changed** — cross-reference the change-log filename here |
| `Response` | **the reader** | **left empty** — whoever reviews the report fills this in |

CSV header (also in `assets/report.header.csv`):

```csv
#,Description,Page,Evidence,Analysis,Response
```

## Severity / wording conventions

Prefix `Descriptions` with a severity tag so the sheet can be sorted:

- `[BLOCKER]` — stops a required check from passing (build fails, dump won't import, portal
  won't connect, an instance's login errors).
- `[ERROR]` — a real defect that we worked around; the client should fix at source.
- `[WARNING]` — non-blocking; environment-specific or cosmetic.
- `[REPRO]` — a reproduction-environment change we made that exists only because we build/run
  outside the client's network (e.g. added a public repo mirror, bind-mounted the DL). Still
  reported because it touches delivered files.

Distinguish clearly in the analysis column between **our** reproduction fixes and **genuine
client source defects** — the latter are the highest-value findings.

## CSV escaping (the analysis column is multi-line)

- Quote any field containing a comma, quote, or newline with double quotes.
- Escape embedded double quotes by doubling them (`"` → `""`).
- Keep newlines inside the quoted field (Google Sheets imports them as in-cell line breaks).

## Deliver a styled spreadsheet (.xlsx)

A plain CSV carries **no** formatting, so importing it never reproduces the team sheet's look
(dark header row, bold titles, borders, frozen header). Also emit `report.xlsx` with
`assets/report-xlsx.py` (openpyxl):

```bash
python3 .claude/skills/pre-upgrade-check/assets/report-xlsx.py \
  pre-upgrade-notes/<run-id>/report.csv        # writes report.xlsx next to it
```

It renders the six columns with a dark header (`#434343`), bold white centered titles, thin
borders, a frozen header row, wrapped cells, and sized columns. **Import the .xlsx**
(File → Import → Upload) to keep the styling; the CSV remains for raw re-use.

## Example rows (markdown twin)

| # | Description | Page | Evidence | Analysis | Response |
|---|---|---|---|---|---|
| 1 | [REPRO] settings.gradle resolves only from the client's internal Nexus, unreachable outside the client network — build cannot fetch the workspace plugin or deps. | | `logs/build.log:1` "Could not resolve ..." | Added the Liferay public CDN + Maven Central as **additional** repositories alongside the Nexus entries (none removed); build then resolves. See `change-logs/001-…md`. | |
| 2 | [ERROR] All bundles fail to jar — `Invalid value for Bundle-Version, <token>` — a CI version placeholder left unsubstituted. | | `logs/build.log:NNNN` | Baked a valid version across `gradle.properties` + all `bnd.bnd`; see `known-build-fixes.md` §1 and `change-logs/00N-…md`. | |
| 3 | [BLOCKER] Login as `<user>` errors after auth: "circumvent the permission checker" for the default site Group. | `http://localhost:8080/` (main → home) | `screenshots/login-error.png`; `logs/catalina-boot.log` (`NoSuchResourcePermissionException … primKey=<groupId>`) | Dump is missing `ResourcePermission` for the site (data gap, not code). Confirm-SQL + fixes in `runtime-findings.md` §2. | |
