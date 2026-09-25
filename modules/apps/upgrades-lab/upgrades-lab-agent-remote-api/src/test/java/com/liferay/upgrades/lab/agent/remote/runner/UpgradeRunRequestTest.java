/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.upgrades.lab.agent.remote.runner;

import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;
import com.liferay.upgrades.lab.agent.remote.constants.UpgradeRunSettingsKeys;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Albert Gomes Cabral
 */
public class UpgradeRunRequestTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testGetCredentialKeyReference() {
		String credentialKeyReference = RandomTestUtil.randomString();

		UpgradeRunRequest upgradeRunRequest = _createUpgradeRunRequest(
			credentialKeyReference, RandomTestUtil.randomString(),
			_getRequiredSettings());

		Assert.assertEquals(
			credentialKeyReference,
			upgradeRunRequest.getCredentialKeyReference());

		for (String emptyCredentialKeyReference : new String[] {null, ""}) {
			upgradeRunRequest = _createUpgradeRunRequest(
				emptyCredentialKeyReference, RandomTestUtil.randomString(),
				_getRequiredSettings());

			Assert.assertNull(upgradeRunRequest.getCredentialKeyReference());
		}
	}

	@Test
	public void testGetLicensePath() {
		String licensePath = RandomTestUtil.randomString();

		UpgradeRunRequest upgradeRunRequest = _createUpgradeRunRequest(
			RandomTestUtil.randomString(), licensePath, _getRequiredSettings());

		Assert.assertEquals(licensePath, upgradeRunRequest.getLicensePath());

		for (String emptyLicensePath : new String[] {null, ""}) {
			upgradeRunRequest = _createUpgradeRunRequest(
				RandomTestUtil.randomString(), emptyLicensePath,
				_getRequiredSettings());

			Assert.assertNull(upgradeRunRequest.getLicensePath());
		}
	}

	@Test
	public void testGetSettings() {
		Map<String, String> settings = _getRequiredSettings();

		settings.put(
			UpgradeRunSettingsKeys.SEARCH_ENGINE,
			RandomTestUtil.randomString());

		UpgradeRunRequest upgradeRunRequest = _createUpgradeRunRequest(
			settings);

		Assert.assertEquals(settings, upgradeRunRequest.getSettings());

		Map<String, String> requestSettings = upgradeRunRequest.getSettings();

		Assert.assertThrows(
			UnsupportedOperationException.class,
			() -> requestSettings.put(
				RandomTestUtil.randomString(), RandomTestUtil.randomString()));
	}

	private UpgradeRunRequest _createUpgradeRunRequest(
		Map<String, String> settings) {

		return _createUpgradeRunRequest(
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			settings);
	}

	private UpgradeRunRequest _createUpgradeRunRequest(
		String credentialKeyReference, String licensePath,
		Map<String, String> settings) {

		return new UpgradeRunRequest(
			RandomTestUtil.randomString(), RandomTestUtil.randomLong(),
			credentialKeyReference, licensePath, RandomTestUtil.randomString(),
			settings, RandomTestUtil.randomLong(),
			RandomTestUtil.randomString());
	}

	private Map<String, String> _getRequiredSettings() {
		Map<String, String> settings = new HashMap<>();

		for (String key : UpgradeRunSettingsKeys.REQUIRED) {
			settings.put(key, RandomTestUtil.randomString());
		}

		return settings;
	}

}