# Specify — Ponto 5: moedas, XP e níveis

**Estado:** pronto para executar specify em outro chat.
**Data:** 2026-11-06.

## Como usar

Na raiz `C:/Users/gabri/Projetos/ethos`, execute `$speckit-specify` usando o corpo em inglês
após `---`. Execute somente specify. Plan, tasks, analyze, implement, commits e publicação
exigem solicitação separada. Este arquivo é um prompt, não uma spec já executada.

## Referências e origem

- `AGENTS.md` e `.specify/memory/constitution.md` v1.0.0.
- `specs/001-android-foundation/`, `specs/002-app-shell/`, `specs/003-task-lists/`
  e `specs/004-habits-goals/`: specs, planos e evidências atuais.
- `TODO/planejamento_ponto_5.md`: tabelas e decisões desta rodada.
- Planejamentos dos pontos 2, 3 e 4: revisão, correções, histórico e limites de integração.
- `TODO/planejamento_lembretes_tempo.md`: funcionalidades futuras, fora deste escopo.
- `prototypes/hoje/index.html`: referência visual B; seus números ilustrativos não são as tabelas finais.
- Origem: conversa de planejamento do Ethos em 2026-11-06. ID, link e caminho de transcript
  indisponíveis neste contexto; as decisões necessárias estão reunidas abaixo.
- Sem contraparte de backend.

---

Create the feature specification for Ethos point 5: local coins, XP, levels, configurable earning
and penalty tables, and transaction history integrated with existing tasks and habits. Execute
only the Spec Kit specify stage. Do not generate plan/tasks, implement, commit, publish, or
advance automatically. The user designs here and gives saved prompts to another chat.

## Verify current state before specifying changes

Work in C:/Users/gabri/Projetos/ethos. Read AGENTS.md, constitution v1.0.0, the first four feature
specifications/plans/verification, and relevant task/habit persistence and domain code. Respect
clarifications already encoded in those specs instead of reopening accepted behavior.

At handoff, Android foundation, visual shell, tasks/lists, and habits/goals are implemented.
The app uses Kotlin, AGP 9 built-in Kotlin, Compose Material 3, Navigation Compose, Hilt/KSP,
DataStore appearance settings, and minimum SDK 24. There are task occurrences and correction
audits; habits have records, versioned rules, active intervals, period overrides, review outcomes,
audits, and streaks. Inspect these actual structures for integration rather than replacing them.

TaskDatabase currently has Room schema version 2 with task/list and habit entities, exported
schemas 1/2 and an existing migration. Inspect the current source for changes by other chats.
There is no implemented wallet, monetary ledger, XP progression, reward redemption, reminders,
or timer engine. Current balance/level placeholders are not actual user currency.

The managed context reports API 36 checks, but minimum API 24, a physical device and TalkBack
audio validation remain unverified; use actual verification records, not assumed coverage.
Initial distribution remains personal APK use. All accounting must work locally/offline without
accounts, network services, new permissions, real money, purchasing, or monetization.

## Purpose and responsibilities

Make approved task/habit outcomes produce real persistent coins/XP and levels. Present live
balance and progress in the existing Hoje tiles, useful details, an honest transaction history,
and editable tables in settings. Preserve existing navigation and model B's compact visual style.

Coins measure spendable personal rewards; XP measures accumulated progress and determines level.
They are separate quantities. Do not introduce health, lives, or energy: the user rejected energy.
Spending coins must never be treated as losing XP or failing a habit.

Use Room as the source of truth for accounting and durable table history where needed, with
non-destructive migration and exported schema. Keep domain math independent of Android, preserve
existing tasks/habits/settings, and avoid unrelated architectural rewrites. Exact schema,
transaction boundaries, consistency strategy and library choices belong to the later plan.

## Approved starting values and separation of difficulty/importance

Start the wallet at 0 coins and progress at 0 accumulated XP, level 1. Do not grant a signup
bonus or import sample balance/level from the prototype. No manual mint-coins button.

Difficulty ALONE determines rewards on valid fulfillment:

