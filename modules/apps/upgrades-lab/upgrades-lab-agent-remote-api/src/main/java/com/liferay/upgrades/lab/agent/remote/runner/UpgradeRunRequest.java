/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.runner;

import com.liferay.portal.kernel.util.Validator;
import com.liferay.upgrades.lab.agent.remote.constants.UpgradeRunConstants;

import java.util.Map;

/**
 * @author Albert Gomes Cabral
 */
public class UpgradeRunRequest {

	public UpgradeRunRequest(
		String branch, long companyId, String credentialKeyReference,
		int firstPhase, String licensePath, String repositoryURL,
		String resultBranch, Map<String, String> settings, long upgradeRunId,
		String upgradeTargetVersion, String workspacePath) {

		_branch = branch;
		_companyId = companyId;

		if (Validator.isNull(credentialKeyReference)) {
			_credentialKeyReference = null;
		}
		else {
			_credentialKeyReference = credentialKeyReference;
		}

		_firstPhase = firstPhase;

		if (Validator.isNull(licensePath)) {
			_licensePath = null;
		}
		else {
			_licensePath = licensePath;
		}

		_repositoryURL = repositoryURL;

		if (Validator.isNull(resultBranch)) {
			_resultBranch = null;
		}
		else {
			_resultBranch = resultBranch;
		}

		_settings = Map.copyOf(settings);
		_upgradeRunId = upgradeRunId;
		_upgradeTargetVersion = upgradeTargetVersion;

		if (Validator.isNull(workspacePath)) {
			_workspacePath = null;
		}
		else {
			_workspacePath = workspacePath;
		}
	}

	/**
	 * @param credentialKeyReference the key reference of the private key the
	 *        runner reaches the repository with, or <code>null</code> when the
	 *        repository needs no credential
	 * @param licensePath the path, on the runner's host, of the DXP license
	 *        file or of a folder holding it, which the agent places in the
	 *        workspace, or <code>null</code> when the runner falls back to its
	 *        configured license path
	 */
	public UpgradeRunRequest(
		String branch, long companyId, String credentialKeyReference,
		String licensePath, String repositoryURL, Map<String, String> settings,
		long upgradeRunId, String upgradeTargetVersion) {

		this(
			branch, companyId, credentialKeyReference,
			UpgradeRunConstants.PHASE_FIRST, licensePath, repositoryURL, null,
			settings, upgradeRunId, upgradeTargetVersion, null);
	}

	public String getBranch() {
		return _branch;
	}

	public long getCompanyId() {
		return _companyId;
	}

	public String getCredentialKeyReference() {
		return _credentialKeyReference;
	}

	public int getFirstPhase() {
		return _firstPhase;
	}

	public String getLicensePath() {
		return _licensePath;
	}

	public String getRepositoryURL() {
		return _repositoryURL;
	}

	public String getResultBranch() {
		return _resultBranch;
	}

	public Map<String, String> getSettings() {
		return _settings;
	}

	public long getUpgradeRunId() {
		return _upgradeRunId;
	}

	public String getUpgradeTargetVersion() {
		return _upgradeTargetVersion;
	}

	public String getWorkspacePath() {
		return _workspacePath;
	}

	private final String _branch;
	private final long _companyId;
	private final String _credentialKeyReference;
	private final int _firstPhase;
	private final String _licensePath;
	private final String _repositoryURL;
	private final String _resultBranch;
	private final Map<String, String> _settings;
	private final long _upgradeRunId;
	private final String _upgradeTargetVersion;
	private final String _workspacePath;

}