package com.spectral.ghostdetector.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.spectral.ghostdetector.R

object GColors {
    val Void = Color(0xFF020604)
    val Deep = Color(0xFF06110C)
    val Panel = Color(0xFF0A1611)
    val PanelHi = Color(0xFF10221A)
    val Neon = Color(0xFF39FF88)
    val NeonSoft = Color(0xFF7DFFB5)
    val NeonDim = Color(0xFF1C8F52)
    val Stroke = Color(0x4439FF88)
    val Amber = Color(0xFFFFC048)
    val Danger = Color(0xFFFF3B4E)
    val TextPrimary = Color(0xFFE8FFF1)
    val TextSecondary = Color(0xFF8FB8A0)
    val TextMuted = Color(0xFF557566)
}

@OptIn(ExperimentalTextApi::class)
object GFonts {
    val Orbitron = FontFamily(
        Font(R.font.orbitron, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        Font(R.font.orbitron, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
        Font(R.font.orbitron, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
        Font(R.font.orbitron, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
        Font(R.font.orbitron, FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900))),
    )
    val Mono = FontFamily(Font(R.font.sharetech_mono))
    val Rajdhani = FontFamily(
        Font(R.font.rajdhani_semibold, FontWeight.Normal),
        Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
        Font(R.font.rajdhani_bold, FontWeight.Bold),
    )
}

private val typography = Typography(
    displayLarge = TextStyle(fontFamily = GFonts.Orbitron, fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = 3.sp),
    headlineLarge = TextStyle(fontFamily = GFonts.Orbitron, fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = 2.sp),
    headlineSmall = TextStyle(fontFamily = GFonts.Orbitron, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 1.5.sp),
    titleLarge = TextStyle(fontFamily = GFonts.Rajdhani, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = GFonts.Rajdhani, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontFamily = GFonts.Rajdhani, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    bodyMedium = TextStyle(fontFamily = GFonts.Rajdhani, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelLarge = TextStyle(fontFamily = GFonts.Orbitron, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 2.sp),
    labelMedium = TextStyle(fontFamily = GFonts.Mono, fontSize = 12.sp, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontFamily = GFonts.Mono, fontSize = 10.sp, letterSpacing = 1.sp),
)

@Composable
fun GhostTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = GColors.Neon,
            onPrimary = GColors.Void,
            secondary = GColors.Amber,
            background = GColors.Void,
            onBackground = GColors.TextPrimary,
            surface = GColors.Panel,
            onSurface = GColors.TextPrimary,
            error = GColors.Danger,
        ),
        typography = typography,
        content = content,
    )
}
