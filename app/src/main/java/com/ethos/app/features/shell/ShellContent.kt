package com.ethos.app.features.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FilterRow(filter: ContentFilter, onFilter: (ContentFilter) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ContentFilter.entries.forEach { value ->
            FilterChip(
                selected = filter == value,
                onClick = { onFilter(value) },
                label = { Text(stringResource(value.title)) },
                modifier = Modifier.sizeIn(minHeight = 48.dp).testTag("filter_${value.name}"),
            )
        }
    }
}

@Composable
fun BalanceTiles(presentation: ShellPresentation, onRewards: () -> Unit) {
    BoxWithConstraints {
        if (maxWidth >= 300.dp && LocalDensity.current.fontScale < 1.5f) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BalanceTile(presentation, onRewards, Modifier.weight(1f))
                LevelTile(presentation, Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BalanceTile(presentation, onRewards, Modifier.fillMaxWidth())
                LevelTile(presentation, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BalanceTile(presentation: ShellPresentation, onRewards: () -> Unit, modifier: Modifier) {
    val label = stringResource(R.string.open_rewards)
    Card(
        onClick = onRewards,
        modifier = modifier.semantics { contentDescription = label },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.balance), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(presentation.balance), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun LevelTile(presentation: ShellPresentation, modifier: Modifier) {
    OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.level), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(presentation.level), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun TodayContent(
    presentation: ShellPresentation,
    filter: ContentFilter,
    onFilter: (ContentFilter) -> Unit,
    onRewards: () -> Unit,
    onReview: () -> Unit,
) {
    Text(
        DateFormat.getDateInstance(DateFormat.FULL, Locale.forLanguageTag("pt-BR")).format(Date()),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(stringResource(R.string.one_step), style = MaterialTheme.typography.headlineSmall)
    BalanceTiles(presentation, onRewards)
    if (presentation.pendingItems.isNotEmpty()) {
        Card(onClick = onReview, modifier = Modifier.fillMaxWidth()) {
            Text(
                pluralStringResource(
                    R.plurals.pending_count, presentation.pendingItems.size, presentation.pendingItems.size,
                ),
                Modifier.padding(16.dp),
            )
        }
    }
    FilterRow(filter, onFilter)
    val items = presentation.todayItems.matching(filter)
    if (items.isEmpty()) {
        when (filter) {
            ContentFilter.ALL -> EmptyContent(R.string.today_empty, R.string.today_empty_detail)
            ContentFilter.TASKS -> EmptyContent(R.string.tasks_empty, R.string.tasks_empty_detail)
            ContentFilter.HABITS -> EmptyContent(R.string.habits_empty, R.string.habits_empty_detail)
        }
    } else {
        items.forEach { ItemCard(it) }
    }
    Text(stringResource(R.string.future_add), style = MaterialTheme.typography.labelLarge)
}

@Composable
fun ReviewContent(items: List<ShellItem>, filter: ContentFilter, onFilter: (ContentFilter) -> Unit) {
    Text(stringResource(R.string.review_hint), style = MaterialTheme.typography.bodyMedium)
    FilterRow(filter, onFilter)
    val matching = items.matching(filter)
    if (matching.isEmpty()) {
        val detail = when (filter) {
            ContentFilter.ALL -> R.string.review_empty_detail
            ContentFilter.TASKS -> R.string.review_tasks_empty
            ContentFilter.HABITS -> R.string.review_habits_empty
        }
        EmptyContent(R.string.review_empty, detail)
    } else {
        matching.forEach { item ->
            ItemCard(item)
            Text(stringResource(R.string.future_review), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun EmptyContent(title: Int, detail: Int) {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ItemCard(item: ShellItem) {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(item.type.title), style = MaterialTheme.typography.labelSmall)
            Text(stringResource(item.title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(item.detail), style = MaterialTheme.typography.bodySmall)
        }
    }
}
