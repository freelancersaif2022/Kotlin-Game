package com.flashshare.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashshare.app.data.Category
import com.flashshare.app.ui.components.GradientButton
import com.flashshare.app.ui.components.categoryColor
import com.flashshare.app.ui.components.categoryIcon
import com.flashshare.app.ui.theme.Brand
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// region splash
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val logo = remember { Animatable(0f) }
    val text = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { logo.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 220f)) }
        delay(250)
        text.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        delay(450)
        onDone()
    }
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LogoTile(Modifier.scale(0.4f + 0.6f * logo.value).rotate(-25f * (1f - logo.value)).graphicsLayer { alpha = logo.value.coerceIn(0f, 1f) }, 104.dp)
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier.graphicsLayer { alpha = text.value; translationY = (1f - text.value) * 40f },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("FlashShare", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(6.dp))
                Text("Ultra-fast offline file transfer", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            "5 GHz • 6 GHz Wi-Fi Direct",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 32.dp).graphicsLayer { alpha = text.value },
        )
    }
}

@Composable
fun LogoTile(modifier: Modifier = Modifier, size: Dp) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(Brand.main),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Bolt, null, tint = Color.White, modifier = Modifier.size(size * 0.62f))
    }
}
// endregion

// region onboarding
private data class Page(val title: String, val text: String, val art: @Composable () -> Unit)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = remember {
        listOf(
            Page("Share at lightning speed", "Send photos, videos, apps and huge files phone-to-phone up to 200× faster than Bluetooth.") { ArtPhones() },
            Page("Scan once, connect instantly", "The receiver shows a QR code. Scan it from the sending phone – connected in seconds, no pairing.") { ArtScan() },
            Page("Forced 5 GHz & 6 GHz", "FlashShare locks the direct link to the fastest Wi-Fi band. No internet and no mobile data used.") { ArtWaves() },
            Page("Send anything, any size", "Multiple files, whole folders, apps, contacts – even clone your old phone to a new one.") { ArtOrbit() },
        )
    }
    val state = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = state.currentPage == pages.lastIndex

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.padding(start = 12.dp).weight(1f), verticalAlignment = Alignment.CenterVertically) {
                LogoTile(size = 28.dp)
                Spacer(Modifier.width(8.dp))
                Text("FlashShare", style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = onFinish, modifier = Modifier.graphicsLayer { alpha = if (last) 0f else 1f }, enabled = !last) {
                Text("Skip")
            }
        }
        HorizontalPager(state, Modifier.weight(1f)) { i ->
            val p = pages[i]
            val offset = ((state.currentPage - i) + state.currentPageOffsetFraction)
            Column(
                Modifier.fillMaxSize().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1f).graphicsLayer {
                        val o = kotlin.math.abs(offset).coerceIn(0f, 1f)
                        alpha = 1f - o * 0.6f; scaleX = 1f - o * 0.15f; scaleY = scaleX
                    },
                    contentAlignment = Alignment.Center,
                ) { p.art() }
                Spacer(Modifier.height(24.dp))
                Text(p.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { translationX = offset * 120f })
                Spacer(Modifier.height(12.dp))
                Text(p.text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { translationX = offset * 200f })
            }
        }
        Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f)) {
                repeat(pages.size) { i ->
                    val selected = state.currentPage == i
                    val w by animateDpAsState(if (selected) 26.dp else 8.dp, spring(stiffness = 400f), label = "dot")
                    Box(
                        Modifier.padding(end = 6.dp).height(8.dp).width(w).clip(CircleShape)
                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    )
                }
            }
            GradientButton(
                text = if (last) "Get started" else "Next",
                onClick = { if (last) onFinish() else scope.launch { state.animateScrollToPage(state.currentPage + 1) } },
                icon = if (last) Icons.Rounded.RocketLaunch else Icons.Rounded.ArrowForward,
                modifier = Modifier.widthIn(min = 140.dp),
            )
        }
    }
}

