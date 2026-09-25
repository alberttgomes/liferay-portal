# Reusing a client-provided docker environment

Step 4 prefers reusing whatever runnable environment the client shipped over scaffolding a new
one. A client should ship a runnable docker-compose; if they didn't, ask and then scaffold one
(reproducing the team's base-workspace structure) and report the addition. Either way the
runtime is a **reported artifact**.

## The canonical structure

A well-formed delivery mirrors the team's base workspace — the **image-based** shape the
scaffold also produces:

```
<workspace.root>/
├── docker-compose.yml                       # official liferay/dxp image + granular bind mounts
├── docker-compose/
│   ├── database/dump/                        # the DB dump (auto-restored on first init)
│   └── liferay/mnt/liferay/files/            # → /mnt/liferay/files (portal-ext, osgi/configs, deploy key)
│       ├── osgi/configs/…ElasticsearchConfiguration.config
│       └── deploy/…activation-key…xml
├── bundles/                                  # runtime, git-ignored — the BUILD OUTPUT is mounted in
│   ├── osgi/modules, osgi/war, deploy
│   └── data/document_library/
├── configs/                                  # per-env portal configs (baked at build time)
├── modules/  themes/                         # source
└── gradle.properties, settings.gradle
```

The Liferay service runs the stock `liferay/dxp:<tag>` image; customization is layered **only**
through the `./bundles/*` and `/mnt/liferay/files` bind mounts. There is no bundle tar and no
`Dockerfile` in this shape.

## Detect

Search, in order, for a delivered environment:

1. The workspace root and `docker.compose.file` from CLAUDE.md.
2. **Sibling delivery folders** next to the workspace — the dump and docker env often arrive in
   a separate folder (e.g. `<delivery>/<customer>-docker/`).
3. Inside any such folder, look for: `docker-compose.yml` / `compose.yml`, a
   `docker-compose/database/dump/` (or any `dump/` mounted at `/docker-entrypoint-initdb.d`),
   a `docker-compose/liferay/mnt/liferay/files/` config tree, a `document_library/`, and —
   only in the **bundle-fallback** shape — a `bundle/` (`tomcat-*`, `osgi/`, `portal-ext.properties`)
   plus a `Dockerfile`.

## What to copy vs reference

| Piece | Size | Action | Why |
|---|---|---|---|
| `docker-compose.yml` | small | **copy** into workspace root | ships to client; the entrypoint |
| `docker-compose/` config tree (`database/dump/`, `liferay/mnt/liferay/files/`) | small (dump aside) | **copy** | ES/mail/portal config + auto-restore mount |
| the DB dump | large file | **copy/stage** into `docker-compose/database/dump/` | drives auto-restore (Check 2) |
| `document_library/` | multi-GB | **bind-mount** from the original path | read-only data; copying duplicates many GB |
| custom module jars / theme war | — | **build output**, not copied | Step 3's build lands them in `bundles/osgi/modules` · `bundles/deploy` · `bundles/osgi/war` |
| `Dockerfile` + `bundle/` (fallback shape only) | small / multi-GB | **copy** | only when an official image can't be pulled for the exact patch level |

Because the Liferay service is an official **image**, there is no multi-GB bundle to duplicate —
only the document library is large, and it is bind-mounted. When bind-mounting the DL, point the
compose volume at the original absolute path rather than a relative `./bundles/data/document_library`.
Record the deviation in the report.

For a **machine-specific** DL path, keep the committed compose portable and point the mount at the
store with a git-ignored `docker-compose.override.yml` (a `liferay` `volumes:` entry for
`/opt/liferay/data/document_library`) instead of editing `docker-compose.yml`.

**Search topology — reproduce what the client shipped.** Some 7.x dev envs run **embedded**
Elasticsearch (no ES service, no `ElasticsearchConfiguration.config`); reproduce embedded and flag it
as an upgrade-target concern rather than forcing a container (see `runtime-findings.md` §5).

**Git hygiene.** Render `assets/gitignore.template` as the workspace root `.gitignore` so the dump,
document library, activation key, and any `docker-compose.override.yml` stay out of git (keep the
folders via `.gitkeep` + the committed `01-restore.sh`). Keep the repo **private** —
`portal-ext.properties` may carry app encryption seeds.

> **Confirm first** before copying multi-GB assets — check available disk.

## The prebuilt-jar replacement rule (critical)

The whole point of `pre-upgrade-check` is to validate the **source build**. In the image model
this is naturally clean: the container starts stock and mounts **only** what the source build
produced into `bundles/`. So there is no client bundle `deploy/` to surgically clean —

- Step 3's build writes the custom jars to `bundles/osgi/modules` (and `bundles/deploy`) and the
  gulp-built theme war to `bundles/osgi/war`; those are what the container mounts.
- The only cleanup needed is removing any **client-shipped prebuilt custom jars** that already
  sit inside `bundles/` from the delivery, so the freshly built ones aren't shadowed.

Identify the custom artifacts (vs Liferay platform jars):

```bash
# Custom modules usually share the workspace group prefix or a module's Bundle-SymbolicName.
# 1) collect the workspace symbolic names:
grep -rhoP 'Bundle-SymbolicName:\s*\K\S+' <modules.root> --include=bnd.bnd | sort -u
# 2) match those against jars already staged in the bundles mounts:
find bundles/osgi/modules bundles/deploy -name '*.jar'
# 3) the theme war:
find bundles/osgi/war -name '*-theme.war'
```

Remove any matched **prebuilt** custom jars/war shipped in the delivery, then stage this run's
built jars into `bundles/osgi/modules` / `bundles/deploy` and the gulp-built theme war into
`bundles/osgi/war`. **Never touch the stock image's platform jars.** (In the bundle-fallback
shape the same rule applies inside the bundle's `osgi/modules` · `deploy` · `osgi/war`, leaving
`osgi/core`, `osgi/marketplace`, `osgi/portal`, `osgi/static`, … untouched.)

