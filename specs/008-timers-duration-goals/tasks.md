# Tasks: Cronômetros, metas de tempo e recompensas temporizadas

**Input**: plan.md, spec.md, research.md, data-model.md, contracts/timers.md, quickstart.md
**Tests**: required by spec FR-019 and constitution V.
Paths relative to `app/src/main/java/com/ethos/app/` unless stated.

## Phase 1: Setup
- [X] T001 Declare FOREGROUND_SERVICE/FOREGROUND_SERVICE_SPECIAL_USE, `TimerService` (specialUse + subtype property) and
  `TimerActionReceiver` (not exported) in `app/src/main/AndroidManifest.xml`
- [X] T002 [P] Add Portuguese timer/reward-duration strings in `app/src/main/res/values/strings.xml`

## Phase 2: Foundational
- [X] T003 Create models, `TimerClock`, `TimerProblem`, `TimerRepository` in `core/timers/domain/TimerModels.kt`
- [X] T004 Implement pure `TimerRules` (duration, recovery, cap, day/week allocation, progress, validation) in
  `core/timers/domain/TimerRules.kt`
- [X] T005 [P] JVM tests with fake clock (pause, cap, reboot, 40+50+90, Sunday split, midnight/DST, overlap) in
  `app/src/test/java/com/ethos/app/core/timers/TimerRulesTest.kt`
- [X] T006 Add `Reward.minutes`, timer entities/DAO, `TIMER_MIGRATION_5_6`, schema 6 in `core/timers/data/TimerStorage.kt`,
  `core/rewards/domain/RewardModels.kt`, `core/tasks/data/TaskDatabase.kt`, `core/tasks/data/TaskModule.kt`
- [X] T007 Implement `TimerStore` transaction helpers and `RoomTimerRepository` in `core/timers/data/RoomTimerRepository.kt`

## Phase 3: US1 Contar tempo (P1) — MVP
- [X] T008 [US1] `AndroidTimerClock`, `TimerCoordinator`, notification gateway in `core/timers/platform/TimerPlatform.kt`
- [X] T009 [US1] `TimerViewModel` (live display tick, start/conflict/pause/resume/finish) in `features/timers/TimerViewModel.kt`
- [X] T010 [US1] Timer dialog, conflict dialog and Hoje session cards in `features/timers/TimerContent.kt`;
  `LocalOpenTimer` in `core/ui/timers/TimerControl.kt`
- [X] T011 [US1] Cronômetro buttons in `features/tasks/TasksContent.kt` and `features/habits/HabitsScreen.kt`; wire in
  `MainActivity.kt`

## Phase 4: US2 Metas e confirmação (P1)
- [X] T012 [US2] Goal editing/progress per period and reached prompt in `features/timers/TimerContent.kt`/`TimerViewModel.kt`
- [X] T013 [US2] Route confirmation to existing task completion and habit record (`HabitsViewModel.recordDuration`) in
  `MainActivity.kt`, `features/habits/HabitsViewModel.kt`

## Phase 5: US3 Segundo plano e reinício (P1)
- [X] T014 [US3] `TimerService` (FGS, 10 s checkpoint, shutdown checkpoint, cap stop, alerts) and `TimerActionReceiver`
  in `core/timers/platform/TimerService.kt`
- [X] T015 [US3] Start coordinator in `EthosApplication.kt`, sync on Activity start, `ethos-timer` URI in `MainActivity.kt`;
  notification permission explanation in timer dialog

## Phase 6: US4 Recompensa temporizada (P2)
- [X] T016 [US4] `confirm(expected, pauseRunning)` + RUNNING_CONFLICT + session insert in
  `core/rewards/data/RoomRewardRepository.kt`, `core/rewards/domain/RewardRepository.kt`
- [X] T017 [US4] Duration field, pause-confirmation, reward sessions in `features/rewards/RewardsViewModel.kt`,
  `features/rewards/RewardsContent.kt`; update `app/src/test/java/com/ethos/app/core/rewards/RewardsViewModelTest.kt`

## Phase 7: US5 Tempo manual e correções (P2)
- [X] T018 [US5] Manual add/correct/delete with audit and below-goal warning in `features/timers/TimerContent.kt`,
  `features/timers/TimerViewModel.kt`

## Phase 8: Polish & verification
- [X] T019 [P] Room migration 5→6 + full chain, repository and purchase idempotency tests in
  `app/src/androidTest/java/com/ethos/app/core/timers/TimerDatabaseTest.kt`
- [X] T020 Build/JVM/lint/release/androidTest (+connected if available); runtime notification check; write
  `specs/008-timers-duration-goals/verification.md`; update spec status and `AGENTS.md` context lines

## Dependencies
Setup → Foundational (T003→T004→T006→T007) → US1 → US2/US3 (independent) → US4 (needs US1/US3 platform) → US5 → Polish.
Parallel: T002, T005, T019 against implementation of other files.

## Strategy
MVP = US1 (+ foundational). Then US2, US3, US4, US5; verify once at the end and on each compile.
