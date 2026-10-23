package com.saif.pymaster.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.saif.pymaster.R

/** Brand colours shared by every theme variant. */
object Brand {
    val PythonBlue = Color(0xFF3776AB)
    val PythonBlueBright = Color(0xFF4B8FD6)
    val PythonYellow = Color(0xFFFFD43B)
    val Cyan = Color(0xFF00D1FF)
    val Success = Color(0xFF22C55E)
    val Danger = Color(0xFFFF5C6C)
    val Warning = Color(0xFFFFA94D)
    val Purple = Color(0xFF9B7BFF)
    val White = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xB3FFFFFF)
    val TextMuted = Color(0x80FFFFFF)
    val Divider = Color(0x14FFFFFF)
}

@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val stroke: Color,
    val terminal: Color,
)

enum class ThemeVariant(val id: String, val title: String, val colors: AppColors) {
    Navy(
        "navy", "Midnight Navy",
        AppColors(Color(0xFF0B0F17), Color(0xFF141A26), Color(0xFF1C2433), Color(0x1FFFFFFF), Color(0xFF070A10))
    ),
    Amoled(
        "amoled", "AMOLED Black",
        AppColors(Color(0xFF000000), Color(0xFF0E1015), Color(0xFF171A21), Color(0x1AFFFFFF), Color(0xFF000000))
    ),
    DeepSpace(
        "space", "Deep Space",
        AppColors(Color(0xFF090C1D), Color(0xFF121733), Color(0xFF1B2147), Color(0x24FFFFFF), Color(0xFF05070F))
    ),
    Carbon(
        "carbon", "Carbon",
        AppColors(Color(0xFF101214), Color(0xFF191C20), Color(0xFF23272D), Color(0x1FFFFFFF), Color(0xFF0A0B0D))
    );

    companion object {
        fun of(id: String) = entries.firstOrNull { it.id == id } ?: Navy
    }
}

val LocalAppColors = staticCompositionLocalOf { ThemeVariant.Navy.colors }

object Fonts {
    val Poppins = FontFamily(
        Font(R.font.poppins_regular, FontWeight.Normal),
        Font(R.font.poppins_medium, FontWeight.Medium),
        Font(R.font.poppins_semibold, FontWeight.SemiBold),
        Font(R.font.poppins_bold, FontWeight.Bold),
    )
    val Mono = FontFamily(
        Font(R.font.jetbrains_mono, FontWeight.Normal),
        Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
    )
}

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = Fonts.Poppins, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Fonts.Poppins, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Fonts.Poppins, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Fonts.Poppins, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Fonts.Poppins, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = Fonts.Poppins, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Fonts.Poppins, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = Fonts.Poppins, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = Fonts.Poppins, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Fonts.Poppins, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Fonts.Poppins, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Fonts.Poppins, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

@Composable
fun PyMasterTheme(
    variant: ThemeVariant = ThemeVariant.Navy,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val c = variant.colors
    val scheme = darkColorScheme(
        primary = Brand.PythonBlueBright,
        onPrimary = Brand.White,
        primaryContainer = Brand.PythonBlue,
        onPrimaryContainer = Brand.White,
        secondary = Brand.PythonYellow,
        onSecondary = Color(0xFF14110A),
        tertiary = Brand.Cyan,
        onTertiary = Color(0xFF00141A),
        background = c.background,
        onBackground = Brand.White,
        surface = c.surface,
        onSurface = Brand.White,
        surfaceVariant = c.surfaceHigh,
        onSurfaceVariant = Brand.TextSecondary,
        surfaceContainer = c.surface,
        surfaceContainerHigh = c.surfaceHigh,
        surfaceContainerHighest = c.surfaceHigh,
        surfaceContainerLow = c.surface,
        surfaceContainerLowest = c.background,
        outline = c.stroke,
        outlineVariant = c.stroke,
        error = Brand.Danger,
        onError = Brand.White,
        inverseSurface = Brand.White,
        inverseOnSurface = c.background,
    )
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalAppColors provides c,
        LocalDensity provides Density(density.density, density.fontScale * fontScale),
    ) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}
