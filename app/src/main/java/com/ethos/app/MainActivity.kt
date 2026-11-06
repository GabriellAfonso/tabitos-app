package com.ethos.app

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ethos.app.core.preferences.AppearanceMode
import com.ethos.app.core.ui.theme.EthosTheme
import com.ethos.app.features.settings.AppearanceScreen
import com.ethos.app.features.settings.AppearanceViewModel
import com.ethos.app.features.settings.SettingsContent
import com.ethos.app.features.shell.EthosShell
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appearanceViewModel: AppearanceViewModel = viewModel()
            AppearanceScreen(appearanceViewModel) { state, onSelect, onRetry ->
                val dark = when (state.mode) {
                    AppearanceMode.SYSTEM -> isSystemInDarkTheme()
                    AppearanceMode.LIGHT -> false
                    AppearanceMode.DARK -> true
                }
                SideEffect {
                    val bars = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                    enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                }
                EthosTheme(darkTheme = dark) {
                    EthosShell(settingsContent = { SettingsContent(state, onSelect, onRetry) })
                }
            }
        }
    }
}
