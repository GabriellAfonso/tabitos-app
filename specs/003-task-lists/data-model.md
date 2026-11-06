# Data model

- ListRecord: id, name, color token, icon token, position, deleted.
- TaskDefinition: id, createdDay, template fields, recurrence pattern/weekday mask/monthDay/interval,
  start/next due day, next ordinal, maximum scheduled count or inclusive until date, stopped.
- Occurrence: id (root:ordinal), taskId, ordinal, title/description/list/difficulty/importance,
  deadline kind/start/end/optional minute, planned day, status OPEN/DONE/NOT_DONE/DELETED,
  actual completion day, active outcome id, position, acted-on flag.
- ChecklistRecord: id, ownerId (definition or occurrence), label, position, checked.
- AuditRecord: id, occurrenceId, type, recordedAt, actualDay, originalDeadline, relatedOutcomeId.

List/task/occurrence/checklist/audit IDs are stable. No financial amounts are fabricated.
Deletion is a tombstone, not a financial reversal. Audit rows are append-only. Corrections point at
prior outcome IDs; active outcome clears on undo. Repeated undo is invalid rather than double reversal.

Validation: trimmed required titles/list names, positive intervals/counts, nonempty weekday mask,
month day 1..31, minute 0..1439 only for day deadlines, inclusive end not before start.
Completion date lies between task creation and today. Overdue date edit requires explicit review.

Transitions: OPEN -> DONE/NOT_DONE; resolved -> OPEN by attributable undo; active -> DELETED.
Expired OPEN remains OPEN plus derived review membership. Reviewed reschedule appends MISSED_DEADLINE,
retains identity and original deadline audit, updates current deadline and remains OPEN.
Definitions drive recurrence, occurrence snapshots isolate exceptions. Transactions upsert changed
aggregate rows; no physical cascades through history, no destructive resets.

Recurrence and deadline kind are independent for dated/period tasks: a scheduled anchor maps to the containing week for weekly deadlines; ending rules count/check anchors, not the period end.
