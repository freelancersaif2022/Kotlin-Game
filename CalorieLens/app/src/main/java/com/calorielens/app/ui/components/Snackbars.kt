package com.calorielens.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.calorielens.app.ui.theme.AppTheme
import com.calorielens.app.ui.theme.Brand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class SnackType(val color: Color, val icon: ImageVector) {
    SUCCESS(Brand.Success, Icons.Rounded.CheckCircle),
    ERROR(Brand.Danger, Icons.Rounded.ErrorOutline),
    INFO(Brand.Info, Icons.Rounded.Info),
    PREMIUM(Brand.Gold, Icons.Rounded.WorkspacePremium),
}

private data class AppSnackVisuals(
    override val message: String,
    val title: String?,
    val type: SnackType,
    override val actionLabel: String?,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration =
        if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
}

@Stable
class SnackbarController(val hostState: SnackbarHostState, private val scope: CoroutineScope) {
    fun show(
        message: String,
        type: SnackType = SnackType.INFO,
        title: String? = null,
        actionLabel: String? = null,
        onDismissed: (() -> Unit)? = null,
        onAction: (() -> Unit)? = null,
    ) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val result = hostState.showSnackbar(AppSnackVisuals(message, title, type, actionLabel))
            if (result == SnackbarResult.ActionPerformed) onAction?.invoke() else onDismissed?.invoke()
        }
    }
}

val LocalSnackbar = staticCompositionLocalOf<SnackbarController> { error("SnackbarController not provided") }

@Composable
fun AppSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data -> AppSnackbar(data) }
}

@Composable
private fun AppSnackbar(data: SnackbarData) {
    val visuals = data.visuals as? AppSnackVisuals
    val type = visuals?.type ?: SnackType.INFO
    val offset = remember(data) { Animatable(1f) }
    LaunchedEffect(data) {
        offset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    val density = LocalDensity.current
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth()
            .graphicsLayer {
                translationY = offset.value * with(density) { 80.dp.toPx() }
                alpha = 1f - offset.value * 0.8f
            }
            .shadow(24.dp, shape, ambientColor = type.color.copy(alpha = 0.4f), spotColor = type.color.copy(alpha = 0.4f))
            .clip(shape)
            .background(if (AppTheme.colors.isDark) Color(0xFF1A1E26) else Color(0xFF15181E))
            .border(1.dp, type.color.copy(alpha = 0.35f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { data.dismiss() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(type.color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(type.icon, contentDescription = null, tint = type.color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            visuals?.title?.let {
                Text(it, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                data.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = if (visuals?.title != null) 0.7f else 0.92f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        data.visuals.actionLabel?.let { label ->
            Spacer(Modifier.width(8.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = type.color,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { data.performAction() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}
