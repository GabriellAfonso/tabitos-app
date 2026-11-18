# Feature Specification: Statistics stay fast with a large history

**Feature Branch**: `031-statistics-large-history` (no branch created; work stays on the current branch)

**Created**: 2026-11-18

**Status**: Implemented (2026-11-18); phone timing pending

**Input**: Follow-up of spec 027 (conversation of 2026-11-17/18, no transcript id available). The user said
Estatísticas "com o tempo vai crescer muito e começar a demorar" and asked for a plan so it does not get slow with a
large history. Proposed decisions (accepted when the user asked to run specify → plan → tasks → implement without
answering the open questions): target "under 1 second with 5 years of synthetic use", scope limited to Estatísticas,
schema change allowed only if it preserves data. Numbers 028–030 were taken by other work, so this is spec 031.

## Current state (verified in code, 2026-11-18)

- Estatísticas (specs 022/024) reads the full task, habit, timer and reward/wallet snapshots and recomputes the
  whole page in memory on every change of any of them and on every control change (period ‹ ›, period kind,
  filter, item, "Ver mais").
- Each recomputation rebuilds the history of all time (every task audit, habit record, closed habit period,
  timer stretch and ledger entry), then filters it to the chosen period and filter, then sorts it.
- The habit card evaluates each habit's streak and success rate over its periods on every recomputation.
- A running timer writes a checkpoint every 10 seconds; each checkpoint changes the timer snapshot, so with the page
  open and a timer counting, everything is recomputed every 10 seconds, although counting time never enters the
  statistics (only closed stretches count).
- Statistics only compute while the page is open (spec 027 keeps it so).

## Decisions

1. Measure first, on a synthetic dataset that resembles 5 years of daily use (no personal data): time to compute
   the page when opening it, and when changing period or filter.
2. Separate the work that depends only on the stored data (built once per data change) from the work that depends
   on the controls (period, filter, item, page size), so changing period, filter or "Ver mais" never rebuilds the
   history of all time.
3. Data changes that cannot change any number or row (a running timer's checkpoint) do not trigger a recomputation.
4. The numbers and rows shown stay exactly the same as today for every period, filter and item.
5. No schema change unless measurement shows the database read itself is the bottleneck; if a change is needed it
   must be a data-preserving migration (7 → 8) with a migration test. Revised during planning if measurement allows.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Opening Estatísticas with years of history (Priority: P1)

After years of use, the user opens Estatísticas and sees the Resumo quickly.

**Why this priority**: The user's concern; the cost grows every day.

**Independent Test**: With the 5-year synthetic dataset, measure the page computation on open; compare with the
target and with the baseline.

**Acceptance Scenarios**:

1. **Given** 5 years of synthetic history, **When** the page is opened, **Then** its content is computed within the
   target (SC-001).
2. **Given** any dataset, **When** the page is opened, **Then** every number and row is identical to the current
   implementation.

---

### User Story 2 - Moving between periods and filters (Priority: P1)

The user goes ‹ › between weeks, switches Semana/Mês/Ano/Tudo, changes the filter or taps "Ver mais" and the page
answers at once.

**Why this priority**: These are the most frequent interactions on the page and today redo all the work.

**Independent Test**: With the synthetic dataset, measure the time of a period or filter change after the page is
open.

**Acceptance Scenarios**:

1. **Given** the page is open, **When** the period, filter, item or page size changes, **Then** the history of all
   time is not rebuilt and the new content is computed within SC-002.

---

### User Story 3 - A timer counting while the page is open (Priority: P2)

With a timer running, the page does not keep recomputing every 10 seconds.

**Independent Test**: Feed timer snapshots that differ only by the running stretch's checkpoint; the page content is
not recomputed.

**Acceptance Scenarios**:

1. **Given** a running timer, **When** only its checkpoint changes, **Then** no recomputation happens.
2. **Given** a running timer, **When** it is paused (a stretch closes), **Then** the time card and history update.

### Edge Cases

- Empty database: page shows the same empty states as today.
- Erasing a deleted item's history (spec 022) or any edit while the page is open: content updates as today.
- Day change while the page is open: the current period follows the clock as today.
- Game mode toggled while open: coins card and filter appear/disappear as today.

## Requirements *(mandatory)*

### Escopo e restrições do Ethos

- In scope: measurement on synthetic data, caching data-only work per data change, ignoring changes that cannot
  affect the page, keeping results identical.
- Out of scope: the full-history load inside task rules on every action (domain change, later); Calendar and other
  pages; visual changes.
- Persistence: read-only page; no schema change expected (decision 5). User's real routine untouched.
- Failures: existing loading/error/retry states kept.
- Accessibility: unchanged.
- Permissions and personal data: none; measurement uses synthetic data only and logs nothing.

### Functional Requirements

- **FR-001**: A repeatable measurement MUST exist for page computation on a synthetic 5-year dataset (open, period
  change, filter change), runnable on the JVM.
- **FR-002**: Work that depends only on stored data MUST be computed once per data change and reused by every
  control change.
- **FR-003**: A period, period kind, filter, item or page-size change MUST NOT rebuild the all-time history.
- **FR-004**: A data change that cannot change any shown value (running stretch checkpoint) MUST NOT trigger a
  recomputation.
- **FR-005**: For every period kind, filter, item and page size, the content MUST equal the current computation.
- **FR-006**: Loading, error, retry, erase-history and game-mode behaviors MUST stay as they are.

### Key Entities

- **Prepared statistics data**: everything derived only from the stored snapshots, the day and game mode (first data
  day, credited timer time, all-time history items in display order, per-habit data reused across periods).
- **Controls**: period, selection (filter/item), page size.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: With the 5-year synthetic dataset, computing the page on open takes under 1 second on the JVM test
  machine (proxy for the phone; the phone measurement is a manual check).
- **SC-002**: With the page open, a period or filter change computes in under 100 ms on the same machine.
- **SC-003**: Equality tests show identical content between the old and the new computation for all period kinds and
  filters on the synthetic dataset.
- **SC-004**: No recomputation for checkpoint-only timer changes (test).
- **SC-005**: No existing test fails; no new lint warning.

## Assumptions

- 5 years of "daily use" means: 6 daily recurring tasks completed most days, 6 habits (4 to practice, 2 to avoid)
  recorded most days, about one timer stretch per day and a ledger entry per completion.
- The JVM machine is faster than the phone; meeting SC-001/SC-002 with margin on the JVM is taken as the
  engineering target, and the phone timing is recorded when the user checks it.
