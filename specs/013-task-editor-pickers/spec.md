# Feature Specification: Task Editor Pickers and Quick Capture

**Feature Branch**: `master` (no branch hook registered)

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "Task date/time pickers and lighter task editor. Replace typed dates/times with
pickers in the task flow, use one consistent pt-BR date format, quick capture first with the rest under
'Mais opções' in a full-screen editor, show the review confirmation only when the deadline really passed,
edit checklists as items, simplify the repeat choice and clarify the weekly deadline labels. No domain,
schema or rule changes."

**Origin**: Claude Code analysis conversation on 2026-11-10 (no transcript ID available). All decisions
from that conversation are recorded in this file. Related specs: `specs/003-task-lists/spec.md`,
`specs/012-direct-task-event-checks/spec.md`.

## Current State (verified in code on 2026-11-10)

- The task editor is a single small dialog with about 15 fields always visible: title, description,
  list, difficulty, importance, coin estimate, deadline kind, date, time, checklist, 8 repeat chips,
  recurrence start, ending, change scope and a review checkbox.
- Due date, recurrence start, final date and the "data em que foi feita" field in the task detail are free
  text in `AAAA-MM-DD`; the time is free text in `HH:mm`. Invalid text only fails on save.
- Dates are shown in different formats: list rows use `dd/MM/yyyy` (or "Hoje"/"Amanhã"), the task detail
  shows the raw ISO value (`2026-11-10`), and the habit calendar month title shows `2026-11`.
- When editing any task, the text "Revise o prazo vencido…" and the checkbox "Confirmo a revisão do prazo
  original" are always shown, although the rules only require that confirmation when an overdue open
  occurrence has its date changed.
- The checklist is a multiline text field ("um item por linha").
- Repeat choice is a single row of 8 chips: Não repete, Diário, Dias úteis, Semanal, Dias da semana
  escolhidos, Mensal, "A cada X dias · calendário fixo", "A cada X dias · após concluir".
- Deadline choice shows "Nesta semana" and "Nesta semana — dias úteis", whose difference (until Sunday vs
  until Friday, per spec 003 FR-008) is not visible.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Pick dates and times instead of typing them (Priority: P1)

As a user creating or editing a task, I choose the due date, time, recurrence start, final date and the
actual completion date with calendar and clock pickers, so I never type `AAAA-MM-DD` or `HH:mm` and never
lose a save to a typo.

**Why this priority**: Typed dates are the most frequent source of friction and errors in the task flow.

**Independent Test**: Create a task with a specific day and time, a recurring task with a final date, and
resolve a task with a past completion date, using only pickers; verify stored values equal the chosen ones.

**Acceptance Scenarios**:

1. **Given** the editor with a specific-day deadline, **When** I tap the date field, **Then** a calendar
   picker opens on the current value (or today if empty) and the chosen day is shown in the field in the
   app date format.
2. **Given** a chosen date, **When** I tap the time field, **Then** a 24-hour clock picker opens; I can
   confirm a time or clear it to leave the task without time.
3. **Given** a recurring task, **When** I choose the start and the final date, **Then** both use the same
   calendar picker and the final date cannot be earlier than the start date.
4. **Given** an open task detail, **When** I set "data em que foi feita", **Then** a calendar picker opens,
   days after today are not selectable, and the default is today.
5. **Given** any picker, **When** I cancel it, **Then** the previous value is kept unchanged.
6. **Given** the device time zone differs from UTC, **When** I pick a day, **Then** the stored date is
   exactly the picked calendar day (no shift to the previous or next day).

---

### User Story 2 - Capture a task quickly, with details on demand (Priority: P1)

As a user who just wants to write something down, I see only title, date/time and list first, and open
"Mais opções" only when I need difficulty, importance, description, checklist, repetition or ending.

**Why this priority**: Capturing a task is the most common action; today it requires scrolling past every
field.

**Independent Test**: Create a task by typing only a title and saving; then create one using every
advanced field; both must persist with the same results the current editor produces.

**Acceptance Scenarios**:

1. **Given** a new task, **When** the editor opens, **Then** it is a full-screen editor showing title,
   deadline (kind + date + time), list and a collapsed "Mais opções" section, with the keyboard focused on
   the title.
2. **Given** "Mais opções" collapsed, **When** I expand it, **Then** it shows description, checklist,
   difficulty, importance with the coin estimate, repetition and ending, keeping every capability that the
   current editor offers.
3. **Given** I am editing a task that already uses any advanced field with a non-default value, **When**
   the editor opens, **Then** "Mais opções" starts expanded.
4. **Given** I only typed a title, **When** I save, **Then** the task is created with the same defaults as
   today (deadline on today, medium difficulty and importance, no repetition).
5. **Given** the editor is open, **When** I press Back or Cancel with unsaved changes, **Then** I am asked
   to confirm discarding them; without changes it closes directly.

---

