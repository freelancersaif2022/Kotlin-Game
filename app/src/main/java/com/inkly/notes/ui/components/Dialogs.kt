package com.inkly.notes.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Animated dialog host: custom fading scrim + springy scale-in card, with a real exit animation
 * (the window stays alive until the card has animated out).
 */
@Composable
fun AnimatedDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    dismissible: Boolean = true,
    alignBottom: Boolean = false,
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
        AnimatedVisibility(
            visibleState = state,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(200)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { if (dismissible) onDismiss() },
                contentAlignment = if (alignBottom) Alignment.BottomCenter else Alignment.Center,
            ) {
                val cardEnter = if (alignBottom) {
                    slideInVertically(spring(dampingRatio = 0.82f, stiffness = 380f)) { it }
                } else {
                    scaleIn(spring(dampingRatio = 0.68f, stiffness = 420f), initialScale = 0.82f) + fadeIn(tween(160))
                }
                val cardExit = if (alignBottom) {
                    slideOutVertically(tween(220)) { it }
                } else {
                    scaleOut(tween(180), targetScale = 0.9f) + fadeOut(tween(160))
                }
                Surface(
                    modifier = Modifier
                        .animateEnterExit(enter = cardEnter, exit = cardExit)
                        .padding(
                            start = 20.dp, end = 20.dp,
                            bottom = if (alignBottom) 0.dp else 20.dp, top = 20.dp,
                        )
                        .widthIn(max = 440.dp)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {},
                    shape = if (alignBottom) RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    else RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    tonalElevation = 0.dp,
                    shadowElevation = 24.dp,
                ) {
                    Column(
                        Modifier.padding(
                            start = 24.dp, end = 24.dp, top = 24.dp,
                            bottom = if (alignBottom) 36.dp else 20.dp,
                        ),
                        content = content,
                    )
                }
            }
        }
    }
}

@Composable
fun DialogIcon(icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(30.dp))
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
) {
    val accent = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    AnimatedDialog(visible = visible, onDismiss = onDismiss) {
        DialogIcon(icon, accent, Modifier.align(Alignment.CenterHorizontally))
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
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = if (destructive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
            ),
        ) { Text(confirmText, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text(dismissText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Non-dismissible progress popup with a breathing icon. */
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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
        Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
