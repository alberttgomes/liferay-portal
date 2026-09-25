/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.runner;

import com.liferay.portal.kernel.util.Validator;
import com.liferay.upgrades.lab.agent.remote.constants.UpgradeRunSettingsKeys;

import java.util.Map;

/**
 * @author Albert Gomes Cabral
 */
public class UpgradeRunRequest {

	/**
	 * @param credentialKeyReference the key reference of the private key the
	 *        runner reaches the repository with, or <code>null</code> when the
	 *        repository needs no credential
	 */
	public UpgradeRunRequest(
		String branch, long companyId, String credentialKeyReference,
		String repositoryURL, Map<String, String> settings,
		String targetRelease, long upgradeRunId) {

		if (Validator.isNull(branch)) {
			throw new IllegalArgumentException("Branch is null");
		}

		if (Validator.isNull(repositoryURL)) {
			throw new IllegalArgumentException("Repository URL is null");
		}

		if (Validator.isNull(targetRelease)) {
			throw new IllegalArgumentException("Target release is null");
		}

		if (settings == null) {
			throw new IllegalArgumentException("Settings are null");
		}

		for (String key : UpgradeRunSettingsKeys.REQUIRED) {
			if (Validator.isNull(settings.get(key))) {
				throw new IllegalArgumentException(
					"Setting \"" + key + "\" is null");
			}
		}

		_branch = branch;
		_companyId = companyId;

		if (Validator.isNull(credentialKeyReference)) {
			_credentialKeyReference = null;
		}
		else {
			_credentialKeyReference = credentialKeyReference;
		}

		_repositoryURL = repositoryURL;
		_settings = Map.copyOf(settings);
		_targetRelease = targetRelease;
		_upgradeRunId = upgradeRunId;
	}

	public String getBranch() {
		return _branch;
	}

	public long getCompanyId() {
		return _companyId;
	}

	/**
	 * Returns the key reference of the private key the runner reaches the
	 * repository with, or <code>null</code> when the repository needs no
	 * credential.
	 */
	public String getCredentialKeyReference() {
		return _credentialKeyReference;
	}

	public String getRepositoryURL() {
		return _repositoryURL;
	}

	/**
	 * Returns the workspace settings the runner writes into the workspace
	 * before invoking the agent, keyed by {@link UpgradeRunSettingsKeys}.
	 */
	public Map<String, String> getSettings() {
		return _settings;
	}

	public String getTargetRelease() {
		return _targetRelease;
	}

	public long getUpgradeRunId() {
		return _upgradeRunId;
	}

	private final String _branch;
	private final long _companyId;
	private final String _credentialKeyReference;
	private final String _repositoryURL;
	private final Map<String, String> _settings;
	private final String _targetRelease;
	private final long _upgradeRunId;

}