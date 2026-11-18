# Verification: Statistics stay fast with a large history (spec 031)

**Date**: 2026-11-18

## Executed

- `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`: passed.
- JVM tests: 274 passed, 0 failed. New `StatisticsLargeHistoryTest` (4):
  - prepared content equals a fresh computation for every period kind (plus a past month and year), filter,
    item and a larger page, with and without game mode (FR-005, SC-003);
  - open on 5 synthetic years under 1 s (measured 265 ms alone, 655 ms inside the full run) (SC-001);
  - slowest period/filter/item/page change under 100 ms (measured 3–8 ms) (SC-002);
  - a running stretch's checkpoint does not change what statistics read; closing it does (SC-004).
- Existing statistics, habit rule and habit ViewModel tests pass unchanged (results identical).
- Lint: no new warning.

## Not executed

- Phone timing of Estatísticas with real data (manual check).
- Instrumented tests: none added; none run.

## Notes

- `HabitRules.periods` was optimized (same output); Hoje and habit cards use it as well.
- No schema change.
