/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.exception;

import com.liferay.portal.kernel.exception.PortalException;

/**
 * @author Albert Gomes Cabral
 */
public class UpgradeRunRequestException extends PortalException {

	public UpgradeRunRequestException() {
	}

	public UpgradeRunRequestException(String msg) {
		super(msg);
	}

	public UpgradeRunRequestException(String msg, Throwable throwable) {
		super(msg, throwable);
	}

	public UpgradeRunRequestException(Throwable throwable) {
		super(throwable);
	}

}