# Implementation Plan: Statistics page (with history) and erasing history of deleted items

**Branch**: `022-statistics-history` (no branch; current branch) | **Date**: 2026-11-17 |
**Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/022-statistics-history/spec.md`

## Summary

New `features/statistics` page reached from the ☰ menu, with tabs Resumo and Histórico sharing one calendar period
(week/month/year/all). All numbers and history items are computed by pure Kotlin functions from the snapshots the
app already loads (tasks, habits, timers, accounting, rewards); the page is read-only. "Histórico de moedas" leaves
Configurações and its ledger entries become the Moedas filter. Erasing the history of a deleted task/habit is a new
explicit, atomic operation: task erasure is expressed as pure `TaskRules` actions (testable on the JVM) whose
removed rows the repository deletes; habit erasure is a new repository operation; both clean timer sessions,
reminders and anonymize ledger titles in the same Room transaction. No schema change (still version 7).

## Technical Context

**Language/Version**: Kotlin 2.3.10 (AGP 9.0.1 built-in Kotlin)

**Primary Dependencies**: Jetpack Compose (BOM 2026.02.00) + Material 3, Navigation Compose 2.9.8, Hilt 2.59.2,
Room 2.8.5, Coroutines/Flow 1.10.2. No new dependency.

**Storage**: Room `ethos-tasks.db` schema 7 (unchanged); new DAO delete/update queries only. No DataStore key.

**Testing**: JUnit 4 + kotlinx-coroutines-test (JVM); Room/Compose instrumented tests written, not executed
without explicit authorization.

**Target Platform**: Android (minSdk 24, target/compile 36)

**Project Type**: single-module Android app (`:app`)

**Performance Goals**: period/filter change without perceptible wait for a year of typical personal data;
computation off the main thread (`Dispatchers.Default`), history paged by 50 items.

**Constraints**: offline; read-only page; erase in one transaction; no `now()` in UI/rules (`TaskClock`,
`TimerClock.zone()`); texts in `strings.xml`; 120-column lines.

**Scale/Scope**: one person's data (hundreds to a few thousand tasks/records per year).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- I: Spec 022 exists with scope and acceptance; the user explicitly requested specify → plan → tasks → implement
  in this conversation. PASS.
- II: Offline, Room stays source of truth; no schema change, so no migration; erase is explicit, confirmed and
  transactional; balances are never recomputed. The period is in memory by decision (FR-007), not a preference.
  PASS.
- III: `features/statistics` reads core repositories only (no feature imports other feature implementations);
  calculations in pure functions (`StatisticsSummary.kt`, `StatisticsHistory.kt`, `StatisticsPeriod.kt`); task
  erasure in `TaskRules` (domain, no Android); cross-table cleanup in `core/history/data`. No new module or
  dependency. PASS.
- IV: Screen/Content split, loading/empty/content/error states, light/dark, TalkBack descriptions for bars,
  arrows and items, 48 dp targets, no personal data in logs; erasing personal data only by explicit choice. PASS.
- V: JVM tests for periods, summary numbers, history items/filters, task erasure rules; instrumented tests for DAO
  erasure written and recorded as not executed. PASS.

Post-design re-check: PASS (no violations; Complexity Tracking empty).

## Project Structure

### Documentation (this feature)

```text
specs/022-statistics-history/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/ui.md
├── checklists/requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
app/src/main/java/com/ethos/app/
├── EthosApp.kt                                  # STATISTICS destination; HISTORY removed from Settings
├── core/
│   ├── history/data/HistoryErasure.kt           # NEW: cross-table cleanup + ledger anonymization
│   ├── habits/domain/HabitRules.kt              # rate over a date range (shared with successRate)
│   ├── habits/domain/HabitRepository.kt         # + erase(habitId)
│   ├── habits/data/{HabitDao,RoomHabitRepository}.kt
│   ├── tasks/domain/{TaskModels,TaskRules}.kt   # Delete(eraseHistory), EraseHistory
│   ├── tasks/data/{TaskDao,RoomTaskRepository}.kt
│   ├── timers/data/TimerStorage.kt              # delete queries
│   ├── reminders/data/ReminderStorage.kt        # delete queries
│   ├── trackers/data/TrackerStorage.kt          # delete queries
│   └── accounting/data/AccountingDao.kt         # anonymize queries
├── features/
│   ├── statistics/                              # NEW feature
│   │   ├── StatisticsPeriod.kt                  # period kinds, ranges, labels data, navigation
│   │   ├── StatisticsSummary.kt                 # Resumo numbers
│   │   ├── StatisticsHistory.kt                 # history items, filters, ledger matching, paging
│   │   ├── StatisticsViewModel.kt               # combine sources, period store, erase
│   │   ├── StatisticsScreen.kt                  # collects state
│   │   └── StatisticsContent.kt                 # tabs, period row, Resumo, Histórico, dialogs
│   ├── accounting/AccountingContent.kt          # AccountingHistory removed
│   ├── accounting/AccountingPresentation.kt     # removed (historyDays)
│   ├── shell/ShellModels.kt                     # STATISTICS in drawer; HISTORY removed
│   ├── tasks/TasksContent.kt                    # delete dialog checkbox
│   └── habits/{HabitsScreen,HabitsViewModel}.kt # exclude dialog checkbox, "Apagar por completo"
└── res/values/strings.xml, res/drawable/ic_bar_chart.xml, ic_chevron_left.xml

app/src/test/java/com/ethos/app/
├── features/statistics/{StatisticsPeriodTest,StatisticsSummaryTest,StatisticsHistoryTest}.kt
└── core/tasks/domain/TaskEraseTest.kt
app/src/androidTest/java/com/ethos/app/core/history/HistoryErasureTest.kt
```

**Structure Decision**: one new feature package plus small core additions; existing composition root wires it.

## Complexity Tracking

None.
