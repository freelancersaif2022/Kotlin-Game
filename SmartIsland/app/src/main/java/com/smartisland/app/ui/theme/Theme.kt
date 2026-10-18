package com.smartisland.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Colors that differ between light and dark mode. Accent colors are shared and live on [Palette]. */
@Immutable
data class ThemeColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val primarySoft: Color,
    val tabBar: Color,
    val snackbar: Color,
    val hero: Brush,
)

private val LightColors = ThemeColors(
    isDark = false,
    background = Color(0xFFF4F4F8),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFEDEDF3),
    outline = Color(0x14000000),
    textPrimary = Color(0xFF111118),
    textSecondary = Color(0xFF5E5F6E),
    textTertiary = Color(0xFF9697A6),
    primarySoft = Color(0xFF6447F0),
    tabBar = Color(0xF7FFFFFF),
    snackbar = Color(0xFFFFFFFF),
    hero = Brush.verticalGradient(listOf(Color(0xFFE9E2FF), Color(0xFFF1EEFB), Color(0xFFF4F4F8))),
)

private val DarkColors = ThemeColors(
    isDark = true,
    background = Color(0xFF07070B),
    surface = Color(0xFF12121A),
    surfaceHigh = Color(0xFF1C1C27),
    outline = Color(0x14FFFFFF),
    textPrimary = Color(0xFFF5F5F7),
    textSecondary = Color(0xFF9A9AA6),
    textTertiary = Color(0xFF63636E),
    primarySoft = Color(0xFFB9A6FF),
    tabBar = Color(0xF2141420),
    snackbar = Color(0xFF1C1C27),
    hero = Brush.verticalGradient(listOf(Color(0xFF241A52), Color(0xFF0E0B1F), Color(0xFF07070B))),
)

val LocalThemeColors = staticCompositionLocalOf { LightColors }

object Palette {
    // Theme-dependent neutrals.
    val Background: Color @Composable @ReadOnlyComposable get() = LocalThemeColors.current.background
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalThemeColors.current.surface
    val SurfaceHigh: Color @Composable @ReadOnlyComposable get() = LocalThemeColors.current.surfaceHigh
    val Outline: Color @Composable @ReadOnlyComposable get() = LocalThemeColors.current.outline
    val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalThemeColors.current.textPrimary
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalThemeColors.current.textSecondary
    val TextTertiary: Color @Composable @ReadOnlyComposable get() = LocalThemeColors.current.textTertiary
    val PrimarySoft: Color @Composable @ReadOnlyComposable get() = LocalThemeColors.current.primarySoft
    val TabBar: Color @Composable @ReadOnlyComposable get() = LocalThemeColors.current.tabBar
    val Snackbar: Color @Composable @ReadOnlyComposable get() = LocalThemeColors.current.snackbar
    val HeroGradient: Brush @Composable @ReadOnlyComposable get() = LocalThemeColors.current.hero
    val isDark: Boolean @Composable @ReadOnlyComposable get() = LocalThemeColors.current.isDark

    // Shared accents.
    val Primary = Color(0xFF7C5CFF)
    val Secondary = Color(0xFF0A84FF)
    val Cyan = Color(0xFF32ADE6)
    val Success = Color(0xFF30C755)
    val Warning = Color(0xFFFF9F0A)
    val Error = Color(0xFFFF453A)
    val Gold = Color(0xFFFFC53D)
    val GoldDeep = Color(0xFFB7791F)
    val OnGold = Color(0xFF3A2600)

    val PrimaryGradient = Brush.linearGradient(listOf(Color(0xFF8E72FF), Color(0xFF5B3DF5)))
    val GoldGradient = Brush.linearGradient(listOf(Color(0xFFFFE7A3), Color(0xFFFFC53D), Color(0xFFE59A1E)))
    val GoldCardGradient = Brush.linearGradient(listOf(Color(0xFF3B2A0A), Color(0xFF8A5A12), Color(0xFFE0A42F)))

    /** The phone wallpaper in island previews stays dark in both themes, like a real lock screen. */
    val WallpaperGradient = Brush.linearGradient(listOf(Color(0xFF4A35A8), Color(0xFF1B2A57), Color(0xFF0B0B14)))
}

private fun scheme(c: ThemeColors) = if (c.isDark) {
    darkColorScheme(
        primary = Palette.Primary, onPrimary = Color.White,
        primaryContainer = Color(0xFF2C2166), onPrimaryContainer = c.primarySoft,
        secondary = Palette.Secondary, onSecondary = Color.White, tertiary = Palette.Gold,
        background = c.background, onBackground = c.textPrimary,
        surface = c.surface, onSurface = c.textPrimary,
        surfaceVariant = c.surfaceHigh, onSurfaceVariant = c.textSecondary,
        surfaceContainer = c.surface, surfaceContainerHigh = c.surfaceHigh, surfaceContainerHighest = c.surfaceHigh,
        outline = Color(0x29FFFFFF), outlineVariant = c.outline, error = Palette.Error,
    )
} else {
    lightColorScheme(
        primary = Palette.Primary, onPrimary = Color.White,
        primaryContainer = Color(0xFFE6DFFF), onPrimaryContainer = c.primarySoft,
        secondary = Palette.Secondary, onSecondary = Color.White, tertiary = Palette.GoldDeep,
        background = c.background, onBackground = c.textPrimary,
        surface = c.surface, onSurface = c.textPrimary,
        surfaceVariant = c.surfaceHigh, onSurfaceVariant = c.textSecondary,
        surfaceContainer = c.surface, surfaceContainerHigh = c.surfaceHigh, surfaceContainerHighest = c.surfaceHigh,
        outline = Color(0x29000000), outlineVariant = c.outline, error = Palette.Error,
    )
}

private val LightScheme = scheme(LightColors)
private val DarkScheme = scheme(DarkColors)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp),
)

@Composable
fun SmartIslandTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    CompositionLocalProvider(LocalThemeColors provides colors) {
        MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, typography = AppTypography, content = content)
    }
}
