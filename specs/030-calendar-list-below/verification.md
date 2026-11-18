# Verification: Calendar with compact grid and day list below (2026-11-18)

## Executed

- `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin`:
  passed.
- JVM tests: 270 passed, 0 failed. New: `CalendarPresentationTest` (8: period labels, "Ir para hoje" visibility,
  active filter chips and removal, inbox/removed list, type toggle, habits offered in the sheet, task meta,
  avoid-habit records and separate day marks). `CalendarModelTest` (4) unchanged and passing.
- Lint: no new warnings (remaining ones are the existing Gradle/AGP, launcher icon, data extraction and
  `PluralsCandidate` warnings).

## Not executed

- Instrumented tests were not run (needs explicit authorization, AGENTS.md). `TrackerUiTest
  .calendarCellsDescribeContentAndDayShowsNoData` was updated to the new rows ("Feito", "Preencher") and compiles.
- Manual checks on a device: grid plus the start of the day list without scrolling, font at 200% (the grid scrolls
  sideways as before), TalkBack on cells, filter chips and the agenda, dark theme.

## Notes

- No change to rules, repositories or schema (Room 7). `CalendarDay.tracked` and `HabitDayRecords.breaking` are
  derived in `CalendarModel` from existing data.
- Removed strings used only by the old layout: `calendar_types`, `calendar_week_range`, `calendar_habit_records`,
  `calendar_tracker_fill`. New drawable `ic_filter_list`.
