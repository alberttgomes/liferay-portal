# Source Formatter Check Classes — Reference

32 check classes filtered by authors Nicolas Moura, Michael Cavalcanti, Tamyris Bernardo, Kyle Miho, and Albert Gomes Cabral.
Each entry contains: what the check does, the input the test provides, and the expected output after the check runs.

---

## 1. JavaUpgradeModelPermissionsCheck

**Author:** Michael Cavalcanti
**What it does:** Replaces direct `setGroupPermissions()` / `setGuestPermissions()` calls on `ServiceContext` with the `ModelPermissions` / `ModelPermissionsFactory` API. Generates a null-check guard: if `getModelPermissions()` returns null it creates one via the factory; otherwise it adds role permissions to the existing instance.

**INPUT:**
```java
import com.liferay.portal.kernel.service.ServiceContext;

@Component(immediate = true)
public class UpgradeJavaModelPermissionsCheck {

	@Override
	public App addOrUpdate() throws IOException, PortalException {
		ServiceContext serviceContext;

		String[] groupPermissions = {"VIEW"};
		String[] guestPermissions = {"VIEW"};

		serviceContext.setAddGroupPermissions(true);
		serviceContext.setDeriveDefaultPermissions(true);
		serviceContext.setGroupPermissions(groupPermissions);
		serviceContext.setGuestPermissions(guestPermissions);
	}

	@Override
	public App addOrUpdate2() throws IOException, PortalException {
		ServiceContext serviceContext;

		String[] groupPermissions = {"VIEW"};

		serviceContext.setAddGroupPermissions(true);
		serviceContext.setDeriveDefaultPermissions(true);
		serviceContext.setGroupPermissions(groupPermissions);
	}

	@Override
	public App addOrUpdate3() throws IOException, PortalException {
		ServiceContext serviceContext2;

		String[] guestPermissions = {"VIEW"};

		serviceContext2.setAddGroupPermissions(true);
		serviceContext2.setDeriveDefaultPermissions(true);
		serviceContext2.setGuestPermissions(guestPermissions);
	}

	@Override
	public App addOrUpdate4() throws IOException, PortalException {
		ServiceContext serviceContext2;

		serviceContext2.setAddGroupPermissions(true);
		serviceContext2.setDeriveDefaultPermissions(true);
	}

}
```

**EXPECTED OUTPUT:**
```java
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.service.permission.ModelPermissions;
import com.liferay.portal.kernel.service.permission.ModelPermissionsFactory;

@Component(immediate = true)
public class UpgradeJavaModelPermissionsCheck {

	@Override
	public App addOrUpdate() throws IOException, PortalException {
		ServiceContext serviceContext;

		String[] groupPermissions = {"VIEW"};
		String[] guestPermissions = {"VIEW"};

		serviceContext.setAddGroupPermissions(true);
		serviceContext.setDeriveDefaultPermissions(true);

		ModelPermissions modelPermissions = serviceContext.getModelPermissions();

		if (modelPermissions == null) {
			modelPermissions = ModelPermissionsFactory.create(groupPermissions, guestPermissions);
		}
		else {
			modelPermissions.addRolePermissions(RoleConstants.PLACEHOLDER_DEFAULT_GROUP_ROLE, groupPermissions);
			modelPermissions.addRolePermissions(RoleConstants.GUEST, guestPermissions);
		}

		serviceContext.setModelPermissions(modelPermissions);
	}

	@Override
	public App addOrUpdate2() throws IOException, PortalException {
		ServiceContext serviceContext;

		String[] groupPermissions = {"VIEW"};

		serviceContext.setAddGroupPermissions(true);
		serviceContext.setDeriveDefaultPermissions(true);

		ModelPermissions modelPermissions = serviceContext.getModelPermissions();

		if (modelPermissions == null) {
			modelPermissions = ModelPermissionsFactory.create(groupPermissions, new String[0]);
		}
		else {
			modelPermissions.addRolePermissions(RoleConstants.PLACEHOLDER_DEFAULT_GROUP_ROLE, groupPermissions);
		}

		serviceContext.setModelPermissions(modelPermissions);
	}

	@Override
	public App addOrUpdate3() throws IOException, PortalException {
		ServiceContext serviceContext2;

		String[] guestPermissions = {"VIEW"};

		serviceContext2.setAddGroupPermissions(true);
		serviceContext2.setDeriveDefaultPermissions(true);

		ModelPermissions modelPermissions = serviceContext2.getModelPermissions();

		if (modelPermissions == null) {
			modelPermissions = ModelPermissionsFactory.create(new String[0], guestPermissions);
		}
		else {
			modelPermissions.addRolePermissions(RoleConstants.GUEST, guestPermissions);
		}

		serviceContext2.setModelPermissions(modelPermissions);
	}

	@Override
	public App addOrUpdate4() throws IOException, PortalException {
		ServiceContext serviceContext2;

		serviceContext2.setAddGroupPermissions(true);
		serviceContext2.setDeriveDefaultPermissions(true);
	}

}
```

---

## 2. UpgradeJavaBaseFragmentCollectionContributorExtendedClassesCheck

**Author:** Kyle Miho
**What it does:** Reads the string returned by `getFragmentCollectionKey()` and injects it as a `fragment.collection.key` property into the `@Component` annotation.

**INPUT:**
```java
@Component(service = FragmentCollectionContributor.class)
public class UpgradeJavaBaseFragmentCollectionContributorExtendedClassesCheck
	extends BaseFragmentCollectionContributor {

	@Override
	public String getFragmentCollectionKey() {
		return "FRAGMENT_COLLECTION_KEY";
	}

}
```

**EXPECTED OUTPUT:**
```java
@Component(
	property = "fragment.collection.key=FRAGMENT_COLLECTION_KEY",
	service = FragmentCollectionContributor.class
)
public class UpgradeJavaBaseFragmentCollectionContributorExtendedClassesCheck
	extends BaseFragmentCollectionContributor {

	@Override
	public String getFragmentCollectionKey() {
		return "FRAGMENT_COLLECTION_KEY";
	}

}
```

---

## 3. UpgradeJavaDisplayPageInfoItemCapabilityCheck

**Author:** Kyle Miho
**What it does:** Replaces a `@Reference`-injected `DisplayPageInfoItemCapability` field with a generic `InfoItemCapability` field filtered by `info.item.capability.key`. Adds the `InfoItemCapability` import.

**INPUT:**
```java
public class UpgradeJavaDisplayPageInfoItemCapabilityCheck {

	@Reference
	private DisplayPageInfoItemCapability _displayPageInfoItemCapability;

}
```

