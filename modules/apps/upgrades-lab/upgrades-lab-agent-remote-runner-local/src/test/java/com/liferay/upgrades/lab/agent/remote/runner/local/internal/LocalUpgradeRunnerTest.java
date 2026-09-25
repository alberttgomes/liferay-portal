/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.runner.local.internal;

import com.liferay.petra.concurrent.NoticeableThreadPoolExecutor;
import com.liferay.petra.concurrent.ThreadPoolHandlerAdapter;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.security.key.KeyReference;
import com.liferay.portal.security.key.secret.Secret;
import com.liferay.portal.security.key.secret.SecretManager;
import com.liferay.portal.security.key.secret.exception.SecretException;
import com.liferay.portal.test.rule.LiferayUnitTestRule;
import com.liferay.upgrades.lab.agent.remote.constants.UpgradeRunConstants;
import com.liferay.upgrades.lab.agent.remote.constants.UpgradeRunSettingsKeys;
import com.liferay.upgrades.lab.agent.remote.exception.UpgradeRunnerException;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunRequest;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunnerState;

import java.io.File;

import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.osgi.framework.Bundle;

/**
 * @author Albert Gomes Cabral
 */
public class LocalUpgradeRunnerTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() throws Exception {
		_testPath = Files.createTempDirectory("upgrade-run-test-");

		_agentURLs = _createAgent();

		_localUpgradeRunner = new LocalUpgradeRunner();

		ReflectionTestUtil.setFieldValue(
			_localUpgradeRunner, "_bundle",
			ProxyUtil.newProxyInstance(
				Bundle.class.getClassLoader(), new Class<?>[] {Bundle.class},
				(proxy, method, arguments) -> {
					if (Objects.equals(method.getName(), "findEntries") &&
						(_agentURLs != null)) {

						return Collections.enumeration(_agentURLs);
					}

					return null;
				}));
		ReflectionTestUtil.setFieldValue(
			_localUpgradeRunner, "_secretManager", new TestSecretManager(null));

		_createRepository();

