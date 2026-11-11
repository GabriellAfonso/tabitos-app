package com.ethos.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethos.app.core.ui.theme.EthosTheme

/** Amber coin glyph used wherever an amount of coins is shown (spec 010). */
@Composable
fun CoinGlyph(modifier: Modifier = Modifier) {
    Box(modifier.size(18.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.Center) {
        Text("◈", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp, lineHeight = 12.sp)
    }
}

@Composable
fun CoinAmount(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.titleLarge) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CoinGlyph()
        Text(text, style = style, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold)
    }
}

/** Small uppercase label above a value, as in the model B wallet. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier, style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp)
}

/** Tappable notice row ("Você tem 2 pendências para revisar ›"). [outlined] marks gentle reminders. */
@Composable
fun ActionBanner(text: String, symbol: String, onClick: () -> Unit, modifier: Modifier = Modifier,
                 outlined: Boolean = false, urgent: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Card(onClick, modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = when {
            urgent -> colors.error
            outlined -> colors.surfaceContainerLow
            else -> colors.secondaryContainer
        }, contentColor = if (urgent) colors.onError else colors.onSurface),
        border = BorderStroke(1.dp, if (urgent) colors.error else if (outlined) colors.primary else colors.outlineVariant)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (urgent) Canvas(Modifier.size(40.dp)) {
                val stroke = 2.5.dp.toPx()
                drawCircle(colors.onError, radius = size.minDimension / 2 - stroke, style = Stroke(stroke))
                drawLine(colors.onError, center, Offset(center.x, size.height * 0.25f), stroke)
                drawLine(colors.onError, center, Offset(size.width * 0.7f, size.height * 0.6f), stroke)
            } else Text(symbol, color = colors.primary)
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("›", color = if (urgent) colors.onError else colors.onSurfaceVariant)
        }
    }
}

/** Pill-shaped single choice control; each option is selectable for accessibility and tests. */
@Composable
fun <T> SegmentedControl(
    options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit,
    modifier: Modifier = Modifier, tag: (T) -> String? = { null },
) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.secondaryContainer)
        .padding(4.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            val on = option == selected
            val text = label(option)
            Box(Modifier.weight(1f).heightIn(min = 40.dp).clip(RoundedCornerShape(10.dp))
                .background(if (on) colors.surfaceContainerLow else Color.Transparent)
                .selectable(on, role = Role.Tab) { onSelect(option) }
                .then(tag(option)?.let { Modifier.testTag(it) } ?: Modifier)
                .padding(horizontal = 4.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(text, style = MaterialTheme.typography.labelLarge,
                    color = if (on) colors.onSurface else colors.onSurfaceVariant)
            }
        }
    }
}

/** Monday–Sunday bars; filled days are the ones with records. */
@Composable
fun WeekBars(hits: List<Boolean>, description: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        hits.forEach { hit ->
            Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(4.dp))
                .background(if (hit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
        }
    }
}

/** Rounded check box drawn as in the prototype; the caller supplies click semantics. */
@Composable
fun RoundCheck(checked: Boolean, modifier: Modifier = Modifier, crossed: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.size(24.dp).clip(RoundedCornerShape(7.dp))
        .background(if (checked) colors.primary else Color.Transparent)
        .border(1.5.dp, if (checked) colors.primary else colors.onSurfaceVariant, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center) {
        if (checked) Text("✓", color = colors.onPrimary, fontSize = 13.sp)
        else if (crossed) Text("×", color = colors.onSurfaceVariant, fontSize = 20.sp)
    }
}

@Preview
@Composable
private fun ComponentsPreview() {
    EthosTheme(darkTheme = false) {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Eyebrow("Suas moedas")
                CoinAmount("148")
                ActionBanner("Você tem 2 pendências para revisar", "◷", {}, urgent = true)
                ActionBanner("1 acompanhamento para preencher", "✎", {}, outlined = true)
                SegmentedControl(listOf("Tudo", "Tarefas", "Hábitos"), "Tudo", { it }, {})
                WeekBars(listOf(true, true, false, true, false, false, false), "Semana")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { RoundCheck(false); RoundCheck(true) }
            }
        }
    }
}
