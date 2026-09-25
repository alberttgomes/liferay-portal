---

argument-hint: "[run-id]"
description: Validate the client artifacts delivered before an upgrade by reproducing the client's original environment from source. Use when the user invokes `/pre-upgrade-check` or asks to validate, preflight, or reproduce a delivered workspace, dump, and Docker environment.
name: pre-upgrade-check

---

# Pre-Upgrade Check

A preflight that runs before `/upgrade-init` and the upgrade phases. The team receives the client's artifacts in a Drive folder — workspace source, a database dump, and sometimes a document-library archive and a runnable Docker environment. This skill proves those artifacts are sound by reproducing the client's original environment from source. When something is wrong it fixes what it safely can, documents every source change for the client, and reports the rest.

| Check | Must Pass |
| --- | --- |
| CHECK 1 | All modules and themes build from source, not from prebuilt jars. |
| CHECK 2 | The database dump is importable. |
| CHECK 3 | The original Liferay connects to the imported database. |
| CHECK 4 | A reindex completes against the real data. |

| Output | What It Is |
| --- | --- |
| `report.csv` and `report.md` | The artifacts-feedback report, one row per issue. |
| `change-logs/NNN-*.md` | One per source edit or artifact replacement — the record returned to the client. |
| `UPGRADE-README.md` | How to stand the original source-version environment up. |

## Scope and Boundary

- Source version only. This skill never touches the upgrade target. It reproduces the client's current version — no schema migration, no version bumps, no API remediation.

- From source, never prebuilt. Prebuilt jars the client delivered are replaced by the freshly built artifacts and never trusted as the thing under test.

- No database writes beyond the import. Bringing the database container up and consuming the dump restore is allowed; running arbitrary SQL is not. Hand any data inspection to the user.

- No agent commentary in files that ship to the client. Files the agent authors carry no explanatory comments, and files derived from the client's keep only the client's, byte for byte. The reasoning belongs in the change-logs and the report.

## Prerequisites

- A Liferay workspace was delivered, with `gradlew`, `gradle.properties`, and `settings.gradle`.

- A database dump was delivered, at a known path or discoverable in the delivery folder.

- Docker is available, for the database container and, for CHECK 3 and CHECK 4, the portal.

- CHECK 3 and CHECK 4 additionally need a runnable source-version runtime — either one the client shipped, in the base-workspace shape with the source-line activation key and config, or one that can be scaffolded from the official `liferay/dxp` image or a client bundle. When neither is available, CHECK 1 and CHECK 2 still run and the other two are reported BLOCKED rather than failed.

This skill is Git-optional. When the workspace is not a repository it runs in report-only mode and the change-logs are the audit trail.

## Reading CLAUDE.md

`CLAUDE.md` is usually still all `TODO` at preflight time, because this runs before `/upgrade-init`. Detection is the primary source, and these fields are fallbacks and overrides only.

| Field | Used For | Fallback |
| --- | --- | --- |
| **customer.name** | Report title and file labels | The workspace directory name, else `client` |
| **db.name** | Database and restore target | Compose environment such as `POSTGRES_DB`, else `lportal` |
| **docker.compose.file** | Existing compose to reuse | Auto-detect in the workspace and sibling delivery folders, else `docker-compose.yml` |
| **docker.services.database** | Database service name | Detect from compose, else `database` |
| **docker.services.portal** | Portal service name | Detect from compose, else `liferay` |
| **docker.services.search** | Search service name | Detect from compose, else `elasticsearch` |
| **modules.root** | Where to walk modules | `liferay.workspace.modules.dir`, else `modules/` |
| **themes.root** | Where to walk themes | `liferay.workspace.themes.dir`, else `themes/` |

Never derive the upgrade target from `CLAUDE.md` here — this skill is source-only.

## Gotchas

Read [`references/gotchas.md`](references/gotchas.md) before starting. Three of them change control flow and are repeated here:

- The prebuilt-jar trap. When prebuilt custom jars are not displaced by the source build, the run passes without validating anything.

- `/docker-entrypoint-initdb.d/` runs only against an empty volume, so re-importing needs `docker compose down -v` first.

- There is no reindex API, so CHECK 4 must go through the Search Administration UI.

## Verification Loop

Each check is a loop, not a single pass — fix one thing, re-observe, and only then move on. The skip guard on each step is that loop's exit predicate, which is what lets a re-run resume at the first non-passing check instead of starting over.

- CHECK 1 — build, fix, rebuild. Exits on `BUILD SUCCESSFUL` for every module, or on a blocker that cannot be resolved. A failed theme does not block.

- CHECK 2 — import, evaluate the restore log, fix, re-import. Exits on a complete, untruncated restore.

- CHECK 3 — boot, evaluate the log, fix config, search, or compose but never the database, restart, re-verify, then log in at every access point. Exits on a clean boot and a successful login per instance.

