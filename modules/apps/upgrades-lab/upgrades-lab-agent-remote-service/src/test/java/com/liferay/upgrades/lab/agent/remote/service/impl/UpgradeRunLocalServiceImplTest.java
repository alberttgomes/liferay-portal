/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.service.impl;

import com.liferay.counter.kernel.service.CounterLocalService;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.test.rule.LiferayUnitTestRule;
import com.liferay.upgrades.lab.agent.remote.constants.UpgradeRunConstants;
import com.liferay.upgrades.lab.agent.remote.constants.UpgradeRunSettingsKeys;
import com.liferay.upgrades.lab.agent.remote.exception.UpgradeRunBranchException;
import com.liferay.upgrades.lab.agent.remote.exception.UpgradeRunCredentialKeyReferenceException;
import com.liferay.upgrades.lab.agent.remote.exception.UpgradeRunFirstPhaseException;
import com.liferay.upgrades.lab.agent.remote.exception.UpgradeRunRepositoryURLException;
import com.liferay.upgrades.lab.agent.remote.exception.UpgradeRunResultBranchException;
import com.liferay.upgrades.lab.agent.remote.exception.UpgradeRunSettingsException;
import com.liferay.upgrades.lab.agent.remote.exception.UpgradeRunStatusException;
import com.liferay.upgrades.lab.agent.remote.exception.UpgradeRunnerException;
import com.liferay.upgrades.lab.agent.remote.model.UpgradeRun;
import com.liferay.upgrades.lab.agent.remote.model.impl.UpgradeRunImpl;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunRequest;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunner;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunnerState;
import com.liferay.upgrades.lab.agent.remote.service.persistence.UpgradeRunPersistence;

