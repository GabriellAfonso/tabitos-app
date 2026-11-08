# Contracts: timers

## Domain (`core/timers/domain`, no Android)
```kotlin
interface TimerClock { fun wallMillis(): Long; fun elapsedMillis(): Long; fun bootCount(): Int?; fun zone(): ZoneId }
enum class StartResult { STARTED, CONFLICT, UNAVAILABLE }
interface TimerRepository {
    fun observe(): Flow<TimerSnapshot>
    suspend fun start(kind: TimerKind, sourceId: String, title: String, pauseOther: Boolean): StartResult
    suspend fun pause(sessionId: String, kind: TimerAuditKind = PAUSE)
    suspend fun finish(sessionId: String)
    suspend fun sync(): TimerSync          // recovery + cap; returns running session and next cap delay
    suspend fun checkpoint()
    suspend fun saveGoal(sourceKey: String, minutes: Int?)
    suspend fun addManual(kind: TimerKind, sourceId: String, title: String, start: Long, end: Long)
    suspend fun correct(segmentId: String, start: Long, end: Long)
    suspend fun delete(segmentId: String)
    suspend fun confirmed(sessionId: String)
}
```
`TimerRules`: duration, recovery, allocation by day/week, progress, validation (`TimerProblem`).

## Rewards
- `Reward.minutes: Int?` (1..1440). `RewardRepository.confirm(expected, pauseRunning = false)` adds
  `PurchaseResult.RUNNING_CONFLICT` (nothing charged).

## Platform
- `TimerCoordinator.sync()` after any mutation: starts/stops `TimerService`, posts alerts once per REACHED/EXHAUSTED audit.
- `TimerService` (FGS specialUse, not exported), `TimerActionReceiver` (not exported, action PAUSE + session id).
- Activity URI `ethos-timer://local/OPEN/<sessionId>`.

## UI
- `LocalOpenTimer: (TimerKind, sourceId, title) -> Unit` opens the timer sheet from task/habit details.
- Timer dialog: elapsed/remaining, mode toggle, Iniciar/Pausar/Retomar/Encerrar, meta, progresso do período,
  adicionar tempo, corrigir/excluir trechos, histórico. Hoje shows active/paused session cards.
- Rewards: duration field in editor, "Comprar e iniciar", pause-confirmation dialog, purchased-time sessions list.
