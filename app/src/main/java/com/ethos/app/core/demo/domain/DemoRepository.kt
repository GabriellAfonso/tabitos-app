package com.ethos.app.core.demo.domain

interface DemoRepository {
    /** Replaces local content atomically. Call only after explicit destructive confirmation. */
    suspend fun restore()
}

class DemoTimerRunningException : IllegalStateException()
