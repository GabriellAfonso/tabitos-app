# Feature Specification: Calendar with compact grid and day list below

**Feature Branch**: `030-calendar-list-below` (no branch created; work stays on the current branch)

**Created**: 2026-11-17

**Status**: Implemented (2026-11-17)

**Input**: Design conversation on 2026-11-17 with prototype `prototypes/calendario/index.html` (rounds 1–3) and
`TODO/refazer_calendario.md`. The user chose option "C · Lista abaixo" and asked to apply it (short spec, no
plan/tasks documents).

## Current state (verified 2026-11-17)

- `CalendarContent` (spec 009) stacks in one scrolling column: period title, "Anterior · Ir para hoje · Próximo",
  the Mês | Semana | Dia control, three chip rows (Tipos, Lista, Hábito), the grid with one summed number per cell
  and the selected day's items below it. On a phone the grid reaches below the fold and the day is off screen.
- The user mainly opens the calendar to see upcoming tasks and uses it little otherwise.
- The user's real routine is stored in the app; this feature changes no data, rule or schema (Room stays 7).

## Decisions

1. Mês, Semana and Dia stay. The segmented control is centered at the top of the page. "Ir para hoje" is an icon
   at its left, shown only when the visible period does not contain today (the slot stays reserved so the control
   does not move); the filter icon is at its right and shows a dot while any filter is active.
2. One centered period row below: `‹ Novembro 2026 ›` (month), `16–22 nov` / `30 nov – 6 dez` (week),
   `ter, 17 nov` (day); the year shows on week/day labels outside the current year. Arrows move by the view's unit.
3. Filters (type, list, habit) move to a bottom sheet ("Mostrar", "Lista", "Hábito", Limpar, Pronto) and apply as
   they are tapped. Each active filter appears as a removable chip under the period row. Choosing a habit still
   hides tasks; excluded habits are listed only while selected.
4. Month view (option C): compact grid always visible at the top, not scrolling with the list. Each cell marks
   what it has, separately: filled primary dot for tasks, filled neutral dot for habit records, tertiary ring for a
   tracker entry; a legend under the grid names them. Below a divider, only the selected day's list scrolls, with
   the day name pinned at its top. Tapping a day only changes the list.
5. Week view becomes an agenda of the seven days (Monday–Sunday): day number on the left, tasks, a one-line
   summary of habit records and the tracker line on the right, "Nada" when empty. Tapping a day number opens the
   Day view for that date.
6. Day view: the day name and its list, with the arrows moving one day.
7. Day list: "Tarefas" (title, list name, time when set, "Vencida" when its due date passed), "Hábitos" (habit tile,
   "Feito" for practice habits or "N vezes" for avoid/limit, which keep the error container colors) and
   "Acompanhamentos" (habit name, "preenchido às HH:mm" or "Sem dados", "Preencher"/"Editar" inside the tracker
   limits). Tapping a task or habit opens its existing popup; empty days say "Nada neste dia" (with "com os
   filtros atuais" when a filter is active).
8. Grid cells keep their TalkBack text (date and counts, "sem dados" for trackers) and the sideways scroll that
   only appears with large fonts.

## Unchanged rules (spec 009)

- Tasks per day keep the `inDay` rule of the former typed-date lookup; completed tasks stay hidden.
- Habit records are shown by their real date; the calendar creates no habit scheduling.
- A day without a tracker entry shows "Sem dados", never zero; "Preencher" only inside the tracker limits.
- Filters combine; the view, anchor and filters survive recreation through `SavedStateHandle` as before.

## Out of scope / open

- Completing a task straight from the calendar, showing completed tasks, a "+" that creates a task on the chosen
  day, and which view opens first stay as they are and remain open questions in `TODO/refazer_calendario.md`.
- Options A (full-screen grid with a sheet), B (grid collapses on tap) and D (compact grid that collapses on
  scroll) were not chosen.

## Acceptance

- In Month view on a phone the whole grid and the start of the selected day's list are visible without scrolling.
- Pure functions in `CalendarPresentation.kt` (period label, "Hoje" visibility, active filter chips and removal,
  habits offered in the sheet, task meta) are covered by JVM tests; existing `CalendarModelTest` keeps passing.
- `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` passes. Instrumented tests are not run
  without explicit authorization.
