# Runtime findings (CHECK 3 boot + login)

Recurring issues that appear only once the portal boots and a user logs in — many are **not**
visible in a boot-only check, which is why Step 8 logs in to each delivered access point. Each entry:
how to detect, the SQL to hand the user (the agent never queries the DB), and whether to fix or flag.

## 1. Service Builder build-number skew (source vs dump)

**Detect (boot log):**
```
com.liferay.portal.kernel.exception.OldServiceComponentException:
  Build namespace <NAMESPACE> has build number N which is newer than M
```
**Read the direction carefully:** `N` is the number recorded **in the database** (from the dump),
`M` is the build number of the **module being deployed** (the source). `N > M` therefore means the
**delivered source is OLDER than the delivered dump** for that Service Builder module — the DB was
produced by a newer build of the module than the source we received. Liferay only migrates a schema
**forward**, so it refuses to run older code against a newer-schema DB.

- **Source side:** the module's `service.properties` (`build.namespace`, `build.number`, `build.date`)
  — packaged into the built jar; Service Builder bumps `build.number` whenever entities/columns change.
- **DB side (user runs):**
  ```sql
  SELECT buildnamespace, buildnumber, to_timestamp(builddate/1000) AS built_at
  FROM servicecomponent WHERE buildnamespace = '<NAMESPACE>' ORDER BY buildnumber DESC;
  ```

