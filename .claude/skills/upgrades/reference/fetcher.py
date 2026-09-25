#!/usr/bin/env python3
"""
Liferay upgrade reference-data fetcher.

Reads the authoritative mapping files from a local liferay-portal-ee checkout
(required — GitHub fetch is not supported). The files are copied from
<local>/modules/util/source-formatter/src/main/resources/dependencies/ into the
cache under source-id `local:<tag-or-dirname>` and indexed identically.

After copying, automatically generates index_simple.md and index_complex.md
from replacements.json using the integrated indexer.

Files fetched (from modules/util/source-formatter/src/main/resources/dependencies/):
	replacements.json
	imports.txt
	jakarta-transform-dependencies.txt
	jakarta-transform-osgi-contracts.txt
	closeable-type-names.json
	generic-type-names.json

Generated after fetch:
	index_simple.md   — simple patterns (simple_rename, param_change, param_reorder)
	index_complex.md  — complex patterns (class_swap, class_structure, regex_replace, message_only)

Cache layout:
	.claude/cache/reference/<source-id>/
		replacements.json
		imports.txt
		...
		index_simple.md
		index_complex.md
		_source.json    # metadata: requested tag, actual source, fetched-at

<source-id> is:
	local--<label>       e.g. "local--2026.q1.0" (from --local)

Only uses the Python stdlib (json, argparse, pathlib).
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import sys

from pathlib import Path

from typing import Optional

UPSTREAM_PATH = "modules/util/source-formatter/src/main/resources/dependencies"

FILES = [
	"replacements.json",
	"imports.txt",
	"jakarta-transform-dependencies.txt",
	"jakarta-transform-osgi-contracts.txt",
	"closeable-type-names.json",
	"generic-type-names.json",
]

ESSENTIAL_FILES = {
	"replacements.json",
	"imports.txt",
	"jakarta-transform-dependencies.txt",
}

# Index files generated from replacements.json
INDEX_FILES = {
	"index_simple.md",
	"index_complex.md",
}

# ─────────────────────────────────────────────
# Indexer logic (integrated from generate_index.py)
# ─────────────────────────────────────────────

SIMPLE_TYPES = {"simple_rename", "param_change", "param_reorder", "static_method_rename"}
COMPLEX_TYPES = {"class_swap", "class_structure", "regex_replace", "message_only"}

from collections import defaultdict


def _classify_transformation(entry: dict) -> str:
	has_class_structure = entry.get("classStructurePattern", False)
	has_new_reference = "newReference" in entry
	has_params = "from" in entry and "(" in entry.get("from", "")
	has_regex = "from" in entry and entry.get("from", "").startswith("regex:")
	has_message = entry.get("hasMessage", False)
	from_val = entry.get("from", "")
	to_val = entry.get("to", "")

	if has_message:
		return "message_only"
	if has_class_structure:
		return "class_structure"
	if has_new_reference:
		return "class_swap"
	if has_regex:
		return "regex_replace"
	if has_params:
		if to_val and "param#" in to_val:
			params_in_to = [p for p in to_val.split("param#") if p and p[0].isdigit()]
			indices = [int(p.split("#")[0]) for p in params_in_to if "#" in p]
			if indices != sorted(indices):
				return "param_reorder"
		return "param_change"
	if from_val and to_val and "." in from_val:
		return "static_method_rename"
	return "simple_rename"


def _extract_classes(entry: dict) -> list[str]:
	classes = entry.get("classNames", [])
	if not classes:
		from_val = entry.get("from", "")
		if "." in from_val:
			parts = from_val.split(".")
			if parts[0][0].isupper():
				classes = [parts[0]]
		if not classes:
			classes = ["(no specific class)"]
	return classes


def _format_simple_line(entry: dict, transformation_type: str) -> str:
	from_val = entry.get("from", "N/A")
	to_val = entry.get("to", "N/A")
	ticket = entry.get("issueKey", "N/A")
	valid_ext = entry.get("validExtensions", ["java"])
	ext_str = ", ".join(valid_ext)
	return f"- `{from_val}` → `{to_val}` [{ticket}, {transformation_type}, {ext_str}]"


def _format_complex_block(entry: dict, transformation_type: str) -> str:
	lines = []
	from_val = entry.get("from", "")
	to_val = entry.get("to", "")
	ticket = entry.get("issueKey", "N/A")
	valid_ext = entry.get("validExtensions", ["java"])
	new_imports = entry.get("newImports", [])
	new_reference = entry.get("newReference", "")
	new_methods = entry.get("newMethods", [])
	methods_to_format = entry.get("methodsToFormat", [])
	has_message = entry.get("hasMessage", False)
	skip_val = entry.get("skipParametersValidation", False)

	label = from_val if from_val else "(class structure pattern)"
	lines.append(f"#### `{label}`")
	lines.append(
		f"- **ticket:** `{ticket}` | **type:** `{transformation_type}` | "
		f"**extensions:** `{', '.join(valid_ext)}`"
	)
	if from_val:
		lines.append(f"- **from:** `{from_val}`")
	if to_val:
		lines.append(f"- **to:** `{to_val}`")
	if new_imports:
		lines.append(f"- **newImports:** `{', '.join(new_imports)}`")
	if new_reference:
		lines.append(f"- **newReference:** `{new_reference}`")
	if skip_val:
		lines.append(f"- **skipParametersValidation:** true")
	if has_message:
		lines.append(f"- **hasMessage:** true — cannot be fully automated, only sends a warning")
	if new_methods:
		lines.append(f"- **newMethods ({len(new_methods)}):**")
		for m in new_methods:
			lines.append(f"  ```\n  {m}\n  ```")
	if methods_to_format:
		lines.append(f"- **methodsToFormat ({len(methods_to_format)}):**")
		for m in methods_to_format:
			lines.append(f"  - from: `{m.get('from', '')}`")
			lines.append(f"  - to:   `{m.get('to', '')}`")

	return "\n".join(lines)


def _read_target_tag(workspace: Path) -> Optional[str]:
	claude_md = workspace / "CLAUDE.md"
	if not claude_md.exists():
		return None
	for raw in claude_md.read_text().splitlines():
		line = raw.strip().lstrip("-").strip()
		if line.startswith("`upgrade.target.version`"):
			_, _, rest = line.partition(":")
			value = rest.strip().strip("`").strip()
			if value and not value.lower().startswith("todo"):
				return value
	return None


def _safe_id(source_id: str) -> str:
	return source_id.replace(":", "--").replace("/", "-")


def _cache_is_complete(cache_dir: Path) -> bool:
	"""Cache is complete when essential files AND both index files are present."""
	if not cache_dir.is_dir():
		return False
	present = {p.name for p in cache_dir.iterdir()}
	required = ESSENTIAL_FILES | INDEX_FILES | {"_source.json"}
	return required.issubset(present)


def copy_all_local(local_root: Path, target_dir: Path) -> list[str]:
	"""Copy the mapping files from a local liferay-portal checkout into the cache."""
	src_dir = local_root / UPSTREAM_PATH
	if not src_dir.is_dir():
		raise RuntimeError(
			f"local source does not contain '{UPSTREAM_PATH}'.\n"
			f"  expected: {src_dir}\n"
			f"  is '{local_root}' the root of a liferay-portal checkout?"
		)
	target_dir.mkdir(parents=True, exist_ok=True)
	written = []
	for fname in FILES:
		src = src_dir / fname
		if not src.exists():
			print(f"  [warn] {fname} not present in local source; skipping", file=sys.stderr)
			continue
		content = src.read_bytes()
		(target_dir / fname).write_bytes(content)
		written.append(fname)
		print(f"  [ok]   {fname} ({len(content)} bytes)")
	return written


def generate_indexes(cache_dir: Path, source_ref: str) -> None:
	"""
	Read replacements.json from cache_dir and generate index_simple.md
	and index_complex.md in the same directory.
	"""
	replacements_path = cache_dir / "replacements.json"
	if not replacements_path.exists():
		print("  [warn] replacements.json not found; skipping index generation", file=sys.stderr)
		return

	print("  [idx]  generating indexes from replacements.json...")

	with open(replacements_path, "r", encoding="utf-8") as f:
		data = json.load(f)

	if not isinstance(data, list):
		print("  [warn] replacements.json is not a JSON array; skipping index generation",
			  file=sys.stderr)
		return

	simple_by_class: dict = defaultdict(list)
	complex_by_class: dict = defaultdict(list)
	type_counts: dict = defaultdict(int)

	for entry in data:
		t = _classify_transformation(entry)
		type_counts[t] += 1
		for cls in _extract_classes(entry):
			if t in SIMPLE_TYPES:
				simple_by_class[cls].append((entry, t))
			else:
				complex_by_class[cls].append((entry, t))

	# ── index_simple.md ──────────────────────────────────────────────────────
	simple_lines = [
		"# Simple Patterns Index",
		"",
		f"Auto-generated from `replacements.json` at source `{source_ref}`.",
		"Covers: `simple_rename`, `param_change`, `param_reorder`, `static_method_rename`.",
		"",
		"**How to read:** `from` → `to` [ticket, type, extensions]",
		"",
		"**param#x# syntax:** references the original parameter at index x (0-based).",
		"",
		f"Total patterns: **{sum(len(v) for v in simple_by_class.values())}** "
		f"across **{len(simple_by_class)}** classes.",
		"",
		"---",
		"",
	]
	for cls in sorted(simple_by_class.keys()):
		simple_lines.append(f"### `{cls}`")
		simple_lines.append("")
		for entry, t in simple_by_class[cls]:
			simple_lines.append(_format_simple_line(entry, t))
		simple_lines.append("")

	simple_out = cache_dir / "index_simple.md"
	simple_out.write_text("\n".join(simple_lines), encoding="utf-8")
	print(f"  [ok]   index_simple.md ({len(simple_lines)} lines)")

	# ── index_complex.md ─────────────────────────────────────────────────────
	complex_lines = [
		"# Complex Patterns Index",
		"",
		f"Auto-generated from `replacements.json` at source `{source_ref}`.",
		"Covers: `class_swap`, `class_structure`, `regex_replace`, `message_only`.",
		"",
		"**Types:**",
		"- `class_swap`: method moved to a different class — requires newImports and newReference",
		"- `class_structure`: changes to class declarations, method signatures, or new methods to add",
		"- `regex_replace`: pattern matched via regex instead of plain string",
		"- `message_only`: cannot be automated — SF only sends a warning message",
		"",
		f"Total patterns: **{sum(len(v) for v in complex_by_class.values())}** "
		f"across **{len(complex_by_class)}** classes.",
		"",
		"---",
		"",
	]
	for cls in sorted(complex_by_class.keys()):
		complex_lines.append(f"### `{cls}`")
		complex_lines.append("")
		for entry, t in complex_by_class[cls]:
			complex_lines.append(_format_complex_block(entry, t))
			complex_lines.append("")

	complex_out = cache_dir / "index_complex.md"
	complex_out.write_text("\n".join(complex_lines), encoding="utf-8")
	print(f"  [ok]   index_complex.md ({len(complex_lines)} lines)")

	print("  [idx]  breakdown:")
	for t, count in sorted(type_counts.items(), key=lambda x: -x[1]):
		category = "simple" if t in SIMPLE_TYPES else "complex"
		print(f"           [{category}] {t}: {count}")


# ─────────────────────────────────────────────
# Entry point
# ─────────────────────────────────────────────

def main() -> int:
	parser = argparse.ArgumentParser(description="Fetch Liferay reference mapping files from a local liferay-portal-ee checkout.")
	parser.add_argument(
		"--local", required=True,
		help=(
			"Root of a local liferay-portal-ee checkout (e.g. .../liferay-portal-ee). "
			"Reads the mapping files from disk. Required."
		),
	)
	parser.add_argument(
		"--tag", default=None,
		help="Cache label (e.g., 2026.q1.0). If omitted, read from CLAUDE.md; falls back to the checkout directory name.",
	)
	parser.add_argument(
		"--cache-dir", default=".claude/cache/reference",
		help="Cache root. Default: .claude/cache/reference",
	)
	parser.add_argument(
		"--workspace-root", default=".",
		help="Workspace root (where CLAUDE.md lives). Default: cwd.",
	)
	parser.add_argument(
		"--force", action="store_true",
		help="Bypass the cache and re-copy.",
	)
	parser.add_argument(
		"--skip-index", action="store_true",
		help="Copy files only, skip index generation (not recommended).",
	)
	args = parser.parse_args()

	workspace = Path(args.workspace_root).resolve()
	cache_root = (workspace / args.cache_dir).resolve()

	local_root = Path(args.local).expanduser().resolve()
	if not local_root.is_dir():
		print(f"error: --local path not found or not a directory: {local_root}",
			  file=sys.stderr)
		return 2
	if not (local_root / UPSTREAM_PATH).is_dir():
		print(
			f"error: --local path does not look like a liferay-portal checkout.\n"
			f"  expected: {local_root / UPSTREAM_PATH}\n"
			f"  point --local at the repository root.",
			file=sys.stderr,
		)
		return 2

	requested_tag = args.tag or _read_target_tag(workspace)
	label = requested_tag or local_root.name
	source_id = f"local:{label}"
	cache_dir = cache_root / _safe_id(source_id)

	if _cache_is_complete(cache_dir) and not args.force:
		print(f"Cache hit for {source_id}; skipping copy (use --force to re-copy).")
		record_to_upgrade_state(workspace, label, label, "local", str(local_root))
		return 0

	print(f"Copying from local checkout {local_root} (label: {label})...")
	copy_all_local(local_root, cache_dir)

	if not args.skip_index:
		generate_indexes(cache_dir, label)
	write_source_metadata(cache_dir, label, label, "local", str(local_root))
	record_to_upgrade_state(workspace, label, label, "local", str(local_root))
	return 0


def write_source_metadata(
	cache_dir: Path, requested_tag: str, actual_ref: str, actual_kind: str,
	local_path: Optional[str] = None,
) -> None:
	payload = {
		"requested_tag": requested_tag,
		"actual_ref": actual_ref,
		"actual_kind": actual_kind,
		"fetched_at": dt.datetime.now(dt.timezone.utc).isoformat(),
	}
	if local_path is not None:
		payload["local_path"] = local_path
	(cache_dir / "_source.json").write_text(json.dumps(payload, indent=2) + "\n")


def record_to_upgrade_state(
	workspace_root: Path, requested_tag: str, actual_ref: str, actual_kind: str,
	local_path: Optional[str] = None,
) -> None:
	state_file = workspace_root / "upgrade-state.md"
	if not state_file.exists():
		return
	marker = "## Reference data source"
	content = state_file.read_text()
	entry_lines = [
		f"- Requested tag: `{requested_tag}`",
		f"- Actual source served: `{actual_kind}:{actual_ref}`",
	]
	if local_path is not None:
		entry_lines.append(f"- Local checkout: `{local_path}`")
	entry_lines += [
		f"- Fetched at: `{dt.datetime.now(dt.timezone.utc).isoformat()}`",
		f"- Indexes generated: `index_simple.md`, `index_complex.md`",
	]
	entry = "\n".join(entry_lines) + "\n"

	if marker in content:
		pre, _, rest = content.partition(marker)
		lines = rest.splitlines(keepends=True)
		i = 1
		while i < len(lines) and not lines[i].startswith("## "):
			i += 1
		new_section = marker + lines[0] + "\n" + entry + "\n"
		state_file.write_text(pre + new_section + "".join(lines[i:]))
	else:
		state_file.write_text(content.rstrip() + f"\n\n{marker}\n\n{entry}\n")


if __name__ == "__main__":
	sys.exit(main())
