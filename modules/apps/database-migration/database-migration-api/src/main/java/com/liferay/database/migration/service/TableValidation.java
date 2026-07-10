/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service;

import java.util.List;

/**
 * @author Albert Gomes Cabral
 */
public interface TableValidation {

	public List<ColumnValidation> getColumnValidations();

	public long getSourceRowCount();

	public String getStatus();

	public String getTableName();

	public long getTargetRowCount();

}