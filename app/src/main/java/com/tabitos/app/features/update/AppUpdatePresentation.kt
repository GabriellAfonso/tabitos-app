package com.tabitos.app.features.update

import com.tabitos.app.core.update.domain.AppUpdatePhase

/** What the "Atualização pronta" strip offers (spec 039). */
enum class UpdateNotice {
    /** Reiniciar and Agora não. */
    READY,
    /** Installing restarts the app and would pause the counting timer, so only Agora não is offered. */
    WAITING_TIMER,
}

fun updateNotice(phase: AppUpdatePhase, timerRunning: Boolean): UpdateNotice? = when {
    phase != AppUpdatePhase.READY -> null
    timerRunning -> UpdateNotice.WAITING_TIMER
    else -> UpdateNotice.READY
}
