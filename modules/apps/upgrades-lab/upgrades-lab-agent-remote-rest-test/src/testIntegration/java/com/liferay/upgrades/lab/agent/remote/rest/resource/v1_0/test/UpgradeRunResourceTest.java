/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.rest.resource.v1_0.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.function.UnsafeRunnable;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.test.log.LogCapture;
import com.liferay.portal.test.log.LoggerTestUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.upgrades.lab.agent.remote.constants.UpgradeRunConstants;
import com.liferay.upgrades.lab.agent.remote.rest.client.dto.v1_0.UpgradeRun;
import com.liferay.upgrades.lab.agent.remote.rest.client.dto.v1_0.UpgradeRunResumption;
import com.liferay.upgrades.lab.agent.remote.rest.client.problem.Problem;
import com.liferay.upgrades.lab.agent.remote.rest.client.resource.v1_0.UpgradeRunResource;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunner;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunnerState;
import com.liferay.upgrades.lab.agent.remote.service.UpgradeRunLocalService;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Albert Gomes Cabral
 */
@RunWith(Arquillian.class)
public class UpgradeRunResourceTest extends BaseUpgradeRunResourceTestCase {

	@Override
	@Test
	public void testClientSerDesToDTO() throws Exception {
		super.testClientSerDesToDTO();
	}

	@Override
	@Test
	public void testClientSerDesToJSON() throws Exception {
		super.testClientSerDesToJSON();
	}

	@Override
	@Test
	public void testEscapeRegexInStringFields() throws Exception {
		super.testEscapeRegexInStringFields();
	}

	@Override
	@Test
	public void testGetUpgradeRunByExternalReferenceCode() throws Exception {
		try (LogCapture logCapture = _captureRunnerErrors()) {
			super.testGetUpgradeRunByExternalReferenceCode();

			_testGetUpgradeRunWithoutOmniadmin();

			_waitForRunner();
		}
	}

	@Override
	@Test
	public void testGraphQLGetUpgradeRunByExternalReferenceCode()
		throws Exception {

		try (LogCapture logCapture = _captureRunnerErrors()) {
			super.testGraphQLGetUpgradeRunByExternalReferenceCode();

			_waitForRunner();
		}
	}

	@Override
	@Test
	public void testGraphQLGetUpgradeRunByExternalReferenceCodeNotFound()
		throws Exception {

		super.testGraphQLGetUpgradeRunByExternalReferenceCodeNotFound();
	}

	@Override
	@Test
	public void testGraphQLPostUpgradeRun() throws Exception {
		try (LogCapture logCapture = _captureRunnerErrors()) {
			super.testGraphQLPostUpgradeRun();

			_waitForRunner();
		}
	}

	@Override
	@Test
	public void testGraphQLPostUpgradeRunByExternalReferenceCodeCancel()
		throws Exception {

		try (LogCapture logCapture = _captureRunnerErrors()) {
			super.testGraphQLPostUpgradeRunByExternalReferenceCodeCancel();

			_waitForRunner();
		}
	}

	@Override
	@Test
	public void testGraphQLPostUpgradeRunByExternalReferenceCodeResume()
		throws Exception {

		try (LogCapture logCapture = _captureRunnerErrors()) {
			super.testGraphQLPostUpgradeRunByExternalReferenceCodeResume();

			_waitForRunner();
		}
	}

	@Override
	@Test
	public void testPostUpgradeRun() throws Exception {
		try (LogCapture logCapture = _captureRunnerErrors()) {
			super.testPostUpgradeRun();

			_testPostUpgradeRunWithoutOmniadmin();

			_waitForRunner();
		}
	}

	@Override
	@Test
	public void testPostUpgradeRunByExternalReferenceCodeCancel()
		throws Exception {

		try (LogCapture logCapture = _captureRunnerErrors()) {
			super.testPostUpgradeRunByExternalReferenceCodeCancel();

			_testCancelUpgradeRunWithoutOmniadmin();

			_waitForRunner();
		}
	}

	@Override
	@Test
	public void testPostUpgradeRunByExternalReferenceCodeResume()
		throws Exception {

		try (LogCapture logCapture = _captureRunnerErrors()) {
			super.testPostUpgradeRunByExternalReferenceCodeResume();

			_testResumeUpgradeRunWithoutOmniadmin();

			_waitForRunner();
		}
	}

	@Override
	protected String[] getAdditionalAssertFieldNames() {
		return new String[] {
			"branch", "customerName", "dbTargetType", "dbTargetVersion",
			"licensePath", "nodeVersion", "repositoryURL", "searchVersion",
			"upgradeSourceVersion", "upgradeTargetJavaVersion",
			"upgradeTargetVersion"
		};
	}

	@Override
	protected UpgradeRun randomUpgradeRun() throws Exception {
		UpgradeRun upgradeRun = super.randomUpgradeRun();

		upgradeRun.setCredentialKeyReference(() -> null);

		return upgradeRun;
	}

	@Override
	protected UpgradeRun
			testGetUpgradeRunByExternalReferenceCode_addUpgradeRun()
		throws Exception {

		return _postUpgradeRun(randomUpgradeRun());
	}

	@Override
	protected UpgradeRun testGraphQLUpgradeRun_addUpgradeRun(
			UpgradeRun upgradeRun)
		throws Exception {

		return _trackUpgradeRun(
			super.testGraphQLUpgradeRun_addUpgradeRun(upgradeRun));
	}

