# Remarcar tarefa (adiar e antecipar no mesmo lugar)

**Estado**: pronto

**Como usar**: em outro chat, executar `$speckit-specify` (ou `/speckit-specify` no Claude Code) passando como
descrição todo o corpo abaixo da linha `---`. Antes de perguntar qualquer coisa, o chat executor deve ler este
arquivo, `.specify/memory/constitution.md`, `AGENTS.md` e as referências; perguntar só o que estiver em
"Open questions".

**Referências**:
- `specs/021-task-postpone-detail-popup/spec.md` (Adiar atual, pop-up de detalhes, lembretes, FR-012a)
- `specs/022-statistics-history/spec.md` e `specs/024-statistics-item-filter/spec.md` (contagem "Adiadas", aba Histórico)
- `specs/013-task-editor-pickers/spec.md` (seletor de datas, formato pt-BR das datas)
- `specs/016-timed-tasks-habits/spec.md` e `specs/018-timer-discard/spec.md` (mudança de 2026-11-18 nas
  mensagens "Desfazer")

**Origem**: conversa de design no Claude Code em 2026-11-18 (sem ID de transcript acessível neste arquivo). Todas
as decisões estão escritas abaixo; o arquivo não depende da conversa.

Salvar este prompt não autoriza especificar, planejar, implementar nem fazer commits.

---

# Feature: "Remarcar" — move a task to an earlier or later day from the detail popup

## Verified current state (2026-11-18)

- The task detail popup (spec 021) shows two large buttons for an open task: "Concluir" (with "▾") and "Adiar".
  "Adiar" is offered only when `TaskOccurrence.canPostpone` is true: status OPEN, `DeadlineKind.DAY` and a date.
- "Adiar" opens a bottom sheet (`PostponeSheet` in `features/tasks/TaskDetail.kt`) titled `Adiar “<title>”`, with
  the line "Prazo atual: <data> · <hora> · o horário se mantém" and two options:
  - "Amanhã" (weekday, date and time); it is shown but disabled when the deadline already is tomorrow.
  - "Escolher data" ("Abre o calendário"): `EthosDatePickerDialog(initial = today + 1, min = today,
    excluded = current deadline)`.
  - Notes: recurring task → "Só esta vez. A série continua: próxima em <data>."; overdue task → "O prazo de
    <data> fica registrado como perdido, sem penalidade." (error container).
- Domain: `TaskAction.Postpone(id, to)` in `TaskRules` rejects `to < today` and `to == current date`
  (`TaskProblem.INVALID_DATE`). **It already accepts a date before the current deadline**; only the UI prevents it
  today, and such a move would be recorded as `POSTPONED`.
- `moveDate` is an "only this occurrence" date edit: time and other fields stay, other occurrences keep their
  dates; an overdue occurrence also gets `AuditKind.MISSED_DEADLINE` (no coins/XP change).
- `TaskAction.UndoPostpone(id, to)` puts the occurrence back on the previous date (may be in the past) and is
  recorded as `AuditKind.POSTPONE_UNDONE`. Postpone and undo both keep the previous deadline in
  `originalDeadline` and the new date in `actualDate`. `AuditKind` is persisted by name (TEXT): adding a value at
  the end needs no schema change (Room schema 7).
- Reminders (`ReminderRules.postponeChosen`, `RoomTaskRepository.moveReminders`, same transaction): relative
  reminders follow the deadline; fixed-date ("chosen") reminders move by the same number of days keeping the time
  of day (a recurring occurrence gets its own occurrence-scoped copy); weekday reminders do not change. Undo moves
  them back.
- Statistics (spec 022/024): "Adiadas" in Resumo counts `AuditKind.POSTPONED` records whose `recordedAt` is in the
  period (`StatisticsSummary.kt`). The Histórico tab maps `POSTPONED` → "Adiada de <de> para <para>" and
  `POSTPONE_UNDONE` → "Adiamento desfeito: voltou para <data>" (shown as an adjustment).
- After a postpone the popup closes and a snackbar "Adiada para amanhã" / "Adiada para <dia>" with "Desfazer" and
  "Fechar" appears (`PostponedSnackbar`). Since 2026-11-18 it times out by itself (8 s base, stretched by
  `LocalAccessibilityManager.calculateRecommendedTimeoutMillis`) and uses theme colours
  (`surfaceContainerHighest` / `onSurface`, action in `primary`). It is now the **only** "Desfazer" snackbar in the
  app: the ones for "Concluída pelo cronômetro", "Cronômetro iniciado" and habit "Feito hoje" were removed the same
  day because each duplicated a one-tap way back (uncheck the card, tap the habit again, "Descartar contagem").
- The notification action "Adiar" only snoozes the reminder (spec 007/021) and is unrelated to this feature.
- The user's real routine is stored in the app and must be preserved (no destructive migration, no data reset).

## Goal

Let the user move a task to any day from today on — earlier or later than its current deadline — from the same
place where "Adiar" is today. Example from the user: the task is due Saturday, today is Tuesday, and they want it
on Thursday; today that is impossible without opening the editor.

## Decisions (with reasons)

1. **Name: "Remarcar".** The "Adiar" button and sheet are renamed "Remarcar" (sheet title `Remarcar “<title>”`).
   Reason: the action now goes both ways; the user rejected "Mudar data" as too long. "Remarcar" fits the large
   button next to "Concluir".
2. **Options are dates, not verbs: "Hoje", "Amanhã", "Escolher data"**, in this order, each with weekday/date and
   the kept time as the current "Amanhã" option shows. Reason: whether a choice is a postpone or an advance
   depends on the current deadline (with a Saturday deadline, "Amanhã" on Tuesday is an advance), so labels like
   "Adiar para amanhã" would be wrong.
