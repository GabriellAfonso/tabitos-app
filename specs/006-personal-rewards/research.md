# Research: Personal rewards
Read-only research agent required by local speckit-plan phase0 inspected existing code and environment.

## Shared accounting
Decision: append PURCHASE LedgerEntry in TaskDatabase.withTransaction, without earning entitlement.
Rationale: AccountingRules.reconcile starts with existing entries and only reconciles selected entitlements;
AccountingStore snapshots therefore preserve spending naturally. All source repositories share transactions.
Alternatives: task penalties/overwriting totals/second ledger rejected as corrupting domain semantics.

## Durable confirmation
Decision: persisted intent contains UUID, full reward revision/details, date, disclosed balance and usage;
purchase PK is that UUID. Check committed purchase FIRST on retries, then current state against preview.
Keep an unacknowledged intent/receipt recoverable across cold start; close only on user cancellation/ack.
Stale intent refreshes explicitly, requiring another confirmation; no debit on stale attempt.
Rationale: SavedStateHandle alone may not cover uncertain cold-launch commit; one tiny intent table does.
Alternatives: per-reward deduplication forbids legitimate repeats; UI-only busy flag cannot prevent retries.

## Limits and retention
Decision: count immutable purchases by reward and stored LocalDate using TaskClock.today(); weeks Monday,
months calendar; soft-delete definitions. Preset icons, move-up/down ordering; positive Long prices checked
at input/commit with exact arithmetic. No product maximum invented. Revision detects definition edits.
Rationale: avoids mutable quota drift and retains original snapshots. Zone/date changes do not rewrite history.

## Environment
Existing pinned versions/build setup retained. Java/SDK/Pixel_5 AVD present. Plain adb has sandbox home
permission failure; use scoped Android home or request execution escalation only if necessary.
No new API/library selection; repository code is primary evidence. No external web research required.
No unresolved technical clarification.
