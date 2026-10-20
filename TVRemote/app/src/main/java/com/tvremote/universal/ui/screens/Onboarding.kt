package com.tvremote.universal.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvremote.universal.R
import com.tvremote.universal.data.TvPlatform
import com.tvremote.universal.ui.components.PairingLessonIllustration
import com.tvremote.universal.ui.components.PlatformBadge
import com.tvremote.universal.ui.components.PrimaryButton
import com.tvremote.universal.ui.components.RemoteLessonIllustration
import com.tvremote.universal.ui.components.SignalIllustration
import com.tvremote.universal.ui.theme.Navy
import com.tvremote.universal.ui.theme.NavyDeep
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ================================================================ in-app splash

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val done by rememberUpdatedState(onDone)
    val scale = remember { Animatable(0.82f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(420)) }
        scale.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        delay(500)
        done()
    }
    val t = rememberInfiniteTransition(label = "splash")
    val glow by t.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1200), androidx.compose.animation.core.RepeatMode.Reverse), label = "glow")
    val bar by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "bar")

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy, NavyDeep))),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(420.dp)
                .graphicsLayer { this.alpha = glow * 0.9f }
                .background(Brush.radialGradient(listOf(Color(0xFF1F5BFF).copy(alpha = 0.45f), Color.Transparent))),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            },
        ) {
            Image(
                painter = painterResource(R.drawable.app_art),
                contentDescription = null,
                modifier = Modifier
                    .size(196.dp)
                    .shadow(30.dp, RoundedCornerShape(46.dp), ambientColor = Color(0xFF2F7BFF), spotColor = Color(0xFF2F7BFF))
                    .clip(RoundedCornerShape(46.dp)),
            )
            Spacer(Modifier.height(30.dp))
            Text("Universal Remote", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(6.dp))
            Text("Control every smart TV from your phone", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 56.dp)
                .width(120.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.35f)
                    .height(4.dp)
                    .graphicsLayer { translationX = (bar * 1.35f - 0.35f) * 120.dp.toPx() }
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1F5BFF), Color(0xFF00C2FF)))),
            )
        }
    }
}

// ================================================================ onboarding

private data class Lesson(val tag: String, val title: String, val body: String)

private val lessons = listOf(
    Lesson(
        "STEP 1 · CONNECT",
        "Your phone becomes\nthe TV remote",
        "Works over Wi-Fi with Samsung, LG, Sony, TCL, Hisense, Philips, Roku, Vizio, Fire TV and every Android & Google TV.",
    ),
    Lesson(
        "STEP 2 · PAIR ONCE",
        "Find & pair in seconds",
        "Keep the phone and TV on the same Wi-Fi. We find your TV automatically — confirm once on the TV and you're done.",
    ),
    Lesson(
        "STEP 3 · CONTROL",
        "Everything in one remote",
        "D-pad, volume, channels, keyboard, touchpad, app shortcuts and power on — fast, smooth and beautifully simple.",
    ),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pager = rememberPagerState { lessons.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == lessons.lastIndex
    val scheme = MaterialTheme.colorScheme

    Box(Modifier.fillMaxSize().background(scheme.background)) {
        // soft brand glow at the top
        Box(
            Modifier
                .fillMaxWidth()
                .height(420.dp)
                .background(Brush.verticalGradient(listOf(scheme.primary.copy(alpha = 0.14f), Color.Transparent))),
        )
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Universal Remote",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onBackground,
                    modifier = Modifier.padding(start = 10.dp).weight(1f),
                )
                if (!last) TextButton(onClick = onFinish) { Text("Skip", color = scheme.primary) }
                else Spacer(Modifier.height(48.dp))
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(Modifier.height(300.dp), contentAlignment = Alignment.Center) {
                        when (index) {
                            0 -> SignalIllustration()
                            1 -> PairingLessonIllustration()
                            else -> RemoteLessonIllustration()
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    val lesson = lessons[index]
                    Text(
                        lesson.tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(scheme.primary.copy(alpha = 0.10f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        lesson.title,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        color = scheme.onBackground,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        lesson.body,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = scheme.onSurfaceVariant,
                    )
                    if (index == 0) {
                        Spacer(Modifier.height(18.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) { TvPlatform.entries.forEach { PlatformBadge(it) } }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.Center) {
                repeat(lessons.size) { i ->
                    val selected = pager.currentPage == i
                    val width by animateDpAsState(if (selected) 28.dp else 8.dp, label = "dot")
                    val color by animateColorAsState(if (selected) scheme.primary else scheme.outline, label = "dotColor")
                    Box(
                        Modifier.padding(horizontal = 4.dp).size(width = width, height = 8.dp).clip(CircleShape).background(color),
                    )
                }
            }
            AnimatedContent(
                targetState = last,
                transitionSpec = { (fadeIn() + slideInVertically { it / 3 }) togetherWith fadeOut() },
                label = "cta",
            ) { isLast ->
                PrimaryButton(
                    text = if (isLast) "Get started" else "Continue",
                    onClick = {
                        if (isLast) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
