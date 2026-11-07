# Specify — Ponto 6: recompensas pessoais

**Estado:** pronto para executar specify em outro chat.
**Data:** 2026-11-07.

## Como usar

Na raiz `C:/Users/gabri/Projetos/ethos`, execute `$speckit-specify` usando o corpo em inglês
após `---`. Execute somente specify. Plan, tasks, analyze, implement, commits e publicação
dependem de solicitação separada. Este arquivo é um prompt de contexto, não uma spec já executada.

## Referências e origem

- `AGENTS.md` e `.specify/memory/constitution.md` v1.0.0.
- `specs/005-coins-xp-levels/`: spec, plan, contratos, modelo e verificação da carteira existente.
- `specs/001-android-foundation/` a `specs/004-habits-goals/`: comportamento e histórico anteriores.
- `TODO/planejamento_ponto_6.md`: seis decisões iniciais e quatro detalhes finais aprovados.
- `TODO/planejamento_lembretes_tempo.md`: funcionalidades futuras, fora deste escopo.
- `prototypes/hoje/index.html`: referência visual B aprovada, sem importar seus preços fictícios.
- Origem: conversa de planejamento do Ethos, iniciada em 2026-11-06 e continuada em 2026-11-07.
  ID, link e caminho de transcript indisponíveis neste contexto; as decisões estão reunidas abaixo.
- Sem contraparte de backend.

---

Create the feature specification for Ethos point 6: personal rewards and immediate-use purchases
using the existing local coin wallet. Execute only the Spec Kit specify stage. Do not generate
plan/tasks, implement code, commit, publish, or automatically run subsequent phases.

## Inspect and preserve current implementation

Work in C:/Users/gabri/Projetos/ethos. Read AGENTS.md, constitution v1.0.0, the existing feature
specifications and relevant code before specifying changes. The user discusses product behavior
here and gives saved prompts to another chat. This prompt contains the decisions already closed;
do not present those choices again as unanswered clarification questions.

Foundation, shell, tasks/lists, habits/goals, and coins/XP/levels have been implemented. Inspect
actual code in case another chat has changed it. The existing app uses Kotlin, AGP 9 built-in
Kotlin, Compose Material 3, Navigation Compose, Hilt/KSP, minimum SDK 24, and offline Room storage.
DataStore retains light/dark/system appearance. Personal APK distribution remains the objective.

At handoff, Room TaskDatabase is schema version 3 with exported schemas 1/2/3 and preserving
migrations. It contains task/habit data plus activation, price, entitlement, and ledger entities.
The accounting domain exposes AccountingRepository and AccountingSnapshot with ledger entries,
current balance, total XP, and derived progress. Historical financial entries are immutable;
source corrections use linked accounting operations and atomic persistence.

Recompensas already exists as a navigation destination reached through Hoje's balance tile and
the side menu, but it does not yet allow spending. Histórico de moedas already shows actual
gains/penalties/reversals. Reuse and extend these surfaces rather than creating a second wallet,
parallel ledger, unrelated store navigation, or resetting the user's earned balance.

The managed context reports 65 JVM and 21 Android tests passing on API 36 for the prior feature;
API 24 runtime, physical hardware and TalkBack audio remain unverified. Inspect verification
records rather than claiming previous evidence proves this new feature.

## Purpose and bounded scope

Let the owner define personal rewards and spend earned coins on them, for example playing for
half an hour or buying a snack. This is a personal self-management record, not a real-money
purchase, payment processor, external reservation, device restriction, or proof of actual use.

Implement real reward creation/editing, ordering, optional per-item purchase limits, confirmed
coin spending, immediate-use purchase records, and historical preservation. Everything works
offline with no login, services, new runtime permissions, or store publication work.

## Approved reward fields and organization

- Name, optional description, icon, and coin cost.
- Cost must be a whole integer greater than zero. No free or negative-cost reward.
- Optional maximum number of purchases per day, week, or month, configured PER REWARD.
- With no limit configured, quantity is unrestricted as long as the wallet can pay each purchase.
- Allow reordering rewards in the store.
- Allow editing rewards; edits affect future availability/prices only, not past purchase records.
- Excluding a reward requires explicit confirmation, removes it from the store, and preserves
  every historical purchase and its original details. Do not refund earlier purchases.

