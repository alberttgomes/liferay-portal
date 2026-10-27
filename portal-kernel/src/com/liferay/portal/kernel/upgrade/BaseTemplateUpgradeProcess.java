/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.upgrade;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.upgrade.util.TemplateReplacement;
import com.liferay.portal.kernel.upgrade.util.TemplateUpgradeProcessUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Albert Gomes Cabral
 * @author Renato Rego
 */
public abstract class BaseTemplateUpgradeProcess extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		List<TemplateReplacement> templateReplacements =
			_getTemplateReplacements();

		if (templateReplacements.isEmpty()) {
			return;
		}

		for (String[] tableAndColumnNames : getTableAndColumnNames()) {
			DBInspector dbInspector = new DBInspector(connection);

			String tableName = dbInspector.normalizeName(
				tableAndColumnNames[0]);

			if (!dbInspector.hasTable(tableName)) {
				if (_log.isInfoEnabled()) {
					_log.info("Table " + tableName + " does not exist");
				}

				continue;
			}

			String columnName = dbInspector.normalizeName(
				tableAndColumnNames[1]);

			if (!dbInspector.hasColumn(tableName, columnName)) {
				if (_log.isInfoEnabled()) {
					_log.info(
						StringBundler.concat(
							"Table ", tableName, " does not have column ",
							columnName));
				}

				continue;
			}

			String[] primaryKeyColumnNames = getPrimaryKeyColumnNames(
				connection, tableName);

			if (primaryKeyColumnNames.length == 0) {
				throw new Exception(
					"Table " + tableName + " has no primary key");
			}

			Queue<String> modifiedKeys = new ConcurrentLinkedQueue<>();

			String whereClause = null;

			if (tableAndColumnNames.length > 2) {
				whereClause = tableAndColumnNames[2];
			}

			processConcurrently(
				_getSelectSQL(
					columnName, primaryKeyColumnNames, tableName, whereClause),
				_getUpdateSQL(columnName, primaryKeyColumnNames, tableName),
				resultSet -> _getValues(
					columnName, primaryKeyColumnNames, resultSet),
				(values, preparedStatement) -> {
					String modifiedKey = _updateValue(
						columnName, preparedStatement, primaryKeyColumnNames,
						tableName, templateReplacements, values);

					if (modifiedKey == null) {
						return;
					}

					modifiedKeys.add(modifiedKey);
				},
				StringBundler.concat(
					"Unable to upgrade templates in table ", tableName,
					" column ", columnName));

			if (!_log.isInfoEnabled()) {
				continue;
			}

			if (modifiedKeys.isEmpty()) {
				_log.info(
					StringBundler.concat(
						"Table ", tableName, " column ", columnName,
						" was not updated"));

				continue;
			}

			_log.info(
				StringBundler.concat(
					"Table ", tableName, " column ", columnName,
					" was updated for records with primary keys (",
					StringUtil.merge(primaryKeyColumnNames, ", "), "): ",
					StringUtil.merge(modifiedKeys, ", ")));
		}
	}

	protected abstract String[][] getTableAndColumnNames();

	protected boolean isSquareBracketSyntax() {
		return false;
	}

	private String _getKey(String[] primaryKeyColumnNames, Object[] values) {
		StringBundler sb = new StringBundler(
			(primaryKeyColumnNames.length * 2) + 1);

		sb.append("(");

		for (int i = 0; i < primaryKeyColumnNames.length; i++) {
			sb.append(values[i]);
			sb.append(", ");
		}

		sb.setIndex(sb.index() - 1);

		sb.append(")");

		return sb.toString();
	}

	private String _getSelectSQL(
		String columnName, String[] primaryKeyColumnNames, String tableName,
		String whereClause) {

		String selectSQL = StringBundler.concat(
			"select ", StringUtil.merge(primaryKeyColumnNames, ", "), ", ",
			columnName, " from ", tableName, " where ", columnName,
			" is not null");

		if (Validator.isNull(whereClause)) {
			return selectSQL;
		}

		return StringBundler.concat(selectSQL, " and ", whereClause);
	}

	private List<TemplateReplacement> _getTemplateReplacements()
		throws Exception {

		List<TemplateReplacement> templateReplacements = new ArrayList<>();

		Class<?> clazz = getClass();

		String name = "dependencies/template-replacements.json";

		if (clazz.getResource(name) == null) {
			return templateReplacements;
		}

		JSONArray jsonArray = JSONFactoryUtil.createJSONArray(
			StringUtil.read(clazz, name));

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);

			templateReplacements.add(
				new TemplateReplacement(
					jsonObject.getString("contextVariable"),
					jsonObject.getString("deprecatedClassName"),
					jsonObject.getString("from"),
					jsonObject.getBoolean("hasMessage"),
					jsonObject.getString("id"),
					JSONUtil.toStringArray(
						jsonObject.getJSONArray("methodNames")),
					JSONUtil.toStringArray(
						jsonObject.getJSONArray("newAssigns")),
					jsonObject.getString("to")));
		}

		return templateReplacements;
	}

	private String _getUpdateSQL(
		String columnName, String[] primaryKeyColumnNames, String tableName) {

		return StringBundler.concat(
			"update ", tableName, " set ", columnName, " = ? where ",
			StringUtil.merge(primaryKeyColumnNames, " = ? and "), " = ?");
	}

	private Object[] _getValues(
			String columnName, String[] primaryKeyColumnNames,
			ResultSet resultSet)
		throws SQLException {

		Object[] values = new Object[primaryKeyColumnNames.length + 1];

		for (int i = 0; i < primaryKeyColumnNames.length; i++) {
			values[i] = resultSet.getObject(primaryKeyColumnNames[i]);
		}

		values[primaryKeyColumnNames.length] = resultSet.getString(columnName);

		return values;
	}

	private String _updateValue(
			String columnName, PreparedStatement preparedStatement,
			String[] primaryKeyColumnNames, String tableName,
			List<TemplateReplacement> templateReplacements, Object[] values)
		throws SQLException {

		String template = (String)values[values.length - 1];

		if (_log.isWarnEnabled()) {
			List<String> messageIds = TemplateUpgradeProcessUtil.getMessageIds(
				template, templateReplacements);

			if (!messageIds.isEmpty()) {
				_log.warn(
					StringBundler.concat(
						"Table ", tableName, " column ", columnName,
						" record with primary keys ",
						_getKey(primaryKeyColumnNames, values),
						" requires a manual fix for patterns ",
						StringUtil.merge(messageIds, ", ")));
			}
		}

		String newTemplate = TemplateUpgradeProcessUtil.replace(
			isSquareBracketSyntax(), template, templateReplacements);

		if (template.equals(newTemplate)) {
			return null;
		}

		preparedStatement.setString(1, newTemplate);

		for (int i = 0; i < primaryKeyColumnNames.length; i++) {
			preparedStatement.setObject(i + 2, values[i]);
		}

		preparedStatement.addBatch();

		return _getKey(primaryKeyColumnNames, values);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		BaseTemplateUpgradeProcess.class);

}