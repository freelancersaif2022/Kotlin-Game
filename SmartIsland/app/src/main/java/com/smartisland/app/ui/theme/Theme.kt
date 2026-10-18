package com.smartisland.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Palette {
    val Background = Color(0xFF07070B)
    val Surface = Color(0xFF12121A)
    val SurfaceHigh = Color(0xFF1A1A25)
    val Outline = Color(0x14FFFFFF)
    val Primary = Color(0xFF7C5CFF)
    val PrimarySoft = Color(0xFFB9A6FF)
    val Secondary = Color(0xFF0A84FF)
    val Cyan = Color(0xFF64D2FF)
    val Success = Color(0xFF30D158)
    val Warning = Color(0xFFFF9F0A)
    val Error = Color(0xFFFF453A)
    val Gold = Color(0xFFFFCC4D)
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFF9A9AA6)
    val TextTertiary = Color(0xFF63636E)

    val PrimaryGradient = Brush.linearGradient(listOf(Color(0xFF8E72FF), Color(0xFF5B3DF5)))
    val GoldGradient = Brush.linearGradient(listOf(Color(0xFFFFE08A), Color(0xFFFFB340)))
    val HeroGradient = Brush.verticalGradient(listOf(Color(0xFF241A52), Color(0xFF0E0B1F), Background))
    val WallpaperGradient = Brush.linearGradient(listOf(Color(0xFF3B2A86), Color(0xFF13203F), Color(0xFF0B0B12)))
}

private val Scheme = darkColorScheme(
    primary = Palette.Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2C2166),
    onPrimaryContainer = Palette.PrimarySoft,
    secondary = Palette.Secondary,
    onSecondary = Color.White,
    tertiary = Palette.Gold,
    background = Palette.Background,
    onBackground = Palette.TextPrimary,
    surface = Palette.Surface,
    onSurface = Palette.TextPrimary,
    surfaceVariant = Palette.SurfaceHigh,
    onSurfaceVariant = Palette.TextSecondary,
    surfaceContainer = Palette.Surface,
    surfaceContainerHigh = Palette.SurfaceHigh,
    surfaceContainerHighest = Color(0xFF242432),
    outline = Color(0x29FFFFFF),
    outlineVariant = Palette.Outline,
    error = Palette.Error,
)

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
fun SmartIslandTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = AppTypography, content = content)
}
