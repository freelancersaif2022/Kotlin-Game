package com.pdfmaster.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.pdfmaster.app.data.prefs.ThemeMode

private val LightColors = lightColorScheme(
    primary = Brand.Red,
    onPrimary = Color.White,
    primaryContainer = Brand.RedSoft,
    onPrimaryContainer = Brand.RedDeep,
    secondary = Brand.Ink,
    onSecondary = Color.White,
    secondaryContainer = Brand.LightGray,
    onSecondaryContainer = Brand.Ink,
    tertiary = Brand.Gold,
    background = Brand.Background,
    onBackground = Brand.Ink,
    surface = Color.White,
    onSurface = Brand.Ink,
    surfaceVariant = Brand.LightGray,
    onSurfaceVariant = Color(0xFF5F6478),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFAFC),
    surfaceContainer = Color(0xFFF4F5F8),
    surfaceContainerHigh = Color(0xFFEEF0F5),
    surfaceContainerHighest = Color(0xFFE6E8EF),
    outline = Color(0xFFD5D8E2),
    outlineVariant = Color(0xFFE8EAF0),
    error = Brand.RedDeep,
    scrim = Color(0xFF0B0C10),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF4D52),
    onPrimary = Color.White,
    primaryContainer = Brand.RedSoftDark,
    onPrimaryContainer = Color(0xFFFFB3B5),
    secondary = Color(0xFFE6E7EE),
    onSecondary = Brand.Ink,
    secondaryContainer = Brand.DarkSurfaceHigh,
    onSecondaryContainer = Color(0xFFE6E7EE),
    tertiary = Brand.Gold,
    background = Brand.DarkBackground,
    onBackground = Color(0xFFF1F2F6),
    surface = Brand.DarkSurface,
    onSurface = Color(0xFFF1F2F6),
    surfaceVariant = Brand.DarkSurfaceHigh,
    onSurfaceVariant = Color(0xFFA3A7B7),
    surfaceContainerLowest = Color(0xFF0B0C10),
    surfaceContainerLow = Color(0xFF14151A),
    surfaceContainer = Color(0xFF1A1B21),
    surfaceContainerHigh = Color(0xFF22242C),
    surfaceContainerHighest = Color(0xFF2A2C35),
    outline = Color(0xFF3A3D49),
    outlineVariant = Brand.DarkOutline,
    error = Color(0xFFFF6B6F),
    scrim = Color.Black,
)

@Immutable
data class ExtendedColors(
    val gold: Color,
    val goldSoft: Color,
    val success: Color,
    val warning: Color,
    val info: Color,
    val cardBorder: Color,
    val subtle: Color,
    val pageBackground: Color,
    val isDark: Boolean,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(Brand.Gold, Brand.GoldSoft, Brand.Success, Brand.Warning, Brand.Info, Color(0xFFEDEFF4), Brand.Gray, Color(0xFFE9EBF0), false)
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun PdfMasterTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val extended = if (dark) {
        ExtendedColors(Brand.Gold, Color(0xFF3A2E10), Color(0xFF34D399), Brand.Warning, Color(0xFF60A5FA), Brand.DarkOutline, Color(0xFF9A9EAF), Color(0xFF08090C), true)
    } else {
        ExtendedColors(Brand.Gold, Brand.GoldSoft, Brand.Success, Brand.Warning, Brand.Info, Color(0xFFEDEFF4), Brand.Gray, Color(0xFFE9EBF0), false)
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

object AppTheme {
    val colors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
