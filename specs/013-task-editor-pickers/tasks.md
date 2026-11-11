---
description: "Task list for spec 013 — task editor pickers and quick capture"
---

# Tasks: Task Editor Pickers and Quick Capture

**Input**: Design documents from `specs/013-task-editor-pickers/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui-contract.md

**Tests**: Principle V — JVM tests for pure mapping/format/visibility rules and ViewModel discard state;
instrumented test for the editor flow and accessibility labels. No schema/migration change.

## Format: `[ID] [P?] [Story] Description`

Paths are relative to `app/src/` unless stated. `T` = `main/java/com/ethos/app`.

## Phase 1: Setup

- [X] T001 Confirm no dependency change is needed: Material 3 `DatePicker`/`TimePicker` available via
  the existing BOM in `gradle/libs.versions.toml` (record in verification.md)

## Phase 2: Foundational (blocking)

- [X] T002 [P] Create pure pt-BR formatter `EthosDates` (short/long date, `DateText` relative result,
  month title, week range, `HH:mm`) in `main/java/com/ethos/app/core/ui/format/EthosDates.kt`
- [X] T003 [P] JVM tests for `EthosDates` (today/tomorrow/yesterday, same year, other year, month title,
  week range across months, Mon–Fri range) in `test/java/com/ethos/app/core/ui/format/EthosDatesTest.kt`
- [X] T004 [P] Create `DateField`/`TimeField` composables with M3 pickers, UTC conversion, min/max
  selectable dates, clear action and semantics in `main/java/com/ethos/app/features/tasks/TaskPickers.kt`
- [X] T005 Add new strings (picker labels, Mais opções, repetition options, weekly deadline labels,
  checklist actions, discard dialog, relative words) in `main/res/values/strings.xml`

## Phase 3: User Story 1 — Pick dates and times (P1) 🎯 MVP

**Goal**: No typed date/time in the task flow. **Independent test**: create day+time, recurring with final
date and resolve with past completion date using only pickers.

- [X] T006 [US1] Replace date, time, start date and final date `Input`s by `DateField`/`TimeField` writing
  ISO text into the draft; final date min = start date in `main/java/com/ethos/app/features/tasks/TaskEditor.kt`
- [X] T007 [US1] Replace "data em que foi feita" `Input` by `DateField` (max = today) in
  `main/java/com/ethos/app/features/tasks/TasksContent.kt`
- [X] T008 [US1] Switching deadline to "Sem data" clears date/time text in the draft in
  `main/java/com/ethos/app/features/tasks/TaskEditor.kt`

## Phase 4: User Story 2 — Quick capture and "Mais opções" (P1)

**Goal**: Full-screen editor, quick fields first. **Independent test**: title-only save equals current
defaults; all advanced fields still persist.

- [X] T009 [P] [US2] Add pure `hasAdvancedOptions(draft)` in
  `main/java/com/ethos/app/features/tasks/TaskEditorPresentation.kt`
- [X] T010 [US2] Add `draftOriginal` (SavedState), `discardConfirmation`, `closeDraft()` and
  `confirmDiscard(Boolean)` in `main/java/com/ethos/app/features/tasks/TasksViewModel.kt`
- [X] T011 [US2] Rewrite `TaskEditor` as full-screen `Dialog` (safe drawing + IME padding) with header
  actions, title focus on new task, quick fields, collapsible "Mais opções" (rememberSaveable seeded by
  `hasAdvancedOptions`) in `main/java/com/ethos/app/features/tasks/TaskEditor.kt`
- [X] T012 [US2] Wire `closeDraft` and discard `AlertDialog` in `TaskDialogs` in
  `main/java/com/ethos/app/features/tasks/TasksContent.kt`
- [X] T013 [P] [US2] ViewModel tests: close unchanged closes; changed asks; confirm discards; cancel keeps
  in `test/java/com/ethos/app/features/tasks/TasksViewModelTest.kt`

## Phase 5: User Story 3 — Consistent date wording (P2)

- [X] T014 [P] [US3] Use `EthosDates` short format (+ `HH:mm`) in `TaskRow` and long format in task detail
  in `main/java/com/ethos/app/features/tasks/TasksContent.kt`
- [X] T015 [P] [US3] Use `EthosDates.monthTitle` for the habit calendar title in
  `main/java/com/ethos/app/features/habits/HabitsContent.kt`

## Phase 6: User Story 4 — Review confirmation only when needed (P2)

- [X] T016 [P] [US4] Add pure `needsReviewConfirmation(draft, occurrence, today)` in
  `main/java/com/ethos/app/features/tasks/TaskEditorPresentation.kt`
- [X] T017 [US4] Show review text/checkbox only when it returns true in
  `main/java/com/ethos/app/features/tasks/TaskEditor.kt` (editor receives the occurrence and today)

## Phase 7: User Story 5 — Checklist items (P3)

- [X] T018 [P] [US5] Add pure checklist helpers (split/join, add, remove, move) in
  `main/java/com/ethos/app/features/tasks/TaskEditorPresentation.kt`
- [X] T019 [US5] Checklist item editor (field per item, Enter adds next, remove, up/down with item
  descriptions, "Adicionar item") in `main/java/com/ethos/app/features/tasks/TaskEditor.kt`

## Phase 8: User Story 6 — Repetition and weekly labels (P3)

- [X] T020 [P] [US6] Add `RepeatChoice`, `repeatChoiceOf(kind)`, `weeklyKind(bits)` and kind mapping for
  sub-options in `main/java/com/ethos/app/features/tasks/TaskEditorPresentation.kt`
- [X] T021 [US6] Repetition UI: 5 options + sub-options (Todos os dias/Dias úteis; weekday chips; month
  day; interval + mode) calling existing `onRepeat`/`onChange` in
  `main/java/com/ethos/app/features/tasks/TaskEditor.kt`
- [X] T022 [US6] Weekly deadline chips "Até domingo"/"Até sexta (dias úteis)" with current week range in
  `main/java/com/ethos/app/features/tasks/TaskEditor.kt`
- [X] T023 [P] [US6] JVM tests: 8 kinds round-trip, weekly bits → kind, last-day guard, review visibility,
  advanced rule, checklist helpers in `test/java/com/ethos/app/features/tasks/TaskEditorPresentationTest.kt`

## Phase 9: Polish & verification

- [X] T024 Update instrumented capture test for item checklist and add editor test (quick capture, Mais
  opções, review hidden for non-overdue) in `androidTest/java/com/ethos/app/features/tasks/`
- [X] T025 Remove obsolete strings (`date_format`, `time_format`, `checklist_field`, `actual_completion`,
  long hints) and unused labels in `main/res/values/strings.xml`
- [X] T026 Run `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` and
  instrumented tests if an emulator is available; record results in
  `specs/013-task-editor-pickers/verification.md`
- [X] T027 Update spec 003 note and AGENTS.md context lines for spec 013 in `specs/003-task-lists/spec.md`
  and `AGENTS.md`

## Dependencies

- Phase 2 blocks all stories (T002 formatter, T004 pickers, T005 strings).
- US1 → US2 share `TaskEditor.kt`; implement sequentially in file order T006 → T011 → T017 → T019 → T021.
- US3 independent after T002. Pure functions T009/T016/T018/T020 parallel with each other.

## Parallel examples

- T002 ‖ T004 ‖ T009 ‖ T016 ‖ T018 ‖ T020 (separate concerns, pure files)
- T014 ‖ T015 (different files)

## Implementation strategy

MVP = Phase 2 + US1 (pickers). Then US2 (biggest UX gain), US3–US6, polish. Single developer: execute in
ID order, `TaskEditor.kt` edited as one rewrite covering T006/T008/T011/T017/T019/T021/T022.
