# Research — 2026-11-07

Read-only research agent required by local plan skill consulted current official docs.

- Decision: one-shot AlarmManager exact idle when API<31 or canScheduleExactAlarms, otherwise inexact idle
  with explicit approximate status. Contextual SCHEDULE_EXACT_ALARM special access, not USE_EXACT_ALARM.
  Rationale: user-selected times; WorkManager is deferrable. No FGS/battery exemption.
  [Scheduling](https://developer.android.com/develop/background-work/services/alarms),
  [AlarmManager](https://developer.android.com/reference/android/app/AlarmManager).
  Even exact idle alarms are throttled and device delivery is not guaranteed.
- Decision: POST_NOTIFICATIONS contextually on33+, app + channel capability checks and channel settings links.
  Separate stable Lembretes/Resumos channels respecting user choices.
  [Permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission),
  [Channels](https://developer.android.com/develop/ui/compose/notifications/channels).
- Decision: direct immutable explicit activity PendingIntent for each action; never receiver→startActivity.
  [Trampolines](https://developer.android.com/about/versions/12/behavior-changes-12#notification-trampolines),
  [Security](https://developer.android.com/privacy-and-security/risks/pending-intent).
- Decision: boot after unlock, own package replacement, TIME_SET/TIMEZONE_CHANGED, access-grant broadcast,
  foreground and next-midnight reconciliation. No reliance on manifest DATE_CHANGED.
  [Broadcast exceptions](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions),
  [Intent](https://developer.android.com/reference/android/content/Intent).
- Limitation: force-stop blocks autonomous execution; Android15 also cancels PendingIntents. Reopen rebuilds.
  [Stopped state](https://developer.android.com/about/versions/15/behavior-changes-all#stopped-state).
- Application policies (inference, not Android mandates): DST gap shifts forward; overlap earlier offset once;
  snooze absolute instant;2min individual grace; stable recovery tag with onlyAlertOnce for crash retries.
  OS display and Room cannot share transaction, so do not claim exactly-once physical display.
- Repository inspection: current source schema4/rewards implemented, TaskRules generates next occurrence,
  shared domain contracts and completion financial preview exist. Preserve these rather than parallel engines.
