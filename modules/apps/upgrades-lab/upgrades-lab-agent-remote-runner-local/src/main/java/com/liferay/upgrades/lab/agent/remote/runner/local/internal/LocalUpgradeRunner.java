/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.runner.local.internal;

import com.liferay.petra.concurrent.NoticeableExecutorService;
import com.liferay.petra.executor.PortalExecutorManager;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.security.key.KeyReferenceUtil;
import com.liferay.portal.security.key.secret.Secret;
import com.liferay.portal.security.key.secret.SecretManager;
import com.liferay.upgrades.lab.agent.remote.constants.UpgradeRunConstants;
import com.liferay.upgrades.lab.agent.remote.exception.UpgradeRunnerException;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunRequest;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunner;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunnerState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
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

		// Record the cancellation before interrupting the worker, so that
		// whatever the worker reports afterward finds a terminal state

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

		String externalReferenceCode = UUID.randomUUID(
		).toString();

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

			if (future != null) {
				future.cancel(true);
			}
		}

		_localUpgradeRuns.clear();
	}

	private String _createPullRequest(
			Map<String, String> environment, Path logPath, Path clonePath,
			String upgradeBranch, String resultBranch, String targetRelease,
			UpgradeRunnerState upgradeRunnerState)
		throws Exception {

		String[] ghCommand = PropsUtil.getArray(_PROPS_KEY_GH_COMMAND);

		if (ArrayUtil.isEmpty(ghCommand)) {
			ghCommand = new String[] {"gh"};
		}

		String output = _executeCommandForOutput(
			environment, logPath, clonePath,
			ArrayUtil.append(
				ghCommand,
				new String[] {
					"pr", "create", "--base", upgradeBranch, "--head",
					resultBranch, "--title",
					_getPullRequestTitle(targetRelease, upgradeRunnerState),
					"--body", _getPullRequestBody(upgradeRunnerState)
				}));

		return _getPullRequestURL(output);
	}

	private void _deleteIfExists(Path path) {
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
			Map<String, String> environment, Path logPath, Path workPath,
			String... arguments)
		throws Exception {

		ProcessBuilder processBuilder = new ProcessBuilder(arguments);

		processBuilder.redirectOutput(
			ProcessBuilder.Redirect.appendTo(logPath.toFile()));

		Process process = _startProcess(processBuilder, environment, workPath);

		_waitFor(process, arguments, logPath);
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

		String output = null;

		try {
			ProcessBuilder processBuilder = new ProcessBuilder(arguments);

			processBuilder.redirectOutput(outputPath.toFile());

			Process process = _startProcess(
				processBuilder, environment, workPath);

			_waitFor(process, arguments, logPath);
		}
		finally {
			output = Files.readString(outputPath, StandardCharsets.UTF_8);

			Files.writeString(
				logPath, output, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.APPEND);

			_deleteIfExists(outputPath);
		}

		return output;
	}

	private void _executeUpgradeRun(
		LocalUpgradeRun localUpgradeRun, UpgradeRunRequest upgradeRunRequest) {

		Path clonePath = null;
		Map<String, String> environment = null;
		Path logPath = null;
		Path privateKeyPath = null;

		try {
			localUpgradeRun.setUpgradeRunnerState(
				new UpgradeRunnerState(
					null, null, UpgradeRunConstants.STATUS_VALIDATING,
					"Validating the repository and the branch"));

			if (Validator.isNotNull(
					upgradeRunRequest.getCredentialKeyReference())) {

				privateKeyPath = _writePrivateKey(upgradeRunRequest);
			}

			Path workPath = Files.createTempDirectory("upgrade-run-");

			logPath = workPath.resolve(_LOG_FILE_NAME);

			environment = _getEnvironment(privateKeyPath, upgradeRunRequest);

			_executeCommand(
				environment, logPath, workPath, "git", "ls-remote",
				"--exit-code", upgradeRunRequest.getRepositoryURL(),
				upgradeRunRequest.getBranch());

			clonePath = workPath.resolve(_WORK_DIR_NAME);

			localUpgradeRun.setUpgradeRunnerState(
				new UpgradeRunnerState(
					null, null, UpgradeRunConstants.STATUS_CLONING,
					"Cloning " + upgradeRunRequest.getRepositoryURL(),
					clonePath.toString()));

			_executeCommand(
				environment, logPath, workPath, "git", "clone", "--branch",
				upgradeRunRequest.getBranch(), "--depth", "1",
				"--filter=blob:none", upgradeRunRequest.getRepositoryURL(),
				_WORK_DIR_NAME);

			Path settingsPath = _writeSettings(
				clonePath, upgradeRunRequest.getSettings());

			environment.put(
				_ENVIRONMENT_SETTINGS_PATH, settingsPath.toString());

			_installAgent(
				clonePath,
				_bundle.findEntries(_AGENT_RESOURCE_PATH, "*", true));

			UpgradeRunnerState upgradeRunnerState = null;

			String[] agentCommand = PropsUtil.getArray(
				_PROPS_KEY_AGENT_COMMAND);

			if (ArrayUtil.isNotEmpty(agentCommand)) {
				localUpgradeRun.setUpgradeRunnerState(
					new UpgradeRunnerState(
						null, null, UpgradeRunConstants.STATUS_RUNNING,
						"Upgrading to " + upgradeRunRequest.getTargetRelease(),
						clonePath.toString()));

				_executeCommand(environment, logPath, clonePath, agentCommand);

				upgradeRunnerState = new UpgradeRunnerState(
					null, null, UpgradeRunConstants.STATUS_SUCCESSFUL,
					"The upgrade agent finished", clonePath.toString());
			}
			else {
				upgradeRunnerState = _runPhases(
					localUpgradeRun, environment, logPath, clonePath);
			}

			localUpgradeRun.setUpgradeRunnerState(
				new UpgradeRunnerState(
					null, upgradeRunnerState.getResultBranch(),
					UpgradeRunConstants.STATUS_PUBLISHING,
					"Publishing the result", clonePath.toString()));

			localUpgradeRun.setUpgradeRunnerState(
				_publish(
					environment, logPath, clonePath, upgradeRunRequest,
					upgradeRunnerState));
		}
		catch (InterruptedException interruptedException) {

			// The wait that was interrupted already destroyed its child
			// process, and cancel recorded the terminal state

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
					environment, logPath, clonePath, upgradeRunRequest,
					UpgradeRunConstants.STATUS_TIMED_OUT,
					timedOutException.getMessage()));
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
					environment, logPath, clonePath, upgradeRunRequest,
					UpgradeRunConstants.STATUS_FAILED, exception.getMessage()));
		}
		finally {
			_deleteIfExists(privateKeyPath);
		}
	}

	private String[] _getAgentCommand(String[] claudeCommand, String skill) {
		return ArrayUtil.append(claudeCommand, new String[] {"--print", skill});
	}

	private Path _getAgentTargetPath(Path clonePath, String relativePath) {
		if (relativePath.startsWith(_AGENT_TEMPLATES_PATH)) {
			String name = relativePath.substring(
				_AGENT_TEMPLATES_PATH.length());

			if (!name.endsWith(_TEMPLATE_SUFFIX)) {
				return null;
			}

			return clonePath.resolve(
				name.substring(0, name.length() - _TEMPLATE_SUFFIX.length()));
		}

		for (String agentPath : _AGENT_PATHS) {
			if (relativePath.startsWith(agentPath)) {
				return clonePath.resolve(
					".claude"
				).resolve(
					relativePath
				);
			}
		}

		return null;
	}

	private List<String> _getBranches(
			Map<String, String> environment, Path logPath, Path clonePath)
		throws Exception {

		List<String> branches = new ArrayList<>();

		String output = _executeCommandForOutput(
			environment, logPath, clonePath, "git", "for-each-ref",
			"--format=%(refname:short)", "--sort=-committerdate",
			"refs/heads/" + _PHASE_BRANCH_PREFIX + "*",
			"refs/heads/" + _UPGRADE_BRANCH_PREFIX);

		for (String line : StringUtil.splitLines(output)) {
			line = line.trim();

			if (Validator.isNotNull(line)) {
				branches.add(line);
			}
		}

		return branches;
	}

	private Map<String, String> _getEnvironment(
			Path privateKeyPath, UpgradeRunRequest upgradeRunRequest)
		throws Exception {

		Map<String, String> environment = _getGitEnvironment(privateKeyPath);

		String apiKeyReference = GetterUtil.getString(
			PropsUtil.get(_PROPS_KEY_API_KEY_REFERENCE));

		if (Validator.isNull(apiKeyReference)) {
			return environment;
		}

		try (Secret secret = _secretManager.getSecret(
				upgradeRunRequest.getCompanyId(),
				KeyReferenceUtil.toKeyReference(apiKeyReference))) {

			environment.put("ANTHROPIC_API_KEY", new String(secret.getChars()));
		}

		return environment;
	}

	private Map<String, String> _getGitEnvironment(Path privateKeyPath) {
		return HashMapBuilder.put(
			"GIT_SSH_COMMAND",
			() -> {
				if (privateKeyPath == null) {
					return null;
				}

				return StringBundler.concat(
					"ssh -i ", privateKeyPath, " -o BatchMode=yes -o ",
					"IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new");
			}
		).put(
			"GIT_TERMINAL_PROMPT", "0"
		).build();
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

	private String _getPhaseStatus(String tracker, int phase) {
		for (String line : StringUtil.splitLines(tracker)) {
			String row = line.trim();

			if (!row.startsWith(StringPool.PIPE)) {
				continue;
			}

			row = row.substring(1);

			if (row.endsWith(StringPool.PIPE)) {
				row = row.substring(0, row.length() - 1);
			}

			String[] cells = StringUtil.split(row, CharPool.PIPE);

			if ((cells.length < 3) ||
				!Objects.equals(cells[0].trim(), String.valueOf(phase))) {

				continue;
			}

			for (String cell : cells) {
				String status = StringUtil.toLowerCase(
					StringUtil.removeChar(cell.trim(), CharPool.PRIME));

				if (status.equals("todo")) {
					return _PHASE_STATUS_PENDING;
				}

				if (ArrayUtil.contains(_PHASE_STATUSES, status)) {
					return status;
				}
			}
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
		String targetRelease, UpgradeRunnerState upgradeRunnerState) {

		if (upgradeRunnerState.getStatus() ==
				UpgradeRunConstants.STATUS_SUCCESSFUL) {

			return "Upgrade to " + targetRelease;
		}

		return StringBundler.concat(
			"Upgrade to ", targetRelease, " (",
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
			if (branch.startsWith(_PHASE_BRANCH_PREFIX)) {
				return branch;
			}
		}

		return _getUpgradeBranch(branches);
	}

	private String _getUpgradeBranch(List<String> branches) {
		for (String branch : branches) {
			if (branch.startsWith(_UPGRADE_BRANCH_PREFIX)) {
				return branch;
			}
		}

		return null;
	}

	private String _getWorkspacePath(Path clonePath) {
		if (clonePath == null) {
			return null;
		}

		return clonePath.toString();
	}

	private void _installAgent(Path clonePath, Enumeration<URL> urlEnumeration)
		throws IOException {

		if (urlEnumeration == null) {
			throw new IOException(
				"The runner bundle does not carry the upgrade agent");
		}

		String marker = _AGENT_RESOURCE_PATH + StringPool.SLASH;

		while (urlEnumeration.hasMoreElements()) {
			URL url = urlEnumeration.nextElement();

			String path = url.getPath();

			int index = path.indexOf(marker);

			if (path.endsWith(StringPool.SLASH) || (index < 0)) {
				continue;
			}

			Path targetPath = _getAgentTargetPath(
				clonePath, path.substring(index + marker.length()));

			if (targetPath == null) {
				continue;
			}

			Files.createDirectories(targetPath.getParent());

			try (InputStream inputStream = url.openStream()) {
				Files.copy(
					inputStream, targetPath,
					StandardCopyOption.REPLACE_EXISTING);
			}
		}

		Path excludePath = clonePath.resolve(".git/info/exclude");

		Files.createDirectories(excludePath.getParent());

		Files.write(
			excludePath, Arrays.asList(_EXCLUDED_AGENT_PATHS),
			StandardCharsets.UTF_8, StandardOpenOption.APPEND,
			StandardOpenOption.CREATE);
	}

	private UpgradeRunnerState _publish(
			Map<String, String> environment, Path logPath, Path clonePath,
			UpgradeRunRequest upgradeRunRequest,
			UpgradeRunnerState upgradeRunnerState)
		throws Exception {

		List<String> branches = _pushBranches(environment, logPath, clonePath);

		String upgradeBranch = _getUpgradeBranch(branches);

		if (upgradeBranch == null) {
			return new UpgradeRunnerState(
				null, null, upgradeRunnerState.getStatus(),
				upgradeRunnerState.getStatusMessage(), clonePath.toString());
		}

		String resultBranch = _getResultBranch(branches);

		String pullRequestURL = null;

		if (!Objects.equals(resultBranch, upgradeBranch)) {
			try {
				pullRequestURL = _createPullRequest(
					environment, logPath, clonePath, upgradeBranch,
					resultBranch, upgradeRunRequest.getTargetRelease(),
					upgradeRunnerState);
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
			upgradeRunnerState.getStatusMessage(), clonePath.toString());
	}

	private UpgradeRunnerState _publishPartially(
		Map<String, String> environment, Path logPath, Path clonePath,
		UpgradeRunRequest upgradeRunRequest, int status, String statusMessage) {

		String resultBranch = null;

		if (clonePath != null) {
			try {
				resultBranch = _getResultBranch(
					_pushBranches(environment, logPath, clonePath));
			}
			catch (Exception exception) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						"Unable to push the branches of upgrade run " +
							upgradeRunRequest.getUpgradeRunId(),
						exception);
				}
			}
		}

		return new UpgradeRunnerState(
			null, resultBranch, status, statusMessage,
			_getWorkspacePath(clonePath));
	}

	private List<String> _pushBranches(
			Map<String, String> environment, Path logPath, Path clonePath)
		throws Exception {

		List<String> branches = _getBranches(environment, logPath, clonePath);

		if (branches.isEmpty()) {
			return branches;
		}

		_executeCommand(
			environment, logPath, clonePath,
			ArrayUtil.append(
				new String[] {"git", "push", "--force", "origin"},
				branches.toArray(new String[0])));

		return branches;
	}

	private UpgradeRunnerState _runPhases(
			LocalUpgradeRun localUpgradeRun, Map<String, String> environment,
			Path logPath, Path clonePath)
		throws Exception {

		String[] claudeCommand = PropsUtil.getArray(_PROPS_KEY_CLAUDE_COMMAND);

		if (ArrayUtil.isEmpty(claudeCommand)) {
			claudeCommand = new String[] {"claude"};
		}

		String workspacePath = clonePath.toString();

		localUpgradeRun.setUpgradeRunnerState(
			new UpgradeRunnerState(
				null, null, UpgradeRunConstants.STATUS_RUNNING,
				"Initializing the workspace", workspacePath));

		_executeCommand(
			environment, logPath, clonePath,
			_getAgentCommand(claudeCommand, "/upgrade-init"));

		String resultBranch = _getUpgradeBranch(
			_getBranches(environment, logPath, clonePath));

		if (resultBranch == null) {
			throw new UpgradeRunnerException(
				"The upgrade agent did not initialize the workspace. See " +
					logPath + ".");
		}

		Path trackerPath = clonePath.resolve(_TRACKER_FILE_NAME);

		for (int phase = 1; phase <= _PHASE_NAMES.length; phase++) {
			String phaseName = _PHASE_NAMES[phase - 1];

			String status = _PHASE_STATUS_PENDING;

			for (int attempt = 1; attempt <= _MAX_PHASE_ATTEMPTS; attempt++) {
				StringBundler sb = new StringBundler(8);

				sb.append("Phase ");
				sb.append(phase);
				sb.append(" of ");
				sb.append(_PHASE_NAMES.length);
				sb.append(": ");
				sb.append(phaseName);

				if (attempt > 1) {
					sb.append(", attempt ");
					sb.append(attempt);
				}

				localUpgradeRun.setUpgradeRunnerState(
					new UpgradeRunnerState(
						null, resultBranch, UpgradeRunConstants.STATUS_RUNNING,
						sb.toString(), workspacePath));

				_executeCommand(
					environment, logPath, clonePath,
					_getAgentCommand(claudeCommand, "/upgrade-phase " + phase));

				status = _getPhaseStatus(
					Files.readString(trackerPath, StandardCharsets.UTF_8),
					phase);

				if (status.equals(_PHASE_STATUS_COMPLETE) ||
					status.equals(_PHASE_STATUS_BLOCKED) ||
					status.equals("skipped")) {

					break;
				}
			}

			resultBranch = _getResultBranch(
				_pushBranches(environment, logPath, clonePath));

			if (status.equals(_PHASE_STATUS_BLOCKED)) {
				return new UpgradeRunnerState(
					null, resultBranch, UpgradeRunConstants.STATUS_BLOCKED,
					StringBundler.concat(
						"Phase ", phase, ", ", phaseName,
						", is blocked. The items flagged for review are in ",
						"upgrade-notes on branch ", resultBranch, "."),
					workspacePath);
			}

			if (!status.equals(_PHASE_STATUS_COMPLETE) &&
				!status.equals("skipped")) {

				throw new UpgradeRunnerException(
					StringBundler.concat(
						"Phase ", phase, ", ", phaseName,
						", did not finish after ", _MAX_PHASE_ATTEMPTS,
						" attempts. Its last status was \"", status, "\"."));
			}
		}

		return new UpgradeRunnerState(
			null, resultBranch, UpgradeRunConstants.STATUS_SUCCESSFUL,
			"The upgrade agent completed every phase", workspacePath);
	}

	private Process _startProcess(
			ProcessBuilder processBuilder, Map<String, String> environment,
			Path workPath)
		throws Exception {

		processBuilder.directory(workPath.toFile());
		processBuilder.redirectErrorStream(true);

		Map<String, String> processEnvironment = processBuilder.environment();

		processEnvironment.putAll(environment);

		Process process = processBuilder.start();

		// Nothing is ever written to the child's standard input, and Claude
		// Code waits for it before starting. Hand it end of file right away.

		OutputStream outputStream = process.getOutputStream();

		outputStream.close();

		return process;
	}

	private void _waitFor(Process process, String[] arguments, Path logPath)
		throws Exception {

		boolean finished = false;

		try {
			finished = process.waitFor(_TIMEOUT, TimeUnit.SECONDS);
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
					"\" did not finish within ", _TIMEOUT, " seconds"));
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

		return privateKeyPath;
	}

	private Path _writeSettings(Path clonePath, Map<String, String> settings)
		throws IOException {

		StringBundler sb = new StringBundler(4 * settings.size());

		for (Map.Entry<String, String> entry :
				new TreeMap<>(settings).entrySet()) {

			sb.append(entry.getKey());
			sb.append(CharPool.EQUAL);
			sb.append(entry.getValue());
			sb.append(CharPool.NEW_LINE);
		}

		Path settingsPath = clonePath.resolve(_SETTINGS_FILE_NAME);

		Files.writeString(settingsPath, sb.toString());

		return settingsPath;
	}

	private static final String[] _AGENT_PATHS = {
		"hooks/", "reference/", "skills/"
	};

	private static final String _AGENT_RESOURCE_PATH = "META-INF/upgrade-agent";

	private static final String _AGENT_TEMPLATES_PATH = "templates/";

	private static final String _ENVIRONMENT_SETTINGS_PATH =
		"UPGRADE_RUN_SETTINGS";

	private static final String[] _EXCLUDED_AGENT_PATHS = {
		".claude/", ".mcp.json", "CLAUDE.md", "upgrade-run-result.properties",
		"upgrade-run.properties", "upgrade-state.md"
	};

	private static final String _LOG_FILE_NAME = "upgrade-run.log";

	private static final int _MAX_PHASE_ATTEMPTS = 3;

	private static final String _PHASE_BRANCH_PREFIX = "phase";

	private static final String[] _PHASE_NAMES = {
		"Upgrade Environment", "Fix Compile", "Fix Startup", "Check Reindex"
	};

	private static final String _PHASE_STATUS_BLOCKED = "blocked";

	private static final String _PHASE_STATUS_COMPLETE = "complete";

	private static final String _PHASE_STATUS_PENDING = "pending";

	private static final String[] _PHASE_STATUSES = {
		"blocked", "complete", "deferred", "in-progress", "pending", "skipped"
	};

	private static final String _PROPS_KEY_AGENT_COMMAND =
		"upgrades.lab.agent.remote.runner.local.agent.command";

	private static final String _PROPS_KEY_API_KEY_REFERENCE =
		"upgrades.lab.agent.remote.runner.local.api.key.reference";

	private static final String _PROPS_KEY_CLAUDE_COMMAND =
		"upgrades.lab.agent.remote.runner.local.claude.command";

	private static final String _PROPS_KEY_GH_COMMAND =
		"upgrades.lab.agent.remote.runner.local.gh.command";

	private static final String _SETTINGS_FILE_NAME = "upgrade-run.properties";

	private static final String _TEMPLATE_SUFFIX = ".template";

	private static final long _TIMEOUT = 3600;

	private static final String _TRACKER_FILE_NAME = "upgrade-state.md";

	private static final String _UPGRADE_BRANCH_PREFIX = "upgrade/";

	private static final String _WORK_DIR_NAME = "workspace";

	private static final Log _log = LogFactoryUtil.getLog(
		LocalUpgradeRunner.class);

	private Bundle _bundle;
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

			// A terminal state is final. A worker that was cancelled, or that
			// reports late, must not overwrite it.

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