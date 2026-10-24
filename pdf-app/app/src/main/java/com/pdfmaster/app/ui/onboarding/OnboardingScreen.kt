package com.pdfmaster.app.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.pdfmaster.app.R
import com.pdfmaster.app.ui.LocalAppDeps
import com.pdfmaster.app.ui.components.PrimaryButton
import com.pdfmaster.app.ui.navigation.Routes
import com.pdfmaster.app.ui.theme.AppTheme
import com.pdfmaster.app.ui.theme.Brand
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class Page(@DrawableRes val image: Int, val title: String, val accent: String, val body: String)

private val pages = listOf(
    Page(R.drawable.img_onboarding_1_read, "Read PDFs", "beautifully", "A fast, smooth reader with search, bookmarks, night mode and page thumbnails."),
    Page(R.drawable.img_onboarding_2_edit_sign, "Edit, annotate", "& e-sign", "Highlight, draw, add notes and sign contracts in seconds — right on your phone."),
    Page(R.drawable.img_onboarding_3_tools, "Every PDF tool", "in one app", "Merge, split, compress, organize and convert files without uploading anything."),
    Page(R.drawable.img_onboarding_4_scan_secure, "Scan, OCR", "& protect", "Scan paper documents, pull out text and lock files with AES-256 passwords."),
)

@Composable
fun OnboardingScreen(nav: NavHostController) {
    val deps = LocalAppDeps.current
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    fun finish() {
        scope.launch {
            deps.prefs.setOnboardingDone()
            val showPaywall = !deps.premium.isPremium && !deps.prefs.paywallShown.first()
            nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            if (showPaywall) {
                deps.prefs.setPaywallShown()
                nav.navigate(Routes.premium("onboarding"))
            }
        }
    }

    val float = rememberInfiniteTransition(label = "float")
    val dy by float.animateFloat(-8f, 8f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dy")
    val glow = if (AppTheme.colors.isDark) Brand.Red.copy(alpha = 0.16f) else Brand.RedSoft

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.img_logo_wordmark), null, Modifier.padding(start = 8.dp).height(26.dp))
            Spacer(Modifier.weight(1f))
            AnimatedContent(!last, label = "skip") { visible ->
                if (visible) {
                    TextButton(onClick = { finish() }) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }
        }

        HorizontalPager(pager, modifier = Modifier.weight(1f).fillMaxWidth()) { index ->
            val offset = (pager.currentPage - index) + pager.currentPageOffsetFraction
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxWidth(0.82f).aspectRatio(1f).graphicsLayer { alpha = 1f - offset.absoluteValue.coerceIn(0f, 1f) * 0.6f }) {
                    drawCircle(Brush.radialGradient(listOf(glow, glow.copy(alpha = 0f))), radius = size.minDimension / 2)
                }
                Image(
                    painterResource(pages[index].image), null,
                    modifier = Modifier
                        .fillMaxWidth(0.84f)
                        .aspectRatio(1f)
                        .graphicsLayer {
                            translationX = offset * size.width * 0.35f
                            translationY = dy * density
                            val s = 1f - offset.absoluteValue.coerceIn(0f, 1f) * 0.18f
                            scaleX = s; scaleY = s
                            alpha = 1f - offset.absoluteValue.coerceIn(0f, 1f)
                        },
                )
            }
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(
                targetState = pager.currentPage,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInVertically(tween(380)) { it / 3 * dir } + fadeIn(tween(380))) togetherWith (slideOutVertically(tween(260)) { -it / 3 * dir } + fadeOut(tween(200)))
                },
                label = "text",
            ) { i ->
                val page = pages[i]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        buildAnnotatedString {
                            append(page.title)
                            append(" ")
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(page.accent) }
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        page.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.height(72.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { i ->
                    val selected = i == pager.currentPage
                    val w by animateDpAsState(if (selected) 28.dp else 8.dp, tween(300), label = "dot")
                    Box(
                        Modifier.height(8.dp).width(w).clip(RoundedCornerShape(4.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            PrimaryButton(
                text = if (last) "Get Started" else "Continue",
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                iconAtEnd = true,
                onClick = {
                    if (last) finish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "By continuing you agree to our Terms & Privacy Policy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