| Difficulty | Coins gained | XP gained |
| --- | ---: | ---: |
| Fácil | 5 | 10 |
| Média | 10 | 20 |
| Difícil | 20 | 40 |

Importance ALONE determines failure penalty:

| Importance | Coins lost | XP lost for failure |
| --- | ---: | ---: |
| Baixa | 5 | 0 |
| Média | 10 | 0 |
| Alta | 20 | 0 |

Do not combine difficulty and importance into both calculations or make hard tasks lose less
merely because they are hard. A hard/low-importance task has a high reward and low penalty;
an easy/high-importance task has a low reward and high penalty.

Existing defaults remain Média/Média. Difícil suggests Alta unless importance was already manually
chosen. Preserve the current implemented manual-override behavior, not a new financial heuristic.

## Exactly when rewards and penalties occur

### Tasks

- Completing a main task/occurrence awards its difficulty-based coins and XP once.
- Checklist items have no separate reward. Checking all items is not an automatic task completion.
- Confirming Não será feita creates the importance-based coin penalty for that occurrence only.
- Passing a date/time, opening the app, showing an overdue entry, or entering review does not
  itself authorize a penalty. Keep existing explicit review semantics.
- Recurring work is accounted per occurrence identity. Resolve/undo/re-resolve must not duplicate
  gains or penalties; one occurrence can have a corrected history without multiple active awards.
- Existing overdue-rescheduling review must not bypass or silently duplicate its recorded outcome.

### Practice habits

- Reward each valid distinct practice DAY as it is recorded, not only when a weekly/monthly
  target is reached. Several records on one date cannot multiply days or rewards.
- Once the practice target is met, reject further practice records for that period; keep it
  in Metas cumpridas. No extra-day reward and no separate target-closing bonus.
- Corrections/deletions remain available. If correction makes the target incomplete, allow
  valid practice entries again without double payment.
- If a practice period is incomplete and its failure is explicitly confirmed, keep rewards
  from the days actually fulfilled, and charge ONE importance-based penalty for the failed
  period/obligation. Do not charge once per missing day of a flexible weekly/monthly target.
- Respect the daily/specified-weekday obligations and period identity in the existing habit spec.

### Avoid and limit habits

- Each confirmed prohibited event of an avoid habit is individually penalized by importance.
- For limit habits, occurrences within the maximum are not penalized. Each excess occurrence
  is individually penalized, even when several happen on the same day.
- Continue accepting records beyond the maximum: do not apply the practice-cap rule to negative
  or limit events. Respect existing daily/weekly/monthly limits and approved avoid review period.
- On closing a period, after confirming that its event records are complete, award ONE set of
  difficulty-based coins/XP if the user stayed within the goal.
- If the goal was exceeded, no successful-period closing reward. Keep event-level penalties;
  do not add another failure penalty merely for closing the already-exceeded period.
- Zero recorded negative events is not automatic success: period reward requires the existing
  completeness confirmation. Reopening/reconfirming review must not award the same period twice.

### Paused/excluded/deleted work

- Do not penalize inactive intervals or award unearned fulfillment for them.
- Deleting a task/list/habit is not a failure or an automatic reversal of past financial history.
- Excluding/restoring habits keeps previous records and does not produce duplicate awards or
  retroactive obligations for the excluded interval. Preserve existing task/habit history rules.

## Coin floor and no hidden debt

Coins may be negative due to penalties/corrections, but the balance must never be less than −300.
The floor prevents long absences or several failures from creating unrecoverable debt.
Keep confirmed incidents/history even when their financial impact is zero at the floor.

Record both the reason/nominal calculation and actual applied change where needed to explain
the floor. Example: balance −295 and nominal penalty 20 → actual deduction 5, final balance −300.
Do not store an extra 15 as deferred debt to collect from future gains. Undoing that penalty
returns 5, not 20. Every displayed resulting balance must match the effective transaction.

Reward redemptions, when the later reward feature exists, require enough coins to cover full
cost and cannot create debt. This rule does not authorize reward CRUD/redemption in this feature.

