<#assign themeDisplay = request['theme-display'] />
<#assign
	themeDisplay = request.getAttribute("LIFERAY_SHARED_THEME_DISPLAY")
	portlet_setup = portlet_display.getPortletSetup()
/>
<#assign
	themeDisplay = request["theme-display"]
/>

<#assign portletPreferences = portletDisplay.getPortletSetup() />

${portlet_setup.getValue("displayStyle", "")}