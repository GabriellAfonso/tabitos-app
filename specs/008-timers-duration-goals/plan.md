# Implementation Plan: Cronômetros, metas de tempo e recompensas temporizadas

**Branch**: current branch retained | **Date**: 2026-11-07 | **Spec**: [spec.md](spec.md)
**Input**: specs/008-timers-duration-goals/spec.md

## Summary
Source-bound sessions (task occurrence, practice habit, timed reward purchase) built from persisted segments measured
by the monotonic clock, with boot-count recovery, objective caps, civil day/week allocation, audited manual time and
corrections. One foreground `specialUse` service keeps a running session visible and checkpointed. Goals live in a
separate table and confirm through existing task/habit/accounting flows. Timed rewards start their countdown inside
the purchase transaction. Room 5→6 additive. No new libraries. Details: [research.md](research.md),
[data-model.md](data-model.md), [contracts/timers.md](contracts/timers.md).

## Technical Context
**Language/Version**: existing Kotlin 2.3 / AGP 9 built-in Kotlin, JVM 17.
**Primary Dependencies**: existing Compose Material 3, Hilt/KSP, Room 2.8.5, Coroutines/Flow; platform FGS/notifications.
**Storage**: Room schema 6 (new `timer_*` tables, `reward_minutes` columns); DataStore unchanged.
**Testing**: JUnit with fake `TimerClock`; Room migration/repository instrumentation; Compose UI smoke; runtime FGS check.
**Target Platform**: Android 24–36, target 36.
**Project Type**: offline native app, single `:app`.
**Performance Goals**: UI display tick 1 s only while visible; service wakes ≤ every 10 s; queries bounded by personal data.
**Constraints**: force-stop/OEM kills stop checkpoints (≤10 s loss); no exact alarm or battery exemption; no personal logs.
**Scale/Scope**: one running session, a few paused sessions, hundreds of segments.

## Constitution Check
Pre-research and post-design:
- I PASS: spec exists; user explicitly authorized specify → plan → tasks → implement in this chat.
- II PASS: offline Room source of truth, additive preserving migration 5→6 with test, failures surfaced as errors.
  Dates/zone/reboot rules defined in research R1–R5.
- III PASS: pure `core/timers/domain`; data/platform in `core/timers`; UI in `features/timers`. Rewards core calls a
  timer data helper inside the shared transaction (core-to-core). Features connect by CompositionLocal/callbacks.
  New FGS justified by checkpoint/cap needs (R3). No new modules/libraries.
- IV PASS: Portuguese resources, private notifications, contextual permission reuse, accessible buttons/labels,
  loading/error states, themes inherited.
- V PASS: rule tests with controlled clocks, migration/repository tests, runtime evidence recorded honestly.
No deviations. Hooks do not authorize commits; none requested.

## Project Structure
Docs: `specs/008-timers-duration-goals/` (spec, plan, research, data-model, contracts, quickstart, tasks, verification).
Source `app/src/main/java/com/ethos/app/`:
- `core/timers/domain/`: models, `TimerClock`, `TimerRules`, `TimerRepository`.
- `core/timers/data/`: entities, DAO, `TIMER_MIGRATION_5_6`, `TimerStore` (transaction helpers), `RoomTimerRepository`.
- `core/timers/platform/`: `AndroidTimerClock`, `TimerCoordinator`, `TimerService`, `TimerActionReceiver`, notifications.
- `core/ui/timers/`: `LocalOpenTimer`.
- `features/timers/`: `TimerViewModel`, `TimerContent` (dialog, Hoje cards, history, manual editor).
- Changes: rewards domain/data/UI (minutes, confirm with pauseRunning), task/habit details (Cronômetro button),
  MainActivity (URI, dialogs, confirmation routing), TaskDatabase/TaskModule (schema 6), manifest, strings.
Tests: `app/src/test/.../core/timers/TimerRulesTest.kt`, rewards VM tests updated;
`app/src/androidTest/.../core/timers/TimerDatabaseTest.kt` (migration, repository, purchase).

## Key design
- Start: transaction; if another RUNNING and !pauseOther → CONFLICT; else pause it (close open segment, audit PAUSE),
  reuse the open session of the same source (resume) or create one, insert open segment with limit = remaining
  (task/habit goal or reward purchase), audit START/RESUME. Reject when remaining = 0 (prompt confirm instead).
- Sync (coordinator, app start, Activity start, service loop): recover boot mismatch (close at checkpoint, PAUSED,
  RECOVERED), close capped segment (PAUSED, reached, audit REACHED/EXHAUSTED), pause if source no longer
  OPEN/ACTIVE. Then start or stop the service; alerts posted once per new REACHED/EXHAUSTED audit id.
- Goal confirmation: UI prompt for reached sessions; Confirmar → mark session FINISHED + CONFIRMED audit, then route to
  task completion (existing checklist/financial dialogs) or habit record (financial preview); the domain prevents
  double awards (NOT_OPEN, existing-record check). Agora não → keep paused, dismissable.
- Reward purchase: `RewardsViewModel.confirm()` checks running session from `TimerRepository`; if present, shows
  pause confirmation first; repository revalidates inside the transaction (RUNNING_CONFLICT if changed).
- Display: UI computes live elapsed from snapshot + clock each second while visible (display only).

## Verification
Follow [quickstart.md](quickstart.md); record results/limits in verification.md.
