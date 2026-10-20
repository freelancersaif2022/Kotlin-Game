package com.tvremote.universal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Brand = Color(0xFF2F54EB)
val BrandLight = Color(0xFF5B7BFF)
val BrandDeep = Color(0xFF1D39C4)
val Violet = Color(0xFF7A5AF8)
val PowerRed = Color(0xFFF04438)
val Success = Color(0xFF12B76A)
val Warning = Color(0xFFF79009)
val Gold = Color(0xFFF5B400)

private val LightScheme = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6EBFF),
    onPrimaryContainer = Color(0xFF0A1C6B),
    secondary = Violet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE7FF),
    onSecondaryContainer = Color(0xFF2A1670),
    tertiary = Success,
    background = Color(0xFFF3F5FA),
    onBackground = Color(0xFF101828),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101828),
    surfaceVariant = Color(0xFFEEF1F7),
    onSurfaceVariant = Color(0xFF5D6679),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F9FC),
    surfaceContainer = Color(0xFFF1F3F9),
    surfaceContainerHigh = Color(0xFFEAEDF5),
    surfaceContainerHighest = Color(0xFFE3E7F0),
    outline = Color(0xFFD0D5E2),
    outlineVariant = Color(0xFFE4E7EF),
    error = PowerRed,
    onError = Color.White,
    errorContainer = Color(0xFFFEE4E2),
    onErrorContainer = Color(0xFF7A271A),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF7090FF),
    onPrimary = Color(0xFF071660),
    primaryContainer = Color(0xFF1C2D7A),
    onPrimaryContainer = Color(0xFFDDE4FF),
    secondary = Color(0xFF9E87FF),
    onSecondary = Color(0xFF1E0C5C),
    secondaryContainer = Color(0xFF34237A),
    onSecondaryContainer = Color(0xFFE9E2FF),
    tertiary = Color(0xFF32D583),
    background = Color(0xFF0B0E14),
    onBackground = Color(0xFFEEF1F7),
    surface = Color(0xFF141821),
    onSurface = Color(0xFFEEF1F7),
    surfaceVariant = Color(0xFF1D222D),
    onSurfaceVariant = Color(0xFF9BA4B5),
    surfaceContainerLowest = Color(0xFF0E1118),
    surfaceContainerLow = Color(0xFF12161E),
    surfaceContainer = Color(0xFF181C25),
    surfaceContainerHigh = Color(0xFF1E232E),
    surfaceContainerHighest = Color(0xFF252B37),
    outline = Color(0xFF353C4A),
    outlineVariant = Color(0xFF262C38),
    error = Color(0xFFFF6B5F),
    onError = Color(0xFF4A0C06),
    errorContainer = Color(0xFF55160C),
    onErrorContainer = Color(0xFFFFDAD5),
)

@Immutable
data class ExtraColors(
    val keyTop: Color,
    val keyBottom: Color,
    val keyBorder: Color,
    val keyIcon: Color,
    val keyShadow: Color,
    val panel: Color,
    val panelBorder: Color,
    val power: Color,
    val success: Color,
    val warning: Color,
    val gold: Color,
    val heroStart: Color,
    val heroEnd: Color,
)

private val LightExtra = ExtraColors(
    keyTop = Color(0xFFFFFFFF),
    keyBottom = Color(0xFFF2F4F9),
    keyBorder = Color(0xFFE2E6EF),
    keyIcon = Color(0xFF1D2433),
    keyShadow = Color(0x1F1A2B5C),
    panel = Color(0xFFFFFFFF),
    panelBorder = Color(0xFFE6E9F1),
    power = PowerRed,
    success = Success,
    warning = Warning,
    gold = Gold,
    heroStart = Brand,
    heroEnd = Violet,
)

private val DarkExtra = ExtraColors(
    keyTop = Color(0xFF242A36),
    keyBottom = Color(0xFF1A1F29),
    keyBorder = Color(0xFF2E3542),
    keyIcon = Color(0xFFE8ECF4),
    keyShadow = Color(0x66000000),
    panel = Color(0xFF151A23),
    panelBorder = Color(0xFF232937),
    power = Color(0xFFFF5A4E),
    success = Color(0xFF32D583),
    warning = Color(0xFFFDB022),
    gold = Color(0xFFFFC53D),
    heroStart = Color(0xFF3D5CF5),
    heroEnd = Color(0xFF7A5AF8),
)

val LocalExtraColors = staticCompositionLocalOf { LightExtra }

object AppTheme {
    val extra: ExtraColors
        @Composable get() = LocalExtraColors.current
}

private val AppTypography = Typography().let { base ->
    Typography(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge,
        bodyMedium = base.bodyMedium,
        bodySmall = base.bodySmall,
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalExtraColors provides if (dark) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

val MonoDigits = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = 2.sp)
