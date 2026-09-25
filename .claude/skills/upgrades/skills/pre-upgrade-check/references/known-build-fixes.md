# Known build fixes (CHECK 1)

Recurring reasons a delivered workspace fails to build **from source outside the client's CI /
network**, with the safe reproduction fix for each. Every fix still gets a change-log + report row
(Step 3). Ordered by how often they bite.

## 1. CI version token in `version` / `Bundle-Version` (bundles fail to jar)

**Symptom.** Compilation succeeds (0 compile errors) but **every** module's `jar`/BND task fails:

```
error : Invalid value for Bundle-Version, <token> does not match \d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?
> Bundle <name>-<token>.jar has errors
```

**Cause.** The project version is a placeholder the client's CI substitutes at build time — a
non-semver token (commonly a `##...##` marker). It appears in `gradle.properties` (`version=<token>`),
in **every** `modules/**/bnd.bnd` (`Bundle-Version: <token>`), and often the theme
`package.json`/`package-lock.json`. Outside CI the token is never replaced, so OSGi rejects the
`Bundle-Version`. WARs (themes) are not OSGi-version-validated, so the theme may build while all
module jars fail — a tell-tale sign.

**Detect.** `version=` in `gradle.properties` (or `Bundle-Version:` in a `bnd.bnd`) is not a valid
version (contains non `[0-9.]` chars / matches `##.*##`). A `buildnumber` file and/or an
`updateVersion.sh` in the workspace confirm CI substitution.

**Fix — bake a valid version:**

```bash
V=1.0.0   # or the client's build number
sed -i "s/version=<token>/version=$V/" gradle.properties
find modules -name bnd.bnd ! -path '*/build/*' -exec sed -i "s/<token>/$V/g" {} +
# theme, if it uses the same token:
sed -i "s/<token>/$V/g" <themes.root>/*/package.json <themes.root>/*/package-lock.json
```

**Decision (record it):** either **bake** the version (a fresh clone builds with no extra step —
diverges from the client CI, touches many files) **or** keep the token and document the substitution
step in `UPGRADE-README.md` (preserves the CI contract; a clone must run the `sed` first). Prefer
baking when the goal is a self-contained reproduction repo; keep the token when the repo feeds the
client's own CI. Either way this is a **reproduction-environment** change (not a source defect).

## 2. Internal-only repositories (nothing resolves off-network)

**Symptom.** Settings evaluation / dependency resolution fails: `Could not resolve …` / `Could not
get resource … <internal-host>`, often `Name or service not known` for a private host.

**Cause.** `settings.gradle` (and sometimes `build.gradle`) declare only a private Nexus /
Artifactory. Off the client network those hosts are unreachable — even the Liferay workspace plugin
can't resolve at settings-evaluation time.

**Fix.** Add the Liferay public CDN + Maven Central **alongside** the private entries (never remove
them — they are valid inside the client network), in **every** repository block (both the
`gradle.allprojects { buildscript { repositories } }` / `repositories` blocks and the top-level
`buildscript { repositories }`):

```gradle
mavenCentral()
maven { url "https://repository-cdn.liferay.com/nexus/content/groups/public" }
```

Proprietary artifacts that exist only in the private Nexus/Artifactory still won't resolve
off-network — if the build then fails on a client-internal library (not a public Liferay/Maven
artifact), that is a **BLOCKER** requiring client-network access (or the artifact), not a fix.

## 3. Source JDK not on the host

Gradle 6.x will not run on JDK 17+ (see `source-version-matrix.md` for the source-line JDK).

**Look properly before concluding it is absent** — probing only `/usr/lib/jvm` has produced a false
"no JDK 8 here" on a machine that had one all along:

```bash
ls -d /opt/java/jdk8 /usr/lib/jvm/*1.8* /usr/lib/jvm/java-8* \
      "$HOME/.sdkman/candidates/java"/8* 2>/dev/null
/usr/libexec/java_home -v 1.8 2>/dev/null          # macOS
grep -rn 'JAVA_HOME=' ~/.bashrc ~/.zshrc 2>/dev/null # team switcher functions/aliases
```

Shell aliases such as `java8` are often functions exporting `JAVA_HOME`; they do not survive into a
non-interactive shell, so read the definition and use the path directly.

