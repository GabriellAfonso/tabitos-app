# Verification: Game Mode Toggle (2026-11-11)

## Executed

- `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin`:
  passed.
- JVM: 155 tests, 0 failures. New:
  - `GameModeScoringTest`: no entry while off; avoid event while off has no penalty; outcome from off is never
    back-paid; paid outcome undone while off is reversed (and re-check while off pays nothing); outcome from off
    undone and redone while on pays once; moving a paid practice day while off keeps its award.
  - `GameModeRepositoryTest`: default on, persisted across store instances, theme untouched.
  - `GameModeViewModelTest`: off asks confirmation, cancel keeps on, on applies directly, blocked while a reward
    timer counts (task timer does not block).
  - `TasksViewModelTest` / `HabitsViewModelTest`: no financial confirmation while off.
  - `ShellModelsTest`: drawer hides Rewards/History only when off.
- Lint: no new warnings (15 pre-existing: dependency versions, manifest).

## Not executed

- Instrumented tests (only compiled; constructors updated to `FixedGameMode(on = true)`), emulator/device checks,
  TalkBack and large-font checks of the switch and dialog (standing user rule: needs explicit authorization).
- Manual quickstart scenarios in `quickstart.md`.

## Notes

- No Room schema change; outcomes recorded while off use the existing exempt entitlement state.
- The debug demo restore always seeds a scored wallet and never writes the mode preference.
