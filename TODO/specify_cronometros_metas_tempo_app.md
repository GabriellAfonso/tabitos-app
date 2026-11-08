# Specify — Ponto 8: cronômetros e metas de tempo

**Estado:** pronto para executar specify em outro chat.
**Data:** 2026-11-07.

## Como usar

Na raiz `C:/Users/gabri/Projetos/ethos`, execute `$speckit-specify` com o corpo em inglês
após `---`. Execute somente specify. Plan, tasks, analyze, implement, commits e publicação
dependem de solicitação separada. Salvar este prompt não executa Spec Kit.

## Referências e origem

- `AGENTS.md` e `.specify/memory/constitution.md` v1.0.0.
- `specs/001-android-foundation/` até `specs/006-personal-rewards/`: specs e contratos existentes.
- `specs/007-reminders-notifications/`: conferir progresso e APIs reais antes de integrar.
- `TODO/planejamento_ponto_8.md`: seis decisões iniciais e quatro detalhes finais aprovados.
- `TODO/planejamento_lembretes_tempo.md`: contexto inicial, refinado pelas decisões do ponto 8.
- `prototypes/hoje/index.html`: direção visual B; não contém regras completas de tempo.
- Origem: conversa de planejamento do Ethos em 2026-11-07. ID, link e caminho de transcript
  indisponíveis neste contexto; o corpo registra as decisões necessárias.
- Sem contraparte de backend.

---

Create the feature specification for Ethos point 8: user-started timers, accumulated duration
goals, and timed rewards integrated with existing tasks, habits, accounting and notifications.
Execute only the Spec Kit specify stage. Do not generate plan/tasks, implement, commit, publish,
or advance automatically. Saved design prompts do not authorize implementation.

## Inspect current state

Work in C:/Users/gabri/Projetos/ethos. Read AGENTS.md, constitution v1.0.0, existing feature specs,
plans, contracts and verification, then inspect relevant Kotlin source before writing the spec.
The owner designs in this conversation and hands a self-contained prompt to another chat.
Do not ask again about the ten approved choices recorded below.

At handoff, foundation, shell, tasks/lists, habits/goals, accounting and personal rewards are
implemented. The app uses Kotlin, AGP 9 built-in Kotlin, Compose Material 3, Hilt/KSP, Navigation
Compose, minimum SDK 24, Room structured storage, and DataStore appearance settings.

Room currently has exported schemas 1/2/3/4 and preserving migrations, with task/habit/history,
financial ledger and reward purchase data. Rewards have positive costs, optional per-item civil
limits, confirmed immediate-use purchases, persistent purchase intent/receipt identity, and
atomic debit/history. Purchases do not change XP and are not refundable after commitment.

A reminders/notifications feature exists under specs/007-reminders-notifications but its actual
implementation status may advance in another chat. At this inspection the main manifest has
no notification/service components and no timer/session main-source files were found. Reinspect
the current source rather than assuming these observations remain true. Reuse the reminder
feature's channels/permission/navigation infrastructure where present; do not replace it with
a competing notification system or claim a planned capability is already implemented.

Previous verification reports API 36 evidence, not validated runtime on API 24, physical hardware
or TalkBack audio. Inspect actual evidence and report this feature's own coverage separately.
Initial distribution remains personal APK. Work offline; no login/backend/cloud dependency.

## Purpose and approved scope

Support examples such as a 30-minute reading task, three hours of an activity per week,
and a purchased two-hour gameplay reward. The user starts/stops tracking; the app does not
detect actual reading/gameplay or enforce device restrictions.

Approved capabilities:
1. Count-up stopwatch and countdown, with start, pause and resume.
2. At most ONE timer actively counting at a time; starting another requires pausing the current.
3. Continue with screen locked/app in background; show a session notification with Pause and
   navigation into the app.
4. Reaching an objective pauses counting and requests confirmation before completing task/goal;
   it never automatically grants coins/XP.
5. Add forgotten time and correct sessions manually, with an audit history.
6. A timed reward purchase debits coins and starts counting immediately; exhaustion only alerts.
7. Multiple sessions accumulate toward a goal; paused time contributes nothing.
8. Allocate elapsed time to its actual date/period, splitting a session across boundaries.
9. After device reboot, recover saved time and leave paused, without counting powered-off time.
10. Purchased reward time can pause/resume without repurchase/refund. If another timer is running,
    confirm pausing it BEFORE committing the timed reward purchase.

## Timer and session model

Expose count-up and countdown modes clearly. Let a user configure a duration objective where
needed; a stopwatch without a chosen endpoint must not invent one. Starting/pausing/resuming
must have clear state and an accessible control in the app.

Maintain one global running session regardless of its source (task, practice activity or reward).
Starting another source requires pausing the current session, not automatically discarding its
elapsed time. Repeated taps/recreation must not create two runners. Paused sessions keep their
own accumulated time and source identity; do not silently merge unrelated activities.

