# Tasks: Habits Without Debt

Paths relative to `app/src/`.

## Phase 1: Domain (US1, US2)

- [X] T001 [US1] Late window and automatic closing (no PENDING) in `main/java/com/ethos/app/core/habits/domain/HabitRules.kt`
- [X] T002 [US1] Reject records/corrections in closed periods in `main/java/com/ethos/app/core/habits/domain/HabitRules.kt`
- [X] T003 [US2] No penalty for missed periods, keep legacy confirmed penalties, auto reward for avoid/limit
  success in `main/java/com/ethos/app/core/accounting/domain/AccountingProjection.kt`
- [X] T004 [US2] `HabitAction.Settle` reconciling all habits in `main/java/com/ethos/app/core/habits/data/RoomHabitRepository.kt`
- [X] T005 [US4] Partial target defaults to available days in `main/java/com/ethos/app/core/habits/domain/HabitRules.kt`
- [X] T006 [US3] `successRate` (30 days / 8 weeks / 6 months) in `main/java/com/ethos/app/core/habits/domain/HabitRules.kt`

## Phase 2: UI

- [X] T007 [US1] Revisão/Hoje count tasks only; no habit review page in `main/java/com/ethos/app/MainActivity.kt`
  and `main/java/com/ethos/app/features/habits/HabitsContent.kt`
- [X] T008 [US1] Settle on load and day change in `main/java/com/ethos/app/features/habits/HabitsViewModel.kt`
- [X] T009 [US3] Card streak + rate, outcome labels, calendar marks in `main/java/com/ethos/app/features/habits/HabitsContent.kt`
  and `HabitsScreen.kt`
- [X] T010 [US4] Plain practice frequencies, breaking group, no partial/preview in
  `main/java/com/ethos/app/features/habits/HabitEditor.kt`, `HabitsScreen.kt`, `HabitsViewModel.kt`
- [X] T011 [US5] "Quebrar hábito" in "+" menu and Hábitos page in `main/java/com/ethos/app/features/shell/TodayCreateButton.kt`
- [X] T012 Strings: remove debt wording, add new labels in `main/res/values/strings.xml`

## Phase 3: Tests and docs

- [X] T013 Rewrite obsolete JVM tests in `test/java/com/ethos/app/core/habits/HabitRulesTest.kt` and
  `test/java/com/ethos/app/core/accounting/AccountingProjectionTest.kt`
- [X] T014 Update instrumented expectations in `androidTest/java/com/ethos/app/features/habits/HabitsContentTest.kt`
  and `androidTest/java/com/ethos/app/core/accounting/AccountingDatabaseTest.kt` (compiled, not run)
- [X] T015 Build, JVM tests, lint; record in `specs/014-habits-without-debt/verification.md`
- [X] T016 Notes in specs 004/005 and AGENTS.md
