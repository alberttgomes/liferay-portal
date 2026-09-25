---

name: upgrade-compile
description: Run this skill when Phase 2 (Fix Compile) of the upgrade playbook executes, or when the user invokes `/upgrade-compile` directly. Walks the Game Plan order from upgrade-analyzer (dependency levels 1..N) and, per module, regenerates Service Builder and REST Builder outputs, migrates javax.* to jakarta.*, then iterates build → remediate → commit until clean, delegating API remediation to deprecated-api-remediation. Runs JSP compilation with a structured reconciliation workflow for hook and fragment-host modules. Trigger also when the user says "fix compile errors", "run the compile phase", or "make the workspace compile".

---

# upgrade-compile (Phase 2 — Fix Compile)

Makes the whole workspace compile against the target version. Processes modules in the **Game Plan**
order produced by `/upgrade-analyzer` — **dependency levels 1..N**, lowest level first — so a
module's dependencies are always clean (and their regenerated APIs available) before it compiles.

## Prerequisites

- Phase 1 (`upgrade-setup-version`) is `complete` in `upgrade-state.md`.
- The Game Plan order is recorded in `upgrade-state.md` (from `/upgrade-analyzer`).
- Working directory is the workspace root.
- Reference data is fetched: `index_simple.md` / `index_complex.md` exist under
  `.claude/cache/reference/<source-id>/` (run `/upgrade-refresh-references` if not).

## Gotchas

- **No `formatSource` here.** API substitutions come from the catalog via
  `deprecated-api-remediation`, not from a source-formatter pass.
- **`bnd.bnd` is not touched here.** Bundle-version / Import-Package constraints do not affect
  compilation; they surface as OSGi resolution problems and belong to **Phase 3**.
- **Service Builder output is checked-in `src/main/java`, not `build/`** — so `gradle clean` does not
  remove it, and `buildService`, seeing no structural change, **silently skips** regenerating and keeps
  the stale output. Any later correction is then a no-op. Delete by the `@generated` marker first
  (Step 1). Observed on `dxp-2026.q1.9-lts`: a stale `FooWrapper` missing
  `cloneWithOriginalValues()` failed `compileJava` while `buildService` reported `BUILD SUCCESSFUL`.
- **Known build false-positives — do not "fix" these.** Fragment-Host modules error on `*.internal.*`
  packages, which the host exports at runtime but are absent from the compile classpath (`bnd.bnd` has
  `Fragment-Host:`) · bnd 6.4 nested-class import misparse → `references/bnd-nested-class-import.md` ·
  POI / commons-io MRJAR `fixupmessages` → `references/bnd-poi-mrjar.md`.
- **Never add a per-theme `settings.gradle`** — it turns the theme into a separate Gradle root,
  detaching it from the workspace and forcing standalone version pins. Same for a per-theme
  `repositories` block, which the Phase 1 `allprojects` repos already provide.
- **Package-level redundancies are invisible to the dependency tree.** `org.osgi:org.osgi.core`,
  `org.osgi.service.component.annotations` and `org.apache.felix.http.servlet-api` are exported by the
  `release.dxp.api` fat JAR but absent from the resolved artifact tree — only a compile probe finds them.

## Reading CLAUDE.md fields

| Field | Used for | Fallback |
|---|---|---|
| `modules.root` | Module discovery root | Default `modules/` |
| `themes.root` | Theme discovery root | Default `themes/` |
| `upgrade.source.version` | JSP reconciliation commit messages | Required |
| `upgrade.target.version` | JSP reconciliation + Jakarta audit | Required |

## Commit convention

Commits follow the `/commit` skill and `.claude/rules/commit.md`: when no ticket applies, prefix the
title with `NOISSUE`. One commit per **(module × pattern)**.

## Gradle wrapper detection

At phase start: if `blade` is in PATH (`type blade 2>/dev/null`) use `blade gw`, else `./gradlew`.
Referred to as `<gw>`.

## Step 0 — Setup