**EXPECTED OUTPUT:**
```java
import com.liferay.info.item.capability.InfoItemCapability;

public class UpgradeJavaDisplayPageInfoItemCapabilityCheck {

	@Reference(
		target = "(info.item.capability.key=" + DisplayPageInfoItemCapability.KEY + ")"
	)
	private InfoItemCapability _displayPageInfoItemCapability;

}
```

---

## 4. UpgradeJavaGetFileMethodCheck

**Author:** Tamyris Bernardo
**What it does:** Replaces `DLFileEntryLocalService.getFile()` / `DLFileEntryLocalServiceUtil.getFile()` calls with `getFileAsStream()` + `FileUtil.createTempFile(inputStream)`. Adds the `FileUtil` import.

**INPUT:**
```java
import com.liferay.document.library.kernel.service.DLFileEntryLocalService;
import com.liferay.document.library.kernel.service.DLFileEntryLocalServiceUtil;

public class UpgradeJavaGetFileMethodCheck {

	public void method() {

		// Test local service class method

		File file = _dlFileEntryLocalService.getFile(
			fileEntryId, fileEntryVersion, true);

		// Test utility class method

		return DLFileEntryLocalServiceUtil.getFile(
			fileEntryId, fileEntryVersion, true);
	}

	@Reference
	private DLFileEntryLocalService _dlFileEntryLocalService;

}
```

**EXPECTED OUTPUT:**
```java
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.document.library.kernel.service.DLFileEntryLocalService;
import com.liferay.document.library.kernel.service.DLFileEntryLocalServiceUtil;

public class UpgradeJavaGetFileMethodCheck {

	public void method() {

		// Test local service class method

		InputStream inputStream = _dlFileEntryLocalService.getFileAsStream(fileEntryId, fileEntryVersion, true);

		File file = FileUtil.createTempFile(inputStream);

		// Test utility class method

		InputStream inputStream = DLFileEntryLocalServiceUtil.getFileAsStream(fileEntryId, fileEntryVersion, true);

		return FileUtil.createTempFile(inputStream);
	}

	@Reference
	private DLFileEntryLocalService _dlFileEntryLocalService;

}
```

---

## 5. UpgradeJavaGetLayoutDisplayPageObjectProviderCheck

**Author:** Michael Cavalcanti
**What it does:** Replaces `getLayoutDisplayPageObjectProvider(classPK)` calls with `getLayoutDisplayPageObjectProvider(infoItemReference)`. Inserts an `InfoItemReference` construction before each call site. When the class name cannot be resolved it inserts a `TO_BE_REPLACED_FOR_CLASSNAME` placeholder. Adds the `InfoItemReference` import.

**INPUT:**
```java
import com.liferay.layout.display.page.LayoutDisplayPageObjectProvider;
import com.liferay.layout.display.page.LayoutDisplayPageProvider;
import com.liferay.layout.display.page.LayoutDisplayPageProviderRegistry;

public class UpgradeJavaGetLayoutDisplayPageObjectProviderCheck {

	public long getDefaultLayoutPageTemplateEntryId(
			String className, long classPK, JournalArticle journalArticle)
		throws PortalException {

		LayoutDisplayPageProvider layoutDisplayPageProvider =
			_layoutDisplayPageProviderRegistry.
				getLayoutDisplayPageProviderByClassName(className);

		LayoutDisplayPageObjectProvider layoutDisplayPageObjectProvider =
			layoutDisplayPageProvider.getLayoutDisplayPageObjectProvider(
				classPK);

		LayoutPageTemplateEntry defaultAssetDisplayPage =
			_layoutPageTemplateEntryService.fetchDefaultLayoutPageTemplateEntry(
				journalArticle.getGroupId(), _portal.getClassNameId(className),
				layoutDisplayPageObjectProvider.getClassTypeId());

		return defaultAssetDisplayPage.getLayoutPageTemplateEntryId();
	}

	public long getDefaultLayoutPageTemplateEntryId2(
			String className, long classPK, JournalArticle journalArticle)
		throws PortalException {

		LayoutDisplayPageObjectProvider layoutDisplayPageObjectProvider =
			layoutDisplayPageProvider.getLayoutDisplayPageObjectProvider(
				classPK);

		// ... (no prior provider lookup — className cannot be resolved)
	}

}
```

**EXPECTED OUTPUT:**
```java
import com.liferay.info.item.InfoItemReference;
import com.liferay.layout.display.page.LayoutDisplayPageObjectProvider;
import com.liferay.layout.display.page.LayoutDisplayPageProvider;
import com.liferay.layout.display.page.LayoutDisplayPageProviderRegistry;

public class UpgradeJavaGetLayoutDisplayPageObjectProviderCheck {

	public long getDefaultLayoutPageTemplateEntryId(
			String className, long classPK, JournalArticle journalArticle)
		throws PortalException {

		LayoutDisplayPageProvider layoutDisplayPageProvider =
			_layoutDisplayPageProviderRegistry.
				getLayoutDisplayPageProviderByClassName(className);

		InfoItemReference infoItemReference = newInfoItemReference(className, classPK);

		LayoutDisplayPageObjectProvider layoutDisplayPageObjectProvider =
			layoutDisplayPageProvider.getLayoutDisplayPageObjectProvider(
				infoItemReference);

		// ...
	}

	public long getDefaultLayoutPageTemplateEntryId2(...) throws PortalException {

		InfoItemReference infoItemReference = newInfoItemReference(TO_BE_REPLACED_FOR_CLASSNAME, classPK);

		LayoutDisplayPageObjectProvider layoutDisplayPageObjectProvider =
			layoutDisplayPageProvider.getLayoutDisplayPageObjectProvider(
				infoItemReference);

		// ...
	}

}
```

---

## 6. UpgradeJavaGetLayoutDisplayPageProviderCheck

**Author:** Michael Cavalcanti
**What it does:** Renames `getLayoutDisplayPageProvider(className)` to `getLayoutDisplayPageProviderByClassName(className)`. No structural changes — just a method rename.

**INPUT:**
```java
public class UpgradeJavaGetLayoutDisplayPageProviderCheck {

	private static LayoutDisplayPageObjectProvider<?>
		_getLayoutDisplayPageObjectProvider(
			InfoItemReference infoItemReference,
			LayoutDisplayPageProviderRegistry layoutDisplayPageProviderRegistry) {

		LayoutDisplayPageProvider<?> layoutDisplayPageProvider =
			layoutDisplayPageProviderRegistry.getLayoutDisplayPageProvider(
				infoItemReference.getClassName());

		return layoutDisplayPageProvider.getLayoutDisplayPageObjectProvider(
			infoItemReference);
	}

}
```

