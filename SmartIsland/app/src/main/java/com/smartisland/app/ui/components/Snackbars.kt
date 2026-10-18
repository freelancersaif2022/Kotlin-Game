package com.smartisland.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.smartisland.app.ui.theme.Palette
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class SnackType(val color: Color) {
    SUCCESS(Palette.Success),
    ERROR(Palette.Error),
    INFO(Palette.Secondary),
    PREMIUM(Palette.Gold),
}

private class AppSnackVisuals(
    override val message: String,
    val type: SnackType,
    override val actionLabel: String?,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration =
        if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
}

/** App-wide snackbar entry point, provided through [LocalSnack]. */
@Stable
class SnackController(private val scope: CoroutineScope) {
    val host = SnackbarHostState()

    fun show(message: String, type: SnackType = SnackType.INFO, action: String? = null, onAction: (() -> Unit)? = null) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            val result = host.showSnackbar(AppSnackVisuals(message, type, action))
            if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }

    fun success(message: String) = show(message, SnackType.SUCCESS)
    fun error(message: String, action: String? = null, onAction: (() -> Unit)? = null) =
        show(message, SnackType.ERROR, action, onAction)
}

val LocalSnack = staticCompositionLocalOf<SnackController> { error("SnackController not provided") }

@Composable
fun AppSnackbarHost(controller: SnackController, modifier: Modifier = Modifier) {
    SnackbarHost(controller.host, modifier) { data ->
        key(data) { SwipeableSnack(data) }
    }
}

/** Slides up on entry; drag it down or sideways to dismiss. */
@Composable
private fun SwipeableSnack(data: SnackbarData) {
    val visuals = data.visuals as? AppSnackVisuals
    val type = visuals?.type ?: SnackType.INFO
    val icon = when (type) {
        SnackType.SUCCESS -> Icons.Rounded.CheckCircle
        SnackType.ERROR -> Icons.Rounded.ErrorOutline
        SnackType.INFO -> Icons.Rounded.Info
        SnackType.PREMIUM -> Icons.Rounded.WorkspacePremium
    }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val enterY = remember { Animatable(with(density) { 80.dp.toPx() }) }
    val dragX = remember { Animatable(0f) }
    val dragY = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enterY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 600f)) }
    val dismissPx = with(density) { 70.dp.toPx() }

    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .graphicsLayer {
                translationX = dragX.value
                translationY = enterY.value + dragY.value.coerceAtLeast(0f)
                alpha = (1f - (abs(dragX.value) + dragY.value.coerceAtLeast(0f)) / (dismissPx * 2.2f)).coerceIn(0f, 1f)
            }
            .pointerInput(data) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (abs(dragX.value) > dismissPx || dragY.value > dismissPx * 0.6f) {
                                data.dismiss()
                            } else {
                                launch { dragX.animateTo(0f, spring()) }
                                dragY.animateTo(0f, spring())
                            }
                        }
                    },
                ) { change, amount ->
                    change.consume()
                    scope.launch {
                        dragX.snapTo(dragX.value + amount.x)
                        dragY.snapTo(dragY.value + amount.y)
                    }
                }
            }
            .shadow(16.dp, shape, ambientColor = type.color.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.25f))
            .clip(shape)
            .background(Palette.Snackbar)
            .border(1.dp, type.color.copy(alpha = 0.35f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(type.color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = type.color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            data.visuals.message,
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        data.visuals.actionLabel?.let { label ->
            Spacer(Modifier.width(8.dp))
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = if (type == SnackType.PREMIUM) Palette.GoldDeep else type.color,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { data.performAction() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}
