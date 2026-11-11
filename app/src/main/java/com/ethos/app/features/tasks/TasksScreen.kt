package com.ethos.app.features.tasks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

@Composable
fun TasksScreen(viewModel: TasksViewModel, content: @Composable (TasksUiState) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TasksRefreshEffect(viewModel)
    content(state)
    TaskDialogs(state, viewModel)
}

/** Keeps "today" and overdue state current while the app is visible. */
@Composable
fun TasksRefreshEffect(viewModel: TasksViewModel) {
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { viewModel.refresh(); delay(60_000) }
        }
    }
}
