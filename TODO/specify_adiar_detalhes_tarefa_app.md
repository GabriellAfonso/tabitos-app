# Specify — Adiar tarefa e novo pop-up de detalhes

Estado: executado (2026-11-16) — virou `specs/021-task-postpone-detail-popup/spec.md`.

Para executar: rodar `/speckit-specify` (Claude) ou `$speckit-specify` (Codex) usando como descrição todo o
corpo abaixo da linha `---`. Salvar este prompt não autoriza especificar, planejar, implementar nem commitar.

Referências:
- `AGENTS.md`, `.specify/memory/constitution.md`
- `specs/003-task-lists/spec.md` (prazos, recorrência, revisão, prazo perdido)
- `specs/007-reminders-notifications/spec.md` (lembretes; "Adiar" da notificação)
- `specs/012-direct-task-event-checks/spec.md`, `specs/013-task-editor-pickers/spec.md` (datas pt-BR, seletores)
- `specs/017-game-mode-toggle/spec.md` (confirmações financeiras)
- `TODO/planejamento_detalhes_tarefa_adiar.md` (decisões e histórico da conversa)
- Protótipos: `prototypes/detalhes-tarefa/index.html` (versão "1 · Mínimo") e `prototypes/adiar-tarefa/index.html`
  (fluxo da folha Adiar). Artifacts: https://claude.ai/artifact/QavvngcAarjE5HEoBihZqL e
  https://claude.ai/artifact/CVVGb3pcFsgr1iQWzZR4qo

Origem: conversa de design no Claude Code em 2026-11-16. Não há ID/transcript acessível para registrar; todas as
decisões estão neste arquivo.

---

# Postpone a task and a cleaner task detail popup

## Verified current state (2026-11-16)

- Tapping a task card (Today, list pages, Review, Calendar) opens `TaskDetail` in
  `app/src/main/java/com/ethos/app/features/tasks/TasksContent.kt`: an `AlertDialog` whose body is one vertical
  column of: title, description, long date label · time, checklist checkboxes, "Cronômetro" (timed tasks only),
  "Lembretes", an always-visible date field "Data em que foi feita", filled button "Concluir tarefa", text buttons
  "Não será feita" and "Editar", list chips for every list (moving the task), "Excluir", and the full
  "Registros e correções" audit list; confirm button "Fechar". Resolved tasks show "Desfazer resultado" instead of
  the open-task actions. The user finds it cluttered: every action has the same visual weight.
- There is no "postpone" action. Changing the day today means: details → Editar → change the date → save.
  `TaskAction.Edit` with `ChangeScope.ONLY_THIS` changes one occurrence. When the occurrence is overdue
  (`needsReview(today)`) and its date changes, `TaskRules.edit` requires `reviewedDeadline = true`
  (otherwise `TaskProblem.REVIEW_REQUIRED`) and appends `AuditKind.MISSED_DEADLINE` (no penalty, original deadline
  kept in history).
- Deadline kinds: `DeadlineKind { NONE, DAY, WEEK, WORK_WEEK }`. Reminders: `ReminderMode { CHOSEN, RELATIVE,
  WEEKDAYS }`. The notification "Adiar" snoozes only the reminder and never changes the task deadline.
- The user has a real routine stored in the app; no data loss or incompatible migration is acceptable.

## Goal

1. Let the user postpone an open task to tomorrow or to a chosen day in two taps from the task detail popup.
2. Redesign the task detail popup so it stays a centered popup but is short, organized and has a clear hierarchy.
3. Remove the audit history from the task detail popup (it will live in a future History tab).

## Decisions (with reasons)

### Popup layout — chosen "1 · Mínimo" (prototype `prototypes/detalhes-tarefa`)
- Stays a centered popup (`AlertDialog`). Bottom sheet and full-screen versions were shown and rejected: the user
  wants the popup they know, only polished.
- Top to bottom:
  1. Title (strikethrough + muted when resolved).
  2. One summary line with icons: deadline (pt-BR short label + time; "· vencida" in the warning tone when
     overdue), list (icon + name), repetition label when recurring, number of reminders when open, and a "▶ <goal>"
     chip for timed tasks that starts the timer (replaces the separate "Cronômetro" button).
  3. Description clamped to 2 lines with "Ver tudo" that expands it in place.
  4. Checklist, compact rows, still toggleable while open (same `TaskAction.Check` rules).
  5. Two large buttons side by side: "Concluir" (filled) with an attached "▾" that opens "Concluí em outro dia…",
     and "Adiar" (tonal). Reason: concluding and postponing are the frequent actions.
  6. One row of five labelled icon buttons: "Não será feita", "Editar", "Lembretes", "Lista", "Excluir".
     Reason: keeps every existing action visible without stacking full-width text buttons.
- No "Fechar" button: the popup closes on outside tap / system back (as `onDismissRequest` already allows).
- The always-visible "Data em que foi feita" field is removed. "Concluir" completes with today's date; "▾ →
  Concluí em outro dia…" opens the existing date picker limited to today and earlier (and not before creation),
  then completes with that date. Same validation as today (`Resolve.actualDate`).
- "Lista" opens a choice of active lists (name + icon, current one marked) and applies `TaskAction.Move`. The
  inline list chips are removed.
