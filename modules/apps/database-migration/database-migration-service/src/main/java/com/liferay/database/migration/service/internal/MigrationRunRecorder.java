/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.database.migration.service.MigrationStatus;
import com.liferay.object.constants.ObjectDefinitionConstants;
import com.liferay.object.constants.ObjectEntryFolderConstants;
import com.liferay.object.constants.ObjectFieldConstants;
import com.liferay.object.field.util.ObjectFieldUtil;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.model.ObjectField;
import com.liferay.object.service.ObjectDefinitionLocalServiceUtil;
import com.liferay.object.service.ObjectEntryLocalServiceUtil;
import com.liferay.object.service.ObjectFieldLocalServiceUtil;
import com.liferay.petra.function.UnsafeRunnable;
import com.liferay.petra.function.UnsafeSupplier;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.LocaleUtil;

import java.io.Serializable;

import java.sql.Connection;
import java.sql.PreparedStatement;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.sql.DataSource;

/**
 * @author Albert Gomes Cabral
 */
public class MigrationRunRecorder {

	public long record(
		long companyId, long userId, String migrationName, String sourceJDBCURL,
		String targetJDBCURL, MigrationStatus migrationStatus) {

		return _supplyAs(
			companyId, userId, "Unable to record the migration run", 0L,
			() -> {
				ObjectDefinition objectDefinition = _getObjectDefinition(
					companyId, userId);

				ObjectEntry objectEntry =
					ObjectEntryLocalServiceUtil.addObjectEntry(
						0, userId, objectDefinition.getObjectDefinitionId(),
						ObjectEntryFolderConstants.
							PARENT_OBJECT_ENTRY_FOLDER_ID_DEFAULT,
						null,
						HashMapBuilder.<String, Serializable>put(
							"durationSeconds", 0
						).put(
							"errorCount", 0
						).put(
							"migrationName", migrationName
						).put(
							"migrationStatus", _STATUS_PENDING
						).put(
							"rowsCopied", 0L
						).put(
							"sourceURL", sourceJDBCURL
						).put(
							"startedAt",
							new Date(migrationStatus.getStartTime())
						).put(
							"tableCount", 0
						).put(
							"targetURL", targetJDBCURL
						).build(),
						_createServiceContext(companyId, userId));

				return objectEntry.getObjectEntryId();
			});
	}

	public void updateRecord(
		long companyId, long userId, String migrationName, String sourceJDBCURL,
		String targetJDBCURL, DataSource targetDataSource, long objectEntryId,
		MigrationStatus migrationStatus) {

		if (objectEntryId <= 0) {
			return;
		}

		_runAs(
			companyId, userId, "Unable to update the migration run",
			() -> ObjectEntryLocalServiceUtil.updateObjectEntry(
				userId, objectEntryId,
				ObjectEntryFolderConstants.
					PARENT_OBJECT_ENTRY_FOLDER_ID_DEFAULT,
				HashMapBuilder.<String, Serializable>put(
					"durationSeconds",
					(int)(migrationStatus.getElapsedTime() / 1000)
				).put(
					"errorCount",
					migrationStatus.getMigrationErrors(
					).size()
				).put(
					"migrationName", migrationName
				).put(
					"migrationStatus", _getStatus(migrationStatus)
				).put(
					"rowsCopied", _getRowsCopied(migrationStatus)
				).put(
					"sourceURL", sourceJDBCURL
				).put(
					"startedAt", new Date(migrationStatus.getStartTime())
				).put(
					"tableCount",
					migrationStatus.getTableRowCounts(
					).size()
				).put(
					"targetURL", targetJDBCURL
				).build(),
				_createServiceContext(companyId, userId)));

		_postRecord(
			companyId, userId, targetDataSource, objectEntryId,
			migrationStatus);
	}

	private Map<Locale, String> _createLabelMap(String value) {
		return Collections.singletonMap(LocaleUtil.getDefault(), value);
	}

	private ServiceContext _createServiceContext(long companyId, long userId) {
		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setCompanyId(companyId);
		serviceContext.setUserId(userId);

		return serviceContext;
	}

	private String _getDBColumnName(
			ObjectDefinition objectDefinition, String name)
		throws PortalException {

		ObjectField objectField = _getObjectField(objectDefinition, name);

		return objectField.getDBColumnName();
	}

