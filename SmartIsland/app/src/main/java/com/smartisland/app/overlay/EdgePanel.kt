package com.smartisland.app.overlay

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

enum class EdgeAction { VOLUME_UP, VOLUME_DOWN, LOCK, POWER, SCREENSHOT }

/**
 * Floating side handle that opens a compact power + volume panel: a software replacement for broken
 * or worn hardware keys.
 */
class EdgePanelOverlay(
    private val context: Context,
    private val volumePercent: () -> Int,
    private val onMoved: (offsetDp: Float) -> Unit,
    private val onAction: (EdgeAction) -> Unit,
) {
    private val open = mutableStateOf(false)

    /** Dark handle while our own (light themed) app is in front; light handle over everything else. */
    val darkHandle = mutableStateOf(false)
    private var window: OverlayWindow? = null
    private var right = true
    private val density = context.resources.displayMetrics.density
    private val slop = ViewConfiguration.get(context).scaledTouchSlop

    fun show(onRight: Boolean, offsetDp: Float) {
        if (window == null || onRight != right) {
            hide()
            right = onRight
            val gravity = (if (onRight) Gravity.END else Gravity.START) or Gravity.CENTER_VERTICAL
            window = OverlayWindow(context, gravity, onOutsideTouch = { open.value = false }, touchInterceptor = ::dragHandle) {
                EdgePanel(onRight, open, darkHandle.value, volumePercent) { action ->
                    if (action == EdgeAction.SCREENSHOT) open.value = false
                    onAction(action)
                }
            }.also { it.show() }
        }
        if (!dragging) window?.move(0, clampY((offsetDp * density).roundToInt()))
    }

    // Dragging the closed handle up or down moves it along the edge -------------------------------

    private var downX = 0f
    private var downY = 0f
    private var startY = 0
    private var dragging = false

    private fun dragHandle(ev: MotionEvent): Boolean {
        val w = window ?: return false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
                startY = w.y
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = ev.rawY - downY
                if (!dragging && !open.value && abs(dy) > slop && abs(dy) > abs(ev.rawX - downX)) dragging = true
                if (dragging) w.move(0, clampY(startY + dy.roundToInt()))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (dragging) {
                dragging = false
                onMoved(w.y / density)
                return true
            }
        }
        return dragging
    }

    /** Keeps the handle, and the panel it opens around itself, fully on screen. */
    private fun clampY(y: Int): Int {
        val limit = (context.resources.displayMetrics.heightPixels / 2 - (190 * density).roundToInt()).coerceAtLeast(0)
        return y.coerceIn(-limit, limit)
    }

    fun hide() {
        window?.hide()
        window = null
        open.value = false
    }

    fun setVisible(visible: Boolean) = window?.setVisible(visible)
}

@Composable
private fun EdgePanel(
    onRight: Boolean,
    open: MutableState<Boolean>,
    darkHandle: Boolean,
    volumePercent: () -> Int,
    onAction: (EdgeAction) -> Unit,
) {
    var lastTouch by remember { mutableLongStateOf(0L) }
    var percent by remember { mutableIntStateOf(volumePercent()) }
    LaunchedEffect(open.value, lastTouch) {
        if (open.value) {
            percent = volumePercent()
            delay(4500)
            open.value = false
        }
    }
    val drag = rememberDraggableState { delta ->
        if ((onRight && delta < -4f) || (!onRight && delta > 4f)) open.value = true
    }

    AnimatedContent(
        targetState = open.value,
        transitionSpec = {
            // The window resizes once (snap); the panel itself scales out of the screen edge.
            val origin = TransformOrigin(if (onRight) 1f else 0f, 0.5f)
            (fadeIn(tween(140)) + scaleIn(spring(dampingRatio = 0.78f, stiffness = 520f), initialScale = 0.82f, transformOrigin = origin))
                .togetherWith(fadeOut(tween(90)) + scaleOut(tween(110), targetScale = 0.9f, transformOrigin = origin))
                .using(SizeTransform(clip = false) { _, _ -> snap() })
        },
        label = "edge",
    ) { isOpen ->
        if (!isOpen) {
            Box(
                Modifier
                    .width(22.dp)
                    .height(110.dp)
                    .draggable(drag, Orientation.Horizontal)
                    .clickable { open.value = true },
                contentAlignment = if (onRight) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .width(5.dp)
                        .height(78.dp)
                        .clip(CircleShape)
                        .background(if (darkHandle) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.75f))
                        .border(0.5.dp, if (darkHandle) Color.Transparent else Color.Black.copy(alpha = 0.18f), CircleShape),
                )
            }
        } else {
            Column(
                Modifier
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xE6101016))
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                fun act(a: EdgeAction) {
                    lastTouch = System.nanoTime()
                    onAction(a)
                    if (a == EdgeAction.VOLUME_UP || a == EdgeAction.VOLUME_DOWN) percent = volumePercent()
                }
                PanelButton(Icons.Rounded.Add, Color(0xFF2C2C35)) { act(EdgeAction.VOLUME_UP) }
                Text("$percent%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                PanelButton(Icons.Rounded.Remove, Color(0xFF2C2C35)) { act(EdgeAction.VOLUME_DOWN) }
                Box(
                    Modifier
                        .width(28.dp)
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.12f)),
                )
                PanelButton(Icons.Rounded.Lock, Color(0xFF7C5CFF)) { act(EdgeAction.LOCK) }
                PanelButton(Icons.Rounded.PowerSettingsNew, Color(0xFFFF453A)) { act(EdgeAction.POWER) }
                PanelButton(Icons.Rounded.CropFree, Color(0xFF0A84FF)) { act(EdgeAction.SCREENSHOT) }
            }
        }
    }
}

@Composable
private fun PanelButton(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}
