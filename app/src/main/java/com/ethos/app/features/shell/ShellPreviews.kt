package com.ethos.app.features.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import com.ethos.app.core.ui.theme.EthosTheme

// Samples are referenced only by these preview entry points, never by the application root.
private val sampleItems = listOf(
    ShellItem("walk", ContentFilter.TASKS, R.string.sample_task, R.string.sample_task_meta),
    ShellItem("read", ContentFilter.HABITS, R.string.sample_habit, R.string.sample_habit_meta),
)
private val samplePending = listOf(
    ShellItem("project", ContentFilter.TASKS, R.string.sample_pending, R.string.sample_pending_meta),
    ShellItem("read", ContentFilter.HABITS, R.string.sample_habit, R.string.sample_pending_meta),
)
private val samplePresentation = ShellPresentation(
    todayItems = sampleItems,
    pendingItems = samplePending,
    balance = R.string.sample_balance,
    level = R.string.sample_level,
)

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun TodayPopulatedPreview() {
    EthosTheme { EthosShell(samplePresentation, settingsContent = {}) }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820, fontScale = 2f)
@Composable
private fun TodayEmptyLargePreview() {
    EthosTheme { EthosShell(settingsContent = {}) }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun TodayDarkPreview() {
    EthosTheme(darkTheme = true) { EthosShell(samplePresentation, settingsContent = {}) }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun ReviewPopulatedPreview() {
    EthosTheme {
        ShellPage {
            var filter by rememberSaveable { mutableStateOf(ContentFilter.ALL) }
            Text(stringResource(R.string.development_sample))
            ReviewContent(samplePending, filter) { filter = it }
        }
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun ReviewEmptyPreview() {
    EthosTheme { ShellPage { ReviewContent(emptyList(), ContentFilter.ALL, {}) } }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun RewardsPreview() {
    EthosTheme {
        ShellPage {
            Text(stringResource(R.string.development_sample))
            BalanceTiles(samplePresentation, {})
            OutlinedCard {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.sample_reward))
                    Text(stringResource(R.string.sample_reward_detail))
                    Text(stringResource(R.string.sample_cost))
                    Text(stringResource(R.string.future_redeem))
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun HistoryPreview() {
    EthosTheme {
        ShellPage {
            Text(stringResource(R.string.development_sample))
            OutlinedCard {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.sample_history))
                    Text(stringResource(R.string.sample_history_value))
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun ListTabsPreview() {
    EthosTheme {
        ShellPage {
            Text(stringResource(R.string.development_sample))
            Text(stringResource(R.string.sample_list))
            var filter by rememberSaveable { mutableStateOf(ContentFilter.TASKS) }
            // List detail has only tasks/habits, unlike Hoje/review which also offer Tudo.
            androidx.compose.foundation.layout.Row {
                listOf(ContentFilter.TASKS, ContentFilter.HABITS).forEach { tab ->
                    androidx.compose.material3.FilterChip(
                        selected = filter == tab,
                        onClick = { filter = tab },
                        label = { Text(stringResource(tab.title)) },
                    )
                }
            }
            sampleItems.matching(filter).forEach { ItemCard(it) }
        }
    }
}
