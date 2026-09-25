# Dump format detection & the no-DB-writes inspection queries

How `pre-upgrade-check` Step 2 decides the DB engine + restore tool from the dump itself, and
the inspection queries it **hands to the user** (it never runs SQL).

## Magic-byte → engine / restore tool

Run `file <dump>` and read the leading bytes (`head -c 16 <dump> | xxd`).

| Leading bytes / `file` output | Engine | Dump format | Restore with |
|---|---|---|---|
| `PGDMP` | PostgreSQL | custom (`pg_dump -Fc`) or directory/tar | `pg_restore` |
| `-- PostgreSQL database dump` (ASCII) | PostgreSQL | plain SQL | `psql -f` / `psql <` |
| `-- MySQL dump` / `-- MariaDB dump` (ASCII) | MySQL/MariaDB | plain SQL | `mysql <` |
| `SQLite format 3\0` | SQLite | file | not a Liferay portal dump — **flag** |
| `TAPE` / `Microsoft SQL Server` / `.bak` | SQL Server | native backup | `RESTORE DATABASE` — **hand to user** (we don't run mssql tooling) |
| gzip `1f 8b` / zip `50 4b` | (compressed) | unwrap first | decompress, then re-detect |

A `pg_restore` server only needs to be **>=** the version that wrote the dump (forward
compatible). The `PGDMP` archive version maps roughly:

| `PGDMP` archive version | Written by PostgreSQL (approx) | Safe server to restore on |
|---|---|---|
| v1.12–v1.13 | 9.6–10 | 11+ |
| v1.14 | 11–12 | 12+ |
| v1.15–v1.16 | 12–15 | 13+ (any current) |

Read it with `file` (e.g. `PostgreSQL custom database dump - v1.16-0`).

## Cross-check against the runtime JDBC config

Compare the detected engine with the workspace/runtime config and **flag any mismatch** as a
report row (it tells the client their runtime config and their dump disagree):

- `bundle/portal-ext.properties` / `portal-setup-wizard.properties` → `jdbc.default.driverClassName`
  (`org.postgresql.Driver`, `com.mysql.cj.jdbc.Driver`, …).
- compose env (`POSTGRES_*`, `MYSQL_*`) and the DB service image.

The runtime that `pre-upgrade-check` brings up follows the **dump** engine, since the goal is
to reproduce the environment the dump came from.

## Restore command templates

```bash
# PostgreSQL custom  (anonymized dump: --no-owner/--no-privileges expected)
pg_restore -U <user> -d <db> --clean --if-exists --no-owner --no-privileges <dump>

# PostgreSQL plain SQL
psql -U <user> -d <db> -f <dump>

# MySQL plain SQL
mysql -u <user> -p<pass> <db> < <dump>
```

When the dump is mounted into `/docker-entrypoint-initdb.d/` of a `postgres`/`mysql` image, the
container **auto-restores on first init** — prefer that path and just monitor the DB log.

## Acceptable vs flaggable restore output

- **Acceptable:** `role "<x>" does not exist`, owner/privilege warnings (anonymized dump,
  restored with `--no-owner`), `DROP ... IF EXISTS` notices.
- **Flag:** `encoding` mismatch, `unsupported version`, `could not execute query`, missing
  extension (`pg_trgm`, `uuid-ossp`, `unaccent`), abrupt EOF / truncation (verify size and
  table count against the source delivery).

## Inspection queries — HAND THESE TO THE USER (never run them)

The agent does not open a SQL session. When a check needs data facts, output the query and
ask the user to run it and paste the result.

```sql
-- Confirm the dump's REAL source build/schema (vs the declared product line)
SELECT buildNumber, schemaVersion, servletContextName
FROM release_
WHERE servletContextName = 'portal';

-- Company / virtual host (sanity that data is present)
SELECT companyId, webId, mx FROM company;

-- Rough table population sanity (top tables by row estimate)
SELECT relname, n_live_tup
FROM pg_stat_user_tables
ORDER BY n_live_tup DESC
LIMIT 25;

-- After a reindex error mentioning orphans, e.g. assets without a resource:
SELECT COUNT(*) FROM assetentry ae
LEFT JOIN classname_ cn ON cn.classNameId = ae.classNameId
WHERE cn.classNameId IS NULL;
```

Restore-success sanity the user can also run (mirrors the client README):

```bash
docker exec -it <db-container> psql -U <user> -d <db> -c "\dt" | head -40
# hundreds of Liferay tables ⇒ restore succeeded
```
