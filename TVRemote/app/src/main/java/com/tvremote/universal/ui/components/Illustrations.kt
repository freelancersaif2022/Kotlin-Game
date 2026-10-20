package com.tvremote.universal.ui.components

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
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvremote.universal.data.TvPlatform
import com.tvremote.universal.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// ============================================================== building blocks

/** A stylised flat-screen TV whose screen hosts [screen] content. */
@Composable
fun MiniTv(width: Dp, modifier: Modifier = Modifier, glow: Float = 0f, screen: @Composable BoxScope.() -> Unit) {
    val extra = AppTheme.extra
    val scheme = MaterialTheme.colorScheme
    val height = width * 0.6f
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(width, height)
                .shadow(16.dp + (16 * glow).dp, RoundedCornerShape(12.dp), ambientColor = extra.glow, spotColor = extra.glow)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0B0F1A))
                .padding(5.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF0A1640).copy(alpha = 1f),
                            scheme.primary.copy(alpha = 0.55f + 0.45f * glow),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
            content = screen,
        )
        Box(
            Modifier
                .width(width * 0.22f)
                .height(8.dp)
                .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                .background(Color(0xFF2A3245)),
        )
    }
}

/** A stylised phone outline. */
@Composable
fun MiniPhone(width: Dp, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val extra = AppTheme.extra
    Box(
        modifier
            .size(width, width * 1.9f)
            .shadow(14.dp, RoundedCornerShape(width * 0.18f), ambientColor = extra.keyShadow, spotColor = extra.keyShadow)
            .clip(RoundedCornerShape(width * 0.18f))
            .background(Color(0xFF0B0F1A))
            .padding(4.dp)
            .clip(RoundedCornerShape(width * 0.15f))
            .background(extra.panel),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

// ============================================================== onboarding lessons

/** Lesson 1: the phone sends signals to the TV, which lights up. */
@Composable
fun SignalIllustration(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "signal")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "p")
    val glow = ((p - 0.55f) / 0.45f).coerceIn(0f, 1f)
    val scheme = MaterialTheme.colorScheme
    Box(modifier.size(280.dp, 260.dp)) {
        MiniTv(200.dp, Modifier.align(Alignment.TopEnd), glow = glow) {
            TvControlScreen()
        }
        Canvas(Modifier.fillMaxSize()) {
            val origin = Offset(size.width * 0.2f, size.height * 0.62f)
            for (i in 0 until 3) {
                val local = (p + i / 3f) % 1f
                val r = 20.dp.toPx() + local * size.width * 0.5f
                drawArc(
                    color = scheme.primary.copy(alpha = (1f - local) * 0.85f),
                    startAngle = -80f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(origin.x - r, origin.y - r),
                    size = Size(r * 2, r * 2),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        MiniPhone(64.dp, Modifier.align(Alignment.BottomStart).offset(x = 18.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(AppTheme.extra.power))
                Box(Modifier.size(30.dp).clip(CircleShape).background(brandBrush()))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { Box(Modifier.size(8.dp).clip(CircleShape).background(scheme.outline)) }
                }
            }
        }
    }
}

/** Lesson 2: scan the Wi-Fi, find the TV and type the code once. */
@Composable
fun PairingLessonIllustration(modifier: Modifier = Modifier) {
    val code = "4A9F2C"
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            for (i in 0..code.length + 3) {
                step = i
                delay(if (i == 0) 900 else 380)
            }
        }
    }
    val typed = step.coerceAtMost(code.length)
    val done = step > code.length
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        MiniTv(210.dp, glow = if (done) 1f else 0f) {
            if (done) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(AppTheme.extra.success),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp)) }
                    Spacer(Modifier.height(4.dp))
                    Text("Paired", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Pairing code", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                    Text(code, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                }
            }
        }
        Spacer(Modifier.height(26.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            code.forEachIndexed { i, c ->
                val filled = i < typed
                val active = i == typed && !done
                Box(
                    Modifier
                        .size(34.dp, 42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppTheme.extra.panel)
                        .border(
                            if (active || done) 2.dp else 1.dp,
                            when {
                                done -> AppTheme.extra.success
                                active -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            },
                            RoundedCornerShape(10.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (filled) Text("$c", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier
                    .size(30.dp)
                    .graphicsLayer { alpha = if (done) 1f else 0f }
                    .clip(CircleShape)
                    .background(AppTheme.extra.success),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) }
        }
    }
}

/** Lesson 3: a remote whose keys light up in turn, plus the extra features. */
@Composable
fun RemoteLessonIllustration(modifier: Modifier = Modifier) {
    var active by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            active = (active + 1) % 5
            delay(550)
        }
    }
    val scheme = MaterialTheme.colorScheme
    val extra = AppTheme.extra
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(180.dp)
                .shadow(16.dp, CircleShape, ambientColor = extra.glow, spotColor = extra.glow)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(extra.keyTop, extra.keyBottom)))
                .border(2.dp, Brush.sweepGradient(listOf(scheme.primary, scheme.secondary, scheme.primary)), CircleShape),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val c = center
                val r = size.minDimension * 0.36f
                val angles = listOf(-90f, 0f, 90f, 180f)
                angles.forEachIndexed { i, a ->
                    val rad = Math.toRadians(a.toDouble())
                    val pos = Offset(c.x + r * cos(rad).toFloat(), c.y + r * sin(rad).toFloat())
                    if (active == i) drawCircle(scheme.primary.copy(alpha = 0.22f), radius = 26.dp.toPx(), center = pos)
                    rotate(a + 90f, pivot = pos) {
                        val path = Path().apply {
                            moveTo(pos.x - 8.dp.toPx(), pos.y + 4.dp.toPx())
                            lineTo(pos.x, pos.y - 5.dp.toPx())
                            lineTo(pos.x + 8.dp.toPx(), pos.y + 4.dp.toPx())
                        }
                        drawPath(
                            path,
                            if (active == i) scheme.primary else extra.keyIcon,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                }
            }
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .scale(if (active == 4) 0.9f else 1f)
                    .clip(CircleShape)
                    .background(brandBrush()),
                contentAlignment = Alignment.Center,
            ) { Text("OK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        }
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(Icons.Rounded.Keyboard, Icons.Rounded.TouchApp, Icons.Rounded.Apps, Icons.Rounded.PowerSettingsNew).forEachIndexed { i, icon ->
                val on = active % 4 == i
                Box(
                    Modifier
                        .size(48.dp)
                        .shadow(if (on) 10.dp else 3.dp, RoundedCornerShape(14.dp), ambientColor = extra.glow, spotColor = extra.glow)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (on) brandBrush() else Brush.verticalGradient(listOf(extra.keyTop, extra.keyBottom))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = if (on) Color.White else extra.keyIcon, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

// ============================================================== discovery radar

/** Radar with a rotating sweep beam and blips for the TVs found so far. */
@Composable
fun RadarScanner(active: Boolean, found: Int, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val extra = AppTheme.extra
    val t = rememberInfiniteTransition(label = "radar")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "angle")
    val pulse by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "pulse")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            drawCircle(Brush.radialGradient(listOf(scheme.primary.copy(alpha = 0.16f), Color.Transparent), center, r), r)
            for (i in 1..4) {
                drawCircle(scheme.primary.copy(alpha = 0.16f), radius = r * i / 4f, style = Stroke(1.dp.toPx()))
            }
            drawLine(scheme.primary.copy(alpha = 0.12f), Offset(center.x - r, center.y), Offset(center.x + r, center.y), 1.dp.toPx())
            drawLine(scheme.primary.copy(alpha = 0.12f), Offset(center.x, center.y - r), Offset(center.x, center.y + r), 1.dp.toPx())
            if (active) {
                rotate(angle) {
                    drawCircle(
                        Brush.sweepGradient(
                            0f to Color.Transparent,
                            0.75f to Color.Transparent,
                            1f to scheme.secondary.copy(alpha = 0.55f),
                            center = center,
                        ),
                        radius = r,
                    )
                    drawLine(scheme.secondary, center, Offset(center.x + r, center.y), 2.dp.toPx(), cap = StrokeCap.Round)
                }
                drawCircle(scheme.primary.copy(alpha = (1 - pulse) * 0.5f), radius = r * pulse, style = Stroke(2.dp.toPx()))
            }
            // Blips for discovered TVs
            val spots = listOf(0.62f to 35f, 0.78f to 140f, 0.5f to 230f, 0.85f to 300f, 0.4f to 95f, 0.7f to 190f)
            for (i in 0 until found.coerceAtMost(spots.size)) {
                val (d, a) = spots[i]
                val rad = Math.toRadians(a.toDouble())
                val pos = Offset(center.x + r * d * cos(rad).toFloat(), center.y + r * d * sin(rad).toFloat())
                drawCircle(extra.success.copy(alpha = 0.3f), 9.dp.toPx(), pos)
                drawCircle(extra.success, 4.5.dp.toPx(), pos)
            }
        }
        Box(
            Modifier
                .size(70.dp)
                .shadow(18.dp, CircleShape, ambientColor = extra.glow, spotColor = extra.glow)
                .clip(CircleShape)
                .background(brandBrush()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

// ============================================================== per-platform pairing animation

/** Shows what pairing looks like on the TV for this platform (used in Pair and Help). */
@Composable
fun PairingAnimation(platform: TvPlatform, modifier: Modifier = Modifier, tvWidth: Dp = 220.dp) {
    when (platform) {
        TvPlatform.ANDROID_TV -> CodeTypingAnimation("4A9F2C", "Pairing code", modifier, tvWidth)
        TvPlatform.VIZIO -> CodeTypingAnimation("4071", "Enter this PIN", modifier, tvWidth)
        TvPlatform.ROKU -> InstantConnectAnimation(modifier, tvWidth)
        TvPlatform.SAMSUNG -> AllowPopupAnimation("Allow \"Universal Remote\" to connect?", "Deny", "Allow", modifier, tvWidth)
        TvPlatform.LG -> AllowPopupAnimation("Universal Remote is requesting access", "Decline", "Accept", modifier, tvWidth)
        TvPlatform.FIRE_TV -> AllowPopupAnimation("Allow USB debugging?", "Cancel", "OK", modifier, tvWidth)
    }
}

@Composable
private fun CodeTypingAnimation(code: String, label: String, modifier: Modifier, tvWidth: Dp) {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(code) {
        while (true) {
            for (i in 0..code.length + 3) {
                step = i
                delay(if (i == 0) 800 else 360)
            }
        }
    }
    val typed = step.coerceAtMost(code.length)
    val done = step > code.length
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        MiniTv(tvWidth) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                Text(code, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            code.forEachIndexed { i, c ->
                val active = i == typed && !done
                Box(
                    Modifier
                        .size(32.dp, 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppTheme.extra.panel)
                        .border(
                            if (active || done) 2.dp else 1.dp,
                            when {
                                done -> AppTheme.extra.success
                                active -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            },
                            RoundedCornerShape(10.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (i < typed) Text("$c", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun AllowPopupAnimation(question: String, no: String, yes: String, modifier: Modifier, tvWidth: Dp) {
    val t = rememberInfiniteTransition(label = "allow")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "p")
    val pressed = p in 0.55f..0.68f
    val accepted = p > 0.68f
    val extra = AppTheme.extra
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        MiniTv(tvWidth, glow = if (accepted) 1f else 0.2f) {
            if (!accepted) {
                Column(
                    Modifier
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xF0151B2B))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(question, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 4.dp),
                        ) { Text(no, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp) }
                        Box(
                            Modifier
                                .scale(if (pressed) 0.9f else 1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .border(2.dp, Color.White.copy(alpha = if (p > 0.25f) 0.9f else 0f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) { Text(yes, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            } else {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(extra.success),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp)) }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            if (accepted) "Connected" else "Press \"$yes\" with your TV remote",
            style = MaterialTheme.typography.labelLarge,
            color = if (accepted) extra.success else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InstantConnectAnimation(modifier: Modifier, tvWidth: Dp) {
    val t = rememberInfiniteTransition(label = "instant")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "p")
    val extra = AppTheme.extra
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        MiniTv(tvWidth, glow = p) {
            Box(
                Modifier.size(40.dp).graphicsLayer { alpha = p; scaleX = 0.6f + 0.4f * p; scaleY = 0.6f + 0.4f * p }
                    .clip(CircleShape).background(extra.success),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp)) }
        }
        Spacer(Modifier.height(14.dp))
        Text("No code needed — connects instantly", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ============================================================== success check

/** A check mark that draws itself inside glowing rings. */
@Composable
fun SuccessCheck(modifier: Modifier = Modifier, size: Dp = 170.dp) {
    val extra = AppTheme.extra
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
    val t = rememberInfiniteTransition(label = "ring")
    val ring by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2
            drawCircle(extra.success.copy(alpha = (1 - ring) * 0.35f), radius = r * (0.55f + 0.45f * ring), style = Stroke(3.dp.toPx()))
            drawCircle(extra.success.copy(alpha = 0.10f), radius = r * 0.72f)
        }
        Box(
            Modifier
                .size(size * 0.5f)
                .scale(0.6f + 0.4f * progress.value)
                .shadow(20.dp, CircleShape, ambientColor = extra.success, spotColor = extra.success)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF32D583), Color(0xFF079455)))),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = this.size.width
                val path = Path().apply {
                    moveTo(w * 0.28f, w * 0.52f)
                    lineTo(w * 0.44f, w * 0.67f)
                    lineTo(w * 0.73f, w * 0.36f)
                }
                val measure = PathMeasure().apply { setPath(path, false) }
                val partial = Path()
                measure.getSegment(0f, measure.length * progress.value, partial, true)
                drawPath(partial, Color.White, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
            }
        }
    }
}


// ============================================================== TV home screen being controlled

private val tileColors = listOf(Color(0xFFFF0033), Color(0xFF141414), Color(0xFF00A8E1), Color(0xFF0B1C5A))

/** A tiny TV home screen: the focus moves across app tiles and the volume bar reacts. */
@Composable
fun TvControlScreen() {
    var focus by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(900)
            focus = (focus + 1) % 8
        }
    }
    val tile = focus % 4
    val volume = 0.3f + 0.15f * (focus % 5)
    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF32D583)))
            Spacer(Modifier.width(4.dp))
            Text("Home", color = Color.White.copy(alpha = 0.85f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            tileColors.forEachIndexed { i, c ->
                val focused = i == tile
                Box(
                    Modifier
                        .weight(1f)
                        .height(26.dp)
                        .scale(if (focused) 1.12f else 1f)
                        .clip(RoundedCornerShape(5.dp))
                        .background(c)
                        .border(if (focused) 2.dp else 0.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(5.dp)),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("VOL", color = Color.White.copy(alpha = 0.7f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))) {
                Box(Modifier.fillMaxWidth(volume).height(4.dp).clip(CircleShape).background(Color(0xFF00C2FF)))
            }
        }
    }
}

// ============================================================== help: power on

private fun powerSetting(platform: TvPlatform): String = when (platform) {
    TvPlatform.ANDROID_TV -> "Turn on \"Network standby\" or \"Remote start\""
    TvPlatform.SAMSUNG -> "Turn on \"Power On with Mobile\""
    TvPlatform.LG -> "Turn on \"Turn on via Wi-Fi\""
    TvPlatform.ROKU -> "Turn on \"Fast TV start\""
    TvPlatform.VIZIO -> "Set Power Mode to \"Quick Start\""
    TvPlatform.FIRE_TV -> "Enable HDMI-CEC on your TV"
}

/** Phone sends a wake signal, the sleeping TV powers on. */
@Composable
fun PowerOnAnimation(platform: TvPlatform, modifier: Modifier = Modifier, tvWidth: Dp = 200.dp) {
    val t = rememberInfiniteTransition(label = "power")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "p")
    val on = p > 0.55f
    val scheme = MaterialTheme.colorScheme
    val extra = AppTheme.extra
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(tvWidth + 70.dp, tvWidth * 0.6f + 40.dp)) {
            MiniTv(tvWidth, Modifier.align(Alignment.TopEnd), glow = if (on) 1f else 0f) {
                if (on) {
                    TvControlScreen()
                } else {
                    Box(Modifier.fillMaxSize().background(Color(0xFF05070D)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null, tint = Color.White.copy(alpha = 0.25f), modifier = Modifier.size(26.dp))
                        Text("Standby", color = Color.White.copy(alpha = 0.35f), fontSize = 9.sp)
                    }
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                if (p in 0.08f..0.6f) {
                    val origin = Offset(28.dp.toPx(), size.height - 30.dp.toPx())
                    for (i in 0 until 3) {
                        val local = ((p - 0.08f) / 0.52f * 1.5f + i / 3f) % 1f
                        val r = 16.dp.toPx() + local * size.width * 0.45f
                        drawArc(
                            color = scheme.primary.copy(alpha = (1f - local) * 0.9f),
                            startAngle = -75f,
                            sweepAngle = 60f,
                            useCenter = false,
                            topLeft = Offset(origin.x - r, origin.y - r),
                            size = Size(r * 2, r * 2),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                }
            }
            MiniPhone(36.dp, Modifier.align(Alignment.BottomStart).offset(x = 10.dp)) {
                Box(
                    Modifier.size(20.dp).clip(CircleShape).background(extra.power),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp)) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.clip(RoundedCornerShape(12.dp)).background(scheme.primary.copy(alpha = 0.1f)).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(powerSetting(platform), style = MaterialTheme.typography.labelMedium, color = scheme.primary)
        }
    }
}

// ============================================================== help: fix issues

private fun fixChecks(platform: TvPlatform): List<String> = listOf("Phone and TV on the same Wi-Fi") + when (platform) {
    TvPlatform.ANDROID_TV -> listOf("Update Android TV Remote Service", "Restart the TV and pair again")
    TvPlatform.SAMSUNG -> listOf("Allow in Device Connection Manager", "Turn on IP Remote")
    TvPlatform.LG -> listOf("Turn on LG Connect Apps", "Restart the TV and pair again")
    TvPlatform.ROKU -> listOf("Control by mobile apps: Default", "Restart the Roku")
    TvPlatform.VIZIO -> listOf("TV fully on, not in standby", "Update SmartCast")
    TvPlatform.FIRE_TV -> listOf("ADB debugging turned on", "Select OK on the TV popup")
}

/** A checklist that ticks itself off, then shows "Connected". */
@Composable
fun FixIssuesAnimation(platform: TvPlatform, modifier: Modifier = Modifier) {
    val checks = remember(platform) { fixChecks(platform) }
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(platform) {
        while (true) {
            for (i in 0..checks.size + 2) {
                step = i
                delay(if (i == 0) 600 else 750)
            }
        }
    }
    val scheme = MaterialTheme.colorScheme
    val extra = AppTheme.extra
    val t = rememberInfiniteTransition(label = "fix")
    val pulse by t.animateFloat(0.5f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "pulse")
    Column(modifier.padding(horizontal = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        checks.forEachIndexed { i, label ->
            val done = i < step
            val active = i == step
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(extra.panel)
                    .border(1.dp, if (done) extra.success.copy(alpha = 0.5f) else extra.panelBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (done) extra.success else Color.Transparent)
                        .border(2.dp, if (done) extra.success else scheme.primary.copy(alpha = if (active) pulse else 0.3f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
            }
        }
        val solved = step > checks.size
        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = if (solved) 1f else 0f }
                .clip(RoundedCornerShape(14.dp))
                .background(extra.success.copy(alpha = 0.14f))
                .padding(10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = extra.success, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Fixed — your TV connects again", style = MaterialTheme.typography.labelLarge, color = extra.success)
        }
    }
}
