# `replacements.json` — annotated schema reference

This file is the canonical reference for interpreting Liferay's source-formatter `replacements.json`. Read this when `SKILL.md` isn't enough detail for a specific entry.

Upstream path: `modules/util/source-formatter/src/main/resources/dependencies/replacements.json` in `liferay/liferay-portal`.

Cached locally at: `.claude/cache/reference/<source-id>/replacements.json`.

---

## Entry taxonomy

There are four kinds of entries. The skill dispatches to a different handler for each.

### 1. Method-signature replacement (most common, ~90% of entries)

```json
{
	"classNames": [
		"AccountGroupLocalService",
		"AccountGroupLocalServiceUtil",
		"AccountGroupService",
		"AccountGroupServiceUtil"
	],
	"from": "fetchByExternalReferenceCode(long paramName, String paramName)",
	"issueKey": "LPD-7748",
	"to": "fetchAccountGroupByExternalReferenceCode(param#1#, param#0#)"
}
```

Interpretation:

- The call site must be a method call on an instance or static reference of one of the four `classNames`.
- The `from` signature uses `paramName` as a literal placeholder — all parameters are named `paramName`. Distinguish them by position (`param#0#`, `param#1#`) in the `to`.
- Parameter types in `from` (`long`, `String`) must match the actual call-site argument types for the replacement to apply.
- `to` can reorder parameters (here: the original `(long, String)` becomes `(String, long)`), rename the method (here: `fetchByExternalReferenceCode` → `fetchAccountGroupByExternalReferenceCode`), or do both.

Autonomy note: parameter reorders with type-distinct arguments (here `long` vs `String`) are safe to apply autonomously. Reorders where two or more arguments share the same type (e.g. `(long, long)`) require human confirmation.

### 2. Regex replacement

```json
{
	"from": "regex:serviceLocator[\\n\\s]*\\.[\\n\\s]*findService\\([\\n\\s]*\"com\\.liferay\\.dynamic\\.data\\.mapping\\.storage\\.StorageEngine\"\\)",
	"issueKey": "LPD-7762",
	"skipParametersValidation": true,
	"to": "staticUtil[\"com.liferay.dynamic.data.mapping.kernel.StorageEngineManagerUtil\"]",
	"validExtensions": [
		"ftl"
	]
}
```

Interpretation:

- `from` starts with `regex:` — treat the rest as a Java regex (source-formatter uses Java's regex engine; Python's `re` is compatible for most cases but has subtle differences around lookbehind and possessive quantifiers).
- `skipParametersValidation: true` tells the normal parameter-type check to stand down.
- `to` can reference named captures as `${name}` (see structural examples below).
- `validExtensions` restricts which file types the regex applies to.

Autonomy: apply only when the regex is simple enough that the diff is human-readable. For multi-line captures with reordering, flag for confirmation.

### 3. Structural class pattern

```json
{
	"classNames": [
		"BaseModelListener"
	],
	"classStructurePattern": true,
	"issueKey": "LPD-7865",
	"methodsToFormat": [
		{
			"from": "regex:(?<methodDeclaration>public\\s*void\\s*(onAfterUpdate|onBeforeUpdate))\\((?<paramType>[\\S]*)\\s*(?<paramName>[\\S]*)\\)",
			"to": "${methodDeclaration}(${paramType} original${paramName}, ${paramType} ${paramName})"
		}
	]
}
```

Interpretation:

- Applies only when the file's class extends/implements one of `classNames`.
- For each entry in `methodsToFormat`, apply the `from`/`to` pair within that class.
- Named regex captures (`?<name>`) are referenced as `${name}` in `to`.

Autonomy: almost always requires confirmation. Structural rewrites touch method signatures and can silently change the class contract.

### 4. Symbol-only replacement

```json
{
	"from": "themeDisplay.getPortletGroupId",
	"issueKey": "LPS-186809",
	"to": "themeDisplay.getScopeGroupId",
	"validExtensions": [
		"ftl",
		"jsp",
		"jspf"
	]
}
```

Interpretation:

- No `classNames`; apply wherever the literal `from` string appears.
- `validExtensions` restricts scope.

Autonomy: safe to apply autonomously for most renames (like this one — `getPortletGroupId` and `getScopeGroupId` are long-established equivalents). Double-check when the rename is in a fast-moving area (commerce, workflow).

---

## Field reference

### `classNames`

Array of strings. For method-signature and structural entries, identifies the receiver types. Match logic:

- Instance calls: resolve the variable's declared type (or inferred type on `var` / lambda); match against any entry in the array.
- Static calls: match the static class name directly.
- Chained calls: match against the type of the expression before the method. For `foo.getBar().doStuff()`, the `classNames` check runs against the return type of `getBar()`.

