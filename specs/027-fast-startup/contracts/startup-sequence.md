# Contract: StartupSequence (spec 027)

Internal contract between the composition root and the ViewModels; no external interface.

```kotlin
enum class DeferredSource { CALENDAR, REWARDS, REMINDERS }

class StartupSequence {
    fun markTodayShown()
    fun request(sources: Set<DeferredSource>)
    fun markLoaded(source: DeferredSource)
    suspend fun awaitTodayShown()
    suspend fun awaitTurn(source: DeferredSource)
    companion object { fun released(): StartupSequence }
}
```

Guarantees:
1. `awaitTurn` for a requested source never waits for Hoje or for other sources.
2. Without requests, deferred sources start one at a time after Hoje, in `PRELOAD_ORDER`; a source whose first load
   failed still releases the next one.
3. All calls are idempotent and safe from any thread.

Callers:
- `EthosApp`: `markTodayShown()` when `todayReady`; `request(destination sources)` when the shell shows a
  destination; `request(all)` while an `AppRequests` notification request is pending.
- Calendar/Rewards/Reminders ViewModels: `awaitTurn` before the first observe, `markLoaded` after the first emission
  or failure, `request` from entry points that need their data.
- Tasks/Habits ViewModels: `awaitTodayShown` before the open-time refresh/settle writes.
