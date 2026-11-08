# Validation guide

1. `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
   :app:assembleDebugAndroidTest --console=plain`; `connectedDebugAndroidTest` when an emulator is available.
2. JVM: TimerRules with fake clock — pause excluded, cap, reboot recovery, 40+50+90 = 3 h, Sunday 23:50→Monday 00:20
   split 10/20, midnight and DST splits, manual validation and overlap.
3. Room: migration 1→…→6 and 5→6 preserve rows; start/conflict/pause/resume single running; timed purchase with and
   without pauseRunning, repeated confirm → 1 ledger entry, 1 purchase, 1 session; resume no allowance.
4. Runtime: start task timer, lock screen, notification Pause; recreate Activity; kill process and reopen;
   simulate reboot via boot-count mismatch test.
5. Reach a 1-minute goal: pauses, alerts, confirmation opens checklist/financial flow; second confirm awards nothing.
6. Timed reward 1 minute with another timer running: cancel (no charge), confirm (pause + charge + countdown), pause,
   resume, exhaust alert.
7. Deny notifications: in-app controls still work with explanation. Light/dark, font 200%.
8. Record evidence and uncovered API 24/physical/TalkBack/OEM limits in verification.md.
