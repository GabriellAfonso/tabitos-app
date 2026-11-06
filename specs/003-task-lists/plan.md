# Implementation Plan: Lists, tasks and recurrence

**Branch**: master retained | **Date**: 2026-11-06 | **Spec**: [spec.md](spec.md)
**Input**: specs/003-task-lists/spec.md; all three clarification groups approved.

## Summary
Add an offline Room-backed organizer to the existing shell, with shared task contracts, pure
rules and transactionally persisted occurrence snapshots/history. Compose screens connect through
MainActivity slots, keeping feature implementations independent.

## Technical Context
**Language/Version**: existing Kotlin 2.3.10, AGP 9.0.1 built-in Kotlin, JVM17/JDK21.
**Primary Dependencies**: existing Compose/Hilt/KSP/Navigation/Lifecycle; Room 2.8.5 and
core-library desugaring 2.1.5 (java.time on API24/25), pinned in catalog.
**Storage**: Room database v1, exported schema, no destructive fallback; appearance DataStore retained.
**Testing**: JVM rules/recurrence/ViewModel failures; Android Room close/reopen/transaction and Compose flows.
**Target Platform**: Android min24/target36, single :app module, com.ethos.app.
**Project Type**: native mobile app for one owner.
**Performance Goals**: no main-thread database operations; lazy scrolling of growing occurrences.
**Constraints**: offline, no personal logs/permissions/accounts/financial calculations.
**Scale/Scope**: owner-managed lists/tasks and retained history; no cloud or speculative framework.

## Constitution Check
Pre-research and post-design: all five gates PASS.
- I: validated spec and current request authorize stages; read-only analysis precedes code.
- II: Room structured records, transaction boundaries, exported v1, no migration from nonexistent DB.
  Future schema evolution requires migration tests. DataStore appearance is untouched.
- III: immutable domain snapshots, repository contract, pure rule service, ViewModel StateFlow and
  Screen/content split. MainActivity composes task/shell features using slots; no cross-feature imports.
- IV: Portuguese resources, current themes, labeled 48dp controls, scrolling/dialogs, durable errors.
  Backup already disabled; no new permissions/logs.
- V: meaningful recurrence/correction/failure and database tests plus build/lint/R8/UI validation;
  unavailable device coverage is recorded explicitly.

## Project Structure
Documentation: spec.md, plan.md, research.md, data-model.md, contracts/ui.md, quickstart.md,
tasks.md, checklists/requirements.md and verification.md under specs/003-task-lists/.
Source under app/src/main/java/com/ethos/app/:
- core/tasks/domain/{TaskModels,TaskRepository,TaskRules}.kt: contracts and pure business rules.
- core/tasks/data/{TaskEntities,TaskDao,TaskDatabase,RoomTaskRepository,TaskModule}.kt.
- features/tasks/{TasksViewModel,TasksScreen,TasksContent,TaskEditor}.kt.
- MainActivity.kt supplies drawer and destination content slots to features/shell/EthosShell.kt.
- res/values/strings.xml: visible Portuguese interface.
Tests mirror these under app/src/test/ and app/src/androidTest/; schemas in app/schemas/.

## Date and recurrence design
All deadlines are local civil dates stored as epoch days; optional time is minute-of-day and does
not determine overdue state. Each refresh obtains current zone/today, so changing timezone changes
which day is today, never shifts saved dates. Audit timestamps are instants; actual completion is a
civil date bounded by root task creation date and current local today. Refresh on foreground and a
short foreground ticker (at most one minute); no background work or alarm permissions.
Weekly period start/end are saved explicitly, Monday–Sunday or Monday–Friday; do not roll them forward.

Each recurring task has stable root identity and monotonically numbered slots. Materialize fixed
calendar occurrences through today plus one next occurrence, retaining every missed slot. Transaction
and root+ordinal uniqueness prevent duplicates. Completion-relative series generate a successor only
on explicit resolution; calculate from actual completion or previous planned date for Não será feita.
Monthly calculation retains chosen day independently of clamped dates. Finite limits count slots,
including deleted exceptions. End dates are inclusive. No automatic failures or penalties.

Occurrence-only edits replace its snapshot and preserve the root cadence. Future-scope edits retain
prior slots and consumed count, reset the future rule anchor at the selected slot, replace only
untouched future slots and preserve identity/ordinal where regenerated. Reject conflicting future
changes when later occurrences have user actions; explain correction is required first.
Undo of completion-relative outcomes blocks if successors were acted on (checklist/edit/outcome/review),
otherwise hides untouched generated successors and restores the predecessor. Re-resolution reuses
successor ordinal/identity and resets untouched state; audit history remains. Deleting a pending
completion-relative slot advances from its planned anchor without a failure, consuming that slot.

## Persistence and transitions
Normalized tables: lists, task definitions, occurrences, checklist items and audit records. Occurrence
attributes are snapshots; checklist template rows belong to definition and check rows to occurrence.
Soft deletion hides active content but retains audit references and identity; list deletion includes
all owned active occurrences even when one-off moves differ from root membership.
A repository mutation loads the aggregate inside Room withTransaction, applies pure rules, and upserts
normalized rows atomically (no JSON cache). This intentionally simple owner-scale aggregate strategy
can be optimized with targeted queries later; no full-table destructive replacement. A transaction
failure rolls back, propagates an error and leaves retry input available. Serialized UI actions plus
idempotent IDs prevent repeated creation. No event framework or ledger is introduced.

## UI and error design
Task UI owns capture/editor/list manager, detail/checklist, completion date/confirmation, review,
calendar date selection and scope dialogs. Pending reorder uses accessible move controls. Drawer lists
are real and include Inbox/create/manage. Hoje retains balance/level shell tiles and type filters.
ViewModel owns actions, confirmations, draft and durable retry state; collectors are lifecycle aware.
Completed and Não será feita outcomes remain inspectable/reopenable; correction history is visible in detail.

## Complexity Tracking
No constitutional deviations; aggregate transaction reads are a documented simplicity tradeoff.

Weekly deadlines also support recurrence: each scheduled anchor maps to that civil week's Monday–Sunday/Friday period, with independent slots, ending counts based on scheduled anchors, and no fixed completion day. Undated tasks do not recur until a dated/period deadline is chosen.

## Execution notes
Implementation and20/20 tasks complete. Final build/JVM/lint/R8/Android and manual evidence are in verification.md. Context script could not access python3.exe; only the managed SPECKIT block was updated directly. No commit/publication/version change.
