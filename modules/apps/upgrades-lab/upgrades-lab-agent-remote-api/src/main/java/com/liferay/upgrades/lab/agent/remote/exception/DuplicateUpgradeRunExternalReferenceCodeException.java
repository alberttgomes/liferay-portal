/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.exception;

import com.liferay.portal.kernel.exception.DuplicateExternalReferenceCodeException;

/**
 * @author Albert Gomes Cabral
 */
public class DuplicateUpgradeRunExternalReferenceCodeException
	extends DuplicateExternalReferenceCodeException {

	public DuplicateUpgradeRunExternalReferenceCodeException() {
	}

	public DuplicateUpgradeRunExternalReferenceCodeException(String msg) {
		super(msg);
	}

	public DuplicateUpgradeRunExternalReferenceCodeException(
		String msg, Throwable throwable) {

		super(msg, throwable);
	}

	public DuplicateUpgradeRunExternalReferenceCodeException(
		Throwable throwable) {

		super(throwable);
	}

}