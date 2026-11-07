# Implementation Plan: Personal rewards
**Branch**: Existing branch retained | **Date**: 2026-11-07 | **Spec**: [spec.md](spec.md)

## Summary
Extend the existing offline wallet with owner-defined rewards, immediate confirmed purchases and calendar
limits. Persist a confirmation intent to recover uncertain outcomes without duplicate debits. No new dependency.

## Technical Context
**Language/Version**: Existing Kotlin2.3.10 / Java17 / AGP9.0.1 built-in Kotlin.
**Primary Dependencies**: Existing Compose Material3, Hilt/KSP, Room2.8.5, Coroutines/Flow, Navigation.
**Storage**: TaskDatabase schema4, preserving migration3→4 and existing1→2→3; DataStore unchanged.
**Testing**: JUnit4 rules/ViewModel, Android Room/migrations/rollback/concurrency and Compose UI.
**Target Platform**: Android API24–36; available emulator API36; API24/physical/TalkBack audio may remain unverified.
**Project Type**: Single-module Android app.
**Performance Goals**: Local responsive interactions; no history scan benchmark claimed.
**Constraints**: Offline, no permissions, no real currency, no version increase/publication.
**Scale/Scope**: One local owner; store/editor/confirmation and existing coin history.

## Constitution Check
Before research: I PASS (validated spec + explicit implementation request); II PASS (shared Room transaction,
exported schema and preserving migration); III PASS (pure domain/repository, feature UI/ViewModel); IV PASS
(Portuguese resources, existing themes, scrollable dialogs, state/retry); V PASS (rules/Room/UI verification).
After design: all five gates PASS; no exemptions. Implementation must record actual results/limits.

## Project Structure
Documentation: spec.md, checklists/requirements.md, plan.md, research.md, data-model.md,
contracts/rewards.md, quickstart.md, tasks.md; verification.md after implementation.
Source under app/src/main/java/com/ethos/app/: core/rewards/domain/, core/rewards/data/,
features/rewards/; integration in core/tasks/data/TaskDatabase.kt and TaskModule.kt,
features/shell/EthosShell.kt, MainActivity.kt and core/ui/accounting/AccountingConsequences.kt.
Tests under matching app/src/test and app/src/androidTest paths; schema app/schemas/.../4.json.
**Structure Decision**: Reuse one database/accounting ledger; no additional modules or generic commerce layer.

## Delivery sequence
Setup → domain/rules tests → schema/migration/atomic repository tests and implementation → store/editor →
confirmation/recovery → limits/history regression → build/JVM/lint/R8/Android/UI checks and evidence.
