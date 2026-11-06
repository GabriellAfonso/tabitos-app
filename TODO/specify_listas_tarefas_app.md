# Specify — Ponto 3: listas, tarefas e recorrências

**Estado:** pronto para executar a etapa specify em outro chat.
**Data:** 2026-11-06.

## Como usar

Na raiz `C:/Users/gabri/Projetos/ethos`, execute `$speckit-specify` com o corpo em inglês
após o separador `---`. Execute somente specify. Plan, tasks, analyze, implement e commits
dependem de solicitação separada. Este arquivo não é uma spec já executada.

## Referências e origem

- `AGENTS.md` e `.specify/memory/constitution.md` v1.0.0.
- `specs/001-android-foundation/` e `specs/002-app-shell/`: specs, planos e verificação existentes.
- `TODO/planejamento_ponto_3.md`: decisões desta rodada.
- `TODO/planejamento_ponto_2.md`: regras de revisão, recorrências e integrações futuras.
- `TODO/planejamento_lembretes_tempo.md`: funcionalidades futuras, fora deste ponto.
- `prototypes/hoje/index.html`: referência visual B aprovada; não é especificação completa do domínio.
- Origem: conversa de design do Ethos em 2026-11-06. ID, link e caminho de transcript não estão
  disponíveis; as decisões necessárias estão reunidas abaixo.
- Não há contraparte de backend.

---

Create the feature specification for Ethos point 3: locally persisted lists, tasks, simple
checklists, deadlines, recurrence, and task review. Execute only the Spec Kit specify stage.
Do not implement code, generate plan/tasks, commit, publish, or advance automatically.

## Current state and prerequisites

Inspect the current repository at C:/Users/gabri/Projetos/ethos first. Read AGENTS.md,
constitution v1.0.0, the foundation and app-shell specifications/plans/verification, and relevant
current Kotlin source. Both foundation and app shell were implemented in another chat.

The existing app uses Kotlin with AGP 9 built-in Kotlin, Compose Material 3, Hilt/KSP, a single
:app module, base identity com.ethos.app, minimum SDK 24, and Navigation Compose. Its shell
includes Hoje, Tarefas, Hábitos, Calendário, Revisar pendências, Recompensas, Histórico de moedas,
and Configurações. Light/dark/system appearance is persisted with Preferences DataStore.
Inspect actual files rather than assuming these descriptions supersede subsequent changes.

Current shell models such as ShellItem/ShellPresentation are presentation/sample structures,
not an existing task domain. At handoff there is no implemented Room task database, recurrence
engine, habit engine, wallet, XP system, or reward engine. Extend the approved shell rather
than rebuilding it, importing prototype demo data, or creating a parallel organizer application.

The product initially serves its owner through APK distribution. All task operations in this
feature must work offline, without accounts, network services, or new runtime permissions.

## Objective and architectural constraints

Deliver the first genuinely usable local lists/tasks workflow, using Room as the source of
truth and real data in the existing shell. Persist lists, tasks, checklists, occurrences,
recurrence definitions, review outcomes, and relevant history. Preserve appearance preferences.

Use feature-oriented MVVM, Hilt/KSP, Coroutines/Flow, immutable StateFlow, and lifecycle-aware
collection consistent with the constitution. Keep domain logic independent of Android/Room.
Use cases must encapsulate actual rules rather than simply add pass-through layers.
Export/version Room schemas and define/test migrations when existing schemas evolve. Do not
use destructive fallback or JSON snapshots as the task database.

Exact library versions, database design, scheduling strategy, transaction boundaries, source
layout, and recurrence algorithms belong to the later technical plan. Do not preempt them
with an unverified implementation recipe or add backend/WorkManager requirements by reflex.

## Lists and Inbox

- Users can create personalized lists with a name, color, and icon, rename them, and reorder them.
- Each list has Tarefas and Hábitos tabs. Only task behavior becomes functional here; the habits
  tab must remain an honest empty/future state until its own feature exists.
