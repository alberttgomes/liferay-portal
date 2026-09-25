# Phase 3 report formats

Three deliverables under `.upgrade-frontend-flow/reports/<run_id>/`. Image and trace links use **relative paths** (`evidence/<test-id>/...`) so the reports render in any Markdown viewer opened on the reports folder.

## `bugs.md`

````markdown
# Frontend flow regression report

**Original portal:** <ORIGINAL_URL>
**Upgraded portal:** <UPGRADE_URL>
**Run:** <run_id>
**Test cases:** <total> — pass: <N> · locator-fixed: <M> · bugs: <K> · blocked: <B> · deleted: <D>

## Summary by flow

| Flow | Context | Tests | Pass | Locator-fixed | Bugs |
|------|---------|------:|-----:|--------------:|-----:|
| Join Site | site-membership | 3 | 2 | 1 | 1 |

## Locator fixes applied

One line per `locator-fixed` case: flow, element, commit SHA. These passed after repair — listed for review traceability, not action.

| Flow | Element | Commit |
|------|---------|--------|
| Join Site | joinSiteButton | `<sha>` |

## Bugs

### Bug #001 — Join Site - membership button missing after join <a id="bug-001"></a>

- **Test case:** `user can join an open site` — `tests/site-membership/joinSite/joinSite.spec.ts`
- **Severity:** High
- **Original URL:** <ORIGINAL_URL>/web/guest/sites
- **Upgrade URL:** <UPGRADE_URL>/web/guest/sites

**BDD:**

```
Given the user is logged in
When the user joins the "Open Site" site
Then the membership badge is displayed
```

**Expected behavior (original):** <what the original portal does — one paragraph>

**Current behavior (upgrade):** <what the upgraded portal does instead, including the Playwright error>

| Expected (original) | Current (upgrade) |
|---|---|
| ![original](evidence/site-membership-joinsite-user-can-join-an-open-site/original.png) | ![upgrade](evidence/site-membership-joinsite-user-can-join-an-open-site/upgrade.png) |

**Evidence:**
- Trace: `evidence/<test-id>/trace.zip`
- Error: `evidence/<test-id>/error.txt`

**Steps to reproduce:** the BDD steps above, executed on <UPGRADE_URL>.

---

## Original-portal cleanup

The suite now targets the upgraded portal only. `<ORIGINAL_URL>` was removed from the code and config in commit `<cleanup_sha>`, and `BASE_URL` in `.env` now points at the upgraded portal. The references in this report are historical — the original portal is the baseline this run compared against.
````

## `bugs.csv`

Written for **every** bug, as the machine-readable twin of `bugs.md` and the handoff into whatever tracker the team uses.

Exact column order. Any column can be remapped on import:

```csv
"Bug ID","Summary","Issue Type","Priority","Labels","Components","Environment","Description","Attachments"
```

| Column | Content |
|---|---|
| `Bug ID` | `#001`, `#002`, … — matches `bugs.md` |
| `Summary` | `<flow> - <short error>`, ≤ 120 chars (e.g. `Join Site - membership button missing after join`) |
| `Issue Type` | Always `Bug` |
| `Priority` | `Highest` (Critical) / `High` / `Medium` / `Low` |
| `Labels` | Space-separated: `liferay-upgrade frontend-flow-<run_id> functional-regression` |
| `Components` | `Functional` |
| `Environment` | `Upgraded portal @ <UPGRADE_URL>` |
| `Description` | Markdown (template below) — CSV-quoted, internal `"` escaped as `""` |
| `Attachments` | Semicolon-separated local relative paths: `evidence/<test-id>/upgrade.png;evidence/<test-id>/original.png;evidence/<test-id>/trace.zip` — local files must be attached manually after any import |

### `Description` template

The description carries three things, in order: the test case's BDD, the current-vs-expected images, and the URLs on both portals.

```
*Test case:* user can join an open site ({{tests/site-membership/joinSite/joinSite.spec.ts}})
*Severity:* High

h3. BDD
{code:none}
Given the user is logged in
When the user joins the "Open Site" site
Then the membership badge is displayed
{code}

h3. Current behavior (upgrade)
<one paragraph + the Playwright error in a {code} block>
!upgrade.png|width=600!

h3. Expected behavior (original)
<one paragraph>
!original.png|width=600!

h3. URLs
- Original: https://portal.example.com/web/guest/sites
- Upgrade: http://localhost:8080/web/guest/sites

h3. Steps to reproduce
# Execute the BDD steps above on the upgraded portal
# Observe the failure at the "Then" step
```

The `upgrade.png` and `original.png` references render once the files from the `Attachments` column are attached to the issue, which is a manual step after import.

### Import instructions (include in the final chat output)

> The CSV at `.upgrade-frontend-flow/reports/<run_id>/bugs.csv` is ready to import into the team's tracker.
>
> 1. Import `bugs.csv` and choose the target project.
> 1. Confirm the column mapping.
> 1. After import, attach the evidence: open each issue and upload that bug's `evidence/<test-id>/` files. The image references in the description start rendering then.

## `blocked-tests.md`

The manual handoff for every test case that could not be automated. Three parts, because each group needs something different from the reader: the **removed** cases are gone from the suite and have to be executed by hand from now on; the **kept** cases are still in the repo waiting to be unblocked; the **ambiguous** cases need a decision about what the test should assert before anyone can write them.

````markdown
# Test cases not automated — manual handling required

