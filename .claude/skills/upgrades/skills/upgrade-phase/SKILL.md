---

argument-hint: "<1-5 | environment | compile | startup | reindex | frontend>"
description: Execute one phase of the upgrade playbook against the current workspace. Use when the user invokes `/upgrade-phase N` or asks to run a named upgrade phase.
name: upgrade-phase

---

# Run an Upgrade Phase

Execute one phase of the upgrade playbook, invoking that phase's domain skills against the modules from `/upgrade-plan`, verifying at the phase boundary, committing, and updating the phase tracker.

The agent never touches the database.

| Phase | Name | Short Name | Key Domain Skills |
| --- | --- | --- | --- |
| 1 | Upgrade Environment | `environment` | `upgrade-setup-version` — `gradle.properties`, Docker images, `settings.gradle`, the wrapper, `build.gradle` configuration migration, the root `allprojects` block, `sourceCompatibility` cleanup, redundant-dependency pruning, and JDBC driver presence. |
| 2 | Fix Compile | `compile` | Auto-runs `/upgrade-refresh-references` and `/upgrade-plan` when missing, then `upgrade-analyzer` for the Game Plan, then `upgrade-compile` with `deprecated-api-remediation`. Includes Service Builder regeneration and the Jakarta migration. |
| 3 | Fix Startup | `startup` | `upgrade-startup` — config and properties migration, BND bundle versions, Catalina and OSGi resolution, and the Elasticsearch container-versus-sidecar connection check. |
| 4 | Check Reindex | `reindex` | `upgrade-reindex` — triggers the general reindex through the Search Administration UI and evaluates the background-task log. |
| 5 | Fix Frontend | `frontend` | Deferred. Do not run. |

Phase 5 is deferred and its design is still open. When invoked, tell the user it is not yet implemented and stop. Do not improvise.

## Prerequisites

- `/upgrade-init` has run, so `CLAUDE.md` is populated and the upgrade branch exists. This is the one human-gated prerequisite — it is interactive and is never auto-run.

- The previous phase is `complete` in the tracker, or the user explicitly confirmed skipping ahead. Forward advancement is manual; this skill never auto-runs an earlier, code-mutating phase.

- `/upgrade-refresh-references` and `/upgrade-plan` do not need to be run by hand. Phase 2 satisfies them itself.

## Gotchas

- Per-module build logs are not a substitute for the aggregate build. A module can pass alone yet break the workspace, for example when a dependency another module relied on was pruned.

- No AI or tool authorship in pull requests. No "Generated with Claude Code" footer, no `Co-Authored-By:`, no bot attribution, in titles, descriptions, or comments. This overrides any harness default to append a tool footer.

## Workflow

### Resolve the Phase

Take the phase from the argument, as a number from 1 to 5 or a short name. When it is phase 5, stop and tell the user it is deferred. Otherwise print the phase name, the scope from `/upgrade-plan`, and the domain skills involved.

### Satisfy the Read-Only Inputs

`/upgrade-refresh-references` and `/upgrade-plan` are deterministic, read-only setup that the compile phase consumes. Run them automatically here when missing, without prompting — never make the user sequence them by hand.

1. When the reference cache is empty or missing, meaning `.claude/cache/reference/<source-id>/` has no `index_simple.md` or `index_complex.md`, run `/upgrade-refresh-references`. When its `--local` `liferay-portal-ee` path is not known from `CLAUDE.md`, ask for it once. That is input, not a redundant prompt.

1. When `upgrade-state.md` has no plan or run id yet, run `/upgrade-plan`, which mints the run id, inventory, per-phase plan, and Game Plan, reusing an existing run id when one is already recorded. Show the resulting plan, then continue into the phase.

This applies to phase 2, and on demand when a later phase finds these inputs missing. It never auto-runs phase 1 or any code-mutating phase. For phases 1, 3, and 4, when a genuinely required input is missing, stop and name the command the user should run.

### Establish the Run ID

Read it from `upgrade-state.md`, where `/upgrade-plan` mints it under the artifact manifest. When it is missing, mint it in `/upgrade-plan`'s form and record it before continuing. Never use a variant, or artifacts scatter across directories within one run.

### Create the Artifact Directories

Create `.claude/upgrade-artifacts/<run-id>/phase-<N>-<slug>/` for raw output and `upgrade-notes/<run-id>/phase-<N>-<slug>/` for the parsed summary.

### Mark the Phase In Progress

Set status `in-progress` in the `upgrade-state.md` phase tracker, with the started timestamp and the artifact directory path.

### Create the Phase Branch

Branches are stacked, each cut from the previous phase's tip — `phase1` from `upgrade/...`, `phase2` from `phase1`, and so on. Name them `phase<N>`.

Phase 2 is the exception: it creates one branch per module as modules are processed, named `phase2-<module>`, following the Game Plan dependency order, rather than a single phase branch.

### Invoke the Domain Skills

