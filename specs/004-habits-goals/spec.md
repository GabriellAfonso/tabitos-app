# Feature Specification: Habits and goals

> Spec 014 supersedes the review rules here: habit periods close by themselves 2 days after their end
> (success or missed), never become pending, never go to Revisão and missed periods cost no coins.
> Avoid/limit succeed by default without events. Records are accepted only before a period closes.

**Feature Branch**: master retained
**Created**: 2026-11-06
**Status**: Implemented; clarifications approved 2026-11-06; evidence and limits in verification.md.
**Input**: TODO/specify_habitos_metas_app.md and current authorization for specify, plan, tasks, implement.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create, organize and practice (Priority: P1)
The owner captures habits offline, optionally organizes them in existing lists and records practice.
**Why this priority**: Delivers useful tracking without list setup.
**Independent Test**: Create in Inbox, record three distinct days, move to a list and relaunch.
**Acceptance Scenarios**:
1. **Given** no lists, **When** saving a named daily habit, **Then** it persists in Inbox.
2. **Given** manual importance, **When** changing difficulty, **Then** preserve importance.
3. **Given** three days/week, **When** recording three times Monday, **Then** progress is one day;
   records Wednesday and Friday complete the goal immediately.
4. **Given** a Monday obligation, **When** recording Tuesday, **Then** Monday remains reviewable.
5. **Given** a ten-days/month goal created November 25, **When** choosing three for November,
   **Then** December uses ten; impossible distinct-day targets require adjustment.

### User Story 2 - Track unwanted events and review periods (Priority: P1)
The owner records avoid/limit events and explicitly resolves incomplete or unconfirmed periods.
**Why this priority**: Missing data must not become assumed success or failure.
**Independent Test**: Record two same-day events against one/week and review after Sunday.
**Acceptance Scenarios**:
1. **Given** one/week limit, **When** adding two same-day events, **Then** only the second exceeds it.
2. **Given** zero events after an avoid day, **When** reviewing, **Then** ask completeness confirmation.
3. **Given** two of three weekly practice days, **When** the week ends, **Then** create one period
   review and no automatic failure, penalty or streak break.
4. **Given** tasks and habits pending, **When** filtering, **Then** counts match the drawer and Hoje.

### User Story 3 - Correct and consult history (Priority: P1)
The owner enters forgotten records, corrects errors and sees trustworthy progress and sequences.
**Why this priority**: Historical correctness is a prerequisite for future accounting.
**Independent Test**: Record yesterday, correct date, delete an error and inspect the calendar/streak.
**Acceptance Scenarios**:
1. **Given** an active date since creation, **When** recording it, **Then** count its actual date,
   retain entry time and reject future dates or dates before creation.
2. **Given** a reviewed period, **When** a correction changes its outcome, **Then** reopen its review,
   recompute progress/streaks and retain correction provenance.
3. **Given** a pending period between successes, **When** displaying streaks, **Then** disclose
   unresolved continuity; confirmed failure breaks current streak and preserves best.

### User Story 4 - Adapt tracking without losing history (Priority: P2)
The owner pauses, edits, excludes and restores habits while preserving historical rules.
**Why this priority**: Changes must not erase history or create retroactive demands.
**Independent Test**: Pause midweek, resume, change next-period goal/type, exclude and restore.
**Acceptance Scenarios**:
1. **Given** old pending periods, **When** pausing, **Then** keep old reviews, suspend inactive-date
   demands and offer explicit partial-week/month adjustment.
2. **Given** a goal edit, **When** choosing current or next period, **Then** preview current
   recalculation before confirmation and retain older goals.
3. **Given** records, **When** changing type/period, **Then** apply only after the current old-rule
   period ends, preserving prior rules and records.
4. **Given** exclusion directly or through list deletion, **When** inspecting Hábitos excluídos,
   **Then** history remains available; restore treats excluded dates as pause.
5. **Given** the original list was deleted, **When** restoring, **Then** require a destination choice
   among Inbox and available lists; offer partial-period adjustment without retroactive obligations.

### Edge Cases
- Monday/Sunday rollover, short/leap months, long absence and timezone changes.
- Repeated taps, multiple same-day practice records and equal-time unwanted events.
- Occurrence limits greater than available days versus impossible distinct-day goals.
- Same-period pause/resume, excluded dates, deleted lists and corrected reviewed outcomes.
- Write failure, process death, long Portuguese labels, landscape and 200% font scale.

