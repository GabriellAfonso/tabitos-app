# Research: Android foundation

## Toolchain

Decision: AGP 9.0.1 and Gradle 9.3.1, JDK 21 running build, JVM 17 bytecode,
SDK 36 / min 24, Build Tools 36.0.0. Installed SDK includes 36; installed Gradle includes 9.3.1.
Rationale: reproducible stable patch versions with existing local prerequisites.
Alternative: newer AGP not required for one static screen.
Sources: https://developer.android.com/build/releases/agp-9-0-0-release-notes
and https://docs.gradle.org/9.3.1/release-notes.html . AGP requires Gradle >=9.1, JDK >=17.

Decision: built-in Kotlin with Compose compiler plugin 2.3.10 and KSP 2.3.4.
Rationale: Compose compiler must match Kotlin; explicit root buildscript classpath upgrades AGP minimum Kotlin 2.2.10.
No kotlin-android plugin or builtInKotlin/newDsl opt-outs.
Sources: https://developer.android.com/build/migrate-to-built-in-kotlin
and https://developer.android.com/jetpack/androidx/releases/compose-kotlin
and https://github.com/google/ksp/releases/tag/2.3.4 .

Decision: Hilt 2.59.2 with the same version for plugin/runtime/compiler.
Rationale: official release explicitly requires AGP 9 and fixes previous integration issues.
Source: https://github.com/google/dagger/releases/tag/dagger-2.59.2
and https://dagger.dev/hilt/gradle-setup .

Decision: Compose BOM 2026.02.00, Activity Compose 1.12.4, only Material3/UI/preview dependencies needed.
Rationale: pinned coordinated Compose family; compile/build/lint will verify the resolved combination.
Source: https://developer.android.com/jetpack/compose/bom
and https://developer.android.com/jetpack/androidx/releases/activity .

## Architecture and scope

Decision: Hilt Application and Activity, stateless FoundationScreen, shared minimal system theme.
Alternative: dummy injected repository, ViewModel, navigation graph and database rejected because no behavior needs them.
No entities or storage; no artificial injection demonstration. Preview uses explicit literal text.

## Signing

Decision: debug uses standard development identity; release unsigned and R8 enabled.
Owner signs locally through Android Studio using an external key. No custom credential automation.
Source: https://developer.android.com/studio/publish/app-signing .

## Uncertainties

No unresolved design questions. Actual build and device coverage must be recorded during implementation.

Compatibility qualification: Kotlin 2.3.10 vendor-tested range ends at Gradle 9.0.0 / AGP 9.0.0.
The selected newer combination requires actual build evidence; it is not claimed as vendor-certified.
Source: https://kotlinlang.org/docs/gradle-configure-project.html .
