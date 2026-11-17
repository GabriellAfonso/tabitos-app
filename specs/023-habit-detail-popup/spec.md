# Feature Specification: Habit detail popup with the week strip

**Feature Branch**: `023-habit-detail-popup` (no branch created; work stays on the current branch)

**Created**: 2026-11-17

**Status**: Implemented (2026-11-17)

**Input**: Design conversation on 2026-11-17 with prototype `prototypes/detalhes-habito/index.html`; the user chose
option "B · Com a semana" and asked for a short spec followed by implementation (no plan/tasks documents).

## Current state (verified 2026-11-17)

- Tapping a habit card (Hoje, list pages, Calendário, Settings › Hábitos excluídos) opens `HabitDialogs` in
  `features/habits/HabitsScreen.kt`: one long `AlertDialog` column with the description, "Sequência" twice, the
  current period, a month calendar with stacked "Mês anterior"/"Próximo mês" buttons, a typed "Data real
  (AAAA-MM-DD)" field with "Hoje"/"Fiz ontem", "Registrar ocorrência/prática", every record with "Corrigir
  registro"/"Excluir registro incorreto", "Progresso por período" for every period, nine text actions of the same
  weight and "Fechar".
- Good and bad habits share that popup. The task detail popup was already redone (spec 021, "Mínimo").
- The user's real routine is stored in the app and must be preserved; this feature changes no data or schema.

## Decisions

1. The habit popup follows the task popup skeleton (spec 021): habit icon tile and name, a summary line, the
   description clamped to 2 lines with "Ver tudo", the main action, a row of labelled icons, no "Fechar" (closes
   on outside tap or back).
2. Summary line: frequency ("Todo dia", "Dias escolhidos", "3× por semana", "Evitar", "Até 2 por dia" …) and the
   list (inbox or list name, list color). A stats line shows "Sequência: N · recorde M · X% de constância"
   (rate omitted while there is none), using the existing `HabitRules.streak` and `HabitRules.successRate`.
3. Week strip: the last 7 days ending today. Each day shows its weekday ("Hoje" for today), the day of month and a
   mark that never relies on color alone:
   - good habit: ✓ done, ✕ missed (closed daily period without a record), empty otherwise;
   - bad habit: the number of records that day; red fill when that day is over its limit (daily rule),
     ✓ when it ended within the limit.
   A day is tappable only while the rules accept records on it: not before creation, active (not paused or
   excluded), on a scheduled weekday for daily practice, and its period not yet closed
   (`HabitRules.closed`, 2 late days). Closed days are dimmed and disabled.
4. Tapping an open day of a good habit marks or unmarks it (`HabitAction.SetPractice`). Tapping an open day of a
   bad habit opens a bottom sheet with − and + that removes the latest record of that day or adds one
   (`HabitAction.Correct(delete)` / `HabitAction.Record`), keeping the existing financial confirmation.
5. Progress meter only where it adds information: weekly/monthly practice ("Esta semana: 2 de 3 dias") and limit
   habits ("Hoje: 1 de 2 permitidos", or per week/month), with a segmented bar.
6. Main action:
   - good habit, today open and not done: "Marcar hoje"; with a duration goal also "▶ N min" that opens the timer;
   - good habit done today: strip "✓ Feito hoje" with "Desmarcar";
   - bad habit: red "+ Registrar recaída" (avoid) or "+ Registrar uso" (limit); when today has records, a red strip
     "Hoje: N recaídas/usos" with "Desfazer 1"; avoid without records shows "Hoje: sem recaídas";
   - paused: strip "Pausado. Dias pausados não contam como perdidos." with "Retomar";
   - excluded: strip "Excluído do acompanhamento." with "Restaurar hábito" and "Apagar por completo";
   - today not available (not scheduled today or not active): no main button; the strip still works.
7. Marking a timed habit without the timer asks "Concluir sem completar o tempo?" for any day (spec 016), not only
   from the Hoje card.
8. Icon row (not for excluded habits): Editar, Lembretes, Acompanhar (tracker hub), Pausar/Retomar, Excluir. Pause,
   resume, exclude and restore keep the existing lifecycle confirmation dialog.
9. Removed from the popup: the typed date field, "Hoje"/"Fiz ontem", "Registrar ocorrência/prática", "Corrigir
   registro" (unmark the wrong day and mark the right one), per-record buttons, "Progresso por período", the month
   calendar and old-unit records. History stays available in Estatísticas › Histórico (spec 022) and the month
   view in Calendário (spec 009).
10. The icon-with-label action, summary item and expandable description used by the task popup move to
    `core/ui/components` and are shared by both popups.

## Out of scope

- A link from the popup to Estatísticas filtered by habit (Statistics has no per-habit filter yet).
- Reminder count in the summary line (habits do not load reminders today).
- Any change to habit rules, coins, XP, schema or the Hoje card.

## Acceptance criteria

- AC1: a daily good habit shows the last 7 days; tapping yesterday (open) marks it and tapping it again unmarks it;
  a day 3+ days ago is disabled.
- AC2: a weekly good habit can mark any day of the current week and, through Tuesday (2 late days after Sunday), of the previous week; capacity
  rules still apply.
- AC3: a bad habit shows the number of records per day; + and − in the day sheet add/remove one record of that
  day with financial confirmation when game mode is on.
- AC4: a timed good habit asks for confirmation before marking any day by hand.
- AC5: paused and excluded habits show their strip and no tappable days.
- AC6: TalkBack reads each day as "<date>, feito/perdido/em aberto/sem recaída/N registros/passou do limite".
- AC7: no change to stored data; existing rules tests keep passing.
