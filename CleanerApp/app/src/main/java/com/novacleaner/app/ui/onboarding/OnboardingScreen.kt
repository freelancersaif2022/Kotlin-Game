package com.novacleaner.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.novacleaner.app.core.AppGraph
import com.novacleaner.app.ui.components.GlossyIcon
import com.novacleaner.app.ui.components.GradientButton
import com.novacleaner.app.ui.navigation.LocalNavigator
import com.novacleaner.app.ui.navigation.Routes
import com.novacleaner.app.ui.navigation.Tool
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

private data class Page(
    val title: String,
    val body: String,
    val hero: ImageVector,
    val colors: List<Color>,
    val satellites: List<Pair<ImageVector, List<Color>>>,
)

private val pages = listOf(
    Page(
        "Deep-clean junk\nin one tap",
        "Find cache, temporary files, empty folders and leftover packages hiding on your storage.",
        Icons.Rounded.CleaningServices, Tool.JUNK.colors,
        listOf(Icons.Rounded.Delete to Tool.SCREENSHOTS.colors, Icons.Rounded.Storage to Tool.LARGE_FILES.colors, Icons.Rounded.Bolt to Tool.BATTERY.colors),
    ),
    Page(
        "Reclaim space from\nbig & duplicate files",
        "Spot large videos, duplicate photos and old screenshots – review them and remove what you don't need.",
        Icons.Rounded.FolderOpen, Tool.LARGE_FILES.colors,
        listOf(Icons.Rounded.PhotoLibrary to Tool.DUPLICATES.colors, Icons.Rounded.Screenshot to Tool.SCREENSHOTS.colors, Icons.Rounded.Storage to Tool.JUNK.colors),
    ),
    Page(
        "Know every app\ninside out",
        "See each app's size, install date and when you last used it. Uninstall the ones you've forgotten about.",
        Icons.Rounded.Apps, Tool.APPS.colors,
        listOf(Icons.Rounded.History to Tool.DATA_USAGE.colors, Icons.Rounded.Storage to Tool.LARGE_FILES.colors, Icons.Rounded.Delete to Tool.SCREENSHOTS.colors),
    ),
    Page(
        "Stay on top of\nyour data",
        "Track mobile & Wi-Fi usage per app, set a monthly limit and watch your live network speed.",
        Icons.Rounded.DataUsage, Tool.DATA_USAGE.colors,
        listOf(Icons.Rounded.Speed to Tool.SPEED.colors, Icons.Rounded.Wifi to Tool.APPS.colors, Icons.Rounded.Bolt to Tool.BATTERY.colors),
    ),
)

@Composable
fun OnboardingScreen() {
    val navigator = LocalNavigator.current
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLast = pager.currentPage == pages.lastIndex
    val accent by animateColorAsState(pages[pager.currentPage].colors.last(), tween(500), label = "accent")

    fun finish() {
        scope.launch {
            AppGraph.prefs.setOnboardingDone()
            navigator.nav.navigate(Routes.PERMISSIONS) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(listOf(accent.copy(alpha = 0.22f), Color.Transparent), Offset(size.width / 2, size.height * 0.32f), size.width * 0.8f),
                size.width * 0.8f, Offset(size.width / 2, size.height * 0.32f),
            )
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                if (!isLast) TextButton(onClick = ::finish) {
                    Text("Skip", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else Spacer(Modifier.height(48.dp))
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
                val page = pages[index]
                val offset = ((pager.currentPage - index) + pager.currentPageOffsetFraction).absoluteValue
                Column(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 1f - offset.coerceIn(0f, 1f) * 0.6f
                            val s = 1f - offset.coerceIn(0f, 1f) * 0.1f
                            scaleX = s; scaleY = s
                        }
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    HeroIllustration(page)
                    Spacer(Modifier.height(40.dp))
                    Text(page.title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        page.body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                pages.indices.forEach { i ->
                    val selected = i == pager.currentPage
                    val width by animateDpAsState(if (selected) 26.dp else 8.dp, label = "dot")
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) accent else MaterialTheme.colorScheme.outline),
                    )
                }
            }
            AnimatedContent(isLast, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) }, label = "cta") { last ->
                GradientButton(
                    text = if (last) "Get Started" else "Next",
                    onClick = {
                        if (last) finish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    },
                    icon = if (last) null else Icons.Rounded.ArrowForward,
                    colors = pages[pager.currentPage].colors,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun HeroIllustration(page: Page) {
    val transition = rememberInfiniteTransition(label = "hero")
    val angle by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "orbit")
    val float by transition.animateFloat(-6f, 6f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "float")
    val ring = page.colors.first()
    Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(ring.copy(alpha = 0.10f), size.minDimension / 2)
            drawCircle(ring.copy(alpha = 0.16f), size.minDimension / 2 * 0.72f)
            drawCircle(ring.copy(alpha = 0.22f), size.minDimension / 2 * 0.72f, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
        }
        page.satellites.forEachIndexed { i, (icon, colors) ->
            val a = ((angle + i * 120f) * PI / 180.0)
            val r = 112f
            GlossyIcon(
                icon, colors, size = 50.dp,
                modifier = Modifier.offset((cos(a) * r).dp, (sin(a) * r).dp),
            )
        }
        GlossyIcon(page.hero, page.colors, size = 128.dp, modifier = Modifier.offset(y = float.dp))
    }
}
