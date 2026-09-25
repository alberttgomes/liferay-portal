# Known code changes — Claude-automated patterns

This file documents code changes that cannot be handled by the Source Formatter
(SF) but can be applied by Claude during the upgrade automation phase.

Each entry classifies the case as:
- **autonomous** — Claude applies without confirmation, logs the change
- **flag** — Claude applies the documented transformation autonomously (using the documented default for any value or variable name that must be chosen), then records it under "Flagged for human review" in `upgrade-state.md` tagged `[applied — verify]` — **no `#automation`**. Claude does **not** stop and ask: this is a change we already know cannot be SF-automated but for which we have a documented, deterministic fix, so the flag (not a prompt) is the safety net — a human verifies it at PR time (the customer may extend the touched class, or a defaulted business value may need correcting). This replaces the former "confirm" (stop-and-ask) class.
- **stub** — Claude adds a stub implementation and flags `[not applied — resolve]`; a human must fill in the business logic. This one is *not* autonomous — the documented fix is incomplete by design.

Entries are organized by the Liferay version from which the change originates.

---

## From 6.1

### hibernate-entitymanager version bump

| | |
|---|---|
| **File** | `build.gradle` |
| **Autonomy** | autonomous |
| **Ticket** | LPS-159684 |

**Detection:** line contains `org.hibernate` + `hibernate-entitymanager` + `3.3.2.GA`

**Transformation:**

```groovy
// Before
compile group: "org.hibernate", name: "hibernate-entitymanager", version: "3.3.2.GA"

// After
compile group: "org.hibernate", name: "hibernate-entitymanager", version: "3.4.0.GA"
```

---

### TCK_URL check removal

| | |
|---|---|
| **File** | `*.java` |
| **Autonomy** | autonomous |
| **Ticket** | LPS-159680 |

**Detection:** `if` condition containing both `SESSION_ENABLE_PERSISTENT_COOKIES` and `TCK_URL`

**Transformation:** remove the `|| GetterUtil.getBoolean(PropsUtil.get(PropsKeys.TCK_URL))` clause entirely, keeping the surrounding `if` intact.

```java
// Before
if (!GetterUtil.getBoolean(PropsUtil.get(PropsKeys.SESSION_ENABLE_PERSISTENT_COOKIES))
        || GetterUtil.getBoolean(PropsUtil.get(PropsKeys.TCK_URL))) {

// After
if (!GetterUtil.getBoolean(PropsUtil.get(PropsKeys.SESSION_ENABLE_PERSISTENT_COOKIES))) {
```

---

### DLFileEntryLocalServiceUtil.addFileEntry signature change

| | |
|---|---|
| **File** | `*.java` |
| **Autonomy** | flag |

**Detection:** call to `addFileEntry` on `DLFileEntryLocalServiceUtil` with the old 13-parameter signature (ending in `file, is, size, serviceContext`).

**What changes:**
- Import: `com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil` → `com.liferay.document.library.kernel.service.DLFileEntryLocalServiceUtil`
- New parameters added: `externalReferenceCode` (String, first), `urlTitle` (String, after `title`), `expirationDate` (Date, before `serviceContext`), `reviewDate` (Date, before `serviceContext`)
- `fieldsMap` type changed: `Map<String, Fields>` → `Map<String, DDMFormValues>`
- Parameter `is` renamed to `inputStream`

**Applied autonomously; flagged for verification because:** the values for `externalReferenceCode`, `urlTitle`, `expirationDate`, and `reviewDate` must be determined from the business context. Claude will propose `null` as default for new parameters and flag for review.

```java
// Before
DLFileEntryLocalServiceUtil.addFileEntry(
    userId, groupId, repositoryId, folderId,
    sourceFileName, mimeType, title, description,
    changeLog, fileEntryTypeId, fieldsMap,
    file, is, size, serviceContext);

// After (proposed — confirm values of new params)
DLFileEntryLocalServiceUtil.addFileEntry(
    null, userId, groupId, repositoryId, folderId,
    sourceFileName, mimeType, title, null, description,
    changeLog, fileEntryTypeId, ddmFormValuesMap,
    file, inputStream, size, null, null, serviceContext);
```

---

### DLFileEntryLocalServiceUtil.updateFileEntry signature change

| | |
|---|---|
| **File** | `*.java` |
| **Autonomy** | flag |

**Detection:** call to `updateFileEntry` on `DLFileEntryLocalServiceUtil` with the old 14-parameter signature containing `boolean majorVersion`.

**What changes:**
- Import same as above
- `majorVersion` (boolean) removed
- New parameters added: `urlTitle` (String), `dlVersionNumberIncrease` (DLVersionNumberIncrease), `expirationDate` (Date), `reviewDate` (Date)
- `fieldsMap` type changed: `Map<String, Fields>` → `Map<String, DDMFormValues>`
- Parameter `is` renamed to `inputStream`

