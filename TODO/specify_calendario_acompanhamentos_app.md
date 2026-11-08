# Specify — Ponto 9: calendário e acompanhamentos personalizados

**Estado:** pronto para executar specify em outro chat.
**Data:** 2026-11-07.

## Como usar

Na raiz `C:/Users/gabri/Projetos/ethos`, execute `$speckit-specify` (no Claude: `/speckit-specify`)
com o corpo em inglês após `---`. Execute somente specify. Plan, tasks, analyze, implement, commits
e publicação dependem de solicitação separada. Salvar este prompt não executa Spec Kit.

## Referências e origem

- `AGENTS.md` e `.specify/memory/constitution.md` v1.0.0.
- `specs/001-android-foundation/` até `specs/008-timers-duration-goals/`: specs, planos, contratos
  e verificações existentes, em especial 002 (shell/Calendário), 004 (hábitos) e 007 (lembretes).
- `TODO/planejamento_calendario_acompanhamentos.md`: decisões aprovadas do ponto 9.
- `prototypes/hoje/index.html`: direção visual B; não contém o calendário nem o construtor.
- Origem: conversa Codex "Analise o app base em Kotlin", ID `01a1b12c-0445-7cc0-a8a0-d3ff1377af37`,
  transcript `~/.codex/sessions/2026/11/06/rollout-2026-11-06T11-03-34-01a1b12c-0445-7cc0-a8a0-d3ff1377af37.jsonl`
  (mensagens de 2026-11-07 22:09–22:19). Prompt consolidado pelo Claude Code a partir desse
  transcript; o corpo abaixo registra todas as decisões sem depender dele.
- Sem contraparte de backend.

---

Create the feature specification for Ethos point 9: a full calendar (monthly/weekly/daily views
with filters) and user-built habit trackers ("acompanhamentos") — per-habit custom forms whose
dated entries give context about a positive or negative habit, with history and simple analytics.
Execute only the Spec Kit specify stage. Do not generate plan/tasks, implement, commit, publish,
or advance automatically. Saved design prompts do not authorize implementation.

## Inspect current state

Work in C:/Users/gabri/Projetos/ethos. Read AGENTS.md, constitution v1.0.0, existing feature specs,
plans, contracts and verification files, then inspect relevant Kotlin source before writing the
spec. The owner designed this feature in a separate conversation and hands over this self-contained
prompt. Do not ask again about any approved choice recorded below.

At handoff (verified 2026-11-07), features 001–008 are implemented: foundation, shell, lists/tasks/
checklists/deadlines/recurrence/review, habits/goals, coins/XP/levels, personal rewards, reminders/
notifications and timers/duration goals. Stack: Kotlin, AGP 9 built-in Kotlin, Compose Material 3,
Hilt/KSP, Navigation Compose, minimum SDK 24, Room (exported schemas 1–6 with preserving migrations),
DataStore for appearance preferences only.

Observed source facts to re-verify, not assume:
- The bottom navigation already has a Calendário destination (`ShellDestination.CALENDAR`). It
  currently maps to `TaskPage.CALENDAR` in `features/tasks/TasksContent.kt`: a free-text
  `yyyy-MM-dd` field that lists active task occurrences for one typed date. There is no month/
  week/day grid, no filters and no habit records in that view. A `calendar_empty` placeholder
  string still exists in the shell.
- Habit detail (`features/habits/HabitsScreen.kt`, `HabitCalendar` in `HabitsContent.kt`) shows a
  per-habit monthly grid of records grouped by actual date, followed by individually correctable
  records. Habits support practice/avoid/limit goals, Monday–Sunday civil weeks and calendar
  months, neutral inactive dates and back-dated records (spec 004).
- Reminders live in `core/reminders` with `ReminderKind { TASK, HABIT }`, modes CHOSEN/RELATIVE/
  WEEKDAYS, local alarms with approximate fallback, snooze replacing the same slot, in-context
  notification permission, existing channels and no-burst recovery (spec 007). Timers have their
  own channels in `core/timers`.

Previous verification covers API 36 emulator only; API 24, physical device and TalkBack audio were
not validated. Report this feature's own coverage separately. Personal APK; offline; no login,
backend or cloud.

## Purpose

