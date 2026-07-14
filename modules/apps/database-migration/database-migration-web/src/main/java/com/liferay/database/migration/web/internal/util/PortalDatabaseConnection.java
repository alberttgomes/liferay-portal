/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.web.internal.util;

import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.Validator;

/**
 * @author Albert Gomes Cabral
 */
public class PortalDatabaseConnection {

	public static String getJDBCURL() {
		return PropsValues.JDBC_DEFAULT_URL;
	}

	public static String getPassword() {
		return PropsValues.JDBC_DEFAULT_PASSWORD;
	}

	public static String getUserName() {
		return PropsValues.JDBC_DEFAULT_USERNAME;
	}

	public static boolean isAvailable() {
		if (Validator.isNotNull(PropsValues.JDBC_DEFAULT_JNDI_NAME) ||
			Validator.isNull(PropsValues.JDBC_DEFAULT_URL)) {

			return false;
		}

		return true;
	}

}