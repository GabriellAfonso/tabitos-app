# Data model (Room schema 7)

All new tables are additive (migration 6→7 creates them; no existing table changes).

## trackers
| column | type | notes |
|---|---|---|
| id | TEXT PK | UUID |
| habitId | TEXT NOT NULL | unique index — one tracker per habit |
| created | INTEGER NOT NULL | epoch day (civil date of creation) |
| multiple | INTEGER NOT NULL | 0 = one per day (default), 1 = many |
| active | INTEGER NOT NULL | 0 = archived (read-only, no reminders) |
| version | INTEGER NOT NULL | current version number (≥1) |
| updated | INTEGER NOT NULL | wall millis |

## tracker_versions
PK (`trackerId`, `number`); `created` wall millis. Immutable once written.

## tracker_questions
PK (`trackerId`, `version`, `questionId`). Columns: `position` INT, `type` TEXT (SCALE/CHOICE/TEXT), `title` TEXT,
`required` INT, `isPrimary` INT, `min` INT, `max` INT, `minLabel` TEXT, `maxLabel` TEXT, `multiple` INT.
Validation: title non-blank; SCALE `min < max`, range within −1000..1000; at most one primary and only on SCALE;
CHOICE has ≥1 option.

## tracker_options
PK (`trackerId`, `version`, `optionId`). Columns: `questionId`, `position`, `label` (non-blank), `description`.

## tracker_entries
| column | type | notes |
|---|---|---|
| id | TEXT PK | UUID |
| trackerId | TEXT NOT NULL | index |
| version | INTEGER NOT NULL | version answered |
| date | INTEGER NOT NULL | reference epoch day |
| dayKey | TEXT NULL | date ISO in single mode, NULL in multiple; unique (`trackerId`, `dayKey`) |
| filledAt | INTEGER NOT NULL | real moment first saved |
| updatedAt | INTEGER NOT NULL | last edit |

## tracker_answers
PK (`entryId`, `questionId`). Columns: `scale` INT NULL, `choices` TEXT (newline-joined option ids, empty if none),
`text` TEXT. Rows only for answered questions. Deleted with their entry in the same transaction.

## Reminders (existing tables)
`reminder_definitions` row: id = groupId = `tracker:<trackerId>`, kind `TRACKER`, sourceId trackerId, mode WEEKDAYS,
weekdays 127, minute chosen. Deliveries key `<groupId>|<trackerId>|<date>`, periodKey = date.

## Domain state transitions
- Tracker: none → active(v1) → active(vN+1 on structural change) ⇄ archived.
- Entry: new (current version) → edited (same version, updatedAt) → deleted (confirmation).
- Pending today: reminder enabled ∧ tracker active ∧ habit ACTIVE ∧ habit interval active today ∧ now ≥ reminder
  time ∧ no entry with date today.
