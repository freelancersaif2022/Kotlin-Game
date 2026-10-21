package com.novacleaner.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.novacleaner.app.R

@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: FontWeight) = Font(
    resId = R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Jakarta = FontFamily(
    jakarta(FontWeight.Normal),
    jakarta(FontWeight.Medium),
    jakarta(FontWeight.SemiBold),
    jakarta(FontWeight.Bold),
    jakarta(FontWeight.ExtraBold),
)

private fun style(size: Int, weight: FontWeight, line: Int, spacing: Double = 0.0) = TextStyle(
    fontFamily = Jakarta,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.em,
)

val NovaTypography = Typography(
    displayLarge = style(52, FontWeight.ExtraBold, 58, -0.02),
    displayMedium = style(42, FontWeight.ExtraBold, 48, -0.02),
    displaySmall = style(34, FontWeight.Bold, 40, -0.01),
    headlineLarge = style(30, FontWeight.Bold, 36, -0.01),
    headlineMedium = style(26, FontWeight.Bold, 32, -0.01),
    headlineSmall = style(22, FontWeight.Bold, 28),
    titleLarge = style(20, FontWeight.Bold, 26),
    titleMedium = style(16, FontWeight.SemiBold, 22),
    titleSmall = style(14, FontWeight.SemiBold, 20),
    bodyLarge = style(16, FontWeight.Normal, 24),
    bodyMedium = style(14, FontWeight.Normal, 20),
    bodySmall = style(12, FontWeight.Normal, 16),
    labelLarge = style(15, FontWeight.Bold, 20, 0.01),
    labelMedium = style(12, FontWeight.SemiBold, 16, 0.01),
    labelSmall = style(11, FontWeight.SemiBold, 14, 0.02),
)
