# Tasks: Postpone a task and a cleaner task detail popup

**Input**: `specs/021-task-postpone-detail-popup/` (plan.md, spec.md, research.md, data-model.md,
contracts/task-detail-ui.md, quickstart.md)

**Tests**: JVM tests for the new business rules are required by the constitution (principle V) and the spec
(acceptance criterion 11 of the source prompt).

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup

- [X] T001 Confirm no other usage of history-only strings (`task_history`, `audit_*`, `audit_line`,
  `actual_date_line`, `original_deadline_line`) and of `ListChoices` outside the editor, via search in
  app/src/main/java and app/src/main/res/values/strings.xml

## Phase 2: Foundational

- [X] T002 Add `TaskAction.Postpone(id, to)`, `TaskAction.UndoPostpone(id, to)` and
  `TaskOccurrence.canPostpone` (open, DAY deadline, date set) in
  app/src/main/java/com/ethos/app/core/tasks/domain/TaskModels.kt
- [X] T003 Implement postpone/undo on top of `edit(ONLY_THIS, reviewedDeadline = true)` with bounds
  (`to >= today`, `to != date` for Postpone) in app/src/main/java/com/ethos/app/core/tasks/domain/TaskRules.kt

## Phase 3: User Story 1 - Postpone a task (P1)

**Goal**: move one occurrence to tomorrow or a chosen day from the popup, with undo.
**Independent test**: quickstart steps 2–5.

- [X] T004 [P] [US1] JVM tests: postpone keeps time, bounds rejected, overdue writes MISSED_DEADLINE without
  review flag, recurring next occurrence unchanged, not DAY / resolved rejected, undo restores date and history is
  append-only, in app/src/test/java/com/ethos/app/core/tasks/domain/TaskPostponeTest.kt
- [X] T005 [US1] Extract `EthosDatePickerDialog(initial, min, max, excluded, onConfirm, onDismiss)` from
  `DateField` and reuse it in `DateField` in app/src/main/java/com/ethos/app/features/tasks/TaskPickers.kt
- [X] T006 [P] [US1] Pure presentation helpers (postpone visibility, tomorrow, next series date, overdue note
  date, summary parts, feedback label) in app/src/main/java/com/ethos/app/features/tasks/TaskDetailPresentation.kt
- [X] T007 [US1] ViewModel: `postponeSheet`/`postponePicker` state, `postpone(to)`, `postponed` feedback,
  `undoPostpone()`, `dismissPostponed()` in app/src/main/java/com/ethos/app/features/tasks/TasksViewModel.kt
- [X] T008 [US1] Postpone `ModalBottomSheet` (Amanhã, Escolher data, notes) and picker in
  app/src/main/java/com/ethos/app/features/tasks/TaskDetail.kt
- [X] T009 [US1] `PostponeFeedback` snackbar and its place in the snackbar chain in
  app/src/main/java/com/ethos/app/features/tasks/TasksContent.kt and app/src/main/java/com/ethos/app/EthosApp.kt
- [X] T010 [P] [US1] Strings for sheet, notes and feedback in app/src/main/res/values/strings.xml

## Phase 4: User Story 2 - Reminders follow (P1)

**Goal**: fixed-date reminders move by the same days; relative follow; weekday unchanged.
**Independent test**: quickstart step 6.

- [X] T011 [P] [US2] `ReminderRules.postponeChosen(defs, occurrence, days, recurring)` in
  app/src/main/java/com/ethos/app/core/reminders/domain/ReminderRules.kt
- [X] T012 [P] [US2] JVM tests: occurrence-scoped shift, recurring series override, non-recurring series untouched,
  relative/weekday untouched, undo shifts back, scheduled slot moves, in
  app/src/test/java/com/ethos/app/core/reminders/ReminderPostponeTest.kt
- [X] T013 [US2] Upsert shifted definitions in the same transaction for Postpone/UndoPostpone in
  app/src/main/java/com/ethos/app/core/tasks/data/RoomTaskRepository.kt

## Phase 5: User Story 3 - Cleaner popup (P2)

**Goal**: "Mínimo" layout per contracts/task-detail-ui.md.
**Independent test**: quickstart steps 1, 7–10.

- [X] T014 [US3] Move the popup out of TasksContent into `TaskDetail` with the Mínimo layout (title, summary,
  description clamp, checklist, Concluir ▾ / Adiar, five labelled icons, resolved strip, no history, no Fechar)
  in app/src/main/java/com/ethos/app/features/tasks/TaskDetail.kt and app/src/main/java/com/ethos/app/features/tasks/TasksContent.kt
- [X] T015 [US3] "Concluí em outro dia…" picker (min creation, max today) and "Lista" choice dialog wired to
  `complete` and `TaskAction.Move` in app/src/main/java/com/ethos/app/features/tasks/TaskDetail.kt and
  app/src/main/java/com/ethos/app/features/tasks/TasksViewModel.kt
- [X] T016 [P] [US3] Strings for popup labels; remove history-only strings confirmed in T001, in
  app/src/main/res/values/strings.xml
- [X] T017 [P] [US3] Preview of the popup content with explicit data in
  app/src/main/java/com/ethos/app/features/tasks/TaskDetail.kt

## Phase 6: Polish

- [X] T018 Run `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` and fix failures
- [X] T019 Write specs/021-task-postpone-detail-popup/verification.md (executed checks, not executed checks)
  and add the spec 021 line in the SPECKIT block of AGENTS.md
- [X] T020 Install the debug build on the user's phone preserving data (`adb install -r`), as explicitly requested

## Dependencies

- T002 → T003 → (T004, T007, T013). T005 → T008, T015. T006 → T008, T014. T011 → T012, T013.
- US1 and US2 share T002–T003; US3 (T014) hosts the Adiar button of US1, so T008 and T014 touch the same file
  and run sequentially.

## Parallel examples

- After T003: T004, T006, T010, T011, T012 in parallel (different files).

## Implementation strategy

MVP = Phases 2–4 (postpone with reminders) behind the existing popup; then Phase 5 replaces the popup. In this
session all phases run in order, then T018–T020.