**EXPECTED OUTPUT:**
```java
public class UpgradeJavaGetLayoutDisplayPageProviderCheck {

	private static LayoutDisplayPageObjectProvider<?>
		_getLayoutDisplayPageObjectProvider(
			InfoItemReference infoItemReference,
			LayoutDisplayPageProviderRegistry layoutDisplayPageProviderRegistry) {

		LayoutDisplayPageProvider<?> layoutDisplayPageProvider =
			layoutDisplayPageProviderRegistry.getLayoutDisplayPageProviderByClassName(
				infoItemReference.getClassName());

		return layoutDisplayPageProvider.getLayoutDisplayPageObjectProvider(
			infoItemReference);
	}

}
```

---

## 7. UpgradeJavaPortletIdMethodCheck

**Author:** Tamyris Bernardo
**What it does:** Replaces `document.get(Field.PORTLET_ID)` with `PortletProviderUtil.getPortletId(document.get(Field.ENTRY_CLASS_NAME), PortletProvider.Action.VIEW)`. Adds `PortletProvider` and `PortletProviderUtil` imports.

**INPUT:**
```java
public class UpgradeJavaPortletIdMethodCheck {

	public String method() {
		return document.get(Field.PORTLET_ID);
	}

}
```

**EXPECTED OUTPUT:**
```java
import com.liferay.portal.kernel.portlet.PortletProvider;
import com.liferay.portal.kernel.portlet.PortletProviderUtil;

public class UpgradeJavaPortletIdMethodCheck {

	public String method() {
		return PortletProviderUtil.getPortletId(document.get(Field.ENTRY_CLASS_NAME), PortletProvider.Action.VIEW);
	}

}
```

---

## 8. UpgradeJavaProductDTOConverterReferenceCheck

**Author:** Kyle Miho
**What it does:** Replaces a `@Reference`-injected `ProductDTOConverter` field with a `DTOConverter<CPDefinition, Product>` field filtered by `component.name`. Adds the `DTOConverter`, `CPDefinition`, and `Product` imports.

**INPUT:**
```java
public class UpgradeJavaProductDTOConverterReferenceCheck {

	@Reference
	private ProductDTOConverter _productDTOConverter;

}
```

**EXPECTED OUTPUT:**
```java
import com.liferay.commerce.product.model.CPDefinition;
import com.liferay.headless.commerce.admin.catalog.dto.v1_0.Product;
import com.liferay.portal.vulcan.dto.converter.DTOConverter;

public class UpgradeJavaProductDTOConverterReferenceCheck {

	@Reference(
		target = "(component.name=com.liferay.headless.commerce.machine.learning.internal.dto.v1_0.converter.ProductDTOConverter)"
	)
	private DTOConverter<CPDefinition, Product> _productDTOConverter;

}
```

---

## 9. UpgradeJavaSchedulerEntryImplConstructorCheck

**Author:** Michael Cavalcanti
**What it does:** Fixes `SchedulerEntryImpl` subclass constructors to match the new two-argument signature. Empty / bare `super()` constructors get `super(null, null)`. Constructors delegating to another `SchedulerEntryImpl` expand the fields from that instance. `Trigger` parameters are renamed to `TriggerConfiguration`. Adds the `TriggerConfiguration` import.

**INPUT:**
```java
public class UpgradeJavaSchedulerEntryImplConstructorCheck
		extends SchedulerEntryImpl {

	public UpgradeJavaSchedulerEntryImplConstructorCheck() {
    }

	public UpgradeJavaSchedulerEntryImplConstructorCheck() {
		super();
	}

	public UpgradeJavaSchedulerEntryImplConstructorCheck(
			SchedulerEntryImpl schedulerEntry) {
		super();
	}

	public UpgradeJavaSchedulerEntryImplConstructorCheck(
			String eventListenerClass, Trigger trigger, String description) {
		super(eventListenerClass, trigger, description);
	}

}
```

**EXPECTED OUTPUT:**
```java
import com.liferay.portal.kernel.scheduler.TriggerConfiguration;

public class UpgradeJavaSchedulerEntryImplConstructorCheck
		extends SchedulerEntryImpl {

	public UpgradeJavaSchedulerEntryImplConstructorCheck() {
		super(null, null);
    }

	public UpgradeJavaSchedulerEntryImplConstructorCheck() {
		super(null, null);
	}

	public UpgradeJavaSchedulerEntryImplConstructorCheck(
			SchedulerEntryImpl schedulerEntry) {
		super(schedulerEntry.getEventListenerClass(), schedulerEntry.getTriggerConfiguration(), schedulerEntry.getDescription());
	}

	public UpgradeJavaSchedulerEntryImplConstructorCheck(
			String eventListenerClass, TriggerConfiguration trigger, String description) {
		super(eventListenerClass, trigger, description);
	}

}
```

---

## 10. UpgradeJavaScreenContributorClassCheck

**Author:** Tamyris Bernardo
**What it does:** Converts a `PortalSettingsConfigurationScreenContributor` implementor into a `ConfigurationScreenWrapper` subclass. The outer class gets a `getConfigurationScreen()` override that delegates to a factory; all original method overrides move into a new private inner class that still implements the contributor interface. Existing `@Reference` fields stay on the outer class so the inner class can access them through the enclosing instance.

**INPUT:**
```java
@Component(service = PortalSettingsConfigurationScreenContributor.class)
public class UpgradeJavaScreenContributorClassCheck
	implements PortalSettingsConfigurationScreenContributor {

	@Override
	public String getCategoryKey() { return "test-category-key"; }

	@Override
	public String getJspPath() { return "/portal_settings/test.jsp"; }

	@Override
	public String getKey() { return "test-key"; }

	@Override
	public String getName(Locale locale) {
		return _language.get(locale, "test-configuration-name");
	}

	@Override
	public String getSaveMVCActionCommandName() {
		return "/test-name/test-command-name";
	}

	@Override
	public ServletContext getServletContext() { return _servletContext; }

	@Override
	public void setAttributes(
		HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse) {
		httpServletRequest.setAttribute(null, null);
	}

	@Reference
	private Language _language;

	@Reference(target = "(osgi.web.symbolicname=com.test.web)")
	private ServletContext _servletContext;

}
```

