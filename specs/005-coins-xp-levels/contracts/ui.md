# UI and repository contracts
AccountingRepository observes activation, committed prices, totals and immutable ledger; savePrices validates
all nine integer values and commits atomically. Source repositories own source+ledger atomicity.
AccountingViewModel exposes immutable loading/content/error/saving state, retained draft and retry.

Hoje retains coin/level cards, real values, total XP and within-level progress. Coin route stays Recompensas;
that page remains unavailable for spending. Drawer history displays real gains/penalties/reversals or empty.
Settings retains appearance; accounting section explains independent difficulty/importance, floor and linear
curve; nine editable values are labelled by category and unit, valid0..10000, no reset/manual currency grant.
Task/habit editors show current estimates and expandable explanation/table. Detail/review actions disclose
financial outcomes before mutation, including corrected historical prices and effective floor clipping.
UI uses state passed via shared core context; no Composable accesses repositories or computes business effects.
Screen lifecycle collects StateFlow; loading/read failure never masquerades as a zero wallet.
