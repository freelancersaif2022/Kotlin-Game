package com.saif.imagetopdf.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val card = remember { Animatable(0f) }
    val lines = remember { Animatable(0f) }
    val badge = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }
    val bar = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { card.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow)) }
        launch { delay(250); lines.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        launch { delay(800); badge.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)) }
        launch { delay(450); title.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        launch { delay(700); tagline.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        bar.animateTo(1f, tween(2300, easing = FastOutSlowInEasing))
        delay(150)
        onFinished()
    }

    val loop = rememberInfiniteTransition(label = "splash")
    val glow by loop.animateFloat(0.35f, 0.7f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "glow")
    val scan by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "scan")

    Box(Modifier.fillMaxSize().background(Palette.header), contentAlignment = Alignment.Center) {
        // Soft halo behind the logo.
        Canvas(Modifier.size(340.dp).graphicsLayer { alpha = glow * card.value }) {
            drawCircle(Brush.radialGradient(listOf(Palette.Royal.copy(alpha = 0.55f), Color.Transparent)))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(width = 118.dp, height = 148.dp)
                    .graphicsLayer {
                        val s = 0.4f + 0.6f * card.value
                        scaleX = s; scaleY = s
                        rotationZ = -10f * (1f - card.value)
                        alpha = card.value.coerceIn(0f, 1f)
                    },
            ) {
                Canvas(Modifier.fillMaxSize()) { drawDocument(lines.value, scan) }
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 14.dp)
                        .graphicsLayer {
                            scaleX = badge.value; scaleY = badge.value
                            translationX = 18.dp.toPx()
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .background(Palette.Red)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) { Text("PDF", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp) }
            }

            Spacer(Modifier.height(40.dp))
            Text(
                "Image to PDF",
                color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                modifier = Modifier.graphicsLayer { alpha = title.value; translationY = (1 - title.value) * 40f },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Photos rebuilt as real, editable documents",
                color = Palette.Sky, fontSize = 15.sp,
                modifier = Modifier.graphicsLayer { alpha = tagline.value; translationY = (1 - tagline.value) * 30f },
            )
        }

        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.width(140.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.15f))) {
                Box(Modifier.fillMaxWidth(bar.value).height(3.dp).background(Brush.horizontalGradient(listOf(Palette.Sky, Color.White))))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "ON-DEVICE  ·  PRIVATE  ·  OFFLINE",
                color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, letterSpacing = 2.sp,
                modifier = Modifier.graphicsLayer { alpha = tagline.value },
            )
        }
    }
}

/** A paper sheet whose text lines type themselves in, with a scanner beam passing over. */
private fun DrawScope.drawDocument(progress: Float, scan: Float) {
    val r = 14.dp.toPx()
    drawRoundRect(Color.Black.copy(alpha = 0.25f), topLeft = Offset(0f, 8.dp.toPx()), size = size, cornerRadius = CornerRadius(r))
    drawRoundRect(Color.White, size = size, cornerRadius = CornerRadius(r))

    val pad = 16.dp.toPx()
    val barH = 6.dp.toPx()
    val widths = floatArrayOf(0.55f, 0.9f, 0.8f, 0.92f, 0.6f, 0.85f)
    widths.forEachIndexed { i, w ->
        val start = i / widths.size.toFloat()
        val t = ((progress - start) * widths.size).coerceIn(0f, 1f)
        if (t <= 0f) return@forEachIndexed
        val y = pad + i * (barH + 9.dp.toPx())
        val color = if (i == 0) Palette.Navy else Color(0xFFCBD5E1)
        drawRoundRect(
            color, topLeft = Offset(pad, y),
            size = Size((size.width - pad * 2) * w * t, if (i == 0) barH * 1.4f else barH),
            cornerRadius = CornerRadius(barH),
        )
    }

    val y = size.height * scan
    drawRect(
        Brush.verticalGradient(listOf(Color.Transparent, Palette.Royal.copy(alpha = 0.28f)), startY = y - 36.dp.toPx(), endY = y),
        topLeft = Offset(0f, (y - 36.dp.toPx()).coerceAtLeast(0f)),
        size = Size(size.width, minOf(36.dp.toPx(), y)),
    )
    drawRect(Palette.Royal, topLeft = Offset(0f, y - 1.dp.toPx()), size = Size(size.width, 2.dp.toPx()))
}
