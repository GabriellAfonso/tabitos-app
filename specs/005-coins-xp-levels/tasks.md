# Tasks: Coins, XP and levels
**Input**: spec.md, plan.md, research.md, data-model.md, contracts/ui.md, quickstart.md
**Tests**: Required by financial rules, persistence/migration and constitution V.
**Path base**: app/src/main/java/com/ethos/app unless full path stated.

## Phase 1: Setup
- [x] T001 Confirm existing toolchain/ignore rules and approved spec in specs/005-coins-xp-levels/plan.md.

## Phase 2: Foundation
- [x] T002 Write default/threshold/floor/chain tests in app/src/test/java/com/ethos/app/core/accounting/AccountingRulesTest.kt.
- [x] T003 Implement pure models/math/reconciliation in core/accounting/domain/AccountingModels.kt and AccountingRules.kt (FR001/002/010/011/014/020/021).
- [x] T004 Implement projection in core/accounting/domain/AccountingProjection.kt (FR004–009/012/019).
- [x] T005 Add entities/DAO/migration2→3 and register database schema in core/accounting/data/ and core/tasks/data/TaskDatabase.kt (FR013/019).
- [x] T006 Implement activation/reconciliation store and repository in core/accounting/data/AccountingStore.kt and RoomAccountingRepository.kt (FR013/015/019).

## Phase 3: US1 — task earnings (P1)
Independent test: easy task completion/relaunch/undo, explicit failure, recurring occurrence identity.
- [x] T007 [US1] Integrate atomic accounting into core/tasks/data/RoomTaskRepository.kt (FR003/004/013).
- [x] T008 [US1] Test completion/failure/undo/retry/old-history in app/src/androidTest/java/com/ethos/app/core/accounting/AccountingDatabaseTest.kt.

## Phase 4: US2 — habit accounting (P1)
Independent test: distinct-day practice, cap/correction, limit excess and confirmed closing outcomes.
- [x] T009 [US2] Test practice cap and habit projections in app/src/test/java/com/ethos/app/core/accounting/AccountingProjectionTest.kt.
- [x] T010 [US2] Add practice-only cap in core/habits/domain/HabitRules.kt and atomic integration in core/habits/data/RoomHabitRepository.kt (FR005–009/012/013).
- [x] T011 [US2] Test event reclassification/period review and invalidation in app/src/androidTest/java/com/ethos/app/core/accounting/AccountingDatabaseTest.kt.

## Phase 5: US3 — correction/history (P1)
Independent test:−295 penalty→−300, exact refund, linked corrections and preserved source deletion.
- [x] T012 [US3] Verify rollback/migration/baseline and correction chains in app/src/androidTest/java/com/ethos/app/core/accounting/AccountingDatabaseTest.kt (FR010–013/018/019/021).
- [x] T013 [US3] Add immutable lifecycle state/history/error recovery in features/accounting/AccountingViewModel.kt, AccountingScreen.kt and AccountingContent.kt (FR018/022).

## Phase 6: US4 — settings and UI consequences (P2)
Independent test: edit tables, inspect current estimate, old reversal, live Hoje thresholds.
- [x] T014 [US4] Implement validated retained table draft/settings UI in features/accounting/AccountingContent.kt and ViewModel (FR015/020/022).
- [x] T015 [US4] Compose live wallet/history/settings at MainActivity.kt and shell slots in features/shell/EthosShell.kt (FR014/017/018).
- [x] T016 [US4] Add shared financial context/estimates/confirmation to core/ui/accounting/AccountingConsequences.kt and existing task/habit forms/details (FR016/022).
- [x] T017 [US4] Add Portuguese strings and financial UI/failed-save tests in app/src/main/res/values/strings.xml and app/src/androidTest/java/com/ethos/app/core/accounting/AccountingUiTest.kt.

## Phase 7: Validation
- [x] T018 Run debug/JVM/lint/release and regression checks; record results in specs/005-coins-xp-levels/verification.md.
- [x] T019 Run available Android migration/UI/accessibility checks; record coverage limits in specs/005-coins-xp-levels/verification.md (SC001–007).
- [x] T020 Update implemented status/evidence and managed context in specs/005-coins-xp-levels/spec.md and AGENTS.md SPECKIT block only.

## Dependencies and execution
T001→T002–T006→US1→US2→US3→US4→validation. US1 is independently verifiable after foundation.
US2 uses shared accounting but its tests need no task operations. US3 correction engine is foundational;
history surface can validate on either source. US4 requires committed tables/wallet projection.
Parallel opportunities: independent test files can be prepared concurrently after model contract; production
mutations and shared-file integration remain sequential. Examples per story: task tests vs habit fixture,
habit cap tests vs history presentation, history labels vs migration fixtures, settings draft tests vs strings.
MVP is US1, but requested delivery includes all four stories. Commits explicitly authorized after validation; no push/publication authorized.

## Analysis coverage
FR001/002→T002/003;003/004→T007/008;005–009→T004/009–011;010/011→T002/003/012;
012→T004/010/011;013→T005–008/010/012;014→T002/003/015;015→T006/014;
016→T016/017;017→T015;018→T012/013/015;019→T004–006/008/012;
020→T003/014;021→T003/012;022→T013–019. SC001–007→T018/019.
