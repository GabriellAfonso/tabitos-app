# Data model

- Habit: id, created civil date, title/description/color/icon/listId, difficulty/importance/manual,
  status ACTIVE/PAUSED/EXCLUDED. listId references shared soft-deleted task_lists identity.
- Rule: id, habitId, effective date, type PRACTICE/AVOID/LIMIT, unit DAY/WEEK/MONTH,
  target and weekday mask (scheduled practice uses DAY with mask). Future revisions preserved.
- Interval: id, habitId, inclusive start and exclusive end; active dates only.
- Override: habitId+type+unit+periodStart key and explicit target for partial period.
- Record: stable id, habitId, actual epoch day, entered timestamp, deleted flag. Never hard purge.
- Review: stable period key, confirmed outcome and invalidated flag; pending derived when expired
  incomplete practice or unconfirmed avoid/limit, plus invalidated previously confirmed result.
- Audit: id, habitId, record/review reference, timestamp, action, previous/next date or outcome.

Mutations validate names, enums, nonnegative limits/positive practice goals, selected weekdays,
existing active lists, capacity of qualifying active days, bounded active record dates and stable IDs.
Correction recalculates impacted periods; old rule rows survive edits. Habit/list exclusion closes
intervals atomically. Restoration opens an interval and chooses a valid list explicitly when missing.
Schema2 adds normalized tables only; migration does not alter existing five task tables.
