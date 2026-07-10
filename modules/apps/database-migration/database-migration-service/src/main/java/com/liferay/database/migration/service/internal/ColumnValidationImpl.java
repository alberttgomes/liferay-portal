/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.database.migration.service.ColumnValidation;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Albert Gomes Cabral
 */
public class ColumnValidationImpl implements ColumnValidation, Serializable {

	public static final String STATUS_ADDED = "ADDED";

	public static final String STATUS_CUSTOM = "CUSTOM";

	public static final String STATUS_NOT_MIGRATED = "NOT_MIGRATED";

	public static final String STATUS_VALID = "VALID";

	public ColumnValidationImpl(
		String columnName, String sourceType, String targetType,
		String status) {

		_columnName = columnName;
		_sourceType = sourceType;
		_targetType = targetType;
		_status = status;
	}

	@Override
	public String getColumnName() {
		return _columnName;
	}

	@Override
	public String getSourceType() {
		return _sourceType;
	}

	@Override
	public String getStatus() {
		return _status;
	}

	@Override
	public String getTargetType() {
		return _targetType;
	}

	@Serial
	private static final long serialVersionUID = 1L;

	private final String _columnName;
	private final String _sourceType;
	private final String _status;
	private final String _targetType;

}