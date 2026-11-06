# Tasks: Interface, navigation and appearance
**Input**: spec.md, plan.md, research.md, data-model.md, contracts/ui.md
**Tests**: meaningful preference persistence/failure and navigation/UI checks required by FR-014.

## Phase 1: Setup
- [X] T001 Pin navigation, lifecycle, DataStore and test dependencies in gradle/libs.versions.toml and app/build.gradle.kts.

## Phase 2: Foundation
- [X] T002 Implement sage light/dark theme in app/src/main/java/com/ethos/app/core/ui/theme/EthosTheme.kt.

## Phase 3: US1 - Navigate organizer (P1, MVP)
Goal: offline eight-destination navigation; independent test: drawer, shortcuts, repeated taps and Back.
- [X] T003 [US1] Add navigation regression tests in app/src/androidTest/java/com/ethos/app/features/shell/ShellNavigationTest.kt.
- [X] T004 [US1] Implement routes, drawer, bottom bar and state restoration in app/src/main/java/com/ethos/app/features/shell/EthosShell.kt.
- [X] T005 [US1] Connect shell and appearance/settings slot in app/src/main/java/com/ethos/app/MainActivity.kt.

## Phase 4: US2 - Honest empty and preview states (P1)
Goal: compact presentation with no fake runtime records; independent test: all filters and sample previews.
- [X] T006 [US2] Implement filter/item models and empty/populated reusable content in app/src/main/java/com/ethos/app/features/shell/ShellModels.kt and ShellContent.kt.
- [X] T007 [US2] Add explicit sample Hoje/review/reward/history/list previews in app/src/main/java/com/ethos/app/features/shell/ShellPreviews.kt.
- [X] T008 [US2] Add Portuguese accessible labels and future capability messages in app/src/main/res/values/strings.xml.

## Phase 5: US3 - Persist appearance (P2)
Goal: saved global appearance and recoverable errors; independent test: temp-file persistence/failures/relaunch.
- [X] T009 [P] [US3] Add repository persistence/unknown-value tests in app/src/test/java/com/ethos/app/core/preferences/AppearanceRepositoryTest.kt.
- [X] T010 [P] [US3] Add ViewModel failure/retry tests in app/src/test/java/com/ethos/app/features/settings/AppearanceViewModelTest.kt.
- [X] T011 [US3] Implement repository, singleton DataStore and Hilt bindings in app/src/main/java/com/ethos/app/core/preferences/.
- [X] T012 [US3] Implement lifecycle Screen, StateFlow ViewModel and settings content in app/src/main/java/com/ethos/app/features/settings/.

## Phase 6: Polish and verification
- [X] T013 Run build, JVM tests, lint, unsigned R8 release and available connected UI tests; record evidence in specs/002-app-shell/verification.md.
- [X] T014 Validate themes/relaunch/filter restoration/large fonts/landscape/insets and preview samples; record coverage/limitations in specs/002-app-shell/verification.md.
- [X] T015 Update README.md and only managed SPECKIT block in AGENTS.md with actual delivery state.

## Dependencies & Execution Order
T001 -> T002 -> US1 -> US2 -> US3 -> verification. T005 initially connects default theme until US3
integration completes. US3 tests precede implementation. T009/T010 can run in parallel as separate
files, but execution here is sequential. US1/US2 share content boundaries, so no parallel mutation.

## Parallel Examples
US1: navigation test design and resource inventory can be reviewed independently; T004 precedes T005.
US2: sample-data design can be prepared independently after models, T007 follows T006.
US3: T009 and T010 have separate files and no mutual dependency.

## Implementation Strategy
MVP is US1; verify then add honest US2 presentation and US3 preference persistence. Complete all
three as requested, then broad verification once. No commit, push, publication or version increase.