**Flag** (don't "fix"): the source and dump are from different points in time. Ask the client for the
source at the DB's build, or a dump taken at the source's build. Non-fatal for boot but the component
is out of sync. (If the module number is *newer* than the DB's, it's the normal upgrade-forward path,
not this finding.)

## 2. Post-login missing site permission ("circumvent the permission checker")

**Detect** — login authenticates, but the post-login landing renders an error page; boot log shows:
```
[MainServlet] ActionException: java.lang.IllegalArgumentException:
  Someone may be trying to circumvent the permission checker:
  {companyId=…, name=com.liferay.portal.kernel.model.Group, primKey=<groupId>, scope=4}
  at com.liferay.portal.events.ServicePreAction.run(...)
Caused by: com.liferay.portal.kernel.exception.NoSuchResourcePermissionException: {… primKey=<groupId> …}
```
**Cause.** Authentication succeeds; core `ServicePreAction` (building the `ThemeDisplay` for the
user's default site) checks a permission on Group `<groupId>` and finds **zero** `ResourcePermission`
rows, so the PermissionChecker aborts. This is a **dump data gap** (the export/anonymization dropped
the site's resource permissions), **not** a source defect — the custom code in the filter chain is
not the origin (the throw is in core `ServicePreAction`).

**User runs:**
```sql
SELECT count(*) FROM resourcepermission
WHERE companyid=<companyId> AND name='com.liferay.portal.kernel.model.Group'
  AND scope=4 AND primkey='<groupId>';   -- 0 confirms the gap
```
**Flag** as a **BLOCKER** for that instance's login. Resolution (client/DBA): re-export the dump with
`ResourcePermission` for all sites, or regenerate the site's default resource permissions; confirm
whether it also reproduces on the source system.

## 3. `@Component` on a static utility class (DS activation failure)

**Detect (boot log):**
```
ERROR [<UtilityClass>] bundle <bsn>:<ver> : Constructor with 0 arguments not found. Component will fail.
ERROR [<UtilityClass>] : Error during instantiation of the implementation object: Constructor not found.
```
**Cause.** A class with only `static` methods (typically a private constructor that throws
`IllegalStateException("Utility class")`) is annotated `@Component`. OSGi Declarative Services cannot
instantiate it, so the component fails to activate on every boot — it could never have worked as a
service.

**Safe fix (mechanical):** remove the `@Component` annotation + its import when **both** hold — the
class exposes only static methods **and** nothing `@Reference`s it as a service
(`grep -rn "@Reference" ... <UtilityClass>` → none; all call sites are `<UtilityClass>.method(...)`).
Rebuild that module; the bundle then starts clean. Genuine source defect → change-log + report row
(`[applied — verify]`).

## 4. Document library not mounted

`DLFileEntry`/image reads fail with `NoSuchFileException /opt/liferay/data/document_library/...` and
`ImageImpl` "Error reading image"; content is not indexed. Provide the store (Step 7) — copy it, or a
git-ignored `docker-compose.override.yml` pointing at an existing path (see `reuse-client-docker.md`).
Verify a document/image serves HTTP 200 once mounted.

## 5. Delivered search topology is embedded Elasticsearch

Older 7.x dev environments often run **embedded** Elasticsearch (no separate ES container; no
`ElasticsearchConfiguration.config`, or `operationMode` defaulting to embedded). Reproduce the search
topology **as delivered** — do not force a separate ES container just to satisfy a preference. Then
**flag embedded ES as an upgrade-target concern** (embedded ES is removed/discouraged in later
Liferay; the target needs an external Elasticsearch/OpenSearch). If the delivered configs are REMOTE
(transport `9300` for 6.x, REST `9200` for 7.x+), reproduce with the matching ES container instead.

## 6. A bundle running in the client's portal has no source in the delivery

**Compare sets, not counts.** On one delivery the prebuilt jar count and the source module count both
came to 92 and looked like a clean 1:1 match; the runtime actually contained **93** custom bundles. The
count agreed by coincidence.

**Detect** — resolve `Bundle-SymbolicName` from both sides and diff them. Unfold MANIFEST continuation
lines first, or long names get truncated at the 72-byte wrap and produce phantom mismatches:

```bash
unfold() { unzip -p "$1" META-INF/MANIFEST.MF | tr -d '\r' \
  | sed -e :a -e '$!N;s/\n //;ta' -e 'P;D' | grep -m1 '^Bundle-SymbolicName:' \
  | sed 's/Bundle-SymbolicName: *//;s/;.*//'; }

for j in <bundle>/osgi/modules/*.jar; do unfold "$j"; done | sort -u > /tmp/prebuilt
grep -rhoP 'Bundle-SymbolicName:\s*\K\S+' <workspaces> --include=bnd.bnd | sort -u > /tmp/source

comm -23 /tmp/prebuilt /tmp/source   # in the runtime, no source delivered  <- the finding
comm -13 /tmp/prebuilt /tmp/source   # in source, never deployed
```

**Flag as a BLOCKER; do not paper over it** by staging the client's prebuilt jar — that is precisely the
artifact this check exists to avoid trusting, and staging it hides the gap. CHECK 1 can then only claim
N-1 of N bundles.

**Look for collateral.** The missing bundle may be an OSGi **fragment**: on that delivery
`com.<customer>.asset.publisher.fragment` (`Fragment-Host: com.liferay.asset.publisher.web`) re-exported
two `internal` packages, and a second, source-complete bundle imported exactly those — so one missing
source file stopped two bundles from resolving. Fragments re-exporting a host's `internal` packages are
also among the most upgrade-fragile constructs there are; flag them for the target.

## 7. A bundle imports an `internal` package the producer does not export

**Detect (boot log):**
```
org.osgi.framework.BundleException: Could not resolve module: <consumer>
  Unresolved requirement: Import-Package: <producer>.internal.<something>
```
Then compare the two `bnd.bnd` files: the producer's `Export-Package` will not list it.

**Check the client's own prebuilt jars before deciding what it means.** If their jars carry the identical
mismatch, the bundle is inactive **in production too** — it is a long-standing defect, not something the
reproduction introduced, and the affected feature has not been running.

**Flag, do not fix.** The one-line fix (adding the package to the producer's exports) would activate a
portlet that has evidently not run, with unknown effects on the pages hosting it; and exporting an
`internal` package is the wrong fix architecturally. Both are the client's decisions.

## 8. `OutOfMemoryError: GC overhead limit exceeded` on the search threads

**Detect:**
```
[elasticsearch[<node>][refresh][T#22089]] ERROR ...Engine - already closed by tragic event
java.lang.OutOfMemoryError: GC overhead limit exceeded
```

**Cause.** With embedded Elasticsearch (§5) the search engine lives **inside the portal JVM** and
competes for the same heap. The 7.2-era default pins the young generation to 1536m of a 2560m heap,
leaving barely 1 GB tenured for the portal *and* the indices. The usual trigger is not steady-state load
but **hot-deploying many modules into a running portal** — replacing dozens of jars under a live
`osgi/modules` mount makes Liferay redeploy them one at a time, and the OSGi churn plus index-refresh
storm exhausts the heap. A very high refresh thread number (`T#22089`) is the tell.

**Fix the trigger, not the symptom.** Stop the portal, deploy, then start it once — so the modules are
present at boot instead of arriving individually. A full reindex against real data completes fine on the
delivered heap. Only if the OOM persists should the heap be raised (`LIFERAY_JVM_OPTS`, see
`reuse-client-docker.md` §Where the JVM options come from), and that is a deviation from the delivered
configuration and therefore a report row. Make the deploy script refuse to run while the portal is up.
