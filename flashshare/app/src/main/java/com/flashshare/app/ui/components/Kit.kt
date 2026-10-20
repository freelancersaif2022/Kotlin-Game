package com.flashshare.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flashshare.app.ui.theme.Brand
import com.flashshare.app.ui.theme.LocalExtra
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// region snackbars
enum class SnackKind { SUCCESS, INFO, ERROR, PREMIUM }

data class SnackMsg(val text: String, val kind: SnackKind, val id: Long = System.nanoTime())

/** App-wide snackbar queue – every confirmation in the app goes through here. */
object Snack {
    private val _flow = MutableSharedFlow<SnackMsg>(extraBufferCapacity = 16)
    val flow = _flow.asSharedFlow()

    fun show(text: String, kind: SnackKind = guess(text)) {
        _flow.tryEmit(SnackMsg(text, kind))
    }

    private fun guess(t: String): SnackKind {
        val l = t.lowercase()
        return when {
            "premium" in l && ("activ" in l || "welcome" in l) -> SnackKind.PREMIUM
            listOf("couldn't", "failed", "no app", "not ", "error", "wrong", "unavailable", "isn't").any { it in l } -> SnackKind.ERROR
            listOf("freed", "cleared", "deleted", "saved", "added", "active", "restored", "copied", "done").any { it in l } -> SnackKind.SUCCESS
            else -> SnackKind.INFO
        }
    }
}

@Composable
fun SnackHost(modifier: Modifier = Modifier) {
    var current by remember { mutableStateOf<SnackMsg?>(null) }
    val queue = remember { ArrayDeque<SnackMsg>() }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { Snack.flow.collect { queue.addLast(it); if (current == null) current = queue.removeFirst() } }
    LaunchedEffect(current) {
        if (current == null) return@LaunchedEffect
        visible = true
        delay(2800)
        visible = false
        delay(260)
        current = queue.removeFirstOrNull()
    }
    Box(modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible && current != null, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
            current?.let { SnackCard(it) { visible = false } }
        }
    }
}

@Composable
private fun SnackCard(m: SnackMsg, onDismiss: () -> Unit) {
    val (brush, icon) = when (m.kind) {
        SnackKind.SUCCESS -> Brand.mint to Icons.Rounded.CheckCircle
        SnackKind.ERROR -> Brush.linearGradient(listOf(Color(0xFFFF5E62), Color(0xFFFF2D55))) to Icons.Rounded.ErrorOutline
        SnackKind.PREMIUM -> Brand.premium to Icons.Rounded.WorkspacePremium
        SnackKind.INFO -> Brand.main to Icons.Rounded.Info
    }
    val dark = LocalExtra.current.isDark
    Row(
        Modifier.widthIn(max = 520.dp).fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(22.dp))
            .background(if (dark) Color(0xFF1E2638) else Color(0xFF111827))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(brush), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(m.text, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}
// endregion

// region dialogs
/** The one dialog style used everywhere: glowing gradient icon, centered text, full-width actions. */
@Composable
fun AppDialog(
    onDismiss: () -> Unit,
    icon: ImageVector,
    title: String,
    message: String? = null,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String? = "Cancel",
    iconBrush: Brush = Brand.main,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val appear = remember { Animatable(0.85f) }
        LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 500f)) }
        Column(
            Modifier.padding(24.dp).widthIn(max = 420.dp).fillMaxWidth()
                .graphicsLayer { scaleX = appear.value; scaleY = appear.value }
                .clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val brush = if (destructive) Brush.linearGradient(listOf(Color(0xFFFF6B6B), Color(0xFFFF2D55))) else iconBrush
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(84.dp).clip(CircleShape).background(brush).graphicsLayer { alpha = 0.18f })
                GlossyIcon(icon, brush, 60.dp, corner = 30.dp)
            }
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            if (message != null) {
                Spacer(Modifier.height(8.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            if (content != null) {
                Spacer(Modifier.height(16.dp))
                content()
            }
            Spacer(Modifier.height(22.dp))
            GradientButton(confirmText, onClick = onConfirm, brush = brush, enabled = confirmEnabled, modifier = Modifier.fillMaxWidth(), height = 52.dp)
            if (dismissText != null) {
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(dismissText, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}
// endregion

// region icons & badges
/** 3D-looking app-style icon: gradient squircle, glossy top highlight, soft shadow. */
@Composable
fun GlossyIcon(icon: ImageVector, brush: Brush, size: Dp, modifier: Modifier = Modifier, corner: Dp = size * 0.3f, iconScale: Float = 0.5f) {
    Box(
        modifier.size(size)
            .shadow(size * 0.18f, RoundedCornerShape(corner), ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(corner))
            .background(brush)
            .drawWithContent {
                drawContent()
                // glossy highlight on the upper half
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0f)), endY = this.size.height * 0.6f),
                    topLeft = Offset(this.size.width * 0.06f, this.size.height * 0.04f),
                    size = Size(this.size.width * 0.88f, this.size.height * 0.5f),
                    cornerRadius = CornerRadius(corner.toPx() * 0.8f),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * iconScale).graphicsLayer {
            shadowElevation = 4f
        })
    }
}

