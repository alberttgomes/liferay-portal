/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service;

/**
 * @author Albert Gomes Cabral
 */
public interface ColumnValidation {

	public String getColumnName();

	public String getSourceType();

	public String getStatus();

	public String getTargetType();

}