---
name: upgrade-setup-version
description: Run this skill when Phase 1 (Upgrade Environment) of the upgrade playbook executes, or when the user invokes `/upgrade-setup-version` directly. Applies the workspace-level ENVIRONMENT setup to point a Liferay workspace at the target version — gradle.properties, docker-compose image tags (Liferay + database + search), settings.gradle, gradle wrapper, build.gradle dependency-configuration migration, release.portal.api→release.dxp.api, sourceCompatibility cleanup, the root `allprojects` block, redundant release.dxp.api dependency pruning, and JDBC driver presence. Trigger also when the user says "set up the workspace version", "run phase 1", or "point the workspace at the target version".

---

# upgrade-setup-version (Phase 1 — Upgrade Environment)

Applies the **environment / build-tooling** changes needed to point the Liferay workspace at
`upgrade.target.version`. Runs as Phase 1 of the upgrade, or standalone. Each sub-step commits
atomically so the work is always recoverable.

Environment setup only — no source migration. Source-level work belongs to Phase 2 (`upgrade-compile`).

`release.dxp.api` is a fat JAR with an empty POM, so it shows as a leaf in the dep tree; audit it with
`unzip -l` and see `references/release-dxp-api-redundant.md` for the provided / not-provided packages.

## Prerequisites

- Upgrade branch is checked out.
- `upgrade.target.version` is known. If `CLAUDE.md` is missing or has it as `TODO:`, derive it from
  the **current** `liferay.workspace.product` in `gradle.properties` and confirm the new value.
- Working tree is clean. If `git status` is non-empty, surface it and stop.

## Step 0 — Locate the workspace root

Do **not** assume the workspace is at the repo root.

1. From the current directory, search for the closest directory containing all three of: `gradlew`,
   `gradle.properties`, `settings.gradle`.
2. Treat that directory as `${workspace.root}`. All paths below are relative to it.
3. If multiple candidates exist, ask the user. If none exist, stop and surface the error.

## Commit convention

`<TICKET> <subject>`, per the `commit` skill. Under `/upgrade-phase`, `<TICKET>` is the **Technical
`NOISSUE` prefix required by `.claude/rules/commit.md` when no ticket applies.

## Reading values from CLAUDE.md (with fallback)

| Field | Used for | Fallback |
|---|---|---|
| `upgrade.target.version` | `liferay.workspace.product`, docker tag | Read current `liferay.workspace.product`, ask user to confirm |
| `upgrade.target.java.version` | Docker `JAVA_VERSION`, `allprojects` Java level | Step 0b blade probe; else infer (≥ 2025.q3 → 21, else 17/11) |
| `node.version` | `allprojects` node block | Step 0b / ask user (target-era default: 2024.Q4+ → 20.x) |
| `db.target.type` / `db.target.version` | docker-compose **database** image; JDBC driver decision | Ask user (gathered by `/upgrade-init`) |
| `search.version` | docker-compose **search** image | Ask user (gathered by `/upgrade-init`) |
| `upgrade.workspace.plugin.version` | `settings.gradle` | Step 0b probe; else ask; skip Step 3 if declined |
| `upgrade.gradle.version` | `gradle-wrapper.properties` | Step 0b probe; else ask; skip Step 4 if declined |
| `docker.compose.file` | Docker compose path | Auto-detect (workspace root and `docker/`) |
| `modules.root` | module discovery | Default `modules/` |

> **New CLAUDE.md fields** (`node.version`, `db.target.type`, `db.target.version`, `search.version`)
> are populated by `/upgrade-init`. If `/upgrade-init` has not yet been updated to gather them,
> ask the user and continue; do not block.

### Version derivation

**Fallback only** — used when blade is unavailable. Given `upgrade.target.version = "2026.q1.6-lts"`:
- **`liferay.workspace.product`**: keep the current value's prefix, replace the version part
  (`dxp-7.3.10-ga1` → `dxp-2026.q1.6-lts`).
- **Docker tag**: strip `dxp-`/`portal-` prefix (`2026.q1.6-lts`).

## Scope-creep guardrail

Before any mass-sweep edit (Steps 5, 6, 7): print the file count and a sample diff of 2-3 files and
**confirm with the user** — especially when a single pass touches more than 10 files.

