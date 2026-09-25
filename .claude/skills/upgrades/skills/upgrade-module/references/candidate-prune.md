# `CANDIDATE_PRUNE` allowlist

Reference for `upgrade-module`'s **step 4b** (compile-verified prune). Lists the `(group, name)` pairs that are candidates for removal *only after a successful compile-without-them*. Every entry here failed `upgrade-setup-version`'s Step 8b "definite redundancy" test (i.e. `release.dxp.api`'s POM does not declare them as Maven dependencies) but is plausibly already provided at the package level by the runtime, so it is worth a compile-verified probe.

The list is intentionally small. **Do not add an entry without a known reason it might be redundant** — false positives cost compile time, false negatives at most leave a redundant line in place.

## Schema

Each entry is a `(group, name)` pair, optionally with:

- `version` — match only when this version literal is present (used to scope javax-era artifacts).
- `condition` — extra runtime test before the candidate is even considered.

## Entries

| group | name | version | condition | rationale |
|---|---|---|---|---|
| `org.osgi` | `org.osgi.annotation.versioning` | (any) | (always) | Package `org.osgi.annotation.versioning` is exported by the OSGi framework at runtime; the explicit dependency is only needed when the module's bytecode actually references the annotation. |
| `org.osgi` | `org.osgi.core` | (any) | (always) | Most code uses higher-level OSGi APIs already provided by `release.dxp.api`. |
| `org.osgi` | `org.osgi.service.component.annotations` | (no `version:` attribute) | (always) | The unversioned form. Explicit-version duplicates of the same `(group, name)` are caught by Step 8b's duplicate detector and never reach step 4b. |
| `javax.servlet` | `servlet-api` | `2.5` | target ≥ 2025.q3 (Jakarta is in effect) | Pre-Jakarta servlet API. After Jakarta migration, code references `jakarta.servlet.*` and the legacy artifact is dead weight. |
| `javax.ws.rs` | `javax.ws.rs-api` | `2.1` | target ≥ 2025.q3 (Jakarta is in effect) | Same reasoning, JAX-RS side. |
| `com.liferay.jakarta.portlet` | `com.liferay.jakarta.portlet-api` | (any) | target ≥ 2025.q3 | Liferay shim that bundled the Jakarta portlet API during the transition. From 2025.q3 onward `release.dxp.api` carries the canonical Jakarta portlet API. |

## How a candidate is matched in `build.gradle`

The same line patterns recognised by `upgrade-setup-version`'s `references/release-dxp-api-redundant.md` apply here (single-line map, multi-line map, quoted-coordinate string). A candidate matches only when:

1. The module's `build.gradle` declares `release.dxp.api` in the same top-level `dependencies { … }` block.
2. The line tokenises cleanly to `(group, name)` (and `version` if the entry pins one).
3. Any `condition` evaluates true for the configured `upgrade.target.version`.

## Tier evidence

Step 4b reuses the parsed compile-classpath dep-tree from `upgrade-setup-version`'s Step 8b (cached at `.claude/cache/upgrade/gw-deps-<target>/<gradle-project>.txt`) to classify each matched candidate into one of four tiers. The tier dictates whether a compile probe is needed and, if so, in what order. See the parser definition and the three sets — `RESOLVED_BY_RELEASE_DXP_API`, `OVERRIDDEN_FROM_RELEASE_DXP_API`, `CONSTRAINT_ONLY_ROOTS` — in `upgrade-setup-version/references/release-dxp-api-redundant.md` "Resolution algorithm — primary path".

| Tier | Condition | Action | Justification recorded |
|---|---|---|---|
| 1 — auto-remove | GA ∈ `CONSTRAINT_ONLY_ROOTS` ∧ GA ∈ `RESOLVED_BY_RELEASE_DXP_API` | Remove without a compile probe. Constraint-only declarations contribute no classes; removal cannot fail compilation. | `auto-removed (constraint-only + provided)` |
| 2 — probe-first | GA ∈ `RESOLVED_BY_RELEASE_DXP_API` and NOT constraint-only | Probe via comment-out + `compileJava`. High-confidence removable; ordered first in the probe loop. | `removed (probed; provided transitively)` or `kept (used by <symbol>)` |
| 3 — probe-with-skepticism | GA NOT under `release.dxp.api`'s subtree but on the allowlist (canonical case: `org.osgi.annotation.versioning`) | Probe; ordered after Tier 2. The redundancy is package-level, not artifact-level — only compilation tells. | `removed (probed; package-level provision)` or `kept (used by <symbol>)` |
| 4 — skip | GA is a root contributor on the classpath (not a child of `release.dxp.api`, not constraint-only) | No probe. Real classpath contributor. | `kept (real classpath contributor)` |

### Typical mapping per allowlist entry

The mapping below is **typical, not guaranteed** — actual tier depends on the workspace's effective dependency-management for the configured target:

| Allowlist entry | Typical tier (target ≥ 2025.q3) | Why |
|---|---|---|
| `org.osgi:org.osgi.annotation.versioning` | Tier 3 | Package-exported by the OSGi runtime; no Maven dep on the artifact. Compile-probe required. |
| `org.osgi:org.osgi.core` | Tier 1 or 2 | Usually surfaces under `release.dxp.api`'s subtree; constraint-only when the workspace pins a version explicitly. |
| `org.osgi:org.osgi.service.component.annotations` (unversioned) | Tier 2 | Provided transitively by `release.dxp.api`. |
| `javax.servlet:servlet-api:2.5` | Tier 4 (or Tier 1 if redundantly constrained) | Not on the post-Jakarta classpath. The `kept (real classpath contributor)` outcome means somebody is still depending on the legacy javax form — flag for human review rather than removing silently. |
| `javax.ws.rs:javax.ws.rs-api:2.1` | Tier 4 (or Tier 1) | Same reasoning as servlet-api. |
| `com.liferay.jakarta.portlet:com.liferay.jakarta.portlet-api` | Tier 2 | The Jakarta portlet API is now provided by `release.dxp.api` itself. |

## Adding new entries

Before adding an entry, verify:

- It is **not** in `release.dxp.api`'s POM `<dependencies>` / `<dependencyManagement>` for any target version we care about (otherwise it belongs in Step 8b's automatic `PROVIDED_GA`, not here).
- A real module exists where removing it succeeds compilation in some cases and fails in others — i.e. the compile-verified probe is genuinely necessary.
- The `condition` is precisely scoped — broad conditions trigger pointless compile probes.

If unsure: do not add.
