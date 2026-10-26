package com.zyropay.app.ui.screens.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zyropay.app.R
import com.zyropay.app.data.AppRepository
import com.zyropay.app.navigation.Routes
import com.zyropay.app.navigation.navigateClear
import com.zyropay.app.ui.components.ButtonStyle
import com.zyropay.app.ui.components.ZButton
import com.zyropay.app.ui.components.ZImage
import com.zyropay.app.ui.theme.Brand
import com.zyropay.app.ui.theme.Z
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun SplashScreen(nav: NavController) {
    val logoScale = remember { Animatable(1f) }
    val wordAlpha = remember { Animatable(0f) }
    val tagAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { logoScale.animateTo(0.72f, tween(520, easing = FastOutSlowInEasing)) }
        delay(260)
        launch { wordAlpha.animateTo(1f, tween(420)) }
        delay(240)
        tagAlpha.animateTo(1f, tween(380))
        delay(650)
        val dest = when {
            !AppRepository.onboardingSeen.value -> Routes.ONBOARDING
            !AppRepository.loggedIn.value -> Routes.WELCOME
            AppRepository.security.value.passcode.isEmpty() -> Routes.passcode("setup")
            else -> Routes.lock(false)
        }
        AppRepository.unlock()
        nav.navigateClear(dest)
    }
    Box(Modifier.fillMaxSize().background(Brand.Primary)) {
        GlowBackground()
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            ZImage(
                R.drawable.brand_logo_symbol_white,
                Modifier.size(128.dp).graphicsLayer { scaleX = logoScale.value; scaleY = logoScale.value },
            )
            ZImage(
                R.drawable.brand_wordmark_white,
                Modifier.width(150.dp).graphicsLayer { alpha = wordAlpha.value; translationY = (1 - wordAlpha.value) * 24f },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Spend crypto like cash. Anywhere.",
                style = Z.type.bodySmall,
                color = Color.White.copy(alpha = 0.78f * tagAlpha.value),
            )
        }
        Text(
            "Secured · Licensed · Global",
            style = Z.type.caption,
            color = Color.White.copy(alpha = 0.55f * tagAlpha.value),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 28.dp),
        )
    }
}

@Composable
private fun GlowBackground() {
    val t = rememberInfiniteTransition(label = "glow")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(4200), RepeatMode.Reverse), label = "glowP")
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.linearGradient(listOf(Color(0xFF4A2FE0), Color(0xFF5B3DF5), Color(0xFF7A4DFB)), Offset.Zero, Offset(size.width, size.height)))
        drawCircle(
            Brush.radialGradient(listOf(Color(0x5500D1FF), Color.Transparent), center = Offset(size.width * (0.85f - p * 0.1f), size.height * 0.15f), radius = size.width * 0.8f),
            radius = size.width * 0.8f, center = Offset(size.width * (0.85f - p * 0.1f), size.height * 0.15f),
        )
        drawCircle(
            Brush.radialGradient(listOf(Color(0x558F5BFF), Color.Transparent), center = Offset(size.width * 0.1f, size.height * (0.85f + p * 0.05f)), radius = size.width),
            radius = size.width, center = Offset(size.width * 0.1f, size.height * (0.85f + p * 0.05f)),
        )
    }
}

private data class Page(val image: Int, val title: String, val body: String)

private val pages = listOf(
    Page(R.drawable.onboarding_1, "Your crypto,\none card", "Turn USDT, USDC, BTC and more into spending power with a Zyro Pay card — virtual in seconds, metal in days."),
    Page(R.drawable.onboarding_2, "Pay anywhere\nin the world", "Accepted at millions of merchants online and in-store across 170+ countries, with contactless and mobile wallets."),
    Page(R.drawable.onboarding_3, "Instant top-up\n& swap", "Deposit on 8+ networks, swap between coins at live rates and top up your card instantly — no waiting."),
    Page(R.drawable.onboarding_4, "Bank-grade\nsecurity", "Passcode, biometrics, 2FA and instant card freeze keep your money safe. You stay in control, always."),
)

