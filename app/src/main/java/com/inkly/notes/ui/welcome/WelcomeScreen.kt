package com.inkly.notes.ui.welcome

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.inkly.notes.ui.components.InklyLogo
import com.inkly.notes.ui.components.pressScale
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private data class Page(val icon: ImageVector, val accent: Color, val title: String, val body: String)

private val pages = listOf(
    Page(
        Icons.Rounded.Edit, Color(0xFF4F46E5),
        "Capture every thought",
        "Tap + and start writing on a clean lined page. Every word is saved automatically as you type.",
    ),
    Page(
        Icons.Rounded.AutoAwesome, Color(0xFFF59E0B),
        "Beautifully organised",
        "Colour-code, pin and search your notes in a calm, distraction-free space — in light or dark mode.",
    ),
    Page(
        Icons.Rounded.CloudDone, Color(0xFF0EA5E9),
        "Safe in Google Drive",
        "Connect Google Drive for automatic daily backups in the background, and restore anytime.",
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InklyLogo(32.dp)
            Spacer(Modifier.width(10.dp))
            Text("Inkly", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDone) { Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val offset = (pager.currentPage - page) + pager.currentPageOffsetFraction
            val p = pages[page]
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp)
                    .graphicsLayer {
                        val f = 1f - abs(offset).coerceIn(0f, 1f)
                        alpha = 0.25f + 0.75f * f
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Illustration(
                    p.icon, p.accent,
                    Modifier.graphicsLayer {
                        translationX = offset * size.width * 0.45f
                        val s = 1f - abs(offset).coerceIn(0f, 1f) * 0.2f
                        scaleX = s; scaleY = s
                    },
                )
                Spacer(Modifier.height(48.dp))
                Text(
                    p.title,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    p.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { i ->
                    val selected = pager.currentPage == i
                    val w by animateDpAsState(if (selected) 28.dp else 8.dp, label = "w")
                    val c by animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        label = "c",
                    )
                    Box(Modifier.height(8.dp).width(w).clip(CircleShape).background(c))
                }
            }
            Spacer(Modifier.weight(1f))
            val interaction = remember { MutableInteractionSource() }
            val btnWidth by animateDpAsState(if (last) 168.dp else 120.dp, label = "bw")
            Button(
                onClick = {
                    if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                interactionSource = interaction,
                modifier = Modifier.height(54.dp).width(btnWidth).pressScale(interaction),
                shape = RoundedCornerShape(18.dp),
            ) {
                AnimatedContent(
                    last,
                    transitionSpec = {
                        (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                    },
                    label = "btn",
                ) { isLast ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isLast) "Get started" else "Next")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Illustration(icon: ImageVector, accent: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "ill")
    val bob by t.animateFloat(-8f, 8f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "bob")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(12000)), label = "spin")
    val breathe by t.animateFloat(0.94f, 1.04f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "br")

    Box(modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(260.dp)) {
            val c = center
            drawCircle(accent.copy(alpha = 0.06f), radius = size.minDimension / 2f * breathe)
            drawCircle(accent.copy(alpha = 0.10f), radius = size.minDimension / 2.7f)
            val orbit = size.minDimension / 2.25f
            listOf(0f, 120f, 240f).forEachIndexed { i, base ->
                val a = Math.toRadians((base + spin * (if (i % 2 == 0) 1f else -0.7f)).toDouble())
                drawCircle(
                    accent.copy(alpha = 0.55f - i * 0.12f),
                    radius = (7 - i * 1.5f).dp.toPx(),
                    center = Offset(c.x + (orbit * cos(a)).toFloat(), c.y + (orbit * sin(a)).toFloat()),
                )
            }
        }
        Box(
            Modifier
                .graphicsLayer { translationY = bob * density }
                .size(120.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.85f), accent))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(56.dp))
        }
    }
}
