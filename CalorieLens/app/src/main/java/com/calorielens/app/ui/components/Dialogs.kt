package com.calorielens.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.calorielens.app.ui.theme.AppTheme
import com.calorielens.app.ui.theme.Brand
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Base popup with a springy scale-in and a frosted card. */
@Composable
fun AppDialog(
    onDismiss: () -> Unit,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
            usePlatformDefaultWidth = false,
        ),
    ) {
        val scale = remember { Animatable(0.85f) }
        val alpha = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            alpha.animateTo(1f, tween(160))
        }
        LaunchedEffect(Unit) {
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
        val shape = RoundedCornerShape(28.dp)
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value }
                .clip(shape)
                .background(AppTheme.colors.cardElevated)
                .border(1.dp, AppTheme.colors.stroke, shape)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
fun DialogIcon(icon: ImageVector, color: Color) {
    Box(
        Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.06f)))),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun MessageDialog(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String? = null,
    destructive: Boolean = false,
) {
    AppDialog(onDismiss = onDismiss) {
        DialogIcon(icon, iconColor)
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        GradientButton(
            text = confirmText,
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            height = 52.dp,
            brush = if (destructive) Brush.linearGradient(listOf(Color(0xFFFF6A7F), Brand.Danger)) else Brand.primaryGradient,
            contentColor = if (destructive) Color.White else Color(0xFF04140F),
        )
        if (dismissText != null) {
            Spacer(Modifier.height(10.dp))
            OutlineButton(dismissText, onDismiss, Modifier.fillMaxWidth(), height = 50.dp)
        }
    }
}

/** Non-dismissible progress popup with an orbiting arc. */
@Composable
fun LoadingDialog(title: String, message: String, accent: Color = Brand.Mint) {
    AppDialog(onDismiss = {}, dismissible = false) {
        val transition = rememberInfiniteTransition(label = "loading")
        val angle by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "angle")
        Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(accent.copy(alpha = 0.12f), style = Stroke(6.dp.toPx()))
                rotate(angle) {
                    drawArc(
                        Brush.sweepGradient(listOf(Color.Transparent, accent)),
                        0f, 300f, false,
                        topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width - 6.dp.toPx(), size.height - 6.dp.toPx()),
                        style = Stroke(6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.muted, textAlign = TextAlign.Center)
    }
}

private data class Particle(val angle: Float, val speed: Float, val color: Color, val size: Float, val spin: Float)

/** One-shot confetti burst drawn behind its content. */
@Composable
fun ConfettiBurst(modifier: Modifier = Modifier) {
    val colors = listOf(Brand.Gold, Brand.Mint, Brand.Cyan, Brand.Protein, Brand.Fat, Brand.GoldLight)
    val particles = remember {
        List(70) {
            Particle(
                angle = Random.nextFloat() * 360f,
                speed = 0.35f + Random.nextFloat() * 0.65f,
                color = colors[it % colors.size],
                size = 4f + Random.nextFloat() * 6f,
                spin = Random.nextFloat() * 720f - 360f,
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1800, easing = LinearEasing)) }
    Canvas(modifier) {
        val t = progress.value
        val maxR = size.minDimension * 0.75f
        particles.forEach { p ->
            val rad = Math.toRadians(p.angle.toDouble())
            val dist = maxR * p.speed * (1 - (1 - t) * (1 - t))
            val gravity = size.height * 0.35f * t * t
            val pos = Offset(center.x + (cos(rad) * dist).toFloat(), center.y + (sin(rad) * dist).toFloat() + gravity)
            val alpha = (1f - t).coerceIn(0f, 1f)
            rotate(p.spin * t, pos) {
                drawRect(
                    p.color.copy(alpha = alpha),
                    topLeft = Offset(pos.x - p.size.dp.toPx() / 2, pos.y - p.size.dp.toPx() / 4),
                    size = androidx.compose.ui.geometry.Size(p.size.dp.toPx(), p.size.dp.toPx() / 2),
                )
            }
        }
    }
}

@Composable
fun DialogActionsRow(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { content() }
}

@Composable
fun BulletLine(text: String, color: Color = Brand.Mint) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