**EXPECTED OUTPUT:**
```java
import com.liferay.configuration.admin.display.ConfigurationScreen;
import com.liferay.configuration.admin.display.ConfigurationScreenWrapper;
import com.liferay.portal.settings.configuration.admin.display.PortalSettingsConfigurationScreenFactory;

@Component(service = ConfigurationScreen.class)
public class UpgradeJavaScreenContributorClassCheck
	extends ConfigurationScreenWrapper {

	@Override
	protected ConfigurationScreen getConfigurationScreen() {
		return _portalSettingsConfigurationScreenFactory.create(
			new UpgradeJavaClassCheck());
	}

	@Reference
	private PortalSettingsConfigurationScreenFactory _portalSettingsConfigurationScreenFactory;

	@Reference
	private Language _language;

	@Reference(target = "(osgi.web.symbolicname=com.test.web)")
	private ServletContext _servletContext;

	private class UpgradeJavaClassCheck
		implements PortalSettingsConfigurationScreenContributor {

		@Override
		public String getCategoryKey() { return "test-category-key"; }

		@Override
		public String getJspPath() { return "/portal_settings/test.jsp"; }

		@Override
		public String getKey() { return "test-key"; }

		@Override
		public String getName(Locale locale) {
			return _language.get(locale, "test-configuration-name");
		}

		@Override
		public String getSaveMVCActionCommandName() {
			return "/test-name/test-command-name";
		}

		@Override
		public ServletContext getServletContext() { return _servletContext; }

		@Override
		public void setAttributes(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) {
			httpServletRequest.setAttribute(null, null);
		}

	}

}
```

---

## 11. UpgradeJavaServiceReferenceAnnotationCheck

**Author:** Tamyris Bernardo
**What it does:** Replaces Liferay Spring Extender `@ServiceReference(type = X.class)` with OSGi `@Reference`. Swaps the import. The `type =` attribute is dropped because the field type already carries that information under DS.

**INPUT:**
```java
import com.liferay.portal.spring.extender.service.ServiceReference;

public class UpgradeJavaServiceReferenceAnnotationCheck {

	@ServiceReference(
		type = com.liferay.portal.kernel.service.ResourceLocalService.class
	)
	protected com.liferay.portal.kernel.service.ResourceLocalService
		resourceLocalService;

	@ServiceReference(type = FavoriteLocalService.class)
	private FavoriteLocalService _favoriteLocalService;

}
```

**EXPECTED OUTPUT:**
```java
import org.osgi.service.component.annotations.Reference;

public class UpgradeJavaServiceReferenceAnnotationCheck {

	@Reference
	protected com.liferay.portal.kernel.service.ResourceLocalService
		resourceLocalService;

	@Reference
	private FavoriteLocalService _favoriteLocalService;

}
```

---

## 12. UpgradeJavaSortFieldNameTranslatorCheck

**Author:** Kyle Miho
**What it does:** Removes the `ENTRY_CLASS_NAME_PROPERTY_KEY` property from `@Component`, adds an import for the entity class, and inserts a `getEntityClass()` override returning that class literal.

**INPUT:**
```java
@Component(
	property = ContributorConstants.ENTRY_CLASS_NAME_PROPERTY_KEY + "=com.liferay.source.formatter.dependencies.upgrade.model.UpgradeJava",
	service = SortFieldNameTranslator.class
)
public class UpgradeJavaSortFieldNameTranslatorCheck
	implements SortFieldNameTranslator {
}
```

**EXPECTED OUTPUT:**
```java
import com.liferay.source.formatter.dependencies.upgrade.model.UpgradeJava;

@Component(
	service = SortFieldNameTranslator.class
)
public class UpgradeJavaSortFieldNameTranslatorCheck
	implements SortFieldNameTranslator {

	@Override
	public Class<?> getEntityClass() {
		return UpgradeJava.class;
	}

}
```

---

## 13. UpgradeRejectedExecutionHandlerCheck

**Authors:** Nícolas Moura, Tamyris Bernardo
**What it does:** Replaces the Liferay `CallerRunsPolicy` anonymous class with `ThreadPoolExecutor.CallerRunsPolicy` from the JDK. Swaps the `RejectedExecutionHandler` import from `com.liferay.portal.kernel.concurrent` to `java.util.concurrent` and adds the `java.util.concurrent.ThreadPoolExecutor` import.

**INPUT:**
```java
import com.liferay.portal.kernel.concurrent.CallerRunsPolicy;
import com.liferay.portal.kernel.concurrent.RejectedExecutionHandler;

public class UpgradeRejectedExecutionHandlerCheck {

	public void method() {
		RejectedExecutionHandler rejectedExecutionHandler =
			new CallerRunsPolicy() {
			};
	}

}
```

**EXPECTED OUTPUT:**
```java
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.RejectedExecutionHandler;

public class UpgradeRejectedExecutionHandlerCheck {

	public void method() {
		RejectedExecutionHandler rejectedExecutionHandler =
			new ThreadPoolExecutor.CallerRunsPolicy() {
			};
	}

}
```

---

## 14. UpgradeJSPFieldSetGroupCheck

**Author:** Tamyris Bernardo
**What it does:** Removes the `<liferay-frontend:fieldset-group>` wrapper tag entirely, leaving any content that was inside it in place (here the tag was empty).

**INPUT:**
```jsp
<liferay-frontend:fieldset-group>

</liferay-frontend:fieldset-group>
```

**EXPECTED OUTPUT:**
```jsp

```

---

## 15. UpgradePortletFTLCheck

**Author:** Tamyris Bernardo
**What it does:** Updates FTL portlet topper markup: adds `cadmin` to the header CSS class, switches the icon-options macro from `liferay_portlet` to `liferay_frontend` with `direction="right cadmin"`, and prepends a `portletTitleMenu.setDirection("right cadmin")` call before the menu macro.

**INPUT:**
```ftl
<header class="portlet-topper">
	<@liferay_portlet["icon-options"] portletConfigurationIcons=portlet_configuration_icons />

	<@liferay_ui["menu"] menu=portletTitleMenu />
</header>
```

**EXPECTED OUTPUT:**
```ftl
<header class="cadmin portlet-topper">
	<@liferay_frontend["icon-options"] direction="right cadmin" portletConfigurationIcons=portlet_configuration_icons />

	${portletTitleMenu.setDirection("right cadmin")}
	<@liferay_ui["menu"] menu=portletTitleMenu />
</header>
```

---

## 16. UpgradeSCSSMixinsCheck

**Author:** Michael Cavalcanti
**What it does:** Replaces deprecated Liferay responsive SCSS mixins (`lg`, `md`, `sm`, `xs`, `respond-to`, `media-query`) with Bootstrap `media-breakpoint-*` equivalents.

