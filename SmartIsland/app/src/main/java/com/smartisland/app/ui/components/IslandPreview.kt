package com.smartisland.app.ui.components

import android.media.AudioManager
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
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

    val showcase: List<Pair<IslandActivity, Boolean>> = listOf(
        IslandActivity.Idle to false,
        media() to false,
        media() to true,
        charging() to false,
        incomingCall() to true,
        notice() to true,
        volume() to false,
    )
}

/**
 * Top of a phone screen (wallpaper + status bar) with the island drawn at its real size and offset, so
 * size sliders show exactly what the user will get.
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
    val shape = RoundedCornerShape(30.dp)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(Palette.WallpaperGradient),
    ) {
        val maxW = maxWidth
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

@Composable
private fun StatusBar(modifier: Modifier) {
    var time by remember { mutableStateOf(clock()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            time = clock()
        }
    }
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(time, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("5G  ▂▄▆█  86%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun clock(): String = SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())

/** Cycles through [Demo.showcase] to show off the island when nothing real is happening. */
@Composable
fun rememberShowcase(intervalMs: Long = 2600L): Pair<IslandActivity, Boolean> {
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(intervalMs) {
        while (true) {
            delay(intervalMs)
            index = (index + 1) % Demo.showcase.size
        }
    }
    return Demo.showcase[index]
}

@Composable
fun PreviewCaption(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(14.dp))
    }
}
