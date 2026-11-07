# Implementation Plan: Habits and goals

**Branch**: master retained | **Date**: 2026-11-06 | **Spec**: [spec.md](spec.md)

## Summary
Extend existing offline task database with normalized habits, historical rules, activity intervals,
records, overrides and reviews. Pure period evaluation drives UI progress, pending review and streaks.
Use MainActivity slots for task/habit integration without cross-feature implementation imports.

## Technical Context
- Language: Kotlin 2.3.10, AGP 9.0.1 built-in Kotlin, existing single :app, min24/target36.
- Dependencies: existing Room2.8.5, Compose Material3, Hilt/KSP, Lifecycle/Flow, java.time desugaring.
  No new dependency/version change required.
- Storage: existing TaskDatabase v1 -> v2, exported schemas and registered non-destructive migration.
- Testing: pure calendar/correction/streak rules, ViewModel failures, Room migration/atomicity and UI.
- Performance: IO transactions, lazy presentation, owner-scale history, no main-thread storage.
- Constraints: offline, no personal logs/permissions/backend/currency/timers.

## Constitution Check
Before research and after design all gates PASS:
I: approved spec/current request authorize subsequent stages; analyze before code.
II: Room source of truth, normalized rows, tested preserving migration; DataStore unchanged.
III: domain pure; repository contract; immutable StateFlow, lifecycle Screen/content split and slots.
IV: Portuguese resources, accessible controls, enlarged-font/scroll handling and durable errors.
V: meaningful boundary/failure/migration/UI checks with executed evidence and explicit limitations.

## Project Structure
Under app/src/main/java/com/ethos/app/:
- core/habits/domain/{HabitModels,HabitRules,HabitRepository}.kt
- core/habits/data/{HabitEntities,HabitDao,RoomHabitRepository,HabitMigration}.kt
- core/tasks/data/{TaskDatabase,TaskModule,RoomTaskRepository}.kt shared storage/list atomicity.
- features/habits/{HabitsViewModel,HabitsScreen,HabitsContent,HabitEditor}.kt
- MainActivity.kt composes content slots; tasks/TasksContent.kt offers lazy extra content for list/today/review.
- res/values/strings.xml; tests mirror packages; app/schemas/ contains schema2.
Documentation: research.md, data-model.md, contracts/ui.md, quickstart.md, tasks.md, verification.md.

## Evaluation and transition design
Calendar period boundaries use historical rule revisions; type changes start after old period ends.
New weekly/monthly rule beginning midperiod requires explicit partial target. Effective revisions
are clipped to their dates; no rewrite of earlier rules. Civil dates never shift with timezone;
TaskClock controls today and instant timestamps. Refresh foreground/minute, no background work.
Activity intervals use inclusive start/exclusive end. Pause/exclusion today makes today inactive;
resume today reactivates it. Same-day transitions retain records already entered (including their original active-date eligibility), while new records
must target active dates. Partial goals use explicit period override; no implicit prorating.
Preserve prior pending periods, including intervals before pause. Evaluate only periods with active dates.
Practice counts distinct qualifying active dates; avoid/limit count events. Stable period identity is
habit + type/unit + calendar start; goal-only edits keep the same period identity. Reviewed result stores its outcome; corrections changing outcome
invalidate confirmation and reopen one review, even if practice now otherwise meets target.
Streaks traverse chronological eligible periods, neutral fully inactive/unscheduled periods; pending
gaps show uncertainty, confirmed failures reset current, best preserved. Unit/type changes begin a
new streak segment; historical best remains shown with its original unit.

## Persistence and error design
All mutation reads/rules/upserts in one Room transaction. List deletion excludes habits in the SAME
transaction and closes active intervals, preserving records. Share a core persistence operation,
never import feature UI implementations. Soft-deleted records and append-only audits retain correction
identity. Retry uses stable action/record IDs. No JSON caches, ledger or event framework.
ViewModel retains drafts and durable retry action; busy guard serializes writes, cancellation propagates.
Compose only sends actions; domain validates capacity and active-date bounds. Current-goal edit has
preview before commit; restore without original list has explicit destination picker.

## Complexity Tracking
No constitutional deviations. Effective rule rows/intervals are required for historical corrections;
no general-purpose versioning framework. Existing owner-scale snapshot transaction pattern retained.

## Execution notes
21/21 tasks complete. Final checks, artifacts, visual evidence and environment limitations are in
verification.md. No new dependency, commit, publication or version change. Context updates stay within
AGENTS.md SPECKIT markers. Temporary read-only AVD shut down after validation.
