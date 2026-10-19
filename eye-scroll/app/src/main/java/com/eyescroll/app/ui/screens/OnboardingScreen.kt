package com.eyescroll.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eyescroll.app.ui.components.BulletRow
import com.eyescroll.app.ui.components.GlassCard
import com.eyescroll.app.ui.components.GradientButton
import com.eyescroll.app.ui.components.AirGestureGlyph
import com.eyescroll.app.ui.theme.EsColors
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pager = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("AirScroll", style = MaterialTheme.typography.titleLarge, color = EsColors.Text)
            Spacer(Modifier.weight(1f))
            if (pager.currentPage < 2) {
                Text(
                    "Skip",
                    style = MaterialTheme.typography.labelLarge,
                    color = EsColors.TextMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onFinish() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> OnboardingPage(
                    title = "Scroll without\ntouching the screen",
                    body = "Point your finger at the phone and move it up or down in the air. The front camera sees it and the app scrolls for you — Chrome, Instagram, YouTube, anything."
                ) { PhoneDemo() }
                1 -> OnboardingPage(
                    title = "Camera only\nwhen you want",
                    body = "Tap the floating dot to turn air scrolling on, tap again and the camera is off instantly. It also switches off by itself when you stop using it.",
                ) { AirGestureGlyph(Modifier.size(width = 180.dp, height = 260.dp)) }
                else -> OnboardingPage(title = "Ready in 1 minute", body = null) {
                    GlassCard(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                        BulletRow(Icons.Filled.ThumbUp, "Hands-free", "Cooking, eating, wet or dirty hands, phone on a stand — scroll without touching it.")
                        BulletRow(Icons.Filled.KeyboardArrowDown, "Two gesture modes", "Swipe: flick your finger. Point & hold: keep scrolling while your finger stays up or down.", EsColors.Secondary)
                        BulletRow(Icons.Filled.Lock, "100% on your phone", "Camera frames are analysed on-device and discarded instantly. Nothing is recorded or uploaded.", EsColors.Success)
                        BulletRow(Icons.Filled.Build, "Quick setup", "Allow the camera and turn on the AirScroll accessibility service.", EsColors.Warning)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
            repeat(3) { i ->
                val w by animateDpAsState(if (pager.currentPage == i) 28.dp else 8.dp, tween(300), label = "dot")
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(w)
                        .clip(RoundedCornerShape(50))
                        .background(if (pager.currentPage == i) EsColors.Primary else Color.White.copy(alpha = 0.18f)),
                )
            }
        }

        GradientButton(
            text = if (pager.currentPage == 2) "Get started" else "Next",
            onClick = {
                if (pager.currentPage == 2) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 20.dp),
        )
    }
}

@Composable
private fun OnboardingPage(title: String, body: String?, illustration: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { illustration() }
        Text(
            title,
            style = MaterialTheme.typography.displaySmall,
            color = EsColors.Text,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp),
        )
        if (body != null) {
            Spacer(Modifier.height(14.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyLarge,
                color = EsColors.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** A phone whose feed scrolls while a hand gestures in the air next to it. */
@Composable
private fun PhoneDemo() {
    val t = rememberInfiniteTransition(label = "phone")
    val scroll by t.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3600
                0f at 0
                0f at 800
                -0.25f at 1300 using FastOutSlowInEasing
                -0.25f at 2600
                -0.5f at 3100 using FastOutSlowInEasing
                -0.5f at 3450
                0f at 3600
            },
        ),
        label = "scroll",
    )
    Box(Modifier.size(width = 230.dp, height = 320.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(width = 170.dp, height = 310.dp)) {
            val w = size.width
            val h = size.height
            val r = CornerRadius(w * 0.16f)
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF141D33), Color(0xFF0B1122))), cornerRadius = r)
            drawRoundRect(Color.White.copy(alpha = 0.14f), cornerRadius = r, style = Stroke(width = 3f))
            val pad = w * 0.08f
            clipRect(pad, h * 0.06f, w - pad, h * 0.95f) {
                val cardH = h * 0.22f
                for (i in 0 until 10) {
                    val top = h * 0.08f + i * (cardH + h * 0.03f) + scroll * h * 1.5f
                    val colors = if (i % 2 == 0) listOf(EsColors.Primary.copy(alpha = 0.5f), Color(0xFF4F8CFF).copy(alpha = 0.35f))
                    else listOf(EsColors.Secondary.copy(alpha = 0.5f), Color(0xFFFF7AC6).copy(alpha = 0.3f))
                    drawRoundRect(
                        Brush.linearGradient(colors, Offset(pad, top), Offset(w - pad, top + cardH)),
                        topLeft = Offset(pad, top),
                        size = Size(w - 2 * pad, cardH * 0.62f),
                        cornerRadius = CornerRadius(14f),
                    )
                    drawRoundRect(Color.White.copy(alpha = 0.22f), Offset(pad, top + cardH * 0.7f), Size((w - 2 * pad) * 0.8f, cardH * 0.08f), CornerRadius(6f))
                    drawRoundRect(Color.White.copy(alpha = 0.12f), Offset(pad, top + cardH * 0.84f), Size((w - 2 * pad) * 0.55f, cardH * 0.08f), CornerRadius(6f))
                }
            }
        }
        AirGestureGlyph(Modifier.align(Alignment.CenterEnd).offset(x = 22.dp).size(width = 90.dp, height = 150.dp), glow = false)
    }
}
