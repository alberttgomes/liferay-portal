/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.dynamic.data.mapping.internal.upgrade.v7_1_2.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.dynamic.data.mapping.constants.DDMTemplateConstants;
import com.liferay.dynamic.data.mapping.model.DDMTemplate;
import com.liferay.dynamic.data.mapping.service.DDMTemplateLocalService;
import com.liferay.journal.model.JournalArticle;
import com.liferay.petra.string.StringPool;
import com.liferay.petra.string.StringUtil;
import com.liferay.portal.kernel.cache.MultiVMPool;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.template.TemplateConstants;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.upgrade.BaseTemplateUpgradeProcess;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.test.log.LogCapture;
import com.liferay.portal.test.log.LogEntry;
import com.liferay.portal.test.log.LoggerTestUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.upgrade.registry.UpgradeStepRegistrator;
import com.liferay.portal.upgrade.test.util.UpgradeTestUtil;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Albert Gomes Cabral
 */
@RunWith(Arquillian.class)
public class DDMTemplateScriptUpgradeProcessTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();

		_serviceContext = ServiceContextTestUtil.getServiceContext(
			_group.getGroupId(), TestPropsValues.getUserId());
	}

	@Test
	public void testUpgrade() throws Exception {
		DDMTemplate ddmTemplate = _addDDMTemplate(
			TemplateConstants.LANG_TYPE_FTL, _read("ddm-template-content.ftl"));

		_runUpgrade();

		String expectedScript = _read("expected-ddm-template-content.ftl");

		Assert.assertEquals(expectedScript, _getScript(ddmTemplate));

		_runUpgrade();

		Assert.assertEquals(expectedScript, _getScript(ddmTemplate));
	}

	@Test
	public void testUpgradeWithBrowserSnifferUtil() throws Exception {
		DDMTemplate ddmTemplate = _addDDMTemplate(
			TemplateConstants.LANG_TYPE_FTL,
			_read("ddm-template-browser-sniffer-content.ftl"));

		_runUpgrade();

		Assert.assertEquals(
			_read("expected-ddm-template-browser-sniffer-content.ftl"),
			_getScript(ddmTemplate));
	}

	@Test
	public void testUpgradeWithManualPattern() throws Exception {
		String script =
			"<#assign dlUtil = staticUtil[\"com.liferay.document.library." +
				"kernel.util.DLUtil\"] />\n${dlUtil.getImagePreviewURL(" +
					"fileEntry, themeDisplay)}";

		DDMTemplate ddmTemplate = _addDDMTemplate(
			TemplateConstants.LANG_TYPE_FTL, script);

		try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				BaseTemplateUpgradeProcess.class.getName(),
				LoggerTestUtil.WARN)) {

			_runUpgrade();

			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertTrue(
				logEntries.toString(),
				logEntries.stream(
				).anyMatch(
					logEntry -> {
						String message = logEntry.getMessage();

						return message.contains(
							String.valueOf(ddmTemplate.getTemplateId())) &&
							   message.endsWith("FTL-007");
					}
				));
		}

		Assert.assertEquals(script, _getScript(ddmTemplate));
	}

	@Test
	public void testUpgradeWithVelocityTemplate() throws Exception {
		String script =
			"#set ($viewURL = $httpUtil.removeParameter($currentURL, " +
				"\"redirect\"))";

		DDMTemplate ddmTemplate = _addDDMTemplate(
			TemplateConstants.LANG_TYPE_VM, script);

		_runUpgrade();

		Assert.assertEquals(script, _getScript(ddmTemplate));
	}

	private DDMTemplate _addDDMTemplate(String language, String script)
		throws Exception {

		return _ddmTemplateLocalService.addTemplate(
			null, TestPropsValues.getUserId(), _group.getGroupId(),
			_portal.getClassNameId(DDMTemplate.class), 0,
			_portal.getClassNameId(JournalArticle.class),
			HashMapBuilder.put(
				LocaleUtil.US, "DDMTemplate Name"
			).build(),
			HashMapBuilder.put(
				LocaleUtil.US, "DDMTemplate Description"
			).build(),
			DDMTemplateConstants.TEMPLATE_TYPE_DISPLAY, StringPool.BLANK,
			language, script, _serviceContext);
	}

	private String _getScript(DDMTemplate ddmTemplate) throws Exception {
		ddmTemplate = _ddmTemplateLocalService.getTemplate(
			ddmTemplate.getTemplateId());

		return ddmTemplate.getScript();
	}

	private String _read(String fileName) throws Exception {
		Class<?> clazz = getClass();

		return StringUtil.read(
			clazz.getClassLoader(),
			"com/liferay/dynamic/data/mapping/dependencies/upgrade.v7_1_2/" +
				fileName);
	}

	private void _runUpgrade() throws Exception {
		UpgradeProcess upgradeProcess = UpgradeTestUtil.getUpgradeStep(
			_upgradeStepRegistrator, _CLASS_NAME);

		upgradeProcess.upgrade();

		_multiVMPool.clear();
	}

	private static final String _CLASS_NAME =
		"com.liferay.dynamic.data.mapping.internal.upgrade.v7_1_2." +
			"DDMTemplateScriptUpgradeProcess";

	@Inject
	private DDMTemplateLocalService _ddmTemplateLocalService;

	@DeleteAfterTestRun
	private Group _group;

	@Inject
	private MultiVMPool _multiVMPool;

	@Inject
	private Portal _portal;

	private ServiceContext _serviceContext;

	@Inject(
		filter = "(&(component.name=com.liferay.dynamic.data.mapping.internal.upgrade.registry.DDMServiceUpgradeStepRegistrator))"
	)
	private UpgradeStepRegistrator _upgradeStepRegistrator;

}