	@Override
	protected UpgradeRun testPostUpgradeRun_addUpgradeRun(UpgradeRun upgradeRun)
		throws Exception {

		return _postUpgradeRun(upgradeRun);
	}

	@Override
	protected UpgradeRun
			testPostUpgradeRunByExternalReferenceCodeCancel_addUpgradeRun(
				UpgradeRun upgradeRun)
		throws Exception {

		UpgradeRun postUpgradeRun = _postUpgradeRun(upgradeRun);

		return upgradeRunResource.postUpgradeRunByExternalReferenceCodeCancel(
			postUpgradeRun.getExternalReferenceCode());
	}

	@Override
	protected UpgradeRun
			testPostUpgradeRunByExternalReferenceCodeResume_addUpgradeRun(
				UpgradeRun upgradeRun)
		throws Exception {

		UpgradeRun postUpgradeRun = _postUpgradeRun(upgradeRun);

		_waitForRunner();

		return _trackUpgradeRun(
			upgradeRunResource.postUpgradeRunByExternalReferenceCodeResume(
				postUpgradeRun.getExternalReferenceCode(),
				new UpgradeRunResumption()));
	}

	private void _assertProblemStatus(
			String status, UnsafeRunnable<Exception> unsafeRunnable)
		throws Exception {

		try {
			unsafeRunnable.run();

			Assert.fail();
		}
		catch (Problem.ProblemException problemException) {
			Problem problem = problemException.getProblem();

			Assert.assertEquals(status, problem.getStatus());
		}
	}

	private LogCapture _captureRunnerErrors() {
		return LoggerTestUtil.configureLog4JLogger(
			"com.liferay.upgrades.lab.agent.remote.runner.local.internal." +
				"LocalUpgradeRunner",
			LoggerTestUtil.ERROR);
	}

	private UpgradeRunResource _createUserUpgradeRunResource()
		throws Exception {

		User user = UserTestUtil.addUser(testCompany, "test");

		return UpgradeRunResource.builder(
		).authentication(
			user.getEmailAddress(), "test"
		).endpoint(
			testCompany.getVirtualHostname(),
			PortalUtil.getPortalServerPort(false), "http"
		).locale(
			LocaleUtil.getDefault()
		).build();
	}

	private UpgradeRun _postUpgradeRun(UpgradeRun upgradeRun) throws Exception {
		return _trackUpgradeRun(upgradeRunResource.postUpgradeRun(upgradeRun));
	}

	private void _testCancelUpgradeRunWithoutOmniadmin() throws Exception {
		UpgradeRun upgradeRun = _postUpgradeRun(randomUpgradeRun());

		UpgradeRunResource userUpgradeRunResource =
			_createUserUpgradeRunResource();

		_assertProblemStatus(
			"FORBIDDEN",
			() ->
				userUpgradeRunResource.
					postUpgradeRunByExternalReferenceCodeCancel(
						upgradeRun.getExternalReferenceCode()));
	}

	private void _testGetUpgradeRunWithoutOmniadmin() throws Exception {
		UpgradeRun upgradeRun = _postUpgradeRun(randomUpgradeRun());

		UpgradeRunResource userUpgradeRunResource =
			_createUserUpgradeRunResource();

		_assertProblemStatus(
			"NOT_FOUND",
			() -> userUpgradeRunResource.getUpgradeRunByExternalReferenceCode(
				upgradeRun.getExternalReferenceCode()));
	}

	private void _testPostUpgradeRunWithoutOmniadmin() throws Exception {
		UpgradeRunResource userUpgradeRunResource =
			_createUserUpgradeRunResource();

		_assertProblemStatus(
			"FORBIDDEN",
			() -> userUpgradeRunResource.postUpgradeRun(randomUpgradeRun()));
	}

	private void _testResumeUpgradeRunWithoutOmniadmin() throws Exception {
		UpgradeRun upgradeRun = _postUpgradeRun(randomUpgradeRun());

		UpgradeRunResource userUpgradeRunResource =
			_createUserUpgradeRunResource();

		_assertProblemStatus(
			"FORBIDDEN",
			() ->
				userUpgradeRunResource.
					postUpgradeRunByExternalReferenceCodeResume(
						upgradeRun.getExternalReferenceCode(),
						new UpgradeRunResumption()));
	}

	private UpgradeRun _trackUpgradeRun(UpgradeRun upgradeRun)
		throws Exception {

		_upgradeRuns.add(
			_upgradeRunLocalService.getUpgradeRun(upgradeRun.getId()));

		return upgradeRun;
	}

	private void _waitForRunner() throws Exception {
		for (com.liferay.upgrades.lab.agent.remote.model.UpgradeRun upgradeRun :
				_upgradeRuns) {

			UpgradeRunnerState upgradeRunnerState = null;

			for (int i = 0; i < 60; i++) {
				upgradeRunnerState = _upgradeRunner.getUpgradeRunnerState(
					upgradeRun.getExternalReferenceCode());

				if (UpgradeRunConstants.isTerminal(
						upgradeRunnerState.getStatus())) {

					break;
				}

				Thread.sleep(500);
			}

			Assert.assertTrue(
				upgradeRunnerState.getStatusMessage(),
				UpgradeRunConstants.isTerminal(upgradeRunnerState.getStatus()));
		}
	}

	@Inject
	private UpgradeRunLocalService _upgradeRunLocalService;

	@Inject
	private UpgradeRunner _upgradeRunner;

	@DeleteAfterTestRun
	private final List<com.liferay.upgrades.lab.agent.remote.model.UpgradeRun>
		_upgradeRuns = new ArrayList<>();

}