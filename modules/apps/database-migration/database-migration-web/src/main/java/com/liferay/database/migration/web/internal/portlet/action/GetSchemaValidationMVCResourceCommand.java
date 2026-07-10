/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.web.internal.portlet.action;

import com.liferay.database.migration.service.ColumnValidation;
import com.liferay.database.migration.service.DatabaseMigrationManager;
import com.liferay.database.migration.service.MigrationStatus;
import com.liferay.database.migration.service.TableValidation;
import com.liferay.database.migration.web.internal.constants.DatabaseMigrationPortletKeys;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Albert Gomes Cabral
 */
@Component(
	property = {
		"jakarta.portlet.name=" + DatabaseMigrationPortletKeys.DATABASE_MIGRATION,
		"mvc.command.name=/database_migration/get_schema_validation"
	},
	service = MVCResourceCommand.class
)
public class GetSchemaValidationMVCResourceCommand
	extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		MigrationStatus migrationStatus =
			_databaseMigrationManager.getMigrationStatus();

		List<TableValidation> tableValidations =
			migrationStatus.getTableValidations();

		JSONArray tableValidationsJSONArray = _jsonFactory.createJSONArray();

		for (TableValidation tableValidation : tableValidations) {
			JSONArray columnValidationsJSONArray =
				_jsonFactory.createJSONArray();

			for (ColumnValidation columnValidation :
					tableValidation.getColumnValidations()) {

				columnValidationsJSONArray.put(
					JSONUtil.put(
						"columnName", columnValidation.getColumnName()
					).put(
						"sourceType", columnValidation.getSourceType()
					).put(
						"status", columnValidation.getStatus()
					).put(
						"targetType", columnValidation.getTargetType()
					));
			}

			tableValidationsJSONArray.put(
				JSONUtil.put(
					"columnValidations", columnValidationsJSONArray
				).put(
					"sourceRowCount", tableValidation.getSourceRowCount()
				).put(
					"status", tableValidation.getStatus()
				).put(
					"tableName", tableValidation.getTableName()
				).put(
					"targetRowCount", tableValidation.getTargetRowCount()
				));
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse,
			JSONUtil.put(
				"available", !tableValidations.isEmpty()
			).put(
				"tableValidations", tableValidationsJSONArray
			));
	}

	@Reference
	private DatabaseMigrationManager _databaseMigrationManager;

	@Reference
	private JSONFactory _jsonFactory;

}