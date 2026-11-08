# Implementation Plan: Lembretes e notificações

**Branch**: current branch retained | **Date**: 2026-11-07 | **Spec**: [spec.md](spec.md)
**Input**: specs/007-reminders-notifications/spec.md

## Summary
Optional local reminders, safe notification actions/snooze, relevance reconciliation and daily summary.
Room schema5 stores definitions/deliveries/settings; one AlarmManager wake handles next event/midnight.
No new dependencies, services, network or automatic domain outcomes.

## Technical Context
**Language/Version**: existing Kotlin/AGP9, JVM17.
**Primary Dependencies**: existing Compose Material3, Hilt/KSP, Room2.8.5, Flow; platform alarms/notifications.
**Storage**: additive Room4→5, original schemas retained; existing DataStore appearance unchanged.
**Testing**: JUnit pure policies, Room migration/reopen, Compose configuration/actions, available API36 runtime.
**Target Platform**: Android24–36, target36, one Activity.
**Project Type**: native offline app, one :app.
**Performance Goals**: one pending OS wake, bounded projection, no stale individual bursts.
**Constraints**: system/device/force-stop restrictions prevent delivery guarantees; no personal logs.
**Scale/Scope**: personal data, multiple reminders/item and one replacement snooze per definition/occurrence.

## Constitution Check
Pre-research and post-design: I PASS spec and explicit implementation authorization; II PASS local Room,
additive preserving migration; III PASS pure domain, core contracts, UI/ViewModel/repository;
IV PASS Portuguese resources, private notification, accessible themes/contextual permission;
V PASS meaningful policy/persistence/action tests and honest runtime evidence. No deviations.
Analyze before implementation. Hooks cannot authorize commits; no commit/push/publication requested.

## Project Structure
Documentation under specs/007-reminders-notifications: spec, plan, research, data-model, contracts/reminders,
quickstart, tasks, verification. Source under app/src/main/java/com/ethos/app/:
- core/reminders/domain/: models, pure time/projection/relevance/summary rules.
- core/reminders/data/: Room entities, DAO, migration and repository.
- core/reminders/platform/: coordinator, single next alarm, gateway, receivers.
- features/reminders/: ViewModel, configuration and snooze UI.
Existing task/habit detail and settings connect via callbacks; MainActivity handles validated requests.
Tests under app/src/test and app/src/androidTest; exported schema5 under app/schemas.

## Scheduling and data design
Definition modes: chosen civil datetime, relative offset from due date with explicit reference minute if
source has no time, weekdays/time for habits and weekly tasks. Never change a source deadline.
Series scope: stable groupId, taskId, fromOrdinal, optional exact occurrence override. Within each group
most specific eligible ordinal/occurrence wins; disabled overrides suppress inherited rules. Future series
edits replace overrides at/after the chosen ordinal, keeping earlier definitions. Future series
keeps task IDs/ordinals in existing TaskRules. Chosen time series uses displacement from selected planned
anchor; weekly dates are clipped to real period. No copied recurrence algorithm.
Project existing TaskRules.Refresh to bounded future horizon including offsets. Projection is pure;
opening future target materializes through its anchor with explicit repository action using same generator.
After-completion still has one unresolved occurrence. Preserve monthly clamp, endings/counts and history.
Delivery key: group+occurrence/period+civil slot. Persist scheduled instant, snooze and terminal state.
Edits never replay a terminal slot; saved edits use a new cutoff so past new slots do not become alerts.
Repeated snooze replaces row; past custom instant rejected. Habit snooze
expires with original period; task stays relevant while OPEN. Practice uses real count/target; avoid/limit
continue. Invalid sources cancel pending and displayed notices, preserving unrelated items.

Coordinator serializes reconciliations, observes committed source/definition changes while process lives,
and reconciles foreground/alarm/boot/package replacement/timezone/clock/exact-access grant. Next civil
midnight wakes for rollover/projection. DST gaps shift forward, overlaps earlier offset once. Civil times
follow device zone; snooze persists instant. Exact idle alarm if available, honest inexact fallback otherwise.
Two minute ALARM grace for individual notices; later or recovery-triggered due rows consolidate into one
stable recovery tag. Blocked rows stay pending. Posting then acknowledging is retry-safe by stable tag,
but no claim of atomic exactly-once physical display across OS and Room. Dismiss never removes definition.

## Platform, UI and privacy
Declare POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED, SCHEDULE_EXACT_ALARM only. No USE_EXACT_ALARM,
foreground service or battery exemption. Contextual runtime request after enabling; optional explained exact
settings access. Separate reminder/summary channels; status checks permission/app/channel. Settings links.
VISIBILITY_PRIVATE and generic public version; respect user channel choices. No content logs.
Alarm receiver nonexported; recovery allowlisted system actions only, no domain mutation from extras.
Immutable explicit activity PendingIntents with scoped data URI. Direct MainActivity actions avoid
notification trampolines. singleTop/onNewIntent supports warm launch; pending action retained until loaded.
Concluir enters existing detail/complete flow with checklist and financial confirmation; habit never completes.
Removed/resolved target has honest fallback, predictable Back. Snooze chooser10/30/60/custom future.
Reminder manager in existing details, same UI style/themes, scrollable Portuguese forms, retained draft/retry.
Daily summary disabled until chosen minute; counts match actual Hoje task.inDay and current visible habit
periods plus review needsReview/PENDING. Empty day shows zero counts once. Daily/recovery tags distinct.
Room settings row is structured scheduling state coupled to deliveries, not appearance preferences.

## Verification
Follow quickstart; debug/JVM/lint/R8/androidTest build and connected tests when available. Test pure rules,
migration chains, actions and real timed delivery, snooze/cancellation, denied permission/channel, recovery,
reboot/time changes, font200%, light/dark/landscape. Record untested API24/physical/TalkBack conditions.