## Requirements *(mandatory)*

### Escopo e restrições do Ethos
Offline habits integrate existing Hoje/Hábitos/Inbox/list tabs/history/single review destination.
Preserve approved model B navigation, tasks and appearance. No permissions or personal-content logs.
Exclude live coins/XP/levels, ledger/formulas, reward redemption, reminders/notifications, timers,
rolling goals, backend/accounts/sync, analytics, publishing and app blocking.

### Functional Requirements
- **FR-001**: Create/edit/organize with approved fields, Inbox and manual-importance preservation.
- **FR-002**: Practice daily, on selected weekdays, distinct days per calendar week/month.
- **FR-003**: Recognize practice success immediately; keep incomplete period/day obligations reviewable.
- **FR-004**: Count avoid events individually; use daily completeness review even with zero events.
- **FR-005**: Count limit events individually per chosen day/week/month; classify within/excess once.
- **FR-006**: Explicit initial/paused/resumed partial-period overrides; validate distinct-day capacity,
  never silently prorate, waive or apply day-count restrictions to occurrence limits.
- **FR-007**: Bounded retroactive records, actual date separate from entry timestamp, correct/delete
  with stable provenance and recomputation of progress/review/streaks.
- **FR-008**: Block record dates in paused/excluded intervals; allow correction of active-date records.
- **FR-009**: Pause preserves old pending work; fully inactive periods create no demands.
- **FR-010**: Goal edits offer current-period preview or next-period effect; type/period changes are
  future-only, effective after the current old-rule period. Retain historical configuration.
- **FR-011**: Show history calendar/progress/current and best streak with neutral inactive dates,
  disclosed pending continuity, confirmed failures and recalculation after corrections.
- **FR-012**: Exclude without destroying history, including list deletion. Restore from Hábitos
  excluídos; ask destination if the original list is unavailable. No retroactive demands/rewards.
- **FR-013**: Real Hoje/type filters/review counters; collapsible Metas cumpridas for attained flexible
  practice; avoid/limit stay visible. No competing review/navigation system or fictitious data.
- **FR-014**: Reopen reviewed periods if corrections alter outcome; closure cannot duplicate event
  accounting triggers. Preserve stable identity/provenance without implementing money/XP.
- **FR-015**: Persist across relaunch/upgrades preserving tasks/lists/checklists/audits/appearance.
  Failed writes retain input, expose retry, never claim success; repeated actions do not duplicate.
- **FR-016**: Portuguese accessible light/dark/system interface, honest loading/empty/content/error,
  reachable actions at large fonts, truthful unavailable monetary displays.

### Detailed approved behavior
The following normative rules expand FR-001 through FR-016; clarification below resolves open cases.
## Fields, lists, and defaults

- Required name; optional description; color; icon; optional list; difficulty; importance;
  habit type and its goal/limit/schedule configuration.
- Habits can live in Caixa de entrada without a personalized list. Do not require a list.
- Reuse existing lists and their separate Tarefas/Hábitos tabs, rather than treating a habit
  as a checklist task or inventing a separate list hierarchy.
- Difficulty: Fácil/Média/Difícil. Importance: Baixa/Média/Alta. Both start at Média.
- Selecting Difícil suggests Alta only when importance has not already been explicitly adjusted;
  preserve manual importance choices across difficulty changes, as with tasks.

## Habit types: practice, avoid, limit

Practice builds a behavior through daily goals, specified weekdays, or a number of distinct days
within a calendar week or month. Avoid records unwanted occurrences. Limit allows a maximum
number of occurrences per selected day/week/month. These count differently and must not share
an ambiguous generic counter.

### Practice goals

- Offer specified days (e.g. Monday/Wednesday/Friday) and flexible frequency within a period.
- Include daily, weekly, and monthly practice goals in this first habit version.
- Weekly flexible goals count DISTINCT DAYS: three practices per week means practice on three
  different days; three records on Monday cannot satisfy the goal. Same rule for monthly goals,
  such as ten distinct days in a month. Do not offer same-day multiple-credit as an alternative.
- Calendar weeks run Monday through Sunday. Calendar months run from their first to last day,
  not rolling 7-/30-day windows.
- Specified weekdays are separate scheduled obligations: Tuesday practice does not automatically
  substitute for missed Monday. Monday remains reviewable for a forgotten record or confirmed
  failure. A practice on an off-schedule day must not silently satisfy another date.
