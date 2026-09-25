/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.service.impl;

import com.liferay.counter.kernel.service.CounterLocalService;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.test.rule.LiferayUnitTestRule;
import com.liferay.upgrades.lab.agent.remote.constants.UpgradeRunConstants;
import com.liferay.upgrades.lab.agent.remote.constants.UpgradeRunSettingsKeys;
import com.liferay.upgrades.lab.agent.remote.exception.UpgradeRunRequestException;
import com.liferay.upgrades.lab.agent.remote.exception.UpgradeRunnerException;
import com.liferay.upgrades.lab.agent.remote.model.UpgradeRun;
import com.liferay.upgrades.lab.agent.remote.model.impl.UpgradeRunImpl;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunRequest;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunner;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunnerState;
import com.liferay.upgrades.lab.agent.remote.service.persistence.UpgradeRunPersistence;

import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Albert Gomes Cabral
 */
public class UpgradeRunLocalServiceImplTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() throws Exception {
		_upgradeRunLocalServiceImpl = new UpgradeRunLocalServiceImpl();

		UserLocalService userLocalService = Mockito.mock(
			UserLocalService.class);

		User user = Mockito.mock(User.class);

		Mockito.when(
			user.getCompanyId()
		).thenReturn(
			_COMPANY_ID
		);

		Mockito.when(
			user.getUserId()
		).thenReturn(
			_USER_ID
		);

		Mockito.when(
			userLocalService.getUser(_USER_ID)
		).thenReturn(
			user
		);

		ReflectionTestUtil.setFieldValue(
			_upgradeRunLocalServiceImpl, "_userLocalService", userLocalService);

		CounterLocalService counterLocalService = Mockito.mock(
			CounterLocalService.class);

		Mockito.when(
			counterLocalService.increment(UpgradeRun.class.getName())
		).thenReturn(
			_UPGRADE_RUN_ID
		);

		ReflectionTestUtil.setFieldValue(
			_upgradeRunLocalServiceImpl, "counterLocalService",
			counterLocalService);

		_upgradeRunPersistence = Mockito.mock(UpgradeRunPersistence.class);

		Mockito.when(
			_upgradeRunPersistence.create(_UPGRADE_RUN_ID)
		).thenReturn(
			new UpgradeRunImpl()
		);

		Mockito.when(
			_upgradeRunPersistence.update(Mockito.any(UpgradeRun.class))
		).thenAnswer(
			invocationOnMock -> invocationOnMock.getArgument(0)
		);

		ReflectionTestUtil.setFieldValue(
			_upgradeRunLocalServiceImpl, "upgradeRunPersistence",
			_upgradeRunPersistence);

		_upgradeRunner = Mockito.mock(UpgradeRunner.class);

