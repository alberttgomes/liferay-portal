/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.database.migration.service.ColumnValidation;
import com.liferay.database.migration.service.MigrationError;
import com.liferay.database.migration.service.MigrationStatus;
import com.liferay.database.migration.service.TableValidation;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;

import java.sql.Connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

/**
 * @author Albert Gomes Cabral
 */
public class DatabaseMigrator {

	public MigrationStatus getMigrationStatus() {
		MigrationStatus migrationStatus = _migrationStatus.get();

		if (migrationStatus == null) {
			return new MigrationStatusImpl(
				MigrationStatus.PHASE_IDLE, "No migration in progress");
		}

		return migrationStatus;
	}

	public boolean isMigrationRunning() {
		MigrationStatus migrationStatus = _migrationStatus.get();

		if (migrationStatus == null) {
			return false;
		}

		int phase = migrationStatus.getPhase();

		if ((phase != MigrationStatus.PHASE_IDLE) &&
			(phase != MigrationStatus.PHASE_COMPLETED) &&
			(phase != MigrationStatus.PHASE_ERROR)) {

			return true;
		}

		return false;
	}

	public void migrate(
			String sourceJDBCURL, String sourceUserName, String sourcePassword,
			String targetJDBCURL, String targetUserName, String targetPassword,
			long companyId, long userId, String migrationName,
			PortableSchemaProvider portableSchemaProvider)
		throws Exception {

		MigrationStatusImpl migrationStatusImpl = new MigrationStatusImpl(
			MigrationStatus.PHASE_DISCOVERY, "Connecting to databases");

		_migrationStatus.set(migrationStatusImpl);

		DataSource sourceDataSource = null;
		DataSource targetDataSource = null;

		long migrationRunObjectEntryId = _migrationRunRecorder.record(
			companyId, userId, migrationName, sourceJDBCURL, targetJDBCURL,
			migrationStatusImpl);

		try {
			sourceDataSource = MigrationDataSourceFactory.initDataSource(
				sourceJDBCURL, sourceUserName, sourcePassword);
			targetDataSource = MigrationDataSourceFactory.initDataSource(
				targetJDBCURL, targetUserName, targetPassword);

			List<String> tableNames = _discoverTables(
				sourceDataSource, migrationStatusImpl);

			_createSchema(
				sourceDataSource, targetDataSource, portableSchemaProvider,
				migrationStatusImpl);

			_copyTables(
				sourceDataSource, targetDataSource, tableNames,
				migrationStatusImpl);

			_createIndexes(
				sourceDataSource, targetDataSource, tableNames,
				migrationStatusImpl);

			_buildSchemaValidation(
				sourceDataSource, targetDataSource, tableNames,
				portableSchemaProvider, migrationStatusImpl);

			migrationStatusImpl.setPhase(MigrationStatus.PHASE_COMPLETED);

			List<MigrationError> migrationErrors =
				migrationStatusImpl.getMigrationErrors();

			String message = "Migrated " + tableNames.size() + " tables";

			if (!migrationErrors.isEmpty()) {
				message += ", skipped " + migrationErrors.size() + " rows";
			}

			migrationStatusImpl.setMessage(message);
			migrationStatusImpl.setProgress(100);

			_migrationRunRecorder.updateRecord(
				companyId, userId, migrationName, sourceJDBCURL, targetJDBCURL,
				targetDataSource, migrationRunObjectEntryId,
				migrationStatusImpl);
		}
		catch (Exception exception) {
			migrationStatusImpl.setPhase(MigrationStatus.PHASE_ERROR);
			migrationStatusImpl.setMessage(exception.getMessage());

			_migrationRunRecorder.updateRecord(
				companyId, userId, migrationName, sourceJDBCURL, targetJDBCURL,
				targetDataSource, migrationRunObjectEntryId,
				migrationStatusImpl);

			_log.error("Database migration failed", exception);

			throw exception;
		}
		finally {
			MigrationDataSourceFactory.destroy(sourceDataSource);
			MigrationDataSourceFactory.destroy(targetDataSource);
		}
	}

