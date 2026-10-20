package com.flashshare.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashshare.app.R
import com.flashshare.app.data.Category
import com.flashshare.app.ui.components.*
import com.flashshare.app.ui.theme.Brand
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// region splash
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val logo = remember { Animatable(0f) }
    val text = remember { Animatable(0f) }
    val t = rememberInfiniteTransition(label = "splash")
    val halo by t.animateFloat(0.85f, 1.15f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "halo")
    LaunchedEffect(Unit) {
        launch { logo.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 200f)) }
        delay(280)
        text.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        delay(520)
        onDone()
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        AmbientBlobs(Modifier.fillMaxSize())
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(170.dp).scale(halo * logo.value).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Brand.Violet.copy(alpha = 0.28f), Color.Transparent))))
                LogoTile(Modifier.scale(0.5f + 0.5f * logo.value).graphicsLayer { alpha = logo.value.coerceIn(0f, 1f); rotationZ = -20f * (1f - logo.value) }, 112.dp)
            }
            Spacer(Modifier.height(22.dp))
            Column(
                Modifier.graphicsLayer { alpha = text.value; translationY = (1f - text.value) * 40f },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("FlashShare", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(6.dp))
                Text("Ultra-fast offline file transfer", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun LogoTile(modifier: Modifier = Modifier, size: Dp) {
    Box(
        modifier.size(size).shadow(size * 0.16f, RoundedCornerShape(size * 0.3f), spotColor = Brand.Violet)
            .clip(RoundedCornerShape(size * 0.3f)).background(Brand.main),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.ic_logo_mark), null, Modifier.fillMaxSize())
    }
}

/** Soft drifting color blobs used behind intro screens. */
@Composable
fun AmbientBlobs(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "blobs")
    val a by t.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "a")
    val c1 = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val c2 = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
    val c3 = Color(0xFF19C3FF).copy(alpha = 0.10f)
    Canvas(modifier) {
        val w = size.width; val h = size.height
        fun blob(cx: Float, cy: Float, r: Float, c: Color) = drawCircle(Brush.radialGradient(listOf(c, Color.Transparent), Offset(cx, cy), r), r, Offset(cx, cy))
        blob(w * (0.2f + 0.1f * cos(a)), h * (0.18f + 0.05f * sin(a)), w * 0.7f, c1)
        blob(w * (0.85f + 0.08f * sin(a)), h * (0.45f + 0.06f * cos(a)), w * 0.6f, c2)
        blob(w * (0.3f + 0.1f * sin(a * 2)), h * (0.85f + 0.04f * cos(a)), w * 0.6f, c3)
    }
}
// endregion

