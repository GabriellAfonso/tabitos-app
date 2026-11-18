# Verification: Growing editor sheet (2026-11-17)

## Executed

- `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`: passed.
- JVM tests: 262 passed, 0 failed. New tests: `HabitEditorPresentationTest` (6) and
  `TaskEditorPresentationTest` (3 new; the `hasAdvancedOptions` test was removed with the function).
- Lint: no new warnings. The remaining warnings are the existing ones (Gradle/AGP versions, launcher icon, data
  extraction rules, `PluralsCandidate` on strings from earlier specs). The resources only the old editors used
  were removed (`fewer_options`, `variant`, `variant_normal`, `habit_mon`…`habit_sun`, `ic_keyboard_arrow_up`).
- `:app:compileDebugAndroidTestKotlin`: compiles with the updated instrumented tests.

## Not executed

- Instrumented tests (`TaskEditorTest`, `TasksContentTest`, `HabitsContentTest`) were updated but not run.
  Running them needs explicit authorization (AGENTS.md).
- Manual checks from `quickstart.md` on a device or emulator: keyboard behavior when the compact sheet opens,
  font at 200%, TalkBack reading of chips and rows, dark theme.

## Notes

- No change to drafts, ViewModels, rules or schema (7). The habit rule mapping moved from Composables to
  `HabitEditorPresentation.kt` with the same written values, pinned by tests.
- `ScopeChoice` now uses radio rows. The list editor dialog (`TasksContent`), which also uses it, shows the same
  radio rows.
