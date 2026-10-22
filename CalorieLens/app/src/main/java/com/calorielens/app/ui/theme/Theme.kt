package com.calorielens.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calorielens.app.domain.ThemeMode

object Brand {
    val Mint = Color(0xFF2EE6A6)
    val Cyan = Color(0xFF22C3F0)
    val Ember = Color(0xFFFF6A3D)
    val Amber = Color(0xFFFFB547)
    val Protein = Color(0xFFFF5D8F)
    val Carbs = Color(0xFFFFC247)
    val Fat = Color(0xFF8B7CFF)
    val Fiber = Color(0xFF2EE6A6)
    val Danger = Color(0xFFFF5470)
    val Success = Color(0xFF2EE6A6)
    val Info = Color(0xFF5B8CFF)
    val Warning = Color(0xFFFFB547)

    val GoldLight = Color(0xFFFFE7A3)
    val Gold = Color(0xFFFFC94D)
    val GoldDeep = Color(0xFFF59E0B)
    val GoldDark = Color(0xFFB8741A)

    val primaryGradient = Brush.linearGradient(listOf(Mint, Cyan))
    val calorieGradient = Brush.linearGradient(listOf(Amber, Ember))
    val goldGradient = Brush.linearGradient(listOf(GoldLight, Gold, GoldDeep))
}

@Immutable
data class ExtendedColors(
    val card: Color,
    val cardElevated: Color,
    val stroke: Color,
    val muted: Color,
    val faint: Color,
    val isDark: Boolean,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(Color(0xFF111318), Color(0xFF181B22), Color(0xFF242833), Color(0xFF9AA3B2), Color(0xFF5E6675), true)
}

private val DarkScheme = darkColorScheme(
    primary = Brand.Mint,
    onPrimary = Color(0xFF00241A),
    primaryContainer = Color(0xFF0E3B2E),
    onPrimaryContainer = Brand.Mint,
    secondary = Brand.Cyan,
    onSecondary = Color(0xFF00202B),
    tertiary = Brand.Amber,
    background = Color(0xFF07080B),
    onBackground = Color(0xFFF2F4F8),
    surface = Color(0xFF0D0F13),
    onSurface = Color(0xFFF2F4F8),
    surfaceVariant = Color(0xFF181B22),
    onSurfaceVariant = Color(0xFF9AA3B2),
    surfaceContainer = Color(0xFF111318),
    surfaceContainerHigh = Color(0xFF181B22),
    surfaceContainerHighest = Color(0xFF1F232B),
    surfaceContainerLow = Color(0xFF0D0F13),
    outline = Color(0xFF2A2F3A),
    outlineVariant = Color(0xFF1F232B),
    error = Brand.Danger,
    inverseSurface = Color(0xFFF2F4F8),
    inverseOnSurface = Color(0xFF111318),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0FB57E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4F8EA),
    onPrimaryContainer = Color(0xFF00382A),
    secondary = Color(0xFF0E9FD0),
    onSecondary = Color.White,
    tertiary = Color(0xFFE08A12),
    background = Color(0xFFF4F6FA),
    onBackground = Color(0xFF0E1116),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E1116),
    surfaceVariant = Color(0xFFEDF0F5),
    onSurfaceVariant = Color(0xFF5D6676),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF1F3F7),
    surfaceContainerHighest = Color(0xFFE8EBF1),
    surfaceContainerLow = Color(0xFFF7F8FB),
    outline = Color(0xFFD9DEE7),
    outlineVariant = Color(0xFFE6E9EF),
    error = Color(0xFFE5354F),
    inverseSurface = Color(0xFF15181E),
    inverseOnSurface = Color(0xFFF2F4F8),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Black, fontSize = 52.sp, letterSpacing = (-1.5).sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, letterSpacing = (-1).sp),
    displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 0.8.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun isAppInDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun CalorieLensTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val extended = if (darkTheme) {
        ExtendedColors(
            card = Color(0xFF111318),
            cardElevated = Color(0xFF181B22),
            stroke = Color(0xFF232733),
            muted = Color(0xFF9AA3B2),
            faint = Color(0xFF5E6675),
            isDark = true,
        )
    } else {
        ExtendedColors(
            card = Color(0xFFFFFFFF),
            cardElevated = Color(0xFFF7F8FB),
            stroke = Color(0xFFE3E7EE),
            muted = Color(0xFF5D6676),
            faint = Color(0xFF9AA3B2),
            isDark = false,
        )
    }
    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
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
