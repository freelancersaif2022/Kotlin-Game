package com.voxcast.mic.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxcast.mic.ui.components.RealisticMicrophone
import com.voxcast.mic.ui.components.VoxBackground
import com.voxcast.mic.ui.components.gradientTint
import com.voxcast.mic.ui.theme.VoxColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val done by rememberUpdatedState(onFinished)
    val micIn = remember { Animatable(0f) }
    val titleIn = remember { Animatable(0f) }
    val tagIn = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }

    val t = rememberInfiniteTransition(label = "splash")
    val wave by t.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(1_400, easing = LinearEasing)), label = "wave")

    LaunchedEffect(Unit) {
        launch { micIn.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 140f)) }
        launch { progress.animateTo(1f, tween(2_000, easing = FastOutSlowInEasing)) }
        delay(350)
        launch { titleIn.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
        delay(300)
        tagIn.animateTo(1f, tween(600))
        delay(900)
        done()
    }

    VoxBackground {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(width = 150.dp, height = 250.dp)
                    .graphicsLayer {
                        val s = 0.6f + 0.4f * micIn.value
                        scaleX = s; scaleY = s
                        alpha = micIn.value.coerceIn(0f, 1f)
                        translationY = (1f - micIn.value) * 60f
                    },
            ) {
                // Simulated voice so the mic glows during the intro.
                RealisticMicrophone(level = { 0.35f + 0.25f * sin(wave) }, live = true, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "VoxCast",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                modifier = Modifier
                    .gradientTint(Brush.horizontalGradient(listOf(Color.White, VoxColors.Cyan, VoxColors.Violet)))
                    .graphicsLayer {
                        alpha = titleIn.value
                        translationY = (1f - titleIn.value) * 30f
                    },
                letterSpacing = (-1 + (1 - titleIn.value) * 8).sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "WIRELESS MIC  ·  ZERO NOISE",
                style = MaterialTheme.typography.labelMedium,
                color = VoxColors.TextDim,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.alpha(tagIn.value),
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Canvas(
                Modifier
                    .width(140.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f)),
            ) {
                drawLine(
                    Brush.horizontalGradient(listOf(VoxColors.Cyan, VoxColors.Violet)),
                    Offset(0f, size.height / 2), Offset(size.width * progress.value, size.height / 2),
                    strokeWidth = size.height, cap = StrokeCap.Round,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text("No internet · No servers · Just Wi-Fi", style = MaterialTheme.typography.bodySmall, color = VoxColors.TextFaint)
        }
    }
}
