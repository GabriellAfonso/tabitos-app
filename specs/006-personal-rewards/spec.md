# Feature Specification: Personal rewards

**Feature Branch**: Existing branch retained; no Git mutation.
**Created**: 2026-11-07
**Status**: Implemented; verification evidence and limits in verification.md.
**Input**: TODO/specify_recompensas_app.md; TODO/planejamento_ponto_6.md.
Current user request authorizes specify, plan, tasks, analyze and implement sequentially, overriding
saved prompt's earlier specify-only restriction. No commits or publication.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Organize rewards (Priority: P1)
Owner creates, edits, orders and deletes personal rewards offline.
**Why this priority**: Real purchases require owner-defined items rather than sample data.
**Independent Test**: Create two rewards, edit/order, relaunch and confirm persisted details.
**Acceptance Scenarios**:
1. **Given** an empty store, **When** opening, **Then** show real balance and create action without samples.
2. **Given** a form, **When** saving name/icon/positive integer cost and optional description/limit,
   **Then** persist valid data; reject blank name, invalid cost/limit without phantom saves.
3. **Given** items, **When** editing/reordering and relaunching, **Then** retain their order/details.
4. **Given** a purchased item, **When** confirming deletion, **Then** remove it from store, preserve history
   and issue no refund; cancelling deletion leaves the item available.

### User Story 2 - Confirm immediate purchases (Priority: P1)
Owner spends earned coins on immediate use after a clear confirmation.
**Why this priority**: Core value and protection against mistakes.
**Independent Test**: Earn coins through a task, cancel one preview, confirm another and relaunch.
**Acceptance Scenarios**:
1. **Given** an affordable item, **When** opening confirmation, **Then** show item, full price, resulting
   balance and allowance AFTER purchase if limited, with confirm and cancel/back actions.
2. **Given** confirmation, **When** cancelling/backing out, **Then** spend nothing and consume no quota.
3. **Given** exact price balance, **When** confirming, **Then** record immediate use, zero remaining coins
   and unchanged XP/level. Negative/insufficient balances block purchase with an explanation.
4. **Given** taps/retries/process recreation/concurrent requests, **When** retrying one confirmed intent,
   **Then** commit at most one debit/use record; failed attempts leave no partial effects.
5. **Given** a committed purchase, **When** intentionally buying again, **Then** allow a distinct purchase
   if affordable/within quota. No unused inventory, separate use action, cancellation or refund exists.
6. **Given** changed reward/price/wallet/quota/date after preview, **When** confirming, **Then** refresh
   and explain before obtaining valid confirmation; never silently change the charge/result/allowance.

### User Story 3 - Respect per-item calendar limits (Priority: P1)
Owner restricts independent rewards by day, Monday–Sunday week or calendar month.
**Why this priority**: Restrictions must remain trustworthy through editing and restarts.
**Independent Test**: Exhaust a limit, edit it and its period, advance date and check allowance/history.
**Acceptance Scenarios**:
1. **Given** independently limited and unlimited items, **When** buying one, **Then** only its quota changes;
   unlimited items have no artificial quantity restriction.
2. **Given** exhausted allowance, **When** crossing the applicable civil boundary, **Then** renew allowance
   without deleting history; weeks start Monday, months on day one.
3. **Given** past purchases, **When** editing fields/limit/period, **Then** count purchases in the newly
   applicable period; reducing limit below usage blocks further purchases without retrospective penalty.

### User Story 4 - Trust history and wallet (Priority: P2)
Owner sees understandable spending in existing coin history after editing/deleting rewards.
**Why this priority**: Existing accounting must remain trustworthy.
**Independent Test**: Buy, edit/delete reward, complete/correct tasks/habits, restart and inspect history.
**Acceptance Scenarios**:
1. **Given** purchases, **When** viewing history after edit/deletion, **Then** preserve original name,
   description/icon/price, reason/date/time, actual debit/resulting balance and stable purchase reference.
2. **Given** spending, **When** refreshing/correcting tasks/habits, **Then** preserve it exactly once and
   keep existing original-price corrections, XP rules and correction floor.
3. **Given** failed writes/relaunch/upgrade, **When** recovering, **Then** preserve purchases/quota and all
   existing data consistently, with actionable errors rather than phantom success.

