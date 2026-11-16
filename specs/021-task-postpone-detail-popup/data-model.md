# Data Model: Postpone a task and a cleaner task detail popup

No Room schema change (stays at version 7). Existing entities are reused.

## TaskOccurrence (existing)

- Relevant fields: `id`, `taskId`, `ordinal`, `fields.deadline`, `fields.date`, `fields.minute`, `plannedDate`,
  `dueDate`, `status`.
- **Postponable** when `status == OPEN`, `fields.deadline == DAY` and `fields.date != null`.
- Postpone changes `fields.date` and `dueDate` (via `makeOccurrence`); `plannedDate` changes only for non-recurring
  tasks (existing ONLY_THIS behaviour); `minute` unchanged; `touched = true`.

## TaskAction (extended)

- `Postpone(id: String, to: LocalDate)` — validation: postponable; `to >= today`; `to != fields.date`.
  Errors: `NOT_OPEN` (not open), `INVALID_INPUT` (not DAY / no date), `INVALID_DATE` (bounds).
- `UndoPostpone(id: String, to: LocalDate)` — validation: postponable; any `to` different from the current date.

## TaskAudit (existing, append-only)

- Postpone of an overdue occurrence appends `MISSED_DEADLINE` (original deadline kept) then `EDITED`.
- Postpone of a non-overdue occurrence appends `EDITED`. Undo appends `EDITED`. Nothing is removed.

## ReminderDefinition (existing)

- CHOSEN occurrence-scoped (`occurrenceId == o.id`): `chosenDate` shifted by `days`.
- CHOSEN series-scoped on a recurring task: new occurrence-scoped override
  `id = "<groupId>:<occurrenceId>"`, `groupId` same, `occurrenceId = o.id`, `fromOrdinal = o.ordinal`,
  `chosenDate = plannedDate + anchorDay + days`, `minute`, `enabled`, `createdAt` copied.
- CHOSEN series-scoped on a non-recurring task, RELATIVE, WEEKDAYS: unchanged (follow by themselves / not
  applicable).

## UI state

- `TasksUiState.postponed: PostponeFeedback?` — `(occurrenceId, from, to)` for the "Desfazer" snackbar.
- `TasksUiState.reminders` — reminder definitions, only to count reminders in the summary line.
- Sheet, pickers, list choice and description expansion are local `rememberSaveable` UI state of the popup
  (no business meaning).
- `completionText` keeps carrying the completion date; "Concluí em outro dia" sets it from the picker.
