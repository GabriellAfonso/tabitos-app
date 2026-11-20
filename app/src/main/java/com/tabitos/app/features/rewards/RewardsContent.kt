package com.tabitos.app.features.rewards

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.tabitos.app.R
import com.tabitos.app.core.rewards.domain.Purchase
import com.tabitos.app.core.rewards.domain.Reward
import com.tabitos.app.core.ui.components.CoinAmount
import com.tabitos.app.core.ui.components.CoinGlyph
import com.tabitos.app.core.ui.components.Eyebrow
import com.tabitos.app.core.ui.components.LazyReorderState
import com.tabitos.app.core.ui.components.lazyReorderGestures
import com.tabitos.app.core.ui.components.moved
import com.tabitos.app.core.ui.components.reorderActions
import com.tabitos.app.core.ui.components.reorderableItem
import com.tabitos.app.core.ui.rewards.rewardIcon
import com.tabitos.app.core.ui.theme.TabitosTheme
import java.time.LocalDate
import kotlinx.coroutines.delay

data class RewardActions(
    val edit: (Reward?) -> Unit = {}, val draft: (RewardDraft?) -> Unit = {}, val save: () -> Unit = {},
    val deletion: (String?) -> Unit = {}, val delete: () -> Unit = {}, val order: (List<String>) -> Unit = {},
    val preview: (String) -> Unit = {}, val confirm: () -> Unit = {}, val close: () -> Unit = {},
    val retry: () -> Unit = {}, val confirmPause: () -> Unit = {}, val cancelPause: () -> Unit = {},
    /** Estatísticas › Histórico filtered by coins. */
    val openHistory: () -> Unit = {},
)

@Composable
fun RewardsScreen(viewModel: RewardsViewModel, openHistory: () -> Unit, sessions: @Composable () -> Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { viewModel.refresh(); delay(REFRESH_MILLIS) }
        }
    }
    RewardsContent(state, RewardActions(edit = viewModel::edit, draft = viewModel::draft, save = viewModel::save,
        deletion = viewModel::deletion, delete = viewModel::delete, order = viewModel::order,
        preview = viewModel::preview, confirm = viewModel::confirm, close = viewModel::close,
        retry = viewModel::retry, confirmPause = { viewModel.confirm(true) }, cancelPause = viewModel::cancelPause,
        openHistory = openHistory), sessions)
}

@Composable
internal fun RewardError(state: RewardsUiState, actions: RewardActions) {
    if (state.error) {
        Text(stringResource(R.string.reward_error), color = MaterialTheme.colorScheme.error)
        TextButton(actions.retry, enabled = !state.busy) { Text(stringResource(R.string.retry)) }
    }
}

/**
 * Reward store as a compact list (option A of prototypes/recompensas): wallet card, one row per reward with the
 * price on its buy button or the reason it cannot be bought, Editar/Excluir under ⋮ and a floating create button.
 */
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
    val ready = !state.loading && !state.error
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)
            .lazyReorderGestures(drag, dragEnabled, { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                actions.order), state = listState,
            contentPadding = PaddingValues(top = 12.dp, bottom = FAB_CLEARANCE),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                if (state.loading) Text(stringResource(R.string.finance_loading))
                RewardError(state, actions)
                if (ready) WalletCard(state.snapshot.wallet.balance, actions.openHistory)
            }
            item { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { sessions() } }
            if (ready && state.cards.isEmpty()) item { EmptyStore() }
            if (!state.loading) itemsIndexed(cards, key = { _, card -> card.reward.id }) { index, card ->
                val strip = state.snapshot.stripIntent(card.reward.id)
                    ?.let { purchaseStrip(it, state.runningTimer, state.unavailable) }
                RewardRow(card, enabled = ready && !state.busy, actions,
                    strip?.let { { PurchaseStripContent(it, state, actions) } },
                    Modifier.reorderableItem(this, drag, card.reward.id)
                        .reorderActions(index, ids.lastIndex, dragEnabled) { actions.order(ids.moved(index, it)) })
            }
        }
        if (ready) ExtendedFloatingActionButton(onClick = { actions.edit(null) },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp),
            icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
            text = { Text(stringResource(R.string.reward_create)) })
        state.snapshot.receipt()?.let { purchase ->
            PurchaseReceipt(purchase, state.snapshot.wallet.balance, actions.close,
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = FAB_CLEARANCE))
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

