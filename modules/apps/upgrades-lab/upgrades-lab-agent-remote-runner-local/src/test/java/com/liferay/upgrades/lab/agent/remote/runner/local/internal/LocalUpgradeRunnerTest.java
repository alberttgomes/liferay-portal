/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.runner.local.internal;

import com.liferay.petra.concurrent.NoticeableThreadPoolExecutor;
import com.liferay.petra.concurrent.ThreadPoolHandlerAdapter;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
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
import com.liferay.upgrades.lab.agent.remote.runner.local.internal.configuration.LocalUpgradeRunnerConfiguration;

import java.io.File;
import java.io.InputStream;

import java.net.URI;
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
			_localUpgradeRunner, "_configurationProvider",
			ProxyUtil.newProxyInstance(
				ConfigurationProvider.class.getClassLoader(),
				new Class<?>[] {ConfigurationProvider.class},
				(proxy, method, arguments) -> {
					if (Objects.equals(
							method.getName(), "getCompanyConfiguration")) {

						return ConfigurableUtil.createConfigurable(
							LocalUpgradeRunnerConfiguration.class,
							_configurationProperties);
					}

					return null;
				}));
		ReflectionTestUtil.setFieldValue(
			_localUpgradeRunner, "_secretManager", new TestSecretManager(null));

		_createRepository();

		_claudeCommand = _createClaudeCommand();
		_ghCommand = _createGHCommand();

		_configurationProperties = _getProperties(
			_testPath.resolve("licenses"));
	}

	@After
	public void tearDown() throws Exception {
		_deleteDirectory(_testPath.toFile());
	}

	@Test
	public void testCancel() throws Exception {
		Assert.assertThrows(
			UpgradeRunnerException.class,
			() -> _localUpgradeRunner.cancel(RandomTestUtil.randomString()));

		// Block the worker inside the secret lookup, the first call the run
		// makes, so the cancel arrives while the run is still validating.

		CountDownLatch countDownLatch = new CountDownLatch(1);

		ReflectionTestUtil.setFieldValue(
			_localUpgradeRunner, "_secretManager",
			new TestSecretManager(countDownLatch));

		NoticeableThreadPoolExecutor noticeableThreadPoolExecutor =
			_setUpExecutorService();

		try {
			String externalReferenceCode = _localUpgradeRunner.submit(
				_getUpgradeRunRequest(
					"${secretRef:*:" + RandomTestUtil.randomString() + "}",
					null));

			Assert.assertTrue(countDownLatch.await(10, TimeUnit.SECONDS));

			UpgradeRunnerState upgradeRunnerState =
				_localUpgradeRunner.getUpgradeRunnerState(
					externalReferenceCode);

			Assert.assertEquals(
				UpgradeRunConstants.STATUS_VALIDATING,
				upgradeRunnerState.getStatus());

			_localUpgradeRunner.cancel(externalReferenceCode);

			// Let the interrupted worker run its error handling to the end
			// before checking that it left the cancellation alone.

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
					RandomTestUtil.randomString()));

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
	public void testStartProcess() throws Exception {
		Map<String, String> hostEnvironment = HashMapBuilder.put(
			"ANTHROPIC_BASE_URL", "https://gateway.example.com"
		).put(
			"CLAUDE_CODE_USE_BEDROCK", "1"
		).put(
			"CLAUDE_CONFIG_DIR", "/home/liferay/.claude"
		).build();

		List<String> variables = _startProcess(
			Collections.singletonMap("ANTHROPIC_API_KEY", "configured"),
			hostEnvironment);

		Assert.assertTrue(
			variables.toString(),
			variables.contains("ANTHROPIC_API_KEY=configured"));
		Assert.assertFalse(
			variables.toString(),
			variables.contains(
				"ANTHROPIC_BASE_URL=https://gateway.example.com"));
		Assert.assertFalse(
			variables.toString(),
			variables.contains("CLAUDE_CODE_USE_BEDROCK=1"));
		Assert.assertFalse(
			variables.toString(),
			variables.contains("CLAUDE_CONFIG_DIR=/home/liferay/.claude"));

		variables = _startProcess(Collections.emptyMap(), hostEnvironment);

		Assert.assertTrue(
			variables.toString(),
			variables.contains(
				"ANTHROPIC_BASE_URL=https://gateway.example.com"));
		Assert.assertTrue(
			variables.toString(),
			variables.contains("CLAUDE_CODE_USE_BEDROCK=1"));
		Assert.assertTrue(
			variables.toString(),
			variables.contains("CLAUDE_CONFIG_DIR=/home/liferay/.claude"));
	}

	@Test
	public void testSubmit() throws Exception {
		Assert.assertThrows(
			UpgradeRunnerException.class,
			() -> _localUpgradeRunner.submit(null));

		_writePhaseStatuses(
			"complete", "`phase2-x` -> `phase2-y` | Complete", "skipped",
			"complete");

		Path licensePath = _writeLicense(_testPath.resolve("license.xml"));

		UpgradeRunnerState upgradeRunnerState = _submit(
			_getUpgradeRunRequest(
				"${secretRef:*:" + RandomTestUtil.randomString() + "}",
				licensePath.toString()));

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
				"--permission-mode bypassPermissions --print /upgrade-init",
				"--permission-mode bypassPermissions --print /upgrade-phase 1",
				"--permission-mode bypassPermissions --print /upgrade-phase 2",
				"--permission-mode bypassPermissions --print /upgrade-phase 3",
				"--permission-mode bypassPermissions --print /upgrade-phase 4"),
			_readLines("claude-arguments"));

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

		Assert.assertTrue(
			gitSSHCommand, gitSSHCommand.startsWith(_SSH_COMMAND + " -i "));
		Assert.assertTrue(
			gitSSHCommand, gitSSHCommand.contains("-o IdentitiesOnly=yes"));
		Assert.assertTrue(
			gitSSHCommand,
			gitSSHCommand.contains("-o StrictHostKeyChecking=accept-new"));

		Assert.assertEquals(
			Arrays.asList("anthropic-api-key", "", ""),
			_readLines("credentials"));

		String privateKeyPath = StringUtil.extractFirst(
			gitSSHCommand.substring(_SSH_COMMAND.length() + 4),
			StringPool.SPACE);

		Assert.assertFalse(Files.exists(Path.of(privateKeyPath)));

		Assert.assertEquals(
			Arrays.asList(licensePath.toString()), _readLines("license-path"));
		Assert.assertEquals(Files.readString(licensePath), _read("license"));

		Path workspacePath = Path.of(upgradeRunnerState.getWorkspacePath());

		_assertInstalled(
			"hooks/guard_bash.py", ".claude/hooks/guard_bash.py",
			workspacePath);
		_assertInstalled(
			"reference/README.md", ".claude/reference/README.md",
			workspacePath);
		_assertInstalled(
			"skills/upgrade-run/SKILL.md",
			".claude/skills/upgrade-run/SKILL.md", workspacePath);
		_assertInstalled(
			"templates/.mcp.json.template", ".mcp.json", workspacePath);
		_assertInstalled(
			"templates/CLAUDE.md.template", "CLAUDE.md", workspacePath);

		Assert.assertFalse(
			Files.exists(workspacePath.resolve(".claude/README.md")));
		Assert.assertFalse(Files.exists(workspacePath.resolve("README.md")));
		Assert.assertFalse(Files.exists(workspacePath.resolve("notes.txt")));

		List<String> excludedPaths = Files.readAllLines(
			workspacePath.resolve(".git/info/exclude"), StandardCharsets.UTF_8);

		Assert.assertTrue(
			excludedPaths.toString(),
			excludedPaths.containsAll(
				Arrays.asList(
					".claude/", ".mcp.json", "CLAUDE.md",
					"upgrade-run-result.properties", "upgrade-run.properties",
					"upgrade-state.md")));

		Assert.assertEquals(
			Arrays.asList(
				"main", "phase1", "phase2", "phase3", "phase4",
				"upgrade/7.4-to-2026.q1.0"),
			_getRemoteBranches());

		String ghArguments = _read("gh-arguments");

		Assert.assertTrue(
			ghArguments,
			ghArguments.startsWith(
				"pr\ncreate\n--base\nupgrade/7.4-to-2026.q1.0\n--body\n" +
					"The upgrade agent completed every phase.\n"));
		Assert.assertTrue(ghArguments, ghArguments.contains("upgrade-notes"));
		Assert.assertTrue(
			ghArguments,
			ghArguments.endsWith(
				"--head\nphase4\n--title\nUpgrade to 2026.q1.0\n"));
	}

	@Test
	public void testSubmitBlocked() throws Exception {
		_writePhaseStatuses("complete", "blocked", "complete", "complete");

		UpgradeRunnerState upgradeRunnerState = _submit(
			_getUpgradeRunRequest(null, null));

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
				"--permission-mode bypassPermissions --print /upgrade-init",
				"--permission-mode bypassPermissions --print /upgrade-phase 1",
				"--permission-mode bypassPermissions --print /upgrade-phase 2"),
			_readLines("claude-arguments"));

		List<String> environment = _readLines("environment");

		Assert.assertEquals(Arrays.asList("0", _SSH_COMMAND), environment);

		Assert.assertFalse(Files.exists(_testPath.resolve("license")));

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
	public void testSubmitResumeFromResultBranch() throws Exception {
		_writePhaseStatuses("complete", "complete", "blocked");

		UpgradeRunnerState upgradeRunnerState = _submit(
			_getUpgradeRunRequest(null, null));

		Assert.assertEquals(
			upgradeRunnerState.getStatusMessage(),
			UpgradeRunConstants.STATUS_BLOCKED, upgradeRunnerState.getStatus());

		Path workspacePath = Path.of(upgradeRunnerState.getWorkspacePath());

		_deleteDirectory(workspacePath.toFile());

		Files.writeString(
			_testPath.resolve("pull-request"), RandomTestUtil.randomString());

		_writePhaseStatuses("complete", "complete", "complete", "complete");

		Files.delete(_testPath.resolve("trackers"));

		UpgradeRunnerState resumedUpgradeRunnerState = _submit(
			_getUpgradeRunRequest(
				null, 3, null, "phase3",
				upgradeRunnerState.getWorkspacePath()));

		Assert.assertEquals(
			resumedUpgradeRunnerState.getStatusMessage(),
			UpgradeRunConstants.STATUS_SUCCESSFUL,
			resumedUpgradeRunnerState.getStatus());
		Assert.assertEquals(
			"https://github.com/acme/workspace/pull/7",
			resumedUpgradeRunnerState.getPullRequestURL());
		Assert.assertEquals(
			"phase4", resumedUpgradeRunnerState.getResultBranch());
		Assert.assertNotEquals(
			upgradeRunnerState.getWorkspacePath(),
			resumedUpgradeRunnerState.getWorkspacePath());

		List<String> claudeArguments = _readLines("claude-arguments");

		Assert.assertEquals(
			Arrays.asList(
				"--permission-mode bypassPermissions --print /upgrade-init",
				"--permission-mode bypassPermissions --print /upgrade-phase 3",
				"--permission-mode bypassPermissions --print /upgrade-phase 4"),
			claudeArguments.subList(4, claudeArguments.size()));

		Path resumedWorkspacePath = Path.of(
			resumedUpgradeRunnerState.getWorkspacePath());

		Assert.assertEquals(
			Arrays.asList(
				"phase1", "phase2", "phase3", "phase4",
				"upgrade/7.4-to-2026.q1.0"),
			Arrays.asList(
				StringUtil.splitLines(
					_executeGit(
						resumedWorkspacePath, "for-each-ref",
						"--format=%(refname:short)", "refs/heads"))));

		Assert.assertEquals(
			Arrays.asList(
				"| Phase | Name | Status | Started |",
				"| --- | --- | --- | --- |",
				"| 1 | Upgrade Environment | complete | complete |",
				"| 2 | Fix Compile | complete | complete |",
				"| 3 | Fix Startup | pending | pending |",
				"| 4 | Check Reindex | pending | pending |",
				"| 5 | Fix Frontend | — | deferred |",
				"| 3 | Phase | complete |"),
			_readLines("trackers"));
		Assert.assertEquals(
			Arrays.asList(
				"main", "phase1", "phase2", "phase3", "phase4",
				"upgrade/7.4-to-2026.q1.0"),
			_getRemoteBranches());

		String ghArguments = _read("gh-arguments");

		Assert.assertTrue(
			ghArguments,
			ghArguments.startsWith(
				"pr\nlist\n--base\nupgrade/7.4-to-2026.q1.0\n--head\n" +
					"phase4\n"));
	}

	@Test
	public void testSubmitResumeInWorkspace() throws Exception {
		_writePhaseStatuses("complete", "blocked");

		UpgradeRunnerState upgradeRunnerState = _submit(
			_getUpgradeRunRequest(null, null));

		Assert.assertEquals(
			upgradeRunnerState.getStatusMessage(),
			UpgradeRunConstants.STATUS_BLOCKED, upgradeRunnerState.getStatus());

		Path workspacePath = Path.of(upgradeRunnerState.getWorkspacePath());

		Files.writeString(workspacePath.resolve("CLAUDE.md"), "configured");

		_writePhaseStatuses("complete", "complete", "complete", "complete");

		Files.delete(_testPath.resolve("trackers"));

		UpgradeRunnerState resumedUpgradeRunnerState = _submit(
			_getUpgradeRunRequest(
				null, 2, null, "phase2", workspacePath.toString()));

		Assert.assertEquals(
			resumedUpgradeRunnerState.getStatusMessage(),
			UpgradeRunConstants.STATUS_SUCCESSFUL,
			resumedUpgradeRunnerState.getStatus());
		Assert.assertEquals(
			"phase4", resumedUpgradeRunnerState.getResultBranch());
		Assert.assertEquals(
			workspacePath.toString(),
			resumedUpgradeRunnerState.getWorkspacePath());

		List<String> claudeArguments = _readLines("claude-arguments");

		Assert.assertEquals(
			Arrays.asList(
				"--permission-mode bypassPermissions --print /upgrade-phase 2",
				"--permission-mode bypassPermissions --print /upgrade-phase 3",
				"--permission-mode bypassPermissions --print /upgrade-phase 4"),
			claudeArguments.subList(3, claudeArguments.size()));

		Assert.assertEquals(
			"configured", Files.readString(workspacePath.resolve("CLAUDE.md")));
		_assertInstalled(
			"hooks/guard_bash.py", ".claude/hooks/guard_bash.py",
			workspacePath);
		Assert.assertEquals(
			Arrays.asList(
				"| 2 | Phase | pending |", "| 2 | Phase | complete |",
				"| 3 | Phase | complete |"),
			_readLines("trackers"));

		List<String> excludedPaths = Files.readAllLines(
			workspacePath.resolve(".git/info/exclude"), StandardCharsets.UTF_8);

		Assert.assertEquals(
			excludedPaths.toString(), 1,
			Collections.frequency(excludedPaths, "upgrade-state.md"));

		Assert.assertEquals(
			Arrays.asList(
				"main", "phase1", "phase2", "phase3", "phase4",
				"upgrade/7.4-to-2026.q1.0"),
			_getRemoteBranches());
	}

	@Test
	public void testSubmitResumeInWorkspaceWithChangedSettings()
		throws Exception {

		_writePhaseStatuses("complete", "blocked");

		UpgradeRunnerState upgradeRunnerState = _submit(
			_getUpgradeRunRequest(null, null));

		Assert.assertEquals(
			upgradeRunnerState.getStatusMessage(),
			UpgradeRunConstants.STATUS_BLOCKED, upgradeRunnerState.getStatus());

		_writePhaseStatuses("complete", "complete", "complete", "complete");

		UpgradeRunnerState resumedUpgradeRunnerState = _submit(
			_getUpgradeRunRequest(
				null, 2, null, "phase2",
				HashMapBuilder.putAll(
					_getSettings()
				).put(
					UpgradeRunSettingsKeys.SEARCH_VERSION, "8.19.11"
				).build(),
				upgradeRunnerState.getWorkspacePath()));

		Assert.assertEquals(
			resumedUpgradeRunnerState.getStatusMessage(),
			UpgradeRunConstants.STATUS_SUCCESSFUL,
			resumedUpgradeRunnerState.getStatus());

		List<String> claudeArguments = _readLines("claude-arguments");

		Assert.assertEquals(
			Arrays.asList(
				"--permission-mode bypassPermissions --print /upgrade-init",
				"--permission-mode bypassPermissions --print /upgrade-phase 2",
				"--permission-mode bypassPermissions --print /upgrade-phase 3",
				"--permission-mode bypassPermissions --print /upgrade-phase 4"),
			claudeArguments.subList(3, claudeArguments.size()));

		Assert.assertEquals(
			Arrays.asList(
				"customer.name=acme", "db.target.type=mysql",
				"db.target.version=8.0", "node.version=20.18.0",
				"search.version=8.19.11", "upgrade.source.version=7.4.13-u92",
				"upgrade.target.java.version=21"),
			_readLines("settings"));
	}

	@Test
	public void testSubmitResumeInWorkspaceWithUnfinishedPhase()
		throws Exception {

		_writePhaseStatuses("complete", "in-progress");

		UpgradeRunnerState upgradeRunnerState = _submit(
			_getUpgradeRunRequest(null, null));

		Assert.assertEquals(
			UpgradeRunConstants.STATUS_FAILED, upgradeRunnerState.getStatus());
		Assert.assertEquals(
			"Phase 2, Fix Compile, did not finish after 3 attempts. Its last " +
				"status was \"in-progress\".",
			upgradeRunnerState.getStatusMessage());
		Assert.assertEquals("phase2", upgradeRunnerState.getResultBranch());

		_writePhaseStatuses("complete", "complete", "complete", "complete");

		Files.delete(_testPath.resolve("trackers"));

		UpgradeRunnerState resumedUpgradeRunnerState = _submit(
			_getUpgradeRunRequest(
				null, 2, null, "phase2",
				upgradeRunnerState.getWorkspacePath()));

		Assert.assertEquals(
			resumedUpgradeRunnerState.getStatusMessage(),
			UpgradeRunConstants.STATUS_SUCCESSFUL,
			resumedUpgradeRunnerState.getStatus());
		Assert.assertEquals(
			"phase4", resumedUpgradeRunnerState.getResultBranch());

		List<String> claudeArguments = _readLines("claude-arguments");

		Assert.assertEquals(
			Arrays.asList(
				"--permission-mode bypassPermissions --print /upgrade-phase 2",
				"--permission-mode bypassPermissions --print /upgrade-phase 3",
				"--permission-mode bypassPermissions --print /upgrade-phase 4"),
			claudeArguments.subList(5, claudeArguments.size()));

		Assert.assertEquals(
			Arrays.asList(
				"| 2 | Phase | in-progress |", "| 2 | Phase | complete |",
				"| 3 | Phase | complete |"),
			_readLines("trackers"));
	}

	@Test
	public void testSubmitUnfinishedPhase() throws Exception {
		_writePhaseStatuses("TODO | in-progress");

		UpgradeRunnerState upgradeRunnerState = _submit(
			_getUpgradeRunRequest(null, null));

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
				"--permission-mode bypassPermissions --print /upgrade-init",
				"--permission-mode bypassPermissions --print /upgrade-phase 1",
				"--permission-mode bypassPermissions --print /upgrade-phase 1",
				"--permission-mode bypassPermissions --print /upgrade-phase 1"),
			_readLines("claude-arguments"));
		Assert.assertEquals(
			Arrays.asList("main", "phase1", "upgrade/7.4-to-2026.q1.0"),
			_getRemoteBranches());
		Assert.assertFalse(Files.exists(_testPath.resolve("gh-arguments")));
	}

	@Test
	public void testSubmitWithAgentCommand() throws Exception {
		_configurationProperties.put(
			"agentCommand",
			new String[] {
				_writeScript(
					"agent",
					new String[] {
						"cp \"$UPGRADE_RUN_SETTINGS\" \"$dir/settings\""
					})
			});

		UpgradeRunnerState upgradeRunnerState = _submit(
			_getUpgradeRunRequest(null, null));

		Assert.assertEquals(
			upgradeRunnerState.getStatusMessage(),
			UpgradeRunConstants.STATUS_SUCCESSFUL,
			upgradeRunnerState.getStatus());
		Assert.assertEquals(
			"The upgrade agent finished",
			upgradeRunnerState.getStatusMessage());
		Assert.assertNull(upgradeRunnerState.getResultBranch());

		Assert.assertEquals(
			Arrays.asList(
				"customer.name=acme", "db.target.type=mysql",
				"db.target.version=8.0", "node.version=20.18.0",
				"search.version=8.17.4", "upgrade.source.version=7.4.13-u92",
				"upgrade.target.java.version=21"),
			_readLines("settings"));
		Assert.assertFalse(Files.exists(_testPath.resolve("claude-arguments")));
	}

	@Test
	public void testSubmitWithConfiguredLicensePath() throws Exception {
		_writePhaseStatuses("complete", "complete", "complete", "complete");

		Path licensesPath = _testPath.resolve("licenses");

		Files.createDirectories(licensesPath);

		Files.writeString(
			licensesPath.resolve("a.xml"),
			"<?xml version=\"1.0\"?>\n<licensee>acme</licensee>");

		Path licensePath = _writeLicense(licensesPath.resolve("b.xml"));

		Files.writeString(licensesPath.resolve("c.txt"), "<license/>");

		_configurationProperties = _getProperties(licensesPath);

		UpgradeRunnerState upgradeRunnerState = _submit(
			_getUpgradeRunRequest(null, null));

		Assert.assertEquals(
			upgradeRunnerState.getStatusMessage(),
			UpgradeRunConstants.STATUS_SUCCESSFUL,
			upgradeRunnerState.getStatus());

		Assert.assertEquals(
			Arrays.asList(licensePath.toString()), _readLines("license-path"));
	}

	@Test
	public void testSubmitWithCredentialReference() throws Exception {
		_writePhaseStatuses("complete", "complete", "complete", "complete");

		_configurationProperties.put(
			"claudeCredential", "${secretRef:*:sk-ant-oat01-stored}");

		UpgradeRunnerState upgradeRunnerState = _submit(
			_getUpgradeRunRequest(null, null));

		Assert.assertEquals(
			upgradeRunnerState.getStatusMessage(),
			UpgradeRunConstants.STATUS_SUCCESSFUL,
			upgradeRunnerState.getStatus());

		Assert.assertEquals(
			Arrays.asList("", "sk-ant-oat01-stored", ""),
			_readLines("credentials"));
	}

	@Test
	public void testSubmitWithHostCredentials() throws Exception {
		_writePhaseStatuses("complete", "complete", "complete", "complete");

		Path claudeConfigPath = _testPath.resolve("claude-config");

		_configurationProperties.remove("claudeCredential");
		_configurationProperties.put(
			"claudeConfigDirectory", claudeConfigPath.toString());
		_configurationProperties.put("hostCredentialsEnabled", true);

		UpgradeRunnerState upgradeRunnerState = _submit(
			_getUpgradeRunRequest(null, null));

		Assert.assertEquals(
			upgradeRunnerState.getStatusMessage(),
			UpgradeRunConstants.STATUS_SUCCESSFUL,
			upgradeRunnerState.getStatus());

		List<String> credentials = _readLines("credentials");

		Assert.assertEquals(claudeConfigPath.toString(), credentials.get(2));
	}

	@Test
	public void testSubmitWithOAuthToken() throws Exception {
		_writePhaseStatuses("complete", "complete", "complete", "complete");

		_configurationProperties.put("claudeCredential", "sk-ant-oat01-acme");

		UpgradeRunnerState upgradeRunnerState = _submit(
			_getUpgradeRunRequest(null, null));

		Assert.assertEquals(
			upgradeRunnerState.getStatusMessage(),
			UpgradeRunConstants.STATUS_SUCCESSFUL,
			upgradeRunnerState.getStatus());

		Assert.assertEquals(
			Arrays.asList("", "sk-ant-oat01-acme", ""),
			_readLines("credentials"));
	}

	@Test
	public void testSubmitWithoutAgent() throws Exception {
		_agentURLs = null;

		UpgradeRunnerState upgradeRunnerState = _submit(
			_getUpgradeRunRequest(null, null));

		Assert.assertEquals(
			UpgradeRunConstants.STATUS_FAILED, upgradeRunnerState.getStatus());
		Assert.assertEquals(
			"The runner bundle does not carry the upgrade agent",
			upgradeRunnerState.getStatusMessage());
		Assert.assertNull(upgradeRunnerState.getResultBranch());

		Assert.assertFalse(Files.exists(_testPath.resolve("claude-arguments")));
	}

	@Test
	public void testSubmitWithoutCredentials() throws Exception {
		_configurationProperties.remove("claudeCredential");

		UpgradeRunnerState upgradeRunnerState = _submit(
			_getUpgradeRunRequest(null, null));

		Assert.assertEquals(
			UpgradeRunConstants.STATUS_FAILED, upgradeRunnerState.getStatus());
		Assert.assertEquals(
			"No Anthropic credential is configured and host credentials are " +
				"disabled",
			upgradeRunnerState.getStatusMessage());

		Assert.assertFalse(Files.exists(_testPath.resolve("claude-arguments")));
	}

	@Test
	public void testSubmitWithoutLicense() throws Exception {
		Path path = _testPath.resolve("id_ed25519");

		Files.writeString(path, RandomTestUtil.randomString());

		UpgradeRunnerState upgradeRunnerState = _submit(
			_getUpgradeRunRequest(null, path.toString()));

		Assert.assertEquals(
			UpgradeRunConstants.STATUS_FAILED, upgradeRunnerState.getStatus());
		Assert.assertEquals(
			"No DXP license was found in " + path,
			upgradeRunnerState.getStatusMessage());

		Assert.assertFalse(Files.exists(_testPath.resolve("claude-arguments")));
	}

	@Test
	public void testWaitFor() throws Exception {
		ProcessBuilder processBuilder = new ProcessBuilder("sleep", "30");

		Process process = processBuilder.start();

		try {
			UpgradeRunnerException upgradeRunnerException = Assert.assertThrows(
				UpgradeRunnerException.class,
				() -> ReflectionTestUtil.invoke(
					_localUpgradeRunner, "_waitFor",
					new Class<?>[] {
						String[].class, Path.class, Process.class, long.class
					},
					new String[] {"sleep", "30"}, _testPath.resolve("log"),
					process, 1L));

			Assert.assertEquals(
				"Command \"sleep 30\" did not finish within 1 seconds",
				upgradeRunnerException.getMessage());

			Assert.assertTrue(process.waitFor(10, TimeUnit.SECONDS));
		}
		finally {
			process.destroyForcibly();
		}
	}

	private void _assertInstalled(
			String content, String relativePath, Path workspacePath)
		throws Exception {

		Assert.assertEquals(
			content, Files.readString(workspacePath.resolve(relativePath)));
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
					"templates/CLAUDE.md.template", "templates/notes.txt",
					"templates/upgrade-state.md.template"
				}) {

			Path path = agentPath.resolve(relativePath);

			Files.createDirectories(path.getParent());

			if (relativePath.equals("templates/upgrade-state.md.template")) {
				Files.write(
					path,
					Arrays.asList(
						"| Phase | Name | Status | Started |",
						"| --- | --- | --- | --- |",
						"| 1 | Upgrade Environment | TODO | pending |",
						"| 2 | Fix Compile | TODO | pending |",
						"| 3 | Fix Startup | TODO | pending |",
						"| 4 | Check Reindex | TODO | pending |",
						"| 5 | Fix Frontend | — | deferred |"),
					StandardCharsets.UTF_8);
			}
			else {
				Files.writeString(path, relativePath);
			}

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
				"\t\t-c commit.gpgsign=false -c user.name=Agent \\",
				"\t\t-c user.email=agent@liferay.com \\",
				"\t\tcommit --allow-empty --message \"$2\" --quiet", "}",
				"if [ \"$4\" = /upgrade-init ]; then",
				"\tcp \"$UPGRADE_RUN_SETTINGS\" \"$dir/settings\"",
				"\tprintf '%s\\n' \"$GIT_TERMINAL_PROMPT\" \\",
				"\t\t> \"$dir/environment\"",
				"\tif [ -n \"$GIT_SSH_COMMAND\" ]; then",
				"\t\tprintf '%s\\n' \"$GIT_SSH_COMMAND\" \\",
				"\t\t\t>> \"$dir/environment\"", "\tfi",
				"\tprintf '%s\\n' \"$ANTHROPIC_API_KEY\" \\",
				"\t\t\"$CLAUDE_CODE_OAUTH_TOKEN\" \"$CLAUDE_CONFIG_DIR\" \\",
				"\t\t> \"$dir/credentials\"",
				"\tif [ -n \"$UPGRADE_RUN_LICENSE_PATH\" ]; then",
				"\t\tcp \"$UPGRADE_RUN_LICENSE_PATH\" \"$dir/license\"",
				"\t\tprintf '%s\\n' \"$UPGRADE_RUN_LICENSE_PATH\" \\",
				"\t\t\t> \"$dir/license-path\"", "\tfi",
				"\tif ! git checkout upgrade/7.4-to-2026.q1.0 --quiet \\",
				"\t\t2>/dev/null; then",
				"\t\tgit checkout -b upgrade/7.4-to-2026.q1.0 --quiet",
				"\t\tcommit 2026-01-01T00:00:00 Initialize", "\tfi",
				"elif [ \"${4#/upgrade-phase }\" != \"$4\" ]; then",
				"\tphase=\"${4#/upgrade-phase }\"",
				"\tstatuses=\"$dir/phase-statuses\"",
				"\tstatus=\"$(sed -n \"${phase}p\" \"$statuses\")\"",
				"\tcat upgrade-state.md >> \"$dir/trackers\"",
				"\tprintf '| %s | Phase | %s |\\n' \"$phase\" \\",
				"\t\t\"$status\" > upgrade-state.md",
				"\tif [ \"$phase\" -gt 1 ]; then",
				"\t\tgit checkout \"phase$((phase - 1))\" --quiet", "\tfi",
				"\tgit checkout -B \"phase$phase\" --quiet",
				"\tdate=\"2026-01-0$((phase + 1))T00:00:00\"",
				"\tcommit \"$date\" \"Phase $phase\"", "fi"
			});
	}

	private String _createGHCommand() throws Exception {
		return _writeScript(
			"gh",
			new String[] {
				"printf '%s\\n' \"$@\" > \"$dir/gh-arguments\"",
				"if [ \"$2\" = list ]; then",
				"\techo 'https://github.com/acme/workspace/pull/7'", "\texit 0",
				"fi", "if [ -e \"$dir/pull-request\" ]; then",
				"\techo 'a pull request for branch \"phase4\" already exists'",
				"\texit 1", "fi", "echo 'Creating pull request'", "echo",
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
			ArrayUtil.append(
				new String[] {"git", "-c", "commit.gpgsign=false"}, arguments));

		processBuilder.directory(workPath.toFile());
		processBuilder.redirectErrorStream(true);

		Process process = processBuilder.start();

		InputStream inputStream = process.getInputStream();

		String output = new String(
			inputStream.readAllBytes(), StandardCharsets.UTF_8);

		Assert.assertEquals(output, 0, process.waitFor());

		return output;
	}

	private Map<String, Object> _getProperties(Path licensePath) {
		return HashMapBuilder.<String, Object>put(
			"claudeCommand", new String[] {_claudeCommand}
		).put(
			"claudeCredential", "anthropic-api-key"
		).put(
			"ghCommand", new String[] {_ghCommand}
		).put(
			"licensePath", licensePath.toString()
		).build();
	}

	private List<String> _getRemoteBranches() throws Exception {
		return Arrays.asList(
			StringUtil.splitLines(
				_executeGit(
					_testPath.resolve("remote.git"), "for-each-ref",
					"--format=%(refname:short)", "refs/heads")));
	}

	private Map<String, String> _getSettings() {
		return HashMapBuilder.put(
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
		).build();
	}

	private UpgradeRunRequest _getUpgradeRunRequest(
		String credentialKeyReference, int firstPhase, String licensePath,
		String resultBranch, Map<String, String> settings,
		String workspacePath) {

		return new UpgradeRunRequest(
			"main", RandomTestUtil.randomLong(), credentialKeyReference,
			firstPhase, licensePath,
			"file://" + _testPath.resolve("remote.git"), resultBranch, settings,
			RandomTestUtil.randomLong(), "2026.q1.0", workspacePath);
	}

	private UpgradeRunRequest _getUpgradeRunRequest(
		String credentialKeyReference, int firstPhase, String licensePath,
		String resultBranch, String workspacePath) {

		return _getUpgradeRunRequest(
			credentialKeyReference, firstPhase, licensePath, resultBranch,
			_getSettings(), workspacePath);
	}

	private UpgradeRunRequest _getUpgradeRunRequest(
		String credentialKeyReference, String licensePath) {

		return _getUpgradeRunRequest(
			credentialKeyReference, UpgradeRunConstants.PHASE_FIRST,
			licensePath, null, null);
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

	private List<String> _startProcess(
			Map<String, String> environment,
			Map<String, String> hostEnvironment)
		throws Exception {

		ProcessBuilder processBuilder = new ProcessBuilder("env");

		Map<String, String> processEnvironment = processBuilder.environment();

		processEnvironment.putAll(hostEnvironment);

		Path outputPath = _testPath.resolve("process-environment");

		processBuilder.redirectOutput(outputPath.toFile());

		Process process = ReflectionTestUtil.invoke(
			_localUpgradeRunner, "_startProcess",
			new Class<?>[] {Map.class, ProcessBuilder.class, Path.class},
			environment, processBuilder, _testPath);

		Assert.assertEquals(0, process.waitFor());

		return Files.readAllLines(outputPath, StandardCharsets.UTF_8);
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
		URI uri = path.toUri();

		return uri.toURL();
	}

	private Path _writeLicense(Path path) throws Exception {
		Files.writeString(
			path,
			"<?xml version=\"1.0\"?>\n<!-- <licenses> -->\n<license\n>" +
				"<product-version>7.4</product-version></license>");

		return path;
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

	private static final String _SSH_COMMAND =
		"ssh -o BatchMode=yes -o ServerAliveCountMax=3 -o " +
			"ServerAliveInterval=15";

	private List<URL> _agentURLs;
	private String _claudeCommand;
	private Map<String, Object> _configurationProperties;
	private String _ghCommand;
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
				return new Secret(keyReference, keyReference.getIdentifier());
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