- Provide a Caixa de entrada for tasks not assigned to a personalized list. A user must not have
  to create or choose a list to capture a task. Inbox is a distinct system destination, not a
  fabricated user list that must be deleted or renamed like other lists.
- Allow moving tasks between lists and Inbox without changing their history or rewarding them.
- A project can be represented by a named list; do not add a separate Project domain.
- Deleting a user list deletes its contained tasks and, once habits exist, its habits too.
  Do not silently move its contents to Inbox. Ask explicit confirmation that all those items
  will be deleted. Deleting a list does not itself incur a failure penalty.
- Preserve already-recorded coin/XP transactions and balances when deleting a list; deletion
  is not a financial reversal. This is a future integration requirement, not authorization to
  implement an accounting engine or habit CRUD in this delivery.

## Task fields and organization

Tasks support a required title, optional description, optional personalized-list membership
(otherwise Inbox), deadline, optional time, optional recurrence, difficulty, importance, and
an optional simple checklist. Do not make nonessential fields mandatory just to create a task.

Difficulty options are Fácil / Média / Difícil. Importance options are Baixa / Média / Alta.
Both start at Média. Selecting Difícil suggests importance Alta only if the user has not already
explicitly edited importance. Preserve a manual importance choice across later difficulty changes.
Difficulty represents effort; importance represents consequence. Future numeric gains/losses
are not defined by this specification.

Lists show pending tasks first and completed tasks in a collapsible section. Users can manually
reorder pending tasks. Keep completion/history separate from the current visual ordering.
Show tasks using the approved compact model B, respecting the existing Tudo/Tarefas/Hábitos
filters rather than replacing its navigation with a new visual concept.

## Deadlines and Hoje

Support these deadline forms:
- No date: keep the task in Inbox or its list, without automatically showing it in Hoje.
  It does not become overdue or incur a deadline penalty. Assigning a deadline moves it into
  the appropriate date/period view. Do not confuse an undated task with a weekly deadline.
- A specific day, with optional time: show in the relevant day. Time helps organize the agenda,
  but passing the time does not mark failure or immediately require review. The task becomes
  overdue for review after the day ends.
- Nesta semana: no fixed day, with deadline through Sunday; weeks run Monday through Sunday.
- Nesta semana — dias úteis: no fixed day, with deadline through Friday. Weekdays mean Monday
  through Friday without holiday calendars.

Weekly tasks appear in Hoje below day-specific tasks in a section labeled
"Nesta semana · sem dia definido", under Tudo/Tarefas filters. They may be completed during
their period without first assigning a specific day. At period end unresolved work enters
review; the full week enters review on the following Monday, the weekdays deadline after Friday.
Do not keep expired weekly work appearing as if it belongs to the new week.

The later plan must explicitly define local dates, timezone/clock handling, and date boundaries
so day/period behavior is deterministic and testable. Those details have not been individually
approved as product behavior; do not claim a timezone policy was already chosen by the user.

## Simple checklists

- A task may contain independently checkable checklist items.
- These are not full subtasks: no separate dates, difficulty, importance, coins, or XP.
- Rewards belong to the main task only, not to each checked item.
- Checking every item does not automatically complete the task. Completion remains an explicit
  user action.
- If completion is requested with unchecked items, show the remaining count and ask confirmation,
  e.g. "Restam 2 itens. Concluir mesmo assim?". Allow confirming or returning to the checklist.
- Each new recurring occurrence starts with all checklist items unchecked, retaining their labels.
  Checking an item in one occurrence must not affect another occurrence's checked state.

## Completion, review, and correction

Missing a deadline is not proof that a task was not done. Do not automatically apply penalties
or silently mark tasks failed based only on time passing. Integrate real task review entries,
counts, and Hoje notices into the existing Revisar pendências shell with Tudo/Tarefas/Hábitos
filters. The habits filter may remain empty until the habit domain is implemented.

The user can:
- Complete the task, including recording forgotten work after the original day.
- Choose Não será feita to explicitly confirm that the occurrence will not be done.
- Reschedule overdue work, but only after reviewing the missed original deadline. Changing the
  date must not silently erase the original review requirement or assume automatic failure.
