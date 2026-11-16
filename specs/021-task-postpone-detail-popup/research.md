# Research: Postpone a task and a cleaner task detail popup

## R1 — Postpone as a domain action reusing "edit only this occurrence"

- **Decision**: add `TaskAction.Postpone(id, to)` and `TaskAction.UndoPostpone(id, to)`. Both delegate to the
  existing `edit` path with `ChangeScope.ONLY_THIS`, the occurrence's current fields with `date = to`, the
  definition's current rule and `reviewedDeadline = true`. `Postpone` additionally requires: occurrence open,
  `DeadlineKind.DAY`, a date, `to >= today`, `to != current date` (else `INVALID_DATE`). `UndoPostpone` requires
  open + DAY + date and accepts any date (the original may be in the past).
- **Rationale**: `edit` already writes `MISSED_DEADLINE` when an overdue date changes, keeps `plannedDate` for
  recurring ONLY_THIS edits (series untouched), marks the occurrence touched and appends `EDITED`. Reusing it keeps
  one rule for date changes and an append-only history (undo appends `EDITED`).
- **Alternatives**: calling `TaskAction.Edit` from the ViewModel (rejected: bounds and "only DAY" would live in UI);
  a new column "postponed from" (rejected: schema change with no requirement).

## R2 — Fixed-date reminders move with the task

- **Findings** (`ReminderRules.schedule`): RELATIVE uses `dueDate` → follows automatically. CHOSEN uses
  `chosenDate` when the definition is scoped to the occurrence (`occurrenceId == o.id`), otherwise
  `plannedDate + anchorDay`. Non-recurring edits update `plannedDate` (series-scoped CHOSEN follows); recurring
  ONLY_THIS edits keep `plannedDate` (series-scoped CHOSEN would stay on the old day). WEEKDAYS spans
  `periodStart..dueDate` (only week deadlines, never postponed).
- **Decision**: `ReminderRules.postponeChosen(defs, occurrence, days, recurring)` returns definitions to upsert, from
  `effective(defs, occurrence)` filtered to CHOSEN: occurrence-scoped → `chosenDate + days`; series-scoped on a
  recurring task → a new occurrence-scoped override (`id = "<groupId>:<occurrenceId>"`, same group, minute,
  `createdAt` of the series rule, `chosenDate = plannedDate + anchorDay + days`); series-scoped on a non-recurring
  task → nothing (already follows). Undo calls it with `-days`.
- **Rationale**: matches how the reminder screen already scopes "only this occurrence" (same id format); delivery
  keys include the slot date, so a moved reminder gets a new key and is scheduled again while the old one is
  cancelled by `reconcileRows` (satisfies "fires again if already delivered and now in the future").
- **Alternatives**: changing `plannedDate` for recurring postpone (rejected: `plannedDate` drives series generation).

## R3 — Atomicity

- **Decision**: in `RoomTaskRepository.execute`, for `Postpone`/`UndoPostpone`, inside the existing
  `withTransaction`, compute `days = to − previous date`, read reminder definitions through `database.reminders()`,
  and upsert `postponeChosen` results. Accounting is not touched.
- **Rationale**: tasks and reminders share `TaskDatabase`; the reminder coordinator already reacts to both tables.

## R4 — Popup structure ("Mínimo")

- **Decision**: keep `AlertDialog` (no `confirmButton` content besides an empty slot is not allowed, so the dialog
  uses `BasicAlertDialog`/`AlertDialog` with custom content and no buttons). Content: title, summary `FlowRow`
  (deadline, list, repeat, reminders count, ▶ timer chip), description with `maxLines = 2` + "Ver tudo",
  checklist, two-button row (split "Concluir | ▾" + tonal "Adiar"), five labelled `IconButton`s row. Resolved:
  status strip with "Desfazer" (`TaskAction.Undo` through existing financial confirmation).
- **Rationale**: what the user chose in the prototype; closing via `onDismissRequest` (outside tap / back).
- **Alternatives**: bottom sheet / full screen (rejected by the user).

## R5 — Postpone sheet and pickers

- **Decision**: `ModalBottomSheet` over the dialog with "Amanhã" and "Escolher data" plus notes (series / overdue).
  Extract the date picker dialog from `DateField` into `EthosDatePickerDialog(initial, min, max, excluded,
  onConfirm, onDismiss)` reused by `DateField`, "Escolher data" (min = today, excluded = current date) and
  "Concluí em outro dia…" (min = creation date, max = today).
- **Rationale**: same picker look as the editor (spec 013), no typing, no duplicate component.

## R6 — Undo feedback

- **Decision**: `TasksUiState.postponed: PostponeFeedback(id, from, to)?` set after a successful commit; the
  snackbar in `EthosApp`'s chain shows "Adiada para amanhã/<ter, 17 nov>" with "Desfazer" and a close action;
  undo dispatches `UndoPostpone(id, from)` and clears it; dismiss clears it. State-based (survives recomposition),
  and the result is already persisted, so losing the message loses nothing critical.

## R7 — History out of the popup and dead code

- **Decision**: remove the history list from the popup; remove strings only used by it (`task_history`,
  `audit_*`, `audit_line`, `actual_date_line`, `original_deadline_line`) after checking no other usage; audits keep
  being written. The future History tab will add what it needs.
