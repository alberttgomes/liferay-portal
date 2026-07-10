/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.database.migration.service.MigrationError;
import com.liferay.petra.io.unsync.UnsyncBufferedReader;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.DateUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.io.Reader;

import java.math.BigDecimal;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Timestamp;
import java.sql.Types;

import java.text.DateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import javax.sql.DataSource;

/**
 * @author Albert Gomes Cabral
 */
public class TableCopier {

	public TableCopier(
		DataSource sourceDataSource, DataSource targetDataSource) {

		_sourceDataSource = sourceDataSource;
		_targetDataSource = targetDataSource;
	}

	public long copyTable(
			String tableName, Consumer<MigrationError> migrationErrorConsumer)
		throws Exception {

		Map<String, Integer> sourceColumnTypes;
		Map<String, Integer> targetColumnTypes;
		Map<String, String> targetColumnTypeNames;
		List<String> primaryKeyColumnNames;

		try (Connection sourceConnection = _sourceDataSource.getConnection();
			Connection targetConnection = _targetDataSource.getConnection()) {

			sourceColumnTypes = MigrationUtil.getColumnTypes(
				sourceConnection, tableName);
			targetColumnTypes = MigrationUtil.getColumnTypes(
				targetConnection, tableName);
			targetColumnTypeNames = MigrationUtil.getColumnTypeNames(
				targetConnection, tableName);
			primaryKeyColumnNames = MigrationUtil.getPrimaryKeyColumnNames(
				targetConnection, tableName);
		}

		Set<String> targetColumnNames = new TreeSet<>(
			String.CASE_INSENSITIVE_ORDER);

		targetColumnNames.addAll(targetColumnTypes.keySet());

		List<String> columnNames = new ArrayList<>();

		for (String columnName : sourceColumnTypes.keySet()) {
			if (targetColumnNames.contains(columnName)) {
				columnNames.add(columnName);
			}
		}

		if (columnNames.isEmpty()) {
			return 0;
		}

		List<String> valuePlaceholders = new ArrayList<>();

		for (String columnName : columnNames) {
			if (_isOIDColumn(targetColumnTypeNames.get(columnName))) {
				valuePlaceholders.add("lo_from_bytea(0, ?)");
			}
			else {
				valuePlaceholders.add("?");
			}
		}

		String selectSQL = StringBundler.concat(
			"select ", StringUtil.merge(columnNames), " from ", tableName);
		String insertSQL = StringBundler.concat(
			"insert into ", tableName, " (", StringUtil.merge(columnNames),
			") values (", StringUtil.merge(valuePlaceholders), ")");

		long rowCount = 0;

		try (Connection sourceConnection = _sourceDataSource.getConnection();
			Connection targetConnection = _targetDataSource.getConnection();
			PreparedStatement selectPreparedStatement =
				sourceConnection.prepareStatement(selectSQL);
			PreparedStatement insertPreparedStatement =
				AutoBatchPreparedStatementUtil.autoBatch(
					targetConnection, insertSQL)) {

			targetConnection.setAutoCommit(false);

			selectPreparedStatement.setFetchSize(
				_getFetchSize(sourceConnection));

			try (ResultSet resultSet = selectPreparedStatement.executeQuery()) {
				long uncommittedCount = 0;

				while (resultSet.next()) {
					Savepoint savepoint = targetConnection.setSavepoint();

					try {
						for (int i = 0; i < columnNames.size(); i++) {
							String columnName = columnNames.get(i);

							_getAndSetColumn(
								columnName, i + 1, insertPreparedStatement,
								resultSet, sourceColumnTypes.get(columnName),
								targetColumnTypes.get(columnName),
								_isOIDColumn(
									targetColumnTypeNames.get(columnName)));
						}

						insertPreparedStatement.addBatch();

						insertPreparedStatement.executeBatch();

						targetConnection.releaseSavepoint(savepoint);

						rowCount++;

						uncommittedCount++;

						if (uncommittedCount >= _COMMIT_BATCH_SIZE) {
							targetConnection.commit();

							uncommittedCount = 0;
						}
					}
					catch (Exception exception) {
						targetConnection.rollback(savepoint);

						migrationErrorConsumer.accept(
							_toMigrationError(
								tableName, primaryKeyColumnNames,
								targetColumnTypes, resultSet, exception));

						if (_log.isWarnEnabled()) {
							_log.warn(
								StringBundler.concat(
									"Skipped a row in ", tableName, ": ",
									exception.getMessage()));
						}
					}
				}

				targetConnection.commit();
			}
			catch (Exception exception) {
				targetConnection.rollback();

				throw exception;
			}
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				StringBundler.concat(
					"Copied ", rowCount, " rows into ", tableName));
		}

