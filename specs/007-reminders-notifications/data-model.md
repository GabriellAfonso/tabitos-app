# Data model

Additive schema5, preserving schemas1–4 and all accounting/history.

- ReminderDefinition: id PK, groupId, kind TASK/HABIT, sourceId task definition/habit,
  occurrenceId nullable, fromOrdinal, enabled, mode CHOSEN/RELATIVE/WEEKDAYS, anchorDay displacement,
  chosenDate nullable, minute0..1439, weekdays1..127, offsetMinutes>=0, referenceMinute nullable, createdAt.
  Per-group ordinal precedence plus exact occurrence override; disabled overrides suppress inherited rule.
  This-and-future edits replace overrides from that ordinal; earlier scopes remain untouched.
- ReminderDelivery: key PK, groupId, kind, sourceId actual occurrence/habit, periodKey, slotDate,
  anchorDate, dueAt millis, snoozed, state PENDING/POSTED/RECOVERED/CANCELLED.
  Stable key prevents slot replay, replace snooze in same row. Terminal records retained.
- ReminderSettings: id1 PK, summaryMinute nullable (disabled), lastSummaryDate nullable.
  Stored with structured delivery state; no duplicate appearance preference storage.

No destructive cascade to source history. Transactional validated writes. Civil pending instants recomputed
after zone/clock changes; snoozed instants remain absolute. String notification tags avoid hash collisions.
