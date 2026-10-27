package com.pdfstudio.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Brand {
    val Coral = Color(0xFFFF5A5F)
    val Magenta = Color(0xFFB5179E)
    val gradient = Brush.linearGradient(listOf(Coral, Magenta))

    val SplitColors = listOf(Color(0xFFFF7A59), Color(0xFFE5284F))
    val CropColors = listOf(Color(0xFF8B5CF6), Color(0xFF5B3FD6))
    val ConvertColors = listOf(Color(0xFF14C3A2), Color(0xFF0E8F87))
}

private val LightColors = lightColorScheme(
    primary = Color(0xFFDA2449),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDE3),
    onPrimaryContainer = Color(0xFF3F0012),
    secondary = Color(0xFF9E1C8C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFBD7F2),
    onSecondaryContainer = Color(0xFF38002F),
    background = Color(0xFFF5F6FA),
    onBackground = Color(0xFF14161B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14161B),
    surfaceVariant = Color(0xFFECEEF3),
    onSurfaceVariant = Color(0xFF5A5F6C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF7F8FB),
    surfaceContainerHigh = Color(0xFFF0F1F5),
    surfaceContainerHighest = Color(0xFFE8EAF0),
    outline = Color(0xFFC6CAD4),
    outlineVariant = Color(0xFFE1E4EB),
    error = Color(0xFFD32F2F),
    inverseSurface = Color(0xFF22252C),
    inverseOnSurface = Color(0xFFF1F2F6),
    inversePrimary = Color(0xFFFF8FA3),
    scrim = Color(0xFF000000),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF6F8A),
    onPrimary = Color(0xFF4A0014),
    primaryContainer = Color(0xFF6E0E28),
    onPrimaryContainer = Color(0xFFFFDDE3),
    secondary = Color(0xFFF08BDD),
    onSecondary = Color(0xFF4A003E),
    secondaryContainer = Color(0xFF5E0F52),
    onSecondaryContainer = Color(0xFFFBD7F2),
    background = Color(0xFF0D0E12),
    onBackground = Color(0xFFEDEEF2),
    surface = Color(0xFF16181E),
    onSurface = Color(0xFFEDEEF2),
    surfaceVariant = Color(0xFF23262E),
    onSurfaceVariant = Color(0xFFAEB3C0),
    surfaceContainerLowest = Color(0xFF0B0C10),
    surfaceContainerLow = Color(0xFF16181E),
    surfaceContainer = Color(0xFF1B1D24),
    surfaceContainerHigh = Color(0xFF22252D),
    surfaceContainerHighest = Color(0xFF2B2E37),
    outline = Color(0xFF454A56),
    outlineVariant = Color(0xFF2C3039),
    error = Color(0xFFFF6B6B),
    inverseSurface = Color(0xFFEDEEF2),
    inverseOnSurface = Color(0xFF1B1D24),
    inversePrimary = Color(0xFFDA2449),
    scrim = Color(0xFF000000),
)

private val AppTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
        labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp),
    )
}

@Composable
private fun animateColor(target: Color): Color {
    val color by animateColorAsState(target, animationSpec = tween(450), label = "theme")
    return color
}

/** Animates the key colors so switching dark / light mode cross-fades smoothly. */
@Composable
private fun ColorScheme.animated(): ColorScheme = copy(
    primary = animateColor(primary),
    onPrimary = animateColor(onPrimary),
    primaryContainer = animateColor(primaryContainer),
    onPrimaryContainer = animateColor(onPrimaryContainer),
    background = animateColor(background),
    onBackground = animateColor(onBackground),
    surface = animateColor(surface),
    onSurface = animateColor(onSurface),
    surfaceVariant = animateColor(surfaceVariant),
    onSurfaceVariant = animateColor(onSurfaceVariant),
    surfaceContainerLow = animateColor(surfaceContainerLow),
    surfaceContainer = animateColor(surfaceContainer),
    surfaceContainerHigh = animateColor(surfaceContainerHigh),
    surfaceContainerHighest = animateColor(surfaceContainerHighest),
    outline = animateColor(outline),
    outlineVariant = animateColor(outlineVariant),
)

@Composable
fun PdfStudioTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = (if (darkTheme) DarkColors else LightColors).animated(),
        typography = AppTypography,
        content = content,
    )
}
