package com.pdfmaster.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.pdfmaster.app.R
import com.pdfmaster.app.ui.LocalAppDeps
import com.pdfmaster.app.ui.navigation.Routes
import com.pdfmaster.app.ui.theme.AppTheme
import com.pdfmaster.app.ui.theme.Brand
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun SplashScreen(nav: NavHostController) {
    val deps = LocalAppDeps.current
    val logoScale = remember { Animatable(0.5f) }
    val logoAlpha = remember { Animatable(0f) }
    val wordAlpha = remember { Animatable(0f) }
    val wordOffset = remember { Animatable(24f) }
    val tagAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(380)) }
        launch { logoScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
        delay(380)
        launch { wordAlpha.animateTo(1f, tween(500)) }
        launch { wordOffset.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) }
        delay(300)
        tagAlpha.animateTo(1f, tween(500))
    }

    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        withTimeoutOrNull(4_000) { deps.ads.consentDone.first { it } }
        val onboarded = deps.prefs.onboardingDone.first()
        val elapsed = System.currentTimeMillis() - start
        if (elapsed < 1_900) delay(1_900 - elapsed)
        nav.navigate(if (onboarded) Routes.HOME else Routes.ONBOARDING) {
            popUpTo(Routes.SPLASH) { inclusive = true }
        }
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val ring by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "ring")
    val bar by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing)), label = "bar")
    val bg = MaterialTheme.colorScheme.background
    val glow = if (AppTheme.colors.isDark) Brand.Red.copy(alpha = 0.18f) else Brand.RedSoft

    Box(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(glow, bg), radius = 1400f)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(260.dp)) {
            for (i in 0..1) {
                val t = (ring + i * 0.5f) % 1f
                drawCircle(Brand.Red.copy(alpha = (1f - t) * 0.12f * logoAlpha.value), radius = size.minDimension / 2 * (0.45f + t * 0.55f))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painterResource(R.drawable.img_logo_mark), contentDescription = null,
                modifier = Modifier.size(132.dp).graphicsLayer {
                    scaleX = logoScale.value; scaleY = logoScale.value; alpha = logoAlpha.value
                },
            )
            Spacer(Modifier.height(18.dp))
            Text(
                buildTitle(),
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.graphicsLayer { alpha = wordAlpha.value; translationY = wordOffset.value * density },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "All-in-One PDF Toolkit",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { alpha = tagAlpha.value },
            )
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 48.dp).graphicsLayer { alpha = tagAlpha.value },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.width(120.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.35f)
                        .height(4.dp)
                        .graphicsLayer { translationX = (bar * 1.65f - 0.35f) * 120.dp.toPx() }
                        .clip(RoundedCornerShape(2.dp))
                        .background(Brand.RedHorizontal),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text("Fast · Secure · Works offline", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun buildTitle() = androidx.compose.ui.text.buildAnnotatedString {
    pushStyle(androidx.compose.ui.text.SpanStyle(color = Brand.Red))
    append("PDF")
    pop()
    pushStyle(androidx.compose.ui.text.SpanStyle(color = MaterialTheme.colorScheme.onBackground))
    append(" Master")
    pop()
}
