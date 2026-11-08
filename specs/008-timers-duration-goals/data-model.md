# Data model (Room schema 6)

Migration 5→6 is additive and preserving.

## Changed
- `rewards`, `reward_intents`, `reward_purchases`: new nullable `reward_minutes INTEGER` (embedded
  `Reward.minutes`). Existing rows keep NULL = non-timed; snapshots/prices unchanged.

## New tables
- `timer_goals(sourceKey PK TEXT, minutes INTEGER, updated INTEGER)`
  - sourceKey `task:<taskDefinitionId>` or `habit:<habitId>`; minutes 1..1440.
- `timer_sessions(id PK, kind TEXT, sourceId TEXT, title TEXT, state TEXT, limitMillis INTEGER?, created INTEGER,
  reached INTEGER, finished INTEGER, purchaseId TEXT?)`
  - kind TASK (sourceId = occurrence id) / HABIT (habit id) / REWARD (purchase id); state RUNNING / PAUSED / FINISHED.
  - limitMillis: total purchased time for REWARD; goals are read from `timer_goals` for the others.
  - Invariants (enforced in transactions): ≤1 RUNNING overall; ≤1 non-FINISHED per (kind, sourceId).
- `timer_segments(id PK, sessionId, startWall INTEGER, endWall INTEGER?, startElapsed INTEGER?, bootCount INTEGER?,
  checkpointElapsed INTEGER?, limitMillis INTEGER?, manual INTEGER, active INTEGER, created INTEGER)`
  - Running segment: endWall NULL, startElapsed/bootCount/checkpoint set. Closed: endWall = startWall + duration.
  - Index on sessionId. At most one open segment (only within the RUNNING session).
- `timer_audits(id PK, sessionId, kind TEXT, at INTEGER, segmentId TEXT?, beforeStart INTEGER?, beforeEnd INTEGER?,
  afterStart INTEGER?, afterEnd INTEGER?)`
  - kind START, PAUSE, RESUME, RECOVERED, REACHED, EXHAUSTED, MANUAL, CORRECTED, DELETED, FINISHED, CONFIRMED.

## State transitions
- none → RUNNING (start; previous RUNNING paused first when confirmed).
- RUNNING → PAUSED (user, notification, cap reached, source no longer open/active, reboot recovery).
- PAUSED → RUNNING (resume; blocked while reached/exhausted with no remaining).
- PAUSED → FINISHED (user finishes task/habit session, or goal confirmed). REWARD sessions never refund.

## Derived values (pure)
- duration(segment, clock), session total, source progress (task: all active segments of occurrence; habit: allocated
  inside current DAY/WEEK period), remaining = goal − progress, allocation by date/week.
