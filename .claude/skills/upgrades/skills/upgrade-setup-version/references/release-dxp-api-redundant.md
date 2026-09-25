# `release.dxp.api`-redundant dependency pruning

## What `release.dxp.api` actually is

In Liferay DXP 2025.Q3+ / 2026.Qx it is a **fat JAR** (~70 MB, ~36k entries) bundling compiled API
classes from across the platform. Its Maven POM has no transitive deps — everything reaches the
classpath via the embedded classes, which is why the dep tree shows it as a leaf. To audit contents:

```bash
unzip -l ~/.gradle/caches/modules-2/files-2.1/com.liferay.portal/release.dxp.api/<version>/*/release.dxp.api-*.jar | grep "<package>/"
```

Typically **provided** (safe to drop explicit module deps for these): `com.liferay.portal.kernel.*`,
`com.liferay.dynamic.data.mapping.*`, `com.liferay.portal.upgrade.*`,
`org.osgi.service.component.annotations.*`, `jakarta.servlet.*`, `jakarta.ws.rs.*`, `jakarta.mail.*`,
`jakarta.xml.bind.*`, `jakarta.persistence.*`. Typically **NOT provided**: `jakarta.xml.ws.*`,
`jakarta.jws.*`, `javax.xml.rpc.*`, `org.apache.axis.*`, `javax.mail:mail`.

Reference for **Step 8b** of `upgrade-setup-version`. Documents the line-level patterns the skill recognises in `build.gradle`, the **primary** resolution algorithm based on `blade gw :<module>:dependencies`, and the **fallback** POM-walk algorithm used when Gradle resolution fails.

The list of *which* GAs are redundant is not in this file — that list is the runtime cache produced by either resolution path:

- Primary: `.claude/cache/upgrade/gw-deps-<sanitized-target>/<gradle-project>.txt` (raw Gradle dep-tree per module, keyed by build.gradle hash).
- Fallback: `.claude/cache/upgrade/release-dxp-api-<sanitized-target>-provided.json` (artifact-level GA set built from the BOM POM).

Changing target versions does not require editing this reference.

---

## Recognised line patterns

The skill operates only on the **top-level** `dependencies { … }` block of a `build.gradle`. Nested closures (e.g. `subprojects { dependencies { … } }`) are not touched.

Inside that block, these forms are matched and considered for pruning:

| Form | Example | Notes |
|---|---|---|
| Single-line map (compileOnly) | `compileOnly group: "com.liferay.portal", name: "release.dxp.api"` | Most common. Order of `group:` / `name:` is normalised before matching. |
| Single-line map (other configs) | `implementation group: "X", name: "Y"`, `provided group: "X", name: "Y"`, `runtimeOnly group: "X", name: "Y"` | All scopes treated equivalently — pruning a redundant declaration is safe regardless of scope. |
| Single-line map with version | `compileOnly group: "X", name: "Y", version: "1.2"` | Removed when `(X, Y)` is in `PROVIDED_GA`. The explicit version is meaningless once `release.dxp.api` is in scope. |
| Multi-line map | ```gradle\ncompileOnly(\n  group: "X",\n  name: "Y",\n  version: "1.2"\n)\n``` | Recognised by line-by-line state machine: opening token to matching `)`. Removed as a contiguous block. |
| Quoted-coordinate string | `compileOnly "X:Y:1.2"` or `compileOnly 'X:Y'` | Split on `:`, mapped to `(group, name)`, then matched. |
| Duplicate of an earlier line | Same `(group, name)` declared more than once in the block. | First occurrence kept, later duplicates removed regardless of `PROVIDED_GA`. |

Anything that does not parse cleanly (e.g. interpolated `${var}` group/name, custom function calls) is **left alone** — the skill never edits a line it cannot tokenise unambiguously.

## Pruning rule

For each `build.gradle` under `modules.root`:

