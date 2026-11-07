# Specify — Ponto 7: lembretes e notificações

**Estado:** pronto para executar specify em outro chat.
**Data:** 2026-11-07.

## Como usar

Na raiz `C:/Users/gabri/Projetos/ethos`, execute `$speckit-specify` com o corpo em inglês
após `---`. Execute somente specify. Plan, tasks, analyze, implement, commits e publicação
dependem de solicitação separada. Salvar este arquivo não executa Spec Kit.

## Referências e origem

- `AGENTS.md` e `.specify/memory/constitution.md` v1.0.0.
- `specs/001-android-foundation/` até `specs/005-coins-xp-levels/`: comportamento existente.
- `specs/006-personal-rewards/`: conferir o estado atual; estava em planejamento no último contexto.
- `TODO/planejamento_ponto_7.md`: seis decisões iniciais e três detalhes finais aprovados.
- `TODO/planejamento_lembretes_tempo.md`: intenções futuras de cronômetros, fora deste ponto.
- Origem: conversa de planejamento do Ethos em 2026-11-07. ID, link e caminho de transcript
  indisponíveis neste contexto; decisões necessárias registradas integralmente abaixo.
- Sem contraparte de backend.

---

Create the feature specification for Ethos point 7: optional task/habit reminders and local
notifications, snoozing, and a configurable daily summary. Execute only the Spec Kit specify
stage. Do not generate plan/tasks, implement, commit, publish, or advance automatically.

## Inspect the current repository

Work in C:/Users/gabri/Projetos/ethos. Read AGENTS.md, constitution v1.0.0, existing feature
specifications/plans/verification, and relevant task/habit/accounting/navigation code first.
The user discusses design in one chat and passes saved prompts to another. Do not ask again
about the nine approved choices recorded here.

At handoff, foundation, shell, tasks/lists, habits/goals, and coins/XP are implemented. Rewards
have a specification/plan under specs/006-personal-rewards; another chat may advance that work,
so inspect its actual state and preserve it. Do not claim the reward feature is complete merely
because a plan exists, or modify its files without a requirement from this feature.

The app is native Kotlin, minimum SDK 24, AGP 9 built-in Kotlin, Compose Material 3, Hilt/KSP,
Navigation Compose, Room for structured data, and DataStore for preferences. The current app has
one launcher MainActivity and EthosApplication. At inspection the main manifest declares no
notification permission or reminder/alarm receivers/services; no reminder implementation was found
in main source. Reinspect rather than assuming that remains unchanged.

Task occurrences, civil dates and deadlines, recurring series, review, habit rules/periods and
accounting already have real behavior. Reuse those identities and rules rather than creating
an unrelated scheduling/task system. Wallet outcome mutations are already consistent with source
changes; notification actions must preserve that integration.

The owner's initial distribution is by personal APK. All reminders are LOCAL and offline, without
accounts, backend, Firebase, network push, analytics, or new service credentials.

## Approved scope

1. Optional reminders for tasks and habits.
2. Multiple reminder definitions per item.
3. Snooze for 10 minutes, 30 minutes, 1 hour, or a chosen future time, without changing deadlines.
4. Task notification actions Concluir, Adiar, Abrir; habit actions Abrir, Adiar only.
5. An optional configurable daily summary of Hoje items and review count.
6. Recovery without a flood of stale notifications after downtime or disabled permissions.
7. Cancellation of obsolete reminders, including fulfilled practice periods.
8. Explicit reminder schedules for weekly tasks with no fixed due day.
9. Respect for existing incomplete-checklist confirmation and all task/accounting rules.

Timer/session notifications belong to point 8, not this feature. No deadline or financial rule
changes are authorized merely because a reminder fired, was dismissed, or was missed.

## Reminder configuration

Reminders are optional; do not create unsolicited default alerts for every task/habit. Let the
user configure multiple reminders for an item and edit/remove them. Examples such as one day
before and 30 minutes before must be possible where the item has an appropriate reference date/time.

For tasks, support a chosen notification date/time and an advance offset from the task's applicable
date/time. Keep the deadline distinct from notification timing. Do not silently assign a task
deadline time just to support a relative reminder; request the necessary reminder reference in
the reminder form or guide the user to an explicit chosen reminder time.

For habits, let the user choose reminder times and weekdays. Reminder weekdays are cues, not
a change to the actual goal's specified days or flexible distinct-day count. Avoid and limit
habits must open their real details rather than automatically logging an unwanted occurrence.

Weekly tasks without a fixed due day use user-chosen reminder weekdays/times within their
applicable period, without changing the Sunday/Friday deadline or forcing a specific completion
day. Flexible habit schedules similarly must not create unintended daily obligations.

Recurring task reminders must stay linked to the right occurrence/scope. Use existing occurrence
versus future-series behavior when editing related configuration; the later plan should document
how reminder rules project onto occurrences without duplicating schedules or reinterpreting
recurrence anchors. Do not change monthly fallback, interval anchors, series end/count or review.

