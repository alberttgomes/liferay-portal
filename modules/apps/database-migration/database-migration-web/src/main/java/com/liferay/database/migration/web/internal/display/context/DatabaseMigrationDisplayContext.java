/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.web.internal.display.context;

import com.liferay.portal.kernel.util.HashMapBuilder;

import jakarta.portlet.RenderResponse;
import jakarta.portlet.ResourceURL;

import java.util.Map;

/**
 * @author Albert Gomes Cabral
 */
public class DatabaseMigrationDisplayContext {

	public DatabaseMigrationDisplayContext(RenderResponse renderResponse) {
		_renderResponse = renderResponse;
	}

	public Map<String, Object> getReactData() {
		return HashMapBuilder.<String, Object>put(
			"namespace", _renderResponse.getNamespace()
		).put(
			"schemaValidationURL",
			_createResourceURL("/database_migration/get_schema_validation")
		).put(
			"startMigrationURL",
			_createResourceURL("/database_migration/start_migration")
		).put(
			"statusURL", _createResourceURL("/database_migration/get_status")
		).put(
			"testConnectionURL",
			_createResourceURL("/database_migration/test_connection")
		).build();
	}

	private String _createResourceURL(String resourceID) {
		ResourceURL resourceURL = _renderResponse.createResourceURL();

		resourceURL.setResourceID(resourceID);

		return resourceURL.toString();
	}

	private final RenderResponse _renderResponse;

}