**Recognise the wrong-JDK failure.** It does not mention Java. Gradle 5.x/6.x on a modern JDK dies
inside Groovy, and the stack trace is tens of kilobytes long:

```
java.lang.NoClassDefFoundError: Could not initialize class org.codehaus.groovy.vmplugin.v7.Java7
	at org.codehaus.groovy.reflection.GroovyClassValueFactory.<clinit>(...)
```

Any build script the team ships should assert `java -version` reports the source line's JDK and stop
with that one-line explanation, rather than letting the caller read the trace.

If the host genuinely lacks it, build in a container instead of changing the host:

```bash
docker run --rm -u "$(id -u):$(id -g)" -e HOME=/tmp -e GRADLE_USER_HOME=/tmp/.gradle \
  -v "$PWD":/ws -w /ws eclipse-temurin:<source-java>-jdk ./gradlew clean deploy --console=plain
```

## 4. Theme build is fragile — do not let it block CHECK 1

Old `liferay-theme-tasks` (gulp) pipelines frequently fail on a modern Node (`node-sass` native
build errors, or a gulp stream `TypeError`), independently of the Java module build. Build the theme
in a **period-correct** Node container (see the theme-tooling→Node table in
`source-version-matrix.md`), then stage the WAR:

```bash
docker run --rm -e HOME=/tmp -v "$PWD/<themes.root>/<theme>":/t -w /t \
  node:<ver> sh -c "npm install && npx gulp build"     # then copy dist/*.war into bundles/osgi/war
```

If the theme still won't build with the available Node, **report it and move on** — the module build
(CHECK 1) and the portal (CHECK 3) do not depend on the theme WAR (the site falls back to a stock
theme). Flag it for the frontend/Node toolchain, do not treat it as a CHECK 1 failure.

## 5. Multi-workspace deliveries (the build order is not obvious)

**Symptom.** One delivery turns out to be *several* Liferay workspaces. Building any one of them alone
fails at `compileJava`:

```
Could not find base-module:com.everis.liferay.base:1.0.0.
Could not find productos-sb:com.sam.liferay.productos.api:1.0.0.
  Searched in the following locations:
    - file:/root/.m2/repository/...
    - https://repository-cdn.liferay.com/...
```

**Cause.** The workspaces are not peers. Satellite workspaces declare plain Maven coordinates on shared
modules that physically live in a *different* (core) workspace — group = the producing project's
directory name, artifact id = its `Bundle-SymbolicName`. The client's CI resolves them from a private
Artifactory, so nothing resolves off-network.

**Detect.**
```bash
grep -rhoE 'compile "[^"]+:[^"]+:[^"]+"' <ws>/modules --include=build.gradle | sort -u
grep -rl 'mavenLocal()' <ws>/modules --include=build.gradle | head
```
`mavenLocal()` in the consuming modules is the tell: it is the path the client's own build anticipates.

**Fix (no source edit).** Build the core workspace first and publish it locally, then the rest:
```bash
( cd <core-ws>  && ./gradlew clean deploy publishToMavenLocal )
( cd <other-ws> && ./gradlew clean deploy )
```
This *strengthens* CHECK 1 — the shared jars the satellites compile against are the ones just built
from source, not binaries pulled from the client's Artifactory. Record the order in
`UPGRADE-README.md`; a clean clone cannot discover it.

## 6. `gradlew` committed non-executable

**Symptom.** `./gradlew: Permission denied`, exit **126**, before Gradle starts.

**Detect the mode in git, not just on disk** — a `chmod` in the working tree hides it:
```bash
git ls-files -s | grep 'gradlew$'      # 100644 = broken, 100755 = correct
```
Mixed modes across a multi-workspace delivery are common (one workspace right, the rest wrong).

**Fix.** `chmod 755` (or `git update-index --chmod=+x` on Windows). Mode only, no content change.
Recommend the client add `.gitattributes` with `gradlew text eol=lf`.

## 7. macOS AppleDouble files that look like build output

A delivery zipped on macOS carries `__MACOSX/` resource forks that **mirror the source tree, including
`build/`**. The result is files named exactly like jars:

