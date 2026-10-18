package com.smartisland.app.island.ui

import android.app.PendingIntent
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smartisland.app.data.IslandSettings
import com.smartisland.app.island.IslandActivity
import com.smartisland.app.island.MediaCommands
import kotlinx.coroutines.flow.first

/** Callbacks the island needs from its host. The in-app previews use [None]. */
interface IslandActions {
    val media: MediaCommands? get() = null
    fun send(intent: PendingIntent?) {}
    fun dismissTransient() {}
    fun openApp() {}

    object None : IslandActions
}

enum class IslandMode { IDLE, COMPACT, EXPANDED }

/** Smallest gap between the screen top and an active (non-idle) island. */
val MinActiveTop = 8.dp

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
    onFootprint: ((Rect) -> Unit)? = null,
) {
    val mode = modeFor(activity, expanded)
    val baseW = settings.widthDp.dp
    val baseH = settings.heightDp.dp

    val targetW = when (mode) {
        IslandMode.IDLE -> baseW
        IslandMode.COMPACT -> baseW + compactExtra(activity)
        IslandMode.EXPANDED -> settings.expandedWidthDp.dp
    }.coerceAtMost(maxWidth)
    val targetH = when (mode) {
        IslandMode.IDLE, IslandMode.COMPACT -> baseH
        IslandMode.EXPANDED -> expandedHeight(activity).coerceAtLeast(baseH)
    }

    val springW = spring<Dp>(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow)
    val springH = spring<Dp>(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow)
    // Animated values are read only in the layout and draw phases, so a spring frame never recomposes.
    val widthState = animateDpAsState(targetW, springW, label = "islandW")
    val heightState = animateDpAsState(targetH, springH, label = "islandH")
    // Live activities never touch the screen edge: when the idle pill sits at the very top (e.g. fitted
    // to the camera), the active island drops down just enough to keep its rounded top visible.
    val targetLift = if (mode == IslandMode.IDLE) 0.dp else (MinActiveTop - settings.topOffsetDp.dp).coerceAtLeast(0.dp)
    val liftState = animateDpAsState(targetLift, springH, label = "islandLift")

    val compactRadius = baseH / 2
    val expandedRadius = (targetH / 2).coerceAtMost(40.dp)
    val targetRadius = (if (mode == IslandMode.EXPANDED) expandedRadius else compactRadius) *
        (0.35f + 0.65f * settings.roundness.coerceIn(0f, 1f))
    val radiusState = animateDpAsState(targetRadius, springH, label = "islandR")

    var pressed by remember { mutableStateOf(false) }
    val pressState = animateFloatAsState(if (pressed) 0.96f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "press")

    val haptics = LocalHapticFeedback.current
    val currentActivity by rememberUpdatedState(activity)
    val currentMode by rememberUpdatedState(mode)
    val currentExpanded by rememberUpdatedState(expanded)
    val useHaptics by rememberUpdatedState(settings.haptics)
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val currentActions by rememberUpdatedState(actions)

    val accent by animateColorAsState(Color(settings.accentColor), tween(300), label = "accent")
    val glowOn = glowAllowed && settings.glow
    val glowState = animateFloatAsState(if (glowOn) 1f else 0f, tween(300), label = "glow")

    // The footprint (and so the overlay window) jumps to the biggest size of a transition up front and
    // shrinks once the spring settles: two window relayouts per change instead of one per frame.
    var holdW by remember { mutableStateOf(targetW) }
    var holdH by remember { mutableStateOf(targetH) }
    val footW = maxOf(targetW, holdW)
    val footH = maxOf(targetH + targetLift, holdH)
    SideEffect {
        if (holdW != footW) holdW = footW
        if (holdH != footH) holdH = footH
    }
    LaunchedEffect(targetW, targetH, targetLift) {
        snapshotFlow { widthState.value == targetW && heightState.value == targetH && liftState.value == targetLift }.first { it }
        holdW = targetW
        holdH = targetH + targetLift
    }

    Box(
        modifier
            .padding(IslandOuterPadding)
            .then(if (onFootprint != null) Modifier.onGloballyPositioned { onFootprint(it.boundsInWindow()) } else Modifier)
            .size(footW, footH),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .layout { measurable, _ ->
                    val w = widthState.value.roundToPx()
                    val h = heightState.value.roundToPx()
                    val lift = liftState.value.roundToPx()
                    val placeable = measurable.measure(Constraints.fixed(w, h))
                    layout(w, h + lift) { placeable.place(0, lift) }
                }
                .drawBehind {
                    val glow = glowState.value
                    if (glow <= 0f) return@drawBehind
                    // A soft halo built from fading rings: identical on every device and API level,
                    // unlike colored elevation shadows.
                    val r = radiusState.value.toPx()
                    val step = 1.8.dp.toPx()
                    for (i in 1..5) {
                        val grow = step * i
                        drawRoundRect(
                            color = accent.copy(alpha = glow * (0.34f / i)),
                            topLeft = Offset(-grow, -grow),
                            size = Size(size.width + grow * 2, size.height + grow * 2),
                            cornerRadius = CornerRadius(r + grow, r + grow),
                            style = Stroke(width = step * 1.4f),
                        )
                    }
                }
                .graphicsLayer {
                    scaleX = pressState.value
                    scaleY = pressState.value
                    shape = RoundedCornerShape(radiusState.value)
                    clip = true
                }
                .drawWithContent {
                    drawContent()
                    val glow = glowState.value
                    if (glow > 0f) {
                        val stroke = 1.2.dp.toPx()
                        val r = radiusState.value.toPx()
                        drawRoundRect(
                            color = accent.copy(alpha = 0.9f * glow),
                            topLeft = Offset(stroke / 2, stroke / 2),
                            size = Size(size.width - stroke, size.height - stroke),
                            cornerRadius = CornerRadius(r, r),
                            style = Stroke(stroke),
                        )
                    }
                }
                .background(Color.Black)
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
                    (fadeIn(tween(220, delayMillis = 70)) + scaleIn(tween(300, delayMillis = 30), initialScale = 0.88f))
                        .togetherWith(fadeOut(tween(80)))
                        .using(SizeTransform(clip = false) { _, _ -> snap() })
                },
                label = "islandContent",
            ) { frame ->
                IslandContent(frame.activity, frame.mode, baseH, actions, accent)
            }
        }
    }
}

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
