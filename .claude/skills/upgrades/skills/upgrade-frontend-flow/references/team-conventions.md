# Team conventions for the automated-tests repository

Distilled from the Upgrade team's "Patterns and Best Practices in Playwright testing" standard, plus the strongest patterns observed across the team's existing project branches. These rules are mandatory — when an existing branch deviates from them, follow the rule, not the branch.

## Repository shape

One project per branch. Every project follows the same TypeScript layout:

```
fixtures/                  # test.extend() fixture definitions, one per flow
pages/                     # page-object classes, mirrored by context/flow
tests/
├── @setup/                # auth setup projects writing storageState to tmp/.auth/
└── <context>/<flow>/      # spec files + per-feature config.ts
utils/                     # shared helpers (e.g. isAuthenticated)
playwright.config.ts       # imports the per-feature configs as projects
{project}.config.ts        # env + credentials, loaded from .env via loadEnvFile()
.scripts/                  # generate-test-files.sh — the scaffolder
.env                       # BASE_URL, ORIGINAL_URL, UPGRADE_URL, credentials — never committed
```

## Commit pattern

Three formats, all ticket-prefixed. With no ticket, use the `NOISSUE` prefix required by `.claude/rules/commit.md`:

```
NOISSUE Create project-name tests environment        # initial scaffold (generate-test-files run)
NOISSUE Add test-case-name test case                 # one commit per test case — these are review checkpoints
NOISSUE (Add|Feat|Fix|Remove|Refactor) element-name class/test/fixture/test case
```

