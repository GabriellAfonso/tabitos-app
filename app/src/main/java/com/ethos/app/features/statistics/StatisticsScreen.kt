package com.ethos.app.features.statistics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.ZoneId

/** Connects the statistics ViewModel to [StatisticsContent]; [today] and [zone] come from the injected clocks. */
@Composable
fun StatisticsScreen(
    today: LocalDate, zone: ZoneId, navigation: StatisticsNavigation,
    viewModel: StatisticsViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Filter, item and tab only live while the page is open (spec 024).
    DisposableEffect(viewModel) { onDispose { viewModel.leave() } }
    StatisticsContent(state, today, zone, StatisticsActions(
        selectKind = viewModel::selectKind,
        previous = viewModel::previous,
        next = viewModel::next,
        selectFilter = viewModel::selectFilter,
        selectItem = viewModel::selectItem,
        selectTab = viewModel::selectTab,
        showMore = viewModel::showMore,
        retry = viewModel::retry,
        openDeleted = viewModel::openDeleted,
        askEraseConfirmation = viewModel::askEraseConfirmation,
        confirmErase = viewModel::confirmErase,
        viewDeletedHistory = viewModel::viewDeletedHistory,
        navigation = navigation,
    ))
}
