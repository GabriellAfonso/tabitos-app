# Implementation Plan: Coins, XP and levels
**Branch**: master retained | **Date**: 2026-11-06 | **Spec**: [spec.md](spec.md)

## Summary
Add atomic local accounting to existing task/habit writes, with immutable ledger entries, reusable
outcome prices, activation baselines and accessible live wallet/settings/history UI.

## Technical Context
**Language/Version**: existing Kotlin2.3.10, AGP9.0.1 built-in Kotlin, Gradle9.3.1, JVM17.
**Primary Dependencies**: existing Compose/Material3, Hilt/KSP, Room2.8.5, Coroutines/Flow; no new dependencies.
**Storage**: TaskDatabase3, normalized wallet activation, prices, entitlements and append-only ledger;
appearance stays in DataStore. Explicit preserving 2→3 migration, retaining 1→2.
**Testing**: JVM math/reconciliation/cap tests; Room migration/rollback/retry and Compose integration tests;
debug/JVM/lint/R8 release plus available API36 runtime checks.
**Target Platform**: Android min24/target36, single :app module.
**Project Type**: local native mobile application.
**Performance Goals**: deterministic synchronous financial consequences within each source transaction;
no polling worker or background accounting service.
**Constraints**: offline, no new permissions/accounts/services, source provenance not logged.
**Scale/Scope**: one owner, three categories, existing task/habit histories and three financial UI surfaces.

## Constitution Check
Before research and after design: I–V PASS.
I: validated spec, explicit stage authorization, analysis before implementation.
II: same Room transaction for source and accounting, preserving migration and durable baseline.
III: pure domain arithmetic/projection/reconciliation, shared core contracts, ViewModel StateFlow and slots
for feature composition at MainActivity; no cross-feature implementation imports.
IV: Portuguese resources, lifecycle collection, retry states, themes, 48dp controls and enlarged fonts.
V: financial edge cases, rollback/migration/regression and honest device/accessibility evidence.

## Project Structure
Documentation: spec, plan, research, data-model, contracts/ui, quickstart, tasks, verification.
Under app/src/main/java/com/ethos/app/:
- core/accounting/domain/{AccountingModels,AccountingRules,AccountingProjection,AccountingRepository}.kt
- core/accounting/data/{AccountingEntities,AccountingDao,AccountingMigration,AccountingStore,RoomAccountingRepository}.kt
- core/ui/accounting/AccountingConsequences.kt: shared pure state-driven estimates/explanation.
- features/accounting/{AccountingViewModel,AccountingScreen,AccountingContent}.kt
- existing core/tasks/data and core/habits/data repositories call AccountingStore in their transactions.
- MainActivity composes wallet/settings/history and injects financial context around task/habit content.
Tests under app/src/test and app/src/androidTest with matching core/accounting paths; schema3 under app/schemas.

## Accounting decisions
Activation takes a durable baseline of existing outcomes before the first mutation, not a timestamp-only
cutoff. Old fulfilled/failed outcomes are unpriced; undo changes no money. New actions, including retroactive
records and old open tasks, count. When an old baseline outcome is undone then newly resolved, its new
application uses the first new financial price. Baseline records themselves cannot gain through refresh.
Persist baseline habit record identities too, so moving an old record or changing which limit slot it
occupies cannot back-pay/back-charge that old record. New retroactive record identities remain eligible.
Each entitlement has a stable task-occurrence/outcome, habit/day, unwanted-record or period/outcome key.
Preserve first committed price and latest active application. Reconcile changed source outcomes in the
same Room transaction, first reversals then applications, deterministic key order.
Original entries never change. Reversal links the active entry; reapplication links the last reversal,
restoring only reversed effective coins plus original XP. Failure refund uses effective charge; a later
new application of that penalty uses its preserved nominal price clipped against the current balance.
Deleted/excluded sources do not trigger missing-source reversal; reconcile only relevant changed subjects.
MISSED_DEADLINE is a zero-financial-impact audit; only explicit Não será feita penalizes tasks.
Practice cap belongs to domain Record validation; unwanted events remain unrestricted.
Limit ordering reuses date/entered/id. Invalidated reviews reverse obsolete period accounting.
Table inputs are integer0..10000, zero allowed, no reset button. All nine values save atomically.
Long checked arithmetic for coins/XP; level thresholds evaluated with BigInteger to avoid overflow,
binary search level and exact integer progress, no maximum-level policy introduced.

## Verification strategy
Run meaningful domain tests, preserving migration with old task/habit data and activation snapshots,
transaction rollback on rejected writes, repeated source actions, original prices and correction chains.
Use existing Android toolchain; record unavailable API24/physical/TalkBack without assuming coverage.
Run .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease.
Connected checks run separately from release. No clean/rerun-tasks or cache deletion by default.