- CHECK 4 — trigger, tail, and on a non-database cause fix and re-trigger. Exits when the reindex completes. A database-rooted error is handed to the user as SQL, never executed, and the check is reported BLOCKED rather than retried.

## Workflow

Run the steps in order. Each has a skip guard so a re-run resumes at the first non-passing check. Write everything for a run under `pre-upgrade-notes/<run-id>/`.

### Set Up the Run

Resolve `<workspace.root>` to the nearest ancestor or descendant directory containing `gradlew`, `gradle.properties`, and `settings.gradle`. Set `<run-id>` to `preflight-<UTC-yyyymmdd-HHMM>`, and create `<workspace.root>/pre-upgrade-notes/<run-id>/` with `logs/`, `change-logs/`, and `screenshots/`.

Detect Git with `git -C <workspace.root> rev-parse`. When it is not a repository, note report-only mode.

Skip when the user passed an existing `<run-id>` to resume — reuse its directory and re-enter at the first non-passing check.

### Inventory the Workspace

Auto-detect without prompting and snapshot to `inventory.md`:

1. `<source-version>` from **liferay.workspace.product**, verbatim, for example `dxp-7.2-sp8`. The `dxp-` or `portal-` prefix distinguishes DXP from CE.

1. `<source-java>` from the source line, through [`references/source-version-matrix.md`](references/source-version-matrix.md). 7.2 maps to Java 8.

1. Build tooling read verbatim from files — Gradle wrapper version, workspace-plugin version from `settings.gradle`, bnd version, and Node package manager.

1. Module inventory by walking **modules.root** and counting `bnd.bnd` per subdirectory, and theme inventory by walking **themes.root** and classifying each theme as gulp/npm or Gradle.

Confirm with the user only on conflict, when `CLAUDE.md` disagrees with `gradle.properties`.

Skip when `inventory.md` already exists for this run.

### Detect the Database Engine and Dump Format

This is a deterministic read. Never ask the user the engine.

1. Locate the dump in the delivery folder, a sibling `*_docker/dump/`, or a path the user gave.

1. Run `file <dump>` and read the leading bytes, mapping through [`references/dump-format-detection.md`](references/dump-format-detection.md). `PGDMP` means PostgreSQL custom format, restored with `pg_restore`. A leading `-- PostgreSQL database dump` or `-- MySQL dump` means plain SQL, restored with `psql -f` or `mysql <`. An MSSQL `.bak` header means a SQL Server `RESTORE DATABASE`, which is handed to the user because the team does not run mssql tooling.

1. Cross-check the detected engine against the workspace and runtime JDBC config in `portal-ext.properties` and the compose environment. Flag any mismatch as a report row.

Skip when the engine and format are already recorded for this run.

### CHECK 1: Build Everything From Source

1. Use the workspace `./gradlew`. Confirm the active JDK matches `<source-java>` — Gradle 6.6.1 will not run on JDK 17 or later, so flag it and select a JDK 8 when one is available.

1. Run the full build, capturing the complete log and never truncating it:

	```bash
	./gradlew clean build --console=plain 2>&1 | tee pre-upgrade-notes/<run-id>/logs/build.log
	```

1. Build each gulp or npm theme separately, since it is not part of the Gradle build:

	```bash
	(cd <themes.root>/<theme> && npm ci && npx gulp build)
	```

1. Iterate build, fix, rebuild until green or until a blocker cannot be resolved. For every fix, write a change-log from `assets/change-log.template.md` and add a report row classified as either a reproduction-environment fix, needed only because the build runs outside the client's network, or a genuine source defect, where the delivered source does not build against its own version. Genuine defects are the highest-value findings — confirm before editing unless the fix is mechanical and unambiguous.

The artifacts produced here are the only ones allowed downstream. The client's prebuilt jars never satisfy a check.

Consult [`references/known-build-fixes.md`](references/known-build-fixes.md) first for common failures. The recurring ones are a CI version token that fails every bundle at jar time, internal-only repositories that must be supplemented rather than replaced, and a missing source JDK or fragile gulp theme tooling that is best handled in a period-correct container. Never let a failed theme build block CHECK 1 — neither the module build nor the portal depends on the theme WAR.

Skip when `build.log` shows `BUILD SUCCESSFUL` and no source has changed since.

### Provision the Source-Version Runtime

Prefer reuse over scaffolding. Detail in [`references/reuse-client-docker.md`](references/reuse-client-docker.md).

1. Search the workspace, **docker.compose.file**, and sibling delivery folders such as a `*_docker/` next to the workspace for the canonical base-workspace shape — `docker-compose.yml`, a `docker-compose/database/dump/`, a `docker-compose/liferay/mnt/liferay/files/` config tree, a `document_library/`, and in the bundle-fallback shape a `Dockerfile` and `bundle/`.

