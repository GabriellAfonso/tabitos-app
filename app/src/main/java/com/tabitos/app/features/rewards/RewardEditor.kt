package com.tabitos.app.features.rewards

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tabitos.app.R
import com.tabitos.app.core.icons.AppIcon
import com.tabitos.app.core.ui.components.ChoiceSheet
import com.tabitos.app.core.ui.components.EditorChip
import com.tabitos.app.core.ui.components.EditorDescriptionLine
import com.tabitos.app.core.ui.components.EditorIconTile
import com.tabitos.app.core.ui.components.EditorNameField
import com.tabitos.app.core.ui.components.EditorValueRow
import com.tabitos.app.core.ui.components.FocusOnCreate
import com.tabitos.app.core.ui.components.GrowingSheet
import com.tabitos.app.core.ui.components.IconPicker
import com.tabitos.app.core.ui.components.NumberStepper
import com.tabitos.app.core.ui.components.RadioOptions
import com.tabitos.app.core.ui.rewards.rewardAppIcon
import com.tabitos.app.core.ui.rewards.rewardIcon

/** Icons offered in the reward editor's row; the rest are under "see more". */
private val rewardPrincipalIcons = listOf(AppIcon.GIFT, AppIcon.GAME, AppIcon.FOOD, AppIcon.REST, AppIcon.MOVIE)

private enum class RewardEditorSheet(@param:StringRes val title: Int) {
    ICON(R.string.reward_icon), DESCRIPTION(R.string.editor_description), COST(R.string.reward_cost_label),
    LIMIT(R.string.reward_limit_label), USE(R.string.reward_use_label),
}

/**
 * Reward editor in the growing sheet (option A of prototypes/recompensa-editor): creating opens compact with
 * price, limit and use chips and the keyboard; editing opens full screen with value rows. Rules are unchanged.
 */
