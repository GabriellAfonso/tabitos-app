# Data model
TaskDatabase4 adds three tables; existing tables/ledger columns unchanged.

- RewardEntity (rewards): string id PK; embedded pure Reward with name, description, icon key,
  Long cost>0, nullable Int limit>0 and period DAY/WEEK/MONTH, Long order/revision, deleted flag.
  Blank name/invalid icon/mismatched limit-period rejected. Edits bump revision. Delete is soft.
- PurchaseIntentEntity (reward_intents): string id PK UUID; embedded Reward snapshot prefixed reward_,
  LocalDate date, disclosed Long balance, Int used, Boolean closed. Open → committed receipt or cancelled;
  committed stays recoverable until acknowledged. Refresh replaces preview under same uncommitted identity.
- PurchaseEntity (reward_purchases): string id PK matching intent; embedded original Reward snapshot,
  LocalDate date, Long time, Long ledgerSequence with unique index. Immutable.
- Existing accounting_ledger: append sequence=max+1, key=purchase:<id>, kind=PURCHASE, nominal/effective
  coins=-cost, xp=0, balance=current-cost>=0, totalXp unchanged, no entitlement.

Quota is count of purchases with reward.id in inclusive civil start/end dates, derived transactionally.
Store order is persisted via adjacent swaps. Changes cannot erase usage/history. All numeric operations
that can overflow use checked arithmetic. Intent/purchase relation enforces idempotency by PK plus transaction.
Migration3→4 only creates new tables/index; old tables/preferences remain untouched; schemas1–4 versioned.
