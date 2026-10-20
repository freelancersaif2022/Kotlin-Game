package com.tvremote.universal.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Brand palette taken from the app icon: electric blue + cyan glow on deep navy.
val Brand = Color(0xFF1F5BFF)
val BrandCyan = Color(0xFF00B2FF)
val Navy = Color(0xFF020A28)
val NavyDeep = Color(0xFF010619)
val PowerRed = Color(0xFFF04438)
val Success = Color(0xFF12B76A)
val Warning = Color(0xFFF79009)
val Gold = Color(0xFFF5B400)
val GoldDeep = Color(0xFFE08A00)

private val LightScheme = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3EBFF),
    onPrimaryContainer = Color(0xFF071A66),
    secondary = Color(0xFF0091D5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF3FF),
    onSecondaryContainer = Color(0xFF003550),
    tertiary = Success,
    background = Color(0xFFF4F6FB),
    onBackground = Color(0xFF0E1525),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E1525),
    surfaceVariant = Color(0xFFECEFF6),
    onSurfaceVariant = Color(0xFF586379),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F9FD),
    surfaceContainer = Color(0xFFF1F3F9),
    surfaceContainerHigh = Color(0xFFEAEDF5),
    surfaceContainerHighest = Color(0xFFE2E6F0),
    inverseSurface = Color(0xFF151C2C),
    inverseOnSurface = Color(0xFFF1F4FA),
    outline = Color(0xFFCDD3E1),
    outlineVariant = Color(0xFFE3E7F0),
    error = PowerRed,
    onError = Color.White,
    errorContainer = Color(0xFFFEE4E2),
    onErrorContainer = Color(0xFF7A271A),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF5B8CFF),
    onPrimary = Color(0xFF041450),
    primaryContainer = Color(0xFF14286E),
    onPrimaryContainer = Color(0xFFDCE5FF),
    secondary = Color(0xFF3CC8FF),
    onSecondary = Color(0xFF00283D),
    secondaryContainer = Color(0xFF0B3A57),
    onSecondaryContainer = Color(0xFFD5F1FF),
    tertiary = Color(0xFF32D583),
    background = Color(0xFF060B18),
    onBackground = Color(0xFFEDF1F9),
    surface = Color(0xFF0E1526),
    onSurface = Color(0xFFEDF1F9),
    surfaceVariant = Color(0xFF172036),
    onSurfaceVariant = Color(0xFFA2ADC2),
    surfaceContainerLowest = Color(0xFF080D1A),
    surfaceContainerLow = Color(0xFF0C1322),
    surfaceContainer = Color(0xFF111A2D),
    surfaceContainerHigh = Color(0xFF162036),
    surfaceContainerHighest = Color(0xFF1C2740),
    inverseSurface = Color(0xFFE8ECF5),
    inverseOnSurface = Color(0xFF111826),
    outline = Color(0xFF34405A),
    outlineVariant = Color(0xFF222C43),
    error = Color(0xFFFF6B5F),
    onError = Color(0xFF4A0C06),
    errorContainer = Color(0xFF4F1610),
    onErrorContainer = Color(0xFFFFDAD5),
)

@Immutable
data class ExtraColors(
    val isDark: Boolean,
    val keyTop: Color,
    val keyBottom: Color,
    val keyBorder: Color,
    val keyIcon: Color,
    val keyShadow: Color,
    val keyHighlight: Color,
    val panel: Color,
    val panelBorder: Color,
    val power: Color,
    val success: Color,
    val warning: Color,
    val gold: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val glow: Color,
    val snackbar: Color,
    val onSnackbar: Color,
)

private val LightExtra = ExtraColors(
    isDark = false,
    keyTop = Color(0xFFFFFFFF),
    keyBottom = Color(0xFFEFF2F8),
    keyBorder = Color(0xFFDDE2EC),
    keyIcon = Color(0xFF1A2233),
    keyShadow = Color(0x2A1A2B5C),
    keyHighlight = Color(0xCCFFFFFF),
    panel = Color(0xFFFFFFFF),
    panelBorder = Color(0xFFE5E9F2),
    power = PowerRed,
    success = Success,
    warning = Warning,
    gold = Gold,
    heroStart = Color(0xFF0A1A5C),
    heroEnd = Brand,
    glow = Color(0xFF2F7BFF),
    snackbar = Color(0xFF141B2B),
    onSnackbar = Color(0xFFF3F6FC),
)

private val DarkExtra = ExtraColors(
    isDark = true,
    keyTop = Color(0xFF1E2638),
    keyBottom = Color(0xFF0F1524),
    keyBorder = Color(0xFF2B3650),
    keyIcon = Color(0xFFEAF0FA),
    keyShadow = Color(0x99000000),
    keyHighlight = Color(0x22FFFFFF),
    panel = Color(0xFF0F1729),
    panelBorder = Color(0xFF1E2942),
    power = Color(0xFFFF5A4E),
    success = Color(0xFF32D583),
    warning = Color(0xFFFDB022),
    gold = Color(0xFFFFC53D),
    heroStart = Color(0xFF020A28),
    heroEnd = Color(0xFF1946D9),
    glow = Color(0xFF3D8BFF),
    snackbar = Color(0xFFE9EEF8),
    onSnackbar = Color(0xFF0E1525),
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
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge,
        bodyMedium = base.bodyMedium,
        bodySmall = base.bodySmall,
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
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
    val scheme = if (dark) DarkScheme else LightScheme
    MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes) {
        // Every Text/Icon outside a Surface inherits the correct color for the theme.
        CompositionLocalProvider(
            LocalExtraColors provides if (dark) DarkExtra else LightExtra,
            LocalContentColor provides scheme.onBackground,
            content = content,
        )
    }
}

val MonoDigits = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = 1.sp)