	private ObjectDefinition _getObjectDefinition(long companyId, long userId)
		throws Exception {

		ObjectDefinition objectDefinition =
			ObjectDefinitionLocalServiceUtil.fetchObjectDefinition(
				companyId, "C_MigrationRun");

		if (objectDefinition != null) {
			return objectDefinition;
		}

		objectDefinition =
			ObjectDefinitionLocalServiceUtil.addCustomObjectDefinition(
				null, userId, 0, null, true, false, true, false, true, false,
				false, false, false, null, _createLabelMap("Migration Run"),
				"MigrationRun", "100", null, _createLabelMap("Migration Runs"),
				true, ObjectDefinitionConstants.SCOPE_COMPANY,
				ObjectDefinitionConstants.STORAGE_TYPE_DEFAULT,
				Collections.emptyList(),
				Arrays.asList(
					ObjectFieldUtil.createObjectField(
						ObjectFieldConstants.BUSINESS_TYPE_TEXT,
						ObjectFieldConstants.DB_TYPE_STRING, true, false, null,
						"Migration Name", "migrationName", false),
					ObjectFieldUtil.createObjectField(
						ObjectFieldConstants.BUSINESS_TYPE_DATE,
						ObjectFieldConstants.DB_TYPE_DATE, true, false, null,
						"Started At", "startedAt", false),
					ObjectFieldUtil.createObjectField(
						ObjectFieldConstants.BUSINESS_TYPE_TEXT,
						ObjectFieldConstants.DB_TYPE_STRING, false, false, null,
						"Source URL", "sourceURL", false),
					ObjectFieldUtil.createObjectField(
						ObjectFieldConstants.BUSINESS_TYPE_TEXT,
						ObjectFieldConstants.DB_TYPE_STRING, false, false, null,
						"Target URL", "targetURL", false),
					ObjectFieldUtil.createObjectField(
						ObjectFieldConstants.BUSINESS_TYPE_TEXT,
						ObjectFieldConstants.DB_TYPE_STRING, true, false, null,
						"Status", "migrationStatus", false),
					ObjectFieldUtil.createObjectField(
						ObjectFieldConstants.BUSINESS_TYPE_INTEGER,
						ObjectFieldConstants.DB_TYPE_INTEGER, false, false,
						null, "Table Count", "tableCount", false),
					ObjectFieldUtil.createObjectField(
						ObjectFieldConstants.BUSINESS_TYPE_LONG_INTEGER,
						ObjectFieldConstants.DB_TYPE_LONG, false, false, null,
						"Rows Copied", "rowsCopied", false),
					ObjectFieldUtil.createObjectField(
						ObjectFieldConstants.BUSINESS_TYPE_INTEGER,
						ObjectFieldConstants.DB_TYPE_INTEGER, false, false,
						null, "Error Count", "errorCount", false),
					ObjectFieldUtil.createObjectField(
						ObjectFieldConstants.BUSINESS_TYPE_INTEGER,
						ObjectFieldConstants.DB_TYPE_INTEGER, false, false,
						null, "Duration Seconds", "durationSeconds", false)),
				Collections.emptyList(),
				_createServiceContext(companyId, userId));

		return ObjectDefinitionLocalServiceUtil.publishCustomObjectDefinition(
			userId, objectDefinition.getObjectDefinitionId());
	}

	private ObjectField _getObjectField(
			ObjectDefinition objectDefinition, String name)
		throws PortalException {

		return ObjectFieldLocalServiceUtil.getObjectField(
			objectDefinition.getObjectDefinitionId(), name);
	}

	private long _getRowsCopied(MigrationStatus migrationStatus) {
		long rowsCopied = 0;

		for (long rowCount :
				migrationStatus.getTableRowCounts(
				).values()) {

			rowsCopied += rowCount;
		}

		return rowsCopied;
	}

	private String _getStatus(MigrationStatus migrationStatus) {
		if (migrationStatus.getPhase() == MigrationStatus.PHASE_ERROR) {
			return _STATUS_ERROR;
		}

		return _STATUS_COMPLETED;
	}

	private void _postRecord(
		long companyId, long userId, DataSource targetDataSource,
		long objectEntryId, MigrationStatus migrationStatus) {

		if ((targetDataSource == null) || (objectEntryId <= 0)) {
			return;
		}

		_runAs(
			companyId, userId,
			"Unable to post the migration run to the target database",
			() -> {
				ObjectDefinition objectDefinition = _getObjectDefinition(
					companyId, userId);

				ObjectField migrationStatusObjectField = _getObjectField(
					objectDefinition, "migrationStatus");

				String sql = StringBundler.concat(
					"update ", migrationStatusObjectField.getDBTableName(),
					" set ", migrationStatusObjectField.getDBColumnName(),
					" = ?, ", _getDBColumnName(objectDefinition, "rowsCopied"),
					" = ?, ", _getDBColumnName(objectDefinition, "errorCount"),
					" = ?, ", _getDBColumnName(objectDefinition, "tableCount"),
					" = ?, ",
					_getDBColumnName(objectDefinition, "durationSeconds"),
					" = ? where ",
					objectDefinition.getPKObjectFieldDBColumnName(), " = ?");

				try (Connection connection = targetDataSource.getConnection();

					PreparedStatement preparedStatement =
						connection.prepareStatement(sql)) {

					preparedStatement.setString(1, _getStatus(migrationStatus));
					preparedStatement.setLong(
						2, _getRowsCopied(migrationStatus));
					preparedStatement.setInt(
						3,
						migrationStatus.getMigrationErrors(
						).size());
					preparedStatement.setInt(
						4,
						migrationStatus.getTableRowCounts(
						).size());
					preparedStatement.setInt(
						5, (int)(migrationStatus.getElapsedTime() / 1000));
					preparedStatement.setLong(6, objectEntryId);

					preparedStatement.executeUpdate();
				}
			});
	}

	private void _runAs(
		long companyId, long userId, String errorMessage,
		UnsafeRunnable<Exception> unsafeRunnable) {

		_supplyAs(
			companyId, userId, errorMessage, null,
			() -> {
				unsafeRunnable.run();

				return null;
			});
	}

	private <T> T _supplyAs(
		long companyId, long userId, String errorMessage, T defaultValue,
		UnsafeSupplier<T, Exception> unsafeSupplier) {

		String principalName = PrincipalThreadLocal.getName();

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(companyId)) {

			PrincipalThreadLocal.setName(userId);

			return unsafeSupplier.get();
		}
		catch (Exception exception) {
			_log.error(errorMessage, exception);

			return defaultValue;
		}
		finally {
			PrincipalThreadLocal.setName(principalName);
		}
	}

	private static final String _STATUS_COMPLETED = "COMPLETED";

	private static final String _STATUS_ERROR = "ERROR";

	private static final String _STATUS_PENDING = "PENDING";

	private static final Log _log = LogFactoryUtil.getLog(
		MigrationRunRecorder.class);

}