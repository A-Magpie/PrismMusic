package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Pure AMOLED Dark Base
val AmoledBlack = Color(0xFF000000)
val DarkSurface = Color(0xFF0C0C12)
val DarkSurfaceElevated = Color(0xFF14141E)
val DarkCardGlass = Color(0xCC12121A)
val GlassBorder = Color(0x2EFFFFFF)
val GlassHighlight = Color(0x1AFFFFFF)

// Accent Colors (Cyan replaced with sleek gray per requirement; Purple completely removed)
val AccentGray = Color(0xFFAAAAAA)
val AccentCyan = AccentGray // Alias for backwards compatibility so all previous usages become sleek gray
val AccentPurple = AccentGray // Fallback alias to guarantee no purple ever displays
val AccentPink = Color(0xFFE040FB)
val AccentAmber = Color(0xFFFFB74D)
val AccentRed = Color(0xFFFF5252)

// Text Colors
val TextWhite = Color(0xFFFFFFFF)
val TextSecondary = Color(0xCCFFFFFF)
val TextMuted = Color(0x80FFFFFF)
val TextSubtle = Color(0x4DFFFFFF)
