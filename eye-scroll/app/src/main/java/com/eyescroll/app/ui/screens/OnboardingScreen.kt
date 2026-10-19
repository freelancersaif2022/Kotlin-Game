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
import com.eyescroll.app.ui.components.PadGlyph
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
            Text("FingerScroll", style = MaterialTheme.typography.titleLarge, color = EsColors.Text)
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
                    title = "Scroll any app\nwith a tiny swipe",
                    body = "A small floating pad sits at the edge of your screen. Move your thumb slightly up or down on it and the app scrolls for you — smooth and fast.",
                ) { PhoneDemo() }
                1 -> OnboardingPage(
                    title = "Swipe, or hold\nto keep going",
                    body = "Slight move and release = one smooth scroll. Move and keep holding = keeps scrolling until you let go. Long-press the pad to move it anywhere.",
                ) { PadGlyph(Modifier.size(width = 160.dp, height = 260.dp)) }
                else -> OnboardingPage(title = "Ready in 30 seconds", body = null) {
                    GlassCard(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                        BulletRow(Icons.Filled.ThumbUp, "One-handed comfort", "Scroll long pages, feeds and chats with a small thumb movement instead of big swipes.")
                        BulletRow(Icons.Filled.KeyboardArrowDown, "Works everywhere", "Chrome, Instagram, YouTube, WhatsApp, news apps, PDFs and more.", EsColors.Secondary)
                        BulletRow(Icons.Filled.Lock, "Private", "No camera, no internet, no data collected. Nothing on your screen is read or stored.", EsColors.Success)
                        BulletRow(Icons.Filled.Build, "One permission", "Just turn on the FingerScroll accessibility service.", EsColors.Warning)
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

/** A phone whose feed scrolls while a fingertip nudges the floating pad. */
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
                0f at 900
                -0.32f at 1500 using FastOutSlowInEasing
                -0.32f at 2500
                0f at 3100 using FastOutSlowInEasing
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
        PadGlyph(Modifier.align(Alignment.CenterEnd).offset(x = (-4).dp).size(width = 48.dp, height = 120.dp))
    }
}
