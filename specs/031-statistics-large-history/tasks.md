# Tasks: Statistics stay fast with a large history

**Input**: `specs/031-statistics-large-history/` (spec, plan, research)

## Phase 1: Measurement

- [X] T001 Move the current computation out of the ViewModel into
  `app/src/main/java/com/ethos/app/features/statistics/StatisticsComputation.kt`
- [X] T002 Synthetic 5-year dataset in `app/src/test/java/com/ethos/app/features/statistics/StatisticsDataset.kt`
- [X] T003 Baseline timing test (open, period change, filter change) in
  `app/src/test/java/com/ethos/app/features/statistics/StatisticsLargeHistoryTest.kt`; record numbers in research

## Phase 2: User Story 1 + 2 — prepare once, derive per control (P1)

- [X] T004 [US1] `PreparedStatistics.prepare` and `content(controls)` in `StatisticsComputation.kt`, with prepared
  history items (`PreparedHistory`) in `StatisticsHistory.kt`
- [X] T005 [US2] `StatisticsViewModel` prepares on data/day/game-mode changes and derives on control changes
- [X] T006 [US1] Equality test: reused prepared content equals a fresh preparation for every period kind, filter,
  item and page size on the synthetic data, in `StatisticsLargeHistoryTest.kt`
- [X] T007 [US1] Timing assertions for SC-001/SC-002 in `StatisticsLargeHistoryTest.kt`

## Phase 3: User Story 3 — ignore checkpoints (P2)

- [X] T008 [US3] Map timers to the parts statistics use and `distinctUntilChanged` in `StatisticsViewModel.kt`
- [X] T009 [US3] Test that checkpoint-only snapshots map to equal values and a closed stretch does not

## Phase 4: Polish

- [X] T010 Run `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`
- [X] T011 `verification.md`, spec status, SPECKIT block in `AGENTS.md`

## Dependencies

T001 → T002 → T003 → T004 → T005/T006/T007 → T008 → T009 → T010 → T011.

Added during implementation: `HabitRules.periods` indexed per call (quadratic before) and
`HabitRules.streak`/`successRate` overloads over prepared periods (research R5).