// region onboarding
private data class Page(val title: String, val text: String, val art: @Composable () -> Unit)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = remember {
        listOf(
            Page("Share at lightning speed", "Send photos, videos, apps and huge files phone-to-phone – up to 200× faster than Bluetooth.") { ArtTransfer() },
            Page("Scan once. Connected.", "The receiver shows a QR code. Scan it from the sending phone – no pairing, no waiting.") { ArtScan() },
            Page("Forced 5 GHz & 6 GHz", "FlashShare locks the direct link to the fastest Wi-Fi band. No internet and no mobile data used.") { ArtSpeed() },
            Page("Send anything, any size", "Multiple files, whole folders, apps and contacts – even clone your old phone to a new one.") { ArtOrbit() },
        )
    }
    val state = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = state.currentPage == pages.lastIndex

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AmbientBlobs(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.padding(start = 8.dp).weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    LogoTile(size = 30.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("FlashShare", style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = onFinish, modifier = Modifier.graphicsLayer { alpha = if (last) 0f else 1f }, enabled = !last) {
                    Text("Skip", fontWeight = FontWeight.SemiBold)
                }
            }
            HorizontalPager(state, Modifier.weight(1f)) { i ->
                val p = pages[i]
                val offset = (state.currentPage - i) + state.currentPageOffsetFraction
                Column(Modifier.fillMaxSize().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(1f).graphicsLayer {
                            val o = kotlin.math.abs(offset).coerceIn(0f, 1f)
                            alpha = 1f - o * 0.7f; scaleX = 1f - o * 0.2f; scaleY = scaleX; rotationY = offset * -18f
                            cameraDistance = 12f * density
                        },
                        contentAlignment = Alignment.Center,
                    ) { p.art() }
                    Spacer(Modifier.height(20.dp))
                    Text(p.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer { translationX = offset * 140f; alpha = 1f - kotlin.math.abs(offset) })
                    Spacer(Modifier.height(12.dp))
                    Text(p.text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer { translationX = offset * 240f; alpha = 1f - kotlin.math.abs(offset) })
                }
            }
            Row(Modifier.fillMaxWidth().padding(24.dp).height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f)) {
                    repeat(pages.size) { i ->
                        val selected = state.currentPage == i
                        val w by animateDpAsState(if (selected) 28.dp else 8.dp, spring(stiffness = 400f), label = "dot")
                        Box(
                            Modifier.padding(end = 6.dp).height(8.dp).width(w).clip(CircleShape)
                                .background(if (selected) Brand.main else Brush.linearGradient(listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline)))
                        )
                    }
                }
                AnimatedContent(last, transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.8f)) }, label = "next") { isLast ->
                    if (isLast) {
                        GradientButton("Get started", onClick = onFinish, icon = Icons.Rounded.RocketLaunch, modifier = Modifier.widthIn(min = 170.dp))
                    } else {
                        NextRingButton((state.currentPage + 1f) / pages.size) { scope.launch { state.animateScrollToPage(state.currentPage + 1) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextRingButton(progress: Float, onClick: () -> Unit) {
    val p by animateFloatAsState(progress, tween(500, easing = FastOutSlowInEasing), label = "ring")
    val track = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    Box(Modifier.size(64.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(track, size.minDimension / 2 - 2.dp.toPx(), style = Stroke(3.dp.toPx()))
            drawArc(primary, -90f, 360f * p, false, Offset(2.dp.toPx(), 2.dp.toPx()), Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
        Box(Modifier.size(50.dp).clip(CircleShape).background(Brand.main), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, "Next", tint = Color.White)
        }
    }
}

@Composable
private fun Stage(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize(0.94f).clip(CircleShape).background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0f)))))
        content()
    }
}

