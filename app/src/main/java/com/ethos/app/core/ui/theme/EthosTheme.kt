package com.ethos.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Ameixa palette (spec 010): plum highlights, lilac-tinted neutrals; tertiary amber is reserved for coins.
private val LightColors = lightColorScheme(
    primary = Color(0xFF6E4A8E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF1E7F8),
    onPrimaryContainer = Color(0xFF2E1A40),
    secondary = Color(0xFF6B5C78),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1E7F8),
    onSecondaryContainer = Color(0xFF2E1A40),
    tertiary = Color(0xFF93601A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFBF0E0),
    onTertiaryContainer = Color(0xFF3A2400),
    background = Color(0xFFFBF8FC),
    onBackground = Color(0xFF231D29),
    surface = Color(0xFFFBF8FC),
    onSurface = Color(0xFF231D29),
    surfaceVariant = Color(0xFFF1E7F8),
    onSurfaceVariant = Color(0xFF655C6E),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF3EEF6),
    surfaceContainerHigh = Color(0xFFF1E9F5),
    surfaceContainerHighest = Color(0xFFF1E7F8),
    outline = Color(0xFF8F8498),
    outlineVariant = Color(0xFFE6DEEB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD7B8F2),
    onPrimary = Color(0xFF3B1F55),
    primaryContainer = Color(0xFF44325A),
    onPrimaryContainer = Color(0xFFF1E2FC),
    secondary = Color(0xFFCDBEDA),
    onSecondary = Color(0xFF352A40),
    secondaryContainer = Color(0xFF3A2D47),
    onSecondaryContainer = Color(0xFFF1E2FC),
    tertiary = Color(0xFFE8BE7A),
    onTertiary = Color(0xFF422C00),
    tertiaryContainer = Color(0xFF352A1F),
    onTertiaryContainer = Color(0xFFFFDDB0),
    background = Color(0xFF171419),
    onBackground = Color(0xFFECE4F0),
    surface = Color(0xFF171419),
    onSurface = Color(0xFFECE4F0),
    surfaceVariant = Color(0xFF2C2334),
    onSurfaceVariant = Color(0xFFB1A6B8),
    surfaceContainerLowest = Color(0xFF120F14),
    surfaceContainerLow = Color(0xFF211C25),
    surfaceContainer = Color(0xFF1F1A23),
    surfaceContainerHigh = Color(0xFF2A2430),
    surfaceContainerHighest = Color(0xFF2C2334),
    outline = Color(0xFF82788A),
    outlineVariant = Color(0xFF372F3D),
)

@Composable
fun EthosTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
