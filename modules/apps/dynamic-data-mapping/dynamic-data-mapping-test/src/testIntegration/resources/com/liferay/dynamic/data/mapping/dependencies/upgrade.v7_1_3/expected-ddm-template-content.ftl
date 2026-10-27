<#assign
	portlet_setup = portlet_display.getPortletPreferences()
/>

<#assign portletPreferences = portletDisplay.getPortletPreferences() />

${portlet_setup.getValue("displayStyle", "")}