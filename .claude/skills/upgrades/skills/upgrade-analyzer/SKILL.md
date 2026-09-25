---

name: upgrade-analyzer
description: Run this skill at the start of Phase 2 (Fix Compile) of the upgrade playbook, or when the user invokes `/upgrade-analyzer` directly. This skill analyzes the Liferay workspace by walking the file tree, parsing build files and OSGi metadata, and producing two strategic roadmaps: a Game Plan (compile-time dependency ordering for Java/Gradle modules) and a Startup Game Plan (OSGi runtime startup ordering by module category). The output drives the per-module order in Phase 2 (Fix Compile) and the deploy/bring-up order in Phase 3 (Fix Startup). Trigger also when the user says "analyze the upgrade order", "run the dependency analyzer", or "show me the build order for this workspace".

---

# upgrade-analyzer

Reads build files and OSGi metadata, then writes the **Game Plan** (Phase 2 module order) and the
**Startup Game Plan** (Phase 3 bring-up order) as `.txt` and `.csv`. No external tool required.

## Prerequisites

- `/upgrade-init` has run; `CLAUDE.md` is populated.
- The upgrade branch is checked out.
- Working directory is the workspace root.

## Step 0 — Setup

Read `modules.root` from `CLAUDE.md` (default: `modules/`). Establish the artifact output directory:

```
.claude/upgrade-artifacts/<run-id>/phase-2-compile/upgrade-analyzer/
```

Generate a timestamp string in milliseconds (use `date +%s%3N` via Bash) for output file names.

---

## PART 1 — Game Plan (compile-time dependency order)

### Step 1 — Discover projects

Walk the workspace tree recursively, skipping these directories at every level:
`.git`, `bin`, `build`, `dist`, `node_modules`, `node_module_cache`, `target`, `.gradle`, `.idea`

For each directory, classify it as a project if it matches **any** of:

| Type | Detection rule |
|---|---|
| **Gradle** | Contains `build.gradle` AND has a `src/` subdirectory |
| **Maven** | Contains `pom.xml` |
| **JS Portlet** | Contains `package.json` AND does NOT have `build.gradle` or `pom.xml` |
| **Theme** | Contains `liferay-theme.json` OR `liferay-look-and-feel.xml` |

For each discovered project, derive its **Gradle key**: take its path relative to the workspace root, replace each `/` with `:`, and prepend `:`. Example: `modules/acme/acme-api` → `:modules:acme:acme-api`.

---

### Step 2 — Parse inter-project dependencies

For each Gradle project, read its `build.gradle` and extract references to other workspace projects using this regex pattern:

```
project\s*\(\s*['"]([^'"]+)['"]\s*\)
```

Each match is a dependency on another workspace module (identified by its Gradle key). Ignore references to projects not in the discovered set (external dependencies).

For Maven projects, read `pom.xml` and look for `<artifactId>` references within `<dependency>` blocks that match known project names. Correlate by artifact ID to workspace keys.

For JS/Theme projects with no `build.gradle`, treat them as having zero workspace dependencies.

---

### Step 3 — Build the dependency graph

For each project P:
- `dependencies(P)` = set of workspace projects P directly depends on
- `consumers(P)` = set of workspace projects that directly depend on P (inverse)
- `consumer_count(P)` = `|consumers(P)|`

Build both directions simultaneously as you process `build.gradle` files.

---

### Step 4 — Compute dependency levels

Assign a level to each project using the following rule:

```
level(P) = 1                                        if dependencies(P) is empty
level(P) = 1 + max(level(D) for D in dependencies(P))   otherwise
```

Apply iteratively until stable (handle cycles: if a cycle is detected, assign the cycle members to the same level and log a warning).

---

### Step 5 — Sort within each level

Within each level, sort projects by:

1. `consumer_count` descending (highest-impact first)

1. Gradle key ascending (alphabetical tiebreaker)

---

### Step 5b — Identify generated-source groups (Phase 2 branch/PR units)

Some projects are **generated-source groups**: a Service Builder or REST Builder generator produces
sources across several sibling sub-modules from a single descriptor, so Phase 2 must treat the whole
group as **one branch and pull request** (running `buildService` / `buildREST` once regenerates the
entire group — splitting it across PRs fragments a single generated artifact, and the `-api` half won't
compile without the regen committed beside it).

Detect a group by its generator descriptor anywhere in a project's subtree:

