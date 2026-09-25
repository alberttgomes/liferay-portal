# bnd 6.4 false-positive `Import-Package` entries that block OSGi resolution

## Where this applies

Liferay Workspace Plugin 15.x → 17.x all embed `biz.aQute.bndlib 6.4.0` + `com.liferay.ant.bnd 3.2.19` (the highest published version of the latter as of 2026.q1). The Jasper-aware analyzer inside `com.liferay.ant.bnd` hard-codes a small set of legacy `javax.*` strings and emits matching `Import-Package` entries even when the source, compile classpath, and runtime have already migrated to `jakarta.*`. The bundle then fails OSGi resolution at boot with:

```
Could not resolve module: <bundle> [<id>]
  Unresolved requirement: Import-Package: javax.servlet
```

There is no upstream fix available — the `com.liferay.ant.bnd` artifact is unchanged across the workspace plugin lineage and the `javax.*` strings live in `JspAnalyzerPlugin.class` bytecode.

## Detection

After Phase 4 boot, in Gogo Shell:

```
diag <bundle-id>
```

Returns a list of `Unresolved requirement: Import-Package: <pkg>` lines. The mandatory (non-`resolution:="optional"`) entries are the blockers.

Cross-check: the **source code and compile classpath of the failing module are clean of `<pkg>`**. If both confirm, you're in the false-positive case described here — not in a legitimate missing import.

## Known false-positives in 2026.q1

| Package | Origin (why bnd emits it) | When it surfaces | Fix |
|---|---|---|---|
| `javax.servlet`, `javax.servlet.http` | `com.liferay.ant.bnd 3.2.19` `JspAnalyzerPlugin` emits these for any module whose JSPs reference Liferay taglibs (`<liferay-theme:defineObjects />`, AUI, portlet, etc.) — hardcoded in plugin bytecode | Any module with JSPs under `META-INF/resources/` | `Import-Package: !javax.servlet*, *` in module `bnd.bnd` |
| `javax.xml.bind` | POI 4.x bytecode references JAXB internally. JAXB was removed from the JDK at Java 11 and moved to `jakarta.xml.bind` at Jakarta EE 9+ | Modules that `compileInclude` POI (typically for Excel export) | `Import-Package: !javax.xml.bind*, *` |
| `javax.mail`, `javax.mail.internet` | Mail moved to `jakarta.mail` at Jakarta EE 9+. Common transitive carrier: `nimbusds-oauth2-oidc-sdk` and similar OIDC/SAML libraries | Modules using nimbus or other older mail-using deps | `Import-Package: !javax.mail*, *` |
| `org.apache.tools.ant` | POI 4.x bytecode references Apache Ant build-tool classes for legacy XML utilities. Ant is a build-only artifact — not exported by the runtime framework | POI-using modules | `Import-Package: !org.apache.tools.ant*, *` |

## Fix recipe (per affected module)

Edit the module's `bnd.bnd`:

- If an `Import-Package:` directive already exists, append `!<pkg>*,\` before the closing `*`.
- If not, add a new directive:
    ```properties
    Import-Package: \
        !javax.servlet*,\
        *
    ```

The trailing `*` is **required** — it means "auto-detect everything else normally". Without it, only the excluded packages are listed and the bundle won't import anything else.

Multiple exclusions stack:

```properties
Import-Package: \
    !javax.servlet*,\
    !javax.xml.bind*,\
    !org.apache.tools.ant*,\
    *
```

Rebuild the module, redeploy. In Gogo Shell, `refresh` triggers a re-resolve without a full container restart (see the SKILL.md tip on OSGi state caching for when restart is needed instead).

## Why per-module instead of workspace-wide

A workspace-level `bundleDefaultInstructions` block in root `build.gradle` does the same thing in one place. Per-module exclusion is preferred for visibility — each affected `bnd.bnd` shows that the module is opting out of a specific bnd false-positive. Reviewers see the intent at the module where it applies; future module changes are less likely to silently inherit unexpected behavior.

## Avoid the over-broad escape hatch

`Import-Package: !javax.*, *` is tempting but wrong — it would silently strip legitimate `javax.crypto`, `javax.net.ssl`, `javax.sql`, `javax.xml.xpath`, etc. that the JDK still provides as `javax`. Stick to the specific package families that are confirmed false-positives.

## Verification

After the rebuild + redeploy:

1. Inspect the new JAR's manifest:
    ```bash
    unzip -p bundles/osgi/modules/<bundle>.jar META-INF/MANIFEST.MF | tr -d '\r' | grep -A 30 "^Import-Package:"
    ```
    The excluded packages should no longer appear.

2. In Gogo: `lb -s <bundle-symbolic-name>` should report `Active`.

3. `diag <bundle-id>` should print no `Unresolved requirement` lines, or only `resolution:="optional"` lines (which don't block resolution).