1. When found, incorporate it. Copy the small pieces into `<workspace.root>` and stage the dump into `docker-compose/database/dump/`. Bind-mount the large `document_library/` from its existing path. Add a report row recording where the environment came from.

1. When not found, ask whether to scaffold one. On confirmation, render `assets/docker-compose.source.template.yml` for the source version, stage the Elasticsearch connection config under `docker-compose/liferay/mnt/liferay/files/osgi/configs/`, and add a report row, because the scaffold ships to the client. When no image is pullable for the exact source patch level, fall back to the bundle-on-JDK-base shape in [`references/source-version-matrix.md`](references/source-version-matrix.md).

1. Declare a top-level `name: <customer>` in the compose file. Declaring it in the file means it cannot be forgotten, unlike `--project-name` on the command line.

1. Reproduce the runtime's `osgi/marketplace` deliberately. Compare the delivered `.lpkg` set against the official image's by name and size before deciding whether to mount individual files or the whole directory.

Confirm before writing or copying files into the client workspace, and confirm the exact image or bundle tag.

Skip when the workspace already has a source-version compose wired to the dump and document library.

### Replace Prebuilt Artifacts With the Source Build

The runtime must run the agent's build, not the client's prebuilt jars. In the image model the container starts stock and mounts only `bundles/*`, so the build output from CHECK 1 is what runs, and the only cleanup is displacing prebuilt custom jars the client shipped inside `bundles/`.

1. Identify the client's prebuilt custom artifacts by group or symbolic name matching the workspace modules, such as `com.<customer>.*` or each module's `bnd.bnd` **Bundle-SymbolicName**. Look in `bundles/osgi/modules`, `bundles/deploy`, and `bundles/osgi/war`.

1. Remove those prebuilt custom jars and WARs. Never touch the stock image's platform jars — in the bundle-fallback shape, leave `osgi/core`, `osgi/marketplace`, and `osgi/portal` alone.

1. Stage the freshly built jars into `bundles/osgi/modules` and `bundles/deploy`, and the gulp-built theme WAR into `bundles/osgi/war`.

1. Record exactly which prebuilt artifacts were displaced, as one change-log and one report row.

Skip when the mounts already carry this run's built artifacts and no prebuilt custom jar remains.

### CHECK 2: Import the Dump

1. Bring up only the database container with `docker compose up -d <db-service>`.

1. Import. When the setup auto-restores, because the dump sits under `docker-compose/database/dump/` mounted into `/docker-entrypoint-initdb.d`, monitor the database log to completion using a background `Monitor` until-loop on `pg_isready` or the restore's completion line — never a foreground sleep. Otherwise run the restore directly, which is an import rather than a query:

	```bash
	docker compose exec -T <db-service> pg_restore -U <user> -d <db.name> \
		--clean --if-exists --no-owner --no-privileges /dump/<dump-file>
	```

	Capture either path to `logs/db-import.log`.

1. Evaluate. Owner, role, and privilege warnings on an anonymized dump restored with `--no-owner` are acceptable. Flag encoding mismatches, server-version incompatibility, missing extensions such as `pg_trgm` or `uuid-ossp`, and any truncated transfer, sanity-checking the restored size and table count against the source.

1. Hand any data inspection to the user as SQL rather than opening a session. [`references/dump-format-detection.md`](references/dump-format-detection.md) carries the query that confirms the real source build.

Skip when `db-import.log` shows a clean restore for this run.

### Incorporate the Document Library

When a document-library archive or volume was delivered, bind-mount it at `/opt/liferay/data/document_library`, from `bundles/data/document_library` or the delivered path. When none was delivered, mark it not provided in the report and README. This is not a failure.

Skip when the document library is already wired or confirmed absent.

### CHECK 3: Connect the Portal to the Imported Database

1. Bring up the database, search, and portal services with `docker compose up -d <db-service> <search-service> <portal-service>`.

1. Reuse the `upgrade-startup` skill's Catalina-phase log monitoring, targeted at the source version, capturing to `logs/catalina-boot.log`. Confirm that JDBC connects with no `Could not create connection to database server`; that the portal connects to the delivered search topology reproduced as delivered, whether a separate `elasticsearch` container or the embedded engine, flagging embedded Elasticsearch as an upgrade-target concern; that a DXP activation key registers with no redirect loop to `/c/portal/license_activation`; and that Catalina initializes with no blocking `SEVERE`. Environment-specific unreachable hosts such as LDAP and internal APIs are non-blocking warnings.