**INPUT:**
```scss
@include lg() {}
@include md() {}
@include media-query(md) {}
@include media-query(null, $screen-xs-max) {}
@include media-query(sm) {}
@include respond-to(desktop) {}
@include respond-to(phone) {}
@include respond-to(tablet) {}
@include respond-to(desktop, tablet) {}
@include respond-to(phone, tablet) {}
@include sm() {}
@include xs() {}
```

**EXPECTED OUTPUT:**
```scss
@include media-breakpoint-up(xl) {}
@include media-breakpoint-up(lg) {}
@include media-breakpoint-up(lg) {}
@include media-query(null, $screen-xs-max) {}
@include media-breakpoint-up(md) {}
@include media-breakpoint-up(lg) {}
@include media-breakpoint-down(sm) {}
@include media-breakpoint-only(md) {}
@include media-breakpoint-up(md) {}
@include media-breakpoint-down(md) {}
@include media-breakpoint-up(md) {}
@include media-breakpoint-up(sm) {}
```

---

## 17. UpgradeSCSSNodeSassPatternsCheck

**Author:** Tamyris Bernardo
**What it does:** Converts SCSS division expressions (`a / b`) to `math.div(a, b)` and updates CSS variable interpolation from `#{$var-#{$key}}` patterns to string-concatenation form. Prepends `@use "sass:math"` when any division is converted.

**INPUT:**
```scss
$control-panel-spacing-mobile: ( $spacer / 1.25 );
$line-height-h1: 50 / 44;
$line-height-h2: 5 / 4;
$multiplier: nth($value, 1) / $spacer;

@each {
	.bg-#{$color} {}
}

@mixin make($list: ".col-xs-#{$i}, .col-sm-#{$i}") {}

(#{$prop}: var(--btn-#{$key}-#{$prefix}#{$prop}))

.col-#{$class}-push-#{$index} {}
.col-#{$class}-push-0 {}
```

**EXPECTED OUTPUT:**
```scss
@use "sass:math";
$control-panel-spacing-mobile: ( math.div($spacer, 1.25) );
$line-height-h1: math.div(50, 44);
$line-height-h2: math.div(5, 4);
$multiplier: math.div(nth($value, 1), $spacer);

@each {
	#{'.bg-' + $color} {}
}

@mixin make($list: "#{'.col-xs-' + $i}, #{'.col-sm-' + $i}") {}

(#{$prop}: var(#{'--btn-' + $key + '-' + $prefix + $prop}))

#{'.col-' + $class + '-push-' + $index} {}
#{'.col-' + $class + '-push-0'} {}
```

---

## 18. XMLUpgradeCompatibilityVersionCheck

**Author:** Michael Cavalcanti
**What it does:** Collapses multiple `<version>` entries in the `<compatibility>` block down to a single entry matching the target Liferay upgrade version.

**INPUT:**
```xml
<?xml version="1.0"?>
<!DOCTYPE look-and-feel PUBLIC "-//Liferay//DTD Look and Feel 7.4.0//EN" "...">

<look-and-feel>
	<compatibility>
		<version>7.1.0+</version>
		<version>7.2.0+</version>
		<version>7.3.0+</version>
	</compatibility>
</look-and-feel>
```

**EXPECTED OUTPUT:**
```xml
<?xml version="1.0"?>
<!DOCTYPE look-and-feel PUBLIC "-//Liferay//DTD Look and Feel 7.4.0//EN" "...">

<look-and-feel>
	<compatibility>
		<version>7.4.0+</version>
	</compatibility>
</look-and-feel>
```

---

## 19. UpgradeBNDDeclarativeServicesCheck

**Author:** Kyle Miho
**What it does:** Adds `-dsannotations-options: inherit` to `bnd.bnd` files that have `Liferay-Service: true` but lack that option, ensuring DS annotations on superclasses are picked up.

**INPUT:**
```
Bundle-Name: Test Declarative Services
Bundle-SymbolicName: com.test.declarative.services
Bundle-Version: 1.0.0
Liferay-Require-SchemaVersion: 1.0.0
Liferay-Service: true
```

**EXPECTED OUTPUT:**
```
Bundle-Name: Test Declarative Services
Bundle-SymbolicName: com.test.declarative.services
Bundle-Version: 1.0.0
Liferay-Require-SchemaVersion: 1.0.0
Liferay-Service: true
-dsannotations-options: inherit
```

---

## 20. XMLUpgradeDeclarativeServicesCheck

**Author:** Kyle Miho
**What it does:** Adds `dependency-injector="ds"` to the `<service-builder>` root element in `service.xml` files that do not already declare it.

**INPUT:**
```xml
<service-builder auto-import-default-references="false" auto-namespace-tables="false"
    mvcc-enabled="true" package-path="com.test.declarative.services">
	<!-- ... -->
</service-builder>
```

**EXPECTED OUTPUT:**
```xml
<service-builder dependency-injector="ds" auto-import-default-references="false"
    auto-namespace-tables="false" mvcc-enabled="true"
    package-path="com.test.declarative.services">
	<!-- ... -->
</service-builder>
```

---

## 21. UpgradeJavaFinderImplCheck

**Author:** Kyle Miho
**What it does:** Extends `BaseAddComponentAnnotationCheck` to add a `@Component(service = XxxFinder.class)` annotation to classes that extend `FinderBaseImpl` and implement a Finder interface. Adds the `@Component` import.

**INPUT:**
```java
package com.liferay.source.formatter.dependencies.upgrade.service.persistence.impl;

public class UpgradeJavaFinderImplCheck
	extends UpgradeJavaFinderBaseImplCheck implements UpgradeJavaFinderCheck {
}
```

**EXPECTED OUTPUT:**
```java
package com.liferay.source.formatter.dependencies.upgrade.service.persistence.impl;

import org.osgi.service.component.annotations.Component;

@Component(service = UpgradeJavaFinder.class)
public class UpgradeJavaFinderImplCheck
	extends UpgradeJavaFinderBaseImplCheck implements UpgradeJavaFinderCheck {
}
```

---

## 22. UpgradeJavaLocalServiceImplCheck

**Author:** Kyle Miho
**What it does:** Extends `BaseAddComponentAnnotationCheck` to add `@Component(property = "model.class.name=...", service = AopService.class)` to `LocalServiceBaseImpl` subclasses. Adds `AopService` and `@Component` imports.

**INPUT:**
```java
package com.liferay.source.formatter.dependencies.upgrade.service.impl;

public class UpgradeJavaLocalServiceImplCheck
	extends UpgradeJavaLocalServiceBaseImplCheck {
}
```

