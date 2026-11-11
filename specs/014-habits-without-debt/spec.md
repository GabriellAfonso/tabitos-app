# Feature Specification: Habits Without Debt

**Feature Branch**: `master` (no branch hook registered)

**Created**: 2026-11-10

**Status**: Implemented (see verification.md)

**Input**: User description: "Habits must not behave like tasks. A missed habit opportunity simply passes:
it never becomes a pending item, never goes to Revisão and never costs coins. Periods close automatically
after a 2-day late-recording window. Avoid/limit habits succeed by default when no event is recorded.
Habit metric is consistency (streak and success rate). Simplify the habit editor. Creation keeps an
explicit choice between Tarefa, Hábito and Quebrar hábito. Recurring tasks stay as they are."

**Origin**: Claude Code conversation on 2026-11-10 (no transcript ID available). The user brought an
outside explanation: a task's value is the result and it keeps waiting when skipped; a habit's value is
consistency and a skipped occurrence just passes ("Se eu pular hoje, amanhã isso ainda tá me esperando?").
All decisions are recorded in this file. Related specs: `specs/004-habits-goals/spec.md`,
`specs/005-coins-xp-levels/spec.md`, `specs/011-habit-toggle-demo/spec.md`.

## Current State (verified in code on 2026-11-10)

- `HabitRules.periods` marks an ended period without success and without review as `PENDING`; pending
  periods feed the Revisão count (`HabitsUiState.pendingCount`) and wait for `HabitAction.Review`.
- Practice success is recognized immediately; failure only after review. Avoid/limit periods always need a
  review after they end, even with zero events (spec 004 FR-004).
- `AccountingProjection.habits` charges an importance penalty for a confirmed practice `FAILURE` period,
  a penalty for each avoid/limit event above the target, and rewards an avoid/limit period only when it was
  reviewed as `SUCCESS`.
- Streaks become `uncertain` and reset while a period is `PENDING`.
- Records may be added or corrected for any past date since creation; corrections can reopen reviewed
  periods.
- The habit editor asks for a partial-period target when starting mid-period (`PARTIAL_REQUIRED`) and
  saves through a "Prévia do progresso recalculado" step.
- The "+" on Hoje offers "Nova tarefa" and "Novo hábito"; the habit type (praticar/evitar/limitar) is
  chosen inside the habit editor.

## Decisions (user, 2026-11-10)

1. A missed habit never removes coins.
2. Late-recording window: 2 days after a period ends.
3. Periods pending today close as missed, with no penalty and no review.
4. Creation stays explicit: the user chooses Tarefa, Hábito or Quebrar hábito. A single question that
   infers the type ("deve ficar pendente?") was rejected.
5. Recurring tasks keep their current behavior (missed occurrences still go to review).
6. Accepted from the proposal: automatic closing, avoid/limit default success, consistency metrics,
   simplified habit editor, Revisão only for tasks.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A missed habit just passes (Priority: P1)

As a user, when I do not practice a habit, the day (or week/month) closes by itself as missed. Nothing is
added to Revisão, no coins are taken, and I am not asked to confirm anything.

**Why this priority**: This is the core rule that separates habits from tasks.

**Independent Test**: Create a daily practice habit, skip a day, advance the clock 3 days; verify the day
is "perdido", Revisão shows no habit item, balance unchanged and streak reset.

**Acceptance Scenarios**:

1. **Given** a daily practice habit without a record on Monday, **When** Thursday starts, **Then** Monday is
   closed as missed, without review and without coin change.
2. **Given** the same habit, **When** it is Tuesday or Wednesday, **Then** Monday is still open for a late
   record and is not shown as pending anywhere.
3. **Given** a weekly "3x por semana" habit with 2 records, **When** the week ends and 2 days pass, **Then**
   the week closes as missed with no penalty.
4. **Given** Revisão, **When** opened, **Then** it lists only tasks; its badge and the Hoje banner count only
   tasks.
5. **Given** a practice record, **When** made inside an open or late window, **Then** the existing reward
   for the practice is granted once, as today.

---

### User Story 2 - Breaking a habit succeeds by default (Priority: P1)

