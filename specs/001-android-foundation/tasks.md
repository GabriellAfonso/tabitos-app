# Tasks: Android foundation

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/launch.md, quickstart.md.
No artificial unit tests; actual runtime verification remains distinct from build checks.

## Phase 1: Setup

- [X] T001 Create ignore rules and central repositories/catalog in .gitignore, settings.gradle.kts and gradle/libs.versions.toml (FR-001, FR-007).
- [X] T002 Generate official pinned Wrapper in gradlew, gradlew.bat and gradle/wrapper/ (FR-001).

## Phase 2: Foundational

- [X] T003 Configure plugins and Kotlin built-in alignment in build.gradle.kts and gradle.properties (FR-001).

## Phase 3: US1 - Build the foundation

Goal: reproducible debug and unsigned optimized release packages.
Independent test: build both variants without personal signing credentials.

- [X] T004 [US1] Configure app/build.gradle.kts and app/proguard-rules.pro with min24, SDK36, Hilt/KSP and R8 release (FR-001, FR-002, FR-003, FR-006).
- [X] T005 [US1] Add Hilt entry points in app/src/main/java/com/ethos/app/EthosApplication.kt and MainActivity.kt and app/src/main/AndroidManifest.xml (FR-001, FR-004).

## Phase 4: US2 - Launch offline

Goal: readable static screen without runtime prerequisites.
Independent test: install and open offline, checking appearance/fonts/rotation.

- [X] T006 [P] [US2] Add app/src/main/java/com/ethos/app/core/ui/theme/EthosTheme.kt and app/src/main/res/values/themes.xml (FR-005).
- [X] T007 [US2] Add stateless screen and preview in app/src/main/java/com/ethos/app/features/foundation/FoundationScreen.kt and text in app/src/main/res/values/strings.xml (FR-004, FR-005).
- [X] T008 [US2] Verify offline launch and accessibility on available devices; record minimum-version and missing coverage in specs/001-android-foundation/verification.md (SC-002, SC-003).

## Phase 5: US3 - Maintain safely

Goal: documented reproducibility, safe local signing and honest evidence.
Independent test: follow README commands and inspect source exclusions/results.

- [X] T009 [P] [US3] Write prerequisites, artifact paths, install commands and local release signing workflow in README.md (FR-008).
- [X] T010 [US3] Inspect source/manifest/signing exclusions and generated Hilt output; record in specs/001-android-foundation/verification.md (FR-007, SC-004).

## Phase 6: Polish and verification

- [X] T011 Run debug build, unit-test task, lint and unsigned R8 release; record commands/artifacts/limitations in specs/001-android-foundation/verification.md (SC-001, SC-005).

## Dependencies and parallel examples

T001 -> T002 -> T003 -> US1 (T004 -> T005) -> US2 (T006 -> T007 -> T008).
US3 T009 can run after the build configuration is settled, alongside UI work;
T010 follows implementation. T011 follows source completion.
T006 and T009 affect independent files and can be done in parallel; no further agent delegation required.
US1 is the buildable MVP; US2 demonstrates runtime, US3 completes handoff.
Unavailable runtime scenarios leave T008 incomplete, with explicit evidence.

## Implementation strategy

Complete setup and build foundations before UI; then document and validate all deliverables.
All 11 tasks have checklist IDs and file paths; US1=2, US2=3, US3=2, shared=4.