**EXPECTED OUTPUT:**
```java
package com.liferay.source.formatter.dependencies.upgrade.service.impl;

import com.liferay.portal.aop.AopService;
import org.osgi.service.component.annotations.Component;

@Component(
	property = "model.class.name=com.liferay.source.formatter.dependencies.upgrade.model.UpgradeJava",
	service = AopService.class
)
public class UpgradeJavaLocalServiceImplCheck
	extends UpgradeJavaLocalServiceBaseImplCheck {
}
```

---

## 23. UpgradeJavaServiceImplCheck

**Author:** Kyle Miho
**What it does:** Extends `BaseAddComponentAnnotationCheck` to add `@Component` with JSON web service context properties (extracted from `service.xml`) to `ServiceBaseImpl` subclasses, registering them as `AopService`. Adds `AopService` and `@Component` imports.

**INPUT:**
```java
package com.liferay.source.formatter.dependencies.upgrade.service.impl;

public class UpgradeJavaServiceImplCheck
	extends UpgradeJavaServiceBaseImplCheck {
}
```

**EXPECTED OUTPUT:**
```java
package com.liferay.source.formatter.dependencies.upgrade.service.impl;

import com.liferay.portal.aop.AopService;
import org.osgi.service.component.annotations.Component;

@Component(
	property = {
		"json.web.service.context.name=upgrade",
		"json.web.service.context.path=UpgradeJava"
	},
	service = AopService.class
)
public class UpgradeJavaServiceImplCheck
	extends UpgradeJavaServiceBaseImplCheck {
}
```

---

## 24. UpgradeJavaFDSDataProviderCheck — Commerce variant

**Authors:** Kyle Miho, Michael Cavalcanti
**What it does:** Migrates `CommerceDataSetDataProvider` to `FDSDataProvider`. Renames the component property key from `commerce.data.provider.key` to `fds.data.provider.key`, updates the service interface and implements declaration, replaces `Filter`/`Pagination` parameters with `FDSKeywords`/`FDSPagination`, renames `countItems` to `getItemsCount`. Swaps all imports.

**INPUT:**
```java
import com.liferay.commerce.frontend.CommerceDataSetDataProvider;
import com.liferay.frontend.taglib.clay.data.Filter;
import com.liferay.frontend.taglib.clay.data.Pagination;

@Component(
	enabled = false, immediate = true,
	property = "commerce.data.provider.key=" + UpgradeJavaDataSetConstants.TEST_CONSTANT,
	service = CommerceDataSetDataProvider.class
)
public class UpgradeJavaCommerceDataSetDataProviderCheck
	implements CommerceDataSetDataProvider<UpgradeJava> {

	@Override
	public List getItems(HttpServletRequest httpServletRequest, Filter filter,
			Pagination pagination, Sort sort) throws PortalException {
		return _original.getItems(httpServletRequest, filter, pagination, sort);
	}

	@Override
	public int countItems(HttpServletRequest httpServletRequest, Filter filter)
		throws PortalException {
		return _original.countItems(httpServletRequest, filter);
	}

	private FDSDataProvider<?> _original;

}
```

**EXPECTED OUTPUT:**
```java
import com.liferay.frontend.data.set.provider.FDSDataProvider;
import com.liferay.frontend.data.set.provider.search.FDSKeywords;
import com.liferay.frontend.data.set.provider.search.FDSPagination;

@Component(
	enabled = false, immediate = true,
	property = "fds.data.provider.key=" + UpgradeJavaDataSetConstants.TEST_CONSTANT,
	service = FDSDataProvider.class
)
public class UpgradeJavaCommerceDataSetDataProviderCheck
	implements FDSDataProvider<UpgradeJava> {

	@Override
	public List getItems(FDSKeywords fDSKeywords, FDSPagination fDSPagination,
			HttpServletRequest httpServletRequest, Sort sort) throws PortalException {
		return _original.getItems(fDSKeywords, fDSPagination, httpServletRequest, sort);
	}

	@Override
	public int getItemsCount(FDSKeywords fDSKeywords, HttpServletRequest httpServletRequest)
		throws PortalException {
		return _original.getItemsCount(fDSKeywords, httpServletRequest);
	}

	private FDSDataProvider<?> _original;

}
```

---

## 25. UpgradeJavaFDSDataProviderCheck — Clay variant

**Authors:** Kyle Miho, Michael Cavalcanti
**What it does:** Same migration as above but starting from `ClayDataSetDataProvider`. Renames `clay.data.provider.key` to `fds.data.provider.key`, replaces interface and parameter types identically. Swaps all imports.

**INPUT:**
```java
import com.liferay.frontend.taglib.clay.data.set.provider.ClayDataSetDataProvider;
import com.liferay.frontend.taglib.clay.data.Filter;
import com.liferay.frontend.taglib.clay.data.Pagination;

@Component(
	enabled = false, immediate = true,
	property = "clay.data.provider.key=" + UpgradeJavaDataSetConstants.TEST_CONSTANT,
	service = ClayDataSetDataProvider.class
)
public class UpgradeJavaClayDataSetDataProviderCheck
	implements ClayDataSetDataProvider<UpgradeJava> {

	@Override
	public List getItems(HttpServletRequest httpServletRequest, Filter filter,
			Pagination pagination, Sort sort) throws PortalException {
		return _original.getItems(httpServletRequest, filter, pagination, sort);
	}

	@Override
	public int getItemsCount(HttpServletRequest httpServletRequest, Filter filter)
		throws PortalException {
		return _original.getItemsCount(httpServletRequest, filter);
	}

	private FDSDataProvider<?> _original;

}
```

**EXPECTED OUTPUT:**
```java
import com.liferay.frontend.data.set.provider.FDSDataProvider;
import com.liferay.frontend.data.set.provider.search.FDSKeywords;
import com.liferay.frontend.data.set.provider.search.FDSPagination;

@Component(
	enabled = false, immediate = true,
	property = "fds.data.provider.key=" + UpgradeJavaDataSetConstants.TEST_CONSTANT,
	service = FDSDataProvider.class
)
public class UpgradeJavaClayDataSetDataProviderCheck
	implements FDSDataProvider<UpgradeJava> {

	@Override
	public List getItems(FDSKeywords fDSKeywords, FDSPagination fDSPagination,
			HttpServletRequest httpServletRequest, Sort sort) throws PortalException {
		return _original.getItems(fDSKeywords, fDSPagination, httpServletRequest, sort);
	}

	@Override
	public int getItemsCount(FDSKeywords fDSKeywords, HttpServletRequest httpServletRequest)
		throws PortalException {
		return _original.getItemsCount(fDSKeywords, httpServletRequest);
	}

	private FDSDataProvider<?> _original;

}
```

---

