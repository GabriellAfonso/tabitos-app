# Feature Specification: Interface, navigation and appearance

**Feature Branch**: master (existing; no branch creation requested)
**Created**: 2026-11-06
**Status**: Implemented; evidence and coverage limitations in verification.md
**Input**: TODO/specify_interface_navegacao_app.md; current request authorizes all subsequent stages.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Navigate the organizer (Priority: P1)
The owner opens Hoje offline and moves between recognizable destinations.
**Why this priority**: This shell supports future organizer features.
**Independent Test**: Open all destinations, repeat taps and press Back.
**Acceptance Scenarios**:
1. **Given** offline launch, **When** opening Ethos, **Then** Hoje shows today's local date, neutral
   balance/level tiles, compact content and Tudo/Tarefas/Hábitos filters.
2. **Given** any screen, **When** choosing a bottom item, **Then** Hoje, Tarefas, Hábitos or Calendário
   opens with visible destination identity; repeated selections do not accumulate copies.
3. **Given** the drawer, **When** choosing Hoje, Revisar pendências, Recompensas, Histórico de moedas
   or Configurações, **Then** the drawer closes and that destination opens.
4. **Given** an open drawer, **When** pressing Back, **Then** it closes first; otherwise Back returns
   from another destination to Hoje, then exits. Rotation restores the destination and filters.
5. **Given** Hoje, **When** tapping balance or settings, **Then** rewards or settings opens.

### User Story 2 - Understand empty and future capabilities (Priority: P1)
The owner sees honest empty content, with examples confined to development presentations.
**Why this priority**: Fabricated progress would misrepresent personal records.
**Independent Test**: Inspect runtime empty screens and explicit populated development samples.
**Acceptance Scenarios**:
1. **Given** no records, **When** changing each Hoje/review filter, **Then** selection changes and
   relevant empty content appears, without pending notices/counters or fictional records.
2. **Given** sample pending items, **When** filtering, **Then** only matching types appear;
   notice/menu counts match the total and disappear at zero.
3. **Given** rewards/history/calendar/list areas, **When** viewed, **Then** scope is explained and
   unavailable creation, confirmation or redemption never claims success.
4. **Given** development samples, **When** inspected, **Then** compact task/habit rows, reward cards,
   history rows and list Tarefas/Hábitos tabs can be evaluated without production seed data.

### User Story 3 - Retain appearance (Priority: P2)
The owner selects Claro, Escuro or Seguir o sistema and keeps it across launches.
**Why this priority**: Appearance is the only persistent user data in this delivery.
**Independent Test**: Switch modes, navigate/relaunch, and simulate read/write failures.
**Acceptance Scenarios**:
1. **Given** settings, **When** selecting each appearance, **Then** all screens update without a
   manual restart and the saved choice survives reopening.
2. **Given** system selection, **When** device appearance changes, **Then** Ethos follows it;
   explicit light/dark selection overrides the device.
3. **Given** a preference read/write failure, **When** operating settings, **Then** a recoverable
   message offers retry and never reports an unsaved choice as saved.
4. **Given** settings, **When** opening earnings/penalties information, **Then** agreed principles
   are distinguished from undecided numeric formulas.

### Edge Cases
- Back closes drawer before navigating; fresh launch starts Hoje, restoration retains destination.
- Unknown stored appearance defaults to system; failed reads expose retry with a system fallback.
- Rapid selections cannot let an earlier save overwrite a later selection.
- At 200% fonts, landscape, long Portuguese labels and system bars, scrolling keeps controls reachable.
- Review remains reachable at zero without a badge or Hoje notice.

## Requirements *(mandatory)*

### Escopo e restrições do Ethos
Visual shell reflecting prototypes/hoje/index.html round 02 model B: compact cards, sage accents,
light/dark hierarchy. Exclude browser controls, phone frames and simulated system bars.
Offline, no login, backend, new permissions, publication or personal domain records.
Preserve foundation/build/signing. Exclude CRUD, recurrence, habit goals, calendar engine,
currency/XP computation and reward management/redemption.

