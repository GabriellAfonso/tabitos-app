# Implementation Plan: Habits Without Debt

**Branch**: `master` | **Date**: 2026-11-10 | **Spec**: [spec.md](spec.md)

Plan, tasks and implementation were run in one autonomous session explicitly authorized by the user
("vai fazendo e quando acabar roda o git-commit").

## Summary

Period outcomes are computed, so the rule change lives in `HabitRules.periods`: a period is open until its
end plus `LATE_DAYS = 2`, then closes as SUCCESS or FAILURE; PENDING is never produced. Records and
corrections are rejected for closed periods. The ledger projection drops penalties for missed periods
(keeping penalties confirmed by legacy reviews, so history is not refunded) and rewards avoid/limit
success on closing without review. A new `HabitAction.Settle` reconciles all habit owners so automatic
closings reach the ledger; `HabitsViewModel` runs it on first load and when the day changes. UI removes
habit review, partial target and preview, adds streak + success rate, calendar marks, plain practice
frequencies, and the "Quebrar hábito" entry.

## Technical Context

Kotlin 2.3.10, Compose BOM 2026.02.00, Room 2.8.5 schema 7 (unchanged, no migration: reviews table kept as
history). JUnit4 JVM tests; instrumented tests updated but run only with user authorization.

## Constitution Check

- I: spec 014 written with user decisions; execution explicitly authorized. ✅
- II: no schema change; stored reviews/records preserved; failures keep existing error/retry paths. ✅
- III: rules in `domain/` (no Android); ViewModel triggers settle; Composables only render. ✅
- IV: texts in resources; calendar marks have spoken labels and symbols, not color only. ✅
- V: JVM tests rewritten for the new rules (window, auto success, closed-period guard, streak, rate,
  partial default, no penalty, legacy penalty kept). Instrumented tests not run (see verification). ✅

## Source changes

- `core/habits/domain/HabitRules.kt`, `HabitModels.kt` (Settle), `core/habits/data/RoomHabitRepository.kt`
- `core/accounting/domain/AccountingProjection.kt`, `core/accounting/data/RoomAccountingRepository.kt`
- `features/habits/HabitsViewModel.kt`, `HabitsContent.kt`, `HabitsScreen.kt`, `HabitEditor.kt`
- `features/shell/TodayCreateButton.kt`, `EthosShell.kt`, `MainActivity.kt`, `features/tasks/TasksContent.kt`
- `res/values/strings.xml`
