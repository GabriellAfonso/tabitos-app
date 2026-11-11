# Feature Specification: Lists, tasks and recurrence

> Spec 012 updates the card checkbox: direct completion/reopening, without checklist/financial
> confirmation. Completed tasks appear in an initially expanded Concluídas section at the end.
> Detail/notification flows retain their confirmations and recurrence guards remain unchanged.

**Feature Branch**: master (retained)
**Created**: 2026-11-06
**Status**: Implemented; clarifications approved on 2026-11-06; evidence/limitations in verification.md
**Input**: TODO/specify_listas_tarefas_app.md; current request authorizes specify, plan, tasks and implement.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Capture and organize (Priority: P1)
The owner captures real work offline without first configuring lists.
**Why this priority**: This is the first usable task workflow.
**Independent Test**: Capture in Inbox, organize into two lists, reorder and relaunch.
**Acceptance Scenarios**:
1. **Given** no lists, **When** saving only a title, **Then** a pending Inbox task survives reopening.
2. **Given** lists, **When** editing name/color/icon/order, **Then** all changes persist.
3. **Given** a task, **When** moving between lists/Inbox, **Then** identity/history remain intact.
4. **Given** pending/completed work, **When** viewing a list, **Then** pending work comes first with
   manual order, and completed work occupies a collapsible section.
5. **Given** a populated list, **When** deleting, **Then** confirm removal of all contents; cancel
   preserves contents, confirm removes them without moving to Inbox or applying a penalty.

### User Story 2 - Complete and correct (Priority: P1)
The owner maintains task details/checklists and records actual completion accurately.
**Why this priority**: Records must reflect work and remain correctable.
**Independent Test**: Complete with unchecked items, enter a forgotten completion and reopen it.
**Acceptance Scenarios**:
1. **Given** a new task, **When** editing, **Then** only title is required; difficulty/importance start Média.
2. **Given** untouched importance, **When** choosing Difícil, **Then** suggest Alta; manual importance
   takes precedence across later difficulty changes.
3. **Given** two unchecked items, **When** completing, **Then** ask confirmation with remaining count;
   cancel returns to checklist and confirm completes. Checking all items never auto-completes.
4. **Given** work created yesterday, **When** recording yesterday's completion today, **Then** actual
   completion differs from entry time; reject completion before task creation.
5. **Given** completion or Não será feita, **When** undoing, **Then** reopen with attributable correction
   history and no duplicate outcome, reward or reversal.

### User Story 3 - Follow deadlines and review (Priority: P1)
The owner sees today's work and explicitly reviews expired deadlines without assumed failure.
**Why this priority**: A missed deadline does not prove the task was not done.
**Independent Test**: Exercise no-date/day/week deadlines across day/Friday/Sunday boundaries.
**Acceptance Scenarios**:
1. **Given** an undated task, **When** dates advance, **Then** it stays in its list/Inbox without Hoje/review.
2. **Given** a dated task with time, **When** time passes, **Then** no failure/review occurs until day end.
3. **Given** weekly work, **When** opening Hoje Tudo/Tarefas, **Then** it appears after day work under
   “Nesta semana · sem dia definido” and can be completed without choosing a day.
4. **Given** expired Friday/Sunday work, **When** opening the app, **Then** review retains the old period,
   with no automatic failure/penalty or relabeling as new-week work.
5. **Given** overdue work, **When** reviewing, **Then** allow completion, Não será feita, deletion or
   review of original deadline before rescheduling; counters and Hoje/drawer notices agree.
6. **Given** zero review entries, **When** navigating, **Then** hide notice/badge and retain review access;
   Hábitos filtering remains an honest empty state.

### User Story 4 - Repeat and change routines (Priority: P2)
The owner schedules recurring work and changes its future without rewriting earlier history.
**Why this priority**: Repeated work must not drift, disappear or duplicate after absence.
**Independent Test**: Check every pattern, ending, scope and correction using controlled dates.
**Acceptance Scenarios**:
1. **Given** daily fixed-calendar work Monday–Wednesday, **When** opening Thursday, **Then** three
   distinct review occurrences remain, without automatic penalties or duplicates after reopening.
2. **Given** monthly day 31, **When** crossing February, **Then** clamp to its last day and return to
   March 31; include February 29 in leap years.
3. **Given** every-X-days after completion, **When** resolving, **Then** completion uses actual date + X;
   Não será feita uses planned date + X, even already overdue. Before resolution keep one pending occurrence.
