# Research: Coins, XP and levels

## Atomic integration
Decision: accounting runs inside existing Room source transactions.
Rationale: task/habit repositories already use withTransaction; polling flows can lose/double effects.
Alternatives: asynchronous collectors and UI-only guards rejected for process-death consistency.
Verified paths: core/tasks/data/RoomTaskRepository.kt, core/habits/data/RoomHabitRepository.kt.

## Stable source identities and activation
Decision: baseline existing outcomes/records before mutations, durable entitlement keys independent of
random audit UUIDs. Use occurrence identity, habit/day, unwanted record identity and HabitPeriod.key.
Rationale: audit UUIDs change at re-resolution; timestamp cutoffs fail on clock changes/equal timestamps.
Alternative: historical sweep rejected by approved1A. Persisted baseline suppresses old outcomes.

## Habit semantics
Decision: reuse HabitRules.periods/excessIds and date→entered→id ordering; add practice-only cap.
Rationale: existing Record accepts over-target practice; view-only filtering is insufficient.
Reviewed invalidation reverses period awards/penalties, never duplicates per-event deductions.
Alternative: separate accounting calendar rejected to prevent divergence.

## Correction chain and pricing
Decision: first financial entry price, integer0..10000, no reset. Append linked entries; reapplication
of a floor-clipped award restores only effective reversal, original XP. Refund effective penalties.
Rationale: explicit user1A/2A/3A. No hidden debt and no correction-based coin minting.
Alternative: always regrant original full award after clipping rejected as exploitable.

## Arithmetic and dependencies
Decision: checked Long balances/XP, BigInteger threshold comparisons and integer binary search.
Rationale: avoid floating rounding and intermediate overflow while retaining accepted curve.
Alternative: Double inverse formula alone risks exact boundaries; no new numerical dependency needed.
No dependency changes or external API additions: use locally verified existing toolchain.

Research agent inspected source read-only as explicitly required by speckit-plan; findings consolidated here.
