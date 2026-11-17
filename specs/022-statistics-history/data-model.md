# Data model: spec 022

No persisted entity is added or changed (Room schema 7). The models below are in-memory, immutable.

## StatisticsPeriod
- `kind: PeriodKind` (WEEK, MONTH, YEAR, ALL), `anchor: LocalDate` (any day inside the period; ignored for ALL).
- Derived: `range(firstDay, today): ClosedRange<LocalDate>` (WEEK Monday–Sunday, MONTH calendar month, YEAR
  calendar year, ALL `firstDay..today`), `previous()`, `next()`, `hasPrevious(firstDay)` (start > firstDay),
  `hasNext(today)` (not the current period), `current(kind, today)`.
- Label data `PeriodLabel`: THIS_WEEK, LAST_WEEK, WEEK_RANGE(start, end), MONTH(yearMonth), YEAR(year),
  ALL(since); formatted to text in the composable layer with resources.
- Validation: ALL has no previous/next.

## BucketUnit / Bucket
- `BucketUnit`: DAY, MONTH, YEAR. `Bucket(start: LocalDate, unit, count: Int, future: Boolean)`.

## TaskSummary
- `completed: Int`, `onTimePercent: Int?`, `postponed: Int`, `buckets: List<Bucket>`.

## HabitSummary
- `items: List<HabitStat>` with `HabitStat(id, name, color, icon, bad: Boolean, ratePercent: Int?, streak: Int,
  unit: HabitUnit)`; `overallPercent: Int?`. Only ACTIVE/PAUSED habits.

## TimeSummary
- `totalMillis: Long`, `items: List<TimeStat(kind, sourceId, title, millis)>` sorted by millis desc.

## CoinSummary (game mode only)
- `earned: Long`, `spent: Long`, `progress: Progress`, `totalXp: Long`.

## HistoryItem
- `id: String` (stable key), `moment: Long`, `day: LocalDate`, `kind: HistoryKind`, `subject: HistorySubject`,
  `title: String`, `from: LocalDate?`, `to: LocalDate?`, `minutes: Long?`, `coins: Long?`, `xp: Long?`,
  `adjustment: Boolean`, `ledgerKind: String?`.
- `HistoryKind`: TASK_COMPLETED, TASK_NOT_DONE, TASK_MISSED, TASK_POSTPONED, TASK_POSTPONE_UNDONE, TASK_UNDONE,
  HABIT_DONE, HABIT_OCCURRENCE, HABIT_DAY_OK, HABIT_DAY_OVER, TIMER, COINS.
- `HistorySubject`: `Task(occurrenceId, deleted)`, `Habit(habitId, deleted)`, `Reward(rewardId?)`, `None`.
- `HistoryFilter`: ALL, TASKS, HABITS, TIME, COINS — `COINS` only offered in game mode.
- Grouping: `HistoryDay(day, items)` newest first; month header when kind is YEAR/ALL and month changes.

## Erasure (tasks, pure)
- `TaskAction.Delete(id, scope, eraseHistory: Boolean = false)`; `TaskAction.EraseHistory(id)`.
- Result: snapshot without erased occurrences/audits (and definition when allowed). Repository diff →
  `HistoryErasure.taskTraces(occurrenceIds, definitionIds)`.

## Erasure (habits)
- `HabitRepository.erase(habitId)` → `HistoryErasure.habit(habitId)`.

## State transitions
- Task occurrence: OPEN/DONE/NOT_DONE --Delete(erase=false)--> DELETED (kept) ; --Delete(erase=true)--> removed.
  DELETED --EraseHistory--> removed.
- Habit: ACTIVE/PAUSED --Exclude--> EXCLUDED (kept, restorable); any --erase--> removed.
