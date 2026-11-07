# Specify — Ponto 4: hábitos e metas

**Estado:** pronto para executar specify em outro chat.
**Data:** 2026-11-06.

## Como usar

Na raiz `C:/Users/gabri/Projetos/ethos`, execute `$speckit-specify` com o corpo em inglês
após `---`. Execute somente specify. Plan, tasks, analyze, implement e commits exigem solicitação
separada. Salvar este prompt não executa Spec Kit.

## Referências e origem

- `AGENTS.md` e `.specify/memory/constitution.md` v1.0.0.
- `specs/001-android-foundation/`, `specs/002-app-shell/`, `specs/003-task-lists/`:
  especificações, planos e evidências existentes.
- `TODO/planejamento_ponto_4.md`: decisões de hábitos desta rodada.
- `TODO/planejamento_ponto_2.md`: contexto de revisão e gamificação futura.
- `TODO/planejamento_lembretes_tempo.md`: lembretes e tempo, fora deste ponto.
- `prototypes/hoje/index.html`: direção visual B aprovada, com valores apenas ilustrativos.
- Origem: conversa de planejamento do Ethos em 2026-11-06. ID, link e caminho de transcript
  indisponíveis neste contexto. O corpo registra as decisões necessárias.
- Não existe contraparte de backend.

---

Create the feature specification for Ethos point 4: habits and goals. Execute only the Spec Kit
specify stage. Do not implement, generate plan/tasks, commit, publish, or run subsequent stages.

## Inspect current state and preserve existing work

Work in C:/Users/gabri/Projetos/ethos. Read AGENTS.md, constitution v1.0.0, the first three feature
specifications/plans/verification, and relevant Kotlin code before specifying changes. The user
designs features in this conversation and hands saved prompts to another chat. Do not ask again
about the settled decisions below or treat this handoff as implementation authorization.

Foundation, visual shell, and lists/tasks were implemented in another chat. The app uses native
Kotlin, Compose Material 3, AGP 9 built-in Kotlin, Hilt/KSP, Navigation Compose, Preferences DataStore
for light/dark/system appearance, and minimum SDK 24. It already has personal lists, Inbox,
task checklists, deadlines, recurrence, correction history, and a combined pending-review shell.

At handoff, TaskDatabase is a Room database at schema version 1, exportSchema=true, with lists,
task definitions, occurrences, checklists, and audits. Inspect actual source and exported schemas
because another chat may have changed them. Extend/reuse shared list identity and shell integration;
do not rebuild tasks or create a second unrelated system of user lists.

There is no implemented habit engine or live coins/XP/levels/reminder engine at handoff. Existing
sample presentation data is not a habit domain. Check actual verification files before claiming
coverage; the managed context reports API 36 checks and no API 24/device/TalkBack audio validation.

## Purpose and scope

Implement genuinely usable, offline habit management and goals: create, organize, record, review,
correct, pause, edit, preserve history, delete from active tracking, and restore. Integrate habits
into Hoje, Hábitos, Inbox, per-list Hábitos tabs, and Revisar pendências in the existing approved
model B. Do not change the approved navigation merely to introduce habits.

Use Room as the source of truth, with schema export and tested non-destructive migrations for
changes to the existing schema. Preserve tasks, lists, checklist/audit data, and appearance settings.
Keep domain rules independent of Android, state immutable and lifecycle-aware, and persistence
failures recoverable. Exact schema, repository boundaries, library versions, algorithms, and
transaction design belong to the later plan. Avoid speculative abstractions or new modules.

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
- Later user clarification during point 5: once a practice target is met, reject additional
  practice entries for that period. Corrections/deletions remain available; if they make the
  target incomplete, allow valid entries again. Do not pay a target-closing bonus or extra-day
  rewards. This restriction does not apply to avoid/limit events, which remain recordable.
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

## Future gamification semantics, not a live engine here

The accepted future model has coins, XP/levels, no energy, difficulty and importance. Penalties
affect coins only, with balance never below −300. Failures do not remove XP or levels. Each
confirmed prohibited/excess occurrence is a potential penalty, but close-of-period review must
not duplicate it. Correcting records will adjust corresponding transactions exactly once.

Do not implement live coins/XP, progression formulas, a reward/penalty matrix, or reward redemption
as a dependency of making habits work. Preserve domain identity/provenance for later integration
without a speculative plugin/event framework. Leave unavailable monetary displays truthful.
Prototype numeric values and sample rewards are not approved balancing defaults.

## Explicitly deferred

- Live coins, XP/levels, accounting, balance changes, final difficulty/importance formula,
  transaction reversal implementation, rewards, and reward redemption.
- Notifications, reminders, permissions, alarms, push services, and background reminder delivery.
- Stopwatch/countdown sessions, duration goals, weekly time totals, timed tasks, or timed rewards.
  These remain wanted future features in TODO/planejamento_lembretes_tempo.md.
- Accounts, backend, synchronization, analytics, publishing, monetization, or app blocking.
- Rolling-window goals, automatic proportional targets, exact-frequency rules beyond those
  explicitly agreed, or arbitrary units not discussed by the user.

## Acceptance and verification expectations

Write independently testable scenarios covering:
1. Create/edit/organize habits with the agreed fields, Inbox support, list tabs, defaults, and
   preservation of manual importance choices; existing task flows remain working.
2. Daily/scheduled/flexible weekly/monthly practice; distinct-day counting; no same-day repeated
   credit; no automatic substitution of scheduled weekdays.
3. Immediate recognition of completed practice goals, incomplete-period review aggregation,
   and correct day/week/month boundaries including short months and leap years.
4. Avoid/multiple-occurrence recording; daily/weekly/monthly maximum limits; within/over-limit
   classification; completeness confirmation even with zero records; no duplicate penalty trigger.
5. Partial-period overrides for practice and limits, warnings for impossible distinct-day targets,
   and normal goals in subsequent full periods without silent prorating.
6. Retroactive records, creation/future bounds, date versus entry timestamp, record corrections,
   deletion of erroneous records, and recalculation of goals/review/streak state.
7. Pause/resume with preserved old pending periods, no paused-interval demands, partial-goal
   adjustment, effective-date choices for goal edits, and unchanged prior period configuration.
8. History calendar/progress, streak units, neutral pauses/non-scheduled dates, unresolved review,
   confirmed failures, preserved best streak, and corrected historical sequences.
9. Excluded habit history retained, Hábitos excluídos consultation/restoration, list deletion
   integration, and no retroactive charges or duplicate rewards after restoring.
10. Real Hoje/review content and filters, Metas cumpridas, persistent visibility of avoid/limit,
    no fictitious production data, and accessibility/large-font/light/dark compatibility.
11. Room schema migration preserving existing tasks/lists/audits, persistence across relaunch,
    meaningful rule/database/UI tests with controllable time and recoverable persistence errors.
12. Relevant build, lint and regression checks when implementation is later requested, with
    executed evidence and unavailable runtime/device coverage reported separately.

## Remaining consistency decisions

The settled user choices above are authoritative. Do not reopen them. Minor implementation details
belong to planning; genuine unspecified product interactions must be identified for normal clarify
rather than silently attributed to the user. Examples: records during paused/excluded intervals,
changing habit type or period length, a deleted destination list on restore, the exact avoid-habit
review period, and streak/accounting effects of retroactively correcting an already reviewed
period. Do not invent these as previously accepted rules or expand into notifications/currency
to resolve them. Reuse the existing task date policy when appropriate only after inspecting it.
