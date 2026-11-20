package com.tabitos.app.features.trackers

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Full-screen tracker page above the current one (spec 034). It is a window, so the habit popup that opened it
 * stays below and system Back returns to it. [body] scrolls on its own; [bottomBar] stays above the keyboard.
 */
@Composable
internal fun TrackerFullScreen(
    title: String,
    subtitle: String,
    busy: Boolean,
    onClose: () -> Unit,
    @DrawableRes closeIcon: Int,
    @StringRes closeLabel: Int,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: (@Composable RowScope.() -> Unit)? = null,
    body: @Composable ColumnScope.() -> Unit,
) {
    val close = { if (!busy) onClose() }
    Dialog(onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize().semantics { paneTitle = title }, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                TrackerTopBar(title, subtitle, busy, close, closeIcon, closeLabel, actions)
                Column(Modifier.weight(1f).fillMaxWidth()) { body() }
                if (bottomBar != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically, content = bottomBar)
                }
            }
        }
    }
}

@Composable
private fun TrackerTopBar(
    title: String, subtitle: String, busy: Boolean, close: () -> Unit, @DrawableRes closeIcon: Int,
    @StringRes closeLabel: Int, actions: @Composable RowScope.() -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        val description = stringResource(closeLabel)
        IconButton(close, enabled = !busy, modifier = Modifier.semantics { contentDescription = description }) {
            Icon(painterResource(closeIcon), contentDescription = null)
        }
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        actions()
    }
}
