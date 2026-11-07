# Tasks: Personal rewards
**Input**: specs/006-personal-rewards/spec.md, plan.md, research.md, data-model.md, contracts/rewards.md.
Paths below use K=app/src/main/java/com/ethos/app; J=app/src/test/java/com/ethos/app;
A=app/src/androidTest/java/com/ethos/app. These are exact repository path prefixes.

## Phase 1: Setup
- [X] T001 Verify existing dependencies/ignores and feature context in .gitignore and .specify/feature.json.

## Phase 2: Foundation
- [X] T002 Define pure reward/intent/purchase/snapshot/repository contracts in K/core/rewards/domain/RewardModels.kt and RewardRepository.kt (FR001–018).
- [X] T003 Define schema4 entities/DAO/migration in K/core/rewards/data/ and integrate K/core/tasks/data/TaskDatabase.kt and TaskModule.kt; export app/schemas/com.ethos.app.core.tasks.data.TaskDatabase/4.json (FR002,013,014,017).

## Phase 3: US1 - Organize rewards (P1 MVP)
Goal: usable real empty store and persisted CRUD/order. Independent test: create/edit/order/delete/relaunch.
- [X] T004 [US1] Write validation/order/preservation tests in J/core/rewards/RewardRulesTest.kt and A/core/rewards/RewardDatabaseTest.kt before implementation (FR001–003).
- [X] T005 [US1] Implement validation and save/order/delete in K/core/rewards/domain/RewardRules.kt and K/core/rewards/data/RoomRewardRepository.kt (FR001–003).
- [X] T006 [US1] Implement retained editor/store state and errors in K/features/rewards/RewardsViewModel.kt; test in J/core/rewards/RewardsViewModelTest.kt (FR001,004,018).
- [X] T007 [US1] Build store/editor/confirmed deletion in K/features/rewards/RewardsContent.kt; integrate K/MainActivity.kt, K/features/shell/EthosShell.kt and app/src/main/res/values/strings.xml (FR001–004,018).

## Phase 4: US2 - Confirm purchases (P1)
Goal: safe immediate use with durable result. Independent test: cancel/exact balance/retry/relaunch.
- [X] T008 [US2] Add exact/negative/insufficient, retry, concurrent and rollback tests in A/core/rewards/RewardDatabaseTest.kt (FR005–009,012–014).
- [X] T009 [US2] Implement persisted preview, atomic debit/use, stale recheck and acknowledgement in K/core/rewards/data/RoomRewardRepository.kt (FR005–009,012–014).
- [X] T010 [US2] Add recoverable confirmation/result/error state and UI in K/features/rewards/RewardsViewModel.kt and RewardsContent.kt; test A/core/rewards/RewardsUiTest.kt (FR005–009,012–014,018).

## Phase 5: US3 - Independent limits (P1)
Goal: civil allowances survive edits. Independent test: boundaries and reduced/changed limits.
- [X] T011 [US3] Add daily/week/month/year/edit tests in J/core/rewards/RewardRulesTest.kt and A/core/rewards/RewardDatabaseTest.kt (FR010–012).
- [X] T012 [US3] Implement calendar usage rules and card/preview allowance in K/core/rewards/domain/RewardRules.kt and K/features/rewards/RewardsContent.kt (FR004,010–012).

## Phase 6: US4 - Trust history (P2)
Goal: original details and accounting retained. Independent test: buy/edit/delete/correct/reopen.
- [X] T013 [US4] Test migration3→4 and existing full-chain migrations in A/core/rewards/RewardDatabaseTest.kt, A/core/accounting/AccountingDatabaseTest.kt and A/core/habits/HabitDatabaseTest.kt (FR015–017).
- [X] T014 [US4] Render original purchase snapshot/reference through existing K/core/ui/accounting/AccountingConsequences.kt and K/features/accounting/AccountingContent.kt (FR015).
- [X] T015 [US4] Verify task/habit refresh/correction preserves spending and durable pending/receipt after reopen in A/core/rewards/RewardDatabaseTest.kt (FR009,014–017).

## Phase 7: Verification
- [X] T016 Run debug/JVM/lint/R8/Android verification and record actual results/limits in specs/006-personal-rewards/verification.md (SC001–006).
- [X] T017 Validate UI themes/200% fonts/navigation/error/accessibility labels with A/core/rewards/RewardsUiTest.kt and record evidence/limits in specs/006-personal-rewards/verification.md (FR018,SC006).
- [X] T018 Reconcile completion/status and managed context in specs/006-personal-rewards/tasks.md, spec.md and AGENTS.md; no commits/publication.

## Dependencies & execution
T001→T002→T003; US1 tests before CRUD implementation; US2 after definitions/foundation; US3 uses purchase
records; US4 uses confirmed purchases; final verification after all. Foundation contract includes limits
but rule implementation follows US3 tests. Tests are written before corresponding behavior.

## Parallel opportunities
No parallel agent implementation required. US1 validation test editing can be independent of UI resources;
US2 DB tests independent of UI tests; US3 pure calendar tests independent of cards; US4 history rendering
independent of migration fixtures. Shared repository/VM edits remain sequential. No [P] marker used because
planned implementation is sequential and most changes share files.

## Implementation strategy
MVP is US1 (real store without purchases). Then US2 immediate purchases, US3 limits and US4 history.
All authorized stories delivered together after verification. No automatic Git actions.

## Coverage mapping
FR001 T002,004–007;002 T003–005;003 T004–007;004 T006,007,012;005–008 T008–010;
009 T008,009,015;010–011 T011–012;012 T008–012;013 T003,008,009;014 T003,008–010,015;
015 T013–015;016 T015;017 T003,013,015;018 T006,007,010,017.
SC001 T004–007,013,016;SC002 T008–010,016;SC003 T011,012,016;SC004 T008,009,015,016;
SC005 T008–010,013–016;SC006 T007,010,017.18 FR and6 SC fully covered;18 tasks.
