# Research: Game Mode Toggle

## R1 — How to stop scoring while keeping the ledger consistent

- **Decision**: add `scoring: Boolean` to `AccountingRules.reconcile`; when false, newly active outcomes
  (without a price origin) become exempt active entitlements with no ledger entry.
- **Rationale**: the existing `exempt` flag already means "this outcome exists but never paid": deactivating
  it creates no entry and releases it (`exempt = false`), and re-activating a released entitlement while
  scoring pays normally. This gives every rule of the spec (no pay while OFF, no retroactive pay, reversal
  of paid outcomes across modes, re-check while ON pays) without new columns or schema change.
- **Alternatives**: skip `reconcile` entirely while OFF — rejected: paid outcomes undone while OFF would not
  be reversed (FR-006) and entitlements would drift from outcomes, so turning ON would pay all of them
  retroactively at the next reconcile. A new `suspended` column — rejected: needs migration and duplicates
  `exempt`.

## R2 — Where to read the mode

- **Decision**: repositories read `GameModeSource.enabled.first()` before the Room transaction.
- **Rationale**: enforces the rule in the data layer for every entry point (UI, Undo, Settle, timer
  completion, notification actions). Reading before the transaction avoids suspending on DataStore while
  holding the database lock.
- **Alternatives**: ViewModel passes a flag in each action — rejected: Settle/timer paths could forget it.

## R3 — Practice-day moves with a price origin while OFF

- **Decision**: keep the current correction path (origin price re-applied).
- **Rationale**: moving the date of a paid practice day is a correction, not a new outcome; treating it as
  new while OFF would reverse the original and pay nothing, a hidden loss.

## R4 — Preference storage and failures

- **Decision**: key `game_mode` in the existing `appearance` DataStore, default `true`. A read failure in
  Settings shows the existing read-error card pattern; in repositories the failure propagates as a storage
  error (no silent scoring).
- **Alternatives**: separate DataStore file — rejected: no need, same lifecycle as theme/alarm.

## R5 — Blocking while a reward timer counts

- **Decision**: `GameModeViewModel` observes `TimerRepository` and disables the switch while a session with
  `kind == REWARD` is `RUNNING` (same rule shape as demo restore).

## R6 — Hiding UI

- **Decision**: explicit `gameMode` in UiStates and parameters (no CompositionLocal) so previews/tests can set
  it; drawer filtering in pure `ShellDestination.drawerFor(gameMode)`; redirect of game-only destinations to
  Hoje in the shell.
