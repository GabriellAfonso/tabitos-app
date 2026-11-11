# Data Model: Task Editor Pickers and Quick Capture

No stored entity changes. Room schema 7, `TaskFields`, `RepeatRule` and `TaskDefinition` are unchanged.

## UI-only models

### TaskDraft (existing, unchanged shape)

Internal values written by pickers and item editors; never displayed raw.

| Field | Written by | Format |
|-------|-----------|--------|
| `dateText` | due date picker | ISO `yyyy-MM-dd` or empty |
| `timeText` | time picker | `HH:mm` or empty |
| `startText`, `untilText` | recurrence pickers | ISO |
| `checklistText` | checklist item fields | items joined by `\n` |
| `rule.kind`, `rule.weekdays`, `intervalText` | repetition choice | existing semantics |

### TasksUiState additions

| Field | Meaning |
|-------|---------|
| `discardConfirmation: Boolean` | editor asked to close with unsaved changes |

ViewModel keeps `draftOriginal: TaskDraft?` (SavedState key `taskDraftOriginal`).

### RepeatChoice (new, pure)

`NONE, DAILY, WEEKLY, MONTHLY, INTERVAL` plus sub-options:

| RepeatKind | Choice | Sub-option |
|-----------|--------|------------|
| NONE | NONE | — |
| DAILY | DAILY | all days |
| WEEKDAYS | DAILY | workdays |
| WEEKLY | WEEKLY | 1 weekday bit |
| CUSTOM | WEEKLY | ≥ 2 weekday bits |
| MONTHLY | MONTHLY | month day |
| FIXED_INTERVAL | INTERVAL | count from planned date |
| AFTER_COMPLETION | INTERVAL | count from completion |

Validation: weekly selection keeps ≥ 1 bit; interval ≥ 1 (existing parse rules).

### DateText (new, pure result)

`Today | Tomorrow | Yesterday | Plain(text)` — relative words resolved from resources in Compose.
