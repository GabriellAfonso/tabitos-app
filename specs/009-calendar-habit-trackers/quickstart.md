# Validation guide

1. `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
   :app:assembleDebugAndroidTest --console=plain`; `connectedDebugAndroidTest` with the Pixel_5 API 36 emulator.
2. JVM `TrackerRulesTest`: four question types, required flags, primary default 0–10 and custom range, at most one
   primary not moved, reorder kept; option create/rename/reorder/remove with historical label; versioning (old entry
   edit uses old version without new required question); date limits creation..today; single-mode duplicate
   rejected, multiple-mode individual entries; analytics gaps, no averaging, range-change flag, option counts.
3. JVM `CalendarModelTest`: month/week cells Monday-first, parity with old `inDay` lookup, filters by type/list/habit,
   tracker indicator, "Sem dados" flag. `ReminderRulesTest`: tracker schedule only fillable days, relevance cleared
   by an entry, past slots cancelled, paused habit/archived tracker stop.
4. Room `TrackerDatabaseTest`: 6→7 migration preserves every schema-6 row and creates empty tracker tables; full chain
   1→7 opens; unique day index; tracker actions leave ledger/habit records unchanged.
5. UI: builder with the four types, entry form with scale buttons at font scale 2.0, calendar cells with descriptions,
   day sheet "Sem dados". Runtime: tracker notification neutral, ongoing, FILL/SNOOZE actions; saving removes it.
6. Manual: Calendário tab month/week/day, filters, open task/habit/tracker from a day; light/dark.
7. Record evidence and uncovered API 24/physical/TalkBack audio in verification.md.