		return rowCount;
	}

	private String _formatValue(String value, int targetType) {
		if (value == null) {
			return "NULL";
		}

		if (_isNumericType(targetType)) {
			return value;
		}

		return StringBundler.concat(
			StringPool.APOSTROPHE,
			StringUtil.replace(value, CharPool.APOSTROPHE, "''"),
			StringPool.APOSTROPHE);
	}

	private void _getAndSetColumn(
			String columnName, int index, PreparedStatement preparedStatement,
			ResultSet resultSet, int sourceType, int targetType,
			boolean oidTarget)
		throws Exception {

		if (oidTarget) {
			byte[] value = null;

			if (sourceType == Types.BLOB) {
				Blob blob = resultSet.getBlob(columnName);

				if (blob != null) {
					value = blob.getBytes(1, (int)blob.length());
				}
			}
			else {
				value = resultSet.getBytes(columnName);
			}

			if (value == null) {
				preparedStatement.setNull(index, Types.BINARY);
			}
			else {
				preparedStatement.setBytes(index, value);
			}

			return;
		}

		String valueString = null;

		if ((sourceType == Types.BIGINT) || (sourceType == Types.NUMERIC)) {
			if ((targetType == Types.DECIMAL) ||
				(targetType == Types.NUMERIC)) {

				BigDecimal value = resultSet.getBigDecimal(columnName);

				if (value == null) {
					preparedStatement.setNull(index, targetType);
				}
				else {
					preparedStatement.setBigDecimal(index, value);
				}

				return;
			}

			long value = resultSet.getLong(columnName);

			if ((value == 0L) && resultSet.wasNull()) {
				preparedStatement.setNull(index, targetType);

				return;
			}

			if ((targetType == Types.BIGINT) || (targetType == Types.NUMERIC)) {
				preparedStatement.setLong(index, value);

				return;
			}

			if ((targetType == Types.BIT) || (targetType == Types.BOOLEAN)) {
				preparedStatement.setBoolean(index, value != 0);

				return;
			}

			valueString = String.valueOf(value);
		}
		else if ((sourceType == Types.BINARY) ||
				 (sourceType == Types.LONGVARBINARY) ||
				 (sourceType == Types.VARBINARY)) {

			byte[] value = resultSet.getBytes(columnName);

			if (value == null) {
				preparedStatement.setNull(index, targetType);

				return;
			}

			preparedStatement.setBytes(index, value);

			return;
		}
		else if (sourceType == Types.BLOB) {
			Blob value = resultSet.getBlob(columnName);

			if (value == null) {
				preparedStatement.setNull(index, targetType);

				return;
			}

			preparedStatement.setBytes(
				index, value.getBytes(1, (int)value.length()));

			return;
		}
		else if ((sourceType == Types.BOOLEAN) || (sourceType == Types.BIT)) {
			boolean value = resultSet.getBoolean(columnName);

			if (!value && resultSet.wasNull()) {
				preparedStatement.setNull(index, targetType);

				return;
			}

			if ((targetType == Types.BOOLEAN) || (targetType == Types.BIT)) {
				preparedStatement.setBoolean(index, value);

				return;
			}

			valueString = value ? "1" : "0";
		}
		else if (sourceType == Types.CLOB) {
			Clob value = resultSet.getClob(columnName);

			if (value == null) {
				preparedStatement.setNull(index, targetType);

				return;
			}

			try (Reader reader = value.getCharacterStream();

				UnsyncBufferedReader unsyncBufferedReader =
					new UnsyncBufferedReader(reader)) {

				StringBundler sb = new StringBundler();

				String line = null;

				while ((line = unsyncBufferedReader.readLine()) != null) {
					if (sb.length() != 0) {
						sb.append(StringPool.NEW_LINE);
					}

					sb.append(line);
				}

				valueString = sb.toString();
			}
		}
		else if (sourceType == Types.DECIMAL) {
			BigDecimal value = resultSet.getBigDecimal(columnName);

			if (value == null) {
				preparedStatement.setNull(index, targetType);

				return;
			}

			if (targetType == Types.NUMERIC) {
				preparedStatement.setBigDecimal(index, value);

				return;
			}

			valueString = value.toString();
		}
		else if ((sourceType == Types.DOUBLE) || (sourceType == Types.FLOAT) ||
				 (sourceType == Types.REAL) ||
				 (sourceType == MigrationUtil.ORACLE_TYPE_BINARY_DOUBLE) ||
				 (sourceType == MigrationUtil.ORACLE_TYPE_BINARY_FLOAT)) {

			double value = resultSet.getDouble(columnName);

			if ((value == 0.0) && resultSet.wasNull()) {
				preparedStatement.setNull(index, targetType);

				return;
			}

			if (targetType == Types.DOUBLE) {
				preparedStatement.setDouble(index, value);

				return;
			}

			valueString = String.valueOf(value);
		}
		else if (sourceType == Types.INTEGER) {
			int value = resultSet.getInt(columnName);

			if ((value == 0) && resultSet.wasNull()) {
				preparedStatement.setNull(index, targetType);

				return;
			}

			if (targetType == Types.INTEGER) {
				preparedStatement.setInt(index, value);

				return;
			}

			valueString = String.valueOf(value);
		}
		else if ((sourceType == Types.CHAR) ||
				 (sourceType == Types.LONGNVARCHAR) ||
				 (sourceType == Types.LONGVARCHAR) ||
				 (sourceType == Types.NCHAR) ||
				 (sourceType == Types.NVARCHAR) ||
				 (sourceType == Types.VARCHAR)) {

			String value = resultSet.getString(columnName);

			if (value == null) {
				preparedStatement.setNull(index, targetType);

				return;
			}

			preparedStatement.setString(index, _getSanitizedString(value));

			return;
		}
		else if ((sourceType == Types.DATE) || (sourceType == Types.TIME) ||
				 (sourceType == Types.TIMESTAMP)) {

			Timestamp value = resultSet.getTimestamp(columnName);

			if (value == null) {
				preparedStatement.setNull(index, targetType);

				return;
			}

			if (targetType == Types.TIMESTAMP) {
				preparedStatement.setTimestamp(index, value);

				return;
			}

			valueString = value.toString();
		}
		else if ((sourceType == Types.TINYINT) ||
				 (sourceType == Types.SMALLINT)) {

			short value = resultSet.getShort(columnName);

			if ((value == 0) && resultSet.wasNull()) {
				preparedStatement.setNull(index, targetType);

				return;
			}

			if ((targetType == Types.BIT) || (targetType == Types.BOOLEAN)) {
				preparedStatement.setBoolean(index, value != 0);

				return;
			}

			valueString = String.valueOf(value);
		}
		else {
			String value = resultSet.getString(columnName);

			if (value == null) {
				preparedStatement.setNull(index, targetType);

				return;
			}

			valueString = value;
		}

		_setColumn(index, preparedStatement, targetType, valueString);
	}

	private int _getFetchSize(Connection connection) throws Exception {
		DatabaseMetaData databaseMetaData = connection.getMetaData();

		String databaseProductName = StringUtil.toLowerCase(
			GetterUtil.getString(databaseMetaData.getDatabaseProductName()));

		if (databaseProductName.contains("mariadb") ||
			databaseProductName.contains("mysql")) {

			return Integer.MIN_VALUE;
		}

		return _FETCH_SIZE;
	}

	private String _getSanitizedString(String value) {
		return StringUtil.removeSubstring(value, StringPool.NULL_CHAR);
	}

	private String _getWhereClause(
		List<String> primaryKeyColumnNames,
		Map<String, Integer> targetColumnTypes, ResultSet resultSet) {

		if (primaryKeyColumnNames.isEmpty()) {
			return StringPool.BLANK;
		}

		StringBundler sb = new StringBundler(
			(primaryKeyColumnNames.size() * 4) - 1);

		for (int i = 0; i < primaryKeyColumnNames.size(); i++) {
			String primaryKeyColumnName = primaryKeyColumnNames.get(i);

			if (i > 0) {
				sb.append(" and ");
			}

			String value = null;

			try {
				value = resultSet.getString(primaryKeyColumnName);
			}
			catch (SQLException sqlException) {
				if (_log.isDebugEnabled()) {
					_log.debug(sqlException);
				}
			}

			sb.append(primaryKeyColumnName);
			sb.append(" = ");
			sb.append(
				_formatValue(
					value,
					GetterUtil.getInteger(
						targetColumnTypes.get(primaryKeyColumnName),
						Types.VARCHAR)));
		}

		return sb.toString();
	}

	private boolean _isNumericType(int targetType) {
		if ((targetType == Types.BIGINT) || (targetType == Types.BIT) ||
			(targetType == Types.BOOLEAN) || (targetType == Types.DECIMAL) ||
			(targetType == Types.DOUBLE) || (targetType == Types.FLOAT) ||
			(targetType == Types.INTEGER) || (targetType == Types.NUMERIC) ||
			(targetType == Types.REAL) || (targetType == Types.SMALLINT) ||
			(targetType == Types.TINYINT)) {

			return true;
		}

		return false;
	}

	private boolean _isOIDColumn(String columnTypeName) {
		return "oid".equalsIgnoreCase(columnTypeName);
	}

	private void _setColumn(
			int index, PreparedStatement preparedStatement, int targetType,
			String value)
		throws Exception {

		if (targetType == Types.BIGINT) {
			preparedStatement.setLong(index, GetterUtil.getLong(value));
		}
		else if ((targetType == Types.BIT) || (targetType == Types.BOOLEAN)) {
			preparedStatement.setBoolean(index, GetterUtil.getBoolean(value));
		}
		else if ((targetType == Types.BINARY) ||
				 (targetType == Types.LONGVARBINARY) ||
				 (targetType == Types.VARBINARY)) {

			preparedStatement.setBytes(index, Base64.decode(value));
		}
		else if ((targetType == Types.CHAR) ||
				 (targetType == Types.LONGNVARCHAR) ||
				 (targetType == Types.LONGVARCHAR) ||
				 (targetType == Types.NVARCHAR) ||
				 (targetType == Types.VARCHAR)) {

			preparedStatement.setString(index, _getSanitizedString(value));
		}
		else if (targetType == Types.DECIMAL) {
			preparedStatement.setBigDecimal(
				index, (BigDecimal)GetterUtil.get(value, BigDecimal.ZERO));
		}
		else if ((targetType == Types.DOUBLE) || (targetType == Types.FLOAT) ||
				 (targetType == Types.REAL)) {

			preparedStatement.setDouble(index, GetterUtil.getDouble(value));
		}
		else if (targetType == Types.INTEGER) {
			preparedStatement.setInt(index, GetterUtil.getInteger(value));
		}
		else if (targetType == Types.NUMERIC) {
			preparedStatement.setBigDecimal(
				index, (BigDecimal)GetterUtil.get(value, BigDecimal.ZERO));
		}
		else if ((targetType == Types.SMALLINT) ||
				 (targetType == Types.TINYINT)) {

			preparedStatement.setShort(index, GetterUtil.getShort(value));
		}
		else if ((targetType == Types.DATE) || (targetType == Types.TIME) ||
				 (targetType == Types.TIMESTAMP)) {

			DateFormat dateFormat = DateUtil.getISOFormat();

			Date date = dateFormat.parse(value);

			preparedStatement.setTimestamp(
				index, new Timestamp(date.getTime()));
		}
		else {
			preparedStatement.setString(index, _getSanitizedString(value));
		}
	}

	private MigrationError _toMigrationError(
		String tableName, List<String> primaryKeyColumnNames,
		Map<String, Integer> targetColumnTypes, ResultSet resultSet,
		Exception exception) {

		String sqlState = StringPool.BLANK;

		if (exception instanceof SQLException sqlException) {
			sqlState = GetterUtil.getString(sqlException.getSQLState());
		}

		String whereClause = _getWhereClause(
			primaryKeyColumnNames, targetColumnTypes, resultSet);

		String rowIdentifier = whereClause;

		String suggestedSQL;

		if (Validator.isNull(whereClause)) {
			rowIdentifier = "(no primary key detected)";

			suggestedSQL = StringBundler.concat(
				"-- No primary key detected. Fill in the WHERE clause to ",
				"target the row.\n", "update ", tableName,
				" set <column> = <value> where <condition>;\n", "delete from ",
				tableName, " where <condition>;");
		}
		else {
			suggestedSQL = StringBundler.concat(
				"update ", tableName, " set <column> = <value> where ",
				whereClause, ";\n", "delete from ", tableName, " where ",
				whereClause, ";");
		}

		return new MigrationErrorImpl(
			tableName, rowIdentifier, sqlState, exception.getMessage(),
			suggestedSQL);
	}

	private static final int _COMMIT_BATCH_SIZE = 1000;

	private static final int _FETCH_SIZE = 2500;

	private static final Log _log = LogFactoryUtil.getLog(TableCopier.class);

	private final DataSource _sourceDataSource;
	private final DataSource _targetDataSource;

}