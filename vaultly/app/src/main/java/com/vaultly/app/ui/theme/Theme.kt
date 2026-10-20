package com.vaultly.app.ui.theme

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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultly.app.R

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

object Palette {
    val Ink = Color(0xFF0E1626)
    val Ink2 = Color(0xFF1C2B48)
    val InkDeep = Color(0xFF070C16)
    val Gold = Color(0xFFC9A35A)
    val GoldLight = Color(0xFFEBD49A)
    val GoldDeep = Color(0xFFA67A30)
    val Ivory = Color(0xFFF6F4EE)
    val Green = Color(0xFF1C8C60)
    val Amber = Color(0xFFC88208)
    val Red = Color(0xFFD04545)
}

/** Colours outside the Material scheme: deadline states and the premium "ink card". */
@Immutable
data class VaultlyColors(
    val success: Color,
    val warning: Color,
    val danger: Color,
    val muted: Color,
    val gold: Color,
    val hairline: Color,
    val card: Color,
    val inkBrush: Brush,
    val goldBrush: Brush,
    val dark: Boolean,
)

val LocalVaultlyColors = staticCompositionLocalOf {
    VaultlyColors(
        Palette.Green, Palette.Amber, Palette.Red, Color.Gray, Palette.Gold, Color.LightGray, Color.White,
        Brush.linearGradient(listOf(Palette.Ink2, Palette.Ink)), Brush.linearGradient(listOf(Palette.GoldLight, Palette.Gold)), false,
    )
}

private val LightScheme = lightColorScheme(
    primary = Palette.Ink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E9F0),
    onPrimaryContainer = Palette.Ink,
    secondary = Palette.GoldDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4E9CF),
    onSecondaryContainer = Color(0xFF4A3410),
    background = Palette.Ivory,
    onBackground = Palette.Ink,
    surface = Palette.Ivory,
    onSurface = Palette.Ink,
    surfaceVariant = Color(0xFFEDEAE2),
    onSurfaceVariant = Color(0xFF666C7A),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFAF7),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFEDEAE2),
    outline = Color(0xFFD9D5CA),
    outlineVariant = Color(0xFFE7E3DA),
    error = Palette.Red,
)

private val DarkScheme = darkColorScheme(
    primary = Palette.GoldLight,
    onPrimary = Palette.Ink,
    primaryContainer = Color(0xFF2A3550),
    onPrimaryContainer = Color(0xFFF2EFE8),
    secondary = Palette.Gold,
    onSecondary = Palette.Ink,
    secondaryContainer = Color(0xFF3A3020),
    onSecondaryContainer = Palette.GoldLight,
    background = Color(0xFF0A0F1A),
    onBackground = Color(0xFFF2EFE8),
    surface = Color(0xFF0A0F1A),
    onSurface = Color(0xFFF2EFE8),
    surfaceVariant = Color(0xFF1A2336),
    onSurfaceVariant = Color(0xFF9AA3B5),
    surfaceContainerLowest = Color(0xFF0D1320),
    surfaceContainerLow = Color(0xFF111827),
    surfaceContainer = Color(0xFF131B2B),
    surfaceContainerHigh = Color(0xFF172033),
    surfaceContainerHighest = Color(0xFF1C263B),
    outline = Color(0xFF2E3953),
    outlineVariant = Color(0xFF222C42),
    error = Color(0xFFFF7A7A),
)

private fun type(): Typography {
    fun s(size: Int, weight: FontWeight, line: Int, spacing: Double = 0.0) =
        TextStyle(fontFamily = Manrope, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)
    return Typography(
        displayLarge = s(48, FontWeight.ExtraBold, 54, -1.5),
        displayMedium = s(38, FontWeight.ExtraBold, 44, -1.0),
        displaySmall = s(32, FontWeight.Bold, 38, -0.8),
        headlineLarge = s(28, FontWeight.Bold, 34, -0.6),
        headlineMedium = s(24, FontWeight.Bold, 30, -0.4),
        headlineSmall = s(20, FontWeight.Bold, 26, -0.2),
        titleLarge = s(18, FontWeight.Bold, 24, -0.2),
        titleMedium = s(16, FontWeight.SemiBold, 22),
        titleSmall = s(14, FontWeight.SemiBold, 20),
        bodyLarge = s(16, FontWeight.Medium, 24),
        bodyMedium = s(14, FontWeight.Medium, 20),
        bodySmall = s(12, FontWeight.Medium, 16),
        labelLarge = s(15, FontWeight.Bold, 20, 0.1),
        labelMedium = s(12, FontWeight.SemiBold, 16, 0.2),
        labelSmall = s(11, FontWeight.Bold, 14, 0.8),
    )
}

@Composable
fun VaultlyTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val scheme = if (dark) DarkScheme else LightScheme
    val extra = if (dark) VaultlyColors(
        success = Color(0xFF3CC28C), warning = Color(0xFFF0B341), danger = Color(0xFFFF6B6B),
        muted = Color(0xFF8D96A8), gold = Palette.GoldLight, hairline = Color(0xFF222C42), card = Color(0xFF131B2B),
        inkBrush = Brush.linearGradient(listOf(Color(0xFF22325A), Color(0xFF111A2E), Color(0xFF0B1220))),
        goldBrush = Brush.linearGradient(listOf(Palette.GoldLight, Palette.Gold, Palette.GoldDeep)),
        dark = true,
    ) else VaultlyColors(
        success = Palette.Green, warning = Palette.Amber, danger = Palette.Red,
        muted = Color(0xFF7A808E), gold = Palette.GoldDeep, hairline = Color(0xFFE7E3DA), card = Color.White,
        inkBrush = Brush.linearGradient(listOf(Color(0xFF233559), Palette.Ink, Palette.InkDeep)),
        goldBrush = Brush.linearGradient(listOf(Palette.GoldLight, Palette.Gold, Palette.GoldDeep)),
        dark = false,
    )
    CompositionLocalProvider(LocalVaultlyColors provides extra) {
        MaterialTheme(
            colorScheme = scheme,
            typography = type(),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(22.dp),
                extraLarge = RoundedCornerShape(28.dp),
            ),
            content = content,
        )
    }
}

object VaultlyTheme {
    val colors: VaultlyColors @Composable get() = LocalVaultlyColors.current
}
