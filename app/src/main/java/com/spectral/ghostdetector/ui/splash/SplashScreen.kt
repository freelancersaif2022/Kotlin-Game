package com.spectral.ghostdetector.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectral.ghostdetector.R
import com.spectral.ghostdetector.audio.Sfx
import com.spectral.ghostdetector.data.GhostNames
import com.spectral.ghostdetector.ui.components.DisclaimerText
import com.spectral.ghostdetector.ui.components.GhostBackground
import com.spectral.ghostdetector.ui.components.PulseRings
import com.spectral.ghostdetector.ui.components.RadarScope
import com.spectral.ghostdetector.ui.rememberApp
import com.spectral.ghostdetector.ui.theme.GColors
import com.spectral.ghostdetector.ui.theme.GFonts
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val app = rememberApp()
    val progress = remember { Animatable(0f) }
    val logs = remember { mutableStateListOf<String>() }
    var titleAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        delay(250) // let SoundPool finish decoding
        app.sounds.play(Sfx.BOOT, 0.9f)
        launch {
            // flickering neon title
            repeat(9) {
                titleAlpha = if (it % 2 == 0) Random.nextFloat() * 0.6f + 0.3f else 0.1f
                delay(Random.nextLong(40, 110))
            }
            titleAlpha = 1f
            delay(1300)
            app.sounds.play(Sfx.GLITCH, 0.35f)
            titleAlpha = 0.25f; delay(60); titleAlpha = 1f
        }
        launch { progress.animateTo(1f, tween(3600, easing = LinearEasing)) }
        val steps = listOf(
            "Calibrating EMF sensor array",
            "Loading spectral database (${GhostNames.count.coerceAtLeast(600)}+ entities)",
            "Thermal variance module",
            "EVP frequency analyzer",
            "Night-vision optics",
            "Paranormal uplink established",
        )
        delay(350)
        for (s in steps) {
            logs += "> $s ... OK"
            app.sounds.play(Sfx.TICK, 0.5f)
            delay(520)
        }
        delay(450)
        onFinished()
    }

    GhostBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.9f))
            Box(Modifier.size(250.dp), contentAlignment = Alignment.Center) {
                PulseRings(Modifier.fillMaxSize())
                RadarScope(Modifier.size(200.dp), speedMs = 1800)
                Image(
                    painterResource(R.drawable.ic_splash_logo),
                    contentDescription = null,
                    modifier = Modifier.size(150.dp).alpha(titleAlpha.coerceAtLeast(0.4f)),
                )
            }
            Spacer(Modifier.height(26.dp))
            Text(
                "GHOST",
                modifier = Modifier.alpha(titleAlpha),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp, letterSpacing = 10.sp),
                color = GColors.Neon,
            )
            Text(
                "DETECTOR",
                modifier = Modifier.alpha(titleAlpha),
                style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 9.sp),
                color = GColors.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "SPECTRAL RADAR  ·  EMF  ·  EVP  ·  THERMAL",
                color = GColors.TextSecondary, fontFamily = GFonts.Mono, fontSize = 11.sp, letterSpacing = 2.sp,
            )
            Spacer(Modifier.weight(0.6f))

            Column(Modifier.fillMaxWidth().height(118.dp), verticalArrangement = Arrangement.Bottom) {
                logs.takeLast(5).forEach {
                    Text(it, color = GColors.NeonSoft.copy(alpha = 0.85f), fontFamily = GFonts.Mono, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("INITIALIZING", color = GColors.TextSecondary, fontFamily = GFonts.Mono, fontSize = 11.sp)
                Text("${(progress.value * 100).toInt()}%", color = GColors.Neon, fontFamily = GFonts.Mono, fontSize = 11.sp)
            }
            Spacer(Modifier.height(6.dp))
            Canvas(Modifier.fillMaxWidth().height(6.dp)) {
                val r = CornerRadius(size.height / 2)
                drawRoundRect(GColors.Neon.copy(alpha = 0.12f), cornerRadius = r)
                drawRoundRect(
                    Brush.horizontalGradient(listOf(GColors.NeonDim, GColors.Neon)),
                    size = Size(size.width * progress.value, size.height), cornerRadius = r,
                )
                drawCircle(
                    GColors.Neon.copy(alpha = 0.5f), radius = size.height * 1.6f,
                    center = Offset(size.width * progress.value, size.height / 2),
                )
            }
            Spacer(Modifier.height(22.dp))
            DisclaimerText(Modifier.fillMaxWidth().padding(bottom = 14.dp))
            Text(
                "v1.0", color = GColors.TextMuted, fontFamily = GFonts.Mono, fontSize = 9.sp,
                textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 6.dp),
            )
        }
    }
}
