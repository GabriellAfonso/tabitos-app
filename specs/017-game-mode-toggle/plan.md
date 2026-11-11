# Implementation Plan: Game Mode Toggle

**Branch**: `017-game-mode-toggle` (work stays on the current branch; no branch created) | **Date**: 2026-11-11 |
**Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/017-game-mode-toggle/spec.md`

## Summary

A DataStore preference `game_mode` (default ON) decides whether new task/habit outcomes are scored. The
pure `AccountingRules.reconcile` gains a `scoring` flag: when false, an outcome that becomes active is stored
as an **exempt active entitlement without a ledger entry**, while deactivations still reverse what was paid.
Task and habit Room repositories read the mode right before each write transaction. The UI hides the game
layer (drawer Rewards/History, wallet tiles, price settings, difficulty/estimate, financial confirmations,
reward timer sessions) through explicit `gameMode` state, and a new "Modo jogo" switch tops Settings with an
off-confirmation dialog and a block while a reward timer is counting.

## Technical Context

**Language/Version**: Kotlin (AGP 9 built-in Kotlin), Compose + Material 3.

**Primary Dependencies**: existing only — Hilt/KSP, Room 2.8.5, DataStore Preferences, Coroutines/Flow.

**Storage**: DataStore `appearance` store, new boolean key `game_mode`. Room schema 7 unchanged.

**Testing**: JVM (JUnit, fakes): reconcile with `scoring = false`, cross-mode reversal, no retroactive pay,
ViewModels skipping financial preview, game mode ViewModel (confirmation, block), DataStore persistence.
Instrumented tests updated to compile; not executed (standing user rule).

**Target Platform**: Android, minSdk 24, target/compile 36.

**Project Type**: single-module mobile app (`:app`).

**Performance Goals**: switching applies in <1 s (DataStore flow emission recomposes immediately).

**Constraints**: no Activity restart; no data deletion; enforcement in the data layer (FR-004).

**Scale/Scope**: ~15 source files touched, 2 new (game mode repository, Settings switch/ViewModel).

## Constitution Check

*Pre-research and post-design: PASS.*

- I: spec 017 with scope/acceptance exists; implementation requested explicitly by the user in this session.
- II: preference in DataStore; Room remains source of truth; no schema change, so no migration; ledger
  history and entitlements are never deleted or rewritten.
- III: read-only `GameModeSource` contract in `core/preferences`, consumed by core repositories and
  ViewModels; no feature imports another feature's implementation; rule stays in pure `AccountingRules`.
- IV: switch with `Role.Switch`, 56 dp target, texts in `strings.xml`, dialog in Portuguese; no logs of
  content; no new permission.
- V: JVM tests for the scoring rule and switch flow; build/lint run; instrumented tests compile only and
  are reported as not executed.

## Design

1. **Preference** (`core/preferences/GameMode.kt`): `interface GameModeSource { val enabled: Flow<Boolean> }`,
   `interface GameModeRepository : GameModeSource { suspend fun setEnabled(Boolean) }`, DataStore
   implementation on the existing store (`game_mode`, default `true`), `FixedGameMode(on)` for the demo
   seed and tests. Hilt binds both interfaces to the DataStore implementation.
2. **Rule** (`AccountingRules.reconcile(..., scoring: Boolean = true)`): when `scoring` is false and an
   outcome becomes active without a price origin, the entitlement becomes `active = true, exempt = true`
   with no ledger entry. Existing exempt semantics then give: undo while OFF/ON → inactive, non-exempt, no
   entry; re-check while ON → pays (REAPPLY/GAIN); paid outcome undone while OFF → REVERSAL as today.
   Practice-day moves with a price origin keep today's correction path so moving a paid day while OFF is
   not a hidden refund. The default value keeps all existing callers/tests unchanged.
3. **Store/repositories**: `AccountingStore.reconcile(..., scoring)`; `RoomTaskRepository` and
   `RoomHabitRepository` receive a `GameModeSource` and read `enabled.first()` before opening the
   transaction (covers manual, Undo, Settle, timer completion and notification actions — all go through
   `execute`). `TaskModule` passes the injected source; `DemoSeed` uses `FixedGameMode(true)` so the
   fictitious wallet is always seeded and the preference is never written.
4. **ViewModels**: `TasksViewModel` and `HabitsViewModel` get an optional `GameModeSource` (like the existing
   optional `accounting`), expose `gameMode` in their UiState and skip `preview*` (financial confirmation)
   when OFF. Editors read `state.gameMode` to hide difficulty/estimate; drafts keep stored difficulty and
   new drafts already default to difficulty 1.
5. **Settings** (`features/settings/GameModeSetting.kt`): `GameModeViewModel` (preference + timer
   snapshot) with state `enabled, loading, confirmingOff, rewardTimerRunning, error`; content draws the
   switch, a confirm dialog and the blocked explanation. Placed first in Settings.
6. **Shell/app root**: `ShellDestination.isGameOnly` and `drawerFor(gameMode)` (pure); `EthosShell` takes
   `gameMode` and redirects a game-only current destination to Hoje. `EthosApp` hides `WalletTiles`,
   `AccountingSettings`, reward timer sessions and ignores REWARD timer requests while OFF.

## Project Structure

### Documentation (this feature)

```text
specs/017-game-mode-toggle/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── checklists/requirements.md
└── tasks.md            # /speckit-tasks
```

### Source Code

```text
app/src/main/java/com/ethos/app/
├── EthosApp.kt                                   # hide wallet/prices/reward sessions, pass gameMode
├── core/preferences/GameMode.kt                  # NEW contracts + DataStore + FixedGameMode
├── core/preferences/PreferencesModule.kt         # bindings
├── core/accounting/domain/AccountingRules.kt     # scoring flag
├── core/accounting/data/AccountingStore.kt       # pass scoring
├── core/tasks/data/RoomTaskRepository.kt         # read mode before write
├── core/habits/data/RoomHabitRepository.kt       # read mode before write
├── core/tasks/data/TaskModule.kt                 # inject source
├── core/demo/data/DemoSeed.kt                    # FixedGameMode(true)
├── features/shell/ShellModels.kt, EthosShell.kt  # drawer filter + redirect
├── features/settings/GameModeSetting.kt          # NEW ViewModel + switch + dialog
├── features/tasks/TasksViewModel.kt, TaskEditor.kt, TasksContent.kt
├── features/habits/HabitsViewModel.kt, HabitEditor.kt
└── res/values/strings.xml
app/src/test/java/com/ethos/app/...               # JVM tests
app/src/androidTest/...                           # constructor updates (compile only)
```

**Structure Decision**: existing single-module layout; game mode contract in `core/preferences` because both
core repositories and several features consume it.

## Complexity Tracking

No violations.
