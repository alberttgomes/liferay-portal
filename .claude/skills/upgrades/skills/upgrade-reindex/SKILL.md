---

name: upgrade-reindex
description: Run this skill when Phase 4 (Check Reindex) of the upgrade playbook executes, or when the user invokes `/upgrade-reindex` directly. Triggers a general search-engine reindex on the upgraded portal and evaluates the reindex log to confirm the index rebuilds cleanly. There is NO reindex API (no REST/JSONWS/Gogo) — the reindex is triggered through the Search Admin UI driven by Playwright MCP (primary) or by a human (fallback), then it runs as async BackgroundTasks whose log is tailed and evaluated. Trigger also when the user says "run the reindex", "check the reindex", or "rebuild the search index".

---

# upgrade-reindex (Phase 4 — Check Reindex)

Triggers a **general reindex** of the search engine and **evaluates the log** to confirm the search
index rebuilds cleanly after the upgrade.

## Scope boundary

- Search-engine (Elasticsearch / OpenSearch) reindex only — **NOT** a database operation.
- Connecting to the *right* Elasticsearch (configured container vs. embedded sidecar) is already
  handled in **Phase 3 (Fix Startup)**. This phase assumes the portal is up and bound to the
  configured ES container.

## Prerequisites

- Phase 3 (Fix Startup) is `complete`, **including the S6 admin-access gate** (`upgrade-startup`):
  portal running, all bundles `Active`, bound to the configured ES container, **DXP license
  registered, and an omniadmin can authenticate and reach Control Panel → Search Administration.**
  Phase 4 **cannot run without S6** — the reindex has no headless API and must be triggered through
  that admin UI.

## Gotchas

A reindex on an **empty / seed dataset** completes in sub-second and proves only the **trigger path** —
not real indexing. The DB-rooted-error autonomy boundary below is exercised only when there is
**actual content** to index. On a populated upgrade, expect per-indexer progress in the log and budget
time accordingly — that is where data-integrity errors surface.

## Mechanism — there is no reindex API; trigger via the Search Admin UI

Verified against the master/2026.q1 source: **no REST/JAX-RS, JSONWS, or Gogo reindex command
exists.** The "Reindex all" action is the Search Admin portlet action `/portal_search_admin/edit`
(`cmd=reindex`, blank `className` ⇒ general / all indexers), which calls `IndexWriterHelper.reindex(…)`.
With a blank `className` it fires **two** background tasks — `ReindexPortalBackgroundTaskExecutor`
(the general reindex) **and** `ReindexIndexReindexerBackgroundTaskExecutor` (the `IndexReindexer`s —
synonyms, rankings) — so the UI action is a more faithful "reindex all" than a hand-written Groovy. It
runs as **asynchronous BackgroundTasks** → tail the log for progress, completion, and errors.

Driving this action **headless is unreliable** on 2026.q1 (captcha- and JS-gated forms, portlet
action-routing quirks, authToken rotation). So the channel is a real browser or a human, in this
order:

| Order | Channel | Notes |
|---|---|---|
| **Primary** | **Playwright MCP** drives the real UI | Control Panel → Search Administration → **Reindex all**. The reindex action is **not** captcha-gated, and a full browser runs the page JS that a hand-built POST cannot. |
| **Fallback** | **Human** clicks "Reindex all" | when Playwright MCP is not available in the session, or an unexpected gate (MFA, captcha) appears. The agent tails and evaluates the log. |
| **Demoted** | Groovy in the **Script Console** | documented alternative only — the Script Console is the worst headless surface (captcha + JS-gated submit). Script below. |

### Groovy alternative (Script Console only)

```groovy
import com.liferay.portal.kernel.search.IndexWriterHelperUtil
import com.liferay.portal.kernel.search.background.task.ReindexBackgroundTaskConstants
import com.liferay.portal.kernel.util.PortalUtil
long[] companyIds = PortalUtil.getCompanyIds()
def ctx = new HashMap<String, java.io.Serializable>()
ctx.put(ReindexBackgroundTaskConstants.EXECUTION_MODE, "full")
IndexWriterHelperUtil.reindex(0L, "reindex", companyIds, "", ctx)   // blank className = general
```

This calls only `IndexWriterHelper.reindex` — it does **not** also run the `IndexReindexer` tasks that
the UI "Reindex all" triggers, so it is a *narrower* reindex than the UI path.

## Workflow

1. **Confirm Phase 3 S6 gate** — portal up, ES-container-bound, omniadmin reachable. If not, stop and
   return to Phase 3.
2. **Ensure the reindex UI is reachable** (carried from S6; re-verify here):
   - License registered (no redirect to `/c/portal/license_activation`).
   - Login renders + omniadmin authenticates. If a **deployed-throwing theme** blocks login →
     neutralize at the **deploy/OSGi level** so the site falls back to Classic (a *missing* theme
     already auto-falls-back — no action); record + flag for Phase 5; restore after the run.
3. **ES index cleanup (inherited from the removed startup C4.b)** — if any non-blocking Elasticsearch
   index cleanup is needed, do it here (ES is this phase's domain). Config / index level only — **no
   database writes**.
4. **Trigger the general reindex** via Playwright MCP (Control Panel → Search Administration →
   "Reindex all"); human fallback otherwise. **Guardrails** (it logs in as omniadmin):
   - **Confirm with the user before logging in / triggering** (outward-ish, privileged action).
   - Act **only** on the reindex control — navigate straight to Search Administration, assert the
     page, click only "Reindex all".
   - Capture **before/after screenshots** into the artifact dir as evidence.
5. **Tail and evaluate the reindex log** — both background tasks
   (`ReindexPortalBackgroundTaskExecutor` + `ReindexIndexReindexerBackgroundTaskExecutor`) reach
   `Finished`, with no errors.
6. **Restore** any temporary theme / deploy changes made in step 2.

### Checklist

Copy into `upgrade-notes/<run-id>/phase-4-reindex/summary.md`.

- [ ] Phase 3 S6 gate confirmed (license, login, omniadmin, Search Admin reachable)
- [ ] Reindex triggered via the Search Admin UI, with before/after screenshots captured
- [ ] **Both** background-task executors reached `Finished`, with no errors
- [ ] Any DB-rooted error recorded as suggested investigative SQL — **none executed**
- [ ] Step 6 — every temporary theme / deploy change from step 2 **restored**
- [ ] Report states whether the run was against real data or an empty/seed dataset

## Autonomy boundaries

- **Autonomous:** tailing/evaluating the log; **non-database** fixes (ES config/connection, OSGi,
  indexer wiring) + restart + re-verify; deploy/OSGi-level theme neutralization in step 2
  (config/deploy, no DB); ES index cleanup in step 3 (config/index level).
- **Database-rooted errors** (data integrity, orphaned references surfaced during reindex) → **never**
  auto-fix; **suggest investigative SQL queries** for the user / DBA instead.
- **Confirm first:** logging in as omniadmin and triggering the reindex (step 4); completing any
  pending admin password reset (a credential change — normally already handled by Phase 3 S6).
- **Never:** execute SQL or any database write; reset the admin password silently; declare Phase 4
  complete on an empty-dataset run without saying so in the report.

## Output

- Reindex outcome (clean / errors), the evaluated log, and Playwright before/after screenshots under
  `.claude/upgrade-artifacts/<run-id>/phase-4-reindex/`.
- Any DB-rooted errors listed as **suggested investigative SQL** (not executed).
- Whether validation was against **real data** or **empty/seed data**.
- Next: `/upgrade-phase 5` (Fix Frontend — **deferred**).
