package com.zyropay.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.zyropay.app.R

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun style(size: Int, line: Int, weight: FontWeight, spacing: Double = 0.0, tnum: Boolean = false) = TextStyle(
    fontFamily = Inter,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = spacing.em,
    fontFeatureSettings = if (tnum) "tnum" else null,
)

class ZyroType {
    val display = style(34, 40, FontWeight.Bold, -0.02)
    val h1 = style(28, 34, FontWeight.Bold, -0.02)
    val h2 = style(22, 28, FontWeight.SemiBold, -0.01)
    val h3 = style(18, 24, FontWeight.SemiBold, -0.01)
    val title = style(16, 22, FontWeight.SemiBold)
    val body = style(16, 24, FontWeight.Normal)
    val bodyMedium = style(15, 22, FontWeight.Medium)
    val bodySmall = style(14, 20, FontWeight.Normal)
    val bodySmallMedium = style(14, 20, FontWeight.Medium)
    val label = style(13, 16, FontWeight.Medium)
    val caption = style(12, 16, FontWeight.Normal)
    val captionMedium = style(12, 16, FontWeight.Medium)
    val tiny = style(10, 12, FontWeight.SemiBold, 0.02)
    val balance = style(36, 42, FontWeight.Bold, -0.02, tnum = true)
    val amount = style(44, 52, FontWeight.Bold, -0.02, tnum = true)
    val number = style(16, 22, FontWeight.SemiBold, tnum = true)
    val numberSmall = style(14, 20, FontWeight.Medium, tnum = true)
    val button = style(16, 20, FontWeight.SemiBold)
}

val ZyroTypography = ZyroType()

val ZyroMaterialTypography = Typography(
    displayLarge = ZyroTypography.display,
    displayMedium = ZyroTypography.h1,
    displaySmall = ZyroTypography.h1,
    headlineLarge = ZyroTypography.h1,
    headlineMedium = ZyroTypography.h2,
    headlineSmall = ZyroTypography.h3,
    titleLarge = ZyroTypography.h3,
    titleMedium = ZyroTypography.title,
    titleSmall = ZyroTypography.bodySmallMedium,
    bodyLarge = ZyroTypography.body,
    bodyMedium = ZyroTypography.bodySmall,
    bodySmall = ZyroTypography.caption,
    labelLarge = ZyroTypography.button,
    labelMedium = ZyroTypography.label,
    labelSmall = ZyroTypography.caption,
)
