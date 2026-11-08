# Tasks: Calendário completo e acompanhamentos personalizados

**Input**: spec.md, plan.md, research.md, data-model.md, contracts/trackers.md, quickstart.md
**Tests**: required by spec FR-029 and constitution V. Base path `app/src/main/java/com/ethos/app/` = `src/`.

## Phase 1: Setup

- [X] T001 Add Portuguese strings for trackers, calendar views/filters, analytics and tracker notification in
  app/src/main/res/values/strings.xml

## Phase 2: Foundational

- [X] T002 Create tracker models, actions, problems and `TrackerRepository` in src/core/trackers/domain/TrackerModels.kt
- [X] T003 Create entities, DAO and `TRACKER_MIGRATION_6_7` in src/core/trackers/data/TrackerStorage.kt; register
  entities, version 7 and `trackers()` DAO in src/core/tasks/data/TaskDatabase.kt; migration + provider in
  src/core/tasks/data/TaskModule.kt; export app/schemas/.../7.json
- [X] T004 Implement `RoomTrackerRepository` (observe; transactional execute writing only tracker tables) in
  src/core/trackers/data/RoomTrackerRepository.kt
- [X] T005 Add `LocalOpenTracker` in src/core/ui/trackers/TrackerControl.kt

## Phase 3: US1 Builder (P1)

Independent test: create a four-type form, reorder, reopen; primary stays in place.
- [X] T006 [US1] Implement form validation and versioning (`saveForm`, `setActive`) in
  src/core/trackers/domain/TrackerRules.kt
- [X] T007 [P] [US1] JVM tests for builder rules in app/src/test/java/com/ethos/app/core/trackers/TrackerRulesTest.kt
- [X] T008 [US1] `TrackerViewModel` with hub/builder draft state, save, archive/restore in
  src/features/trackers/TrackerViewModel.kt
- [X] T009 [US1] Builder and hub UI in src/features/trackers/TrackerContent.kt; "Acompanhamento" button in
  src/features/habits/HabitsScreen.kt

## Phase 4: US2 Entries (P1)

Independent test: fill yesterday, reject tomorrow, single-mode reopen, multiple-mode three entries, delete.
- [X] T010 [US2] Entry rules (`fillable`, `entryFor`, `saveEntry`, `deleteEntry`) in src/core/trackers/domain/TrackerRules.kt
- [X] T011 [P] [US2] JVM tests for entries and zero-effect boundaries in
  app/src/test/java/com/ethos/app/core/trackers/TrackerRulesTest.kt
- [X] T012 [US2] Entry form (scale value buttons, choices, text, required markers), history list and delete
  confirmation in src/features/trackers/TrackerContent.kt and src/features/trackers/TrackerViewModel.kt

## Phase 5: US3 Versions (P1)

Independent test: remove option + add required question, edit old entry.
- [X] T013 [US3] Render/edit entries with their own version; historical option labels in
  src/features/trackers/TrackerContent.kt
- [X] T014 [P] [US3] Versioning JVM tests in app/src/test/java/com/ethos/app/core/trackers/TrackerRulesTest.kt

## Phase 6: US4 Calendar (P2)

Independent test: month/week/day, filters, day sheet, fill from a day.
- [X] T015 [US4] Pure `CalendarModel` (cells, day details, filters, parity) in src/features/calendar/CalendarModel.kt
- [X] T016 [P] [US4] JVM tests in app/src/test/java/com/ethos/app/features/calendar/CalendarModelTest.kt
- [X] T017 [US4] `CalendarViewModel` in src/features/calendar/CalendarViewModel.kt
- [X] T018 [US4] `CalendarContent` (views, navigation, filters, accessible cells, day sheet) in
  src/features/calendar/CalendarContent.kt
- [X] T019 [US4] Replace `TaskPage.CALENDAR` in src/features/tasks/TasksContent.kt and wire Calendário,
  tracker dialogs and `LocalOpenTracker` in src/MainActivity.kt

## Phase 7: US5 Analytics (P3)

Independent test: 7/14/30/90 with gaps and multi-entry day.
- [X] T020 [US5] `analytics` in src/core/trackers/domain/TrackerRules.kt
- [X] T021 [P] [US5] Analytics JVM tests in app/src/test/java/com/ethos/app/core/trackers/TrackerRulesTest.kt
- [X] T022 [US5] Period chips, Canvas dot plot with gaps, value list, option frequency in
  src/features/trackers/TrackerContent.kt

## Phase 8: US6 Reminder (P3)

Independent test: notice at time, dismiss keeps pending, fill resolves.
- [X] T023 [US6] `ReminderKind.TRACKER`, sources with trackers, schedule/relevance/snooze in
  src/core/reminders/domain/ReminderModels.kt and src/core/reminders/domain/ReminderRules.kt
- [X] T024 [US6] Coordinator observes trackers, posts tracker rows directly; platform neutral ongoing notification with
  FILL/SNOOZE in src/core/reminders/platform/ReminderCoordinator.kt and ReminderPlatform.kt
- [X] T025 [US6] FILL routing in src/features/reminders/ReminderViewModel.kt and src/MainActivity.kt; reminder time in
  builder and `pending` + Hoje card in src/features/trackers/
- [X] T026 [P] [US6] Tracker reminder rule tests in app/src/test/java/com/ethos/app/core/reminders/ReminderRulesTest.kt

## Phase 9: Polish & verification

- [X] T027 Room tests (6→7 preservation, 1→7 chain, unique day index, no ledger/habit change) in
  app/src/androidTest/java/com/ethos/app/core/trackers/TrackerDatabaseTest.kt; add 6→7 to existing DB tests
- [X] T028 [P] Compose UI tests (builder, entry at font 2.0, calendar cells) in
  app/src/androidTest/java/com/ethos/app/features/trackers/TrackerUiTest.kt
- [X] T029 [P] Notification runtime test in app/src/androidTest/java/com/ethos/app/TrackerReminderRuntimeTest.kt
- [X] T030 Build, unit, lint, release, instrumented run; write specs/009-calendar-habit-trackers/verification.md;
  update AGENTS.md context block and spec status

## Dependencies
Setup → Foundational → US1 → US2 → US3 (same files) ; US4 needs Foundational (+US2 for fill) ; US5 needs US2 ;
US6 needs US2. Polish last.

## Parallel examples
T007/T011/T014/T021 tests can be written alongside rules; T016 alongside T015; T028/T029 after UI exists.

## Strategy
MVP = US1+US2+US3 (tracker usable from habit detail). Then calendar, analytics, reminder.