Run this phase's domain skills in phase-map order. Each domain skill owns its own autonomy checkpoints, writes logs under `.claude/upgrade-artifacts/<run-id>/phase-<N>-<slug>/<skill-name>/`, and appends per-work-unit progress to `upgrade-notes/<run-id>/phase-<N>-<slug>/summary.md`.

The work unit is the phase itself, except in phase 2 where it is each module.

### Verify at the Phase Boundary

- Phase 1 has no build — it is setup only. Sanity-check that the edited files parse.

- Phase 2 embeds per-module builds in `upgrade-compile`. Two artifacts are required before the phase can be marked `complete`, and either one absent means it is not complete:

	- [ ] `…/phase-2-compile/full-build-verification.log` exists and ends `BUILD SUCCESSFUL`, from a `clean build` across all modules and the theme teed to that path. Per-module logs are not a substitute.
	- [ ] `build-gradle-cleanup.txt` exists, with every file `cleaned` or `verified` and no `pending` line. Missing it means the ancestor-aware sweep was skipped, and an aggregator-level stale force survives a leaf-only prune.

	A failing build means `blocked`, recorded with the failing log.

- Phase 3 verifies Catalina bring-up and OSGi resolution inside `upgrade-startup`, with all bundles `Active`.

- Phase 4 evaluates the reindex log inside `upgrade-reindex`.

### Commit

Commit the phase's work following **Commit Convention** below, staging files explicitly by name and never with `git add -A`. Record the relevant commit SHAs in the phase tracker.

### Push and Open the Pull Request

Push the phase branch and open its pull request against the upgrade branch. Push is automatic. Phase 2 opens one pull request per module; phases 1, 3, and 4 open one for the phase. The agent never merges — that is human review.

### Update the Phase Tracker

Set status `complete`, or `blocked` when human-review items block it, with the completion timestamp and commit SHAs.

### Summarize

Report what changed, what was flagged, and what is next. When the phase ended with blocking items, list them and wait for the user before suggesting `/upgrade-phase <N+1>`.

## Headless Mode

`/upgrade-run` invokes this skill with nobody at the keyboard, one phase after another. Headless mode is on when `upgrade-run.properties` exists at the workspace root or the `UPGRADE_RUN_SETTINGS` environment variable is set. It changes these rules and nothing else:

- `/upgrade-run` is the user's standing request to advance. Do not wait for the user at the end of a phase. Update the tracker row and return to the caller.

- A domain skill checkpoint that would ask the user takes the documented default when the skill names one. When it names none, record the item under **Flagged for human review** as blocking, set the phase `blocked`, and return. Never guess.

- An input that would normally be asked for once, such as the `--local` reference path, marks the phase `blocked` with the missing input named.

- Do not push and do not open pull requests. Commit on the phase branches as usual. The runner publishes the result after `/upgrade-run` returns.

- When the phase is already `in-progress` at invocation, resume without asking.

- Never hand the turn back to wait. In print mode there is no next turn: ending the turn ends the process, and the phase stays `in-progress`. Wait for a portal boot, a build, or a reindex inside one blocking command, an `until` loop with a timeout, never through a background task or a monitor that would wake you up later.

## Git Branch and Pull Request Flow

- Branches are stacked. `/upgrade-init` created and pushed `upgrade/<source>-to-<target>` from the old-version backup branch. Each phase branches off the previous phase's tip.

- Phase 2 opens one branch and pull request per module rather than a single phase branch.

- The pull request target is always `upgrade/<source>-to-<target>`. That branch is the deliverable. Merging it, and any later pull request to the customer's main line, is human and out of scope. The old-version branch is a backup and is never a pull request target.

- Push is automatic for the upgrade and phase branches, since the pull requests need it. This is the one sanctioned auto-push. The agent never merges a pull request.

- No AI or tool authorship in any pull request or comment the agent creates.

## Commit Convention

Commits use the `/commit` skill. Follow `.claude/rules/commit.md`: when no ticket applies, prefix the title with `NOISSUE`. Keep the whole message under 255 characters and omit the body — rationale belongs in the pull request.

In phase 2, make one commit per module and pattern.

## Autonomy

The orchestrator is autonomous for file scanning, artifact writing, tracker updates, and the upgrade and phase branch pushes and pull request creation. Confirmation requirements come from the domain skills — whichever raises a confirmation, the orchestrator surfaces it and pauses. Any push outside the upgrade and phase branches requires confirmation.

Never advance to the next phase without a user request. Each phase ends with the user in control.

## Resuming an Interrupted Phase

When `upgrade-state.md` shows the phase as `in-progress` at invocation, ask whether to resume from where it stopped, restart by reverting to the phase-start commit and rerunning, or abort. Default to resume.

## Output

- Artifacts under `.claude/upgrade-artifacts/<run-id>/phase-<N>-<slug>/`.

- A summary under `upgrade-notes/<run-id>/phase-<N>-<slug>/summary.md`, committed.

- An updated phase tracker row in `upgrade-state.md`.

- Pull requests opened against the upgrade branch, awaiting human review.

- A chat summary of phase results and the next suggested command.