## 26. UpgradeSetResultsSetTotalMethodCheck — Java

**Author:** Tamyris Bernardo
**What it does:** Consolidates `SearchContainer` method calls. `setResults(x)` alone becomes `setResultsAndTotal(x)`. `setResults(x)` + `setTotal(n)` together become `setResultsAndTotal(() -> x, n)` wrapped in a try/catch. A lone `setTotal(n)` becomes `setResultsAndTotal(searchContainer::getResults, n)`.

**INPUT:**
```java
public class UpgradeJavaSetResultsSetTotalMethodCheck {

	public SearchContainer<User> setResults(List<User> results) {
		SearchContainer<Entry> searchContainer = new SearchContainer();
		searchContainer.setResults(results);
		return searchContainer;
	}

	public SearchContainer<User> setResultsAndTotal(List<User> results, int total) {
		SearchContainer<Entry> searchContainer = new SearchContainer();
		searchContainer.setResults(results);
		searchContainer.setTotal(total);
		return searchContainer;
	}

	public SearchContainer<User> setTotal(int total) {
		SearchContainer<Entry> searchContainer = new SearchContainer();
		searchContainer.setTotal(total);
		return searchContainer;
	}

}
```

**EXPECTED OUTPUT:**
```java
public class UpgradeJavaSetResultsSetTotalMethodCheck {

	public SearchContainer<User> setResults(List<User> results) {
		SearchContainer<Entry> searchContainer = new SearchContainer();
		searchContainer.setResultsAndTotal(results);
		return searchContainer;
	}

	public SearchContainer<User> setResultsAndTotal(List<User> results, int total) {
		SearchContainer<Entry> searchContainer = new SearchContainer();
		try {
			searchContainer.setResultsAndTotal(() -> results, total);
		}
		catch (Exception exception) {
			throw new PortalException(exception);
		}
		return searchContainer;
	}

	public SearchContainer<User> setTotal(int total) {
		SearchContainer<Entry> searchContainer = new SearchContainer();
		searchContainer.setResultsAndTotal(searchContainer::getResults, total);
		return searchContainer;
	}

}
```

---

## 27. UpgradeSetResultsSetTotalMethodCheck — JSP

**Author:** Tamyris Bernardo
**What it does:** Same consolidation as above applied to JSP scriptlet blocks.

**INPUT:**
```jsp
<%
SearchContainer<Entry> searchContainer = new SearchContainer();

searchContainer.setResults(resultsParameter);
searchContainer.setTotal(totalParameter);
%>
```

**EXPECTED OUTPUT:**
```jsp
<%
SearchContainer<Entry> searchContainer = new SearchContainer();

try {
	searchContainer.setResultsAndTotal(() -> resultsParameter, totalParameter);
}
catch (Exception exception) {
	throw new PortalException(exception);
}
%>
```

---

## 28. UpgradeSetResultsSetTotalMethodCheck — JSPF

**Author:** Tamyris Bernardo
**What it does:** Identical transformation applied to `.jspf` fragment files, confirming the check handles both JSP and JSPF file types.

**INPUT:**
```jspf
<%
SearchContainer<Entry> searchContainer = new SearchContainer();

searchContainer.setResults(resultsParameter);
searchContainer.setTotal(totalParameter);
%>
```

**EXPECTED OUTPUT:**
```jspf
<%
SearchContainer<Entry> searchContainer = new SearchContainer();

try {
	searchContainer.setResultsAndTotal(() -> resultsParameter, totalParameter);
}
catch (Exception exception) {
	throw new PortalException(exception);
}
%>
```

---

## 29. UpgradeJavaAssetEntryAssetCategoriesCheck

**Author:** Nícolas Moura
**What it does:** Migrates the removed `AssetCategoryLocalService.{add,delete}AssetEntryAssetCategory(ies)` calls to `AssetEntryAssetCategoryRelLocalService.{add,delete}AssetEntryAssetCategoryRel`. A `List<AssetCategory>` argument becomes a `for (AssetCategory ...)` loop calling the Rel service with `assetCategory.getCategoryId()`; a `long[]` argument becomes a `for (long ...)` loop; single `AssetCategory` / `long` arguments become a single Rel call. Adds a `@Reference AssetEntryAssetCategoryRelLocalService` field (the old `AssetCategoryLocalService` field stays) and the `com.liferay.asset.entry.rel.service.AssetEntryAssetCategoryRelLocalService` import.

**INPUT:**
```java
import com.liferay.asset.kernel.service.AssetCategoryLocalService;

_assetCategoryLocalService.addAssetEntryAssetCategories(entryId, assetCategories);
_assetCategoryLocalService.addAssetEntryAssetCategories(entryId, assetCategoryIds);
_assetCategoryLocalService.addAssetEntryAssetCategory(entryId, assetCategory);
_assetCategoryLocalService.addAssetEntryAssetCategory(entryId, assetCategoryId);
// delete* variants are identical with the delete prefix

@Reference
private AssetCategoryLocalService _assetCategoryLocalService;
```

**EXPECTED OUTPUT:**
```java
import com.liferay.asset.entry.rel.service.AssetEntryAssetCategoryRelLocalService;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;

for (AssetCategory assetCategory : assetCategories) {
	_assetEntryAssetCategoryRelLocalService.addAssetEntryAssetCategoryRel(
		entryId, assetCategory.getCategoryId());
}

for (long assetCategoryId : assetCategoryIds) {
	_assetEntryAssetCategoryRelLocalService.addAssetEntryAssetCategoryRel(
		entryId, assetCategoryId);
}

_assetEntryAssetCategoryRelLocalService.addAssetEntryAssetCategoryRel(
	entryId, assetCategory.getCategoryId());

_assetEntryAssetCategoryRelLocalService.addAssetEntryAssetCategoryRel(
	entryId, assetCategoryId);

@Reference
private AssetCategoryLocalService _assetCategoryLocalService;

@Reference
private AssetEntryAssetCategoryRelLocalService
	_assetEntryAssetCategoryRelLocalService;
```

---

## 30. UpgradeJavaBaseModelListenerCheck

**Authors:** Kyle Miho, Michael Cavalcanti
**What it does:** For classes extending `BaseModelListener<T>`, the `onAfterUpdate` / `onBeforeUpdate` overrides gained a leading `original` parameter. Rewrites the method signature `(t model)` → `(t originalModel, t model)`. In `super.onAfterUpdate(...)` / `super.onBeforeUpdate(...)` calls: when the enclosing method already has the `originalModel` parameter, pass `(originalModel, model)`; when it does not (the call sits inside a different lifecycle method, e.g. `onAfterCreate`), pass `((t)model.clone(), model)`. `onAfterCreate` / `onAfterRemove` signatures are left untouched.