```
__MACOSX/<ws>/modules/<m>/build/libs/._com.customer.module-1.0.0.jar
```

They are not jars. On one delivery `find . -path '*/build/libs/*.jar'` returned **109** hits of which
**16** were real, so a glob-based staging step would have shipped 93 junk "bundles" into
`osgi/modules` and the run would have appeared to pass CHECK 1 without validating anything.

**Always filter when staging or counting:**
```bash
find . -path '*/build/libs/*.jar' -not -path '*__MACOSX*' -not -name '._*'
```
Report the metadata (and `.DS_Store`) as a hygiene row and add `__MACOSX/`, `.DS_Store`, `._*` to the
root `.gitignore`.

## 8. Scope a Gradle run with the working directory

Gradle restricts task execution to the project tree it is invoked from. In a Liferay workspace that
gives a clean split with no extra flags:

```bash
( cd <ws>/modules && ./gradlew clean deploy )   # modules only, zero :themes: tasks
( cd <ws>/themes  && ./gradlew clean deploy )   # themes only
```

Prefer this to running at the workspace root with `--continue` and then filtering the log: a fragile
gulp theme (§4) then cannot fail the module build at all, and the exit status stays meaningful.

Pass `-Pliferay.workspace.home.dir=<shared>/bundles` so several workspaces deploy into one bundle tree
— the one the compose file mounts — instead of each writing to its own `<ws>/bundles`.

## 9. Blade CLI is not unattended-safe on a workspace containing `pom.xml`

**Symptom.** `blade gw …` produces no output and never returns; piping its stdout makes it look hung.

**Cause.** Blade 8.x detects the stray `pom.xml` many Liferay workspaces still carry and asks
interactively, *even when `.blade.properties` already says `profile.name=gradle`*:

```
WARNING: blade commands will not function properly in a Maven workspace unless the blade
profile is set to "maven". Should the settings for this workspace be updated? (Y/n)
Should blade remember this setting for this workspace? (Y/n)
```

**Answering `Y` is destructive**: it rewrites `.blade.properties` to `profile.name=maven` and that
workspace stops building. A *finite* piped answer stream is also a trap — blade loops on the second
prompt once stdin hits EOF.

**Fix.**
```bash
blade gw --profile-name gradle clean deploy … < <(yes n)
```
`--profile-name gradle` removes the prompt entirely (verified: 0 prompts, stdout redirected to a file);
the endless `yes n` cannot EOF and covers any future prompt. Then assert nothing drifted:
```bash
grep -L '^profile.name=gradle' */.blade.properties
```
`--profile-name` rewrites the timestamp comment in each `.blade.properties` — cosmetic, but do not
commit it. Note `blade gw` only delegates to the workspace's own wrapper, so `./gradlew` is always a
valid substitute if blade is unavailable.

## 10. The Gradle *daemon* runs out of memory on a large workspace

**Symptom.** Configuration fails before any module compiles, and the message is about GC, not memory
limits:

```
A problem occurred configuring project ':modules:common:base-module'.
> Failed to notify project evaluation listener.
   > GC overhead limit exceeded
...
Starting a Gradle Daemon, 1 busy and 2 stopped Daemons could not be reused
```

**Cause.** Two things compound. Gradle 5.x's default daemon heap is small, and a workspace with dozens
of modules (62 on one delivery) exhausts it during *configuration*. Worse, every run that dies leaves a
daemon behind — eight stale `GradleDaemon` processes had accumulated and were holding the RAM that the
next run needed.

**Fix.** Do not rely on the daemon for batch builds, and give Gradle a real heap:

```bash
export GRADLE_OPTS="-Xmx2g"
./gradlew … --console=plain --no-daemon
```

Do **not** fix it by editing the client's `gradle.properties` (`org.gradle.jvmargs`) — that is a source
change for a purely local constraint. When a run has already failed, reclaim the RAM first:

```bash
pkill -f '[G]radleDaemon'
```

Watch for this on any host that is also running the portal container: the portal takes a fixed
multi-gigabyte heap of its own, so a build and a running portal compete. Note this is a *different*
OOM from the embedded-Elasticsearch one in `runtime-findings.md` §8 — that one is the portal JVM at
runtime, this one is the build JVM at configuration time.
