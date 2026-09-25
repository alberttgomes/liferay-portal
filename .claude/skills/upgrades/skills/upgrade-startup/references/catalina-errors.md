# Catalina-phase error catalog (Step C4)

Symptom-keyed fixes for `SEVERE` / `ERROR` entries during Liferay Portal initialization. Read the
entry matching the trace before fixing. Stop the portal, apply the fix, restart (Step C3), and verify
the error is gone before moving to the next one.

`C4.e` (the DXP license gate) is **not** here — it stays in `SKILL.md` because it is a phase gate, not
a symptom, and Phase 4 depends on it.

## C4.a — Outdated web.xml

**Symptom:**

```
SEVERE [main] org.apache.catalina.core.StandardContext.startInternal Error during ServletContainerInitializer processing
javax.servlet.ServletException: java.lang.NullPointerException: Cannot invoke "javax.servlet.FilterRegistration$Dynamic.setAsyncSupported(boolean)" because "dynamic" is null
```

**Fix:** the customer's `web.xml` contains parameters that no longer match the new Liferay version.
Compare with the reference `web.xml` from the target version and apply the necessary changes. If there
are no meaningful customizations, delete the file — Liferay generates a clean one on next startup.

## C4.b — Elasticsearch: connected to the container, not the sidecar?

Liferay auto-starts an **embedded sidecar** Elasticsearch when it cannot reach the configured
container — undesirable for an upgrade, since the Phase 4 reindex must hit the real container. During
Catalina monitoring, verify which one the portal bound to.

**Check the log** for sidecar-fallback / connection messages, e.g.:

```
... using the embedded (sidecar) Elasticsearch
... UnknownHostException / Connection refused to <configured-es-host>
```

**If it fell back to the sidecar,** fix the connection configuration so the portal binds to the
configured container. Two things to know for the target connector (2026.q1):

- **The connector PID is `elasticsearch8`.** The `elasticsearch7` connector **and** the legacy
  *unversioned* connector were **removed** — the only config file is
  `osgi/configs/com.liferay.portal.search.elasticsearch8.configuration.ElasticsearchConfiguration.config`.
  An overlay still carrying the old `elasticsearch7`/unversioned PID is **orphaned (ignored)**, so the
  portal silently defaults to the embedded sidecar. Rename/migrate the `.config` to the es8 PID.
- **`operationMode` is gone.** The es8 model dropped it; the embedded sidecar is now gated by
  **`productionModeEnabled`** — set `productionModeEnabled=B"true"` in the `.config` so the sidecar
  stays off, and align `clusterName` to the compose container's `cluster.name`.

Set host / port / network settings in that es8 `.config` and the compose search service, restart, and
re-verify. This is **config-level only — no database writes** (it replaces the old
`Configuration_`-table row deletion, which violated the no-database rule).

> **Boundary:** Phase 3 ensures the portal connects to the *right* Elasticsearch. The **reindex
> itself, and any ES index cleanup, is Phase 4** (`upgrade-reindex`) — not here.

## C4.c — Environment-specific host unreachable (LDAP, external APIs, etc.)

**Symptom:**

```
WARN SafePortalLDAPImpl: Unable to bind to the LDAP server
javax.naming.CommunicationException: <internal-hostname>:389
```

Or:

```
ERROR ... Unable to access API or service ...
com.customer.SomeException: Failed to connect to <internal-service>
```

These are caused by services only available in the customer's production environment. **Do not
attempt to fix the underlying service.** Log them as environment-specific warnings in
`upgrade-state.md` and continue — they will not block the upgrade validation.

## C4.d — Other Catalina errors

For any other `SEVERE` or `ERROR` not covered above:

1. Read the full stack trace.
1. Check the `upgrade-state.md` decision log for a documented solution.
1. Attempt a fix based on the error message.
1. If the error is clearly environment-specific (external hostname, customer-specific API), log it as
   a known environment warning and continue.
1. If the error is blocking and cannot be resolved, flag it for human review and pause the phase.
