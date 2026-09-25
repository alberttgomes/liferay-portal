/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.runner;

import com.liferay.upgrades.lab.agent.remote.exception.UpgradeRunnerException;

import org.osgi.annotation.versioning.ProviderType;

/**
 * @author Albert Gomes Cabral
 */
@ProviderType
public interface UpgradeRunner {

	/**
	 * Stops the run and releases whatever the substrate allocated for it.
	 * Cancelling a run that already finished does nothing.
	 */
	public void cancel(String externalReferenceCode)
		throws UpgradeRunnerException;

	public UpgradeRunnerState getUpgradeRunnerState(
			String externalReferenceCode)
		throws UpgradeRunnerException;

	/**
	 * Hands the run to the substrate and returns the identifier the substrate
	 * knows it by, which the portal stores as the run's external reference
	 * code to poll and cancel with.
	 */
	public String submit(UpgradeRunRequest upgradeRunRequest)
		throws UpgradeRunnerException;

}