Let each person build their own tracking form for a habit — positive (e.g. "Disposição para
praticar") or negative (e.g. "Intensidade da vontade") — to record context and follow its evolution
over time, not merely mark an occurrence. Also turn the Calendário tab into a real calendar that
gathers tasks, habits and tracker entries by date.

The owner supplied a long reference text ("Diário de Acompanhamento de Hábitos e Compulsões":
urge intensity, routine impact, triggers, behavior, duration, after-feeling, notes, dashboards,
goals, privacy, export, permanent deletion). It is inspiration only. Do NOT ship a fixed
compulsion/pornography form, prefilled intimate examples, clinical modules, diagnoses or a user
profile derived from answers. Every question is created by the user; examples may appear only as
neutral, editable suggestions if the plan finds them useful, never as mandatory content.

## Approved scope — tracker builder

1. Each habit may have its own tracker form, created and edited by the user. Trackers are
   optional; habits without one behave exactly as today.
2. First-version field types: numeric scale, single choice, multiple choice, free text.
3. The user marks each question as required or optional.
4. Entry frequency per tracker: one entry per day (DEFAULT) or multiple entries per day, chosen
   by the user.
5. The user may mark ONE scale as the primary indicator, used for highlight in summaries and in
   charts. Scale configuration: name, minimum, maximum and descriptions of both extremes; default
   range 0–10.
6. The user can reorder sections/questions freely and that order is preserved. Being primary
   defines highlight/analysis only; it does NOT force the scale to the top.
7. Choice questions: create, rename and reorder options; each question is configured as single or
   multiple answer. Example: "Quanto a vontade prejudicou sua rotina?" with Nada/Levemente/
   Moderadamente/Muito (options may carry short descriptions); "Quais gatilhos você percebeu?"
   with multi-select options.
8. Removing an option hides it from new entries and preserves it, with its historical label, in
   past entries that used it.
9. Form changes apply to new entries. Past entries keep the questions, options, scales and labels
   of the form version they were answered with. Editing an old entry uses its original version
   and never demands questions added later. Reordering the current form must not silently rewrite
   historical structure.

## Approved scope — entries and dates

10. Entries have a reference date chosen by the user (e.g. fill yesterday after forgetting),
    distinct from the real moment of filling, which is also recorded.
11. Allowed reference dates: from the tracker's creation date through today. Reject future dates
    and dates before creation.
12. In one-per-day mode, choosing an already filled date opens that entry for editing; never
    create a duplicate. In multiple-per-day mode, each entry is kept and shown individually.
13. Filling or not filling a tracker NEVER changes coins/XP, never completes a practice, never
    records a relapse/occurrence and never affects goals, streaks, review or rewards. Answers are
    context stored separately from habit records. A high urge value is not a failure.

## Approved scope — tracker reminder

14. The user may set a reminder at a chosen time to fill the tracker.
15. The owner wants a persistent notice until resolved. Use an ongoing notification where the OS
    allows it, but do NOT promise an undismissable notification: since Android 14 users can
    dismiss most ongoing notifications (consult current official docs). Dismissing the
    notification does not resolve the pending state; the pending tracker stays visible in the app.
    Do not use a foreground service just to keep this notice alive.
16. Actions: Preencher (opens the form for the pending date) and Adiar (snooze). Opening the form
    does not resolve the pending state; only saving a valid entry does.
17. Notification text is neutral, e.g. "Você tem um acompanhamento para preencher". Never expose
    habit name, questions or answers in the notification, lock screen or channel names.
18. Reuse the existing reminders infrastructure (channels, permission flow, scheduling, snooze,
    exact/approximate fallback, no-burst recovery, navigation into the existing Activity) by
    extending it, not by building a competing notification system. Stop reminding when the habit
    is paused/excluded or the tracker is removed, consistent with spec 007.

## Approved scope — calendar

19. Monthly, weekly and daily views in the Calendário tab, replacing the free-text date lookup.
20. Filters by list, by habit and by type: tasks, habits and trackers.
21. Tapping a date opens its items and records and allows filling or editing that date's tracker
    entry, respecting the tracker's frequency mode and the date limits above.
22. Days with tracker entries show a discreet indicator. A day without an entry is shown as
    "Sem dados" — never as zero intensity, confirmed abstinence or failure.

## Approved scope — analytics

23. Selecting a tracker shows the evolution of its primary scale and the frequency of chosen
    options, for periods of 7, 14, 30 and 90 days.
24. Never mix scales from different habits/trackers in one series. Never present associations
    between answers as diagnosis or causality.
25. In multiple-per-day mode, show each entry individually in history and charts; do not sum or
    average daily values automatically.
26. Missing days are gaps ("Sem dados"), not zeros.

## Explicit exclusions for this delivery

PIN/biometric lock, export, specific encryption schemes, permanent "erase everything" flows,
clinical modules, goals/indicators computed from tracker answers, interval-between-episodes
analytics, behavior-duration tracking inside trackers (timers are feature 008), cloud sync and
analytics/telemetry. Keep privacy local and do not invent these requirements. Do not change the
existing backup/data-extraction configuration without an explicit decision; if relevant, note it
as an open item.

## Minimal integration assumptions (document as assumptions, do not re-ask)

These were not individually discussed; the spec should state them as assumptions with the
smallest behavior consistent with approved rules, and the owner may revise later:
- Reminder schedule: one chosen time, daily, on dates where the tracker can be filled; reuse
  existing reminder modes (e.g. WEEKDAYS) only if it adds no new product rule. Snooze reuses the
  existing reminder snooze options.
- Pending state: in one-per-day mode, a date is pending until its entry is saved; in
  multiple-per-day mode, saving at least one valid entry for that date resolves that day's notice.
  Pending never accumulates into a burst of past notifications (follow 007 recovery rules).
- Entry deletion: allow deleting an individual entry with confirmation, reusing the existing
  record-correction patterns; no bulk/permanent wipe feature.
- Tracker lifecycle follows the habit's existing pause/exclusion semantics; history is preserved
  where the habit's history is preserved.
- Calendar habit content shows existing habit records by actual date; whether/how to show which
  habits apply on a given day must derive only from existing habit rules, without inventing new
  scheduling semantics.
- If a primary scale's min/max changes between form versions, charts must not silently rescale
  old values as if comparable; the plan defines the minimal honest presentation.

## Data, architecture and UI expectations

- Room is the source of truth for tracker definitions, versioned form structure, options, entries
  and answers. Add schema 7 with a preserving 6→7 migration; preserve all existing data (tasks,
  occurrences, habits/records, lists, ledger, rewards/purchases, reminders, timers, preferences).
  No destructive fallback, no JSON cache replacing tables, no demo data in production.
- Respect AGENTS.md architecture: features do not import other features' implementations; share
  contracts through `core` when needed; `domain/` free of Android/Room/Compose.
- `StateFlow<UiState>` per ViewModel, Screen/Content split, loading/empty/content/error where
  applicable, previews for shared components with explicit data.
- Portuguese visible strings in `strings.xml`, English identifiers, light/dark/system theme, model B
  visual direction, accessibility (scale controls usable with TalkBack and large fonts, icon
  descriptions, calendar cells with meaningful content descriptions).
- Never log tracker names, questions or answers.

## Acceptance and verification expectations

Write independently testable scenarios covering at least:
1. Building a form with the four field types, required/optional flags, primary scale (default
   0–10, custom range/extremes), reordering preserved, primary not forced to top.
2. Option create/rename/reorder, single vs multiple answer, removed option preserved historically.
3. Form versioning: new entries use the current version; editing old entries uses their version
   without requiring later questions; reorder does not rewrite history.
4. Reference date vs filled-at time; allowed range creation..today; one-per-day edit instead of
   duplicate; multiple-per-day individual entries.
5. Zero effect on coins/XP, goals, streaks, review, rewards and habit records from any tracker
   action.
6. Reminder at chosen time with neutral text, Preencher/Adiar, pending resolved only by saving,
   dismissal not resolving, permission/channel denied states, stopping on pause/exclusion/removal,
   no burst after reboot or missed delivery, honest Android 14+ dismissibility limit.
7. Calendar month/week/day views, filters by list/habit/type, day sheet with tasks/habit records/
   tracker entries, fill/edit from a date within limits, discreet indicator, "Sem dados".
8. Analytics for 7/14/30/90 days: primary scale evolution, option frequency, no cross-tracker
   mixing, individual multi-entry points, gaps not zeros, no causal/diagnostic wording.
9. Existing features unchanged: task calendar lookup replaced without losing access to a date's
   tasks; habit detail calendar, reminders, timers and accounting keep their behavior.
10. Exported schema 7 and preserving migration test after the full 1→6 chain, pure domain tests
    with controlled clocks/dates, Room tests, UI/notification integration checks, and stated
    coverage gaps (API 24, physical device, TalkBack audio) when implementation is later authorized.

## Closed choices and planning discipline

All items 1–26 above were explicitly approved by the owner. Do not ask again about per-habit forms,
field types, required/optional, frequency mode and default, primary scale and its default range,
section reordering, option editing, date limits, reminder persistence/actions/privacy, zero domain
effects, calendar views/filters/indicators, analytics periods or form versioning.

If the scope is too large for one coherent spec, the spec may define prioritized, independently
testable user stories (e.g. P1 tracker builder + entries, P2 calendar, P3 analytics + reminder)
rather than splitting into new prompts. Resolve routine mechanics in the later plan using existing
contracts and current official Android documentation; escalate only a genuine blocking
contradiction. This handoff authorizes specification only, not subsequent phases or Git mutations.
