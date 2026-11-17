# Research: Statistics page and erasing history (spec 022)

All decisions below resolve the open questions of the prompt and the technical unknowns found while reading the
code on 2026-11-17.

## R1. Where the numbers come from
- **Decision**: compute everything in memory from the snapshots already exposed by `TaskRepository`,
  `HabitRepository`, `TimerRepository`, `AccountingRepository` and `RewardRepository`, in pure functions, on
  `Dispatchers.Default`.
- **Rationale**: every repository already loads full snapshots; no new query, no schema change, same code paths as
  the rest of the app (numbers cannot diverge).
- **Alternatives**: SQL aggregate queries (faster for huge data, but duplicates rules such as habit periods and
  ledger semantics); a statistics table (needs migration and sync with every write).

## R2. Period state
- **Decision**: `StatisticsPeriodStore` — a Hilt `@Singleton` holding a `MutableStateFlow<StatisticsPeriod?>`;
  `null` means "this week" computed from `TaskClock`.
- **Rationale**: survives navigation (the ViewModel of a destination can be cleared) and resets on a new process,
  exactly FR-007; no DataStore.
- **Alternatives**: `SavedStateHandle` (would survive process death — not wanted); DataStore (rejected by user).

## R3. Task numbers
- Completed = occurrences with status DONE and `completedDate` in range (DELETED excluded: deleting changes the
  status). Undo resets status, so undone tasks do not count.
- On time = DONE with deadline ≠ NONE and `completedDate <= dueDate` (`dueDate` is the deadline day for DAY and
  the window end for WEEK/WORK_WEEK); ratio over DONE with deadline; `null` (shown "—") when 0.
- Postponed = `POSTPONED` audits whose `recordedAt` falls in range (local zone).
- Chart buckets: DAY for week/month, MONTH for year/all, YEAR for all when data spans more than 2 calendar years
  (`lastYear - firstYear + 1 > 2`).

## R4. Habit numbers equal to the card
- **Decision**: add `HabitRules.successRate(s, id, today, range)` using the same `countsForRate`/`standing`
  predicates as the card (`successRate(s, id, today)` is refactored to share them). Periods overlapping the range
  count. Streak = `HabitRules.streak(...).current` (the card value, as of today).
- **Rationale**: one implementation, so Resumo and card cannot diverge (SC-002). The card's own "last 30 days /
  8 weeks / 6 months" window is a period choice of the card; for a range the same rule is applied to that range.
- Overall constancy = mean of the non-null rates of listed habits (rounded down).
- Bad habit = current rule type AVOID or LIMIT → red (errorContainer) styling, same as cards.

## R5. Timer time
- **Decision**: sum of closed (`endWall != null`), active segments of TASK and HABIT sessions, allocated to local
  days with `TimerRules.allocate`, restricted to the range. Per activity grouped by (kind, sourceId), title from the
  session (copied at start). History "Tempo" items: one per session per local day with credited time.
- **Rationale**: sessions are often left paused instead of finished; counting only FINISHED would hide most time.
  A segment still counting is not closed, so it is not counted (edge case "still running").
- Rewards (`TimerKind.REWARD`) excluded.

## R6. Coins in the history
- Moedas filter: every ledger entry as its own row, same title, kind, coins, XP and time as the former page.
- Other filters: ledger entries are attached to the item they belong to — a task item matches entries whose key is
  `task:<occurrenceId>:*` recorded within 5 s of the audit; a habit record matches `practice:<habitId>:<date>` or
  `event:<recordId>` within 5 s of `entered`; a settled bad-habit period matches `period:<key>:*` at any time.
  Unmatched entries (e.g. purchases, later reversals, corrections) are shown as their own coin item in Tudo.
- Coins earned = sum of positive `coins` of GAIN/REAPPLY entries in range; spent = −sum of PURCHASE coins in range.
  Level/XP = current `AccountingSnapshot.progress`.
- Game mode off: no ledger items, no coin values, no Moedas filter, no "Moedas e nível" section.

## R7. History item placement and kinds
- Items are placed on the local day of the moment they were recorded (same as the former coin history), except
  habit records (their `date`) and settled bad-habit periods (period end).
- Task audit kinds shown: COMPLETED, NOT_DONE, MISSED_DEADLINE, POSTPONED, UNDONE, POSTPONE_UNDONE (adjustment).
  Hidden: CREATED, EDITED, CHECKED, DELETED.
- Adjustment style: POSTPONE_UNDONE, ledger REVERSAL and CORRECTION.
- Paging: 50 items per page; "Mostrar mais" adds 50; filter/period change resets to 50.

## R8. Erasing a task's history
- **Decision**: `TaskAction.Delete(id, scope, eraseHistory = false)` and `TaskAction.EraseHistory(id)` handled in
  `TaskRules` (pure): erased occurrences and their audits leave the snapshot; a definition leaves when it is stopped
  or non-recurring and no occurrence remains. `RoomTaskRepository` deletes rows missing after the rule and calls
  `HistoryErasure.taskTraces(...)` in the same transaction.
- Scope: Delete + "Só esta" erases that occurrence; Delete + "Todas" erases every occurrence of the series (past
  included) and the definition. EraseHistory (from Histórico) requires a DELETED occurrence and erases every
  DELETED occurrence of its series.
- Regeneration safety: an erased occurrence always has `ordinal < nextOrdinal`, and `generate()` only creates
  ordinals ≥ `nextOrdinal`; an erased tombstone behaves like a DELETED one in the "latest future occurrence" check.

## R9. Erasing a habit
- **Decision**: `HabitRepository.erase(habitId)`; the delete dialog with the box checked calls it instead of
  excluding; "Hábitos excluídos" and the Histórico dialog call it too. One transaction.

## R10. Tables that reference a task or habit (FR-025)

| Table | Task erase | Habit erase |
|---|---|---|
| task_occurrences | delete erased occurrences | — |
| task_definitions | delete when no occurrence remains and series stopped/non-recurring | — |
| task_checklist | delete owners = occurrence ids and `template:<definition>` | — |
| task_audit | delete by occurrenceId | — |
| habits, habit_rules, habit_intervals, habit_records, habit_audits | — | delete by habit id |
| habit_overrides, habit_reviews | — | delete keys starting with `<habitId>:` |
| trackers, tracker_versions/questions/options/entries/answers | — | delete the habit's tracker and all its rows |
| timer_sessions, timer_segments, timer_audits | delete TASK sessions of erased occurrences | delete HABIT sessions |
| timer_goals | delete `task:<definition>` when definition erased | delete `habit:<id>` |
| reminder_definitions | delete TASK reminders of erased occurrences, all of an erased definition | HABIT reminders and TRACKER reminders of its tracker |
| reminder_deliveries | delete rows of erased sources (no content) | same |
| accounting_entitlements | title → "" for owner `task:<occurrence>`; amounts/active kept | owner `habit:<id>` and `baseline:habit:<id>` |
| accounting_ledger | title → "" for keys of those entitlements; amounts/balances kept | same |
| reward_* | untouched | untouched |

Blank ledger titles are displayed as "Tarefa apagada" (key `task:`) or "Hábito apagado" (habit keys); storing no
localized text in the database.

## R11. Lists
- Deleting a list keeps history (no checkbox in this version); its tasks' items appear as deleted and can be erased
  from Histórico.

## R12. Running timer of an erased item
- Deleting already closes the source and the timer coordinator pauses it; erasing removes the session in the same
  transaction, the coordinator then finds no running session and stops the service. No extra guard.