@Composable
private fun PhoneShape(modifier: Modifier, brush: Brush, content: @Composable BoxScope.() -> Unit = {}) {
    Box(
        modifier.size(96.dp, 180.dp).clip(RoundedCornerShape(22.dp)).background(brush)
            .border(4.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center, content = content,
    )
}

@Composable
private fun ArtPhones() {
    val t = rememberInfiniteTransition(label = "phones")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "p")
    val bob by t.animateFloat(-6f, 6f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize(0.92f).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)))
        Row(Modifier.fillMaxWidth(0.86f), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            PhoneShape(Modifier.offset { IntOffset(0, bob.roundToInt()) }.rotate(-8f), Brand.send) {
                Icon(Icons.Rounded.Upload, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            PhoneShape(Modifier.offset { IntOffset(0, -bob.roundToInt()) }.rotate(8f), Brand.receive) {
                Icon(Icons.Rounded.Download, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }
        val cats = listOf(Category.PHOTO, Category.VIDEO, Category.MUSIC, Category.APP)
        BoxWithConstraints(Modifier.fillMaxWidth(0.5f).height(140.dp)) {
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()
            cats.forEachIndexed { i, c ->
                val f = (p + i / cats.size.toFloat()) % 1f
                val x = f * w - 18.dp.value * 2
                val y = h / 2 - sin(f * Math.PI).toFloat() * h * 0.45f - 18.dp.value
                Box(
                    Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) }.size(36.dp)
                        .graphicsLayer { alpha = sin(f * Math.PI).toFloat().coerceIn(0f, 1f); val s = 0.7f + 0.3f * sin(f * Math.PI).toFloat(); scaleX = s; scaleY = s }
                        .clip(RoundedCornerShape(10.dp)).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) { Icon(categoryIcon(c), null, tint = categoryColor(c), modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
private fun ArtScan() {
    val t = rememberInfiniteTransition(label = "scan")
    val line by t.animateFloat(0.08f, 0.92f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "line")
    val primary = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize(0.92f).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)))
        Box(Modifier.size(200.dp).clip(RoundedCornerShape(28.dp)).background(Color.White).padding(24.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val cells = 9
                val cs = size.width / cells
                for (y in 0 until cells) for (x in 0 until cells) {
                    val corner = (x < 3 && y < 3) || (x > 5 && y < 3) || (x < 3 && y > 5)
                    val on = if (corner) (x % 2 == 0 && y % 2 == 0) || (x == 1 || y == 1).not() else ((x * 7 + y * 3) % 5 < 2)
                    if (on) drawRoundRect(Color(0xFF0E1426), Offset(x * cs + 1, y * cs + 1), androidx.compose.ui.geometry.Size(cs - 2, cs - 2),
                        androidx.compose.ui.geometry.CornerRadius(4f))
                }
                drawLine(Brush.horizontalGradient(listOf(Color.Transparent, primary, Color.Transparent)), Offset(-20f, size.height * line),
                    Offset(size.width + 20f, size.height * line), strokeWidth = 6f)
            }
        }
        Canvas(Modifier.size(244.dp)) {
            val l = 40.dp.toPx(); val sw = 6.dp.toPx()
            val c = primary
            listOf(Offset(0f, 0f) to Offset(1f, 1f), Offset(size.width, 0f) to Offset(-1f, 1f), Offset(0f, size.height) to Offset(1f, -1f), Offset(size.width, size.height) to Offset(-1f, -1f))
                .forEach { (o, d) ->
                    drawLine(c, o, Offset(o.x + l * d.x, o.y), sw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    drawLine(c, o, Offset(o.x, o.y + l * d.y), sw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                }
        }
    }
}

@Composable
private fun ArtWaves() {
    val t = rememberInfiniteTransition(label = "waves")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "p")
    val needle by t.animateFloat(0.15f, 0.92f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "needle")
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize(0.92f).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)))
        Canvas(Modifier.fillMaxSize(0.8f)) {
            val c = Offset(size.width / 2, size.height * 0.62f)
            for (i in 0 until 4) {
                val f = (p + i / 4f) % 1f
                val r = size.width * (0.18f + 0.34f * f)
                drawArc(Brush.sweepGradient(listOf(primary, secondary, primary), c), 205f, 130f, false,
                    Offset(c.x - r, c.y - r), androidx.compose.ui.geometry.Size(r * 2, r * 2), alpha = 1f - f,
                    style = Stroke(10.dp.toPx() * (1f - f * 0.5f), cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
            // speed gauge
            val gr = size.width * 0.16f
            drawArc(Color.White, 180f, 180f, false, Offset(c.x - gr, c.y - gr), androidx.compose.ui.geometry.Size(gr * 2, gr * 2), style = Stroke(8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
            drawArc(Brand.mint, 180f, 180f * needle, false, Offset(c.x - gr, c.y - gr), androidx.compose.ui.geometry.Size(gr * 2, gr * 2), style = Stroke(8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
            val a = Math.toRadians(180.0 + 180.0 * needle)
            drawLine(Color(0xFF0E1426), c, Offset(c.x + (gr * 0.8f * cos(a)).toFloat(), c.y + (gr * 0.8f * sin(a)).toFloat()), 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawCircle(Color(0xFF0E1426), 6.dp.toPx(), c)
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)) {
            listOf("5 GHz", "6 GHz").forEachIndexed { i, s ->
                Box(Modifier.padding(4.dp).clip(RoundedCornerShape(50)).background(if (i == 0) Brand.send else Brand.receive).padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Text(s, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun ArtOrbit() {
    val t = rememberInfiniteTransition(label = "orbit")
    val a by t.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "a")
    val pulse by t.animateFloat(0.92f, 1.06f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse")
    val items: List<Pair<ImageVector, Color>> = Category.entries.map { categoryIcon(it) to categoryColor(it) } + (Icons.Rounded.Folder to Color(0xFF00B0FF))
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize(0.92f).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)))
        BoxWithConstraints(Modifier.fillMaxSize(0.8f), contentAlignment = Alignment.Center) {
            val r = constraints.maxWidth / 2f * 0.78f
            items.forEachIndexed { i, (icon, color) ->
                val ang = Math.toRadians((a + i * 360f / items.size).toDouble())
                Box(
                    Modifier.offset { IntOffset((r * cos(ang)).roundToInt(), (r * sin(ang)).roundToInt()) }
                        .size(52.dp).clip(RoundedCornerShape(16.dp)).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, null, tint = color, modifier = Modifier.size(28.dp)) }
            }
            LogoTile(Modifier.scale(pulse), 96.dp)
        }
    }
}
// endregion
