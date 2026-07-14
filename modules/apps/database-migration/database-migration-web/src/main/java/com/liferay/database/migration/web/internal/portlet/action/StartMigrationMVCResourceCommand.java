/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.web.internal.portlet.action;

import com.liferay.database.migration.service.DatabaseMigrationManager;
import com.liferay.database.migration.web.internal.constants.DatabaseMigrationPortletKeys;
import com.liferay.database.migration.web.internal.util.PortalDatabaseConnection;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Albert Gomes Cabral
 */
@Component(
	property = {
		"jakarta.portlet.name=" + DatabaseMigrationPortletKeys.DATABASE_MIGRATION,
		"mvc.command.name=/database_migration/start_migration"
	},
	service = MVCResourceCommand.class
)
public class StartMigrationMVCResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		boolean useCurrentSourceConnection = ParamUtil.getBoolean(
			resourceRequest, "useCurrentSourceConnection");

		String sourceJDBCURL = ParamUtil.getString(
			resourceRequest, "sourceJDBCURL");
		String sourceUserName = ParamUtil.getString(
			resourceRequest, "sourceUserName");
		String sourcePassword = ParamUtil.getString(
			resourceRequest, "sourcePassword");

		if (useCurrentSourceConnection &&
			PortalDatabaseConnection.isAvailable()) {

			sourceJDBCURL = PortalDatabaseConnection.getJDBCURL();
			sourceUserName = PortalDatabaseConnection.getUserName();
			sourcePassword = PortalDatabaseConnection.getPassword();
		}

		String targetJDBCURL = ParamUtil.getString(
			resourceRequest, "targetJDBCURL");
		String targetUserName = ParamUtil.getString(
			resourceRequest, "targetUserName");

		String error = null;

		if (Validator.isNull(sourceJDBCURL) ||
			Validator.isNull(sourceUserName) ||
			Validator.isNull(targetJDBCURL) ||
			Validator.isNull(targetUserName)) {

			error = "connectionInformationRequired";
		}
		else if (!StringUtil.toLowerCase(
					targetJDBCURL
				).contains(
					"postgresql"
				)) {

			error = "targetDatabaseMustBePostgreSQL";
		}
		else {
			ThemeDisplay themeDisplay =
				(ThemeDisplay)resourceRequest.getAttribute(
					WebKeys.THEME_DISPLAY);

			try {
				_databaseMigrationManager.startMigration(
					sourceJDBCURL, sourceUserName, sourcePassword,
					targetJDBCURL, targetUserName,
					ParamUtil.getString(resourceRequest, "targetPassword"),
					themeDisplay.getCompanyId(), themeDisplay.getUserId(),
					ParamUtil.getString(resourceRequest, "migrationName"));
			}
			catch (IllegalStateException illegalStateException) {
				if (_log.isDebugEnabled()) {
					_log.debug(illegalStateException);
				}

				error = "migrationAlreadyRunning";
			}
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse,
			JSONUtil.put(
				"error", error
			).put(
				"started", Validator.isNull(error)
			));
	}

	private static final Log _log = LogFactoryUtil.getLog(
		StartMigrationMVCResourceCommand.class);

	@Reference
	private DatabaseMigrationManager _databaseMigrationManager;

}