package com.smartisland.app.island.ui

import android.app.PendingIntent
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smartisland.app.data.IslandSettings
import com.smartisland.app.island.IslandActivity
import com.smartisland.app.island.MediaCommands

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

    val glowColor = Color(accentFor(activity, settings))
    val shape = RoundedCornerShape(radius)

    Box(modifier.padding(IslandOuterPadding), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier
                .size(width, height)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .then(
                    if (glowAllowed && settings.glow && mode != IslandMode.IDLE) {
                        Modifier.shadow(10.dp, shape, ambientColor = glowColor, spotColor = glowColor)
                    } else {
                        Modifier
                    },
                )
                .clip(shape)
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
                    (fadeIn(tween(240, delayMillis = 80)) + scaleIn(tween(320, delayMillis = 40), initialScale = 0.86f))
                        .togetherWith(fadeOut(tween(90)))
                        .using(SizeTransform(clip = false))
                },
                label = "islandContent",
            ) { frame ->
                IslandContent(frame.activity, frame.mode, baseH, actions)
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

private fun accentFor(activity: IslandActivity, settings: IslandSettings): Int = when (activity) {
    is IslandActivity.Media -> activity.accent
    is IslandActivity.Call -> IslandColors.GREEN_ARGB
    is IslandActivity.Charging -> IslandColors.GREEN_ARGB
    is IslandActivity.LowBattery -> IslandColors.RED_ARGB
    else -> settings.accentColor
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
