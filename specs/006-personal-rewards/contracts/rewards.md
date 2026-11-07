# Rewards contract
Repository exposes observable snapshot of active rewards, purchases, wallet and recoverable open intent.
Mutations: save definition, move adjacent, soft delete, preview/open intent, confirm intent, close intent.
All writes are transactional; persistence errors throw to ViewModel (retain draft/intent, offer safe retry).
Confirm returns purchased receipt, stale-refreshed preview, or unavailable reason. Existing receipt is returned
before checking changed/deleted reward. Each deliberate purchase starts a new id; retry uses same id.
Confirm receives the exact displayed PurchaseIntent; a stale rendered preview cannot confirm a refreshed persisted price. No refund API. Failed transaction leaves ledger, purchase and quota unchanged.

UI: existing Recompensas route opens live store. Editor supports preset icon/name/description/cost/optional
limit+unit; errors retain input. Cards show price/status and edit/move/delete actions. Confirmation shows
snapshot details, full price, balance AFTER and allowance AFTER if limited. Cancelling closes only pending
intent; committed receipt dismiss acknowledges result, never refunds. Stale status requires another tap.
Busy actions disabled. Portuguese resources, scrollable editor/dialogs, accessible labeled ordering,
current theme/navigation/insets. History labels PURCHASE and displays immutable original details/reference.