1. Log in, because a clean boot is not enough. For each delivered access point and instance, log in with the delivered credentials through Playwright MCP, confirming first, and confirm the home page renders rather than an error page. A post-login `NoSuchResourcePermissionException`, or "Someone may be trying to circumvent the permission checker", means the dump is missing that site's `ResourcePermission` rows and is a blocker for that instance. Client portals often expose multiple sites on vanity hostnames, so keep `LIFERAY_VIRTUAL_...VALID_...HOSTS=*`, document the `/etc/hosts` entries in the README, and verify each host. Using `curl -H "Host: <vanity-host>" http://localhost:8080/` avoids editing the hosts file.

1. For database-rooted boot or login errors, suggest investigative SQL from [`references/runtime-findings.md`](references/runtime-findings.md) for the user and never execute it.

Skip when a clean boot log and a successful login per instance exist for this run.

### CHECK 4: Complete a Reindex

Reuse the `upgrade-reindex` skill's mechanics.

1. Trigger a general reindex through Control Panel, Search Administration, Reindex all, driven by Playwright MCP or by a human as fallback. Confirm before logging in or triggering, since both are privileged. Capture before and after screenshots to `screenshots/`.

1. Tail and evaluate the reindex log into `logs/reindex.log`, confirming completion with no errors.

1. State explicitly in the report that the reindex ran against real data from the imported dump, which is the meaningful case, rather than an empty or seed dataset.

1. For database-rooted reindex errors such as orphaned references or integrity failures, suggest SQL for the user or DBA and never execute it.

Skip when a clean reindex log exists for this run.

### Generate the Deliverables

Generate after the checks. Partial output is allowed, with blocked rows marked. Column definitions are in [`references/report-columns.md`](references/report-columns.md).

1. The feedback report — `report.csv` from `assets/report.header.csv`, a styled `report.xlsx` via `assets/report-xlsx.py`, and `report.md` carrying the same rows as a GitHub table. One row per issue from every preceding step. The `Page` column names the affected page and is blank for build and config rows, `Response` is left empty, and `Analysis` cross-references the change-log filename. Import the `.xlsx` to keep the styling.

1. The per-fix change-logs, already written incrementally under `change-logs/`. Verify that every report row involving a source edit has a matching file.

1. `UPGRADE-README.md`, rendered from `assets/UPGRADE-README.template.md` for the source version and written to `<workspace.root>/UPGRADE-README.md`, since it ships to the client. Copy it into the run directory for the record.

1. The workspace `.gitignore`, rendered from `assets/gitignore.template`, so the dump, document library, activation key, and any `docker-compose.override.yml` stay out of Git while the config tree, `01-restore.sh`, and `docker-compose.yml` are committed. Keep the repository private — `portal-ext.properties` may carry application encryption seeds.

1. The chat summary described under **Output**.

## Checklist

Copy into `pre-upgrade-notes/<run-id>/summary.md`. Every box is an assertion about state, and each one exists to close one of this run's three documented false-pass modes.

- [ ] CHECK 1 — `build.log` ends `BUILD SUCCESSFUL` for every module. A failed theme does not block.
- [ ] No prebuilt client jar remains in the mounts; the source build is what runs.
- [ ] CHECK 2 — `db-import.log` shows a complete restore, not a truncated one.
- [ ] CHECK 3 — a clean boot log and a successful login at every delivered access point.
- [ ] CHECK 4 — the reindex completed, and the report states whether it ran against real data.
- [ ] Every source edit has both a change-log file and a report row.
- [ ] Three deliverables written — report as CSV, xlsx, and Markdown; change-logs; `UPGRADE-README.md`.

## Autonomy Boundaries

Autonomous: all file reads; inventory, version, and engine detection; running the full source build and capturing logs; applying build fixes and writing their change-logs and report rows; incorporating client Docker files; replacing prebuilt artifacts with the source build; bringing containers up and down; consuming or running the restore and checking container readiness; non-database fixes to search, repositories, or compose, followed by restart and re-verification; Playwright-driven reindex log evaluation; and writing all report, README, and change-log files.

Confirm first: copying files into the client workspace, especially a large `bundle/` or `document_library/`; scaffolding a compose file when the client shipped none; the exact image or bundle tag; moving rather than copying a large dump; logging in or triggering the reindex; and any genuine source-defect edit that is not mechanical and unambiguous.

Never: run arbitrary SQL or any schema write beyond the dump import; upgrade or migrate the database schema, since this is the pre-upgrade source version; validate using the client's prebuilt jars; delete the client's internal repository entries, which are valid inside their network; inline database or admin passwords into `CLAUDE.md`, the README, or compose; reset the admin password silently; declare a check passing on a truncated import; claim the reindex meaningful without stating it ran against real data; or add explanatory comments to any file that ships to the client.

## Output

The deliverable layout is `assets/output-layout.txt.template`.

Report per-check PASS, FAIL, or BLOCKED, the count of report rows, the count of change-logs, and pointers to the files. When any check is BLOCKED, say why and what the client or user must provide to unblock it.
