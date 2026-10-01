/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.runner.local.internal;

import com.liferay.petra.concurrent.NoticeableExecutorService;
import com.liferay.petra.executor.PortalExecutorManager;
import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.security.key.KeyReference;
import com.liferay.portal.security.key.KeyReferenceUtil;
import com.liferay.portal.security.key.secret.Secret;
import com.liferay.portal.security.key.secret.SecretManager;
import com.liferay.upgrades.lab.agent.remote.constants.UpgradeRunConstants;
import com.liferay.upgrades.lab.agent.remote.exception.UpgradeRunnerException;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunRequest;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunner;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunnerState;
import com.liferay.upgrades.lab.agent.remote.runner.local.internal.configuration.LocalUpgradeRunnerConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Albert Gomes Cabral
 */
@Component(service = UpgradeRunner.class)
public class LocalUpgradeRunner implements UpgradeRunner {

	@Override
	public void cancel(String externalReferenceCode)
		throws UpgradeRunnerException {

		LocalUpgradeRun localUpgradeRun = _getLocalUpgradeRun(
			externalReferenceCode);

		if (UpgradeRunConstants.isTerminal(localUpgradeRun.getStatus())) {
			return;
		}

		localUpgradeRun.setUpgradeRunnerState(
			new UpgradeRunnerState(
				null, null, UpgradeRunConstants.STATUS_CANCELLED,
				"The run was cancelled"));

		Future<?> future = localUpgradeRun.getFuture();

		future.cancel(true);
	}

	@Override
	public UpgradeRunnerState getUpgradeRunnerState(
			String externalReferenceCode)
		throws UpgradeRunnerException {

		LocalUpgradeRun localUpgradeRun = _getLocalUpgradeRun(
			externalReferenceCode);

		return localUpgradeRun.getUpgradeRunnerState();
	}

	@Override
	public String submit(UpgradeRunRequest upgradeRunRequest)
		throws UpgradeRunnerException {

		if (upgradeRunRequest == null) {
			throw new UpgradeRunnerException("Upgrade run request is null");
		}

		String externalReferenceCode = PortalUUIDUtil.generate();

		LocalUpgradeRun localUpgradeRun = new LocalUpgradeRun();

		localUpgradeRun.setFuture(
			_noticeableExecutorService.submit(
				() -> _executeUpgradeRun(localUpgradeRun, upgradeRunRequest)));

		_localUpgradeRuns.put(externalReferenceCode, localUpgradeRun);

		return externalReferenceCode;
	}

	@Activate
	protected void activate(BundleContext bundleContext) {
		_bundle = bundleContext.getBundle();

		_noticeableExecutorService = _portalExecutorManager.getPortalExecutor(
			LocalUpgradeRunner.class.getName());
	}

	@Deactivate
	protected void deactivate() {
		for (LocalUpgradeRun localUpgradeRun : _localUpgradeRuns.values()) {
			Future<?> future = localUpgradeRun.getFuture();

			future.cancel(true);
		}

		_localUpgradeRuns.clear();
	}

	private String _createPullRequest(
			Map<String, String> environment,
			LocalUpgradeRunnerConfiguration localUpgradeRunnerConfiguration,
			Path logPath, String resultBranch, String upgradeBranch,
			UpgradeRunnerState upgradeRunnerState, String upgradeTargetVersion,
			Path workspacePath)
		throws Exception {

		try {
			return _getPullRequestURL(
				_executeCommandForOutput(
					environment, logPath, workspacePath,
					ArrayUtil.append(
						localUpgradeRunnerConfiguration.ghCommand(),
						new String[] {
							"pr", "create", "--base", upgradeBranch, "--body",
							_getPullRequestBody(upgradeRunnerState), "--head",
							resultBranch, "--title",
							_getPullRequestTitle(
								upgradeRunnerState, upgradeTargetVersion)
						})));
		}
		catch (Exception exception) {
			String pullRequestURL = _getPullRequestURL(
				_executeCommandForOutput(
					environment, logPath, workspacePath,
					ArrayUtil.append(
						localUpgradeRunnerConfiguration.ghCommand(),
						new String[] {
							"pr", "list", "--base", upgradeBranch, "--head",
							resultBranch, "--json", "url", "--jq", ".[].url"
						})));

			if (pullRequestURL == null) {
				throw exception;
			}

			return pullRequestURL;
		}
	}

	private void _deleteTempFile(Path path) {
		if (path == null) {
			return;
		}

		try {
			Files.deleteIfExists(path);
		}
		catch (IOException ioException) {
			_log.error("Unable to delete " + path, ioException);
		}
	}

	private void _executeCommand(
			Map<String, String> environment, Path logPath, long timeoutSeconds,
			Path workPath, String... arguments)
		throws Exception {

		ProcessBuilder processBuilder = new ProcessBuilder(arguments);

		processBuilder.redirectOutput(
			ProcessBuilder.Redirect.appendTo(logPath.toFile()));

		Process process = _startProcess(environment, processBuilder, workPath);

		_waitFor(arguments, logPath, process, timeoutSeconds);
	}

