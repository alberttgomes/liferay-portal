# Theme + workflow XML metadata bump (7.x → target)

## When this applies

After Phase 2 deps modernization, theme deployment may still fail at the portal with:

```
mpsp-<X>-theme.war does not support this version of Liferay
```

and any workflow-definition import may reject the file with a schema-version mismatch. Both are metadata-only issues: text files that declare which Liferay major.minor they target.

The Liferay source-formatter ships `Upgrade`-category checks that handle these (`PropertiesUpgradeLiferayPluginPackageLiferayVersionsCheck`, `XMLUpgradeDTDVersionCheck`, `JSONUpgradeLiferayThemePackageJSONCheck`, `PropertiesUpgradeLiferayPluginPackageFileCheck`). When the source-formatter integration works in the workspace, prefer that. When it has friction (target-version format mismatch, repos block missing in root build.gradle, etc.), the manual checklist below replicates what the checks would do.

## Per-theme checklist

For each theme directory:

### `src/WEB-INF/liferay-plugin-package.properties`

```diff
-liferay-versions=7.3.0+
+liferay-versions=7.4.0+
```

The supported floor matches the major.minor of the target portal version (e.g., `7.4.0+` for any 2026.q1.x build, which is 7.4 codebase).

### `src/WEB-INF/liferay-look-and-feel.xml`

Two changes in the same file. The DOCTYPE (top of file) AND the compatibility block:

```diff
-<!DOCTYPE look-and-feel PUBLIC "-//Liferay//DTD Look and Feel 7.3.0//EN" "http://www.liferay.com/dtd/liferay-look-and-feel_7_3_0.dtd">
+<!DOCTYPE look-and-feel PUBLIC "-//Liferay//DTD Look and Feel 7.4.0//EN" "http://www.liferay.com/dtd/liferay-look-and-feel_7_4_0.dtd">

 <look-and-feel>
     <compatibility>
-        <version>7.3.0+</version>
+        <version>7.4.0+</version>
     </compatibility>
```

### `src/WEB-INF/liferay-hook.xml` (if present)

```diff
-<!DOCTYPE hook PUBLIC "-//Liferay//DTD Hook 7.3.0//EN" "http://www.liferay.com/dtd/liferay-hook_7_3_0.dtd">
+<!DOCTYPE hook PUBLIC "-//Liferay//DTD Hook 7.4.0//EN" "http://www.liferay.com/dtd/liferay-hook_7_4_0.dtd">
```

Not all themes ship a hook — only the ones that override portal behavior. If the file doesn't exist, skip.

### `package.json` (when migrating from node-sass to dart-sass)

See `themes-dart-sass.md` in this same references directory. The `liferay-theme-tasks` and `liferay-frontend-theme-styled/-unstyled` version bumps go in `package.json` and are independent of the metadata bump above.

## Workflow definition XML

Workflow definitions ship as XML and declare the schema URI inline (not via DOCTYPE). The same major.minor floor applies.

For each workflow-definition file the workspace ships (typically under `<module>/workflow-definitions/` or `<module>/src/main/resources/workflow-sources/`):

```diff
 <workflow-definition
-    xmlns="urn:liferay.com:liferay-workflow_7.3.0"
+    xmlns="urn:liferay.com:liferay-workflow_7.4.0"
     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
-    xsi:schemaLocation="urn:liferay.com:liferay-workflow_7.3.0 http://www.liferay.com/dtd/liferay-workflow-definition_7_3_0.xsd"
+    xsi:schemaLocation="urn:liferay.com:liferay-workflow_7.4.0 http://www.liferay.com/dtd/liferay-workflow-definition_7_4_0.xsd"
 >
```

Both the namespace URN and the schemaLocation pair must be updated.

## Detection sweep

```bash
grep -rln "7\.3\.0\|7_3_0" themes/ modules/ --include="*.xml" --include="*.properties" | grep -v "/build/"
```

Run after Phase 2 compile remediation and before Phase 4 startup. The build outputs under `/build/` mirror the src files and don't need separate edits.

## Commit convention

Per the granularity rule in the `commit` skill: one commit per theme (or workflow-bearing module) covering ALL the file changes for that theme's metadata bump. The context is "bring this theme to target 7.4 metadata" — the per-file split would fragment review for no gain.

Subject:

```
<TICKET> <theme>: Bump theme metadata from 7.3 to 7.4
<TICKET> <module>: Bump workflow definition schema from 7.3.0 to 7.4.0
```

For container themes with sub-themes (e.g., `mpsp-booklets-themes` holding two sub-themes), the container is the prefix; mention "in both sub-themes" in the subject if it adds clarity.

No `#automation` label needed — source-formatter `Upgrade` category covers these patterns; the manual application is a workaround for integration friction, not a catalog gap.

## Why not use `formatSource -Pcategory=Upgrade` directly

`./gradlew :themes:<theme>:formatSource -Psource.check.category.names=Upgrade -Pupgrade.to.liferay.version=7.4` SHOULD work but has known friction at 2026.q1:

- `XMLUpgradeDTDVersionCheck.getLPVersion()` reads `upgrade.to.liferay.version`, splits on `.` and uses `[0].[1].0`. The format must be `7.4` or `7.4.x` — purely-quarterly formats like `2026.q1.7` are not accepted.
- The workspace root `build.gradle` must have a `repositories { maven { url "https://repository-cdn.liferay.com/nexus/content/groups/public" } }` block (or `allprojects` equivalent) so the source-formatter Gradle configuration can resolve `com.liferay.source.formatter`. Workspaces upgraded from older versions sometimes lost this.
- `JSONUpgradeLiferayThemePackageJSONCheck` hardcodes `liferayTheme.version=7.4` — the target is fixed even when invoked against a different `upgrade.to.liferay.version`.

If `formatSource` succeeds in your workspace, prefer that path. Otherwise replicate the patterns above manually.
