# Feature Specification: Statistics filtered by item and by bad habits

**Feature Branch**: `024-statistics-item-filter` (no branch created; work stays on the current branch)

**Created**: 2026-11-17

**Status**: Implemented (2026-11-17)

**Input**: Pending item of spec 023 ("Ver histórico" from the habit popup). Decisions taken in conversation on
2026-11-17; the user asked for a short spec followed by implementation (no plan/tasks documents).

## Current state (verified 2026-11-17)

- Estatísticas (spec 022) has a shared period and two tabs. Only the Histórico tab has filters: Tudo, Tarefas,
  Hábitos (good and bad together), Tempo and, in game mode, Moedas. The Resumo tab always shows every card.
- The filter lives in the activity-scoped ViewModel and is kept while the app runs; the tab resets to Resumo.
- There is no way to look at one task or one habit, and no link from the task/habit popups to Estatísticas.

## Decisions

1. Type filters become Tudo, Tarefas, Hábitos, Maus hábitos, Tempo and Moedas (game mode only). "Hábitos" now
   means good habits only; "Maus hábitos" are avoid/limit habits (current rule, or the latest one for habits not
   active today).
2. One item (a habit or a task series) is focused only from the item itself, never from a list, because a list of
   every task would grow without limit (revised 2026-11-17 after trying a "Qual: Todos ▾" picker on the phone):
   - "Ver histórico ›" in the task/habit popup;
   - tapping a habit row in the Resumo habits card;
   - "Ver histórico" in the dialog of a deleted item's history entry (deleted tasks no longer open a popup).
   Items are identified by id, never by name: a habit by `habitId`, a task by its series id (`taskId`), which
   groups every occurrence of a repeating task. The focused item shows as a removable chip above the tabs
   ("Ler 20 páginas ✕", "(excluído)" when excluded/deleted); ✕ returns to its whole type. An item whose history was
   erased has nothing left to show and is not focusable.
3. The filter is shown above the tabs and applies to both:
   - Resumo shows only the cards of the chosen type: Tarefas → tasks and their time; Hábitos/Maus hábitos → the
     habits card with those habits and their time; Tempo → time; Moedas → coins; Tudo → everything.
   - With an item chosen, those cards count only that item (task series or habit). Coins are never split per item.
   - Histórico with an item chosen shows every entry of that item (results, timer time, coins), not only the kind
     of the type filter.
4. "Ver histórico ›" in the habit popup (spec 023) and in the task popup (spec 021) closes the popup and opens
   Estatísticas on the Histórico tab, already filtered by that habit or task series.
5. Filter, item and tab live only in memory: leaving Estatísticas resets them to Tudo and Resumo. Changing the
   type clears the item.

## Out of scope

- Per-item coin totals, per-list filters, an item list or search.
- Any change to rules, stored data or schema.

## Acceptance criteria

- AC1: choosing Maus hábitos lists only avoid/limit habits and the Histórico shows only their occurrences and
  settled days; Hábitos no longer shows them.
- AC2: choosing a repeating task series shows every occurrence's results in Histórico and counts all of them in the
  Resumo tasks card; a different series with the same name is not mixed in.
- AC3: choosing a habit shows only that habit in the Resumo habits card and its timer time in the time card.
- AC4: "Ver histórico" from a popup opens Histórico filtered by that item; leaving the page and coming back shows
  Tudo on Resumo.
- AC5: with game mode off, Moedas is not offered and no coin values appear (spec 017 unchanged).
- AC6: there is no item list; the chip ✕ clears the item and keeps its type.
