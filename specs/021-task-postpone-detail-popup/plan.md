# Implementation Plan: Postpone a task and a cleaner task detail popup

**Branch**: current branch (no feature branch) | **Date**: 2026-11-16 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/021-task-postpone-detail-popup/spec.md`

## Summary

Add a domain action that moves one open specific-day task occurrence to another date (and one that undoes it),
reusing the existing "edit only this occurrence" rule so the missed-deadline record and recurrence isolation stay
as today. In the same Room transaction, shift the task's fixed-date reminders by the same number of days. Rebuild
the task detail `AlertDialog` as the "Mínimo" layout (summary line, two large buttons, five labelled icons, no
history, no Fechar) with a postpone bottom sheet and a "Desfazer" snackbar. No schema change.

## Technical Context

**Language/Version**: Kotlin (AGP 9 built-in Kotlin), Java time API

**Primary Dependencies**: Jetpack Compose + Material 3 (`AlertDialog`, `ModalBottomSheet`, `DatePicker`), Hilt,
Room 2.8.5, Coroutines/Flow — all already in the project; no new dependency

**Storage**: Room `TaskDatabase` (tasks and reminder definitions share it, so one transaction covers both); schema
unchanged (version 7)

**Testing**: JUnit JVM tests (`:app:testDebugUnitTest`) for `TaskRules` and `ReminderRules`; instrumented tests
written only if needed and not executed without authorization

**Target Platform**: Android (minSdk as configured, verified on API 36 emulator historically)

**Project Type**: mobile-app, single `:app` module

**Performance Goals**: postpone feels instant (one transaction, same cost as an edit)

**Constraints**: offline; preserve the user's real routine; no destructive migration; strings in pt-BR resources

**Scale/Scope**: one dialog rebuilt, one bottom sheet, two domain actions, one reminder rule, ~10 new strings

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I** ✅ Spec 021 with acceptance criteria exists; the user explicitly requested specify → plan → tasks →
  implement in this session.
- **II** ✅ Offline only; Room stays the source of truth; no schema change (reminder and task rows already have the
  needed columns); postpone + reminder shift run in a single transaction; failures surface the existing storage
  error and change nothing.
- **III** ✅ Rule lives in `core/tasks/domain/TaskRules` (no Android deps); reminder shift in
  `core/reminders/domain/ReminderRules`; orchestration in `RoomTaskRepository` (core data, same database);
  ViewModel exposes state; Composables only draw. No new layers or modules. The feature `tasks` does not import
  `features/reminders`.
- **IV** ✅ Loading/busy/error reuse existing task states; light/dark via theme; labelled icon buttons with content
  descriptions, 48 dp targets, 200% font considered; no personal data in logs; no new permission.
- **V** ✅ JVM tests for postpone/undo rules (bounds, overdue review, recurrence isolation, no accounting) and for
  the reminder shift; build + JVM tests + lint run; instrumented/device checks recorded as not executed unless
  authorized. Installing the debug build on the user's phone was explicitly requested.

Post-design re-check: still passing (see research decisions R1–R7).

## Project Structure

### Documentation (this feature)

```text
specs/021-task-postpone-detail-popup/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── task-detail-ui.md
├── checklists/requirements.md
└── tasks.md            # /speckit-tasks
```

### Source Code (repository root)

```text
app/src/main/java/com/ethos/app/
├── core/tasks/domain/TaskModels.kt          # TaskAction.Postpone / UndoPostpone, TaskOccurrence.canPostpone
├── core/tasks/domain/TaskRules.kt           # postpone/undo rule on top of edit(ONLY_THIS)
├── core/tasks/data/RoomTaskRepository.kt    # shifts fixed-date reminders in the same transaction
├── core/reminders/domain/ReminderRules.kt   # postponeChosen(defs, occurrence, days)
├── features/tasks/TasksViewModel.kt         # postpone(), undoPostpone(), completion date, sheet state
├── features/tasks/TaskDetail.kt             # NEW: Mínimo popup, postpone sheet, list choice (moved out of TasksContent)
├── features/tasks/TaskDetailPresentation.kt # NEW: pure summary/visibility/date choices
├── features/tasks/TaskPickers.kt            # extract reusable date picker dialog (min/max/excluded day)
├── features/tasks/TasksContent.kt           # remove old TaskDetail; PostponeFeedback snackbar
├── EthosApp.kt                              # snackbar chain shows PostponeFeedback
└── res/values/strings.xml                   # new texts; remove history-only texts
app/src/main/res/drawable/                   # reuse existing ic_* Material Symbols; add only missing ones
app/src/test/java/com/ethos/app/core/...     # TaskPostponeTest, ReminderPostponeTest
```

**Structure Decision**: single `:app` module; feature code in `features/tasks`, rules in `core/*/domain`, atomic
persistence in `core/tasks/data` as today.

## Complexity Tracking

No constitution violations.
