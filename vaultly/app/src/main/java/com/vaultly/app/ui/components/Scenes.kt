package com.vaultly.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultly.app.ui.theme.Manrope
import com.vaultly.app.ui.theme.Palette

/** Looping 0..1 clock for a scene. */
@Composable
fun sceneClock(durationMs: Int = 4200): Float {
    val t = rememberInfiniteTransition(label = "scene")
    val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(durationMs, easing = LinearEasing), RepeatMode.Restart), label = "t")
    return v
}

/** Progress of [t] through the window [a]..[b], eased. */
fun seg(t: Float, a: Float, b: Float): Float = FastOutSlowInEasing.transform(((t - a) / (b - a)).coerceIn(0f, 1f))

/** Fades everything out at the end of each loop so it restarts cleanly. */
private fun loopAlpha(t: Float) = 1f - seg(t, 0.92f, 1f)

private val Paper = Color(0xFFFBFAF6)
private val PaperLine = Color(0xFFD9D5CB)

@Composable
private fun ReceiptCard(width: Dp, height: Dp, modifier: Modifier = Modifier) {
    Column(
        modifier.width(width).height(height).clip(RoundedCornerShape(14.dp)).background(Paper).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(Modifier.fillMaxWidth(0.7f).height(9.dp).clip(CircleShape).background(Palette.Ink.copy(alpha = 0.85f)))
        Box(Modifier.fillMaxWidth(0.45f).height(5.dp).clip(CircleShape).background(PaperLine))
        Spacer(Modifier.height(2.dp))
        repeat(4) { i ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).padding(end = 18.dp).height(5.dp).clip(CircleShape).background(PaperLine))
                Box(Modifier.width(if (i == 0) 26.dp else 20.dp).height(5.dp).clip(CircleShape).background(PaperLine))
            }
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(34.dp).height(8.dp).clip(CircleShape).background(Palette.Ink.copy(alpha = 0.8f)))
            Spacer(Modifier.weight(1f))
            Box(Modifier.width(30.dp).height(8.dp).clip(CircleShape).background(Palette.Ink.copy(alpha = 0.8f)))
        }
    }
}