3. **Hide options that would not change anything** instead of disabling them: deadline today → no "Hoje";
   deadline tomorrow → no "Amanhã". An overdue task shows both (both are later than the old deadline).
   "Escolher data" is always present. Reason: a disabled row is noise; the user asked for the options that make
   sense.
4. **"Escolher data" accepts any day from today on, before or after the deadline**; the current deadline cannot be
   chosen (as today). Days before today stay impossible (`TaskRules` already rejects them). Reason: covers the
   Saturday → Thursday case and keeps the past out (completing on another day already has its own flow).
5. **Direction is derived and recorded separately:**
   - new date **after** the current deadline → `AuditKind.POSTPONED` (unchanged), counts in "Adiadas";
   - new date **before** the current deadline → a **new** `AuditKind` appended at the end of the enum (working
     name `ADVANCED`), with the same `originalDeadline`/`actualDate` fields; it does **not** count in "Adiadas".
   Reason: moving a task earlier is the opposite of procrastinating; counting it as a postpone would make the
   statistic lie.
6. **Overdue tasks keep the spec 021 rule:** remarcar an overdue task records `MISSED_DEADLINE` (no penalty) and
   the sheet keeps the warning. Since every allowed date is ≥ today > old deadline, it is always a postpone.
7. **Time of day is kept; reminders move with the task in both directions:** relative reminders follow the new
   deadline; fixed-date reminders move by the same signed number of days (negative when advancing), keeping the
   time; weekday reminders unchanged. Same transaction as the date change.
8. **Only this occurrence** of a recurring task moves (the existing series note stays). An advance may land on a
   day that already has another occurrence of the same series; that is allowed (as for postpones today).
9. **Keep the "Desfazer" snackbar for remarcar**, auto-dismissing and in theme colours as it is now, with text
   "Remarcada para hoje" / "Remarcada para amanhã" / "Remarcada para <dia>". Reason: remarcando again to fix a
   mistake would leave a wrong postpone in "Adiadas" and possibly a `MISSED_DEADLINE`; undo is the only exact way
   back (it restores the deadline and the reminder moments and records a correcting audit). Undo of an advance
   is recorded with an undo kind distinguishable from the advance itself (see open questions).
10. **No coins or XP** are created by remarcar or its undo (as spec 021 FR-006).
11. **Statistics Histórico** shows advances as their own item (working text "Antecipada de <de> para <para>"), and
    their undo as an adjustment, like postpone undo today. Resumo keeps a single "Adiadas" tile (no new tile).

## Scope

- In: rename and new options in the detail popup sheet; picker accepting earlier dates; new audit kind(s) and
  their mapping in Statistics (summary count unaffected, Histórico item); reminder shift with negative days;
  snackbar texts; strings in `strings.xml`; JVM tests for rules, reminders and statistics mapping.
- Out: shortcuts on cards (long-press, ⋮, swipe), remarcar from notifications (notification "Adiar" stays a
  snooze), week/month deadlines, tasks without date, habits, completion/review/recurrence/reward rules, the task
  editor, an "Antecipadas" statistic, a "postponed N times" counter, any schema version change.

## Acceptance criteria

1. Open task due Saturday, today Tuesday: Remarcar shows "Hoje" (Tuesday), "Amanhã" (Wednesday) and "Escolher
   data"; choosing Thursday in the picker moves only this occurrence to Thursday at the same time, records the new
   advance kind, does not change "Adiadas", and shows "Remarcada para qui., 20 nov · Desfazer" (exact date format
   per spec 013).
2. Task due today: the sheet shows "Amanhã" and "Escolher data" only; choosing tomorrow records `POSTPONED` and
   increments "Adiadas" as today.
3. Task due tomorrow: the sheet shows "Hoje" and "Escolher data" only; "Hoje" records the advance kind.
4. Overdue task: the sheet shows "Hoje", "Amanhã", "Escolher data" and the missed-deadline warning; any choice
   records `MISSED_DEADLINE` + `POSTPONED`, no confirmation, no coins/XP.
5. The picker never allows days before today nor the current deadline; the domain rejects them too.
6. A fixed-date reminder at 09:00 on the deadline day of a task advanced by 2 days is at 09:00 two days earlier;
   if that moment is already past it is not delivered late (same rule as already-fired reminders today). A
   relative "1 h antes" reminder fires 1 h before the new deadline.
7. Desfazer after an advance restores the deadline and reminder moments, adds a correcting audit, and the Histórico
   shows the advance followed by its undo as an adjustment; "Adiadas" is unchanged throughout.
8. The snackbar disappears by itself (8 s base, accessibility-adjusted) and uses theme colours in light and dark.
9. Recurring task: only this occurrence moves; the series note is shown; the next occurrence keeps its date.
10. Existing postpones (`POSTPONED`, `POSTPONE_UNDONE`, older ones stored as `EDITED`) keep being read and shown as
    before; no migration and no data change for the user's existing routine.
11. TalkBack reads each option with its date; touch targets ≥ 48 dp; 200% font does not clip the sheet.

## Open questions (ask the user only these)

1. Final names of the new audit kinds (proposal: `ADVANCED` for the advance and `ADVANCE_UNDONE` for its undo,
   appended at the end of `AuditKind`; or reuse `POSTPONE_UNDONE` for both undos?).
2. Histórico wording for the advance and its undo (proposal: "Antecipada de <de> para <para>" and "Antecipação
   desfeita: voltou para <data>").
3. Initial date selected when the picker opens (proposal: tomorrow, or today when tomorrow is the current
   deadline), and whether the current deadline should be visibly marked in the picker.
4. Should the sheet show a one-line hint under the title when an option would be an advance vs. a postpone, or is
   the date alone enough? (Proposal: date alone; keep "o horário se mantém".)
