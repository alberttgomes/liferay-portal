---

argument-hint: "[target-version]"
description: Configure a Liferay workspace for the upgrade agent. Use when the user invokes `/upgrade-init` or asks to set up, configure, or onboard a workspace for an upgrade. Runs headless, without questions, when a run settings file is present.
name: upgrade-init

---

# Initialize a Workspace for Upgrade

One-shot interactive setup of a Liferay workspace. Runs once per workspace and is idempotent — re-running updates fields without destroying progress. With a run settings file present it runs headless instead; see [Headless Mode](#headless-mode).

## Prerequisites

The workspace root must have `CLAUDE.md` and `upgrade-state.md` (both copied from their templates at install time), an initialized Git repository, and a Liferay Workspace `gradle.properties`. Stop and name what is missing rather than scaffolding it.

## Gotchas

- `CLAUDE.md` and `upgrade-state.md` are the agent's working files, not deliverables. Keep them out of the upgrade branch's commits.

- Write a concrete resolved patch version, never a range. `20.x` and `8.x` look reasonable in a config field, but Phase 1 reads **node.version** and **search.version** verbatim to build a Docker image tag, so a range produces a broken tag.

- The `<old-version>` branch is never a pull request target. The upgrade branch is cut from it, which makes it look like the merge base, but it is a backup of the previous version and stays untouched.

- `/upgrade-plan` is not the next command — `/upgrade-phase 1` is. Phase 2 runs `/upgrade-refresh-references` and `/upgrade-plan` itself, so the user never invokes them by hand.

## Headless Mode

The portal's upgrade runner and `/upgrade-run` drive this skill with nobody at the keyboard. Headless mode is on when the `UPGRADE_RUN_SETTINGS` environment variable names a file, or when `upgrade-run.properties` exists at the workspace root. The file holds one `key=value` line per `CLAUDE.md` field, for example `upgrade.target.version=2026.q1.0`.

Headless mode changes these rules and nothing else:

- Never ask. Every value comes from the settings file, from the workspace inference in the workflow below, or stays `TODO`. Print each resolved value in the report instead of asking for confirmation.

- The settings file wins over inference for any field it names.

- These keys are required: `customer.name`, `db.target.type`, `db.target.version`, `node.version`, `search.version`, `upgrade.source.version`, `upgrade.target.java.version`, and `upgrade.target.version`. A missing one is a blocking validation failure. Report it and stop before writing anything.

- Skip the compatibility matrix lookup for **search.version**. The file supplies the value.

- Do not push the upgrade branch. Create it, or check it out when it already exists, locally only. Publishing is the caller's step.

- On re-run, take the first choice, updating specific fields, without asking.

- Keep `upgrade-run.properties` out of every commit, like `CLAUDE.md` and `upgrade-state.md`.

The validation checks below still run and are still reported. Whether a failed one blocks the run is the caller's decision.

## Workflow

### Read the Workspace README

Look for `README.md`, `README.txt`, or `README.adoc` and parse it for portal start and stop commands, the portal URL, credential references, database connection hints, and Docker service names. Propose whatever is extracted before writing it to `CLAUDE.md` — never silently trust parsed content.

### Inspect the Workspace Shape

Read `gradle.properties` (especially **liferay.workspace.product**) and `settings.gradle`, then walk `modules/`, `themes/`, and `client-extensions/` to count artifacts. These counts seed `upgrade-state.md`.

### Inspect Docker Compose

When `docker-compose.yml` is present, extract service names, image tags, exposed ports, and bind mounts to pre-fill the `docker.*` fields. Flag any mismatch between **liferay.workspace.product** and the Liferay service image tag.

Derive **portal.deploy.folder** from the host side of the bind mount whose container target is `/opt/liferay/osgi/modules`, falling back to `/opt/liferay/deploy` and then to the Liferay Workspace default `bundles/osgi/modules`. Prompt only when neither a compose mount nor the default directory exists.

### Gather the Remaining Fields

Ask for what cannot be inferred, one question at a time or in small logical groups, using selectable options wherever the field is a choice. Pre-fill the auto-detected or recommended value as the default so the user usually just confirms. Never dump every field into one free-text prompt.

Ask in this order:

1. Source version — pre-filled from **liferay.workspace.product**; confirm only when ambiguous.

1. Target version, for example `2026.q1.0`.

1. Target Java version.

1. **node.version** — pre-fill a concrete known-good patch for the target era, for example `20.18.0` for 2024.Q4 and later.

1. Target database type and version, which populate **db.target.type** and **db.target.version**. Ask whether the type stays or changes.

1. **search.version** — ask only after the target version is set, because the recommendation depends on it. Fetch Liferay's compatibility matrix and offer the concrete recommended LTS, for example `8.17.4`, as the default. Fall back to a plain prompt when the fetch fails offline. Phase 4 `upgrade-reindex` reuses this lookup.

Reserve free-text answers for fields with no natural options.

### Write CLAUDE.md

Replace every `TODO:` field with the gathered value and preserve any user-added sections. Never inline passwords or credentials.

### Create the Upgrade Branch

Cut `upgrade/<source>-to-<target>` from the current old-version branch, or check it out and resume when it already exists. Push it to origin with `git push -u origin upgrade/<source>-to-<target>`. Leave the old-version branch untouched.

Do not stage or commit `CLAUDE.md` or `upgrade-state.md` on this branch.

### Initialize upgrade-state.md

Fill in source, target, branch, and started-at. Leave the phase tracker empty — `/upgrade-plan` populates it.

### Validate

Confirm each of the following and report it as passing or failing:

- The `--local` path for `/upgrade-refresh-references` is known.

- Docker Compose is available.

- The deploy folder exists.

This is a one-shot gate, not a loop. List the failing checks as blocking items rather than retrying.

## Autonomy Boundaries

Reading files, parsing the README and compose file, writing confirmed values to `CLAUDE.md`, and creating the upgrade branch are autonomous. Every value written to `CLAUDE.md` must be shown and confirmed first. Never inline credentials, never push anything other than the upgrade branch, and never modify files outside the workspace root and `.claude/`.

## On Re-Run

When `CLAUDE.md` already holds non-`TODO` values, offer three choices and default to the first: update specific fields interactively, re-initialize fully after backing the file up to `CLAUDE.md.backup-<timestamp>`, or abort.

## Output

Report which fields were auto-detected and which were asked, the upgrade branch name, and any validation failure that blocks the next step.

The next command is `/upgrade-phase 1`. Mention `/upgrade-plan` as an optional scope review — never as the main next step. In headless mode the caller is `/upgrade-run`, so name no next command.