@Composable
private fun FoundChip(label: String, value: String, progress: Float) {
    Row(
        Modifier
            .graphicsLayer { alpha = progress; translationX = (1f - progress) * -40.dp.toPx() }
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Palette.GoldLight.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(Color(0xFF3CC28C)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Check, null, Modifier.size(12.dp), tint = Color.White)
        }
        Spacer(Modifier.width(7.dp))
        Column {
            Text(label, fontFamily = Manrope, fontSize = 9.sp, color = Color.White.copy(alpha = 0.55f), fontWeight = FontWeight.SemiBold)
            Text(value, fontFamily = Manrope, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

/** Step 1: a receipt gets scanned and its fields pop out. */
@Composable
fun ScanScene(modifier: Modifier = Modifier) {
    val t = sceneClock()
    val a = loopAlpha(t)
    Box(modifier.size(280.dp, 240.dp).graphicsLayer { alpha = a }, contentAlignment = Alignment.Center) {
        Box(Modifier.offset(x = (-58).dp)) {
            ReceiptCard(124.dp, 190.dp)
            // Scanner beam
            val beam = seg(t, 0.06f, 0.46f)
            if (beam in 0.001f..0.999f) {
                Box(
                    Modifier.width(124.dp).height(36.dp).offset(y = (190 * beam - 18).dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Palette.GoldLight.copy(alpha = 0.75f), Color.Transparent))),
                )
            }
            // Detection frame
            val frame = seg(t, 0f, 0.1f)
            Canvas(Modifier.size(124.dp, 190.dp).graphicsLayer { alpha = frame; scaleX = 1.08f - 0.08f * frame; scaleY = 1.08f - 0.08f * frame }) {
                val c = 18.dp.toPx()
                val s = 3.dp.toPx()
                val col = Color(0xFF3B82F6)
                val w = size.width
                val h = size.height
                listOf(
                    Offset(0f, 0f) to Offset(1f, 1f), Offset(w, 0f) to Offset(-1f, 1f),
                    Offset(w, h) to Offset(-1f, -1f), Offset(0f, h) to Offset(1f, -1f),
                ).forEach { (p, d) ->
                    drawLine(col, p, Offset(p.x + c * d.x, p.y), s, StrokeCap.Round)
                    drawLine(col, p, Offset(p.x, p.y + c * d.y), s, StrokeCap.Round)
                }
            }
        }
        Column(Modifier.offset(x = 74.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FoundChip("STORE", "Northgate", seg(t, 0.46f, 0.56f))
            FoundChip("DATE", "Oct 14, 2026", seg(t, 0.56f, 0.66f))
            FoundChip("TOTAL", "$434.49", seg(t, 0.66f, 0.76f))
        }
    }
}

/** Step 2: the countdown runs down and a reminder lands. */
@Composable
fun RemindScene(modifier: Modifier = Modifier) {
    val t = sceneClock()
    val a = loopAlpha(t)
    val run = seg(t, 0.05f, 0.5f)
    val days = (30 - (27 * run)).toInt()
    Box(modifier.size(280.dp, 240.dp).graphicsLayer { alpha = a }, contentAlignment = Alignment.Center) {
        Box(Modifier.offset(y = 26.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(150.dp)) {
                val s = 11.dp.toPx()
                val tl = Offset(s / 2, s / 2)
                val arc = Size(size.width - s, size.height - s)
                drawArc(Color.White.copy(alpha = 0.1f), -90f, 360f, false, tl, arc, style = Stroke(s, cap = StrokeCap.Round))
                val color = if (days <= 7) Color(0xFFFF6B6B) else if (days <= 14) Color(0xFFF0B341) else Color(0xFF3CC28C)
                drawArc(color, -90f, 360f * (days / 30f), false, tl, arc, style = Stroke(s, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$days", fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, color = Color.White)
                Text("DAYS LEFT", fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.5.sp, color = Color.White.copy(alpha = 0.6f))
            }
        }
        val drop = seg(t, 0.52f, 0.64f)
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer { alpha = drop; translationY = (1f - drop) * -50.dp.toPx() }
                .width(250.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Palette.Ink), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.NotificationsActive, null, Modifier.size(18.dp), tint = Palette.GoldLight)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("3 days left to return", fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Palette.Ink)
                Text("Sony headphones · Northgate", fontFamily = Manrope, fontSize = 11.sp, color = Palette.Ink.copy(alpha = 0.6f))
            }
        }
    }
}

/** Step 3: a claim pack PDF assembles and is ready to share. */
@Composable
fun ClaimScene(modifier: Modifier = Modifier) {
    val t = sceneClock()
    val a = loopAlpha(t)
    Box(modifier.size(280.dp, 240.dp).graphicsLayer { alpha = a }, contentAlignment = Alignment.Center) {
        val doc = seg(t, 0f, 0.14f)
        Column(
            Modifier
                .graphicsLayer { alpha = doc; scaleX = 0.85f + 0.15f * doc; scaleY = 0.85f + 0.15f * doc }
                .width(132.dp).height(178.dp).clip(RoundedCornerShape(14.dp)).background(Paper).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.clip(RoundedCornerShape(5.dp)).background(Color(0xFFE5484D)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text("PDF", fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp, color = Color.White)
            }
            Box(Modifier.fillMaxWidth(0.8f).height(7.dp).clip(CircleShape).background(Palette.Ink.copy(alpha = 0.85f)))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val r = seg(t, 0.16f, 0.28f)
                val p = seg(t, 0.28f, 0.40f)
                Box(Modifier.graphicsLayer { alpha = r; scaleX = r; scaleY = r }.size(48.dp, 62.dp).clip(RoundedCornerShape(6.dp)).background(PaperLine))
                Box(
                    Modifier.graphicsLayer { alpha = p; scaleX = p; scaleY = p }.size(48.dp, 62.dp).clip(RoundedCornerShape(6.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF9DB4D8), Color(0xFF5C7299)))),
                )
            }
            repeat(3) { Box(Modifier.fillMaxWidth(if (it == 2) 0.5f else 0.9f).height(5.dp).clip(CircleShape).background(PaperLine)) }
        }
        val fly = seg(t, 0.46f, 0.66f)
        listOf(
            Triple(Icons.Outlined.Email, -112f, -40f),
            Triple(Icons.Outlined.Share, 112f, -40f),
            Triple(Icons.Outlined.FileDownload, 0f, 104f),
        ).forEach { (icon, x, y) ->
            Box(
                Modifier
                    .graphicsLayer { alpha = fly; translationX = x * fly * density; translationY = y * fly * density }
                    .size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Palette.GoldLight.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, Modifier.size(20.dp), tint = Palette.GoldLight) }
        }
        val ok = seg(t, 0.7f, 0.8f)
        Box(
            Modifier.offset(x = 56.dp, y = (-80).dp).graphicsLayer { scaleX = ok; scaleY = ok; alpha = ok }
                .size(36.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Palette.GoldLight, Palette.Gold))),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Check, null, Modifier.size(20.dp), tint = Palette.Ink) }
    }
}

