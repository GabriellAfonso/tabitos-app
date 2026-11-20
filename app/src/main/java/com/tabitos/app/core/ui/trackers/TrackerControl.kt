package com.tabitos.app.core.ui.trackers

import androidx.compose.runtime.staticCompositionLocalOf

/** Opens the tracker hub of a habit without the habits feature importing the trackers feature. */
val LocalOpenTracker = staticCompositionLocalOf<(String) -> Unit> { {} }
