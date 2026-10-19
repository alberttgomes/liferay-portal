/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.document.library.internal.exportimport.data.handler.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.document.library.kernel.model.DLFileEntryMetadata;
import com.liferay.document.library.kernel.model.DLFileEntryType;
import com.liferay.document.library.kernel.model.DLFileEntryTypeConstants;
import com.liferay.document.library.kernel.service.DLFileEntryTypeLocalServiceUtil;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalServiceUtil;
import com.liferay.dynamic.data.mapping.test.util.DDMStructureTestUtil;
import com.liferay.exportimport.kernel.lar.ExportImportThreadLocal;
import com.liferay.exportimport.kernel.lar.StagedModelDataHandlerUtil;
import com.liferay.exportimport.test.util.lar.BaseStagedModelDataHandlerTestCase;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.StagedModel;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.test.TestInfo;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Máté Thurzó
 */
@RunWith(Arquillian.class)
public class DLFileEntryTypeStagedModelDataHandlerTest
	extends BaseStagedModelDataHandlerTestCase {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Test
	@TestInfo("LPD-110143")
	public void testImportDLFileEntryTypeWithDeletedDataDefinition()
		throws Exception {

		DDMStructure ddmStructure = DDMStructureTestUtil.addStructure(
			stagingGroup.getGroupId(), DLFileEntryMetadata.class.getName());

		DLFileEntryType dlFileEntryType = _addFileEntryType(ddmStructure);

		DDMStructure metadataSetDDMStructure =
			DDMStructureTestUtil.addStructure(
				stagingGroup.getGroupId(), DLFileEntryMetadata.class.getName());

		DLFileEntryTypeLocalServiceUtil.addDDMStructureLinks(
			dlFileEntryType.getFileEntryTypeId(),
			Collections.singleton(metadataSetDDMStructure.getStructureId()));

		DDMStructureLocalServiceUtil.deleteStructure(
			ddmStructure.getStructureId());

		DLFileEntryType importedDLFileEntryType = _exportImportFileEntryType(
			dlFileEntryType);

		DDMStructure importedDDMStructure =
			DDMStructureLocalServiceUtil.getDDMStructureByUuidAndGroupId(
				metadataSetDDMStructure.getUuid(), liveGroup.getGroupId());

		Assert.assertEquals(
			importedDDMStructure.getStructureId(),
			importedDLFileEntryType.getDataDefinitionId());
	}

	@Test
	@TestInfo("LPD-110143")
	public void testImportDLFileEntryTypeWithMetadataSetOfAnotherGroup()
		throws Exception {

		DDMStructure ddmStructure = DDMStructureTestUtil.addStructure(
			stagingGroup.getGroupId(), DLFileEntryMetadata.class.getName());

		DLFileEntryType dlFileEntryType = _addFileEntryType(ddmStructure);

		Group companyGroup = GroupLocalServiceUtil.getCompanyGroup(
			stagingGroup.getCompanyId());

		DDMStructure companyGroupDDMStructure =
			DDMStructureTestUtil.addStructure(
				companyGroup.getGroupId(), DLFileEntryMetadata.class.getName());

		DLFileEntryTypeLocalServiceUtil.addDDMStructureLinks(
			dlFileEntryType.getFileEntryTypeId(),
			new LinkedHashSet<>(
				Arrays.asList(
					companyGroupDDMStructure.getStructureId(),
					ddmStructure.getStructureId())));

		DLFileEntryType importedDLFileEntryType = _exportImportFileEntryType(
			dlFileEntryType);

		DDMStructure importedDDMStructure =
			DDMStructureLocalServiceUtil.getDDMStructureByUuidAndGroupId(
				ddmStructure.getUuid(), liveGroup.getGroupId());

		Assert.assertEquals(
			importedDDMStructure.getStructureId(),
			importedDLFileEntryType.getDataDefinitionId());
	}

	@Override
	protected Map<String, List<StagedModel>> addDependentStagedModelsMap(
			Group group)
		throws Exception {

		Map<String, List<StagedModel>> dependentStagedModelsMap =
			new HashMap<>();

		DDMStructure ddmStructure = DDMStructureTestUtil.addStructure(
			group.getGroupId(), DLFileEntryMetadata.class.getName());

		addDependentStagedModel(
			dependentStagedModelsMap, DDMStructure.class, ddmStructure);

		return dependentStagedModelsMap;
	}

	@Override
	protected StagedModel addStagedModel(
			Group group,
			Map<String, List<StagedModel>> dependentStagedModelsMap)
		throws Exception {

		List<StagedModel> dependentStagedModels = dependentStagedModelsMap.get(
			DDMStructure.class.getSimpleName());

		DDMStructure ddmStructure = (DDMStructure)dependentStagedModels.get(0);

		return DLFileEntryTypeLocalServiceUtil.addFileEntryType(
			null, TestPropsValues.getUserId(), group.getGroupId(),
			ddmStructure.getStructureId(), null,
			Collections.singletonMap(LocaleUtil.US, "New File Entry Type"),
			Collections.singletonMap(LocaleUtil.US, "New File Entry Type"),
			DLFileEntryTypeConstants.FILE_ENTRY_TYPE_SCOPE_DEFAULT,
			ServiceContextTestUtil.getServiceContext(
				group.getGroupId(), TestPropsValues.getUserId()));
	}

	@Override
	protected StagedModel getStagedModel(String uuid, Group group)
		throws PortalException {

		return DLFileEntryTypeLocalServiceUtil.
			getDLFileEntryTypeByUuidAndGroupId(uuid, group.getGroupId());
	}

	@Override
	protected Class<? extends StagedModel> getStagedModelClass() {
		return DLFileEntryType.class;
	}

	@Override
	protected void validateImport(
			Map<String, List<StagedModel>> dependentStagedModelsMap,
			Group group)
		throws Exception {

		List<StagedModel> dependentStagedModels = dependentStagedModelsMap.get(
			DDMStructure.class.getSimpleName());

		Assert.assertEquals(
			dependentStagedModels.toString(), 1, dependentStagedModels.size());

		DDMStructure ddmStructure = (DDMStructure)dependentStagedModels.get(0);

		DDMStructureLocalServiceUtil.getDDMStructureByUuidAndGroupId(
			ddmStructure.getUuid(), group.getGroupId());
	}

	@Override
	protected void validateImportedStagedModel(
			StagedModel stagedModel, StagedModel importedStagedModel)
		throws Exception {

		super.validateImportedStagedModel(stagedModel, importedStagedModel);

		DLFileEntryType dlFileEntryType = (DLFileEntryType)stagedModel;
		DLFileEntryType importedDLFileEntryType =
			(DLFileEntryType)importedStagedModel;

		Assert.assertEquals(
			dlFileEntryType.getName(), importedDLFileEntryType.getName());
		Assert.assertEquals(
			dlFileEntryType.getDescription(),
			importedDLFileEntryType.getDescription());
	}

	private DLFileEntryType _addFileEntryType(DDMStructure ddmStructure)
		throws Exception {

		return DLFileEntryTypeLocalServiceUtil.addFileEntryType(
			null, TestPropsValues.getUserId(), stagingGroup.getGroupId(),
			ddmStructure.getStructureId(), null,
			Collections.singletonMap(
				LocaleUtil.US, RandomTestUtil.randomString()),
			Collections.singletonMap(
				LocaleUtil.US, RandomTestUtil.randomString()),
			DLFileEntryTypeConstants.FILE_ENTRY_TYPE_SCOPE_DEFAULT,
			ServiceContextTestUtil.getServiceContext(
				stagingGroup.getGroupId(), TestPropsValues.getUserId()));
	}

	private DLFileEntryType _exportImportFileEntryType(
			DLFileEntryType dlFileEntryType)
		throws Exception {

		initExport();

		try {
			ExportImportThreadLocal.setPortletExportInProcess(true);

			StagedModelDataHandlerUtil.exportStagedModel(
				portletDataContext, dlFileEntryType);
		}
		finally {
			ExportImportThreadLocal.setPortletExportInProcess(false);
		}

		try (SafeCloseable safeCloseable = initImportWithSafeCloseable()) {
			StagedModel exportedStagedModel = readExportedStagedModel(
				dlFileEntryType);

			try {
				ExportImportThreadLocal.setPortletImportInProcess(true);

				StagedModelDataHandlerUtil.importReferenceStagedModels(
					portletDataContext, exportedStagedModel,
					DDMStructure.class);

				StagedModelDataHandlerUtil.importStagedModel(
					portletDataContext, exportedStagedModel);
			}
			finally {
				ExportImportThreadLocal.setPortletImportInProcess(false);
			}
		}

		return DLFileEntryTypeLocalServiceUtil.
			getDLFileEntryTypeByUuidAndGroupId(
				dlFileEntryType.getUuid(), liveGroup.getGroupId());
	}

}