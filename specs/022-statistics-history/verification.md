# Verification: spec 022 — Statistics page and erasing history

Date: 2026-11-17. Environment: Windows 10, Gradle wrapper, no emulator or device used.

## Executed
- `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin`:
  BUILD SUCCESSFUL.
- JVM tests: 210 run, 0 failures. New: `StatisticsPeriodTest` (8), `StatisticsSummaryTest` (10),
  `StatisticsHistoryTest` (9), `TaskEraseTest` (6). `AccountingPresentationTest` removed with `historyDays`
  (the coin history moved to the Statistics page; its grouping is covered by `StatisticsHistoryTest`).
- Lint: no new warnings. Remaining warnings are pre-existing (dependency/AGP versions, launcher icon shape,
  data extraction rules). The now-unused `ic_history` drawable was removed.

## Written, not executed (need explicit authorization)
- `app/src/androidTest/.../core/history/HistoryErasureTest.kt`: erasing a done task keeps balance/XP and entry
  count, clears ledger titles, removes the task rows and its timer session; delete without erase keeps history;
  erasing a habit keeps the wallet and removes habit rows.
- `ShellNavigationTest` updated to navigate to "Hábitos excluídos" instead of the removed coin history page.

## Not verified
- Manual scenarios of `quickstart.md` on a device/emulator, TalkBack by audio, 200% font, dark theme rendering.
- Performance of "Tudo" with years of real data (computation runs on `Dispatchers.Default`, only while the page
  is visible; history paged by 50).

## Data safety
- No Room schema change (still 7); no migration needed. Existing data is only read, except when the person
  explicitly confirms erasing the history of a deleted task/habit.