### Functional Requirements
- **FR-001**: Start offline at Hoje with date, compact content, neutral balance/level tiles and four bottom destinations.
- **FR-002**: Provide specified drawer destinations, Minhas listas heading and honest future creation status.
- **FR-003**: Close drawer on navigation, avoid duplicate roots and implement drawer-first Back, then Hoje, then exit.
- **FR-004**: Independently select Tudo/Tarefas/Hábitos on Hoje/review and retain filters on restoration.
- **FR-005**: Show relevant empty states everywhere, without sample records or fabricated balance/earned level in production.
- **FR-006**: Show pending notices/counters only at positive counts; demonstrate zero/populated sample states.
- **FR-007**: Open rewards from Hoje balance, settings from header, history/review from drawer; no fifth bottom item.
- **FR-008**: Omit, disable or explain unavailable domain operations; never claim false save/confirmation/redemption.
- **FR-009**: Offer Claro/Escuro/Seguir o sistema globally, retaining selection after reopening.
- **FR-010**: Follow device theme only in system mode; expose preference read/write failures with retry and honest saved status.
- **FR-011**: Explain earnings/penalties in settings without a numeric balancing table.
- **FR-012**: Portuguese content, labeled actions, minimum 48dp touch targets, usable 200% fonts/landscape/system insets.
- **FR-013**: Isolate explicit populated development samples for Hoje/review/rewards/history/list tabs from production.
- **FR-014**: Verify navigation, preference persistence/failures, build and accessibility, recording coverage and limitations.

### Key Entities
- **Appearance preference**: system/light/dark, sole persistent setting.
- **Presentation state**: current destination and independent filters, restorable without domain records.
- **Development sample**: explicit visual records, never production source of truth.

## Success Criteria *(mandatory)*
### Measurable Outcomes
- **SC-001**: All 8 destinations are reachable offline within 2 actions from Hoje; 10 repeated taps add no Back steps.
- **SC-002**: All 6 Hoje/review filter choices show correct empty or sample content; zero fictional live records.
- **SC-003**: All 3 appearances survive reopening; both preference failure scenarios offer recovery without false success.
- **SC-004**: Tested portrait/landscape at 200% fonts retain reachable, labeled actions in both appearances.
- **SC-005**: Every acceptance check records executed evidence or a concrete unverified limitation.

## Assumptions
- Foundation is implemented: specs/001-android-foundation; historical API36 evidence is not new feature coverage.
- No blocking product questions. Minor navigation/restoration choices belong to planning.
- Current instruction supersedes specify-only limits in saved prompt; no commit/publication authorized.
- Full future decisions remain in the source prompt and TODO/planejamento_ponto_2.md. They are deferred:
  lists have task/habit tabs; recurrence includes daily/weekdays/selected weekdays/monthly/every-X-days,
  weekly deadlines without day, Monday–Sunday weeks and Friday weekdays cutoff without holidays.
  Missing monthly days use last day while preserving original day. X-day intervals have fixed-calendar
  and completion-relative modes. Overdue work needs review; no automatic deadline penalty. Failure
  affects one occurrence; rescheduling reviews original deadline; deleting obsolete work is not failure.
  Negative habits allow multiple confirmed events, excess-limit events each penalize individually.
  Difficulty Fácil/Média/Difícil and importance Baixa/Média/Alta are separate; Difícil suggests Alta
  only without prior manual importance. Difficulty increases effort rewards, importance consequences.
  Coins and XP are earned, no energy; failures remove no XP/levels. Coins floor is −300, incidents
  remain in history at the floor, reward redemption requires full funds and cannot create debt.
- Form fields, time zones, habit targets, ledger reversals, formulas/XP curve, reward editing and real
  calendar behavior are future decisions. Prototype amounts, dates, names and levels are examples only.
