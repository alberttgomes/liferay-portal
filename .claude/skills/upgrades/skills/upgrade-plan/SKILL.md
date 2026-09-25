---

name: upgrade-plan
description: Run this skill when the user invokes `/upgrade-plan`, after `/upgrade-init` has completed. This skill performs deep inventory of the Liferay workspace — cataloging every OSGi module, theme, fragment, Service Builder project, JSP override, custom JS file, and FreeMarker template — then produces a concrete per-phase plan tailored to what it found and writes that plan into upgrade-state.md. Trigger also when the user asks "analyze this workspace for upgrade", "what needs to change", "scope the upgrade", or wants a pre-flight audit before touching any code.

---

# /upgrade-plan

Deep workspace inventory and per-phase planning. Read-only — never modifies the codebase. Produces a plan the subsequent `/upgrade-phase N` commands execute.

## Prerequisites

- `/upgrade-init` has been run; `CLAUDE.md` is populated (no TODO values on required fields).
- `/upgrade-refresh-references` has been run at least once (fetcher cache is populated).
- The upgrade branch is checked out.

## Gotchas

- Do not apply any replacements. This is a scan phase only.
- Do not assume the workspace is greenfield. If the source version shows any pre-existing partial upgrade (some modules already on 7.4-style code), call that out explicitly.
- Do not omit empty categories. If the workspace has zero themes, say "themes: 0" rather than leaving the section blank.

## Workflow

1. **Read `CLAUDE.md`** for workspace identity, layout, and behavior overrides.

1. **Read the reference data source metadata.** From `.claude/cache/reference/<source-id>/_source.json`, confirm which tag or master SHA the upgrade will be working against. Record it in `upgrade-state.md`.

1. **Inventory modules.** For each directory under `modules.root`, classify: OSGi service, JSP override module, fragment bundle, portlet (Java), portlet (JS-heavy: React / MetalJS / AUI / other), Service Builder project, JSP hook, theme module, other. Record the count by category.

1. **Inventory themes.** For each directory under `themes.root`, classify: Styled, Unstyled, Classic parent, Client Extension candidate. Note Clay version references.

1. **Inventory client extensions.** Existing CX count; their types (themes, object definitions, custom fields, IFrame, static content, etc.). Flag any modules that look like good CX migration candidates (but don't force).

1. **Scan for deprecated-API density.** For each module, count matches against the cached `replacements.json` without applying anything. This tells `/upgrade-phase 2` (Fix Compile) which modules will have the most churn.

1. **Scan for kernel-split imports.** Against cached `imports.txt`. Most 7.3 codebases have dozens per module.

1. **Scan for `javax.*` imports** in modules that would cross the Jakarta boundary. Record package-level counts (the Jakarta migration runs within Phase 2).

1. **Scan JS sources.** Count AUI usages (`AUI().use`, `A.one`, `A.all`, `aui:script` taglibs in JSPs), MetalJS imports (`metal-*` package imports), Clay 2/3 CSS class uses, `liferay-npm-bundler` 1.x configuration.

1. **Scan JSP overrides.** For each JSP override module, identify the target JSPs and check against the cached Liferay sources at the target tag whether any of them moved, renamed, or changed structurally. Flag the risky ones.

1. **Produce the plan.** Write a plan section into `upgrade-state.md` covering, per phase:
    - What the phase will do on this workspace specifically.
    - Which modules it touches, and their type.
    - Estimated risk (low/medium/high) based on the scan.
    - Items pre-flagged for human review.

1. **Establish the run-id.** `<YYYYMMDD-HHMM>-<branch-name-slug>`. Record under `Artifact manifest` in `upgrade-state.md`. `/upgrade-phase` reuses this value rather than minting its own.

1. **Summarize to the user.** Short summary in chat: inventory counts, the top 5 highest-risk items, and a recommendation on whether to proceed normally or address specific blockers first. Point to the per-phase plan in `upgrade-state.md`; no CSV is emitted.

## Output

In `upgrade-state.md`:

- Populate the **Workspace inventory** section.
- Populate per-phase rows in the **Phase tracker** (keep status `pending`, but add the scoped targets as a note).
- Append any new findings to the **Decision log** as planning-phase entries.
- Populate **Flagged for human review** with items surfaced by the scan.
- Record the run-id under **Artifact manifest**.

In chat: a succinct summary, concluding with the next command (`/upgrade-phase 1`).

## Autonomy

Fully autonomous — this is a read-only command (scan + write of plan artifacts only; no source-tree modifications). No confirmations needed.