## Corrections and idempotency

- Undoing completion reopens the relevant task/record and reverses its ORIGINAL awarded coins/XP,
  not today's edited table. Removing erroneous XP may reduce the level: that is a correction,
  not a failure penalty. Ordinary failures never remove XP or levels.
- If earned coins were already spent, undoing the award may create debt, capped at −300.
  If the full reversal would go below −300, clamp at the floor with no hidden debt.
- Undoing confirmed failure returns exactly its originally EFFECTIVE deduction after floor
  clipping, not a nominal amount that was never actually charged.
- Habit record corrections/deletions recompute valid days, limit classifications, success/failure
  and review states. Adjust the linked accounting consistently without double gains, duplicated
  event penalties, double period penalties, or repeated refunds.
- Preserve original financial entries and link corrections/reversals instead of rewriting
  historical amounts in place. Distinguish correction time from actual practice/completion date.
- Repeated taps, screen recreation, relaunch, retry after a failed write, refresh, recurring
  generation, and repeat review must not apply the same outcome twice.
- Domain outcome changes and financial effects must remain consistent across failure/process death.
  The later plan must define appropriate durable/atomic consistency, not rely only on disabling a button.
- No retroactive currency change from merely editing a title, moving a list, changing appearance,
  browsing history, or recalculating a streak.

There are genuine consistency details not explicitly settled: e.g. undoing an award whose own
coin reversal was floor-clipped and then completing it again, reclassification of limit events
after a retroactive correction, or table changes between correction and re-resolution. Identify
these for clarify/plan rather than inventing an exploitable policy or claiming user approval.

## Approved XP/level progression: linear, no multiplier

Final user decision returned to the initial LINEAR proposal. Discard the intervening proposed
1.10 multiplier, multiples-of-50 rounding, and doubling per level.

- Start at level 1 with 0 accumulated XP.
- Level 1 → 2 requires 100 ADDITIONAL XP.
- Each next transition costs 50 more than the previous: 100, 150, 200, 250, ... .
- From level n to n+1: additional cost = 100 + 50 × (n−1).
- Threshold to reach level n from level 1: 25 × (n−1) × (n+2) accumulated XP.
- Therefore level 2 starts at 100 total XP, level 3 at 250, level 4 at 450.
- Level 99 → 100 costs 5,000 additional XP; reaching level 100 takes 252,450 total XP.
- The level-100 example is not a maximum-level decision. Do not add a level cap silently.

Retain accumulated XP; leveling up does not consume it like coins. Show current level and
progress toward the next threshold, distinguishing total XP from XP within the current level.
Handle multiple levels from one award and exact-threshold boundaries. Correction recomputes
the level from corrected XP, including crossing down a level. The plan must choose numeric
types/limits safely without making overflow or rounding alter the accepted formula.

## Editable tables and historical values

Allow editing coin gains, XP gains, and coin penalties for the existing difficulty/importance
categories in settings, with the approved table as initial values. Values apply prospectively
to new conclusions/occurrences, not by rewriting previous financial records.

Persist the values used by each financial operation. Old records and reversals retain their
original amounts even if the table later changes. Do not recompute all historical progress or
coins from the current table. A retroactively entered event must have an explicitly defined
pricing policy if entry date and actual event date straddle table changes; this has not been
individually decided and must not be presented as an approved policy.

Show earned coins/XP and possible coin penalty in task/habit creation/editing and relevant
confirmations. Provide access to the explanation/table there and in settings. Distinguish an
estimate under current configuration from a committed historical amount. Explain floor clipping
when it affects a penalty/correction. Do not hide the consequence until after confirmation.

Table-input bounds, whether zero is allowed, and any reset-defaults control need a concrete
validation design in clarify/plan; do not invent advanced custom formula editing, per-item price
overrides, a configurable debt floor, or a configurable level curve as accepted requirements.

## Wallet, history, and existing UI integration