- Delete a task that no longer makes sense, without marking Não será feita or incurring a
  deletion penalty. Do not add a separate Cancelar sem penalidade outcome.

Keep actual completion time/date distinct from when the record was entered. A retroactive
completion cannot predate creation of the task. For work that already existed yesterday,
the user may record today that it was completed yesterday. Future-dated completion and unusual
recurrence/date correction edge cases require an explicit policy during clarification/planning;
do not silently invent them as settled decisions.

Support undo of completion: reopen the task/occurrence. The future currency integration must
reverse exactly the coins and XP awarded by that completion, and re-completion must not create
a duplicate reward. Reversal is a correction, not a new failure penalty.

Support undo of Não será feita: reopen the occurrence. The future currency integration returns
exactly the amount actually deducted by that record, not a nominal penalty that was clipped by
the balance floor. Do not issue the same reversal twice.

Historical task states and corrections must remain consistent. For completion-relative series,
the later plan must address undo when a following occurrence already exists or has been acted
on, without duplicating occurrences or deleting legitimate subsequent records silently.

## Recurrence patterns

Include these options in the first task version, rather than leaving them as nonfunctional UI:
- Every day.
- Weekdays (Monday through Friday, no holiday logic).
- A chosen weekday every week, such as every Monday.
- Custom selected weekdays, allowing multiple days of the week.
- Monthly on a chosen day number.
- Every X days with either fixed-calendar dates or completion-relative scheduling.

Monthly rules preserve the original selected day. If the month lacks that date, use its last
day, then return to the selected day when possible: 31 January → 28/29 February → 31 March.
Do not permanently shift the series to the 28th after February or skip the shorter month.

Every-X-days modes:
- Fixed calendar: cadence remains anchored to planned dates, regardless of actual completion.
- After completion: calculate the next due date from the actual completion date entered, not
  the later date on which a forgotten completion was recorded.
- In after-completion mode, keep a single unresolved occurrence; do not generate the next until
  the current one is completed or marked Não será feita.
- When Não será feita resolves an after-completion occurrence, keep the series active and set
  the next due date to X days after that occurrence's planned due date, not its review date.
  That calculated date may already be overdue when reviewed late; explicitly handle this
  consistently rather than quietly changing the approved anchor.

For fixed-calendar recurrence, each missed scheduled occurrence remains a separate review item,
even after days away from the app. Example: daily Monday through Wednesday yields three items
to review. Do not merge, discard, automatically fail, or automatically penalize them.
The plan can choose an efficient materialization/paging strategy, but it must preserve the
individual occurrences and avoid duplicating them after relaunch/recalculation.

Recurring tasks have a start date and these three ending options:
- Nunca, until explicitly stopped/deleted going forward.
- Em uma data, inclusive: an occurrence scheduled on that final day still happens.
- Após X ocorrências, counting scheduled occurrences, including those completed or marked
  Não será feita. Failures do not extend the series to compensate for missed work.

No new occurrences are scheduled beyond the ending rule, but unresolved occurrences already
due remain available for review. The interaction of counting with deleted exceptions or split
series needs an explicit consistency policy in clarification/planning, without resetting limits
silently or representing an unapproved rule as a recorded user choice.

## Series versus occurrence changes

For editing or deleting recurring work, offer these scopes:
- Só esta ocorrência.
- Esta e as próximas.

Preserve earlier history when changing future routine. Changes to checklist content, deadlines,
and other task attributes must obey the selected scope. Não será feita applies only to the
selected occurrence; future scheduled occurrences remain active normally. Editing/moving work
must not accidentally conclude, duplicate, or financially reward it.

Do not invent a bulk deletion of all past financial transactions as a side effect of deleting
a task, occurrence, or list. If conflicts arise between deletion and preserving domain history,
design deletion/history semantics explicitly rather than physically cascading through records
that future accounting must retain.

## Currency/XP boundary: record accepted semantics, defer the engine

