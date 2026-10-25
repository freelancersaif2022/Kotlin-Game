package com.saif.imagetopdf.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saif.imagetopdf.UiState
import com.saif.imagetopdf.engine.Converter
import com.saif.imagetopdf.engine.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProcessingScreen(state: UiState.Processing) {
    val thumb = rememberImage(state.image, 1000)
    val progress by animateFloatAsState(state.progress, tween(700, easing = FastOutSlowInEasing), label = "progress")

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Palette.header)
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("Converting", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    AnimatedContent(
                        targetState = Converter.STAGES.getOrElse(state.stage) { "Finishing" },
                        transitionSpec = {
                            (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                        },
                        label = "stage",
                    ) { Text(it, color = Palette.Sky, fontSize = 14.sp) }
                }
                Text(
                    "${(progress * 100).toInt()}%",
                    color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.height(16.dp))
            GlowBar(progress)
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) { Scanner(thumb) }

        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                .shadow(10.dp, RoundedCornerShape(24.dp), spotColor = Palette.Navy.copy(alpha = 0.2f))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Converter.STAGES.forEachIndexed { i, label -> StageRow(label, done = i < state.stage, active = i == state.stage) }
        }
    }
}

@Composable
private fun GlowBar(progress: Float) {
    val loop = rememberInfiniteTransition(label = "bar")
    val shine by loop.animateFloat(-0.3f, 1.3f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "shine")
    Canvas(Modifier.fillMaxWidth().height(8.dp)) {
        val r = size.height / 2
        drawLine(Color.White.copy(alpha = 0.15f), Offset(r, r), Offset(size.width - r, r), size.height, StrokeCap.Round)
        val end = r + (size.width - 2 * r) * progress
        drawLine(
            Brush.horizontalGradient(listOf(Palette.Sky, Color.White)),
            Offset(r, r), Offset(end, r), size.height, StrokeCap.Round,
        )
        val x = size.width * shine
        if (x < end) drawLine(
            Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.9f), Color.Transparent), x - 40f, x + 40f),
            Offset((x - 40f).coerceAtLeast(r), r), Offset((x + 40f).coerceAtMost(end), r), size.height, StrokeCap.Round,
        )
    }
}

@Composable
private fun Scanner(thumb: Bitmap?) {
    val loop = rememberInfiniteTransition(label = "scan")
    val beam by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "beam")
    val aspect = thumb?.let { it.width.toFloat() / it.height } ?: 0.75f

    Box(
        Modifier
            .aspectRatio(aspect.coerceIn(0.3f, 3f), matchHeightConstraintsFirst = true)
            .shadow(20.dp, RoundedCornerShape(20.dp), spotColor = Palette.Navy.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White),
    ) {
        if (thumb != null) {
            Image(thumb.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxSize().background(Palette.Mist))
        }
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height * beam
            val trail = size.height * 0.18f
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, Palette.Royal.copy(alpha = 0.32f)), startY = y - trail, endY = y),
                topLeft = Offset(0f, (y - trail).coerceAtLeast(0f)), size = Size(size.width, minOf(trail, y)),
            )
            drawRect(Palette.Royal, topLeft = Offset(0f, y - 1.5.dp.toPx()), size = Size(size.width, 3.dp.toPx()))
            // Scanner frame corners
            val c = 26.dp.toPx()
            val m = 12.dp.toPx()
            val s = 3.dp.toPx()
            val col = Color.White
            listOf(
                Offset(m, m) to Offset(1f, 1f), Offset(size.width - m, m) to Offset(-1f, 1f),
                Offset(m, size.height - m) to Offset(1f, -1f), Offset(size.width - m, size.height - m) to Offset(-1f, -1f),
            ).forEach { (p, d) ->
                drawLine(col, p, Offset(p.x + c * d.x, p.y), s, StrokeCap.Round)
                drawLine(col, p, Offset(p.x, p.y + c * d.y), s, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun StageRow(label: String, done: Boolean, active: Boolean) {
    val color by animateColorAsState(if (done || active) Palette.Text else Palette.Muted.copy(alpha = 0.6f), label = "c")
    val check by animateFloatAsState(if (done) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), label = "chk")
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            when {
                active -> CircularProgressIndicator(Modifier.size(20.dp), color = Palette.Royal, strokeWidth = 2.5.dp)
                done -> Box(
                    Modifier.size(22.dp).graphicsLayer { scaleX = check; scaleY = check }.clip(CircleShape).background(Palette.Green),
                    contentAlignment = Alignment.Center,
                ) { Icon(AppIcons.Check, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                else -> Box(Modifier.size(10.dp).clip(CircleShape).background(Palette.Line))
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(label, color = color, fontSize = 14.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
fun rememberImage(uri: Uri, maxSide: Int): Bitmap? {
    val context = LocalContext.current
    val value by produceState<Bitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching { ImageLoader.load(context, uri, minSide = 1, maxSide = maxSide) }.getOrNull()
        }
    }
    return value
}