@Composable
private fun WalletCard(balance: Long, openHistory: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer)) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow(stringResource(R.string.wallet_coins))
                CoinAmount(balance.toString(), style = MaterialTheme.typography.headlineSmall)
            }
            TextButton(openHistory) {
                Text(stringResource(R.string.reward_history), color = MaterialTheme.colorScheme.tertiary)
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun EmptyStore() {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(64.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_storefront), contentDescription = null, Modifier.size(34.dp),
                tint = MaterialTheme.colorScheme.primary)
        }
        Text(stringResource(R.string.reward_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Tapping the row edits the reward; the buy button carries the price and ⋮ holds Editar and Excluir.
 * While a purchase is open for this reward the price gives way to the confirmation [strip] below the row.
 */
@Composable
private fun RewardRow(
    card: RewardCard, enabled: Boolean, actions: RewardActions, strip: (@Composable () -> Unit)?, modifier: Modifier,
) {
    val border = if (strip != null) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else CardDefaults.outlinedCardBorder()
    OutlinedCard(modifier.fillMaxWidth().clickable(enabled = enabled) { actions.edit(card.reward) },
        shape = RoundedCornerShape(17.dp), border = border,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        RewardRowHeader(card, enabled, actions, showsPrice = strip == null)
        strip?.invoke()
    }
}

@Composable
private fun RewardRowHeader(card: RewardCard, enabled: Boolean, actions: RewardActions, showsPrice: Boolean) {
    val reward = card.reward
    val block = card.block()
    Row(Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer,
            RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(rewardIcon(reward.icon)), contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f)) {
            Text(reward.name, style = MaterialTheme.typography.titleMedium, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            RewardFacts(card, block)
        }
        when {
            !showsPrice -> Unit
            block == RewardBlock.None -> {
                val description = stringResource(R.string.reward_buy_price, reward.name, reward.cost)
                FilledTonalButton({ actions.preview(reward.id) }, enabled = enabled,
                    modifier = Modifier.semantics { contentDescription = description }) {
                    CoinGlyph()
                    Text(reward.cost.toString(), Modifier.padding(start = 4.dp))
                }
            }
            else -> Text("$COIN_SYMBOL ${reward.cost}", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = MUTED_ALPHA))
        }
        RewardMenu(reward, enabled, actions)
    }
}

