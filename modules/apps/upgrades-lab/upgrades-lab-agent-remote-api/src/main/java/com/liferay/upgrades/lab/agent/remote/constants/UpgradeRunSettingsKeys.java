/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.constants;

/**
 * @author Albert Gomes Cabral
 */
public class UpgradeRunSettingsKeys {

	public static final String CUSTOMER_NAME = "customer.name";

	public static final String DB_TARGET_TYPE = "db.target.type";

	public static final String DB_TARGET_VERSION = "db.target.version";

	public static final String DOCKER_SERVICES_DATABASE =
		"docker.services.database";

	public static final String DOCKER_SERVICES_PORTAL =
		"docker.services.portal";

	public static final String DOCKER_SERVICES_SEARCH =
		"docker.services.search";

	public static final String NODE_VERSION = "node.version";

	public static final String PORTAL_DEPLOY_FOLDER = "portal.deploy.folder";

	public static final String PORTAL_START_COMMAND = "portal.start.command";

	public static final String PORTAL_STOP_COMMAND = "portal.stop.command";

	public static final String PORTAL_URL = "portal.url";

	/**
	 * Keys with no safe default and no way to infer them without asking. A
	 * concrete patch version is required for {@link #NODE_VERSION} and
	 * {@link #SEARCH_VERSION}: the agent feeds both verbatim into a Docker
	 * image tag, so a range such as <code>20.x</code> yields a tag that does
	 * not resolve.
	 */
	public static final String[] REQUIRED = {
		CUSTOMER_NAME, DB_TARGET_TYPE, DB_TARGET_VERSION, NODE_VERSION,
		UpgradeRunSettingsKeys.SEARCH_VERSION,
		UpgradeRunSettingsKeys.UPGRADE_SOURCE_VERSION,
		UpgradeRunSettingsKeys.UPGRADE_TARGET_JAVA_VERSION
	};

	public static final String SEARCH_ENGINE = "search.engine";

	public static final String SEARCH_VERSION = "search.version";

	public static final String UPGRADE_SOURCE_VERSION =
		"upgrade.source.version";

	public static final String UPGRADE_TARGET_JAVA_VERSION =
		"upgrade.target.java.version";

	public static final String UPGRADE_TARGET_VERSION =
		"upgrade.target.version";

}