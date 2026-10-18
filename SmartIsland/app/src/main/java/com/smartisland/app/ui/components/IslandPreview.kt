package com.smartisland.app.ui.components

import android.media.AudioManager
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartisland.app.data.IslandSettings
import com.smartisland.app.island.IslandActivity
import com.smartisland.app.island.ui.IslandOuterPadding
import com.smartisland.app.island.ui.IslandView
import com.smartisland.app.ui.theme.Palette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/** Sample content for previews, onboarding and the "Try it" buttons. */
object Demo {
    fun media(playing: Boolean = true) = IslandActivity.Media(
        title = "Midnight Drive",
        artist = "Neon Coast",
        art = null,
        accent = 0xFFFF375F.toInt(),
        isPlaying = playing,
        positionMs = 72_000,
        durationMs = 214_000,
        speed = 1f,
        sampledAt = SystemClock.elapsedRealtime(),
        packageName = "demo",
    )

    fun incomingCall() = IslandActivity.Call(
        key = "demo-call", caller = "Alex Morgan", isIncoming = true, startedAt = 0L,
        avatar = null, answer = null, decline = null, hangUp = null, open = null, isDemo = true,
    )

    fun ongoingCall() = incomingCall().copy(isIncoming = false, startedAt = System.currentTimeMillis() - 83_000)

    fun charging() = IslandActivity.Charging(level = 78, isCharging = true, isFast = true, watts = 27.4f)

    fun notice() = IslandActivity.Notice(
        key = "demo-notice", appName = "Messages", title = "Sara",
        text = "Dinner at 8? I booked the rooftop place 🌆", icon = null, open = null,
    )

    fun volume() = IslandActivity.Volume(percent = 64, muted = false)

    fun silent() = IslandActivity.Ringer(AudioManager.RINGER_MODE_SILENT)

    /** A scripted "video": activity, expanded, how long the step stays on screen. */
    private data class Step(val activity: IslandActivity, val expanded: Boolean, val ms: Long)

    private val media = media()
    private val steps = listOf(
        Step(IslandActivity.Idle, false, 1400),
        Step(media, false, 2000),
        Step(media, true, 3000),
        Step(charging(), false, 2200),
        Step(incomingCall(), true, 2600),
        Step(ongoingCall(), false, 2200),
        Step(notice(), true, 2600),
        Step(volume(), false, 1600),
    )

    internal val stepCount get() = steps.size
    internal fun step(i: Int): Pair<IslandActivity, Boolean> = steps[i].let { it.activity to it.expanded }
    internal fun stepMs(i: Int): Long = steps[i].ms
}

/**
 * The top of a real-looking phone (bezel, lock-screen wallpaper, status bar) with the island drawn at
 * its true size and offset, so the sliders show exactly what the user will get.
 */
@Composable
fun PhoneTopPreview(
    settings: IslandSettings,
    activity: IslandActivity,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 230.dp,
    onExpandedChange: (Boolean) -> Unit = {},
) {
    val outer = RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp, bottomStart = 30.dp, bottomEnd = 30.dp)
    val inner = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(outer)
            .background(Brush.verticalGradient(listOf(Color(0xFF3A3A44), Color(0xFF15151B))))
            .padding(1.5.dp)
            .clip(outer)
            .background(Color(0xFF050507))
            .padding(6.dp),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .clip(inner)
                .background(Palette.WallpaperGradient)
                .drawWithCache {
                    // Soft color blobs give the wallpaper depth; the brushes are built once per size,
                    // not on every frame while the screen scrolls.
                    val pinkCenter = Offset(size.width * 0.15f, size.height * 0.95f)
                    val blueCenter = Offset(size.width * 0.9f, size.height * 0.35f)
                    val pink = Brush.radialGradient(listOf(Color(0x99FF5EA8), Color.Transparent), center = pinkCenter, radius = size.width * 0.55f)
                    val blue = Brush.radialGradient(listOf(Color(0x8854C7FF), Color.Transparent), center = blueCenter, radius = size.width * 0.5f)
                    onDrawBehind {
                        drawCircle(pink, radius = size.width * 0.55f, center = pinkCenter)
                        drawCircle(blue, radius = size.width * 0.5f, center = blueCenter)
                    }
                },
        ) {
            val maxW = maxWidth
            LockScreenClock(Modifier.align(Alignment.BottomCenter))
            StatusBar(Modifier.align(Alignment.TopCenter))
            Box(
                Modifier
                    .fillMaxWidth()
                    .offset(x = settings.xOffsetDp.dp * 0.5f, y = settings.topOffsetDp.dp - IslandOuterPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                IslandView(
                    activity = activity,
                    settings = settings,
                    expanded = expanded,
                    onExpandedChange = onExpandedChange,
                    maxWidth = maxW - 16.dp,
                )
            }
        }
    }
}

@Composable
private fun rememberClock(pattern: String): String {
    var text by remember { mutableStateOf(format(pattern)) }
    LaunchedEffect(pattern) {
        while (true) {
            delay(20_000)
            text = format(pattern)
        }
    }
    return text
}

private fun format(pattern: String): String = SimpleDateFormat(pattern, Locale.getDefault()).format(Date())

@Composable
private fun StatusBar(modifier: Modifier) {
    val time = rememberClock("h:mm")
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 22.dp, top = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(time, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Canvas(Modifier.size(width = 54.dp, height = 12.dp)) {
            val white = Color.White
            // Signal bars.
            val bw = 2.6.dp.toPx()
            for (i in 0..3) {
                val h = size.height * (0.4f + i * 0.2f)
                drawRoundRect(white, Offset(i * (bw + 1.4.dp.toPx()), size.height - h), Size(bw, h), CornerRadius(1f, 1f))
            }
            // Wi-Fi arcs.
            val cx = 22.dp.toPx()
            for (i in 0..2) {
                val r = (3 + i * 3).dp.toPx()
                drawArc(
                    white, 225f, 90f, false,
                    topLeft = Offset(cx - r, size.height - r), size = Size(r * 2, r * 2),
                    style = Stroke(1.6.dp.toPx()),
                )
            }
            // Battery.
            val bx = 32.dp.toPx()
            val bwid = 19.dp.toPx()
            drawRoundRect(white.copy(alpha = 0.5f), Offset(bx, 1f), Size(bwid, size.height - 2f), CornerRadius(3.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawRoundRect(white, Offset(bx + 2f, 3f), Size((bwid - 4f) * 0.8f, size.height - 6f), CornerRadius(2.dp.toPx()))
            drawRoundRect(white.copy(alpha = 0.5f), Offset(bx + bwid + 1f, size.height * 0.35f), Size(2f, size.height * 0.3f), CornerRadius(1f))
        }
    }
}

@Composable
private fun LockScreenClock(modifier: Modifier) {
    val date = rememberClock("EEEE, d MMMM")
    val time = rememberClock("h:mm")
    Column(modifier.padding(bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(date, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(time, color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
    }
}

/** Plays the scripted [Demo] sequence: the island's own springs make every transition smooth. */
@Composable
fun rememberShowcase(): Pair<IslandActivity, Boolean> {
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Demo.stepMs(index))
            index = (index + 1) % Demo.stepCount
        }
    }
    return Demo.step(index)
}

@Composable
fun PreviewChip(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.width(0.dp))
        Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
