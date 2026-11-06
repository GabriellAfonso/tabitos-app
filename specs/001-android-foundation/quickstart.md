# Quickstart validation

Use Android Studio supporting AGP 9 (Otter 2025.2.3 or newer), JDK 17+ (21 used here),
Android SDK Platform 36 and Build Tools 36.0.0. Configure SDK through Android Studio's ignored
local.properties or ANDROID_HOME. Initial dependency resolution requires internet.

Windows: `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease`
Linux/macOS: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease`

Expected debug: `app/build/outputs/apk/debug/app-debug.apk`.
Expected release: `app/build/outputs/apk/release/app-release-unsigned.apk`.
Lint: `app/build/reports/lint-results-debug.html`. Empty unit-test tasks are not behavioral coverage.

With authorized device connected: `adb devices`, `adb install -r app/build/outputs/apk/debug/app-debug.apk`,
`adb shell am start -W -n com.ethos.app/.MainActivity`.
Verify offline launch, rotation/relaunch, light/dark, largest font setting and portrait/landscape;
verify screen reader announces Ethos. Repeat on API 24 and one newer API when available.
Record unavailable scenarios explicitly in verification.md.

For release distribution, use Android Studio > Build > Generate Signed App Bundle / APK > APK,
select release and a private keystore outside the repository. Never choose the debug key.
No production key is required for debug or unsigned release validation. Do not commit signing material.
See [launch contract](contracts/launch.md) and [data model](data-model.md).
