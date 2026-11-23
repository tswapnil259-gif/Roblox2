package com.robloxai.studio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF0B1120)
val SlateSurface = Color(0xFF111827)
val ElevatedCard = Color(0xFF1E293B)
val BorderSubtle = Color(0xFF334155)
val RobloxBlue = Color(0xFF3B82F6)
val NeonMint = Color(0xFF10B981)
val AmberWarn = Color(0xFFF59E0B)
val CrimsonError = Color(0xFFEF4444)
val TextPrimary = Color(0xFFF8FAFC)
val TextMuted = Color(0xFF94A3B8)

private val DarkColorPalette = darkColorScheme(
    primary = RobloxBlue,
    secondary = NeonMint,
    tertiary = AmberWarn,
    background = DarkBackground,
    surface = SlateSurface,
    surfaceVariant = ElevatedCard,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextMuted,
    outline = BorderSubtle,
    error = CrimsonError
)

@Composable
fun RobloxAiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorPalette,
        content = content
    )
}
