/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.upgrade.util;

import com.liferay.petra.string.StringBundler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Albert Gomes Cabral
 */
public class TemplateUpgradeProcessUtilTest {

	@Test
	public void testGetMessageIds() {
		List<TemplateReplacement> templateReplacements = Arrays.asList(
			new TemplateReplacement(
				null, _CLASS_NAME_DL_UTIL, null, true, "FTL-007",
				new String[] {"getImagePreviewURL"}, new String[0], null));

		Assert.assertEquals(
			Collections.singletonList("FTL-007"),
			TemplateUpgradeProcessUtil.getMessageIds(
				StringBundler.concat(
					"<#assign dlUtil = staticUtil[\"", _CLASS_NAME_DL_UTIL,
					"\"] />\n",
					"${dlUtil.getImagePreviewURL(fileEntry, themeDisplay)}"),
				templateReplacements));
		Assert.assertEquals(
			Collections.emptyList(),
			TemplateUpgradeProcessUtil.getMessageIds(
				StringBundler.concat(
					"<#assign dlUtil = staticUtil[\"", _CLASS_NAME_DL_UTIL,
					"\"] />\n",
					"${dlUtil.getPreviewURL(fileEntry, fileVersion, ",
					"themeDisplay, \"\")}"),
				templateReplacements));
	}

	@Test
	public void testReplace() {
		List<TemplateReplacement> templateReplacements = Arrays.asList(
			new TemplateReplacement(
				null, null, "themeDisplay.getPortletGroupId", false, "FTL-006",
				new String[0], new String[0], "themeDisplay.getScopeGroupId"));

		Assert.assertEquals(
			"${themeDisplay.getScopeGroupId()} ${_themeDisplay." +
				"getPortletGroupId()}",
			TemplateUpgradeProcessUtil.replace(
				false,
				"${themeDisplay.getPortletGroupId()} ${_themeDisplay." +
					"getPortletGroupId()}",
				templateReplacements));
	}

	@Test
	public void testReplaceWithContextVariable() {
		List<TemplateReplacement> templateReplacements = Arrays.asList(
			new TemplateReplacement(
				"browserSniffer", _CLASS_NAME_BROWSER_SNIFFER_UTIL, null, false,
				"FTL-001", new String[0], new String[0], null));

		Assert.assertEquals(
			"\n${browserSniffer.isMobile(request)} ${browserSniffer10}",
			TemplateUpgradeProcessUtil.replace(
				false,
				StringBundler.concat(
					"<#assign browserSniffer1 = serviceLocator.findService(\"",
					_CLASS_NAME_BROWSER_SNIFFER_UTIL, "\") />\n",
					"${browserSniffer1.isMobile(request)} ",
					"${browserSniffer10}"),
				templateReplacements));
		Assert.assertEquals(
			"\n[#assign  variable = \"variable\" /]\n" +
				"${browserSniffer.isMobile(request)}",
			TemplateUpgradeProcessUtil.replace(
				true,
				StringBundler.concat(
					"[#assign sniffer = staticUtil[\"",
					_CLASS_NAME_BROWSER_SNIFFER_UTIL, "\"] /]\n",
					"[#assign sniffer2 = utilLocator.findUtil(\"",
					_CLASS_NAME_BROWSER_SNIFFER_UTIL,
					"\") variable = \"variable\" /]\n",
					"${sniffer.isMobile(request)}"),
				templateReplacements));
	}

	@Test
	public void testReplaceWithMethodNames() {
		List<TemplateReplacement> templateReplacements = Arrays.asList(
			new TemplateReplacement(
				null, _CLASS_NAME_HTTP_UTIL, null, false, "FTL-004",
				new String[] {"encodeURL"},
				new String[] {
					"urlCodec = staticUtil[\"" + _CLASS_NAME_URL_CODEC + "\"]"
				},
				"urlCodec"));

		String expectedTemplate = StringBundler.concat(
			"<#ftl strip_whitespace=true>\n",
			"<#assign urlCodec = staticUtil[\"", _CLASS_NAME_URL_CODEC,
			"\"] />\n", "<#assign http = staticUtil[\"", _CLASS_NAME_HTTP_UTIL,
			"\"] />\n", "${urlCodec.encodeURL(url)} ${http.URLtoString(url)}");

		Assert.assertEquals(
			expectedTemplate,
			TemplateUpgradeProcessUtil.replace(
				false,
				StringBundler.concat(
					"<#ftl strip_whitespace=true>\n",
					"<#assign http = staticUtil[\"", _CLASS_NAME_HTTP_UTIL,
					"\"] />\n",
					"${http.encodeURL(url)} ${http.URLtoString(url)}"),
				templateReplacements));
		Assert.assertEquals(
			expectedTemplate,
			TemplateUpgradeProcessUtil.replace(
				false, expectedTemplate, templateReplacements));
	}

	@Test
	public void testReplaceWithNewAssigns() {
		List<TemplateReplacement> templateReplacements = Arrays.asList(
			new TemplateReplacement(
				null, null,
				"regex:(?<![\\w.])httpUtil(\\s*\\.\\s*removeParameter\\s*\\()",
				false, "FTL-003", new String[0],
				new String[] {
					StringBundler.concat(
						"httpComponentsUtil = staticUtil[\"",
						_CLASS_NAME_HTTP_COMPONENTS_UTIL, "\"]")
				},
				"httpComponentsUtil$1"));

		String expectedTemplate = StringBundler.concat(
			"[#assign httpComponentsUtil = staticUtil[\"",
			_CLASS_NAME_HTTP_COMPONENTS_UTIL, "\"] /]\n",
			"[#assign viewURL = httpComponentsUtil.removeParameter(viewURL, ",
			"\"redirect\") /]");

		Assert.assertEquals(
			expectedTemplate,
			TemplateUpgradeProcessUtil.replace(
				true,
				"[#assign viewURL = httpUtil.removeParameter(viewURL, " +
					"\"redirect\") /]",
				templateReplacements));
		Assert.assertEquals(
			expectedTemplate,
			TemplateUpgradeProcessUtil.replace(
				true, expectedTemplate, templateReplacements));
	}

	@Test
	public void testReplaceWithNullValue() {
		Assert.assertNull(
			TemplateUpgradeProcessUtil.replace(
				false, null, Collections.emptyList()));
	}

	private static final String _CLASS_NAME_BROWSER_SNIFFER_UTIL =
		"com.liferay.portal.kernel.servlet.BrowserSnifferUtil";

	private static final String _CLASS_NAME_DL_UTIL =
		"com.liferay.document.library.kernel.util.DLUtil";

	private static final String _CLASS_NAME_HTTP_COMPONENTS_UTIL =
		"com.liferay.portal.kernel.util.HttpComponentsUtil";

	private static final String _CLASS_NAME_HTTP_UTIL =
		"com.liferay.portal.kernel.util.HttpUtil";

	private static final String _CLASS_NAME_URL_CODEC =
		"com.liferay.portal.kernel.util.URLCodec";

}