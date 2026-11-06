# Quickstart validation
Prerequisites: existing JDK17+ (21 verified), Android SDK36, Wrapper, dependencies downloadable.
Run from root:
```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
.\gradlew.bat :app:connectedDebugAndroidTest
```
Second command requires emulator/device. Install debug APK, launch com.ethos.app/.MainActivity offline.
Check all eight routes, repeated ten bottom taps then Back, drawer Back/selection, settings shortcuts,
all Hoje/review filters and absence of fabricated progress/notices. Rotate and restore filters/routes.
Select each appearance, force-stop/reopen and compare device dark/light override and system following.
Use 200% fonts, portrait/landscape, system bars, both appearances and screen-reader labels.
Inspect explicit Compose previews: populated Hoje/review/rewards/history/list tabs, pending zero/positive.
JVM tests inject read/write failures, retry, unknown value, temporary-file persistence/reopening.
See contracts/ui.md for expected behavior; record actual results/limitations in verification.md.
No publication/signing credentials needed; release remains unsigned.
