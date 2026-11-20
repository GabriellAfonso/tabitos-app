package com.tabitos.app.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.MotionDurationScale
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.annotation.DrawableRes
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.tabitos.app.R
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tabitos.app.core.ui.theme.TabitosTheme

/** Corner of every card on Hoje and the list pages, so tasks, habits, wallet and notices line up. */
val CardShape = RoundedCornerShape(16.dp)

/** Quiet section label ("Nesta semana", "Hábitos", "Minhas listas"): cards stand out, sections recede. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.semantics { heading() }, style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

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
fun ActionBanner(text: String, @DrawableRes icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier,
                 outlined: Boolean = false, urgent: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Card(onClick, modifier.fillMaxWidth(), shape = CardShape,
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
            } else Icon(painterResource(icon), contentDescription = null, tint = colors.primary)
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null,
                tint = if (urgent) colors.onError else colors.onSurfaceVariant)
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

private const val CHECK_FILL_MILLIS = 300
private const val CHECK_POP_UP_MILLIS = 120
private const val CHECK_POP_DOWN_MILLIS = 260
private const val CHECK_POP_SCALE = 1.25f
private const val CHECK_FILL_SHARE = 0.4f

/** Time the tick takes to fill, draw and settle; callers wait for it before acting on the check. */
const val CHECK_ANIMATION_MILLIS = 400L

/**
 * App animations play at their own speed: the check is a short piece of feedback and must not vanish when the
 * system animator scale is reduced or turned off.
 */
private object FixedMotionScale : MotionDurationScale {
    override val scaleFactor = 1f
}

/**
 * Rounded check box drawn as in the prototype; the caller supplies click semantics. Checking it fills the box,
 * draws the tick and gives a small bounce; the first composition shows the current state without animating.
 */
@Composable
fun RoundCheck(checked: Boolean, modifier: Modifier = Modifier, crossed: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val progress = remember { Animatable(if (checked) 1f else 0f) }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(checked) {
        if (!checked) progress.snapTo(0f)
        else if (progress.value < 1f) withContext(FixedMotionScale) {
            launch {
                pop.animateTo(CHECK_POP_SCALE, tween(CHECK_POP_UP_MILLIS, easing = FastOutSlowInEasing))
                pop.animateTo(1f, tween(CHECK_POP_DOWN_MILLIS, easing = BounceSettleEasing))
            }
            progress.animateTo(1f, tween(CHECK_FILL_MILLIS, easing = LinearEasing))
        }
    }
    val fill = (progress.value / CHECK_FILL_SHARE).coerceAtMost(1f)
    val tick = ((progress.value - CHECK_FILL_SHARE) / (1f - CHECK_FILL_SHARE)).coerceIn(0f, 1f)
    Box(modifier.size(24.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }
        .clip(RoundedCornerShape(7.dp)).background(lerp(Color.Transparent, colors.primary, fill))
        .border(1.5.dp, lerp(colors.onSurfaceVariant, colors.primary, fill), RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center) {
        if (tick > 0f) CheckTick(tick, colors.onPrimary)
        else if (crossed) Icon(painterResource(R.drawable.ic_close), contentDescription = null,
            modifier = Modifier.size(18.dp), tint = colors.onSurfaceVariant)
    }
}

/** Slight undershoot before resting at 1, so the shrink back reads as a soft bounce. */
private val BounceSettleEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/** Tick stroked from its short leg to the end of the long leg up to [progress] (0–1). */
@Composable
private fun CheckTick(progress: Float, color: Color) {
    Canvas(Modifier.size(14.dp)) {
        val start = Offset(size.width * 0.12f, size.height * 0.52f)
        val corner = Offset(size.width * 0.40f, size.height * 0.80f)
        val end = Offset(size.width * 0.90f, size.height * 0.22f)
        val shortLeg = (corner - start).getDistance()
        val longLeg = (end - corner).getDistance()
        val drawn = progress * (shortLeg + longLeg)
        val stroke = 2.dp.toPx()
        fun lerp(from: Offset, to: Offset, fraction: Float) = from + (to - from) * fraction
        drawLine(color, start, lerp(start, corner, (drawn / shortLeg).coerceAtMost(1f)), stroke, StrokeCap.Round)
        if (drawn > shortLeg) {
            drawLine(color, corner, lerp(corner, end, (drawn - shortLeg) / longLeg), stroke, StrokeCap.Round)
        }
    }
}

@Preview
@Composable
private fun ComponentsPreview() {
    TabitosTheme(darkTheme = false) {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Eyebrow("Suas moedas")
                CoinAmount("148")
                ActionBanner("Você tem 2 pendências para revisar", R.drawable.ic_pending_actions, {}, urgent = true)
                ActionBanner("1 acompanhamento para preencher", R.drawable.ic_edit, {}, outlined = true)
                SegmentedControl(listOf("Tudo", "Tarefas", "Hábitos"), "Tudo", { it }, {})
                WeekBars(listOf(true, true, false, true, false, false, false), "Semana")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { RoundCheck(false); RoundCheck(true) }
            }
        }
    }
}
