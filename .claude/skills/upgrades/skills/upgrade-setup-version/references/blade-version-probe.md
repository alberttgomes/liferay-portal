# Step 0b — blade probe mechanics

Read this **before executing Step 0b**. The decisions and hard rules stay in `SKILL.md`; this file is
the how.

## Probe

```bash
PROBE_DIR="$(mktemp -d -t lfrupg-probe-XXXX)"
trap 'rm -rf "$PROBE_DIR"' EXIT
# Run INSIDE an existing empty dir (init into "."). Do NOT pass a non-existent
# subdir path: blade 8.0.x then writes the gradle wrapper but throws
# NoSuchFileException on gradle.properties (partial scaffold — wrapper only).
( cd "$PROBE_DIR" && blade init -v "<resolved-product-key>" . )
```

**Verify the scaffold materialized — do not trust a partial one.** Confirm
`$PROBE_DIR/gradle.properties` **and** `$PROBE_DIR/settings.gradle` both exist. If only the `gradle/`
wrapper is present (the partial-scaffold failure), the probe did not really run — retry once, then
fall back.

## Finding the exact product key

`blade init --list` is the authoritative set of **targetable** keys, but it lists only the **latest
patch per release line** (e.g. `dxp-2026.q1.9-lts`, `dxp-2026.q2.3`) — **not every patch**.

1. Run `blade init --list`. Find the key matching `upgrade.target.version`, allowing for the
   `dxp-`/`portal-` prefix and `-lts` suffix (`2026.q1.7` → look for `dxp-2026.q1.7-lts`). Probe the
   matched key with `blade init -v <key>`.
2. If the exact version is **not targetable** (common: an older patch like `q1.7` when only
   `q1.9-lts` is published, so `blade init -v dxp-2026.q1.7-lts` errors), **do not silently
   substitute a different patch.** Surface the available keys for that line and **ask the user**
   which to use — normally the latest LTS of the line. Never change the quarter/patch number
   unilaterally.

## Read verbatim from the probe

These are authoritative; `CLAUDE.md` is only a cache of them.

| Value | Source in probe | Consumed by |
|---|---|---|
| `liferay.workspace.product` | `gradle.properties` | Step 1 — **copy exactly**, no prefix-strip/derivation |
| Liferay docker image tag | the probe's `docker-compose.yaml` / product info | Step 2 — **copy exactly** (resolves the `-lts` question) |
| `upgrade.workspace.plugin.version` | `settings.gradle` (`com.liferay.workspace` / `…plugins.workspace`) | Step 3 |
| `upgrade.gradle.version` | `gradle/wrapper/gradle-wrapper.properties` (`gradle-<X.Y.Z>-…zip`) | Step 4 |
| `upgrade.target.java.version` | `gradle.properties`/`build.gradle` toolchain | Step 2 (`JAVA_VERSION`), Step 7b |

The release-matched workspace plugin lives in the probe's `settings.gradle` — e.g. `dxp-2026.q1.9-lts`
→ `com.liferay.gradle.plugins.workspace` **`17.1.0`**, *not* the maven-metadata "latest" `17.1.2`.

## Why blade's plugin, and the one exception

Blade's workspace-plugin version keeps **REST Builder, bnd, the Jakarta tooling, and the build
itself** release-matched in Phase 2.

**The Service Builder tool is the exception — blade's plugin does not reliably keep it
release-matched.** A plugin whose bundled SB tool diverges from the target generates a
model/persistence contract incompatible with `release.dxp.api`. Observed at 2026.q1.9: blade's
`17.1.0` SB tool emitted `FinderColumn`-style symbols absent from `release.dxp.api` and failed
`compileJava`.

So treat blade's plugin as the **starting hypothesis** for the SB tool. If Phase 2's **clean** Service
Builder regen produces code that won't compile against `release.dxp.api`, the authoritative SB tool
version is the one the target portal source pins (`liferay-portal-ee` →
`modules/sdk/gradle-plugins-service-builder/build.gradle`) — often carried by an **older** plugin than
blade's (2026.q1.9: tool `1.0.513` via plugin `15.1.3`). Set the workspace plugin to that version
**regen-only**, then restore blade's plugin (`upgrade-compile` Step 1). **Never** add a
`build.gradle` tool force.
