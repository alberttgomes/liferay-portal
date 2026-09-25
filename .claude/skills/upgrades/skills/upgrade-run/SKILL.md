---

argument-hint: "[first-phase]"
description: Run the upgrade playbook unattended from a terminal, from `/upgrade-init` through phase 4, stopping at the first blocked phase. Use when the user invokes `/upgrade-run`.
name: upgrade-run

---

# Run the Upgrade Unattended

Chain `/upgrade-init` and `/upgrade-phase` 1 to 4 in one sitting, with nobody answering questions, so a person can let a prepared workspace run through on its own. The portal's upgrade runner does not call this skill; it invokes `/upgrade-init` and each `/upgrade-phase` as separate processes and reads the phase tracker between them.

## Prerequisites

- `upgrade-run.properties` at the workspace root, or the `UPGRADE_RUN_SETTINGS` environment variable naming it. It is the answer sheet for `/upgrade-init`. Without it there is nothing to answer with — stop and say to run `/upgrade-init` interactively, then `/upgrade-phase`.

- `CLAUDE.md` and `upgrade-state.md` at the workspace root, copied from the templates. The runner installs them; a person copies them from `templates/`.

## Gotchas

- This skill is the user's standing request to advance. It overrides `/upgrade-phase`'s rule of ending each phase with the user in control; that rule exists for interactive use.

- Nothing is pushed. The upgrade branch and the phase branches stay local. Publishing is the runner's step, after this skill returns.

- Phase 5 is deferred. Never invoke it.

- The result file tells whoever started this skill how it ended. Write it before returning, including when stopping early.

- Keep `upgrade-run.properties` and `upgrade-run-result.properties` out of every commit, like `CLAUDE.md` and `upgrade-state.md`.

## Workflow

### Resolve the First Phase

Take the first phase from the argument, defaulting to 1. When it is greater than 1, the tracker in `upgrade-state.md` must show every earlier phase `complete`. Otherwise stop with status `blocked` and say which phase is not.

### Initialize the Workspace

Run `/upgrade-init`. It detects the settings file and runs headless. Skip this step only when the first phase is greater than 1 and `CLAUDE.md` already holds non-`TODO` values.

A missing required key is a blocking failure: write the result with status `blocked` and phase `0`, then stop. Report the other validation checks and continue.

### Run the Phases

For each phase from the first one through 4, in order:

1. Run `/upgrade-phase <N>`. It detects the settings file and runs headless.

1. Read the phase's tracker row in `upgrade-state.md`.

1. When the status is `complete`, continue with the next phase.

1. Otherwise stop. The status is whatever the phase recorded, usually `blocked`.

### Write the Result

Write `upgrade-run-result.properties` at the workspace root with these keys, one per line:

- `status` — `complete` when phase 4 completed, otherwise `blocked`.

- `phase` — the last phase attempted, or `0` when initialization stopped the run.

- `message` — one line saying what happened, for example `Phase 2 is blocked: the --local reference path is not known`.

### Summarize

One short paragraph: which phases completed, where the run stopped and why, and where the blocking items are recorded.

## Output

- `upgrade-run-result.properties` at the workspace root.

- The phase tracker in `upgrade-state.md`, updated by each phase.

- The artifacts and summaries each phase writes.