## Rollback contract

- Every step commits atomically. Reverting = `git revert <sha>`.
- Working-tree pollution (interrupted gradle runs, plugin side-effects) is **NEVER committed**;
  always `git status` + `git restore` first.
- If a step partially completes and the rest is uncertain, abort, restore the tree, and surface to
  the user — do not improvise.

## Gotchas

- **`tail -N` on chained gradle invocations swallows real errors** — capture full output per module
  to a log; never truncate.
- **`--no-daemon` in a tight loop forks fresh JVMs** and has caused spurious "BUILD FAILED in 1s" —
  prefer one gradle invocation with multiple tasks.
- **Liferay workspace plugin 15.x rewrites `.gitignore`** at the workspace root on first task
  invocation — snapshot/restore around any gradle run.
- **A below-floor Elasticsearch image compiles fine and blocks Phase 3 two phases later** with `does
  not meet the minimum version requirement of <X>`. The target enforces a floor — read it from the
  target source, don't assume the current tag is acceptable.
- **The activation key copy must be byte-exact.** A single changed character silently breaks
  activation — never re-emit the key by hand.
- **A bare `java { }` or `node { }` block throws on a subproject that lacks the plugin**, so the root
  `allprojects` block must guard with `plugins.withType(...)`. An unguarded `node { }` is fragile on
  mixed workspaces.

## Steps

Execute in order. Each step is idempotent — a re-run should detect "already applied" and skip
without committing.

---

### Checklist

Copy into `upgrade-notes/<run-id>/phase-1-environment/summary.md` and tick as you go. Runs get
interrupted and resumed, so a step that was **skipped** must read as skipped — never as done.

- [ ] Step 0b — every version resolved from the blade probe or the BOM fallback, **none guessed**
- [ ] Step 1 — `gradle.properties`
- [ ] Step 2 — docker-compose images: Liferay, database, **and** search
- [ ] Step 2b — JDBC driver present for `db.target.type`
- [ ] Step 2c — activation key placed byte-exact, stale source-line keys removed, decision logged
- [ ] Step 3 — `settings.gradle` workspace plugin
- [ ] Step 4 — `gradle-wrapper.properties`
- [ ] Step 5 — legacy dependency configurations migrated
- [ ] Step 6 — `release.*.api` normalized, explicit version dropped
- [ ] Step 7 — `sourceCompatibility` / `targetCompatibility` and empty wrappers removed
- [ ] Step 7b — root `allprojects` block present and guarded
- [ ] Working tree clean; every step either committed or recorded as skipped

### Step 0b — Resolve canonical versions from blade (authoritative source + validity gate)

**This step is MANDATORY and runs first.** `blade init -v <upgrade.target.version>` is both the
**validity gate** for the target version and the **authoritative source** for every version-bearing
value Phase 1 writes — `liferay.workspace.product`, the docker image tag, the workspace-plugin,
gradle and java versions. `CLAUDE.md` is only a cache of them.

**Read `references/blade-version-probe.md` before executing this step** — it carries the probe
command, the partial-scaffold check, how to resolve the exact product key, and the verbatim-read
table.

Hard rules:

- **Never guess "latest"** for the workspace plugin, and **never string-derive**
  `liferay.workspace.product` or the docker image tag. Read them from the probe verbatim. If a value
  cannot be resolved from the probe or the BOM fallback, **stop and ask** — never write an unverified
  version.
- **Never change the quarter/patch number on your own.** When the requested version isn't targetable,
  surface the available keys for that line and confirm.
- **No SB/REST-builder tool pin in any `build.gradle`.** Blade's plugin carries release-matched REST
  Builder, bnd and Jakarta tooling; the Service Builder tool is the one exception and is corrected by
  changing the *plugin* regen-only in Phase 2, never by a tool force.

**Pre-flight:** `command -v blade`. If blade is absent, use the **fallback** below.

**Fallback (blade unavailable):** validate `<upgrade.target.version>` by resolving the release BOM
POM — `https://repository-cdn.liferay.com/nexus/content/repositories/liferay-public-releases/com/liferay/portal/release.dxp.bom/<v>/release.dxp.bom-<v>.pom`
(a 404 means the version string is wrong — try variants). Only then derive product/tag per the
**Version derivation** fallback below. If neither blade nor the BOM resolves, **stop and ask** — do
not write.

