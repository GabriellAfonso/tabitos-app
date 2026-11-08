# Research: Timers, duration goals and timed rewards

Sources: current Ethos code (2026-11-07), Android developer documentation on foreground service types
(Android 14+ mandatory `foregroundServiceType`, `specialUse` with `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`),
background start restrictions (Android 12+), `POST_NOTIFICATIONS` (Android 13+), `SystemClock.elapsedRealtime`,
`Settings.Global.BOOT_COUNT` (API 24+) and `Notification.Builder.setChronometerCountDown` (API 24+).

## R1 Elapsed time source
- Decision: persist each counted segment with wall start (epoch ms, for civil allocation), monotonic start
  (`elapsedRealtime`), boot count and a monotonic checkpoint. Duration of a running segment is
  `elapsedNow - startElapsed` while the boot count matches; otherwise `checkpoint - startElapsed`.
  The end wall instant is always `startWall + duration`. Millisecond integers; display in seconds.
- Rationale: monotonic clock is immune to manual clock/time zone changes and excludes powered-off time
  after a reboot (counter resets); checkpoint gives the last known progress.
- Alternatives: wall-clock difference (breaks on clock changes, counts power-off); UI ticker (stops in
  background, rejected by spec); WorkManager tick (minimum 15 min, explicitly rejected).

## R2 Reboot detection
- Decision: compare stored `Settings.Global.BOOT_COUNT`; when unavailable (-1), treat `elapsedNow < checkpoint`
  as a new boot. On mismatch close the segment at its checkpoint, pause the session and audit RECOVERED.
- Rationale: deterministic, testable with a fake clock; no boot receiver needed because recovery is lazy
  (the next coordinator sync after boot or app launch). Alarms/notifications do not survive reboot anyway.

## R3 Background execution
- Decision: one `TimerService` foreground service while a session is running, type `specialUse`
  (subtype "user-started activity stopwatch/countdown") on API 34+, permissions `FOREGROUND_SERVICE` and
  `FOREGROUND_SERVICE_SPECIAL_USE` (install-time). It checkpoints every 10 s and on `ACTION_SHUTDOWN`
  (dynamic receiver), updates the ongoing notification and pauses exactly at the segment cap.
  `START_STICKY`; started only from foreground user actions or app launch (exceptions swallowed and
  surfaced honestly when the OS refuses). Notification uses a system chronometer (count-up or count-down).
- Rationale: timestamps alone keep time correct, but checkpoints for reboot and an exact objective stop/alert
  need a live process; FGS is the documented mechanism for user-visible ongoing work. No other FGS type
  fits a generic activity stopwatch (not media, health sensor, location or data sync); `shortService`
  stops after ~3 min. No exact alarm or battery exemption is requested.
- Limits: force-stop or OEM killing stops checkpoints; at most 10 s after the last checkpoint is lost after
  power loss/kill followed by reboot. Without notification permission the service still runs (Android 13+
  shows it only in Task Manager); the app explains this.

## R4 Objective cap
- Decision: each running segment stores `limitMillis` = remaining objective when it started
  (task goal remaining, habit period remaining in the start period, purchased time remaining). Duration is
  `min(raw, limit)`, so recovery after kill/reboot never overruns. Reaching the cap closes the segment,
  pauses the session, sets `reached` and posts an alert on a separate default-importance channel.
- Crossing a period boundary does not reset the cap; allocation still splits by real date (R5).

## R5 Civil allocation
- Decision: pure function splits `[startWall, endWall)` at local midnights in the device zone current at
  computation; weekly period = Monday–Sunday (`TemporalAdjusters.previousOrSame(MONDAY)`, as rewards/habits).
  DST is handled by `ZonedDateTime` arithmetic on real instants.

## R6 Goals mapping (minimal integration assumption)
- Task goal: optional minutes per task definition (applies to every occurrence); progress = active segments of
  that occurrence. Confirmation calls existing `TasksViewModel.notification(id, complete = true)`
  (checklist + financial preview + `TaskAction.Resolve`). Already resolved occurrence → NOT_OPEN, no award.
- Habit goal: only PRACTICE habits whose current rule is DAY or WEEK with target 1; progress = allocated
  minutes inside the current period. Confirmation records one practice on the reaching date via existing
  `HabitAction.Record` behind the financial preview; skipped when a record already exists that date.
- Goals live in a separate `timer_goals` table, so task/habit schemas and history are untouched.

## R7 Manual time and overlap
- Decision: manual/corrected intervals must have start < end ≤ now, length ≤ 24 h, start on or after source
  creation, habit dates inside active intervals, and no overlap with any other active segment (global,
  mirroring the single-running-session rule). Correction deactivates the old segment and inserts a new one
  (audit before/after); deletion deactivates. Timed segments after a backward clock change may overlap in wall
  time; durations stay correct (documented).
- Correction that makes a completed source fall below its goal shows a consequence and an Abrir action into the
  existing Undo/record-correction flow; nothing is reverted automatically.

## R8 Timed rewards
- Decision: `Reward.minutes: Int?` embedded (column `reward_minutes`) in rewards, intents and purchase snapshots.
  `confirm(expected, pauseRunning)` in one Room transaction: if another session is running and
  `pauseRunning` is false → `RUNNING_CONFLICT` without charge; otherwise pause it, debit, insert purchase and
  insert running session `purchase:<intentId>`. Retries return PURCHASED by existing receipt check; session id
  is deterministic so no second timer. Service start happens after commit and is recovered by the coordinator.
- Resume of reward sessions consumes no allowance (allowance counts purchases only).

## R9 Notifications/navigation
- Reuse `MainActivity` singleTop with a new `ethos-timer://local/OPEN/<sessionId>` URI; Pause uses a non-exported
  broadcast receiver. Channels `ethos_timers` (low, ongoing) and `ethos_timer_alerts` (default). Private
  visibility with generic public version, as reminders do.