		PropsUtil.set(_PROPS_KEY_CLAUDE_COMMAND, _createClaudeCommand());
		PropsUtil.set(_PROPS_KEY_GH_COMMAND, _createGHCommand());
	}

	@After
	public void tearDown() throws Exception {
		PropsUtil.set(_PROPS_KEY_CLAUDE_COMMAND, StringPool.BLANK);
		PropsUtil.set(_PROPS_KEY_GH_COMMAND, StringPool.BLANK);

		_deleteDirectory(_testPath.toFile());
	}

	@Test
	public void testCancel() throws Exception {
		Assert.assertThrows(
			UpgradeRunnerException.class,
			() -> _localUpgradeRunner.cancel(RandomTestUtil.randomString()));

		// Block the worker inside the secret lookup, the first call the run
		// makes, so the cancel arrives while the run is still validating

		CountDownLatch countDownLatch = new CountDownLatch(1);

		ReflectionTestUtil.setFieldValue(
			_localUpgradeRunner, "_secretManager",
			new TestSecretManager(countDownLatch));

		NoticeableThreadPoolExecutor noticeableThreadPoolExecutor =
			_setUpExecutorService();

		try {
			String externalReferenceCode = _localUpgradeRunner.submit(
				_getUpgradeRunRequest("${secretRef:*:acme-deploy-key}"));

			Assert.assertTrue(countDownLatch.await(10, TimeUnit.SECONDS));

			UpgradeRunnerState upgradeRunnerState =
				_localUpgradeRunner.getUpgradeRunnerState(
					externalReferenceCode);

			Assert.assertEquals(
				UpgradeRunConstants.STATUS_VALIDATING,
				upgradeRunnerState.getStatus());

			_localUpgradeRunner.cancel(externalReferenceCode);

			// Let the interrupted worker run its error handling to the end
			// before checking that it left the cancellation alone

			noticeableThreadPoolExecutor.shutdown();

			Assert.assertTrue(
				noticeableThreadPoolExecutor.awaitTermination(
					10, TimeUnit.SECONDS));

			upgradeRunnerState = _localUpgradeRunner.getUpgradeRunnerState(
				externalReferenceCode);

			Assert.assertEquals(
				UpgradeRunConstants.STATUS_CANCELLED,
				upgradeRunnerState.getStatus());
			Assert.assertEquals(
				"The run was cancelled", upgradeRunnerState.getStatusMessage());

			// A worker that was between commands when the cancel arrived
			// reports its next state late. The cancellation has to survive it.

			Map<String, Object> localUpgradeRuns =
				ReflectionTestUtil.getFieldValue(
					_localUpgradeRunner, "_localUpgradeRuns");

			ReflectionTestUtil.invoke(
				localUpgradeRuns.get(externalReferenceCode),
				"setUpgradeRunnerState",
				new Class<?>[] {UpgradeRunnerState.class},
				new UpgradeRunnerState(
					null, null, UpgradeRunConstants.STATUS_CLONING,
					"Late report"));

			upgradeRunnerState = _localUpgradeRunner.getUpgradeRunnerState(
				externalReferenceCode);

			Assert.assertEquals(
				UpgradeRunConstants.STATUS_CANCELLED,
				upgradeRunnerState.getStatus());

			_localUpgradeRunner.cancel(externalReferenceCode);

			upgradeRunnerState = _localUpgradeRunner.getUpgradeRunnerState(
				externalReferenceCode);

			Assert.assertEquals(
				UpgradeRunConstants.STATUS_CANCELLED,
				upgradeRunnerState.getStatus());
		}
		finally {
			noticeableThreadPoolExecutor.shutdownNow();
		}
	}

	@Test
	public void testGetUpgradeRunnerState() {
		Assert.assertThrows(
			UpgradeRunnerException.class,
			() -> _localUpgradeRunner.getUpgradeRunnerState(
				RandomTestUtil.randomString()));
	}

	@Test
	public void testSubmit() throws Exception {
		Assert.assertThrows(
			UpgradeRunnerException.class,
			() -> _localUpgradeRunner.submit(null));

		_writePhaseStatuses(
			"complete", "`phase2-x` -> `phase2-y` | Complete", "skipped",
			"complete");

		UpgradeRunnerState upgradeRunnerState = _submit(
			_getUpgradeRunRequest("${secretRef:*:acme-deploy-key}"));

		Assert.assertEquals(
			upgradeRunnerState.getStatusMessage(),
			UpgradeRunConstants.STATUS_SUCCESSFUL,
			upgradeRunnerState.getStatus());
		Assert.assertEquals(
			"The upgrade agent completed every phase",
			upgradeRunnerState.getStatusMessage());
		Assert.assertEquals(
			"https://github.com/acme/workspace/pull/7",
			upgradeRunnerState.getPullRequestURL());
		Assert.assertEquals("phase4", upgradeRunnerState.getResultBranch());

		Assert.assertEquals(
			Arrays.asList(
				"--print /upgrade-init", "--print /upgrade-phase 1",
				"--print /upgrade-phase 2", "--print /upgrade-phase 3",
				"--print /upgrade-phase 4"),
			_readLines("claude-arguments"));

		// The agent sees the settings, sorted by key, and the git environment

		Assert.assertEquals(
			Arrays.asList(
				"customer.name=acme", "db.target.type=mysql",
				"db.target.version=8.0", "node.version=20.18.0",
				"search.version=8.17.4", "upgrade.source.version=7.4.13-u92",
				"upgrade.target.java.version=21"),
			_readLines("settings"));

		List<String> environment = _readLines("environment");

		Assert.assertEquals("0", environment.get(0));

		String gitSSHCommand = environment.get(1);

		Assert.assertTrue(gitSSHCommand, gitSSHCommand.startsWith("ssh -i "));
		Assert.assertTrue(
			gitSSHCommand, gitSSHCommand.contains("-o BatchMode=yes"));
		Assert.assertTrue(
			gitSSHCommand, gitSSHCommand.contains("-o IdentitiesOnly=yes"));

		// The private key only lives for the length of the run

		String privateKeyPath = StringUtil.extractFirst(
			gitSSHCommand.substring(7), StringPool.SPACE);

		Assert.assertFalse(Files.exists(Path.of(privateKeyPath)));

		// The agent is installed into the clone and kept out of git

		Path clonePath = Path.of(upgradeRunnerState.getWorkspacePath());

		_assertInstalled(
			clonePath, ".claude/hooks/guard_bash.py", "hooks/guard_bash.py");
		_assertInstalled(
			clonePath, ".claude/reference/README.md", "reference/README.md");
		_assertInstalled(
			clonePath, ".claude/skills/upgrade-run/SKILL.md",
			"skills/upgrade-run/SKILL.md");
		_assertInstalled(
			clonePath, ".mcp.json", "templates/.mcp.json.template");
		_assertInstalled(
			clonePath, "CLAUDE.md", "templates/CLAUDE.md.template");

		Assert.assertFalse(
			Files.exists(clonePath.resolve(".claude/README.md")));
		Assert.assertFalse(Files.exists(clonePath.resolve("README.md")));
		Assert.assertFalse(Files.exists(clonePath.resolve("notes.txt")));

		List<String> excludedPaths = Files.readAllLines(
			clonePath.resolve(".git/info/exclude"), StandardCharsets.UTF_8);

		for (String excludedPath :
				new String[] {
					".claude/", ".mcp.json", "CLAUDE.md",
					"upgrade-run-result.properties", "upgrade-run.properties",
					"upgrade-state.md"
				}) {

			Assert.assertTrue(
				excludedPaths.toString(), excludedPaths.contains(excludedPath));
		}

		// Every branch the agent made reaches the remote, and the pull request
		// asks to merge the last phase into the upgrade branch

		Assert.assertEquals(
			Arrays.asList(
				"main", "phase1", "phase2", "phase3", "phase4",
				"upgrade/7.4-to-2026.q1.0"),
			_getRemoteBranches());

		String ghArguments = _read("gh-arguments");

		Assert.assertTrue(
			ghArguments,
			ghArguments.startsWith(
				StringBundler.concat(
					"pr\ncreate\n--base\nupgrade/7.4-to-2026.q1.0\n--head\n",
					"phase4\n--title\nUpgrade to 2026.q1.0\n--body\n",
					"The upgrade agent completed every phase.\n")));
		Assert.assertTrue(ghArguments, ghArguments.contains("upgrade-notes"));
	}

	@Test
	public void testSubmitBlocked() throws Exception {
		_writePhaseStatuses("complete", "blocked", "complete", "complete");

		UpgradeRunnerState upgradeRunnerState = _submit(
			_getUpgradeRunRequest(null));

		Assert.assertEquals(
			upgradeRunnerState.getStatusMessage(),
			UpgradeRunConstants.STATUS_BLOCKED, upgradeRunnerState.getStatus());
		Assert.assertEquals(
			"Phase 2, Fix Compile, is blocked. The items flagged for review " +
				"are in upgrade-notes on branch phase2.",
			upgradeRunnerState.getStatusMessage());
		Assert.assertEquals(
			"https://github.com/acme/workspace/pull/7",
			upgradeRunnerState.getPullRequestURL());
		Assert.assertEquals("phase2", upgradeRunnerState.getResultBranch());

		Assert.assertEquals(
			Arrays.asList(
				"--print /upgrade-init", "--print /upgrade-phase 1",
				"--print /upgrade-phase 2"),
			_readLines("claude-arguments"));

		List<String> environment = _readLines("environment");

		Assert.assertEquals(environment.toString(), 1, environment.size());
		Assert.assertEquals("0", environment.get(0));

		String ghArguments = _read("gh-arguments");

		Assert.assertTrue(
			ghArguments, ghArguments.contains("--head\nphase2\n"));
		Assert.assertTrue(
			ghArguments,
			ghArguments.contains("--title\nUpgrade to 2026.q1.0 (blocked)\n"));
		Assert.assertTrue(
			ghArguments,
			ghArguments.contains(
				"--body\nThe upgrade agent stopped before finishing"));
		Assert.assertTrue(
			ghArguments, ghArguments.contains("flagged for review"));
	}

	@Test
	public void testSubmitUnfinishedPhase() throws Exception {
		_writePhaseStatuses("TODO | in-progress");

		UpgradeRunnerState upgradeRunnerState = _submit(
			_getUpgradeRunRequest(null));

		Assert.assertEquals(
			UpgradeRunConstants.STATUS_FAILED, upgradeRunnerState.getStatus());
		Assert.assertEquals(
			"Phase 1, Upgrade Environment, did not finish after 3 attempts. " +
				"Its last status was \"pending\".",
			upgradeRunnerState.getStatusMessage());
		Assert.assertNull(upgradeRunnerState.getPullRequestURL());
		Assert.assertEquals("phase1", upgradeRunnerState.getResultBranch());

		Assert.assertEquals(
			Arrays.asList(
				"--print /upgrade-init", "--print /upgrade-phase 1",
				"--print /upgrade-phase 1", "--print /upgrade-phase 1"),
			_readLines("claude-arguments"));
		Assert.assertEquals(
			Arrays.asList("main", "phase1", "upgrade/7.4-to-2026.q1.0"),
			_getRemoteBranches());
		Assert.assertFalse(Files.exists(_testPath.resolve("gh-arguments")));
	}

	@Test
	public void testSubmitWithoutAgent() throws Exception {
		_agentURLs = null;

		UpgradeRunnerState upgradeRunnerState = _submit(
			_getUpgradeRunRequest(null));

		Assert.assertEquals(
			UpgradeRunConstants.STATUS_FAILED, upgradeRunnerState.getStatus());
		Assert.assertEquals(
			"The runner bundle does not carry the upgrade agent",
			upgradeRunnerState.getStatusMessage());
		Assert.assertNull(upgradeRunnerState.getResultBranch());

		Assert.assertFalse(Files.exists(_testPath.resolve("claude-arguments")));
	}

	private void _assertInstalled(
			Path clonePath, String relativePath, String content)
		throws Exception {

		Assert.assertEquals(
			content, Files.readString(clonePath.resolve(relativePath)));
	}

	private List<URL> _createAgent() throws Exception {
		Path agentPath = _testPath.resolve("bundle/META-INF/upgrade-agent");

		List<URL> urls = new ArrayList<>();

		urls.add(_toURL(agentPath));

		for (String relativePath :
				new String[] {
					"README.md", "hooks/guard_bash.py", "reference/README.md",
					"skills/upgrade-run/SKILL.md",
					"templates/.mcp.json.template",
					"templates/CLAUDE.md.template", "templates/notes.txt"
				}) {

			Path path = agentPath.resolve(relativePath);

			Files.createDirectories(path.getParent());

			Files.writeString(path, relativePath);

			urls.add(_toURL(path));
		}

		return urls;
	}

	private String _createClaudeCommand() throws Exception {

		// Stand in for Claude Code. Initializing makes the upgrade branch, and
		// each phase commits to its own branch and records the next status of
		// phase-statuses in the tracker.

		return _writeScript(
			"claude",
			new String[] {
				"printf '%s\\n' \"$*\" >> \"$dir/claude-arguments\"",
				"commit() {",
				"\tGIT_AUTHOR_DATE=\"$1\" GIT_COMMITTER_DATE=\"$1\" git \\",
				"\t\t-c user.email=agent@liferay.com -c user.name=Agent \\",
				"\t\tcommit --allow-empty --message \"$2\" --quiet", "}",
				"case \"$2\" in", "/upgrade-init)",
				"\tcp \"$UPGRADE_RUN_SETTINGS\" \"$dir/settings\"",
				"\tprintf '%s\\n' \"$GIT_TERMINAL_PROMPT\" \\",
				"\t\t> \"$dir/environment\"",
				"\tif [ -n \"$GIT_SSH_COMMAND\" ]; then",
				"\t\tprintf '%s\\n' \"$GIT_SSH_COMMAND\" \\",
				"\t\t\t>> \"$dir/environment\"", "\tfi",
				"\tgit checkout -b upgrade/7.4-to-2026.q1.0 --quiet",
				"\tcommit 2026-01-01T00:00:00 Initialize", "\t;;",
				"\"/upgrade-phase \"*)", "\tphase=\"${2#/upgrade-phase }\"",
				"\tstatuses=\"$dir/phase-statuses\"",
				"\tstatus=\"$(sed -n \"${phase}p\" \"$statuses\")\"",
				"\tprintf '| %s | Phase | %s |\\n' \"$phase\" \\",
				"\t\t\"$status\" > upgrade-state.md",
				"\tgit checkout -B \"phase$phase\" --quiet",
				"\tdate=\"2026-01-0$((phase + 1))T00:00:00\"",
				"\tcommit \"$date\" \"Phase $phase\"", "\t;;", "esac"
			});
	}

	private String _createGHCommand() throws Exception {
		return _writeScript(
			"gh",
			new String[] {
				"printf '%s\\n' \"$@\" > \"$dir/gh-arguments\"",
				"echo 'Creating pull request'", "echo",
				"echo 'https://github.com/acme/workspace/pull/7'"
			});
	}

	private void _createRepository() throws Exception {
		Path seedPath = _testPath.resolve("seed");

		_executeGit(_testPath, "init", "--initial-branch=main", "seed");
		_executeGit(
			seedPath, "-c", "user.email=test@liferay.com", "-c",
			"user.name=Test", "commit", "--allow-empty", "--message", "Seed");
		_executeGit(_testPath, "clone", "--bare", "seed", "remote.git");
	}

	private void _deleteDirectory(File file) {
		File[] files = file.listFiles();

		if (files != null) {
			for (File childFile : files) {
				_deleteDirectory(childFile);
			}
		}

		file.delete();
	}

	private String _executeGit(Path workPath, String... arguments)
		throws Exception {

		ProcessBuilder processBuilder = new ProcessBuilder(
			ArrayUtil.append(new String[] {"git"}, arguments));

		processBuilder.directory(workPath.toFile());
		processBuilder.redirectErrorStream(true);

		Process process = processBuilder.start();

		String output = new String(
			process.getInputStream(
			).readAllBytes(),
			StandardCharsets.UTF_8);

		Assert.assertEquals(output, 0, process.waitFor());

		return output;
	}

	private List<String> _getRemoteBranches() throws Exception {
		return Arrays.asList(
			StringUtil.splitLines(
				_executeGit(
					_testPath.resolve("remote.git"), "for-each-ref",
					"--format=%(refname:short)", "refs/heads")));
	}

	private UpgradeRunRequest _getUpgradeRunRequest(
		String credentialKeyReference) {

		return new UpgradeRunRequest(
			"main", RandomTestUtil.randomLong(), credentialKeyReference,
			"file://" + _testPath.resolve("remote.git"),
			HashMapBuilder.put(
				UpgradeRunSettingsKeys.CUSTOMER_NAME, "acme"
			).put(
				UpgradeRunSettingsKeys.DB_TARGET_TYPE, "mysql"
			).put(
				UpgradeRunSettingsKeys.DB_TARGET_VERSION, "8.0"
			).put(
				UpgradeRunSettingsKeys.NODE_VERSION, "20.18.0"
			).put(
				UpgradeRunSettingsKeys.SEARCH_VERSION, "8.17.4"
			).put(
				UpgradeRunSettingsKeys.UPGRADE_SOURCE_VERSION, "7.4.13-u92"
			).put(
				UpgradeRunSettingsKeys.UPGRADE_TARGET_JAVA_VERSION, "21"
			).build(),
			"2026.q1.0", RandomTestUtil.randomLong());
	}

	private String _read(String fileName) throws Exception {
		return Files.readString(
			_testPath.resolve(fileName), StandardCharsets.UTF_8);
	}

	private List<String> _readLines(String fileName) throws Exception {
		return Files.readAllLines(
			_testPath.resolve(fileName), StandardCharsets.UTF_8);
	}

	private NoticeableThreadPoolExecutor _setUpExecutorService() {
		NoticeableThreadPoolExecutor noticeableThreadPoolExecutor =
			new NoticeableThreadPoolExecutor(
				1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
				Executors.defaultThreadFactory(),
				new ThreadPoolExecutor.AbortPolicy(),
				new ThreadPoolHandlerAdapter());

		ReflectionTestUtil.setFieldValue(
			_localUpgradeRunner, "_noticeableExecutorService",
			noticeableThreadPoolExecutor);

		return noticeableThreadPoolExecutor;
	}

	private UpgradeRunnerState _submit(UpgradeRunRequest upgradeRunRequest)
		throws Exception {

		NoticeableThreadPoolExecutor noticeableThreadPoolExecutor =
			_setUpExecutorService();

		try {
			String externalReferenceCode = _localUpgradeRunner.submit(
				upgradeRunRequest);

			long deadline = System.currentTimeMillis() + 60000;

			while (true) {
				UpgradeRunnerState upgradeRunnerState =
					_localUpgradeRunner.getUpgradeRunnerState(
						externalReferenceCode);

				if (UpgradeRunConstants.isTerminal(
						upgradeRunnerState.getStatus())) {

					return upgradeRunnerState;
				}

				Assert.assertTrue(
					upgradeRunnerState.getStatusMessage(),
					System.currentTimeMillis() < deadline);

				Thread.sleep(100);
			}
		}
		finally {
			noticeableThreadPoolExecutor.shutdownNow();
		}
	}

	private URL _toURL(Path path) throws Exception {
		return path.toUri(
		).toURL();
	}

	private void _writePhaseStatuses(String... phaseStatuses) throws Exception {
		Files.write(
			_testPath.resolve("phase-statuses"), Arrays.asList(phaseStatuses),
			StandardCharsets.UTF_8);
	}

	private String _writeScript(String fileName, String[] lines)
		throws Exception {

		Path path = _testPath.resolve(fileName);

		Files.writeString(
			path,
			StringBundler.concat(
				"#!/bin/sh\nset -e\ndir='", _testPath, "'\n",
				StringUtil.merge(lines, StringPool.NEW_LINE),
				StringPool.NEW_LINE));

		Files.setPosixFilePermissions(
			path, PosixFilePermissions.fromString("rwx------"));

		return path.toString();
	}

	private static final String _PROPS_KEY_CLAUDE_COMMAND =
		"upgrades.lab.agent.remote.runner.local.claude.command";

	private static final String _PROPS_KEY_GH_COMMAND =
		"upgrades.lab.agent.remote.runner.local.gh.command";

	private List<URL> _agentURLs;
	private LocalUpgradeRunner _localUpgradeRunner;
	private Path _testPath;

	private static class TestSecretManager implements SecretManager {

		public TestSecretManager(CountDownLatch countDownLatch) {
			_countDownLatch = countDownLatch;
		}

		@Override
		public void deleteSecret(long companyId, KeyReference keyReference) {
		}

		@Override
		public List<KeyReference> getKeyReferences(
			long companyId, String secretProviderId) {

			return Collections.emptyList();
		}

		@Override
		public Secret getSecret(long companyId, KeyReference keyReference)
			throws SecretException {

			if (_countDownLatch == null) {
				return new Secret(keyReference, "private key");
			}

			_countDownLatch.countDown();

			try {
				Thread.sleep(Long.MAX_VALUE);
			}
			catch (InterruptedException interruptedException) {
				throw new SecretException(interruptedException);
			}

			return null;
		}

		@Override
		public List<String> getSecretProviderIds(long companyId) {
			return Collections.emptyList();
		}

		@Override
		public KeyReference putSecret(long companyId, Secret secret) {
			return null;
		}

		private final CountDownLatch _countDownLatch;

	}

}