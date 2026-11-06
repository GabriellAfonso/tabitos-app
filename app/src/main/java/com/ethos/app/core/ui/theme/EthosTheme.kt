package com.ethos.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF365D47),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9F0E3),
    onPrimaryContainer = Color(0xFF24302A),
    secondary = Color(0xFF365D47),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9F0E3),
    onSecondaryContainer = Color(0xFF24302A),
    background = Color(0xFFF9FAF5),
    onBackground = Color(0xFF24302A),
    surface = Color(0xFFF9FAF5),
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEFF3E9),
    surfaceContainerHigh = Color(0xFFE9F0E3),
    onSurface = Color(0xFF24302A),
    onSurfaceVariant = Color(0xFF59665D),
    outlineVariant = Color(0xFFE4E8DE),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB1D3AE),
    onPrimary = Color(0xFF173823),
    primaryContainer = Color(0xFF303F31),
    onPrimaryContainer = Color(0xFFE5ECE1),
    secondary = Color(0xFFB1D3AE),
    onSecondary = Color(0xFF173823),
    secondaryContainer = Color(0xFF303F31),
    onSecondaryContainer = Color(0xFFE5ECE1),
    background = Color(0xFF171E1A),
    onBackground = Color(0xFFE5ECE1),
    surface = Color(0xFF171E1A),
    surfaceContainerLow = Color(0xFF222C25),
    surfaceContainer = Color(0xFF222C25),
    surfaceContainerHigh = Color(0xFF303F31),
    onSurface = Color(0xFFE5ECE1),
    onSurfaceVariant = Color(0xFFA7B3A9),
    outlineVariant = Color(0xFF354138),
)

@Composable
fun EthosTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
