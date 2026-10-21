package com.netscope.wifianalyzer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.netscope.wifianalyzer.ui.theme.AppColors
import com.netscope.wifianalyzer.ui.theme.Palette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

// ---------------------------------------------------------------- Snackbars

enum class SnackType { Success, Error, Info, Premium }

private class AppSnackVisuals(
    override val message: String,
    val type: SnackType,
    override val actionLabel: String?,
    override val duration: SnackbarDuration,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = true
}

class AppSnackbarController(private val scope: CoroutineScope) {
    val hostState = SnackbarHostState()

    fun show(
        message: String,
        type: SnackType = SnackType.Info,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
    ) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val result = hostState.showSnackbar(
                AppSnackVisuals(
                    message = message,
                    type = type,
                    actionLabel = actionLabel,
                    duration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
                ),
            )
            if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }
}

val LocalSnackbar = staticCompositionLocalOf<AppSnackbarController> { error("No snackbar controller") }

@Composable
fun AppSnackbarHost(controller: AppSnackbarController, modifier: Modifier = Modifier) {
    SnackbarHost(controller.hostState, modifier) { data -> AppSnackbar(data) }
}

@Composable
private fun AppSnackbar(data: SnackbarData) {
    val visuals = data.visuals as? AppSnackVisuals
    val type = visuals?.type ?: SnackType.Info
    val (icon, accent) = when (type) {
        SnackType.Success -> Icons.Rounded.CheckCircle to Palette.Mint
        SnackType.Error -> Icons.Rounded.ErrorOutline to Palette.Rose
        SnackType.Info -> Icons.Rounded.Info to Palette.Cyan
        SnackType.Premium -> AppIcons.Crown to Palette.Gold
    }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(data) { appear.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 400f)) }
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .graphicsLayer {
                translationY = (1f - appear.value) * 60f
                alpha = appear.value.coerceIn(0f, 1f)
            }
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(18.dp), ambientColor = accent, spotColor = accent)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF141C36))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(
            data.visuals.message,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        data.visuals.actionLabel?.let { label ->
            Text(
                label.uppercase(),
                color = accent,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { data.performAction() }.padding(10.dp),
            )
        }
        Icon(
            Icons.Rounded.Close,
            "Dismiss",
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.clip(CircleShape).clickable { data.dismiss() }.padding(8.dp).size(16.dp),
        )
    }
}

// ---------------------------------------------------------------- Dialogs

@Composable
fun AppDialog(
    onDismiss: () -> Unit,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconBrush: Brush? = null,
    title: String,
    message: String? = null,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissText: String? = null,
    onDismissClick: (() -> Unit)? = null,
    dismissible: Boolean = true,
    confirmBrush: Brush? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = dismissible, dismissOnClickOutside = dismissible),
    ) {
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        val scale by animateFloatAsState(if (shown) 1f else 0.85f, spring(dampingRatio = 0.6f, stiffness = 380f), label = "dlgScale")
        val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(180), label = "dlgAlpha")
        Column(
            Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, AppColors.ext.cardBorder, RoundedCornerShape(28.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (icon != null) {
                Box(
                    Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .then(if (iconBrush != null) Modifier.background(iconBrush) else Modifier.background(iconTint.copy(alpha = 0.15f))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = if (iconBrush != null) Color.White else iconTint, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.height(16.dp))
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            if (message != null) {
                Spacer(Modifier.height(8.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.ext.muted, textAlign = TextAlign.Center)
            }
            if (content != null) {
                Spacer(Modifier.height(16.dp))
                content()
            }
            if (confirmText != null && onConfirm != null) {
                Spacer(Modifier.height(22.dp))
                GradientButton(
                    confirmText,
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    brush = confirmBrush ?: AppColors.ext.primaryGradient,
                    contentColor = if (confirmBrush != null) Color.White else Color(0xFF060A17),
                )
            }
            if (dismissText != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    dismissText,
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.ext.muted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { (onDismissClick ?: onDismiss)() }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Confetti

private data class Particle(val x: Float, val delay: Float, val speed: Float, val drift: Float, val size: Float, val color: Color, val spin: Float)

@Composable
fun ConfettiBurst(modifier: Modifier = Modifier, play: Boolean = true) {
    val colors = listOf(Palette.Cyan, Palette.Lilac, Palette.Gold, Palette.Pink, Palette.Mint, Palette.Sky)
    val particles = remember {
        List(90) {
            Particle(
                x = Random.nextFloat(),
                delay = Random.nextFloat() * 0.35f,
                speed = 0.6f + Random.nextFloat() * 0.7f,
                drift = (Random.nextFloat() - 0.5f) * 0.3f,
                size = 6f + Random.nextFloat() * 8f,
                color = colors.random(),
                spin = Random.nextFloat() * 720f,
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(play) {
        if (play) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(2600, easing = LinearEasing))
        }
    }
    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        if (t <= 0f || t >= 1f) return@Canvas
        particles.forEach { p ->
            val local = ((t - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val y = -40f + local * p.speed * size.height * 1.2f
            val x = (p.x + p.drift * local) * size.width
            val a = (1f - local).coerceIn(0f, 1f)
            rotate(p.spin * local, Offset(x, y)) {
                drawRect(p.color.copy(alpha = a), Offset(x - p.size / 2, y - p.size / 4), Size(p.size, p.size / 2))
            }
        }
    }
}

/** A short pop-in scale used when an element first appears. */
@Composable
fun rememberPopIn(delayMs: Int = 0): Float {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        a.animateTo(1f, tween(450, delayMillis = delayMs, easing = FastOutSlowInEasing))
    }
    return a.value
}

@Composable
fun DialogChoiceRow(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { content() }
}

@Composable
fun Scrim(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
}