## Auto-restore note

A `postgres`/`mysql` image runs any `*.sh` / `*.sql` mounted at
`/docker-entrypoint-initdb.d/` **only on first init** (empty data volume). With the dump under
`docker-compose/database/dump/`, Check 2 happens automatically on the first
`docker compose up -d database`. To force a clean re-restore, reset with
`docker compose down -v && docker compose up -d database` (drops the volume). Monitor the DB log
for the restore-complete line; do not blind-sleep.

## Reporting

Add report rows for: the env incorporated (from where), any deviation (DL bind-mount instead of
copy), and the prebuilt artifacts displaced by the source build. If you had to scaffold instead
of reuse, that addition is itself a row + change-log (the client receives a new compose that
reproduces the base-workspace structure).

## Docker bind-mount pitfalls

Three ways a mount silently breaks the portal. All three were hit on a single delivery.

### A missing bind-mount source becomes a directory

Docker **creates the source path as an empty directory** when it does not exist. So a per-file mount of
an artifact that is git-ignored (or wiped by a build script) turns into a directory inside the
container, and the portal dies at startup:

```
java.lang.RuntimeException: java.io.FileNotFoundException:
  /opt/liferay/osgi/marketplace/Liferay Push - API.lpkg (Is a directory)
        at com.liferay.portal.spring.context.PortalContextLoaderListener.contextInitialized(...)
```

Either guarantee the file exists (commit it, if small) or mount its **directory** instead — an empty
directory is usually harmless where a directory-shaped "file" is fatal. Never mount a per-file path out
of a tree that a deploy script recreates.

### A directory mount replaces the target

Mounting a directory hides everything the image had at that path. Before mounting over any populated
image directory, prove your copy is a **superset**, comparing name *and* size:

```bash
find <delivery>/osgi/marketplace -maxdepth 1 -name '*.lpkg' -printf '%s  %f\n' | sort -k2 > /tmp/c
docker run --rm --entrypoint sh <image> -c \
  'find /opt/liferay/osgi/marketplace -maxdepth 1 -name "*.lpkg" -printf "%s  %f\n"' | sort -k2 > /tmp/i
diff /tmp/c /tmp/i
```

On one DXP 7.2 delivery: 224 delivered vs 222 in the image, only-in-image **0**, same-name-different-size
**0** — the delivery was a clean superset (it added `Liferay Push - API/Impl`), so mirroring the whole
folder was safe and reproduced the client's runtime exactly. Had the image held anything the delivery
lacked, a directory mount would have deleted an application from the portal.