**Applied autonomously; flagged for verification because:** the value of `dlVersionNumberIncrease` requires case-by-case analysis. Claude will default to `DLVersionNumberIncrease.NONE` and flag for review.

```java
// Before
DLFileEntryLocalServiceUtil.updateFileEntry(
    userId, fileEntryId, sourceFileName,
    mimeType, title, description, changeLog,
    majorVersion, fileEntryTypeId, fieldsMap,
    file, is, size, serviceContext);

// After (proposed — confirm dlVersionNumberIncrease value)
DLFileEntryLocalServiceUtil.updateFileEntry(
    userId, fileEntryId, sourceFileName,
    mimeType, title, null, description, changeLog,
    DLVersionNumberIncrease.NONE, fileEntryTypeId, ddmFormValuesMap,
    file, inputStream, size, null, null, serviceContext);
```

---

## From 7.1

### schedulerEngineHelper.unschedule signature change

| | |
|---|---|
| **File** | `*.java` |
| **Autonomy** | flag |

**Detection:** call to `_schedulerEngineHelper.unschedule(_schedulerEntryImpl, getStorageType())` — two-argument form.

**What changes:** the method now requires `trigger.getJobName()` and `trigger.getGroupName()` as first two arguments. Claude must locate the `Trigger` field in the class to determine the variable name.

```java
// Before
_schedulerEngineHelper.unschedule(_schedulerEntryImpl, getStorageType());

// After
_schedulerEngineHelper.unschedule(
    trigger.getJobName(), trigger.getGroupName(),
    getStorageType());
```

**Applied autonomously; flagged for verification because:** the `Trigger` variable name must be resolved from the class context. Claude will inspect the class fields for a `Trigger` type declaration and use that variable name.

---

### BaseMessageListener → SchedulerJobConfiguration

| | |
|---|---|
| **File** | `*.java` |
| **Autonomy** | flag |

**Detection:** class that `extends BaseMessageListener` and uses `_schedulerEngineHelper.register(...)` in an `@Activate` method.

**What changes:** structural refactoring of the entire class —
- Class declaration: `extends BaseMessageListener` → `implements SchedulerJobConfiguration`
- Add `@Component(immediate = true, service = SchedulerJobConfiguration.class)`
- Remove `@Activate` method with `_schedulerEngineHelper.register()`
- Remove `@Deactivate` method with `_schedulerEngineHelper.unregister()`
- Move logic from `doReceive(Message message)` into `getJobExecutorUnsafeRunnable()` as a lambda
- Add `getTriggerConfiguration()` method using `TriggerConfiguration.createTriggerConfiguration()`
- Remove `_schedulerEngineHelper` and `_triggerFactory` references

**Applied autonomously; flagged for verification because:** this is a structural class rewrite. Claude applies the full documented transformation and flags it `[applied — verify]`; a human confirms it at PR time (the customer may have customized the listener).

```java
// Before
public class MyListener extends BaseMessageListener {
    @Activate
    protected void activate() { ... }

    @Deactivate
    protected void deactivate() { _schedulerEngineHelper.unregister(this); }

    @Override
    protected void doReceive(Message message) throws Exception { ... }

    @Reference
    private SchedulerEngineHelper _schedulerEngineHelper;

    @Reference
    private TriggerFactory _triggerFactory;
}

// After
@Component(immediate = true, service = SchedulerJobConfiguration.class)
public class MyListener implements SchedulerJobConfiguration {

    @Override
    public UnsafeRunnable<Exception> getJobExecutorUnsafeRunnable() {
        return () -> { /* logic from doReceive */ };
    }

    @Override
    public TriggerConfiguration getTriggerConfiguration() {
        return TriggerConfiguration.createTriggerConfiguration(24, TimeUnit.HOUR);
    }
}
```

---

## From 7.2

### CommerceShippingEngine — new required method

| | |
|---|---|
| **File** | `*.java` |
| **Autonomy** | stub |

**Detection:** class that `implements CommerceShippingEngine` and does NOT already have `getEnabledCommerceShippingOptionsForOrder`.

**What Claude does:** adds a stub implementation returning an empty list. Human must implement the business logic.

```java
// Stub added by Claude — implement business logic
@Override
public List<CommerceShippingOption> getEnabledCommerceShippingOptions(
        CommerceContext arg0, CommerceOrder arg1, Locale arg2)
    throws CommerceShippingEngineException {

    return new ArrayList<>();
}
```

---

## From 7.3

### mockito-core force flag

| | |
|---|---|
| **File** | `*.gradle` |
| **Autonomy** | autonomous |

**Detection:** `testCompile group: "org.mockito", name: "mockito-core"` without `force = true`

```groovy
// Before
testCompile group: "org.mockito", name: "mockito-core", version: "3.8.0"

// After
testCompile(group: "org.mockito", name: "mockito-core", version: "3.8.0") {
    force = true
}
```

