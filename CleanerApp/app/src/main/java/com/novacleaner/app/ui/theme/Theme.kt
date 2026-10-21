package com.novacleaner.app.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Immutable
data class NovaColors(
    val isDark: Boolean,
    val heroGradient: List<Color>,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val cardBorder: Color,
    val subtle: Color,
    val chartWifi: Color,
    val chartMobile: Color,
) {
    val heroBrush: Brush get() = Brush.linearGradient(heroGradient)
}

private val LightNova = NovaColors(
    isDark = false,
    heroGradient = listOf(Emerald500, Emerald600, Emerald700),
    success = Emerald600,
    warning = Amber,
    danger = Coral,
    cardBorder = LightOutline,
    subtle = Color(0xFFF6FAF8),
    chartWifi = Emerald500,
    chartMobile = Tangerine,
)

private val DarkNova = NovaColors(
    isDark = true,
    heroGradient = listOf(Emerald600, Emerald700, Emerald800),
    success = Emerald400,
    warning = Amber,
    danger = Coral,
    cardBorder = DarkOutline,
    subtle = Color(0xFF0D1714),
    chartWifi = Emerald400,
    chartMobile = Tangerine,
)

val LocalNovaColors = staticCompositionLocalOf { LightNova }

private val LightScheme = lightColorScheme(
    primary = Emerald600,
    onPrimary = Color.White,
    primaryContainer = Mint100,
    onPrimaryContainer = Emerald900,
    secondary = Tangerine,
    onSecondary = Color.White,
    tertiary = Violet,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurface,
    surfaceContainerLow = LightSurface,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = Coral,
)

private val DarkScheme = darkColorScheme(
    primary = Emerald400,
    onPrimary = Emerald900,
    primaryContainer = Emerald800,
    onPrimaryContainer = Mint100,
    secondary = Tangerine,
    onSecondary = Color.White,
    tertiary = Violet,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceVariant,
    surfaceContainerLow = DarkSurface,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = Coral,
)

val NovaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** themeMode: 0 = follow system, 1 = light, 2 = dark */
@Composable
fun NovaTheme(themeMode: Int = 0, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    DisposableEffect(dark) {
        (context as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()) { dark },
            navigationBarStyle = SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()) { dark },
        )
        onDispose { }
    }
    CompositionLocalProvider(LocalNovaColors provides if (dark) DarkNova else LightNova) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = NovaTypography,
            shapes = NovaShapes,
            content = content,
        )
    }
}

object Nova {
    val colors: NovaColors
        @Composable get() = LocalNovaColors.current
}