1. Skip the file if no line in the top-level `dependencies { … }` block declares `(com.liferay.portal, release.dxp.api)`.
2. For every other declaration in the block, compute its `(group, name)` and test (using the primary path's three sets):
   - GA ∈ `RESOLVED_BY_RELEASE_DXP_API` → queue for removal, justification `provided transitively by release.dxp.api`.
   - GA ∈ `OVERRIDDEN_FROM_RELEASE_DXP_API` → queue for removal, justification `version overridden by release.dxp.api`.
   - GA ∈ `CONSTRAINT_ONLY_ROOTS` ∧ in either of the above → queue for removal, justification `constraint-only and provided by release.dxp.api`.
   - GA ∈ `CONSTRAINT_ONLY_ROOTS` only → leave (defer to step 4b — could be an intentional version pin).
   - Duplicate of an earlier line in the same block (regardless of provided-set membership) → queue for removal, justification `duplicate of <file>:<earlier-line>`.
3. Otherwise → leave it.

When the **fallback path** is in effect (Gradle resolution failed for the module), only the artifact-level `PROVIDED_GA` test applies; override and constraint-only signals are unavailable.

## What this step does NOT do

- Does not prune declarations whose redundancy is *package-level* (Import-Package) rather than artifact-level. The canonical example is `org.osgi:org.osgi.annotation.versioning` — `release.dxp.api` exports the package but does not declare a Maven dependency on the artifact. These are handled by `upgrade-module`'s compile-verified prune sub-step.
- Does not touch `compileInclude`, `serviceBuilder`, `restBuilder`, or any non-classpath configuration.
- Does not edit nested `subprojects { … }` or `allprojects { … }` blocks.
- Does not rewrite the file's formatting beyond removing the targeted lines (and trailing blank-line collapsing if removal leaves consecutive blank lines).

## Resolution algorithm — primary path (`blade gw dependencies`)

Per qualifying module. Cached at `.claude/cache/upgrade/gw-deps-<sanitized-target>/<sanitized-gradle-project>.txt`, keyed by `(upgrade.target.version, sha256(build.gradle))`. Re-resolved only when the build.gradle changes or the target version changes — other modules' caches are not touched.

1. Derive the Gradle project path from the file location (`modules/foo/foo-service` → `:modules:foo:foo-service`).
2. Run, capturing stdout to the cache file:
   ```bash
   blade gw :<gradle-project>:dependencies --configuration compileClasspath --no-daemon
   ```
3. Parse the captured tree. The parser must recognise:
   - The `compileClasspath - …` header.
   - Tree edges: `+--- ` and `\--- `, plus indentation continuations `|    ` and `     `.
   - Annotations at end-of-line:
     - `*` (already shown elsewhere — deduped reference).
     - `(c)` — constraint-only (declared as a version constraint, not contributing classes).
     - `(n)` — declared but not resolved.
     - `-> X.Y.Z` — declared version was overridden to X.Y.Z by Gradle's resolution.
     - `(*)` — short-form already-shown emitted by some Gradle versions.
   - Each parsed line yields `(group, name, declared-version, resolved-version, parent-line, annotations)`.
4. From the parsed tree build three sets:
   - **`RESOLVED_BY_RELEASE_DXP_API`** — every GA reachable transitively under the `com.liferay.portal:release.dxp.api` subtree, excluding `(c)` constraint-only nodes inside it.
   - **`OVERRIDDEN_FROM_RELEASE_DXP_API`** — every root-level GA whose resolved version came from inside the `release.dxp.api` subtree (the `-> X.Y.Z` arrow points to a version that matches a node under `release.dxp.api`).
   - **`CONSTRAINT_ONLY_ROOTS`** — every root-level GA annotated `(c)`.

The primary path is per-module — different modules may resolve `release.dxp.api`'s contributions differently due to constraints from other declared deps. That is exactly the variance the POM-walk path cannot capture.

## Resolution algorithm — fallback path (POM walk)

Used **only** when the primary path's `gw dependencies` command exits non-zero (broken project, network failure, missing repo, etc.). One-shot per `upgrade.target.version`. Cached at `.claude/cache/upgrade/release-dxp-api-<sanitized-target>-provided.json`. The skill must log the fallback prominently in the Step 8b artifact so reviewers know the evidence channel was POM-only.

1. Resolve the artifact version of `release.dxp.api` for the target:
   1. Run `./gradlew -q properties --no-configuration-cache` against the Step 0 blade probe workspace (or, if Step 0 was skipped, against the current workspace). Read `liferay.workspace.bom.version` or the equivalent property exposed by the workspace plugin.
   2. Fallback: `./gradlew :<probe-module>:dependencyInsight --dependency com.liferay.portal:release.dxp.api` and parse the first resolved-version line.
2. Fetch the POM:
   ```
   https://repository-cdn.liferay.com/nexus/content/groups/public/com/liferay/portal/release.dxp.api/<resolved-version>/release.dxp.api-<resolved-version>.pom
   ```
   Parse `<dependencies>` and recursively follow imported `<dependencyManagement>` BOMs. Collect every `(groupId, artifactId)` pair into `PROVIDED_GA`. Versions are intentionally discarded — the prune is an artifact-presence test, not a version test.
3. Last-resort fallback (POM unavailable / 404 / network error): fetch `release.dxp.api-<resolved-version>.jar`, read `META-INF/MANIFEST.MF`. Parse `Bundle-ClassPath` and `Embedded-Artifacts` headers; map embedded artifacts to GA pairs.
4. Persist the result as JSON:
   ```json
   {
     "target": "dxp-2026.q1.5-lts",
     "resolved_version": "<X.Y.Z>",
     "source": "pom" | "jar-manifest",
     "provided_ga": [
       ["com.liferay.portal", "com.liferay.portal.kernel"],
       ["org.osgi", "org.osgi.core"],
       ["..."]
     ]
   }
   ```

Cache is invalidated **only** when `upgrade.target.version` changes. The fallback path makes at most one network round-trip per target version.

The fallback's `PROVIDED_GA` plays the role of the primary path's `RESOLVED_BY_RELEASE_DXP_API` only — overrides and constraint-only signals are unavailable, and the pruning rule degrades accordingly.

## Idempotency

After Step 8b applies and the workspace is committed, re-running the step must leave every `build.gradle` unchanged: every redundant line is gone, no new ones can appear from the same input, and the cache pins the rule set.
