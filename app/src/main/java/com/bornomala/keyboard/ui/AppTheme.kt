package com.bornomala.keyboard.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B8A5A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEFE4),
    onPrimaryContainer = Color(0xFF073822),
    secondary = Color(0xFFE63946),
    onSecondary = Color.White,
    background = Color(0xFFF7F8FA),
    surface = Color(0xFFF7F8FA),
    surfaceVariant = Color(0xFFE5EAE7),
    onSurface = Color(0xFF17231D),
    onSurfaceVariant = Color(0xFF53675B),
    outline = Color(0xFFD9E1DC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6AD6A0),
    onPrimary = Color(0xFF073822),
    primaryContainer = Color(0xFF1D3528),
    onPrimaryContainer = Color(0xFFDCEFE4),
    secondary = Color(0xFFFF8B94),
    background = Color(0xFF121417),
    surface = Color(0xFF121417),
    surfaceVariant = Color(0xFF262B29),
    onSurface = Color(0xFFF2F7F3),
    onSurfaceVariant = Color(0xFFA8BAB0),
    outline = Color(0xFF3D4942),
)

@Composable
fun BornomalaTheme(dynamic: Boolean = false, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
