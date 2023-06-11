/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.dynamic.data.mapping.internal.upgrade.v5_3_2;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Albert Gomes Cabral
 */
public class DDMTemplateBrowserSnifferUpgradeProcess extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		_upgradeDDMTemplateRemoveBrowserSniffer();
	}

	private void _upgradeDDMTemplateRemoveBrowserSniffer()
		throws Exception {

		try(PreparedStatement selectPrepareStatement =
				connection.prepareStatement(
					StringBundler.concat(
						"select DDMTemplate.script from ",
						"DDMTemplate where templateKey ? = ?"));
			PreparedStatement updatePreparedStatement =
				AutoBatchPreparedStatementUtil.concurrentAutoBatch(
					connection,
					"update DDMTemplate set script = ? where " +
					"templateId = ?")) {

			try(ResultSet resultSet = selectPrepareStatement.executeQuery()) {
					while (resultSet.next()) {
						String data = resultSet.getString(1);

						Pattern patternRegex = Pattern.compile(_BrowserSnifferRegex);

						Matcher browserSnifferMather = patternRegex.matcher(data);

						if (browserSnifferMather.find()) {
							continue;
						}

						String dataResult = browserSnifferMather.replaceAll(
							""
						);

						updatePreparedStatement.setString(1, dataResult);
						updatePreparedStatement.setLong(2, resultSet.getLong(2));

						updatePreparedStatement.addBatch();
					}

					updatePreparedStatement.executeBatch();
			}
		}
	}

	private static final String _BrowserSnifferRegex =
		".*com\\.liferay\\.portal\\.kernel\\.servlet\\.BrowserSnifferUtil.*\\n?";

	private static final String [] _BrowserSnifferValue = {
		"com.liferay.portal.kernel.servlet.BrowserSnifferUtil",
	};
}
