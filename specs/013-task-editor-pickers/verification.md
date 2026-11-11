# Verification: spec 013 — task editor pickers and quick capture

**Date**: 2026-11-10

## Executed

- `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` — success.
- JVM tests: 132 passed, 0 failed. New: `EthosDatesTest`, `TaskEditorPresentationTest` (8 kinds keep their
  rule, choice mapping, weekday guard, advanced rule, review visibility, checklist helpers, picker day
  conversion) and `TasksViewModelTest` (discard flow; editor round trip of all 8 recurrence kinds and
  checklist with identical stored rule).
- Lint: no new warning in touched files. Remaining warnings are pre-existing (unused `habit_*` color
  strings from spec 010, `ModifierParameter` in `TaskRow`, dependency/version notices).
- T001: Material 3 `DatePicker`, `DatePickerDialog` and `TimePicker` come from the existing Compose BOM
  2026.02.00; no dependency change.

## Not executed

- Instrumented tests (`TaskEditorTest` new, `TasksContentTest` updated). A first run on the API 36
  emulator stopped after 7 of 69 tests because the emulator closed (`device 'emulator-5554' not found`);
  no result for the new tests. By user decision (2026-11-10), instrumented tests only run when
  explicitly authorized.
- Manual quickstart scenarios, TalkBack and 200% font scale: not validated.
- Physical device: not used (intentionally).

## Notes

- Saving keeps `TaskRules` and Room unchanged; non-recurring rules are still normalized on save, as before.
- Checklist reordering keeps the existing rule: check state is preserved only when the item keeps index
  and label (spec 003 behavior).