As a user with an avoid or limit habit, a day/period without recorded events counts as success when it
closes; I only act when the unwanted thing happens.

**Why this priority**: Today these habits demand a daily review even when nothing happened.

**Independent Test**: Create an avoid habit, record nothing for a day, advance past the window; verify
success, streak +1, reward granted once, no review.

**Acceptance Scenarios**:

1. **Given** an avoid habit with no events on a day, **When** that day closes, **Then** it is a success,
   the existing success reward is granted once, and no review is requested.
2. **Given** a limit habit "até 2 por semana" with 3 events, **When** the week closes, **Then** it is
   missed, without closing reward; event consequences follow FR-007.
3. **Given** an event recorded during the late window, **When** the period has not closed yet, **Then** the
   outcome uses it; after closing, the outcome no longer changes.

---

### User Story 3 - See consistency, not debt (Priority: P2)

As a user, each habit shows its current streak and success rate, and the habit detail calendar marks done
and missed days.

**Why this priority**: Consistency is the value of a habit; it replaces the pending/review signal.

**Independent Test**: With a mix of done, missed and open days, compare streak, rate and calendar marks.

**Acceptance Scenarios**:

1. **Given** closed periods, **When** viewing the habit card, **Then** it shows "Sequência: N" and the
   success rate of the last closed periods (30 days, 8 weeks or 6 months by unit).
2. **Given** an open or late-window period, **When** computing the streak, **Then** it neither breaks nor
   counts the streak until it closes; streaks are never "incertos".
3. **Given** the habit detail calendar, **When** shown, **Then** days are marked as feito, perdido,
   em aberto or fora do agendamento with distinct, accessible labels.

---

### User Story 4 - Simpler habit editor (Priority: P2)

As a user, I set how often a habit should happen with plain options and save directly.

**Independent Test**: Create each frequency option and verify the stored rule equals today's equivalent.

**Acceptance Scenarios**:

1. **Given** a practice habit, **When** choosing frequency, **Then** the options are: Todo dia, Dias
   escolhidos, X vezes por semana, X vezes por mês, mapped to the existing day/weekday/week/month rules.
2. **Given** a habit started mid-week or mid-month, **When** saving, **Then** no partial-target question is
   asked; the first period's target is limited to the days still available in it.
3. **Given** the editor, **When** I tap Salvar, **Then** the habit is saved without an intermediate preview.

---

### User Story 5 - Explicit creation of task, habit or habit to break (Priority: P3)

As a user, the "+" on Hoje offers Nova tarefa, Novo hábito and Quebrar hábito, so I choose explicitly.

**Independent Test**: Open each option and verify the editor that opens and its preselected type.

**Acceptance Scenarios**:

1. **Given** Hoje, **When** tapping "+", **Then** three options appear: Nova tarefa, Novo hábito, Quebrar
   hábito.
2. **Given** "Quebrar hábito", **When** chosen, **Then** the habit editor opens with the avoid/limit types
   offered (avoid preselected) and no practice option; "Novo hábito" opens with practice preselected.

### Edge Cases

- Period closing while the app is closed: closing is computed from dates on next use; no background job.
- Device date moved backwards: periods reopen only by date arithmetic; already granted rewards follow the
  existing ledger reconciliation and are not duplicated.
- Paused habit: paused days create no opportunity and never count as missed (as today).
- Rule edits mid-period: existing current/next-period choice is kept; closed periods never change.
- Existing reviewed periods: kept as history with their recorded outcome.
- Existing confirmed habit penalties from before this feature: kept in history, not refunded (assumption).
- Timer-completed habit duration goals (spec 008) record through the same rules and respect the window.
- Habit reminders (spec 007) never mention pending reviews for habits.

## Requirements *(mandatory)*

### Escopo e restrições do Ethos

- **In scope**: habit period outcome rules, late window, habit consequences in the ledger projection,
  Revisão/Hoje counts, streak and rate, habit card and detail calendar marks, habit editor frequency and
  removal of partial target/preview, "+" menu options.
- **Out of scope**: task rules and recurring tasks (unchanged), merging tasks and habits, new rewards
  (e.g. streak bonus), calendar screen of spec 009 beyond habit marks, trackers.
