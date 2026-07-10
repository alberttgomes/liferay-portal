/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.database.migration.service.ColumnValidation;
import com.liferay.database.migration.service.TableValidation;

import java.io.Serial;
import java.io.Serializable;

import java.util.List;

/**
 * @author Albert Gomes Cabral
 */
public class TableValidationImpl implements Serializable, TableValidation {

	public static final String STATUS_CUSTOM_TABLE = "CUSTOM_TABLE";

	public static final String STATUS_HAS_CUSTOM_COLUMNS = "HAS_CUSTOM_COLUMNS";

	public static final String STATUS_INCOMPLETE = "INCOMPLETE";

	public static final String STATUS_NOT_MIGRATED = "NOT_MIGRATED";

	public static final String STATUS_OBJECT = "OBJECT";

	public static final String STATUS_ROW_COUNT_MISMATCH = "ROW_COUNT_MISMATCH";

	public static final String STATUS_TARGET_ONLY = "TARGET_ONLY";

	public static final String STATUS_VALID = "VALID";

	public TableValidationImpl(
		String tableName, String status, long sourceRowCount,
		long targetRowCount, List<ColumnValidation> columnValidations) {

		_tableName = tableName;
		_status = status;
		_sourceRowCount = sourceRowCount;
		_targetRowCount = targetRowCount;
		_columnValidations = columnValidations;
	}

	@Override
	public List<ColumnValidation> getColumnValidations() {
		return _columnValidations;
	}

	@Override
	public long getSourceRowCount() {
		return _sourceRowCount;
	}

	@Override
	public String getStatus() {
		return _status;
	}

	@Override
	public String getTableName() {
		return _tableName;
	}

	@Override
	public long getTargetRowCount() {
		return _targetRowCount;
	}

	@Serial
	private static final long serialVersionUID = 1L;

	private final List<ColumnValidation> _columnValidations;
	private final long _sourceRowCount;
	private final String _status;
	private final String _tableName;
	private final long _targetRowCount;

}