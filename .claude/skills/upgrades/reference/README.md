# `.claude/reference/`

Shared reference data and utilities for the upgrade skills. Contents:

## `fetcher.py`

Script that copies authoritative Liferay mapping files from a local
`liferay-portal-ee` checkout. Invoked by `/upgrade-refresh-references`. See the
top-of-file docstring for full usage; the common invocations are:

```bash
# Copy from a local liferay-portal-ee checkout (required)
python3 .claude/reference/fetcher.py --local /home/you/dev/liferay-portal-ee

# Override the cache label (defaults to upgrade.target.version in CLAUDE.md)
python3 .claude/reference/fetcher.py --local /home/you/dev/liferay-portal-ee --tag 2026.q1.0

# Force re-copy (bypass cache)
python3 .claude/reference/fetcher.py --local /home/you/dev/liferay-portal-ee --force
```

Output goes to `.claude/cache/reference/<source-id>/` (gitignored). See the section below on cache layout.

The fetcher uses only Python stdlib and requires no network access.

### How it works

Point `--local` at the root of a `liferay-portal-ee` checkout and the fetcher
copies the mapping files from
`<local>/modules/util/source-formatter/src/main/resources/dependencies/`
into the cache. The tag (from `--tag` or `CLAUDE.md`) is used only as the
cache label; if absent, the checkout's directory name is used. The result is
cached under source-id `local:<label>` (cache dir `local--<label>/`) and
indexed automatically. `_source.json` records `"actual_kind": "local"` and the
absolute `local_path` it was copied from, and the same is reflected in
`upgrade-state.md`.

## `custom-replacements.json` (optional, workspace-provided)

Team-specific additions to `replacements.json`, same schema. Applied after upstream, with team-wins precedence on conflicts. Use for:

- Internal APIs that were renamed.
- Organization-wide naming conventions.
- Workspace-specific shims.

## `custom-imports.txt` (optional, workspace-provided)

Team-specific additions to `imports.txt`, same `old=new` format. Same precedence rule.

## `skipped-replacements.json` (optional, workspace-provided)

Entries from upstream `replacements.json` to explicitly skip for this workspace. Schema:

```json
[
  {"issueKey": "LPD-7748", "reason": "our codebase uses a shim that handles this"},
  {"issueKey": "LPS-186809", "reason": "already migrated in 2024 by the commerce team"}
]
```

Skills read this before applying upstream and omit the listed entries.

## Cache layout (at `.claude/cache/reference/`)

Created by the fetcher; gitignored.

```
.claude/cache/reference/
├── local--2026.q1.0/
│   ├── _source.json
│   ├── closeable-type-names.json
│   ├── generic-type-names.json
│   ├── imports.txt
│   ├── jakarta-transform-dependencies.txt
│   ├── jakarta-transform-osgi-contracts.txt
│   ├── replacements.json
│   ├── index_simple.md
│   └── index_complex.md
```

`_source.json` records the requested tag, local checkout path, and copy timestamp. Skills read this to know which ruleset they're working against and to warn when the label doesn't match `upgrade.target.version` in `CLAUDE.md`.

Each label is cached independently. Switching the target version (e.g., re-targeting from `2026.q1.0` to `2026.q1.1`) does not invalidate the old cache; the skill uses whichever source-id is current in `upgrade-state.md`.