- One concern per commit. The established shape is **two commits per flow**: one for the page class (e.g. `NOISSUE Add User and Role Management class file` in the team's history), one for the spec (`NOISSUE Add User and Role Management test file`). Fixture plumbing that exists only to serve the flow's class travels in the class commit.
- Title only, sentence case, no trailing period, < 72 chars.
- Stage by explicit path. Never `git add -A` / `git add .`.
- **No `Co-Authored-By:`, `Signed-off-by:`, `Generated-by:`, or any AI/tool attribution trailer.** The git author is the only attribution the team wants. This supersedes any global instruction.

## Page-object classes

- File and class name: PascalCase, identical to each other and to the test-map flow name. `ObjectFieldsPage.ts` exports `class ObjectFieldsPage`.
- Attributes use the `readonly` modifier and are initialized in the constructor. No getters/setters, no `public` keyword on methods.
- The scaffolder emits the stub as `constructor(readonly page: Page) {}` — a parameter property. As
  you add locators, move to the explicit form below (declared `readonly` fields, including `page`,
  assigned in the constructor body). Both are valid TypeScript; the explicit form is the team's.
- **No assertions in classes.** Assertions shift testing responsibility into business logic and wreck maintainability — they live exclusively in spec files. A page-class method navigates, clicks, fills, and *returns locators or values*; it never `expect()`s.

```typescript
import {Locator, Page} from '@playwright/test';

export class ObjectFieldsPage {
	readonly addObjectFieldButton: Locator;
	readonly deleteObjectFieldOption: Locator;
	readonly page: Page;

	constructor(page: Page) {
		this.addObjectFieldButton = page.getByLabel('Add Object Field');
		this.deleteObjectFieldOption = page.getByRole('menuitem', {
			name: 'Delete',
		});
		this.page = page;
	}

	async goto(objectDefinitionLabel: string) {
		// navigation steps only — no expects
	}

	getVendorCell(name: string): Locator {
		// parameterized locators are methods, not constructor attributes
		return this.page.getByRole('cell', {name});
	}
}
```

## Fixtures

- File and fixture name: **what the scaffolder emits, derived from the suite path** — a suite
  `Bookshop / Listing` produces `fixtures/bookshopListingTest.ts` exporting `bookshopListingTest`,
  with an interface `BookshopListingFixture`. Older branches name the fixture after the class
  instead (`EditObjectDefinitionPage` → `editObjectDefinitionPagesTest.ts`); both shapes exist in
  the wild. **Do not rename a generated fixture** — the spec imports it by that exact name and the
  rename buys nothing.
- One `test.extend<{...}>()` with an interface listing the page objects the flow needs:

```typescript
import {test} from '@playwright/test';
import {EditObjectDefinitionPage} from '../pages/object-web/EditObjectDefinitionPage';

const editObjectDefinitionPagesTest = test.extend<{
	editObjectDefinitionPage: EditObjectDefinitionPage;
}>({
	editObjectDefinitionPage: async ({page}, use) => {
		await use(new EditObjectDefinitionPage(page));
	},
});

export {editObjectDefinitionPagesTest};
```

- When a spec needs more than one fixture (typically the flow fixture + the login fixture), compose with `mergeTests`:

```typescript
import {expect, mergeTests} from '@playwright/test';
import {workflowManagementTest} from '../../fixtures/workflowManagementTest';
import {loginTest} from '../../fixtures/loginTest';

const test = mergeTests(workflowManagementTest, loginTest);
```

## Spec files

- File name: lowercase first letter, matching the flow (`workflowManagement.spec.ts`).
- **Keep the BDD comment** that `generate-test-files` placed in the spec — it is the traceability link back to the test map. Implement exactly what it says.
- Wrap tests in `test.describe('<Describe column>')` so results are grouped by scope.
- **`test.step()` is opt-in, not the default.** Write the test as a flat sequence first. Add a step only when the body has **two or more distinct multi-action phases** that a reader would otherwise struggle to separate (e.g. a long *Arrange* followed by a long *Act*). Never one step per BDD line, never around a single navigation or a single assertion. Most tests are short enough to need none — a step per line inflates the test, slows the trace, and disguises weak assertions behind an appearance of thoroughness.
- Assertions mirror the `Then` clauses — assert outcomes, not implementation details.

The common case — no steps at all:

```typescript
test.describe('Site Navigation', () => {
	test('Navigate to US Site', async ({page, workflowManagementPage}) => {
		/**
		  Given the user is logged in
		  When the user navigates to the US site
		  Then the US site URL is displayed
		*/
		await workflowManagementPage.navigateToUSSite();

		await expect(page).toHaveURL(/us-site/);
	});
});
```

A step earns its place only when there are genuinely separate phases to label:

```typescript
test.describe('Notification Workflow Management', () => {
	test('Assign a reviewer to a pending notification', async ({
		loginPage,
		page,
		workflowManagementPage,
	}) => {
		/**
		  Given the user is logged in as an administrator
		  When the user assigns a reviewer to a pending notification
		  Then the notification is listed under the reviewer
		*/
		await test.step('Log in and open the workflow list', async () => {
			await loginPage.login(user);
			await page.waitForURL('**/home/*');
			await workflowManagementPage.navigateToUSSite();
			await workflowManagementPage.openPendingNotifications();
		});

		await test.step('Assign the reviewer', async () => {
			await workflowManagementPage.selectNotification('Quarterly report');
			await workflowManagementPage.assignReviewer('Ana Lima');
			await workflowManagementPage.confirmAssignment();
		});

		// A single outcome assertion needs no test.step of its own.
		await expect(workflowManagementPage.reviewerCell).toHaveText('Ana Lima');
	});
});
```

## Test lifecycle and teardown

A test that leaves data behind poisons the next run, and in this suite it also poisons the Phase 2
baseline — a leftover record makes a passing test look like a regression, or a regression look fine.

- **Whatever a test creates, it removes.** Clean up in `test.afterEach`, calling a cleanup method on
  the page class (or a direct API call when one exists — faster and less brittle than driving the UI
  twice).
- **Cleanup is idempotent and never fails the test.** The artifact may already be gone because the
  test failed before creating it. A teardown that throws turns one red test into two mysteries.
- **No assertions in teardown.** The "no assertions in classes" rule extends to the hook: teardown
  removes state, it does not verify it. Something worth asserting belongs in the test body.
- **Suite-scoped cleanup is a project `teardown`, not a hook.** Playwright lets a project name
  another project as its `teardown` — that is where `tmp/.auth/*.json` and shared fixture data go,
  once at the end, rather than in every spec:

```typescript
export const config: Project = {
	name: 'setup',
	teardown: 'cleanup',
	testMatch: 'setup.ts',
	testDir: 'tests/@setup',
};
```

- **Read-only flows get no teardown.** A test that only navigates and asserts has nothing to undo;
  an empty `afterEach` added for symmetry is noise the reviewer has to read past.
- **Never use teardown to fix portal state so a test passes.** Repairing data the flow depends on
  hides the real problem and contaminates the baseline the whole upgrade comparison rests on. A
  missing prerequisite is a `blocked` case, not something to paper over.

## Test design rules

`fullyParallel: true` is the project default, so tests in the same file run concurrently. Everything
below follows from that.

- **Every test stands alone.** No ordering assumptions, no reliance on data a previous test created.
  Any test must pass when it is the only one that runs.
- **Test data is unique per run.** Suffix names with a timestamp or short random token, so two
  workers — or two runs on the same portal — never collide on the same record. A hardcoded
  `"Test Article"` is a flake waiting for a parallel run.
- **Arrange in a fixture, never in a preceding test.** Shared setup belongs in `test.extend()` or a
  setup project; a test that exists only to prepare state for the next one is not a test.
- **`test.describe.configure({mode: 'serial'})` is a last resort.** It is correct only when the
  portal genuinely cannot support concurrent execution of that flow. When you use it, leave one
  comment line saying why — otherwise the next reader deletes it and gets intermittent failures.

## Comments

The scaffolder writes one comment per test and that is the one comment the test should have.

- **The BDD block inside `test()` is mandatory.** `generate-test-files.sh` emits the row's `Steps`
  as a `/** … */` block, with the notes column above it as `# NOTES` lines. Keep it verbatim: it is
  the traceability link back to the test map and the source the bug reports quote. It is
  specification, not commentary. When the skill's ambiguity queue resolves a case, fold the clarification
  into this block so the next reader sees why the assertion is what it is.
- **Everything else goes uncommented.** Locator and method names already say what the code does; a
  comment restating it is noise a reviewer must read and a future edit must maintain.
- **The one exception is a non-obvious *why*** — a Liferay trap the next developer would "simplify"
  and break: a Clay component rendering at `<body>` level, a SennaJS navigation that needs a URL
  wait, an iframe dialog, a portlet AJAX response the test must await. One line, giving the reason,
  never the action:

```typescript
// Clay renders the dropdown in a portal at <body>, so it is not a descendant of the trigger.
readonly categoryOption: Locator;
```

- **Never:** commented-out code, section-divider banners, or JSDoc on page-class methods.

## Synchronization and web-first assertions

Playwright's locators and `expect` already auto-wait. Rely on that instead of manual waits:

- **Never** `page.waitForTimeout(...)` and **never** `waitUntil: 'networkidle'` — both are flaky and non-negotiable to avoid. Wait for a concrete signal instead: a web-first assertion, a URL, or a specific network response.
- Prefer web-first assertions (`toBeVisible`, `toHaveText`, `toHaveURL`, `toHaveCount`) — they poll until the condition holds or the timeout elapses.
- **Never** assert on a snapshotted count: `expect(await locator.count()).toBe(n)` reads a single instant and does not retry. Use `await expect(locator).toHaveCount(n)`, or `await expect.poll(() => locator.count()).toBeGreaterThan(0)` for derived conditions.
- For portlet AJAX (e.g. a Liferay publisher re-rendering after a filter/sort), await the response rather than sleeping:

  ```typescript
  await Promise.all([
      page.waitForResponse(
          (r) => r.url().includes('PublisherPortlet') && r.url().includes('p_p_lifecycle=2')
      ),
      filterOption.click(),
  ]);
  ```

## Selector naming

Semantic, aligned with the page/component context:

- Start with the context of the page or component.
- End with the name of the HTML element.
- Examples: `profileSettingsButton`, `countrySelector`, `newsPageTitle`, `addObjectFieldButton`.

## Locator strategy

Priority order (the team's strongest branches are role-based throughout — follow them, not the CSS-heavy ones):

1. `getByRole(role, {name})` — resilient and accessibility-aligned.
2. `getByLabel` / `getByPlaceholder` — form fields.
3. `getByText` — static text with no role.
4. `getByTestId` — only when the markup offers a stable `data-testid`.
5. CSS — last resort, and only for stable, semantic attributes (e.g. `button[data-action="add"]`). Never auto-generated classes or IDs.

See `locator-discovery.md` for how to derive these from the live portal and for Liferay-specific traps (generated portlet IDs are forbidden as selectors).

## Authentication pattern

- `tests/@setup/` holds setup projects (e.g. `setup:admin`) that log in and persist `storageState` to `tmp/.auth/login<Role>.json`, guarded by `utils/isAuthenticated.ts` (skips login when the file already exists).
- A feature that needs an authenticated session declares it in its `config.ts`:

```typescript
export const config: Project = {
	dependencies: ['setup:admin'],
	name: 'featureName',
	testDir: 'tests/feature',
	use: {
		storageState: 'tmp/.auth/loginAdmin.json',
	},
};
```

- Anonymous flows omit `dependencies` and `storageState` entirely.
- `tmp/.auth/*.json` is environment-specific — it must be deleted when the suite switches portals (the skill does this at the start of Phase 2).

## .env policy

`.env` stores the system URLs and the user credentials, and is **never committed**. Historically it
was distributed per project through the team drive; `/upgrade-frontend-flow` now writes it in Phase 0
from the values the executor supplies, which is the same policy seen from the other side — the file
is generated locally and stays local.

Never inline a URL or credential in code: read it through the `{project}.config.ts` indirection.

`BASE_URL` is the load-bearing name. The generated `playwright.config.ts` wires `use.baseURL` only
from a variable that normalizes to `baseUrl`, so `ORIGINAL_URL` alone leaves the suite with no base
URL. Keep `BASE_URL` pointed at the portal the current phase targets and override it per command
(`BASE_URL=$UPGRADE_URL npx playwright test …`) rather than editing the file mid-run.

## Playwright config defaults

`playwright.config.ts` imports the per-feature `config.ts` files as projects. The scaffolder
generates it with `trace: 'on-first-retry'` and **none** of the defaults below, so patching it is
part of Phase 0 — not an optional improvement. Set these project-wide:

- `fullyParallel: true` — parallelize tests *within* a file, not just across files. Specs that hold more than one `test()` otherwise run serially, slowing the full Phase 2 run.
- `retries: process.env.CI ? 2 : 0` — absorb genuine Liferay flakiness (loading masks, publisher AJAX, SennaJS) on CI so a flake is not mis-triaged as a bug.
- `forbidOnly: !!process.env.CI` — fail the build if a `test.only` was committed by mistake; otherwise CI greens a run that silently skipped every other test.
- **Trace vs. retries interaction:** `trace: 'on-first-retry'` only fires when a retry happens — with `retries: 0` it **never captures a trace** on a local run. Either keep `on-first-retry` alongside `retries > 0`, or use `trace: 'retain-on-failure'` so local runs (without the Phase 2 `--trace` CLI override) still produce traces.

## Formatting

Prettier formats, ESLint enforces quality. Run both (when configured in the repo) on every changed file before committing — a commit that fails the repo's lint is rework for the reviewer.

## Canonical examples in the team's branches

When in doubt about shape, read these (branch:path):

| Pattern | Where |
|---|---|
| Rich page object with fluent methods | `autosar-2026.q1:pages/autosarPartnerManagementTool/editVendorInformation/EditVendorInformationPage.ts` |
| Dual-role auth setup | `autosar-2026.q1:tests/@setup/admin/adminSetup.ts` + `utils/isAuthenticated.ts` |
| Spec with BDD comment | `kaufmann-7.4:tests/cotizarPage/cotizar/cotizar.spec.ts` |
| Fixture definition | `lee-health-2025.q1:fixtures/academicAndMedicalEducationNewsTest.ts` |
| Form-fill with data builder | `petros.2025.q4:pages/emailCases/publicManifestationSubmission/PublicManifestationSubmissionPage.ts` |

Known anti-patterns in older branches — do **not** reproduce them: CSS-id-heavy locators (`page.locator('#_com_liferay_...')`), empty stub page classes with all logic in the spec, assertions inside page classes.
