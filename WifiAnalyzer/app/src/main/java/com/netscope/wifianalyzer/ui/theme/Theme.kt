package com.netscope.wifianalyzer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Palette {
    val Navy950 = Color(0xFF060A17)
    val Navy900 = Color(0xFF0B1124)
    val Navy850 = Color(0xFF0F1730)
    val Navy800 = Color(0xFF141E3D)
    val Navy700 = Color(0xFF1C2852)
    val Cyan = Color(0xFF22D3EE)
    val Sky = Color(0xFF38BDF8)
    val Blue = Color(0xFF3B82F6)
    val Violet = Color(0xFF8B5CF6)
    val Lilac = Color(0xFFA78BFA)
    val Pink = Color(0xFFEC4899)
    val Green = Color(0xFF22C55E)
    val Mint = Color(0xFF34D399)
    val Amber = Color(0xFFF59E0B)
    val Gold = Color(0xFFFBBF24)
    val Orange = Color(0xFFFB923C)
    val Red = Color(0xFFEF4444)
    val Rose = Color(0xFFF43F5E)
}

@Immutable
data class ExtendedColors(
    val cardBorder: Color,
    val muted: Color,
    val subtle: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val gold: Color,
    val download: Color,
    val upload: Color,
    val ping: Color,
    val heroGradient: Brush,
    val primaryGradient: Brush,
    val premiumGradient: Brush,
    val isDark: Boolean,
)

val LocalExtendedColors = staticCompositionLocalOf<ExtendedColors> { error("No colors provided") }

private val DarkScheme = darkColorScheme(
    primary = Palette.Cyan,
    onPrimary = Palette.Navy950,
    primaryContainer = Color(0xFF0E3A4A),
    onPrimaryContainer = Color(0xFFCFFAFE),
    secondary = Palette.Lilac,
    onSecondary = Palette.Navy950,
    secondaryContainer = Color(0xFF2E2360),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = Palette.Gold,
    background = Palette.Navy950,
    onBackground = Color(0xFFE8ECF8),
    surface = Palette.Navy900,
    onSurface = Color(0xFFE8ECF8),
    surfaceVariant = Palette.Navy800,
    onSurfaceVariant = Color(0xFFA9B4D0),
    surfaceContainer = Palette.Navy850,
    surfaceContainerHigh = Palette.Navy800,
    surfaceContainerHighest = Palette.Navy700,
    surfaceContainerLow = Palette.Navy900,
    outline = Color(0xFF2B3866),
    outlineVariant = Color(0xFF1E2A50),
    error = Palette.Rose,
    inverseSurface = Color(0xFFE8ECF8),
    inverseOnSurface = Palette.Navy900,
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0891B2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFFAFE),
    onPrimaryContainer = Color(0xFF083344),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF2E1065),
    tertiary = Color(0xFFD97706),
    background = Color(0xFFF4F6FC),
    onBackground = Color(0xFF0B1124),
    surface = Color.White,
    onSurface = Color(0xFF0B1124),
    surfaceVariant = Color(0xFFEDF1FA),
    onSurfaceVariant = Color(0xFF55607D),
    surfaceContainer = Color(0xFFF8FAFF),
    surfaceContainerHigh = Color(0xFFEEF2FB),
    surfaceContainerHighest = Color(0xFFE4E9F5),
    surfaceContainerLow = Color.White,
    outline = Color(0xFFD3DAEA),
    outlineVariant = Color(0xFFE3E8F3),
    error = Color(0xFFDC2626),
)

private val AppTypography = Typography().let { t ->
    val f = FontFamily.SansSerif
    t.copy(
        displayLarge = TextStyle(fontFamily = f, fontWeight = FontWeight.Bold, fontSize = 56.sp, letterSpacing = (-1.5).sp),
        displayMedium = TextStyle(fontFamily = f, fontWeight = FontWeight.Bold, fontSize = 44.sp, letterSpacing = (-1).sp),
        displaySmall = TextStyle(fontFamily = f, fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = (-0.5).sp),
        headlineLarge = TextStyle(fontFamily = f, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.5).sp),
        headlineMedium = TextStyle(fontFamily = f, fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = (-0.3).sp),
        headlineSmall = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
        titleLarge = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        titleMedium = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.1.sp),
        titleSmall = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp),
        bodyLarge = TextStyle(fontFamily = f, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = f, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = f, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = TextStyle(fontFamily = f, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.2.sp),
        labelMedium = TextStyle(fontFamily = f, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.3.sp),
        labelSmall = TextStyle(fontFamily = f, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.4.sp),
    )
}

@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    val ext = if (dark) {
        ExtendedColors(
            cardBorder = Color(0x1FFFFFFF),
            muted = Color(0xFFA9B4D0),
            subtle = Color(0xFF6B7899),
            success = Palette.Mint,
            warning = Palette.Gold,
            danger = Palette.Rose,
            gold = Palette.Gold,
            download = Palette.Cyan,
            upload = Palette.Lilac,
            ping = Palette.Gold,
            heroGradient = Brush.linearGradient(listOf(Color(0xFF1A2A6C), Color(0xFF11185A), Color(0xFF2A1260))),
            primaryGradient = Brush.horizontalGradient(listOf(Palette.Cyan, Palette.Blue, Palette.Violet)),
            premiumGradient = Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFF97316), Color(0xFFEC4899))),
            isDark = true,
        )
    } else {
        ExtendedColors(
            cardBorder = Color(0x14000000),
            muted = Color(0xFF55607D),
            subtle = Color(0xFF8A94AD),
            success = Color(0xFF16A34A),
            warning = Color(0xFFD97706),
            danger = Color(0xFFDC2626),
            gold = Color(0xFFD97706),
            download = Color(0xFF0891B2),
            upload = Color(0xFF7C3AED),
            ping = Color(0xFFD97706),
            heroGradient = Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF4F46E5), Color(0xFF7C3AED))),
            primaryGradient = Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF8B5CF6))),
            premiumGradient = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFF97316), Color(0xFFEC4899))),
            isDark = false,
        )
    }
    CompositionLocalProvider(LocalExtendedColors provides ext) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            content = content,
        )
    }
}

object AppColors {
    val ext: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
