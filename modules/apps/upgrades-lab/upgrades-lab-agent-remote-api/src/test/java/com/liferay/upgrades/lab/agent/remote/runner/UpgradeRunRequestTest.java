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
		Map<String, String> settings = _getRequiredSettings();

		UpgradeRunRequest upgradeRunRequest = new UpgradeRunRequest(
			RandomTestUtil.randomString(), RandomTestUtil.randomLong(),
			"${secretRef:*:acme-deploy-key}", RandomTestUtil.randomString(),
			settings, RandomTestUtil.randomString(),
			RandomTestUtil.randomLong());

		Assert.assertEquals(
			"${secretRef:*:acme-deploy-key}",
			upgradeRunRequest.getCredentialKeyReference());

		for (String credentialKeyReference : new String[] {null, ""}) {
			upgradeRunRequest = new UpgradeRunRequest(
				RandomTestUtil.randomString(), RandomTestUtil.randomLong(),
				credentialKeyReference, RandomTestUtil.randomString(), settings,
				RandomTestUtil.randomString(), RandomTestUtil.randomLong());

			Assert.assertNull(upgradeRunRequest.getCredentialKeyReference());
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

	@Test
	public void testMissingRequiredSetting() {
		for (String key : UpgradeRunSettingsKeys.REQUIRED) {
			Map<String, String> settings = _getRequiredSettings();

			settings.remove(key);

			IllegalArgumentException illegalArgumentException =
				Assert.assertThrows(
					IllegalArgumentException.class,
					() -> _createUpgradeRunRequest(settings));

			Assert.assertEquals(
				"Setting \"" + key + "\" is null",
				illegalArgumentException.getMessage());
		}
	}

	@Test
	public void testNullSettings() {
		Assert.assertThrows(
			IllegalArgumentException.class,
			() -> _createUpgradeRunRequest(null));
	}

	private UpgradeRunRequest _createUpgradeRunRequest(
		Map<String, String> settings) {

		return new UpgradeRunRequest(
			RandomTestUtil.randomString(), RandomTestUtil.randomLong(),
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			settings, RandomTestUtil.randomString(),
			RandomTestUtil.randomLong());
	}

	private Map<String, String> _getRequiredSettings() {
		Map<String, String> settings = new HashMap<>();

		for (String key : UpgradeRunSettingsKeys.REQUIRED) {
			settings.put(key, RandomTestUtil.randomString());
		}

		return settings;
	}

}