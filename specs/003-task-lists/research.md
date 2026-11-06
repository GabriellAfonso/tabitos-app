# Research: Lists and tasks

## Room and tooling
Decision: Room 2.8.5 runtime/ktx/compiler/testing and Gradle plugin; existing KSP2 retained.
Rationale: stable release supports min23 and Kotlin2+, compatible in principle with min24/Kotlin2.3.
Actual build is the compatibility evidence. Plugin exports tracked schemas to app/schemas.
Alternative: raw KSP schemaLocation requires explicit tracked argument provider; avoid that extra code.
Sources: https://developer.android.com/jetpack/androidx/releases/room
https://developer.android.com/build/migrate-to-built-in-kotlin

## Civil dates on minimum Android
Decision: java.time with desugar_jdk_libs 2.1.5 and core library desugaring.
Rationale: deterministic LocalDate logic on API24/25, no custom date arithmetic library.
Store epochDay and separate audit epoch millis; read device zone on each refresh.
Sources: https://developer.android.com/studio/write/java8-support
https://github.com/google/desugar_jdk_libs/blob/master/CHANGELOG.md

## Persistence validation
Decision: pure JVM rules tests and instrumented Room in-memory/file-backed reopen tests.
No previous database exists, so v1 requires no fictitious migration. Version/schema changes later
must include tested explicit/automatic migration; destructive fallback is forbidden.
Sources: https://developer.android.com/training/data-storage/room/testing-db
https://developer.android.com/training/data-storage/room/migrating-db-versions

## Product consistency
Decision: user-approved local dates/future-completion rejection, explicit missed-deadline review
on same open task, stable cadence/finite slot counts and dependent-undo guard.
Rationale: preserves history while avoiding accidental duplication/erasure.
Alternatives rejected: moving deadlines with timezone, silently erasing review, resetting finite counts,
or removing acted-on successors on undo. No technical or product clarification remains open.