- Start at zero; no manual minting or arbitrary balance-edit controls.
- Replace Hoje's unavailable/sample tiles with real coins, level, and XP progress. Keep approved
  routes/positions and compact appearance, accessible text and dark/light/system themes.
- Histórico de moedas lists gains, penalties, redemptions when that future feature produces
  them, and reversals, showing reason, date, actual applied delta and resulting balance.
- Include useful provenance for source task/habit/period and original linked operations even
  when the originating item/list is later excluded/deleted. Never display fabricated history.
- Explain coin/XP distinction, debt floor and level progression. Make corrections understandable
  without exposing implementation IDs as product UI.
- Keep navigation to Recompensas honest; it is not permission to create a fake redeemable shop
  or build the whole rewards feature. No synthetic debit just to populate a history screenshot.

## Existing records and activation

The user may already have tasks/habit history from earlier features. Zero initial coins/XP is
approved, but automatic back-payment or back-charging of that pre-existing history has NOT been
approved. Inspect available audit provenance and identify the activation/migration policy as
needed during clarification/planning. Do not silently sweep all past completions/failures into
a new wallet, discard task/habit history, or grant sample currency. Keep migration preservative
and accounting retries idempotent whichever explicit policy is chosen.

## Scope exclusions

- Reward creation/editing/redemption, reward cancellation, timed rewards or an actual spending UI
  belong to point 6; preserve future integration constraints only.
- Reminder/notification/alarm services and permissions, timers, duration goals and weekly time totals.
- Energy/health, streak-based currency bonuses, achievements, login streak prizes, XP multipliers,
  purchase of coins, manual currency grants, real money, accounts, backend or synchronization.
- Redesign of already-approved task/habit behavior or navigation unrelated to accounting integration.
- New balancing amounts, automatic table inflation with level, or per-item monetary overrides.

## Acceptance and verification expectations

Write independently testable scenarios covering:
1. Zero starting coins/XP and level 1, offline persistence, activation/migration preserving existing
   tasks, habits, historical records and appearance preferences.
2. Exact default reward/penalty tables, independent roles of difficulty/importance, form previews,
   and preserved manual importance choices.
3. One award per task occurrence, no checklist awards, reviewed failure penalties only, and no
   double payments across undo/re-complete, refresh/relaunch and recurring occurrences.
4. Practice daily rewards up to the target, same-day duplicate prevention, no closing bonus/extra
   records at full target, retained successful-day earnings plus one confirmed incomplete-period penalty.
5. Avoid/limit per-event penalties, no penalty within allowed maximum, successful-period reward
   only after completeness confirmation, and no duplicated failure charge at period close.
6. Floor clipping including −295 → −300, zero-impact incident at the floor, effective-value refund,
   reversal of spent earnings, and no deferred invisible debt.
7. Original-value reversals after table edits; corrected habit period outcomes; accounting and
   domain consistency under retry/failure, with no double refunds or phantom XP.
8. Exact level thresholds, progress between levels, multiple-level gains, correction-related level
   reduction, no XP loss for ordinary failure, and 99 → 100 math matching the approved examples.
9. Settings edits persist and affect only prospective operations per the clarified timing policy;
   historical entries retain original prices; invalid values fail safely without corrupting balances.
10. History shows reasons/dates/effective values/resulting balance, links corrections and survives
    source deletion; no fabricated transactions or manual mint-coins controls.
11. Room migration and database consistency tests, deterministic domain math tests, relevant
    task/habit/UI integration regression checks, accessibility and existing appearance behavior.
12. Relevant build, lint, tests and runtime checks when implementation is later authorized, with
    evidence and coverage limitations reported honestly instead of assumed success.

## Clarification discipline

Do not ask the user to repeat the approved tables, floor, level curve, per-day practice reward,
confirmed avoid/limit closing reward, or original-value reversal decisions. Reuse existing task/
habit clarifications and inspect the code first. Identify only genuine unresolved interactions,
such as activation of old history, table timing for retroactive entries, and correction chains
at a clipped floor. State those as unresolved details, not reasons to add unrelated product scope.
This prompt authorizes specification only; subsequent phases need a separate request.
