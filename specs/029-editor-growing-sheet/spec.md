# Feature Specification: Growing editor sheet for tasks, good habits and bad habits

**Feature Branch**: `029-editor-growing-sheet` (no branch created; work stays on the current branch)

**Created**: 2026-11-17

**Status**: Implemented (2026-11-17); instrumented tests and device checks not run

**Input**: Design conversation on 2026-11-17 with prototype `prototypes/editores-criacao/index.html`. In round 1 the
user chose "A · Folha rápida"; in round 2 they chose "A2 · Folha que cresce" and asked to run specify, plan, tasks
and implement. Decisions are recorded in `TODO/editores_criacao.md`.

## Current state (verified 2026-11-17)

- `features/tasks/TaskEditor.kt` is a full-screen dialog with a "Cancelar · Nova/Editar tarefa · Salvar" top bar.
  It shows quick fields (title, Tipo Normal/Com cronômetro, Prazo chips, date and time pickers, Lista chips), then
  a "Mais opções" toggle with description, checklist, difficulty (game mode), importance, coin estimate, repetition
  and ending. When editing it also shows the recurring scope choice and the review confirmation.
- `features/habits/HabitEditor.kt` is a full-screen dialog with a headline title, the coin estimate above the name,
  name, description, rule chips (practice frequency, timed, or avoid/limit), the goal-effect choice and future type
  hint when editing, difficulty (game mode), importance, color, icon (one row plus "Ver mais", spec 028), list chips
  stacked one per line, and Cancelar/Salvar at the bottom.
- Both editors serve creation and editing (`draft.editing`). The "+" menu picks the type: Criar tarefa, Criar bom
  hábito, Quebrar mau hábito (spec 020).
- The user's real routine is stored in the app. This feature changes no stored data, rules or schema (7).

## Decisions

1. One "growing sheet" layout for the three types (task, good habit, bad habit), used for creating and editing.
2. **Compact state (create)**: a bottom sheet over the current page. The name field is focused and the keyboard
   opens. Below it are a description line ("Descrição", which opens a text sheet), then quick chips that show the
   current value and open a choice sheet, then a round Save button.
   - Task chips: when (summary of deadline, date and time, e.g. "Amanhã 09:00", "Até dom, 22 nov", "Sem data"),
     repetition, list, timer toggle ("Cronômetro" adds the timer; "Com cronômetro ✕" removes it) and duration
     while timed.
   - Good habit chips: frequency, days (only for chosen days), times per week or month (weekly/monthly), timer
     toggle and minutes while timed, color and icon.
   - Bad habit chips: avoid/limit, limit ("No máximo N por dia/semana/mês", limit only; one sheet sets both the
     count and the period), color and icon.
   - For habits, the color and icon tile next to the name also opens the color and icon sheet.
   - A last chip, "Mais", expands the sheet.
3. **Expanded state**: the sheet takes the full screen and the keyboard closes. A header has a collapse arrow
   (create) or a close button (edit) and the title (Nova tarefa, Novo hábito, Quebrar hábito, Editar tarefa,
   Editar hábito). The name and quick chips stay at the top. Below, "Mais opções" lists the remaining fields as
   rows showing label and value, each opening its choice sheet:
   - task: repetition ending (only when repeating), description, checklist (not timed), importance, difficulty
     (game mode only);
   - habits: list, description, importance, difficulty (game mode only).
   The coin and XP estimate appears in the expanded state only, and only with game mode on. The Save button is a
   text button at the bottom. The collapse arrow returns to the compact state and keeps everything typed.
4. **Edit** opens directly in the expanded state, without focusing the name, so the keyboard stays closed.
   Edit-only controls stay above Save: "Só esta / Esta e as próximas" for recurring tasks, the review confirmation
   when an overdue occurrence's date changes, the habit goal-effect choice and the future type hint.
5. **Choice sheets**: single choices are radio lists. Date and time keep the existing pickers (no typing). The
   checklist sheet keeps item editing with drag reorder and TalkBack actions (spec 015). The color and icon sheet
   reuses the existing color picker and the icon row with "Ver mais" (spec 028). Number values (minutes, times,
   max count, monthly day, interval, total count) keep the existing − / + stepper inside their sheet.
6. Out of the editor: reminders stay in the detail popups, and there is no type switcher (the "+" menu still picks
   the type). Both were open points in the prototype; the user approved A2 without them.
7. Rules unchanged: 5 repeat choices over 8 RepeatKind, practice frequencies, timed items without checklist,
   game mode hiding difficulty and estimate, bad habit starting red with the block icon, save validation and error
   messages, busy state blocking edits.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Quick capture (Priority: P1)

From Hoje, the user taps "+", chooses a type, types a name and saves without leaving the page context.

**Why this priority**: creating items is the most frequent action and the main reason for the redesign.

**Independent Test**: open "+" › Criar tarefa, type "Comprar pão", tap Save: the task appears in Caixa de entrada
with no date.

**Acceptance Scenarios**:

1. **Given** Hoje is open, **When** the user chooses Criar tarefa, **Then** a sheet opens over Hoje with the name
   focused and the keyboard open.
