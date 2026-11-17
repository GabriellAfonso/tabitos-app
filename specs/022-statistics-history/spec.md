# Feature Specification: Statistics page (with history) and erasing history of deleted items

**Feature Branch**: `022-statistics-history` (no branch created; work stays on the current branch)

**Created**: 2026-11-17

**Status**: Implemented (2026-11-17)

**Input**: User description: body of `TODO/specify_estatisticas_app.md` ("Statistics page (with history) and erasing
history of deleted items"), decided in a design conversation on 2026-11-16/17 with prototype
`prototypes/estatisticas/index.html` (round 3, option P1) and notes in `TODO/planejamento_estatisticas.md`.

## Current state (verified 2026-11-17)

- The only history screen is "Histórico de moedas", a sub-page of Configurações shown only in game mode. It lists
  ledger entries grouped by day (gain, penalty, reversal, reapply, date correction, reward purchase).
- Task audits are still recorded but shown nowhere since spec 021 removed "Registros e correções" from the task
  detail popup.
- Since 2026-11-17 (spec 021 FR-012a) a postpone is recorded as its own kind (`POSTPONED`) and its undo as
  `POSTPONE_UNDONE`, keeping the previous deadline and the new date. Earlier postpones are stored as edits and cannot
  be told apart; they do not count as postponed.
- Available data: task occurrences and audits, habit records and periods (spec 014 constancy/streak rules), timer
  sessions/segments (tasks, habits, timed rewards), ledger entries (each stores a copied title), reward purchases.
- Deleting a habit moves it to "Hábitos excluídos" (Configurações), restorable, with history kept. Deleting a task
  keeps its audits. Recurring delete asks for a scope labelled "Só esta" / "Todas".
- The user is using the app with their real routine (since 2026-11-11). Existing data must be preserved.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See how I am doing in a period (Priority: P1)

As a user, I open "Estatísticas" from the ☰ menu and, on the "Resumo" tab, see for this week how many tasks I
completed, how many on time, how many I postponed, a bar chart per day, the constancy and streak of each habit,
the time spent with timers and, in game mode, coins earned/spent and my level.

**Why this priority**: it is the main new value; it reuses existing data without changing any rule.

**Independent Test**: with sample data in this week, open Estatísticas and compare every number with the data
(tasks completed, on time, postponed, habit card numbers, timer totals, wallet history).

**Acceptance Scenarios**:

1. **Given** the ☰ menu, **When** I open it, **Then** "Estatísticas" appears next to Hoje, Calendário, Revisar
   pendências and Recompensas, with game mode on and off.
2. **Given** I open Estatísticas, **Then** the "Resumo" tab is selected and the period is "Esta semana" (Monday to
   Sunday containing today).
3. **Given** 4 tasks completed this week, 3 of them with a deadline and 2 of those completed on or before their
   deadline day, **Then** Tarefas shows 4 completed and "67% no prazo"; tasks without a date are left out of the
   percentage; with no completed dated task the percentage shows "—".
4. **Given** 2 postpones recorded this week (and one older edit-recorded postpone), **Then** "Adiadas" shows 2.
5. **Given** the week chart, **Then** it shows one bar per day Monday–Sunday with completed tasks; future days of
   the current period are empty bars.
6. **Given** active and paused habits, **Then** each one shows constancy % and current streak equal to its habit
   card for the same period; bad habits are shown in red; deleted habits are not listed; an overall constancy is
   shown.
7. **Given** finished timer sessions of tasks, habits and a timed reward in the period, **Then** "Tempo com
   cronômetro" shows the total and per task/habit, excluding the reward session.
8. **Given** game mode on, **Then** "Moedas e nível" shows coins earned, coins spent on rewards in the period, the
   current level and XP progress; **Given** game mode off, **Then** that section is absent.
9. **Given** no data in the period, **Then** each section shows an empty message instead of zeros-only charts.

---

### User Story 2 - Change the period (Priority: P1)

As a user, I change between week, month, year and all time with one row `‹ Esta semana ▾ ›`.

**Why this priority**: the Resumo is only useful if I can look at other periods.

**Independent Test**: move back and forth with the arrows, change kind, reopen the app.

**Acceptance Scenarios**:

1. **Given** the period row, **When** I tap the name, **Then** a menu with Semana / Mês / Ano / Tudo opens with the
   current kind checked; choosing one goes to the current period of that kind (this week/month/year).
2. **Given** a week/month/year, **When** I tap ‹ or ›, **Then** the previous/next period of that kind is shown;
   ‹ is disabled when the period starts on or before the first data day; › is disabled at the current period.
3. **Given** "Tudo", **Then** there are no arrows and the label shows "Tudo" with "desde <first data date>".
4. **Given** labels, **Then** weeks read "Esta semana", "Semana passada" or "2 nov a 8 nov" (year only if different
   from the current one); months "Novembro 2026"; years "2026".
5. **Given** month, the chart is per day; **Given** year or "Tudo", the chart is per month; in "Tudo" with more than
   2 years of data the chart is per year.
6. **Given** I chose a period, **When** I navigate elsewhere in the app and come back, **Then** the same period is
   shown; **When** the app is reopened (process restarted), **Then** it is "Esta semana" again.
7. **Given** a period change, **Then** both tabs (Resumo and Histórico) use the new period.

---

### User Story 3 - See the history of what happened (Priority: P1)

As a user, on the "Histórico" tab I see a timeline of what happened in the selected period, grouped by day, and can
filter by Tarefas, Hábitos, Tempo or Moedas. It replaces "Histórico de moedas".

**Why this priority**: the task audits are not visible anywhere since spec 021 and the coin history moves here.

**Independent Test**: create completions, a not-done, a missed deadline, a postpone and its undo, a habit done, a
bad habit occurrence, a timer session and a reward purchase; check each appears once in the right day and filter.

**Acceptance Scenarios**:

1. **Given** the Histórico tab, **Then** items are grouped by day, newest first; in Ano/Tudo there are month headers;
   long periods show the first page with "Mostrar mais".
2. **Given** filters Tudo / Tarefas / Hábitos / Tempo / Moedas, **When** I choose one, **Then** only items of that
   kind are listed; Moedas exists only in game mode and lists items with coins/XP.
3. **Given** the item kinds, **Then** shown are: task completed, not done, missed deadline, postponed ("de X para
   Y"), undone; habit done; bad habit occurrence and settled day; timer session finished; reward purchase; ledger
   entries. Not shown: created, edited, checklist item checked.
4. **Given** adjustments (date correction, reversal, undo of postpone), **Then** they appear in a discreet style
   (smaller, muted text), not hidden.
5. **Given** game mode on, **Then** each item shows its coins/XP; **Given** game mode off, **Then** coin/XP values,
   purchases and the Moedas filter are hidden; results recorded with game mode off never show coins.
6. **Given** Configurações, **Then** "Histórico de moedas" is no longer listed; "Hábitos excluídos" still is.
7. **Given** every ledger entry previously shown in "Histórico de moedas", **Then** it appears in Histórico (filter
   Moedas, period Tudo) with the same title, value and date.
8. **Given** an item of an existing task or habit, **When** I tap it, **Then** its existing detail popup opens;
   a reward purchase opens the reward (nothing if the reward was deleted).

---

### User Story 4 - Erase the history of a task or habit when deleting it (Priority: P2)

As a user, when I delete a task or habit I can choose to also erase its history; by default nothing changes.

**Why this priority**: new privacy/cleanliness request; the history would otherwise keep deleted items forever.

**Independent Test**: delete a task with the box unchecked (history stays), another with the box checked (history
gone, balance/XP unchanged), a recurring series with "Todas" + box, a habit with box checked.

**Acceptance Scenarios**:

1. **Given** the task delete dialog, **Then** it has a checkbox "Apagar também o histórico desta tarefa",
   unchecked by default; unchecked keeps today's behavior.
2. **Given** a recurring task, **When** I delete with "Só esta" and the box checked, **Then** only the history of
   that occurrence is erased; **When** with "Todas" and the box checked, **Then** the history of every occurrence
   of the series (past included) is erased.
3. **Given** the habit delete dialog, **Then** it has the same checkbox, unchecked by default; unchecked moves the
   habit to "Hábitos excluídos" as today; checked deletes the habit completely (not restorable) with its records,
   periods, timer sessions and history.
4. **Given** "Hábitos excluídos", **Then** each habit also offers "Apagar por completo", with a confirmation that it
   cannot be undone.
5. **Given** any erase, **Then** the wallet balance, total XP and level stay exactly the same; ledger entries of that
   task/habit remain with the title "Tarefa apagada" / "Hábito apagado".
6. **Given** a persistence failure during an erase, **Then** nothing is erased and an error is shown.

---

### User Story 5 - Erase history from the Histórico tab (Priority: P3)

As a user, when I tap an item of a task or habit that no longer exists, I see it was deleted and can erase its
history.

**Why this priority**: covers items deleted before this feature and deleted without the box.

**Independent Test**: delete a task keeping history, tap its item in Histórico, erase, confirm items are gone.

**Acceptance Scenarios**:

1. **Given** an item of a deleted task (or a habit in "Hábitos excluídos"), **When** I tap it, **Then** a dialog says
   it was deleted and offers "Apagar histórico" and "Cancelar" instead of opening details.
2. **Given** I confirm "Apagar histórico" (the dialog says it cannot be undone), **Then** every history item of that
   task (whole series for a recurring task) or habit disappears from the page; for a habit in "Hábitos excluídos"
   the habit is deleted completely; coins/XP stay the same.

### Edge Cases

- Week crossing a month or year boundary: the week label uses both dates ("29 dez a 4 jan"); the week belongs to
  Monday's week, regardless of month.
- First data day in the middle of a week: ‹ is disabled on that week; "Tudo" starts on that day.
- No data at all: "Tudo" shows "desde <today>" and empty messages; arrows of week/month/year still allow › only up to
  the current period.
- Day change while the page is open: the "current period" follows the injected clock on the next state update; no
  `now()` is read by the UI.
- Task completed on a day after its deadline: counts as completed but not on time; completed earlier than deadline:
  on time. Task with a week/month deadline: on time when completed on or before the last day of that window.
- Task completed and later undone: it no longer counts as completed; the undo item stays in Histórico.
- A postpone undone: the postpone still counts in "Adiadas" (it was recorded); the undo is a discreet item.
- Habit paused part of the period: constancy follows the habit card rule (paused days are not counted).
- Timer time crossing midnight: split between the local days it covered.
- A timer still counting: the stretch in progress is not counted until it is paused or finished.
- Ledger entries whose task/habit no longer exists: still shown in Moedas with their stored title.
- Large font (200%) and TalkBack: the period row, tabs, filters, chart bars (announced as "<dia>: N tarefas") and
  items are reachable and readable; touch targets ≥ 48 dp.
- Very long "Tudo" history: paging keeps the page responsive.
- Deleting a list (which deletes its tasks): keeps their history, as today; items appear as deleted and can be erased
  from Histórico (no checkbox on the list dialog in this version).

## Requirements *(mandatory)*

### Escopo e restrições do Ethos

- **In scope**: Estatísticas page with Resumo and Histórico tabs and a shared period; ☰ menu entry; removing
  "Histórico de moedas" from Configurações; erase-history checkbox in task and habit delete; "Apagar por completo" in
  Hábitos excluídos; "Apagar histórico" from Histórico for deleted items.
- **Out of scope**: habit tracker (spec 009 forms) statistics; editing or correcting records from this page; export;
  sharing; insights or goal texts; changes to completion, review, recurrence, reward or penalty rules; a checkbox in
  the list delete dialog.
- **Offline/persistence**: everything is local and read from the existing database. The page is read-only. Erasing is
  explicit, confirmed and one atomic transaction; a failure leaves everything unchanged and shows an error. No
  destructive migration; any schema change needs a preserving migration with tests.
- **Accessibility**: tabs, period menu, arrows, filters and items with spoken descriptions; chart has a text
  alternative per bar; 200% font; light and dark themes.
- **Permissions/personal data**: no new permission; no task/habit content in logs. Erasing removes personal data by
  the person's explicit choice.
- **Dates/time zone**: local calendar days from the injected clock; weeks Monday–Sunday; events are placed on the
  local day they happened.

### Functional Requirements

- **FR-001**: The ☰ menu MUST list "Estatísticas" with Hoje, Calendário, Revisar pendências and Recompensas, in both
  game modes.
- **FR-002**: The page MUST have tabs "Resumo" and "Histórico" and MUST open on Resumo.
- **FR-003**: Both tabs MUST share one period: kind Semana (Mon–Sun), Mês (calendar month), Ano (calendar year) or
  Tudo (from the first data day to today).
- **FR-004**: The period row MUST be `‹ <label> ▾ ›`; tapping the label opens the kind menu; changing kind MUST go to
  the current period of that kind; "Tudo" MUST have no arrows.
- **FR-005**: ‹ MUST be disabled when the period starts on or before the first data day; › MUST be disabled at the
  current period.
- **FR-006**: Labels MUST follow User Story 2 scenario 4 and the pt-BR date formats of spec 013.
- **FR-007**: The chosen period MUST persist while the app process lives and reset to "Esta semana" on a new process;
  it MUST NOT be stored in preferences.
- **FR-008**: Tarefas MUST show completed count, "% no prazo" (completed with deadline whose completion day is on or
  before the deadline day ÷ completed with deadline; "—" when the divisor is 0) and "Adiadas" (count of `POSTPONED`
  records in the period).
- **FR-009**: The task chart MUST show completed tasks per day (Semana, Mês) or per month (Ano, Tudo), or per year in
  Tudo when the data spans more than 2 calendar years; future buckets of the current period are empty.
- **FR-010**: Hábitos MUST show, for each active or paused habit, constancy % and current streak computed by the same
  functions as the habit card, plus an overall constancy; bad habits in red; deleted habits excluded.
- **FR-011**: "Tempo com cronômetro" MUST show total and per-task/habit duration of timer time already counted (closed
  stretches, paused or finished) of tasks and habits, placed on the local days it happened; timed reward sessions
  MUST be excluded.
- **FR-012**: "Moedas e nível" MUST appear only in game mode and show coins earned (positive entries), coins spent on
  rewards (purchases) in the period, current level and XP progress.
- **FR-013**: Histórico MUST list the item kinds of User Story 3 scenario 3 in the period, grouped by local day,
  newest first, with month headers in Ano/Tudo and paging with "Mostrar mais".
- **FR-014**: Histórico MUST offer filters Tudo, Tarefas, Hábitos, Tempo and (game mode only) Moedas.
- **FR-015**: Adjustment items (date correction, reversal, undo of postpone, reapply) MUST be shown in a discreet
  style.
- **FR-016**: With game mode off, coin/XP values, purchases and the Moedas filter MUST be hidden everywhere on the
  page; exempt results never show coins.
- **FR-017**: "Histórico de moedas" MUST be removed from Configurações with its route and now-unused texts; every
  ledger entry it showed MUST be available in Histórico.
- **FR-018**: Tapping an item MUST open the existing task or habit detail; a purchase opens the reward when it still
  exists; an item of a deleted task/habit MUST open the deleted-item dialog (FR-022).
- **FR-019**: The task delete dialog MUST have the checkbox "Apagar também o histórico desta tarefa", unchecked by
  default; with "Só esta" it erases that occurrence's history, with "Todas" the whole series' history.
- **FR-020**: The habit delete dialog MUST have the same checkbox ("… deste hábito"); checked deletes the habit
  completely (not restorable) with its records, periods, sessions and history; unchecked keeps today's behavior.
- **FR-021**: "Hábitos excluídos" MUST offer "Apagar por completo" with an irreversible-action confirmation.
- **FR-022**: Tapping an item of a task/habit that no longer exists (or is in Hábitos excluídos) MUST show that it
  was deleted and offer "Apagar histórico" with an irreversible-action confirmation.
- **FR-023**: Erasing MUST NOT change the balance, total XP or level; the task/habit's ledger entries MUST be kept
  with the title "Tarefa apagada" / "Hábito apagado".
- **FR-024**: Erasing MUST be one atomic transaction; on failure nothing is erased and an error is shown; nothing is
  erased without an explicit confirmed choice.
- **FR-025**: The plan MUST list every table that references a task or habit and state what is removed and what is
  anonymized.

### Key Entities

- **Statistics period**: kind (week/month/year/all) and anchor date; derives start/end days, label and navigation.
- **Task summary**: completed count, on-time ratio, postponed count, buckets (date range → count).
- **Habit summary**: per-habit constancy and streak (same rules as the card), overall constancy.
- **Timer summary**: total duration and per-activity duration of finished task/habit sessions.
- **Coin summary**: earned, spent on rewards, current level and XP progress.
- **History item**: moment, local day, kind, subject (task/habit/reward and whether it still exists), title, details
  (e.g. postpone from/to), coins/XP, adjustment flag.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: From any main page, the statistics of this week are reachable in 2 taps (☰, Estatísticas).
- **SC-002**: For every habit tested, constancy and streak on Resumo equal the habit card values in the same period.
- **SC-003**: 100% of ledger entries visible in the former "Histórico de moedas" appear in Histórico with the same
  values.
- **SC-004**: After any erase, wallet balance, total XP and level are identical to before in 100% of tested cases.
- **SC-005**: Delete with the box unchecked keeps 100% of the history items of that task/habit.
- **SC-006**: With game mode off, no coin, XP, level or purchase value is visible on the page.
- **SC-007**: Changing period or filter updates the page without a perceptible wait for a year of typical data.

## Assumptions

- "Coins earned" counts positive ledger amounts of task/habit results (gains and reapplies), not reversals; "coins
  spent" counts reward purchases (ledger debits of purchases) in the period.
- Level and XP progress are current values (not as of the end of the period).
- Paging shows 50 history items per page.
- A completed task's day is its recorded completion date; its moment is the recorded resolution time.
- The first data day is the earliest day among task records, habit records, timer sessions and ledger entries.
- The habit overall constancy is the mean of per-habit constancy of the listed habits with at least one counted
  period in the range.
- Deleting a list keeps the history of its tasks (as today); erasing them is done per item from Histórico.
