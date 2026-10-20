/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.fragment.internal.upgrade.v4_1_1;

import com.liferay.portal.kernel.upgrade.BaseTemplateUpgradeProcess;

/**
 * @author Albert Gomes Cabral
 */
public class FragmentEntryTemplateUpgradeProcess
	extends BaseTemplateUpgradeProcess {

	@Override
	protected String[] getKernelReplacementsFileNames() {
		return new String[] {"template-replacements-1.json"};
	}

	@Override
	protected String[][] getTableAndColumnNames() {
		return new String[][] {
			{"FragmentEntry", "html"}, {"FragmentEntryLink", "html"},
			{"FragmentEntryVersion", "html"}
		};
	}

	@Override
	protected boolean isSquareBracketSyntax() {
		return true;
	}

}