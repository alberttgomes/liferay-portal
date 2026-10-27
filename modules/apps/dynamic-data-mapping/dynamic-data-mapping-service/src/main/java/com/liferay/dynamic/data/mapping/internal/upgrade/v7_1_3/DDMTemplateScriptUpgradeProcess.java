/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.dynamic.data.mapping.internal.upgrade.v7_1_3;

import com.liferay.portal.kernel.upgrade.BaseTemplateUpgradeProcess;

/**
 * @author Albert Gomes Cabral
 */
public class DDMTemplateScriptUpgradeProcess
	extends BaseTemplateUpgradeProcess {

	@Override
	protected String[][] getTableAndColumnNames() {
		return new String[][] {
			{"DDMTemplate", "script", "language = 'ftl'"},
			{"DDMTemplateVersion", "script", "language = 'ftl'"}
		};
	}

}