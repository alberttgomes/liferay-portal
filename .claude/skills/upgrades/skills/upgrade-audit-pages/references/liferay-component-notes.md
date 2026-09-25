# Liferay-specific guardrails for the audit

Liferay portals have version-to-version quirks that distort naive visual diffs. Read this before classifying audit findings in any of the components below.

## Selector hygiene

**Never** use Liferay-generated portlet instance IDs as selectors. They look like:

```
_com_liferay_journal_web_portlet_JournalPortlet_INSTANCE_aB3xYz9q_
_com_liferay_login_web_portlet_LoginPortlet_INSTANCE_DEFAULT_
```

The instance suffix changes between deployments and between versions. Selectors that rely on these IDs will produce false-positive structural-change bugs on every run.

**Use instead:**
- `getByRole('button', { name: 'Save' })`
- `getByLabel('Email address')`
- `getByText('Forgot password')`
- Semantic CSS selectors: `[data-qa-id="..."]`, `[aria-label="..."]`

## Components with frequent version-to-version visual changes

Validate findings in these components against `known-design-system-changes.md` before classifying them as bugs.

### Clay / Lexicon components

Liferay's design system. Receives visual updates regularly (spacing, typography, transitions, focus rings, hover states). Common patches between minor versions:

- Button hover/active/focus state colors and transitions.
- Dropdown z-index and arrow positioning.
- Modal padding and border-radius.
- Alert icon size and color.
- Form control focus-ring thickness.

A consistent Clay change across many pages is almost always intentional.

### Navigation menus

Often re-themed between major versions. Watch for:

- Spacing between menu items.
- Mobile breakpoint and hamburger behavior.
- Sub-menu hover delay.
- Active-page indicator (underline → bar → background).

### Page Editor and Fragment Framework

Fragment-based pages can render with structural differences after upgrade because:

- Fragment rendering pipeline evolves between versions.
- Some fragments rely on portal-CSS classes that may have been renamed.
- Layout collections introduced in newer versions render differently from manually composed Page Editor pages.

If a page is fragment-based, expect higher diff noise. Confirm structural changes against the actual fragment definitions in the workspace before reporting them as bugs.

### Login page (sign-in portlet)

Liferay updates the login portlet visuals fairly often: form layout, button placement, "remember me" checkbox styling, password toggle, OAuth provider buttons. Compare against the customer's overrides in `themes/` or `client-extensions/` before flagging — the change might be a target-version override that the customer hasn't yet ported forward.

### Asset routes

Document and image URLs may change shape between versions:

- `/documents/<group-id>/<folder>/<filename>` (older)
- `/o/<group-id>/<file-entry-id>` (newer headless API)
- `/o/headless-delivery/v1.0/...` (Headless Delivery API)

A "broken image" bug may simply be a route shape change. Verify the asset still resolves on the target before classifying as Critical.

### Control Panel and Site Administration

Heavy refactors land here every major release. Expect:

- Breadcrumb structure changes.
- Sidebar nav reorderings.
- New configuration screens that did not exist in the source version.

For admin pages, lower the "missing element" severity by one level (a missing element in admin is more likely a UI relocation than a removal) and inspect the target carefully before reporting.

## Dynamic content masking

Liferay pages contain regions that legitimately differ between any two requests. Mask these before screenshot or pixel-diff:

- `time` elements and `[data-timestamp]` (relative timestamps).
- `p_auth=` query parameters in URLs.
- User-specific avatars and salutations (`Hello, <user>`).
- Real-time counters and badges (`[class*="counter"]`, `[class*="badge"]`).
- Carousels and rotating banners — pause animation via injected CSS:

  ```css
  *, *::before, *::after { animation-play-state: paused !important; transition: none !important; }
  ```

Add row-specific masks via the `notes` column in `.claude/audits/pages.csv`.

## Performance and load-time differences

Out of scope for this audit. The audit captures what is visible/interactive, not page-load timing. If the team needs performance regressions, run a separate Lighthouse or WebPageTest pass.

## Authentication corner cases

- Liferay's session cookie name has changed across versions (`JSESSIONID` → `LFR_SESSION_STATE_*`). The audit skill saves storage state per portal independently, so this is handled — but if login-state reuse fails on one portal, log in fresh for that portal only.
- SSO/SAML flows: out of scope unless `audit.user` and `audit.password` are local accounts. If the portal redirects to an external IdP, instruct the user to set up a local audit account on both portals.