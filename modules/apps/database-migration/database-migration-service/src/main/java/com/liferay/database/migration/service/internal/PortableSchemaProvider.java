/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.URLUtil;

import java.net.URL;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @author Albert Gomes Cabral
 */
public class PortableSchemaProvider {

	public PortableSchemaProvider(BundleContext bundleContext) {
		_indexCorePortalTables();

		for (Bundle bundle : bundleContext.getBundles()) {
			URL url = bundle.getEntry("/META-INF/sql/tables.sql");

			if (url == null) {
				continue;
			}

			try {
				_index(URLUtil.toString(url));
			}
			catch (Exception exception) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						"Unable to read tables.sql from " +
							bundle.getSymbolicName(),
						exception);
				}
			}
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				"Indexed " + _createTableSQLs.size() +
					" portable Liferay table definitions");
		}
	}

	public Set<String> getColumnNames(String tableName) {
		return _columnNames.getOrDefault(tableName, Collections.emptySet());
	}

	public String getCreateTableSQL(String tableName) {
		return _createTableSQLs.get(tableName);
	}

	public boolean hasTable(String tableName) {
		return _createTableSQLs.containsKey(tableName);
	}

	private void _addColumnName(
		Set<String> columnNames, String columnDefinition) {

		String trimmedColumnDefinition = columnDefinition.trim();

		if (trimmedColumnDefinition.isEmpty()) {
			return;
		}

		String lowerCaseColumnDefinition = StringUtil.toLowerCase(
			trimmedColumnDefinition);

		if (lowerCaseColumnDefinition.startsWith("primary key") ||
			lowerCaseColumnDefinition.startsWith("unique")) {

			return;
		}

		int index = trimmedColumnDefinition.indexOf(CharPool.SPACE);

		if (index == -1) {
			columnNames.add(trimmedColumnDefinition);
		}
		else {
			columnNames.add(trimmedColumnDefinition.substring(0, index));
		}
	}

	private void _index(String tablesSQL) {
		for (String statement : StringUtil.split(tablesSQL, ';')) {
			statement = statement.trim();

			String lowerCaseStatement = StringUtil.toLowerCase(statement);

			if (!lowerCaseStatement.startsWith(_CREATE_TABLE)) {
				continue;
			}

			int index = statement.indexOf(CharPool.OPEN_PARENTHESIS);

			if (index == -1) {
				continue;
			}

			String tableName = StringUtil.trim(
				statement.substring(_CREATE_TABLE.length(), index));

			_createTableSQLs.put(
				tableName, statement.concat(StringPool.SEMICOLON));
			_columnNames.put(tableName, _parseColumnNames(statement));
		}
	}

	private void _indexCorePortalTables() {
		ClassLoader classLoader = PortalClassLoaderUtil.getClassLoader();

		URL url = classLoader.getResource(
			"com/liferay/portal/tools/sql/dependencies/portal-tables.sql");

		if (url == null) {
			return;
		}

		try {
			_index(URLUtil.toString(url));
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to read the core portal tables.sql", exception);
			}
		}
	}

	private Set<String> _parseColumnNames(String statement) {
		Set<String> columnNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

		int open = statement.indexOf(CharPool.OPEN_PARENTHESIS);
		int close = statement.lastIndexOf(CharPool.CLOSE_PARENTHESIS);

		if ((open == -1) || (close <= open)) {
			return columnNames;
		}

		String body = statement.substring(open + 1, close);

		int depth = 0;
		int start = 0;

		for (int i = 0; i < body.length(); i++) {
			char character = body.charAt(i);

			if (character == CharPool.OPEN_PARENTHESIS) {
				depth++;
			}
			else if (character == CharPool.CLOSE_PARENTHESIS) {
				depth--;
			}
			else if ((character == CharPool.COMMA) && (depth == 0)) {
				_addColumnName(columnNames, body.substring(start, i));

				start = i + 1;
			}
		}

		_addColumnName(columnNames, body.substring(start));

		return columnNames;
	}

	private static final String _CREATE_TABLE = "create table ";

	private static final Log _log = LogFactoryUtil.getLog(
		PortableSchemaProvider.class);

	private final Map<String, Set<String>> _columnNames = new TreeMap<>(
		String.CASE_INSENSITIVE_ORDER);
	private final Map<String, String> _createTableSQLs = new TreeMap<>(
		String.CASE_INSENSITIVE_ORDER);

}