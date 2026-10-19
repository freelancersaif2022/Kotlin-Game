package com.eyescroll.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object EsColors {
    val Background = Color(0xFF070B14)
    val Surface = Color(0xFF0F1626)
    val SurfaceHigh = Color(0xFF16203A)
    val Stroke = Color(0x1FFFFFFF)
    val Primary = Color(0xFF3DDCFF)
    val Secondary = Color(0xFF7C5CFF)
    val Success = Color(0xFF2EE6A6)
    val Warning = Color(0xFFFFB547)
    val Danger = Color(0xFFFF5C7A)
    val Text = Color(0xFFE8EEF9)
    val TextMuted = Color(0xFF8A97B2)
    val TextFaint = Color(0xFF5A6682)

    val Accent = Brush.linearGradient(listOf(Primary, Color(0xFF4F8CFF), Secondary))
    val AccentSoft = Brush.linearGradient(listOf(Primary.copy(alpha = 0.18f), Secondary.copy(alpha = 0.18f)))
}

private val scheme = darkColorScheme(
    primary = EsColors.Primary,
    onPrimary = Color(0xFF00141C),
    secondary = EsColors.Secondary,
    onSecondary = Color.White,
    tertiary = EsColors.Success,
    background = EsColors.Background,
    onBackground = EsColors.Text,
    surface = EsColors.Surface,
    onSurface = EsColors.Text,
    surfaceVariant = EsColors.SurfaceHigh,
    onSurfaceVariant = EsColors.TextMuted,
    outline = EsColors.Stroke,
    error = EsColors.Danger,
)

private val typography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp),
)

@Composable
fun EyeScrollTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