Also mirror `osgi/marketplace/override/` when the client uses it: a jar dropped there replaces a
same-named jar *inside* an LPKG, so omitting it silently swaps client behaviour for stock behaviour.

**If the mirrored folder is too large to commit, guard it.** A directory mount plus an unpopulated
folder equals a portal that cannot start, so have the deploy script assert the expected file count and
document the copy step in `UPGRADE-README.md` next to the dump and document library.

### Rootless Docker maps container root to the host user

Check with `docker info --format '{{.SecurityOptions}}' | grep -o rootless`. Under rootless Docker, do
**not** pass `-u $(id -u):$(id -g)` to a build container: host-owned bind mounts appear as `root:root`
inside, the unprivileged uid cannot write them, and Gradle fails with
`Could not create parent directory for lock file`. Running as the container's default root maps back to
the host user and files land correctly owned.

## Project isolation: always name the compose project

Declare a top-level project name in the compose file:

```yaml
name: <customer>
```

Without it the project name defaults to the directory name. Teams running several customer
environments side by side have had `docker compose down` in one remove containers belonging to another.
Declaring it in the file means it cannot be forgotten, unlike `--project-name` on the command line
(which still works and overrides the file). The concrete danger is **two deliveries whose folders share a basename**. Client environments
routinely unzip to a generic directory (`liferay-docker`, `docker`, `bundle`), so two customers can
silently share one default project name — and then `docker compose down` in either removes the
other's containers. Distinct-looking paths are no protection; only the basename matters.

**Two distinct failure modes, two distinct fixes.** Do not conflate them:

| Symptom | Cause | Fix |
|---|---|---|
| `down` in one environment removes another's containers | both resolved to the **same project name** (shared directory basename) | explicit `name:` per compose file |
| `up` fails with "container name already in use" | both declare the **same `container_name`** — those are global, not project-scoped | prefix container names per customer |

Client-provided composes commonly use generic names (`liferay-portal`, `liferay-postgres`,
`liferay-mailhog`), so the second mode bites as soon as two deliveries run on one machine. Rename them
to `<customer>-*` in the reproduction compose and report the change.

A `down` can never cross into a project whose name is unique, so naming this environment protects
it, but not the neighbours. Check every environment on the machine:

```bash
docker compose config | head -1      # expect an explicit `name:`
docker ps -a --format '{{.Names}}\t{{.Label "com.docker.compose.project"}}'
```

Explicit `container_name:` entries do not help — they keep
names unique but carry no project label. Verify with:

```bash
docker compose ps --format '{{.Project}}'
docker ps --filter label=com.docker.compose.project=<customer>
```

## Where the JVM options come from

Know this before touching memory, because the two shapes differ:

| Runtime shape | Source of `CATALINA_OPTS` |
|---|---|
| client's delivered bundle (bare JDK image + mounted bundle) | `bundle/tomcat-*/bin/setenv.sh` |
| official `liferay/dxp:<tag>` image | `/opt/liferay/tomcat/bin/setenv.sh`, which then appends `${LIFERAY_JVM_OPTS}` |

On a DXP 7.2 delivery these were **byte-identical**
(`-Xms2560m -Xmx2560m -XX:NewSize=1536m -XX:MaxNewSize=1536m -XX:MetaspaceSize=768m -XX:MaxMetaspaceSize=768m -XX:SurvivorRatio=7`),
so leaving memory unset was the *faithful* reproduction and any override was a deviation to report.

Two things to state correctly when asked: an absent `-Xmx` in the compose file does **not** mean the
portal will size itself to the host — the image hard-codes one, so the heap is fixed regardless of host
RAM. And because the image appends `${LIFERAY_JVM_OPTS}` *after* its own defaults, the last `-Xmx` wins,
which is what makes the env var an effective override. Confirm the effective value rather than the flag:

```bash
docker compose exec -T liferay sh -c \
  'jcmd $(pgrep -f catalina.startup.Bootstrap) VM.flags' | tr ' ' '\n' | grep -E 'MaxHeapSize|NewSize'
```
