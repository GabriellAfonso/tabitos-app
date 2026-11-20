# Feature Specification: Game Mode Toggle ("Modo jogo")

**Feature Branch**: `017-game-mode-toggle`
**Created**: 2026-11-11
**Status**: Implemented
**Input**: `TODO/specify_modo_jogo_app.md` (body after `---`), from a Claude Code planning conversation on
2026-11-11 (no transcript ID available; all decisions are restated below).

## Problem

Not every user wants the gamified layer (coins, XP, levels, personal rewards). Ethos must also work as a
plain task/habit/routine app, and the user must be able to switch between both modes at any time, easily
and without losing data.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Turn the game layer off (Priority: P1)

A user who only wants to organize tasks and habits opens Settings, turns off "Modo jogo", confirms a short
dialog, and from then on sees no coins, XP, levels, rewards, prices, difficulty or financial dialogs.

**Why this priority**: it is the core value of the feature; without it nothing else matters.

**Independent Test**: turn the mode off and walk through Hoje, the drawer, Settings, the task and habit
editors and completion flows; no game element is visible and no confirmation about coins appears.

**Acceptance Scenarios**:

1. **Given** Game mode is ON, **When** the user switches it off in Settings, **Then** a dialog explains that
   coins, XP and rewards will be hidden and nothing will be deleted, with confirm and cancel.
2. **Given** the dialog is shown, **When** the user cancels, **Then** the mode stays ON and nothing changes.
3. **Given** the user confirms, **Then** without restarting the screen, Rewards and History disappear from
   navigation, the balance/shortcut disappears from the Hoje header, price settings disappear from
   Settings, and the difficulty selector and coin/XP estimate disappear from task and habit editors.
4. **Given** Game mode is OFF, **When** the user unchecks a task that had paid coins, or registers an
   avoid/limit occurrence, **Then** the action applies directly without a financial confirmation, and the
   existing Undo still works.
5. **Given** Game mode is OFF, **When** a notification or deep link targets Rewards or History, **Then** the
   app opens Hoje instead.

---

### User Story 2 - Nothing counts while off (Priority: P1)

While Game mode is OFF, completing tasks, marking habits, timers reaching their goal and avoid/limit events
earn no coins/XP and charge no penalties — and are never paid later.

**Why this priority**: the rule must be enforced in the data layer, otherwise turning the mode back on
would show a surprise balance or hidden debt.

**Independent Test**: note balance and XP, turn OFF, complete several tasks/habits and register an
avoid/limit occurrence, turn ON; balance and XP are exactly the same as before.

**Acceptance Scenarios**:

1. **Given** Game mode is OFF, **When** the user completes a task or marks a habit, **Then** no coin/XP
   entry is recorded for that outcome.
2. **Given** outcomes were recorded while OFF, **When** the user turns the mode ON, **Then** balance, XP and
   level are identical to the values before turning OFF (no retroactive payment).
3. **Given** a task was completed and paid while ON, **When** it is unchecked (or undone) while OFF,
   **Then** the coins/XP it earned are reversed silently, keeping the ledger consistent.
4. **Given** a habit period closes automatically while OFF (spec 014 settle), **Then** it earns nothing.
5. **Given** a timed task reaches zero while OFF (spec 016), **Then** it completes normally without coins.

---

### User Story 3 - Turn the game layer back on (Priority: P2)

A user who turned the mode off can turn it back on and find everything as it was.

**Why this priority**: reversibility is what makes the switch safe to try.

**Independent Test**: with rewards, purchases, custom prices and a non-zero balance, turn OFF and ON; all
data is visible again unchanged.

**Acceptance Scenarios**:

1. **Given** Game mode is OFF, **When** the user switches it on, **Then** it applies immediately with no
   dialog, and Rewards, History, balance, prices, difficulty and estimates reappear.
2. **Given** data existed before turning OFF, **Then** rewards, purchase history, prices, balance, XP and
   level are unchanged.
3. **Given** the mode was set either way, **When** the app process is killed and reopened, **Then** the
   mode is the same as before; theme preferences are unaffected.

---

### User Story 4 - Safe switching with an active reward timer (Priority: P3)

Switching the mode is blocked while a timed reward purchase is counting.

**Why this priority**: rare case, but turning off mid-purchase would hide a running paid session.

**Independent Test**: start a timed reward purchase, open Settings; the switch is disabled with an
explanation; pause/finish the session and it becomes available.

**Acceptance Scenarios**:

1. **Given** a reward purchase timer is counting, **When** the user opens Settings, **Then** the switch
   cannot be changed and a short explanation says to pause the reward timer first.
2. **Given** the reward timer is paused or finished, **Then** the switch works normally.

### Edge Cases

- Outcome recorded while OFF, then unchecked while ON and checked again while ON: the new outcome follows
  the mode at the moment it is recorded (it pays), because the first outcome no longer exists.
