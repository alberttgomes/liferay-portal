/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.exception;

import com.liferay.portal.kernel.exception.PortalException;

/**
 * @author Albert Gomes Cabral
 */
public class UpgradeRunRepositoryURLException extends PortalException {

	public UpgradeRunRepositoryURLException() {
	}

	public UpgradeRunRepositoryURLException(String msg) {
		super(msg);
	}

	public UpgradeRunRepositoryURLException(String msg, Throwable throwable) {
		super(msg, throwable);
	}

	public UpgradeRunRepositoryURLException(Throwable throwable) {
		super(throwable);
	}

}