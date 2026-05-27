package com.beatflow.app.presentation.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object BeatFlowColors {
    val HeartRed = Color(0xFFE53935)
    val HeartRedDark = Color(0xFFB71C1C)
    val Primary = Color(0xFFD32F2F)
    val PrimaryDark = Color(0xFFB71C1C)
    val Secondary = Color(0xFF1E88E5)
    val Background = Color(0xFFF5F5F5)
    val Surface = Color(0xFFFFFFFF)
    val OnSurface = Color(0xFF1A1A1A)
    val OnSurfaceVariant = Color(0xFF616161)
    val CardBackground = Color(0xFFFFFFFF)
    val Success = Color(0xFF2E7D32)
    val Warning = Color(0xFFF57F17)
    val ChartLine = Color(0xFFE53935)
    val ChartGrid = Color(0xFFE0E0E0)
}

private val LightColorScheme = lightColorScheme(
    primary = BeatFlowColors.Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFCDD2),
    onPrimaryContainer = BeatFlowColors.PrimaryDark,
    secondary = BeatFlowColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBBDEFB),
    onSecondaryContainer = Color(0xFF0D47A1),
    error = BeatFlowColors.HeartRedDark,
    onError = Color.White,
    background = BeatFlowColors.Background,
    onBackground = BeatFlowColors.OnSurface,
    surface = BeatFlowColors.Surface,
    onSurface = BeatFlowColors.OnSurface,
    onSurfaceVariant = BeatFlowColors.OnSurfaceVariant,
    surfaceVariant = Color(0xFFF5F5F5),
    outline = Color(0xFFE0E0E0)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFEF5350),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC62828),
    onPrimaryContainer = Color(0xFFFFCDD2),
    secondary = Color(0xFF42A5F5),
    onSecondary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFFBDBDBD),
    surfaceVariant = Color(0xFF2C2C2C),
    outline = Color(0xFF424242)
)

@Composable
fun BeatFlowTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
