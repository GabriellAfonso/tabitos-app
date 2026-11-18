# Data model: Fast app startup (spec 027)

No stored data changes. Room schema stays at version 7; no migration. The items below are in-memory types.

## DeferredSource (enum, `core/startup`)

`CALENDAR`, `REWARDS`, `REMINDERS`. Preload order is the explicit list `PRELOAD_ORDER`, not `ordinal`.

## StartupSequence (singleton, `core/startup`)

| State | Meaning | Changes |
|-------|---------|---------|
| todayShown | Hoje's sources finished their first load | false → true once (`markTodayShown`) |
| requested | Deferred sources asked for on demand | grows (`request`) |
| loaded | Deferred sources whose first load ended (content or error) | grows (`markLoaded`) |

Rules:
- `awaitTurn(s)` resumes when `s ∈ requested`, or `todayShown` and every source before `s` in `PRELOAD_ORDER` is in
  `loaded`.
- `awaitTodayShown()` resumes when `todayShown`.
- `released()` builds an instance with `todayShown` true and all sources requested (tests, previews).

## Hoje readiness (pure function, root package)

`todayReady = gameMode loaded ∧ tasks loaded ∧ habits loaded ∧ timers loaded ∧ trackers loaded ∧ wallet loaded`.
A source in error counts as loaded (its page shows the existing error and retry).

## Destination → deferred sources (pure function, root package)

Calendário → {CALENDAR}; Recompensas → {REWARDS}; Configurações → {REMINDERS}; every other destination → {}.

## Changed rows (`core/tasks/data`)

- `changedRows(before, after, key)`: rows of `after` whose key is not in `before` or whose value differs.
- `TaskWrites(lists, definitions, occurrences, checklists: owner → rows, history)`: rows to write for one save,
  derived from the before/after task snapshots in entity form.
