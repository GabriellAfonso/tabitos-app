# Verification: spec 014 — habits without debt

**Date**: 2026-11-10

## Executed

- `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin`
  — success.
- JVM tests: 134 passed, 0 failed. Rewritten for the new rules: open late window then missed close, avoid
  succeeds by itself after the window, late record only inside the window (records and corrections rejected
  after closing), off-day does not replace a missed day, open window neither breaks nor counts a streak,
  missed day breaks streak and keeps best, partial practice target defaults to available days, success
  rate uses closed periods only, missed practice has no penalty, legacy confirmed penalty stays, limit
  within target rewarded only after closing.
- Lint: no new warning from this feature (remaining: pre-existing unused `habit_*` color strings, one
  `ModifierParameter` in `TaskRow`, dependency notices).

## Not executed

- Instrumented tests (by standing user rule: only with explicit authorization). `HabitsContentTest` and
  `AccountingDatabaseTest` expectations were updated and compile; other instrumented tests that may depend
  on old habit review texts were not checked on a device.
- Manual checks of the habit editor, calendar marks, TalkBack and 200% font scale.

## Decisions taken during autonomous execution

- Avoid/limit per-event penalty kept (FR-007).
- "Quebrar hábito" added as third "+" option and on the Hábitos page.
- Penalties confirmed by legacy reviews stay in history (no refund); pending periods close without penalty.
