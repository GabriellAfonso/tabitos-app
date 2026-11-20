package com.tabitos.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tabitos.app.R
import com.tabitos.app.core.ui.theme.TabitosTheme

private val TypedFieldWidth = 88.dp

/**
 * Number chosen with − / + around a highlighted bubble, read as a sentence: "[prefix] (3) [suffix]",
 * e.g. "3 vezes por semana" or "A cada 2 dias". Replaces typed numbers in editors (visual over text).
 * With [typeable], tapping the bubble lets the person type the value instead (for wide ranges like prices).
 */
@Composable
fun NumberStepper(
    value: Int, onChange: (Int) -> Unit, range: IntRange, suffix: String, modifier: Modifier = Modifier,
    prefix: String = "", step: Int = 1, enabled: Boolean = true, typeable: Boolean = false,
) {
    val current = value.coerceIn(range)
    val down = (current - step).coerceAtLeast(range.first)
    val up = (current + step).coerceAtMost(range.last)
    var typing by rememberSaveable { mutableStateOf(false) }
    val spoken = listOf(prefix, current.toString(), suffix).filter { it.isNotBlank() }.joinToString(" ")
    val decrease = stringResource(R.string.stepper_decrease)
    val increase = stringResource(R.string.stepper_increase)
    val type = stringResource(R.string.stepper_type)
    val rowSemantics = if (typing) Modifier else Modifier.clearAndSetSemantics {
        contentDescription = spoken
        customActions = listOfNotNull(
            CustomAccessibilityAction(decrease) { if (enabled && down != current) { onChange(down); true } else false },
            CustomAccessibilityAction(increase) { if (enabled && up != current) { onChange(up); true } else false },
            if (typeable && enabled) CustomAccessibilityAction(type) { typing = true; true } else null,
        )
    }
    Row(modifier.then(rowSemantics), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (prefix.isNotBlank()) Text(prefix, style = MaterialTheme.typography.bodyLarge)
        FilledTonalIconButton({ onChange(down) }, enabled = enabled && down != current && !typing) {
            Icon(painterResource(R.drawable.ic_remove), contentDescription = null)
        }
        if (typing && enabled) {
            TypedNumberField(current, range, spoken) { typed ->
                typed?.let(onChange)
                typing = false
            }
        } else {
            ValueBubble(current, Modifier.clickable(enabled = typeable && enabled) { typing = true })
        }
        FilledTonalIconButton({ onChange(up) }, enabled = enabled && up != current && !typing) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null)
        }
        if (suffix.isNotBlank()) Text(suffix, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ValueBubble(value: Int, modifier: Modifier) {
    Box(Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp)
        .background(MaterialTheme.colorScheme.primary, CircleShape).then(modifier).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center) {
        Text(value.toString(), color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

/** Numeric field in place of the bubble; confirms on Done or when focus leaves, null when nothing valid typed. */
@Composable
private fun TypedNumberField(current: Int, range: IntRange, label: String, finish: (Int?) -> Unit) {
    val start = current.toString()
    var text by remember { mutableStateOf(TextFieldValue(start, TextRange(0, start.length))) }
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val confirm = { finish(parseTypedNumber(text.text, range)) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BasicTextField(
        value = text,
        onValueChange = { typed ->
            text = typed.copy(text = typed.text.filter(Char::isDigit).take(range.last.digits()))
        },
        modifier = Modifier.width(TypedFieldWidth).sizeIn(minHeight = 44.dp).testTag("stepper:typed")
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .focusRequester(focus)
            .onFocusChanged { state -> if (focused && !state.isFocused) confirm(); focused = state.isFocused }
            .semantics { contentDescription = label },
        singleLine = true,
        textStyle = TextStyle(color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center,
            fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.Bold),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimaryContainer),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { confirm() }),
        decorationBox = { inner -> Box(Modifier.padding(vertical = 10.dp), Alignment.Center) { inner() } },
    )
}

/** Typed digits as a value inside [range]; out-of-range numbers are clamped, blank or non-numeric is null. */
internal fun parseTypedNumber(text: String, range: IntRange): Int? =
    text.trim().toLongOrNull()?.coerceIn(range.first.toLong(), range.last.toLong())?.toInt()

private fun Int.digits() = toString().length

@Preview(showBackground = true)
@Composable
private fun NumberStepperPreview() {
    TabitosTheme { NumberStepper(3, {}, 1..7, suffix = "vezes por semana") }
}

@Preview(showBackground = true)
@Composable
private fun TypeableNumberStepperPreview() {
    TabitosTheme { NumberStepper(150, {}, 1..99_999, suffix = "moedas", step = 5, typeable = true) }
}