/** Confirmation under the row: no dialog, so the store stays in view (option C of prototypes/confirmar-compra). */
@Composable
private fun PurchaseStripContent(strip: PurchaseStrip, state: RewardsUiState, actions: RewardActions) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, bottom = 8.dp)
        .semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalDivider(Modifier.padding(bottom = 4.dp), color = colors.outlineVariant)
        if (state.stale) Text(stringResource(R.string.reward_stale), color = colors.tertiary,
            style = MaterialTheme.typography.bodySmall)
        strip.balanceAfter?.let {
            Text(stringResource(R.string.reward_strip_balance, strip.balanceBefore, it),
                style = MaterialTheme.typography.bodyMedium)
        }
        StripLine(if (strip.minutes != null) R.drawable.ic_schedule else R.drawable.ic_bolt,
            strip.minutes?.let { stringResource(R.string.reward_strip_timed, durationText(it)) }
                ?: stringResource(R.string.reward_strip_now))
        if (strip.limit != null && strip.usedAfter != null && strip.block == null) {
            StripLine(R.drawable.ic_repeat, stringResource(R.string.reward_strip_after,
                stringResource(limitUsedLabel(strip.period), strip.usedAfter, strip.limit)))
        }
        strip.pausing?.let {
            StripLine(R.drawable.ic_pause, stringResource(R.string.reward_strip_pause, it), warn = true)
        }
        strip.block?.let {
            Text(stringResource(it.label), color = colors.error, style = MaterialTheme.typography.bodySmall)
        }
        RewardError(state, actions)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(actions.close, enabled = !state.busy) { Text(stringResource(R.string.cancel)) }
            Button(if (strip.pausing != null) actions.confirmPause else actions.confirm,
                enabled = strip.block == null && !state.busy && !state.error && !state.loading) {
                if (strip.pausing != null) Text(stringResource(R.string.reward_pause_buy))
                else {
                    Text(stringResource(R.string.reward_buy), Modifier.padding(end = 6.dp))
                    CoinGlyph()
                    Text(strip.cost.toString(), Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun StripLine(@DrawableRes icon: Int, text: String, warn: Boolean = false) {
    val color = if (warn) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(painterResource(icon), contentDescription = null, Modifier.size(16.dp), tint = color)
        Text(text, color = color, style = MaterialTheme.typography.bodySmall)
    }
}

/** Short receipt after confirming; it closes the intent itself once the accessibility-adjusted time is over. */
@Composable
private fun PurchaseReceipt(purchase: Purchase, balance: Long, close: () -> Unit, modifier: Modifier) {
    val accessibility = LocalAccessibilityManager.current
    val timeout = accessibility?.calculateRecommendedTimeoutMillis(RECEIPT_MILLIS,
        containsIcons = false, containsText = true, containsControls = true) ?: RECEIPT_MILLIS
    LaunchedEffect(purchase.id, timeout) { delay(timeout); close() }
    Snackbar(modifier.padding(12.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dismissActionContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        dismissAction = { TextButton(close) { Text(stringResource(R.string.close)) } }) {
        val name = purchase.reward.name
        Text(if (purchase.reward.minutes != null) stringResource(R.string.reward_bought_timed, name, balance)
            else stringResource(R.string.reward_bought, name, balance))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RewardFacts(card: RewardCard, block: RewardBlock) {
    val reward = card.reward
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        reward.minutes?.let { Text(durationText(it), style = MaterialTheme.typography.bodySmall, color = muted) }
        Text(reward.limit?.let { stringResource(limitUsedLabel(reward.period), card.used, it) }
            ?: stringResource(R.string.reward_no_limit), style = MaterialTheme.typography.bodySmall, color = muted)
        when (block) {
            RewardBlock.None -> Unit
            is RewardBlock.Missing -> BlockTag(stringResource(R.string.reward_missing, block.coins), warn = true)
            is RewardBlock.Exhausted -> BlockTag(stringResource(block.renews), warn = false)
        }
    }
}

@Composable
private fun BlockTag(text: String, warn: Boolean) {
    val colors = MaterialTheme.colorScheme
    Text(text, Modifier.background(if (warn) colors.tertiaryContainer else colors.surfaceContainerHighest,
        RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
        color = if (warn) colors.onTertiaryContainer else colors.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall)
}

@Composable
private fun RewardMenu(reward: Reward, enabled: Boolean, actions: RewardActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        val description = stringResource(R.string.reward_options, reward.name)
        IconButton({ open = true }, enabled = enabled, modifier = Modifier.semantics {
            contentDescription = description }) {
            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = null)
        }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.edit)) },
                onClick = { open = false; actions.edit(reward) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_edit), contentDescription = null) })
            DropdownMenuItem(text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                onClick = { open = false; actions.deletion(reward.id) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null,
                    tint = MaterialTheme.colorScheme.error) })
        }
    }
}

@Preview
@Composable
private fun RewardsStorePreview() {
    val today = LocalDate.of(2026, 11, 19)
    val rewards = listOf(Reward("1", "Pausa de 15 min", icon = "REST", cost = 20, minutes = 15),
        Reward("2", "Episódio de série", icon = "MOVIE", cost = 40, limit = 2),
        Reward("3", "Lanche fora", icon = "FAST_FOOD", cost = 120, limit = 1, period = "WEEK"))
    TabitosTheme {
        RewardsContent(RewardsUiState(loading = false,
            cards = rewards.map { rewardCard(it, emptyList(), balance = 60, today) }), RewardActions())
    }
}

@Preview
@Composable
private fun RewardsEmptyPreview() { TabitosTheme { RewardsContent(RewardsUiState(loading = false), RewardActions()) } }

private const val REFRESH_MILLIS = 30_000L
private const val RECEIPT_MILLIS = 6_000L
private const val MUTED_ALPHA = 0.7f
private const val COIN_SYMBOL = "◈"
private val FAB_CLEARANCE = 96.dp
