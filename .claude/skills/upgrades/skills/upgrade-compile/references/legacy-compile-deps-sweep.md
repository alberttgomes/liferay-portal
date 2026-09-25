# Sweeping legacy 7.x-era compile deps that survive Phase 2

## The trap

After Phase 2 modernization, some modules end up with both `release.dxp.api` (the target fat JAR) AND explicit legacy pins on the compileClasspath. Common survivors:

- `com.liferay.portal:com.liferay.portal.kernel:4.4.0` / `5.4.0` — old portal kernel
- `jstl:jstl:1.2` — legacy JSTL with javax-based TLDs
- `com.liferay:com.liferay.<X>.taglib:<old-version>` — granular taglib pins
- `com.liferay:com.liferay.petra.<X>:3.0.0` — granular petra pieces
- `com.liferay:com.liferay.<X>.api:<old-version>` — old API JARs (journal.api 4.5.3, dynamic.data.mapping.api 5.0.0, application.list.api, etc.)
- `org.osgi:org.osgi.service.component.annotations:1.3.0` and other OSGi annotation pins

These compile clean. `release.dxp.api` covers the same surface and the duplicate symbols don't conflict. The build passes. **The bundle even deploys.** The trap is at OSGi resolution time: bnd analyzes the legacy artifacts on the compileClasspath (including their TLDs), sees that some of them still reference `javax.servlet.*` types, and contributes those references to the bundle's `Import-Package`. At boot, OSGi can't satisfy `javax.servlet` (only `jakarta.servlet` is exported) and the bundle stays in `Installed`.

Phase 2 should sweep these out aggressively even when compileJava + jar both pass.

## Detection

```bash
grep -rEn 'compileOnly group: ?"com\.liferay\.portal", name: ?"com\.liferay\.portal\.kernel"' modules/
grep -rEn 'compileOnly group: ?"jstl"' modules/
grep -rEn 'compileOnly group: ?"com\.liferay", name: ?"com\.liferay\..*\.taglib"' modules/
grep -rEn 'compileOnly group: ?"com\.liferay", name: ?"com\.liferay\.petra\.' modules/
grep -rEn 'compileOnly group: ?"org\.osgi", name: ?"org\.osgi\.service\.component\.annotations"' modules/
```

Any hit in a module whose `build.gradle` ALSO declares `compileOnly group: "com.liferay.portal", name: "release.dxp.api"` is a sweep candidate. Modules that have only legacy deps and NO `release.dxp.api` are also candidates — Phase 2 modernization missed them.

## Fix recipe per module

Edit the module's `build.gradle` so it has only:

- `compileOnly group: "com.liferay.portal", name: "release.dxp.api"` — the target fat JAR
- Any third-party `compileOnly` or `compileInclude` that `release.dxp.api` does NOT supply (commons-io, commons-csv, POI, okhttp, nimbusds, etc.)
- Project references to sibling modules (`compileOnly project(...)`)
- Build-tooling configs (`cssBuilder`, `buildService`, etc.)

Drop every other `com.liferay.*`, `com.liferay.portal.*`, `jstl`, and `org.osgi.service.component.annotations` line — `release.dxp.api` at the target version supplies all of them.

## Verification

After the sweep:

```bash
./gradlew :modules:<module>:clean :modules:<module>:build
```

CompileJava must pass. The resulting JAR is a candidate for OSGi resolution — the workspace's Phase 4 boot will surface whether the sweep was sufficient.

If compileJava fails, the missing API isn't in `release.dxp.api`. Investigate before re-adding the legacy pin: usually the symbol moved to a `*-api` module or was removed.

## Commit convention

One commit per module, per the granularity rule. Subject:

```
<TICKET> <module>: Replace legacy deps by release.dxp.api
```

No body required — the diff shows exactly which deps were dropped, and the "why" is universal across the sweep (legacy deps confuse bnd's manifest analyzer). The PR description carries the central cause once.

## Why this is a Phase 2 concern, not Phase 4

Phase 2 owns the deps modernization step. Discovering the trap during Phase 4 means Phase 2 missed it — the fix belongs in Phase 2's scope and should backflow into the agent's `/upgrade-compile` Step 1 (source-formatter automation) or wherever the deps consolidation lives.

Surfacing this as a separate reference makes the symptom recognizable when it appears mid-Phase-4: the agent shouldn't try to "fix" the bundle by adding more bnd.bnd exclusions when the right move is to clean up the module's classpath at its source.