	private String _executeCommandForOutput(
			Map<String, String> environment, Path logPath, Path workPath,
			String... arguments)
		throws Exception {

		// Reading the output straight from the process would block until the
		// process closes it, and only then would the wait and its timeout
		// start. Let the process write to a file instead, and read it once
		// the wait is over.

		Path outputPath = Files.createTempFile(
			logPath.getParent(), "upgrade-run-output-", ".log");

		String output;

		try {
			ProcessBuilder processBuilder = new ProcessBuilder(arguments);

			processBuilder.redirectOutput(outputPath.toFile());

			Process process = _startProcess(
				environment, processBuilder, workPath);

			_waitFor(arguments, logPath, process, _COMMAND_TIMEOUT_SECONDS);
		}
		finally {
			output = Files.readString(outputPath, StandardCharsets.UTF_8);

			Files.writeString(
				logPath, output, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.APPEND);

			_deleteTempFile(outputPath);
		}

		return output;
	}

	private void _executeUpgradeRun(
		LocalUpgradeRun localUpgradeRun, UpgradeRunRequest upgradeRunRequest) {

		Map<String, String> environment = null;
		Path logPath = null;
		Path privateKeyPath = null;
		Path workspacePath = null;

		try {
			localUpgradeRun.setUpgradeRunnerState(
				new UpgradeRunnerState(
					null, null, UpgradeRunConstants.STATUS_VALIDATING,
					"Validating the repository and the branch"));

			if (Validator.isNotNull(
					upgradeRunRequest.getCredentialKeyReference())) {

				privateKeyPath = _writePrivateKey(upgradeRunRequest);
			}

			LocalUpgradeRunnerConfiguration localUpgradeRunnerConfiguration =
				_configurationProvider.getCompanyConfiguration(
					LocalUpgradeRunnerConfiguration.class,
					upgradeRunRequest.getCompanyId());

			environment = _getEnvironment(
				_getLicensePath(
					upgradeRunRequest.getLicensePath(),
					localUpgradeRunnerConfiguration),
				localUpgradeRunnerConfiguration, privateKeyPath,
				upgradeRunRequest);

			Path reusableWorkspacePath = _getReusableWorkspacePath(
				upgradeRunRequest);

			if (reusableWorkspacePath == null) {
				Path workPath = Files.createTempDirectory("upgrade-run-");

				logPath = workPath.resolve("upgrade-run.log");

				String checkoutBranch = _getCheckoutBranch(upgradeRunRequest);

				_executeCommand(
					environment, logPath, _COMMAND_TIMEOUT_SECONDS, workPath,
					"git", "ls-remote", "--exit-code",
					upgradeRunRequest.getRepositoryURL(), checkoutBranch);

				workspacePath = workPath.resolve(_WORKSPACE_DIR_NAME);

				localUpgradeRun.setUpgradeRunnerState(
					new UpgradeRunnerState(
						null, null, UpgradeRunConstants.STATUS_CLONING,
						"Cloning " + upgradeRunRequest.getRepositoryURL(),
						workspacePath.toString()));

				_executeCommand(
					environment, logPath, _COMMAND_TIMEOUT_SECONDS, workPath,
					"git", "clone", "--branch", checkoutBranch, "--depth", "1",
					"--filter=blob:none", upgradeRunRequest.getRepositoryURL(),
					_WORKSPACE_DIR_NAME);

				if (upgradeRunRequest.getFirstPhase() >
						UpgradeRunConstants.PHASE_FIRST) {

					_executeCommand(
						environment, logPath, _COMMAND_TIMEOUT_SECONDS,
						workspacePath, "git", "fetch", "--depth", "1",
						"--update-head-ok", "origin",
						StringBundler.concat(
							"+refs/heads/",
							UpgradeRunConstants.BRANCH_PREFIX_PHASE,
							"*:refs/heads/",
							UpgradeRunConstants.BRANCH_PREFIX_PHASE, "*"),
						StringBundler.concat(
							"+refs/heads/",
							UpgradeRunConstants.BRANCH_PREFIX_UPGRADE,
							"*:refs/heads/",
							UpgradeRunConstants.BRANCH_PREFIX_UPGRADE, "*"));
				}
			}
			else {
				workspacePath = reusableWorkspacePath;

				logPath = workspacePath.resolveSibling("upgrade-run.log");

				localUpgradeRun.setUpgradeRunnerState(
					new UpgradeRunnerState(
						null, null, UpgradeRunConstants.STATUS_CLONING,
						"Reusing the workspace at " + workspacePath,
						workspacePath.toString()));
			}

			Path settingsPath = workspacePath.resolve("upgrade-run.properties");

			boolean settingsChanged = _writeSettings(
				upgradeRunRequest.getSettings(), settingsPath);

			environment.put("UPGRADE_RUN_SETTINGS", settingsPath.toString());

			_installAgent(
				reusableWorkspacePath != null,
				_bundle.findEntries(_AGENT_RESOURCE_PATH, "*", true),
				workspacePath);

			String[] agentCommand =
				localUpgradeRunnerConfiguration.agentCommand();

			UpgradeRunnerState upgradeRunnerState;

			if (ArrayUtil.isNotEmpty(agentCommand)) {
				localUpgradeRun.setUpgradeRunnerState(
					new UpgradeRunnerState(
						null, null, UpgradeRunConstants.STATUS_RUNNING,
						"Upgrading to " +
							upgradeRunRequest.getUpgradeTargetVersion(),
						workspacePath.toString()));

				_executeCommand(
					environment, logPath, _AGENT_TIMEOUT_SECONDS, workspacePath,
					agentCommand);

				upgradeRunnerState = new UpgradeRunnerState(
					null, null, UpgradeRunConstants.STATUS_SUCCESSFUL,
					"The upgrade agent finished", workspacePath.toString());
			}
			else {
				upgradeRunnerState = _runPhases(
					environment, upgradeRunRequest.getFirstPhase(),
					(reusableWorkspacePath == null) || settingsChanged,
					localUpgradeRun, localUpgradeRunnerConfiguration, logPath,
					workspacePath);
			}

			localUpgradeRun.setUpgradeRunnerState(
				new UpgradeRunnerState(
					null, upgradeRunnerState.getResultBranch(),
					UpgradeRunConstants.STATUS_PUBLISHING,
					"Publishing the result", workspacePath.toString()));

			localUpgradeRun.setUpgradeRunnerState(
				_publish(
					environment, localUpgradeRunnerConfiguration, logPath,
					upgradeRunnerState, upgradeRunRequest, workspacePath));
		}
		catch (InterruptedException interruptedException) {

			// The wait that was interrupted already destroyed its child
			// process, and cancel recorded the terminal state.

			if (_log.isDebugEnabled()) {
				_log.debug(interruptedException);
			}

			if (_log.isInfoEnabled()) {
				_log.info(
					StringBundler.concat(
						"Upgrade run ", upgradeRunRequest.getUpgradeRunId(),
						" was cancelled"));
			}
		}
		catch (TimedOutException timedOutException) {
			_log.error(
				"Unable to carry out upgrade run " +
					upgradeRunRequest.getUpgradeRunId(),
				timedOutException);

			localUpgradeRun.setUpgradeRunnerState(
				_publishPartially(
					environment, logPath, UpgradeRunConstants.STATUS_TIMED_OUT,
					timedOutException.getMessage(), upgradeRunRequest,
					workspacePath));
		}
		catch (Exception exception) {
			if (UpgradeRunConstants.isTerminal(localUpgradeRun.getStatus())) {
				if (_log.isInfoEnabled()) {
					_log.info(
						StringBundler.concat(
							"Upgrade run ", upgradeRunRequest.getUpgradeRunId(),
							" was cancelled: ", exception.getMessage()));
				}

				return;
			}

			_log.error(
				"Unable to carry out upgrade run " +
					upgradeRunRequest.getUpgradeRunId(),
				exception);

			localUpgradeRun.setUpgradeRunnerState(
				_publishPartially(
					environment, logPath, UpgradeRunConstants.STATUS_FAILED,
					exception.getMessage(), upgradeRunRequest, workspacePath));
		}
		finally {
			_deleteTempFile(privateKeyPath);
		}
	}