2. **Given** the compact sheet with a name, **When** the user taps a chip such as "Sem data" and picks "Amanhã",
   **Then** the chip shows "Amanhã" and Save stores the task due tomorrow.
3. **Given** the compact sheet for a good habit, **When** the user taps the frequency chip and picks "Semanal",
   **Then** a "3 vezes por semana" chip appears and Save stores a weekly goal of 3.

### User Story 2 - Full options when needed (Priority: P2)

The user expands the sheet to reach the less common fields without leaving the item being created.

**Why this priority**: every field that exists today must stay reachable.

**Independent Test**: create a task, tap "Mais", add two checklist items and set importance to Alta; after saving,
the details show both.

**Acceptance Scenarios**:

1. **Given** the compact sheet, **When** the user taps "Mais", **Then** the sheet fills the screen, the keyboard
   closes and rows show each remaining field with its value.
2. **Given** the expanded sheet, **When** the user taps the collapse arrow, **Then** the compact sheet returns with
   the same values.
3. **Given** game mode off, **When** the sheet is expanded, **Then** there is no difficulty row and no estimate.

### User Story 3 - Edit an existing item (Priority: P2)

The user edits a task or habit from its detail popup with every field visible at once.

**Independent Test**: edit a recurring task, change its time and choose "Esta e as próximas": the series changes.

**Acceptance Scenarios**:

1. **Given** a task detail popup, **When** the user taps Editar, **Then** the expanded sheet opens with the
   "Editar tarefa" header, the current values and no keyboard.
2. **Given** an overdue occurrence being edited, **When** its date changes, **Then** the review confirmation appears
   above Save and saving without it shows the existing error.
3. **Given** a habit being edited, **Then** the goal-effect choice and the future type hint appear above Save.

### Edge Cases

- Closing the compact sheet by back or outside tap with text typed discards the draft, as the current editor does
  when cancelled; the sheet cannot be closed while a save is running.
- Switching a task to "Com cronômetro" with checklist items shows the existing warning that the checklist will be
  removed.
- Choosing "+ Nova lista" opens the existing new list dialog without losing the draft.
- A save error (blank name, invalid value) appears at the top of the sheet content, in both states.
- Large font: chips wrap to more lines and rows grow; the compact sheet scrolls inside itself instead of covering
  the keyboard.

## Requirements *(mandatory)*

### Escopo e restrições do Ethos

- Scope: the task and habit editors only. Out of scope: reminders in the editor, a type switcher, the list and
  reward editors, rule or schema changes.
- Offline and persistence: unchanged. Saving uses the existing ViewModel actions and repositories.
- Accessibility: each chip and row is announced as "label, value"; the sheet header and Save have labels; touch
  targets are at least 48 dp; font scaling is supported.
- Permissions and personal data: N/A (no new permission; nothing is logged).
- Dates and recurrence: unchanged rules; only the presentation of the controls changes.

### Functional Requirements

- **FR-001**: Creating a task, a good habit or a bad habit MUST open the compact sheet with the name focused.
- **FR-002**: The compact sheet MUST show the quick chips of Decision 2 with their current values; tapping a chip
  MUST open its choice sheet, and choosing a value MUST update the chip.
- **FR-003**: "Mais" MUST expand the sheet to full screen, close the keyboard and show the remaining fields as
  label/value rows (Decision 3). Collapsing MUST keep the draft.
- **FR-004**: Editing MUST open the expanded sheet with the edit title and without focusing the name.
- **FR-005**: The edit-only controls (scope choice, review confirmation, goal effect, future type hint) MUST keep
  their current behavior and appear above Save.
- **FR-006**: Difficulty and the coin/XP estimate MUST be hidden with game mode off; the estimate MUST appear only
  in the expanded state.
- **FR-007**: Every field reachable in the current editors MUST be reachable in the new sheet, with the same
  options, defaults and validation.
- **FR-008**: Date and time MUST use the existing pickers; numbers MUST use the existing − / + steppers.
- **FR-009**: The checklist MUST keep drag reorder with TalkBack actions; color and icon MUST reuse the existing
  pickers (spec 028).
- **FR-010**: Saving, cancelling and error display MUST use the existing ViewModel actions; no stored data, rule or
  schema changes.
- **FR-011**: Chips and rows MUST be announced by screen readers as label and value.

### Key Entities

- No new entity. Existing task and habit drafts are reused as they are.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A task with only a name is created in 2 actions after choosing the type (type the name, tap Save).
- **SC-002**: 100% of the fields of the current editors are reachable when creating and when editing.
- **SC-003**: Every edit-only confirmation available today is still shown in the same situations.
- **SC-004**: Existing rules and ViewModel tests keep passing with no change to stored data.
- **SC-005**: At 200% font size, every control in both states can be reached by scrolling inside the sheet.

## Assumptions

- The task "when" chip opens a sheet with the deadline kinds and, for a specific day, the date and time pickers.
  This matches the compact chips in the prototype (date and time summarized in one chip).
- In the compact state, repetition ending stays in "Mais", as it was under "Mais opções" before.
- Choice sheets are bottom sheets in the app's surface colors; the prototype's colors and copy are guidance, not
  pixel specs.
- Instrumented tests are written but not run without explicit authorization (AGENTS.md).
