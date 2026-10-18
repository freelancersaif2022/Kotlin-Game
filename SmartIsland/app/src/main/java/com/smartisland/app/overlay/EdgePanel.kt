package com.smartisland.app.overlay

import android.content.Context
import android.view.Gravity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class EdgeAction { VOLUME_UP, VOLUME_DOWN, LOCK, POWER, SCREENSHOT }

/**
 * Floating side handle that opens a compact power + volume panel: a software replacement for broken
 * or worn hardware keys.
 */
class EdgePanelOverlay(
    private val context: Context,
    private val volumePercent: () -> Int,
    private val onAction: (EdgeAction) -> Unit,
) {
    private val open = mutableStateOf(false)
    private var window: OverlayWindow? = null
    private var right = true

    fun show(onRight: Boolean) {
        if (window != null && onRight == right) return
        hide()
        right = onRight
        val gravity = (if (onRight) Gravity.END else Gravity.START) or Gravity.CENTER_VERTICAL
        window = OverlayWindow(context, gravity, onOutsideTouch = { open.value = false }) {
            EdgePanel(onRight, open, volumePercent) { action ->
                if (action == EdgeAction.SCREENSHOT) open.value = false
                onAction(action)
            }
        }.also { it.show() }
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
            (fadeIn(tween(180)) + slideInHorizontally(tween(220)) { if (onRight) it / 2 else -it / 2 })
                .togetherWith(fadeOut(tween(120)))
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
                        .background(Color.White.copy(alpha = 0.55f)),
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
