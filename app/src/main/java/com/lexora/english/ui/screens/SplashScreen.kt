package com.lexora.english.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.lexora.english.R
import com.lexora.english.ui.components.AppImage
import com.lexora.english.ui.components.LinearProgress
import com.lexora.english.ui.theme.Brand
import com.lexora.english.ui.theme.Lex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val c = Lex.colors
    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val wordAlpha = remember { Animatable(0f) }
    val wordOffset = remember { Animatable(24f) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(450)) }
        launch { logoScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
        delay(350)
        launch { wordAlpha.animateTo(1f, tween(500)) }
        launch { wordOffset.animateTo(0f, tween(500, easing = FastOutSlowInEasing)) }
        progress.animateTo(1f, tween(1300, easing = FastOutSlowInEasing))
        delay(150)
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (c.isDark) listOf(c.background, c.primarySoft.copy(alpha = 0.6f))
                    else listOf(c.background, c.primarySoft)
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppImage(
                R.drawable.logo_mark,
                Modifier
                    .size(132.dp)
                    .graphicsLayer {
                        scaleX = logoScale.value; scaleY = logoScale.value; alpha = logoAlpha.value
                    },
                crossfade = false,
                contentDescription = "Lexora",
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "Lexora",
                style = MaterialTheme.typography.displayMedium.copy(letterSpacing = (-0.5).sp),
                color = c.text,
                modifier = Modifier.graphicsLayer { alpha = wordAlpha.value; translationY = wordOffset.value },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Master English, A to Z",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                modifier = Modifier.alpha(wordAlpha.value),
            )
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LinearProgress(
                progress.value,
                Modifier
                    .width(120.dp),
                height = 4.dp,
                brush = Brush.horizontalGradient(listOf(Brand.Blue, Brand.Indigo)),
            )
            Spacer(Modifier.height(14.dp))
            Text("100% free · No ads", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
        }
    }
}
