# Contracts: trackers, calendar, tracker reminders

## Repository (core/trackers/domain)
```kotlin
interface TrackerRepository {
    fun observe(): Flow<TrackerSnapshot>
    suspend fun execute(action: TrackerAction)   // throws TrackerRuleException(TrackerProblem)
}
sealed interface TrackerAction {
    SaveForm(trackerId, habitId, multiple, questions)   // creates or versions
    SetActive(trackerId, active)                        // archive / restore
    SaveEntry(entryId, trackerId, date, answers)        // new or edit (same id)
    DeleteEntry(entryId)
}
```
Problems: EMPTY_FORM, QUESTION, SCALE, PRIMARY, OPTIONS, DATE, REQUIRED, ANSWER, INACTIVE, DUPLICATE.
Repository never touches habit, accounting, reward, task or timer tables.

## Pure rules (TrackerRules)
- `saveForm(snapshot, habits, action, today, now): TrackerSnapshot`
- `saveEntry(...)`, `deleteEntry(...)`, `setActive(...)`
- `fillable(tracker, habits, date, today): Boolean`
- `entryFor(snapshot, trackerId, date): TrackerEntry?` (single-mode editing target: latest by filledAt)
- `pending(snapshot, habits, reminders, today, minuteNow): List<Tracker>`
- `analytics(snapshot, trackerId, today, days): TrackerAnalytics` — days 7/14/30/90; `points` per entry,
  `missingDays`, `rangesDiffer`, `options` (questionId, title, optionId, label, count).

## UI entry points
- `LocalOpenTracker: (habitId) -> Unit` in `core/ui/trackers`; habit detail shows "Acompanhamento".
- `TrackerViewModel.fill(trackerId, date)` opens form (existing entry in single mode), `open(habitId)` opens hub.
- `CalendarContent(state, actions)` with `openTask(occurrenceId)`, `openHabit(habitId)`, `fill(trackerId, date)`,
  `openEntry(entryId)`.

## Notification
- Channel `ethos_reminders` (existing). Title "Ethos", text "Você tem um acompanhamento para preencher", public
  version identical; ongoing, not auto-cancel, private visibility.
- Actions: Preencher → `ethos-reminder://local/FILL/<key>`; Adiar → `.../SNOOZE/<key>`; content tap = FILL.
- Resolved when an entry exists for the slot date (reconcile cancels and removes notification).