**Persist:** cache at `.claude/cache/upgrade/probe-<sanitized-target>.json`; write the resolved
values into `CLAUDE.md` (replace `TODO:` markers, and **overwrite any value that disagrees with
blade — blade wins**, log the correction as a human-review note); append one line to
`upgrade-state.md` recording the source (`blade` vs `bom-fallback`).

**Skip if:** the probe cache for the current target already exists and `CLAUDE.md` agrees with it.

**Commit:** `<TICKET> Resolve canonical versions from blade init -v <upgrade.target.version>` — only
if `CLAUDE.md` was modified.

---

### Step 1 — Update `gradle.properties`

1. Replace `liferay.workspace.product` with the value **resolved in Step 0b** (verbatim from blade;
   the string-derivation fallback applies only when blade was unavailable).
2. Remove these legacy property lines (including commented variants):
   `liferay.workspace.target.platform.version`, `liferay.workspace.docker.image.liferay`,
   `liferay.workspace.bundle.url`, `app.server.tomcat.version`.

(Source-formatter is no longer run, so do **not** add `com.liferay.source.formatter.version`.)

**Skip if:** the product already matches and the legacy lines are gone.

**Commit:** `<TICKET> Set workspace product to <new-value>`

---

### Step 2 — Update docker-compose images (Liferay + database + search)

This is environment alignment, not a DB/search operation — it only repoints image tags.

1. Find the compose file via `docker.compose.file` or auto-detect (`workspace.root` and `docker/`;
   skip `build/`, `.gradle/`, `.idea/`, `bin/`, `.git/`).
2. **Liferay image:** replace `image: liferay/dxp:<tag>` with the docker tag **resolved in Step 0b**
   (verbatim from the probe — this is what resolves the `-lts`/no-`-lts` question). Preserve
   leading whitespace. In the Liferay service's `environment:` block, add/update
   `JAVA_VERSION=zulu<N>` per `upgrade.target.java.version` (≥ 2025.q3 → `zulu21`, earlier 7.4 →
   `zulu17`).
3. **Database image:** repoint the DB service image to `db.target.type` / `db.target.version` (e.g.
   `mysql:8.0`, `postgres:16`). If the DB type itself is changing, update the image name and any
   type-specific env. If the compose has no DB service (external DB), skip and note it.
4. **Search image:** repoint the search service image to `search.version` (the engine type does not
   change in the upgrade — only the version). **Mind the minimum-version floor:** the target portal
   enforces a minimum Elasticsearch version and bundles/tests a specific one — set the image **at or
   above** that floor, or the portal halts at startup with `ElasticsearchSearchEngine: … does not meet
   the minimum version requirement of <X>`. Read the authoritative value from the target source
   (`ElasticsearchDistribution.VERSION` / `sidecar.version` in `liferay-portal-ee`); for **2026.q1**
   the floor is **8.19** and the bundled/tested version is **8.19.11**. Don't guess a lower patch — a
   below-floor image (e.g. `8.17.x`) compiles fine but blocks Phase 3 startup.

**Skip if:** all three images and `JAVA_VERSION` already match the targets.

**Commit:** one commit per logical change, e.g. `<TICKET> Bump dxp image to <new-tag>`,
`<TICKET> Bump database image to <db.target.type>:<db.target.version>`,
`<TICKET> Bump search image to <search.version>`.

---

### Step 2b — JDBC driver presence

Liferay bundles **already ship** the common JDBC drivers (MySQL/MariaDB, PostgreSQL, HSQL). Only
**non-bundled** drivers need adding.

1. Read `db.target.type`. If it is MySQL/MariaDB/PostgreSQL/HSQL → Liferay bundles it; **nothing to
   add**.
2. If it is **SQL Server** (`mssql-jdbc`) or **Oracle** (`ojdbc`) → ensure the driver jar is present
   for the target bundle/image (the team's bundle layout, typically
   `tomcat/.../shielded-container-lib/`).
