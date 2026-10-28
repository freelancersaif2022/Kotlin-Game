package com.inkly.notes.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Bottom sheet popup: fading scrim, springy slide-up from the bottom edge, drag handle with
 * swipe-down-to-dismiss and a real exit animation (the window stays until the sheet has left).
 */
@Composable
fun AnimatedDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = remember { MutableTransitionState(false) }
    LaunchedEffect(visible) { state.targetState = visible }
    if (!state.currentState && !state.targetState && state.isIdle) return

    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()
        val drag = remember { Animatable(0f) }
        LaunchedEffect(visible) { if (visible) drag.snapTo(0f) }

        AnimatedVisibility(
            visibleState = state,
            enter = fadeIn(tween(240)),
            exit = fadeOut(tween(220)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF070914).copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { if (dismissible) onDismiss() },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Surface(
                    modifier = Modifier
                        .animateEnterExit(
                            enter = slideInVertically(spring(dampingRatio = 0.86f, stiffness = 420f)) { it },
                            exit = slideOutVertically(tween(240)) { it },
                        )
                        .offset { IntOffset(0, drag.value.roundToInt()) }
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .draggable(
                            orientation = Orientation.Vertical,
                            enabled = dismissible,
                            state = rememberDraggableState { delta ->
                                scope.launch { drag.snapTo((drag.value + delta).coerceAtLeast(0f)) }
                            },
                            onDragStopped = { velocity ->
                                val threshold = with(density) { 110.dp.toPx() }
                                if (drag.value > threshold || velocity > 1800f) onDismiss()
                                else drag.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 500f))
                            },
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {},
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shadowElevation = 24.dp,
                ) {
                    Column(
                        Modifier
                            .navigationBarsPadding()
                            .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
                    ) {
                        Box(
                            Modifier
                                .padding(top = 12.dp, bottom = 18.dp)
                                .align(Alignment.CenterHorizontally)
                                .size(width = 40.dp, height = 5.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (dismissible) 0.28f else 0f)
                                )
                        )
                        content()
                    }
                }
            }
        }
    }
}

/** Icon in a soft halo; [wiggle] gives it a short attention shake when the sheet opens. */
@Composable
fun DialogIcon(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, wiggle: Boolean = false) {
    val pop = remember { Animatable(0.6f) }
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 380f)) }
        if (wiggle) {
            rotation.animateTo(
                0f,
                keyframes {
                    durationMillis = 620
                    -14f at 120
                    11f at 240
                    -7f at 360
                    4f at 480
                },
            )
        }
    }
    Box(
        modifier
            .size(76.dp)
            .scale(pop.value)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon, null,
                tint = tint,
                modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = rotation.value },
            )
        }
    }
}

@Composable
fun ConfirmDialog(
    visible: Boolean,
    icon: ImageVector,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Cancel",
    destructive: Boolean = false,
    confirmIcon: ImageVector? = null,
    preview: (@Composable () -> Unit)? = null,
) {
    val accent = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    AnimatedDialog(visible = visible, onDismiss = onDismiss) {
        DialogIcon(icon, accent, Modifier.align(Alignment.CenterHorizontally), wiggle = destructive)
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        )
        if (preview != null) {
            Spacer(Modifier.height(18.dp))
            preview()
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Text(dismissText, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            }
            val interaction = remember { MutableInteractionSource() }
            Button(
                onClick = onConfirm,
                interactionSource = interaction,
                modifier = Modifier.weight(1f).height(54.dp).pressScale(interaction),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = if (destructive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (confirmIcon != null) {
                    Icon(confirmIcon, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(confirmText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Mini card showing which note is about to be deleted. */
@Composable
fun NotePreviewCard(title: String, body: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = tint,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .padding(top = 2.dp)
                    .size(width = 4.dp, height = 36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    title.ifBlank { "Untitled note" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (body.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteNoteDialog(
    visible: Boolean,
    title: String,
    body: String,
    tint: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(
        visible = visible,
        icon = Icons.Rounded.DeleteOutline,
        title = "Delete this note?",
        message = "It will be permanently removed from this device. This can't be undone.",
        confirmText = "Delete",
        confirmIcon = Icons.Rounded.DeleteOutline,
        destructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        preview = { NotePreviewCard(title, body, tint) },
    )
}

/** Non-dismissible progress sheet with a breathing icon. */
@Composable
fun ProgressDialog(visible: Boolean, icon: ImageVector, title: String, message: String) {
    AnimatedDialog(visible = visible, onDismiss = {}, dismissible = false) {
        val pulse = rememberInfiniteTransition(label = "pulse")
        val s by pulse.animateFloat(
            0.92f, 1.06f,
            infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "s",
        )
        val lift by pulse.animateFloat(
            0f, -6f,
            infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "lift",
        )
        Box(Modifier.align(Alignment.CenterHorizontally).size(84.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(84.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            )
            Icon(
                icon, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp).scale(s).graphicsLayer { translationY = lift * density },
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )
    }
}

@Composable
fun SheetAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp)) }
        Text(label, style = MaterialTheme.typography.titleSmall, color = if (tint == MaterialTheme.colorScheme.error) tint else MaterialTheme.colorScheme.onSurface)
    }
}