Do not introduce required categories, reward difficulty, importance, XP cost, reward bundles,
inventory, custom currencies, or redemption codes. A minimal usable create/edit flow is enough.
Name is needed to identify the reward; only description is explicitly optional. Technical icon
selection/default presentation can follow existing app conventions without reopening product scope.

## Immediate use and confirmation: final user decisions

A purchase IS use. Upon confirmed purchase, debit coins and record that the reward was used
immediately. There is no separate purchased-but-unused state, saved token, stock of rewards,
Activate/Use later button, consumption confirmation, or expiration of an unused entitlement.

The owner explicitly rejected cancellation, returns, and refunds after a successfully confirmed
purchase. The guard against mistakes is a clear confirmation BEFORE committing. Do not add
"undo purchase", "return reward", or a refundable grace period as an unsolicited feature.

The confirmation must show:
- What is being purchased.
- Full coin price.
- Resulting wallet balance.
- Remaining purchase allowance when a limit exists.

Use clear language indicating whether a displayed allowance is before/after this purchase so
the owner understands how many will remain. Do not invent a finite allowance for unlimited items.
Provide a clear confirm action and a cancel/back action. Dismissing/backing out before commitment
must not spend coins or consume quota.

The no-refund policy does not excuse technical corruption. A failed transaction is not a purchase:
no debit without its purchase record, no phantom purchase without debit, and no duplicate charge
from retries. Technical rollback before successful commit is different from a user-facing refund.

## Affordability and accounting integration

- A purchase requires the FULL cost in the existing wallet, leaving balance >= 0 after purchase.
- A negative or insufficient balance blocks purchase even though penalties/corrections elsewhere
  can bring the wallet down to its existing −300 floor.
- Exact available balance is sufficient; purchasing at that price can leave zero.
- A redemption cannot create debt, mint coins, or use XP as payment.
- Spending does not grant/remove XP or change level. It is not a failure or a task/habit outcome.
- Historical coin charges must integrate into the existing ledger, with reason, date, effective
  debit, resulting balance, and stable reference to the purchase.
- Retain existing task/habit source reconciliation and original-value correction semantics.
  Completing, correcting, or refreshing those domains must not erase or duplicate reward spending.

Do not fake spending by creating a task penalty, manipulating a reward entitlement intended for
task earnings, or overwriting totals directly. The later plan chooses the smallest valid extension
to the existing accounting domain, including shared transactions and identity of purchase intents.

## Limits per reward

Examples: one particular snack can be bought at most five times per month; another reward has
its own independent daily limit or no limit. There is no global limit for the whole store.

Use civil calendar periods consistent with the existing app date policy:
- Day: calendar day.
- Week: Monday through Sunday.
- Month: first through last day of the calendar month.

A new period starts a new allowance without deleting any purchase history. Do not use rolling
24-hour/7-day/30-day windows or reset all store limits when any reward changes.

Editing limits counts purchases already made in the applicable current period. If the new limit
is below the number already bought, block additional purchases for the remainder of the period,
without penalizing, cancelling, or rewriting earlier purchases. Do not reset usage just because
the cost, name, icon, description, or limit was edited.

When changing period units, determine usage from preserved purchases of that reward in the newly
selected calendar period, consistent with the approved count-existing-purchases rule. Reuse
current civil date handling; numeric/timezone/storage details belong to planning.

Recheck price, existence/availability, sufficient balance, and current quota when confirming,
not only when first opening a card. If concurrent changes or a period boundary invalidate the
preview, do not silently charge an undisclosed amount. Refresh/explain and obtain valid confirmation
as appropriate. Do not promise a stale price/allowance indefinitely or bypass a newly reached limit.

## Store and history experience

- Keep the approved model B direction and existing Recompensas destination.
- Show current actual coin balance and reward cards with icon, name, description when present,
  cost, and limit status when configured.
- Make it clear when buying is unavailable because of insufficient coins or an exhausted limit;
  retain visible reward information rather than claiming a successful purchase.
- Provide honest empty/create states, editing, reordering, and confirmed deletion. Do not seed
  the prototype's reward examples or grant starting coins in production.
- Integrate confirmed purchases into existing Histórico de moedas. Preserve original reward
  name/description/icon/cost or enough historical snapshot information for understandable old
  records when the current reward is edited/deleted; never replace its charged cost retroactively.
- Preserve purchase identity and period usage through relaunch and recovery. Hiding/removing the
  reward from the store must not remove ledger history.
- Deletion has no restore/archive UI requirement in this delivery; do not add one by analogy
  with habits. Underlying data retention can use an appropriate non-destructive technical strategy.
