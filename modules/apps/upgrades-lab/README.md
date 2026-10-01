# Upgrades Lab Agent Remote

Upgrades Lab lets Liferay Portal drive the upgrade agent, the set of Claude Code skills under `.claude/skills/upgrades`, against a customer repository. A caller submits an **upgrade run** through the headless REST API, the portal hands the run to an **upgrade runner**, and the runner clones the repository and executes the agent on it. The portal stores the run so the caller can poll its status and cancel it.

This is a Labs app. Read the [Current State](#current-state) section before relying on it: the pipeline runs end to end, and that section lists the gaps that remain.

## Modules

| Module | Bundle | Purpose |
| --- | --- | --- |
| `upgrades-lab-agent-remote-api` | `com.liferay.upgrades.lab.agent.remote.api` | The Service Builder API for the `UpgradeRun` entity, the status machine, the run settings keys, and the `UpgradeRunner` SPI. |
| `upgrades-lab-agent-remote-rest-api` | `com.liferay.upgrades.lab.agent.remote.rest.api` | The REST Builder API: the `UpgradeRun` DTO and the `UpgradeRunResource` interface. |
| `upgrades-lab-agent-remote-rest-client` | `com.liferay.upgrades.lab.agent.remote.rest.client` | The generated Java client for the REST API. |
| `upgrades-lab-agent-remote-rest-impl` | `com.liferay.upgrades.lab.agent.remote.rest.impl` | The REST Builder implementation. The handwritten `rest-config.yaml` and `rest-openapi.yaml` live here. |
| `upgrades-lab-agent-remote-rest-test` | `com.liferay.upgrades.lab.agent.remote.rest.test` | Integration tests for the REST resource, run against a deployed bundle. |
| `upgrades-lab-agent-remote-runner-local` | `com.liferay.upgrades.lab.agent.remote.runner.local` | An `UpgradeRunner` that runs the agent as a child process on the portal host. |
| `upgrades-lab-agent-remote-service` | `com.liferay.upgrades.lab.agent.remote.service` | The Service Builder implementation and persistence for `UpgradeRun`. |
| `upgrades-lab-agent-remote-test` | `com.liferay.upgrades.lab.agent.remote.test` | Integration tests for the local service, run against a deployed bundle. |

## The Pipeline

An upgrade run moves through the layers below. Every step is a Java call inside the portal except the last one, which is a child process.

1. **Caller** sends `POST /o/upgrades-lab-agent-remote/v1.0/upgrade-runs` with the repository, the branch, the workspace settings, and, when the repository needs one, the credential key reference.

1. **REST resource** (`UpgradeRunResourceImpl`) passes the payload to the local service and maps the stored run back to the DTO.

1. **Local service** (`UpgradeRunLocalService#addUpgradeRun`) validates the input, builds an `UpgradeRunRequest`, persists the `UpgradeRun` row in status `queued`, and hands the request to the `UpgradeRunner`. The identifier the runner returns is stored as the run's `externalReferenceCode`.

1. **Runner** (`LocalUpgradeRunner`) executes the run on a portal executor thread:

	1. Validates the branch with `git ls-remote --exit-code`. When the run carries a credential key reference, the private key is resolved from the `SecretManager` first and SSH is pinned to it. It then resolves the DXP license from the run's `licensePath`, or from the configured license path when the run names none. See [Supplying the License](#supplying-the-license).

	1. Clones the branch with `--depth 1 --filter=blob:none` into a fresh temp directory under `upgrade-run-*/workspace`.

	1. Writes the settings as `key=value` lines into `upgrade-run.properties` in the clone and exports its path as `UPGRADE_RUN_SETTINGS`.

	1. Unpacks the upgrade agent it carries into the clone and drives it one step at a time: Claude Code runs `/upgrade-init` once, then `/upgrade-phase 1` through `/upgrade-phase 4`, each as its own process. Between phases the runner reads the phase tracker in `upgrade-state.md`, retries a phase whose process ended early, and pushes the branches so far. See [How the Agent Runs](#how-the-agent-runs).

	1. Opens a pull request for the result once the agent finished or stopped at a gate, and pushes whatever branches exist when a run fails or times out. See [How a Run Ends](#how-a-run-ends).

1. **Poll and cancel** go back through the same layers. `GET` asks the runner for its current `UpgradeRunnerState` and records it on the row. `POST .../cancel` asks the runner to cancel and records the cancellation. The run stays stored, so `GET` keeps answering for it.

1. **Resume** submits a new run that continues a finished one from a phase. `POST .../resume` goes through the same layers as a submission: the local service copies the finished run's fields and settings onto a new row, applies the settings the caller sent, and hands the runner a request that names the phase to start at, the finished run's result branch, and its clone. See [Resuming a Run](#resuming-a-run).

The runner reports one of the statuses below. A run moves through the nonterminal statuses in ascending order, skipping phases a runner does not need, and reaches a terminal status from anywhere. Nothing leaves a terminal status, which keeps a late report from overwriting a finished run.

| Status | Value | Terminal | Meaning |
| --- | --- | --- | --- |
| `queued` | 1 | No | Accepted, not started. |
| `validating` | 2 | No | Checking that the branch is reachable. |
| `provisioning` | 3 | No | Allocating a substrate. The local runner skips this. |
| `cloning` | 4 | No | Cloning the repository, or reusing the clone of the run being resumed. |
| `running` | 5 | No | The agent command is executing. |
| `publishing` | 6 | No | Pushing the result branches and opening the pull request. |
| `failed` | 7 | Yes | A command exited nonzero or threw. |
| `cancelled` | 8 | Yes | `POST .../cancel` was called before the run finished. |
| `timed-out` | 9 | Yes | An agent step exceeded its hour, or a Git or `gh` command its ten minutes. |
| `successful` | 10 | Yes | The agent completed every phase and the result is published. |
| `blocked` | 11 | Yes | The agent stopped at a gate that needs a person. What it did get done is published. |

## Workspace Settings

The agent's `/upgrade-init` skill is interactive by default: it asks for the values it cannot infer from the workspace and writes them into the workspace's `CLAUDE.md`. Nobody is there to answer when the portal drives the agent, so a run carries those values itself, and `/upgrade-init` reads them from `upgrade-run.properties` in its headless mode instead of asking. They are the fields of `UpgradeRunSettingsKeys.REQUIRED`.

| Run field | `CLAUDE.md` field | Example | Read by |
| --- | --- | --- | --- |
| `customerName` | **customer.name** | `acme` | `pre-upgrade-check` |
| `dbTargetType` | **db.target.type** | `mysql` | `upgrade-setup-version` |
| `dbTargetVersion` | **db.target.version** | `8.0` | `upgrade-setup-version` |
| `nodeVersion` | **node.version** | `20.18.0` | `upgrade-setup-version`, `upgrade-compile`, `upgrade-module` |
| `searchVersion` | **search.version** | `8.17.4` | `upgrade-setup-version` |
| `upgradeSourceVersion` | **upgrade.source.version** | `7.4.13-u92` | `pre-upgrade-check`, `upgrade-compile` |
| `upgradeTargetJavaVersion` | **upgrade.target.java.version** | `21` | `upgrade-setup-version` |
| `upgradeTargetVersion` | **upgrade.target.version** | `2026.q1.0` | `upgrade-setup-version`, `upgrade-compile`, `upgrade-refresh-references` |

**node.version** and **search.version** must be concrete patch versions. The agent feeds both verbatim into a Docker image tag, so a range such as `20.x` yields a tag that does not resolve.

Every other `CLAUDE.md` field, such as the Docker service names, the portal start command, or the module roots, is inferred by the agent from the workspace's own README and compose file, so a run does not carry it.

## Configuring the Local Runner

The local runner works without configuration. Its bundle carries the upgrade agent, the `.claude/skills/upgrades` tree of this repository, unpacks it into every clone, and runs Claude Code there in print mode with permission prompts bypassed, one skill invocation per step: `/upgrade-init`, then each `/upgrade-phase`. The skills run headless, answering from the run's settings file instead of asking.

Its settings are an OSGi configuration, `LocalUpgradeRunnerConfiguration`, scoped to the portal instance: **Control Panel → Instance Settings → Upgrades → Local Upgrade Runner** holds one instance's values, and **System Settings** or a `.config` file holds the defaults every instance inherits. The runner reads the configuration of the run's company when the run starts, so changes apply without a restart. Every field is optional.

| Field | Default | Meaning |
| --- | --- | --- |
| `agentCommand` | Empty | Replaces the Claude Code steps with a single command, one argument per entry. A run that uses it is judged by its exit code alone. |
| `claudeCommand` | `claude` | The Claude Code command, one argument per entry. The runner appends `--permission-mode bypassPermissions`, `--print`, and the skill for each step, so no permission prompt is ever waiting for an answer. |
| `claudeConfigDirectory` | Empty | The directory Claude Code keeps its settings and login in, exported as `CLAUDE_CONFIG_DIR`. Empty means Claude Code's default, the `.claude` folder of the portal's OS user. |
| `claudeCredential` | Empty | The Anthropic credential the runner exports to the agent: an API key as `ANTHROPIC_API_KEY`, or a token from `claude setup-token` as `CLAUDE_CODE_OAUTH_TOKEN`. A `${secretRef:...}` key reference is resolved through the `SecretManager` instead of being used as it is. See [Authenticating the Agent](#authenticating-the-agent). |
| `ghCommand` | `gh` | The GitHub CLI command that opens the pull request, one argument per entry. |
| `hostCredentialsEnabled` | `false` | Whether a run without `claudeCredential` may use the Claude Code login found on the portal's host. When off, such a run fails before cloning. |
| `licensePath` | `/opt/liferay/upgrades/license` | The DXP license file, or a folder holding one, that a run uses when it names no `licensePath`. See [Supplying the License](#supplying-the-license). |

A `.config` file goes in `osgi/configs` under the Liferay home:

```properties
# com.liferay.upgrades.lab.agent.remote.runner.local.internal.configuration.LocalUpgradeRunnerConfiguration.config
claudeCommand=["/home/liferay/.local/bin/claude"]
claudeCredential="${secretRef:<providerId>:anthropic-api-key}"
ghCommand=["/usr/local/bin/gh"]
licensePath="/opt/liferay/upgrades/license"
```

### Authenticating the Agent

Claude Code looks for a credential in a fixed order: the `ANTHROPIC_*` and `CLAUDE_CODE_*` environment variables first, then the login stored under its configuration directory. The runner decides which of those the agent sees, per run, from the company's configuration:

1. **A configured credential.** When `claudeCredential` is set, the runner exports it as `ANTHROPIC_API_KEY`, or as `CLAUDE_CODE_OAUTH_TOKEN` when it starts with `sk-ant-oat`, the prefix of the long lived token `claude setup-token` issues for a subscription. In this mode the agent's environment carries no other `ANTHROPIC_*` or `CLAUDE_CODE_*` variable: whatever Tomcat's environment holds is stripped, so a gateway, a cloud provider toggle, or a login on the host cannot redirect the run.

1. **The host's login.** Without `claudeCredential`, and only when `hostCredentialsEnabled` is on, the agent runs with the host's environment as it is and Claude Code finds the login of the portal's OS user, or of `claudeConfigDirectory` when that is set. Every company on the host shares that identity, and its plan pays for the run. This mode exists for development and the lab; keep it off where runs are billed to customers.

1. **Neither.** The run fails while validating, with a status message naming the missing configuration, and nothing is cloned.

The credential is stored in the configuration itself, as the AI Hub cell stores its client secret, so a company can be set up from Instance Settings alone. A value of the form `${secretRef:<providerId>:<identifier>}` is treated as a key reference and resolved through the `SecretManager` at run time instead, for installations that keep secrets in a vault.

A run that replaces Claude Code with `agentCommand` is not subject to the check: a configured credential is still exported, but the command is otherwise responsible for its own authentication.

### How the Agent Runs

The bundle carries files, not a running agent. Nothing under `META-INF/upgrade-agent` executes inside the OSGi container or the JVM. When a run reaches `running`, the runner unpacks those files into the clone and starts Claude Code as an ordinary operating system process, a child of the Tomcat JVM, with the clone as its working directory. Each step is one process, exactly as a person would type it in a terminal:

1. `/upgrade-init` reads the settings file and prepares the workspace. The runner then expects the `upgrade/<source>-to-<target>` branch to exist and fails the run when it does not.

1. `/upgrade-phase <N>` for phases 1 to 4. After each process exits the runner reads the phase's row in `upgrade-state.md`. `complete` moves on. `blocked` ends the run in the `blocked` status. Anything else means the process ended before the phase did, for example because the agent handed its turn back to wait for a boot it expected to be woken up for, and the runner invokes the same phase again, up to three times, since `/upgrade-phase` resumes an `in-progress` phase. `statusMessage` names the phase and the attempt throughout.

1. After every completed phase the runner pushes the upgrade and phase branches, so an interrupted run has its finished phases on the remote already.

The portal never reads a result file on this path; the tracker the skills maintain is the contract. `/upgrade-run` remains the entry point for a person who wants the same chain from a terminal.

The process inherits the environment of the operating system user Tomcat runs as, plus the variables in [The Agent Environment](#the-agent-environment). The agent's access is that user's access, no more and no less:

- Claude Code authenticates with that user's own login under its home directory, unless the configuration's `claudeCredential` hands it a credential.

- Git reaches the repository with that user's SSH keys and configuration when the run carries no credential, or with the stored key when it does.

- Docker, Gradle, Node.js, and whatever else the phases invoke resolve through that user's `PATH` and permissions.

On a developer machine where Tomcat runs as the developer, everything the developer can do in a terminal, the agent can do. On a server, the portal's operating system user needs a Claude Code login or an API key, `git` and `claude` on its `PATH` or configured by absolute path, network access to the repository, and Docker for the phases that start the portal. The `claude` binary is resolved through the configuration's `claudeCommand`; Tomcat's `PATH` is often shorter than an interactive shell's, so an absolute path is the safe choice.

Each run is one process, and each process gets one hour. The runner never talks to the Anthropic API itself, holds no conversation state, and cannot answer questions the agent might ask. That is why `/upgrade-run` and the headless modes of `/upgrade-init` and `/upgrade-phase` exist: an unanswered question in print mode ends the process, so the skills must never ask one.

### What the Runner Installs

The bundle holds the agent under `META-INF/upgrade-agent`. The runner copies it into the clone in the layout Claude Code reads.

| Bundle path | Clone path |
| --- | --- |
| `skills/<name>` | `.claude/skills/<name>` |
| `reference` | `.claude/reference` |
| `hooks` | `.claude/hooks` |
| `templates/CLAUDE.md.template` | `CLAUDE.md` |
| `templates/upgrade-state.md.template` | `upgrade-state.md` |
| `templates/.mcp.json.template` | `.mcp.json` |

The workspace settings land next to them in `upgrade-run.properties`, and every one of these paths is added to the clone's `.git/info/exclude` so no phase commits them. A `CLAUDE.md` the customer repository already carries is replaced in the clone, because the skills expect the agent's own file there.

### How a Run Ends

A run ends `successful` when phase 4 completes, `blocked` when a phase records that status, `failed` when a phase does not finish within three attempts or a command fails, and `timed-out` when an agent step exceeds the hour it is given or a Git or `gh` command its ten minutes.

Publishing has two parts:

1. The branches. After every completed phase, and again at the end whatever the outcome short of `cancelled`, the runner pushes the upgrade branch and every `phase*` branch to `origin` with the same credential it cloned with. The push is forced: the agent names its branches the same way on every run, so a later run against the same repository replaces the branches an earlier one left behind. On the success and blocked paths a push failure fails the run, because the branches would otherwise vanish with the clone. On the `failed` and `timed-out` paths it is only logged.

1. The pull request. For a `successful` or `blocked` run, when `gh` is installed and logged in for the portal's operating system user, the runner opens one pull request from the most recently committed phase branch against the upgrade branch. The title names the target version and, for a blocked run, the status. The body says how the run ended and points at the `upgrade-notes` directory the phases committed. A failure here is only logged, since the branches are already safe.

The response to a `GET` then carries the branch in `resultBranch` and the pull request in `pullRequestURL`, so the caller finds the result in the repository rather than on the portal host. `workspacePath` names the clone on the host for anyone who needs the raw working tree, for example to inspect `upgrade-run.log` one directory above it.

Cancelling a run kills the agent process along with it, and the run keeps the `cancelled` status. Nothing is published for a cancelled run.

### Resuming a Run

A finished run never changes status again. To carry on from where it stopped, resume it: the portal creates a new run, and the finished run stays as it is. The new run starts at `firstPhase`, which defaults to the phase of the finished run's result branch, so a run blocked in `phase3` resumes at phase 3. Every phase before it is taken as complete.

The runner continues on the finished run's clone when `workspacePath` still exists on its host, which keeps the uncommitted state a phase left behind, such as the derived compose file phase 3 booted the portal with. The agent's working files survive, and the agent itself is unpacked again so the newest skills apply. When the clone is gone, the runner clones the result branch instead and fetches the upgrade branch and the earlier phase branches, which the next phase stacks on.

The runner runs `/upgrade-init` again only when it has to: on a fresh clone, where the excluded files are gone, or when the resumption changed a setting, since only `/upgrade-init` writes `CLAUDE.md`. A reused clone with the same settings already has its upgrade branch, so the runner goes straight to the phases. Either way, it rewrites the phase tracker before invoking the first phase: the rows before it read `complete`, and the rows from it on read `pending`, so `/upgrade-phase` starts the phase over instead of asking whether to resume it. The one row left alone is the first phase's own when it reads `in-progress`, because the finished run stopped inside that phase, and `/upgrade-phase` resumes it where it stopped. The push at the end replaces the phase branch on the remote with whatever the agent committed.

The settings sent with the resumption are merged over the finished run's settings and written to `upgrade-run.properties` as usual. This is how a phase that was `blocked` on a missing input gets its answer: the phase names the key it needs, and the caller supplies it on the resumption. A `null` value removes the key. The merged settings are what the new run stores, so a later resumption of the new run keeps them.

When the finished run already opened a pull request from the same phase branch, the resumed run finds that pull request instead of opening another one, since the branch now holds the new commits.

### The Agent Environment

The agent command runs with the clone as its working directory and inherits the portal's environment plus the variables below. When the run exports a credential, the inherited `ANTHROPIC_*` and `CLAUDE_CODE_*` variables and `CLAUDE_CONFIG_DIR` are removed first; see [Authenticating the Agent](#authenticating-the-agent).

| Variable | Value |
| --- | --- |
| `ANTHROPIC_API_KEY` | The configured credential, when it is an API key. |
| `CLAUDE_CODE_OAUTH_TOKEN` | The configured credential, when it is a `claude setup-token` token. |
| `CLAUDE_CONFIG_DIR` | The configured Claude Code configuration directory, when one is set. |
| `GIT_SSH_COMMAND` | `ssh` pinned to the run's private key, with `BatchMode=yes`, `IdentitiesOnly=yes`, and `StrictHostKeyChecking=accept-new`. Only set when the run carries a credential key reference. |
| `GIT_TERMINAL_PROMPT` | `0`, so Git never blocks on a prompt. |
| `UPGRADE_RUN_LICENSE_PATH` | The path of the DXP license file the run resolved, as it is on the host; the runner does not copy it. Phase 1 places the license in the workspace's deploy overlay and commits it. Only set when a license was resolved. |
| `UPGRADE_RUN_SETTINGS` | The path of `upgrade-run.properties` inside the clone, holding the workspace settings as sorted `key=value` lines. |

Each agent step has one hour to finish, and each Git or `gh` command ten minutes. Output from every command is appended to `upgrade-run.log` inside the run's temp directory, which lives under Tomcat's `temp` folder. The portal log records the exception when a run fails or times out.

### Storing the Credentials

A credential is optional. A run without `credentialKeyReference` leaves Git to the host's own configuration, which is enough for a public HTTPS URL, a `file://` URL, or an SSH URL the portal's OS user can already reach. Either way `ssh` runs in batch mode with keepalives every fifteen seconds, so a prompt or a dead connection fails the command within a minute instead of hanging it. A private repository the host cannot reach on its own needs a stored private key.

The runner never receives a private key in the clear. It arrives as a **key reference**, a string of the form `${secretRef:<providerId>:<identifier>}` that the portal's `SecretManager` resolves at run time. The provider `*` resolves to the active key manager profile's company secret provider. The profile defaults to `db-company-secret`, but no provider by that ID ships in this repository yet; the only `SecretProvider` implementations are the AWS Secrets Manager ones. Until a database backed provider exists, name a registered provider explicitly in the reference.

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

The printed string is what goes in the `credentialKeyReference` field of a run. The configuration's `claudeCredential` accepts such a reference as well, but it also takes the credential itself, so the `SecretManager` is optional there.

### Supplying the License

Phase 3 boots a DXP portal, which needs a license on the target line; without one, it validates the modules on Hypersonic and stops before the checks that need HTTP. The license never goes through the portal: the runner resolves a path on its host and hands that path to the agent in `UPGRADE_RUN_LICENSE_PATH`, and phase 1 copies the file into the workspace's deploy overlay and commits it.

The path is the run's `licensePath` when it names one, and the configuration's `licensePath` otherwise. Either can be a file or a folder. For a folder, the runner takes the first `.xml` file, by name, whose root element is `license` or `licenses`, the same test Liferay's license installer applies. A file that fails that test is never handed over, so the path cannot carry anything else into the workspace. A run whose own `licensePath` holds no license fails while validating; a configured path that holds none only logs a warning, and a configured path that does not exist is skipped quietly; either way the run goes on without a license.

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
	"licensePath": "/opt/liferay/upgrades/license",
	"nodeVersion": "20.18.0",
	"repositoryURL": "git@github.com:acme/liferay-workspace.git",
	"searchVersion": "8.17.4",
	"upgradeSourceVersion": "7.4.13-u92",
	"upgradeTargetJavaVersion": "21",
	"upgradeTargetVersion": "2026.q1.0"
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

A missing required field is rejected before anything is stored. Only `credentialKeyReference` and `licensePath` are optional. A `credentialKeyReference` that is not a key reference is rejected the same way, and so is a `branch` or `repositoryURL` that starts with a dash, which Git would read as an option. A rejection returns `400` with the reason in `title`. The response to an accepted run is the run the portal created, and `externalReferenceCode` is the handle for the two calls below.

| Field | Direction | Meaning |
| --- | --- | --- |
| `branch` | Write | The branch to upgrade. |
| `credentialKeyReference` | Write, optional | The key reference of the private key the runner reaches the repository with. Leave it out when the repository needs no credential. |
| `customerName`, `dbTargetType`, `dbTargetVersion`, `nodeVersion`, `searchVersion`, `upgradeSourceVersion`, `upgradeTargetJavaVersion`, `upgradeTargetVersion` | Write | The workspace settings. See [Workspace Settings](#workspace-settings). |
| `externalReferenceCode` | Read only | The identifier the runner knows the run by. |
| `firstPhase` | Read only | The phase the agent started at. It is 1 unless the run resumed another run. |
| `id` | Read only | The primary key of the stored run. |
| `licensePath` | Write, optional | The path, on the runner's host, of the DXP license file, or of a folder holding it. Leave it out to use the configured license path. See [Supplying the License](#supplying-the-license). |
| `pullRequestURL` | Read only | The pull request the run opened for its result. Empty until `publishing`, and empty when `gh` is not available on the host. |
| `repositoryURL` | Write | The URL of the repository. SSH, HTTPS, and `file://` all work; see [Storing the Credentials](#storing-the-credentials). |
| `resultBranch` | Read only | The branch the run pushed its result to, the most recently committed phase branch. Empty until `publishing`. |
| `status` | Read only | One of the statuses in the table above. |
| `statusMessage` | Read only | What the run was doing when it last reported. |
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
	--request POST \
	--url "${PORTAL_URL}/o/upgrades-lab-agent-remote/v1.0/upgrade-runs/by-external-reference-code/${EXTERNAL_REFERENCE_CODE}/cancel" \
	--user "${PORTAL_USER}:${PORTAL_PASSWORD}"
```

The response is the run with `status` set to `cancelled`. Cancelling a run that already finished does nothing and returns the run as it is.

### Resume a Run

```bash
UPGRADE_RUN_RESUMPTION=$(cat <<'EOF'
{
	"firstPhase": 4,
	"settings": {
		"reference.local.path": "/opt/liferay/liferay-portal-ee"
	}
}
EOF
)

curl \
	--data "${UPGRADE_RUN_RESUMPTION}" \
	--header "Content-Type: application/json" \
	--request POST \
	--url "${PORTAL_URL}/o/upgrades-lab-agent-remote/v1.0/upgrade-runs/by-external-reference-code/${EXTERNAL_REFERENCE_CODE}/resume" \
	--user "${PORTAL_USER}:${PORTAL_PASSWORD}"
```

The run must be in a terminal status, or the call is rejected with `400`. Both body properties are optional, and an empty body resumes at the phase the run stopped in with its settings unchanged. The response is the new run, with its own `externalReferenceCode` to poll. See [Resuming a Run](#resuming-a-run) for what the runner does with it.

| Property | Meaning |
| --- | --- |
| `firstPhase` | The phase the agent starts at, 1 to 4. Leave it out to start at the phase of the finished run's result branch. A phase above 1 needs a result branch, so a run that never cloned can only be resumed from phase 1. |
| `settings` | Workspace settings to add to or replace among the finished run's settings, keyed as in `upgrade-run.properties`. A `null` value removes the key. |

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
			upgradeSourceVersion = "7.4.13-u92";
			upgradeTargetJavaVersion = "21";
			upgradeTargetVersion = "2026.q1.0";
		}
	});

upgradeRun = upgradeRunResource.getUpgradeRunByExternalReferenceCode(
	upgradeRun.getExternalReferenceCode());
```

## Current State

Submit, poll, cancel, and resume work end to end against the local runner. The gaps that remain are listed below.

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

The entity's schema changed while the bundle version stayed at `1.0.0`. On a bundle that already created the `UpgradeRun` table, either drop the table before redeploying the service module, or set `schema.module.build.auto.upgrade=true` in `portal-ext.properties` and restart with the new service module in place: the portal then rebuilds the table from `tables.sql` on startup, keeping its rows. Deploy the module while Tomcat is stopped, since a hot deploy records the new schema without applying it.

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