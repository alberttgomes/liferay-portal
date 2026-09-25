/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.runner;

import com.liferay.upgrades.lab.agent.remote.constants.UpgradeRunConstants;

/**
 * A runner's view of one run at a moment in time. The result branch and the
 * pull request URL are null until the run reaches
 * {@link UpgradeRunConstants#STATUS_PUBLISHING}.
 *
 * @author Albert Gomes Cabral
 */
public class UpgradeRunnerState {

	public UpgradeRunnerState(
		String pullRequestURL, String resultBranch, int status,
		String statusMessage) {

		this(pullRequestURL, resultBranch, status, statusMessage, null);
	}

	public UpgradeRunnerState(
		String pullRequestURL, String resultBranch, int status,
		String statusMessage, String workspacePath) {

		if (!UpgradeRunConstants.isValidStatus(status)) {
			throw new IllegalArgumentException(
				"Illegal upgrade run status value " + status);
		}

		_pullRequestURL = pullRequestURL;
		_resultBranch = resultBranch;
		_status = status;
		_statusMessage = statusMessage;
		_workspacePath = workspacePath;
	}

	public String getPullRequestURL() {
		return _pullRequestURL;
	}

	public String getResultBranch() {
		return _resultBranch;
	}

	public int getStatus() {
		return _status;
	}

	public String getStatusMessage() {
		return _statusMessage;
	}

	public String getWorkspacePath() {
		return _workspacePath;
	}

	private final String _pullRequestURL;
	private final String _resultBranch;
	private final int _status;
	private final String _statusMessage;
	private final String _workspacePath;

}