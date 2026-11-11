# Tasks: Game Mode Toggle

**Input**: `specs/017-game-mode-toggle/` (spec.md, plan.md, research.md, data-model.md, quickstart.md)
**Tests**: required by the spec (JVM tests for scoring OFF, cross-mode reversal, no retroactive pay,
preference persistence). Instrumented tests are only updated to compile.

## Phase 1: Setup

- [X] T001 Add Portuguese strings for the switch, hint, off-dialog, blocked explanation in
  app/src/main/res/values/strings.xml

## Phase 2: Foundational

- [X] T002 Create `GameModeSource`, `GameModeRepository`, `DataStoreGameModeRepository` (key `game_mode`,
  default true) and `FixedGameMode` in app/src/main/java/com/ethos/app/core/preferences/GameMode.kt
- [X] T003 Bind `GameModeRepository` and `GameModeSource` in
  app/src/main/java/com/ethos/app/core/preferences/PreferencesModule.kt
- [X] T004 [P] JVM test for preference default/persistence/theme isolation in
  app/src/test/java/com/ethos/app/core/preferences/GameModeRepositoryTest.kt

## Phase 3: User Story 2 — Nothing counts while off (P1, data layer first)

**Independent test**: reconcile with scoring off creates no entries; reversal across modes; no retroactive pay.

- [X] T005 [US2] Add `scoring` flag to `AccountingRules.reconcile` (exempt active without entry, origin
  correction path preserved) in app/src/main/java/com/ethos/app/core/accounting/domain/AccountingRules.kt
- [X] T006 [US2] Pass `scoring` through `AccountingStore.reconcile` in
  app/src/main/java/com/ethos/app/core/accounting/data/AccountingStore.kt
- [X] T007 [US2] Read `GameModeSource` before the transaction in
  app/src/main/java/com/ethos/app/core/tasks/data/RoomTaskRepository.kt and
  app/src/main/java/com/ethos/app/core/habits/data/RoomHabitRepository.kt
- [X] T008 [US2] Inject the source in app/src/main/java/com/ethos/app/core/tasks/data/TaskModule.kt; use
  `FixedGameMode(true)` in app/src/main/java/com/ethos/app/core/demo/data/DemoSeed.kt
- [X] T009 [US2] Update instrumented constructors (`RoomTaskRepository`/`RoomHabitRepository`) in
  app/src/androidTest/java/com/ethos/app/**
- [X] T010 [P] [US2] JVM tests (no entry while OFF, avoid penalty OFF, paid undone OFF reverses, OFF outcome
  undone and re-checked ON pays once, mode ON later pays nothing retroactively) in
  app/src/test/java/com/ethos/app/core/accounting/GameModeScoringTest.kt

## Phase 4: User Story 1 — Turn the game layer off (P1)

**Independent test**: with mode OFF, no game element is visible and no financial confirmation appears.

- [X] T011 [US1] `TasksViewModel`: optional `GameModeSource`, `gameMode` in `TasksUiState`, skip preview when
  OFF in app/src/main/java/com/ethos/app/features/tasks/TasksViewModel.kt
- [X] T012 [US1] `HabitsViewModel`: same in app/src/main/java/com/ethos/app/features/habits/HabitsViewModel.kt
- [X] T013 [US1] Hide difficulty selector and estimate when OFF in
  app/src/main/java/com/ethos/app/features/tasks/TaskEditor.kt and TasksContent.kt, and
  app/src/main/java/com/ethos/app/features/habits/HabitEditor.kt
- [X] T014 [US1] `ShellDestination.isGameOnly`/`drawerFor(gameMode)` in
  app/src/main/java/com/ethos/app/features/shell/ShellModels.kt; `EthosShell` takes `gameMode`, filters the
  drawer and redirects game-only destinations to Hoje in EthosShell.kt
- [X] T015 [US1] `EthosApp.kt`: pass `gameMode`, hide `WalletTiles`, `AccountingSettings`, reward timer
  sessions in Hoje, ignore REWARD timer requests while OFF
- [X] T016 [P] [US1] JVM tests: ViewModels skip financial preview when OFF and keep it when ON
  (app/src/test/java/com/ethos/app/features/tasks/TasksViewModelTest.kt,
  app/src/test/java/com/ethos/app/features/habits/HabitsViewModelTest.kt); drawer filter in
  app/src/test/java/com/ethos/app/features/shell/ShellModelsTest.kt

## Phase 5: User Story 3 — Turn back on (P2) and User Story 4 — Block with reward timer (P3)

- [X] T017 [US3] `GameModeViewModel` + `GameModeSetting` (switch, off confirmation, blocked state, errors,
  previews) in app/src/main/java/com/ethos/app/features/settings/GameModeSetting.kt, placed first in Settings
  in EthosApp.kt
- [X] T018 [US4] Block switching while a REWARD session is RUNNING in GameModeSetting.kt
- [X] T019 [P] [US3] JVM tests: off asks confirmation, cancel keeps ON, on applies directly, blocked while
  reward timer runs in app/src/test/java/com/ethos/app/features/settings/GameModeViewModelTest.kt

## Phase 6: Polish

- [X] T020 Run `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` and
  `:app:compileDebugAndroidTestKotlin`; record results in specs/017-game-mode-toggle/verification.md, mark
  spec Implemented, update AGENTS.md summary

## Dependencies

T001–T003 → T005–T008 → T011–T015 → T017–T018 → T020. Tests T004, T010, T016, T019 after their phase code.
US2 is the data-layer base for US1; US3/US4 depend on the repository from T002.

## Parallel examples

- T004 alongside T005; T010 alongside T011; T016 alongside T017.

## Strategy

MVP = Phases 1–4 (rule + hidden UI). Then Settings switch (Phase 5) makes it user-reachable; the app is
only shippable after Phase 5.
