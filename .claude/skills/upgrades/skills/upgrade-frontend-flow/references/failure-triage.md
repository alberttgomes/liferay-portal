# Phase 2 failure triage — locator fix vs. bug

Every Phase 2 failure gets exactly one of two classifications:

- **`locator-fixed`** — the user-facing capability still exists on the upgraded portal, but the DOM/accessibility tree around it changed and the locator no longer resolves. Fix the page class, re-run, commit.
- **`bug`** — the capability is missing, erroring, or behaving differently. Record evidence, touch nothing.

The classification gates whether the agent writes code, so err on the side of `bug`: a wrongly-fixed locator hides a regression; a wrongly-reported bug costs one human review.

> **Triage per test case, not per file.** A spec may hold more than one `test()` — one can legitimately be a `bug` (failing correctly) while a sibling passes. Compare and classify each `test()` on its own; never conclude from a file-level pass/fail (that produced false mismatches when re-running the suite).

## Signature table — locator breakage (fix it)

| Playwright error signature | What it means |
|---|---|
| `TimeoutError: locator.click: Timeout 30000ms exceeded` … `waiting for getByRole(...)` (same for `.fill`, `.hover`, `.selectOption`) | An action step can't find its element — the classic renamed/restructured element |
| `Error: strict mode violation: getByRole(...) resolved to N elements` | The markup multiplied (e.g. a label now appears in a toolbar *and* a menu) — tighten the locator's scope |
| `locator.click: … element is not visible` / `element is not attached to the DOM` | The element moved into a collapsed/portal-rendered container |
| `locator.click: … <other element> intercepts pointer events` | An overlay or restructured wrapper sits on top — usually scoping or a state-wait in the page class, still locator territory |
| `locator.fill: Element is not an <input>` | The tag changed (e.g. AUI input → Clay component) |
| `expect(locator).toBeVisible()` timeout **on a Given/When step** that merely gates navigation/setup | A presence check broke, not the behavior under test |

## Signature table — bug (record, never fix)

| Playwright error signature | What it means |
|---|---|
| `expect(locator).toHaveText/toContainText/toHaveValue/toHaveCount` mismatch — element **found**, content differs | Behavioral or content regression |
| `expect(page).toHaveURL` mismatch after an action succeeded | Navigation/flow outcome changed |
| `page.goto: net::ERR_*`, HTTP 5xx in the failure output, `page.goto: Timeout` | Server-side failure on the upgraded portal — **but only when the rest of the suite still reaches the portal**; if it is portal-wide, it is not a bug, it is a halt (see "Portal health" in `SKILL.md`) |
| Any failure on a **Then step** after the When actions all succeeded | The outcome regressed |
| `pageerror` / unexpected dialog driving the failure | JS regression on the page |
| Element confirmed **absent** from the upgraded portal via MCP snapshot | Removed capability — the strongest bug signal, even though the raw error looks like a locator timeout |

## Mandatory confirmation protocol

**First, rule out a dead portal.** If the failures cluster on navigation across unrelated flows, stop triaging and halt per "Portal health" in `SKILL.md` — a portal that is down or erroring globally manufactures bugs that do not exist. Triage only failures that happened against a portal that was answering.

Then: never classify from the error message alone. Before finalizing:

1. `browser_navigate` to the failing page on the **upgraded** portal (logged in via MCP if the test is authenticated) and reproduce the journey up to the failing step.
2. `browser_snapshot` and answer one question: **is the same user-facing capability present?**
   - Present, but exposed under a different role/name/structure → **locator fix**. Derive the new locator from this snapshot (`locator-discovery.md`).
   - Absent, erroring, or visibly behaving differently → **bug**. You are already on the page — capture the upgrade screenshot now.
3. A locator fix confirms itself by going green. If the fixed test then fails on a content/behavior assertion, **convert the case to `bug`** — keep the locator-fix commit (it is still correct), and capture evidence for the remaining failure.

Borderline cases — apply the tie-breaker "would a human user succeed at this step?":

- Button renamed "Add" → "New Entry": user succeeds (the action exists) → locator fix. If the BDD explicitly asserts the label text, also note the rename in the bug report as Low severity.
- Field moved from the main form into a "Advanced" collapsible: user succeeds with an extra click → locator fix **plus** a Low/Medium bug noting the UX change if the BDD describes the flow without that click.
- Menu option gone entirely: user fails → bug.

## Evidence capture (bug cases)

Into `.upgrade-frontend-flow/reports/<run_id>/evidence/<test-id>/` (`<test-id>` = slugified `<context>-<flow>-<test-case-name>`):

| File | How |
|---|---|
| `upgrade.png` | `browser_take_screenshot` of the failing page/state on the upgraded portal (you are there from the confirmation protocol) |
| `original.png` | Navigate the same journey on the **original** portal with MCP, screenshot the same page/state — this is the "expected" image |
| `trace.zip` | Copy from `test-results/<test-dir>/trace.zip` (the run uses `--trace retain-on-failure`) |
| `error.txt` | The full Playwright error block from the JSON/line reporter output |
| `urls.txt` | Two lines: `original: <url>` / `upgrade: <url>` — the page URLs on each portal |

Record in the state file: `bug_id` (sequential `#001`, `#002`, …), a one-line summary shaped `<flow> - <short error>` (it becomes the bug summary), and the severity per the rubric below.

## Severity rubric

- **Critical** — the flow is impossible: page does not load, login fails, data does not save, a primary user journey is broken.
- **High** — a primary action of the flow fails or produces a wrong outcome; capability removed.
- **Medium** — the flow completes but an assertion on secondary content/state fails; degraded UX (extra steps, moved capability).
- **Low** — text/label drift, cosmetic divergence caught by an assertion.
