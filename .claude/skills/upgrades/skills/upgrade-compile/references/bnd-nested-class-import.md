# bnd 6.4 nested-class import misparse

## Symptom

A bundle fails OSGi resolution with:

```
Unresolved requirement: Import-Package: java.util.Map
```

`java.util.Map` is a CLASS, not a package — no bundle exports a package by that name. The framework's boot delegation can't satisfy it (boot delegation works at the package prefix level; `java.util.Map` isn't a real package). The bundle stays in `Installed` state.

## Cause

bnd 6.4 (embedded in Liferay Workspace Plugin 15.x → 17.x via `biz.aQute.bndlib`) splits any Java import statement on the last `.` to derive a package + class pair. For a nested-class import like `import java.util.Map.Entry;`, bnd reads it as:

- package: `java.util.Map`
- class: `Entry`

and adds `java.util.Map` to the bundle's `Import-Package` manifest entry. The same misparse happens for any `import outer.package.OuterClass.InnerClass;` form.

The bug is present in both Java source analysis AND JSP page-directive analysis (`<%@ page import="..." %>` in `META-INF/resources/**/*.jsp`).

## Detection

Grep all custom modules:

```bash
grep -rnE "import .*\.[A-Z][A-Za-z0-9_]*\.[A-Z]" modules/ --include="*.java"
grep -rnE "page import=\"[^\"]*\.[A-Z][A-Za-z0-9_]*\.[A-Z]" modules/ --include="*.jsp" --include="*.jspf"
```

Both patterns match `Outer.Inner` style nested imports. `java.util.Map.Entry` is the most common in Liferay customizations; less common are inner classes of `AbstractMap`, `WeakHashMap`, etc.

## Fix recipe

Per file: drop the nested import, ensure the outer class is imported, and reference the inner class via `Outer.Inner` inline.

**Java source:**

```diff
 import java.util.HashMap;
 import java.util.Map;
-import java.util.Map.Entry;

-for (Entry<K, V> e : map.entrySet()) { ... }
+for (Map.Entry<K, V> e : map.entrySet()) { ... }

-Function<Entry<K, V>, String> f = ...;
+Function<Map.Entry<K, V>, String> f = ...;

-.collect(Collectors.toMap(keyFn, Entry::getValue));
+.collect(Collectors.toMap(keyFn, Map.Entry::getValue));
```

**JSP page directive:**

```diff
 <%@ page import="java.util.Map" %>
-<%@ page import="java.util.Map.Entry" %>

-for (Entry<String, String> entry : ...) { ... }
+for (Map.Entry<String, String> entry : ...) { ... }
```

The outer-class import (`java.util.Map`) usually already exists. If not, add it.

## Groovy and other source types

`.groovy` files inside `src/main/java` are NOT analyzed by bnd **unless** the module's `build.gradle` applies the Groovy plugin. Check for `apply plugin: "groovy"` or equivalent; if absent, the `.groovy` ships as a resource and never reaches bnd's classpath analysis. The `Map.Entry` import in a `.groovy` script that isn't compiled is safe to leave alone.

## Commit convention

One commit per `(module, file-type)` pattern affected — the granularity rule from the `commit` skill. Subject:

```
<TICKET> <module>: Replace nested 'import outer.X.Y' by 'X.Y' inline reference
```

For JSPs add the file qualifier:

```
<TICKET> <module>: Replace nested '<%@ page import=outer.X.Y %>' by 'X.Y' inline reference in <file>.jsp
```

No `#automation` label — bnd does this consistently across all clients on this stack; the fix is mechanical and ought to be cataloged in source-formatter (it isn't yet, and that's the gap worth flagging in the PR description rather than per commit).
