# Implementation Plan: Android foundation

**Branch**: none | **Date**: 2026-11-06 | **Spec**: [spec.md](spec.md)
**Input**: `specs/001-android-foundation/spec.md`

## Summary

Create one minimal Android application, one launcher Activity and Hilt Application; render Ethos
with Compose Material 3. No domain models, ViewModels, persistence or navigation placeholders.

## Technical Context

**Language/Version**: Kotlin 2.3.10, AGP built-in Kotlin, JVM target 17; build JDK 21 (minimum 17).
**Primary Dependencies**: AGP 9.0.1, Gradle 9.3.1, Compose compiler 2.3.10,
Compose BOM 2026.02.00, Activity Compose 1.12.4, Hilt 2.59.2, KSP 2.3.4.
**Storage**: none.
**Testing**: debug build, unit-test task (NO-SOURCE is not coverage), lint, unsigned R8 release;
manual install/launch/accessibility scenarios when device available.
**Target Platform**: Android minSdk 24, compileSdk/targetSdk 36; Build Tools 36.0.0.
**Project Type**: native mobile app, one `:app` module, `com.ethos.app`.
**Performance Goals**: immediate static screen; no speculative timing threshold.
**Constraints**: offline runtime, no permissions or secrets, no unused future dependencies.
**Scale/Scope**: one owner, one static screen.

## Constitution Check

Before research and after design:
- I PASS: spec approved by checklist; current request explicitly authorizes all subsequent stages.
- II PASS: no data stored; Room/DataStore and migration rules apply when persistence is introduced.
- III PASS: single Activity, Compose, Hilt/KSP. No business behavior requiring ViewModel/repository.
  Navigation is explicitly deferred by source prompt; no empty graph or unused dependency.
- IV PASS: string resources, system light/dark minimal theme, enlarged fonts and safe insets.
  Theme selector/persistence explicitly deferred by user; no data collection or permissions.
- V PASS: build/lint/R8 and actual available runtime checks; no artificial unit tests.
No constitutional rule is weakened: future app requirements are staged according to explicit scope.

## Project Structure

### Documentation (this feature)

`specs/001-android-foundation/`: spec, plan, research, data-model, quickstart,
contracts/launch.md, checklists/requirements.md, tasks.md, verification.md.

### Source Code (repository root)

```text
settings.gradle.kts
build.gradle.kts
gradle.properties
gradle/libs.versions.toml
gradle/wrapper/gradle-wrapper.{jar,properties}
gradlew
gradlew.bat
app/build.gradle.kts
app/proguard-rules.pro
app/src/main/AndroidManifest.xml
app/src/main/java/com/ethos/app/EthosApplication.kt
app/src/main/java/com/ethos/app/MainActivity.kt
app/src/main/java/com/ethos/app/features/foundation/FoundationScreen.kt
app/src/main/java/com/ethos/app/core/ui/theme/EthosTheme.kt
app/src/main/res/values/{strings,themes}.xml
README.md
.gitignore
```

**Structure Decision**: feature screen and shared minimal theme only; no empty architecture directories.

## Build and signing decisions

Central catalog pins all versions. Apply Android, Compose, KSP and Hilt plugins, never kotlin-android.
Root buildscript explicitly aligns the Kotlin Gradle plugin classpath with the Compose compiler version.
Hilt annotations on Application and Activity exercise real generated entry points.
Release enables R8 and optimized default rules; has no signingConfig. Owner uses Android Studio
Generate Signed APK with a key outside source control. Unsigned release remains buildable without it.
Generate official Wrapper using installed Gradle; pin distribution SHA-256. Do not copy reference project files.

## Verification strategy

Run `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease`.
Inspect merged manifest (minSdk, permissions, launcher), generated Hilt sources and unsigned release.
Use adb only if available; check API 24 separately and report absence. Manual matrix:
offline launch, relaunch/rotation, light/dark, maximum font size, portrait/landscape, screen-reader label.
Record commands and honest results in verification.md; leave unavailable validation tasks unchecked.

Compatibility qualification: Kotlin 2.3.10 vendor-tested range ends at Gradle 9.0.0 / AGP 9.0.0.
The selected newer combination requires actual build evidence; it is not claimed as vendor-certified.
Source: https://kotlinlang.org/docs/gradle-configure-project.html .