@Composable
private fun ArtTransfer() = Stage {
    val t = rememberInfiniteTransition(label = "t")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "p")
    val bob by t.animateFloat(-5f, 5f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    PhonesLink(
        left = { AvatarImpl(0, null, 46.dp, ring = true) },
        right = { AvatarImpl(3, null, 46.dp, ring = true) },
        active = true, modifier = Modifier.fillMaxWidth(0.95f), phoneWidth = 88.dp,
    )
    val cats = listOf(Category.PHOTO to "IMG_2041.jpg", Category.VIDEO to "Trip.mp4", Category.APP to "Game.apk")
    BoxWithConstraints(Modifier.fillMaxWidth(0.5f).height(210.dp)) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        cats.forEachIndexed { i, (c, name) ->
            val f = (p + i / cats.size.toFloat()) % 1f
            val arc = sin(f * PI).toFloat()
            Row(
                Modifier.offset { IntOffset((f * w - 50.dp.toPx()).roundToInt(), (h * 0.2f - arc * h * 0.22f).roundToInt()) }
                    .graphicsLayer { alpha = arc.coerceIn(0f, 1f); scaleX = 0.7f + 0.3f * arc; scaleY = scaleX }
                    .shadow(8.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FileThumb(null, c, 22.dp, corner = 6.dp)
                Spacer(Modifier.width(5.dp))
                Text(name, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827), maxLines = 1)
            }
        }
    }
    Box(
        Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).offset { IntOffset(0, bob.roundToInt()) }
            .shadow(10.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(Brand.mint).padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Speed, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("62 MB/s", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ArtScan() = Stage {
    val t = rememberInfiniteTransition(label = "s")
    val line by t.animateFloat(0.1f, 0.9f, infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "line")
    var connected by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { connected = false; delay(2600); connected = true; delay(1400) } }
    val primary = MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically) {
        PhoneMockup(Modifier.rotate(-8f), width = 100.dp, screen = Brush.linearGradient(listOf(Color.White, Color(0xFFEFF2FA)))) {
            Box(Modifier.size(70.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).padding(5.dp)) {
                StyledQr("FLASHSHARE1:demo-qr-code-onboarding", Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.width(18.dp))
        PhoneMockup(Modifier.rotate(8f), width = 100.dp, screen = Brush.linearGradient(listOf(Color(0xFF1B2140), Color(0xFF0B0F1E)))) {
            Box(Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val l = 14.dp.toPx(); val sw = 3.dp.toPx()
                    listOf(Offset.Zero to Offset(1f, 1f), Offset(size.width, 0f) to Offset(-1f, 1f), Offset(0f, size.height) to Offset(1f, -1f), Offset(size.width, size.height) to Offset(-1f, -1f))
                        .forEach { (o, d) ->
                            drawLine(primary, o, Offset(o.x + l * d.x, o.y), sw, cap = StrokeCap.Round)
                            drawLine(primary, o, Offset(o.x, o.y + l * d.y), sw, cap = StrokeCap.Round)
                        }
                    if (!connected) drawLine(Brush.horizontalGradient(listOf(Color.Transparent, primary, Color.Transparent)), Offset(0f, size.height * line), Offset(size.width, size.height * line), 3.dp.toPx())
                }
                androidx.compose.animation.AnimatedVisibility(connected, enter = scaleIn(spring(dampingRatio = 0.4f)) + fadeIn(), exit = fadeOut()) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Brand.mint), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtSpeed() = Stage {
    val t = rememberInfiniteTransition(label = "sp")
    val v by t.animateFloat(0.2f, 0.92f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "v")
    val w by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "w")
    Box(Modifier.fillMaxSize(0.82f).shadow(24.dp, CircleShape, spotColor = Brand.Violet).clip(CircleShape)
        .background(Brush.linearGradient(listOf(Color(0xFF141A3A), Color(0xFF2A1F6B))))) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height * 0.52f)
            for (i in 0 until 3) {
                val f = (w + i / 3f) % 1f
                drawCircle(Color(0xFF19C3FF).copy(alpha = (1 - f) * 0.35f), size.minDimension * (0.25f + 0.25f * f), c, style = Stroke(2.dp.toPx()))
            }
        }
        SpeedGauge(v, "${(v * 120).roundToInt()} MB/s", Modifier.fillMaxSize(0.72f).align(Alignment.Center))
    }
    Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)) {
        listOf("5 GHz" to Brand.send, "6 GHz" to Brand.receive).forEach { (s, b) ->
            Box(Modifier.padding(5.dp).shadow(6.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(b).padding(horizontal = 16.dp, vertical = 7.dp)) {
                Text(s, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ArtOrbit() = Stage {
    val t = rememberInfiniteTransition(label = "orbit")
    val a by t.animateFloat(0f, 360f, infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "a")
    val pulse by t.animateFloat(0.94f, 1.05f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "pulse")
    val items: List<Pair<ImageVector, Brush>> = listOf(
        Icons.Rounded.Image to IconBrushes.cyan, Icons.Rounded.Movie to IconBrushes.pink, Icons.Rounded.MusicNote to IconBrushes.orange,
        Icons.Rounded.Android to IconBrushes.mint, Icons.Rounded.Contacts to IconBrushes.blue, Icons.Rounded.Folder to IconBrushes.violet,
        Icons.Rounded.Description to IconBrushes.red,
    )
    BoxWithConstraints(Modifier.fillMaxSize(0.86f), contentAlignment = Alignment.Center) {
        val r = constraints.maxWidth / 2f * 0.8f
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = 0.5f), r, style = Stroke(1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 10f))))
        }
        items.forEachIndexed { i, (icon, brush) ->
            val ang = Math.toRadians((a + i * 360f / items.size).toDouble())
            val depth = (sin(ang).toFloat() + 1f) / 2f
            GlossyIcon(icon, brush, 50.dp, Modifier.offset { IntOffset((r * cos(ang)).roundToInt(), (r * sin(ang) * 0.55).roundToInt()) }
                .graphicsLayer { val s = 0.75f + 0.35f * depth; scaleX = s; scaleY = s; alpha = 0.6f + 0.4f * depth })
        }
        LogoTile(Modifier.scale(pulse), 96.dp)
    }
}
// endregion