| Group type | Descriptor (anywhere under the group root) | Typical members |
|---|---|---|
| **Service Builder** | `service.xml` | `<name>-api`, `<name>-service` |
| **REST Builder** | `rest-openapi.yaml` or `rest-config.yaml` | `<name>-api`, `<name>-client`, `<name>-impl` |

The **group root** is the nearest common parent directory of those sibling modules (e.g.
`modules/sample-rest-builder/`). The **members** are the discovered Gradle projects under that root.
Keep each member as its own node in the dependency graph (levels are still computed per project); the
group only changes the **Phase 2 work unit**, not the build order. A project with no shared generator is
its own single-module work unit.

Record each group: root, member Gradle keys, generator type, and the earliest member level (the level
at which Phase 2 picks the group up).

---

### Step 6 — Write Game Plan text file

Write to `projects-<timestamp>.txt` in the artifact directory, rendering
`assets/game-plan.txt.template`.

Rules:
- Indent each project line with a single tab character.
- List dependencies in parentheses, comma-separated. If no dependencies, write `()`.
- Write `<consumer_count>` as a plain integer directly after the key (space-separated).

---

### Step 7 — Write Game Plan CSV file

Write to `projects-<timestamp>.csv` in the artifact directory, with the header from
`assets/game-plan.header.csv`.

For each project (same order as the text file):
- **Level**: the computed level number
- **Bundle Name**: the Gradle key (e.g., `:modules:acme:acme-api`)
- **Symbolic Name**: derive from the Gradle key by removing the leading `:` and joining with `.` → `modules.acme.acme-api`; if a `bnd.bnd` exists with `Bundle-SymbolicName:`, use that value instead
- **Dependencies**: comma-separated list of dependency Gradle keys; empty string if none
- **Error columns**: leave all as empty strings (these are filled manually during the upgrade)

---

## PART 2 — Startup Game Plan (OSGi runtime order)

### Step 8 — Categorize OSGi modules

Walk the workspace tree again (same skip list as Step 1). For each directory containing a `bnd.bnd` file, read its content and assign a category using this priority order:

| Priority | Category | Detection rule |
|---|---|---|
| 1 | **Fragment-Host** | `bnd.bnd` contains a line starting with `Fragment-Host:` |
| 2 | **Services and APIs** | `bnd.bnd` contains `Liferay-Service:` OR the directory contains `src/main/resources/META-INF/service.xml` or `src/main/webapp/WEB-INF/service.xml` |
| 3 | **Exporters** | `bnd.bnd` contains `Export-Package:` and none of the above matched |
| 4 | **Plugins** | `bnd.bnd` present but none of the above rules matched, AND module path contains `/portal/` or module name ends with `-portlet` or `-hook` |
| 5 | **Others** | everything else with a `bnd.bnd` |

For directories containing `liferay-theme.json` or `liferay-look-and-feel.xml` (theme projects without `bnd.bnd`), assign to **Others (including themes)**.

Assign the Gradle key using the same derivation as Step 1.

---

### Step 9 — Write Startup Game Plan text file

Write to `projects-<timestamp>-startup.txt` in the artifact directory, rendering
`assets/startup-game-plan.txt.template`.

Within each category, sort alphabetically by Gradle key. If a category has no modules, write its heading with no entries below it.

---

### Step 10 — Write Startup Game Plan CSV file

Write to `projects-<timestamp>-startup.csv` in the artifact directory, with the header from
`assets/startup-game-plan.header.csv`.

For each module (same order as the text file):
- **Category**: the category name (e.g., `Exporters`, `Services and APIs`, etc.)
- **Bundle Name**: Gradle key
- **Symbolic Name**: same derivation as Step 7

---

## PART 3 — Summarize and record findings

### Step 11 — Write findings to upgrade-state.md

Append an **Upgrade Analyzer** section under the Workspace inventory in `upgrade-state.md`, rendering
`assets/upgrade-state-analyzer.md.template`.

### Step 12 — Annotate Phase 2 and Phase 3 in the phase tracker

In the **Phase 2 (Fix Compile)** row of the phase tracker, append:
> "Walk modules by Game Plan level — Level 1 first, then Level 2, … High-consumer modules: <top-3-list>. Generated-source groups (one branch/PR each): <group-roots, or 'none'>."

In the **Phase 3 (Fix Startup)** row, append:
> "Deploy in Startup Game Plan order: Exporters → Services/APIs → Plugins → Fragment-Hosts → Others."

---

## Output

Report after completion per `assets/completion-report.txt.template`.

## Autonomy

Fully autonomous — file walking, parsing, and writing only. No confirmations needed.