4. **Given** ending on a date, **When** scheduled exactly then, **Then** include it; after-X ending counts
   scheduled slots including completed/Não será feita, and retains prior unresolved work for review.
5. **Given** edits/deletions, **When** selecting occurrence-only or this-and-future, **Then** scope applies
   to all attributes/checklist and earlier history is preserved. Não será feita affects one occurrence only.
6. **Given** a new occurrence, **When** viewing checklist, **Then** labels persist but all checks start clear.

### Edge Cases
- Midnight/Friday/Sunday rollover, timezone/clock changes and long app absence.
- Leap years, short months, inclusive series endings and deleted finite-series slots.
- Corrections with later acted-on occurrences, future completion and overdue rescheduling.
- Failed writes, repeated taps and process death must not create false success or duplicates.
- Portuguese long labels, 200% fonts, landscape and light/dark/system appearance.

## Requirements *(mandatory)*

### Escopo e restrições do Ethos
Extend the implemented shell using real local lists/tasks, occurrences, checklists, recurrence,
review and correction history. Preserve compact model B, navigation, themes and type filters.
No accounts, network services or new runtime permissions. Never log task contents.
Exclude habit CRUD/targets, financial/XP/level engines and formulas, reward redemption, reminders,
notifications, timers, backend, synchronization, analytics, publishing, advanced subtasks,
attachments and separate Projects. Calendar work is limited to justified task/date integration.
List Hábitos tabs remain honest future/empty states. Never seed prototype data or financial numbers.

### Functional Requirements
- **FR-001**: Create/edit/rename/reorder lists with name/color/icon and persistent ordering.
  Open tasks in a selected list support long-press drag reorder with edge scrolling and persistent order;
  expose equivalent accessibility actions. Commit one validated order on drop, preserve hidden slots,
  and leave stored order unchanged on gesture cancellation or failed writes.
- **FR-002**: Provide system Inbox, not a renameable/deletable user list; move tasks without changing history.
- **FR-003**: Explicitly confirm list/content deletion, including future habits; no Inbox move or deletion
  penalty. Deletion must not reverse existing future financial transactions or destroy their provenance.
- **FR-004**: Require nonblank title; support optional description/list/deadline/time/recurrence/checklist.
  Difficulty is Fácil/Média/Difícil and importance Baixa/Média/Alta; both default Média.
- **FR-005**: Difícil suggests Alta only until a manual importance choice; preserve that choice thereafter.
- **FR-006**: Persist manual pending order independently of completion/history; collapse completed work.
- **FR-007**: Checklist items have labels/order and independent checks, no independent dates/rewards.
  Completion is explicit; confirm unchecked count. New occurrences retain labels with checks cleared.
- **FR-008**: Support no date, specific day with optional time, week through Sunday and weekdays-week
  through Friday. Week starts Monday; weekdays exclude holidays logic.
- **FR-009**: Undated work never automatically enters Hoje/review. Day work appears on its day; current
  weekly work follows it under the approved weekly label. Expired work never becomes new-period work.
- **FR-010**: Review unresolved day work after day end and weekly work after period end; passing time
  alone never marks failure or applies penalties. Optional time does not trigger early review.
- **FR-011**: Real review supports completion/Não será feita/deletion/reviewed rescheduling, filters,
  matching total drawer count and Hoje notice hidden at zero; habits remain empty.
- **FR-012**: Separate actual completion from entry date; reject completion before creation.
  Reject future completion. Use device-local civil dates; saved deadlines do not shift after timezone changes.
- **FR-013**: Preserve original missed deadline review before rescheduling.
  Record an explicit missed-deadline review outcome, preserving the original deadline and keeping the task open with the new deadline.
- **FR-014**: Support undo completion and Não será feita with stable occurrence/outcome identities and
  attributable corrections, preventing duplicate records and retaining exact future reversal provenance.
- **FR-015**: Implement daily, weekdays, weekly chosen weekday, multiple custom weekdays, monthly
  chosen day and every-X-days recurrence, with fixed-calendar and after-completion interval modes.
- **FR-016**: Monthly rules clamp to last day while preserving the original selected day for later months.
- **FR-017**: Fixed-calendar cadence remains anchored regardless of completion; preserve every missed
  occurrence individually and prevent duplicates across launches/recalculation.
  In the task-list screen, show only the earliest unresolved occurrence (by series ordinal) per recurring
  task. Resolving or deleting it reveals the next unresolved occurrence, including a future one when no
  earlier occurrence remains. Keep resolved history available in its existing section. This presentation
  filter does not delete occurrences or change Hoje, review, calendar, reminders or recurrence generation.