### User Story 3 - Read dates the same way everywhere (Priority: P2)

As a user, I see dates in one short pt-BR format across the task list, the task detail and the habit
calendar title, so I never see ISO values.

**Why this priority**: Inconsistent formats look unfinished and slow reading, but do not block actions.

**Independent Test**: View the same task due today, tomorrow, yesterday, later this year and next year in
the list and detail, and open the habit calendar; compare against the format rules.

**Acceptance Scenarios**:

1. **Given** a date equal to today, tomorrow or yesterday, **When** shown in the list or detail, **Then** it
   reads "Hoje", "Amanhã" or "Ontem".
2. **Given** another date in the current year, **When** shown in the list, **Then** it reads like
   "10 nov"; in the detail it reads like "segunda, 10 de novembro".
3. **Given** a date in another year, **When** shown, **Then** the year is added ("10 nov 2027";
   "quarta, 10 de novembro de 2027").
4. **Given** the habit calendar, **When** a month is shown, **Then** its title reads like "novembro de 2026".
5. **Given** a task with time, **When** shown, **Then** the time uses 24-hour `HH:mm` next to the date.

---

### User Story 4 - Confirm a missed deadline only when it was missed (Priority: P2)

As a user editing a task, I only see the missed-deadline confirmation when the occurrence is overdue and I
change its date, so ordinary edits do not show a warning that does not apply.

**Why this priority**: The current always-visible warning is confusing and teaches users to ignore it.

**Independent Test**: Edit a non-overdue task and an overdue task, with and without changing the date.

**Acceptance Scenarios**:

1. **Given** an open occurrence that is not overdue, **When** I edit it, **Then** no review text or
   checkbox is shown.
2. **Given** an overdue open occurrence, **When** I edit without changing its date, **Then** no review
   confirmation is shown and saving behaves as today.
3. **Given** an overdue open occurrence, **When** I change its date, **Then** the review explanation and
   confirmation appear, and saving without confirming shows the existing review-required message.

---

### User Story 5 - Edit the checklist as items (Priority: P3)

As a user, I add, rename, remove and reorder checklist items one by one instead of editing lines of text.

**Why this priority**: Improves an advanced field used less often than dates.

**Independent Test**: Create a task with three items, remove the middle one, reorder, save and reopen.

**Acceptance Scenarios**:

1. **Given** the checklist section, **When** I tap "Adicionar item", **Then** a new empty item field
   appears focused; pressing Enter on a non-empty item adds the next one.
2. **Given** an item, **When** I tap its remove control, **Then** it disappears; empty items are ignored on
   save.
3. **Given** several items, **When** I move one up or down, **Then** the saved order matches the shown
   order.
4. **Given** an occurrence with checked items, **When** I edit the checklist, **Then** check state follows
   the existing rules of spec 003 for edited checklists.

---

### User Story 6 - Understand repetition and weekly deadlines at a glance (Priority: P3)

As a user, I choose repetition from a small set of clear options and see what each weekly deadline means.

**Why this priority**: Clarifies wording; all capabilities already exist.

**Independent Test**: Recreate each of the 8 existing recurrence kinds through the new choice and verify
the stored kind and parameters are identical to those produced by the current editor.

**Acceptance Scenarios**:

1. **Given** the repetition section, **When** shown, **Then** the first-level options are (renamed 2026-11-11 to Diário/Semanal/Mensal): Não repete,
   Todo dia, Toda semana, Todo mês, A cada X dias.
2. **Given** "Todo dia", **When** chosen, **Then** a secondary choice offers "Todos os dias" (daily) and
   "Dias úteis (seg a sex)" (weekdays).
3. **Given** "Toda semana", **When** chosen, **Then** weekday chips appear; one selected day stores the
   weekly kind and two or more store the custom-weekdays kind, and at least one day must stay selected.
4. **Given** "A cada X dias", **When** chosen, **Then** I enter X and choose "Contar da data prevista"
   (fixed calendar) or "Contar da conclusão" (after completion).
5. **Given** an existing recurring task of any of the 8 kinds, **When** I open it in the editor, **Then**
   the matching option is preselected and saving without changes keeps the same kind and parameters.
6. **Given** the deadline choice, **When** shown, **Then** the weekly options read "Até domingo" and
   "Até sexta (dias úteis)", each with the current week range shown (e.g. "10–16 nov").

### Edge Cases

- Picking a time when the deadline is not a specific day: the time field is hidden, as today.
- Switching the deadline from a specific day to no date clears the date and time from the draft.
- Final date earlier than start date: disallowed in the picker; if loaded from existing data, the
  existing invalid-date message is shown on save.
- Monthly repetition on days 29–31 keeps the current rule behavior; the editor does not change it.
- Large font scale (200%): the full-screen editor scrolls, pickers remain usable, and labels wrap.
- Rotation or process recreation while a picker is open: the draft values already chosen are preserved;
  the picker may close.