Do not count pauses, time before start, or a stopped interval as activity. Derive elapsed time
from a robust time source and persisted segments, not a UI loop that advances once per redraw.
The later plan must specify exact clock/checkpoint/numeric precision and recovery mechanics.

When a configured time objective is reached, stop accumulating extra time for that objective
and show an alert plus an appropriate completion confirmation. Count-up against a fixed target
and countdown must agree on elapsed work; switching display or reopening cannot add time.
Do not silently count beyond the target while waiting for the owner to confirm.

Timer state and domain completion are distinct. An elapsed counter is evidence the user may
confirm, not permission to complete a task/habit, claim a reward, award XP or levy a penalty.
Preserve existing checklist confirmation, occurrence identity, manual review and correction rules.

## Duration goals and accumulation

Support task/activity duration targets and weekly accumulated duration, including:
- Read for 30 minutes, potentially accumulated through multiple sessions.
- Perform an activity for three hours during a calendar week.

Example: 40 + 50 + 90 minutes of actual unpaused sessions equals three hours for the same goal.
Weekly periods use the app's approved Monday–Sunday civil week. Store enough information to
reconstruct credited durations and avoid duplicating totals on relaunch, pause/resume or edits.

Allocate activity to when it was actually performed, not just to the start date or time saved.
Approved boundary example: Sunday 23:50 → Monday 00:20 contributes ten minutes to the old week
and twenty to the new one. Similarly split across midnight; exclude paused segments before
allocating. Display period progress and source session history with understandable units.

Existing habit practice goals count distinct days; this feature must not reinterpret all of
those goals as duration or give per-minute currency. Integrate duration configuration as a clear
criterion where needed, without changing recorded goals/historical outcomes automatically.
The technical/design plan must document the minimal mapping between a confirmed timed task/
activity goal and existing completion/period accounting. Do not invent new reward rates or
require creating duplicate tasks to claim the same work twice.

Future monthly duration targets, Pomodoro cycles, breaks, coaching, focus scoring, automatic
activity detection and app-use surveillance were not requested for this delivery.

## Completion and financial effects

- Hitting a duration target pauses and asks for confirmation to complete the relevant task/goal.
- Confirmation calls existing domain completion flows, including any checklist or review checks;
  never write a ledger award directly from a background tick.
- Completing one occurrence/goal only awards its existing entitlement once. Repeated timer
  alarms, confirming again, switching modes, or resuming UI must not multiply coins or XP.
- Existing reward amounts, difficulty/importance separation, linear XP curve and −300 floor
  remain unchanged. Time elapsed itself has no invented price per minute or failure penalty.
- Manual corrections update goal progress and affected domain outcomes through existing correction
  semantics when applicable, with clear confirmation for financial changes. Do not erase old
  financial history or grant a reward merely because someone edited a duration field.
- If a correction reduces credited duration below a target, maintain coherent pending/completed
  state and stable audit/accounting linkage. The plan must handle correction chains explicitly
  rather than freezing stale success or issuing duplicate reversals.

## Manual time and audit

Allow adding forgotten time and editing erroneous sessions. Record what was changed, when,
and the actual activity interval/date separately from the edit timestamp. Manual input belongs
to the goal/period when the activity happened, consistent with the approved boundary splitting.

Validate impossible/negative intervals and accidental duplicate or overlapping credited time.
Use existing source creation/active-date restrictions where applicable rather than silently
allowing time before the task/habit existed or charging activity in a paused/excluded interval.
Precision, sensible bounds, correction UI and overlap-resolution mechanics are technical/design
details to document, not an excuse to invent biometric verification or extra currencies.

## Background, process loss, reboot and notification behavior

With the app in background or the screen locked, active tracking must continue according to
the approved user action. Provide a notification identifying the session, its time/progress,
Pause and an action to open the relevant app screen. No direct accidental negative-habit logging.
Pausing from the notification must update the same persisted session as pausing in the app.

App UI destruction and DEVICE REBOOT are different:
- Do not reset elapsed time merely because the Activity is recreated or the app is backgrounded.
- After a reboot, restore saved elapsed time and paused state; never count the powered-off gap.
- Do not guess that the user performed activity during an interval for which tracking state
  cannot be recovered. Preserve durable known progress and explain recovery as needed.

Consult current official Android documentation during planning for foreground service types,
permission/notification rules, background launch restrictions, timers/alarms and reboot handling
on minimum/target SDKs. Choose the smallest supported mechanism that meets this actual feature.
Do not promise uninterruptible service under force-stop or device restrictions the OS cannot
honor, use WorkManager as a one-second ticker, or request unrelated access/exemptions.

If permission/device restrictions prevent a desired notification or background capability,
explain the actual limitation and provide the supported in-app path. Do not silently claim a
session alert was delivered, award completion automatically, or block core task management.

