package com.vaultly.app.ui.paywall

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaultly.app.ui.components.PrimaryButton
import com.vaultly.app.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private data class Confetti(val x: Float, val delay: Float, val speed: Float, val drift: Float, val spin: Float, val color: Color, val w: Float, val h: Float)

/** Full-screen "You're Pro" moment: confetti, a springing crown and the unlocked features. */
@Composable
fun ProCelebration(onDone: () -> Unit) {
    BackHandler(onBack = onDone)
    val pieces = remember {
        val rnd = Random(42)
        val palette = listOf(Palette.GoldLight, Palette.Gold, Color.White, Color(0xFF8AB8FF), Color(0xFF3CC28C), Color(0xFFFF8A80))
        List(90) {
            Confetti(
                x = rnd.nextFloat(), delay = rnd.nextFloat() * 0.5f, speed = 0.6f + rnd.nextFloat() * 0.7f,
                drift = (rnd.nextFloat() - 0.5f) * 0.25f, spin = rnd.nextFloat() * 720f - 360f,
                color = palette[rnd.nextInt(palette.size)], w = 6f + rnd.nextFloat() * 6f, h = 10f + rnd.nextFloat() * 8f,
            )
        }
    }
    val clock = rememberInfiniteTransition(label = "confetti")
    val t by clock.animateFloat(0f, 1f, infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart), label = "t")

    val crown = remember { Animatable(0.2f) }
    val glow = remember { Animatable(0f) }
    val rows = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(Unit) {
        launch { crown.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
        launch { glow.animateTo(1f, tween(900)) }
        delay(450)
        rows.forEach { r ->
            launch { r.animateTo(1f, spring(dampingRatio = 0.7f)) }
            delay(160)
        }
    }

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF22335A), Palette.Ink, Palette.InkDeep))),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Soft gold halo behind the crown
            drawCircle(
                Brush.radialGradient(
                    listOf(Palette.GoldLight.copy(alpha = 0.35f * glow.value), Color.Transparent),
                    center = Offset(size.width / 2, size.height * 0.30f), radius = size.width * 0.6f,
                ),
                radius = size.width * 0.6f,
                center = Offset(size.width / 2, size.height * 0.30f),
            )
            pieces.forEach { p ->
                val local = ((t - p.delay) * p.speed).let { if (it < 0) it + 1 else it } % 1f
                val y = -40f + local * (size.height + 80f)
                val x = (p.x + p.drift * local) * size.width
                rotate(p.spin * local, Offset(x, y)) {
                    drawRect(p.color, Offset(x - p.w / 2, y - p.h / 2), Size(p.w * density / 2.5f, p.h * density / 2.5f))
                }
            }
        }
        Column(
            Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.graphicsLayer { scaleX = crown.value; scaleY = crown.value }
                    .size(118.dp).clip(RoundedCornerShape(36.dp))
                    .background(Brush.linearGradient(listOf(Palette.GoldLight, Palette.Gold, Palette.GoldDeep)))
                    .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(36.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.WorkspacePremium, null, Modifier.size(64.dp), tint = Palette.Ink) }
            Spacer(Modifier.height(28.dp))
            Text("WELCOME TO VAULTLY PRO", style = MaterialTheme.typography.labelSmall, color = Palette.GoldLight)
            Spacer(Modifier.height(10.dp))
            Text("You're Pro ✦", style = MaterialTheme.typography.displayMedium, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(
                "Thank you for supporting Vaultly. Everything is unlocked.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            val features = listOf(
                Icons.Outlined.AllInclusive to "Unlimited vault",
                Icons.Outlined.PictureAsPdf to "Unlimited claim packs",
                Icons.Outlined.Block to "No ads, ever",
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                features.forEachIndexed { i, (icon, label) -> Unlocked(icon, label, rows[i].value) }
            }
            Spacer(Modifier.height(36.dp))
            PrimaryButton("Start using Pro", onDone, Modifier.fillMaxWidth(), gold = true)
        }
    }
}

@Composable
private fun Unlocked(icon: ImageVector, label: String, progress: Float) {
    Row(
        Modifier
            .graphicsLayer { alpha = progress; translationY = (1f - progress) * 24.dp.toPx() }
            .width(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Palette.GoldLight.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = Palette.GoldLight)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = Color.White, modifier = Modifier.weight(1f))
        Box(Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF3CC28C)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Check, null, Modifier.size(15.dp), tint = Color.White)
        }
    }
}
