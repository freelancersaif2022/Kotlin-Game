package com.smartisland.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartisland.app.R
import com.smartisland.app.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The app icon as a rounded square, built from the adaptive icon layers so it always matches. */
@Composable
fun AppIcon(size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size * 0.27f)
    Box(
        modifier
            .size(size)
            .shadow(24.dp, shape, ambientColor = Palette.Primary, spotColor = Palette.Primary)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        // Adaptive icons show the centre 72 of 108 units, so the layer is drawn 1.5x and cropped.
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(size * 1.5f))
    }
}

/**
 * Branded launch screen drawn by Compose right after the system splash (which is a plain matching
 * color), so the icon, its animation and the "Smart Island" wordmark look the same on every Android
 * version. The app composes underneath while this plays.
 */
@Composable
fun BrandSplash(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.7f) }
    val iconAlpha = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        launch { iconAlpha.animateTo(1f, tween(260)) }
        scale.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 300f))
        delay(260)
        screenAlpha.animateTo(0f, tween(220))
        onFinished()
    }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screenAlpha.value }
            .background(Palette.Background),
    ) {
        AppIcon(
            120.dp,
            Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    alpha = iconAlpha.value
                },
        )
        Text(
            "Smart Island",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp),
            color = Palette.TextPrimary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp)
                .graphicsLayer { alpha = iconAlpha.value },
        )
    }
}