System clock/timezone changes must not create/erase elapsed work. The plan must explicitly
separate elapsed duration from civil-period allocation and define safe checkpoint/recovery
behavior across date/time changes and daylight-saving boundaries, reusing existing date policy.

## Timed rewards: extend immediate use without introducing unused inventory

Allow a reward to have an associated duration, such as two hours of gameplay. Non-timed rewards
retain the point-6 behavior. Existing purchases retain their original snapshots/prices.

Confirming a timed purchase debits its cost, records immediate use and starts the associated
countdown. Do not create a purchased-but-not-started token, unused time wallet or an Activate
later inventory. The user's original immediate-use purchase decision remains in force.

If another timer is running, first ask confirmation to pause it and transition to the purchased
reward. Resolve that BEFORE charging. Cancelling leaves the purchase uncommitted and the wallet
uncharged; do not pause the original session unexpectedly on a cancelled transition.

Persist purchase receipt/session linkage. Debit, purchase record and durable session-start intent
must be consistent and idempotent, so a retry cannot charge twice or create two reward timers.
Actual background execution setup should recover from durable state without fabricating a second
purchase. Planning must address failures between committing the purchase and showing the timer.

A session that STARTED with purchase can be paused and resumed later without new payment and
without a refund. This is continuation of the SAME used reward, not storing a separate unstarted
reward. Resuming does not consume another per-item redemption allowance; a genuinely new purchase
does. The global single-running-timer rule applies during every resume/transition.

When purchased time runs out, stop/pause at the purchased amount and alert. Do not block games,
close other apps, penalize the owner, silently buy more time or refund unused/paused time.
Respect the no-cancellation/no-refund policy for a committed reward purchase. Correcting a timer
record is not permission to reverse its purchase or create extra paid time without another purchase.

## Existing UI, data and history

Extend existing item details, Hoje and rewards with clear duration/progress/start/resume controls
consistent with model B. Do not replace the whole navigation or add a mandatory top-level
productivity dashboard. Show active/paused session access, totals and session/audit history.

Persist durations, segments, configuration and links in local storage using a preserving Room
migration where needed. Preserve tasks, recurring occurrences, habits/rules/records, lists,
financial ledger, reward limits/purchases, reminder configuration and theme preferences.
Do not reset wallets or populate production with demo sessions.

Portuguese visible strings in resources, English code identifiers, lifecycle-aware state,
light/dark/system appearance, accessible controls and large-font support remain required.
No logging private activity names/history or adding analytics to monitor timer usage.

## Acceptance and verification expectations

Write independently testable scenarios covering:
1. Count-up/countdown start/pause/resume, correct elapsed versus remaining time, no paused-time
   credit, durable session identity, and one global running timer.
2. Multiple sessions sum correctly to task/activity targets and three-hour weekly goals;
   split midnight/week-boundary segments exactly, including Sunday 23:50 → Monday 00:20.
3. Target reached pauses and alerts, no overrun/automatic coins/XP, explicit existing completion
   flow and no duplicate awards across confirmations/retries/corrections.
4. Forgotten/manual time and corrections preserve audit, update actual-date progress and respect
   valid intervals/overlap policy without duplicating sessions or financial outcomes.
5. Screen lock/background tracking, notification Pause/Open, Activity recreation/process recovery,
   and reboot restores known saved time paused without crediting powered-off duration.
6. Relevant permission/device restriction states, notification failure/recovery, current Android
   service/alarm requirements, date/time changes, and honest limits rather than unsupported guarantees.
7. Timed-reward purchase snapshots/cost/quota, immediate start, prior-session pause confirmation,
   cancellation before charge, atomic intent/receipt linkage, no duplicate charge on retry.
8. Pausing/resuming purchased time without repurchase/refund, no extra quota consumed on resume,
   exhaustion alert only, no app blocking or automatic purchase of extra time.
9. Non-timed reward behavior and existing task/habit/reminder/accounting flows remain correct;
   no phantom duration records, retroactive rewards or data loss.
10. Exported schema/preserving migrations, pure time arithmetic/state tests with controlled clocks,
    Room consistency tests, UI/service/notification integration checks and recovery tests.
11. Relevant build, JVM/instrumentation, lint/release and actual background/reboot/runtime checks
    when implementation is later authorized, with evidence and missing SDK/device coverage stated.

## Closed choices and planning discipline

All six initial proposals and four final details were explicitly approved. Do not ask again about
count-up versus countdown, one running session, background counting, manual correction, paused
reward resumption, boundary splitting, or explicit completion confirmation.

Use existing domain contracts and current official Android documentation to resolve routine
implementation mechanics in the later plan. State any minimal integration assumptions explicitly
without claiming they were separately approved, and escalate only a genuine blocking contradiction
outside these recorded choices. Do not invent new timer product features to answer technical
questions. This handoff authorizes specification only, not subsequent phases or Git mutations.
