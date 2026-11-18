# Implementation Plan: Growing editor sheet for tasks, good habits and bad habits

**Branch**: `029-editor-growing-sheet` (no branch; current branch) | **Date**: 2026-11-17 |
**Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/029-editor-growing-sheet/spec.md`

## Summary

Replace the two full-screen editor dialogs (`TaskEditor`, `HabitEditor`) with one shared growing sheet. The sheet
opens compact (name and value chips, keyboard open) when creating, or expanded (full screen with value rows) when
editing or after "Mais". This is a UI-only change. Drafts, ViewModel actions, validation, rules and schema stay as
they are. The habit rule mapping that sits inline in Composables (frequency indexes, `ordinal`) moves to pure,
tested presentation functions.

## Technical Context

**Language/Version**: Kotlin (AGP 9 built-in Kotlin), Jetpack Compose BOM 2026.02.00, Material 3

**Primary Dependencies**: existing only (Compose, Material 3, Hilt). No new dependency.

**Storage**: N/A. Room schema 7 unchanged; drafts unchanged.

**Testing**: JVM unit tests for the presentation functions; Compose instrumented tests updated but not run without
authorization.

**Target Platform**: Android (minSdk 24, verified on API 36 in previous features)

**Project Type**: mobile-app, single `:app` module

**Performance Goals**: the sheet opens without noticeable delay; animating the content size between the two states
must not drop frames on a mid-range phone.

**Constraints**: no business rules in Composables; strings in `strings.xml`; touch targets of 48 dp or more; font
scaling; light and dark theme.

**Scale/Scope**: 2 editor files rewritten, 1 shared component file, 2 presentation files, strings and tests.

## Constitution Check

- I: Spec 029 exists with decisions and acceptance criteria. The user asked for specify → plan → tasks →
  implement in this conversation (2026-11-17). PASS.
- II: No data, schema or offline behavior change. Saving uses the existing ViewModel and repository paths. PASS.
- III: The UI only receives state and callbacks. The shared sheet lives in `core/ui/components`, and feature
  editors keep importing only `core`. No new layer or dependency. PASS.
- IV: Error, busy, empty value and edit states are covered. Chips and rows have "label: value" semantics.
  Strings are in Portuguese resources; light and dark come from the theme. No permissions, nothing logged. PASS.
- V: Pure presentation functions (frequency mapping, chip and row visibility, summaries) get JVM tests. Existing
  rules and ViewModel tests must stay green. Instrumented tests are updated and recorded as not run. PASS.

Post-design re-check: still PASS. The design adds one shared component and two presentation files, with no new
abstraction layer.

## Project Structure

### Documentation (this feature)

```text
specs/029-editor-growing-sheet/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/ui-contract.md
├── checklists/requirements.md
└── tasks.md            # next step (/speckit-tasks)
```

### Source Code

```text
app/src/main/java/com/ethos/app/
├── core/ui/components/
│   └── GrowingSheet.kt          # NEW: GrowingSheet, EditorChip, ChoiceSheet, RadioOptions (+ previews)
├── features/tasks/
│   ├── TaskEditor.kt            # REWRITE: sheet content; keeps ChecklistEditor, ListChoices, ScopeChoice, Input
│   └── TaskEditorPresentation.kt# UPDATE: TaskEditorSheet enum, quick/more visibility; drop hasAdvancedOptions
├── features/habits/
│   ├── HabitEditor.kt           # REWRITE: sheet content; keeps HabitLists/HabitChoices used elsewhere
│   └── HabitEditorPresentation.kt # NEW: PracticeFrequency/LimitPeriod mapping, chip and row lists
└── res/values/strings.xml       # new labels: Mais, Recolher, Descrição, "%1$s: %2$s", sheet titles

app/src/test/java/com/ethos/app/features/
├── tasks/TaskEditorPresentationTest.kt   # UPDATE
└── habits/HabitEditorPresentationTest.kt # NEW