The user's accepted future system earns coins and XP, has levels but no energy, uses difficulty
and importance, and supports personal rewards. Failure penalties affect coins only, with a
−300 floor. Failures never remove XP/levels. Reward redemptions require sufficient funds.

For this feature implement real task states, difficulty/importance, review, and correction
history needed to support later integration. Do not calculate live wallet values, seed a fake
wallet, invent a final reward/penalty matrix, mutate demo balances, or implement the accounting
engine just to make task completion appear rewarded now. Keep current balance/level displays
honest until that feature is implemented.

The future engine must honor exact reversal, effective floor-limited penalties, stable identity
of occurrences/outcomes, and duplicate prevention. The technical plan should preserve enough
provenance for those rules without designing a large speculative plugin/event framework.

Numeric gain/loss previews and their explanatory table will become functional when real balancing
rules exist. Do not silently use prototype +10/+15/−5 amounts as defaults or call unavailable
numeric calculations saved. Clearly state the deferred dependency in generated artifacts.

## Out of scope

- Habit CRUD, habit targets, maximum/minimum frequency evaluation, and negative-event logging.
- Currency/XP formulas, levels progression, ledger, reward CRUD/redemption, or monetary values.
- Reminders, notifications, alarm permissions, notification channels, background delivery.
- Activity timers, countdowns, timed tasks, weekly time totals, or timed reward consumption.
  These are explicitly wanted future features; see TODO/planejamento_lembretes_tempo.md.
- Backend, authentication, synchronization, Firebase, analytics, cloud backup, publishing.
- A separate Project domain, advanced full subtasks, attachments, collaboration, or imported data.
- A complete new calendar experience beyond task/date integration justified by the current shell.
  Do not treat this task feature as authorization to redesign every existing destination.

## Acceptance and verification expectations

Write independently testable user scenarios and criteria covering:
1. Create/edit/reorder/rename lists, Inbox capture, task moves, and confirmed deletion with content.
2. Create/edit/persist tasks with required/optional fields, defaults, manual-importance precedence,
   pending ordering, manual reorder, and collapsible completed content.
3. Complete/reopen tasks, confirm incomplete checklists, and isolate checklist state by occurrence.
4. Undated/day/weekly/weekday-week deadlines, Hoje filters/weekly section, period rollover, and
   no premature failure at an optional time-of-day.
5. Forgotten completion dates versus record dates and rejection of pre-creation completion.
6. Each recurrence mode, multiple selected weekdays, short months/leap years, fixed missed
   occurrences, completion-relative anchors, and no duplicate generation across app launches.
7. Inclusive ending dates, finite occurrence counts, preservation of earlier unresolved work,
   and edits/deletions scoped to an exception or this-and-future occurrences.
8. Review entries and counters, explicit Não será feita, undo, and deadline review before
   rescheduling overdue work without automatic financial penalties.
9. Real persistence across process death/relaunch, recoverable write failures, Room schema
   export, meaningful database integration tests and migration tests when migrations apply.
10. No fake currency mutations or habit/time/notification feature creep; preserve future-history
    semantics while keeping current screens honest about unavailable integrations.
11. Relevant domain tests for date/recurrence/correction edge cases with controlled time,
    appropriate UI tests, build and lint checks, and honest runtime/device coverage reporting
    when implementation is eventually requested. Do not add tests that just mirror code.

Keep Portuguese visible strings in resources, accessibility, large-font behavior, existing themes,
offline operation, release/build behavior, and repository privacy requirements intact.

## Decisions still requiring technical clarification, not already approved product facts

Do not ask the user to repeat the settled rules above. Reinspect current code and documents
first. Where an interaction genuinely remains unspecified, record it explicitly and use the
normal clarification/planning workflow rather than silently implementing a guess. Examples:
timezone changes, a shifted cadence after changing one occurrence, undo with later resolved
occurrences, deleted exceptions in a finite series, and the exact representation of a reviewed
missed deadline when rescheduling. These details are not reasons to expand the feature or invent
notification/gamification behavior. The saved prompt authorizes specifying this feature only.
