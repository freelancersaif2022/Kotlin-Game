package com.pulse.messenger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.messenger.data.ThemeMode

private val LightScheme = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5F5EC),
    onPrimaryContainer = Color(0xFF00382F),
    secondary = TealDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5F5EC),
    onSecondaryContainer = Color(0xFF00382F),
    tertiary = Amber,
    background = Color.White,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = Muted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F8FA),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFF0F2F5),
    outline = Color(0xFFD1D7DB),
    outlineVariant = Color(0xFFE9EDEF),
    error = Danger,
)

private val DarkScheme = darkColorScheme(
    primary = Mint,
    onPrimary = Color(0xFF0B141A),
    primaryContainer = Color(0xFF103D36),
    onPrimaryContainer = Color(0xFFD5F5EC),
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF103D36),
    onSecondaryContainer = Color(0xFFD5F5EC),
    tertiary = Amber,
    background = Color(0xFF0B141A),
    onBackground = Color(0xFFE9EDEF),
    surface = Color(0xFF0B141A),
    onSurface = Color(0xFFE9EDEF),
    surfaceVariant = Color(0xFF202C33),
    onSurfaceVariant = Color(0xFF8696A0),
    surfaceContainerLowest = Color(0xFF0B141A),
    surfaceContainerLow = Color(0xFF111B21),
    surfaceContainer = Color(0xFF111B21),
    surfaceContainerHigh = Color(0xFF1B252C),
    surfaceContainerHighest = Color(0xFF233138),
    outline = Color(0xFF3B4A54),
    outlineVariant = Color(0xFF222D34),
    error = Color(0xFFF2555A),
)

private val PulseTypography = Typography().let { base ->
    base.copy(
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp),
        bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    )
}

private val PulseShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Message text size chosen in Settings > Chats > Font size. */
val LocalMessageTextStyle = staticCompositionLocalOf { TextStyle(fontSize = 15.5.sp, lineHeight = 21.sp) }

@Composable
fun PulseTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    messageFontSize: Float = 15.5f,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(
        LocalPulseColors provides if (dark) DarkPulseColors else LightPulseColors,
        LocalMessageTextStyle provides TextStyle(fontSize = messageFontSize.sp, lineHeight = (messageFontSize * 1.36f).sp),
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = PulseTypography,
            shapes = PulseShapes,
            content = content,
        )
    }
}

object PulseTheme {
    val colors: PulseColors
        @Composable @ReadOnlyComposable get() = LocalPulseColors.current
}
