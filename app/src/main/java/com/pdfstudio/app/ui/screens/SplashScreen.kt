package com.pdfstudio.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.pdfstudio.app.ui.components.AppLogo
import com.pdfstudio.app.ui.theme.Brand
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    val logoScale = remember { Animatable(0.55f) }
    val logoAlpha = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleShift = remember { Animatable(24f) }
    val taglineAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(280)) }
        launch { glow.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        logoScale.animateTo(1f, spring(dampingRatio = 0.52f, stiffness = 260f))
        launch { titleAlpha.animateTo(1f, tween(380)) }
        launch { titleShift.animateTo(0f, tween(420, easing = FastOutSlowInEasing)) }
        delay(160)
        taglineAlpha.animateTo(1f, tween(380))
        delay(550)
        finish()
    }

    val background = MaterialTheme.colorScheme.background
    Box(
        Modifier
            .fillMaxSize()
            .background(background)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        listOf(Brand.Coral.copy(alpha = 0.22f * glow.value), Brand.Magenta.copy(alpha = 0.08f * glow.value), background.copy(alpha = 0f)),
                        center = center,
                        radius = size.minDimension * 0.75f,
                    ),
                    radius = size.minDimension * 0.75f,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppLogo(
                size = 112.dp,
                shine = true,
                modifier = Modifier.graphicsLayer {
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                    alpha = logoAlpha.value
                },
            )
            Spacer(Modifier.height(30.dp))
            Text(
                "PDF Studio",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha.value
                    translationY = titleShift.value * density
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Split  ·  Crop  ·  Convert",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { alpha = taglineAlpha.value },
            )
        }
        LoadingDots(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 56.dp)
                .graphicsLayer { alpha = taglineAlpha.value }
        )
    }
}

@Composable
private fun LoadingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "dots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { i ->
            Box(
                Modifier
                    .size(8.dp)
                    .graphicsLayer {
                        val d = ((phase - i + 3f) % 3f)
                        val t = if (d < 1f) 1f - d else 0f
                        scaleX = 0.7f + 0.5f * t
                        scaleY = 0.7f + 0.5f * t
                        alpha = 0.35f + 0.65f * t
                    }
                    .clip(CircleShape)
                    .background(Brand.gradient)
            )
        }
    }
}