- Outcome recorded while ON, then unchecked while OFF and checked again while OFF: reversal happens, the
  new outcome earns nothing.
- The balance floor (-300) and level rules are unaffected; they simply see no new entries while OFF.
- Notification actions (reminder "Concluir", timer completion) follow the same rule as the in-app flow:
  no financial confirmation while OFF, existing non-financial confirmations stay.
- A timed reward purchase that is paused (not counting) when the mode is turned OFF stays stored and
  hidden; it reappears with the rewards when the mode is turned ON.
- Debug demo restore works in either mode and never changes the mode preference.
- Existing items keep their stored difficulty while OFF; new items created while OFF get the default
  difficulty and can be changed after turning the mode ON.

## Requirements *(mandatory)*

### Escopo e restrições do Ethos

- Local-only; no backend, login or sync. No new permission.
- The mode is a preference (DataStore), like theme; ledger, rewards and prices stay in Room unchanged.
- Visible texts in Portuguese in resources; the switch and dialog are accessible to TalkBack and large
  fonts.
- No Room schema change is expected; if planning finds one necessary, it must have a preserving migration.

### Functional Requirements

- **FR-001**: The app MUST offer a "Modo jogo" switch at the top of Settings, ON by default for new installs
  and for existing users.
- **FR-002**: Turning the mode OFF MUST ask for confirmation in a short dialog stating that coins, XP and
  rewards will be hidden and nothing will be deleted. Turning it ON MUST apply without a dialog.
- **FR-003**: The mode MUST be persisted as a user preference, survive process death/restart and apply
  immediately everywhere without restarting the Activity. Theme preferences MUST be unaffected.
- **FR-004**: The rule MUST be enforced where outcomes are recorded: any task/habit outcome (manual,
  automatic period settle, timer completion, notification action) or avoid/limit event recorded while OFF
  MUST NOT create coin/XP gains or penalties.
- **FR-005**: Outcomes recorded while OFF MUST NOT be paid when the mode is turned ON later; outcomes keep
  the scoring status they had when recorded.
- **FR-006**: Undoing an outcome that paid while ON (uncheck, Undo, correction) MUST reverse its coins/XP
  even while OFF, without any financial confirmation.
- **FR-007**: Turning the mode OFF or ON MUST NOT delete or alter balance, XP, level, rewards, purchases,
  history, prices or item difficulties.
- **FR-008**: While OFF, the app MUST hide: Rewards and History navigation destinations, the balance and
  Rewards shortcut in the Hoje header, price settings, the difficulty selector and coin/XP estimate in task
  and habit editors, financial confirmation dialogs, and timed reward purchases.
- **FR-009**: While OFF, navigation requests to Rewards or History (deep link, notification, saved route)
  MUST land on Hoje.
- **FR-010**: While OFF, items created get the default difficulty; edits keep the stored difficulty.
- **FR-011**: Switching the mode MUST be blocked, with an explanation, while a reward purchase timer is
  counting.
- **FR-012**: Debug demo restore MUST work in both modes and MUST NOT change the mode.
- **FR-013**: Tasks, habits, streaks, consistency, calendar, trackers, reminders and task/habit timers MUST
  behave the same in both modes apart from the hidden game elements.

### Key Entities

- **Game mode preference**: on/off user preference; default off (changed 2026-11-19).
- **Outcome scoring status**: whether an existing task/habit outcome is priced (pays) or exempt; set when
  the outcome is recorded according to the current mode and never changed by later mode switches.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After turning the mode off, zero game elements (coins, XP, level, rewards, prices, difficulty,
  financial dialogs) are visible in Hoje, lists, habits, editors, Settings and the drawer.
- **SC-002**: Balance and XP after an OFF period with any number of outcomes equal the values right before
  turning OFF (difference 0), except for reversals of outcomes paid while ON.
- **SC-003**: Switching applies in under 1 second, without the screen restarting.
- **SC-004**: 100% of rewards, purchases, history entries and prices present before an OFF→ON cycle exist
  after it.
- **SC-005**: The chosen mode is restored after 100% of app restarts.

## Out of scope

- First-launch question choosing the mode.
- Deleting or resetting wallet/rewards data when turning off.
- New gamification, replacement metrics for breaking habits, or partial toggles (e.g. XP without coins).
- Backend, login, sync.

## Assumptions

- Streak and consistency percentage are enough feedback for habits while OFF (decision 9).
- The existing exempt/unpriced entitlement mechanism (`AccountingStore.initialize()`) is a candidate to mark
  outcomes recorded while OFF; the plan decides.
- The Hoje header itself (date/greeting) stays; only the balance/Rewards shortcut is hidden.
- Non-financial confirmations (e.g. "Concluir sem completar o tempo?" from spec 016) remain in both modes.
