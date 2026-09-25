---

name: upgrade-refresh-references
description: Run this skill when the user invokes `/upgrade-refresh-references` to copy or refresh the authoritative Liferay mapping files (replacements.json, imports.txt, jakarta-transform-dependencies.txt, and siblings) from a local liferay-portal-ee checkout, and automatically generate index_simple.md and index_complex.md from the copied replacements.json. This skill wraps the fetcher at `.claude/reference/fetcher.py` — requires `--local <path>` pointing at a liferay-portal-ee checkout, uses the target tag from CLAUDE.md as the cache label, caches by source-id under `.claude/cache/reference/`, generates the pattern indexes in the same cache directory, and records which source was used in upgrade-state.md. Trigger also when the user says "update the reference data", "copy the latest mappings", "regenerate the indexes", "use my local liferay-portal for the references", or when a domain skill reports that its cache is empty.

---

# /upgrade-refresh-references

Copy or refresh the Liferay upstream mapping files from a local liferay-portal-ee
checkout and regenerate the pattern indexes used by the upgrade automation skills.

## What it copies

From a local `liferay-portal-ee` checkout: `replacements.json`, `imports.txt`,
`jakarta-transform-dependencies.txt`, `jakarta-transform-osgi-contracts.txt`,
`closeable-type-names.json`, `generic-type-names.json`.

It then generates two indexes from `replacements.json`, partitioned by pattern type —
`index_simple.md` (simple_rename, param_change, param_reorder, static_method_rename) and
`index_complex.md` (class_swap, class_structure, regex_replace, message_only).

## Gotchas

- **`--local <path>` is required; there is no remote mode.** Both the skill name and `fetcher.py`
  imply a download — nothing is fetched over the network.
- **The path must be the repo root**, the one containing `modules/util/source-formatter/…`. Pointing
  at the source-formatter directory itself is the natural guess and exits `2`.
- **A partial cache regenerates even without `--force`.** If any expected artifact is missing the
  fetcher re-copies and re-indexes, so a cache "hit" is not a guarantee that nothing ran.
- **`<source-id>` is a label, not a portal tag or commit** — it is the target version, else the
  checkout directory name. Resolve it from `upgrade-state.md` rather than deriving or globbing it.

## Prerequisites

- A local `liferay-portal-ee` checkout accessible on disk (the `--local` path must point at the repo root containing `modules/util/source-formatter/...`).
- `CLAUDE.md` exists and has `upgrade.target.version` set (used as the cache label; if absent, the checkout directory name is used).

## Workflow

1. **Parse arguments.** Common ones:
   - `--local <path>` — **required.** Root of the local `liferay-portal-ee` checkout to read files from.
   - `--tag <tag>` — override the cache label (defaults to `upgrade.target.version` in `CLAUDE.md`, then checkout directory name).
   - `--force` — bypass cache, re-copy and regenerate indexes.
   - `--skip-index` — copy files only, skip index generation (not recommended).

1. **Invoke the fetcher.** Run `python3 .claude/reference/fetcher.py --local <path>` with the parsed arguments, from the workspace root.

1. **Interpret the exit code.**
   - `0` — success. Tell the user which source was served, which files were written, and confirm indexes were generated with pattern counts.
   - `2` — `--local` path is missing, not a directory, or isn't a liferay-portal checkout. Tell the user to correct the path (it must point at the repo root containing `modules/util/source-formatter/...`).

1. **Confirm state-file update.** Read `upgrade-state.md` and confirm the "Reference data source" section reflects what the fetcher just wrote, including the indexes generated line.

1. **Report to user.** After a successful run, summarize:
   - Source served (local checkout path + label)
   - Files written (list)
   - Index stats: total simple patterns, total complex patterns, total classes indexed
   - Cache directory path

## Cache completeness

The cache is considered complete only when ALL of the following are present:
- `replacements.json`, `imports.txt`, `jakarta-transform-dependencies.txt`
- `index_simple.md`, `index_complex.md`
- `_source.json`

If any of these are missing, the fetcher will re-copy and regenerate — even without `--force`.

## Where the upgrade skill reads the indexes from

The upgrade automation skill (`SKILL.md`) reads indexes from:

```
.claude/cache/reference/<source-id>/index_simple.md
.claude/cache/reference/<source-id>/index_complex.md
.claude/cache/reference/<source-id>/imports.txt
```

The active `<source-id>` is recorded in `upgrade-state.md` under "Reference data source".
The upgrade skill must resolve the active source-id from `upgrade-state.md` before reading the indexes.

## Autonomy

Fully autonomous.

## When nothing is needed

If the cache is already complete for the requested source-id and `--force` is not set, the fetcher reports cache-hit and exits 0 quickly. It's safe to run this command before starting any phase — it's a no-op when the cache is fresh.

## When to force a refresh

Run with `--force` when:
- The target Liferay version was updated in `CLAUDE.md`
- The local liferay-portal-ee checkout was updated to a newer commit
- The indexes seem stale or out of sync with the actual replacements

