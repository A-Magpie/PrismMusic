package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AmoledDarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00363A),
    onPrimaryContainer = Color(0xFFE0F7FA),
    secondary = AccentCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004D54),
    onSecondaryContainer = Color(0xFF70F5FF),
    tertiary = AccentPink,
    onTertiary = Color.Black,
    background = AmoledBlack,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // Pure AMOLED Dark theme is strictly enforced per PRD
    MaterialTheme(
        colorScheme = AmoledDarkColorScheme,
        typography = Typography,
        content = content
    )
}
