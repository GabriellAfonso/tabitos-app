package com.tabitos.app.features.accounting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AccountingScreen(viewModel: AccountingViewModel, content: @Composable (AccountingUiState) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    content(state)
}
