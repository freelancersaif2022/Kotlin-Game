package com.voxcast.mic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val VoxScheme = darkColorScheme(
    primary = VoxColors.Cyan,
    onPrimary = VoxColors.Background,
    primaryContainer = VoxColors.CyanDeep,
    onPrimaryContainer = VoxColors.Text,
    secondary = VoxColors.Violet,
    onSecondary = VoxColors.Text,
    tertiary = VoxColors.Gold,
    onTertiary = VoxColors.Background,
    background = VoxColors.Background,
    onBackground = VoxColors.Text,
    surface = VoxColors.Surface,
    onSurface = VoxColors.Text,
    surfaceVariant = VoxColors.SurfaceHigh,
    onSurfaceVariant = VoxColors.TextDim,
    surfaceContainer = VoxColors.SurfaceHigh,
    surfaceContainerHigh = VoxColors.SurfaceHighest,
    outline = VoxColors.Outline,
    outlineVariant = VoxColors.OutlineSoft,
    error = VoxColors.Error,
    onError = VoxColors.Text,
)

private val VoxShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun VoxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VoxScheme,
        typography = VoxTypography,
        shapes = VoxShapes,
        content = content,
    )
}
