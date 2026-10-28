package com.inkly.notes.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Brand {
    val Indigo = Color(0xFF4F46E5)
    val IndigoLight = Color(0xFF6D5EF5)
    val IndigoDeep = Color(0xFF3730A3)
    val Amber = Color(0xFFF59E0B)
    val Success = Color(0xFF16A34A)
}

private val LightColors = lightColorScheme(
    primary = Brand.Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Brand.Amber,
    onSecondary = Color(0xFF1F1300),
    secondaryContainer = Color(0xFFFFF1D6),
    onSecondaryContainer = Color(0xFF3D2800),
    tertiary = Color(0xFF0EA5E9),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF6F7FB),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFECEEF5),
    onSurfaceVariant = Color(0xFF64748B),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF1F3F9),
    surfaceContainerHighest = Color(0xFFE9ECF4),
    outline = Color(0xFFCBD2E1),
    outlineVariant = Color(0xFFE3E7F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    inverseSurface = Color(0xFF1E2230),
    inverseOnSurface = Color(0xFFF1F5F9),
    inversePrimary = Color(0xFFA5B4FC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF2B1D00),
    secondaryContainer = Color(0xFF4A3510),
    onSecondaryContainer = Color(0xFFFFE8B3),
    tertiary = Color(0xFF38BDF8),
    background = Color(0xFF0E1016),
    onBackground = Color(0xFFE8EAF0),
    surface = Color(0xFF0E1016),
    onSurface = Color(0xFFE8EAF0),
    surfaceVariant = Color(0xFF222736),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF171B26),
    surfaceContainerLow = Color(0xFF171B26),
    surfaceContainer = Color(0xFF1A1E2A),
    surfaceContainerHigh = Color(0xFF222736),
    surfaceContainerHighest = Color(0xFF2A3040),
    outline = Color(0xFF3A4152),
    outlineVariant = Color(0xFF2A3040),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF5B1A1A),
    onErrorContainer = Color(0xFFFECACA),
    inverseSurface = Color(0xFFE8EAF0),
    inverseOnSurface = Color(0xFF1A1E2A),
    inversePrimary = Brand.Indigo,
)

private val base = Typography()
private val InklyTypography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    bodyMedium = base.bodyMedium.copy(lineHeight = 21.sp),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
)

private val InklyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

val LocalDarkTheme = staticCompositionLocalOf { false }

/** Note tints: index 0 is the neutral card colour. */
object NoteColors {
    private val light = listOf(
        Color.Unspecified, Color(0xFFFFF4D6), Color(0xFFFFE4EC), Color(0xFFDDF7EA),
        Color(0xFFDDEFFE), Color(0xFFECE6FF),
    )
    private val dark = listOf(
        Color.Unspecified, Color(0xFF3A3020), Color(0xFF3A2230), Color(0xFF1E3329),
        Color(0xFF1C2C3D), Color(0xFF2A2540),
    )
    val swatches = listOf(
        Color(0xFFFFFFFF), Color(0xFFFFD873), Color(0xFFFF9EBB), Color(0xFF7EE0B0),
        Color(0xFF8CC8FF), Color(0xFFB9A6FF),
    )
    val count get() = light.size

    @Composable
    fun card(index: Int): Color {
        val list = if (LocalDarkTheme.current) dark else light
        val c = list.getOrElse(index) { Color.Unspecified }
        return if (c == Color.Unspecified) MaterialTheme.colorScheme.surfaceContainerLowest else c
    }

    @Composable
    fun page(index: Int): Color =
        if (index == 0) MaterialTheme.colorScheme.background else card(index)
}

@Composable
fun InklyTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = InklyTypography,
            shapes = InklyShapes,
            content = content,
        )
    }
}