	private void _buildSchemaValidation(
		DataSource sourceDataSource, DataSource targetDataSource,
		List<String> tableNames, PortableSchemaProvider portableSchemaProvider,
		MigrationStatusImpl migrationStatusImpl) {

		migrationStatusImpl.setMessage("Validating the migrated schema");

		try (Connection sourceConnection = sourceDataSource.getConnection();
			Connection targetConnection = targetDataSource.getConnection()) {

			Set<String> sourceTableNames = new TreeSet<>(
				String.CASE_INSENSITIVE_ORDER);

			sourceTableNames.addAll(tableNames);

			Set<String> targetTableNames = MigrationUtil.getTableNames(
				targetConnection);

			Map<String, Long> skippedRowCounts = new TreeMap<>(
				String.CASE_INSENSITIVE_ORDER);

			for (MigrationError migrationError :
					migrationStatusImpl.getMigrationErrors()) {

				skippedRowCounts.merge(
					migrationError.getTableName(), 1L, Long::sum);
			}

			Map<String, Long> tableRowCounts =
				migrationStatusImpl.getTableRowCounts();

			Set<String> allTableNames = new TreeSet<>(
				String.CASE_INSENSITIVE_ORDER);

			allTableNames.addAll(sourceTableNames);
			allTableNames.addAll(targetTableNames);

			List<TableValidation> tableValidations = new ArrayList<>();

			for (String tableName : allTableNames) {
				boolean onSource = sourceTableNames.contains(tableName);
				boolean onTarget = targetTableNames.contains(tableName);

				Map<String, String> sourceColumnTypeNames =
					Collections.emptyMap();

				if (onSource) {
					sourceColumnTypeNames = MigrationUtil.getColumnTypeNames(
						sourceConnection, tableName);
				}

				Map<String, String> targetColumnTypeNames =
					Collections.emptyMap();

				if (onTarget) {
					targetColumnTypeNames = MigrationUtil.getColumnTypeNames(
						targetConnection, tableName);
				}

				Set<String> columnNames = new TreeSet<>(
					String.CASE_INSENSITIVE_ORDER);

				columnNames.addAll(sourceColumnTypeNames.keySet());
				columnNames.addAll(targetColumnTypeNames.keySet());

				boolean standardTable = portableSchemaProvider.hasTable(
					tableName);

				ObjectSchemaProvider objectSchemaProvider =
					new ObjectSchemaProvider(sourceConnection);

				boolean objectTable = objectSchemaProvider.isObjectTable(
					tableName);

				Set<String> knownColumnNames = Collections.emptySet();

				if (standardTable) {
					knownColumnNames = portableSchemaProvider.getColumnNames(
						tableName);
				}
				else if (objectTable) {
					knownColumnNames = objectSchemaProvider.getColumnNames(
						tableName);
				}

				boolean baselineKnown = false;

				if (standardTable || objectTable) {
					baselineKnown = true;
				}

				List<ColumnValidation> columnValidations = new ArrayList<>();

				for (String columnName : columnNames) {
					boolean onSourceColumn = sourceColumnTypeNames.containsKey(
						columnName);
					boolean onTargetColumn = targetColumnTypeNames.containsKey(
						columnName);

					String status = ColumnValidationImpl.STATUS_VALID;

					if (!onTargetColumn) {
						status = ColumnValidationImpl.STATUS_NOT_MIGRATED;
					}
					else if (!onSourceColumn) {
						status = ColumnValidationImpl.STATUS_ADDED;
					}
					else if (baselineKnown &&
							 !knownColumnNames.contains(columnName)) {

						status = ColumnValidationImpl.STATUS_CUSTOM;
					}

					columnValidations.add(
						new ColumnValidationImpl(
							columnName, sourceColumnTypeNames.get(columnName),
							targetColumnTypeNames.get(columnName), status));
				}

				long targetRowCount = GetterUtil.getLong(
					tableRowCounts.get(tableName));

				long sourceRowCount = 0;

				if (onSource) {
					sourceRowCount =
						targetRowCount +
							GetterUtil.getLong(skippedRowCounts.get(tableName));
				}

				tableValidations.add(
					new TableValidationImpl(
						tableName,
						_getTableStatus(
							onSource, onTarget, sourceRowCount, targetRowCount,
							columnValidations, standardTable, objectTable),
						sourceRowCount, targetRowCount, columnValidations));
			}

			migrationStatusImpl.setTableValidations(tableValidations);
		}
		catch (Exception exception) {
			_log.error("Unable to build schema validation", exception);
		}
	}

