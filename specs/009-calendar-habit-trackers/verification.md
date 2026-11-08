# Verification: Calendário completo e acompanhamentos personalizados

Date: 2026-11-07 (emulator clock 2026-11-08). Environment: Windows 10, Gradle wrapper, emulator Pixel_5 AVD API 36
(Android 16). No physical device.

## Executed
- `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease :app:assembleDebugAndroidTest`:
  success after the last code change.
- `:app:lintDebug`: no new warnings. Remaining are pre-existing (Gradle/AGP versions, data extraction rules, launcher
  icon, "momento" typo). Three new warnings found during the run (unused string, two plural candidates) were fixed.
- JVM: 108 tests pass (93 previous + 15 new):
  - `TrackerRulesTest` (9): four question types, required flags, default 0–10 and custom range, primary kept in
    place, invalid structures rejected (empty, scale range, two primaries, choice without options, blank title,
    excluded habit); reference date vs filledAt, creation..today limits, single-mode duplicate rejected and edit keeps
    filledAt/dayKey; multiple-mode individual entries; answer validation per version; versioning (identical re-save
    creates no version, old entry edited with version 1 without the later required question, removed option still
    valid in v1 and invalid in v2, historical label in frequency); analytics gaps, no averaging, range-change flag,
    90-day window; archive/restore, paused/excluded rules, pending state; habit snapshot untouched.
  - `CalendarModelTest` (4): Monday-first month/week ranges and shifts; task parity with the former `inDay` lookup
    over 11 days; type/list/inbox/habit filters combined; tracker entries, fill targets (existing entry in single
    mode), no fill before creation or in the future.
  - `ReminderRulesTest` (+2): tracker slots daily for today..+32, relevant only on their own date (one pending slot
    after 3 days of downtime), cleared by an entry, none for paused habit or archived tracker.
- Instrumented on API 36: full suite 58/58 pass (52 previous + 6 new); `TrackerUiTest` re-run 3/3 after the final
  layout change.
  - `TrackerDatabaseTest`: 6→7 migration preserves every column of every schema-6 table and creates empty tracker
    tables; existing tests now open through the full 1→7 chain (`HabitDatabaseTest` from version 1). Repository stores
    versions/options/descriptions/order, answers with multiple choices, raw duplicate insert rejected by the unique
    day index, DUPLICATE rule, deletion removes answers; habit snapshot and `accounting_ledger` count unchanged.
  - `TrackerReminderRuntimeTest`: real `ReminderCoordinator` + `ReminderPlatform`: due tracker slot posted once with
    title "Ethos", text "Você tem um acompanhamento para preencher", no habit/question text, `FLAG_ONGOING_EVENT`,
    private visibility, actions Preencher/Adiar; simulated dismissal is not reposted and in-app pending remains;
    saving an entry cancels the delivery and removes the notification.
  - `TrackerUiTest`: entry form at font scale 2.0 (scale value buttons with content descriptions, extremes, multiple
    choice with descriptions); builder adds the four types, primary exclusive without reordering, default "Um por
    dia"; calendar cells announce counts / "sem dados", tap selects date, day shows "Sem dados", habit records and
    "Preencher acompanhamento".
- Debug APK installed and launched; Calendário tab screenshot shows month grid, view chips and filters fitting the
  screen (light theme).

## Notes and limits
- Not validated: API 24–33 runtime (below Android 14 ongoing notices are not swipe-dismissible; below 13 no runtime
  notification permission), physical device, OEM battery restrictions, real reboot (covered by rule tests of
  today-only relevance), manual time zone change, TalkBack audio, dark theme screenshots, and the end-to-end tap on
  the notification Preencher action (routing covered by code path `ReminderViewModel.request` → `fill`; the action
  URI is asserted indirectly by action titles).
- Notification dismissal on Android 14+ cannot be prevented; the app keeps the pending state in Hoje and in the
  tracker hub (FR-027). No foreground service is used.
- Integration assumptions recorded in spec: one daily time; multiple mode resolved by one entry; snooze must stay on
  the same civil day (crossing midnight is rejected as invalid input); past days never stay pending as notices.
- Removal is implemented as archive/restore (history read-only); only individual entries can be deleted.
- Open item unchanged: backup/data-extraction rules (`allowBackup=false` today).
