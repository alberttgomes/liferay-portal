# Change-log: <NNN>-<short-slug>

- **Date:** <yyyy-mm-dd>
- **Run:** <run-id>
- **File / artifact:** <relative/path or artifact name>
- **Check / step:** <e.g. Step 3 build — internal Nexus unreachable>
- **Severity:** BLOCKER | ERROR | WARNING | REPRO
- **Report row:** #<n> in report.csv

## What was wrong

<the error, with the exact log excerpt / message>

```
<log excerpt>
```

## Exactly what was changed

<minimal diff (before → after), or an artifact-replacement note for binary changes>

```diff
- <before>
+ <after>
```

## Why

<rationale. Classify: reproduction-environment change (needed only because we build/run
outside the client network) vs genuine source defect (the delivered source is wrong).>

## Client action requested

<what the client must confirm or do — e.g. "confirm these artifacts exist on the Liferay CDN,
or keep Nexus for internal builds". Leave blank if none.>
