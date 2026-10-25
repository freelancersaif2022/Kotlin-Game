package com.twinspace.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Brand {
    val Indigo = Color(0xFF5B6CFF)
    val Violet = Color(0xFF8E5CFF)
    val Mint = Color(0xFF3DDCB4)
    val Amber = Color(0xFFFFB547)
    val gradient = Brush.linearGradient(listOf(Indigo, Violet))
}

/** Colors the Material scheme doesn't cover. */
@Immutable
data class ExtraColors(
    val success: Color,
    val warning: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val iconPlate: Color,
)

val LocalExtraColors = staticCompositionLocalOf {
    ExtraColors(Brand.Mint, Brand.Amber, Brand.Indigo, Brand.Violet, Color.White)
}

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF8D9AFF),
    onPrimary = Color(0xFF0B0F2E),
    primaryContainer = Color(0xFF283078),
    onPrimaryContainer = Color(0xFFDEE1FF),
    secondary = Color(0xFF3DDCB4),
    onSecondary = Color(0xFF00382B),
    secondaryContainer = Color(0xFF0E3B33),
    onSecondaryContainer = Color(0xFFB4F5E2),
    tertiary = Color(0xFFBC9BFF),
    onTertiary = Color(0xFF2A0C66),
    background = Color(0xFF090C18),
    onBackground = Color(0xFFE7E9F5),
    surface = Color(0xFF090C18),
    onSurface = Color(0xFFE7E9F5),
    surfaceVariant = Color(0xFF1A1F38),
    onSurfaceVariant = Color(0xFF9DA3C0),
    surfaceContainerLowest = Color(0xFF070912),
    surfaceContainerLow = Color(0xFF0F1325),
    surfaceContainer = Color(0xFF13182D),
    surfaceContainerHigh = Color(0xFF191F38),
    surfaceContainerHighest = Color(0xFF212844),
    outline = Color(0xFF353C5E),
    outlineVariant = Color(0xFF232945),
    error = Color(0xFFFF6B7D),
    onError = Color(0xFF450010),
    errorContainer = Color(0xFF5C1020),
    onErrorContainer = Color(0xFFFFD9DD),
    inverseSurface = Color(0xFFE7E9F5),
    inverseOnSurface = Color(0xFF13182D),
    inversePrimary = Color(0xFF4F5BD5),
    scrim = Color(0xFF000000),
)

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF4A56D6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1E4FF),
    onPrimaryContainer = Color(0xFF111863),
    secondary = Color(0xFF0B9E80),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDF5EA),
    onSecondaryContainer = Color(0xFF00382B),
    tertiary = Color(0xFF7448DE),
    onTertiary = Color.White,
    background = Color(0xFFF5F6FB),
    onBackground = Color(0xFF11152A),
    surface = Color(0xFFF5F6FB),
    onSurface = Color(0xFF11152A),
    surfaceVariant = Color(0xFFE6E8F3),
    onSurfaceVariant = Color(0xFF5B6180),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFBFE),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF0F1F8),
    surfaceContainerHighest = Color(0xFFE8EAF4),
    outline = Color(0xFFC9CDDF),
    outlineVariant = Color(0xFFE3E5F0),
    error = Color(0xFFD3344B),
    onError = Color.White,
    errorContainer = Color(0xFFFFE0E4),
    onErrorContainer = Color(0xFF5C0A1A),
    inverseSurface = Color(0xFF1C2138),
    inverseOnSurface = Color(0xFFF1F2FA),
    inversePrimary = Color(0xFF8D9AFF),
    scrim = Color(0xFF000000),
)

private val AppTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
        bodyMedium = base.bodyMedium.copy(lineHeight = 21.sp),
    )
}

val LabelTiny = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun TwinSpaceTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val extra = if (dark) {
        ExtraColors(Color(0xFF3DDCB4), Color(0xFFFFC266), Color(0xFF3B47C9), Color(0xFF6D3FD6), Color(0xFF1C2240))
    } else {
        ExtraColors(Color(0xFF0B9E80), Color(0xFFE08A00), Color(0xFF4A56D6), Color(0xFF7D4DE8), Color.White)
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalExtraColors provides extra) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