object IconBrushes {
    val violet = Brush.linearGradient(listOf(Color(0xFFA77BFF), Color(0xFF6A3DFF)))
    val cyan = Brush.linearGradient(listOf(Color(0xFF4FD8FF), Color(0xFF1E7BFF)))
    val mint = Brush.linearGradient(listOf(Color(0xFF3DEBB0), Color(0xFF00A88A)))
    val orange = Brush.linearGradient(listOf(Color(0xFFFFC84D), Color(0xFFFF7A1A)))
    val blue = Brush.linearGradient(listOf(Color(0xFF6E9BFF), Color(0xFF2F4FFF)))
    val pink = Brush.linearGradient(listOf(Color(0xFFFF8AB3), Color(0xFFFF3D6E)))
    val red = Brush.linearGradient(listOf(Color(0xFFFF8A80), Color(0xFFFF3B30)))
    val slate = Brush.linearGradient(listOf(Color(0xFF8E9BB5), Color(0xFF55617A)))
    val gold = Brand.premium
}

/** Animated gold PRO tag with a crown and a light sweep. */
@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(8.dp)).background(Brand.premium).shine(1600).padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(2.dp))
        Text("PRO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp)
    }
}

/** Small king-crown medal shown on the avatar of Premium users. */
@Composable
fun CrownBadge(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).shadow(4.dp, CircleShape).clip(CircleShape).background(Brand.premium)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.WorkspacePremium, "Premium", tint = Color.White, modifier = Modifier.size(size * 0.62f)) }
}

/** A diagonal light sweep that passes over the content every [periodMs]. */
fun Modifier.shine(periodMs: Int = 2400, alpha: Float = 0.35f): Modifier = composed {
    val t = rememberInfiniteTransition(label = "shine")
    val p by t.animateFloat(-0.6f, 1.6f, infiniteRepeatable(tween(periodMs, easing = LinearEasing)), label = "p")
    drawWithContent {
        drawContent()
        val w = size.width
        val x = p * w
        drawRect(
            Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = alpha), Color.White.copy(alpha = 0f)),
                start = Offset(x - w * 0.3f, 0f), end = Offset(x + w * 0.3f, size.height),
            )
        )
    }
}

/** Items slide + fade in one after another the first time they appear. */
fun Modifier.staggerIn(index: Int, key: Any = Unit): Modifier = composed {
    val a = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        delay((index.coerceAtMost(12) * 45).toLong())
        a.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
    }
    graphicsLayer { alpha = a.value; translationY = (1f - a.value) * 40f }
}
// endregion

// region phone mockup
/** A realistic phone: dark bezel, punch-hole camera, gradient screen. */
@Composable
fun PhoneMockup(
    modifier: Modifier = Modifier,
    width: Dp = 104.dp,
    screen: Brush = Brand.main,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val h = width * 2.05f
    Box(
        modifier.size(width, h)
            .shadow(18.dp, RoundedCornerShape(width * 0.2f), ambientColor = Color.Black.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(width * 0.2f))
            .background(Brush.linearGradient(listOf(Color(0xFF2B2F3A), Color(0xFF0D0F14))))
            .padding(width * 0.045f),
    ) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(width * 0.16f)).background(screen), contentAlignment = Alignment.Center) {
            content()
            Box(Modifier.align(Alignment.TopCenter).padding(top = width * 0.06f).size(width * 0.07f).clip(CircleShape).background(Color(0xFF0D0F14)))
            // glass reflection
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent), start = Offset.Zero, end = Offset(size.width, size.height * 0.6f)))
            }
        }
    }
}
// endregion

@Composable
fun CountUpText(target: Long, format: (Long) -> String, style: androidx.compose.ui.text.TextStyle, color: Color = Color.Unspecified, durationMs: Int = 1200) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(target) { a.snapTo(0f); a.animateTo(1f, tween(durationMs, easing = FastOutSlowInEasing)) }
    Text(format((target * a.value).toLong()), style = style, color = color)
}

@Composable
fun PopIn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(state, modifier, enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn()) { content() }
}

@Composable
fun rememberNoRipple() = remember { MutableInteractionSource() }
