package com.novacleaner.app.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.novacleaner.app.ads.AdConfig
import com.novacleaner.app.ads.Consent
import com.novacleaner.app.core.AppGraph
import com.novacleaner.app.ui.components.SparkleLogo
import com.novacleaner.app.ui.navigation.LocalNavigator
import com.novacleaner.app.ui.navigation.Routes
import com.novacleaner.app.ui.navigation.findActivity
import com.novacleaner.app.ui.theme.Emerald500
import com.novacleaner.app.ui.theme.Emerald700
import com.novacleaner.app.ui.theme.Emerald900
import com.novacleaner.app.ui.theme.Lime400
import com.novacleaner.app.ui.theme.Mint300
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val logoScale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0f) }
    var showText by remember { mutableStateOf(false) }
    var showAdNotice by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(500)) }
        launch { logoScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
        launch { delay(350); showText = true }

        val startedAt = System.currentTimeMillis()
        val settings = AppGraph.prefs.snapshot()
        val activity = context.findActivity()
        showAdNotice = settings.onboardingDone && !settings.premium

        var canRequestAds = false
        if (activity != null && !settings.premium) {
            canRequestAds = withTimeoutOrNull(8_000) { Consent.gather(activity) } ?: Consent.canRequestAds(context)
            if (canRequestAds) AppGraph.ads.initialize()
        }

        val elapsed = System.currentTimeMillis() - startedAt
        if (elapsed < MIN_SPLASH_MS) delay(MIN_SPLASH_MS - elapsed)

        // App-open ad on cold start – never on the very first launch.
        if (activity != null && canRequestAds && settings.onboardingDone && !settings.premium) {
            if (AppGraph.appOpen.awaitAvailable(AdConfig.SPLASH_APP_OPEN_TIMEOUT_MS)) {
                suspendCancellableCoroutine { cont ->
                    AppGraph.appOpen.show(activity) { if (cont.isActive) cont.resume(Unit) }
                }
            }
        }
        AppGraph.appOpen.coldStartHandled = true

        val target = if (settings.onboardingDone) Routes.HOME else Routes.ONBOARDING
        navigator.nav.navigate(target) { popUpTo(Routes.SPLASH) { inclusive = true } }
    }

    val transition = rememberInfiniteTransition(label = "splash")
    val orbit by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "orbit")
    val shimmer by transition.animateFloat(-0.4f, 1.4f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "bar")

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Emerald700, Color(0xFF005E49), Emerald900))),
        contentAlignment = Alignment.Center,
    ) {
        // Soft light blobs
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(Mint300.copy(alpha = 0.25f), Color.Transparent), Offset(size.width * 0.15f, size.height * 0.18f), size.width * 0.6f), size.width * 0.6f, Offset(size.width * 0.15f, size.height * 0.18f))
            drawCircle(Brush.radialGradient(listOf(Lime400.copy(alpha = 0.14f), Color.Transparent), Offset(size.width * 0.9f, size.height * 0.85f), size.width * 0.7f), size.width * 0.7f, Offset(size.width * 0.9f, size.height * 0.85f))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().rotate(orbit).alpha(logoAlpha.value)) {
                    val r = size.minDimension / 2 * 0.92f
                    drawCircle(Color.White.copy(alpha = 0.10f), r, center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
                    for (i in 0 until 3) {
                        val a = (i * 120.0 * PI / 180.0).toFloat()
                        drawCircle(if (i == 0) Lime400 else Mint300, if (i == 0) 6f else 4f, Offset(center.x + cos(a) * r, center.y + sin(a) * r))
                    }
                }
                Box(
                    Modifier
                        .size(118.dp)
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                        .clip(RoundedCornerShape(36.dp))
                        .background(Brush.linearGradient(listOf(Emerald500, Emerald700))),
                    contentAlignment = Alignment.Center,
                ) {
                    SparkleLogo(Modifier.size(96.dp))
                }
            }
            Spacer(Modifier.height(28.dp))
            AnimatedVisibility(showText, enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 2 }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nova Cleaner", style = MaterialTheme.typography.displaySmall, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text("Clean  •  Optimize  •  Manage", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.75f))
                }
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.width(140.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.18f))) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width * 0.4f
                    val x = size.width * shimmer - w / 2
                    drawRoundRect(
                        Brush.horizontalGradient(listOf(Color.Transparent, Color.White, Color.Transparent), startX = x, endX = x + w),
                        topLeft = Offset(x, 0f),
                        size = androidx.compose.ui.geometry.Size(w, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height),
                    )
                }
            }
            if (showAdNotice) {
                Spacer(Modifier.height(14.dp))
                Text("This action may contain ads", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.55f))
            }
        }
    }
}

private const val MIN_SPLASH_MS = 2_000L