- Resolved task (done / not done): the two large buttons are replaced by a status strip ("Feita hoje às 10:12" /
  "Marcada como não será feita") with "Desfazer" (`TaskAction.Undo`); Adiar and "Não será feita" do not appear;
  "Lembretes" is disabled; Editar, Lista and Excluir follow current rules.
- Existing confirmations keep working unchanged: unchecked checklist items, timed task "complete without finishing
  the time", financial confirmation for "Não será feita" when game mode is on, delete dialog with scope for
  recurring tasks, editor opening for Editar.

### History out of the popup
- "Registros e correções" is no longer shown in the task detail popup. Audits keep being recorded exactly as
  today; nothing is deleted. A History tab with statistics is a future, separate feature (not in this spec).
- Strings/composables used only by the removed history list must be removed (no dead code); they can be
  reintroduced by the History feature.

### Postpone ("Adiar")
- Only in the task detail popup. No shortcut on the card (no long-press item, no ⋮ button). Reason: tapping the
  card already opens the popup; one place to postpone.
- Shown only for open tasks with `DeadlineKind.DAY` and a date. Hidden for: no date ("Sem data"), week / work-week
  deadlines (decided: not for now), resolved tasks.
- Tapping "Adiar" opens a bottom sheet over the popup titled "Adiar “<title>”", subtitle with the current deadline
  (and "o horário se mantém" when there is a time), with exactly two options:
  - "Amanhã" — subtitle with weekday and date (e.g. "ter, 17 nov") and the time if any.
  - "Escolher data" — opens the existing date picker (spec 013 style, no typing); days before today and the current
    deadline cannot be chosen.
  "Próxima segunda" was offered and rejected.
- Effect: same as editing only this occurrence's date (`ChangeScope.ONLY_THIS`): only the day changes, time stays,
  recurring series keep their own future dates (sheet note: "Só esta vez. A série continua: próxima em <data>.").
  Coins/XP never change on postpone.
- Overdue task: postponing counts as the review — no extra confirmation dialog (decided). The sheet shows a note
  "O prazo de <data> fica registrado como perdido, sem penalidade." and the existing `MISSED_DEADLINE` record is
  written. The task leaves "Revisar pendências" and appears on the new date.
- After postponing the popup closes and a snackbar shows "Adiada para amanhã" / "Adiada para <ter, 17 nov>" with
  "Desfazer". The postpone result is already persisted; the snackbar is only an undo affordance (follow the AGENTS
  rule about transient effects: state with consumption confirmation, nothing critical lost without a collector).
- The notification "Adiar" stays as is (snoozes the reminder only).
- No schema change expected; if planning finds one is needed, it requires a preserving migration.

## Out of scope
- History tab, statistics, "postponed N times" counters.
- Postpone shortcuts on cards, swipe gestures, postponing from notifications.
- Postponing week/month deadlines, tasks without date, habits.
- Changes to completion, review, recurrence, reward or penalty rules.
- Changes to the task editor.

## Acceptance criteria
1. An open task due today with a time: details → Adiar → Amanhã moves only this occurrence to tomorrow with the
   same time; it leaves Today, shows on tomorrow in Calendar, snackbar offers Desfazer.
2. Escolher data cannot select past days nor the current deadline; confirming moves the task to that day.
3. Overdue task: details → Adiar → any option moves it without extra dialog, records the missed deadline without
   penalty and removes it from Review.
4. Recurring task: postponing one occurrence does not change the next occurrences' dates.
5. Desfazer right after postponing restores the previous deadline and reminder moments; history stays append-only.
6. Adiar is absent for tasks without date, week/work-week deadlines and resolved tasks.
7. The popup follows the "Mínimo" layout; no "Fechar" button; closes on outside tap/back; no history list.
8. "Concluir" completes with today; "▾ → Concluí em outro dia…" completes with a past date chosen in the picker;
   all existing confirmations still appear when applicable.
9. "Lista" moves the task to the chosen list; "Excluir" opens the existing delete dialog.
10. Accessibility: every icon button has a visible label and content description; usable with TalkBack; layout
    works at 200% font scale (the five-icon row may wrap labels but must not clip); light and dark themes.
11. JVM tests cover the postpone rule (allowed kinds, date bounds, overdue review, recurrence isolation, undo).

## Resolved after review (2026-11-16)
1. Undo after postponing an overdue task keeps the audit log append-only: the MISSED_DEADLINE record stays and the
   undo appends a correcting record restoring the original date. Nothing is erased from history.
2. Reminders on a postponed task:
   - `RELATIVE` reminders follow the new deadline (already natural).
   - `CHOSEN` (fixed date/time) reminders are postponed too: same time of day, date moved by the same number of days
     as the deadline. A reminder set on the deadline day therefore lands on the day the task was postponed to
     (e.g. Mon 09:00 for a Monday task postponed to Wednesday becomes Wed 09:00). Undo moves them back.
     A moved reminder whose new moment is in the future is scheduled again even if it had already been delivered.
   - `WEEKDAYS` reminders are not tied to a date and stay unchanged.
   - Acceptance: a task with a CHOSEN reminder on its due day, postponed by N days, has that reminder at the same
     time N days later; RELATIVE reminders keep their offset to the new deadline.
