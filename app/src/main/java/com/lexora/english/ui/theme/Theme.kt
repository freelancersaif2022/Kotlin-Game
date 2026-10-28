package com.lexora.english.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

object Brand {
    val Blue = Color(0xFF2F5BEA)
    val BlueDark = Color(0xFF1E3FB8)
    val Indigo = Color(0xFF5B4BDB)
    val Amber = Color(0xFFFFB020)
    val Emerald = Color(0xFF1FB57A)
    val Red = Color(0xFFE5484D)
    val Gold = Color(0xFFC9A227)
    val Sky = Color(0xFF38BDF8)
    val Pink = Color(0xFFEC4899)
    val Orange = Color(0xFFF97316)
    val Teal = Color(0xFF14B8A6)
}

@Immutable
data class LexColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardAlt: Color,
    val border: Color,
    val text: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primary: Color,
    val primarySoft: Color,
    val onPrimary: Color,
    val accent: Color,
    val accentSoft: Color,
    val success: Color,
    val successSoft: Color,
    val error: Color,
    val errorSoft: Color,
    val track: Color,
    val navBar: Color,
    val scrim: Color,
)

private val LightLex = LexColors(
    isDark = false,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    cardAlt = Color(0xFFF5F7FB),
    border = Color(0xFFE9ECF3),
    text = Color(0xFF0B0D17),
    textSecondary = Color(0xFF4A5064),
    textMuted = Color(0xFF8A90A2),
    primary = Brand.Blue,
    primarySoft = Color(0xFFEAF0FF),
    onPrimary = Color.White,
    accent = Brand.Amber,
    accentSoft = Color(0xFFFFF4DC),
    success = Brand.Emerald,
    successSoft = Color(0xFFE3F8EF),
    error = Brand.Red,
    errorSoft = Color(0xFFFDECEC),
    track = Color(0xFFEDEFF5),
    navBar = Color(0xFFFFFFFF),
    scrim = Color(0xB3000000),
)

private val DarkLex = LexColors(
    isDark = true,
    background = Color(0xFF0D0F14),
    surface = Color(0xFF13161D),
    card = Color(0xFF161A22),
    cardAlt = Color(0xFF1C212B),
    border = Color(0xFF262B37),
    text = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFC9CDD8),
    textMuted = Color(0xFF8C93A5),
    primary = Color(0xFF5B82FF),
    primarySoft = Color(0xFF1C2647),
    onPrimary = Color.White,
    accent = Brand.Amber,
    accentSoft = Color(0xFF3A2E12),
    success = Color(0xFF2DD4A0),
    successSoft = Color(0xFF123227),
    error = Color(0xFFFF6B6F),
    errorSoft = Color(0xFF3A1618),
    track = Color(0xFF262B37),
    navBar = Color(0xFF13161D),
    scrim = Color(0xCC000000),
)

val LocalLex = staticCompositionLocalOf { LightLex }

object Lex {
    val colors: LexColors
        @Composable get() = LocalLex.current
}

@Composable
fun LexoraTheme(
    themeMode: String,
    fontScale: Float,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        "dark" -> true
        "system" -> isSystemInDarkTheme()
        else -> false
    }
    val lex = if (dark) DarkLex else LightLex
    val scheme = if (dark) {
        darkColorScheme(
            primary = lex.primary, onPrimary = lex.onPrimary,
            secondary = Brand.Indigo, tertiary = Brand.Amber,
            background = lex.background, onBackground = lex.text,
            surface = lex.surface, onSurface = lex.text,
            surfaceVariant = lex.cardAlt, onSurfaceVariant = lex.textSecondary,
            outline = lex.border, error = lex.error,
        )
    } else {
        lightColorScheme(
            primary = lex.primary, onPrimary = lex.onPrimary,
            secondary = Brand.Indigo, tertiary = Brand.Amber,
            background = lex.background, onBackground = lex.text,
            surface = lex.surface, onSurface = lex.text,
            surfaceVariant = lex.cardAlt, onSurfaceVariant = lex.textSecondary,
            outline = lex.border, error = lex.error,
        )
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

    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalLex provides lex,
        LocalDensity provides Density(density.density, density.fontScale * fontScale),
    ) {
        MaterialTheme(colorScheme = scheme, typography = LexTypography, content = content)
    }
}
