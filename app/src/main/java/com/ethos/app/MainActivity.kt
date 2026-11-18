package com.ethos.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ethos.app.core.timers.platform.TimerCoordinator
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject

/** Only receives Intents and hosts [EthosApp]; the app itself is composed in `EthosApp.kt`. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var timerCoordinator: TimerCoordinator
    private val requests = AppRequests()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) accept(intent)
        else requests.reminder = savedInstanceState.getString(REMINDER_URI)?.let {
            it to savedInstanceState.getString(REMINDER_TOKEN).orEmpty()
        }
        enableEdgeToEdge()
        setContent {
            EthosApp(requests) { dark ->
                val bars = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        accept(intent)
    }

    override fun onStart() {
        super.onStart()
        // Foreground is the reliable moment to (re)attach the session service after kills or refused starts.
        timerCoordinator.request()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        requests.reminder?.let { (uri, token) ->
            outState.putString(REMINDER_URI, uri)
            outState.putString(REMINDER_TOKEN, token)
        }
        super.onSaveInstanceState(outState)
    }

    private fun accept(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.authority != LOCAL_AUTHORITY) return
        when (data.scheme) {
            REMINDER_SCHEME -> requests.reminder = data.toString() to UUID.randomUUID().toString()
            TIMER_SCHEME -> if (data.pathSegments.firstOrNull() == TIMER_OPEN) {
                val sessionId = data.pathSegments.getOrNull(1)
                // "Abrir" on a ringing alert is a deliberate look at the timer, so it silences the alarm (spec 026).
                sessionId?.let(timerCoordinator::stopAlarm)
                requests.timerSession = sessionId
            }
        }
    }

    private companion object {
        const val REMINDER_SCHEME = "ethos-reminder"
        const val TIMER_SCHEME = "ethos-timer"
        const val LOCAL_AUTHORITY = "local"
        const val TIMER_OPEN = "OPEN"
        const val REMINDER_URI = "reminderUri"
        const val REMINDER_TOKEN = "reminderToken"
    }
}
