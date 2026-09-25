/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.service.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.test.log.LogCapture;
import com.liferay.portal.test.log.LogEntry;
import com.liferay.portal.test.log.LoggerTestUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.upgrades.lab.agent.remote.constants.UpgradeRunConstants;
import com.liferay.upgrades.lab.agent.remote.exception.UpgradeRunRequestException;
import com.liferay.upgrades.lab.agent.remote.model.UpgradeRun;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunner;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunnerState;
import com.liferay.upgrades.lab.agent.remote.service.UpgradeRunLocalService;

import java.util.List;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Every submitted run fails inside the local runner, either because the
 * credential key reference does not resolve to a stored secret or because the
 * repository does not exist. The runner logs that failure at error level from
 * its executor thread, so each test captures that logger and waits for the
 * runner to finish before the capture closes.
 *
 * @author Albert Gomes Cabral
 */
@RunWith(Arquillian.class)
public class UpgradeRunLocalServiceTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Test
	public void testAddUpgradeRun() throws Exception {
		try (LogCapture logCapture = _captureRunnerErrors()) {
			_upgradeRun = _addUpgradeRun("acme");

			Assert.assertEquals(
				TestPropsValues.getCompanyId(), _upgradeRun.getCompanyId());
			Assert.assertEquals(
				TestPropsValues.getUserId(), _upgradeRun.getUserId());
			Assert.assertEquals("acme", _upgradeRun.getCustomerName());
			Assert.assertEquals(
				UpgradeRunConstants.STATUS_QUEUED, _upgradeRun.getStatus());
			Assert.assertTrue(
				Validator.isNotNull(_upgradeRun.getExternalReferenceCode()));
			Assert.assertNotNull(_upgradeRun.getStartDate());
			Assert.assertNull(_upgradeRun.getEndDate());

			Assert.assertEquals(
				_upgradeRun,
				_upgradeRunLocalService.fetchUpgradeRunByExternalReferenceCode(
					_upgradeRun.getExternalReferenceCode(),
					_upgradeRun.getCompanyId()));

			_waitForRunner(_upgradeRun.getExternalReferenceCode());
		}
	}

	@Test
	public void testAddUpgradeRunMissingSetting() throws Exception {
		int count = _upgradeRunLocalService.getUpgradeRunsCount();

		Assert.assertThrows(
			UpgradeRunRequestException.class, () -> _addUpgradeRun(null));

		Assert.assertEquals(
			count, _upgradeRunLocalService.getUpgradeRunsCount());
	}

	@Test
	public void testCancelUpgradeRun() throws Exception {
		try (LogCapture logCapture = _captureRunnerErrors()) {
			_upgradeRun = _addUpgradeRun("acme");

			_waitForRunner(_upgradeRun.getExternalReferenceCode());

			UpgradeRun upgradeRun = _upgradeRunLocalService.cancelUpgradeRun(
				_upgradeRun.getCompanyId(),
				_upgradeRun.getExternalReferenceCode());

			Assert.assertEquals(
				UpgradeRunConstants.STATUS_CANCELLED, upgradeRun.getStatus());
			Assert.assertNotNull(upgradeRun.getEndDate());

			upgradeRun = _upgradeRunLocalService.cancelUpgradeRun(
				_upgradeRun.getCompanyId(),
				_upgradeRun.getExternalReferenceCode());

			Assert.assertEquals(
				UpgradeRunConstants.STATUS_CANCELLED, upgradeRun.getStatus());
		}
	}

	@Test
	public void testRefreshUpgradeRun() throws Exception {
		try (LogCapture logCapture = _captureRunnerErrors()) {
			_upgradeRun = _addUpgradeRun("acme");

			UpgradeRun upgradeRun = _refreshUntilTerminal(_upgradeRun);

			Assert.assertEquals(
				UpgradeRunConstants.STATUS_FAILED, upgradeRun.getStatus());
			Assert.assertTrue(
				Validator.isNotNull(upgradeRun.getStatusMessage()));
			Assert.assertNotNull(upgradeRun.getEndDate());

			UpgradeRun refreshedUpgradeRun =
				_upgradeRunLocalService.refreshUpgradeRun(
					_upgradeRun.getCompanyId(),
					_upgradeRun.getExternalReferenceCode());

			Assert.assertEquals(
				upgradeRun.getStatus(), refreshedUpgradeRun.getStatus());
			Assert.assertEquals(
				upgradeRun.getStatusMessage(),
				refreshedUpgradeRun.getStatusMessage());

			_assertRunnerError(logCapture, upgradeRun);
		}
	}

	/**
	 * Without a credential the local runner skips the secret lookup and goes
	 * straight to Git, which fails because the repository does not exist.
	 */
	@Test
	public void testRefreshUpgradeRunWithoutCredential() throws Exception {
		try (LogCapture logCapture = _captureRunnerErrors()) {
			_upgradeRun = _upgradeRunLocalService.addUpgradeRun(
				"main", null, "acme", "mysql", "8.0", "20.18.0",
				"file:///" + RandomTestUtil.randomString(), "8.17.4",
				"2026.q1.0", "7.4.13-u92", "21", TestPropsValues.getUserId());

			Assert.assertTrue(
				Validator.isNull(_upgradeRun.getCredentialKeyReference()));

			UpgradeRun upgradeRun = _refreshUntilTerminal(_upgradeRun);

			Assert.assertEquals(
				UpgradeRunConstants.STATUS_FAILED, upgradeRun.getStatus());

			String statusMessage = upgradeRun.getStatusMessage();

			Assert.assertTrue(
				statusMessage, statusMessage.contains("git ls-remote"));

			_assertRunnerError(logCapture, upgradeRun);
		}
	}

	private UpgradeRun _addUpgradeRun(String customerName) throws Exception {
		return _upgradeRunLocalService.addUpgradeRun(
			"main", "${secretRef:*:" + RandomTestUtil.randomString() + "}",
			customerName, "mysql", "8.0", "20.18.0",
			"git@github.com:acme/workspace.git", "8.17.4", "2026.q1.0",
			"7.4.13-u92", "21", TestPropsValues.getUserId());
	}

	private void _assertRunnerError(
		LogCapture logCapture, UpgradeRun upgradeRun) {

		List<LogEntry> logEntries = logCapture.getLogEntries();

		Assert.assertEquals(logEntries.toString(), 1, logEntries.size());

		LogEntry logEntry = logEntries.get(0);

		Assert.assertEquals(
			"Unable to carry out upgrade run " + upgradeRun.getUpgradeRunId(),
			logEntry.getMessage());
	}

	private LogCapture _captureRunnerErrors() {
		return LoggerTestUtil.configureLog4JLogger(
			_CLASS_NAME_LOCAL_UPGRADE_RUNNER, LoggerTestUtil.ERROR);
	}

	private UpgradeRun _refreshUntilTerminal(UpgradeRun upgradeRun)
		throws Exception {

		for (int i = 0; i < _ATTEMPTS; i++) {
			upgradeRun = _upgradeRunLocalService.refreshUpgradeRun(
				upgradeRun.getCompanyId(),
				upgradeRun.getExternalReferenceCode());

			if (UpgradeRunConstants.isTerminal(upgradeRun.getStatus())) {
				return upgradeRun;
			}

			Thread.sleep(_INTERVAL);
		}

		Assert.assertTrue(
			UpgradeRunConstants.isTerminal(upgradeRun.getStatus()));

		return upgradeRun;
	}

	/**
	 * Blocks until the runner has finished the run. The runner logs a failure
	 * before it records the terminal state, so once this returns the log
	 * capture that wraps the test already holds the entry.
	 */
	private void _waitForRunner(String externalReferenceCode) throws Exception {
		UpgradeRunnerState upgradeRunnerState = null;

		for (int i = 0; i < _ATTEMPTS; i++) {
			upgradeRunnerState = _upgradeRunner.getUpgradeRunnerState(
				externalReferenceCode);

			if (UpgradeRunConstants.isTerminal(
					upgradeRunnerState.getStatus())) {

				return;
			}

			Thread.sleep(_INTERVAL);
		}

		Assert.assertTrue(
			upgradeRunnerState.getStatusMessage(),
			UpgradeRunConstants.isTerminal(upgradeRunnerState.getStatus()));
	}

	private static final int _ATTEMPTS = 60;

	private static final String _CLASS_NAME_LOCAL_UPGRADE_RUNNER =
		"com.liferay.upgrades.lab.agent.remote.runner.local.internal." +
			"LocalUpgradeRunner";

	private static final long _INTERVAL = 500;

	@DeleteAfterTestRun
	private UpgradeRun _upgradeRun;

	@Inject
	private UpgradeRunLocalService _upgradeRunLocalService;

	@Inject
	private UpgradeRunner _upgradeRunner;

}