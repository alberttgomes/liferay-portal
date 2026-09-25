---

name: upgrade-frontend-flow
argument-hint: "[path/to/test-map.tsv]"
description: Implement, run, and triage a Playwright regression suite across a Liferay upgrade, from a test-map .tsv through to a bug report. Use when the user invokes `/upgrade-frontend-flow`, asks to implement a test map, or asks to run the frontend regression flows.

---

# upgrade-frontend-flow

Implements, runs, and triages the frontend team's Playwright regression suite across a Liferay upgrade. The suite is written once against the **original** portal (the pre-upgrade version, running on a remote server) and then executed against the **upgraded** portal (running on localhost) to separate harmless DOM changes from real functional regressions.

A setup phase plus three working phases, tracked in a state file so the run survives interruptions:

0. **Phase 0 — Create environment.** The executor drops the test-map `.tsv` at the repo root and answers one block of questions. The agent creates the branch, writes `.env`, runs the `generate-test-files` script, reconciles what the script leaves out, and commits the scaffold.
1. **Phase 1 — Implement.** For every test case in the test map, fill in the scaffolded page-object class and spec body from the BDD comment, verify it green against the original portal, and commit in the team's pattern.
2. **Phase 2 — Run & fix.** Execute the suite against the upgraded portal. Failures caused purely by locator changes are fixed and committed; everything else is classified as a **bug** and left untouched for the report.
3. **Phase 3 — Report.** Produce `bugs.md` and `bugs.csv` for regressions, and a separate `blocked-tests.md` for the test cases that could not be automated — including the ones deleted from the suite because they depend on missing data or an external API, which the team now has to run by hand.

Everything this skill writes — code, commits, state, reports — lives in the **automated-tests repository**, never in a Liferay workspace.

## Prerequisites

