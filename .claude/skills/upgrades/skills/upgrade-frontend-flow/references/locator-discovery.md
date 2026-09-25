# Locator discovery on a live Liferay portal

How to derive senior-quality Playwright locators from a running portal using Playwright MCP.

**Read this when a locator cannot be written or repaired from the test case and the failure output alone.** It is a targeted lookup, not a discovery pass: navigate to the one page you need, snapshot, derive, go back to coding. Do not run it as a pre-pass over a flow — see "When to open the browser" in `SKILL.md`.

## Lookup workflow

1. **Navigate** to the page the BDD step targets: `browser_navigate` with the full URL (`<base-url>` + the path from the test map's Steps/Notes, or reached by clicking through, mirroring the user journey).

1. **Snapshot** with `browser_snapshot`. The snapshot is the page's accessibility tree — the same tree Playwright's `getByRole` queries. Every node shows `role "accessible name"`.

1. **Derive the locator** from the tree node, in this priority order:

   | Tree node | Locator |
   |---|---|
   | `button "Save"` | `page.getByRole('button', {name: 'Save'})` |
   | `link "View Profile"` | `page.getByRole('link', {name: 'View Profile'})` |
   | `textbox "Email Address"` | `page.getByLabel('Email Address')` |
   | `combobox "Country"` | `page.getByLabel('Country')` |
   | `menuitem "Delete"` | `page.getByRole('menuitem', {name: 'Delete'})` |
   | `heading "News" [level=1]` | `page.getByRole('heading', {name: 'News', level: 1})` |
   | `cell "ACME Corp"` | `page.getByRole('cell', {name: 'ACME Corp'})` |
   | Text with no role | `page.getByText('…')` |
   | Nothing usable in the tree | Inspect the DOM for a `data-testid`, then for a stable semantic attribute; CSS is the last resort |

1. **Verify uniqueness mentally against the snapshot** — if the same role+name appears more than once in the tree, the locator will hit Playwright's strict mode. Disambiguate by scoping (`page.getByRole('navigation').getByRole('link', {name: 'Home'})`), by `exact: true`, or by `level` for headings. Prefer scoping by a parent landmark (region, navigation, dialog, table) over `.nth()` — positional locators break silently.

1. **Interact to reach hidden states.** Menus, modals, and dropdown options don't exist in the tree until opened — use `browser_click` to open them, snapshot again, and derive locators for the revealed elements.

1. **Authenticated pages.** Navigate to `<base-url>/c/portal/login`, fill the form via the snapshot-derived labels, and log in once with the `.env` credentials. The MCP browser session keeps the cookies for the rest of the discovery work. This MCP login is for *discovery only* — the tests themselves authenticate through the `@setup` storageState pattern.

## Liferay-specific pitfalls

- **Never use generated portlet IDs.** Anything shaped like `_com_liferay_..._INSTANCE_xxxx_` (element IDs, `name` attributes, CSS classes ending in instance hashes) changes between versions and deployments. They are the #1 cause of upgrade-broken tests. If a form field's `id` is generated but its `<label>` is stable, `getByLabel` works regardless.
- **Clay dropdowns and modals render in a portal at `<body>` level**, outside the triggering component's DOM subtree. Scope locators for menu items to `getByRole('menu')` / `getByRole('dialog')`, not to the button's container.
- **SennaJS (SPA navigation).** Liferay navigates many links without a full page load. After a click that "changes page", prefer `await expect(page).toHaveURL(...)` or waiting for a concrete element of the destination over `waitForNavigation`-style patterns.
- **Loading masks.** Clay's `loading-animation` overlays intercept pointer events while data loads. If a click flakes with "intercepts pointer events", wait for the target element's own state (e.g. an `expect(locator).toBeEnabled()` in the spec) rather than adding timeouts.
- **iframe dialogs.** Some legacy portlets open dialogs inside iframes. The accessibility snapshot flags them; scope with `page.frameLocator(...)` by the iframe's **title** or another stable attribute — never `frameLocator('iframe').first()`, which silently binds to whatever iframe happens to be first.
- **Localized accessible names.** Role names follow the portal's display language. Discover with the same language/user the tests will run with, and keep the portal language consistent between original and upgraded environments — otherwise every name-based locator "breaks" for the wrong reason.
- **Control/product menu duplication.** Admin pages often render the same action label in both the management toolbar and a kebab menu. Scope to the toolbar (`getByRole('menubar')`, `getByRole('toolbar')`) or the surrounding section to stay strict-mode safe.
- **Choices.js selects have no accessible name.** Filter/sort dropdowns built with Choices.js render a `combobox` role **with no accessible name**, so `getByRole('combobox', {name})` and `.choices`-scoped lookups fail (the `.choices` wrapper also holds the hidden native `<select>`). This is the one sanctioned exception to "no positional locators": use page-level `getByRole('combobox').nth(i)` for the value selects and `getByRole('listbox')` for the sort, then open with `.click()` → `getByRole('option', {name, exact: true}).click()` and await the portlet AJAX (never `waitForTimeout` — see the sync rules in `team-conventions.md`).
- **Search behind a toggle.** Some pages hide the search field until its icon is clicked — the input (`#search-input` and similar) does not exist in the tree until then. Click the toggle before snapshotting for the field.
- **Heavy publisher pages never fire `load`.** Content-heavy publisher pages (large asset lists) can hang the `load` event. A page-class `goto` must pass `{waitUntil: 'domcontentloaded'}` or navigation times out.
- **Control Panel portlets 404 on a direct `goto`.** Admin portlets (System Settings, process/workflow builder, object definitions) return 404 when navigated by their generated URL. Reach them through the product menu instead: `getByRole('button', {name: 'Abrir menu'})` (localized) → the app link → `waitForURL(/<Portlet>/)`. Some portlets are reachable at a stable direct URL (e.g. `ControlPanelWorkflowPortlet`) — prefer that when it exists.

## Phase 2 repairs

When fixing a broken locator on the upgraded portal:

1. Reproduce the journey on the **upgraded** portal with MCP up to the failing step.
2. Snapshot and find the element that serves the **same user-facing purpose** (the role/name may have changed — e.g. a `button "Add"` becoming `button "New Entry"`, or a link becoming a menuitem).
3. Derive the new locator by the same priority rules.
4. Derive from the upgraded portal only. It is the suite's sole target from now on — the original portal is dropped from the repo at the end of Phase 3, so a locator that also works there buys nothing.
5. Change only the page class. The spec, fixture, and assertions stay untouched — if they need to change, the failure was not a locator breakage; re-triage as a bug.