**Run:** <run_id> · **Original portal:** <ORIGINAL_URL>

**Not automated:** <B+D> of <total> test cases — removed from the suite: <D> · kept for analysis: <B>
By category: missing-data: <n> · external-dependency: <n> · broken-page: <n> · permissions: <n> · auth-failure: <n> · missing-scaffold: <n> · implementation-failed: <n> · untestable-oracle: <n> · ambiguous: <n> · other: <n>

## Removed from the suite — run manually

These depend on data or on an external API the suite cannot drive, so their files were deleted (see the commit column). **They will not run again in CI — the team must execute them by hand.**

| Test case | Context / Flow | Category | Manual execution | Files removed | Commit |
|-----------|----------------|----------|------------------|---------------|--------|
| payment is confirmed by the gateway | payments / Gateway | external-dependency | Trigger a sandbox payment on the provider's console, then check the order status in the portal | whole `Gateway` context (5 files) | `<sha>` |

## Kept for analysis

Still scaffolded in the repo. Unblock the environment and these become automatable.

| Test case | Context / Flow | Spec | Category | Reason (short) | Suggested unblock |
|-----------|----------------|------|----------|----------------|-------------------|
| user can submit a manifestation | emailCases / PublicManifestationSubmission | `tests/emailCases/.../publicManifestationSubmission.spec.ts` | broken-page | Form page returns 500 | Fix the page on the original portal, or provide an alternative URL |

## Ambiguous — need a decision before they can be written

The description does not say what the test should verify. Each was put to the executor at the end of Phase 1 and left unresolved. **Answer the question and the case becomes implementable** — no environment work needed.

| Test case | Context / Flow | Question that blocks it |
|-----------|----------------|-------------------------|
| Filter the listing by category | bookshop / Listing | "Then the listing is updated" — assert that only the chosen category's cards remain, that the count changes, or that a specific product appears? |

## Details

One block per case, all three groups, in the order of the tables above.

### user can submit a manifestation

- **Category:** broken-page
- **Spec:** `tests/emailCases/publicManifestationSubmission/publicManifestationSubmission.spec.ts`
- **URL attempted:** <ORIGINAL_URL>/web/guest/manifestations

**BDD:**

```
<the spec's BDD comment, verbatim>
```

**What was attempted:** <what the agent did — navigation, login, retries — and exactly what failed, with error text where useful>

**Suggested unblock action:** <concrete next step for a human>

---

### payment is confirmed by the gateway

- **Category:** external-dependency
- **Status:** removed from the suite — commit `<sha>`
- **URL attempted:** <ORIGINAL_URL>/web/guest/checkout

**BDD:**

```
<the spec's BDD comment, verbatim — copied from state.json, since the spec no longer exists>
```

**What was attempted:** <what the agent did and where the external dependency blocked it>

**Manual execution required:** <the concrete steps a human runs to cover this case by hand>

**Files removed:**

```
tests/payments/gateway/gateway.spec.ts
tests/payments/gateway/config.ts
pages/payments/gateway/GatewayPage.ts
fixtures/gatewayTest.ts
project entry "gateway" in petros.config.ts
```

---
````

The BDD of a removed case comes from `state.json`, not from the spec — the entry is written before the deletion precisely so the report survives it.

Close the file with:

> Cases under **Kept for analysis**: after unblocking one, re-run `/upgrade-frontend-flow` and ask for the case to be reset to `pending` — it will be picked up by Phase 1 (and flow into Phase 2 on the next pass).
> Cases under **Ambiguous**: answer the question in the test map itself, then reset the case to `pending` on the next run — the clarification travels into the spec's BDD comment.
> Cases under **Removed from the suite**: these are permanent. Re-scaffold them with `generate-test-files` only if the data or the external dependency becomes drivable from a test.

## Optional: `report.html`

A self-contained HTML view of the same data, for stakeholders who prefer a browser over Markdown. **Optional** — `bugs.md` / `bugs.csv` / `blocked-tests.md` remain the canonical deliverables (the CSV is the machine-readable twin). When generated, it aggregates the bugs and blocked cases from `state.json`, embeds screenshots inline via `<img src="evidence/<test-id>/...">` (relative paths, same as the Markdown), and stays **self-contained** (no external CSS/JS) so it opens correctly straight from the reports folder.

## Blocked categories

The first two are the only ones that lead to deleting the test case; the rest stay in the repo.

| Category | Meaning |
|---|---|
| `missing-data` | The flow requires seed content/users/objects that don't exist in the environment and cannot be created from a test |
| `external-dependency` | The flow depends on a third-party API or external system the test cannot drive, or which is not working — and which is outside the upgrade's scope |
| `broken-page` | The page the flow needs 404s/500s or renders broken on the original portal |
| `permissions` | The available credentials lack the permission the flow exercises |
| `auth-failure` | Login itself fails for the required role |
| `missing-scaffold` | The test-map row has no matching scaffolded spec/class — usually a status mismatch (the row's status is not the one Phase 0 generated against) |
| `implementation-failed` | The environment is fine but the test could not be made green within the fix-attempt budget |
| `untestable-oracle` | The BDD `Then` is non-testable (e.g. "matches the original environment") and no concrete, observable assertion could be derived from `Steps`/`Notes` |
| `ambiguous` | The description does not say what to verify or which journey to walk, and the ambiguity queue could not resolve it — the unanswered question is recorded on the case |
| `other` | Anything else — the reason field must be specific |
