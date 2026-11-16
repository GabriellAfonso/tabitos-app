# Feature Specification: Postpone a task and a cleaner task detail popup

**Feature Branch**: `021-task-postpone-detail-popup` (no branch created; work stays on the current branch)

**Created**: 2026-11-16

**Status**: Implemented (2026-11-16)

**Input**: User description: body of `TODO/specify_adiar_detalhes_tarefa_app.md` ("Postpone a task and a cleaner
task detail popup"), decided in a design conversation on 2026-11-16 with prototypes
`prototypes/detalhes-tarefa/index.html` (version "1 · Mínimo") and `prototypes/adiar-tarefa/index.html`.

## Current state (verified 2026-11-16)

- Tapping a task card (Hoje, list pages, Revisar pendências, Calendário) opens the task detail popup. It is one
  long column: title, description, date · time, checklist, "Cronômetro" (timed tasks), "Lembretes", an
  always-visible "Data em que foi feita" field, "Concluir tarefa", "Não será feita", "Editar", chips for every
  list, "Excluir", the full "Registros e correções" list and "Fechar". Every action has the same weight; the user
  finds it cluttered.
- There is no postpone action. Changing the day requires opening the editor. Changing the date of an overdue task
  already records a "missed deadline" entry without penalty and requires the review flag.
- The notification "Adiar" only snoozes the reminder.
- The user's real routine is stored in the app and must be preserved.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Postpone a task to tomorrow or another day (Priority: P1)

As a user, from the task detail popup I tap "Adiar" and choose "Amanhã" or "Escolher data"; the task moves to that
day keeping its time, and I can undo right away.

**Why this priority**: it is the new capability the user asked for; today it takes several steps in the editor.

**Independent Test**: open an open task due today with a time, postpone to tomorrow, verify it leaves Hoje, appears
tomorrow in the calendar with the same time, and that "Desfazer" restores it.

**Acceptance Scenarios**:

1. **Given** an open task due today at 18:00, **When** I tap Adiar → Amanhã, **Then** only this occurrence moves to
   tomorrow at 18:00, the popup closes and a message "Adiada para amanhã" with "Desfazer" appears.
2. **Given** the postpone sheet, **When** I tap Escolher data, **Then** a date picker opens where days before today
   and the current deadline cannot be selected; confirming moves the task to the chosen day.
3. **Given** an overdue task, **When** I postpone it, **Then** no extra confirmation is asked, the sheet has warned
   "O prazo de <data> fica registrado como perdido, sem penalidade.", the missed deadline is recorded, no coins or XP
   change, and the task leaves Revisar pendências.
4. **Given** a recurring task, **When** I postpone one occurrence, **Then** the following occurrences keep their own
   dates and the sheet explains "Só esta vez. A série continua: próxima em <data>."
5. **Given** I just postponed a task, **When** I tap Desfazer, **Then** the previous deadline and reminder moments
   return; history keeps the records already written and gains a correcting one.
6. **Given** a task without date, with a week/work-week deadline, or already resolved, **When** I open its details,
   **Then** Adiar is not offered.

---

### User Story 2 - Reminders follow a postponed task (Priority: P1)

As a user, when I postpone a task its reminders move with it, so I am reminded on the new day.

**Why this priority**: a postponed task with reminders on the old day would notify at the wrong moment.

**Independent Test**: create a task with a relative reminder and a fixed-date reminder on its due day, postpone it
by N days, check both reminder moments.

**Acceptance Scenarios**:

1. **Given** a reminder "1 h antes" of the deadline, **When** the task is postponed, **Then** it fires 1 h before
   the new deadline.
2. **Given** a fixed-date reminder at 09:00 on the due day, **When** the task is postponed by N days, **Then** the
   reminder is at 09:00 N days later (on the day the task was postponed to).
3. **Given** a fixed-date reminder that already fired, **When** the task is postponed and the moved moment is in the
   future, **Then** it is scheduled again.
4. **Given** a weekday-based reminder, **When** the task is postponed, **Then** it is unchanged.
5. **Given** I undo the postpone, **Then** fixed-date reminders return to their previous moments.

---

### User Story 3 - A shorter, organized task detail popup (Priority: P2)

As a user, the popup that opens when I tap a task is short and shows what matters first: what the task is, then
the two frequent actions (Concluir, Adiar), then the other actions as labelled icons.

**Why this priority**: the user explicitly asked to keep a popup but make it polished; Adiar lives here.

**Independent Test**: open simple, checklist, overdue, recurring, timed and resolved tasks and compare against the
layout below; every previous action is still reachable.

**Acceptance Scenarios**:

1. **Given** an open task, **When** I open its details, **Then** I see, in order: title; one summary line (deadline
   with time and "· vencida" when overdue, list icon and name, repetition label when recurring, number of reminders,
   "▶ <goal>" for timed tasks); the description limited to 2 lines with "Ver tudo"; the checklist; two large buttons
   "Concluir" (with "▾") and "Adiar"; a row of labelled icons "Não será feita", "Editar", "Lembretes", "Lista",
   "Excluir". There is no "Fechar" button and no history list.
2. **Given** the popup, **When** I tap outside or press back, **Then** it closes.
3. **Given** an open task, **When** I tap Concluir, **Then** it completes with today's date, with the same
   confirmations as today (unchecked checklist items, timed task without finished time).
4. **Given** an open task, **When** I tap "▾" → "Concluí em outro dia…", **Then** a date picker limited to today
   and earlier (not before creation) opens and confirming completes the task with that date.
5. **Given** the icons row, **When** I tap Não será feita / Editar / Lembretes / Excluir, **Then** the existing
   flows run (financial confirmation when game mode is on, editor, reminders screen, delete dialog with scope).
6. **Given** the icons row, **When** I tap Lista, **Then** I choose among active lists (name and icon, current
   marked) and the task moves to the chosen one.
7. **Given** a timed task, **When** I tap the "▶" item in the summary, **Then** its timer opens as today.
8. **Given** a resolved task, **When** I open its details, **Then** a status strip ("Feita <dia> às <hora>" or
   "Marcada como não será feita") with "Desfazer" replaces Concluir/Adiar; Não será feita and Adiar are absent;
   Lembretes is disabled.

### Edge Cases

- Postpone chosen while the app crosses midnight: "Amanhã" is computed from the injected clock at the moment of the
  action; a picker date that became past is rejected with the existing date error and nothing changes.
- Postponing to a day that already has another occurrence of the same series: allowed; both appear on that day.
- Fixed-date reminder on a different day than the deadline (e.g. the day before at 20:00): it keeps the same
  distance, moving by the same number of days.
- Undo after the snackbar is gone: not available; the user can postpone again or edit the date.
- Persistence failure while postponing: the error message is shown, the task and reminders stay unchanged, nothing
  is shown as success.
- Timer counting for the task: postponing does not stop or change the timer.
- Large font (200%): the five labelled icons may wrap their labels but must not be clipped; the two large buttons
  may wrap text.
- Description without text, task without checklist or reminders: the corresponding parts are omitted, not shown
  empty.

## Requirements *(mandatory)*

### Escopo e restrições do Ethos

- **In scope**: postpone action and sheet in the task detail popup; reminder moments on postpone/undo; new layout of
  the task detail popup; removal of the history list from the popup.
- **Out of scope**: History tab and statistics (future feature), "postponed N times" counters, postpone shortcuts on
  cards (long-press, ⋮, swipe), postponing from notifications, postponing week/month deadlines, tasks without date
  or habits, changes to completion/review/recurrence/reward/penalty rules, changes to the task editor.
- **Offline/persistence**: everything is local; a postpone (task date, missed-deadline record, reminder moments) is
  one atomic change; failure leaves all of it unchanged. Audit history stays append-only. No destructive migration;
  if a schema change turns out necessary it must preserve existing data.
- **Accessibility**: icon buttons have visible labels and spoken descriptions; TalkBack can reach every action;
  touch targets ≥ 48 dp; 200% font; light and dark themes.
- **Permissions/personal data**: N/A; no new permission; no task content in logs.
- **Dates/time zone**: local calendar days from the injected clock; time of day is preserved; fixed-date reminders
  shift by whole days in local time.

### Functional Requirements

- **FR-001**: The task detail popup MUST offer "Adiar" only for open tasks with a specific-day deadline and a date.
- **FR-002**: "Adiar" MUST open a sheet titled with the task, showing the current deadline, with exactly two
  options: "Amanhã" (with weekday, date and time) and "Escolher data".
- **FR-003**: "Escolher data" MUST open a date picker that rejects days before today and the current deadline.
- **FR-004**: Postponing MUST change only the date of this occurrence; time, other fields and other occurrences of
  a series stay unchanged.
- **FR-005**: Postponing an overdue task MUST NOT ask for confirmation; it MUST record the missed deadline without
  penalty, as editing the date of an overdue task does today, and the sheet MUST state this before the choice.
- **FR-006**: Postponing MUST NOT create coin or XP entries.
- **FR-007**: After postponing, the popup MUST close and a message "Adiada para amanhã" / "Adiada para <dia>" with
  "Desfazer" MUST appear; the result must already be saved independently of the message.
- **FR-008**: "Desfazer" MUST restore the previous deadline and reminder moments, keeping history append-only (a
  correcting record is added; nothing is erased).
- **FR-009**: Relative reminders MUST follow the new deadline; fixed-date reminders MUST move by the same number of
  days keeping the time of day, and be scheduled again when the moved moment is in the future; weekday reminders
  MUST stay unchanged.
- **FR-010**: Recurring tasks MUST show the note about only this occurrence moving, with the next occurrence date.
- **FR-011**: The popup MUST follow the layout of User Story 3, scenario 1, with no "Fechar" button and closing on
  outside tap/back.
- **FR-012**: The popup MUST NOT show the "Registros e correções" list; audits keep being recorded.
- **FR-013**: "Concluir" MUST complete with today's date; "▾ → Concluí em outro dia…" MUST complete with a chosen
  date between creation and today; the always-visible completion date field MUST be removed.
- **FR-014**: "Lista" MUST open a choice of active lists and move the task; inline list chips MUST be removed.
- **FR-015**: Resolved tasks MUST show the status strip with "Desfazer" instead of Concluir/Adiar, without "Não será
  feita" and with "Lembretes" disabled.
- **FR-016**: All existing confirmations (unchecked checklist, timed completion, financial confirmation in game
  mode, delete scope) MUST keep working unchanged.
- **FR-017**: The notification "Adiar" MUST keep snoozing only the reminder.

### Key Entities

- **Task occurrence**: the dated instance that is postponed (deadline date, time, status, list).
- **Missed-deadline record**: history entry written when an overdue occurrence's date changes; keeps the original
  deadline; append-only.
- **Task reminder definition**: relative (offset from deadline), fixed date/time, or weekday-based; fixed ones move
  with the postpone.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: From a task card, postponing to tomorrow takes 3 taps (card, Adiar, Amanhã).
- **SC-002**: The popup for a simple task (no description, no checklist) fits on a phone screen without scrolling
  at default font size, and is shorter than the current popup for every sample task type.
- **SC-003**: 100% of actions available in the current popup (complete, complete on another day, not done, edit,
  reminders, move list, delete, undo result, timer) remain reachable from the new popup.
- **SC-004**: Postponing never changes coins/XP and never loses the original deadline from history in any tested
  case (on time, overdue, recurring, undo).
- **SC-005**: After a postpone of N days, 100% of fixed-date reminders of that task are at the same time N days
  later, and relative reminders keep their offset.

## Assumptions

- "Amanhã" and the picker bounds use the app's injected clock, as all task dates do.
- Undo is available while the confirmation message is visible (same pattern as other "Desfazer" messages in the app).
- The detail popup opens from the same places as today; this feature does not add new entry points.
- Removing the history list from the popup removes its now-unused texts; the future History feature will add what
  it needs.
- The postpone sheet appears over the popup; once a choice is made both close.
