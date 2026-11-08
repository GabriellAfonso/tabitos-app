# Implementation Plan: Calendário completo e acompanhamentos personalizados

**Branch**: current branch retained | **Date**: 2026-11-07 | **Spec**: [spec.md](spec.md)
**Input**: specs/009-calendar-habit-trackers/spec.md

## Summary
Per-habit trackers with immutable form versions (questions and options copied per version, stable ids across
versions), dated entries bound to the version they answered, and answers stored in their own table. A pure
`TrackerRules` validates forms/entries and computes pending state and analytics; a Room repository writes only
tracker tables, so no accounting/habit path is touched. The Calendário tab becomes a `features/calendar` screen with
month/week/day views and type/list/habit filters, built from task, habit and tracker snapshots by a pure
`CalendarModel`. Tracker reminders extend `core/reminders` with `ReminderKind.TRACKER` (daily WEEKDAYS definition,
neutral ongoing notification, Preencher/Adiar, today-only relevance so no burst). Room 6→7 additive. No new libraries.
Details: [research.md](research.md), [data-model.md](data-model.md), [contracts/trackers.md](contracts/trackers.md).

## Technical Context
**Language/Version**: existing Kotlin 2.3 / AGP 9 built-in Kotlin, JVM 17.
**Primary Dependencies**: existing Compose Material 3 (foundation Canvas for charts), Hilt/KSP, Room 2.8.5,
Coroutines/Flow, Navigation Compose. No new dependency.
**Storage**: Room schema 7 (new `trackers`, `tracker_versions`, `tracker_questions`, `tracker_options`,
`tracker_entries`, `tracker_answers`); reminder definitions reuse `reminder_definitions` with kind `TRACKER`.
DataStore unchanged.
**Testing**: JUnit for `TrackerRules`, `CalendarModel`, `ReminderRules` tracker cases with fixed dates; Room migration
6→7 and full 1→7 chain, repository constraints; Compose UI tests for builder/entry/calendar; notification runtime check.
**Target Platform**: Android 24–36, target 36.
**Project Type**: offline native app, single `:app`.
**Performance Goals**: calendar month derivation over personal-scale data (hundreds of occurrences/records/entries)
in a single pass per emission; analytics over ≤ 90 days.
**Constraints**: notification text neutral; no logs of tracker content; no FGS for persistence; ongoing flag is
best-effort (Android 14+ dismissible).
**Scale/Scope**: one tracker per habit, tens of questions, thousands of entries at most.

## Constitution Check
Pre-research and post-design:
- I PASS: spec exists; user explicitly authorized specify → plan → tasks → implement in this chat.
- II PASS: Room source of truth; additive preserving 6→7 migration tested after the 1→6 chain; unique index enforces
  one entry per day in single mode; failures surface as recoverable errors. Dates use device civil date (R4).
- III PASS: pure `core/trackers/domain` (depends only on core habit domain models); data in `core/trackers/data`;
  UI in `features/trackers` and `features/calendar`. Habits feature opens trackers via `core/ui` CompositionLocal;
  calendar receives callbacks from `MainActivity`. No feature imports another feature.
- IV PASS: Portuguese resources, neutral private notification, existing contextual permission, value buttons for
  scales (TalkBack/large font), calendar cell descriptions, loading/empty/content/error states.
- V PASS: rule tests with fixed dates, migration/repository tests, UI and notification evidence, honest gaps.
No deviations. Hooks do not authorize commits; none requested.

## Project Structure
Docs: `specs/009-calendar-habit-trackers/` (spec, plan, research, data-model, contracts, quickstart, tasks,
verification).
Source `app/src/main/java/com/ethos/app/`:
- `core/trackers/domain/`: `TrackerModels.kt` (models, actions, problems, repository), `TrackerRules.kt`
  (validation, versioning, entry rules, pending, analytics).
- `core/trackers/data/`: `TrackerStorage.kt` (entities, DAO, `TRACKER_MIGRATION_6_7`), `RoomTrackerRepository.kt`.
- `core/ui/trackers/`: `LocalOpenTracker`.
- `features/trackers/`: `TrackerViewModel`, `TrackerContent` (hub, builder, entry form, analytics chart, delete
  confirmation, Hoje pending card).
- `features/calendar/`: `CalendarModel` (pure), `CalendarViewModel`, `CalendarContent`.
- Changes: `core/reminders` (kind TRACKER, sources include trackers, schedule/relevance/snooze, neutral ongoing
  notification with FILL action), `ReminderViewModel.request` (FILL), habit detail (Acompanhamento button),
  `TasksContent` (remove CALENDAR page), `MainActivity` (wiring, Calendário content, notification routing, Hoje card),
  `TaskDatabase`/`TaskModule` (schema 7), strings.
Tests: `app/src/test/.../core/trackers/TrackerRulesTest.kt`, `.../features/calendar/CalendarModelTest.kt`,
`ReminderRulesTest` additions; `app/src/androidTest/.../core/trackers/TrackerDatabaseTest.kt`,
`.../features/trackers/TrackerUiTest.kt`, `TrackerReminderRuntimeTest.kt`; existing DB tests add 6→7.

## Key design
- Save form: validate draft; new tracker → version 1, created today; existing → new version only when the question
  structure differs from the current version; frequency mode is a tracker field (not versioned). Questions/options
  keep their ids; removed options simply are absent from the new version.
- Save entry: tracker active, habit not excluded, date in created..today; new entry uses current version, editing
  keeps the entry's version; validate answers against that version; single mode rejects a second entry on a date
  (`dayKey` unique). Answers replaced atomically in one transaction.
- Archive/restore instead of destructive removal: archived trackers are read-only, keep history and stop reminders.
- Pending (in-app): reminder enabled, tracker fillable today, reminder time passed, no entry today.
- Reminder: definition id `tracker:<trackerId>`, WEEKDAYS 127 at chosen minute; schedule today..+32; relevance only
  on `slotDate == date` and without entry → past slots cancel at midnight, at most one notice per tracker; tracker rows
  due are posted directly on any reconcile (never routed into the recovery summary).
- Calendar: tasks per date use the previous `TaskOccurrence.inDay` (parity); habit records by actual date; tracker
  entries by reference date; filters AND-combined; habit filter hides tasks; list filter applies to tasks and habits
  (trackers via their habit).
- Analytics: last N civil days ending today; primary question id from current version; points per entry with its own
  version range; gaps for days without entries; option counts by option id with historical label.

## Verification
Follow [quickstart.md](quickstart.md); record results/limits in verification.md.
