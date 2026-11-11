# Specify — Modo jogo (ligar/desligar moedas, XP e recompensas)

**Estado:** pronto para executar specify em outro chat.
**Data:** 2026-11-11.

## Como usar

Na raiz `C:/Users/gabri/Projetos/ethos`, execute `$speckit-specify` (ou `/speckit-specify` no Claude Code)
usando o corpo em inglês após `---`. Execute somente specify. Plan, tasks, analyze, implement, commits e
publicação dependem de solicitação separada. Este arquivo é um prompt de contexto, não uma spec já executada.

## Referências e origem

- `AGENTS.md` e `.specify/memory/constitution.md` v1.0.0.
- `specs/005-coins-xp-levels/`: carteira, XP, níveis, preços por categoria e ledger.
- `specs/006-personal-rewards/`: recompensas pessoais e compras.
- `specs/008-timers-duration-goals/`: compra temporizada (consentimento, débito, recibo e sessão).
- `specs/011-habit-toggle-demo/`, `specs/012-direct-task-event-checks/`, `specs/014-habits-without-debt/`:
  confirmações financeiras, estornos, Desfazer e base fictícia de debug.
- `specs/016-timed-tasks-habits/`: tarefas/hábitos com cronômetro (independentes da carteira).
- Origem: conversa de planejamento no Claude Code em 2026-11-11. ID, link e caminho de transcript
  indisponíveis neste contexto; todas as decisões estão reunidas abaixo.
- Sem contraparte de backend.

---

Create the feature specification for Ethos feature 017: a user-controlled **Game mode** ("Modo jogo")
that turns the coin/XP/level/reward layer on or off. Execute only the Spec Kit specify stage. Do not
generate plan/tasks, implement code, commit, publish, or automatically run subsequent phases.

Before asking anything, read this prompt, `AGENTS.md`, the constitution and the referenced specs. Ask
only about points not decided below.

## Motivation

Not every user wants the gamified layer (coins, XP, levels, personal rewards). Ethos must work as a
plain task/habit/routine app when the user prefers, and the user must be able to switch easily between
both modes at any time.

## Verified current state (2026-11-11)

- Accounting lives in `core/accounting` (ledger, prices per difficulty category, entitlements reconciled
  from task/habit outcomes via `AccountingStore.reconcile` inside the task and habit Room repositories).
  Rewards live in `core/rewards`. The balance has a floor of -300.
- `AccountingStore.initialize()` already registers pre-existing outcomes as exempt/unpriced entitlements
  (`exempt = true, priced = false`), so the model already supports "outcome exists but never pays".
- Coin/XP touch points outside the accounting/rewards features:
  - Shell destinations `ShellDestination.REWARDS` and `ShellDestination.HISTORY` (drawer);
  - `TodayHeader` in Hoje (balance and shortcut to Rewards);
  - Settings: `AccountingSettings` (price editing);
  - Task and habit editors: difficulty selector and `FinancialEstimate`;
  - `FinancialConfirmation` in `TasksContent` / `HabitsScreen` (unchecking something that paid, and
    avoid/limit occurrences with financial consequence);
  - Timed reward purchases (spec 008) and their timer sessions;
  - Debug demo seed (`core/demo`).
- Trackers explicitly never touch accounting (`TrackerRules`). Reminders, calendar, timers for
  tasks/habits (spec 016), streaks and consistency percentage do not depend on accounting.
- Appearance preferences are stored in DataStore (`core/preferences`) and applied without restarting
  the Activity.

## Decisions

1. **Default: Game mode ON.** New installs and existing users keep today's behavior. A first-launch
   question choosing the mode is explicitly out of scope (future feature).
2. **Toggle location:** a "Modo jogo" switch at the top of Settings. Switching applies immediately,
   without restarting the Activity. Turning it off shows a short confirmation dialog explaining that
   coins, XP and rewards will be hidden and nothing will be deleted. Turning it on needs no dialog
   beyond normal feedback.
   - Reason: easy, discoverable, reversible.
3. **Storage:** the mode is a user preference stored in DataStore (like the theme). Repositories that
   record outcomes must read the current mode at write time so the rule is enforced in the data layer,
   not only in the UI.
4. **Ledger while OFF: stop scoring (option A).** Outcomes completed while the mode is off never earn
   coins/XP, and are never paid retroactively when the mode is turned back on. The balance resumes
   exactly where it stopped.
   - Reason: simple to explain ("off doesn't count"); avoids hidden debt or a surprise windfall.
   - Because accounting is derived from outcomes via reconcile, outcomes produced while OFF must be
     marked as unpriced/exempt (the existing exempt mechanism is a candidate; plan decides).
   - Avoid/limit habit events registered while OFF never charge penalties.
5. **Reversals across modes:** if an outcome paid while ON is undone while OFF (uncheck, Undo,
   correction), the reversal still happens silently so the ledger stays consistent. No confirmation
   dialog is shown for it while OFF.
6. **Data is preserved:** turning the mode off deletes nothing — balance, XP, levels, rewards, purchase
   history and prices stay stored and hidden. Turning it on shows everything again.
7. **What hides while OFF:**
   - Rewards and History destinations disappear from navigation; deep links/notifications must not land
     on them (fall back to Hoje).
   - Balance/shortcut in the Hoje header disappears.
   - Price settings (`AccountingSettings`) disappear from Settings.
   - Difficulty selector and coin/XP estimate disappear from task and habit editors. New items get the
     default difficulty; existing items keep their stored difficulty untouched.
   - Financial confirmation dialogs are not shown; the actions apply directly and Undo keeps working
     as today.
   - Timed reward purchases are unavailable (they only exist with rewards).
8. **Active reward purchase timer:** switching the mode is blocked while a reward purchase timer
   session is counting (same pattern as the debug demo restore), with an explanation.
9. **Breaking habits without coins:** streak and consistency percentage are enough; no replacement
   metric.
10. **Debug demo restore:** must work in both modes and must not change the mode preference.

## Out of scope

- First-launch onboarding question for the mode.
- Deleting or resetting wallet/rewards data when turning off.
- Any new gamification, replacement metrics or per-feature partial toggles (e.g. XP without coins).
- Backend, login, sync.

## Acceptance criteria (draft for spec)

- Fresh install: Game mode ON, behavior identical to current app.
- Turning OFF in Settings: after confirmation, Rewards/History, balance in Hoje, price settings,
  difficulty/estimates and financial confirmations disappear immediately without restart.
- With mode OFF, completing tasks/habits and registering avoid/limit events create no ledger entries;
  turning ON later shows the same balance/XP as before turning OFF.
- An outcome paid while ON and undone while OFF is reversed in the ledger.
- Turning OFF then ON preserves rewards, purchases, history and prices.
- Switching is blocked while a reward purchase timer is counting.
- Mode survives process death and app restart; theme preferences are unaffected.
- Tasks, habits, calendar, trackers, reminders and task/habit timers behave the same in both modes.
- Tests: JVM tests for the scoring rule while OFF, reversal across modes and no retroactive payment;
  persistence of the preference.

## Open questions

- None decided as open. The executor may ask only about gaps found when reading the code/specs.
