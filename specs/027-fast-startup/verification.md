# Verification: Fast app startup (spec 027)

**Date**: 2026-11-17

## Executed

- `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease`: passed.
- JVM tests: 243 passed, 0 failed. New: `StartupSequenceTest` (5), `TodayReadinessTest` (4), `ChangedRowsTest` (5).
  - `ChangedRowsTest` saves create, edit, checklist check, resolve, set completed, postpone, order, move, delete
    (this / series with history erased), delete list and refresh both as the former full rewrite and as changed
    rows only, over a table model with the DAO's upsert and checklist-replace semantics: tables identical in every
    case; a resolve writes one occurrence and one audit row; a no-change refresh writes nothing.
- Lint: no warning in files touched by this spec (existing warnings are in the version catalog, strings and
  launcher resources).

## Not executed

- Instrumented tests (`ChangedRowsDatabaseTest`: stored tasks match the rules after each action; no-change
  refresh/settle wakes no observer) were written and compiled, not run: they need explicit authorization for the
  emulator.
- SC-001 (time to Hoje before/after) needs the user's phone. APKs, outside the repository (the session scratchpad;
  it can be cleared by a reboot):
  - baseline (measurement only): `C:\Users\gabri\AppData\Local\Temp\claude\5b462ba63dd21399\03fbe4d9-9d8c-418d-ae4c-94351915da48\scratchpad\ethos-027-baseline-debug.apk`
  - final: `C:\Users\gabri\AppData\Local\Temp\claude\5b462ba63dd21399\03fbe4d9-9d8c-418d-ae4c-94351915da48\scratchpad\ethos-027-final-debug.apk`
  Steps in [quickstart.md](quickstart.md). Install only with `adb install -r` over a debug install signed on this
  machine; never uninstall.
- SC-003/SC-005 (pages preloaded after 5 s; notifications and links unchanged): manual checks on the phone pending.

## Results to fill in

| Build | Baseline median | Final median |
|-------|-----------------|--------------|
| debug | — | — |