- The working directory is the root of the automated-tests repository, with a **clean working tree**. If the tree is dirty, stop and ask the user how to proceed.
- The **test-map `.tsv` is at the repo root**. That is the whole hand-off — the scaffold, the `.env`, and the branch are all created by [Phase 0](#phase-0--create-environment).
- Node.js and the repo's dependencies are installable (`npm install`) — every verification rung needs `npx`, and the scaffolder formats its output with the repo's Prettier when present. The scaffolder itself ships with this skill and is seeded into `.scripts/` when the repo has no copy.
- Playwright MCP is available for live locator discovery. If `/mcp` shows it as `failed`, the most common cause is missing Node.js or Playwright browsers — instruct the user to run `npx playwright install chromium`.
- The original portal is reachable. The upgraded portal is only needed from Phase 2 onward. Either one failing to run stops the activity — see [Portal health](#portal-health--halt-rule).
- Read `references/team-conventions.md` **before writing or committing any code** — it is the distilled team standard and is non-negotiable.

## Configuration resolution

No Liferay-workspace `CLAUDE.md` is involved. Resolve every input in this order: **invocation arguments → repo files → the Phase 0 question block.**

Arguments supply the test map (`*.tsv`); the repo supplies the project name when a `{project}.config.ts` already exists, and `.env` supplies the URLs and credentials once Phase 0 has written it. Everything still missing is asked once, in Phase 0, and persisted in the state file so a resume never re-asks. The branch is **always** asked, never derived.

---

## Step 0 — Invocation and resume routing

1. Verify the prerequisites above (clean tree, a `.tsv` at the repo root or given as an argument).

1. Read `.upgrade-frontend-flow/state.json`:

| State | Action |
|---|---|
| File absent | Fresh run — [Phase 0](#phase-0--create-environment), then build the inventory, then Phase 1 |
| `phase: "phase0"` | Phase 0 was interrupted — resume it at the first incomplete step (each one is idempotent) |
| `phase: "phase1"` | Probe the original portal; if up, resume Phase 1 at the first `pending` test case |
| `phase: "phase2"` | Run the Phase 2 gate (health check); if up, resume Phase 2 over the cases still in `implemented` |
| `phase: "report"` | Regenerate the Phase 3 reports, then re-run the original-portal cleanup (idempotent) |
| `phase: "done"` | Ask: re-run Phase 2 (e.g. after a portal fix), or start a fresh run? A fresh run archives the old state to `.upgrade-frontend-flow/state-<run_id>.json` |

1. On anything but a fresh run, load `.env` and probe the phase's portal per [Portal health](#portal-health--halt-rule). If it does not answer, halt there — there is nothing to work against.

On any resume, print a one-line summary of where the run stands before continuing (e.g. `Resuming Phase 1: 12 implemented, 2 blocked, 9 pending`).

---

## Phase 0 — Create environment

Runs once, on a fresh run, before anything else. Every step is idempotent — an existing branch, `.env` or scaffolder copy is reused, never recreated — so an interruption resumes here cleanly. **Read `references/environment-setup.md` first**: it carries the script's flags, the `.env` naming contract and the reconciliation checklist.

1. **Open the run.** Mint `run_id = <YYYYMMDD-HHMMSS>`, create `.upgrade-frontend-flow/`, write the state file with `phase: "phase0"`, and offer to add the dir to `.gitignore` (`<TICKET> Add upgrade-frontend-flow state dir to gitignore`).

1. **Find the test map.** The `.tsv` argument, else the only one at the repo root. Several → ask which. None → ask for the path.

1. **Read the `.tsv` before asking anything** — columns by header name, per the reference. Collect which roles need authentication and which `Test Status` values occur, with row counts. This is what makes the next step one pass instead of three.

1. **Ask the executor, once**, with the detected values pre-filled as defaults:

   - **Branch — always ask, never derive.** Accept a branch name and **propose** it back to confirm or overwrite.
   - **Project name** (the script's `-n`), **status filter** (offer the values found, with counts), **`ORIGINAL_URL`**, **`UPGRADE_URL`**, and one credential pair per role detected above.

1. **Create the branch** off the current one; if it exists, check it out and continue. **Do not push.**

1. **Write `.env`** from `assets/env.example`, after adding `.env` to `.gitignore`. `BASE_URL` starts on the original portal — record `base_url_var: "BASE_URL"` in the state file.

1. **Seed the scaffolder** if `.scripts/generate-test-files.sh` is absent (`cp` this skill's `assets/` copy, keep it executable); otherwise use the repo's.
1. **Run it** — `-n <project> -s <status>`, always non-interactive, plus `-a <storageState>` only when the map has authenticated rows (exact invocation in the reference). A non-zero exit stops Phase 0: print the output verbatim and halt. Do not edit the script to get past an error.

1. **Reconcile what it leaves out** — both mandatory, both detailed in the reference: patch `playwright.config.ts` to the team defaults, and create the `setup` project when the map needs authentication.

1. **Verify.** `npx tsc --noEmit` and `npx playwright test --list` clean, the listing showing every project with no unresolved dependency.

1. **Commit** `<TICKET> Create <project-name> tests environment` — the generated tree, the seeded script, the `.gitignore` change. **Never the `.env`.**

1. Set `phase: "phase1"` and build the inventory below.

---

### Building the test-case inventory (fresh run only)

1. Parse the test-map `.tsv` with the **same column resolution the script uses** — by header name, case- and accent-insensitive, with the header being the first row that carries both a suite and a test-name column. The accepted header aliases (`Context`/`Describe`/`Module`/`Feature` for the suite, `Steps`/`BDD` for the steps, and so on) are tabulated in `references/environment-setup.md`. Do not assume a fixed column list: the sheets vary, and rows above the header are skipped by design.

1. Keep only rows whose status equals the **status filter chosen in Phase 0** — an exact, case-sensitive match, because that is what the script generated against. A row with any other status has no scaffold and must not enter the inventory. If the file has no status column at all, every row is in scope.

1. Map each row to its scaffolded files. The suite column splits on `/` into nested folders, so a suite `Bookshop / Listing` produces `tests/bookshop/listing/listing.spec.ts`, `pages/bookshop/listing/ListingPage.ts` and `fixtures/bookshopListingTest.ts`; the spec's `test.describe()` title is the suite string and the `test()` title is the test-name column. Record the row's **section** — the first suite segment (`Bookshop`) — on the case. If a row has no matching scaffold file, mark it `blocked` with category `missing-scaffold` instead of inventing files.

1. Initialize every case as `pending` and write the state file (schema: see `assets/flow-state.example.json`).

### Pre-flight confirmation

Before starting Phase 1, show:

```
About to implement N test cases (M flows):
  - Original portal: <ORIGINAL_URL>
  - Upgraded portal: <UPGRADE_URL> (used in Phase 2)
  - Ticket / branch: <TICKET> / <branch>
  - Test map:        <path> (N rows with status "<status filter>")
  - Authenticated cases: <X> / anonymous: <Y>

Proceed? [y/N]
```

Wait for confirmation before touching code.

---

## Portal health — halt rule

A portal that cannot be run is not a test result. **Any problem running the original or the upgraded portal stops the activity and gets reported for investigation** — the run then resumes from where it stopped once the portal is fixed. Never absorb a portal problem by marking cases blocked: that buries the real issue and throws away work that was never actually attempted.

**Health check.** Both portals get the same probe — the original before Phase 1 (Step 0 and every Phase 1 resume), the upgraded before Phase 2:

```bash
curl -sf -m 10 -o /dev/null "$ORIGINAL_URL/c/portal/login"   # Phase 1
curl -sf -m 10 -o /dev/null "$UPGRADE_URL/c/portal/login"    # Phase 2
```

**One page broken is not the portal broken.** This is the distinction that decides between `blocked` and a halt:

| Signal | Action |
|---|---|
| One flow's page 404s / 500s while the rest of the portal answers | `blocked` (`broken-page`) — record it and move to the next case |
| Connection refused, connection timeout, 502 / 503 / 504 | **HALT** |
| `/c/portal/login` or the home page does not render | **HALT** |
| Several consecutive cases failing on navigation (not on assertions) | **HALT** — treat it as the portal, not as N coincidences |
| Portal reachable but throwing 500s across unrelated flows | **HALT** |

**Halt procedure:**

1. Checkpoint `state.json`.
2. Print the [progress block](#progress-reporting) — the user must see exactly where the run stands.
3. Print the problem: which portal and URL, the exact command run, the error text verbatim, and the path to any trace or artifact.
4. **Stop.** Leave the remaining cases as `pending` (Phase 1) or `implemented` (Phase 2) — never `blocked`.
5. Close with the resume instruction:

   > The original portal at `<ORIGINAL_URL>` stopped responding — see the error above.
   > Investigate and fix it, then re-run `/upgrade-frontend-flow`: the run resumes at `<flow>` / `<test case>` with no work lost.

Do not try to work around a halted portal — no retry loops beyond the probe, no switching to the other portal, no fixing the portal or its data.

---

## Gotchas

- **`isAuthenticated()` only checks that the state file exists, not that its session is still
  valid.** A dead session therefore green-lights the guard and every authenticated spec fails
  downstream.
- **En-masse failures that land on `/c/portal/login` are an expired session, not a regression —
  and not a portal outage either.** The portal answers its health probe, so this is *not* the
  [halt rule](#portal-health--halt-rule): `rm -f tmp/.auth/*.json` and re-run `--project=setup`
  before triaging any of them as bugs or halting the run.
- **`process.loadEnvFile()` and dotenv never override a variable already set in the environment**,
  so a CLI-prefixed base URL deterministically wins over `.env` — that is why the prefix form is
  safe.
- **Do not `grep | cut` values out of `.env`** — that keeps the surrounding quotes and yields an
  invalid base URL. Source it instead: `set -a; . ./.env; set +a`.
- **`trace: 'on-first-retry'` never captures a trace when `retries: 0`** — a config combination that
  silently produces no evidence for the failures you most need it for. The scaffolder generates
  exactly that pair, which is why Phase 0 patches the config.
- **`BASE_URL` is the only variable `use.baseURL` reads** — `ORIGINAL_URL` lands in
  `environments.originalUrl` and feeds nothing. Without it the suite has no base URL at all.
- **The scaffolder's `-a` emits `dependencies: ['setup']` but never creates the `setup` project** —
  that dangling dependency stops the suite from even listing, so Phase 0 creates it.
- **Re-running the scaffolder discards the Phase 0 reconciliation**, rewriting both configs
  wholesale. Run it once; add a late test-map row by hand, or re-run and redo the reconciliation.
- **Its status match is exact and case-sensitive**, defaulting to `Needs Automation`, not
  `Automated` — a mismatch scaffolds nothing and exits 1.

## Phase 1 — Implement tests against the original portal

Work through `pending` cases in test-map order, grouped by flow (all cases of one flow before moving to the next — they share a page class).

For each test case:

1. **Read the intent — and gate the BDD.** The spec's BDD comment (Given/When/Then) plus the test map's `Steps` and `Notes` columns are the specification. Do not invent behavior beyond them. Two distinct gates apply before writing anything:

   - **Non-testable oracle.** When the `Then` is something the running test cannot observe — e.g. "matches the original environment" — do not write a hollow assertion or pad `test.step`s to look thorough. Use judgment: derive a concrete, observable assertion from the `Steps`/`Notes` (e.g. "the carousel shows ≥ 1 slide", "the listing renders ≥ 1 card", "the page heading is X"). If nothing concrete can be derived, mark the case `blocked` with category `untestable-oracle` for human review rather than committing a weak test.
   - **Ambiguous intent.** When the description does not say **what the test should verify** or **which journey to walk**, the case is not implementable and no amount of looking at the portal will settle it. **Skip it immediately** — do not spend fix attempts, do not open the browser. Set status `ambiguous`, record the BDD verbatim plus the one specific question that would unblock it in `ambiguous_question`, and move to the next case. The [Ambiguity queue](#ambiguity-queue) puts it to the executor at the end of the phase.

1. **Read the scaffold.** The page class (`pages/.../<Flow>Page.ts`), the fixture (`fixtures/<flow>Test.ts`), and the feature `config.ts`. Understand what already exists before adding anything.

1. **Write the code straight from the test case**, per `references/team-conventions.md`. The BDD comment, the test map's `Steps`/`Notes`, and the page classes already implemented in the same context are enough for a first version. Do **not** browse the portal first — the verification run below is the cheaper feedback loop, and an exploratory pass per case is what makes a run drag on. The essentials:
   - Page class: `readonly` locators initialized in the constructor; action methods; **no assertions in classes** — assertions live exclusively in the spec.
   - Spec: keep the BDD comment; `test.describe()` wrapper; assertions mirror the `Then` clauses. **No `test.step()` by default** — add one only when the test has two or more distinct multi-action phases that are hard to read as a flat sequence. Never one per BDD line, never around a single navigation or a single assertion. Most tests need none. Follow the web-first sync rules in `references/team-conventions.md` (no `waitForTimeout`/`networkidle`; web-first assertions).
   - **Comments: the BDD block and nothing else** — keep the generated `/** … */` verbatim, and beyond it comment only a non-obvious *why*. **Clean up what the test creates** in `afterEach`; a test that leaves data behind poisons the next run and the Phase 2 baseline (read-only flows need no hook). Both rules in full: `## Comments` and `## Test lifecycle and teardown` in `references/team-conventions.md`.
   - Authentication: when the row is flagged as needing authentication, the feature `config.ts` already carries `dependencies: ['setup']` and the `storageState` path (Phase 0 wired both). If the flow needs a role with no setup project yet, **create it** — spec under `tests/@setup/`, the `isAuthenticated` guard, and the project entry — per `references/environment-setup.md`, and commit it as `<TICKET> Add <role> setup project`. Compose the login fixture via `mergeTests` when the flow itself exercises login.
   - When unsure about a Playwright API, check the official Playwright documentation (WebFetch `https://playwright.dev/docs/...`) rather than guessing.

1. **Verify.** Climb the ladder (see [Verification loop](#verification-loop)) running against the original portal:

   ```bash
   BASE_URL=$ORIGINAL_URL npx playwright test <spec-path>
   ```

   Iterate fix → re-run until green, with a budget of **~4 fix attempts**. Then:

   - **The portal itself is failing** (connection refused, 502/503/504, login page not rendering, the previous cases failing the same way) → do not mark anything blocked. Halt per [Portal health](#portal-health--halt-rule).
   - **This flow's environment is at fault** (its page broken, data missing, permission denied) → mark the case `blocked` with the appropriate category.
   - **The implementation is at fault and the budget is exhausted** → mark it `blocked` with category `implementation-failed`.

   In the two blocked cases, revert the unverified code changes for that case (keep the tree clean) and move on — never leave a red test committed.

1. **Commit** — two commits per flow, staged by explicit path (never `git add -A`), following [Commit convention](#commit-convention):
   - Page class (and fixture, if touched for this flow): `<TICKET> Feat <flow-name> class` — use `Add` instead of `Feat` when the scaffolded class was an empty stub.
   - Spec: `<TICKET> Add <test-case-name> test case`.
   
   When several test cases of the same flow are implemented together, the class commit covers the class work for all of them; each test case still gets its own spec commit (commits are review checkpoints).

1. **Checkpoint.** Update the case's entry in the state file (`implemented` or `blocked`, commit SHAs, timestamp) after **every** case, never in batch.

### When to open the browser

There is no discovery pass. Reach for Playwright MCP in exactly two situations:

- **You know what the test must verify, but not how the flow behaves.** Walk the journey once — `browser_navigate`, click through the steps the test map describes, `browser_snapshot` where it matters — then write the test from what you saw.
- **The verification run failed on an element you cannot resolve from the error.** Navigate to that one page, snapshot, derive the one locator you need, and go back to coding.

**A walk resolves behavior, never intent.** If what is unclear is *what the test should assert* or *which journey is meant*, the portal cannot answer that — no screen tells you which of several plausible outcomes the test map had in mind. That case is `ambiguous`: skip it on the spot and let the [Ambiguity queue](#ambiguity-queue) ask the executor. Browsing to guess an intent produces a confident test that verifies the wrong thing, which is worse than no test.

Either way it is a *targeted* look, not a survey: the page the step targets, not the whole flow. `references/locator-discovery.md` has the derivation rules and the Liferay-specific traps (generated portlet IDs, Clay portals rendering at `<body>` level, SennaJS navigation, iframe dialogs) — read it when you are deriving a locator, not before starting a case. For authenticated cases, log in once via MCP with the `.env` credentials and keep the session for the rest of the walk.

**One walk per test case.** It counts inside the fix-attempt budget above; if the case is still red after it, mark it blocked rather than browsing further.

### Progress reporting

Silence is a failure mode: a long run with no output leaves the user unable to tell progress from a stall. **Print a progress block when each flow finishes** — not per case, since the work is already grouped by flow:

```
Flow 4/17 — JoinSite (siteMembership): implemented 3, blocked 1, deleted 0
Overall: 11/48 implemented · 3 blocked · 2 deleted · 32 remaining (13 flows left)
```

Rules:

- All counts come from `state.json` — it is already checkpointed per case, so it is the single source of truth and the numbers survive a resume.
- Print the same block **before every stop**, whatever the reason: a portal halt ([Portal health](#portal-health--halt-rule)), the deletion confirmation, the phase exit, or an error you are about to surface. The user must never learn where a run stood only after asking.
- Chat output only — no file. `state.json` is the durable record.

### Blocked test cases

When a case cannot be implemented because of the environment, record — in the state file, structured — the category (`broken-page`, `missing-data`, `external-dependency`, `permissions`, `auth-failure`, `missing-scaffold`, `implementation-failed`, `untestable-oracle`, `ambiguous`, `other`), a precise reason (what was attempted, what failed, URLs involved), and a suggested unblock action. These feed `blocked-tests.md` in Phase 3. Never attempt to "fix" the portal or its data to unblock a test — that contaminates the baseline.

Anything that cannot be automated — a data dependency, an external API, a flow that simply does not work — must reach the report so a human can run it manually. `blocked-tests.md` is that handoff; a case dropped silently is worse than a case reported as blocked.

### Removing non-automatable test cases

Some cases will never be automatable in this suite: the flow depends on data that does not exist, or on a third-party API the test cannot drive. Keeping their scaffolds around leaves dead files that fail forever. **Report them, then delete them.**

**Only two categories trigger deletion:** `missing-data` and `external-dependency`. Everything else (`broken-page`, `permissions`, `auth-failure`, `implementation-failed`, `untestable-oracle`, `ambiguous`, `missing-scaffold`) stays in the repo — those are fixable, and the human queue exists to fix them.

**Record before deleting.** Write the case's full blocked entry to `state.json` first — BDD verbatim, the reason, the manual steps a human needs, and the list of paths about to be removed. The report has to survive the deletion; once the spec is gone the BDD is only recoverable from state.

**Scope of the deletion:**

- **One case.** Remove its `test()` block from the spec. If it was the only case in the spec, delete the spec file too.
- **A whole flow or context.** When *every* case of the flow carries the dependency, remove the whole thing: the spec directory, the page class, the fixture, the feature `config.ts`, its project entry in `{project}.config.ts`, and any `@setup` project used only by it.

**One batch confirmation**, at Phase 1 exit — never per case:

```
About to delete 4 test cases (1 full context):

  external-dependency:
    - tests/payments/gateway/gateway.spec.ts (whole context)
      + pages/payments/gateway/GatewayPage.ts
      + fixtures/gatewayTest.ts
      + tests/payments/gateway/config.ts
      + project entry in petros.config.ts
  missing-data:
    - tests/crm/leadImport/leadImport.spec.ts (1 of 3 cases)

All 4 are recorded in blocked-tests.md.
Delete and commit? [y/N]
```

If declined: delete nothing, leave the cases `blocked` with `delete_declined: true`, and report them as usual.

After deleting, verify nothing is orphaned — `npx tsc --noEmit` and `npx playwright test --list` (no dangling import, no project pointing at a directory that no longer exists) — then commit: `<TICKET> Remove <test-case-name> test case`, or `<TICKET> Remove <flow-name> context` for a whole flow. Set status `deleted` with `deleted_paths[]`.

### Ambiguity queue

The cases skipped as `ambiguous` are the one class of blocker a person clears in a minute, so they get asked rather than reported. When no case is `pending` and at least one is `ambiguous`, put the queue to the executor **before** the deletion batch — a case that gets clarified must not have been deleted first.

Ask **one case at a time**, giving them everything needed to answer without opening the sheet: the case name, the BDD as scaffolded, the `Steps`/`Notes`, then the one specific question, with the plausible readings as options plus "something else / skip".

- **Clarified** → write the answer to `clarification`, set the case back to `pending`, and implement it in the normal loop before the phase closes. The clarification is part of the specification from then on: it travels into the spec's BDD comment so the next reader sees why the assertion is what it is.
- **Not clarified** (the executor does not know either, or defers) → leave it `blocked` with category `ambiguous`. It reaches `blocked-tests.md` with the question intact, which is what a human needs to resolve it later.

Never guess an answer to close the queue, and never let a silent skip stand in for asking — an unasked ambiguous case is the one failure mode this queue exists to prevent.

### Phase 1 exit

When no case is `pending`:

1. Run the [Ambiguity queue](#ambiguity-queue), if any case is `ambiguous`. Implement whatever it clarified.
1. Run the deletion batch above, if any case qualifies.
1. Set `phase: "phase2"` in the state file.
1. Print the summary (`implemented: N, blocked: M, deleted: D`).
1. Proceed to the Phase 2 gate.

---

## Phase 2 gate — upgraded portal health check

The [halt rule](#portal-health--halt-rule) applied at the phase boundary. Runs at the end of Phase 1 and at the start of every resumed invocation while `phase: "phase2"`:

```bash
curl -sf -m 10 -o /dev/null "$UPGRADE_URL/c/portal/login"
```

If the check fails, print:

> Phase 1 complete: N test cases implemented and committed (M blocked).
> The upgraded portal at `<UPGRADE_URL>` is not responding.
> Start it, then re-run `/upgrade-frontend-flow` — the run will resume directly into Phase 2.

and **stop**. The state file already says `phase2`, so the next invocation lands here without redoing any Phase 1 work.

The same rule holds *inside* Phase 2: if the upgraded portal goes down or starts erroring across unrelated flows mid-suite, halt and report it. Do not triage the resulting failures — a dead portal produces bugs that do not exist.

---

## Phase 2 — Run and fix against the upgraded portal

1. **Reset auth state.** Delete `tmp/.auth/*.json`. The stored `storageState` carries cookies scoped to the original portal's domain, and the `isAuthenticated()` guard in `@setup` would skip re-login — without this reset every authenticated test fails spuriously. Note the guard only checks the state file **exists**, not that its session is still **valid**: on a long run the session can expire mid-suite, after which every authenticated spec fails against the login page. If authenticated specs start failing en masse on presence/URL assertions that land on `/c/portal/login`, that is an **expired session, not a regression** — `rm -f tmp/.auth/*.json` and re-run `--project=setup` to mint fresh cookies before re-triaging.

1. **Sanity run.** Pick one test that passed in Phase 1 and run:

   ```bash
   BASE_URL=$UPGRADE_URL npx playwright test <spec-path> --trace on
   ```

   Confirm from the output/trace that requests actually hit the upgraded host before launching the full suite. (`process.loadEnvFile()` and dotenv never override variables already set in the environment, so the CLI-prefixed value deterministically wins over `.env`.)

1. **Full run:**

   ```bash
   BASE_URL=$UPGRADE_URL PLAYWRIGHT_JSON_OUTPUT_NAME=.upgrade-frontend-flow/reports/<run_id>/results.json \
     npx playwright test --trace retain-on-failure --reporter=line,json
   ```

1. **Triage every failure** using `references/failure-triage.md`. The decision in one line: *the same user-facing capability still exists but the DOM around it changed* → **locator fix**; *the capability is missing, erroring, or behaving differently* → **bug**. Before finalizing either classification, navigate the failing page on the **upgraded** portal with Playwright MCP and snapshot it — never classify from the error message alone.

   - **Locator fix:** derive the new locator from the upgraded portal's accessibility tree, change **only the page class**, re-run that spec (still against `UPGRADE_URL`) until green, commit `<TICKET> Fix <element-name> locator`, set status `locator-fixed`. Do **not** re-check the locator against the original portal — the upgraded portal is the suite's only target from here on, and the original is dropped from the repo at the end of Phase 3. If the fixed test then fails on a content/behavior assertion, convert the case to `bug` (keep the locator-fix commit — it is still correct).
   - **Bug:** do **not** modify any code. Capture evidence into `.upgrade-frontend-flow/reports/<run_id>/evidence/<test-id>/`:
     - `upgrade.png` — screenshot of the failing page/state on the upgraded portal (how it is);
     - `original.png` — screenshot of the same page/state on the original portal (how it should be);
     - the trace zip produced by the run, and the full error text;
     - both page URLs (original and upgrade).
     
     Set status `bug` with a one-line summary.

1. Tests that pass get status `upgrade-pass`. Checkpoint the state file after every test's outcome.

1. **Report progress per flow**, under the same rules as [Progress reporting](#progress-reporting), with the triage counters:

   ```
   Flow 4/17 — JoinSite (siteMembership): pass 2, locator-fixed 1, bugs 1
   Overall: 22/48 triaged · 15 pass · 4 locator-fixed · 3 bugs · 26 remaining (13 flows left)
   ```

   The full run produces every result at once, but triage is still worked flow by flow — report as each flow's triage closes, not only at the end.

1. **Phase 2 exit:** when no case remains in `implemented`, set `phase: "report"`, print a summary (`pass: N, locator-fixed: M, bugs: K`), and proceed.

### Checklist — per test case

Copy into the state file's per-case entry. Triage is **per `test()`**, not per file — classifying at
file level produced false mismatches on a real run.

- [ ] Phase 1 — case verified green against the original portal, or recorded `blocked` with a category
- [ ] Phase 1 — an unclear *intent* was skipped as `ambiguous` and asked, never guessed at
- [ ] Phase 1 — no unverified change left committed (revert before marking `blocked`)
- [ ] Phase 2 — failure classified from evidence, **never from the error message alone**
- [ ] Phase 2 — a locator fix works on **both** portals, re-run against each
- [ ] Phase 2 — every non-locator failure carries its evidence set (screenshots, trace, URLs)
- [ ] State file written after the transition

## Phase 3 — Reports

Generate three files under `.upgrade-frontend-flow/reports/<run_id>/`, following the exact templates in `references/report-formats.md`:

- `bugs.md` — human-readable regression report: header with both URLs and run statistics, summary table by flow, then one block per bug (BDD steps as steps-to-reproduce, expected behavior = original portal, current behavior = upgraded portal, side-by-side screenshots, evidence paths).
- `bugs.csv` — one row per bug, the machine-readable twin of `bugs.md`. **Summary** = `<flow> - <short error>`; **Description** = the test case's BDD text + the current-vs-expected screenshots + both page URLs. Remaining columns mirror the agent's audit CSV (`Bug ID`, `Issue Type`, `Priority`, `Labels`, `Components`, `Environment`, `Attachments`).
- `blocked-tests.md` — the manual handoff, in three parts: the cases **removed from the suite** (the `missing-data` / `external-dependency` deletions — what a human must now run by hand, and which files were removed), the cases **kept for analysis** (BDD, what was attempted, why it failed, suggested unblock action), and the **unresolved ambiguous cases** (BDD plus the question the executor could not answer).

### Drop the original portal (after the reports are written)

From here the suite runs **only against the upgraded portal** — the original was the baseline for triage and that job is done. Strip it from the code so nobody inherits a suite pointing at a portal that will be decommissioned.

Order matters: the reports keep their original-portal references on purpose (they are the "expected behavior" evidence), so this runs **after** the three files exist. It is idempotent — a resume in `phase: "report"` can re-run it harmlessly.

1. Point the config at the upgraded portal. `use.baseURL` reads `BASE_URL`, so make the upgrade URL its value: set `BASE_URL` to the upgrade portal in `.env`, and if any code still reads `environments.originalUrl`, repoint it at `environments.baseUrl`.
1. `rm -f tmp/.auth/login-original.json` and any other auth state scoped to the original portal.
1. `grep -rn ORIGINAL_URL` across the repo, **excluding `.upgrade-frontend-flow/`**, and remove every remaining occurrence in code, fixtures, and config.
1. In `.env`, point `BASE_URL` at the upgrade portal and drop `ORIGINAL_URL`. The file stays uncommitted; note in `bugs.md` that `ORIGINAL_URL` is no longer read.
1. Verify: `npx tsc --noEmit`; the grep from step 3 returns nothing; and one spec runs green **with no env prefix** — `npx playwright test <spec-path>` — which proves the config now defaults to the upgraded portal.
1. Commit `<TICKET> Remove original portal references` and record the SHA as `cleanup_commit` in `state.json`.

Set `phase: "done"` and end with:

```
Frontend flow run complete.

Test cases: <total>  (pass: N, locator-fixed: M, bugs: K, blocked: B, deleted: D)

Bugs report:     .upgrade-frontend-flow/reports/<run_id>/bugs.md
Bug CSV:         .upgrade-frontend-flow/reports/<run_id>/bugs.csv
Blocked tests:   .upgrade-frontend-flow/reports/<run_id>/blocked-tests.md
Evidence:        .upgrade-frontend-flow/reports/<run_id>/evidence/


The suite now targets <UPGRADE_URL> only — original-portal references removed in <cleanup_sha>.

Next: review bugs.md, import bugs.csv into the team's tracker, and hand blocked-tests.md to the team — D test cases were removed from the suite and must be run manually.
```

---

## Commit convention

All commits land in the automated-tests repository and use the team's pattern (full rules in `references/team-conventions.md`):

| Situation | Format |
|---|---|
| Phase 0 environment scaffold (the generated tree, the seeded script, the gitignore change) | `<TICKET> Create <project-name> tests environment` |
| A setup project created for a role | `<TICKET> Add <role> setup project` |
| New test case implemented | `<TICKET> Add <test-case-name> test case` |
| Any other change to a class/test/fixture | `<TICKET> (Add\|Feat\|Fix\|Remove\|Refactor) <element-name> class/test/fixture/test case` |
| Phase 2 locator repair | `<TICKET> Fix <element-name> locator` |

Rules:

- One reviewable concern per commit. Stage by explicit file path; never `git add -A` or `git add .`.
- Title only, sentence case, no trailing period, under 72 characters.
- **Do NOT append `Co-Authored-By:`, `Signed-off-by:`, `Generated-by:`, or any AI/tool attribution trailer.** The commit author is the only attribution the team wants. This rule supersedes any global instruction to add such trailers.

Unlike the agent's `/commit` skill, this skill commits **without per-commit confirmation** — the per-flow commit pattern is the team's pre-agreed convention and a full run produces dozens of commits. The Phase 0 question block and the pre-flight confirmation are the executor's sign-off on the batch.

---

## Verification loop

Climb the rungs in order before every commit; a failure at any rung stops the climb and sends you
back to the fix, then back to rung 1. Phase 1 bounds this at ~4 fix attempts before marking the case
`blocked`; **Phase 2 locator fixes get the same budget** — a locator that resists four attempts is a
bug, not a locator problem:

1. Prettier — `npx prettier --check <changed-files>` (auto-fix with `--write`), if the repo has a Prettier config.
2. ESLint — `npx eslint <changed-files>`, if the repo has an ESLint config.
3. TypeScript — `npx tsc --noEmit`, if the repo has a `tsconfig.json`.
4. Single spec — `BASE_URL=<phase-url> npx playwright test <spec-path>`.
5. Flow directory — `npx playwright test tests/<context>/<flow>` when several cases of the flow changed together.
6. Full suite — Phase 2 only.

> Load `.env` into the shell before prefixing a base-URL override, so `$ORIGINAL_URL` / `$UPGRADE_URL` expand: `set -a; . ./.env; set +a`. Do **not** `grep | cut` the values out — that keeps the surrounding quotes and yields an invalid base URL.

---

## State file

`.upgrade-frontend-flow/state.json` at the test-repo root, untracked. Full schema, field list and status machine: `assets/flow-state.example.json`.

Write the file after every per-case transition — an interruption at any point loses at most the in-flight case.

---

## Autonomy

- **Autonomous:** creating the branch the executor confirmed; writing `.env` and the `.gitignore` entry; seeding and running the generator script; reconciling `playwright.config.ts` to the team defaults; creating a `setup` project for a role that needs one; locator discovery via MCP; writing page classes/fixtures/specs; running Playwright/Prettier/ESLint/tsc; committing to the test repo in the team's pattern; fixing locators in Phase 2; creating the bug sub-tasks under a confirmed section↔Task mapping; state checkpointing; evidence capture; report generation; the post-report cleanup of original-portal references in code and config.
- **Requires user confirmation:** the Phase 0 question block (branch, project name, status filter, URLs, credentials); starting the Phase 1 batch (pre-flight); every case in the [Ambiguity queue](#ambiguity-queue); deleting non-automatable test cases or whole contexts (one batch confirmation at Phase 1 exit); including test-map rows whose status is not the chosen filter; adding the state dir to the test repo's `.gitignore`; archiving state for a fresh run; any `git push`.
- **Never:** continue a run while a portal itself is unhealthy — halt and report it for investigation; finish a flow without printing the progress block; fix a Phase 2 failure that is not purely a locator breakage — anything touching behavior, content, or assertions is a bug for the report; modify the portal or its data to unblock a test; delete a test case for any reason other than `missing-data` / `external-dependency`, or before it is recorded in the report; **commit the `.env`** or inline a credential in code, commits, or chat output; **implement an `ambiguous` case by guessing its intent** instead of asking; **re-run the generator script without redoing the config reconciliation**; commit to any repository other than the automated-tests repo; push without being asked.
