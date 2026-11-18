# Tasks: Growing editor sheet for tasks, good habits and bad habits

**Input**: `specs/029-editor-growing-sheet/` (spec.md, plan.md, research.md, data-model.md, contracts/, quickstart.md)

**Tests**: JVM tests for the new presentation functions (constitution V: rule mapping moved out of Composables);
instrumented tests updated for the new selectors and recorded as not run.

Paths are relative to `app/src/` unless they start with `specs/`.

## Phase 1: Setup

- [X] T001 Add editor strings (Mais, Recolher, Descrição, Pronto, label/value format, Limite, Dias, Vezes,
  Cronômetro, Com cronômetro, Cor e ícone, Quando, Novo mau hábito title) in `main/res/values/strings.xml`

## Phase 2: Foundational (blocks all stories)

- [X] T002 Create `GrowingSheet`, `EditorChip`, `ChoiceSheet` and `RadioOptions` with previews in
  `main/java/com/ethos/app/core/ui/components/GrowingSheet.kt` (contract: `contracts/ui-contract.md`)
- [X] T003 [P] Add `TaskEditorSheet`, `showsEndingRow`, `showsChecklistRow` and `showsDurationChip`; remove
  `hasAdvancedOptions` in `main/java/com/ethos/app/features/tasks/TaskEditorPresentation.kt`
- [X] T004 [P] Create `HabitEditorSheet`, `PracticeFrequency`, `practiceFrequencyOf`, `withPracticeFrequency`,
  `withTimed`, `withTimedWeekly`, `withBreakingType`, `toggleHabitWeekday`, `showsDaysChip` and `showsTimesChip`
  in `main/java/com/ethos/app/features/habits/HabitEditorPresentation.kt`
- [X] T005 [P] Update the presentation tests (drop `hasAdvancedOptions`, add visibility) in
  `test/java/com/ethos/app/features/tasks/TaskEditorPresentationTest.kt`
- [X] T006 [P] Create mapping tests that pin the current values (targets, weekdays, MONTH→DAY when timed, AVOID
  target 0, never-empty weekdays) in `test/java/com/ethos/app/features/habits/HabitEditorPresentationTest.kt`

## Phase 3: User Story 1 - Quick capture (P1) 🎯 MVP

**Goal**: create a task, a good habit or a bad habit from the compact sheet.
**Independent test**: "+" › Criar tarefa › type a name › ✓ saves it in Caixa de entrada.

- [X] T007 [US1] Rewrite `TaskEditor` as the `GrowingSheet` compact content: focused name, description line, chips
  for When, Repeat, List, the timer toggle and Duration, plus the When/Repeat/List/Duration/Description sheets, in
  `main/java/com/ethos/app/features/tasks/TaskEditor.kt`
- [X] T008 [US1] Rewrite `HabitEditor` as the `GrowingSheet` compact content: color and icon tile, name,
  description line, chips for Frequency, Days, Times, the timer toggle, Minutes, avoid/limit, Limit and Look,
  plus their sheets, in `main/java/com/ethos/app/features/habits/HabitEditor.kt`

## Phase 4: User Story 2 - Full options (P2)

**Goal**: "Mais" expands to full screen with rows for the remaining fields.
**Independent test**: Mais › Checklist with 2 items and Importância Alta; saved and shown in the details.

- [X] T009 [US2] Add the expanded rows to the task editor (Ending only when repeating, Description, Checklist only
  when not timed, Importance, Difficulty with game mode) with their sheets and the estimate, in
  `main/java/com/ethos/app/features/tasks/TaskEditor.kt`
- [X] T010 [US2] Add the expanded rows to the habit editor (List, Description, Importance, Difficulty with game
  mode) with their sheets and the estimate, in `main/java/com/ethos/app/features/habits/HabitEditor.kt`

## Phase 5: User Story 3 - Edit (P2)

**Goal**: editing opens expanded with the edit-only controls kept.
**Independent test**: edit a recurring task, choose "Esta e as próximas", save; the series changes.

- [X] T011 [US3] Open expanded without focus when editing; keep `ScopeChoice` and the review checkbox inline
  above Save in `main/java/com/ethos/app/features/tasks/TaskEditor.kt`
- [X] T012 [US3] Open expanded when editing; keep the future type hint and the goal-effect choice inline above
  Save in `main/java/com/ethos/app/features/habits/HabitEditor.kt`

## Phase 6: Polish

- [X] T013 [P] Update the instrumented selectors and flows (name tag, `editor-save`, `editor-more`, chip/row
  tags) in `androidTest/java/com/ethos/app/features/tasks/TaskEditorTest.kt`, `TasksContentTest.kt` and any habit
  editor test (not run without authorization)
- [X] T014 Remove the strings, imports and helpers that only the old editors used (`more_options`/`fewer_options`
  only if unused elsewhere) in `main/res/values/strings.xml` and both editor files
- [X] T015 Run `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` and fix failures
- [X] T016 Write `specs/029-editor-growing-sheet/verification.md`, mark the spec Implemented, update the SPECKIT
  block in `AGENTS.md` and `TODO/editores_criacao.md`

## Dependencies

- T001 → T002 → T007/T008. T003 → T007/T009/T011. T004 → T008/T010/T012.
- US1 (T007, T008) → US2 (T009, T010) and US3 (T011, T012). These change the same files, so they run in order
  within each file.
- T013–T016 run after all stories.

## Parallel opportunities

- T003, T004, T005 and T006 (different files).
- The task chain (T007 → T009 → T011) and the habit chain (T008 → T010 → T012) are independent of each other.

## Implementation strategy

MVP = Phase 1–3 (compact capture for the three types). Phases 4–5 restore every remaining field and the edit flow.
All of it ships together, because the old editors are replaced.
