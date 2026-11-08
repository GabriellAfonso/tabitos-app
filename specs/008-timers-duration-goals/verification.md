# Verification: Cronômetros, metas de tempo e recompensas temporizadas

Date: 2026-11-07 (emulator clock 2026-11-08). Environment: Windows 10, Gradle wrapper, emulator Pixel_5 AVD API 36
(Android 16). No physical device.

## Executed
- `:app:assembleDebug :app:testDebugUnitTest :app:assembleRelease :app:assembleDebugAndroidTest`: success.
- `:app:lintDebug`: success; no new warnings (remaining: pre-existing Gradle/AGP version, data extraction, launcher
  icon, "momento" typo). A parallel run once failed reading Hilt release sources being regenerated; lint alone passed.
- JVM: 93 tests pass, including `TimerRulesTest` (7: pause exclusion with clock jump, reboot checkpoint recovery with
  and without boot count, exact cap, 40+50+90 = 3 h and next week empty, Sunday 23:50→Monday 00:20 split 10/20 by day
  and week, DST day in New York, manual validation for invalid/future/>24 h/before source/inactive/overlap) and
  `RewardsViewModelTest` timed purchase consent (ask before charge, cancel charges nothing, non-timed unchanged).
- Instrumented on API 36: full suite 52/52 pass (previous 44 + 8 new), affected classes re-run 21/21 after formatting.
  - `TimerDatabaseTest`: migration 5→6 preserves every column of every schema-5 table and adds NULL durations; empty
    timer tables; one running timer, CONFLICT without consent, pause/resume same session, reboot recovery pauses at
    last checkpoint (powered-off hour not counted); 1-minute goal caps at 60 s, alert once, start refused after
    reach; manual add, overlap rejected, correction audited before/after, deletion; timed purchase returns
    RUNNING_CONFLICT uncharged, consented purchase pauses the task, 1 debit/1 purchase/1 reward session across retries,
    resume without repurchase or allowance use, exhaustion at purchased 2 min, no refund.
  - `TimerRuntimeTest`: real foreground service (specialUse) started; ongoing notification private, ongoing, with
    chronometer, title and Pausar/Abrir; Pausar action persists pause and service stops; a 1-minute objective
    stops exactly at 60 s in background (no UI) with one alert notification; task stays OPEN and no award.
    `dumpsys notification` confirmed `showChronometer=true`, `vis=PRIVATE`, `FOREGROUND_SERVICE`.
  - `TimerUiTest`: session cards at font scale 2.0 show elapsed/remaining and Pausar/Retomar callbacks; reward
    pause-consent dialog Voltar does not confirm.
  - Existing reminder/reward/accounting/habit/task tests updated with migration 5→6 and pass.
- Fresh debug install launches to Hoje without crash (screenshot).

## Notes and limits
- In `TimerRuntimeTest` within the full suite, the first notification update took longer than 10 s because the test
  uses a second Room instance in the app process; wait raised to 30 s. Not observed as product behavior.
- `ReminderRuntimeTest` (feature 007) failed once on real alarm timing and passed on re-runs; unrelated to timers.
- Not validated: API 24–33 runtime (Android < 14 FGS without type, < 13 notification permission), physical device,
  OEM battery restrictions, force-stop, actual power-off reboot (simulated by boot-count/monotonic reset in tests),
  manual clock/time zone change on device, TalkBack audio, landscape/dark screenshots of the timer sheet, and end-to-end
  UI flow from task detail through completion confirmation (covered by units: routing uses existing
  `TasksViewModel.notification` and `HabitsViewModel.recordDuration`).
- Checkpoints every 10 s: after a kill followed by reboot at most ~10 s of real activity may be uncredited.
- Integration assumptions (documented in spec/research, not separately approved): habit duration goals apply to
  practice habits with one practice per day/week; sessions are source-bound; manual overlap is checked globally.
