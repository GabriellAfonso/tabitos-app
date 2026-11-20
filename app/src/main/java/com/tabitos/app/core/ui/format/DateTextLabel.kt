package com.tabitos.app.core.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tabitos.app.R

/** Resolves relative words from resources so they stay translatable. */
@Composable
fun DateText.label(): String = when (this) {
    DateText.Today -> stringResource(R.string.today)
    DateText.Tomorrow -> stringResource(R.string.tomorrow)
    DateText.Yesterday -> stringResource(R.string.yesterday)
    is DateText.Plain -> text
}
