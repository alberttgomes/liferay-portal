# Liferay <source-version> DXP

Liferay DXP <source-version> running on docker compose. This is the **original**
(pre-upgrade) environment, reproduced from the delivered source and validated by
`/pre-upgrade-check`. The custom modules and themes run here are **built from source**, not the
delivered prebuilt jars.

## Requirements

* Liferay Digital Experience Platform <source-version>
* Blade CLI
* Java JDK <source-java>
* Node <node-version> / npm <npm-version>  (for the theme build)
* Gradle <gradle-version> (via the workspace wrapper), Liferay workspace plugin <plugin-version>
* Docker and Docker Compose

> Build dependencies: this workspace originally resolved only from the client-internal
> repository. For reproduction outside that network, the Liferay public CDN + Maven Central
> were added **alongside** the internal entries (none removed) — see `pre-upgrade-notes/`.

## External artifacts (download separately)

The **database dump** and **document library** are large / sensitive and ship out-of-band — they are
**not** in this repository. Download them from `<artifacts-link>` (e.g. the delivery Drive / share)
and place:

| Artifact | Put it at |
|---|---|
| Database dump | `docker-compose/database/dump/` |
| Document library | `docker-compose/liferay/data/document_library/` |
| DXP developer activation key (`*.xml`) | `docker-compose/liferay/mnt/liferay/files/deploy/` |

## Docker

The docker setup creates the following services:
* `database` (<db-engine> <db-version>)
* `elasticsearch` (<search-version>)
* `liferay`
* `mailhog`

### Database

The database dump (<dump-size>, <dump-format>) is placed **unzipped** under
`docker-compose/database/dump/`. It is auto-restored on the first `database` startup (the dump
is mounted into `/docker-entrypoint-initdb.d`). Restore tool: `<pg_restore|psql|mysql>`.

### Document Library

Place the document library under `docker-compose/liferay/data/document_library/` (mounted at
`/opt/liferay/data/document_library`). If the store already lives elsewhere on your machine, point
at it with a git-ignored `docker-compose.override.yml` (a `liferay` `volumes:` entry) instead of
copying it. Without the DL, `DLFileEntry`/image files 404 (`NoSuchFileException`) and their content
is not indexed. (If absent: not provided with these artifacts.)

### Deploy Modules and Themes

This environment runs artifacts **built from source**. The build output lands in `bundles/`
(mounted straight into the container — no image rebuild needed).

1. In the `root` directory
2. Type `blade gw clean deploy` (or `./gradlew clean deploy`)
3. Build any gulp/npm theme: `( cd <themes.root>/<theme> && npm ci && npx gulp build )`

> If the build fails with `Invalid value for Bundle-Version` (the project version is a CI token
> like `##…##`), set a real version in `gradle.properties` and every `bnd.bnd` first. If the
> version was already baked to `<version>` in this repo, no action is needed.

## Building and Starting

1. In the `root` directory
2. Type `docker compose up --build -d database`.
3. Wait for the database import; check it with `docker compose logs -f database` (wait for the
   restore-complete line, then Ctrl+C).
4. Now, type `docker compose up --build -d`.

If Liferay starts before the DB restore finishes (`Connection to <db>:5432 refused`), just
restart the portal — no need to restore again: `docker compose restart liferay`.

## Access

* Portal (main): `<portal-url>` (default `http://localhost:8080`)
* MailHog UI: `http://localhost:8025`

If the client exposes several sites/instances on **vanity hosts** (DB-mapped virtual hosts), add
them to `/etc/hosts` and reach each on `:8080`:

```
##
## <Customer>
##
127.0.0.1 <vanity-host-1>
127.0.0.1 <vanity-host-2>
```

| Instance | URL | Login |
|---|---|---|
| Main | `http://localhost:8080/...` | `<user>` / `<pass>` |
| `<instance-2>` | `http://<vanity-host-1>:8080/...` | `<user>` / `<pass>` |
| `<instance-3>` | `http://<vanity-host-2>:8080/...` | `<user>` / `<pass>` |

Credentials are the anonymized dump's users (fill the placeholders per the delivery notes; never
inline real/production passwords).

## Docs

* Liferay <source-version> documentation.
* Search engine: <search-engine> <search-version> (analysis plugins: <es-plugins>, if applicable).
