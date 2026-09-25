# Pre-Upgrade Check Gotchas

Field findings from real deliveries. Each one has caused a failed or falsely-passing run.

## Build

- A `##…##` version token from the client's CI makes every module jar fail with `Invalid value for Bundle-Version` while compilation itself passes. Themes are WARs and are not OSGi-version validated, so the theme may build while every module jar fails — that asymmetry is the tell. This is the single most common blocker on a first off-CI build.

- Old Gradle needs an old JDK. Gradle 5.x and 6.x will not run on JDK 17 or later, and the failure never mentions Java — it surfaces as a large Groovy `NoClassDefFoundError ... vmplugin.v7.Java7` trace. Probe `/opt/java`, SDKMAN, and `java_home` before concluding the JDK is absent, and assert the version in any script the team ships. See [`known-build-fixes.md`](known-build-fixes.md) §3.

- Gulp theme tooling built on `liferay-theme-tasks` and node-sass can fail on a too-new Node. Pin or flag the source-era Node in the README.

- A delivery can be several workspaces. Satellite workspaces may compile against shared modules that live in a core workspace and are normally served by the client's private Artifactory. Build the core first with `publishToMavenLocal`. See [`known-build-fixes.md`](known-build-fixes.md) §5.

- macOS `__MACOSX` resource forks look like build output. `._<module>.jar` files are not jars and will poison any glob-based staging. See [`known-build-fixes.md`](known-build-fixes.md) §7.

- `gradlew` may be committed non-executable. Check `git ls-files -s`, not the working tree. See [`known-build-fixes.md`](known-build-fixes.md) §6.

- Blade CLI blocks on an interactive prompt when the workspace carries a `pom.xml`, and answering `Y` rewrites the profile to `maven` and breaks the build. See [`known-build-fixes.md`](known-build-fixes.md) §9.

- Two different out-of-memory failures exist and must not be conflated. The build JVM can die at configuration time on a large workspace, and stale Gradle daemons hold the RAM the next run needs ([`known-build-fixes.md`](known-build-fixes.md) §10). The portal JVM can die at runtime because embedded Elasticsearch shares its heap ([`runtime-findings.md`](runtime-findings.md) §8).

## Artifacts

- The prebuilt-jar trap is the central risk of this skill. When prebuilt custom jars the client shipped inside `bundles/` are not displaced by the source build, the run passes without ever validating the source build. Always replace them, and make verification confirm no prebuilt custom jar remains.

- Compare prebuilt and source `Bundle-SymbolicName` sets, not counts. Equal counts have hidden a bundle that had no source at all. See [`runtime-findings.md`](runtime-findings.md) §6.

## Database

- `/docker-entrypoint-initdb.d/` runs only on first init, against an empty data volume. A second `docker compose up -d` silently does not re-import the dump — force it with `docker compose down -v` first.

- A newer database server restoring an older dump is fine; `pg_restore` is forward-compatible. Watch for missing extensions. Owner and role warnings from an anonymized dump are acceptable, not failures.

- When the dump engine differs from the workspace JDBC config, the runtime follows the dump engine. Flag the mismatch for the client.

- postgres 18 changed its data directory layout. Mount the database volume at `/var/lib/postgresql`, the parent, not `/var/lib/postgresql/data`, or the container exits on init with an unused mount or volume error. postgres 17 and earlier still use `/var/lib/postgresql/data`.

- `OldServiceComponentException` at boot means the delivered source and dump are from different points in time for a Service Builder module. Flag it, do not fix. See [`runtime-findings.md`](runtime-findings.md) §1.

## Runtime

- An XML activation key must be staged under `deploy/`, not `data/license`. `LicenseManager` does not read an XML key placed in `data/license` — it logs "No binary licenses found" and the portal loops on `/c/portal/license_activation`. It reads every file in `data/license`, so never leave a placeholder there either. A 7.4 key does not activate a 7.2 portal.

- Point the document library at an existing store with a git-ignored `docker-compose.override.yml` rather than copying many gigabytes. Without it, `DLFileEntry` and image files 404 with `NoSuchFileException` and content is not indexed. See [`runtime-findings.md`](runtime-findings.md) §4.

- A delivered portal often serves several sites or instances on vanity hostnames. Verify each access point at login, not just `localhost`.

- Deploy with the portal stopped. Hot-deploying many modules into a live portal starves embedded Elasticsearch and ends in `OutOfMemoryError`. See [`runtime-findings.md`](runtime-findings.md) §8.

- There is no reindex API — no REST, JSONWS, or Gogo command. CHECK 4 must go through the Search Administration UI, driven by Playwright or a human.

## Docker

- A missing bind-mount source is created as a directory by Docker, which the portal then fails to read as a file. A directory mount also replaces whatever the image had at that path, so compare by name and size before mounting over one. See [`reuse-client-docker.md`](reuse-client-docker.md) §Docker bind-mount pitfalls.

- Name the compose project. Without a top-level `name:`, two deliveries whose folders share a basename resolve to one project, and a `docker compose down` in either removes the other's containers. See [`reuse-client-docker.md`](reuse-client-docker.md) §Project isolation.

- With the image model there is no multi-gigabyte bundle to copy — only `document_library/` is large, and it is read-only data, so bind-mount it from its existing path. In the bundle-fallback shape, copy the bundle, because its deploy folder is mutated.

## Environment

- The workspace may not be a Git repository. The skill runs in report-only mode then, and the change-logs are the audit trail. Do not assume `git`.
