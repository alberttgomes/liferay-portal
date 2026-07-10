/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * @author Albert Gomes Cabral
 */
public class ObjectSchemaProvider {

	public ObjectSchemaProvider(Connection connection) {
		_index(connection);
	}

	public Set<String> getColumnNames(String tableName) {
		return _columnNames.getOrDefault(tableName, Collections.emptySet());
	}

	public boolean isObjectTable(String tableName) {
		if (_tableNames.contains(tableName)) {
			return true;
		}

		return _objectTableNamePattern.matcher(
			StringUtil.toLowerCase(tableName)
		).matches();
	}

	private void _addColumnName(String tableName, String columnName) {
		if (Validator.isNull(tableName) || Validator.isNull(columnName)) {
			return;
		}

		Set<String> columnNames = _columnNames.computeIfAbsent(
			tableName, key -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));

		columnNames.add(columnName);
	}

	private String _getExtensionDBTableName(
		String dbTableName, boolean unmodifiableSystemObject, long companyId) {

		if (!unmodifiableSystemObject) {
			return dbTableName + "_x";
		}

		if (dbTableName.endsWith("_")) {
			return dbTableName + "x_" + companyId;
		}

		return dbTableName + "_x_" + companyId;
	}

	private void _index(Connection connection) {
		try {
			_indexObjectDefinitions(connection);
			_indexObjectFields(connection);
		}
		catch (Exception exception) {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Unable to read Liferay Objects metadata; using table " +
						"name detection",
					exception);
			}
		}
	}

	private void _indexObjectDefinitions(Connection connection)
		throws Exception {

		try (Statement statement = connection.createStatement();

			ResultSet resultSet = statement.executeQuery(
				"select companyId, dbTableName, modifiable, system_, " +
					"pkObjectFieldDBColumnName from ObjectDefinition")) {

			while (resultSet.next()) {
				String dbTableName = resultSet.getString("dbTableName");

				if (Validator.isNull(dbTableName)) {
					continue;
				}

				long companyId = resultSet.getLong("companyId");

				boolean unmodifiableSystemObject = false;

				if (resultSet.getBoolean("system_") &&
					!resultSet.getBoolean("modifiable")) {

					unmodifiableSystemObject = true;
				}

				String extensionDBTableName = _getExtensionDBTableName(
					dbTableName, unmodifiableSystemObject, companyId);

				_tableNames.add(dbTableName);
				_tableNames.add(dbTableName + "_l");
				_tableNames.add(extensionDBTableName);

				String pkObjectFieldDBColumnName = resultSet.getString(
					"pkObjectFieldDBColumnName");

				_addColumnName(dbTableName, pkObjectFieldDBColumnName);
				_addColumnName(extensionDBTableName, pkObjectFieldDBColumnName);
			}
		}
	}

	private void _indexObjectFields(Connection connection) throws Exception {
		try (Statement statement = connection.createStatement();

			ResultSet resultSet = statement.executeQuery(
				"select dbTableName, dbColumnName from ObjectField")) {

			while (resultSet.next()) {
				_addColumnName(
					resultSet.getString("dbTableName"),
					resultSet.getString("dbColumnName"));
			}
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ObjectSchemaProvider.class);

	private static final Pattern _objectTableNamePattern = Pattern.compile(
		"^o_[0-9]+_.*|^l_[0-9]+_.*|.*_x_[0-9]+$");

	private final Map<String, Set<String>> _columnNames = new TreeMap<>(
		String.CASE_INSENSITIVE_ORDER);
	private final Set<String> _tableNames = new TreeSet<>(
		String.CASE_INSENSITIVE_ORDER);

}