### Edge Cases
Exact/negative balance; overflowing numeric input; midnight/Monday/month/year boundaries; local timezone
changes; changed limits/periods; deleted or edited reward with open confirmation; competing confirmations;
retry after uncertain success; long labels/landscape/200% fonts/light/dark/system/accessibility.

## Requirements *(mandatory)*

### Escopo e restrições do Ethos
Reuse Recompensas and Histórico de moedas offline. No new permissions or personal logging. This is a
self-management record, not real payment or proof of activity. Preserve old data/preferences.
Exclude manual grants, XP costs/bonuses, categories/bundles, unused inventory, deferred use, refunds,
restore UI, timers/reminders/background services, device restrictions, backend/login/sync and publishing.
Timers remain future work in TODO/planejamento_lembretes_tempo.md.

### Functional Requirements
- **FR-001**: Create/edit required name/icon/positive whole coin cost, optional description and optional
  positive per-reward limit; validate without phantom saves.
- **FR-002**: Persist definitions/order/limits/purchases/identities across relaunch; allow reordering.
- **FR-003**: Confirm deletion; hide item without losing history or refunding purchases.
- **FR-004**: Show real balance/cards/details/cost/limit status and clear insufficient/exhausted reasons;
  provide honest loading/empty/content/error/create states.
- **FR-005**: Confirmation shows item/full price/resulting balance and allowance AFTER purchase when limited.
- **FR-006**: Cancel/back before commitment spends nothing and consumes no quota.
- **FR-007**: Purchase IS immediate use; no unused entitlement, separate activation, cancellation or refund.
- **FR-008**: Full cost required; resulting balance >=0, including exact-balance success; no XP payment/minting.
- **FR-009**: Preserve XP/levels, earning tables, penalties, -300 correction floor and original-price semantics.
- **FR-010**: Independent optional civil day/Monday–Sunday week/calendar month limits; retain historical purchases.
- **FR-011**: Edits count existing purchases in new applicable period; other edits cannot reset quota;
  limit below usage blocks future purchases without rewriting old purchases.
- **FR-012**: Revalidate existence/details/price/balance/quota/period at commit; stale previews require
  explanation and renewed valid confirmation without silent changes.
- **FR-013**: Debit/purchase/quota effect commit together; failures have no partial effects.
- **FR-014**: Durable intent identity prevents duplicate taps/retry/recreation/concurrent charges;
  distinct later purchases remain possible within balance/quota.
- **FR-015**: Existing ledger preserves original reward name/description/icon/price, reason/date/time,
  effective debit/resulting balance and stable purchase reference after edits/deletion.
- **FR-016**: Task/habit refresh/reconciliation/corrections cannot erase or duplicate spending.
- **FR-017**: Preserve old tasks/habits/audits/prices/activation/ledger/XP/preferences during upgrade;
  validate migration and failure recovery.
- **FR-018**: Portuguese text/current themes/insets/navigation, accessible labels/actions and enlarged-font
  reachability; preserve draft/errors and critical result until acknowledgement; no false success.

### Key Entities *(include if feature involves data)*
- Reward: identity, name/description/icon, current cost, order, optional limit, availability.
- Purchase: durable intent identity, reward reference, original details/cost, civil date/time, linked debit.
- Allowance: derived reward-specific usage in current period, never a global counter.
- Wallet/ledger: existing balance/XP history extended with immutable attributable spending.

## Success Criteria *(mandatory)*

### Measurable Outcomes
- **SC-001**: All valid create/edit/order/delete/relaunch cases preserve 100% of details and old history.
- **SC-002**: Each confirmed purchase has exactly one debit/use record, no XP change and nonnegative balance;
  cancellation/failure cases spend zero coins.
- **SC-003**: All calendar-boundary/edited-limit scenarios respect independent allowances.
- **SC-004**: All duplicate/concurrency/retry cases avoid double charges, debt and excess quota while
  permitting separate eligible purchases.
- **SC-005**: Every stale confirmation blocks silent changes; historical details survive edits/deletion/corrections.
- **SC-006**: Store/editor/confirmation/history actions remain reachable at 200% fonts in light/dark themes,
  with explicitly recorded runtime/accessibility verification limits.

## Assumptions
Existing device-local civil date policy applies; stored purchase dates are not retroactively rewritten on
zone changes. Simple preset icons and accessible move-up/down ordering follow app conventions.
One local owner/existing wallet; no external interfaces. Product decisions in the saved prompt are closed.
Earlier feature scopes remain historical; this feature enables spending they previously deferred.
