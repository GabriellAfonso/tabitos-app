# Implementation Plan: Task Editor Pickers and Quick Capture

**Branch**: `master` (no branch hook) | **Date**: 2026-11-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/013-task-editor-pickers/spec.md`

## Summary

Interface-only change to the task flow. The full-screen task editor shows quick-capture fields first and
the rest under "Mais opções"; dates and times come from Material 3 pickers. A pure date formatter in
`core/ui/format` gives the same pt-BR wording to task rows, task detail and the habit calendar title. Pure
presentation functions map the 5-option repetition choice to the 8 existing `RepeatKind` values, decide
when the review confirmation is needed and when "Mais opções" starts expanded. The `TaskDraft` keeps its
ISO text fields as the internal value written by pickers, so `TasksViewModel.saveDraft`, `TaskRules`,
Room and accounting stay unchanged. The ViewModel gains only discard-confirmation state.

## Technical Context

**Language/Version**: Kotlin 2.3.10 (AGP 9 built-in Kotlin)

**Primary Dependencies**: Compose BOM 2026.02.00, Material 3 (`DatePicker`, `DatePickerDialog`,
`TimePicker`), Hilt, Lifecycle 2.10. No new dependency.

**Storage**: Room schema 7 unchanged; `SavedStateHandle` keeps the draft (already serializable).

**Testing**: JUnit4 JVM tests for pure functions and ViewModel; Compose instrumented tests on API 36
emulator for editor flow.

**Target Platform**: Android, minSdk 24, verified on API 36 emulator.

**Project Type**: Single-module Android app (`:app`).

**Performance Goals**: Editor opens and pickers respond without visible delay (no I/O in pickers).

**Constraints**: Offline; no schema/rule change; pt-BR resources; 48 dp targets; 200% font scale.

**Scale/Scope**: ~6 Kotlin files touched, 2–3 new small files, strings updated.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- I: Spec 013 exists with scope, exclusions and acceptance; user explicitly requested plan, tasks and
  implement in this session (2026-11-10). ✅
- II: No persistence change; saves go through existing `TaskAction.Create/Edit`; Room schema 7 untouched;
  failures keep existing retry card. ✅
- III: Composables only render and call callbacks; mapping/visibility logic in pure Kotlin functions
  without Android types; formatter in `core/ui/format` because tasks and habits both use it (no feature
  imports another feature). No new layer or dependency. ✅
- IV: Texts in `strings.xml`; date fields and checklist controls get content descriptions; editor scrolls
  at 200% font; light/dark via existing theme; no personal data logged; no permission. ✅
- V: JVM tests for formatter, repeat round-trip (8 kinds), review visibility, advanced-expanded rule and
  discard state; instrumented test updated for the new editor; limitations recorded in verification. ✅

Post-design re-check: same result; no violations, Complexity Tracking not needed.

## Project Structure

### Documentation (this feature)

```text
specs/013-task-editor-pickers/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/ui-contract.md
├── checklists/requirements.md
└── tasks.md            # next step
```

### Source Code (repository root)

```text
app/src/main/java/com/ethos/app/
├── core/ui/format/EthosDates.kt            # new: pure pt-BR date/week-range formatting
├── features/tasks/
│   ├── TaskEditor.kt                       # full-screen editor, quick + "Mais opções"
│   ├── TaskEditorPresentation.kt           # new: repeat choice mapping, review/expanded rules
│   ├── TaskPickers.kt                      # new: DateField/TimeField with M3 pickers
│   ├── TasksContent.kt                     # row/detail date text, completion date picker
│   └── TasksViewModel.kt                   # discard confirmation state
├── features/habits/HabitsContent.kt        # month title format only
└── res/values/strings.xml                  # new/changed labels, removed obsolete ones

app/src/test/java/com/ethos/app/
├── core/ui/format/EthosDatesTest.kt
├── features/tasks/TaskEditorPresentationTest.kt
└── features/tasks/TasksViewModelTest.kt    # discard flow

app/src/androidTest/java/com/ethos/app/features/tasks/
├── TasksContentTest.kt                     # updated capture flow
└── TaskEditorTest.kt                       # new: quick capture, Mais opções, review visibility
```

**Structure Decision**: Keep everything in the existing `features/tasks` package plus one shared pure
formatter in `core/ui/format`, used by tasks and habits.

## Complexity Tracking

None.
