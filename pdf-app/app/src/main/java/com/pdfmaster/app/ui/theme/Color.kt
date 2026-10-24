package com.pdfmaster.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object Brand {
    val Red = Color(0xFFE5252A)
    val RedBright = Color(0xFFF0383D)
    val RedDeep = Color(0xFFB3141C)
    val RedSoft = Color(0xFFFDECEC)
    val RedSoftDark = Color(0xFF3A1A1C)
    val Ink = Color(0xFF1B1D29)
    val InkSoft = Color(0xFF3A3D4D)
    val Gray = Color(0xFF8A8FA3)
    val LightGray = Color(0xFFEEF0F5)
    val Background = Color(0xFFF8F9FB)
    val Gold = Color(0xFFFFB300)
    val GoldDeep = Color(0xFFFF8F00)
    val GoldSoft = Color(0xFFFFF4D6)
    val Success = Color(0xFF16A34A)
    val Warning = Color(0xFFF59E0B)
    val Info = Color(0xFF2563EB)

    val DarkBackground = Color(0xFF0F1014)
    val DarkSurface = Color(0xFF17181E)
    val DarkSurfaceHigh = Color(0xFF20222A)
    val DarkOutline = Color(0xFF2C2E38)

    val RedGradient = Brush.linearGradient(listOf(RedBright, Red, RedDeep))
    val RedHorizontal = Brush.horizontalGradient(listOf(RedBright, RedDeep))
    val GoldGradient = Brush.horizontalGradient(listOf(Gold, GoldDeep))
}

/** Accent colours for tool tiles. Kept close to the brand red so the grid reads as one family. */
object ToolColors {
    val Red = Color(0xFFE5252A)
    val Coral = Color(0xFFF2545B)
    val Crimson = Color(0xFFC81E3A)
    val Orange = Color(0xFFF97316)
    val Amber = Color(0xFFF59E0B)
    val Blue = Color(0xFF2563EB)
    val Indigo = Color(0xFF4F46E5)
    val Violet = Color(0xFF7C3AED)
    val Teal = Color(0xFF0D9488)
    val Green = Color(0xFF16A34A)
    val Slate = Color(0xFF475569)
    val Pink = Color(0xFFDB2777)
}