		ReflectionTestUtil.setFieldValue(
			_upgradeRunLocalServiceImpl, "_upgradeRunner", _upgradeRunner);
	}

	@Test
	public void testAddUpgradeRun() throws Exception {
		Mockito.when(
			_upgradeRunner.submit(Mockito.any(UpgradeRunRequest.class))
		).thenReturn(
			_EXTERNAL_REFERENCE_CODE
		);

		UpgradeRun upgradeRun = _addUpgradeRun(
			"${secretRef:*:acme-deploy-key}", "acme");

		ArgumentCaptor<UpgradeRunRequest> argumentCaptor =
			ArgumentCaptor.forClass(UpgradeRunRequest.class);

		Mockito.verify(
			_upgradeRunner
		).submit(
			argumentCaptor.capture()
		);

		UpgradeRunRequest upgradeRunRequest = argumentCaptor.getValue();

		Assert.assertEquals(_COMPANY_ID, upgradeRunRequest.getCompanyId());
		Assert.assertEquals(
			"${secretRef:*:acme-deploy-key}",
			upgradeRunRequest.getCredentialKeyReference());
		Assert.assertEquals(
			_UPGRADE_RUN_ID, upgradeRunRequest.getUpgradeRunId());

		Map<String, String> settings = upgradeRunRequest.getSettings();

		Assert.assertEquals(
			"acme", settings.get(UpgradeRunSettingsKeys.CUSTOMER_NAME));
		Assert.assertEquals(
			upgradeRun.getTargetRelease(),
			settings.get(UpgradeRunSettingsKeys.UPGRADE_TARGET_VERSION));

		Assert.assertEquals(
			_EXTERNAL_REFERENCE_CODE, upgradeRun.getExternalReferenceCode());
		Assert.assertEquals(
			"${secretRef:*:acme-deploy-key}",
			upgradeRun.getCredentialKeyReference());
		Assert.assertEquals("acme", upgradeRun.getCustomerName());
		Assert.assertEquals(
			UpgradeRunConstants.STATUS_QUEUED, upgradeRun.getStatus());
		Assert.assertNotNull(upgradeRun.getStartDate());
	}

	@Test
	public void testAddUpgradeRunMissingSetting() {
		Assert.assertThrows(
			UpgradeRunRequestException.class,
			() -> _addUpgradeRun("${secretRef:*:acme-deploy-key}", null));

		Mockito.verifyNoInteractions(_upgradeRunner);
	}

	@Test
	public void testAddUpgradeRunSubmitFails() throws Exception {
		Mockito.when(
			_upgradeRunner.submit(Mockito.any(UpgradeRunRequest.class))
		).thenThrow(
			new UpgradeRunnerException("No substrate")
		);

		UpgradeRun upgradeRun = _addUpgradeRun(
			"${secretRef:*:acme-deploy-key}", "acme");

		Mockito.verify(
			_upgradeRunPersistence
		).update(
			upgradeRun
		);

		Assert.assertTrue(
			Validator.isNull(upgradeRun.getExternalReferenceCode()));
		Assert.assertEquals(
			UpgradeRunConstants.STATUS_FAILED, upgradeRun.getStatus());
		Assert.assertEquals("No substrate", upgradeRun.getStatusMessage());
		Assert.assertNotNull(upgradeRun.getEndDate());
		Assert.assertNull(upgradeRun.getStartDate());
	}

	@Test
	public void testAddUpgradeRunWithoutCredential() throws Exception {
		Mockito.when(
			_upgradeRunner.submit(Mockito.any(UpgradeRunRequest.class))
		).thenReturn(
			_EXTERNAL_REFERENCE_CODE
		);

		UpgradeRun upgradeRun = _addUpgradeRun("", "acme");

		ArgumentCaptor<UpgradeRunRequest> argumentCaptor =
			ArgumentCaptor.forClass(UpgradeRunRequest.class);

		Mockito.verify(
			_upgradeRunner
		).submit(
			argumentCaptor.capture()
		);

		UpgradeRunRequest upgradeRunRequest = argumentCaptor.getValue();

		Assert.assertNull(upgradeRunRequest.getCredentialKeyReference());

		Assert.assertTrue(
			Validator.isNull(upgradeRun.getCredentialKeyReference()));
		Assert.assertEquals(
			UpgradeRunConstants.STATUS_QUEUED, upgradeRun.getStatus());
	}

	@Test
	public void testCancelUpgradeRun() throws Exception {
		_mockUpgradeRun(UpgradeRunConstants.STATUS_RUNNING);

		UpgradeRun upgradeRun = _upgradeRunLocalServiceImpl.cancelUpgradeRun(
			_COMPANY_ID, _EXTERNAL_REFERENCE_CODE);

		Mockito.verify(
			_upgradeRunner
		).cancel(
			_EXTERNAL_REFERENCE_CODE
		);

		Assert.assertEquals(
			UpgradeRunConstants.STATUS_CANCELLED, upgradeRun.getStatus());
		Assert.assertNotNull(upgradeRun.getEndDate());
	}

	@Test
	public void testCancelUpgradeRunTerminal() throws Exception {
		_mockUpgradeRun(UpgradeRunConstants.STATUS_SUCCESSFUL);

		UpgradeRun upgradeRun = _upgradeRunLocalServiceImpl.cancelUpgradeRun(
			_COMPANY_ID, _EXTERNAL_REFERENCE_CODE);

		Mockito.verifyNoInteractions(_upgradeRunner);

		Assert.assertEquals(
			UpgradeRunConstants.STATUS_SUCCESSFUL, upgradeRun.getStatus());
	}

	@Test
	public void testRefreshUpgradeRun() throws Exception {
		_mockUpgradeRun(UpgradeRunConstants.STATUS_CLONING);

		Mockito.when(
			_upgradeRunner.getUpgradeRunnerState(_EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			new UpgradeRunnerState(
				null, null, UpgradeRunConstants.STATUS_RUNNING, "Upgrading",
				"/tmp/upgrade-run-1/workspace")
		);

		UpgradeRun upgradeRun = _upgradeRunLocalServiceImpl.refreshUpgradeRun(
			_COMPANY_ID, _EXTERNAL_REFERENCE_CODE);

		Assert.assertEquals(
			UpgradeRunConstants.STATUS_RUNNING, upgradeRun.getStatus());
		Assert.assertEquals("Upgrading", upgradeRun.getStatusMessage());
		Assert.assertEquals(
			"/tmp/upgrade-run-1/workspace", upgradeRun.getWorkspacePath());
		Assert.assertNull(upgradeRun.getEndDate());
	}

	@Test
	public void testRefreshUpgradeRunBackwards() throws Exception {
		_mockUpgradeRun(UpgradeRunConstants.STATUS_RUNNING);

		Mockito.when(
			_upgradeRunner.getUpgradeRunnerState(_EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			new UpgradeRunnerState(
				null, null, UpgradeRunConstants.STATUS_CLONING, "Late report")
		);

		UpgradeRun upgradeRun = _upgradeRunLocalServiceImpl.refreshUpgradeRun(
			_COMPANY_ID, _EXTERNAL_REFERENCE_CODE);

		Mockito.verify(
			_upgradeRunPersistence, Mockito.never()
		).update(
			Mockito.any(UpgradeRun.class)
		);

		Assert.assertEquals(
			UpgradeRunConstants.STATUS_RUNNING, upgradeRun.getStatus());
	}

	@Test
	public void testRefreshUpgradeRunPublished() throws Exception {
		_mockUpgradeRun(UpgradeRunConstants.STATUS_PUBLISHING);

		Mockito.when(
			_upgradeRunner.getUpgradeRunnerState(_EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			new UpgradeRunnerState(
				"https://github.com/acme/workspace/pull/1",
				"upgrade/7.4-to-2026.q1", UpgradeRunConstants.STATUS_SUCCESSFUL,
				"Published")
		);

		UpgradeRun upgradeRun = _upgradeRunLocalServiceImpl.refreshUpgradeRun(
			_COMPANY_ID, _EXTERNAL_REFERENCE_CODE);

		Assert.assertEquals(
			"https://github.com/acme/workspace/pull/1",
			upgradeRun.getPullRequestURL());
		Assert.assertEquals(
			"upgrade/7.4-to-2026.q1", upgradeRun.getResultBranch());
		Assert.assertEquals(
			UpgradeRunConstants.STATUS_SUCCESSFUL, upgradeRun.getStatus());
		Assert.assertNotNull(upgradeRun.getEndDate());
	}

	@Test
	public void testRefreshUpgradeRunTerminal() throws Exception {
		_mockUpgradeRun(UpgradeRunConstants.STATUS_FAILED);

		_upgradeRunLocalServiceImpl.refreshUpgradeRun(
			_COMPANY_ID, _EXTERNAL_REFERENCE_CODE);

		Mockito.verifyNoInteractions(_upgradeRunner);
	}

	@Test
	public void testRefreshUpgradeRunUnknownToRunner() throws Exception {
		_mockUpgradeRun(UpgradeRunConstants.STATUS_RUNNING);

		Mockito.when(
			_upgradeRunner.getUpgradeRunnerState(_EXTERNAL_REFERENCE_CODE)
		).thenThrow(
			new UpgradeRunnerException("No run was found")
		);

		UpgradeRun upgradeRun = _upgradeRunLocalServiceImpl.refreshUpgradeRun(
			_COMPANY_ID, _EXTERNAL_REFERENCE_CODE);

		Assert.assertEquals(
			UpgradeRunConstants.STATUS_FAILED, upgradeRun.getStatus());
		Assert.assertEquals("No run was found", upgradeRun.getStatusMessage());
	}

	private UpgradeRun _addUpgradeRun(
			String credentialKeyReference, String customerName)
		throws Exception {

		return _upgradeRunLocalServiceImpl.addUpgradeRun(
			"main", credentialKeyReference, customerName, "mysql", "8.0",
			"20.18.0", "git@github.com:acme/workspace.git", "8.17.4",
			"2026.q1.0", "7.4.13-u92", "21", _USER_ID);
	}

	private UpgradeRun _mockUpgradeRun(int status) throws Exception {
		UpgradeRun upgradeRun = new UpgradeRunImpl();

		upgradeRun.setCompanyId(_COMPANY_ID);
		upgradeRun.setExternalReferenceCode(_EXTERNAL_REFERENCE_CODE);
		upgradeRun.setStatus(status);

		Mockito.when(
			_upgradeRunPersistence.findByERC_C(
				_EXTERNAL_REFERENCE_CODE, _COMPANY_ID)
		).thenReturn(
			upgradeRun
		);

		return upgradeRun;
	}

	private static final long _COMPANY_ID = 20097;

	private static final String _EXTERNAL_REFERENCE_CODE = "run-1";

	private static final long _UPGRADE_RUN_ID = 42;

	private static final long _USER_ID = 20123;

	private UpgradeRunLocalServiceImpl _upgradeRunLocalServiceImpl;
	private UpgradeRunPersistence _upgradeRunPersistence;
	private UpgradeRunner _upgradeRunner;

}