import java.util.Collections;
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

		String credentialKeyReference = _randomKeyReference();
		String customerName = RandomTestUtil.randomString();
		String licensePath = RandomTestUtil.randomString();
		String upgradeTargetVersion = RandomTestUtil.randomString();

		UpgradeRun upgradeRun = _upgradeRunLocalServiceImpl.addUpgradeRun(
			_USER_ID, RandomTestUtil.randomString(), credentialKeyReference,
			customerName, RandomTestUtil.randomString(),
			RandomTestUtil.randomString(), licensePath,
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			RandomTestUtil.randomString(), upgradeTargetVersion);

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
			credentialKeyReference,
			upgradeRunRequest.getCredentialKeyReference());
		Assert.assertEquals(licensePath, upgradeRunRequest.getLicensePath());
		Assert.assertEquals(
			_UPGRADE_RUN_ID, upgradeRunRequest.getUpgradeRunId());
		Assert.assertEquals(
			upgradeTargetVersion, upgradeRunRequest.getUpgradeTargetVersion());

		Map<String, String> settings = upgradeRunRequest.getSettings();

		Assert.assertEquals(
			customerName, settings.get(UpgradeRunSettingsKeys.CUSTOMER_NAME));
		Assert.assertEquals(
			upgradeTargetVersion,
			settings.get(UpgradeRunSettingsKeys.UPGRADE_TARGET_VERSION));

		Assert.assertEquals(
			_EXTERNAL_REFERENCE_CODE, upgradeRun.getExternalReferenceCode());
		Assert.assertEquals(
			credentialKeyReference, upgradeRun.getCredentialKeyReference());
		Assert.assertEquals(customerName, upgradeRun.getCustomerName());
		Assert.assertEquals(licensePath, upgradeRun.getLicensePath());
		Assert.assertEquals(
			UpgradeRunConstants.STATUS_QUEUED, upgradeRun.getStatus());
		Assert.assertEquals(
			upgradeTargetVersion, upgradeRun.getUpgradeTargetVersion());
		Assert.assertNotNull(upgradeRun.getStartDate());
	}

	@Test
	public void testAddUpgradeRunInvalidBranch() {
		UpgradeRunBranchException upgradeRunBranchException =
			Assert.assertThrows(
				UpgradeRunBranchException.class,
				() -> _addUpgradeRun(
					null, _randomKeyReference(), RandomTestUtil.randomString(),
					RandomTestUtil.randomString(),
					RandomTestUtil.randomString()));

		Assert.assertEquals(
			"Branch is null", upgradeRunBranchException.getMessage());

		String branch = "-" + RandomTestUtil.randomString();

		upgradeRunBranchException = Assert.assertThrows(
			UpgradeRunBranchException.class,
			() -> _addUpgradeRun(
				branch, _randomKeyReference(), RandomTestUtil.randomString(),
				RandomTestUtil.randomString(), RandomTestUtil.randomString()));

		Assert.assertEquals(
			"Branch " + branch + " must not start with a dash",
			upgradeRunBranchException.getMessage());

		Mockito.verifyNoInteractions(_upgradeRunner, _upgradeRunPersistence);
	}

	@Test
	public void testAddUpgradeRunInvalidCredentialKeyReference() {
		UpgradeRunCredentialKeyReferenceException
			upgradeRunCredentialKeyReferenceException = Assert.assertThrows(
				UpgradeRunCredentialKeyReferenceException.class,
				() -> _addUpgradeRun(
					RandomTestUtil.randomString(),
					RandomTestUtil.randomString()));

		Assert.assertEquals(
			"Credential key reference is not a key reference",
			upgradeRunCredentialKeyReferenceException.getMessage());

		Mockito.verifyNoInteractions(_upgradeRunner, _upgradeRunPersistence);
	}

	@Test
	public void testAddUpgradeRunInvalidRepositoryURL() {
		UpgradeRunRepositoryURLException upgradeRunRepositoryURLException =
			Assert.assertThrows(
				UpgradeRunRepositoryURLException.class,
				() -> _addUpgradeRun(
					RandomTestUtil.randomString(), _randomKeyReference(),
					RandomTestUtil.randomString(), null,
					RandomTestUtil.randomString()));

		Assert.assertEquals(
			"Repository URL is null",
			upgradeRunRepositoryURLException.getMessage());

		String repositoryURL = "-" + RandomTestUtil.randomString();

		upgradeRunRepositoryURLException = Assert.assertThrows(
			UpgradeRunRepositoryURLException.class,
			() -> _addUpgradeRun(
				RandomTestUtil.randomString(), _randomKeyReference(),
				RandomTestUtil.randomString(), repositoryURL,
				RandomTestUtil.randomString()));

		Assert.assertEquals(
			"Repository URL " + repositoryURL + " must not start with a dash",
			upgradeRunRepositoryURLException.getMessage());

		Mockito.verifyNoInteractions(_upgradeRunner, _upgradeRunPersistence);
	}

	@Test
	public void testAddUpgradeRunMissingSetting() {
		UpgradeRunSettingsException upgradeRunSettingsException =
			Assert.assertThrows(
				UpgradeRunSettingsException.class,
				() -> _addUpgradeRun(_randomKeyReference(), null));

		Assert.assertEquals(
			"Setting \"customer.name\" is null",
			upgradeRunSettingsException.getMessage());

		upgradeRunSettingsException = Assert.assertThrows(
			UpgradeRunSettingsException.class,
			() -> _addUpgradeRun(
				RandomTestUtil.randomString(), _randomKeyReference(),
				RandomTestUtil.randomString(), RandomTestUtil.randomString(),
				null));

		Assert.assertEquals(
			"Setting \"upgrade.target.version\" is null",
			upgradeRunSettingsException.getMessage());

		Mockito.verifyNoInteractions(_upgradeRunner, _upgradeRunPersistence);
	}

	@Test
	public void testAddUpgradeRunSubmitFails() throws Exception {
		String statusMessage = RandomTestUtil.randomString();

		Mockito.when(
			_upgradeRunner.submit(Mockito.any(UpgradeRunRequest.class))
		).thenThrow(
			new UpgradeRunnerException(statusMessage)
		);

		UpgradeRun upgradeRun = _addUpgradeRun(
			_randomKeyReference(), RandomTestUtil.randomString());

		Mockito.verify(
			_upgradeRunPersistence
		).update(
			upgradeRun
		);

		Assert.assertTrue(
			Validator.isNull(upgradeRun.getExternalReferenceCode()));
		Assert.assertEquals(
			UpgradeRunConstants.STATUS_FAILED, upgradeRun.getStatus());
		Assert.assertEquals(statusMessage, upgradeRun.getStatusMessage());
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

		UpgradeRun upgradeRun = _addUpgradeRun(
			"", RandomTestUtil.randomString());

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
			_EXTERNAL_REFERENCE_CODE, _COMPANY_ID);

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
			_EXTERNAL_REFERENCE_CODE, _COMPANY_ID);

		Mockito.verifyNoInteractions(_upgradeRunner);

		Assert.assertEquals(
			UpgradeRunConstants.STATUS_SUCCESSFUL, upgradeRun.getStatus());
	}

	@Test
	public void testRefreshUpgradeRun() throws Exception {
		_mockUpgradeRun(UpgradeRunConstants.STATUS_CLONING);

		String statusMessage = RandomTestUtil.randomString();
		String workspacePath = RandomTestUtil.randomString();

		Mockito.when(
			_upgradeRunner.getUpgradeRunnerState(_EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			new UpgradeRunnerState(
				null, null, UpgradeRunConstants.STATUS_RUNNING, statusMessage,
				workspacePath)
		);

		UpgradeRun upgradeRun = _upgradeRunLocalServiceImpl.refreshUpgradeRun(
			_EXTERNAL_REFERENCE_CODE, _COMPANY_ID);

		Assert.assertEquals(
			UpgradeRunConstants.STATUS_RUNNING, upgradeRun.getStatus());
		Assert.assertEquals(statusMessage, upgradeRun.getStatusMessage());
		Assert.assertEquals(workspacePath, upgradeRun.getWorkspacePath());
		Assert.assertNull(upgradeRun.getEndDate());
	}

	@Test
	public void testRefreshUpgradeRunBackwards() throws Exception {
		_mockUpgradeRun(UpgradeRunConstants.STATUS_RUNNING);

		Mockito.when(
			_upgradeRunner.getUpgradeRunnerState(_EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			new UpgradeRunnerState(
				null, null, UpgradeRunConstants.STATUS_CLONING,
				RandomTestUtil.randomString())
		);

		UpgradeRun upgradeRun = _upgradeRunLocalServiceImpl.refreshUpgradeRun(
			_EXTERNAL_REFERENCE_CODE, _COMPANY_ID);

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

		String pullRequestURL = RandomTestUtil.randomString();
		String resultBranch = RandomTestUtil.randomString();

		Mockito.when(
			_upgradeRunner.getUpgradeRunnerState(_EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			new UpgradeRunnerState(
				pullRequestURL, resultBranch,
				UpgradeRunConstants.STATUS_SUCCESSFUL,
				RandomTestUtil.randomString())
		);

		UpgradeRun upgradeRun = _upgradeRunLocalServiceImpl.refreshUpgradeRun(
			_EXTERNAL_REFERENCE_CODE, _COMPANY_ID);

		Assert.assertEquals(pullRequestURL, upgradeRun.getPullRequestURL());
		Assert.assertEquals(resultBranch, upgradeRun.getResultBranch());
		Assert.assertEquals(
			UpgradeRunConstants.STATUS_SUCCESSFUL, upgradeRun.getStatus());
		Assert.assertNotNull(upgradeRun.getEndDate());
	}

	@Test
	public void testRefreshUpgradeRunStatusTransitions() throws Exception {
		for (int status : _STATUSES) {
			for (int newStatus : _STATUSES) {
				_mockUpgradeRun(status);

				Mockito.when(
					_upgradeRunner.getUpgradeRunnerState(
						_EXTERNAL_REFERENCE_CODE)
				).thenReturn(
					new UpgradeRunnerState(
						null, null, newStatus, RandomTestUtil.randomString())
				);

				UpgradeRun upgradeRun =
					_upgradeRunLocalServiceImpl.refreshUpgradeRun(
						_EXTERNAL_REFERENCE_CODE, _COMPANY_ID);

				String message = status + " to " + newStatus;

				if (UpgradeRunConstants.isTerminal(status) ||
					(!UpgradeRunConstants.isTerminal(newStatus) &&
					 (newStatus < status))) {

					Assert.assertEquals(
						message, status, upgradeRun.getStatus());
				}
				else {
					Assert.assertEquals(
						message, newStatus, upgradeRun.getStatus());
				}
			}
		}
	}

	@Test
	public void testRefreshUpgradeRunTerminal() throws Exception {
		_mockUpgradeRun(UpgradeRunConstants.STATUS_FAILED);

		_upgradeRunLocalServiceImpl.refreshUpgradeRun(
			_EXTERNAL_REFERENCE_CODE, _COMPANY_ID);

		Mockito.verifyNoInteractions(_upgradeRunner);
	}

	@Test
	public void testRefreshUpgradeRunUnknownToRunner() throws Exception {
		_mockUpgradeRun(UpgradeRunConstants.STATUS_RUNNING);

		String statusMessage = RandomTestUtil.randomString();

		Mockito.when(
			_upgradeRunner.getUpgradeRunnerState(_EXTERNAL_REFERENCE_CODE)
		).thenThrow(
			new UpgradeRunnerException(statusMessage)
		);

		UpgradeRun upgradeRun = _upgradeRunLocalServiceImpl.refreshUpgradeRun(
			_EXTERNAL_REFERENCE_CODE, _COMPANY_ID);

		Assert.assertEquals(
			UpgradeRunConstants.STATUS_FAILED, upgradeRun.getStatus());
		Assert.assertEquals(statusMessage, upgradeRun.getStatusMessage());
	}

	@Test
	public void testResumeUpgradeRun() throws Exception {
		UpgradeRun upgradeRun = _mockUpgradeRun(
			UpgradeRunConstants.STATUS_BLOCKED);

		upgradeRun.setBranch(RandomTestUtil.randomString());
		upgradeRun.setCredentialKeyReference(_randomKeyReference());
		upgradeRun.setCustomerName(RandomTestUtil.randomString());
		upgradeRun.setDbTargetType(RandomTestUtil.randomString());
		upgradeRun.setDbTargetVersion(RandomTestUtil.randomString());
		upgradeRun.setLicensePath(RandomTestUtil.randomString());
		upgradeRun.setNodeVersion(RandomTestUtil.randomString());
		upgradeRun.setRepositoryURL(RandomTestUtil.randomString());
		upgradeRun.setResultBranch("phase3");
		upgradeRun.setSearchVersion(RandomTestUtil.randomString());
		upgradeRun.setUpgradeSourceVersion(RandomTestUtil.randomString());
		upgradeRun.setUpgradeTargetJavaVersion(RandomTestUtil.randomString());
		upgradeRun.setUpgradeTargetVersion(RandomTestUtil.randomString());
		upgradeRun.setWorkspacePath(RandomTestUtil.randomString());

		String resumedExternalReferenceCode = RandomTestUtil.randomString();

		Mockito.when(
			_upgradeRunner.submit(Mockito.any(UpgradeRunRequest.class))
		).thenReturn(
			resumedExternalReferenceCode
		);

		String key = RandomTestUtil.randomString();
		String nodeVersion = RandomTestUtil.randomString();
		String value = RandomTestUtil.randomString();

		UpgradeRun resumedUpgradeRun =
			_upgradeRunLocalServiceImpl.resumeUpgradeRun(
				_USER_ID, _EXTERNAL_REFERENCE_CODE, _COMPANY_ID, 0,
				HashMapBuilder.put(
					key, value
				).put(
					UpgradeRunSettingsKeys.NODE_VERSION, nodeVersion
				).build());

		ArgumentCaptor<UpgradeRunRequest> argumentCaptor =
			ArgumentCaptor.forClass(UpgradeRunRequest.class);

		Mockito.verify(
			_upgradeRunner
		).submit(
			argumentCaptor.capture()
		);

		UpgradeRunRequest upgradeRunRequest = argumentCaptor.getValue();

		Assert.assertEquals(
			upgradeRun.getBranch(), upgradeRunRequest.getBranch());
		Assert.assertEquals(
			upgradeRun.getCredentialKeyReference(),
			upgradeRunRequest.getCredentialKeyReference());
		Assert.assertEquals(3, upgradeRunRequest.getFirstPhase());
		Assert.assertEquals(
			upgradeRun.getLicensePath(), upgradeRunRequest.getLicensePath());
		Assert.assertEquals(
			upgradeRun.getRepositoryURL(),
			upgradeRunRequest.getRepositoryURL());
		Assert.assertEquals(
			upgradeRun.getResultBranch(), upgradeRunRequest.getResultBranch());
		Assert.assertEquals(
			HashMapBuilder.put(
				key, value
			).put(
				UpgradeRunSettingsKeys.CUSTOMER_NAME,
				upgradeRun.getCustomerName()
			).put(
				UpgradeRunSettingsKeys.DB_TARGET_TYPE,
				upgradeRun.getDbTargetType()
			).put(
				UpgradeRunSettingsKeys.DB_TARGET_VERSION,
				upgradeRun.getDbTargetVersion()
			).put(
				UpgradeRunSettingsKeys.NODE_VERSION, nodeVersion
			).put(
				UpgradeRunSettingsKeys.SEARCH_VERSION,
				upgradeRun.getSearchVersion()
			).put(
				UpgradeRunSettingsKeys.UPGRADE_SOURCE_VERSION,
				upgradeRun.getUpgradeSourceVersion()
			).put(
				UpgradeRunSettingsKeys.UPGRADE_TARGET_JAVA_VERSION,
				upgradeRun.getUpgradeTargetJavaVersion()
			).put(
				UpgradeRunSettingsKeys.UPGRADE_TARGET_VERSION,
				upgradeRun.getUpgradeTargetVersion()
			).build(),
			upgradeRunRequest.getSettings());
		Assert.assertEquals(
			_UPGRADE_RUN_ID, upgradeRunRequest.getUpgradeRunId());
		Assert.assertEquals(
			upgradeRun.getUpgradeTargetVersion(),
			upgradeRunRequest.getUpgradeTargetVersion());
		Assert.assertEquals(
			upgradeRun.getWorkspacePath(),
			upgradeRunRequest.getWorkspacePath());

		Assert.assertEquals(
			resumedExternalReferenceCode,
			resumedUpgradeRun.getExternalReferenceCode());
		Assert.assertEquals(3, resumedUpgradeRun.getFirstPhase());
		Assert.assertEquals(nodeVersion, resumedUpgradeRun.getNodeVersion());
		Assert.assertTrue(
			Validator.isNull(resumedUpgradeRun.getResultBranch()));
		Assert.assertEquals(
			UpgradeRunConstants.STATUS_QUEUED, resumedUpgradeRun.getStatus());
		Assert.assertTrue(
			Validator.isNull(resumedUpgradeRun.getWorkspacePath()));

		Assert.assertEquals(
			UpgradeRunConstants.STATUS_BLOCKED, upgradeRun.getStatus());

		UpgradeRunSettingsException upgradeRunSettingsException =
			Assert.assertThrows(
				UpgradeRunSettingsException.class,
				() -> _upgradeRunLocalServiceImpl.resumeUpgradeRun(
					_USER_ID, _EXTERNAL_REFERENCE_CODE, _COMPANY_ID, 0,
					Collections.singletonMap(
						UpgradeRunSettingsKeys.CUSTOMER_NAME, null)));

		Assert.assertEquals(
			"Setting \"customer.name\" is null",
			upgradeRunSettingsException.getMessage());
	}

	@Test
	public void testResumeUpgradeRunInvalidFirstPhase() throws Exception {
		UpgradeRun upgradeRun = _mockUpgradeRun(
			UpgradeRunConstants.STATUS_FAILED);

		upgradeRun.setBranch(RandomTestUtil.randomString());
		upgradeRun.setRepositoryURL(RandomTestUtil.randomString());

		for (int firstPhase :
				new int[] {-1, UpgradeRunConstants.PHASE_LAST + 1}) {

			UpgradeRunFirstPhaseException upgradeRunFirstPhaseException =
				Assert.assertThrows(
					UpgradeRunFirstPhaseException.class,
					() -> _upgradeRunLocalServiceImpl.resumeUpgradeRun(
						_USER_ID, _EXTERNAL_REFERENCE_CODE, _COMPANY_ID,
						firstPhase, null));

			Assert.assertEquals(
				StringBundler.concat(
					"First phase ", firstPhase, " is not between ",
					UpgradeRunConstants.PHASE_FIRST, " and ",
					UpgradeRunConstants.PHASE_LAST),
				upgradeRunFirstPhaseException.getMessage());
		}

		Mockito.verifyNoInteractions(_upgradeRunner);
	}

	@Test
	public void testResumeUpgradeRunNotFinished() throws Exception {
		_mockUpgradeRun(UpgradeRunConstants.STATUS_RUNNING);

		UpgradeRunStatusException upgradeRunStatusException =
			Assert.assertThrows(
				UpgradeRunStatusException.class,
				() -> _upgradeRunLocalServiceImpl.resumeUpgradeRun(
					_USER_ID, _EXTERNAL_REFERENCE_CODE, _COMPANY_ID, 0, null));

		Assert.assertEquals(
			StringBundler.concat(
				"Upgrade run ", _EXTERNAL_REFERENCE_CODE, " has not finished"),
			upgradeRunStatusException.getMessage());

		Mockito.verifyNoInteractions(_upgradeRunner);
	}

	@Test
	public void testResumeUpgradeRunWithResultBranch() throws Exception {
		UpgradeRun upgradeRun = _mockFinishedUpgradeRun();

		Mockito.when(
			_upgradeRunner.submit(Mockito.any(UpgradeRunRequest.class))
		).thenReturn(
			RandomTestUtil.randomString()
		);

		Map<String, Integer> firstPhases = HashMapBuilder.put(
			"phase", UpgradeRunConstants.PHASE_FIRST
		).put(
			"phase0", UpgradeRunConstants.PHASE_FIRST
		).put(
			"phase2-x", UpgradeRunConstants.PHASE_FIRST
		).put(
			"phase3", 3
		).put(
			"phase4", UpgradeRunConstants.PHASE_LAST
		).put(
			"phase5", UpgradeRunConstants.PHASE_FIRST
		).put(
			"upgrade/7.4-to-2026.q1.0", UpgradeRunConstants.PHASE_FIRST
		).build();

		for (Map.Entry<String, Integer> entry : firstPhases.entrySet()) {
			upgradeRun.setResultBranch(entry.getKey());

			UpgradeRun resumedUpgradeRun =
				_upgradeRunLocalServiceImpl.resumeUpgradeRun(
					_USER_ID, _EXTERNAL_REFERENCE_CODE, _COMPANY_ID, 0, null);

			Assert.assertEquals(
				entry.getKey(), (int)entry.getValue(),
				resumedUpgradeRun.getFirstPhase());
		}
	}

	@Test
	public void testResumeUpgradeRunWithoutResultBranch() throws Exception {
		UpgradeRun upgradeRun = _mockFinishedUpgradeRun();

		UpgradeRunResultBranchException upgradeRunResultBranchException =
			Assert.assertThrows(
				UpgradeRunResultBranchException.class,
				() -> _upgradeRunLocalServiceImpl.resumeUpgradeRun(
					_USER_ID, _EXTERNAL_REFERENCE_CODE, _COMPANY_ID, 2, null));

		Assert.assertEquals(
			"Result branch is null",
			upgradeRunResultBranchException.getMessage());

		Mockito.verifyNoInteractions(_upgradeRunner);

		Mockito.when(
			_upgradeRunner.submit(Mockito.any(UpgradeRunRequest.class))
		).thenReturn(
			RandomTestUtil.randomString()
		);

		UpgradeRun resumedUpgradeRun =
			_upgradeRunLocalServiceImpl.resumeUpgradeRun(
				_USER_ID, _EXTERNAL_REFERENCE_CODE, _COMPANY_ID, 0, null);

		Assert.assertEquals(
			upgradeRun.getCustomerName(), resumedUpgradeRun.getCustomerName());
		Assert.assertEquals(
			UpgradeRunConstants.PHASE_FIRST, resumedUpgradeRun.getFirstPhase());
		Assert.assertEquals(
			UpgradeRunConstants.STATUS_QUEUED, resumedUpgradeRun.getStatus());
	}

	private UpgradeRun _addUpgradeRun(
			String credentialKeyReference, String customerName)
		throws Exception {

		return _addUpgradeRun(
			RandomTestUtil.randomString(), credentialKeyReference, customerName,
			RandomTestUtil.randomString(), RandomTestUtil.randomString());
	}

	private UpgradeRun _addUpgradeRun(
			String branch, String credentialKeyReference, String customerName,
			String repositoryURL, String upgradeTargetVersion)
		throws Exception {

		return _upgradeRunLocalServiceImpl.addUpgradeRun(
			_USER_ID, branch, credentialKeyReference, customerName,
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			repositoryURL, RandomTestUtil.randomString(),
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			upgradeTargetVersion);
	}

	private UpgradeRun _mockFinishedUpgradeRun() throws Exception {
		UpgradeRun upgradeRun = _mockUpgradeRun(
			UpgradeRunConstants.STATUS_FAILED);

		upgradeRun.setBranch(RandomTestUtil.randomString());
		upgradeRun.setCustomerName(RandomTestUtil.randomString());
		upgradeRun.setDbTargetType(RandomTestUtil.randomString());
		upgradeRun.setDbTargetVersion(RandomTestUtil.randomString());
		upgradeRun.setNodeVersion(RandomTestUtil.randomString());
		upgradeRun.setRepositoryURL(RandomTestUtil.randomString());
		upgradeRun.setSearchVersion(RandomTestUtil.randomString());
		upgradeRun.setUpgradeSourceVersion(RandomTestUtil.randomString());
		upgradeRun.setUpgradeTargetJavaVersion(RandomTestUtil.randomString());
		upgradeRun.setUpgradeTargetVersion(RandomTestUtil.randomString());

		return upgradeRun;
	}

	private UpgradeRun _mockUpgradeRun(int status) throws Exception {
		UpgradeRun upgradeRun = new UpgradeRunImpl();

		upgradeRun.setExternalReferenceCode(_EXTERNAL_REFERENCE_CODE);
		upgradeRun.setCompanyId(_COMPANY_ID);
		upgradeRun.setStatus(status);

		Mockito.when(
			_upgradeRunPersistence.findByERC_C(
				_EXTERNAL_REFERENCE_CODE, _COMPANY_ID)
		).thenReturn(
			upgradeRun
		);

		return upgradeRun;
	}

	private String _randomKeyReference() {
		return "${secretRef:*:" + RandomTestUtil.randomString() + "}";
	}

	private static final long _COMPANY_ID = RandomTestUtil.randomLong();

	private static final String _EXTERNAL_REFERENCE_CODE =
		RandomTestUtil.randomString();

	private static final int[] _STATUSES = {
		UpgradeRunConstants.STATUS_BLOCKED,
		UpgradeRunConstants.STATUS_CANCELLED,
		UpgradeRunConstants.STATUS_CLONING, UpgradeRunConstants.STATUS_FAILED,
		UpgradeRunConstants.STATUS_PROVISIONING,
		UpgradeRunConstants.STATUS_PUBLISHING,
		UpgradeRunConstants.STATUS_QUEUED, UpgradeRunConstants.STATUS_RUNNING,
		UpgradeRunConstants.STATUS_SUCCESSFUL,
		UpgradeRunConstants.STATUS_TIMED_OUT,
		UpgradeRunConstants.STATUS_VALIDATING
	};

	private static final long _UPGRADE_RUN_ID = RandomTestUtil.randomLong();

	private static final long _USER_ID = RandomTestUtil.randomLong();

	private UpgradeRunLocalServiceImpl _upgradeRunLocalServiceImpl;
	private UpgradeRunPersistence _upgradeRunPersistence;
	private UpgradeRunner _upgradeRunner;

}