app/src/androidTest/java/com/ethos/app/features/
├── tasks/TaskEditorTest.kt, TasksContentTest.kt  # UPDATE selectors ("more-options" → "editor-more")
└── habits/HabitsContentTest.kt                   # UPDATE if it opens the editor
```

**Structure Decision**: the sheet shell and its small building blocks are shared by two features, so they go to
`core/ui/components` (AGENTS.md: no duplicated components). Type-specific content stays in each feature.

## Design

1. **`GrowingSheet`** (core): `Dialog` with `usePlatformDefaultWidth = false` and `decorFitsSystemWindows = false`
   that fills the window. Tapping outside the surface calls `onDismiss`; back also calls `onDismiss`. The surface
   is aligned to the bottom:
   - compact: `wrapContentHeight`, top corners 28 dp, `imePadding` + navigation bar padding, inner vertical scroll
     limited to the space above the keyboard;
   - expanded: `fillMaxSize`, `safeDrawingPadding`, header (collapse arrow or close button, title), scrolling body,
     and a footer with a text Save button.
   - `animateContentSize` between states. Parameters: `expanded`, `editing`, `title`, `busy`, `onDismiss`,
     `onCollapse`, `onSave`, slots `name`, `content` (`ColumnScope`). The round Save button appears only when
     compact.
2. **`EditorChip`** (core): an `AssistChip` with a leading icon and the value as text; semantics
   `"$label: $value"`, minimum height 48 dp. **`ChoiceSheet`**: `ModalBottomSheet` (`skipPartiallyExpanded`) with
   a title and a "Pronto" button. **`RadioOptions`**: a `selectableGroup` of radio rows. Expanded rows reuse
   `SettingsLinkRow` (title and value as supporting text).
3. **Task editor**: `TaskEditor(...)` keeps its current signature, so `TasksContent` does not change. Internal state
   uses `rememberSaveable(draft.id)`: `expanded` (starts as `draft.editing`) and `openSheet: TaskEditorSheet?`.
   - Chips: when (deadline/date/time summary), repetition, list, timer toggle and duration while timed, "Mais".
   - Rows (expanded): ending (only when repeating), description, checklist (only when not timed), importance,
     difficulty (only when `onDifficulty != null`); `FinancialEstimate` below the rows when game mode is on.
   - Sheets reuse the current blocks: `DateField`/`TimeField` (When), the repeat sub-options (Repeat), the ending
     options (Ending), `ChecklistEditor` (Checklist), `NumberStepper` (Duration), radio lists (List, Importance,
     Difficulty), a multiline field (Description). The deadline and repeat click handlers move unchanged into
     these sheets.
   - The edit-only `ScopeChoice` and the review checkbox are rendered inline at the end of the content.
4. **Habit editor**: `HabitEditor(state, lists, actions)` keeps its signature.
   - Chips (good habit): frequency, days (chosen days, or timed per day), times (weekly/monthly practice),
     timer toggle and minutes while timed, color and icon. Chips (bad habit): avoid/limit, limit (max per period,
     one chip and one sheet), color and icon.
   - Rows: list, description, importance, difficulty (game mode). The edit-only future type hint and goal effect
     are inline above Save.
   - The rule changes move to `HabitEditorPresentation.kt`: `PracticeFrequency` enum with `practiceFrequencyOf`
     and `withPracticeFrequency`, `withTimed`, `withBreakingType`, `toggleHabitWeekday`. They keep exactly the
     current values (targets "1"/"3"/"10", weekdays 127/0b0011111, MONTH→DAY when timed, AVOID target "0").
5. **Focus and keyboard**: when creating, the name `FocusRequester` requests focus and shows the keyboard. "Mais"
   clears focus and hides the keyboard before expanding.
6. **Errors**: the existing `error()` / `HabitError` slot is the first item of the content in both states.

## Complexity Tracking

None. No constitution violation.
