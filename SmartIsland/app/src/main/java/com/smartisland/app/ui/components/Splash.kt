package com.smartisland.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
 * Branded launch screen drawn by Compose right after the system splash (a plain matching color), so
 * the icon animation and the "Smart Island" wordmark look the same on every Android version. The app
 * composes underneath while this plays.
 */
@Composable
fun BrandSplash(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.72f) }
    val iconAlpha = remember { Animatable(0f) }
    val halo = remember { Animatable(0f) }
    val pill = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        launch { iconAlpha.animateTo(1f, tween(240)) }
        launch { halo.animateTo(1f, tween(700)) }
        scale.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.58f, stiffness = 320f))
        pill.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 420f))
        delay(220)
        screenAlpha.animateTo(0f, tween(220))
        onFinished()
    }
    val dark = Palette.isDark
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screenAlpha.value }
            .background(Palette.HeroGradient)
            .drawBehind {
                // Soft brand glows behind the icon.
                val c = Offset(size.width / 2, size.height / 2)
                drawCircle(
                    Brush.radialGradient(listOf(Palette.Primary.copy(alpha = if (dark) 0.35f else 0.22f), Color.Transparent), center = c, radius = size.width * 0.7f),
                    radius = size.width * 0.7f, center = c,
                )
                drawCircle(
                    Brush.radialGradient(listOf(Color(0x5532ADE6), Color.Transparent), center = Offset(size.width * 0.9f, size.height * 0.15f), radius = size.width * 0.5f),
                    radius = size.width * 0.5f, center = Offset(size.width * 0.9f, size.height * 0.15f),
                )
            },
    ) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                // Expanding halo ring.
                Box(
                    Modifier
                        .size(190.dp)
                        .graphicsLayer {
                            val h = halo.value
                            scaleX = 0.7f + 0.3f * h
                            scaleY = 0.7f + 0.3f * h
                            alpha = (1f - h) * 0.9f
                        }
                        .border(2.dp, Palette.Primary.copy(alpha = 0.6f), RoundedCornerShape(56.dp)),
                )
                AppIcon(
                    128.dp,
                    Modifier.graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        alpha = iconAlpha.value
                    },
                )
            }
            Spacer(Modifier.height(30.dp))
            // A tiny island that morphs open, echoing what the app does.
            Box(
                Modifier
                    .graphicsLayer { alpha = pill.value }
                    .size(width = 40.dp + 70.dp * pill.value, height = 14.dp + 6.dp * pill.value)
                    .clip(CircleShape)
                    .background(Color.Black),
            )
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 34.dp)
                .graphicsLayer { alpha = iconAlpha.value },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Smart Island",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.4.sp),
                color = Palette.TextPrimary,
            )
            Text(
                "Your island. Live.",
                style = MaterialTheme.typography.labelMedium,
                color = Palette.TextSecondary,
            )
        }
    }
}
