package com.voxcast.mic.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.voxcast.mic.ui.theme.VoxColors

/**
 * Animated dialog: card springs up and scales in, the icon badge pops in slightly later,
 * and it animates out before the window closes.
 */
@Composable
fun VoxDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    accent: Color = VoxColors.Cyan,
    iconBrush: Brush = Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.55f))),
    confirmText: String? = "OK",
    onConfirm: (() -> Unit)? = null,
    dismissText: String? = null,
    confirmBrush: Brush = VoxColors.PrimaryGradient,
    loading: Boolean = false,
    dismissible: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val state = remember { MutableTransitionState(false) }
    LaunchedEffect(visible) { state.targetState = visible }
    if (!state.currentState && !state.targetState) return

    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
        ),
    ) {
        AnimatedVisibility(
            visibleState = state,
            enter = fadeIn(tween(180)) + scaleIn(spring(dampingRatio = 0.68f, stiffness = 520f), initialScale = 0.82f) +
                slideInVertically(spring(dampingRatio = 0.75f, stiffness = 420f)) { it / 6 },
            exit = fadeOut(tween(160)) + scaleOut(tween(180), targetScale = 0.9f),
        ) {
            val shape = RoundedCornerShape(30.dp)
            Box(Modifier.padding(horizontal = 26.dp), contentAlignment = Alignment.Center) {
                Column(
                    modifier
                        .fillMaxWidth()
                        .shadow(40.dp, shape, ambientColor = accent, spotColor = accent)
                        .clip(shape)
                        .background(Brush.verticalGradient(listOf(Color(0xFF1F2636), Color(0xFF131824))))
                        .border(1.dp, Brush.verticalGradient(listOf(accent.copy(alpha = 0.55f), VoxColors.OutlineSoft)), shape)
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val iconState = remember { MutableTransitionState(false) }
                    LaunchedEffect(Unit) { iconState.targetState = true }
                    AnimatedVisibility(
                        visibleState = iconState,
                        enter = scaleIn(spring(dampingRatio = 0.45f, stiffness = 380f), initialScale = 0.3f) + fadeIn(),
                    ) {
                        IconBadge(icon = icon, brush = iconBrush, size = 68.dp, glow = accent, iconTint = Color.White)
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(title, style = MaterialTheme.typography.headlineSmall, color = VoxColors.Text, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = VoxColors.TextDim, textAlign = TextAlign.Center)
                    if (content != null) {
                        Spacer(Modifier.height(16.dp))
                        content()
                    }
                    Spacer(Modifier.height(22.dp))
                    if (confirmText != null) {
                        GradientButton(
                            text = confirmText,
                            onClick = { (onConfirm ?: onDismiss)() },
                            brush = confirmBrush,
                            loading = loading,
                            height = 52.dp,
                        )
                    }
                    if (dismissText != null) {
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            GhostButton(dismissText, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}
