/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.constants;

import com.liferay.portal.test.rule.LiferayUnitTestRule;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Albert Gomes Cabral
 */
public class UpgradeRunConstantsTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testGetStatusLabel() {
		Assert.assertEquals(
			UpgradeRunConstants.LABEL_BLOCKED,
			UpgradeRunConstants.getStatusLabel(
				UpgradeRunConstants.STATUS_BLOCKED));
		Assert.assertEquals(
			UpgradeRunConstants.LABEL_QUEUED,
			UpgradeRunConstants.getStatusLabel(
				UpgradeRunConstants.STATUS_QUEUED));
		Assert.assertEquals(
			UpgradeRunConstants.LABEL_TIMED_OUT,
			UpgradeRunConstants.getStatusLabel(
				UpgradeRunConstants.STATUS_TIMED_OUT));

		Assert.assertThrows(
			IllegalArgumentException.class,
			() -> UpgradeRunConstants.getStatusLabel(-1));
	}

	@Test
	public void testIsTerminal() {
		Assert.assertTrue(
			UpgradeRunConstants.isTerminal(UpgradeRunConstants.STATUS_BLOCKED));
		Assert.assertTrue(
			UpgradeRunConstants.isTerminal(
				UpgradeRunConstants.STATUS_CANCELLED));
		Assert.assertTrue(
			UpgradeRunConstants.isTerminal(UpgradeRunConstants.STATUS_FAILED));
		Assert.assertTrue(
			UpgradeRunConstants.isTerminal(
				UpgradeRunConstants.STATUS_SUCCESSFUL));
		Assert.assertTrue(
			UpgradeRunConstants.isTerminal(
				UpgradeRunConstants.STATUS_TIMED_OUT));

		Assert.assertFalse(
			UpgradeRunConstants.isTerminal(UpgradeRunConstants.STATUS_CLONING));
		Assert.assertFalse(
			UpgradeRunConstants.isTerminal(
				UpgradeRunConstants.STATUS_PROVISIONING));
		Assert.assertFalse(
			UpgradeRunConstants.isTerminal(
				UpgradeRunConstants.STATUS_PUBLISHING));
		Assert.assertFalse(
			UpgradeRunConstants.isTerminal(UpgradeRunConstants.STATUS_QUEUED));
		Assert.assertFalse(
			UpgradeRunConstants.isTerminal(UpgradeRunConstants.STATUS_RUNNING));
		Assert.assertFalse(
			UpgradeRunConstants.isTerminal(
				UpgradeRunConstants.STATUS_VALIDATING));
	}

	@Test
	public void testIsValidStatus() {
		for (int status : _STATUSES) {
			Assert.assertTrue(UpgradeRunConstants.isValidStatus(status));
		}

		Assert.assertFalse(UpgradeRunConstants.isValidStatus(-1));
		Assert.assertFalse(UpgradeRunConstants.isValidStatus(0));
		Assert.assertFalse(UpgradeRunConstants.isValidStatus(12));
	}

	@Test
	public void testIsValidTransition() {

		// A run advances through the happy path

		Assert.assertTrue(
			UpgradeRunConstants.isValidTransition(
				UpgradeRunConstants.STATUS_QUEUED,
				UpgradeRunConstants.STATUS_VALIDATING));
		Assert.assertTrue(
			UpgradeRunConstants.isValidTransition(
				UpgradeRunConstants.STATUS_VALIDATING,
				UpgradeRunConstants.STATUS_PROVISIONING));
		Assert.assertTrue(
			UpgradeRunConstants.isValidTransition(
				UpgradeRunConstants.STATUS_PROVISIONING,
				UpgradeRunConstants.STATUS_CLONING));
		Assert.assertTrue(
			UpgradeRunConstants.isValidTransition(
				UpgradeRunConstants.STATUS_CLONING,
				UpgradeRunConstants.STATUS_RUNNING));
		Assert.assertTrue(
			UpgradeRunConstants.isValidTransition(
				UpgradeRunConstants.STATUS_RUNNING,
				UpgradeRunConstants.STATUS_PUBLISHING));
		Assert.assertTrue(
			UpgradeRunConstants.isValidTransition(
				UpgradeRunConstants.STATUS_PUBLISHING,
				UpgradeRunConstants.STATUS_SUCCESSFUL));

		// A runner may skip a phase it does not need

		Assert.assertTrue(
			UpgradeRunConstants.isValidTransition(
				UpgradeRunConstants.STATUS_QUEUED,
				UpgradeRunConstants.STATUS_RUNNING));

		// A run never moves backwards or stays put

		Assert.assertFalse(
			UpgradeRunConstants.isValidTransition(
				UpgradeRunConstants.STATUS_RUNNING,
				UpgradeRunConstants.STATUS_CLONING));
		Assert.assertFalse(
			UpgradeRunConstants.isValidTransition(
				UpgradeRunConstants.STATUS_RUNNING,
				UpgradeRunConstants.STATUS_RUNNING));

		// Nothing leaves a terminal status, so a late callback cannot
		// overwrite a finished run, and a run fails, times out, or is
		// cancelled from anywhere else

		for (int status : _STATUSES) {
			for (int newStatus : _STATUSES) {
				if (UpgradeRunConstants.isTerminal(status)) {
					Assert.assertFalse(
						UpgradeRunConstants.isValidTransition(
							status, newStatus));
				}
				else if (UpgradeRunConstants.isTerminal(newStatus)) {
					Assert.assertTrue(
						UpgradeRunConstants.isValidTransition(
							status, newStatus));
				}
			}
		}
	}

	private static final int[] _STATUSES = {
		UpgradeRunConstants.STATUS_BLOCKED,
		UpgradeRunConstants.STATUS_CANCELLED,
		UpgradeRunConstants.STATUS_CLONING, UpgradeRunConstants.STATUS_FAILED,
		UpgradeRunConstants.STATUS_PROVISIONING,
		UpgradeRunConstants.STATUS_PUBLISHING,
		UpgradeRunConstants.STATUS_QUEUED, UpgradeRunConstants.STATUS_RUNNING,
		UpgradeRunConstants.STATUS_SUCCESSFUL,
		UpgradeRunConstants.STATUS_TIMED_OUT,
		UpgradeRunConstants.STATUS_VALIDATING
	};

}