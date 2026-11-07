# Feature Specification: Coins, XP and levels

**Feature Branch**: master retained
**Created**: 2026-11-06
**Status**: Implemented; clarifications approved 2026-11-06; evidence and limits in verification.md
**Input**: TODO/specify_moedas_xp_niveis_app.md; current request authorizes specify, plan, tasks and implement.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Earn from tasks (Priority: P1)
The owner earns persistent coins and XP from real work offline.
**Why this priority**: Delivers the core accounting value without fabricated balances.
**Independent Test**: Complete an easy task, relaunch, inspect Hoje and undo it.
**Acceptance Scenarios**:
1. **Given** a new wallet, **When** viewing Hoje, **Then** show 0 coins, 0 XP, level 1.
2. **Given** easy/high-importance work, **When** completing, **Then** award 5 coins/10 XP once;
   confirming failure instead deducts 20 coins and no XP.
3. **Given** checklist items, **When** checking all, **Then** no reward until explicit task completion.
4. **Given** overdue work, **When** opening review, **Then** no automatic failure or penalty occurs.
5. **Given** recurring work, **When** resolving distinct occurrences, **Then** account independently;
   repeated taps, retry and relaunch do not duplicate outcomes.

### User Story 2 - Account for habits (Priority: P1)
The owner earns per practice day and records unwanted events and reviewed period outcomes.
**Why this priority**: Distinct-day goals and event limits need different financial treatment.
**Independent Test**: Record a three-day practice goal and review a one-event/week limit.
**Acceptance Scenarios**:
1. **Given** a three-days/week goal, **When** recording three distinct days, **Then** reward each once,
   reject further practice in the period and give no closing bonus; same-day duplicates earn nothing extra.
2. **Given** two of three days, **When** confirming period failure, **Then** keep earnings and deduct
   one importance penalty for the period, never one per missing day.
3. **Given** maximum one event/week, **When** recording three events, **Then** only the second and third
   incur penalties; multiple same-day events and additional events remain recordable.
4. **Given** avoid work, **When** confirming prohibited events, **Then** penalize each event once.
5. **Given** an avoid/limit period within goal, **When** confirming complete records after period end,
   **Then** award one difficulty reward; zero events without confirmation earns nothing.
6. **Given** an exceeded period, **When** closing review, **Then** no reward or extra closing penalty.
7. **Given** corrected practice, **When** its target becomes incomplete, **Then** accept valid practice
   again and reconcile prior awards without double payment.

### User Story 3 - Correct mistakes and inspect history (Priority: P1)
The owner corrects records with attributable financial consequences and no hidden debt.
**Why this priority**: Trustworthy correction is necessary for every balance.
**Independent Test**: Floor-clip a penalty, undo it and inspect linked history.
**Acceptance Scenarios**:
1. **Given** balance −295 and nominal penalty 20, **When** confirming, **Then** deduct 5, end at −300,
   retain nominal/effective amounts and refund exactly 5 when undone.
2. **Given** balance −300, **When** confirming an incident, **Then** retain a zero-impact history entry
   without deferred debt charged against future gains.
3. **Given** an old award and edited tables, **When** undoing, **Then** reverse original coins/XP,
   clipping coins at −300 and recalculating level downward if needed.
4. **Given** failed writes or process interruption, **When** retrying, **Then** source/accounting agree
   durably without partial success, phantom XP, duplicate rewards or repeated refunds.
5. **Given** deleted/excluded source work, **When** inspecting history, **Then** provenance and amounts
   remain; source deletion/exclusion itself neither penalizes nor reverses earnings.

### User Story 4 - Understand and configure progress (Priority: P2)
The owner understands consequences before commitment and edits future earning/penalty values.
**Why this priority**: Makes rewards predictable and configurable.
**Independent Test**: Edit settings, inspect a form estimate and compare historical values.
**Acceptance Scenarios**:
1. **Given** task/habit forms, **When** choosing difficulty/importance, **Then** show estimated coins/XP
   and potential penalty with access to the table/explanation.
2. **Given** old operations, **When** editing tables, **Then** preserve committed values and reversals;
   distinguish current estimates from historical amounts.
