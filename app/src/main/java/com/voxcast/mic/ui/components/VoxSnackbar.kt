package com.voxcast.mic.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voxcast.mic.ui.theme.VoxColors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class MessageType(val color: Color) {
    SUCCESS(VoxColors.Success), INFO(VoxColors.Cyan), WARNING(VoxColors.Warning), ERROR(VoxColors.Error)
}

data class VoxMessage(
    val type: MessageType,
    val title: String,
    val body: String? = null,
    val durationMs: Long = 2_800,
    val id: Long = System.nanoTime(),
)

/** App-wide queue for animated snackbars. Safe to call from any screen. */
object Messenger {
    internal val queue = MutableSharedFlow<VoxMessage>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    fun show(type: MessageType, title: String, body: String? = null) {
        queue.tryEmit(VoxMessage(type, title, body))
    }
    fun success(title: String, body: String? = null) = show(MessageType.SUCCESS, title, body)
    fun info(title: String, body: String? = null) = show(MessageType.INFO, title, body)
    fun warning(title: String, body: String? = null) = show(MessageType.WARNING, title, body)
    fun error(title: String, body: String? = null) = show(MessageType.ERROR, title, body)
}

/**
 * Floating "island" snackbar: drops in from the top with a spring, shows a countdown
 * line, and can be tapped or swiped up to dismiss. Messages are shown one at a time.
 */
@Composable
fun VoxSnackbarHost(modifier: Modifier = Modifier) {
    var current by remember { mutableStateOf<VoxMessage?>(null) }
    var visible by remember { mutableStateOf(false) }
    val progress = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var dismissSignal by remember { mutableStateOf<CompletableDeferred<Unit>?>(null) }

    LaunchedEffect(Unit) {
        Messenger.queue.collect { msg ->
            current = msg
            visible = true
            val signal = CompletableDeferred<Unit>()
            dismissSignal = signal
            progress.snapTo(1f)
            val countdown = scope.launch { progress.animateTo(0f, tween(msg.durationMs.toInt(), easing = LinearEasing)) }
            withTimeoutOrNull(msg.durationMs) { signal.await() }
            countdown.cancel()
            visible = false
            kotlinx.coroutines.delay(320)
        }
    }

    Box(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)) { -it * 2 } +
                fadeIn(tween(180)) + scaleIn(initialScale = 0.86f),
            exit = slideOutVertically(tween(260)) { -it * 2 } + fadeOut(tween(220)) + scaleOut(targetScale = 0.92f),
        ) {
            val msg = current ?: return@AnimatedVisibility
            val shape = RoundedCornerShape(22.dp)
            val icon = when (msg.type) {
                MessageType.SUCCESS -> Icons.Rounded.CheckCircle
                MessageType.INFO -> Icons.Rounded.Info
                MessageType.WARNING -> Icons.Rounded.WarningAmber
                MessageType.ERROR -> Icons.Rounded.ErrorOutline
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .shadow(24.dp, shape, ambientColor = msg.type.color, spotColor = msg.type.color)
                    .clip(shape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF1E2433), Color(0xFF151A25))))
                    .border(1.dp, Brush.horizontalGradient(listOf(msg.type.color.copy(alpha = 0.6f), VoxColors.OutlineSoft)), shape)
                    .clickable { dismissSignal?.complete(Unit) }
                    .pointerInput(msg.id) {
                        detectVerticalDragGestures { _, drag -> if (drag < -12) dismissSignal?.complete(Unit) }
                    },
            ) {
                Row(Modifier.padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        icon = icon,
                        brush = Brush.linearGradient(listOf(msg.type.color, msg.type.color.copy(alpha = 0.55f))),
                        size = 38.dp,
                        iconTint = VoxColors.Background,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(msg.title, style = MaterialTheme.typography.titleSmall, color = VoxColors.Text, fontWeight = FontWeight.Bold)
                        if (!msg.body.isNullOrBlank()) {
                            Text(msg.body, style = MaterialTheme.typography.bodySmall, color = VoxColors.TextDim)
                        }
                    }
                }
                Canvas(Modifier.fillMaxWidth().height(3.dp)) {
                    drawLine(
                        msg.type.color.copy(alpha = 0.85f),
                        Offset(0f, size.height / 2),
                        Offset(size.width * progress.value, size.height / 2),
                        strokeWidth = size.height,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}
