/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.fragment.internal.upgrade.v4_1_1.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.change.tracking.conflict.ConflictInfo;
import com.liferay.change.tracking.model.CTCollection;
import com.liferay.change.tracking.service.CTCollectionLocalService;
import com.liferay.change.tracking.service.CTProcessLocalService;
import com.liferay.fragment.constants.FragmentConstants;
import com.liferay.fragment.model.FragmentCollection;
import com.liferay.fragment.model.FragmentEntry;
import com.liferay.fragment.model.FragmentEntryLink;
import com.liferay.fragment.model.FragmentEntryVersion;
import com.liferay.fragment.service.FragmentCollectionService;
import com.liferay.fragment.service.FragmentEntryLinkLocalService;
import com.liferay.fragment.service.FragmentEntryLocalService;
import com.liferay.fragment.service.FragmentEntryService;
import com.liferay.fragment.test.util.FragmentTestUtil;
import com.liferay.layout.test.util.LayoutTestUtil;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.petra.string.StringPool;
import com.liferay.petra.string.StringUtil;
import com.liferay.portal.kernel.cache.MultiVMPool;
import com.liferay.portal.kernel.change.tracking.CTCollectionThreadLocal;
import com.liferay.portal.kernel.dao.orm.EntityCache;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;
import com.liferay.portal.upgrade.registry.UpgradeStepRegistrator;
import com.liferay.portal.upgrade.test.util.UpgradeTestUtil;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Albert Gomes Cabral
 * @author Saurasish Basak
 */
