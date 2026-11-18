# Research: Statistics stay fast with a large history (spec 031)

Verified in code on 2026-11-18.

## R1. Where the time goes

`StatisticsViewModel.compute(sources, controls, zone)` runs on every emission of
`combine(sources, controls)`:
- `firstDataDay` scans every definition, audit, habit, record, segment and ledger entry.
- `creditedTime` splits every closed timer stretch per local day.
- `historyItems` builds the items of all time: every task audit (with ledger matching), every habit record (with
  `HabitRules.currentRule` per record), every closed avoid/limit period (`HabitRules.periods` per habit, all time),
  every credited timer day and every unmatched ledger entry; then filters by range and filter and sorts.
- `habitSummary` evaluates `HabitRules.streak` and `HabitRules.successRate` per habit.

Only the range/filter/item/limit part depends on the controls. Everything else depends only on the snapshots, the
day and game mode.

Decision: compute a `PreparedStatistics` once per (sources, day, game mode, zone) and derive the page from it per
control change. History items are prepared once per game mode in display order (newest first) for the "all
filters" case, and once for the coins-only case; per control, they are filtered by range, filter and item, which
keeps the order, so no sort is needed.

## R2. Recomputing for nothing

`RoomTimerRepository.checkpoint()` updates the open segment's `checkpointElapsed` every `CHECKPOINT_MILLIS`
(10 s) while counting. Statistics read only closed, active segments (`creditedTime`) and the session titles/kinds.
Decision: map the timer snapshot to the parts statistics use (sessions and closed segments) and apply
`distinctUntilChanged`, so checkpoint-only changes do not reach the computation.

## R3. Database reads

The four repositories already emit full snapshots; Estatísticas only runs while open. Indices would not help
without rewriting the page as per-period SQL queries, which would duplicate the habit rules (`successRate`,
`streak`, closed periods) in SQL. Decision: no schema change (spec decision 5); the measurement (T001) checks that
compute, not read, dominates on the JVM. A per-period SQL rewrite stays a future option if the phone shows the read
as the bottleneck.

## R4. Proving identical results

Keep the current function as the reference inside the test source set? No: tests compare the new
prepared-then-derived result with a direct one-shot computation built from the same pure functions (the old path is
the same code with no cache), for all period kinds, filters and items on the synthetic dataset.

## R5. Measured (JVM, synthetic 5 years: 6 daily tasks, 6 habits, 1 timer stretch/day, ~22k ledger entries)

| Step | Before | After |
|------|--------|-------|
| Open (prepare + "Tudo") | 3 023 ms | 265 ms alone, 655 ms during the full test run |
| Slowest period/filter/item/page change | 2 015 ms | 3–8 ms |
| `habitSummary` | ~2 000 ms per change | 8–16 ms, once per data change |
| All-time history build | 1 163 ms per change | ~300 ms, once per data change |

The dominant cost was `HabitRules.periods`, which scanned every record and interval of the database for each day
of each period (quadratic). It now indexes the habit's records, intervals and overrides once per call, with the
same result (all habit rule tests pass). This shared domain function also serves Hoje and the habit cards, which
get faster too. Periods are built once per data change and reused by streak, rate and the history
(`HabitRules.streak(periods, …)`, `HabitRules.successRate(periods, …, range)`).
Database reads were not the bottleneck on the JVM, so no schema change was made (decision 5).