Missing `classNames` → the entry is symbol-only or regex-only; apply without type resolution.

### `from` and `to`

Strings. `from` is the source pattern; `to` is the replacement. Special prefixes and placeholders:

- `regex:` prefix on `from` → treat the remainder as a regex.
- `param#N#` in `to` → positional placeholder referencing the Nth parameter of the matched signature (0-indexed).
- `${name}` in `to` → named capture reference (regex variants and `methodsToFormat`).
- `param#N#s` (note the trailing `s`) → used when `to` wraps the parameter in an array/collection, as in `new long[] {param#1#s}`. Interpret the `s` as "wrap in literal syntax"; see examples in the file.

### `newImports`

Array of fully-qualified Java import paths. When the entry applies, ensure these imports are present in the file. Insert in the appropriate import group following Liferay source-formatter ordering.

### `newMethods`

Array of method declarations to add to the class if the entry's replacement references a method that wouldn't otherwise exist. Usually accompanies `classStructurePattern: true`. Position the new method in the class after the existing method of the same visibility and category; if ambiguous, flag for human confirmation.

### `newReference`

A new field or static reference to add. Rare. Usually paired with structural patterns.

### `removeImplements`

Interface to remove from the class's `implements` list. Behavior change — always confirm before applying.

### `methodsToFormat`

Used with `classStructurePattern: true`. An array of `{from, to}` sub-entries, applied within the matched class.

### `classStructurePattern`

Boolean. `true` means match on class structure; requires `methodsToFormat`.

### `skipParametersValidation`

Boolean. `true` means don't run the type/count check that normally guards parameter-rewrite entries. Typical for regex entries where the normal "parse the signature, count parameters" logic doesn't apply.

### `hasMessage`

Boolean. `true` indicates the entry is warning-only — the source-formatter emits a message rather than applying a rewrite. The skill treats these as "flag for human review" and surfaces the issue key.

### `validExtensions`

Array of file extensions, without leading dots. Seen values: `java`, `jsp`, `jspf`, `ftl`, `scss`. Absent means `java`-only by default. When present and non-empty, the entry applies only to files with those extensions.

### `issueKey`

Liferay JIRA reference. `LPS-*` prefix = legacy (pre-quarterly), `LPD-*` = modern (post-quarterly-releases). Used for audit trail and optional era filtering.

---

## Parameter placeholders in detail

`param#N#` expands to the Nth argument at the call site, preserving the original expression. So for:

```java
service.copyCPDefinition(groupId, definitionId);
```

with the entry

```json
{
	"classNames": [
		"CPDefinitionLocalService",
		"..."
	],
	"from": "copyCPDefinition(long paramName, long paramName)",
	"newImports": [
		"com.liferay.portal.kernel.workflow.WorkflowConstants"
	],
	"to": "copyCPDefinition(param#0#, param#1#, WorkflowConstants.STATUS_DRAFT)"
}
```

the call site becomes:

```java
service.copyCPDefinition(groupId, definitionId, WorkflowConstants.STATUS_DRAFT);
```

Note: the original arguments `groupId` and `definitionId` are preserved verbatim, and a new argument is appended. The import is added.

---

## Era filtering (optional, off by default)

When the workspace's `CLAUDE.md` sets `issue-key-era-filter = on`, the skill only applies entries whose `issueKey` era is relevant to the upgrade window:

- Source version < 7.4 → apply both `LPS-*` and `LPD-*` entries (legacy era still live until the DXP 2023+ cutover).
- Source version ≥ 7.4 → apply only `LPD-*` entries (legacy `LPS-*` replacements are assumed already applied).

Era filtering is a performance optimization, not a correctness mechanism. The idempotence check (step 3 in `SKILL.md` workflow) catches already-applied entries regardless.

---

## Known gotchas

- **`paramName` as a placeholder looks weird.** Don't be thrown by `long paramName, long paramName` in `from` — it's a convention in this file for "any name, positional". Real call-site variable names (like `groupId`, `definitionId`) replace these at apply time via `param#0#`, `param#1#`, etc.
- **Entries can target multiple `classNames` including the `Util` variant.** E.g., `AccountGroupLocalService` and `AccountGroupLocalServiceUtil` are typically both listed. Match against the call site's resolved type, whichever one it is.
- **Some entries are one-way.** `LPD-*` entries generally only apply when upgrading forward. The skill does not attempt rollback; reverse-application is not supported.
- **Entries targeting future versions can appear on `master`.** If the fetch source is `master` rather than the target tag, some entries may reference `to`-side symbols that don't exist in the user's runtime. The skill's resolution check (step 5 in `SKILL.md`) catches this; surfaces a warning pointing to `upgrade-state.md` for the source record.