3. **Update for the Java upgrade:** if a driver is already present but its version predates the
   target JDK (older drivers fail on Java 21), bump it to a JDK-compatible release.

Phase 3 (`upgrade-startup`) only **verifies** the driver sits in the correct path — adding/updating
is here.

**Skip if:** the DB type is one Liferay bundles, and no version bump is required.

**Commit:** `<TICKET> Add <driver> JDBC driver for <db.target.type>` (or `Update <driver> to <ver>`).

---

### Step 2c — DXP developer license placement

Liferay **DXP requires a license**. With none, the portal redirects every request to
`/c/portal/license_activation` and never boots usable (a Phase 3 blocker). There is **no property to
disable the license** — a **developer activation key** is the supported mechanism. (Liferay **Portal
CE** needs no license — skip this step for `portal-`/CE targets.)

The agent ships a default DXP developer activation key as an asset and places it so the **target**
portal (booted in Phase 3) registers it.

1. **DXP only.** If `liferay.workspace.product` (resolved in Step 0b) is a `portal-`/CE key, skip.
   Run only for `dxp-` targets.
2. **Resolve the committable deploy target.** The key must (a) land in the container's
   file-install/deploy path at boot **and** (b) be **committed** — it ships to the client. `bundles/`
   is **gitignored**, so do **not** use `bundles/deploy`. Use the Liferay docker **"files" overlay**:
   the docker-compose volume whose **container** side is `/mnt/liferay/files` (the image copies its
   contents into `LIFERAY_HOME` at boot). The key goes in that **host** path's **`deploy/`** subdir,
   created if absent — e.g. mount `./docker-compose/liferay/mnt/liferay/files:/mnt/liferay/files` →
   place at `docker-compose/liferay/mnt/liferay/files/deploy/`. If the compose has **no**
   `/mnt/liferay/files` mount, **ask the user** for the tracked deploy overlay — never fall back to a
   gitignored `bundles/` path.
3. **Sweep stale source-line keys (autonomous — do NOT ask).** In an upgrade the deploy overlay
   almost always still holds the **pre-upgrade** version's activation key (e.g. a `7.2` developer
   key), which is **stale by definition** — wrong product line, usually expired, and only clutter the
   target portal processes at boot. Scan the deploy overlay for every `.xml` whose **root element** is
   `<license>`/`<licenses>`, read its `<product-version>`, and **delete** any whose line is **older
   than the target line** (e.g. a `7.2` key when the target is a `2026.qx`/7.4 product). This is
   deterministic cleanup of a now-invalid artifact, so **remove it directly — this is a deliberate
   exception to the usual "file deletion confirms" rule** (see Autonomy boundaries); stage the
   deletion and commit it per the **Commit** block below (folded with the placement in the common
   case). **Never** delete a key already on the **target** line — that may be the workspace's own
   valid license (handled in step 4).
4. **Idempotent presence check — by content, not filename.** Liferay's file-install (`LicenseInstaller`)
   picks up any `.xml` whose **root element** is `<license>` or `<licenses>` (the filename is
   irrelevant). After the step-3 sweep, if a `<license>`/`<licenses>` `.xml` **on the target line**
   still remains (the workspace brought its own valid key), **skip placement** — do not overwrite or
   add a second key.
5. **Place the key.** `cp` the shipped asset
   `.claude/skills/upgrade-setup-version/assets/activation-key-dxpdevelopment-7.4-developeractivationkeys.xml`
   into the resolved deploy overlay (keep a `.xml` extension; **byte-exact** — see `## Gotchas`).
6. **Version-line guard.** The shipped key is the DXP **7.4** developer line, which covers all
   `2026.qx` targets (they are the 7.4 line). If the resolved target is **not** on the 7.4 line, do
   **not** assume it fits — flag for human review and ask for the matching key.

**Commit.** One commit when it is a clean one-for-one swap (`<TICKET> Replace stale <source-line>
developer activation key with <target-line>`); separate commits otherwise. These are normal
**workspace deliverables** on the upgrade branch — they ship to the client and are **not** in the
agent's excluded file set.

**Record in the decision log (required).** Append one line to the **Decision log** in
`upgrade-state.md` naming what was removed, what was placed, and the commit sha. Forced, because
removing a stale source-line key is an autonomous deletion and every autonomous deletion leaves a
trail even when nothing is flagged. Separate from the human-review flag below: the log records what
the agent did, the flag records what the human must still do.

