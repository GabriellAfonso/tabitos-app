# Data Model: Game Mode Toggle

## Game mode preference (DataStore)

| Key | Type | Default | Notes |
|---|---|---|---|
| `game_mode` | Boolean | `true` | Stored in the existing `appearance` preferences file; theme keys untouched. |

## Entitlement (Room `accounting_entitlements`, unchanged schema 7)

Existing fields reused: `active`, `exempt`, `priced`, `lastEntry`.

State transitions added by `scoring = false` (OFF):

| Before | Event | After | Ledger |
|---|---|---|---|
| none / inactive | outcome active, OFF | active, exempt | none |
| active, paid | outcome removed, OFF or ON | inactive | REVERSAL |
| active, exempt | outcome removed, OFF or ON | inactive, exempt = false | none |
| inactive, not exempt | outcome active, ON | active, paid | GAIN/PENALTY/REAPPLY |
| inactive, not exempt | outcome active, OFF | active, exempt | none |

Invariant: switching the mode never changes stored entitlements or entries; only later writes do.
