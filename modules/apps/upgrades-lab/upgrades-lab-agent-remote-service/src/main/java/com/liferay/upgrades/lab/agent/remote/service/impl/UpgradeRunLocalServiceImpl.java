/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.service.impl;

import com.liferay.portal.aop.AopService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.upgrades.lab.agent.remote.constants.UpgradeRunConstants;
import com.liferay.upgrades.lab.agent.remote.constants.UpgradeRunSettingsKeys;
import com.liferay.upgrades.lab.agent.remote.exception.UpgradeRunRequestException;
import com.liferay.upgrades.lab.agent.remote.exception.UpgradeRunnerException;
import com.liferay.upgrades.lab.agent.remote.model.UpgradeRun;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunRequest;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunner;
import com.liferay.upgrades.lab.agent.remote.runner.UpgradeRunnerState;
import com.liferay.upgrades.lab.agent.remote.service.base.UpgradeRunLocalServiceBaseImpl;

import java.util.Date;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Albert Gomes Cabral
 */
@Component(
	property = "model.class.name=com.liferay.upgrades.lab.agent.remote.model.UpgradeRun",
	service = AopService.class
)
public class UpgradeRunLocalServiceImpl extends UpgradeRunLocalServiceBaseImpl {

	@Override
	public UpgradeRun addUpgradeRun(
			String branch, String credentialKeyReference, String customerName,
			String dbTargetType, String dbTargetVersion, String nodeVersion,
			String repositoryURL, String searchVersion, String targetRelease,
			String upgradeSourceVersion, String upgradeTargetJavaVersion,
			long userId)
		throws PortalException {

		User user = _userLocalService.getUser(userId);

		long upgradeRunId = counterLocalService.increment(
			UpgradeRun.class.getName());

		Map<String, String> settings = HashMapBuilder.put(
			UpgradeRunSettingsKeys.CUSTOMER_NAME, customerName
		).put(
			UpgradeRunSettingsKeys.DB_TARGET_TYPE, dbTargetType
		).put(
			UpgradeRunSettingsKeys.DB_TARGET_VERSION, dbTargetVersion
		).put(
			UpgradeRunSettingsKeys.NODE_VERSION, nodeVersion
		).put(
			UpgradeRunSettingsKeys.SEARCH_VERSION, searchVersion
		).put(
			UpgradeRunSettingsKeys.UPGRADE_SOURCE_VERSION, upgradeSourceVersion
		).put(
			UpgradeRunSettingsKeys.UPGRADE_TARGET_JAVA_VERSION,
			upgradeTargetJavaVersion
		).put(
			UpgradeRunSettingsKeys.UPGRADE_TARGET_VERSION, targetRelease
		).build();

		UpgradeRunRequest upgradeRunRequest = null;

		try {
			upgradeRunRequest = new UpgradeRunRequest(
				branch, user.getCompanyId(), credentialKeyReference,
				repositoryURL, settings, targetRelease, upgradeRunId);
		}
		catch (IllegalArgumentException illegalArgumentException) {
			throw new UpgradeRunRequestException(
				illegalArgumentException.getMessage(),
				illegalArgumentException);
		}

		UpgradeRun upgradeRun = upgradeRunPersistence.create(upgradeRunId);

		upgradeRun.setCompanyId(user.getCompanyId());
		upgradeRun.setUserId(user.getUserId());
		upgradeRun.setUserName(user.getFullName());
		upgradeRun.setBranch(branch);
		upgradeRun.setCredentialKeyReference(
			upgradeRunRequest.getCredentialKeyReference());
		upgradeRun.setCustomerName(customerName);
		upgradeRun.setDbTargetType(dbTargetType);
		upgradeRun.setDbTargetVersion(dbTargetVersion);
		upgradeRun.setNodeVersion(nodeVersion);
		upgradeRun.setRepositoryURL(repositoryURL);
		upgradeRun.setSearchVersion(searchVersion);
		upgradeRun.setTargetRelease(targetRelease);
		upgradeRun.setStatus(UpgradeRunConstants.STATUS_QUEUED);
		upgradeRun.setStatusMessage("The run is queued");
		upgradeRun.setUpgradeSourceVersion(upgradeSourceVersion);
		upgradeRun.setUpgradeTargetJavaVersion(upgradeTargetJavaVersion);

		try {
			upgradeRun.setExternalReferenceCode(
				_upgradeRunner.submit(upgradeRunRequest));
		}
		catch (UpgradeRunnerException upgradeRunnerException) {
			upgradeRun.setEndDate(new Date());
			upgradeRun.setStatus(UpgradeRunConstants.STATUS_FAILED);
			upgradeRun.setStatusMessage(upgradeRunnerException.getMessage());

			return upgradeRunPersistence.update(upgradeRun);
		}

		upgradeRun.setStartDate(new Date());

		return upgradeRunPersistence.update(upgradeRun);
	}

	@Override
	public UpgradeRun cancelUpgradeRun(
			long companyId, String externalReferenceCode)
		throws PortalException {

		UpgradeRun upgradeRun = upgradeRunPersistence.findByERC_C(
			externalReferenceCode, companyId);

		if (UpgradeRunConstants.isTerminal(upgradeRun.getStatus())) {
			return upgradeRun;
		}

		_upgradeRunner.cancel(externalReferenceCode);

		return _updateUpgradeRun(
			upgradeRun,
			new UpgradeRunnerState(
				null, null, UpgradeRunConstants.STATUS_CANCELLED,
				"The run was cancelled"));
	}

	@Override
	public UpgradeRun refreshUpgradeRun(
			long companyId, String externalReferenceCode)
		throws PortalException {

		UpgradeRun upgradeRun = upgradeRunPersistence.findByERC_C(
			externalReferenceCode, companyId);

		if (UpgradeRunConstants.isTerminal(upgradeRun.getStatus())) {
			return upgradeRun;
		}

		UpgradeRunnerState upgradeRunnerState = null;

		try {
			upgradeRunnerState = _upgradeRunner.getUpgradeRunnerState(
				externalReferenceCode);
		}
		catch (UpgradeRunnerException upgradeRunnerException) {
			upgradeRunnerState = new UpgradeRunnerState(
				null, null, UpgradeRunConstants.STATUS_FAILED,
				upgradeRunnerException.getMessage());
		}

		return _updateUpgradeRun(upgradeRun, upgradeRunnerState);
	}

	private UpgradeRun _updateUpgradeRun(
		UpgradeRun upgradeRun, UpgradeRunnerState upgradeRunnerState) {

		int status = upgradeRunnerState.getStatus();

		if ((status != upgradeRun.getStatus()) &&
			!UpgradeRunConstants.isValidTransition(
				upgradeRun.getStatus(), status)) {

			return upgradeRun;
		}

		if (upgradeRunnerState.getPullRequestURL() != null) {
			upgradeRun.setPullRequestURL(
				upgradeRunnerState.getPullRequestURL());
		}

		if (upgradeRunnerState.getResultBranch() != null) {
			upgradeRun.setResultBranch(upgradeRunnerState.getResultBranch());
		}

		if (upgradeRunnerState.getWorkspacePath() != null) {
			upgradeRun.setWorkspacePath(upgradeRunnerState.getWorkspacePath());
		}

		upgradeRun.setStatus(status);
		upgradeRun.setStatusMessage(upgradeRunnerState.getStatusMessage());

		if (UpgradeRunConstants.isTerminal(status)) {
			upgradeRun.setEndDate(new Date());
		}

		return upgradeRunPersistence.update(upgradeRun);
	}

	@Reference
	private UpgradeRunner _upgradeRunner;

	@Reference
	private UserLocalService _userLocalService;

}