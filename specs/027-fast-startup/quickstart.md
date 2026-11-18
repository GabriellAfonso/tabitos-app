# Quickstart: validating spec 027

## Automated (always)

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Expected: build passes; JVM tests for `StartupSequence`, Hoje readiness, destination mapping and changed rows pass;
no new lint warning.

Instrumented tests (`connectedDebugAndroidTest`) for the Room save behavior are written but run only with the
user's explicit authorization.

## Measuring startup (on the user's phone)

Warning: install only over the same build type already installed. Never uninstall, and never install a release APK
over a debug install (or the reverse): a signature change forces an uninstall, which erases the routine.

1. Install the baseline APK (measurement only) with `adb install -r <baseline.apk>`.
2. Force-stop the app, open it from the launcher, and read the line
   `adb logcat -d | findstr "Fully drawn"` → `Fully drawn com.ethos.app/.MainActivity: +X ms`.
3. Repeat 5 times; record the median.
4. Install the final APK with `adb install -r`, repeat steps 2–3.
5. Record both medians in `verification.md`.

## Manual checks

- Cold open: Hoje shows one loading text, then tasks, habits, timers, pending tracker card and wallet together.
- Wait 5 s on Hoje, open Calendário, Recompensas and Configurações: no loading state.
- Cold open and immediately open Recompensas: it loads at once and shows the usual content.
- With the app closed, tap a task reminder, a reminder's "Concluir", a tracker reminder and a running timer
  notification: same behavior as before.
- First open of a new day: today's recurring occurrences appear right after Hoje.