- **FR-018**: After-completion mode retains one unresolved occurrence; completion schedules actual date
  + X, Não será feita schedules planned due date + X, even if already overdue at late review.
- **FR-019**: Series start date and endings Nunca/inclusive date/after-X scheduled occurrences are
  functional; failures count and already-due unresolved work survives ending.
- **FR-020**: Offer occurrence-only and this-and-future edit/delete; preserve earlier history and scope
  every attribute/checklist change. Single exceptions retain fixed cadence. Deleted slots count toward finite limits; future edits retain consumed counts. Block dependent undo until later acted-on occurrences are undone; discard only untouched generated successors when recalculating.
- **FR-021**: Persist lists/tasks/checklists/occurrences/rules/review/corrections across process death;
  failures expose retry and retain unsaved input without claiming success.
- **FR-022**: Preserve appearance, Portuguese interface, accessible action labels and usable enlarged-font
  layouts; provide loading/empty/content/error states where applicable.
- **FR-023**: Do not fabricate financial values. Future correction must reverse exact awards or actual
  floor-limited deductions once, never duplicate re-completion rewards, and never treat deletion as reversal.

### Key Entities *(include if feature involves data)*
- **List**: name/color/icon/order, owns tasks; Inbox is a distinct system destination.
- **Task**: identity, creation date, title/description, membership, difficulty/importance.
- **Occurrence**: scheduled unit, original/current deadline, state/order and independent checklist checks.
- **Recurrence definition**: start, pattern/anchor, ending and scoped changes.
- **Checklist item**: ordered label and occurrence-specific check; not a separately rewarded task.
- **Review/outcome/correction**: stable occurrence reference, original deadline, actual completion date,
  entry time, explicit resolution/review and attributable undo history for future integration.

## Success Criteria *(mandatory)*

### Measurable Outcomes
- **SC-001**: Capture one title-only task offline without list setup; all saved fields, order and checks
  survive process termination/relaunch in every persistence scenario.
- **SC-002**: All deadline-boundary scenarios produce expected Hoje/review membership and matching
  counters, with zero automatically failed or penalized tasks.
- **SC-003**: All six recurrence families and both interval modes pass short-month/leap-year/absence/
  relaunch/ending scenarios with zero missing or duplicate scheduled occurrences.
- **SC-004**: All correction/scoped-edit scenarios retain unaffected history and independent checks;
  organization changes create zero implicit rewards, penalties or completions.
- **SC-005**: Primary actions remain reachable in both themes, landscape and 200% fonts; unavailable
  device/screen-reader validation is explicitly reported.
- **SC-006**: Every simulated write failure exposes recovery with zero false success confirmations;
  retry preserves input and produces exactly one intended record.
- **SC-007**: Runtime inspection finds zero prototype records, fake currency mutations, account
  prerequisites or new runtime permissions.

## Assumptions
- Verified source contains shell presentation models and appearance preferences, no task database/domain.
  Foundation/app-shell specifications, plans and verification are prerequisites, not replaced here.
- Current user request supersedes saved specify-only instructions; commits/publication remain unauthorized.
- Technical planning resolves schema, transactions, clock control, occurrence materialization/paging
  and dependency versions according to the constitution, after product clarifications.
- No transcript identifier is available. Sources: input prompt, TODO/planejamento_ponto_3.md,
  TODO/planejamento_ponto_2.md, TODO/planejamento_lembretes_tempo.md and approved model B prototype.
- The user approved all three proposed clarification groups on 2026-11-06. Completion dates use local civil dates and cannot be in the future; reviewed rescheduling retains the open occurrence; recurrence exceptions/counts/dependent undo follow FR-020.

## Clarifications

### Session 2026-11-06
- Q: Date policy? A: Reject future completion; device-local civil dates, saved dates stable across timezone changes.
- Q: Overdue rescheduling? A: Explicit missed-deadline review record and same open task with new deadline.
- Q: Recurrence consistency? A: Fixed cadence for exceptions, deleted slots count, preserve consumed limits; undo later acted-on successors first.

Recurring work may use day or weekly-period deadlines. For weekly periods, the scheduled recurrence date identifies the containing week; it does not require completing on that day. Series endings apply to scheduled occurrence anchors.
