package com.smartisland.app.island.ui

import android.app.PendingIntent
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smartisland.app.data.IslandSettings
import com.smartisland.app.island.IslandActivity
import com.smartisland.app.island.MediaCommands
import kotlin.math.abs

/** Callbacks the island needs from its host. The in-app previews use [None]. */
interface IslandActions {
    val media: MediaCommands? get() = null
    fun send(intent: PendingIntent?) {}
    fun dismissTransient() {}
    fun openApp() {}

    object None : IslandActions
}

enum class IslandMode { IDLE, COMPACT, EXPANDED }

/** Padding around the pill so the glow/shadow is not clipped by the overlay window. */
val IslandOuterPadding = 10.dp

private data class Frame(val activity: IslandActivity, val mode: IslandMode)

/**
 * The island itself: a black capsule that springs between idle, compact and expanded shapes and
 * cross-fades its content, matching the feel of the iOS Dynamic Island.
 */
@Composable
fun IslandView(
    activity: IslandActivity,
    settings: IslandSettings,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    actions: IslandActions = IslandActions.None,
    maxWidth: Dp = 440.dp,
    glowAllowed: Boolean = true,
) {
    val mode = modeFor(activity, expanded)
    val baseH = settings.heightDp.dp
    // At its narrowest the idle pill becomes a circle.
    val baseW = settings.widthDp.dp.coerceAtLeast(baseH)

    val targetW = when (mode) {
        IslandMode.IDLE -> baseW
        // Activities still need room for their content when the idle pill is tiny.
        IslandMode.COMPACT -> baseW.coerceAtLeast(MIN_COMPACT_BASE) + compactExtra(activity)
        IslandMode.EXPANDED -> settings.expandedWidthDp.dp
    }.coerceAtMost(maxWidth)
    val targetH = when (mode) {
        IslandMode.IDLE, IslandMode.COMPACT -> baseH
        IslandMode.EXPANDED -> expandedHeight(activity).coerceAtLeast(baseH)
    }

    val springW = spring<Dp>(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow)
    val springH = spring<Dp>(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow)
    val width by animateDpAsState(targetW, springW, label = "islandW")
    val height by animateDpAsState(targetH, springH, label = "islandH")

    val compactRadius = baseH / 2
    val expandedRadius = (targetH / 2).coerceAtMost(40.dp)
    val targetRadius = (if (mode == IslandMode.EXPANDED) expandedRadius else compactRadius) *
        (0.35f + 0.65f * settings.roundness.coerceIn(0f, 1f))
    val radius by animateDpAsState(targetRadius, springH, label = "islandR")

    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "press")

    val haptics = LocalHapticFeedback.current
    val currentActivity by rememberUpdatedState(activity)
    val currentMode by rememberUpdatedState(mode)
    val currentExpanded by rememberUpdatedState(expanded)
    val useHaptics by rememberUpdatedState(settings.haptics)
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val currentActions by rememberUpdatedState(actions)

    val accent by animateColorAsState(Color(settings.accentColor), tween(300), label = "accent")
    // The outline replaces the glow; the settings screen only lets one of them be on.
    val outlineMode = settings.outline
    val glowOn = glowAllowed && settings.glow && outlineMode == IslandSettings.OUTLINE_OFF
    val glowAlpha by animateFloatAsState(if (glowOn) 1f else 0f, tween(300), label = "glow")
    val shape = RoundedCornerShape(radius)

    // "Hide island": the idle pill shrinks away and comes back for every live activity.
    val hiddenIdle = settings.hideWhenIdle && mode == IslandMode.IDLE
    val appear by animateFloatAsState(
        if (hiddenIdle) 0f else 1f,
        if (hiddenIdle) tween(220) else spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "appear",
    )

    // Runs for as long as the island is visible, so the colors never stop. Only the draw phase reads it.
    val outlinePhase = if (outlineMode != IslandSettings.OUTLINE_OFF && (!hiddenIdle || appear > 0f)) {
        rememberInfiniteTransition(label = "outline").animateFloat(
            0f, 360f, infiniteRepeatable(tween(OUTLINE_CYCLE_MS, easing = LinearEasing)), label = "outlinePhase",
        )
    } else {
        null
    }
    val outlineColor = Color(settings.outlineColor)

    Box(modifier.padding(IslandOuterPadding), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier
                .size(width, height)
                .graphicsLayer {
                    val a = appear.coerceIn(0f, 1f)
                    val scale = pressScale * (0.4f + 0.6f * appear)
                    scaleX = scale
                    scaleY = scale
                    alpha = a
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
                .drawBehind {
                    if (glowAlpha <= 0f) return@drawBehind
                    // A soft halo built from fading rings: identical on every device and API level,
                    // unlike colored elevation shadows.
                    val r = radius.toPx()
                    val step = 1.8.dp.toPx()
                    for (i in 1..5) {
                        val grow = step * i
                        drawRoundRect(
                            color = accent.copy(alpha = glowAlpha * (0.34f / i)),
                            topLeft = Offset(-grow, -grow),
                            size = Size(size.width + grow * 2, size.height + grow * 2),
                            cornerRadius = CornerRadius(r + grow, r + grow),
                            style = Stroke(width = step * 1.4f),
                        )
                    }
                }
                .clip(shape)
                .background(Color.Black)
                .then(if (glowOn) Modifier.border(1.2.dp, accent.copy(alpha = 0.9f * glowAlpha), shape) else Modifier)
                .drawWithContent {
                    drawContent()
                    val phase = outlinePhase?.value ?: return@drawWithContent
                    val w = OutlineWidth.toPx()
                    val r = (radius.toPx() - w / 2).coerceAtLeast(0f)
                    val brush = if (outlineMode == IslandSettings.OUTLINE_RGB) {
                        Brush.sweepGradient(rainbow(phase))
                    } else {
                        Brush.sweepGradient(comet(outlineColor, phase))
                    }
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(w / 2, w / 2),
                        size = Size(size.width - w, size.height - w),
                        cornerRadius = CornerRadius(r, r),
                        style = Stroke(width = w),
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        },
                        onTap = {
                            if (useHaptics) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTap(currentActivity, currentMode, currentExpanded, currentOnExpandedChange, currentActions)
                        },
                        onLongPress = {
                            if (useHaptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPress(currentActivity, currentActions)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = Frame(activity, mode),
                contentKey = { it.activity::class to it.mode },
                transitionSpec = {
                    (fadeIn(tween(240, delayMillis = 80)) + scaleIn(tween(320, delayMillis = 40), initialScale = 0.86f))
                        .togetherWith(fadeOut(tween(90)))
                        .using(SizeTransform(clip = false))
                },
                label = "islandContent",
            ) { frame ->
                IslandContent(frame.activity, frame.mode, baseH, actions, accent)
            }
        }
    }
}

