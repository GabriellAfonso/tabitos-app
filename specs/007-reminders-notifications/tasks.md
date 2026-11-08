# Tasks: Lembretes e notificações

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/reminders.md
**Tests**: mandated policy, persistence/migration, actions/UI and available runtime verification.
**Paths**: package root `app/src/main/java/com/ethos/app/`; test counterpart same package.

## Phase 1: Setup

- [x] T001 Verify platform/permissions and baseline in specs/007-reminders-notifications/research.md.

## Phase 2: Foundation

- [x] T002 Define pure models/repository contract in app/src/main/java/com/ethos/app/core/reminders/domain/ReminderModels.kt.
- [x] T003 Add migration4→5/entities/DAO and DI in app/src/main/java/com/ethos/app/core/reminders/data/ReminderStorage.kt and core/tasks/data/TaskDatabase.kt.

## Phase 3: US1 Optional configuration (P1, MVP)

Independent test: two definitions per task/habit persist, edit/remove with unchanged deadlines/targets.

- [x] T004 [US1] Test recurrence/time/scope projection in app/src/test/java/com/ethos/app/core/reminders/ReminderRulesTest.kt.
- [x] T005 [US1] Implement projection and scope precedence in app/src/main/java/com/ethos/app/core/reminders/domain/ReminderRules.kt; reuse core/tasks/domain/TaskRules.kt.
- [x] T006 [US1] Implement validated persistence in app/src/main/java/com/ethos/app/core/reminders/data/RoomReminderRepository.kt.
- [x] T007 [US1] Add retained configuration ViewModel/UI in app/src/main/java/com/ethos/app/features/reminders/ReminderViewModel.kt and ReminderContent.kt; integrate existing task/habit details via callbacks.

## Phase 4: US2 Actions and snooze (P1)

Independent test: Concluir preserves existing confirmations, habit never records, snooze replaces same slot.

- [x] T008 [US2] Add snooze/revalidation tests in app/src/test/java/com/ethos/app/core/reminders/ReminderRulesTest.kt.
- [x] T009 [US2] Add secure direct activity notification actions in app/src/main/java/com/ethos/app/core/reminders/platform/ReminderPlatform.kt.
- [x] T010 [US2] Integrate pending validated navigation/completion/snooze in app/src/main/java/com/ethos/app/MainActivity.kt and features/reminders/ReminderContent.kt.

## Phase 5: US3 Relevant delivery and recovery (P1)

Independent test: stale sources cancel, blocked delivery remains pending, repeated recovery no burst.

- [x] T011 [US3] Add relevance/DST/recovery policy tests in app/src/test/java/com/ethos/app/core/reminders/ReminderRulesTest.kt.
- [x] T012 [US3] Implement serialized reconciliation, source observation and single next alarm in app/src/main/java/com/ethos/app/core/reminders/platform/ReminderCoordinator.kt.
- [x] T013 [US3] Add secure receivers/permissions in app/src/main/AndroidManifest.xml and application startup in app/src/main/java/com/ethos/app/EthosApplication.kt.
- [x] T014 [US3] Add permission/channel/exact capability controls in app/src/main/java/com/ethos/app/features/reminders/ReminderContent.kt and res/values/strings.xml.

## Phase 6: US4 Daily summary (P2)

Independent test: off initially, chosen time, actual Hoje/review counts, persistence/change/disable.

- [x] T015 [US4] Test summary classification in app/src/test/java/com/ethos/app/core/reminders/ReminderRulesTest.kt.
- [x] T016 [US4] Implement summary settings/notification in app/src/main/java/com/ethos/app/core/reminders/platform/ReminderCoordinator.kt and features/reminders/ReminderContent.kt.

## Phase 7: Polish and verification

- [x] T017 Test migration/persistence/action integration in app/src/androidTest/java/com/ethos/app/ReminderIntegrationTest.kt; export app/schemas/com.ethos.app.core.tasks.data.TaskDatabase/5.json.
- [x] T018 Validate accessible UI and Activity recreation in app/src/androidTest/java/com/ethos/app/ReminderUiTest.kt and app/src/androidTest/java/com/ethos/app/ReminderActivityTest.kt.
- [x] T019 Run builds/JVM/lint/release/available Android/runtime matrix and record evidence/limits in specs/007-reminders-notifications/verification.md.
- [x] T020 Update implemented status and managed AGENTS.md SPECKIT block only, preserve unrelated changes.

## Dependencies and execution strategy

T001→T002→T003; US1 T004→T005→T006→T007; US2 depends on US1, tests before code;
US3 uses US1/2, tests before coordinator; US4 follows shared platform, test then implementation;
T017–20 final integration. MVP US1 alone enables configuration; full request includes all stories.
Parallel opportunities: US1 policy tests and storage fixtures; US2 action policy and UI fixtures;
US3 permission UI and receiver fixtures; US4 summary fixtures and presentation, in distinct files.
No implementation delegation requested. All tasks deliberately sequential checkboxes,20 total:
setup1,foundation2,US1=4,US2=3,US3=4,US4=2,polish4. All tasks include IDs and concrete paths.

## Coverage

FR001–003:T004–007; FR004:T002–006,T017; FR005–007/013:T008–010,T017;
FR008/010–012:T011–014,T019; FR009:T015–016; FR014:T003/T006/T017;
FR015:T004/T008/T011/T015/T017–019. SC001:T004–007; SC002/005:T008–010;
SC003/004/007:T011–014; SC006:T015–016. Every task serves these requirements or mandatory workflow.
