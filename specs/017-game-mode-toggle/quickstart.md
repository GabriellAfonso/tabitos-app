# Quickstart: Game Mode Toggle

## Automated

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Expected: build passes; new JVM tests (accounting scoring OFF, cross-mode reversal, no retroactive pay,
ViewModels skip financial confirmation, Settings switch confirmation/block, preference persistence) pass;
no new lint warnings. Instrumented tests only compile (`assembleDebugAndroidTest` when needed); running them
needs explicit authorization.

## Manual (device/emulator, when authorized)

1. Fresh install: Settings shows "Modo jogo" ON; Hoje shows the wallet.
2. Turn OFF: dialog appears; confirm. Drawer has no Recompensas/Histórico, Hoje no wallet, Settings no
   prices, task/habit editors no difficulty/estimate.
3. Complete a task, mark a habit, register an avoid occurrence: no dialog about coins.
4. Turn ON: balance/XP equal to step 1 values.
5. With ON, complete a task; turn OFF; uncheck it: no dialog; turn ON: balance back to before the task.
6. Start a timed reward purchase; Settings switch is disabled with explanation.
7. Kill the app and reopen: mode persisted; theme unchanged.