private val MIN_COMPACT_BASE = 70.dp
private val OutlineWidth = 2.2.dp
private const val OUTLINE_CYCLE_MS = 2600

/** A full hue wheel turned by [phase] degrees: rotating it is the same as shifting every hue. */
private fun rainbow(phase: Float): List<Color> = List(7) { i -> Color.hsv((i * 60f - phase).mod(360f), 0.85f, 1f) }

/** The chosen color all around, with a bright head running around the island. */
private fun comet(color: Color, phase: Float): List<Color> {
    val head = phase / 360f
    val bright = lerp(color, Color.White, 0.55f)
    val dim = color.copy(alpha = 0.6f)
    return List(COMET_STOPS + 1) { i ->
        val d = abs(i.toFloat() / COMET_STOPS - head).let { minOf(it, 1f - it) }
        val t = (1f - d / 0.22f).coerceIn(0f, 1f)
        lerp(dim, bright, t * t)
    }
}

private const val COMET_STOPS = 36

private fun modeFor(activity: IslandActivity, expanded: Boolean): IslandMode = when (activity) {
    IslandActivity.Idle -> IslandMode.IDLE
    is IslandActivity.Notice -> IslandMode.EXPANDED
    is IslandActivity.Call -> if (activity.isIncoming || expanded) IslandMode.EXPANDED else IslandMode.COMPACT
    is IslandActivity.Media, is IslandActivity.Charging -> if (expanded) IslandMode.EXPANDED else IslandMode.COMPACT
    else -> IslandMode.COMPACT
}

private fun compactExtra(activity: IslandActivity): Dp = when (activity) {
    is IslandActivity.Media -> 96.dp
    is IslandActivity.Call -> 104.dp
    is IslandActivity.Charging, is IslandActivity.LowBattery -> 132.dp
    is IslandActivity.Volume -> 150.dp
    is IslandActivity.Ringer -> 112.dp
    is IslandActivity.Info -> 120.dp
    else -> 0.dp
}

private fun expandedHeight(activity: IslandActivity): Dp = when (activity) {
    is IslandActivity.Media -> 178.dp
    is IslandActivity.Call -> 84.dp
    is IslandActivity.Notice -> 80.dp
    is IslandActivity.Charging -> 92.dp
    else -> 84.dp
}

private fun onTap(
    activity: IslandActivity,
    mode: IslandMode,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    actions: IslandActions,
) {
    when (activity) {
        IslandActivity.Idle -> Unit
        is IslandActivity.Notice -> {
            actions.send(activity.open)
            actions.dismissTransient()
        }
        is IslandActivity.Call -> if (activity.isIncoming) actions.send(activity.open) else onExpandedChange(!expanded)
        is IslandActivity.Media, is IslandActivity.Charging -> onExpandedChange(mode != IslandMode.EXPANDED)
        else -> actions.dismissTransient()
    }
}

private fun onLongPress(activity: IslandActivity, actions: IslandActions) {
    when (activity) {
        is IslandActivity.Media -> actions.media?.open()
        is IslandActivity.Call -> actions.send(activity.open)
        else -> actions.openApp()
    }
}

/** Formats milliseconds as m:ss or h:mm:ss. */
internal fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

internal fun now(): Long = SystemClock.elapsedRealtime()
