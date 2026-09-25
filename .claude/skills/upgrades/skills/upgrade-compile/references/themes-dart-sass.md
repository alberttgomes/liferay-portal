# Themes: migrate from `node-sass` to `dart-sass`

## When this applies

A 7.x → 2026.Qx upgrade where the workspace has Gulp-based themes (under `themes/`) that fail at `yarnInstall` (or `npmInstall`) with a Python error similar to:

```
gyp ERR! stack SyntaxError: Missing parentheses in call to 'print'.
        Did you mean print("...")?
```

The proximate cause is `node-sass` (a legacy native package that bundles `node-gyp`) trying to invoke `python` and finding only Python 3 on the system. `print "..."` is Python-2-only syntax. `node-sass` itself has been deprecated upstream since 2020.

The fix is to migrate themes from `node-sass` to `dart-sass` (the `sass` npm package), which is pure JS and has no native build step.

## Fix per theme

For each affected theme directory, update `package.json`:

```diff
   "devDependencies": {
-    "gulp": "^4.0.2",
-    "liferay-theme-tasks": "^10.4.0"
+    "gulp": "^4.0.2",
+    "liferay-theme-tasks": "^11.5.6"
   },
   "dependencies": {
-    "liferay-frontend-theme-styled": "5.0.15",
-    "liferay-frontend-theme-unstyled": "5.0.20"
+    "liferay-frontend-theme-styled": "6.0.108",
+    "liferay-frontend-theme-unstyled": "6.0.93"
   }
```

The exact `liferay-frontend-theme-styled` / `-unstyled` versions track the target portal — verify the latest 6.x line published on npm before pinning. `liferay-theme-tasks` `11.x` drops `node-sass` and uses `sass` (dart-sass) + `gulp-sass@^5` internally.

## Clean up legacy lock files

Delete any pre-existing per-theme `package-lock.json` that pins `node-sass@^4.x` — leaving them in place will resurrect the broken dependency tree:

```bash
rm -f themes/*/package-lock.json
rm -f themes/*/*/package-lock.json   # for theme containers with sub-themes
```

The Liferay Workspace plugin 15.x bootstraps a single root `yarn.lock` at workspace top during the first build — keep that and let it regenerate transitively for all themes.

## Verification

```bash
<gw> :themes:<theme-name>:clean :themes:<theme-name>:build
```

Look for `Using Dart Sass compiler because other sass compilers are no longer supported` in the output — that's the canonical confirmation the migration took.

## Commit convention

One commit per theme (per the granularity rule in the `commit` skill):

```
<TICKET> <theme-name>: Migrate from node-sass to dart-sass via liferay-theme-tasks 11.5.6
```

The root `yarn.lock` regeneration is workspace-level — separate commit:

```
<TICKET> workspace: Regenerate yarn.lock for themes dart-sass migration
```

Theme containers (a directory holding multiple sub-themes) take the container as the prefix; the contained sub-themes share the commit.

## Optional follow-up

`#upgrade-td` is reasonable here only if the theme has heavy custom SCSS that exercises sass features where node-sass and dart-sass historically diverged (`@import` resolution, deeply nested `@extend`, division operator semantics). For vanilla theme structure the output is identical and no label is needed.
