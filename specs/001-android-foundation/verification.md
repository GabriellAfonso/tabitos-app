# Verification: Android foundation

**Date**: 2026-11-06
**Result**: Implementation complete; 11/11 tasks completed. Runtime verified on API36.
**Limitations**: API24 and physical-device execution unavailable; no auditory TalkBack check.

## Build and tooling

Executed with local JDK21 and Android SDK36 supplied through environment variables:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease --max-workers=2 '-Pkotlin.compiler.execution.strategy=in-process' --console=plain
```

BUILD SUCCESSFUL in 3m26s; 108 actionable tasks (85 executed, 23 up-to-date).

- Debug assembly passed, including Kotlin, KSP/Hilt generation, Hilt Java compilation and bytecode transformation.
- Unit-test task: NO-SOURCE. No unit tests exist; this is not behavioral coverage.
- lintDebug: 0 errors, 10 warnings; release lintVital also passed.
- R8 minification, resource shrinking and unsigned release assembly passed.
- Toolchain combination is verified by this build; vendor matrix qualification remains in research.md.
- Wrapper officially generated with Gradle9.3.1; official distribution SHA256 pinned in its properties.

## Artifacts

| Artifact | Bytes | SHA256 |
|---|---:|---|
| app/build/outputs/apk/debug/app-debug.apk | 11985260 | C2849DD1CDCF674DE151FB9FB5368C554C0B2A7E79A1EFFCBF96314E68818E37 |
| app/build/outputs/apk/release/app-release-unsigned.apk | 769552 | D368F7DC43F5440C5C9E8B1C2B7BB19B1EE0EF539930A50807B67D5842266AAB |

Lint reports: app/build/reports/lint-results-debug.{html,txt,xml}.
R8 mapping: app/build/outputs/mapping/release/.
`apksigner verify` on release returned DOES NOT VERIFY / Missing META-INF/MANIFEST.MF,
expected for the intentionally unsigned package; it requires local production signing before installation.

## Runtime and accessibility

Existing Pixel_5 AVD, API36, launched headless with read-only disk and snapshots disabled.

- `adb install -r .../app-debug.apk`: Success.
- `adb shell am start -W -n com.ethos.app/.MainActivity`: Status ok, COLD.
- Initial accessibility hierarchy exposes android.widget.TextView with text Ethos.
- Enabled airplane mode with `cmd connectivity airplane-mode enable`; confirmed airplane_mode_on=1.
- Set font_scale=2.0 (Android200% maximum preset), rotated and switched night mode.
- Offline force-stop/relaunch: Status ok; Ethos remained visible with no login/permission requirement.
- All four light/dark x portrait/landscape cases exposed Ethos with bounds inside the screen.
- Visual inspection passed for [dark landscape](evidence/dark-landscape-200.png)
  and [light portrait](evidence/light-portrait-200.png), both with font200%.
- Initial normal-font portrait also exposed the full title; rotations recreated the screen successfully.
- Final process remained active. No application crash observed.
- TalkBack audio itself was not tested; accessible text was verified through UIAutomator.
- API24 image is not installed. minSdk24 is configured and confirmed in the merged manifest,
  but this is not a claim of runtime coverage on Android7.

The emulator System UI briefly displayed its own ANR dialog during rotation under host load;
after choosing Wait, Ethos was visible and subsequent checks passed. This is not counted as an app crash.
The read-only emulator was shut down after validation; persistent AVD preferences were not changed.

## Static safety and scope inspection

- Merged manifest: package com.ethos.app, minSdk24, targetSdk36, single MainActivity launcher.
- No network/dangerous runtime permissions. AndroidX adds an internal signature receiver permission.
- Release has no signingConfig and does not use the debug key.
- Generated Hilt Application component and Activity injector verified.
- No kotlin-android plugin or builtInKotlin/newDsl opt-out flags.
- Source Kotlin/build lines respect120 characters; string resource holds visible title.
- No personal signing material, credentials, machine SDK paths or build caches in deliverable source.
- .gitignore excludes local.properties, IDE/build caches and signing material.
- No Git repository exists: exclusions were inspected directly, not claimed as an audit of tracked files.
- No Room, DataStore, ViewModel, navigation, domain models or artificial injected services added.

## Warnings and corrected attempts

Lint: eight newer-version notices retained for deliberately pinned versions; MissingApplicationIcon
and DataExtractionRules warnings remain. Custom branded icon is deferred. No data is stored;
backup policy should be reviewed when persistence is introduced. No warnings were suppressed.
Gradle packaged libandroidx.graphics.path.so unchanged when symbols could not be stripped (nonfatal).

Initial attempts corrected: unsupported Gradle bootstrap option, UTF8 BOM in TOML, PowerShell
argument parsing. A Gradle daemon disappeared during compilation while the emulator was starting;
retry with two workers and in-process Kotlin completed successfully. No cache cleaning was used.
First emulator installation attempts failed before Android services finished booting; later install passed.

## Spec Kit analysis and hooks

Spec checklist:16/16. Analyze ran read-only before implementation:8 requirements,11 tasks,
100% requirement coverage,0 critical findings,0 unmapped tasks,0 relevant duplications/ambiguities.
Optional agent-context hooks after specify/plan were surfaced; plan-required context was updated
only within AGENTS.md SPECKIT markers. No before/after tasks/analyze/implement hooks registered.
No commits, push, publication or production signing performed.
