# bnd: downgrade POI/commons-io MRJAR `module-info.class` errors to warnings

## When this applies

`compileJava` passes but `:jar` (bnd) fails with an error similar to:

```
> Task :modules:<your-module>:jar FAILED

Bundle has errors:
  Found classes in module-info or unsupported version at META-INF/versions/9/module-info.class
  Found classes in module-info or unsupported version at META-INF/versions/9/...
```

The failing classpath entry is typically Apache POI (`poi`, `poi-ooxml`) or Apache Commons IO at version ≥ 5.x — these are published as Multi-Release JARs and ship a `META-INF/versions/9/module-info.class` for JPMS support. bnd 6.x interprets that file as a class in an unsupported class-file version and refuses to package the bundle.

This is a packaging-time error, not a code or runtime problem. POI and commons-io work correctly at runtime — the JPMS `module-info.class` is just metadata.

## Fix

Append `-fixupmessages` to the module's `bnd.bnd` to downgrade the error to a warning:

```properties
-fixupmessages:\
    "Found classes in module-info or unsupported version";\
        is:=warning
```

The `is:=warning` directive tells bnd to keep emitting the message but not fail the build. The bundle JAR is produced correctly; the MRJAR class-file entries are silently passed through.

Existing `-fixupmessages` entries can be appended to — bnd merges multiple instructions via comma-separated lists on continuation lines.

## Verification

Re-run the build:

```bash
<gw> :modules:<your-module>:clean :modules:<your-module>:jar
```

The original error should now print as a `Warning:` line and the bundle should be packaged.

## Commit convention

Per-module — one commit per affected module:

```
<TICKET> <module>: Add -fixupmessages in bnd.bnd to downgrade POI/commons-io MRJAR module-info error to warning
```

No labels needed: this is workspace build configuration, not an API substitution.

## Scope

Common targets in 7.x customizations:
- Web modules embedding POI for `.xlsx` / `.docx` export
- Content-migrator-style modules using commons-io 2.11+
- Any module with a transitive dep on a 5.x+ JPMS-aware Apache Commons library

When the same MRJAR error surfaces across many modules in the same workspace, consider promoting `-fixupmessages` to a workspace-level bnd defaults file rather than repeating per module.
