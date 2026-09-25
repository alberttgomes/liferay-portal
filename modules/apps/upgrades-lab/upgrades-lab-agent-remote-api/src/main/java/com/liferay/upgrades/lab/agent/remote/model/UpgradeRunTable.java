/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.model;

import com.liferay.petra.sql.dsl.Column;
import com.liferay.petra.sql.dsl.base.BaseTable;

import java.sql.Clob;
import java.sql.Types;

import java.util.Date;

/**
 * The table class for the &quot;Upgrades_UpgradeRun&quot; database table.
 *
 * @author Albert Gomes Cabral
 * @see UpgradeRun
 * @generated
 */
public class UpgradeRunTable extends BaseTable<UpgradeRunTable> {

	public static final UpgradeRunTable INSTANCE = new UpgradeRunTable();

	public final Column<UpgradeRunTable, Long> mvccVersion = createColumn(
		"mvccVersion", Long.class, Types.BIGINT, Column.FLAG_NULLITY);
	public final Column<UpgradeRunTable, String> uuid = createColumn(
		"uuid_", String.class, Types.VARCHAR, Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, String> externalReferenceCode =
		createColumn(
			"externalReferenceCode", String.class, Types.VARCHAR,
			Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, Long> upgradeRunId = createColumn(
		"upgradeRunId", Long.class, Types.BIGINT, Column.FLAG_PRIMARY);
	public final Column<UpgradeRunTable, Long> companyId = createColumn(
		"companyId", Long.class, Types.BIGINT, Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, Long> userId = createColumn(
		"userId", Long.class, Types.BIGINT, Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, String> userName = createColumn(
		"userName", String.class, Types.VARCHAR, Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, Date> createDate = createColumn(
		"createDate", Date.class, Types.TIMESTAMP, Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, Date> modifiedDate = createColumn(
		"modifiedDate", Date.class, Types.TIMESTAMP, Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, String> branch = createColumn(
		"branch", String.class, Types.VARCHAR, Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, String> credentialKeyReference =
		createColumn(
			"credentialKeyReference", String.class, Types.VARCHAR,
			Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, String> customerName = createColumn(
		"customerName", String.class, Types.VARCHAR, Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, String> dbTargetType = createColumn(
		"dbTargetType", String.class, Types.VARCHAR, Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, String> dbTargetVersion = createColumn(
		"dbTargetVersion", String.class, Types.VARCHAR, Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, Date> endDate = createColumn(
		"endDate", Date.class, Types.TIMESTAMP, Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, String> nodeVersion = createColumn(
		"nodeVersion", String.class, Types.VARCHAR, Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, String> pullRequestURL = createColumn(
		"pullRequestURL", String.class, Types.VARCHAR, Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, String> repositoryURL = createColumn(
		"repositoryURL", String.class, Types.VARCHAR, Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, String> resultBranch = createColumn(
		"resultBranch", String.class, Types.VARCHAR, Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, String> searchVersion = createColumn(
		"searchVersion", String.class, Types.VARCHAR, Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, Date> startDate = createColumn(
		"startDate", Date.class, Types.TIMESTAMP, Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, String> targetRelease = createColumn(
		"targetRelease", String.class, Types.VARCHAR, Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, String> upgradeSourceVersion =
		createColumn(
			"upgradeSourceVersion", String.class, Types.VARCHAR,
			Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, String> upgradeTargetJavaVersion =
		createColumn(
			"upgradeTargetJavaVersion", String.class, Types.VARCHAR,
			Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, String> workspacePath = createColumn(
		"workspacePath", String.class, Types.VARCHAR, Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, Integer> status = createColumn(
		"status", Integer.class, Types.INTEGER, Column.FLAG_DEFAULT);
	public final Column<UpgradeRunTable, Clob> statusMessage = createColumn(
		"statusMessage", Clob.class, Types.CLOB, Column.FLAG_DEFAULT);

	private UpgradeRunTable() {
		super("Upgrades_UpgradeRun", UpgradeRunTable::new);
	}

}
// LIFERAY-SERVICE-BUILDER-HASH:-1238514847