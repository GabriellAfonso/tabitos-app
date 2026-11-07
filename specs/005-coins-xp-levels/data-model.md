# Data model

- Accounting activation: singleton initialized flag/time; initialization and baseline capture atomic.
- Price: category index0..2, coinGain, xpGain, penalty each0..10000; all rows updated atomically.
- Entitlement: stable key, family, source identity/title/date, first nominal coins/XP, active flag,
  baseline-exempt flag and latest linked entry. Historical prices remain immutable after first pricing.
- Ledger entry: monotonic sequence, entitlement key, original/latest link, operation kind, source snapshot,
  actual date, entry timestamp, nominal/effective coins, XP delta and resulting balance/totalXP.
  Entries append only, never cascade with source deletion. Balance/XP derive from last entry, initially0.

Transitions: absent→baseline active/unpriced at activation or priced active/inactive for new source;
active→inactive appends reversal unless exempt; inactive→active appends first/reapplication.
For clipped award reapplication coin amount equals negated latest reversal; XP equals original XP.
For penalty reapplication nominal original price is clipped anew. Every debit respects−300.
All checked arithmetic failures roll back source and ledger together.

Projection: tasks use occurrence+DONE/NOT_DONE; practice uses habit+actual-day; avoid/limit uses record ID;
closing practice failure/negative success uses period key+outcome. Record ordering uses actual/entered/id.
Reconciliation considers affected source only, preserves removed/excluded provenance and ignores refresh.
Activation suppression is identity based; removing old outcome releases suppression for a new resolution.
Moving a uniquely rewarded practice record to a different qualifying date transfers its committed price
and linked effective reversal into the destination day entitlement. Moving an unpriced baseline record
retains baseline exemption. Splitting duplicate records into a genuinely new distinct day is a new earning.
