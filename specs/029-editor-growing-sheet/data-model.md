# Data Model: Growing editor sheet

No stored entity changes (Room schema 7). `TaskDraft` and `HabitDraft` are reused as they are.

## UI-only state (not persisted, saved across recreation with `rememberSaveable`)

| State | Values | Initial | Notes |
|-------|--------|---------|-------|
| `expanded` | Boolean | `draft.editing` | "Mais" sets true; collapse sets false (create only). |
| `openSheet` (task) | `TaskEditorSheet?`: WHEN, REPEAT, ENDING, LIST, DURATION, DESCRIPTION, CHECKLIST, IMPORTANCE, DIFFICULTY | null | One choice sheet at a time. |
| `openSheet` (habit) | `HabitEditorSheet?`: FREQUENCY, DAYS, TIMES, MINUTES, BREAKING_TYPE, LIMIT, LOOK, LIST, DESCRIPTION, IMPORTANCE, DIFFICULTY | null | Same. |

## Presentation mappings (pure, unchanged values)

- `PracticeFrequency { DAILY, CHOSEN_DAYS, WEEKLY, MONTHLY }`
  - `practiceFrequencyOf(rule)`: DAY with all 7 weekdays → DAILY; DAY otherwise → CHOSEN_DAYS; WEEK → WEEKLY;
    MONTH → MONTHLY.
  - `withPracticeFrequency(draft, f)`: DAILY → DAY, 127, target "1"; CHOSEN_DAYS → DAY, Mon–Fri, "1"; WEEKLY →
    WEEK, 127, "3"; MONTHLY → MONTH, 127, "10". Choosing the current value returns the draft unchanged.
- `withTimed(draft, timed)`: on → target "1", duration defaults to "20", MONTH becomes DAY; off → `timed = false`.
- `withTimedWeekly(draft, weekly)`: target "1", unit WEEK with 127, or DAY keeping the weekdays.
- `withBreakingType(draft, type)`: AVOID → unit DAY, target "0"; LIMIT → target "1".
- `toggleHabitWeekday(rule, index)`: XOR the bit and never leave the selection empty.
- Visibility: `showsDaysChip`, `showsTimesChip` (habit); `showsEndingRow`, `showsChecklistRow`, `showsDurationChip`
  (task).