Persist reminder definitions and relevant delivery/snooze state locally. Exact schema and
placement between Room/DataStore belong to planning. Preserve existing schema/history with
non-destructive migrations whenever structural storage evolves.

## Delivery and Android platform constraints

The desired experience is a reminder at its chosen time, but delivery must be honest about the
actual Android APIs, device settings and permission state. During the later technical plan,
consult CURRENT official Android documentation for minimum/target SDK behavior, notification
permission/channel rules, exact/inexact alarm eligibility, idle/background restrictions,
receivers/PendingIntent security and recovery requirements. Do not copy stale rules from memory.

Choose the smallest supported local scheduling approach. WorkManager is not a blanket substitute
for precise user-selected reminder times. Conversely, do not promise exact delivery if the chosen
API/permission/device state cannot provide it, or silently request broad special access without
explaining its connection to a user-selected reminder. Avoid unnecessary foreground services,
new dependencies, battery exemption requests, or cloud infrastructure.

Request required permissions contextually when the user enables reminder functionality, not as
a prerequisite to basic app use. Handle denied/revoked permission or disabled app/channel alerts
with an understandable status and a path to relevant device settings. Core tasks/habits remain
usable. Never claim a blocked notification was delivered or make permission denial a failure.

Respect system notification channels and user sound/vibration/settings choices. App-specific
quiet-hours, custom sounds, persistent nags or do-not-disturb bypass were not requested.
Only add permissions/components actually justified by the documented plan.

## Snooze

Expose 10 minutes, 30 minutes, 1 hour, or a chosen time through the notification/app flow supported
by the platform. A limited number of notification action buttons does not require inventing four
separate system buttons; Adiar may open a compact appropriate chooser.

Snooze reschedules that reminder/occurrence, not the underlying deadline or whole recurrence.
It must not extend a weekly goal, create an occurrence, confirm failure, or charge coins.
Choosing a time must not silently accept a past instant as a new alert. Repeated snooze actions
should not stack duplicate alerts for the same reminder. The later plan defines a stable identity
and a simple deterministic replacement policy consistent with this behavior.

Before any snoozed reminder is shown or acted upon, revalidate whether its source remains active
and relevant. Completed/deleted/resolved work must not reappear just because a snooze was stored.

## Notification actions and safe navigation

Task notifications expose Concluir, Adiar and Abrir. Habit notifications expose only Abrir and
Adiar: never record practice, a relapse, or an excess event from a single habit notification tap.

Abrir deep-links to the actual item/occurrence or an honest fallback if it was removed/resolved.
Opening an app screen is not permission to mark it done. Use predictable Back behavior and
existing navigation, not duplicate Activities or unrelated task stacks.

Concluir must call the existing task completion flow rather than directly updating a row or
granting coins. If checklist items remain unchecked, open the app for the existing confirmation
with remaining count. If the task/accounting flow requires further confirmation, preserve it.
The action can enter the app flow when needed; it must not bypass a required choice solely to
operate silently in the background.

Revalidate task state when processing actions. Duplicate taps, old notifications, process restart,
or source changes must not conclude twice, award twice, overwrite a corrected history, or throw
an uncaught error. Success should dismiss/update obsolete notices as appropriate. A failed action
must not show completion as committed. Do not penalize dismiss/swipe-away or unanswered reminders.

## Cancellation and source changes

- Cancel relevant scheduled/snoozed alerts for a completed, excluded/deleted, or Não será feita
  task occurrence. Resolving one occurrence must not stop the still-active recurring series.
- Paused/excluded habits stop generating reminders during inactive intervals.
- A practice goal already reached stops reminding for that period; active future periods resume
  according to their schedule. Corrections that reopen the goal must reconcile future relevant
  reminders without replaying an old backlog.
- Avoid/limit habits continue reminders under their chosen schedule even after exceeding a limit,
  so the user can keep recording through the app.
- Task/habit edits, new dates, recurrence edits, series end, list deletion, restoration/resume,
  reminder removal, and disabling the feature must reconcile pending schedules with source state.
- Do not cancel unrelated reminders from other items or treat dismissal as deleting a definition.

## Daily summary

The daily summary starts DISABLED. When enabling it, the user chooses its time; persist that
preference and allow changing/disabling it. Do not pick an unapproved default delivery time and
start sending summaries automatically.

Summarize existing Hoje tasks/habits and the number of pending reviews, respecting actual
domain classification, active states and filters' underlying data. Use a compact readable
notification rather than trying to put a full task manager into system notifications.
Tapping it opens the app. Do not seed example counts or mark missing items failed merely to
populate the summary. Document a simple non-spam presentation for an empty day in planning.

