/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.constants;

import com.liferay.portal.kernel.util.ArrayUtil;

/**
 * @author Albert Gomes Cabral
 */
public class UpgradeRunConstants {

	public static final String LABEL_BLOCKED = "blocked";

	public static final String LABEL_CANCELLED = "cancelled";

	public static final String LABEL_CLONING = "cloning";

	public static final String LABEL_FAILED = "failed";

	public static final String LABEL_PROVISIONING = "provisioning";

	public static final String LABEL_PUBLISHING = "publishing";

	public static final String LABEL_QUEUED = "queued";

	public static final String LABEL_RUNNING = "running";

	public static final String LABEL_SUCCESSFUL = "successful";

	public static final String LABEL_TIMED_OUT = "timed-out";

	public static final String LABEL_VALIDATING = "validating";

	public static final int STATUS_BLOCKED = 11;

	public static final int STATUS_CANCELLED = 8;

	public static final int STATUS_CLONING = 4;

	public static final int STATUS_FAILED = 7;

	public static final int STATUS_PROVISIONING = 3;

	public static final int STATUS_PUBLISHING = 6;

	public static final int STATUS_QUEUED = 1;

	public static final int STATUS_RUNNING = 5;

	public static final int STATUS_SUCCESSFUL = 10;

	public static final int STATUS_TIMED_OUT = 9;

	public static final int STATUS_VALIDATING = 2;

	public static String getStatusLabel(int status) {
		if (status == STATUS_BLOCKED) {
			return LABEL_BLOCKED;
		}
		else if (status == STATUS_CANCELLED) {
			return LABEL_CANCELLED;
		}
		else if (status == STATUS_CLONING) {
			return LABEL_CLONING;
		}
		else if (status == STATUS_FAILED) {
			return LABEL_FAILED;
		}
		else if (status == STATUS_PROVISIONING) {
			return LABEL_PROVISIONING;
		}
		else if (status == STATUS_PUBLISHING) {
			return LABEL_PUBLISHING;
		}
		else if (status == STATUS_QUEUED) {
			return LABEL_QUEUED;
		}
		else if (status == STATUS_RUNNING) {
			return LABEL_RUNNING;
		}
		else if (status == STATUS_SUCCESSFUL) {
			return LABEL_SUCCESSFUL;
		}
		else if (status == STATUS_TIMED_OUT) {
			return LABEL_TIMED_OUT;
		}
		else if (status == STATUS_VALIDATING) {
			return LABEL_VALIDATING;
		}

		throw new IllegalArgumentException(
			"Illegal upgrade run status value " + status);
	}

	public static boolean isTerminal(int status) {
		return ArrayUtil.contains(_STATUSES_TERMINAL, status);
	}

	public static boolean isValidStatus(int status) {
		return ArrayUtil.contains(_STATUSES, status);
	}

	public static boolean isValidTransition(int status, int newStatus) {
		if (isTerminal(status) || (status == newStatus)) {
			return false;
		}

		if (isTerminal(newStatus) || (newStatus > status)) {
			return true;
		}

		return false;
	}

	private static final int[] _STATUSES = {
		STATUS_BLOCKED, STATUS_CANCELLED, STATUS_CLONING, STATUS_FAILED,
		STATUS_PROVISIONING, STATUS_PUBLISHING, STATUS_QUEUED, STATUS_RUNNING,
		STATUS_SUCCESSFUL, STATUS_TIMED_OUT, STATUS_VALIDATING
	};

	private static final int[] _STATUSES_TERMINAL = {
		STATUS_BLOCKED, STATUS_CANCELLED, STATUS_FAILED, STATUS_SUCCESSFUL,
		STATUS_TIMED_OUT
	};

}