- Storage failure on save: the existing error card with retry is shown and the editor stays open.
- Editing a recurring series: the "Só esta / Esta e as próximas" scope choice stays visible as today.

## Requirements *(mandatory)*

### Escopo e restrições do Ethos

- **In scope**: task editor layout and inputs, date/time pickers in the task editor and in the "data em que
  foi feita" field of the task detail, date display format in task rows, task detail and habit calendar
  month title, review-confirmation visibility, checklist item editing, repetition and weekly deadline
  labels, and the strings of these screens.
- **Out of scope**: Room schema, task/recurrence/review/accounting rules, task detail layout (future spec),
  habit editor (except the month title format), navigation, icons, strings of other screens.
- **Offline/persistence**: unchanged; the editor saves through the existing actions. Failures keep the
  existing recoverable error behavior.
- **Accessibility**: date/time fields announce label and value ("Prazo, segunda, 10 de novembro"); remove
  and reorder checklist controls have descriptions including the item text; touch targets ≥ 48 dp.
- **Permissions/personal data**: N/A. No new permissions; task content is not logged.
- **Dates/time zone**: all dates are local calendar days in the device time zone, as today; week starts
  Monday (spec 003 FR-008).

### Functional Requirements

- **FR-001**: The task editor MUST be full-screen, with title, deadline kind, date, time and list visible
  first and the remaining fields inside a "Mais opções" section that starts collapsed for new tasks.
- **FR-002**: "Mais opções" MUST start expanded when editing a task whose description, checklist,
  difficulty, importance, repetition or ending differs from the new-task defaults.
- **FR-003**: Due date, recurrence start, final date and actual completion date MUST be chosen with a
  calendar picker; time MUST be chosen with a 24-hour clock picker. No date or time text entry remains in
  the task flow.
- **FR-004**: Pickers MUST open on the current value (or today), keep the previous value on cancel, allow
  clearing optional values (time, date when the deadline kind allows none), and store exactly the picked
  local day.
- **FR-005**: The actual completion picker MUST not allow days after today; the final-date picker MUST not
  allow days before the start date.
- **FR-006**: The app MUST format dates as defined in User Story 3 in task rows, the task detail and the
  habit calendar month title, using pt-BR month and weekday names and 24-hour time.
- **FR-007**: The review explanation and confirmation MUST be shown only when editing an open occurrence
  that is overdue and whose date differs from its original date; the existing rule that rejects the save
  without confirmation is unchanged.
- **FR-008**: The checklist MUST be edited as a list of item fields with add, remove and move up/down;
  empty items are dropped on save and the saved order equals the shown order.
- **FR-009**: Repetition MUST be chosen through the options of User Story 6, mapping one-to-one onto the
  8 existing recurrence kinds and their parameters; existing tasks MUST round-trip unchanged.
- **FR-010**: Weekly deadline options MUST read "Até domingo" and "Até sexta (dias úteis)" and show the
  current week range.
- **FR-011**: Closing the editor with unsaved changes MUST ask for confirmation before discarding.
- **FR-012**: Saving MUST produce the same stored task, occurrence and audit results as the current editor
  for equivalent inputs.
- **FR-013**: Visible texts touched by this feature MUST be short and user-oriented, in pt-BR resources;
  long rule explanations are replaced by a one-line hint or removed when the picker prevents the error.

### Key Entities

- **Task draft (UI)**: values being edited before save: fields, chosen dates/time, checklist items,
  repetition choice and ending. Maps to the existing task fields and repeat rule; no new stored entity.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A task with title and a specific day can be created with at most 4 interactions after
  opening the editor (type title, open date, pick day, save), with no scrolling at 100% font scale.
- **SC-002**: Zero free-text date or time fields remain in the task editor and task detail.
- **SC-003**: The same date is shown with identical wording in the list and detail for all 5 cases of
  User Story 3; no ISO date appears on any task or habit calendar screen.
- **SC-004**: All 8 recurrence kinds round-trip through the editor with identical stored kind and
  parameters (automated check).
- **SC-005**: The review confirmation appears in 0 of the non-overdue edit cases and in 100% of the
  overdue date-change cases.
- **SC-006**: Every flow in this spec is operable with TalkBack and at 200% font scale on a 360 dp wide
  screen.

## Assumptions

- Material 3 date and time pickers are acceptable as the picker style; no custom calendar is built.
- The default for a new task stays "specific day = today", as in the current editor.
- Choosing weekdays Monday to Friday under "Toda semana" stores the custom-weekdays kind; the weekdays
  kind is reached through "Todo dia → Dias úteis". Existing tasks keep whichever kind they already have.
- The coin estimate stays next to difficulty and importance inside "Mais opções".
- Checklist reordering uses up/down controls (drag can come later); this is enough for accessibility.
- The habit calendar change is limited to the month title text; its layout is out of scope.