- Recognize practice success as soon as qualifying records reach the target. No extra confirmation
  is required at Sunday/month end for a goal already met.
- Incomplete flexible weekly/monthly goals produce ONE review item for the whole period, with
  progress such as 2 of 3 days or 8 of 10 days; do not create one failure per missing day.
- Missed scheduled practice days are reviewed for their actual date; do not infer failure solely
  from missing data or impose automatic deductions when the day/period ends.

### Avoid and limit

- Avoid habits allow multiple recorded occurrences per day. Each confirmed prohibited occurrence
  is a separate penalty trigger for the future currency system.
- Limit habits let the user choose a maximum per DAY, WEEK, or MONTH. Count occurrences, not
  distinct days: two events on one day are two occurrences.
- Events within the limit are not penalized. Each event exceeding the limit is independently
  penalized once the currency feature exists. Example: maximum one delivery/week → first allowed,
  second and later each exceed the limit. Do not turn this into one penalty per day or period.
- Keep avoid/limit habits visible in Hoje throughout their active period, even after the limit
  is exceeded, so additional occurrences can still be recorded.
- At period close, ask in review: "Os registros deste período estão completos?". This applies
  even if zero events were recorded. Let the user confirm completeness or add forgotten events.
  No events is not proof of success without this confirmation.
- Closing review must assess actual records without penalizing already-confirmed events again.
  Period failure and event-level excess must not create duplicate accounting triggers.

## First partial period and impossible goals

If a weekly/monthly habit starts partway through a period, let the user set a goal ONLY for that
first partial period. Later complete periods use the normal target. Do not simply waive the
first period, automatically prorate it, or silently choose a target for the user.

Example: create a ten-days/month practice habit on November 25, choose three days for the rest
of November, and use ten days starting December. Warn and ask for adjustment when the distinct-day
goal exceeds available days (e.g. five required, three remaining). Do not silently rewrite the
target or apply this day-count validation to occurrence limits.

The same first-period override applies to maximum-limit habits: a normal four/month limit may
have a different user-chosen initial partial-month limit. An occurrence limit is not constrained
to the number of remaining calendar days.

## Recording and corrections

- Allow current and retroactive records, such as "Fiz ontem" or another selected date.
- Reject dates before habit creation and future dates. Preserve the distinction between the
  actual event/practice date and the time the record was entered.
- Count records in their actual day/week/month, not the entry date.
- Allow correcting or deleting mistaken records; recompute affected progress, review state,
  and streaks. Multiple practice records on the same date must not count as multiple distinct days.
- Preserve enough correction provenance for future exact, idempotent coin/XP adjustments.
  Do not invent formulas, award demonstration currency, or implement a ledger in this feature.
- Later corrections can invalidate previously recognized success or confirm an incomplete goal;
  make consistency explicit rather than freezing stale totals or creating duplicate reviews.

## Pause, resume, and goal edits

- Include Pausar hábito in this first version. Keep history and suspend demands/new pending
  obligations for the paused interval. Existing pending periods before the pause remain reviewable.
- If pause or resume occurs within a week/month, allow the user to adjust the goal for that
  partially active period, using the same explicit adjustment pattern as initial creation.
  Do not silently change the objective or bill fully paused periods retroactively.
- Editing a goal offers "A partir do próximo período" or "Já neste período". Before confirming
  a current-period change, show how its progress will be recalculated. Preserve past period goals
  and history rather than rewriting all past performance from today's configuration.
- Retain historical configuration/active intervals sufficiently to evaluate corrections against
  the correct period rules. The data model is a later planning responsibility, not a license
  to create excessive general-purpose versioning infrastructure.

## History and streaks

- Habit history includes a calendar of records and progress by period, e.g. 3 of 4 days/week.
- Show current streak and previous best, separate from XP and levels.
- Daily habits count qualifying days; weekly goals count successful weeks; monthly goals count
  successful months. For specified weekdays, off-schedule days do not count or break the sequence.
- Paused intervals and unscheduled days neither add to nor break the streak.
- A period awaiting review remains pending without automatically breaking the streak. Do not
  present a confirmed continuous streak through an unresolved period as if it were already known.
- Confirmed failure breaks the current streak while retaining the previous best.
- Corrections recompute streaks, including earlier periods when required. Define how to present
  current provisional progress without awarding multiple sequence credits for one period.