3. **Given** a clipped consequence, **When** confirming, **Then** disclose effective change and floor.
4. **Given** total XP 100/250/450, **When** viewing progress, **Then** show levels 2/3/4 and distinguish
   total XP from XP within the current level and the next transition cost.
5. **Given** Recompensas is unavailable, **When** opening, **Then** show an honest state without a fake
   shop, synthetic debits, sample transactions or a manual minting control.

### Edge Cases
- Exact thresholds, multiple-level gains, correction-related level reduction and large accumulated XP.
- Same-day duplicates, equal-time unwanted events, retroactive limit reclassification and reopened review.
- Floor-clipped reversal/re-resolution chains and edited tables between outcomes.
- Pre-existing records, open old work and partially active periods at wallet activation.
- Paused/excluded dates, deleted lists, civil-date boundaries and reviewed overdue rescheduling.
- Invalid table input, failed writes, repeated actions, screen recreation and process death.
- Portuguese long labels, landscape, 200% fonts and light/dark/system themes.

## Requirements *(mandatory)*

### Escopo e restrições do Ethos
Offline coins, XP, levels, editable tables and financial history integrate existing tasks/habits,
Hoje, settings and approved compact navigation. Preserve data and appearance. No new permissions,
accounts, backend, synchronization, personal logs or real money.
Exclude reward CRUD/redemption/spending, reminders, timers, energy/health, achievements, streak bonuses,
multipliers, manual balance editing, custom formulas, per-item overrides and unrelated redesigns.
Future redemption requires enough coins for full cost and cannot create debt; no spending UI is authorized.

### Functional Requirements
- **FR-001**: Start at 0 coins/0 XP/level 1 without sample balance, signup bonus or manual grants.
- **FR-002**: Difficulty alone determines gains: Fácil 5 coins/10 XP, Média 10/20, Difícil 20/40.
  Importance alone determines coin penalty: Baixa 5, Média 10, Alta 20; ordinary failure never removes XP.
- **FR-003**: Preserve Média/Média defaults and Difícil→Alta suggestion without overriding manual importance.
- **FR-004**: Award once per task occurrence completion; penalize confirmed Não será feita only.
  No checklist reward or elapsed-time penalty. Preserve explicit missed-deadline rescheduling review;
  planning must map that recorded outcome explicitly without bypassing or duplicating financial effects.
- **FR-005**: Reward valid distinct practice days up to target; reject additional practice after target,
  retain correction/deletion, allow practice again when incomplete and give no closing bonus.
- **FR-006**: Confirmed incomplete practice incurs one importance penalty per obligation/period,
  retaining day earnings; reuse scheduled-day obligations and flexible-period identity.
- **FR-007**: Penalize each confirmed prohibited avoid event and each limit-exceeding event once;
  within-limit events cost nothing; continue accepting excess events, including multiple on one date.
- **FR-008**: Reward successful avoid/limit periods once after end-of-period completeness confirmation.
  Exceeded periods receive no reward or additional closing penalty; missing records imply no success.
- **FR-009**: Inactive intervals create no unearned fulfillment/penalty. Exclusion/restoration/deletion,
  moving lists, title/appearance edits, history browsing and streak recalculation cannot themselves mint
  currency or reverse historical earnings. Preserve existing task/habit date and review rules.
- **FR-010**: Minimum balance is −300. Retain nominal and effective coin changes and zero-impact
  incidents, with no deferred debt. Undo penalties refunds only their original effective deduction.
- **FR-011**: Undo gains reverses original committed coins/XP, clipping coins at the floor and recomputing
  level. Preserve original financial entries and link corrections instead of rewriting historical amounts.
- **FR-012**: Reconcile changed habit days/events/period outcomes/reviews without duplicate active gains,
  repeated refunds, event penalties or double closing penalties.
- **FR-013**: Persist source outcomes and financial effects consistently across write failure/process death;
  repeat review, refresh, recurrence generation and retries cannot apply an outcome twice.
- **FR-014**: Accumulate XP without consuming it at level-up. Transition n→n+1 costs 100+50×(n−1);
  level n starts at 25×(n−1)×(n+2) total XP. No level cap. Handle exact thresholds and multi-level gains.
  Level 99→100 costs 5,000 XP; level 100 begins at 252,450 total XP. Prevent overflow/rounding errors.
