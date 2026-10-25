package com.twinspace.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.twinspace.app.ui.theme.Brand

enum class DialogTone { PRIMARY, DESTRUCTIVE, WARNING }

/**
 * The app's single dialog style: floating card, tinted icon medallion, centred title and
 * full-width actions. Content slots below the message hold fields, checkboxes or lists.
 */
@Composable
fun ProDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector? = null,
    tone: DialogTone = DialogTone.PRIMARY,
    confirmEnabled: Boolean = true,
    dismissText: String? = "Cancel",
    header: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val accent = when (tone) {
        DialogTone.PRIMARY -> colors.primary
        DialogTone.DESTRUCTIVE -> colors.error
        DialogTone.WARNING -> com.twinspace.app.ui.theme.LocalExtraColors.current.warning
    }
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AnimatedVisibility(
            visibleState = visible,
            enter = fadeIn(tween(160)) + scaleIn(spring(dampingRatio = 0.8f, stiffness = 500f), initialScale = 0.9f),
        ) {
            Surface(
                modifier = modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .imePadding(),
                shape = RoundedCornerShape(28.dp),
                color = colors.surfaceContainerHigh,
                border = BorderStroke(1.dp, colors.outlineVariant),
                shadowElevation = 24.dp,
            ) {
                Column(
                    Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when {
                        header != null -> header()
                        icon != null -> Box(
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (tone == DialogTone.PRIMARY) Brand.gradient
                                        else Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.8f))),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    if (header != null || icon != null) Spacer(Modifier.height(18.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    if (message != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (content != null) {
                        Spacer(Modifier.height(18.dp))
                        Column(Modifier.fillMaxWidth(), content = content)
                    }
                    Spacer(Modifier.height(24.dp))
                    DialogButton(confirmText, tone, confirmEnabled, onConfirm)
                    if (dismissText != null) {
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(dismissText, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogButton(text: String, tone: DialogTone, enabled: Boolean, onClick: () -> Unit) {
    if (tone == DialogTone.PRIMARY) {
        GradientButton(text, onClick, Modifier.fillMaxWidth().height(52.dp), enabled = enabled)
        return
    }
    val colors = MaterialTheme.colorScheme
    val bg = when (tone) {
        DialogTone.DESTRUCTIVE -> colors.error
        else -> com.twinspace.app.ui.theme.LocalExtraColors.current.warning
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) bg else colors.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 16.sp,
            color = if (enabled) Color.White else colors.onSurfaceVariant,
        )
    }
}

/** A checkable line used inside dialogs ("I understand…"). */
@Composable
fun DialogCheck(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceContainerHighest)
            .clickable { onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Checkbox(checked = checked, onCheckedChange = onChange)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
    }
}

/** Bullet line with a small check icon, for explanatory dialogs. */
@Composable
fun DialogPoint(text: String) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Rounded.CheckCircle,
            null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 1.dp).size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Rounded, elevated snackbar that matches the dialogs. */
@Composable
fun ProSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data -> ProSnackbar(data) }
}

@Composable
private fun ProSnackbar(data: SnackbarData) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .widthIn(max = 560.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = colors.inverseSurface,
        shadowElevation = 12.dp,
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Info, null, tint = colors.inversePrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                data.visuals.message,
                modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inverseOnSurface,
            )
            val action = data.visuals.actionLabel
            if (action != null) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { data.performAction() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(action, style = MaterialTheme.typography.labelLarge, color = colors.inversePrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Modal progress card shown while a request runs inside the space. */
@Composable
fun BusyCard(text: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceContainerHigh,
        border = BorderStroke(1.dp, colors.outlineVariant),
        shadowElevation = 16.dp,
        modifier = Modifier.padding(horizontal = 40.dp).widthIn(max = 360.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text(text, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Spacer(Modifier.height(6.dp))
            Text("This only takes a moment", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = colors.primary,
                trackColor = colors.primary.copy(alpha = 0.15f),
            )
        }
    }
}
