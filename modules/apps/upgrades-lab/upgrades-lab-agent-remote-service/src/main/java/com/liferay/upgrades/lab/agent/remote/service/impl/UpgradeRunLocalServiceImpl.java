/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.service.impl;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.aop.AopService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.security.key.KeyReferenceUtil;
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
			long userId, String branch, String credentialKeyReference,
			String customerName, String dbTargetType, String dbTargetVersion,
			String licensePath, String nodeVersion, String repositoryURL,
			String searchVersion, String upgradeSourceVersion,
			String upgradeTargetJavaVersion, String upgradeTargetVersion)
		throws PortalException {

		return _addUpgradeRun(
			userId, branch, credentialKeyReference,
			UpgradeRunConstants.PHASE_FIRST, licensePath, repositoryURL, null,
			HashMapBuilder.put(
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
				UpgradeRunSettingsKeys.UPGRADE_SOURCE_VERSION,
				upgradeSourceVersion
			).put(
				UpgradeRunSettingsKeys.UPGRADE_TARGET_JAVA_VERSION,
				upgradeTargetJavaVersion
			).put(
				UpgradeRunSettingsKeys.UPGRADE_TARGET_VERSION,
				upgradeTargetVersion
			).build(),
			null);
	}

	@Override
	public UpgradeRun cancelUpgradeRun(
			String externalReferenceCode, long companyId)
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
			String externalReferenceCode, long companyId)
		throws PortalException {

		UpgradeRun upgradeRun = upgradeRunPersistence.findByERC_C(
			externalReferenceCode, companyId);

		if (UpgradeRunConstants.isTerminal(upgradeRun.getStatus())) {
			return upgradeRun;
		}

		UpgradeRunnerState upgradeRunnerState;

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

	@Override
	public UpgradeRun resumeUpgradeRun(
			long userId, String externalReferenceCode, long companyId,
			int firstPhase, Map<String, String> settings)
		throws PortalException {

		UpgradeRun upgradeRun = upgradeRunPersistence.findByERC_C(
			externalReferenceCode, companyId);

		if (!UpgradeRunConstants.isTerminal(upgradeRun.getStatus())) {
			throw new UpgradeRunStatusException(
				StringBundler.concat(
					"Upgrade run ", externalReferenceCode,
					" has not finished"));
		}

		if (firstPhase == 0) {
			firstPhase = _getFirstPhase(upgradeRun.getResultBranch());
		}

		Map<String, String> resumedSettings = _getSettings(upgradeRun);

		if (settings != null) {
			for (Map.Entry<String, String> entry : settings.entrySet()) {
				if (entry.getValue() == null) {
					resumedSettings.remove(entry.getKey());
				}
				else {
					resumedSettings.put(entry.getKey(), entry.getValue());
				}
			}
		}

		return _addUpgradeRun(
			userId, upgradeRun.getBranch(),
			upgradeRun.getCredentialKeyReference(), firstPhase,
			upgradeRun.getLicensePath(), upgradeRun.getRepositoryURL(),
			upgradeRun.getResultBranch(), resumedSettings,
			upgradeRun.getWorkspacePath());
	}

	private UpgradeRun _addUpgradeRun(
			long userId, String branch, String credentialKeyReference,
			int firstPhase, String licensePath, String repositoryURL,
			String resultBranch, Map<String, String> settings,
			String workspacePath)
		throws PortalException {

		_validate(
			branch, credentialKeyReference, firstPhase, repositoryURL,
			resultBranch, settings);

		User user = _userLocalService.getUser(userId);

		long upgradeRunId = counterLocalService.increment(
			UpgradeRun.class.getName());

		String upgradeTargetVersion = settings.get(
			UpgradeRunSettingsKeys.UPGRADE_TARGET_VERSION);

		UpgradeRunRequest upgradeRunRequest = new UpgradeRunRequest(
			branch, user.getCompanyId(), credentialKeyReference, firstPhase,
			licensePath, repositoryURL, resultBranch, settings, upgradeRunId,
			upgradeTargetVersion, workspacePath);

		UpgradeRun upgradeRun = upgradeRunPersistence.create(upgradeRunId);

		upgradeRun.setCompanyId(user.getCompanyId());
		upgradeRun.setUserId(user.getUserId());
		upgradeRun.setUserName(user.getFullName());
		upgradeRun.setBranch(branch);
		upgradeRun.setCredentialKeyReference(
			upgradeRunRequest.getCredentialKeyReference());
		upgradeRun.setCustomerName(
			settings.get(UpgradeRunSettingsKeys.CUSTOMER_NAME));
		upgradeRun.setDbTargetType(
			settings.get(UpgradeRunSettingsKeys.DB_TARGET_TYPE));
		upgradeRun.setDbTargetVersion(
			settings.get(UpgradeRunSettingsKeys.DB_TARGET_VERSION));
		upgradeRun.setFirstPhase(firstPhase);
		upgradeRun.setLicensePath(upgradeRunRequest.getLicensePath());
		upgradeRun.setNodeVersion(
			settings.get(UpgradeRunSettingsKeys.NODE_VERSION));
		upgradeRun.setRepositoryURL(repositoryURL);
		upgradeRun.setSearchVersion(
			settings.get(UpgradeRunSettingsKeys.SEARCH_VERSION));
		upgradeRun.setUpgradeSourceVersion(
			settings.get(UpgradeRunSettingsKeys.UPGRADE_SOURCE_VERSION));
		upgradeRun.setUpgradeTargetJavaVersion(
			settings.get(UpgradeRunSettingsKeys.UPGRADE_TARGET_JAVA_VERSION));
		upgradeRun.setUpgradeTargetVersion(upgradeTargetVersion);
		upgradeRun.setStatus(UpgradeRunConstants.STATUS_QUEUED);
		upgradeRun.setStatusMessage("The run is queued");

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

	private int _getFirstPhase(String resultBranch) {
		if ((resultBranch == null) ||
			!resultBranch.startsWith(UpgradeRunConstants.BRANCH_PREFIX_PHASE)) {

			return UpgradeRunConstants.PHASE_FIRST;
		}

		int phase = GetterUtil.getInteger(
			resultBranch.substring(
				UpgradeRunConstants.BRANCH_PREFIX_PHASE.length()),
			UpgradeRunConstants.PHASE_FIRST);

		if ((phase < UpgradeRunConstants.PHASE_FIRST) ||
			(phase > UpgradeRunConstants.PHASE_LAST)) {

			return UpgradeRunConstants.PHASE_FIRST;
		}

		return phase;
	}

	private Map<String, String> _getSettings(UpgradeRun upgradeRun) {
		return HashMapBuilder.put(
			UpgradeRunSettingsKeys.CUSTOMER_NAME, upgradeRun.getCustomerName()
		).put(
			UpgradeRunSettingsKeys.DB_TARGET_TYPE, upgradeRun.getDbTargetType()
		).put(
			UpgradeRunSettingsKeys.DB_TARGET_VERSION,
			upgradeRun.getDbTargetVersion()
		).put(
			UpgradeRunSettingsKeys.NODE_VERSION, upgradeRun.getNodeVersion()
		).put(
			UpgradeRunSettingsKeys.SEARCH_VERSION, upgradeRun.getSearchVersion()
		).put(
			UpgradeRunSettingsKeys.UPGRADE_SOURCE_VERSION,
			upgradeRun.getUpgradeSourceVersion()
		).put(
			UpgradeRunSettingsKeys.UPGRADE_TARGET_JAVA_VERSION,
			upgradeRun.getUpgradeTargetJavaVersion()
		).put(
			UpgradeRunSettingsKeys.UPGRADE_TARGET_VERSION,
			upgradeRun.getUpgradeTargetVersion()
		).build();
	}

	private boolean _isValidTransition(int status, int newStatus) {
		if (UpgradeRunConstants.isTerminal(status) || (status == newStatus)) {
			return false;
		}

		if (UpgradeRunConstants.isTerminal(newStatus) || (newStatus > status)) {
			return true;
		}

		return false;
	}

	private UpgradeRun _updateUpgradeRun(
		UpgradeRun upgradeRun, UpgradeRunnerState upgradeRunnerState) {

		int status = upgradeRunnerState.getStatus();

		if ((status != upgradeRun.getStatus()) &&
			!_isValidTransition(upgradeRun.getStatus(), status)) {

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

	private void _validate(
			String branch, String credentialKeyReference, int firstPhase,
			String repositoryURL, String resultBranch,
			Map<String, String> settings)
		throws PortalException {

		if (Validator.isNull(branch)) {
			throw new UpgradeRunBranchException("Branch is null");
		}

		if (branch.startsWith(StringPool.DASH)) {
			throw new UpgradeRunBranchException(
				"Branch " + branch + " must not start with a dash");
		}

		if (Validator.isNotNull(credentialKeyReference) &&
			(KeyReferenceUtil.parseKeyReference(credentialKeyReference) ==
				null)) {

			throw new UpgradeRunCredentialKeyReferenceException(
				"Credential key reference is not a key reference");
		}

		if ((firstPhase < UpgradeRunConstants.PHASE_FIRST) ||
			(firstPhase > UpgradeRunConstants.PHASE_LAST)) {

			throw new UpgradeRunFirstPhaseException(
				StringBundler.concat(
					"First phase ", firstPhase, " is not between ",
					UpgradeRunConstants.PHASE_FIRST, " and ",
					UpgradeRunConstants.PHASE_LAST));
		}

		if (Validator.isNull(repositoryURL)) {
			throw new UpgradeRunRepositoryURLException(
				"Repository URL is null");
		}

		if (repositoryURL.startsWith(StringPool.DASH)) {
			throw new UpgradeRunRepositoryURLException(
				"Repository URL " + repositoryURL +
					" must not start with a dash");
		}

		if (firstPhase > UpgradeRunConstants.PHASE_FIRST) {
			if (Validator.isNull(resultBranch)) {
				throw new UpgradeRunResultBranchException(
					"Result branch is null");
			}

			if (resultBranch.startsWith(StringPool.DASH)) {
				throw new UpgradeRunResultBranchException(
					"Result branch " + resultBranch +
						" must not start with a dash");
			}
		}

		for (String key : UpgradeRunSettingsKeys.REQUIRED) {
			if (Validator.isNull(settings.get(key))) {
				throw new UpgradeRunSettingsException(
					"Setting \"" + key + "\" is null");
			}
		}
	}

	@Reference
	private UpgradeRunner _upgradeRunner;

	@Reference
	private UserLocalService _userLocalService;

}