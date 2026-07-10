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

import java.util.Map;
import java.util.TreeMap;

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

	public String getCreateTableSQL(String tableName) {
		return _createTableSQLs.get(tableName);
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

	private static final String _CREATE_TABLE = "create table ";

	private static final Log _log = LogFactoryUtil.getLog(
		PortableSchemaProvider.class);

	private final Map<String, String> _createTableSQLs = new TreeMap<>(
		String.CASE_INSENSITIVE_ORDER);

}