/** Pro: purchases keep stacking with no limit. */
@Composable
fun UnlimitedScene(modifier: Modifier = Modifier) {
    val t = sceneClock(3600)
    val a = loopAlpha(t)
    val items = listOf(
        Triple(Icons.Outlined.Devices, "Headphones", "$399"),
        Triple(Icons.Outlined.Kitchen, "Coffee machine", "$249"),
        Triple(Icons.Outlined.Weekend, "Sofa", "$1,190"),
    )
    Box(modifier.size(280.dp, 210.dp).graphicsLayer { alpha = a }, contentAlignment = Alignment.Center) {
        Column(Modifier.offset(x = (-20).dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEachIndexed { i, (icon, name, price) ->
                val p = seg(t, 0.08f + i * 0.13f, 0.22f + i * 0.13f)
                Row(
                    Modifier.graphicsLayer { alpha = p; translationY = (1f - p) * 30.dp.toPx() }
                        .width(200.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.09f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(icon, null, Modifier.size(16.dp), tint = Palette.GoldLight)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(name, fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White, modifier = Modifier.weight(1f))
                    Text(price, fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
            }
        }
        val inf = seg(t, 0.5f, 0.62f)
        Box(
            Modifier.offset(x = 104.dp, y = (-62).dp).graphicsLayer { scaleX = inf; scaleY = inf; alpha = inf }
                .size(54.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Palette.GoldLight, Palette.Gold))),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.AllInclusive, null, Modifier.size(28.dp), tint = Palette.Ink) }
    }
}

/** Pro: the ad slot gets crossed out and disappears. */
@Composable
fun NoAdsScene(modifier: Modifier = Modifier) {
    val t = sceneClock(3600)
    val a = loopAlpha(t)
    val strike = seg(t, 0.25f, 0.45f)
    val gone = seg(t, 0.5f, 0.62f)
    Box(modifier.size(280.dp, 210.dp).graphicsLayer { alpha = a }, contentAlignment = Alignment.Center) {
        Box(
            Modifier.graphicsLayer { alpha = 1f - gone; scaleX = 1f - 0.2f * gone; scaleY = 1f - 0.2f * gone }
                .width(220.dp).height(86.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("AD", fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Color.White.copy(alpha = 0.4f), letterSpacing = 6.sp)
            Canvas(Modifier.fillMaxSize()) {
                drawLine(
                    Color(0xFFFF6B6B), Offset(24.dp.toPx(), size.height - 18.dp.toPx()),
                    Offset(24.dp.toPx() + (size.width - 48.dp.toPx()) * strike, size.height - 18.dp.toPx() - (size.height - 36.dp.toPx()) * strike),
                    5.dp.toPx(), StrokeCap.Round,
                )
            }
        }
        val calm = seg(t, 0.6f, 0.74f)
        Column(Modifier.graphicsLayer { alpha = calm; scaleX = 0.9f + 0.1f * calm; scaleY = 0.9f + 0.1f * calm }, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(58.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Palette.GoldLight, Palette.Gold))), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Check, null, Modifier.size(30.dp), tint = Palette.Ink)
            }
            Spacer(Modifier.height(10.dp))
            Text("100% ad-free", fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        }
    }
}

@Composable
fun SceneIcon(icon: ImageVector) = Icon(icon, null, tint = Palette.GoldLight)