**Confirmed at:** Phase 3 (`upgrade-startup` Catalina gate) — `License registered for DXP Development`
in the log / no `license_activation` redirect.

**Flag for human review:** this is a **developer** key — it must be replaced with the customer's
**production** license before go-live.

**Skip if:** target is CE/portal, or a `<license>`/`<licenses>` `.xml` **on the target line** is
already present in the deploy overlay (a source-line key is not a reason to skip — it is removed in
step 3).

---

### Step 3 — Update `settings.gradle` workspace plugin

Replace the `com.liferay.gradle.plugins.workspace` version string. Drop any peer plugins the new
workspace plugin bundles or no longer needs (`biz.aQute.bnd`, `net.saliman.properties` for 15.x).

**Commit:** `<TICKET> Bump workspace plugin to <new-version>`

---

### Step 4 — Update `gradle-wrapper.properties`

Update `distributionUrl` to `gradle-<version>-bin.zip` matching the workspace plugin's minimum
(15.x → 8.5+). Enable `validateDistributionUrl=true` if missing.

**Commit:** `<TICKET> Bump Gradle wrapper to <new-version>`

---

### Step 5 — Migrate legacy Gradle dependency configurations

Across all `build.gradle` files under `workspace.root`:

| From | To |
|---|---|
| `compile ` | `compileOnly ` |
| `testCompile ` | `testCompileOnly ` |
| `runtime ` | `runtimeOnly ` |
| `testRuntime ` | `testRuntimeOnly ` |

Liferay maps `compile` → **`compileOnly`** (not `implementation`): OSGi modules compile against
portal packages the runtime provides and must not bundle them. Use line-anchored regex
(`^[[:space:]]*<keyword>[[:space:]]`) so `compileClasspath`/`compileJava`/`compileOnly` are not
corrupted.

**Stage only:** `git add '**/*.gradle'` — never `git add -A`.

**Commit:** `<TICKET> Migrate legacy Gradle dependency configurations`

---

### Step 6 — Normalize the `release.*.api` dependency to `release.dxp.api` (no version)

The **only** release-API change in Phase 1 (the obvious, mechanical one). Any `release.portal.api`
**or** `release.dxp.api` declaration becomes `release.dxp.api` **without an explicit `version:`** —
the version is managed by the workspace product (the target). So:
- `… name: "release.portal.api", version: "7.2"` → `… name: "release.dxp.api"` (drop the version)
- `… name: "release.dxp.api", version: "7.2"` → `… name: "release.dxp.api"` (drop the version)
- `… name: "release.portal.api"` (no version) → `… name: "release.dxp.api"`

Apply even in commented-out lines. Dropping the explicit version is required — keeping `"7.2"` pins
a stale release.

> **Not here:** consolidating *other* individual Liferay deps into `release.dxp.api`, and pruning
> redundant/unused deps, is the per-module **compile-verified dependency cleanup** in Phase 2
> (`upgrade-compile` **Step 3b**, or `upgrade-module` **step 4b** standalone). Phase 1 only
> normalizes the `release.*.api` line. The dep-tree resolution logic lives in
> `references/release-dxp-api-redundant.md`, consumed by Phase 2.

**Commit:** `<TICKET> Normalize release.dxp.api dependency (drop version)`

---

### Step 7 — Remove `sourceCompatibility` / `targetCompatibility` AND empty wrappers

Two passes:

1. **Delete the lines.** Any line containing `sourceCompatibility` or `targetCompatibility`.
2. **Delete the now-dead wrapper.** After pass 1, if a Java-compile wrapper —
   `compileJava { … }`, `tasks.withType(JavaCompile) { … }`, or a `java { … }` block — has a body
   with **no actual configuration left** (only whitespace and/or **comments**, e.g. an orphaned
   `// Generated classes using Jodd…` comment that was only explaining the removed
   `sourceCompatibility`), **remove the entire block**, including those now-orphaned comments. Use a
   slurp-mode pass (`perl -0777`) that matches each wrapper whose body is comments-and-whitespace
   only.

