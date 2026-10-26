package com.zyropay.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object Brand {
    val Primary = Color(0xFF5B3DF5)
    val PrimaryDark = Color(0xFF3E25C9)
    val PrimaryLight = Color(0xFFEEEAFE)
    val GradientStart = Color(0xFF5B3DF5)
    val GradientMid = Color(0xFF8F5BFF)
    val GradientEnd = Color(0xFF00D1FF)
    val Success = Color(0xFF16C47F)
    val Warning = Color(0xFFFFB020)
    val Error = Color(0xFFF04438)
    val Info = Color(0xFF2E90FA)
    val Gold = Color(0xFFE8B84A)

    val heroGradient = Brush.linearGradient(listOf(Color(0xFF4A2FE0), Color(0xFF6A45F7), Color(0xFF8F5BFF)))
    val accentGradient = Brush.linearGradient(listOf(GradientStart, GradientMid, GradientEnd))
    val buttonGradient = Brush.horizontalGradient(listOf(Color(0xFF5B3DF5), Color(0xFF7A4DFB)))
}

@Immutable
data class ZyroColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val outline: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val primary: Color,
    val primarySoft: Color,
    val onPrimarySoft: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val error: Color,
    val errorSoft: Color,
    val info: Color,
    val infoSoft: Color,
    val scrim: Color,
    val shimmer: Color,
    val keypadPressed: Color,
)

val LightZyroColors = ZyroColors(
    isDark = false,
    background = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F3F7),
    surfaceElevated = Color(0xFFFFFFFF),
    outline = Color(0xFFE4E7EC),
    divider = Color(0xFFEEF0F4),
    textPrimary = Color(0xFF0B0F19),
    textSecondary = Color(0xFF5D6679),
    textTertiary = Color(0xFF98A2B3),
    primary = Brand.Primary,
    primarySoft = Color(0xFFEEEAFE),
    onPrimarySoft = Color(0xFF4A2FE0),
    success = Brand.Success,
    successSoft = Color(0xFFE3F8EF),
    warning = Color(0xFFE59A00),
    warningSoft = Color(0xFFFFF4DC),
    error = Brand.Error,
    errorSoft = Color(0xFFFEE9E7),
    info = Brand.Info,
    infoSoft = Color(0xFFE6F1FE),
    scrim = Color(0x990B0F19),
    shimmer = Color(0xFFE9ECF2),
    keypadPressed = Color(0xFFEDEFF4),
)

val DarkZyroColors = ZyroColors(
    isDark = true,
    background = Color(0xFF0A0B10),
    surface = Color(0xFF14161F),
    surfaceVariant = Color(0xFF1C1F2B),
    surfaceElevated = Color(0xFF1A1D28),
    outline = Color(0xFF2A2E3D),
    divider = Color(0xFF1F2230),
    textPrimary = Color(0xFFF5F6FA),
    textSecondary = Color(0xFFA3AAC0),
    textTertiary = Color(0xFF6B7290),
    primary = Color(0xFF7259FF),
    primarySoft = Color(0xFF231C4D),
    onPrimarySoft = Color(0xFFB9ABFF),
    success = Color(0xFF24D18C),
    successSoft = Color(0xFF0F2E22),
    warning = Color(0xFFFFB020),
    warningSoft = Color(0xFF33270D),
    error = Color(0xFFFF5A4E),
    errorSoft = Color(0xFF3A1513),
    info = Color(0xFF4CA3FF),
    infoSoft = Color(0xFF10243D),
    scrim = Color(0xB3000000),
    shimmer = Color(0xFF1F2230),
    keypadPressed = Color(0xFF1F2230),
)

val LocalZyroColors = staticCompositionLocalOf { LightZyroColors }

private fun ZyroColors.toMaterial(): ColorScheme = if (isDark) {
    darkColorScheme(
        primary = primary, onPrimary = Color.White, primaryContainer = primarySoft,
        onPrimaryContainer = onPrimarySoft, secondary = Brand.GradientMid, background = background,
        onBackground = textPrimary, surface = surface, onSurface = textPrimary,
        surfaceVariant = surfaceVariant, onSurfaceVariant = textSecondary, outline = outline,
        outlineVariant = divider, error = error, onError = Color.White,
        surfaceContainer = surface, surfaceContainerLow = surface, surfaceContainerHigh = surfaceElevated,
        surfaceContainerHighest = surfaceVariant, surfaceContainerLowest = background,
        scrim = Color.Black,
    )
} else {
    lightColorScheme(
        primary = primary, onPrimary = Color.White, primaryContainer = primarySoft,
        onPrimaryContainer = onPrimarySoft, secondary = Brand.GradientMid, background = background,
        onBackground = textPrimary, surface = surface, onSurface = textPrimary,
        surfaceVariant = surfaceVariant, onSurfaceVariant = textSecondary, outline = outline,
        outlineVariant = divider, error = error, onError = Color.White,
        surfaceContainer = surface, surfaceContainerLow = surface, surfaceContainerHigh = surface,
        surfaceContainerHighest = surfaceVariant, surfaceContainerLowest = background,
        scrim = Color.Black,
    )
}

@Composable
fun ZyroTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) DarkZyroColors else LightZyroColors
    CompositionLocalProvider(LocalZyroColors provides colors) {
        MaterialTheme(colorScheme = colors.toMaterial(), typography = ZyroMaterialTypography, content = content)
    }
}

/** Shorthand accessors: `Z.colors.textPrimary`, `Z.type.h2`. */
object Z {
    val colors: ZyroColors
        @Composable get() = LocalZyroColors.current
    val type: ZyroType
        get() = ZyroTypography
}
