# Feature Specification: Timed Tasks and Habits

**Created**: 2026-11-11 | **Status**: Implemented

**Origin**: Claude Code conversation on 2026-11-11 (no transcript ID available). All decisions below were
taken there with the user. Supersedes parts of `specs/008-timers-duration-goals/spec.md`.

## Problem

The timer is an option hidden in every task/habit detail ("Cronômetro"), and the duration goal is set
inside the timer sheet. Many editor options do not make sense together (a checklist with a timer). The
user wants the timer to be a **variant chosen when creating**, which enables only compatible options.

## Decisions

1. **Variant, not a new primitive type.** Creating a task offers *Normal* or *Com cronômetro*; creating a
   practice habit offers *Normal* or *Com cronômetro*. "Quebrar hábito" (avoid/limit) has no timer.
2. **Compatibility**:

   | | Task normal | Task timed | Habit normal | Habit timed |
   |---|---|---|---|---|
   | Checklist | yes | **no** | — | — |
   | Duration | — | yes (minutes) | — | yes, per day or per week |
   | Deadline/date/time, repetition, reminders | yes | yes | frequency | per day (chosen weekdays) or per week |

3. **Card control**: a timed item shows a ▶ button with the remaining time on its card in Hoje and lists;
   tapping starts/pauses the countdown there. Elapsed time is kept when paused (resume continues).
4. **Reaching zero completes automatically** with the normal coins/XP, using the date the time was reached
   (also when the app was closed: completion is applied on the next sync). An in-app "Concluída pelo
   cronômetro · Desfazer" message offers undo. The old "Concluir?" prompt is removed.
5. **Alarm**: one global setting "Alarme ao terminar o cronômetro" (on by default). On: alert notification
   on an alarm channel (sound + vibration, high importance). Off: silent notification.
6. **Manual completion stays possible** (real life happens away from the phone), but checking a timed task
   or marking a timed habit asks "Concluir sem completar o tempo?" first.
7. **Free stopwatch removed** from normal tasks/habits: it is a time *log*, not a task/habit. Idea kept in
   `TODO/` for a future "registro de tempo". Reward timed purchases (spec 008 FR-013..015) are unchanged.
8. **Switching variant later is allowed**: to timed removes the checklist (with a warning in the editor); to
   normal removes the duration goal (recorded time is kept).
9. Existing data is not a priority (app in development; phone data was wiped on 2026-11-11). No migration of
   old goal/checklist combinations is needed; Room schema is unchanged (variant = duration goal exists).

## Functional requirements

- **FR-001**: Task editor shows the variant choice; timed hides the checklist and shows "Duração (min)".
- **FR-002**: Practice habit editor shows the variant choice; timed limits frequency to per day (weekdays)
  or per week and shows "Duração (min)" for that period.
- **FR-003**: Saving stores the task/habit and its duration goal; normal variant removes any goal.
- **FR-004**: Timed cards show ▶/❚❚ with remaining time; only one timer counts at a time (spec 008 FR-002).
- **FR-005**: An objective reached is completed automatically once (persisted confirmation audit), with the
  reach date; undo is offered in-app.
- **FR-006**: Alarm setting controls the alert channel; default on.
- **FR-007**: Manual completion of a timed item asks for confirmation.
- **FR-008**: No timer entry point for normal items; the timer sheet no longer edits goals nor toggles
  count-up mode.

## Out of scope

- Task detail redesign (next spec), separate time log feature, reward timers.

## Acceptance

1. A timed task reaching 0 rings (if enabled) and appears completed with coins, and can be undone.
2. A timed habit "20 min por dia" is marked done for the day when 20 min accumulate.
3. A normal task shows no timer anywhere; a timed task shows no checklist.
4. Checking a timed task manually asks for confirmation first.

## Verification (2026-11-11)

- Build, 140 JVM tests and lint pass; instrumented tests compile but were not run (standing user rule).
- New JVM tests: timed task saves goal and drops checklist, manual completion asks first, reached duration
  completes once with the reach date and can be undone, pending reached audits.
- Installed on the user's phone for manual checks; alarm sound, background completion and TalkBack not
  verified yet.
- Known limit: the completion date is the moment the reach was detected (sync), which can be later than the
  real end when the app was closed.

## Follow-up (2026-11-11)

- Task editor list choice gets "+ Criar lista": the list editor opens on top and the new list is selected
  for the task being written.
- Frequency labels renamed for clarity: Diário, Semanal, Mensal (tasks' repetition and habits' frequency,
  including timed habits).
