# Verification — spec 021 (2026-11-16)

## Executed

- `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin`:
  success. 176 JVM tests, 0 failures, including `TaskPostponeTest` (6) and `ReminderPostponeTest` (4).
- Lint: 21 warnings, all pre-existing types (AGP/dependency versions, launcher icon, data extraction rules);
  the one new warning (PluralsCandidate) was fixed.
- Instrumented tests updated for the new labels ("Concluir", "Desfazer") and compiled; not executed.
- Debug build installed on the user's phone with `adb install -r` at the user's request; update kept the original
  install (first install 2026-11-07), no schema change, user data preserved. Not opened or exercised by the agent.

## Not executed

- Instrumented tests (`connectedDebugAndroidTest`), emulator, manual walkthrough of quickstart.md on the device.
- TalkBack, 200% font and dark theme checks of the new popup and sheet.

## Notes

- The same install also carries the uncommitted spec 014 change (avoid/limit streak counts ended days in the
  late window; habit detail calendar shows all seven weekdays).