@RunWith(Arquillian.class)
public class FragmentEntryTemplateUpgradeProcessTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();

		_serviceContext = ServiceContextTestUtil.getServiceContext(
			_group.getGroupId(), TestPropsValues.getUserId());
	}

	@Test
	public void testUpgrade() throws Exception {
		FragmentEntry fragmentEntry = _addFragmentEntry("<div></div>");

		fragmentEntry = _updateHTML(
			"fragment-entry-content.html", fragmentEntry);

		Layout layout = LayoutTestUtil.addTypeContentLayout(_group);

		FragmentEntryLink fragmentEntryLink =
			FragmentTestUtil.addFragmentEntryLink(
				fragmentEntry, layout.getPlid());

		fragmentEntryLink.setHtml(fragmentEntry.getHtml());

		fragmentEntryLink =
			_fragmentEntryLinkLocalService.updateFragmentEntryLink(
				fragmentEntryLink);

		_runUpgrade();

		String expectedHTML = _read("expected-fragment-entry-content.html");

		_assertHTML(expectedHTML, fragmentEntry, fragmentEntryLink);

		_runUpgrade();

		_assertHTML(expectedHTML, fragmentEntry, fragmentEntryLink);
	}

	@Test
	public void testUpgradeWithBrowserSnifferUtil() throws Exception {
		FragmentEntry fragmentEntry = _addFragmentEntry("<div></div>");

		fragmentEntry = _updateHTML(
			"fragment-entry-browser-sniffer-content.html", fragmentEntry);

		_runUpgrade();

		fragmentEntry = _fragmentEntryLocalService.getFragmentEntry(
			fragmentEntry.getFragmentEntryId());

		Assert.assertEquals(
			_read("expected-fragment-entry-browser-sniffer-content.html"),
			fragmentEntry.getHtml());
	}

	@Test
	public void testUpgradeWithCTCollection() throws Exception {
		CTCollection ctCollection = _ctCollectionLocalService.addCTCollection(
			null, TestPropsValues.getCompanyId(), TestPropsValues.getUserId(),
			0, RandomTestUtil.randomString(), null);

		FragmentEntry fragmentEntry = _addFragmentEntry(
			_read("fragment-entry.html"));

		try (SafeCloseable safeCloseable =
				CTCollectionThreadLocal.setCTCollectionIdWithSafeCloseable(
					ctCollection.getCtCollectionId())) {

			fragmentEntry.setHtml(_read("updated-fragment-entry.html"));

			fragmentEntry = _fragmentEntryLocalService.updateFragmentEntry(
				fragmentEntry);
		}

		Map<Long, List<ConflictInfo>> conflictsMap =
			_ctCollectionLocalService.checkConflicts(ctCollection);

		Assert.assertTrue(conflictsMap.isEmpty());

		_ctProcessLocalService.addCTProcess(
			TestPropsValues.getUserId(), ctCollection.getCtCollectionId());

		FragmentEntryVersion fragmentEntryVersion =
			_fragmentEntryLocalService.fetchLatestVersion(fragmentEntry);

		Assert.assertEquals(2, fragmentEntryVersion.getVersion());

		_runUpgrade();

		try (SafeCloseable safeCloseable =
				CTCollectionThreadLocal.setCTCollectionIdWithSafeCloseable(
					ctCollection.getCtCollectionId())) {

			FragmentEntry ctFragmentEntry =
				_fragmentEntryLocalService.getFragmentEntry(
					fragmentEntry.getFragmentEntryId());

			Assert.assertNotEquals(
				ctFragmentEntry.getHtml(), fragmentEntry.getHtml());
		}
	}

	private FragmentEntry _addFragmentEntry(String html) throws Exception {
		FragmentCollection fragmentCollection =
			_fragmentCollectionService.addFragmentCollection(
				null, _group.getGroupId(), "Fragment Collection",
				StringPool.BLANK, _serviceContext);

		return _fragmentEntryService.addFragmentEntry(
			null, _group.getGroupId(),
			fragmentCollection.getFragmentCollectionId(), null,
			"FragmentEntry Name", null, html, null, false, null, null, 0, false,
			false, FragmentConstants.TYPE_COMPONENT, null,
			WorkflowConstants.STATUS_APPROVED, _serviceContext);
	}

	private void _assertHTML(
			String expectedHTML, FragmentEntry fragmentEntry,
			FragmentEntryLink fragmentEntryLink)
		throws Exception {

		fragmentEntry = _fragmentEntryLocalService.getFragmentEntry(
			fragmentEntry.getFragmentEntryId());

		Assert.assertEquals(expectedHTML, fragmentEntry.getHtml());

		fragmentEntryLink = _fragmentEntryLinkLocalService.getFragmentEntryLink(
			fragmentEntryLink.getFragmentEntryLinkId());

		Assert.assertEquals(expectedHTML, fragmentEntryLink.getHtml());
	}

	private String _read(String fileName) throws Exception {
		Class<?> clazz = getClass();

		return StringUtil.read(
			clazz.getClassLoader(),
			"com/liferay/fragment/dependencies/upgrade.v4_1_1/" + fileName);
	}

	private void _runUpgrade() throws Exception {
		UpgradeProcess upgradeProcess = UpgradeTestUtil.getUpgradeStep(
			_upgradeStepRegistrator, _CLASS_NAME);

		upgradeProcess.upgrade();

		_entityCache.clearCache();
		_multiVMPool.clear();
	}

	private FragmentEntry _updateHTML(
			String fileName, FragmentEntry fragmentEntry)
		throws Exception {

		fragmentEntry.setHtml(_read(fileName));

		return _fragmentEntryLocalService.updateFragmentEntry(fragmentEntry);
	}

	private static final String _CLASS_NAME =
		"com.liferay.fragment.internal.upgrade.v4_1_1." +
			"FragmentEntryTemplateUpgradeProcess";

	@Inject
	private CTCollectionLocalService _ctCollectionLocalService;

	@Inject
	private CTProcessLocalService _ctProcessLocalService;

	@Inject
	private EntityCache _entityCache;

	@Inject
	private FragmentCollectionService _fragmentCollectionService;

	@Inject
	private FragmentEntryLinkLocalService _fragmentEntryLinkLocalService;

	@Inject
	private FragmentEntryLocalService _fragmentEntryLocalService;

	@Inject
	private FragmentEntryService _fragmentEntryService;

	@DeleteAfterTestRun
	private Group _group;

	@Inject
	private MultiVMPool _multiVMPool;

	private ServiceContext _serviceContext;

	@Inject(
		filter = "(&(component.name=com.liferay.fragment.internal.upgrade.registry.FragmentServiceUpgradeStepRegistrator))"
	)
	private UpgradeStepRegistrator _upgradeStepRegistrator;

}