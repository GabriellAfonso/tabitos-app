package com.ethos.app.features.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethos.app.core.preferences.AppearanceMode

@Composable
fun AppearanceScreen(
    viewModel: AppearanceViewModel,
    content: @Composable (AppearanceUiState, (AppearanceMode) -> Unit, () -> Unit) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    content(state, viewModel::select, viewModel::retry)
}
