#!/usr/bin/env python3
"""PreToolUse guard for Bash calls.

Hard-blocks three things the team forbids, regardless of what the conversation
asked for:
  1. Any direct database-client invocation (Claude must never touch a DB).
  2. Any `git commit` carrying a forbidden attribution trailer.
  3. Any `git commit` labeled `#automation` whose subject names an identifier that
     IS in the upstream catalog (a catalog HIT) or in known-manual-changes.md (a
     documented known-manual-change) — both are changes we already know, so neither
     is `#automation` (deprecated-api-remediation §3g). The reason names the match.

Registered in .claude/settings.json under hooks.PreToolUse with matcher "Bash".
Reads the PreToolUse payload on stdin and, to block, prints a deny decision to
stdout and exits 0 (the preferred form — the reason is fed back to Claude).
A human can still run any command themselves via the `!` prefix, which is a
user action and does not trigger this hook.
"""
import glob
import json
import os
import re
import sys


def deny(reason):
    print(json.dumps({"hookSpecificOutput": {
        "hookEventName": "PreToolUse",
        "permissionDecision": "deny",
        "permissionDecisionReason": reason,
    }}))
    sys.exit(0)


data = json.load(sys.stdin)
if data.get("tool_name") != "Bash":
    sys.exit(0)
cmd = (data.get("tool_input") or {}).get("command", "")

# (1) Block ALL direct SQL clients — even when wrapped in `docker exec`.
# Each client is listed explicitly so the trailing \b distinguishes e.g.
# `mysql` from `mysqldump` / `mysql_config`.
SQL_CLIENTS = (
    r"mysql|mariadb|mysqldump|mariadb-dump|psql|pg_dump|pg_dumpall|pg_restore|"
    r"sqlcmd|sqlplus|mongo|mongosh|mongodump|mongorestore|sqlite3|cqlsh"
)
if re.search(rf"(?:^|[\s;|&()])(?:sudo\s+)?(?:{SQL_CLIENTS})\b", cmd):
    deny(
        "Direct database-client access is forbidden by team policy: Claude must "
        "never connect to any database. Database work is performed by a human "
        "out-of-band — if a DB command is genuinely needed, the human runs it "
        "themselves with the `!` prefix, which bypasses this guard."
    )

# (2) Block forbidden commit trailers (matches the commit skill's trailer policy).
if re.search(r"\bgit\s+commit\b", cmd) and re.search(
        r"(?i)\b(co-authored-by|signed-off-by|generated-by|reported-by)\b", cmd):
    deny(
        "Attribution trailers (Co-Authored-By / Signed-off-by / Generated-by / "
        "Reported-by) are forbidden. Re-run `git commit` with no trailer. See the "
        "commit skill's trailer policy."
    )

# (3) Block a `git commit` that claims `#automation` when an identifier in the
# message is actually a change we already know — i.e. covered by either the
# upstream catalog OR the documented known-manual-changes. `#automation` means
# "genuinely novel, hand-derived catalog MISS"; if the symbol is in
# replacements.json / the indexes / imports.txt it is a catalog HIT, and if it is
# in known-manual-changes.md it is a documented known-manual-change — neither gets
# `#automation` (there is nothing to harvest). This is the §3g coverage grep,
# enforced: the observed failures are mislabeling a cataloged fix (BasePanelApp /
# LPD-7870) and a documented known-manual-change (SchedulerJobConfiguration)
# `#automation`.
if re.search(r"\bgit\s+commit\b", cmd) and re.search(r"(?i)#automation\b", cmd):
    known_sources = [
        (p, "catalog")
        for d in glob.glob(".claude/cache/reference/*/")
        for name in ("replacements.json", "index_complex.md",
                     "index_simple.md", "imports.txt")
        for p in (os.path.join(d, name),)
        if os.path.isfile(p)
    ]
    kmc = (".claude/skills/deprecated-api-remediation/references/"
           "known-manual-changes.md")
    if os.path.isfile(kmc):
        known_sources.append((kmc, "known-manual"))
    # Candidate identifiers: multi-segment CamelCase (e.g. `BasePanelApp`,
    # `FooPersistenceImpl`) — specific enough to rarely false-positive.
    tokens = set(re.findall(r"\b[A-Z][a-z0-9]+(?:[A-Z][a-z0-9]+)+\b", cmd))
    for path, kind in known_sources:
        try:
            with open(path, encoding="utf-8", errors="ignore") as fh:
                text = fh.read()
        except OSError:
            continue
        for tok in tokens:
            if re.search(r"\b" + re.escape(tok) + r"\b", text):
                if kind == "catalog":
                    deny(
                        f"This commit is labeled `#automation` (catalog MISS) but "
                        f"`{tok}` appears in the upstream catalog ({path}). That makes it a "
                        f"catalog HIT: apply the catalog `to` / EXPECTED OUTPUT and commit "
                        f"it as `Replace X by Y` citing the entry's issueKey, with NO "
                        f"`#automation` label (deprecated-api-remediation §3g). If `{tok}` "
                        f"is genuinely unrelated, drop it from the message or run the commit "
                        f"yourself with the `!` prefix (bypasses this guard)."
                    )
                else:
                    deny(
                        f"This commit is labeled `#automation` (catalog MISS) but "
                        f"`{tok}` is a documented known-manual-change ({path}). Apply it per "
                        f"known-manual-changes.md (flag class → autonomous + "
                        f"`[applied — verify]`; stub class → stub + `[not applied — resolve]`) "
                        f"and commit it WITHOUT `#automation` — it is documented, so there is "
                        f"nothing to harvest (deprecated-api-remediation §3g). If `{tok}` is "
                        f"genuinely unrelated, drop it from the message or run the commit "
                        f"yourself with the `!` prefix (bypasses this guard)."
                    )

sys.exit(0)