	private Path _findLicensePath(Path path) throws Exception {
		if (Files.isRegularFile(path)) {
			if (_isLicense(path)) {
				return path;
			}

			return null;
		}

		if (!Files.isDirectory(path)) {
			return null;
		}

		List<Path> paths = new ArrayList<>();

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
				path, "*.xml")) {

			for (Path childPath : directoryStream) {
				paths.add(childPath);
			}
		}

		Collections.sort(paths);

		for (Path childPath : paths) {
			if (Files.isRegularFile(childPath) && _isLicense(childPath)) {
				return childPath;
			}
		}

		return null;
	}

	private Path _getAgentTargetPath(String relativePath, Path workspacePath) {
		String agentTemplatesPathPrefix =
			_AGENT_TEMPLATES_PATH + StringPool.SLASH;

		if (relativePath.startsWith(agentTemplatesPathPrefix)) {
			String name = relativePath.substring(
				agentTemplatesPathPrefix.length());

			if (!name.endsWith(_AGENT_TEMPLATE_SUFFIX)) {
				return null;
			}

			return workspacePath.resolve(
				name.substring(
					0, name.length() - _AGENT_TEMPLATE_SUFFIX.length()));
		}

		for (String agentPath : new String[] {"hooks", "reference", "skills"}) {
			if (relativePath.startsWith(agentPath + StringPool.SLASH)) {
				return workspacePath.resolve(".claude/" + relativePath);
			}
		}

		return null;
	}

	private List<String> _getBranches(
			Map<String, String> environment, Path logPath, Path workspacePath)
		throws Exception {

		return TransformUtil.transformToList(
			StringUtil.splitLines(
				_executeCommandForOutput(
					environment, logPath, workspacePath, "git", "for-each-ref",
					"--format=%(refname:short)", "--sort=-committerdate",
					"refs/heads/" + UpgradeRunConstants.BRANCH_PREFIX_PHASE +
						"*",
					"refs/heads/" + UpgradeRunConstants.BRANCH_PREFIX_UPGRADE +
						"*")),
			line -> {
				String branch = line.trim();

				if (Validator.isNull(branch)) {
					return null;
				}

				return branch;
			});
	}

	private String _getCheckoutBranch(UpgradeRunRequest upgradeRunRequest) {
		if (upgradeRunRequest.getFirstPhase() >
				UpgradeRunConstants.PHASE_FIRST) {

			return upgradeRunRequest.getResultBranch();
		}

		return upgradeRunRequest.getBranch();
	}

	private String[] _getClaudeCommand(String[] claudeCommand, String skill) {
		return ArrayUtil.append(
			claudeCommand,
			new String[] {
				"--permission-mode", "bypassPermissions", "--print", skill
			});
	}

	private Map<String, String> _getEnvironment(
			Path licensePath,
			LocalUpgradeRunnerConfiguration localUpgradeRunnerConfiguration,
			Path privateKeyPath, UpgradeRunRequest upgradeRunRequest)
		throws Exception {

		Map<String, String> environment = _getGitEnvironment(privateKeyPath);

		if (licensePath != null) {
			environment.put("UPGRADE_RUN_LICENSE_PATH", licensePath.toString());
		}

		String claudeConfigDirectory =
			localUpgradeRunnerConfiguration.claudeConfigDirectory();

		if (Validator.isNotNull(claudeConfigDirectory)) {
			environment.put("CLAUDE_CONFIG_DIR", claudeConfigDirectory);
		}

		String claudeCredential =
			localUpgradeRunnerConfiguration.claudeCredential();

		if (Validator.isNotNull(claudeCredential)) {
			KeyReference keyReference = KeyReferenceUtil.parseKeyReference(
				claudeCredential);

			if (keyReference != null) {
				try (Secret secret = _secretManager.getSecret(
						upgradeRunRequest.getCompanyId(), keyReference)) {

					claudeCredential = new String(secret.getChars());
				}
			}

			if (claudeCredential.startsWith("sk-ant-oat")) {
				environment.put("CLAUDE_CODE_OAUTH_TOKEN", claudeCredential);
			}
			else {
				environment.put("ANTHROPIC_API_KEY", claudeCredential);
			}

			return environment;
		}

		if (ArrayUtil.isNotEmpty(
				localUpgradeRunnerConfiguration.agentCommand())) {

			return environment;
		}

		if (!localUpgradeRunnerConfiguration.hostCredentialsEnabled()) {
			throw new UpgradeRunnerException(
				"No Anthropic credential is configured and host credentials " +
					"are disabled");
		}

		return environment;
	}

	private Map<String, String> _getGitEnvironment(Path privateKeyPath) {
		return HashMapBuilder.put(
			"GIT_SSH_COMMAND",
			() -> {
				if (privateKeyPath == null) {
					return _SSH_COMMAND;
				}

				return StringBundler.concat(
					_SSH_COMMAND, " -i ", privateKeyPath,
					" -o IdentitiesOnly=yes -o ",
					"StrictHostKeyChecking=accept-new");
			}
		).put(
			"GIT_TERMINAL_PROMPT", "0"
		).build();
	}

	private Path _getLicensePath(
			String licensePathString,
			LocalUpgradeRunnerConfiguration localUpgradeRunnerConfiguration)
		throws Exception {

		if (Validator.isNotNull(licensePathString)) {
			Path licensePath = _findLicensePath(Path.of(licensePathString));

			if (licensePath == null) {
				throw new UpgradeRunnerException(
					"No DXP license was found in " + licensePathString);
			}

			return licensePath;
		}

		String configuredLicensePathString =
			localUpgradeRunnerConfiguration.licensePath();

		if (Validator.isNull(configuredLicensePathString)) {
			return null;
		}

		Path configuredLicensePath = Path.of(configuredLicensePathString);

		if (Files.notExists(configuredLicensePath)) {
			return null;
		}

		Path licensePath = _findLicensePath(configuredLicensePath);

		if ((licensePath == null) && _log.isWarnEnabled()) {
			_log.warn(
				"No DXP license was found in " + configuredLicensePathString);
		}

		return licensePath;
	}

	private LocalUpgradeRun _getLocalUpgradeRun(String externalReferenceCode)
		throws UpgradeRunnerException {

		LocalUpgradeRun localUpgradeRun = _localUpgradeRuns.get(
			externalReferenceCode);

		if (localUpgradeRun == null) {
			throw new UpgradeRunnerException(
				"No run was found for external reference code " +
					externalReferenceCode);
		}

		return localUpgradeRun;
	}

	private String _getPhaseStatus(int phase, String trackerMarkdown) {
		for (String line : StringUtil.splitLines(trackerMarkdown)) {
			String row = line.trim();

			if (!row.startsWith(StringPool.PIPE) ||
				!row.endsWith(StringPool.PIPE)) {

				continue;
			}

			List<String> cells = _getTrackerCells(row);

			if ((cells.size() < 3) ||
				(GetterUtil.getInteger(cells.get(0)) != phase)) {

				continue;
			}

			return _getTrackerStatus(cells);
		}

		return _PHASE_STATUS_PENDING;
	}

	private String _getPullRequestBody(UpgradeRunnerState upgradeRunnerState) {
		if (upgradeRunnerState.getStatus() ==
				UpgradeRunConstants.STATUS_SUCCESSFUL) {

			return StringBundler.concat(
				"The upgrade agent completed every phase.\n\n",
				upgradeRunnerState.getStatusMessage(),
				"\n\nThe phase summaries are under upgrade-notes on this ",
				"branch.");
		}

		return StringBundler.concat(
			"The upgrade agent stopped before finishing and needs a person to ",
			"continue.\n\n", upgradeRunnerState.getStatusMessage(),
			"\n\nThe phase summaries and the items flagged for review are ",
			"under upgrade-notes on this branch.");
	}

	private String _getPullRequestTitle(
		UpgradeRunnerState upgradeRunnerState, String upgradeTargetVersion) {

		if (upgradeRunnerState.getStatus() ==
				UpgradeRunConstants.STATUS_SUCCESSFUL) {

			return "Upgrade to " + upgradeTargetVersion;
		}

		return StringBundler.concat(
			"Upgrade to ", upgradeTargetVersion, " (",
			UpgradeRunConstants.getStatusLabel(upgradeRunnerState.getStatus()),
			")");
	}

	private String _getPullRequestURL(String output) {
		String pullRequestURL = null;

		for (String line : StringUtil.splitLines(output)) {
			line = line.trim();

			if (line.startsWith("https://")) {
				pullRequestURL = line;
			}
		}

		return pullRequestURL;
	}

	private String _getResultBranch(List<String> branches) {
		for (String branch : branches) {
			if (branch.startsWith(UpgradeRunConstants.BRANCH_PREFIX_PHASE)) {
				return branch;
			}
		}

		return _getUpgradeBranch(branches);
	}

	private Path _getReusableWorkspacePath(
		UpgradeRunRequest upgradeRunRequest) {

		String workspacePathString = upgradeRunRequest.getWorkspacePath();

		if (workspacePathString == null) {
			return null;
		}

		Path workspacePath = Path.of(workspacePathString);

		if ((workspacePath.getParent() != null) &&
			Files.isDirectory(workspacePath.resolve(".git"))) {

			return workspacePath;
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				StringBundler.concat(
					"Cloning upgrade run ", upgradeRunRequest.getUpgradeRunId(),
					" again because ", workspacePathString, " is gone"));
		}

		return null;
	}

	private List<String> _getTrackerCells(String row) {
		List<String> cells = new ArrayList<>();

		int offset = 1;

		while (true) {
			int index = row.indexOf(CharPool.PIPE, offset);

			if (index < 0) {
				return cells;
			}

			cells.add(row.substring(offset, index));

			offset = index + 1;
		}
	}

	private String _getTrackerStatus(List<String> cells) {
		for (String cell : cells) {
			String status = StringUtil.toLowerCase(
				StringUtil.removeChar(StringUtil.trim(cell), CharPool.PRIME));

			if (status.equals(_PHASE_STATUS_TODO)) {
				return _PHASE_STATUS_PENDING;
			}

			if (ArrayUtil.contains(_PHASE_STATUSES, status)) {
				return status;
			}
		}

		return _PHASE_STATUS_PENDING;
	}

	private String _getUpgradeBranch(List<String> branches) {
		for (String branch : branches) {
			if (branch.startsWith(UpgradeRunConstants.BRANCH_PREFIX_UPGRADE)) {
				return branch;
			}
		}

		return null;
	}

	private void _installAgent(
			boolean reused, Enumeration<URL> urlEnumeration, Path workspacePath)
		throws IOException {

		if (urlEnumeration == null) {
			throw new IOException(
				"The runner bundle does not carry the upgrade agent");
		}

		String agentResourcePathPrefix =
			_AGENT_RESOURCE_PATH + StringPool.SLASH;

		for (URL url : Collections.list(urlEnumeration)) {
			String pathString = url.getPath();

			int index = pathString.indexOf(agentResourcePathPrefix);

			if (pathString.endsWith(StringPool.SLASH) || (index < 0)) {
				continue;
			}

			String relativePath = pathString.substring(
				index + agentResourcePathPrefix.length());

			Path targetPath = _getAgentTargetPath(relativePath, workspacePath);

			if ((targetPath == null) ||
				(reused &&
				 relativePath.startsWith(
					 _AGENT_TEMPLATES_PATH + StringPool.SLASH) &&
				 Files.exists(targetPath))) {

				continue;
			}

			Files.createDirectories(targetPath.getParent());

			try (InputStream inputStream = url.openStream()) {
				Files.copy(
					inputStream, targetPath,
					StandardCopyOption.REPLACE_EXISTING);
			}
		}

		Path excludePath = workspacePath.resolve(".git/info/exclude");

		Files.createDirectories(excludePath.getParent());

		List<String> excludedPaths = ListUtil.fromArray(
			".claude/", ".mcp.json", "CLAUDE.md",
			"upgrade-run-result.properties", "upgrade-run.properties",
			"upgrade-state.md");

		if (Files.exists(excludePath)) {
			excludedPaths.removeAll(
				Files.readAllLines(excludePath, StandardCharsets.UTF_8));
		}

		if (!excludedPaths.isEmpty()) {
			Files.write(
				excludePath, excludedPaths, StandardCharsets.UTF_8,
				StandardOpenOption.APPEND, StandardOpenOption.CREATE);
		}
	}

	private boolean _isLicense(Path path) throws Exception {
		String xml = Files.readString(path, StandardCharsets.UTF_8);

		int index = 0;

		while (true) {
			index = xml.indexOf(CharPool.LESS_THAN, index);

			if (index < 0) {
				return false;
			}

			if (xml.startsWith("<!--", index)) {
				index = xml.indexOf("-->", index);

				if (index < 0) {
					return false;
				}
			}
			else if (xml.startsWith("<?", index) ||
					 xml.startsWith("<!", index)) {

				index++;
			}
			else {
				int end = index + 1;

				while ((end < xml.length()) &&
					   (Character.isLetterOrDigit(xml.charAt(end)) ||
						(xml.charAt(end) == CharPool.COLON) ||
						(xml.charAt(end) == CharPool.DASH) ||
						(xml.charAt(end) == CharPool.PERIOD) ||
						(xml.charAt(end) == CharPool.UNDERLINE))) {

					end++;
				}

				String name = xml.substring(index + 1, end);

				if (name.equals("license") || name.equals("licenses")) {
					return true;
				}

				return false;
			}
		}
	}

	private UpgradeRunnerState _publish(
			Map<String, String> environment,
			LocalUpgradeRunnerConfiguration localUpgradeRunnerConfiguration,
			Path logPath, UpgradeRunnerState upgradeRunnerState,
			UpgradeRunRequest upgradeRunRequest, Path workspacePath)
		throws Exception {

		List<String> branches = _pushBranches(
			environment, logPath, workspacePath);

		String upgradeBranch = _getUpgradeBranch(branches);

		if (upgradeBranch == null) {
			return new UpgradeRunnerState(
				null, null, upgradeRunnerState.getStatus(),
				upgradeRunnerState.getStatusMessage(),
				workspacePath.toString());
		}

		String resultBranch = _getResultBranch(branches);

		String pullRequestURL = null;

		if (!Objects.equals(resultBranch, upgradeBranch)) {
			try {
				pullRequestURL = _createPullRequest(
					environment, localUpgradeRunnerConfiguration, logPath,
					resultBranch, upgradeBranch, upgradeRunnerState,
					upgradeRunRequest.getUpgradeTargetVersion(), workspacePath);
			}
			catch (Exception exception) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						"Unable to open the pull request for upgrade run " +
							upgradeRunRequest.getUpgradeRunId(),
						exception);
				}
			}
		}

		return new UpgradeRunnerState(
			pullRequestURL, resultBranch, upgradeRunnerState.getStatus(),
			upgradeRunnerState.getStatusMessage(), workspacePath.toString());
	}

	private UpgradeRunnerState _publishPartially(
		Map<String, String> environment, Path logPath, int status,
		String statusMessage, UpgradeRunRequest upgradeRunRequest,
		Path workspacePath) {

		if (workspacePath == null) {
			return new UpgradeRunnerState(
				null, null, status, statusMessage, null);
		}

		String resultBranch = null;

		try {
			resultBranch = _getResultBranch(
				_pushBranches(environment, logPath, workspacePath));
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to push the branches of upgrade run " +
						upgradeRunRequest.getUpgradeRunId(),
					exception);
			}
		}

		return new UpgradeRunnerState(
			null, resultBranch, status, statusMessage,
			workspacePath.toString());
	}

	private List<String> _pushBranches(
			Map<String, String> environment, Path logPath, Path workspacePath)
		throws Exception {

		List<String> branches = _getBranches(
			environment, logPath, workspacePath);

		if (branches.isEmpty()) {
			return branches;
		}

		_executeCommand(
			environment, logPath, _COMMAND_TIMEOUT_SECONDS, workspacePath,
			ArrayUtil.append(
				new String[] {"git", "push", "--force", "origin"},
				ArrayUtil.toStringArray(branches)));

		return branches;
	}

	private void _resetTracker(int firstPhase, Path trackerPath)
		throws Exception {

		if (Files.notExists(trackerPath)) {
			return;
		}

		Files.write(
			trackerPath,
			TransformUtil.transform(
				Files.readAllLines(trackerPath, StandardCharsets.UTF_8),
				line -> _resetTrackerRow(firstPhase, line)),
			StandardCharsets.UTF_8);
	}

	private String _resetTrackerRow(int firstPhase, String line) {
		String row = line.trim();

		if (!row.startsWith(StringPool.PIPE) ||
			!row.endsWith(StringPool.PIPE)) {

			return line;
		}

		List<String> cells = _getTrackerCells(row);

		if (cells.size() < 3) {
			return line;
		}

		int phase = GetterUtil.getInteger(cells.get(0));

		if ((phase < UpgradeRunConstants.PHASE_FIRST) ||
			(phase > UpgradeRunConstants.PHASE_LAST)) {

			return line;
		}

		String status = _PHASE_STATUS_PENDING;

		if (phase < firstPhase) {
			status = _PHASE_STATUS_COMPLETE;
		}
		else if ((phase == firstPhase) &&
				 _PHASE_STATUS_IN_PROGRESS.equals(_getTrackerStatus(cells))) {

			return line;
		}

		for (int i = 1; i < cells.size(); i++) {
			String cell = StringUtil.toLowerCase(
				StringUtil.removeChar(
					StringUtil.trim(cells.get(i)), CharPool.PRIME));

			if (!cell.equals(_PHASE_STATUS_TODO) &&
				!ArrayUtil.contains(_PHASE_STATUSES, cell)) {

				continue;
			}

			if ((phase < firstPhase) && cell.equals(_PHASE_STATUS_SKIPPED)) {
				continue;
			}

			cells.set(
				i,
				StringBundler.concat(
					StringPool.SPACE, status, StringPool.SPACE));
		}

		StringBundler sb = new StringBundler((2 * cells.size()) + 1);

		sb.append(StringPool.PIPE);

		for (String cell : cells) {
			sb.append(cell);
			sb.append(StringPool.PIPE);
		}

		return sb.toString();
	}

	private UpgradeRunnerState _runPhases(
			Map<String, String> environment, int firstPhase,
			boolean initRequired, LocalUpgradeRun localUpgradeRun,
			LocalUpgradeRunnerConfiguration localUpgradeRunnerConfiguration,
			Path logPath, Path workspacePath)
		throws Exception {

		localUpgradeRun.setUpgradeRunnerState(
			new UpgradeRunnerState(
				null, null, UpgradeRunConstants.STATUS_RUNNING,
				"Initializing the workspace", workspacePath.toString()));

		String resultBranch = null;

		if (!initRequired) {
			resultBranch = _getUpgradeBranch(
				_getBranches(environment, logPath, workspacePath));
		}

		String[] claudeCommand =
			localUpgradeRunnerConfiguration.claudeCommand();

		if (resultBranch == null) {
			_executeCommand(
				environment, logPath, _AGENT_TIMEOUT_SECONDS, workspacePath,
				_getClaudeCommand(claudeCommand, "/upgrade-init"));

			resultBranch = _getUpgradeBranch(
				_getBranches(environment, logPath, workspacePath));
		}

		if (resultBranch == null) {
			throw new UpgradeRunnerException(
				"The upgrade agent did not initialize the workspace. See " +
					logPath + ".");
		}

		Path trackerPath = workspacePath.resolve("upgrade-state.md");

		_resetTracker(firstPhase, trackerPath);

		if (firstPhase > UpgradeRunConstants.PHASE_FIRST) {
			resultBranch = _getResultBranch(
				_getBranches(environment, logPath, workspacePath));
		}

		for (int phase = firstPhase; phase <= UpgradeRunConstants.PHASE_LAST;
			 phase++) {

			String phaseName = _PHASE_NAMES[phase - 1];

			String status = _PHASE_STATUS_PENDING;

			for (int attempt = 1; attempt <= _MAX_PHASE_ATTEMPTS; attempt++) {
				StringBundler sb = new StringBundler(8);

				sb.append("Phase ");
				sb.append(phase);
				sb.append(" of ");
				sb.append(UpgradeRunConstants.PHASE_LAST);
				sb.append(": ");
				sb.append(phaseName);

				if (attempt > 1) {
					sb.append(", attempt ");
					sb.append(attempt);
				}

				localUpgradeRun.setUpgradeRunnerState(
					new UpgradeRunnerState(
						null, resultBranch, UpgradeRunConstants.STATUS_RUNNING,
						sb.toString(), workspacePath.toString()));

				_executeCommand(
					environment, logPath, _AGENT_TIMEOUT_SECONDS, workspacePath,
					_getClaudeCommand(
						claudeCommand, "/upgrade-phase " + phase));

				status = _getPhaseStatus(
					phase,
					Files.readString(trackerPath, StandardCharsets.UTF_8));

				if (status.equals(_PHASE_STATUS_COMPLETE) ||
					status.equals(_PHASE_STATUS_BLOCKED) ||
					status.equals(_PHASE_STATUS_SKIPPED)) {

					break;
				}
			}

			resultBranch = _getResultBranch(
				_pushBranches(environment, logPath, workspacePath));

			if (status.equals(_PHASE_STATUS_BLOCKED)) {
				return new UpgradeRunnerState(
					null, resultBranch, UpgradeRunConstants.STATUS_BLOCKED,
					StringBundler.concat(
						"Phase ", phase, ", ", phaseName,
						", is blocked. The items flagged for review are in ",
						"upgrade-notes on branch ", resultBranch, "."),
					workspacePath.toString());
			}

			if (!status.equals(_PHASE_STATUS_COMPLETE) &&
				!status.equals(_PHASE_STATUS_SKIPPED)) {

				throw new UpgradeRunnerException(
					StringBundler.concat(
						"Phase ", phase, ", ", phaseName,
						", did not finish after ", _MAX_PHASE_ATTEMPTS,
						" attempts. Its last status was \"", status, "\"."));
			}
		}

		return new UpgradeRunnerState(
			null, resultBranch, UpgradeRunConstants.STATUS_SUCCESSFUL,
			"The upgrade agent completed every phase",
			workspacePath.toString());
	}

	private Process _startProcess(
			Map<String, String> environment, ProcessBuilder processBuilder,
			Path workPath)
		throws Exception {

		processBuilder.directory(workPath.toFile());
		processBuilder.redirectErrorStream(true);

		Map<String, String> processEnvironment = processBuilder.environment();

		if (environment.containsKey("ANTHROPIC_API_KEY") ||
			environment.containsKey("CLAUDE_CODE_OAUTH_TOKEN")) {

			Set<String> names = processEnvironment.keySet();

			names.removeIf(
				name ->
					name.startsWith("ANTHROPIC_") ||
					name.startsWith("CLAUDE_CODE_") ||
					name.equals("CLAUDE_CONFIG_DIR"));
		}

		processEnvironment.putAll(environment);

		Process process = processBuilder.start();

		// Nothing is ever written to the child's standard input, and Claude
		// Code waits for it before starting. Hand it end of file right away.

		OutputStream outputStream = process.getOutputStream();

		outputStream.close();

		return process;
	}

	private void _waitFor(
			String[] arguments, Path logPath, Process process,
			long timeoutSeconds)
		throws Exception {

		boolean finished;

		try {
			finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
		}
		catch (InterruptedException interruptedException) {
			process.destroyForcibly();

			throw interruptedException;
		}

		if (!finished) {
			process.destroyForcibly();

			throw new TimedOutException(
				StringBundler.concat(
					"Command \"", StringUtil.merge(arguments, StringPool.SPACE),
					"\" did not finish within ", timeoutSeconds, " seconds"));
		}

		if (process.exitValue() != 0) {
			throw new UpgradeRunnerException(
				StringBundler.concat(
					"Command \"", StringUtil.merge(arguments, StringPool.SPACE),
					"\" exited with ", process.exitValue(), ". See ", logPath,
					"."));
		}
	}

	private Path _writePrivateKey(UpgradeRunRequest upgradeRunRequest)
		throws Exception {

		Set<PosixFilePermission> posixFilePermissions =
			PosixFilePermissions.fromString("rw-------");

		Path privateKeyPath = Files.createTempFile(
			"upgrade-run-key-", null,
			PosixFilePermissions.asFileAttribute(posixFilePermissions));

		try (Secret secret = _secretManager.getSecret(
				upgradeRunRequest.getCompanyId(),
				KeyReferenceUtil.toKeyReference(
					upgradeRunRequest.getCredentialKeyReference()))) {

			Files.write(privateKeyPath, secret.getBytes());
		}
		catch (Exception exception) {
			_deleteTempFile(privateKeyPath);

			throw exception;
		}

		return privateKeyPath;
	}

	private boolean _writeSettings(
			Map<String, String> settings, Path settingsPath)
		throws IOException {

		StringBundler sb = new StringBundler(4 * settings.size());

		for (Map.Entry<String, String> entry :
				new TreeMap<>(settings).entrySet()) {

			sb.append(entry.getKey());
			sb.append(CharPool.EQUAL);
			sb.append(entry.getValue());
			sb.append(CharPool.NEW_LINE);
		}

		String content = sb.toString();

		if (Files.exists(settingsPath) &&
			content.equals(Files.readString(settingsPath))) {

			return false;
		}

		Files.writeString(settingsPath, content);

		return true;
	}

	private static final String _AGENT_RESOURCE_PATH = "META-INF/upgrade-agent";

	private static final String _AGENT_TEMPLATE_SUFFIX = ".template";

	private static final String _AGENT_TEMPLATES_PATH = "templates";

	private static final long _AGENT_TIMEOUT_SECONDS = 3600;

	private static final long _COMMAND_TIMEOUT_SECONDS = 600;

	private static final int _MAX_PHASE_ATTEMPTS = 3;

	private static final String[] _PHASE_NAMES = {
		"Upgrade Environment", "Fix Compile", "Fix Startup", "Check Reindex"
	};

	private static final String _PHASE_STATUS_BLOCKED = "blocked";

	private static final String _PHASE_STATUS_COMPLETE = "complete";

	private static final String _PHASE_STATUS_IN_PROGRESS = "in-progress";

	private static final String _PHASE_STATUS_PENDING = "pending";

	private static final String _PHASE_STATUS_SKIPPED = "skipped";

	private static final String _PHASE_STATUS_TODO = "todo";

	private static final String[] _PHASE_STATUSES = {
		_PHASE_STATUS_BLOCKED, _PHASE_STATUS_COMPLETE, "deferred",
		_PHASE_STATUS_IN_PROGRESS, _PHASE_STATUS_PENDING, _PHASE_STATUS_SKIPPED
	};

	private static final String _SSH_COMMAND =
		"ssh -o BatchMode=yes -o ServerAliveCountMax=3 -o " +
			"ServerAliveInterval=15";

	private static final String _WORKSPACE_DIR_NAME = "workspace";

	private static final Log _log = LogFactoryUtil.getLog(
		LocalUpgradeRunner.class);

	private Bundle _bundle;

	@Reference
	private ConfigurationProvider _configurationProvider;

	private final Map<String, LocalUpgradeRun> _localUpgradeRuns =
		new ConcurrentHashMap<>();
	private NoticeableExecutorService _noticeableExecutorService;

	@Reference
	private PortalExecutorManager _portalExecutorManager;

	@Reference
	private SecretManager _secretManager;

	private static class LocalUpgradeRun {

		public Future<?> getFuture() {
			return _future;
		}

		public int getStatus() {
			return _upgradeRunnerState.getStatus();
		}

		public UpgradeRunnerState getUpgradeRunnerState() {
			return _upgradeRunnerState;
		}

		public void setFuture(Future<?> future) {
			_future = future;
		}

		public synchronized void setUpgradeRunnerState(
			UpgradeRunnerState upgradeRunnerState) {

			if (UpgradeRunConstants.isTerminal(
					_upgradeRunnerState.getStatus())) {

				return;
			}

			_upgradeRunnerState = upgradeRunnerState;
		}

		private volatile Future<?> _future;
		private volatile UpgradeRunnerState _upgradeRunnerState =
			new UpgradeRunnerState(
				null, null, UpgradeRunConstants.STATUS_QUEUED,
				"The run is queued");

	}

	private static class TimedOutException extends UpgradeRunnerException {

		private TimedOutException(String msg) {
			super(msg);
		}

	}

}