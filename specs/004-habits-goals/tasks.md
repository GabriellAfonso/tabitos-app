# Tasks: Habits and goals

Input: spec.md, plan.md, research.md, data-model.md, contracts/ui.md, quickstart.md.
Tests follow constitution V. Paths below are relative to repository root.

## Phase 1: Setup
- [X] T001 Verify ignore/build/dependency configuration in .gitignore and app/build.gradle.kts; retain versions.

## Phase 2: Foundation
- [X] T002 Define historical models/actions/contracts in app/src/main/java/com/ethos/app/core/habits/domain/HabitModels.kt and HabitRepository.kt (FR001-015).
- [X] T003 Add normalized Room entities/DAO/migration in app/src/main/java/com/ethos/app/core/habits/data/ and update core/tasks/data/TaskDatabase.kt and TaskModule.kt (FR015).

## Phase 3: US1 Capture and practice (P1, MVP)
Independent test: Inbox creation, distinct days and partial goals survive reopening.
- [X] T004 [US1] Add calendar/distinct-day/capacity tests in app/src/test/java/com/ethos/app/core/habits/HabitRulesTest.kt (FR001-003,006).
- [X] T005 [US1] Implement evaluation/create/record and partial goals in app/src/main/java/com/ethos/app/core/habits/domain/HabitRules.kt (FR001-003,006-008).
- [X] T006 [US1] Add transactional repository and DI in app/src/main/java/com/ethos/app/core/habits/data/RoomHabitRepository.kt and core/tasks/data/TaskModule.kt (FR015).
- [X] T007 [US1] Add immutable state/drafts/retry in app/src/main/java/com/ethos/app/features/habits/HabitsViewModel.kt and HabitsScreen.kt (FR015-016).
- [X] T008 [US1] Implement capture/editor/progress in app/src/main/java/com/ethos/app/features/habits/HabitEditor.kt and HabitsContent.kt; texts in app/src/main/res/values/strings.xml (FR001-003,006,013,016).

## Phase 4: US2 Events and review (P1)
Independent test: two same-day events, one/week limit, closed zero-event avoid day.
- [X] T009 [US2] Test event/review/counter boundaries in app/src/test/java/com/ethos/app/core/habits/HabitRulesTest.kt (FR004-005,013-014).
- [X] T010 [US2] Implement event completeness/period review in app/src/main/java/com/ethos/app/core/habits/domain/HabitRules.kt (FR004-005,014).
- [X] T011 [US2] Integrate lazy content/filters/aggregate counts in app/src/main/java/com/ethos/app/MainActivity.kt, features/shell/EthosShell.kt and features/tasks/TasksContent.kt (FR013,016).

## Phase 5: US3 Correction and history (P1)
Independent test: correction changing reviewed outcome reopens exactly one review.
- [X] T012 [US3] Test corrections and streak uncertainty in app/src/test/java/com/ethos/app/core/habits/HabitRulesTest.kt (FR007-008,011,014).
- [X] T013 [US3] Implement corrections/audits/history/streaks in app/src/main/java/com/ethos/app/core/habits/domain/HabitRules.kt (FR007-008,011,014).
- [X] T014 [US3] Add calendar/period/record correction UI in app/src/main/java/com/ethos/app/features/habits/HabitsContent.kt (FR007,011,016).

## Phase 6: US4 Lifecycle and historical edits (P2)
Independent test: pause/resume, future type change and restoration after list deletion.
- [X] T015 [US4] Test inactive dates, historical revisions and restoration in app/src/test/java/com/ethos/app/core/habits/HabitRulesTest.kt (FR008-012).
- [X] T016 [US4] Implement interval/goal/type/lifecycle actions in app/src/main/java/com/ethos/app/core/habits/domain/HabitRules.kt (FR008-012).
- [X] T017 [US4] Atomically exclude list-owned habits in app/src/main/java/com/ethos/app/core/tasks/data/RoomTaskRepository.kt (FR012,015).
- [X] T018 [US4] Add preview/partial-target/destination/lifecycle UI in app/src/main/java/com/ethos/app/features/habits/HabitEditor.kt and HabitsContent.kt (FR006,009-012,016).

## Phase 7: Verification and polish
- [X] T019 Test read/write failure/retry in app/src/test/java/com/ethos/app/features/habits/HabitsViewModelTest.kt (FR015).
- [X] T020 Test populated v1 migration/reopen/atomic exclusion and UI in app/src/androidTest/java/com/ethos/app/core/habits/HabitDatabaseTest.kt and features/habits/HabitsContentTest.kt (FR012-016).
- [X] T021 Export app/schemas/com.ethos.app.core.tasks.data.TaskDatabase/2.json; run debug/JVM/lint/release/available connected and visual checks, record specs/004-habits-goals/verification.md and managed AGENTS.md (SC001-007).

## Dependencies and execution order
T001 -> T002 -> T003 blocks stories. US1 then US2/US3 then US4 then verification.
Tests precede corresponding rules. Shared-file edits remain sequential. US2/US3 tests can be authored
independently after models. No parallel implementation agents required. MVP is US1; user authorized
all stories, so continue incrementally through full scope. For each story, UI and isolated tests
may be prepared in separate files, but run their checks before declaring that story complete.
