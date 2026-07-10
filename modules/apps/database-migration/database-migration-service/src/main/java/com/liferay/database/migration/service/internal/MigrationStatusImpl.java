/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service.internal;

import com.liferay.database.migration.service.MigrationError;
import com.liferay.database.migration.service.MigrationStatus;
import com.liferay.database.migration.service.TableValidation;

import java.io.Serial;
import java.io.Serializable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Albert Gomes Cabral
 */
public class MigrationStatusImpl implements MigrationStatus, Serializable {

	public MigrationStatusImpl(int phase, String message) {
		_phase = phase;
		_message = message;

		_startTime = System.currentTimeMillis();
	}

	public void addMigrationError(MigrationError migrationError) {
		_migrationErrors.add(migrationError);
	}

	public void addTableRowCount(String tableName, long rowCount) {
		_tableRowCounts.put(tableName, rowCount);
	}

	@Override
	public long getElapsedTime() {
		return System.currentTimeMillis() - _startTime;
	}

	@Override
	public String getMessage() {
		return _message;
	}

	@Override
	public List<MigrationError> getMigrationErrors() {
		return _migrationErrors;
	}

	@Override
	public int getPhase() {
		return _phase;
	}

	@Override
	public String getPhaseLabel() {
		if (_phase == PHASE_COMPLETED) {
			return "Completed";
		}

		if (_phase == PHASE_DATA_LOAD) {
			return "Copying Data";
		}

		if (_phase == PHASE_DISCOVERY) {
			return "Discovery";
		}

		if (_phase == PHASE_ERROR) {
			return "Error";
		}

		if (_phase == PHASE_INDEX_CREATION) {
			return "Creating Indexes";
		}

		if (_phase == PHASE_SCHEMA_CREATION) {
			return "Creating Schema";
		}

		return "Idle";
	}

	@Override
	public int getProgress() {
		return _progress;
	}

	@Override
	public long getStartTime() {
		return _startTime;
	}

	@Override
	public Map<String, Long> getTableRowCounts() {
		return _tableRowCounts;
	}

	@Override
	public List<TableValidation> getTableValidations() {
		return _tableValidations;
	}

	public void setMessage(String message) {
		_message = message;
	}

	public void setPhase(int phase) {
		_phase = phase;
	}

	public void setProgress(int progress) {
		_progress = Math.min(100, Math.max(0, progress));
	}

	public void setTableValidations(List<TableValidation> tableValidations) {
		_tableValidations = tableValidations;
	}

	@Serial
	private static final long serialVersionUID = 2L;

	private volatile String _message;
	private final List<MigrationError> _migrationErrors =
		new CopyOnWriteArrayList<>();
	private volatile int _phase;
	private volatile int _progress;
	private final long _startTime;
	private final Map<String, Long> _tableRowCounts = new ConcurrentHashMap<>();
	private volatile List<TableValidation> _tableValidations =
		Collections.emptyList();

}