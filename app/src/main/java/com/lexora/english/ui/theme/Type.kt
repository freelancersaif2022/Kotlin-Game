package com.lexora.english.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lexora.english.R

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

val Playfair = FontFamily(Font(R.font.playfair_display, FontWeight.Normal))

private fun style(size: Int, weight: FontWeight, line: Int, spacing: Float = 0f) = TextStyle(
    fontFamily = Poppins,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp,
)

val LexTypography = Typography(
    displayLarge = style(40, FontWeight.Bold, 48, -0.5f),
    displayMedium = style(34, FontWeight.Bold, 42, -0.4f),
    displaySmall = style(28, FontWeight.Bold, 36, -0.3f),
    headlineLarge = style(26, FontWeight.Bold, 34, -0.2f),
    headlineMedium = style(22, FontWeight.SemiBold, 30, -0.1f),
    headlineSmall = style(20, FontWeight.SemiBold, 28),
    titleLarge = style(18, FontWeight.SemiBold, 26),
    titleMedium = style(16, FontWeight.SemiBold, 24),
    titleSmall = style(14, FontWeight.SemiBold, 20),
    bodyLarge = style(16, FontWeight.Normal, 25),
    bodyMedium = style(14, FontWeight.Normal, 22),
    bodySmall = style(12, FontWeight.Normal, 18),
    labelLarge = style(14, FontWeight.SemiBold, 20, 0.1f),
    labelMedium = style(12, FontWeight.Medium, 16, 0.2f),
    labelSmall = style(11, FontWeight.Medium, 14, 0.3f),
)