- User-visible text is Portuguese in resources; identifiers are English. Support current themes,
  accessibility labels, large fonts, system insets and current navigation/back behavior.

## Persistence, concurrency, and failure safety

Reward definition, ordering, limit configuration, purchases and spending must survive app relaunch.
Extend Room with exported schema and tested preserving migrations. Do not destroy existing lists,
tasks, habits, audits, tables, activation state, ledger, XP or appearance settings.

A successful purchase must commit debit, purchase record, and the effect on quota consistently.
Revalidate against current database state and use durable identity/idempotency. Two quick taps,
process recreation, retry after uncertain success, or concurrent purchase attempts must not
overspend, exceed the limit, or double-charge one confirmed purchase intent.

After a successful transaction, intentional later purchases of the same reward remain allowed
if affordable and within quota. Duplicate prevention must not accidentally forbid legitimate
repeat purchases. Inconsistent intermediate UI must not grant optimistic use as final success.
Failures should explain what happened and support safe retry, with correct committed state.

These are consistency/verification requirements, not a request for a distributed payment system,
network queue or a large generic commerce framework. The technical plan should use existing local
Room/accounting infrastructure and minimal suitable abstractions.

## Deferred features and boundaries

- Timers/countdowns for rewards such as two hours of gameplay are explicitly wanted later;
  see TODO/planejamento_lembretes_tempo.md. Do not implement them or introduce saved unused
  time credits in this feature. Immediate use is the current purchase rule.
- No reminders/notifications, alarm permissions, background timing services, app blocking,
  system usage surveillance, actual food ordering, or automatic activity verification.
- No cancellation/refunds of committed purchases, unused reward inventory, deferred consumption,
  cooldowns, subscriptions, scheduled rewards, price discounts, bonus XP, real-money transactions,
  login, backend, synchronization, analytics, or publishing.
- Do not change approved earning/penalty tables, −300 correction floor, XP progression,
  task/habit rules or original-price correction behavior merely to support spending.

## Acceptance and verification expectations

Write independently testable scenarios covering:
1. Create/edit/reorder rewards with required fields and positive integer cost, optional limits,
   persisted state, honest empty screens and validation/errors without phantom saves.
2. Open confirmation showing the selected item, price, actual resulting balance, and applicable
   quota. Cancel/back spends nothing; successful confirmation immediately records use.
3. Affordability at exact balance, insufficient balance, and negative balance; successful debit
   never creates debt and never changes XP/level or creates manual currency.
4. Per-item independent daily/weekly/monthly limits, unlimited items, calendar boundary reset,
   retained historical purchases, and counting current-period purchases after limit edits.
5. Reducing a limit below existing usage blocks further purchases without charging/cancelling
   past ones; price/name edits do not reset usage or rewrite historical price.
6. Ledger history with original purchase details, effective debit/resulting balance and preserved
   records after reward editing/deletion; deletion asks confirmation and does not refund.
7. No separate use-later state and no user cancellation/refund after committed purchase.
8. Durable duplicate prevention, legitimate repeat purchases, atomic debit/purchase/quota,
   concurrent balance/quota checks, retry and process-death recovery without partial effects.
9. Revalidation if price/reward/balance/period changes while confirmation is open, with no
   silently different charge or stale allowance bypass.
10. Task/habit accounting refresh and corrections preserve legitimate spending. Existing history,
    prices, activation behavior, appearance and navigation remain intact.
11. Exported schema/preserving migration, meaningful pure domain and Room integration tests,
    confirmation/error UI checks and accessibility/large-font/theme regression checks.
12. Build, unit/instrumentation tests and lint/release checks pertinent to the eventual authorized
    implementation, with actual evidence and coverage limitations reported separately.

## Scope is settled; resolve technical details in planning

The user answered six initial questions and approved all four final proposals before asking
for this prompt. Do not ask again whether purchases are immediate, refundable, limited per item,
or whether edited/deleted rewards preserve history. Those choices are closed.

Use existing app conventions for simple form/icon/order/date details and document technical
choices in the later plan. This prompt deliberately closes known product gaps rather than
deferring another batch of product questions. Only escalate a genuine contradiction or new
blocking requirement unsupported by the inspected code and recorded decisions; do not invent
such a blocker from routine implementation choices. Specification alone does not authorize
the later plan/tasks/implementation stages or Git mutations.