@Composable
fun RewardEditor(state: RewardsUiState, d: RewardDraft, actions: RewardActions) {
    val busy = state.busy
    var sheet by rememberSaveable(d.id) { mutableStateOf<RewardEditorSheet?>(null) }
    val focus = remember { FocusRequester() }
    FocusOnCreate(d.id, !d.editing, focus)
    GrowingSheet(
        expanded = d.editing,
        title = stringResource(if (d.editing) R.string.reward_edit_title else R.string.reward_create),
        busy = busy, onDismiss = { actions.draft(null) }, onCollapse = null, onSave = actions.save,
        name = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EditorIconTile(rewardIcon(d.icon), MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.primary, stringResource(R.string.reward_change_icon), !busy) {
                    sheet = RewardEditorSheet.ICON
                }
                EditorNameField(d.name, { actions.draft(d.copy(name = it)) }, stringResource(R.string.reward_name),
                    !busy, focus, Modifier.weight(1f))
            }
        },
    ) {
        if (state.invalid) Text(stringResource(R.string.reward_invalid), color = MaterialTheme.colorScheme.error)
        RewardError(state, actions)
        if (d.editing) RewardRows(d, busy) { sheet = it }
        else {
            EditorDescriptionLine(d.description, !busy) { sheet = RewardEditorSheet.DESCRIPTION }
            RewardChips(d, busy) { sheet = it }
        }
        sheet?.let { RewardChoiceSheet(it, d, state.snapshot.wallet.balance, busy, actions.draft) { sheet = null } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RewardChips(d: RewardDraft, busy: Boolean, open: (RewardEditorSheet) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SheetChip(RewardEditorSheet.COST, costValue(d), R.drawable.ic_toll, busy, open,
            tint = MaterialTheme.colorScheme.tertiary)
        SheetChip(RewardEditorSheet.LIMIT, limitValue(d), R.drawable.ic_repeat, busy, open)
        SheetChip(RewardEditorSheet.USE, useValue(d),
            if (d.minutes != null) R.drawable.ic_notification_timer else R.drawable.ic_bolt, busy, open)
    }
}

@Composable
private fun SheetChip(
    sheet: RewardEditorSheet, value: String, @DrawableRes icon: Int, busy: Boolean, open: (RewardEditorSheet) -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    EditorChip(stringResource(sheet.title), value, icon, { open(sheet) }, Modifier.testTag("chip:${sheet.name}"),
        enabled = !busy, tint = tint)
}

@Composable
private fun RewardRows(d: RewardDraft, busy: Boolean, open: (RewardEditorSheet) -> Unit) {
    SheetRow(RewardEditorSheet.DESCRIPTION, d.description.ifBlank { stringResource(R.string.editor_none) },
        R.drawable.ic_edit_note, busy, open)
    SheetRow(RewardEditorSheet.COST, costValue(d), R.drawable.ic_toll, busy, open)
    SheetRow(RewardEditorSheet.LIMIT, limitValue(d), R.drawable.ic_repeat, busy, open)
    SheetRow(RewardEditorSheet.USE, useValue(d), R.drawable.ic_notification_timer, busy, open)
    Text(stringResource(R.string.reward_edit_hint), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SheetRow(
    sheet: RewardEditorSheet, value: String, @DrawableRes icon: Int, busy: Boolean, open: (RewardEditorSheet) -> Unit,
) {
    EditorValueRow(stringResource(sheet.title), value, icon, { open(sheet) }, Modifier.testTag("row:${sheet.name}"),
        enabled = !busy)
}

@Composable
private fun RewardChoiceSheet(
    sheet: RewardEditorSheet, d: RewardDraft, balance: Long, busy: Boolean, change: (RewardDraft?) -> Unit,
    close: () -> Unit,
) {
    ChoiceSheet(stringResource(sheet.title), close) {
        when (sheet) {
            RewardEditorSheet.ICON -> IconPicker(R.string.reward_icon, rewardPrincipalIcons, rewardAppIcon(d.icon),
                !busy) { change(d.copy(icon = it.name)) }
            RewardEditorSheet.DESCRIPTION -> OutlinedTextField(d.description,
                { change(d.copy(description = it)) }, Modifier.fillMaxWidth(), enabled = !busy,
                label = { Text(stringResource(R.string.editor_description)) })
            RewardEditorSheet.COST -> CostOptions(d, balance, busy, change)
            RewardEditorSheet.LIMIT -> {
                RadioOptions(limitOptions, d.limit?.let { d.period }, enabled = !busy,
                    onSelect = { change(d.withLimitPeriod(it)) }, label = { stringResource(limitOptionLabel(it)) })
                d.limit?.let { limit ->
                    NumberStepper(limit, { change(d.copy(limit = it)) }, LIMIT_COUNT_RANGE,
                        stringResource(limitTimesLabel(d.period)), enabled = !busy)
                }
            }
            RewardEditorSheet.USE -> {
                RadioOptions(listOf(false, true), d.minutes != null, enabled = !busy,
                    onSelect = { change(d.withTimed(it)) },
                    label = { stringResource(if (it) R.string.reward_use_timed else R.string.reward_use_now) })
                val hint = if (d.minutes != null) R.string.reward_timed_hint else R.string.reward_use_now_hint
                Text(stringResource(hint),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                d.minutes?.let { minutes ->
                    NumberStepper(minutes, { change(d.copy(minutes = it)) }, REWARD_MINUTES_RANGE,
                        stringResource(R.string.minutes), step = REWARD_MINUTES_STEP, enabled = !busy)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CostOptions(d: RewardDraft, balance: Long, busy: Boolean, change: (RewardDraft?) -> Unit) {
    NumberStepper(d.cost.toInt().coerceIn(COST_RANGE), { change(d.copy(cost = it.toLong())) }, COST_RANGE,
        stringResource(R.string.reward_cost_unit), step = COST_STEP, enabled = !busy, typeable = true)
    Text(stringResource(R.string.reward_cost_type_hint), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(stringResource(R.string.reward_cost_shortcuts), style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        COST_SHORTCUTS.forEach { cost ->
            FilterChip(d.cost == cost, { change(d.copy(cost = cost)) }, enabled = !busy,
                label = { Text(cost.toString()) }, modifier = Modifier.sizeIn(minHeight = 48.dp))
        }
    }
    Text(if (balance >= d.cost) stringResource(R.string.reward_cost_after, balance, balance - d.cost)
        else stringResource(R.string.reward_cost_missing, balance, d.cost - balance),
        style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun costValue(d: RewardDraft) = stringResource(R.string.reward_cost_value, d.cost)

@Composable
private fun limitValue(d: RewardDraft) =
    d.limit?.let { stringResource(limitValueLabel(d.period), it) } ?: stringResource(R.string.reward_no_limit)

@Composable
private fun useValue(d: RewardDraft) = d.minutes?.let { stringResource(R.string.reward_use_timed_value,
    durationText(it)) } ?: stringResource(R.string.reward_use_now)
