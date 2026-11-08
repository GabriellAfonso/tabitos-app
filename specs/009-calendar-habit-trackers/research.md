# Research: Calendário e acompanhamentos

## R1 — Form versioning
- Decision: every structural save creates an immutable version with full copies of questions and options; ids of
  questions/options are stable across versions; entries store `version`.
- Rationale: past entries render exactly their structure (FR-007/008); reorder of the current form cannot rewrite
  history; analytics can group by stable ids.
- Alternatives: soft-delete flags and per-field history (complex queries, easy to rewrite history by accident);
  JSON blob per version (forbidden as cache substitute and hard to validate).

## R2 — One entry per day enforcement
- Decision: `tracker_entries.dayKey` = date in single mode, NULL in multiple mode, unique index
  (`trackerId`, `dayKey`). Domain also rejects; UI opens the existing entry instead.
- Rationale: storage-level guarantee against double taps/races (SC-002). SQLite allows many NULLs in unique indexes.
- Mode switch: existing rows keep their keys; multiple→single never inserts on a date that already has entries.

## R3 — Removal semantics
- Decision: "Arquivar" (inactive) with "Restaurar"; history read-only while archived. Individual entry deletion with
  confirmation is the only delete.
- Rationale: permanent erase flows are excluded; archive stops reminders (FR-026) without silent data loss.

## R4 — Dates and clock
- Decision: reference date = civil `LocalDate` of the device zone at action time (`TaskClock.today()`); filledAt /
  updatedAt = wall millis. Allowed range created..today. Weeks Monday–Sunday as in specs 002/004.

## R5 — Persistent reminder on Android
- Decision: `Notification.Builder.setOngoing(true)`, `setAutoCancel(false)`, private visibility with neutral public
  version, existing `ethos_reminders` channel ("Lembretes"), actions Preencher (FILL) / Adiar (SNOOZE). No FGS.
- Rationale: official behavior change in Android 14 (API 34): users can dismiss notifications with
  `FLAG_ONGOING_EVENT` (exceptions: while the device is locked, foreground-service/media/call styles in specific
  cases). Below 14 ongoing notifications are not swipe-dismissible. Therefore the app treats the notification as a
  hint and keeps the pending state in Room-derived UI (FR-027). A foreground service only to pin a notice would
  violate FGS type policies and the spec.
- Dismissal: no delete intent is needed; delivery state stays POSTED, so the app does not repost the same slot
  (no nagging loop) while in-app pending remains.

## R6 — No-burst recovery
- Decision: tracker relevance requires `slotDate == checkDate`; schedule spans today..+32 only. After downtime the
  reconcile cancels past slots and posts today's slot once if its time passed. Tracker rows are posted directly
  rather than counted in the 007 recovery summary, because there is at most one per tracker.
- Snooze: reuse existing options; target must be on the same civil day (otherwise rejected as input error), since
  the slot belongs to that date.

## R7 — Calendar architecture
- Decision: `CalendarViewModel` combines Task/Habit/Tracker repositories; pure `CalendarModel` builds month/week cells
  and day details. Task detail, habit detail and tracker forms open through callbacks provided by `MainActivity`.
- Rationale: keeps features decoupled (no feature-to-feature imports) and makes filtering testable on the JVM.
- Task parity: day tasks = `activeOccurrences.filter { inDay(date) }` exactly as the old lookup.

## R8 — Charts without libraries
- Decision: Compose foundation `Canvas` dot plot (one column per day, a dot per entry, gap column labelled
  "Sem dados"), plus an accessible text list of values with their ranges. Canvas gets a summary content description.
- Alternatives: chart library (new dependency, no need).

## R9 — Backup
- `allowBackup="false"` already; no change. Open item: data-extraction rules review stays out of scope.
