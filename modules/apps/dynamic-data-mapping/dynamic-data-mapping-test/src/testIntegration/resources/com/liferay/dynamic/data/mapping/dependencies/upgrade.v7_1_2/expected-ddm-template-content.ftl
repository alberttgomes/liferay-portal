<#assign httpComponentsUtil = staticUtil["com.liferay.portal.kernel.util.HttpComponentsUtil"] />
<#setting url_escaping_charset='UTF-8'>

<#assign
	dlUtil = staticUtil["com.liferay.document.library.kernel.util.DLUtil"]
	http = staticUtil["com.liferay.portal.kernel.util.HttpUtil"]
	storageEngine = staticUtil["com.liferay.dynamic.data.mapping.kernel.StorageEngineManagerUtil"]
	stringPool = staticUtil["com.liferay.petra.string.StringPool"]

	viewURL = httpComponentsUtil.removeParameter(themeDisplay.getURLCurrent(), "redirect")
	queryString = httpComponentsUtil.getQueryString(viewURL)
/>

${urlCodec.encodeURL(viewURL)}
${themeDisplay.getScopeGroupId()}
${dlUtil.getPreviewURL(fileEntry, fileEntry.getFileVersion(), themeDisplay, "")}

<#assign
	fieldValuesMap = renderer.getDDMFormValuesReader().getDDMFormValues().getDDMFormFieldValuesReferencesMap(false)
	nestedFieldValuesMap = renderer.getDDMFormValuesReader().getDDMFormValues().getDDMFormFieldValuesReferencesMap(true)
	xPathSelector = saxReaderUtil.createXPath("dynamic-element[@field-reference='title']")
/>

<@liferay_asset["asset-tags-summary"]
	className=entry.getClassName()
	classPK=entry.getClassPK()
/>

<#assign
	
	path = httpComponentsUtil.getPath(viewURL)
	
/>

${jsonFactoryUtil.looseSerialize(entry)}
${portalUtil.getCurrentURL(request)}
${jsonFactoryUtil.createJSONObject()}
${journalArticleLocalService.getArticleContent(article, article.getDDMTemplateKey(), "VIEW", locale, null, themeDisplay)}

<#list cpContentHelper.getImages(cpCatalogEntry.getCPDefinitionId(), false, themeDisplay) as cpMedia>
	${cpMedia.getURL()}
</#list>

<#list curCommerceOrderItem.getJson()?eval_json as option>
	${option.key}
</#list>

<#assign
	groupImage = ((.vars['ProgressImage'].data?trim?starts_with("[") || .vars['ProgressImage'].data?trim?starts_with("{"))?then(.vars['ProgressImage'].data?eval_json, [.vars['ProgressImage'].data]))
	selectedOptions = ((cur_Field.getData()?trim?starts_with("[") || cur_Field.getData()?trim?starts_with("{"))?then(cur_Field.getData()?eval_json, [cur_Field.getData()]))
/>

${groupImage[0]}