**INPUT:**
```java
public class Foo extends BaseModelListener<t> {

	@Override
	public void onAfterCreate(t model) {
		super.onAfterUpdate(model);
	}

	@Override
	public void onAfterUpdate(t model) {
		super.onAfterUpdate(model);
	}

	@Override
	public void onBeforeUpdate(t model) {
		super.onBeforeUpdate(model);
	}

}
```

**EXPECTED OUTPUT:**
```java
public class Foo extends BaseModelListener<t> {

	@Override
	public void onAfterCreate(t model) {
		super.onAfterUpdate((t)model.clone(), model);
	}

	@Override
	public void onAfterUpdate(t originalModel, t model) {
		super.onAfterUpdate(originalModel, model);
	}

	@Override
	public void onBeforeUpdate(t originalModel, t model) {
		super.onBeforeUpdate(originalModel, model);
	}

}
```

---

## 31. UpgradeJavaDDMFormValuesSerializerTrackerCheck

**Author:** Nícolas Moura
**What it does:** Removes the deprecated `DDMFormValuesSerializerTracker` indirection. Deletes the `@Reference`-annotated `setDDMFormValuesSerializerTracker(...)` setter and the `_ddmFormValuesSerializerTracker` field, and the `DDMFormValuesSerializerTracker` import. Replaces the `_ddmFormValuesSerializerTracker.getDDMFormValuesSerializer("json")` lookup with a directly-injected `@Reference(target = "(ddm.form.values.serializer.type=json)") private DDMFormValuesSerializer _ddmFormValuesSerializer;`, rewriting the use site to call `_ddmFormValuesSerializer` directly.

**INPUT:**
```java
import com.liferay.dynamic.data.mapping.io.DDMFormValuesSerializer;
import com.liferay.dynamic.data.mapping.io.DDMFormValuesSerializerTracker;

DDMFormValuesSerializer ddmFormValuesSerializer =
	_ddmFormValuesSerializerTracker.getDDMFormValuesSerializer("json");

DDMFormValuesSerializerSerializeResponse ddmFormValuesSerializerSerializeResponse =
	ddmFormValuesSerializer.serialize(builder.build());

@Reference(unbind = "-")
protected void setDDMFormValuesSerializerTracker(
	DDMFormValuesSerializerTracker ddmFormValuesSerializerTracker) {

	_ddmFormValuesSerializerTracker = ddmFormValuesSerializerTracker;
}

private DDMFormValuesSerializerTracker _ddmFormValuesSerializerTracker;
```

**EXPECTED OUTPUT:**
```java
import com.liferay.dynamic.data.mapping.io.DDMFormValuesSerializer;

DDMFormValuesSerializerSerializeResponse ddmFormValuesSerializerSerializeResponse =
	_ddmFormValuesSerializer.serialize(builder.build());

@Reference(target = "(ddm.form.values.serializer.type=json)")
private DDMFormValuesSerializer _ddmFormValuesSerializer;
```

---

## 32. UpgradeJavaFacetedSearcherCheck

**Author:** Nícolas Moura
**What it does:** Replaces the removed `FacetedSearcher.getInstance()` factory call. The `Indexer<?> indexer = FacetedSearcher.getInstance();` assignment becomes `FacetedSearcher facetedSearcher = FacetedSearcherManagerUtil.createFacetedSearcher();`, all references to the old variable are renamed to `facetedSearcher`, and the `com.liferay.portal.kernel.search.facet.faceted.searcher.FacetedSearcherManagerUtil` import is added.

**INPUT:**
```java
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.SearchContext;

Indexer<?> indexer = FacetedSearcher.getInstance();

return indexer.search(searchContext);
```

**EXPECTED OUTPUT:**
```java
import com.liferay.portal.kernel.search.facet.faceted.searcher.FacetedSearcherManagerUtil;

import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.SearchContext;

FacetedSearcher facetedSearcher = FacetedSearcherManagerUtil.createFacetedSearcher();

return facetedSearcher.search(searchContext);
```

---

## 33. UpgradeJavaGetFDSTableSchemaParameterCheck

**Author:** Albert Gomes Cabral
**What it does:** For classes extending `BaseTableFDSView`, adds a `Locale locale` parameter to the now-parameterized `getFDSTableSchema()` override (and the `java.util.Locale` import). Collapses the imperative pattern where a `FDSTableSchemaField` local is captured from `fDSTableSchemaBuilder.add(...)` and then mutated via `setContentRenderer(...)` / `setSortable(...)` into a single fluent `add(name, label, fdsTableSchemaField -> fdsTableSchemaField.setContentRenderer(...).setSortable(...))` lambda call. Plain `add(name, label)` calls with no follow-up mutation are left as-is.

**INPUT:**
```java
@Override
public FDSTableSchema getFDSTableSchema() {
	FDSTableSchemaField invoiceIdFDSTableSchemaField =
		fDSTableSchemaBuilder.add("invoiceId", "invoice-id-field");

	invoiceIdFDSTableSchemaField.setContentRenderer("link");
	invoiceIdFDSTableSchemaField.setSortable(true);

	fDSTableSchemaBuilder.add("createdBy", "invoice-created-by");

	return fDSTableSchemaBuilder.build();
}
```

**EXPECTED OUTPUT:**
```java
import java.util.Locale;

@Override
public FDSTableSchema getFDSTableSchema(Locale locale) {
	fDSTableSchemaBuilder.add(
		"invoiceId", "invoice-id-field",
		fdsTableSchemaField -> fdsTableSchemaField.setContentRenderer("link")
		.setSortable(true));

	fDSTableSchemaBuilder.add("createdBy", "invoice-created-by");

	return fDSTableSchemaBuilder.build();
}
```

---

## 34. UpgradeJavaStorageTypeAwareCheck

**Author:** Tamyris Bernardo
**What it does:** Removes the no-longer-needed `StorageTypeAware` marker interface from the `implements` clause and strips the `@Override` annotation from the `getStorageType()` method. The method body itself is kept (the class still declares `getStorageType()`, it just no longer overrides an interface method).

**INPUT:**
```java
public class Foo
	extends SchedulerEntryImpl implements SchedulerEntry, StorageTypeAware {

	@Override
	public StorageType getStorageType() {
		return _storageType;
	}

}
```

**EXPECTED OUTPUT:**
```java
public class Foo
	extends SchedulerEntryImpl implements SchedulerEntry {

	public StorageType getStorageType() {
		return _storageType;
	}

}
```