---

### jersey-common missing version

| | |
|---|---|
| **File** | `*.gradle` |
| **Autonomy** | autonomous |

**Detection:** `org.glassfish.jersey.core:jersey-common` dependency without a `version` attribute.

```groovy
// Before
testImplementation group: "org.glassfish.jersey.core", name: "jersey-common"

// After
testImplementation group: "org.glassfish.jersey.core", name: "jersey-common", version: "2.38"
```

---

### mockito-core strictly version

| | |
|---|---|
| **File** | `*.gradle` |
| **Autonomy** | autonomous |

**Detection:** `org.mockito:mockito-core` with `powermock-api-mockito2` in the same `build.gradle` — the strict version constraint is needed to avoid `NoClassDefFoundError`.

```groovy
// Before
testImplementation group: "org.mockito", name: "mockito-core", version: "3.8.0"
testImplementation group: "org.powermock", name: "powermock-api-mockito2", version: "2.0.9"

// After
testImplementation("org.mockito:mockito-core") {
    version {
        strictly("3.8.0")
    }
}
testImplementation group: "org.powermock", name: "powermock-api-mockito2", version: "2.0.9"
```

---

### ConfigurationBeanDeclaration class removal

| | |
|---|---|
| **File** | `*.java` |
| **Autonomy** | flag |

**Detection:** class that `implements ConfigurationBeanDeclaration` and contains ONLY the `getConfigurationBeanClass()` method (no other logic).

**What Claude does:** proposes deletion of the entire file. Requires confirmation before deleting.

> This is a breaking change introduced in Liferay 7.4. The class is no longer needed and its presence causes compile errors.

---

### journalConverter.getDDMFormValues → FieldsToDDMFormValuesConverter

| | |
|---|---|
| **File** | `*.java` |
| **Autonomy** | flag |

**Detection:** call to `journalConverter.getDDMFormValues(ddmStructure, fields)`

**What changes:**
- New import: `com.liferay.dynamic.data.mapping.util.FieldsToDDMFormValuesConverter`
- Replace the method call with retrieval from `HttpServletRequest` attribute + `convert()`

```java
// Before
ddmFormValues = journalConverter.getDDMFormValues(ddmStructure, fields);

// After
FieldsToDDMFormValuesConverter fieldsToDDMFormValuesConverter =
    (FieldsToDDMFormValuesConverter)_httpServletRequest.getAttribute(
        FieldsToDDMFormValuesConverter.class.getName());

_ddmFormValues = fieldsToDDMFormValuesConverter.convert(ddmStructure, fields);
```

**Applied autonomously; flagged for verification because:** the variable name for the result (`ddmFormValues` vs `_ddmFormValues`) and the `HttpServletRequest` variable name must be confirmed from the class context.

---

## From 7.4

### destination.register → BundleContext.registerService

| | |
|---|---|
| **File** | `*.java` |
| **Autonomy** | flag |

**Detection:** use of `destination.register(messageListener)` / `destination.unregister(messageListener)` pattern where `destination` is obtained from `_messageBus.getDestination(...)`.

**What changes:** complete refactoring of how message listeners are registered —
- Add `_bundleContext` field with `@Activate` injection
- Replace `destination.register(listener)` with `_bundleContext.registerService(MessageListener.class, listener, MapUtil.singletonDictionary("destination.name", ...))`
- Replace `destination.unregister(listener)` with `serviceRegistration.unregister()`
- Change `List<ServiceRegistration<Destination>>` to `List<ServiceRegistration<?>>`
- New imports: `MapUtil`, `BundleContext`, `ServiceRegistration`, `MessageListener`, `Activate`

**Applied autonomously; flagged for verification because:** this is the most structurally complex change on this list. Claude applies the full documented transformation and flags it `[applied — verify]`; the `destination.getName()` value and the `ServiceRegistration` storage strategy must be verified per class at PR time.

```java
// Before
Destination destination = _messageBus.getDestination(
    DestinationNames.BACKGROUND_TASK_STATUS);

destination.register(messageListener);
...
destination.unregister(messageListener);

@Reference
private MessageBus _messageBus;

// After
@Activate
protected void activate(BundleContext bundleContext) {
    _bundleContext = bundleContext;
}

ServiceRegistration<MessageListener> serviceRegistration =
    _bundleContext.registerService(
        MessageListener.class, messageListener,
        MapUtil.singletonDictionary(
            "destination.name",
            DestinationNames.BACKGROUND_TASK_STATUS));
...
serviceRegistration.unregister();

private BundleContext _bundleContext;
```

---

## Cases NOT automated (even by Claude)

| Case | Reason |
|------|--------|
| `CommerceShippingEngine.getEnabledCommerceShippingOptionsForOrder` content | Business logic — Claude adds the stub but cannot implement the shipping logic |
| Liferay workspace version upgrade | Requires knowing the latest version for the target — external data |