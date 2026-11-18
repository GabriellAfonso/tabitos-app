# Feature Specification: Fast app startup (Today first, staged preloading)

**Feature Branch**: `027-fast-startup` (no branch created; work stays on the current branch)

**Created**: 2026-11-17

**Status**: Implemented (2026-11-17); startup timing on the phone pending

**Input**: User description: "pq sempre q abro o app tem meio q um loading de tarefas? [...] deveria carregar primeiro a
primeira coisa q vc vai ver q no caso é o hoje, e as outras uma por vez pra ja meio q ficar pre carregado, e as
estatisticas talvez só quando clicar nelas". Decisions taken in conversation on 2026-11-17 (no transcript id
available); the numbers 025 and 026 were already taken, so this is spec 027.

## Current state (verified in code, 2026-11-17)

- On every cold open, Hoje shows "Carregando tarefas…" / "Carregando hábitos…" for a noticeable time, even with
  only a handful of tasks. Startup time has never been measured.
- `EthosApp.kt` creates the activity-scoped ViewModels at once: tasks, habits, accounting, rewards, reminders,
  timers, trackers, calendar, game mode and statistics. All of them except Statistics start reading the database
  immediately. Statistics already computes only while its page is open (`WhileSubscribed`).
- Repositories read inside database transactions (tasks, habits, accounting, rewards, reminders). These always use
  the database's single write connection, so reads from different pages wait for each other and Hoje's first read
  queues behind sources Hoje does not show.
- Opening the tasks ViewModel starts the first read and the recurrence refresh at the same time. The refresh reads
  everything, regenerates recurrences and, when anything changed (typical on the first open of a day), rewrites every
  list, task, occurrence, checklist item and audit row, even unchanged ones. Every page observing task data then
  reads everything again.
- Each task read loads every occurrence ever generated and the whole task change history, so the work grows with
  time, not with the number of tasks visible on Hoje.

## Decisions

1. Measure first: record the time from opening the app until Hoje shows its content, before and after the change,
   on a debug and a release build. Measurement never records task, habit or list content.
2. Hoje loads first. "Hoje's sources" are everything Hoje draws: tasks, habits, the timer state shown on cards,
   pending trackers (the "Preencher" card), the game mode preference and the wallet/coin prices. Hoje is shown
   complete, not in pieces.
3. Every other source (calendar, rewards, reminder settings, and any other source Hoje does not draw)
   starts loading only after Hoje showed its content, one source at a time, as preloading, so the other pages are
   usually ready when opened.
4. Something that needs a source not yet loaded never waits for the queue: opening a page, a notification action or a
   popup link loads that source immediately, and the result is exactly what it is today.
5. Statistics stays computed only while its page is open (already true). Making it fast with a very large history
   is a separate future spec (see Out of scope).
6. Hoje's reads never queue behind sources Hoje does not draw or behind the open-time refresh. Each read still sees a
   consistent snapshot (never half of a write). Revised during planning (research.md R2): fully parallel reads would
   require changing how the database is opened, which risks the stored routine and is not done here.
7. The recurrence refresh on open runs after Hoje's first content, not competing with it. It still runs on every
   open and on day change, as today.
8. Saving task and habit data writes only what changed. The resulting database must be identical to what the full
   rewrite produces today. Habits were added during planning: the habit closing that runs on every open
   (HabitAction.Settle) also rewrites every habit row today, even when nothing changed.
9. No schema change (stays at version 7), no rule change, no visible change other than loading faster.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Hoje appears quickly on open (Priority: P1)

The user opens the app to check today's routine. Hoje's tasks and habits show up quickly, without a long
"Carregando" phase caused by pages they are not looking at.

**Why this priority**: It is the problem the user reported and happens on every open.

**Independent Test**: Measure the time to Hoje's content on the same phone and data before and after; confirm other
sources start loading only after Hoje's content is shown.

**Acceptance Scenarios**:

