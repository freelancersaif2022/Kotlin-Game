package com.clonehub.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val Indigo = Color(0xFF6D5EF7)
val Violet = Color(0xFF9B4DFF)

private val DarkColors = darkColorScheme(
    primary = Indigo,
    secondary = Violet,
    background = Color(0xFF0D0B1A),
    surface = Color(0xFF17142B),
    surfaceVariant = Color(0xFF221E3D),
    onBackground = Color(0xFFECE9F7),
    onSurface = Color(0xFFECE9F7),
)

private val LightColors = lightColorScheme(
    primary = Indigo,
    secondary = Violet,
    background = Color(0xFFF6F5FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDEAF8),
    onBackground = Color(0xFF16132A),
    onSurface = Color(0xFF16132A),
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.5).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

@Composable
fun CloneHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val light = colors.background.luminance() > 0.5f
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = light
            controller.isAppearanceLightNavigationBars = light
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
        }
    }

    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
