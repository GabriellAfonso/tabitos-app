package com.ethos.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import com.ethos.app.core.ui.theme.EthosTheme

/**
 * Number chosen with − / + around a highlighted bubble, read as a sentence: "[prefix] (3) [suffix]",
 * e.g. "3 vezes por semana" or "A cada 2 dias". Replaces typed numbers in editors (visual over text).
 */
@Composable
fun NumberStepper(
    value: Int, onChange: (Int) -> Unit, range: IntRange, suffix: String, modifier: Modifier = Modifier,
    prefix: String = "", step: Int = 1, enabled: Boolean = true,
) {
    val current = value.coerceIn(range)
    val down = (current - step).coerceAtLeast(range.first)
    val up = (current + step).coerceAtMost(range.last)
    val spoken = listOf(prefix, current.toString(), suffix).filter { it.isNotBlank() }.joinToString(" ")
    val decrease = stringResource(R.string.stepper_decrease)
    val increase = stringResource(R.string.stepper_increase)
    Row(modifier.clearAndSetSemantics {
        contentDescription = spoken
        customActions = listOf(
            CustomAccessibilityAction(decrease) { if (enabled && down != current) { onChange(down); true } else false },
            CustomAccessibilityAction(increase) { if (enabled && up != current) { onChange(up); true } else false },
        )
    }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (prefix.isNotBlank()) Text(prefix, style = MaterialTheme.typography.bodyLarge)
        FilledTonalIconButton({ onChange(down) }, enabled = enabled && down != current) {
            Icon(painterResource(R.drawable.ic_remove), contentDescription = null)
        }
        Box(Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
            Text(current.toString(), color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        FilledTonalIconButton({ onChange(up) }, enabled = enabled && up != current) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null)
        }
        if (suffix.isNotBlank()) Text(suffix, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true)
@Composable
private fun NumberStepperPreview() {
    EthosTheme { NumberStepper(3, {}, 1..7, suffix = "vezes por semana") }
}