- **FR-015**: Edit category coin gains/XP gains/coin penalties prospectively in settings; preserve each
  operation's committed values and original-value reversals rather than repricing history.
- **FR-016**: Show form estimates and explanation/table access, and disclose consequences in relevant
  confirmations, including effective clipping. Distinguish estimates from historical committed prices.
- **FR-017**: Hoje shows live coins/level/next-level progress in approved compact positions/routes.
- **FR-018**: Financial history shows reason, actual date, entry/correction time, applied delta and
  resulting balance, with understandable original/correction links and provenance surviving source deletion.
  Future redemptions appear only when that future feature produces them; never fabricate records.
- **FR-019**: Preserve all existing tasks/habits/history/preferences during activation;
  Activation keeps old history unpriced. New actions after activation count, including old open tasks and newly entered retroactive records; undo of an unpriced old outcome has no financial effect.
- **FR-020**: Preserve prospective pricing;
  Use the table at first financial entry, even for retroactive dates. Corrections and re-resolution preserve that price. Inputs are integers 0 through 10,000; no reset-defaults control.
- **FR-021**: Correction chains must not permit coin minting through floor clipping;
  Re-completion restores only the effective coins removed by its prior reversal, plus original XP. Order limit events by actual date, entry time, then stable identity; reconcile reclassification through linked adjustments preserving original prices.
- **FR-022**: Honest loading/empty/content/error states, retained input/retry on write failure, accessible
  Portuguese text and reachable actions at enlarged fonts in light/dark/system themes. No false success.

### Key Entities *(include if feature involves data)*
- Wallet/progress: effective balance, accumulated XP, derived level and next-threshold progress.
- Price configuration: category gains/penalties, effective timing and committed historical values.
- Financial operation: source/outcome identity, reason, nominal/effective coins, XP, resulting balance,
  actual date, entry time, original/correction link and retained human-readable provenance.
- Activation boundary: distinguishes existing history from eligible new operations.
- Source outcome: task occurrence, practice day, unwanted event or confirmed habit period.

## Success Criteria *(mandatory)*

### Measurable Outcomes
- **SC-001**: All default-table scenarios match exact approved values; ordinary failure removes zero XP.
- **SC-002**: All repeat/retry/relaunch cases produce exactly one effective outcome, zero duplicate
  rewards/penalties/refunds and zero source/accounting inconsistencies.
- **SC-003**: Every floor/correction case stays at or above −300 with matching displayed balances,
  zero deferred debt and refunds matching the original effective deductions.
- **SC-004**: All habit counting/review cases match approved rules with zero automatic closing awards,
  double period penalties or extra practice-day rewards.
- **SC-005**: All threshold cases match the linear curve, including 252,450 XP for level 100;
  all historical values remain unchanged after table-edit scenarios.
- **SC-006**: Activation/relaunch preserves 100% of existing data/preferences and committed financial
  operations, with no fabricated transactions or unauthorized back-payment/back-charging.
- **SC-007**: Primary actions remain reachable in both themes, landscape and 200% fonts; all tested
  failures offer recovery and show zero false saved confirmations.

## Assumptions
- Existing foundation/shell/tasks/habits are implemented. Inspected task/habit repositories transact
  writes and retain occurrence/record/review audits. At feature start, database version was 2 with schemas 1/2.
- Existing civil-date, recurrence, daily avoid review, active-interval and correction policies apply.
- Active template is .specify/templates/spec-template.md; no preset override found.
- Current request supersedes the saved prompt's specify-only handoff; push/publication/version
  bumps remain unauthorized. Commits were later explicitly authorized after completion. Clarification groups 1A, 2A and 3A were explicitly approved.
- Source transcript ID/link/path is unavailable; the prompt and TODO/planejamento_ponto_5.md convey decisions.
- Technical consistency/numeric/migration design belongs to plan after clarifications.
- Previous evidence covers API36; API24, physical hardware and auditory TalkBack remain unverified.

## Clarifications

### Session 2026-11-06
- User approved 1A, 2A, 3A as encoded in FR-019/020/021.
- Activation snapshots existing outcomes as unpriced, without sweeping old history into the wallet.
- Correction timestamps remain distinct from practice/completion dates.
