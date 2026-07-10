/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.database.migration.service;

import java.util.List;
import java.util.Map;

/**
 * @author Albert Gomes Cabral
 */
public interface MigrationStatus {

	public static final int PHASE_COMPLETED = 6;

	public static final int PHASE_DATA_LOAD = 4;

	public static final int PHASE_DISCOVERY = 1;

	public static final int PHASE_ERROR = -1;

	public static final int PHASE_IDLE = 0;

	public static final int PHASE_INDEX_CREATION = 5;

	public static final int PHASE_SCHEMA_CREATION = 3;

	public long getElapsedTime();

	public String getMessage();

	public List<MigrationError> getMigrationErrors();

	public int getPhase();

	public String getPhaseLabel();

	public int getProgress();

	public long getStartTime();

	public Map<String, Long> getTableRowCounts();

	public List<TableValidation> getTableValidations();

}