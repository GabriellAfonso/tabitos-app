package com.ethos.app.features.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.ethos.app.R
import com.ethos.app.core.icons.AppIcon
import com.ethos.app.core.rewards.domain.*
import com.ethos.app.core.ui.components.CoinAmount
import com.ethos.app.core.ui.components.CoinGlyph
import com.ethos.app.core.ui.components.Eyebrow
import com.ethos.app.core.ui.components.IconPicker
import com.ethos.app.core.ui.components.LazyReorderState
import com.ethos.app.core.ui.components.lazyReorderGestures
import com.ethos.app.core.ui.components.moved
import com.ethos.app.core.ui.components.reorderActions
import com.ethos.app.core.ui.components.reorderableItem
import com.ethos.app.core.ui.rewards.rewardAppIcon
import com.ethos.app.core.ui.rewards.rewardIcon
import kotlinx.coroutines.delay

data class RewardActions(
    val edit: (Reward?) -> Unit = {}, val draft: (List<String>?) -> Unit = {}, val save: () -> Unit = {},
    val deletion: (String?) -> Unit = {}, val delete: () -> Unit = {}, val order: (List<String>) -> Unit = {},
    val preview: (String) -> Unit = {}, val confirm: () -> Unit = {}, val close: () -> Unit = {},
    val retry: () -> Unit = {}, val confirmPause: () -> Unit = {}, val cancelPause: () -> Unit = {},
)

/** Icons offered in the reward editor's row; the rest are under "see more". */
private val rewardPrincipalIcons = listOf(AppIcon.GIFT, AppIcon.GAME, AppIcon.FOOD, AppIcon.REST)

@Composable
fun RewardsScreen(viewModel: RewardsViewModel, sessions: @Composable () -> Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { viewModel.refresh(); delay(30_000) }
        }
    }
    RewardsContent(state, RewardActions(viewModel::edit, viewModel::draft, viewModel::save,
        viewModel::deletion, viewModel::delete, viewModel::order, viewModel::preview,
        viewModel::confirm, viewModel::close, viewModel::retry, { viewModel.confirm(true) },
        viewModel::cancelPause), sessions)
}

@Composable
private fun RewardError(state: RewardsUiState, actions: RewardActions) {
    if (state.error) {
        Text(stringResource(R.string.reward_error), color = MaterialTheme.colorScheme.error)
        TextButton(actions.retry, enabled = !state.busy) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
fun RewardsContent(state: RewardsUiState, actions: RewardActions, sessions: @Composable () -> Unit = {}) {
    val listState = rememberLazyListState()
    val ids = state.cards.map { it.reward.id }
    val drag = remember(ids) { LazyReorderState(listState, ids) }
    val dragEnabled = !state.busy && !state.error && !state.loading
    val haptic = LocalHapticFeedback.current
    val edge = with(LocalDensity.current) { 64.dp.toPx() }
    LaunchedEffect(drag, drag.draggedId, dragEnabled) {
        if (dragEnabled) drag.autoScroll(edge) else if (drag.draggedId != null) drag.cancel()
    }
    val cards = drag.order.mapNotNull { id -> state.cards.firstOrNull { it.reward.id == id } }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)
        .lazyReorderGestures(drag, dragEnabled, { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
            actions.order), state = listState,
        contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            if (state.loading) Text(stringResource(R.string.finance_loading))
            RewardError(state, actions)
            if (!state.loading && !state.error) {
                Eyebrow(stringResource(R.string.wallet_coins))
                CoinAmount(state.snapshot.wallet.balance.toString(),
                    style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.reward_immediate))
                Button({ actions.edit(null) }, enabled = !state.busy) {
                    Text(stringResource(R.string.reward_create))
                }
            }
        }
        item { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { sessions() } }
        if (!state.loading && !state.error && state.cards.isEmpty()) item {
            Text(stringResource(R.string.reward_empty))
        }
        if (!state.loading) itemsIndexed(cards, key = { _, card -> card.reward.id }) { index, card ->
            val reward = card.reward
            OutlinedCard(Modifier.reorderableItem(this, drag, reward.id).fillMaxWidth()
                .reorderActions(index, ids.lastIndex, dragEnabled) { actions.order(ids.moved(index, it)) },
                shape = RoundedCornerShape(17.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(rewardIcon(reward.icon)), contentDescription = null,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(14.dp))
                                .padding(10.dp))
                        Text(reward.name, style = MaterialTheme.typography.titleMedium)
                    }
                    if (reward.description.isNotEmpty()) Text(reward.description)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        CoinGlyph()
                        Text(stringResource(R.string.reward_price, reward.cost),
                            color = MaterialTheme.colorScheme.tertiary)
                    }
                    reward.minutes?.let { Text(stringResource(R.string.reward_minutes_line, durationLabel(it))) }
                    reward.limit?.let { limit ->
                        Text(stringResource(R.string.reward_limit_status, card.used, limit,
                            stringResource(periodLabel(reward.period))))
                    }
                    if (!card.available) Text(stringResource(
                        if (reward.limit != null && card.used >= reward.limit) R.string.reward_exhausted
                        else R.string.reward_insufficient))
                    Button({ actions.preview(reward.id) }, enabled = card.available && !state.busy && !state.error) {
                        Text(stringResource(R.string.reward_buy))
                    }
                    // Vertical actions remain reachable at large fonts and on narrow screens.
                    TextButton({ actions.edit(reward) }, enabled = !state.busy && !state.error) {
                        Text(stringResource(R.string.reward_edit, reward.name))
                    }
                    TextButton({ actions.deletion(reward.id) }, enabled = !state.busy && !state.error) {
                        Text(stringResource(R.string.reward_delete, reward.name))
                    }
                }
            }
        }
    }
    state.draft?.let { RewardEditor(state, it, actions) }
    state.deleteId?.let { id ->
        AlertDialog(onDismissRequest = { if (!state.busy) actions.deletion(null) },
            title = { Text(stringResource(R.string.reward_delete_title)) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.reward_delete_body,
                    state.snapshot.rewards.firstOrNull { it.id == id }?.name.orEmpty()))
                RewardError(state, actions)
            } },
            confirmButton = { TextButton(actions.delete, enabled = !state.busy && !state.loading) {
                Text(stringResource(R.string.delete))
            } }, dismissButton = { TextButton({ actions.deletion(null) }, enabled = !state.busy) {
                Text(stringResource(R.string.cancel))
            } })
    }
    state.snapshot.intent?.let { intent -> PurchaseDialog(state, intent, actions) }
    state.pauseRequired?.let { title ->
        AlertDialog(onDismissRequest = { if (!state.busy) actions.cancelPause() },
            title = { Text(stringResource(R.string.reward_pause_title)) },
            text = { Text(stringResource(R.string.reward_pause_body, title)) },
            confirmButton = { TextButton(actions.confirmPause, enabled = !state.busy) {
                Text(stringResource(R.string.reward_pause_confirm)) } },
            dismissButton = { TextButton(actions.cancelPause, enabled = !state.busy) {
                Text(stringResource(R.string.cancel)) } })
    }
}