1. **Given** the app is closed, **When** the user opens it, **Then** Hoje shows its tasks and habits before any
   source Hoje does not draw starts loading.
2. **Given** game mode is on, **When** Hoje first shows its content, **Then** coin prices and timer state on the cards
   are already correct (no card first appears without price or with a wrong timer state).
3. **Given** it is the first open of a new day, **When** the app opens, **Then** Hoje shows content first and the
   day's new recurring occurrences appear right after, with the same result as today.

---

### User Story 2 - Other pages are ready when opened (Priority: P2)

After Hoje appears, the app quietly loads the other pages' data one at a time, so Calendário, Recompensas and the
others usually open without a loading phase.

**Why this priority**: Keeps the rest of the app as responsive as today while Hoje gets faster.

**Independent Test**: Open the app, wait a few seconds on Hoje, then open each page from the menu: none shows a
loading state. Open a page immediately after launch: it loads right away instead of waiting for the queue.

**Acceptance Scenarios**:

1. **Given** Hoje was shown and preloading finished, **When** the user opens Calendário or Recompensas, **Then** the
   page shows content without a loading state.
2. **Given** preloading has not reached Recompensas yet, **When** the user opens Recompensas, **Then** it starts
   loading immediately and shows the same content it shows today.
3. **Given** the user opens Estatísticas, **Then** its behavior and numbers are the same as today.

---

### User Story 3 - Notifications and links keep working (Priority: P2)

Tapping a notification (reminder, tracker, timer, daily summary) or a link inside a popup opens the right place
with the right data, even right after a cold start.

**Why this priority**: Staging the loads must not break existing entry points that depend on other sources.

**Independent Test**: With the app closed, trigger each notification type and tap it and its actions; compare with
the current behavior.

**Acceptance Scenarios**:

1. **Given** the app is closed, **When** the user taps a tracker reminder, **Then** the tracker form opens as today.
2. **Given** the app is closed, **When** the user taps "Concluir" on a task reminder, **Then** the same confirmations
   and result as today happen.
3. **Given** the app is closed, **When** the user taps a running timer's notification, **Then** the timer sheet opens
   as today.

---

### User Story 4 - Saving stays correct and gets lighter (Priority: P3)

Completing, editing, postponing or reordering tasks, and recording or closing habits, gives exactly the same stored
result, while writing only what changed.

**Why this priority**: Saves work on every action and on every day's refresh, but touches the user's real data, so
correctness comes before speed.

**Independent Test**: For each task action, compare the database after the new save with the database after the
current full rewrite, starting from the same data.

**Acceptance Scenarios**:

1. **Given** any stored task data and any task action, **When** the action is saved, **Then** every table holds the
   same rows as with the full rewrite.
2. **Given** an action that changes nothing, **When** it is saved, **Then** nothing is written.

---

### Edge Cases

- The database fails to open or a read fails during staged loading: the affected page shows its existing error and
  retry, and the other sources keep loading.
- The user creates a task or habit while preloading is still running: the action works as today; preloading never
  blocks or delays user actions.
- The recurrence refresh fails after Hoje showed content: the existing storage error is shown, as today.
- The day changes while the app is open: refresh behavior is unchanged.
- A running timer exists at cold start: its card and notification are correct from Hoje's first frame.
- The app is opened from a notification straight into another page: that page's sources load first, and Hoje's
  sources load as needed for it.
- Checklist items reordered, added or removed, tasks deleted, history erased (spec 022): saved rows equal the full
  rewrite, including removed rows.

## Requirements *(mandatory)*

### Escopo e restrições do Ethos

- In scope: startup measurement, loading order, Hoje's reads not queuing behind other sources, refresh timing,
  writing only changed task and habit rows.
- Out of scope: statistics with a very large history (future spec: aggregation per period in the database, date
  indices with a migration 7→8, paging the Histórico from the database, caching closed periods, a synthetic
  multi-year dataset); reducing the full history the task rules load on every action (domain change, for later);
  any visual change.