	private void _copyTables(
			DataSource sourceDataSource, DataSource targetDataSource,
			List<String> tableNames, MigrationStatusImpl migrationStatusImpl)
		throws Exception {

		migrationStatusImpl.setPhase(MigrationStatus.PHASE_DATA_LOAD);

		TableCopier tableCopier = new TableCopier(
			sourceDataSource, targetDataSource);

		int copied = 0;

		for (String tableName : tableNames) {
			migrationStatusImpl.setMessage("Copying " + tableName);

			long rowCount = tableCopier.copyTable(
				tableName, migrationStatusImpl::addMigrationError);

			migrationStatusImpl.addTableRowCount(tableName, rowCount);

			copied++;

			migrationStatusImpl.setProgress((copied * 100) / tableNames.size());
		}
	}

	private void _createIndexes(
			DataSource sourceDataSource, DataSource targetDataSource,
			List<String> tableNames, MigrationStatusImpl migrationStatusImpl)
		throws Exception {

		migrationStatusImpl.setPhase(MigrationStatus.PHASE_INDEX_CREATION);
		migrationStatusImpl.setProgress(0);

		SchemaCreator schemaCreator = new SchemaCreator(
			sourceDataSource, targetDataSource, null);

		AtomicInteger completed = new AtomicInteger();

		int total = Math.max(1, tableNames.size());

		List<String> createdIndexNames = schemaCreator.createIndexes(
			tableNames,
			tableName -> {
				migrationStatusImpl.setMessage(
					"Creating indexes for " + tableName);
				migrationStatusImpl.setProgress(
					(completed.incrementAndGet() * 100) / total);
			});

		String message = "Created " + createdIndexNames.size() + " indexes";

		migrationStatusImpl.setMessage(message);

		if (_log.isInfoEnabled()) {
			_log.info(message);
		}
	}

	private void _createSchema(
			DataSource sourceDataSource, DataSource targetDataSource,
			PortableSchemaProvider portableSchemaProvider,
			MigrationStatusImpl migrationStatusImpl)
		throws Exception {

		migrationStatusImpl.setPhase(MigrationStatus.PHASE_SCHEMA_CREATION);
		migrationStatusImpl.setMessage("Creating target schema");

		SchemaCreator schemaCreator = new SchemaCreator(
			sourceDataSource, targetDataSource, portableSchemaProvider);

		List<String> createdTableNames = schemaCreator.create();

		String message = "Created " + createdTableNames.size() + " tables";

		migrationStatusImpl.setMessage(message);

		if (_log.isInfoEnabled()) {
			_log.info(message);
		}
	}

	private List<String> _discoverTables(
			DataSource sourceDataSource,
			MigrationStatusImpl migrationStatusImpl)
		throws Exception {

		try (Connection connection = sourceDataSource.getConnection()) {
			Set<String> tableNames = MigrationUtil.getTableNames(connection);

			migrationStatusImpl.setMessage(
				"Found " + tableNames.size() + " source tables");

			if (_log.isInfoEnabled()) {
				_log.info("Found " + tableNames.size() + " source tables");
			}

			return new ArrayList<>(tableNames);
		}
	}

	private String _getTableStatus(
		boolean onSource, boolean onTarget, long sourceRowCount,
		long targetRowCount, List<ColumnValidation> columnValidations,
		boolean standardTable, boolean objectTable) {

		if (!onTarget) {
			return TableValidationImpl.STATUS_NOT_MIGRATED;
		}

		for (ColumnValidation columnValidation : columnValidations) {
			if (ColumnValidationImpl.STATUS_NOT_MIGRATED.equals(
					columnValidation.getStatus())) {

				return TableValidationImpl.STATUS_INCOMPLETE;
			}
		}

		if (onSource && (sourceRowCount != targetRowCount)) {
			return TableValidationImpl.STATUS_ROW_COUNT_MISMATCH;
		}

		if (standardTable) {
			for (ColumnValidation columnValidation : columnValidations) {
				if (ColumnValidationImpl.STATUS_CUSTOM.equals(
						columnValidation.getStatus())) {

					return TableValidationImpl.STATUS_HAS_CUSTOM_COLUMNS;
				}
			}
		}

		if (!onSource) {
			return TableValidationImpl.STATUS_TARGET_ONLY;
		}

		if (!standardTable) {
			if (objectTable) {
				return TableValidationImpl.STATUS_OBJECT;
			}

			return TableValidationImpl.STATUS_CUSTOM_TABLE;
		}

		return TableValidationImpl.STATUS_VALID;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DatabaseMigrator.class);

	private final MigrationRunRecorder _migrationRunRecorder =
		new MigrationRunRecorder();
	private final AtomicReference<MigrationStatus> _migrationStatus =
		new AtomicReference<>();

}