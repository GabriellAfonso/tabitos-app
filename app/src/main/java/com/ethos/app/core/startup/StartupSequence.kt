package com.ethos.app.core.startup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Data that Hoje does not draw, loaded after Hoje on a cold open (spec 027). */
enum class DeferredSource { CALENDAR, REWARDS, REMINDERS }

/** The order in which deferred sources start once Hoje is shown, one at a time. */
val PRELOAD_ORDER: List<DeferredSource> = listOf(DeferredSource.CALENDAR, DeferredSource.REWARDS,
    DeferredSource.REMINDERS)

/**
 * Orders the database work of a cold open: Hoje's sources first, then each deferred source in [PRELOAD_ORDER]
 * after the previous one finished its first load. Asking for a source (its page opened, a notification) starts it
 * at once, so nothing the user is waiting for stays in the queue.
 */
@Singleton
class StartupSequence private constructor(todayShown: Boolean, requested: Set<DeferredSource>) {
    @Inject constructor() : this(false, emptySet())

    private val todayShown = MutableStateFlow(todayShown)
    private val requested = MutableStateFlow(requested)
    private val loaded = MutableStateFlow(emptySet<DeferredSource>())

    fun markTodayShown() { todayShown.value = true }
    fun request(sources: Set<DeferredSource>) { if (sources.isNotEmpty()) requested.update { it + sources } }
    /** Called after a source's first load ended, with content or with an error, so the next one can start. */
    fun markLoaded(source: DeferredSource) { loaded.update { it + source } }

    suspend fun awaitTodayShown() { todayShown.first { it } }

    suspend fun awaitTurn(source: DeferredSource) {
        combine(todayShown, requested, loaded) { shown, asked, done ->
            source in asked || shown && PRELOAD_ORDER.takeWhile { it != source }.all { it in done }
        }.first { it }
    }

    companion object {
        /** Nothing waits: for tests and previews that build ViewModels directly. */
        fun released() = StartupSequence(todayShown = true, requested = DeferredSource.entries.toSet())
    }
}
