# Upgrades Lab Agent Remote

Upgrades Lab lets Liferay Portal drive the upgrade agent, the set of Claude Code skills under `.claude/skills/upgrades`, against a customer repository. A caller submits an **upgrade run** through the headless REST API, the portal hands the run to an **upgrade runner**, and the runner clones the repository and executes the agent on it. The portal stores the run so the caller can poll its status and cancel it.

This is a Labs app. Read the [Current State](#current-state) section before relying on it: the pipeline runs end to end, but publishing the result is not implemented yet.

## Modules

| Module | Bundle | Purpose |
| --- | --- | --- |
| `upgrades-lab-agent-remote-api` | `com.liferay.upgrades.lab.agent.remote.api` | The Service Builder API for the `UpgradeRun` entity, the status machine, the run settings keys, and the `UpgradeRunner` SPI. |
| `upgrades-lab-agent-remote-rest-api` | `com.liferay.upgrades.lab.agent.remote.rest.api` | The REST Builder API: the `UpgradeRun` DTO and the `UpgradeRunResource` interface. |
| `upgrades-lab-agent-remote-rest-client` | `com.liferay.upgrades.lab.agent.remote.rest.client` | The generated Java client for the REST API. |
| `upgrades-lab-agent-remote-rest-impl` | `com.liferay.upgrades.lab.agent.remote.rest.impl` | The REST Builder implementation. The hand-written `rest-config.yaml` and `rest-openapi.yaml` live here. |
| `upgrades-lab-agent-remote-rest-test` | `com.liferay.upgrades.lab.agent.remote.rest.test` | Integration tests for the REST resource, run against a deployed bundle. |
| `upgrades-lab-agent-remote-runner-local` | `com.liferay.upgrades.lab.agent.remote.runner.local` | An `UpgradeRunner` that runs the agent as a child process on the portal host. |
| `upgrades-lab-agent-remote-service` | `com.liferay.upgrades.lab.agent.remote.service` | The Service Builder implementation and persistence for `UpgradeRun`. |
| `upgrades-lab-agent-remote-test` | `com.liferay.upgrades.lab.agent.remote.test` | Integration tests for the local service, run against a deployed bundle. |

## The Pipeline

An upgrade run moves through the layers below. Every step is a Java call inside the portal except the last one, which is a child process.

1. **Caller** sends `POST /o/upgrades-lab-agent-remote/v1.0/upgrade-runs` with the repository, the branch, the target release, the workspace settings, and, when the repository needs one, the credential key reference.

1. **REST resource** (`UpgradeRunResourceImpl`) passes the payload to the local service and maps the stored run back to the DTO.

1. **Local service** (`UpgradeRunLocalService#addUpgradeRun`) builds an `UpgradeRunRequest`, which rejects a run missing any required setting, persists the `UpgradeRun` row in status `queued`, and hands the request to the `UpgradeRunner`. The identifier the runner returns is stored as the run's `externalReferenceCode`.

1. **Runner** (`LocalUpgradeRunner`) executes the run on a portal executor thread:

	1. Validates the branch with `git ls-remote --exit-code`. When the run carries a credential key reference, the private key is resolved from the `SecretManager` first and SSH is pinned to it.

	1. Clones the branch with `--depth 1 --filter=blob:none` into a fresh temp directory under `upgrade-run-*/workspace`.

	1. Writes the settings as `key=value` lines into `upgrade-run.properties` in the clone and exports its path as `UPGRADE_RUN_SETTINGS`.

	1. Unpacks the upgrade agent it carries into the clone and drives it one step at a time: Claude Code runs `/upgrade-init` once, then `/upgrade-phase 1` through `/upgrade-phase 4`, each as its own process. Between phases the runner reads the phase tracker in `upgrade-state.md`, retries a phase whose process ended early, and pushes the branches so far. See [How the Agent Runs](#how-the-agent-runs).

	1. Opens a pull request for the result once the agent finished or stopped at a gate, and pushes whatever branches exist when a run fails or times out. See [How a Run Ends](#how-a-run-ends).

1. **Poll and cancel** go back through the same layers. `GET` asks the runner for its current `UpgradeRunnerState` and records it on the row. `DELETE` asks the runner to cancel and records the cancellation.

The runner reports one of the statuses below. A run moves through the non-terminal statuses in ascending order, skipping phases a runner does not need, and reaches a terminal status from anywhere. Nothing leaves a terminal status, which keeps a late report from overwriting a finished run.

| Status | Value | Terminal | Meaning |
| --- | --- | --- | --- |
| `queued` | 1 | No | Accepted, not started. |
| `validating` | 2 | No | Checking that the branch is reachable. |
| `provisioning` | 3 | No | Allocating a substrate. The local runner skips this. |
| `cloning` | 4 | No | Cloning the repository. |
| `running` | 5 | No | The agent command is executing. |
| `publishing` | 6 | No | Pushing the result branches and opening the pull request. |
| `failed` | 7 | Yes | A command exited non zero or threw. |
| `cancelled` | 8 | Yes | `DELETE` was called before the run finished. |
| `timed-out` | 9 | Yes | A command exceeded the one hour limit. |
| `successful` | 10 | Yes | The agent completed every phase and the result is published. |
| `blocked` | 11 | Yes | The agent stopped at a gate that needs a person. What it did get done is published. |

## Workspace Settings

The agent's `/upgrade-init` skill is interactive by default: it asks for the values it cannot infer from the workspace and writes them into the workspace's `CLAUDE.md`. Nobody is there to answer when the portal drives the agent, so a run carries those values itself, and `/upgrade-init` reads them from `upgrade-run.properties` in its headless mode instead of asking. They are the fields of `UpgradeRunSettingsKeys.REQUIRED`, plus the target release, which doubles as the agent's **upgrade.target.version**.

| Run field | `CLAUDE.md` field | Example | Read by |
| --- | --- | --- | --- |
| `customerName` | **customer.name** | `acme` | `pre-upgrade-check` |
| `dbTargetType` | **db.target.type** | `mysql` | `upgrade-setup-version` |
| `dbTargetVersion` | **db.target.version** | `8.0` | `upgrade-setup-version` |
| `nodeVersion` | **node.version** | `20.18.0` | `upgrade-setup-version`, `upgrade-compile`, `upgrade-module` |
| `searchVersion` | **search.version** | `8.17.4` | `upgrade-setup-version` |
| `targetRelease` | **upgrade.target.version** | `2026.q1.0` | `upgrade-setup-version`, `upgrade-compile`, `upgrade-refresh-references` |
| `upgradeSourceVersion` | **upgrade.source.version** | `7.4.13-u92` | `pre-upgrade-check`, `upgrade-compile` |
| `upgradeTargetJavaVersion` | **upgrade.target.java.version** | `21` | `upgrade-setup-version` |

**node.version** and **search.version** must be concrete patch versions. The agent feeds both verbatim into a Docker image tag, so a range such as `20.x` yields a tag that does not resolve.

Every other `CLAUDE.md` field, such as the Docker service names, the portal start command, or the module roots, is inferred by the agent from the workspace's own README and compose file, so a run does not carry it.

## Configuring the Local Runner

The local runner works without configuration. Its bundle carries the upgrade agent, the `.claude/skills/upgrades` tree of this repository, unpacks it into every clone, and runs Claude Code there in print mode, one skill invocation per step: `/upgrade-init`, then each `/upgrade-phase`. The skills run headless, answering from the run's settings file instead of asking.

Four optional keys in `portal-ext.properties` adjust it.

```properties
# The Claude Code command, one argument per comma separated value. Defaults
# to "claude" on the portal's PATH. Add here whatever flags Claude Code needs
# to run unattended in your environment; the runner appends --print and the
# skill invocation for each step.
upgrades.lab.agent.remote.runner.local.claude.command=/home/liferay/.local/bin/claude

# The GitHub CLI command that opens the pull request, one argument per comma
# separated value. Defaults to "gh" on the portal's PATH.
upgrades.lab.agent.remote.runner.local.gh.command=/usr/local/bin/gh

# A key reference to the Anthropic API key. When set, the runner exports it
# to the agent as ANTHROPIC_API_KEY. Without it the agent uses the portal OS
# user's own Claude Code login.
upgrades.lab.agent.remote.runner.local.api.key.reference=${secretRef:<providerId>:anthropic-api-key}

# Replaces the default agent command entirely, one argument per comma
# separated value. Wrap anything with arguments in a script. A custom command
# is judged by its exit code alone.
upgrades.lab.agent.remote.runner.local.agent.command=/opt/upgrade-agent/run.sh
```

### How the Agent Runs

The bundle carries files, not a running agent. Nothing under `META-INF/upgrade-agent` executes inside the OSGi container or the JVM. When a run reaches `running`, the runner unpacks those files into the clone and starts Claude Code as an ordinary operating system process, a child of the Tomcat JVM, with the clone as its working directory. Each step is one process, exactly as a person would type it in a terminal:

1. `/upgrade-init` reads the settings file and prepares the workspace. The runner then expects the `upgrade/<source>-to-<target>` branch to exist and fails the run when it does not.

1. `/upgrade-phase <N>` for phases 1 to 4. After each process exits the runner reads the phase's row in `upgrade-state.md`. `complete` moves on. `blocked` ends the run in the `blocked` status. Anything else means the process ended before the phase did, for example because the agent handed its turn back to wait for a boot it expected to be woken up for, and the runner invokes the same phase again, up to three times, since `/upgrade-phase` resumes an in-progress phase. `statusMessage` names the phase and the attempt throughout.

1. After every completed phase the runner pushes the upgrade and phase branches, so an interrupted run has its finished phases on the remote already.

The portal never reads a result file on this path; the tracker the skills maintain is the contract. `/upgrade-run` remains the entry point for a person who wants the same chain from a terminal.

The process inherits the environment of the operating system user Tomcat runs as, plus the variables in [The Agent Environment](#the-agent-environment). The agent's access is that user's access, no more and no less:

- Claude Code authenticates with that user's own login under its home directory, unless `upgrades.lab.agent.remote.runner.local.api.key.reference` hands it an API key.

- Git reaches the repository with that user's SSH keys and configuration when the run carries no credential, or with the stored key when it does.

- Docker, Gradle, Node.js, and whatever else the phases invoke resolve through that user's `PATH` and permissions.

On a developer machine where Tomcat runs as the developer, everything the developer can do in a terminal, the agent can do. On a server, the portal's operating system user needs a Claude Code login or an API key, `git` and `claude` on its `PATH` or configured by absolute path, network access to the repository, and Docker for the phases that start the portal. The `claude` binary is resolved through `upgrades.lab.agent.remote.runner.local.claude.command`; Tomcat's `PATH` is often shorter than an interactive shell's, so an absolute path is the safe choice.

Each run is one process, and each process gets one hour. The runner never talks to the Anthropic API itself, holds no conversation state, and cannot answer questions the agent might ask. That is why `/upgrade-run` and the headless modes of `/upgrade-init` and `/upgrade-phase` exist: an unanswered question in print mode ends the process, so the skills must never ask one.

### What the Runner Installs

The bundle holds the agent under `META-INF/upgrade-agent`. The runner copies it into the clone in the layout Claude Code reads.

| Bundle path | Clone path |
| --- | --- |
| `skills/<name>/` | `.claude/skills/<name>/` |
| `reference/` | `.claude/reference/` |
| `hooks/` | `.claude/hooks/` |
| `templates/CLAUDE.md.template` | `CLAUDE.md` |
| `templates/upgrade-state.md.template` | `upgrade-state.md` |
| `templates/.mcp.json.template` | `.mcp.json` |

The workspace settings land next to them in `upgrade-run.properties`, and every one of these paths is added to the clone's `.git/info/exclude` so no phase commits them. A `CLAUDE.md` the customer repository already carries is replaced in the clone, because the skills expect the agent's own file there.

### How a Run Ends

A run ends `successful` when phase 4 completes, `blocked` when a phase records that status, `failed` when a phase does not finish within three attempts or a command fails, and `timed-out` when one process exceeds the hour it is given.

Publishing has two parts:

1. The branches. After every completed phase, and again at the end whatever the outcome short of `cancelled`, the runner pushes the upgrade branch and every `phase*` branch to `origin` with the same credential it cloned with. The push is forced: the agent names its branches the same way on every run, so a later run against the same repository replaces the branches an earlier one left behind. On the success and blocked paths a push failure fails the run, because the branches would otherwise vanish with the clone. On the failure and time-out paths it is only logged.

1. The pull request. For a `successful` or `blocked` run, when `gh` is installed and logged in for the portal's operating system user, the runner opens one pull request from the most recently committed phase branch against the upgrade branch. The title names the target release and, for a blocked run, the status. The body says how the run ended and points at the `upgrade-notes` directory the phases committed. A failure here is only logged, since the branches are already safe.

The response to a `GET` then carries the branch in `resultBranch` and the pull request in `pullRequestURL`, so the caller finds the result in the repository rather than on the portal host. `workspacePath` names the clone on the host for anyone who needs the raw working tree, for example to inspect `upgrade-run.log` one directory above it.

Cancelling a run kills the agent process along with it, and the run keeps the `cancelled` status. Nothing is published for a cancelled run.

### The Agent Environment

The agent command runs with the clone as its working directory and inherits the portal's environment plus the variables below.

| Variable | Value |
| --- | --- |
| `ANTHROPIC_API_KEY` | The secret behind the configured API key reference, when one is set. |
| `GIT_SSH_COMMAND` | `ssh` pinned to the run's private key, with `BatchMode=yes`, `IdentitiesOnly=yes`, and `StrictHostKeyChecking=accept-new`. Only set when the run carries a credential key reference. |
| `GIT_TERMINAL_PROMPT` | `0`, so Git never blocks on a prompt. |
| `UPGRADE_RUN_SETTINGS` | The path of `upgrade-run.properties` inside the clone, holding the workspace settings as sorted `key=value` lines. |

Each command has one hour to finish. Output from every command is appended to `upgrade-run.log` inside the run's temp directory, which lives under Tomcat's `temp` folder. The portal log records the exception when a run fails or times out.

### Storing the Credentials

A credential is optional. A run without `credentialKeyReference` leaves Git to the host's own configuration, which is enough for a public HTTPS URL, a `file://` URL, or an SSH URL the portal's OS user can already reach. A private repository the host cannot reach on its own needs a stored private key.

The runner never receives a private key or an API key in the clear. Both arrive as **key references**, strings of the form `${secretRef:<providerId>:<identifier>}` that the portal's `SecretManager` resolves at run time. The provider `*` resolves to the active key manager profile's company secret provider. The profile defaults to `db-company-secret`, but no provider by that ID ships in this repository yet; the only `SecretProvider` implementations are the AWS Secrets Manager ones. Until a database-backed provider exists, name a registered provider explicitly in the reference.

There is no UI or REST endpoint for adding secrets yet. Until one exists, put them through the `SecretManager` OSGi service, for example from the Server Administration script console. `companyId` is not a predefined variable there, so resolve it, for example with `PortalUtil.getDefaultCompanyId()`.

```groovy
import com.liferay.portal.security.key.KeyReference
import com.liferay.portal.security.key.KeyReferenceUtil
import com.liferay.portal.security.key.secret.Secret
import com.liferay.portal.security.key.secret.SecretManager
import com.liferay.portal.kernel.module.service.Snapshot

def secretManager = new Snapshot<>(Snapshot.class, SecretManager.class).get()

def privateKey = new File("/path/to/id_ed25519").text

def keyReference = secretManager.putSecret(
	companyId,
	new Secret(new KeyReference("acme-deploy-key", "<providerId>", KeyReference.Type.SECRET), privateKey))

out.println(KeyReferenceUtil.toKeyReferenceString(keyReference))
```

The printed string is what goes in the `credentialKeyReference` field of a run, or in the `upgrades.lab.agent.remote.runner.local.api.key.reference` property for the API key.

## Using the Headless API

The REST application is mounted at `/o/upgrades-lab-agent-remote/v1.0`. Authenticate the same way as any Liferay headless API, with basic authentication or an OAuth 2 token. The generated OpenAPI document is at `/o/upgrades-lab-agent-remote/v1.0/openapi.yaml`, and the same operations are exposed through GraphQL at `/o/graphql`.

### Submit a Run

```bash
UPGRADE_RUN=$(cat <<'EOF'
{
	"branch": "main",
	"credentialKeyReference": "${secretRef:<providerId>:acme-deploy-key}",
	"customerName": "acme",
	"dbTargetType": "mysql",
	"dbTargetVersion": "8.0",
	"nodeVersion": "20.18.0",
	"repositoryURL": "git@github.com:acme/liferay-workspace.git",
	"searchVersion": "8.17.4",
	"targetRelease": "2026.q1.0",
	"upgradeSourceVersion": "7.4.13-u92",
	"upgradeTargetJavaVersion": "21"
}
EOF
)

curl \
	--data "${UPGRADE_RUN}" \
	--header "Content-Type: application/json" \
	--request POST \
	--url "${PORTAL_URL}/o/upgrades-lab-agent-remote/v1.0/upgrade-runs" \
	--user "${PORTAL_USER}:${PORTAL_PASSWORD}"
```

A missing required field is rejected before anything is stored. Only `credentialKeyReference` is optional. The response is the run the portal created, and `externalReferenceCode` is the handle for the two calls below.

| Field | Direction | Meaning |
| --- | --- | --- |
| `branch` | Write | The branch to upgrade. |
| `credentialKeyReference` | Write, optional | The key reference of the private key the runner reaches the repository with. Leave it out when the repository needs no credential. |
| `customerName`, `dbTargetType`, `dbTargetVersion`, `nodeVersion`, `searchVersion`, `upgradeSourceVersion`, `upgradeTargetJavaVersion` | Write | The workspace settings. See [Workspace Settings](#workspace-settings). |
| `externalReferenceCode` | Read only | The identifier the runner knows the run by. |
| `pullRequestURL` | Read only | The pull request the run opened for its result. Empty until `publishing`, and empty when `gh` is not available on the host. |
| `repositoryURL` | Write | The URL of the repository. SSH, HTTPS, and `file://` all work; see [Storing the Credentials](#storing-the-credentials). |
| `resultBranch` | Read only | The branch the run pushed its result to, the most recently committed phase branch. Empty until `publishing`. |
| `status` | Read only | One of the statuses in the table above. |
| `statusMessage` | Read only | What the run was doing when it last reported. |
| `targetRelease` | Write | The release to upgrade to. |
| `upgradeRunId` | Read only | The primary key of the stored run. |
| `workspacePath` | Read only | The directory on the runner host where the run cloned and upgraded the repository. Set once cloning starts. |

### Poll a Run

```bash
curl \
	--url "${PORTAL_URL}/o/upgrades-lab-agent-remote/v1.0/upgrade-runs/by-external-reference-code/${EXTERNAL_REFERENCE_CODE}" \
	--user "${PORTAL_USER}:${PORTAL_PASSWORD}"
```

Each `GET` asks the runner for its current state and records it on the stored run before answering. Poll until `status` is one of `blocked`, `cancelled`, `failed`, `successful`, or `timed-out`. A run the runner no longer knows, for example after a portal restart, comes back as `failed` so the caller stops polling.

### Cancel a Run

```bash
curl \
	--request DELETE \
	--url "${PORTAL_URL}/o/upgrades-lab-agent-remote/v1.0/upgrade-runs/by-external-reference-code/${EXTERNAL_REFERENCE_CODE}" \
	--user "${PORTAL_USER}:${PORTAL_PASSWORD}"
```

Cancelling a run that already finished does nothing and still returns `204`.

### Java Client

The `upgrades-lab-agent-remote-rest-client` module is a Java client for the REST API. Add it as a dependency and use the generated resource.

```java
UpgradeRunResource upgradeRunResource = UpgradeRunResource.builder(
).authentication(
	"test@liferay.com", "test"
).build();

UpgradeRun upgradeRun = upgradeRunResource.postUpgradeRun(
	new UpgradeRun() {
		{
			branch = "main";
			credentialKeyReference = "${secretRef:<providerId>:acme-deploy-key}";
			customerName = "acme";
			dbTargetType = "mysql";
			dbTargetVersion = "8.0";
			nodeVersion = "20.18.0";
			repositoryURL = "git@github.com:acme/liferay-workspace.git";
			searchVersion = "8.17.4";
			targetRelease = "2026.q1.0";
			upgradeSourceVersion = "7.4.13-u92";
			upgradeTargetJavaVersion = "21";
		}
	});

upgradeRun = upgradeRunResource.getUpgradeRunByExternalReferenceCode(
	upgradeRun.getExternalReferenceCode());
```

## Current State

Submit, poll, and cancel work end to end against the local runner. The gaps that remain are listed below.

- **Pull requests depend on `gh`.** Pushing needs only Git, but opening the pull request shells out to the GitHub CLI on the portal host, logged in as the portal's operating system user. Without it a run still publishes its branches and reports them in `resultBranch`, with `pullRequestURL` empty. A token carried on the run, once a secret provider exists, is the path to removing that dependency.

- **The local runner keeps runs in memory.** Its map of runs is lost on portal restart or module redeploy, and a run that is still executing is cancelled on deactivate. The persisted `UpgradeRun` row is the durable record, and the next `GET` marks such a run `failed`.

- **One runner at a time.** The local service binds to whichever `UpgradeRunner` is registered and does not record `runnerType` yet. Selecting a runner per run is open.

- **No UI or REST endpoint for secrets.** See [Storing the Credentials](#storing-the-credentials).

- **JSON web services are off.** The entity is `remote-service="false"`, so the headless REST API is the only remote entry point.

- **No permission checks.** The REST resource acts for the authenticated user without checking a portal permission. Restrict access at the OAuth 2 application or the network until one exists.

## Development

Deploy the api, service, REST API, REST implementation, REST client, and runner modules from each module root. The test modules are never deployed by hand; `testIntegration` wires them in itself.

```bash
../../../../gradlew deploy
```

The entity's schema changed while the bundle version stayed at `1.0.0`. On a bundle that already created the `Upgrades_UpgradeRun` table, drop the table or reset the database before redeploying the service module.

Run the unit tests.

```bash
(cd upgrades-lab-agent-remote-api && ../../../../gradlew test)
(cd upgrades-lab-agent-remote-runner-local && ../../../../gradlew test)
(cd upgrades-lab-agent-remote-service && ../../../../gradlew test)
```

Run the integration tests against a running bundle with the other modules deployed. The service tests submit runs whose credential does not resolve, so the local runner fails them without reaching Git or the agent command.

```bash
(cd upgrades-lab-agent-remote-rest-test && ../../../../gradlew testIntegration --tests UpgradeRunResourceTest)
(cd upgrades-lab-agent-remote-test && ../../../../gradlew testIntegration --tests UpgradeRunLocalServiceTest)
```

When editing `service.xml` or an `*Impl` class, run `buildService` from the service module and commit the regenerated output on its own. When editing `rest-openapi.yaml` or `rest-config.yaml`, run `buildREST` from the REST implementation module and do the same. Do not hand edit any file tagged `@generated` or `@Generated("")`.