1. Read the Game Plan module order from `upgrade-state.md`.
2. Write artifacts under the phase directories `upgrade-phase` established (Workflow step 4).
3. **Jakarta availability audit (once).** Audit the `release.dxp.api` fat JAR to learn which
   `jakarta.*` prefixes are provided, so per-module migration only renames prefixes that resolve:
   ```bash
   jar=$(find ~/.gradle/caches -path "*release.dxp.api/*" -name "*.jar" | head -1)
   for pkg in jakarta/servlet jakarta/portlet jakarta/ws/rs jakarta/mail jakarta/jws \
              jakarta/xml/bind jakarta/xml/ws jakarta/persistence; do
     echo "$pkg: $(unzip -l "$jar" | grep -c "$pkg/")"
   done
   ```
   Record the provided set. Prefixes NOT in the JAR (`jakarta.xml.ws`, `jakarta.jws`, …) are either
   left as `javax.*` or get a jakarta-named dep added — decide per occurrence in Step 3.

## Module processing order

1. All **non-theme** modules from the Game Plan — Level 1 first, then Level 2, … (the "1 to N
   levels" loop).
2. All **theme** modules after every regular module is clean.

---

## Regular module steps

Each work unit is one **module** (full exact name, including sub-modules) — **except a generated-source
group, which is a single work unit spanning all its sub-modules** (see next paragraph). Wrap each work
unit as its own branch and pull request (compile is the one phase where the
agent **creates** the TTs — idempotent create → In-Progress → commits keyed to the TT → PR → Peer
Review + set its "Git Pull Request" field; the auto-created TT is left open as a guard).

**Generated-source grouping — one branch / PR per generator.** A Service Builder project (`service.xml`
→ `-api` + `-service`) or a REST Builder project (`rest-openapi.yaml` / `rest-config.yaml` → `-api` +
`-client` + `-impl`) is **one work unit**: a single branch and one PR covering every
sub-module in the group. `buildService` / `buildREST` regenerates the **whole group from one descriptor**,
so splitting the sub-modules across branches/PRs would fragment a single generated artifact (and the
`-api` half won't compile without the regen committed alongside it). Process the group when its
**earliest** Game-Plan level comes up, run the regen once (Steps 1–2), then build → remediate → commit
each sub-module on that one branch. Standalone modules (no shared generator) each stay their own work
unit. Name the branch after the generator project — e.g. `phase2-sample-rest-builder`. (The analyzer
flags these groups in the Game Plan; if it didn't, detect them here: a directory whose subtree contains
a `service.xml` or `rest-openapi.yaml`/`rest-config.yaml`, with its `-api`/`-service` or
`-api`/`-client`/`-impl` siblings as members.)

### Checklist — per module

Copy into `upgrade-notes/<run-id>/phase-2-compile/summary.md` and tick per module. Every box is an
**assertion about state**, not a task to consider — an unticked box means the module is not done.

- [ ] Step 1 — Service Builder regen ran on a **clean** slate (`@generated` deleted first), or no `service.xml`
- [ ] Step 2 — REST Builder regen ran, or no `rest-config.yaml`
- [ ] Step 3 — Jakarta prefixes renamed only where `release.dxp.api` provides them
- [ ] Step 3b — static dependency pass applied
- [ ] Step 3b — **compile-verified probe ran** (the static pass alone is not the cleanup)
- [ ] Step 3b — `<module>-dependency-prune.txt` exists **and agrees with the committed `build.gradle`**
- [ ] Step 4 — module builds clean
- [ ] Step 5 — JSP compilation clean, or no `.jsp`/`.jspf`
- [ ] Working tree clean; every change committed at (module × pattern) granularity

### Step 1 — Service Builder regen (if `service.xml` present)

When the module reaches its turn, update its `service.xml` for the target version (DTD / namespace),
then regenerate so the API is available to dependents **before** they compile.

**Force a clean regen** (see `## Gotchas` — `buildService` silently skips otherwise). **Before
running `buildService`, delete the previously SB-generated sources in both the `-api` and `-service`
modules** so the tool regenerates everything against the current SB tool and the target
`release.dxp.api`. Identify them by the `@generated` marker SB writes into every file it produces (the
hand-written `*Impl` entry points — `<Entity>Impl`, `<Entity>LocalServiceImpl`, `<Entity>ServiceImpl` —
do **not** carry it, so they survive):

```bash
grep -rl '@generated' <api-module>/src/main/java <service-module>/src/main/java | xargs rm -f
<gw> :<gradle-path>:buildService --console=plain
```

Prefer **one gradle invocation with all SB tasks** over a per-module `--no-daemon` loop (fresh-JVM
loops have produced spurious failures). Watch for the workspace plugin 15.x rewriting the root
`.gitignore` — snapshot and `git restore` it if it changes. Commit per module.
**Message hint:** `Regenerate Service Builder for <module>`

**The SB tool must match the target — set it via the workspace plugin, never a `build.gradle` force.**
The SB tool generates the model/wrapper/persistence by reflecting over the target `BaseModel`, so the
tool that produces code compiling against `release.dxp.api:<target>` is the one Liferay used to build
that release. blade's Step 0b plugin (Phase 1) is the *starting* hypothesis. If a **clean** regen still
produces code that won't compile against `release.dxp.api`, the SB tool is wrong — **read the
authoritative tool version from the target portal source**
(`~/dev/projects/liferay-portal-ee` at the target tag →
`modules/sdk/gradle-plugins-service-builder/build.gradle`, e.g.
`com.liferay.portal.tools.service.builder` `1.0.513` for `2026.q1.9`).

**The workspace plugin has two roles — separate them; the SB-tool plugin is a regen-only, temporary
setting.** blade's release-matched plugin (e.g. `17.1.0`) drives REST Builder, bnd, the Jakarta tooling
and the actual build, and is correct for those. The SB-tool-matching plugin (e.g. `15.1.3`, carrying
tool `1.0.513`) is needed **only to generate** the Service Builder sources. So:

1. Set `com.liferay.gradle.plugins.workspace` in `settings.gradle` to the SB-tool-matching version
   **temporarily**, run the **clean** regen (delete `@generated` + `buildService`), and **commit the
   generated sources**.
2. **Restore `settings.gradle` to blade's release-matched plugin** for the rest of the phase. The
   committed SB sources are not regenerated again, so the release plugin compiles them fine while
   keeping REST/bnd/Jakarta correct. **Do not re-run a clean SB regen under the release plugin** — it
   would regenerate with the wrong tool and reintroduce the mismatch.
3. Record **both** in `upgrade-state.md`: the release plugin left in `settings.gradle`, and the SB-tool
   version that produced the committed generated sources (they diverge by design — a human-review note).

Never add an SB-tool `resolutionStrategy.force` to any `build.gradle`. (Observed on `dxp-2026.q1.9-lts`:
regenerated SB with ws plugin `15.1.3` / tool `1.0.513`, then built everything with `17.1.0`.)

### Step 2 — REST Builder regen (if `rest-config.yaml` present)

```bash
<gw> :<gradle-path>:buildREST --console=plain
```

Skip with an N/A note if no `rest-config.yaml`. Commit per module.
**Message hint:** `Regenerate REST Builder for <module>`

### Step 3 — Jakarta migration (`javax.*` → `jakarta.*`)

Migrate prefixes confirmed available in the Step 0 audit. Use line-anchored, word-boundary
substitution. For **7.4 / 2026.Qx** targets, `javax.portlet` → `jakarta.portlet` (the fat JAR
provides `jakarta.portlet`; confirm it is in the Step 0 audit before migrating). **Keep** only the
prefixes the platform does not re-map (`javax.sql`, `javax.crypto`, `javax.net`,
`javax.xml.parsers/transform/stream/datatype/namespace`):

```bash
find <module-path> -name "*.java" -not -path "*/build/*" -print0 | xargs -0 sed -i -E \
  -e 's/\bjavax\.servlet\b/jakarta.servlet/g' \
  -e 's/\bjavax\.portlet\b/jakarta.portlet/g' \
  -e 's/\bjavax\.ws\.rs\b/jakarta.ws.rs/g' \
  -e 's/\bjavax\.mail\b/jakarta.mail/g' \
  -e 's/\bjavax\.persistence\b/jakarta.persistence/g' \
  -e 's/\bjavax\.xml\.bind\b/jakarta.xml.bind/g'
```

The `javax.portlet` rule covers **both** imports (`import javax.portlet.Portlet`) **and** the
`@Component` property keys in the same `.java` files (`"javax.portlet.name=…"`,
`init-param`, `display-name`, `resource-bundle`, `security-role-ref`) → `jakarta.portlet.*`. Also
**remove the `javax.portlet:portlet-api` dependency** from `build.gradle` — `release.dxp.api` provides
`jakarta.portlet` (the Step 3b prune will catch it, but drop it here with the rename).

Cross-check `.bnd`, `.xml`, `.jsp`, `.tld`, `.properties` for `javax.*` literals (e.g.
`Import-Package`, `web.xml`). For prefixes NOT in the fat JAR (`jakarta.xml.ws`, `jakarta.jws`),
either leave as `javax.*` or add the jakarta-named dep — state the choice in the commit body.
**Message hint:** `Migrate javax.* imports to jakarta.* in <module>`

**Jakarta API dependency additions.** Two `jakarta.*` prefixes are not provided by the platform and
need an explicit API dependency once a module imports them (mirrors Liferay's
`GradleMissingDependenciesForUpgradeJava21Check`). After the prefix rename, for each module add the
matching dependency if a class imports it and the dep is not already declared (idempotent):

| Import seen in module | Dependency to add (GAV) |
|---|---|
| `import jakarta.annotation.*` | `jakarta.annotation:jakarta.annotation-api:2.1.1` |
| `import jakarta.xml.bind.annotation.*` | `jakarta.xml.bind:jakarta.xml.bind-api:4.0.2` |

Configuration: `compileOnly`, except modules whose directory name ends in `-test`, which use
`testIntegrationImplementation`. The compile-verified prune (Step 3b) still treats these as
candidates afterward — if nothing references them post-build they are removed.

> Eclipse Transformer CLI is the canonical fallback for non-import cases (MANIFEST.MF, taglib URIs):
> `java -jar org.eclipse.transformer.cli-<ver>.jar <module> <module>`.

### Step 3b — Dependency cleanup

Clean the module's `build.gradle` deps so the build starts lean and ends minimal. Two passes:

**Static (before the build):**
- **Consolidate to `release.dxp.api`:** individual Liferay deps the BOM provides at the artifact
  level → replace with a single `compileOnly … release.dxp.api` (no version). Use the resolution
  logic in `upgrade-setup-version/references/release-dxp-api-redundant.md`. Keep what the BOM does
  not provide.
- **Drop obviously-unused:** a declared dep with no `import` / reference anywhere in the module.
- **Remove stale version forces; never add new ones.** A pre-existing `resolutionStrategy.force` (or
  `configurations.all { resolutionStrategy { force … } }`) that pins an artifact to an old version
  overrides the target `release.dxp.api` and breaks signatures (observed: a forced
  `com.liferay.portal.vulcan.api` pinning a pre-target version) — **remove it** and let the target
  resolve. Such forces also live in **ancestor `build.gradle` files** (a module-group root's
  `subprojects{}`/`allprojects{}` block), invisible to the per-leaf prune — those are swept by the
  **Hierarchical `build.gradle` cleanup** below. And **never add** a `force` / tool-version pin as a
  workaround for a build failure (e.g. a Service Builder tool mismatch): a wrong version means the
  **workspace plugin** is wrong — fix it via the workspace plugin (Step 1's SB clean-regen +
  authoritative tool), not with a `build.gradle` force here.

**Compile-verified probe (MANDATORY — THIS is the dependency cleanup; the static pass is not).**
Once the module reaches a clean build in Step 4, you **must** run this probe and emit its artifact.
The static pass alone leaves `org.osgi.*`, `cssBuilder`, and other workspace/`release.dxp.api`-provided
pins behind — a `Clean redundant dependencies` commit that ran only the static pass is **incomplete**.
Run this concrete loop (the full per-candidate algorithm is `upgrade-module` step 4b):

1. **List every top-level declared dep** in `dependencies { … }` — classpath deps **and** build-config
   configs (`cssBuilder`/`parentThemes`/`portalCommonCSS`/`themeBuilder`). **No exclusions.**
2. **For each candidate, one at a time:** comment the **whole line** out — locating it by its **exact
   content**, never by a cached line number (each comment/restore shifts the line numbers below it, so
   a line-number-driven loop edits the wrong line on the next candidate and corrupts the file —
   observed: a false `kept` on `org.osgi.service.component.annotations`) → run the **full module build**
   `<gw> :<path>:clean :<path>:build` (incl. bnd + CSS/theme tasks) → if it still builds it was
   redundant (**stays removed**); if it breaks, **restore the line**. Record the decision.
3. **Emit the artifact** `…/phase-2-compile/<module>-dependency-prune.txt` — one line per candidate
   in the verdict format of `upgrade-module/assets/dependency-prune.txt.template`, or the single line
   `(no candidates found)`.

This is the only way to catch **package-level** redundancies (see `## Gotchas`) and the
workspace-provided build-config pins. Opt-out: `upgrade.module.dependency.prune=false`.

> **Completion gate.** A module is **not** done — and its `Clean redundant dependencies` commit is
> **not** valid — until `<module>-dependency-prune.txt` exists **and agrees with the committed
> `build.gradle`**: every `removed` dep absent from it, every `kept` dep present. If the artifact is
> missing the probe was skipped — run it now; if you re-prune after a slip, regenerate it. The artifact
> is the audit record, so it must not lie. Do not skip the probe because the module needed heavy earlier
> intervention (SB regen, tool fights) — a module that compiles clean still gets the probe. (Observed:
> the static pass ran, `org.osgi.*`/`cssBuilder` survived and no artifact was produced; and separately a
> re-pruned module's artifact still said `kept` for a dep the commit had removed.)

**Commit:** `<TICKET> Clean redundant dependencies` (the module's dep cleanup as its own commit).

**Hierarchical `build.gradle` cleanup (ancestor-aware, manifest-tracked).** A module's *effective* build
config is its own `build.gradle` **plus** everything its ancestors inject via `subprojects {}` /
`allprojects {}` / `configurations.all {}` — forces, version pins, dep injections, repos, plugin config.
The per-leaf prune above sees only the leaf, so an ancestor-level stale pin (e.g. a
`resolutionStrategy.force` on an old `vulcan.api` in a module-group root) survives invisibly and
overrides the target `release.dxp.api` for every child. Sweep the whole hierarchy with a manifest:

1. **Inventory (once per phase).** List **every** `build.gradle` in the workspace and classify each:
   `root` (workspace root) · `aggregator` (has a `subprojects{}`/`allprojects{}`/`configurations.all{}`
   block, groups modules, no real `dependencies{}` of its own) · `leaf` (a real module). Write the
   manifest `…/phase-2-compile/build-gradle-cleanup.txt` — one line per file with status `pending`.
2. **Ancestor-walk (per module).** When you clean a leaf at `modules/<group>/<M>/build.gradle`, also
   inspect its ancestor chain up to the root — `modules/<group>/build.gradle`, `modules/build.gradle`,
   `<root>/build.gradle` — for anything in a `subprojects{}`/`allprojects{}`/`configurations.all{}`
   block that affects that module's build: stale `resolutionStrategy.force` / version pins (remove —
   same doctrine as leaf forces; let the target resolve), dep injections, repos. Removing an
   ancestor-level pin changes the effective config of **all** that ancestor's children, so re-verify
   them with their full build; if a removal breaks a child, restore it.
3. **Checkpoint.** Clean each ancestor **once** — the first module to reach it does the work and flips
   its manifest line to `cleaned`; modules sharing that ancestor **skip** it (never re-consult a
   `cleaned` file). Mark each leaf `cleaned` as its prune completes.
4. **Completion gate.** Phase 2 is **not** done until **every** `build.gradle` in the manifest is
   `cleaned` / `verified clean` (visited, nothing to change). An aggregator that owns no leaf of its own
   still must be visited — that is exactly what let a `vulcan.api` force at a module-group root survive
   an entire phase. Re-derive the manifest from the tree and confirm no `pending` line remains.

Opt-out follows the prune (`upgrade.module.dependency.prune=false` skips this too).

**`-includeresource` → `compileInclude` (if the module bundles a dependency jar via bnd).** Modules
that hand-bundle a dependency by naming its jar in the `bnd.bnd` `-includeresource` / `Include-Resource`
instruction should move to the Gradle `compileInclude` configuration (mirrors Liferay's
`UpgradeBNDIncludeResourceCheck` + `UpgradeGradleIncludeResourceCheck`). For each jar listed in
`-includeresource` that corresponds to a declared `build.gradle` dependency:

1. Re-declare that dependency with the `compileInclude` configuration (drop the old configuration line).
2. Add `liferayOSGi { expandCompileInclude = true }` to `build.gradle` (once per module).
3. Remove the migrated jar entries from the `bnd.bnd` `-includeresource` / `Include-Resource`
   instruction — and remove the whole instruction if no entries remain.

Skip modules with no `-includeresource`, and leave non-jar `-includeresource` entries (e.g. bundled
static files) untouched. Commit. **Message hint:** `Migrate -includeresource jars to compileInclude`

### Step 4 — Verification loop (build → remediate → commit)

Loop until the module builds clean. **Exit condition:** `<gw> :<path>:clean :<path>:build` succeeds
with no remediation applied on the final pass.

```bash
<gw> :<gradle-path>:clean :<gradle-path>:build
```

On compile failure:

1. Check the decision log in `upgrade-state.md` for the same error pattern.
2. **Remediate via `deprecated-api-remediation`** — it is the **Phase 2 primary applier** of
   Liferay's `replacements.json` catalog. It consults
   `.claude/cache/reference/<source-id>/index_simple.md` and `index_complex.md`, applies catalog
   hits exactly (`Replace X by Y`, no `#automation` label — Liferay already covers it), and flags
   catalog misses for a manual fix carrying the `#automation` label (so the gap is harvested at the
   compile→startup boundary). For structural transforms not in `replacements.json`, it falls back to
   `check-classes-reference.md`.
3. For a catalog **miss**, **first run `deprecated-api-remediation` §3g (catalog-coverage guard)** —
   re-check the symbol/class against *all* catalog sources (imports.txt, both indexes **including the
   `regex_replace` entries**, and `check-classes-reference.md`). Only label a fix `#automation` when
   none match. If a source matches, it is a **hit**: apply the catalog `to` and commit it as
   `Replace X by Y` with **no** `#automation` — do not hand-derive it. Then, for a genuine miss, look
   the symbol up against `~/dev/projects/liferay-portal-ee` at the target tag before inventing a fix.
4. **Re-run the build and repeat from step 1** until it is clean. **One commit per pattern**, and a catalog hit is **never bundled**
   into a manual/`#automation` commit (keep them separate). **Message hint:** `Replace X by Y`

Log known build false-positives as such rather than "fixing" them — see `## Gotchas`.

**Redundant deps surfacing at compile:** the compile-verified prune of `release.dxp.api`-provided
package-level deps (deferred from Phase 1 Step 8b) is handled per `references/legacy-compile-deps-sweep.md`
(and `upgrade-module` step 4b).

### Step 5 — JSP compilation (only if the module has `.jsp`/`.jspf` under `src/`)

#### 5a — Enable the compileJSP plugin

Add `apply plugin: "com.liferay.jasper.jspc"` to the module's `build.gradle`. (Removed again in 5d.)

#### 5b — Reconcile JSP overrides (hook / fragment-host modules)

If the module overrides platform JSPs (`bnd.bnd` has `Fragment-Host:`, or it is a JSP hook), for
each overridden JSP — three commits:
1. **Copy the original JSP** from `liferay/liferay-portal` at the `upgrade.source.version` tag.
   **Hint:** `<module> - <jsp> copy logic from <source-version>`
2. **Copy the target JSP** from the same repo at the `upgrade.target.version` tag (replace).
   **Hint:** `<module> - <jsp> copy logic from <target-version>`
3. **Re-apply the client's custom code** onto the target JSP, resolving conflicts.
   **Hint:** `<module> - <jsp> apply custom code`

If the original JSP cannot be found (moved/removed), flag for human review and continue.

#### 5c — Run compileJSP and fix errors

```bash
<gw> :<gradle-path>:compileJSP
```

Delete `<module-path>/build` before each rerun. For `com.liferay.*.internal.*` access errors, add
the packages to `Export-Package` in `bnd.bnd` (accessible at runtime after redeploy).
**Hint:** `Fix internal package exports in <module>`

#### 5d — Remove the compileJSP plugin

Remove the `apply plugin: "com.liferay.jasper.jspc"` line. **Hint:** `Remove compileJSP plugin from <module>`

---

## Theme module steps

Process themes after all regular modules are clean.

### Checklist — per theme

Same rules as the per-module checklist above.

- [ ] Theme Step 0 — theme builds **in-workspace** (no per-theme `settings.gradle` / `repositories` added)
- [ ] Theme Step 0 — `<theme>-dependency-prune.txt` exists and agrees with the committed `build.gradle`
- [ ] Theme Step 1 — generated files cleaned
- [ ] Theme Step 3 — metadata bumped **and** `build.gradle` changed (a byte-identical `build.gradle` means the prune never ran)

### Theme Step 0 — Build in-workspace; prune `build.gradle` to minimal

Build the theme **as part of the workspace** (`<gw> :themes:<theme>:build`), never standalone. The
workspace plugin auto-discovers `themes/` (via `liferay.workspace.themes.dir`) and the Phase 1 root
`allprojects` block already provides the repositories — so the theme needs almost nothing of its own:

- **Prune the theme `build.gradle` to the minimum**, compile-verified against the **in-workspace**
  build: the workspace plugin's theme support + `release.dxp.api`/target platform provide
  `cssBuilder`, `parentThemes` (`frontend.theme.styled`/`unstyled`), `portalCommonCSS`, and the theme
  builder. Remove each such declaration → rebuild in-workspace → if it still builds it was provided,
  keep it removed; if it breaks, restore. **Do not pin versions** on the ones that stay.
- If a legacy theme carries its own `buildscript { … }` applying the theme-builder plugin standalone,
  prefer letting the workspace plugin apply it — drop the standalone `buildscript` once the
  in-workspace build succeeds without it.

This is the theme analog of the Step 3b compile-verified prune — same opt-out
`upgrade.module.dependency.prune`, same completion gate, with the artifact at
`…/phase-2-compile/<theme>-dependency-prune.txt`. **Observed failure to avoid:** only bumping the theme
metadata and leaving `build.gradle` byte-identical to the source version — that means this prune never
ran (`cssBuilder`/`parentThemes` still pinned).

**Hint:** `Prune <theme> build.gradle to workspace-provided defaults`

### Theme Step 1 — Clean generated files

Delete if present: `package-lock.json`, `yarn.lock`, `node_modules/`.

### Theme Step 2 — Node version (fallback only)

The Node version is set **globally** by the Phase 1 root `allprojects` block (`node.version`). This
per-theme step is now a **fallback/override only** — apply it only if a theme pins its own
`node.nodeVersion` that conflicts with the global and must differ. Default: rely on the global.

### Theme Step 3 — Theme metadata + dart-sass + build

- Bump theme / workflow XML metadata per `references/theme-and-workflow-metadata-bump.md` — apply
  **all three** metadata changes, not just the DOCTYPE: (1) the `<!DOCTYPE … DTD … >` version in
  `liferay-look-and-feel.xml` (`XMLUpgradeDTDVersionCheck`), (2) the `<compatibility><version>`
  inside the same file → `<target.major>.<target.minor>.0+` (`XMLUpgradeCompatibilityVersionCheck`),
  and (3) `liferay-versions` in `liferay-plugin-package.properties` → the single target floor
  `<target.major>.<target.minor>.0+` (`PropertiesUpgradeLiferayPluginPackageLiferayVersionsCheck`).
  **Observed gap:** bumping only the DOCTYPE and leaving `<compatibility>` / `liferay-versions` at the
  old version.
- Migrate `node-sass` → `dart-sass` per `references/themes-dart-sass.md`.
- Run the Step 4 build-and-remediate loop using the theme's Gradle path.

---

## Phase completion

Write `upgrade-notes/<run-id>/phase-2-compile/summary.md`, rendering
`assets/summary.md.template`.

Update the Phase 2 row in `upgrade-state.md` to `complete` (or `blocked`). The `#automation` harvest
into `LPD-57087` runs at the phase boundary — it is owned by `/upgrade-phase`, not this skill.

Report to the user: modules processed / clean / blocked, blocked list, and
`Next: /upgrade-phase 3 (Fix Startup)`.

---

## Autonomy

- **Autonomous:** Game Plan walk, Service Builder / REST regen, the Jakarta sed for fat-JAR-confirmed
  prefixes, build runs, catalog-hit remediation via `deprecated-api-remediation`, committing fixes,
  the JSP plugin toggle.
- **Requires user input:** a manual fix for any catalog-miss error with no documented solution; JSP
  custom-code re-application (5b); `bnd.bnd` `Export-Package` decisions; Jakarta prefixes not in the
  fat JAR (leave-as-javax vs add-dep).
- **Never:** run `./gradlew formatSource`; touch `bnd.bnd` bundle-version constraints (Phase 3); skip
  a module silently — if it cannot compile after exhausting fixes, flag it `blocked` and move on.