- Offline and persistence: everything stays local. The stored result of every action is unchanged; no schema
  change, no migration, no data reset.
- Failures: existing loading, error and retry states are kept per page.
- Accessibility: unchanged; loading states that remain keep their current texts.
- Permissions and personal data: no new permission. Measurement logs only timings, never task, habit or list
  content.
- Recurrences and reminders: rules, dates and time zones unchanged; only when the open-time refresh starts changes.
- The user's real routine is in the app: no data loss, reset or incompatible change is allowed.

### Functional Requirements

- **FR-001**: The app MUST provide a way to measure the time from opening to Hoje's content being shown, on debug
  and release builds, without recording personal content.
- **FR-002**: On open, the app MUST load Hoje's sources (tasks, habits, timer state, game mode and, in game mode,
  coin prices) before any other source.
- **FR-003**: Hoje MUST show its content only when all of Hoje's sources are ready, so no card appears with missing
  or wrong price or timer state.
- **FR-004**: Sources Hoje does not draw MUST start loading only after Hoje's content is shown, one at a time.
- **FR-005**: Opening a page, tapping a notification or its action, or following a popup link MUST load the needed
  sources immediately, regardless of the preloading order, with the same result as today.
- **FR-006**: Statistics MUST keep computing only while its page is open, with the same numbers as today.
- **FR-007**: Hoje's first reads MUST NOT wait for reads of sources Hoje does not draw nor for the open-time refresh
  and habit closing, and each read MUST see a consistent snapshot.
- **FR-008**: The recurrence refresh and the habit closing on open MUST start after Hoje's first content and MUST
  produce the same results as today.
- **FR-009**: Saving task and habit data MUST write only rows that were added, changed or removed, and MUST leave
  the database identical to the current full rewrite for every task and habit action.
- **FR-010**: An action or refresh that changes nothing MUST write nothing.
- **FR-011**: Preloading MUST NOT block, delay or reorder user actions.
- **FR-012**: Each page MUST keep its current loading, empty, error and retry behavior when its source is opened
  before preloading reached it.
- **FR-013**: No schema change, rule change or visible change other than load timing is allowed.

### Key Entities

- **Data source**: one group of data a page needs (tasks, habits, timers, game mode, prices, rewards, reminders,
  trackers, calendar, statistics). Has a state: not started, loading, ready or failed.
- **Hoje's sources**: the sources Hoje draws; loaded first and shown together.
- **Preloading order**: the sequence in which the remaining sources start after Hoje is shown.
- **Startup measurement**: elapsed time from open to Hoje's content; contains only timings and build type.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On the user's phone and data, the time from opening to Hoje's content on a release build is measured
  before and after, and is lower after; both values are recorded in the verification document.
- **SC-002**: In 100% of cold opens, no source Hoje does not draw starts loading before Hoje's content is shown.
- **SC-003**: After waiting 5 seconds on Hoje, opening any page from the menu shows content without a loading state.
- **SC-004**: For every task and habit action covered by tests, the database after saving equals the database
  produced by the full rewrite (100% of compared rows).
- **SC-005**: Every notification type and action and every popup link behaves as before the change.
- **SC-006**: No existing test fails and no new lint warning appears.

## Assumptions

- "Opening" means a cold start (process not running); a warm return to the app already shows content and is not
  the target.
- The before/after measurement is taken by the user on their own phone, or on the emulator only with explicit
  authorization, as the project rules require; JVM tests cover ordering and saving.
- Preloading order after Hoje follows how often pages are used from the menu (Calendário, Revisar, Recompensas,
  then the rest); the exact order is a planning detail and can be changed without changing behavior.
- Reminders and timer scheduling triggered by the system (alarms, boot) are not part of the app's open path and keep
  working as today.
- The measurement may show that a release build is already fast; even so, the staged loading and lighter saving are
  kept, because the reported cost grows with history.
