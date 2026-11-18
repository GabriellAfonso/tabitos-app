# Tasks: Fast app startup (Today first, staged preloading)

**Input**: Design documents from `specs/027-fast-startup/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/startup-sequence.md, quickstart.md

**Tests**: required by the spec (FR-009/SC-004) and the constitution (persistence affected).

## Phase 1: Setup (measurement baseline)

- [X] T001 Add `todayReady(...)` over the loading flags of game mode, tasks, habits, timers, trackers and wallet in
  `app/src/main/java/com/ethos/app/TodayReadiness.kt`
- [X] T002 Call `ReportDrawnWhen { todayReady }` in `EthosApp` (`app/src/main/java/com/ethos/app/EthosApp.kt`)
- [X] T003 Build `:app:assembleDebug` and copy the APK to the session scratchpad as the baseline (no install)

## Phase 2: Foundational

- [X] T004 [P] Create `DeferredSource`, `PRELOAD_ORDER` and `StartupSequence` (`markTodayShown`, `request`,
  `markLoaded`, `awaitTodayShown`, `awaitTurn`, `released()`), Hilt singleton, in
  `app/src/main/java/com/ethos/app/core/startup/StartupSequence.kt`
- [X] T005 [P] JVM tests for the sequence contract (order, request skips the queue, failure releases next,
  idempotence, `released()`) in `app/src/test/java/com/ethos/app/core/startup/StartupSequenceTest.kt`
- [X] T006 [P] JVM tests for `todayReady` in `app/src/test/java/com/ethos/app/TodayReadinessTest.kt`

## Phase 3: User Story 1 — Hoje appears quickly (P1) 🎯 MVP

**Goal**: Hoje's reads never queue behind other sources or the open-time writes; Hoje appears complete.
**Independent test**: cold open shows Hoje before Calendar/Rewards/Reminders start; `Fully drawn` time is lower.

- [X] T007 [US1] `TasksViewModel.refresh()` awaits `awaitTodayShown()` before `TaskAction.Refresh` (today updates at
  once) in `app/src/main/java/com/ethos/app/features/tasks/TasksViewModel.kt`
- [X] T008 [US1] `HabitsViewModel.settle()` awaits `awaitTodayShown()` in
  `app/src/main/java/com/ethos/app/features/habits/HabitsViewModel.kt`
- [X] T009 [US1] Hoje shows only the loading text until `todayReady`, then header cards and habits together
  (`TaskDestination` in `app/src/main/java/com/ethos/app/EthosApp.kt`)
- [X] T010 [US1] Create `StartupViewModel` exposing `markTodayShown`/`request` in
  `app/src/main/java/com/ethos/app/features/shell/StartupViewModel.kt`; `EthosApp` marks Hoje shown when ready

## Phase 4: User Story 2 — Other pages preloaded (P2)

**Goal**: Calendar, Rewards and Reminders start after Hoje, one at a time; opening their page starts them at once.
**Independent test**: wait 5 s on Hoje, open each page: no loading; open Recompensas right after launch: loads at once.

- [X] T011 [P] [US2] Calendar waits `awaitTurn(CALENDAR)`, marks loaded on first emission/failure; `retry` requests,
  in `app/src/main/java/com/ethos/app/features/calendar/CalendarViewModel.kt`
- [X] T012 [P] [US2] Rewards waits `awaitTurn(REWARDS)`, marks loaded; `retry` requests, in
  `app/src/main/java/com/ethos/app/features/rewards/RewardsViewModel.kt`
- [X] T013 [P] [US2] Reminders waits `awaitTurn(REMINDERS)`, marks loaded; `open`/`edit`/`retry` request, in
  `app/src/main/java/com/ethos/app/features/reminders/ReminderViewModel.kt`
- [X] T014 [US2] Add `ShellDestination.deferredSources()` to `app/src/main/java/com/ethos/app/TodayReadiness.kt` and
  request them for each shown destination in `app/src/main/java/com/ethos/app/EthosApp.kt`
- [X] T015 [P] [US2] JVM tests for `deferredSources()` in `app/src/test/java/com/ethos/app/TodayReadinessTest.kt`

## Phase 5: User Story 3 — Notifications and links keep working (P2)

**Goal**: a pending notification/popup request never waits for the preload queue.
**Independent test**: with the app closed, tap each notification type and action; same behavior as before.

- [X] T016 [US3] While any `AppRequests` request is pending, request every deferred source, in
  `app/src/main/java/com/ethos/app/EthosApp.kt`

## Phase 6: User Story 4 — Saving writes only changes (P3)

**Goal**: task and habit saves write only changed rows; database identical to the full rewrite.
**Independent test**: changed-rows model equals full rewrite for each action; no-change save writes nothing.

- [X] T017 [US4] Create `changedRows(before, after, key)` and `TaskWrites.between(before, after)` in
  `app/src/main/java/com/ethos/app/core/tasks/data/ChangedRows.kt`
- [X] T018 [US4] `RoomTaskRepository.write` writes only `TaskWrites` in
  `app/src/main/java/com/ethos/app/core/tasks/data/RoomTaskRepository.kt`
- [X] T019 [US4] `RoomHabitRepository.execute` upserts only changed rows per table in
  `app/src/main/java/com/ethos/app/core/habits/data/RoomHabitRepository.kt`
- [X] T020 [P] [US4] JVM tests: entity round trip lossless; table model after changed rows equals full rewrite for
  create, edit, resolve, postpone, reorder, checklist change and delete; no change → no rows, in
  `app/src/test/java/com/ethos/app/core/tasks/data/ChangedRowsTest.kt`
- [X] T021 [P] [US4] Instrumented Room test: after real task and habit actions the database equals the rules' result;
  no-change refresh/settle causes no invalidation, in
  `app/src/androidTest/java/com/ethos/app/core/tasks/data/ChangedRowsDatabaseTest.kt` (not run without authorization)

## Phase 7: Polish

- [X] T022 Run `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` and `:app:assembleRelease`
- [X] T023 Write `specs/027-fast-startup/verification.md` (results, baseline APK path, measurement steps pending on
  the phone, tests not executed); mark spec status and update the SPECKIT block in `AGENTS.md`

## Dependencies

- T001 → T002 → T003 (baseline before any behavior change).
- T004 blocks T007–T016. T017 blocks T018–T020.
- US1 (T007–T010) before US2/US3 (they rely on `markTodayShown`). US4 is independent of US1–US3.

## Parallel examples

- T004, T005, T006 together; T011, T012, T013 together; T020, T021 together.

## Implementation strategy

MVP = Phase 1–3 (measurement + Hoje first). Then US2/US3 (preloading, routing), then US4 (lighter saves), then
verification.
