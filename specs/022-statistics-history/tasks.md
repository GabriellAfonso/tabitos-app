# Tasks: Statistics page (with history) and erasing history of deleted items

**Input**: `specs/022-statistics-history/` (spec, plan, research, data-model, contracts/ui.md, quickstart)

**Tests**: required by the spec (JVM tests for statistics calculations and erasing history).

## Phase 1: Setup

- [X] T001 [P] Add icons `ic_bar_chart.xml` and `ic_chevron_left.xml` (Material Symbols Rounded) in
  app/src/main/res/drawable/
- [X] T002 [P] Add the page's pt-BR strings (tabs, period labels, sections, history kinds, filters, erase dialogs,
  checkbox labels, deleted titles) in app/src/main/res/values/strings.xml

## Phase 2: Foundational

- [X] T003 Add `HabitRules.successRate(s, id, today, range)` sharing the card predicates with the existing
  `successRate` in app/src/main/java/com/ethos/app/core/habits/domain/HabitRules.kt
- [X] T004 Create `StatisticsPeriod`, `PeriodKind`, `PeriodLabel`, ranges, navigation and `StatisticsPeriodStore`
  (@Singleton) in app/src/main/java/com/ethos/app/features/statistics/StatisticsPeriod.kt
- [X] T005 [P] JVM tests for periods (week Mon–Sun, month/year bounds, ALL, previous/next limits, labels across
  year boundary) in app/src/test/java/com/ethos/app/features/statistics/StatisticsPeriodTest.kt

## Phase 3: User Story 1 — Resumo (P1)

**Independent test**: sample data in this week; numbers match tasks, habit cards, timers and ledger.

- [X] T006 [US1] Implement `taskSummary`, `habitSummary`, `timeSummary`, `coinSummary`, buckets in
  app/src/main/java/com/ethos/app/features/statistics/StatisticsSummary.kt
- [X] T007 [P] [US1] JVM tests: completed, % no prazo (with/without deadline, week deadline, late), postponed only
  POSTPONED, buckets per day/month/year with future flag, habit rate equals card rule, time excludes rewards and
  open segments, coins earned/spent in app/src/test/java/com/ethos/app/features/statistics/StatisticsSummaryTest.kt
- [X] T008 [US1] `StatisticsViewModel` combining task/habit/timer/accounting/reward/game-mode flows on
  Dispatchers.Default with loading/error/retry in app/src/main/java/com/ethos/app/features/statistics/StatisticsViewModel.kt
- [X] T009 [US1] `StatisticsScreen` + `StatisticsContent` (tabs, Resumo cards, bar chart with TalkBack text,
  previews) in app/src/main/java/com/ethos/app/features/statistics/StatisticsScreen.kt and StatisticsContent.kt
- [X] T010 [US1] Add `ShellDestination.STATISTICS` to the drawer in
  app/src/main/java/com/ethos/app/features/shell/ShellModels.kt and wire it in app/src/main/java/com/ethos/app/EthosApp.kt

## Phase 4: User Story 2 — Period (P1)

**Independent test**: arrows, kind menu, Tudo without arrows, reset on new process.

- [X] T011 [US2] Period row (‹ label ▾ ›, DropdownMenu, disabled arrows, "desde") in
  app/src/main/java/com/ethos/app/features/statistics/StatisticsContent.kt, actions in StatisticsViewModel.kt
  backed by `StatisticsPeriodStore`

## Phase 5: User Story 3 — Histórico (P1)

**Independent test**: each kind of event appears once in the right day and filter; Moedas equals old coin history.

- [X] T012 [US3] Implement `historyItems`, ledger matching, filters, day/month grouping and paging in
  app/src/main/java/com/ethos/app/features/statistics/StatisticsHistory.kt
- [X] T013 [P] [US3] JVM tests: shown/hidden audit kinds, postpone from/to, adjustments, ledger attach within 5 s,
  unmatched entries, Moedas lists every entry, game mode off hides coins, deleted subjects, paging in
  app/src/test/java/com/ethos/app/features/statistics/StatisticsHistoryTest.kt
- [X] T014 [US3] Histórico tab UI (filter chips, headers, discreet rows, Mostrar mais, tap routing) in
  app/src/main/java/com/ethos/app/features/statistics/StatisticsContent.kt
- [X] T015 [US3] Remove `ShellDestination.HISTORY`, its Settings link, `AccountingHistory`, `historyDays` and
  `AccountingPresentationTest` (moved behavior) in features/shell/ShellModels.kt, EthosApp.kt,
  features/accounting/AccountingContent.kt, features/accounting/AccountingPresentation.kt,
  app/src/test/java/com/ethos/app/features/accounting/AccountingPresentationTest.kt; drop now-unused strings

## Phase 6: User Story 4 — Erase when deleting (P2)

**Independent test**: delete unchecked keeps history; checked erases; balance/XP unchanged.

- [X] T016 [US4] `TaskAction.Delete(eraseHistory)` and `TaskAction.EraseHistory` in
  app/src/main/java/com/ethos/app/core/tasks/domain/TaskModels.kt and erase rules in TaskRules.kt
- [X] T017 [P] [US4] JVM tests for task erasure (single, recurring Só esta/Todas, from history, not regenerated,
  definition kept while series alive, non-deleted rejected) in
  app/src/test/java/com/ethos/app/core/tasks/domain/TaskEraseTest.kt
- [X] T018 [US4] DAO delete/anonymize queries in core/tasks/data/TaskDao.kt, core/habits/data/HabitDao.kt,
  core/timers/data/TimerStorage.kt, core/reminders/data/ReminderStorage.kt, core/trackers/data/TrackerStorage.kt,
  core/accounting/data/AccountingDao.kt
- [X] T019 [US4] `HistoryErasure` (task traces, habit) in
  app/src/main/java/com/ethos/app/core/history/data/HistoryErasure.kt; call it from RoomTaskRepository (removed
  rows diff, same transaction) and new `HabitRepository.erase` in RoomHabitRepository; update test fakes
- [X] T020 [US4] Task delete dialog checkbox in app/src/main/java/com/ethos/app/features/tasks/TasksContent.kt
- [X] T021 [US4] Habit exclude dialog checkbox, "Apagar por completo" with confirmation in
  app/src/main/java/com/ethos/app/features/habits/HabitsScreen.kt and `erase` in HabitsViewModel.kt
- [X] T022 [P] [US4] Instrumented test (written, not executed) of erasure across tables keeping balance/XP in
  app/src/androidTest/java/com/ethos/app/core/history/HistoryErasureTest.kt

## Phase 7: User Story 5 — Erase from Histórico (P3)

- [X] T023 [US5] Deleted-item dialog + irreversible confirmation and `erase` in StatisticsContent.kt /
  StatisticsViewModel.kt; blank ledger titles shown as "Tarefa apagada"/"Hábito apagado"

## Phase 8: Polish

- [X] T024 Update AGENTS.md SPECKIT block, spec status, `TODO/planejamento_estatisticas.md`; write
  specs/022-statistics-history/verification.md
- [X] T025 Run `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` and fix issues

## Dependencies

Setup → Foundational → US1 → US2 → US3 → US4 → US5 → Polish. US4 (T016–T022) is independent of US1–US3 and may
run in parallel; US5 needs US3 and US4.

## Parallel examples
- T001, T002, T005 together; T007 alongside T008; T013 alongside T014; T017 and T022 alongside T018–T021.

## Implementation strategy
MVP = US1 + US2 (Resumo with period). Then US3 (history replaces coin history), US4 (erase on delete), US5.
