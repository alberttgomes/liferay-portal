<#setting url_escaping_charset='ISO-8859-1'>

<#assign
	dlUtil = staticUtil["com.liferay.document.library.kernel.util.DLUtil"]
	http = staticUtil["com.liferay.portal.kernel.util.HttpUtil"]
	storageEngine = serviceLocator.findService("com.liferay.dynamic.data.mapping.storage.StorageEngine")
	stringPool = staticUtil["com.liferay.portal.kernel.util.StringPool"]

	viewURL = httpUtil.removeParameter(themeDisplay.getURLCurrent(), "redirect")
	queryString = http.getQueryString(viewURL)
/>

${http.encodeURL(viewURL)}
${themeDisplay.getPortletGroupId()}
${dlUtil.getPreviewURL(fileEntry, fileEntry.getFileVersion(), themeDisplay, "")}

<#assign
	fieldValuesMap = renderer.getDDMFormValuesReader().getDDMFormValues().getDDMFormFieldValuesMap()
	nestedFieldValuesMap = renderer.getDDMFormValuesReader().getDDMFormValues().getDDMFormFieldValuesMap(true)
	xPathSelector = saxReaderUtil.createXPath("dynamic-element[@name='title']")
/>

<@liferay_ui["asset-tags-summary"]
	className=entry.getClassName()
	classPK=entry.getClassPK()
/>

<#assign
	jsonFactory = serviceLocator.findService("com.liferay.portal.kernel.json.JSONFactoryUtil")
	path = httpUtil.getPath(viewURL)
	portalHelper = staticUtil["com.liferay.portal.kernel.util.PortalUtil"]
/>

${jsonFactory.looseSerialize(entry)}
${portalHelper.getCurrentURL(request)}
${serviceLocator.findService("com.liferay.portal.kernel.json.JSONFactoryUtil").createJSONObject()}
${journalArticleLocalService.getArticleContent(article, article.getDDMTemplateKey(), "VIEW", locale, themeDisplay)}

<#list cpContentHelper.getImages(cpCatalogEntry.getCPDefinitionId(), themeDisplay) as cpMedia>
	${cpMedia.getURL()}
</#list>

<#list curCommerceOrderItem.getJson()?eval as option>
	${option.key}
</#list>

<#assign
	groupImage = .vars['ProgressImage'].data?eval
	selectedOptions = cur_Field.getData()?eval
/>

${groupImage[0]}