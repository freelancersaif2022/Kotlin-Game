package com.flashshare.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.flashshare.app.data.ThemeMode

object Brand {
    val Blue = Color(0xFF2F5BFF)
    val Violet = Color(0xFF7B4DFF)
    val Cyan = Color(0xFF19C3FF)
    val Pink = Color(0xFFE04DFF)
    val Mint = Color(0xFF00C48C)
    val Gold = Color(0xFFFFB800)
    val Orange = Color(0xFFFF7A00)
    val Red = Color(0xFFFF4D5E)

    val main = Brush.linearGradient(listOf(Color(0xFF3D7BFF), Color(0xFF8A3DFF)))
    /** Same gradient as the app icon. */
    val logo = Brush.linearGradient(listOf(Color(0xFF4F7BFF), Color(0xFF6D3DFF), Color(0xFFE0479E)))
    val send = Brush.linearGradient(listOf(Color(0xFF2F6BFF), Color(0xFF19B8FF)))
    val receive = Brush.linearGradient(listOf(Color(0xFF7B3DFF), Color(0xFFD94DFF)))
    val premium = Brush.linearGradient(listOf(Color(0xFFFFC233), Color(0xFFFF7A00)))
    val mint = Brush.linearGradient(listOf(Color(0xFF00C48C), Color(0xFF00A3FF)))
}

/** Extra colors that Material's scheme doesn't cover. */
data class Extra(val cardBorder: Color, val subtle: Color, val success: Color, val heroText: Color, val isDark: Boolean)

val LocalExtra = staticCompositionLocalOf { Extra(Color(0xFFE3E8F4), Color(0xFF6B7487), Brand.Mint, Color.White, false) }

private val Light = lightColorScheme(
    primary = Brand.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E9FF),
    onPrimaryContainer = Color(0xFF0B1E66),
    secondary = Brand.Violet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE5FF),
    onSecondaryContainer = Color(0xFF2A1166),
    tertiary = Brand.Mint,
    background = Color(0xFFF5F7FD),
    onBackground = Color(0xFF0E1426),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E1426),
    surfaceVariant = Color(0xFFEFF2FA),
    onSurfaceVariant = Color(0xFF5B6478),
    surfaceContainer = Color(0xFFF1F4FB),
    surfaceContainerHigh = Color(0xFFEAEEF8),
    surfaceContainerHighest = Color(0xFFE3E8F4),
    surfaceContainerLow = Color(0xFFF8F9FE),
    outline = Color(0xFFD5DBEA),
    outlineVariant = Color(0xFFE6EAF4),
    error = Brand.Red,
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7B96FF),
    onPrimary = Color(0xFF051446),
    primaryContainer = Color(0xFF1E2E6E),
    onPrimaryContainer = Color(0xFFDDE4FF),
    secondary = Color(0xFFAE90FF),
    onSecondary = Color(0xFF1E0A52),
    secondaryContainer = Color(0xFF34226E),
    onSecondaryContainer = Color(0xFFEBE2FF),
    tertiary = Color(0xFF3DDBAA),
    background = Color(0xFF0A0E18),
    onBackground = Color(0xFFE8ECF7),
    surface = Color(0xFF121827),
    onSurface = Color(0xFFE8ECF7),
    surfaceVariant = Color(0xFF1A2235),
    onSurfaceVariant = Color(0xFF9AA4BA),
    surfaceContainer = Color(0xFF161D2D),
    surfaceContainerHigh = Color(0xFF1C2436),
    surfaceContainerHighest = Color(0xFF232C40),
    surfaceContainerLow = Color(0xFF111725),
    outline = Color(0xFF2C3650),
    outlineVariant = Color(0xFF222B40),
    error = Color(0xFFFF6B78),
)

private val AppTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    )
}

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
)

@Composable
fun FlashShareTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    val extra = if (dark) {
        Extra(Color(0xFF263048), Color(0xFF8A94AA), Color(0xFF3DDBAA), Color.White, true)
    } else {
        Extra(Color(0xFFE3E8F4), Color(0xFF6B7487), Brand.Mint, Color.White, false)
    }
    val scheme = if (dark) Dark else Light
    CompositionLocalProvider(LocalExtra provides extra) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes) {
            // Text/icons without an explicit color follow the theme (white in dark mode) even outside a Surface.
            CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides scheme.onBackground, content = content)
        }
    }
}
