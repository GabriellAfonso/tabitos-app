# Research: Fast app startup (spec 027)

All findings were verified in the code on 2026-11-17 (Room 2.8.5 classes inspected from the Gradle cache).

## R1. What runs on a cold open today

- `EthosApp.kt` creates ten activity-scoped ViewModels. On creation:
  - Tasks: observes tasks, reminders, timer goals and game mode, and starts `TaskAction.Refresh` at the same time.
  - Habits: observes habits; on the first emission it runs `HabitAction.Settle`, which always upserts every row of
    every habit table (`RoomHabitRepository.execute`), so every habit observer re-reads.
  - Calendar: observes tasks + habits + trackers (a second full read of tasks and habits).
  - Reminders: observes reminders + tasks + habits (a third full read of tasks and habits).
  - Rewards, accounting, timers, trackers, game mode: one observation each.
  - Statistics: computes only while its page collects (`WhileSubscribed`); not part of the open path.
- Every `observe()` builds its own cold flow, so each ViewModel triggers its own full read, and every write
  invalidates the tables and makes every observer read everything again.
- `TaskAction.Refresh` writes when recurrences were generated; `write()` then upserts every list, definition,
  occurrence and audit row and deletes/reinserts every checklist.

Decision: the open path is reduced by ordering (R3), deferring writes (R4) and writing only changes (R5).

## R2. Can reads run in parallel without the write connection?

- The database is built without a SQLite driver (`Room.databaseBuilder(...).build()`), so Room 2.8.5 runs in
  compatibility mode on top of `SupportSQLiteDatabase` (`PassthroughConnectionPool`).
- In that mode every transaction, including `useReaderConnection { withTransaction(DEFERRED) }`, goes through the
  framework `SQLiteDatabase`, whose transactions always take the primary (write) connection. A read-only
  transaction on a secondary connection exists only from API 35 (`beginTransactionReadOnly`); minSdk is 24.
- Reading without a transaction is not consistent: the task snapshot is five queries (lists, definitions,
  occurrences, checklist, audit). A task created between two queries would show an occurrence without its
  definition, and code such as `TasksViewModel.edit` looks definitions up with `first { }`.

Decision: keep the transactional reads. Hoje stops queuing behind other sources because those sources start only
after Hoje is shown (R3), and the open-time writes start after it too (R4).

Alternatives rejected:
- Opening Room with `AndroidSQLiteDriver` (real reader connections): changes how all seven migrations run
  (`migrate(SQLiteConnection)`), the migration tests and the open callbacks. High risk to the user's stored routine
  for a gain the staging already captures.
- Sharing one hot snapshot per repository (`shareIn` in an application scope): removes duplicate reads, but a
  replayed failure breaks the existing per-page retry, and after staging the duplicates no longer happen on the
  open path. Left for the statistics/large-history spec.

## R3. Staged loading mechanism

Decision: a singleton `StartupSequence` (pure Kotlin + coroutines, `core/startup`) that knows:
- whether Hoje was shown (`markTodayShown()`);
- which deferred sources were requested on demand (`request(sources)`);
- which deferred sources finished their first load (`markLoaded(source)`), success or failure.
`awaitTurn(source)` returns when the source was requested, or when Hoje was shown and every source before it in the
explicit preload order has finished. `awaitTodayShown()` returns when Hoje was shown.

Deferred sources and order: `CALENDAR`, `REWARDS`, `REMINDERS` (an explicit list, never `ordinal`).

- Calendar, Rewards and Reminders ViewModels start observing after `awaitTurn`. Their public entry points that need
  data (`open`, `retry` and the page itself) request their source, which starts it at once.
- Navigation: when the shell shows a destination, `EthosApp` requests that destination's deferred sources
  (Calendário → CALENDAR, Recompensas → REWARDS, Configurações → REMINDERS), via a pure mapping function.
- Notification and popup requests (`AppRequests`): reminder routing needs only tasks, habits and trackers, which are
  Hoje's sources, and `ReminderViewModel.request` uses the coordinator, not the observed state. Any pending request
  still requests every deferred source, so a cold start from a notification never waits for the queue.
- Who marks Hoje as shown: `EthosApp` computes `todayReady` with a pure function over the loading flags of Hoje's
  sources and, when true, calls `markTodayShown()` and `ReportDrawnWhen` (R6).
- Tests and previews: the ViewModels receive `StartupSequence.released()` by default (everything already allowed),
  keeping existing tests unchanged.

Alternatives rejected:
- Lazy ViewModels created only by their page: notifications, dialogs and the root composition use them, so moving
  their scope changes routing and recreation behavior.
- `WhileSubscribed` for Reminders: its state is collected at the root for dialogs and permission handling, so it
  would still start at once.

## R4. When the open-time writes run

Decision: `TasksViewModel.refresh()` keeps updating `today` at once, but its background `TaskAction.Refresh` first
awaits `awaitTodayShown()`. `HabitsViewModel.settle()` does the same. Both run on every open and day change, as now.
Hoje shows the stored occurrences first; generated ones appear right after (today's observe and refresh already race
in this way, so the visible result is the same).

## R5. Writing only what changed

- Habits: entities embed the domain value (`HabitEntity(@Embedded val value: Habit)`), so the domain lists are the
  rows. For each table, upsert only the rows of `after` whose key is new or whose value differs from `before`.
  `removeReplacedRules` stays (a `DELETE` that removes nothing does not invalidate).
- Tasks: compare in entity space. For lists, definitions, occurrences and audit rows, upsert the `after` entities that
  are absent from or different in `before`'s entities. Checklists: for each owner (definition template or
  occurrence) present in `after`, clear and reinsert only when its rows differ. This is exactly what the full rewrite
  leaves in the database, provided entity → domain → entity is lossless; a JVM test asserts that round trip.
- An empty upsert writes nothing, so a no-change refresh or settle triggers no invalidation.
- Shared helper: `changedRows(before, after, key)` in `core/tasks/data` (the database package all repositories use).

Verification: JVM tests apply both the full rewrite and the changed rows to an in-memory table model built from the
same `before` and compare tables; instrumented tests check that the Room database after real actions equals the
rules' result and that a no-change refresh/settle causes no invalidation (written, run only with authorization).

## R6. Measuring startup

Decision: `ReportDrawnWhen { todayReady }` (activity-compose 1.12.4, already a dependency). Android logs
`Fully drawn com.ethos.app/.MainActivity: +Ns` in logcat for debug and release; it contains no personal content.
Baseline: a debug APK built with only the measurement change, saved outside the repo, installed with
`adb install -r` (keeps data) on the build type already installed. A release APK must never replace a debug install
(different signature would force an uninstall, which erases the routine).

Not adopted now: Baseline Profiles (need a separate macrobenchmark module); noted as a later option.
