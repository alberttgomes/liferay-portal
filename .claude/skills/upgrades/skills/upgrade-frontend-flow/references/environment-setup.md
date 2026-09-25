# Phase 0 — environment setup details

Everything the agent needs to bootstrap the test repo from nothing but a test-map `.tsv`.
Read this when running Phase 0; `SKILL.md` carries the step order, this file carries the contracts.

## The generator script

`generate-test-files.sh` is the frontend team's scaffolder. It ships with this skill at
`assets/generate-test-files.sh` and is expected at `.scripts/generate-test-files.sh` in the test
repo. If the repo has no copy, `cp` the asset there (keep it executable) and commit it with the
environment commit. If the repo already has one, **use the repo's copy** — the team may have
moved ahead of the shipped version.

```bash
.scripts/generate-test-files.sh -n "<project>" -s "<status>" [-a tmp/.auth/login.json] <test-map>.tsv
```

| Flag | Meaning |
|---|---|
| `-n, --project-name` | Names `{project}.config.ts` and its exported `<project>Config`. **Always pass it** — without it the script prompts on stdin and the run hangs. |
| `-s, --status` | Generate only rows whose status column equals this value. Default `Needs Automation`. Match is **exact and case-sensitive**. |
| `-a, --auth-storage` | For rows with a truthy auth column, emit `dependencies: ['setup']` + `use.storageState: <path>` into the feature `config.ts`. Omit it and no auth wiring is emitted at all. |
| `-e, --ignore-env` | Skip `.env` — also skips generating `{project}.config.ts`. The agent never uses this. |

The script `cd`s to the git top level before doing anything, so pass the `.tsv` path relative to the
repo root or absolute.

### What it writes

For a suite `Bookshop / Listing`:

```
pages/bookshop/listing/ListingPage.ts    # class stub: constructor(readonly page: Page) {}
fixtures/bookshopListingTest.ts          # test.extend, named after the SUITE path
tests/bookshop/listing/config.ts         # Project entry: name, testDir, testMatch
tests/bookshop/listing/listing.spec.ts   # test.describe + one empty test() per row
playwright.config.ts                     # regenerated wholesale, imports every config.ts
{project}.config.ts                      # regenerated wholesale, reads .env
```

The suite column splits on `/` into nested folders; the spec file is named after the **last**
segment. `test.describe()` gets the suite string **untrimmed of case** (as written in the sheet),
while folder and identifier names are normalized and accent-folded. Rows sharing a suite land in
one spec file, one fixture and one page class.

Each generated `test()` carries the row's `Steps` as a `/** … */` comment, split on `And` / `When` /
`Then`, with the notes column above it as `# NOTES` lines. That comment is the specification —
never rewrite it.

### Column resolution

Columns are resolved by **header name**, case- and accent-insensitive, not by position. The header
is the first row holding both a suite column and a test-name column, so guide and title rows above
it are skipped automatically. Rows with an empty suite or test name are skipped too.

| Role | Accepted headers |
|---|---|
| Suite (**required**) | `Context`, `Describe`, `Module`, `Modulo`, `Feature`, `Funcionalidade` |
| Test name (**required**) | `Test Description`, `Test Case Name`, `Test Name`, `Caso de Teste` |
| Status | `Test Status`, `Status`, `Situacao` |
| Authentication | anything containing `authentication` / `autenticacao` |
| Steps | `Steps`, `BDD`, `Passos` |
| Notes | `Comments`, `Notes`, `Observacoes`, `Notas` |

Truthy authentication values: `yes`, `y`, `true`, `1`, `sim`, `s`.

With no status column present, every row is generated regardless of `-s`.

### Read the `.tsv` header before asking the executor anything

Parse the header and the rows first, using the table above. That gives you, before the first
question: which roles need credentials, which `Test Status` values actually occur in the file
(offer those as the options), and how many rows each status holds. Asking for a status that no row
carries wastes a run — the script exits 1 and lists the values it saw.

## The `.env` contract

Write `.env` from `assets/env.example`. The script's parser decides what the generated
`{project}.config.ts` exposes:

- A key containing `BASE_URL` or ending in `_URL` → `config.environments.<camelCased key>`.
- Any other key → `config.users.<role>.<last underscore segment, lowercased>`, where `<role>` is the
  key with a trailing `_EMAIL` / `_PASSWORD` stripped. So `ADMIN_EMAIL` + `ADMIN_PASSWORD` become
  `config.users.admin.email` and `config.users.admin.password`.

**`BASE_URL` is load-bearing.** The generated `playwright.config.ts` sets `use.baseURL` only when a
variable normalizes to `baseUrl` — i.e. only from a key literally named `BASE_URL`. `ORIGINAL_URL`
lands in `config.environments.originalUrl` and does **not** feed `baseURL`. So `base_url_var` is
always `BASE_URL`, and switching portals is a per-command prefix:

```bash
BASE_URL=$UPGRADE_URL npx playwright test <spec-path>
```

`loadEnvFile()` never overrides a variable already set in the environment, so the prefix
deterministically wins over the file.

Add `.env` to `.gitignore` **before** writing it. Never commit it, never echo a credential into
chat or a commit message.

## Post-scaffold reconciliation

The script's output is a starting point, not a finished config. Two things must be fixed before the
suite is usable — do them **after** the script runs, and never re-run the script afterwards without
redoing them: it regenerates `playwright.config.ts` and `{project}.config.ts` wholesale and your
edits are lost.

### 1. Patch `playwright.config.ts` to the team defaults

The generated config sets `trace: 'on-first-retry'` and no `retries`, which means it **never
captures a trace on a local run** — the exact trap `team-conventions.md` warns about. It also omits
the parallelism and CI guards. Bring it to:

```typescript
fullyParallel: true,
forbidOnly: !!process.env.CI,
retries: process.env.CI ? 2 : 0,
use: {
    baseURL: <project>Config.environments.baseUrl,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
},
```

### 2. Create the `setup` project when the test map needs authentication

`-a` makes the script emit `dependencies: ['setup']` into each authenticated feature `config.ts`,
but it **does not create the `setup` project itself**. Left alone, that is a dangling dependency:
Playwright refuses to list or run the suite. Create it:

- `tests/@setup/setup.ts` — logs in with `<project>Config.users.<role>` and saves
  `storageState` to the path passed to `-a` (`tmp/.auth/login.json` by default).
- `utils/isAuthenticated.ts` — the guard that skips the login when the state file already exists,
  per the authentication pattern in `team-conventions.md`.
- A `setup` entry in `playwright.config.ts`'s `projects[]`, listed before the feature projects, with
  `testMatch` pointing at the setup spec and its own `teardown` project if you add one.

One `setup` project per role. The script hardcodes the project name `setup` (singular) and a single
`storageState` path, so a test map needing two roles needs a second project plus a per-role
`storageState` — wire the extra role's `config.ts` by hand and record it in the state file.

Verify both fixes with `npx tsc --noEmit` and `npx playwright test --list`: the listing must show
every generated project and no unresolved dependency.

## Prettier

The script formats its output with `./node_modules/.bin/prettier --config ./.prettierrc` when that
binary exists. On a fresh checkout it does not — run `npm install` before the script if you want the
generated files formatted, or run Prettier yourself as part of the verification ladder.