## Deletion is removal from active tracking, not destruction of history

The user explicitly rejected a proposal to delete a habit's past records. Excluding a habit
must remove it from active tracking while preserving its past records/history for consultation.
It must not itself impose a penalty, reverse historical coins/XP, or erase past financial data.

Provide a "Hábitos excluídos" history section where excluded habits can be inspected and restored.
Restoration treats the excluded interval as a pause: no retroactive obligations, with prior
history intact. If restored partway through week/month, offer the same partial-period goal
adjustment as resume. Restoring does not earn duplicate rewards.

This history-preservation rule applies when the containing list is deleted too. The earlier
"delete list and all its contents" means remove those habits from active tracking, not purge
their historical records. Preserve existing task behavior unless a change is required for the
new habit integration. If a deleted list no longer exists when restoring, record an explicit
policy during clarification/planning; a destination for restoration has not been user-approved.

## Hoje and review integration

- Integrate into existing Tudo/Tarefas/Hábitos filters and actual aggregate pending counts.
- Flexible weekly/monthly practice habits appear in Hoje while incomplete, with their progress
  (e.g. 2 of 3 days). Once attained, move to a collapsible "Metas cumpridas" section until the
  next period. Do not reset historical records when a new period starts.
- Avoid/limit habits stay visible, even over limit. Do not put them into Metas cumpridas merely
  because no unwanted event has yet been logged.
- Use the single existing Revisar pendências destination in the drawer, its counter, and Hoje
  notice, rather than adding a bottom navigation item or a separate competing review system.
- Review allows fixing forgotten records before confirming success/failure/completeness. Missing
  records and elapsed time alone never authorize automatic penalties or confirmed streak failures.
- Keep real task and habit counts/type filters correct, with no prototype records seeded into
  production. Empty states and persistence errors must be honest and recoverable.


### Key Entities *(include if feature involves data)*
- Habit: stable identity, creation, fields, shared-list membership and tracking state.
- Period rule: type/schedule/unit/target with effective dates and preserved historical settings.
- Active interval/override: active dates and explicit partial-period target.
- Record/correction: event identity, actual date, entry time and attributable changes.
- Period review: stable habit/period identity, completeness/outcome and linked corrections.
- History/streak: derived progress, current/best and unresolved continuity.

## Success Criteria *(mandatory)*

### Measurable Outcomes
- **SC-001**: Every supported type can be captured offline without personal lists; all saved values
  survive process termination/relaunch in every persistence scenario.
- **SC-002**: All calendar/counting scenarios yield expected progress with zero duplicate day/period
  credits, automatic penalties or assumed failures due solely to missing records.
- **SC-003**: Review shows each pending period once and all total/type counters agree.
- **SC-004**: All correction/edit/pause/restore cases retain unaffected history and produce zero
  obligations for fully inactive periods; future changes leave historical goals unchanged.
- **SC-005**: All failed-write cases offer recovery with retained input and zero false success;
  successful retry creates exactly one intended change.
- **SC-006**: Primary actions remain reachable in both themes, landscape and 200% fonts;
  unavailable device/screen-reader coverage is reported explicitly.
- **SC-007**: Upgrade preservation scenarios retain 100% of existing lists/tasks/checklists/audits
  and appearance; runtime contains zero prototype habits or fabricated financial values.

## Assumptions
- Verified TaskDatabase exports schema 1 with lists/definitions/occurrences/checklists/audits.
  Existing task dates are local civil dates, stable across timezone changes; reuse this policy.
- Existing foundation/shell/tasks are prerequisites. Prior verification covers API36; API24,
  physical device and auditory TalkBack remain unverified.
- No transcript identifier is available. Saved prompt and its referenced TODO documents convey decisions.
- Active spec-template is .specify/templates/spec-template.md; no preset override found.
- Current request supersedes saved specify-only instruction; commits/publication remain unauthorized.

## Clarifications

### Session 2026-11-06
- Future-only type/period edits are allowed and preserve prior rules/history.
- Block records in paused/excluded dates; allow corrections to active-date history.
- When restoring without original list, ask destination explicitly.
- Avoid review is daily; corrections changing a reviewed period outcome reopen review.
- Planning convention: future type/period changes begin after the current old-rule period ends;
  current streak starts a new segment when its unit/type changes; prior best stays in history with unit.
