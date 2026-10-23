package com.saif.pymaster.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saif.pymaster.R
import com.saif.pymaster.data.AppState
import com.saif.pymaster.ui.Routes
import com.saif.pymaster.ui.components.GradientProgressBar
import com.saif.pymaster.ui.theme.Brand
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(state: AppState?, onDone: (String) -> Unit) {
    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }
    val currentState by rememberUpdatedState(state)

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(600)) }
        launch { logoScale.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        launch {
            delay(450)
            textAlpha.animateTo(1f, tween(700))
        }
        progress.animateTo(1f, tween(2200, easing = LinearEasing))
        val s = snapshotFlow { currentState }.filterNotNull().first()
        onDone(
            when {
                s.profile != null -> Routes.HOME
                s.onboarded -> Routes.SETUP
                else -> Routes.ONBOARDING
            }
        )
    }

    val glow = rememberInfiniteTransition(label = "glow")
    val pulse by glow.animateFloat(
        0.92f, 1.04f,
        infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(Modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.splash_bg), null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painterResource(R.drawable.logo), "PyMaster logo",
                modifier = Modifier
                    .size(150.dp)
                    .scale(logoScale.value * pulse)
                    .alpha(logoAlpha.value),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Brand.White)) { append("Py") }
                    withStyle(SpanStyle(color = Brand.PythonYellow)) { append("Master") }
                },
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 38.sp, letterSpacing = 0.5.sp),
                modifier = Modifier.alpha(textAlpha.value),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Learn Python  •  Build Projects  •  Get Certified",
                style = MaterialTheme.typography.bodyMedium,
                color = Brand.TextSecondary,
                modifier = Modifier.alpha(textAlpha.value),
            )
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GradientProgressBar(
                progress.value,
                modifier = Modifier.width(160.dp),
                height = 4.dp,
                brush = Brush.horizontalGradient(listOf(Brand.PythonBlue, Brand.Cyan, Brand.PythonYellow)),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "Crafted by Saif A.",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.55f),
            )
        }
    }
}