private fun durationLabel(minutes: Int) =
    if (minutes >= 60) "${minutes / 60} h ${"%02d".format(minutes % 60)} min" else "$minutes min"

private fun periodLabel(period: String) = when (period) {
    "WEEK" -> R.string.reward_week
    "MONTH" -> R.string.reward_month
    else -> R.string.reward_day
}

@Composable
private fun RewardEditor(state: RewardsUiState, draft: List<String>, actions: RewardActions) {
    fun field(index: Int, value: String) { actions.draft(draft.toMutableList().also { it[index] = value }) }
    AlertDialog(onDismissRequest = { if (!state.busy) actions.draft(null) },
        title = { Text(stringResource(R.string.reward_editor)) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to R.string.reward_name, 2 to R.string.reward_description,
                    4 to R.string.reward_cost, 5 to R.string.reward_limit, 7 to R.string.reward_minutes,
                ).forEach { (index, label) ->
                    OutlinedTextField(draft[index], { field(index, it) }, enabled = !state.busy,
                        label = { Text(stringResource(label)) }, modifier = Modifier.fillMaxWidth(),
                        singleLine = index != 2)
                }
                IconPicker(R.string.reward_icon, rewardPrincipalIcons, rewardAppIcon(draft[3]), !state.busy) {
                    field(3, it.name) }
                Text(stringResource(R.string.reward_period))
                RewardRules.periods.forEach { period ->
                    FilterChip(selected = draft[6] == period, onClick = { field(6, period) },
                        enabled = !state.busy, label = { Text(stringResource(periodLabel(period))) })
                }
                if (state.invalid) Text(stringResource(R.string.reward_invalid),
                    color = MaterialTheme.colorScheme.error)
                RewardError(state, actions)
            }
        }, confirmButton = { TextButton(actions.save, enabled = !state.busy && !state.loading) {
            Text(stringResource(R.string.save))
        } }, dismissButton = { TextButton({ actions.draft(null) }, enabled = !state.busy) {
            Text(stringResource(R.string.cancel))
        } })
}

@Composable
private fun PurchaseDialog(state: RewardsUiState, intent: PurchaseIntent, actions: RewardActions) {
    val receipt = state.snapshot.purchases.firstOrNull { it.id == intent.id }
    val preview = purchasePresentation(intent)
    AlertDialog(onDismissRequest = { if (!state.busy) actions.close() },
        title = { Text(stringResource(if (receipt != null) R.string.reward_purchased else R.string.reward_confirm)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val reward = receipt?.reward ?: intent.reward
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(painterResource(rewardIcon(reward.icon)), contentDescription = null)
                    Text(reward.name)
                }
                if (reward.description.isNotEmpty()) Text(reward.description)
                Text(stringResource(R.string.reward_price, reward.cost))
                reward.minutes?.let { Text(stringResource(R.string.reward_minutes_line, durationLabel(it))) }
                if (receipt != null) {
                    Text(stringResource(R.string.reward_used))
                    Text(stringResource(R.string.reward_reference, receipt.id))
                } else {
                    preview.balanceAfter?.let { Text(stringResource(R.string.reward_after, it)) }
                    preview.allowanceAfter?.let { Text(stringResource(R.string.reward_allowance_after, it)) }
                    Text(stringResource(if (reward.minutes != null) R.string.reward_timed_hint
                        else R.string.reward_immediate))
                    if (state.stale) Text(stringResource(R.string.reward_stale))
                    if (state.unavailable || !preview.available) Text(stringResource(
                        if (reward.deleted) R.string.reward_removed
                        else if (preview.exhausted) R.string.reward_exhausted else R.string.reward_insufficient))
                }
                RewardError(state, actions)
            }
        }, confirmButton = {
            if (receipt != null) TextButton(actions.close, enabled = !state.busy) {
                Text(stringResource(R.string.reward_done))
            } else TextButton(actions.confirm,
                enabled = preview.available && !state.busy && !state.error && !state.loading) {
                Text(stringResource(R.string.reward_confirm_action))
            }
        }, dismissButton = {
            if (receipt == null) TextButton(actions.close, enabled = !state.busy) {
                Text(stringResource(R.string.cancel))
            }
        })
}

@Preview
@Composable
private fun RewardsEmptyPreview() { MaterialTheme { RewardsContent(RewardsUiState(loading = false), RewardActions()) } }