- **Offline/persistence**: computed from stored records/rules/intervals; Room is the source of truth. Any
  schema change requires a preserving migration; existing records and reviews are preserved.
- **Accessibility**: calendar marks have text labels, not color only; streak/rate readable by TalkBack;
  200% font scale.
- **Permissions/personal data**: N/A; no new permission, no logging of habit names.
- **Dates/time zone**: local calendar days; week Monday–Sunday; window = 2 calendar days after the period's
  last day (a Monday daily period closes when Thursday starts).

### Functional Requirements

- **FR-001**: A habit period MUST be "em aberto" until its last day, "janela de registro" for the 2 days
  after it, and "fechado" afterwards. Closed outcomes are success or missed; there is no pending state.
- **FR-002**: Practice periods MUST succeed when the target is reached (immediately, as today) and close as
  missed otherwise.
- **FR-003**: Avoid/limit periods MUST close as success when events are within the target (zero for avoid)
  and as missed otherwise; no completeness review is requested.
- **FR-004**: Habits MUST NOT appear in Revisão; Revisão badge, Hoje banner and filters count only tasks.
- **FR-005**: Missed habit periods MUST NOT change coins or XP.
- **FR-006**: Practice rewards stay as today; avoid/limit success rewards MUST be granted once when the
  period closes as success, without review.
- **FR-007**: Avoid/limit events above the target keep the current per-event coin penalty (decided by the
  agent during autonomous execution, 2026-11-10: recording the unwanted act is an explicit action, not a
  missed opportunity; revisit if the user prefers removing it).
- **FR-008**: Records and corrections MUST be accepted only for dates whose period is not closed; closed
  periods are immutable.
- **FR-009**: Periods pending at the moment of upgrade MUST be treated as closed (missed or success per
  FR-002/003) with no penalty and no review.
- **FR-010**: Streak MUST count consecutive successful closed periods, ignoring open/window periods; the
  "incerto" state is removed.
- **FR-011**: The habit card MUST show the streak and the success rate of the last 30 daily, 8 weekly or 6
  monthly closed periods; the detail calendar MUST mark feito, perdido, em aberto and fora do agendamento.
- **FR-012**: The practice editor MUST offer Todo dia, Dias escolhidos, X vezes por semana, X vezes por mês;
  it MUST NOT ask for a partial target nor show the recalculated preview; the first partial period's target
  is min(target, available days).
- **FR-013**: The "+" menu MUST offer Nova tarefa, Novo hábito and Quebrar hábito, opening the right editor
  with the matching type group preselected.
- **FR-014**: Texts MUST describe habits as chances ("perdido", "sequência"), never as debts ("pendente",
  "revisar") in touched screens.

### Key Entities

- **Habit period (computed)**: start, end, target, count, state (aberto / janela / fechado), outcome
  (sucesso / perdido). No stored pending state.
- **Habit review (existing, stored)**: kept for history; no longer created for new periods.
- **Financial outcome (computed)**: habit failure penalties removed; avoid/limit success no longer depends
  on review.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 0 habit items in Revisão in every scenario of this spec.
- **SC-002**: 0 coin changes caused by missed habit periods across a 60-day simulated history.
- **SC-003**: An avoid habit with no events for 7 days earns exactly 7 success rewards with zero user
  actions.
- **SC-004**: Creating a practice habit takes no extra step beyond name, frequency and Salvar.
- **SC-005**: Existing users' pending habit periods disappear from Revisão after the upgrade with balance
  unchanged.

## Assumptions

- Reviews and penalties recorded before this feature remain as history; no refunds.
- The existing current/next-period choice when editing a goal stays.
- Habit lists, colors, icons, pause/exclude, reminders and timers behave as today except where stated.
- "Quebrar hábito" groups the existing avoid and limit types; no new habit type is created.
- "Quebrar hábito" is implemented as a third "+" option (literal reading of the user's answer; decided by the
  agent during autonomous execution, 2026-11-10). The Hábitos page also offers "Quebrar hábito".
- Automatic closings reach the ledger through a settle step run when habits load and when the day changes.
