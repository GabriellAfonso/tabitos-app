# Tasks: Lists, tasks and recurrence

**Input**: specs/003-task-lists/{spec,plan,research,data-model,quickstart}.md and contracts/ui.md.
**Tests**: Required by constitution for rules, persistence, failures and UI changes.

## Phase 1: Setup
- [X] T001 Pin Room/desugaring and schema export in gradle/libs.versions.toml and app/build.gradle.kts.

## Phase 2: Foundation
- [X] T002 Define immutable models/repository/actions in app/src/main/java/com/ethos/app/core/tasks/domain/TaskModels.kt and TaskRepository.kt.
- [X] T003 Implement normalized Room entities/DAO/database/Hilt in app/src/main/java/com/ethos/app/core/tasks/data/.

## Phase 3: US1 Capture and organize (P1, MVP)
Goal: persisted Inbox/list workflows. Independently verify capture/move/order/relaunch.
- [X] T004 [US1] Add organization rule tests in app/src/test/java/com/ethos/app/core/tasks/domain/TaskRulesTest.kt (FR-001..006).
- [X] T005 [US1] Implement list/task organization rules in app/src/main/java/com/ethos/app/core/tasks/domain/TaskRules.kt.
- [X] T006 [US1] Persist transactionally and test reopen/rollback in app/src/main/java/com/ethos/app/core/tasks/data/RoomTaskRepository.kt and app/src/androidTest/java/com/ethos/app/core/tasks/data/TaskDatabaseTest.kt.
- [X] T007 [US1] Build state/draft/error handling in app/src/main/java/com/ethos/app/features/tasks/TasksViewModel.kt and TasksScreen.kt.
- [X] T008 [US1] Implement capture/list editor/order/delete/move UI in app/src/main/java/com/ethos/app/features/tasks/TasksContent.kt and TaskEditor.kt.

## Phase 4: US2 Complete and correct (P1)
Goal: explicit checklist completion/corrections. Independently verify unchecked confirmation and undo.
- [X] T009 [US2] Test completion dates/checklist/outcome corrections in app/src/test/java/com/ethos/app/core/tasks/domain/TaskRulesTest.kt (FR-007/012/014/023).
- [X] T010 [US2] Implement outcome/checklist/correction rules in app/src/main/java/com/ethos/app/core/tasks/domain/TaskRules.kt.
- [X] T011 [US2] Implement detail/completion-date/unchecked confirmation/history UI in app/src/main/java/com/ethos/app/features/tasks/TasksContent.kt.

## Phase 5: US3 Deadlines and review (P1)
Goal: correct Hoje/review membership and reviewed rescheduling. Verify all boundary types.
- [X] T012 [US3] Test day/week/clock boundaries and reschedule review in app/src/test/java/com/ethos/app/core/tasks/domain/TaskRulesTest.kt (FR-008..013).
- [X] T013 [US3] Implement date membership/review/reschedule in app/src/main/java/com/ethos/app/core/tasks/domain/TaskRules.kt.
- [X] T014 [US3] Integrate Hoje/review/calendar/drawer slots and counts in app/src/main/java/com/ethos/app/features/shell/EthosShell.kt and MainActivity.kt.

## Phase 6: US4 Recurrence and scopes (P2)
Goal: all patterns/endings, isolated occurrences and preserved history. Verify leap/absence/undo conflicts.
- [X] T015 [US4] Test recurrence/counts/scope/dependent undo in app/src/test/java/com/ethos/app/core/tasks/domain/RecurrenceTest.kt (FR-015..020).
- [X] T016 [US4] Implement generation/ends/scopes/dependent correction in app/src/main/java/com/ethos/app/core/tasks/domain/TaskRules.kt.
- [X] T017 [US4] Expose all recurrence modes/endings/scopes in app/src/main/java/com/ethos/app/features/tasks/TaskEditor.kt and TasksContent.kt.

## Phase 7: Polish and verification
- [X] T018 Test durable ViewModel failures/retry/duplicate saves in app/src/test/java/com/ethos/app/features/tasks/TasksViewModelTest.kt (FR-021/022).
- [X] T019 Add Portuguese resources/previews and Compose flow tests in app/src/main/res/values/strings.xml and app/src/androidTest/java/com/ethos/app/features/tasks/TasksContentTest.kt (SC-001..007).
- [X] T020 Run build/JVM/lint/R8/connected and visual/accessibility checks; export schema1 in app/schemas/ and record evidence/limitations in specs/003-task-lists/verification.md.

## Dependencies and opportunities
T001 -> T002 -> T003 -> US1 -> US2/US3 -> US4 -> polish. Shared rules/UI files execute
sequentially. Independent test-file authoring can be parallel after models, but no extra agents
are required. Story1 is the MVP; all four stories remain required by this request.
Implementation proceeds by test cases before related rule changes, then end-to-end integration.
All 20 tasks have IDs and exact paths; US1=5, US2=3, US3=3, US4=3, setup/foundation=3, polish=3.
