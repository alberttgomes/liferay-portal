/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.database.migration.service.DatabaseMigrationManager;
import com.liferay.database.migration.service.MigrationStatus;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.dao.jdbc.DataSourceFactoryUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;

import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Albert Gomes Cabral
 */
@RunWith(Arquillian.class)
public class DatabaseMigrationManagerTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@BeforeClass
	public static void setUpClass() throws Exception {
		_sourceURL = System.getProperty("database.migration.source.url");
		_targetURL = System.getProperty("database.migration.target.url");

		Assume.assumeTrue(
			"Set database.migration.source.url and " +
				"database.migration.target.url to two dedicated, empty " +
					"PostgreSQL databases to run this test",
			(_sourceURL != null) && (_targetURL != null));

		_sourceUserName = GetterUtil.getString(
			System.getProperty("database.migration.source.username"),
			"postgres");
		_sourcePassword = GetterUtil.getString(
			System.getProperty("database.migration.source.password"));
		_targetUserName = GetterUtil.getString(
			System.getProperty("database.migration.target.username"),
			"postgres");
		_targetPassword = GetterUtil.getString(
			System.getProperty("database.migration.target.password"));

		_sourceDataSource = DataSourceFactoryUtil.initDataSource(
			_CLASS_NAME_DRIVER, _sourceURL, _sourceUserName, _sourcePassword,
			"");
		_targetDataSource = DataSourceFactoryUtil.initDataSource(
			_CLASS_NAME_DRIVER, _targetURL, _targetUserName, _targetPassword,
			"");
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		if ((_sourceDataSource == null) || (_targetDataSource == null)) {
			return;
		}

		DataSourceFactoryUtil.destroyDataSource(_sourceDataSource);
		DataSourceFactoryUtil.destroyDataSource(_targetDataSource);
	}

	@Before
	public void setUp() throws Exception {
		_dropTestTables();
	}

	@After
	public void tearDown() throws Exception {
		_dropTestTables();
	}

	@Test
	public void testMigrate() throws Exception {
		_setUpSourceSchema();
		_setUpTargetSchema();
		_insertSourceData();

		_databaseMigrationManager.startMigration(
			_sourceURL, _sourceUserName, _sourcePassword, _targetURL,
			_targetUserName, _targetPassword, TestPropsValues.getCompanyId(),
			TestPropsValues.getUserId(), "Test Migration");

		_waitForMigrationToComplete();

		_assertCustomTableMigrated();
		_assertCustomColumnMigrated();
		_assertStatusCompleted();
	}

	@Test
	public void testMigrateEmptySourceTable() throws Exception {
		_execute(
			_sourceDataSource,
			StringBundler.concat(
				"create table ", _TABLE_PARTIAL,
				" (entry_id bigint not null primary key, title varchar(75), ",
				"extra_count bigint)"));

		_databaseMigrationManager.startMigration(
			_sourceURL, _sourceUserName, _sourcePassword, _targetURL,
			_targetUserName, _targetPassword, TestPropsValues.getCompanyId(),
			TestPropsValues.getUserId(), "Test Empty Source Table Migration");

		_waitForMigrationToComplete();

		MigrationStatus migrationStatus =
			_databaseMigrationManager.getMigrationStatus();

		Assert.assertEquals(
			migrationStatus.getMessage(), MigrationStatus.PHASE_COMPLETED,
			migrationStatus.getPhase());

		try (Connection connection = _targetDataSource.getConnection();

			PreparedStatement preparedStatement = connection.prepareStatement(
				"select count(*) as row_count from " + _TABLE_PARTIAL);

			ResultSet resultSet = preparedStatement.executeQuery()) {

			Assert.assertTrue(resultSet.next());
			Assert.assertEquals(0, resultSet.getLong("row_count"));
		}
	}

	@Test
	public void testTestConnectionInvalid() throws Exception {
		boolean failed = false;

		try {
			_databaseMigrationManager.testConnection(
				"jdbc:postgresql://127.0.0.1:1/nonexistent", _sourceUserName,
				_sourcePassword);
		}
		catch (Exception exception) {
			failed = true;
		}

		Assert.assertTrue(failed);
	}

	@Test
	public void testTestConnectionValid() throws Exception {
		_databaseMigrationManager.testConnection(
			_sourceURL, _sourceUserName, _sourcePassword);
		_databaseMigrationManager.testConnection(
			_targetURL, _targetUserName, _targetPassword);
	}

	private void _assertCustomColumnMigrated() throws Exception {
		try (Connection connection = _targetDataSource.getConnection();

			PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select entry_id, title, extra_count from ", _TABLE_PARTIAL,
					" order by entry_id"));

			ResultSet resultSet = preparedStatement.executeQuery()) {

			Assert.assertTrue(resultSet.next());
			Assert.assertEquals(10, resultSet.getLong("entry_id"));
			Assert.assertEquals("Title-10", resultSet.getString("title"));
			Assert.assertEquals(111, resultSet.getLong("extra_count"));

			Assert.assertTrue(resultSet.next());
			Assert.assertEquals(20, resultSet.getLong("entry_id"));
			Assert.assertEquals("Title-20", resultSet.getString("title"));
			Assert.assertEquals(222, resultSet.getLong("extra_count"));

			Assert.assertFalse(resultSet.next());
		}
	}

	private void _assertCustomTableMigrated() throws Exception {
		try (Connection connection = _targetDataSource.getConnection();

			PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select id, name, amount, ratio, active, created, ",
					"payload, description from ", _TABLE_ALL_TYPES,
					" order by id"));

			ResultSet resultSet = preparedStatement.executeQuery()) {

			Assert.assertTrue(resultSet.next());
			Assert.assertEquals(1, resultSet.getLong("id"));
			Assert.assertEquals("Alpha", resultSet.getString("name"));
			Assert.assertEquals(1000, resultSet.getLong("amount"));
			Assert.assertEquals(1.5, resultSet.getDouble("ratio"), 0.0001);
			Assert.assertTrue(resultSet.getBoolean("active"));
			Assert.assertEquals(
				new Timestamp(_CREATED_TIME),
				resultSet.getTimestamp("created"));
			Assert.assertArrayEquals(_PAYLOAD, resultSet.getBytes("payload"));
			Assert.assertEquals(
				"alpha-description", resultSet.getString("description"));

			Assert.assertTrue(resultSet.next());
			Assert.assertEquals(2, resultSet.getLong("id"));
			Assert.assertEquals("Beta", resultSet.getString("name"));
			Assert.assertFalse(resultSet.getBoolean("active"));
			Assert.assertEquals(
				_LONG_DESCRIPTION, resultSet.getString("description"));

			Assert.assertTrue(resultSet.next());
			Assert.assertEquals(3, resultSet.getLong("id"));
			Assert.assertNull(resultSet.getString("name"));
			Assert.assertNull(resultSet.getObject("amount"));
			Assert.assertNull(resultSet.getTimestamp("created"));
			Assert.assertNull(resultSet.getBytes("payload"));
			Assert.assertNull(resultSet.getString("description"));

			Assert.assertFalse(resultSet.next());
		}
	}

	private void _assertStatusCompleted() {
		MigrationStatus migrationStatus =
			_databaseMigrationManager.getMigrationStatus();

		Assert.assertEquals(
			migrationStatus.getMessage(), MigrationStatus.PHASE_COMPLETED,
			migrationStatus.getPhase());
		Assert.assertEquals(100, migrationStatus.getProgress());

		Map<String, Long> tableRowCounts = migrationStatus.getTableRowCounts();

		Assert.assertEquals(
			tableRowCounts.toString(), Long.valueOf(3),
			tableRowCounts.get(_TABLE_ALL_TYPES));
		Assert.assertEquals(
			tableRowCounts.toString(), Long.valueOf(2),
			tableRowCounts.get(_TABLE_PARTIAL));

		Assert.assertFalse(_databaseMigrationManager.isMigrationRunning());
	}

	private void _dropTestTables() throws Exception {
		_execute(_sourceDataSource, "drop table if exists " + _TABLE_ALL_TYPES);
		_execute(_sourceDataSource, "drop table if exists " + _TABLE_PARTIAL);
		_execute(_targetDataSource, "drop table if exists " + _TABLE_ALL_TYPES);
		_execute(_targetDataSource, "drop table if exists " + _TABLE_PARTIAL);
	}

	private void _execute(DataSource dataSource, String sql) throws Exception {
		try (Connection connection = dataSource.getConnection();

			Statement statement = connection.createStatement()) {

			statement.executeUpdate(sql);
		}
	}

	private void _insertSourceData() throws Exception {
		try (Connection connection = _sourceDataSource.getConnection();

			PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"insert into ", _TABLE_ALL_TYPES,
					" (id, name, amount, ratio, active, created, payload, ",
					"description) values (?, ?, ?, ?, ?, ?, ?, ?)"))) {

			preparedStatement.setLong(1, 1);
			preparedStatement.setString(2, "Alpha");
			preparedStatement.setLong(3, 1000);
			preparedStatement.setDouble(4, 1.5);
			preparedStatement.setBoolean(5, true);
			preparedStatement.setTimestamp(6, new Timestamp(_CREATED_TIME));
			preparedStatement.setBytes(7, _PAYLOAD);
			preparedStatement.setString(8, "alpha-description");

			preparedStatement.executeUpdate();

			preparedStatement.setLong(1, 2);
			preparedStatement.setString(2, "Beta");
			preparedStatement.setLong(3, 2000);
			preparedStatement.setDouble(4, 2.5);
			preparedStatement.setBoolean(5, false);
			preparedStatement.setTimestamp(6, new Timestamp(_CREATED_TIME));
			preparedStatement.setBytes(7, _PAYLOAD);
			preparedStatement.setString(8, _LONG_DESCRIPTION);

			preparedStatement.executeUpdate();

			preparedStatement.setLong(1, 3);
			preparedStatement.setNull(2, Types.VARCHAR);
			preparedStatement.setNull(3, Types.BIGINT);
			preparedStatement.setNull(4, Types.DOUBLE);
			preparedStatement.setNull(5, Types.BOOLEAN);
			preparedStatement.setNull(6, Types.TIMESTAMP);
			preparedStatement.setNull(7, Types.BINARY);
			preparedStatement.setNull(8, Types.VARCHAR);

			preparedStatement.executeUpdate();
		}

		try (Connection connection = _sourceDataSource.getConnection();

			PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"insert into ", _TABLE_PARTIAL,
					" (entry_id, title, extra_count) values (?, ?, ?)"))) {

			preparedStatement.setLong(1, 10);
			preparedStatement.setString(2, "Title-10");
			preparedStatement.setLong(3, 111);

			preparedStatement.executeUpdate();

			preparedStatement.setLong(1, 20);
			preparedStatement.setString(2, "Title-20");
			preparedStatement.setLong(3, 222);

			preparedStatement.executeUpdate();
		}
	}

	private void _setUpSourceSchema() throws Exception {
		_execute(
			_sourceDataSource,
			StringBundler.concat(
				"create table ", _TABLE_ALL_TYPES,
				" (id bigint not null primary key, name varchar(75), amount ",
				"bigint, ratio double precision, active boolean, created ",
				"timestamp, payload bytea, description text)"));
		_execute(
			_sourceDataSource,
			StringBundler.concat(
				"create table ", _TABLE_PARTIAL,
				" (entry_id bigint not null primary key, title varchar(75), ",
				"extra_count bigint)"));
	}

	private void _setUpTargetSchema() throws Exception {
		_execute(
			_targetDataSource,
			StringBundler.concat(
				"create table ", _TABLE_PARTIAL,
				" (entry_id bigint not null primary key, title varchar(75))"));
	}

	private void _waitForMigrationToComplete() throws Exception {
		long endTime = System.currentTimeMillis() + 300000;

		MigrationStatus migrationStatus =
			_databaseMigrationManager.getMigrationStatus();

		while ((migrationStatus.getPhase() !=
					MigrationStatus.PHASE_COMPLETED) &&
			   (migrationStatus.getPhase() != MigrationStatus.PHASE_ERROR) &&
			   (System.currentTimeMillis() < endTime)) {

			Thread.sleep(500);

			migrationStatus = _databaseMigrationManager.getMigrationStatus();
		}
	}

	private static final String _CLASS_NAME_DRIVER = "org.postgresql.Driver";

	private static final long _CREATED_TIME = 1600000000000L;

	private static final String _LONG_DESCRIPTION = "x".repeat(5000);

	private static final byte[] _PAYLOAD = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

	private static final String _TABLE_ALL_TYPES = "dbmigration_all_types";

	private static final String _TABLE_PARTIAL = "dbmigration_partial";

	private static DataSource _sourceDataSource;
	private static String _sourcePassword;
	private static String _sourceURL;
	private static String _sourceUserName;
	private static DataSource _targetDataSource;
	private static String _targetPassword;
	private static String _targetURL;
	private static String _targetUserName;

	@Inject
	private DatabaseMigrationManager _databaseMigrationManager;

}