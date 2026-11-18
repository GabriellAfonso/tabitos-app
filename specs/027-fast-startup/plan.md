# Implementation Plan: Fast app startup (Today first, staged preloading)

**Branch**: no branch (work stays on `master`) | **Date**: 2026-11-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/027-fast-startup/spec.md`

## Summary

Hoje is slow to show because, on a cold open, ten ViewModels read the database at once through transactions that
share the single write connection, and the open-time recurrence refresh and habit closing rewrite every row. The plan:
measure with `ReportDrawnWhen`; load Hoje's sources first and start Calendar, Rewards and Reminders afterwards, one at
a time, through a small `StartupSequence` (on-demand requests skip the queue); start the open-time refresh/settle
writes after Hoje is shown; and write only changed task and habit rows. No schema or rule change.
Details: [research.md](research.md).

## Technical Context

**Language/Version**: Kotlin (AGP 9 built-in Kotlin), minSdk 24, target/compile 36

**Primary Dependencies**: Jetpack Compose (BOM 2026.02.00), activity-compose 1.12.4 (`ReportDrawnWhen`), Hilt,
Room 2.8.5 (compatibility mode, no driver), Coroutines/Flow. No new dependency.

**Storage**: Room schema 7, unchanged; DataStore unchanged

**Testing**: JUnit + coroutines-test on JVM; Room instrumented tests written, run only with authorization

**Target Platform**: Android phone (user's real routine installed)

**Project Type**: mobile-app, single `:app` module

**Performance Goals**: Hoje's content shown faster on cold open than the measured baseline (SC-001); no source Hoje
does not draw starts before Hoje (SC-002)

**Constraints**: no data change, no schema change, no visible change other than timing; no personal content in logs

**Scale/Scope**: ~10 files touched in `core/` and `features/`, plus `EthosApp.kt`

## Constitution Check

*Before research*

- I (spec first): spec 027 has scope, decisions and acceptance; the user asked for plan → tasks → implement. PASS
- II (local data): no schema change, no migration; saves must leave the same database (FR-009) with tests. PASS
- III (architecture): `StartupSequence` is one small class for a documented need (ordering across ViewModels);
  no new module or dependency; ViewModels keep `viewModelScope`; pure functions for readiness/mapping. PASS
- IV (UI/privacy): Hoje keeps its existing loading text and error/retry states; measurement logs only timing. PASS
- V (verification): JVM tests for sequence, readiness, mapping and changed rows; Room tests written; limitations
  recorded. PASS

*After design*: unchanged. Driver migration and shared hot flows were rejected (research R2) because they put the
stored routine at risk or break retry; no violation to justify.

## Design

1. **Measurement** (`EthosApp.kt`, `TodayReadiness.kt`): `todayReady(...)` over the loading flags of game mode,
   tasks, habits, timers, trackers and wallet; `ReportDrawnWhen { todayReady }`. Built first so a baseline APK exists.
2. **Hoje complete** (`EthosApp.kt` `TaskDestination`): on Hoje, while `!todayReady`, show only the existing
   "Carregando tarefas…" (no header cards, no habit items), then everything at once.
3. **StartupSequence** (`core/startup/StartupSequence.kt`): contract in [contracts/startup-sequence.md](
   contracts/startup-sequence.md). Provided as a Hilt singleton; ViewModels default to `StartupSequence.released()`.
4. **Deferred ViewModels**: Calendar, Rewards, Reminders `init` → `launch { awaitTurn(source); observe() }`;
   first emission or failure → `markLoaded`. Entry points needing data (`retry`, Reminders `open`) request first.
5. **Root wiring** (`EthosApp.kt`, `features/shell/StartupViewModel.kt`): `markTodayShown()` when ready;
   `request(destination.deferredSources())` per shown destination; `request(all)` while any `AppRequests` is pending.
6. **Open-time writes**: `TasksViewModel.refresh()` and `HabitsViewModel.settle()` await `awaitTodayShown()` before
   calling the repository; `today` updates stay immediate.
7. **Changed rows**: `core/tasks/data/ChangedRows.kt` (`changedRows`, `TaskWrites.between`); `RoomTaskRepository.write`
   and `RoomHabitRepository.execute` write only changes. Lossless entity round trip asserted by tests.

## Project Structure

### Documentation (this feature)

```text
specs/027-fast-startup/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/startup-sequence.md
├── checklists/requirements.md
└── tasks.md            (next step)
```

### Source Code

```text
app/src/main/java/com/ethos/app/
├── EthosApp.kt                         # readiness, ReportDrawnWhen, requests, Hoje complete
├── TodayReadiness.kt                   # todayReady(), ShellDestination.deferredSources()
├── core/startup/StartupSequence.kt     # DeferredSource, StartupSequence, Hilt binding
├── core/tasks/data/ChangedRows.kt      # changedRows(), TaskWrites
├── core/tasks/data/RoomTaskRepository.kt
├── core/habits/data/RoomHabitRepository.kt
├── features/shell/StartupViewModel.kt
├── features/tasks/TasksViewModel.kt
├── features/habits/HabitsViewModel.kt
├── features/calendar/CalendarViewModel.kt
├── features/rewards/RewardsViewModel.kt
└── features/reminders/ReminderViewModel.kt
app/src/test/java/com/ethos/app/
├── TodayReadinessTest.kt
├── core/startup/StartupSequenceTest.kt
└── core/tasks/data/ChangedRowsTest.kt
app/src/androidTest/java/com/ethos/app/core/tasks/data/ChangedRowsDatabaseTest.kt
```

**Structure Decision**: single `:app` module as today; the new sequence lives in `core/startup` because three
features and the root use it.

## Complexity Tracking

No constitution violations.
