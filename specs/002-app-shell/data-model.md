# Data model
- AppearanceMode: SYSTEM (default), LIGHT, DARK. Stored as enum-name string in appearance_mode key.
  Unknown value maps SYSTEM; missing value maps SYSTEM. This is a preference, not a domain record.
- AppearanceUiState: saved mode, loading, saving, error READ/WRITE or none, attempted mode for retry.
  Initial loading -> saved mode or recoverable read error/system fallback. Save -> commit new mode
  or retain old mode/write error. Retry repeats failed operation. UI never announces optimistic save.
- ShellDestination: today/tasks/habits/calendar/review/rewards/history/settings; title resource and icon.
- ContentFilter: ALL/TASKS/HABITS; today/review each remember independently across restoration.
- ShellItem and sample reward/history cards: presentation-only resource identifiers and item type;
  immutable explicit preview input. Production uses empty lists, absent balance/level and zero pending.
No task/list entities, database, XP formulas, date scheduling or personal history are persisted.
