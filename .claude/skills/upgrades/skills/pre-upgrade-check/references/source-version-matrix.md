# Source-version matrix

`pre-upgrade-check` reproduces the client's **current** version. Given
`liferay.workspace.product` (the source line), this table fixes the runtime knobs. These are
**source-version** values — do not confuse with upgrade-target values used elsewhere in the
agent.

| Source line | Java (JDK) | Search engine + client | Search port(s) | Notes |
|---|---|---|---|---|
| `dxp-7.1-*` / `portal-7.1-*` | 8 | Elasticsearch 6.x, **transport client** | 9300 (+9200) | analysis plugins required |
| `dxp-7.2-*` / `portal-7.2-*` | 8 | Elasticsearch 6.8.x, **transport client** | 9300 (+9200) | analysis plugins required |
| `dxp-7.3-*` / `portal-7.3-*` | 8 or 11 | Elasticsearch 7.x, **REST** | 9200 | sidecar gated by `operationMode` |
| `dxp-7.4-*` / `portal-7.4-*` / `2023.q*` | 11 | Elasticsearch 7.x, **REST** | 9200 | connector PID `elasticsearch7` |
| `2024.q*` | 17 | Elasticsearch 8.x, **REST** | 9200 | connector PID `elasticsearch8` |
| `2025.q*+` | 21 | Elasticsearch 8.x, **REST** | 9200 | connector PID `elasticsearch8` |

## Elasticsearch analysis plugins (7.1–7.3 era)

Liferay 7.1–7.3 require these ES plugins on the search node. The scaffold installs them on the
`elasticsearch` service via an inline `command:` on first boot; note them in the README too (a
client-shipped ES image usually already carries them):

```
analysis-icu  analysis-kuromoji  analysis-smartcn  analysis-stempel
```

## Activation key (DXP only)

A **DXP** source line needs a developer activation key **matching its own version line** — a
7.4 key does not activate a 7.2 portal. Verify it is present; if absent, flag that a source-line
developer key is required before checks 3–4 can pass. CE/portal lines: skip.

**Placement depends on the key format — this trips people up:**
- An **XML** key (the form downloaded from the customer portal) must go in the **`deploy/`** folder
  (`docker-compose/liferay/mnt/liferay/files/deploy/`); the auto-deploy scanner registers it and
  writes the binary form into `data/license`. An XML key dropped directly in `data/license` is **not
  read** — `LicenseManager` logs "No binary licenses found" and the portal loops on
  `/c/portal/license_activation`.
- A **binary `.li`** key (e.g. from a delivered bundle at `bundle/data/license/LiferayActivationKey_*.li`)
  goes in `data/license`.
- `LicenseManager` reads **every** file in `data/license`, so never leave a placeholder there (a
  `.gitkeep` triggers "Failed to read license file …"). Git-ignore the key (a credential); keep the
  dir via `deploy/.gitkeep` instead.

## Runtime image / bundle

- **Preferred:** the official `liferay/dxp:<source-tag>` (or `liferay/portal:<tag>` for CE)
  image customized purely through granular bind mounts from `./bundles/*` + `/mnt/liferay/files`
  — the base-workspace structure; no Dockerfile or bundle tar. DXP SP/quarterly tags are gated
  on Docker Hub — confirm the exact pullable tag with the user.
- **Fallback:** the client's delivered bundle run on a JDK base image (e.g.
  `eclipse-temurin:<java>-jdk`) via a `Dockerfile`/`build:` — used **only** when no official
  image can be pulled for the client's exact source patch level.

## Theme tooling (gulp/npm themes)

Old `liferay-theme-tasks` majors pin to an old Node line; building on a too-new Node breaks
node-sass/gulp. Pin/flag the source-era Node in the README:

| liferay-theme-tasks | Liferay era | Node line |
|---|---|---|
| `^9.x` | 7.1 | 8–10 |
| `^11.x` | 7.2–7.3 | 10–14 |
| `^12.x`+ | 7.4+ | 14+ |