The normal daily summary and the missed-reminder recovery summary are distinct triggers; neither
should duplicate every missed individual alert. No financial or habit record is changed by a
summary being shown, read, dismissed, or skipped.

## Downtime, recovery and time changes

After downtime such as the phone being off or notifications being disabled, do not emit all
stale scheduled notifications at once. Present a consolidated summary of reminders that are
STILL RELEVANT and schedule future ones. Resolved/deleted/inactive sources are not relevant.
Do not claim delivery while permission remains disabled or retroactively assign a missed action.

Delivery/recovery reconciliation must be idempotent. Reopening the app or repeated recovery
signals must not produce repeated copies of the same backlog summary or reschedule duplicate
alerts. Store enough state for safe recovery without making background delivery guarantees the
OS cannot honor.

The technical plan must explicitly cover process death, reboot, applicable app updates, clock/
timezone/date changes, day/week/month rollover and daylight-saving transitions as relevant to
the chosen APIs. Keep the existing civil deadline policy; a notification schedule adjustment
must not rewrite source deadlines, past periods or accounting. Define and document safe technical
policies for ambiguous/missing local times rather than asking the owner to design alarm APIs.

Explain actual limitations, including user/device restrictions that cannot be bypassed safely,
and exercise the supported recovery paths in verification. Do not request permission to unrelated
device data or send cloud messages to compensate for system restrictions.

## UI, privacy and accessibility

Extend existing task/habit editing/detail flows with reminder controls and settings with summary/
notification status. Follow the approved model B and existing forms, Portuguese strings in
resources, English identifiers, light/dark/system appearance, large fonts and accessible controls.
No global UI redesign is authorized.

Notifications should identify the relevant item, time/context and available action without
exposing internal IDs or financial implementation details. Respect system lock-screen privacy
settings and use a conservative presentation for personal task/habit content. Do not log personal
content or add telemetry. Use secure, scoped component/action identities rather than permitting
external arbitrary task mutations through an exposed receiver.

## Explicit exclusions

- Stopwatch/countdown, task-duration sessions, weekly time totals or timed reward alerts: point 8.
- Cloud push, backend, login, Firebase, analytics, synchronization or publication.
- Automatic negative-habit recording, direct habit completion actions from a notification,
  automatic task failures, penalties for ignoring reminders, or a new XP/reward policy.
- Automatic deadline changes on snooze, unsolicited default reminders, full-screen alarm UI,
  app blocking, persistent harassment, or system restriction bypass.
- A new global quiet-hours/custom-sound/campaign system not discussed with the user.

## Acceptance and verification expectations

Write independently testable scenarios covering:
1. Optional creation/edit/removal of multiple task reminders and habit weekdays/times, persisted
   configuration, unambiguous chosen-time/relative reference and no silent source deadline edits.
2. Week-without-fixed-day reminder schedules, recurring occurrence identity, future-series rules,
   and retained monthly/interval/end-of-series behavior.
3. Task Concluir/Adiar/Abrir and habit Abrir/Adiar; target navigation, incomplete-checklist
   confirmation, stale-state revalidation and no bypass of existing accounting flow.
4. Snooze presets/custom time, replacement rather than duplicates, unchanged deadline/no penalty,
   and cancellation if the source becomes resolved/inactive before firing.
5. Cancellation/reconciliation on task completion/deletion/Não será feita, habit pause/exclusion,
   practice-cap attainment, edits and future active periods; avoid/limit reminders remain eligible.
6. Daily summary initially off, chosen time, correct Hoje/review data, click navigation, persistence,
   update/disable and no fabricated counts or automatic domain changes.
7. Blocked/denied/revoked notification capability and relevant device settings; app still works,
   blocked delivery is reported honestly, contextual permission requests and supported scheduling.
8. Downtime recovery as one relevant summary instead of stale-alert bursts; dismissed/resolved
   items absent, repeat reconciliation idempotent, future schedules valid after reboot/time changes.
9. Safe PendingIntent/receiver/navigation behavior as required by target/minimum SDK, no unintended
   externally triggered mutations, no sensitive content logs or privacy regressions.
10. Pure scheduling/relevance tests with controlled time, meaningful platform/Room/action/UI
    integration tests, preserved schemas/history, and regression tests for task/habit/accounting.
11. Build, unit/instrumentation, lint/release and available runtime notification checks when
    implementation is authorized. Distinguish verified SDK/device scenarios from limitations;
    do not use compilation alone as proof that a timed notification was actually delivered.

## Closed product choices and technical planning

All six initial proposals and three final details were explicitly approved. Do not reopen them
as product questions. Use the existing civil date/navigation/accounting conventions and current
official Android docs to resolve routine scheduling/permission/platform details in the plan.
State the chosen technical policies and limitations clearly; do not substitute a demand for the
user to choose Android API mechanics. Escalate only a genuine new contradiction/blocker beyond
the recorded scope. This prompt authorizes specification only, not any subsequent phase.
