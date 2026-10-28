package com.inkly.notes.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkly.notes.R
import com.inkly.notes.ui.components.BrandGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val logoScale = remember { Animatable(1f) }
    val logoLift = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textLift = remember { Animatable(24f) }
    val ripple = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { ripple.animateTo(1f, tween(1100, easing = LinearEasing)) }
        launch {
            logoScale.animateTo(0.86f, tween(160))
            logoScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 320f))
        }
        delay(180)
        launch { logoLift.animateTo(-36f, tween(520, easing = FastOutSlowInEasing)) }
        launch { textLift.animateTo(0f, tween(560, easing = FastOutSlowInEasing)) }
        textAlpha.animateTo(1f, tween(480))
        delay(420)
        onFinished()
    }

    Box(Modifier.fillMaxSize().background(BrandGradient), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension * (0.25f + ripple.value * 0.55f)
            drawCircle(
                Color.White.copy(alpha = 0.16f * (1f - ripple.value)),
                radius = r,
                style = Stroke(width = 2.dp.toPx()),
            )
            drawCircle(Color.White.copy(alpha = 0.05f), radius = size.minDimension * 0.42f)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        scaleX = logoScale.value; scaleY = logoScale.value
                        translationY = logoLift.value * density
                    },
            )
            Text(
                "Inkly",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha.value
                    translationY = (textLift.value - 64f) * density
                },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Thoughts, beautifully kept.",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 15.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha.value
                    translationY = (textLift.value * 1.4f - 64f) * density
                },
            )
        }
        Text(
            "Private • Synced • Fast",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp,
            letterSpacing = 1.5.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .graphicsLayer { alpha = textAlpha.value },
        )
    }
}
