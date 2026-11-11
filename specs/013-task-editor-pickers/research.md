# Research: Task Editor Pickers and Quick Capture

## R1 — Picker components

- **Decision**: Material 3 `DatePickerDialog` + `DatePicker` and `TimePicker` (24 h) inside an
  `AlertDialog`, from the Compose BOM already in use.
- **Rationale**: No new dependency; accessible; matches the app's Material 3 base.
- **Alternatives**: platform `android.app.DatePickerDialog` (needs Context, inconsistent theming);
  custom calendar (unneeded work).

## R2 — UTC millis vs local day

- **Decision**: Convert with UTC on both directions: `LocalDate.atStartOfDay(UTC).toInstant()` for
  initial/selectable dates and `Instant.ofEpochMilli(ms).atZone(UTC).toLocalDate()` for the result.
- **Rationale**: The M3 date picker represents a day as UTC midnight; using the device zone shifts the day
  in negative offsets such as `America/Sao_Paulo` (spec scenario 1.6).
- **Alternatives**: device zone conversion (off-by-one bug).

## R3 — Draft representation

- **Decision**: Keep `TaskDraft` text fields (`dateText`, `timeText`, `startText`, `untilText`,
  `checklistText`) as internal ISO / newline values written by pickers and item fields; never shown to the
  user.
- **Rationale**: `saveDraft` parsing, SavedState serialization and tests remain valid; FR-012 (same stored
  results) is guaranteed by construction. Checklist items are single-line, so newline joining is lossless;
  `TaskRules` already trims and drops empty items.
- **Alternatives**: typed fields (`LocalDate?`) in the draft — cleaner but changes the ViewModel parsing and
  saved-state shape with no user-visible benefit in this spec.

## R4 — Repetition choice mapping

- **Decision**: `RepeatChoice { NONE, DAILY, WEEKLY, MONTHLY, INTERVAL }` derived from `RepeatKind`:
  DAILY/WEEKDAYS → DAILY (variant all/workdays), WEEKLY/CUSTOM → WEEKLY, MONTHLY → MONTHLY,
  FIXED_INTERVAL/AFTER_COMPLETION → INTERVAL (mode planned/completion). Weekly kind is computed from the
  selected weekday bits: one bit → WEEKLY, more → CUSTOM; deselecting the last day is ignored.
- **Rationale**: Every kind is reachable and existing tasks preselect the right option (FR-009).
- **Alternatives**: auto-converting Mon–Fri custom to WEEKDAYS (would silently change stored kind).

## R5 — Review confirmation visibility

- **Decision**: Show when `draft.editing`, the occurrence is `needsReview(today)` and the draft date
  differs from the occurrence's current `fields.date`; computed in a pure function.
- **Rationale**: Mirrors `TaskRules` (`dateChanged && o.needsReview(today)`), so the UI asks exactly when
  the rule requires it.

## R6 — Date wording

- **Decision**: Pure `EthosDates` with pt-BR `Locale`; short = "10 nov" (month abbreviation without the
  trailing dot Java adds), long = "segunda, 10 de novembro" (weekday without "-feira"); year appended when
  different from today's year; relative words come from string resources through a small enum result.
- **Rationale**: Testable on the JVM; one source for list, detail and habit calendar.

## R7 — Discard confirmation

- **Decision**: ViewModel stores the draft as opened (`draftOriginal`, also in SavedState); `closeDraft()`
  closes directly when equal, otherwise sets `discardConfirmation = true`.
- **Rationale**: State survives recreation (AGENTS.md: avoid transient effects for actions that matter).