@Composable
fun OnboardingScreen(nav: NavController) {
    val c = Z.colors
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val finish = {
        AppRepository.completeOnboarding()
        nav.navigateClear(Routes.WELCOME)
    }
    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            ZImage(if (c.isDark) R.drawable.brand_logo_horizontal_dark else R.drawable.brand_logo_horizontal, Modifier.height(26.dp))
            Spacer(Modifier.weight(1f))
            if (pager.currentPage < pages.lastIndex) {
                Text(
                    "Skip", style = Z.type.bodySmallMedium, color = c.textSecondary,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { finish() }.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), beyondViewportPageCount = 1) { index ->
            val page = pages[index]
            val offset = ((pager.currentPage - index) + pager.currentPageOffsetFraction)
            Column(Modifier.fillMaxSize().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.weight(0.4f))
                FloatingImage(page.image, Modifier.fillMaxWidth(0.92f).aspectRatio(1f).graphicsLayer {
                    translationX = offset * size.width * 0.35f
                    alpha = 1f - offset.absoluteValue.coerceIn(0f, 1f) * 0.6f
                    val s = 1f - offset.absoluteValue.coerceIn(0f, 1f) * 0.12f
                    scaleX = s; scaleY = s
                })
                Spacer(Modifier.height(20.dp))
                Text(page.title, style = Z.type.h1, color = c.textPrimary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(page.body, style = Z.type.body, color = c.textSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.weight(0.6f))
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.Center) {
            repeat(pages.size) { i ->
                val w by animateDpAsState(if (i == pager.currentPage) 24.dp else 8.dp, spring(stiffness = 500f), label = "dot")
                Box(
                    Modifier.padding(horizontal = 3.dp).height(8.dp).width(w).clip(RoundedCornerShape(4.dp))
                        .background(if (i == pager.currentPage) c.primary else c.outline),
                )
            }
        }
        val last = pager.currentPage == pages.lastIndex
        ZButton(
            if (last) "Get started" else "Next",
            onClick = { if (last) finish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FloatingImage(res: Int, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "float")
    val y by t.animateFloat(-10f, 10f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y")
    Box(modifier, contentAlignment = Alignment.Center) {
        ZImage(res, Modifier.fillMaxSize().graphicsLayer { translationY = y * density })
    }
}

@Composable
fun WelcomeScreen(nav: NavController) {
    val c = Z.colors
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    val user by AppRepository.user.collectAsState()
    Box(Modifier.fillMaxSize().background(c.background)) {
        Canvas(Modifier.fillMaxWidth().fillMaxHeight(0.62f)) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF4A2FE0), Color(0xFF6A45F7), Color(0xFF8F5BFF).copy(alpha = 0f))))
            drawCircle(Brush.radialGradient(listOf(Color(0x6600D1FF), Color.Transparent), Offset(size.width * 0.9f, size.height * 0.2f), size.width * 0.7f), size.width * 0.7f, Offset(size.width * 0.9f, size.height * 0.2f))
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(16.dp))
            ZImage(R.drawable.brand_logo_horizontal_white, Modifier.height(28.dp))
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                ZImage(
                    R.drawable.welcome_hero,
                    Modifier.fillMaxWidth(0.82f).aspectRatio(1200f / 1400f).graphicsLayer {
                        alpha = enter.value
                        translationY = (1 - enter.value) * 80f
                        rotationZ = (1 - enter.value) * -6f
                    },
                )
            }
            Text(
                "The crypto card\nfor everyday life",
                style = Z.type.display,
                color = c.textPrimary,
                modifier = Modifier.graphicsLayer { alpha = enter.value },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Join millions spending USDT, BTC and more at 130M+ merchants worldwide.",
                style = Z.type.body,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(28.dp))
            ZButton("Create account", onClick = { nav.navigate(Routes.SIGNUP) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            ZButton(
                if (user.email.isNotEmpty() || user.phone.isNotEmpty()) "Log in" else "I already have an account",
                onClick = { nav.navigate(Routes.LOGIN) }, modifier = Modifier.fillMaxWidth(), style = ButtonStyle.Outline,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "By continuing you agree to our Terms of Service and Privacy Policy",
                style = Z.type.caption, color = c.textTertiary, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { nav.navigate(Routes.legal("terms")) },
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}