**Preserve** a wrapper only if it still contains **real configuration** — an actual statement, not
just a comment. A comment that only described the removed pin is dead once the pin is gone. Run this
**before** Step 7b — Step 7
strips the stale per-module pins; Step 7b sets one correct global value.

**Commit:** `<TICKET> Remove sourceCompatibility/targetCompatibility from modules`

---

### Step 7b — Root `build.gradle` `allprojects` block

Inject an `allprojects { … }` block into the **workspace-root** `build.gradle` so gradle commands
work across all subprojects — especially **themes**, which otherwise fail in Phase 2 for lack of
repositories / node config. Canonical shape: render
`assets/allprojects.gradle.template`.

Rules:
- **Java level** ← `upgrade.target.java.version` → renders `JavaVersion.VERSION_<n>`.
- **Node version** ← `node.version` (CLAUDE.md).
- **Repositories** — match the workspace's own `settings.gradle` convention: `mavenLocal()` + the
  Liferay **CDN** (`repository-cdn.liferay.com`). Do **not** add the non-CDN origin
  (`repository.liferay.com`) — it is a redundant fallback the workspace doesn't use; `mavenLocal()`
  lets locally-built artifacts (e.g. Service Builder APIs generated in Phase 2) resolve.
- **`java { }` / `node { }` guards — detect-and-choose:** a bare `java { … }` throws on a
  subproject without the `java` plugin, and a bare `node { … }` throws on a subproject without the
  node plugin (the `node` block comes from `com.liferay.gradle.plugins.node`). Guard **both**: if any
  subproject lacks the plugin, wrap the block so it applies only where the plugin is present —
  `plugins.withType(JavaPlugin) { java { … } }` for Java, and the equivalent for node (guard on the
  node plugin, e.g. `plugins.withType(...) { node { … } }` / `plugins.withId("...") { … }`). Only
  emit a literal bare block when **every** subproject applies that plugin. (Observed failure: an
  unguarded `node { }` is fragile on mixed workspaces.)
- **Idempotent — just add it (no confirmation):** write the block directly; skip any repos/blocks
  already present. (No confirm prompt even though the root `build.gradle` may be
  workspace-plugin-managed — the block is additive and idempotent.)
- **Coordination with Phase 2:** with node set globally here, `upgrade-compile`'s per-theme
  Node-version step becomes a fallback/override only.

**Skip if:** an equivalent `allprojects` block (java level + node + repos) is already present.

**Commit:** `<TICKET> Add root allprojects block (java <N>, node <node.version>, liferay repos)`

---

## Autonomy boundaries

- **Autonomous:** file reads; edits in Steps 1, 3, 4, 5, 6, 7, **7b** (the `allprojects` block —
  add it directly, idempotent); docker image repoints in Step 2; DXP license placement in Step 2c
  (additive, idempotent, content-checked) **including the autonomous removal of stale source-line
  activation keys (Step 2c.3)**.
- **Confirm first (always):** any mass-sweep edit touching more than 10 files; Step 2b when
  adding/replacing a JDBC driver jar.
- **Never:** write `liferay.workspace.product`, the workspace-plugin version, the gradle/java
  version, or a docker image tag that was **not resolved via blade** (Step 0b) **or** the BOM
  fallback — no "latest", no string-guessed tag; if it cannot be resolved, **stop and ask**. Never
  add `liferay.workspace.target.platform.version` (the product supersedes it — Step 1 removes it).
  Never add an SB/REST builder tool version pin to a `build.gradle` (use blade's plugin instead).
- **Never:** run `./gradlew formatSource` or `./gradlew upgradeJakarta`;
  write to or upgrade the database; push to remote or open PRs without explicit instruction; commit
  a partial state from an interrupted gradle task.

## Output

After all steps complete, report: (1) steps that ran and committed (with SHAs) — including any
**stale source-line activation keys removed** in Step 2c.3; (2) steps skipped (already applied or
N/A); (3) warnings (e.g. workspace plugin version not provided); (4) items flagged for human review
(Step 2b driver decisions; Step 2c — replace the dev activation key with the customer's production
license before go-live). Next suggested command:
`/upgrade-phase 2` (Fix Compile).
