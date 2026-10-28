package com.lexora.english.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lexora.english.R
import com.lexora.english.ui.components.AppImage
import com.lexora.english.ui.components.PrimaryButton
import com.lexora.english.ui.theme.Lex
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class OnboardPage(val image: Int, val title: String, val body: String)

private val pages = listOf(
    OnboardPage(R.drawable.onboard_1_learn, "Learn English from A to Z", "A complete, structured course — from the alphabet to professional writing — with clear examples for every concept."),
    OnboardPage(R.drawable.onboard_2_practice, "Practice every single day", "5 fresh questions daily, timed tests, writing feedback, speaking and listening practice that build real confidence."),
    OnboardPage(R.drawable.onboard_3_projects, "Real-life English projects", "Apply what you learn in 15 real situations: interviews, travel, meetings, emails and more."),
    OnboardPage(R.drawable.onboard_4_certificate, "Earn your certificate", "Finish the course and all projects to receive a professional certificate with your name on it."),
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val c = Lex.colors
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.primarySoft.copy(alpha = if (c.isDark) 0.5f else 1f), c.background, c.background)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppImage(R.drawable.logo_mark, Modifier.size(30.dp), crossfade = false)
                Spacer(Modifier.width(8.dp))
                Text("Lexora", style = MaterialTheme.typography.titleLarge, color = c.text, modifier = Modifier.weight(1f))
                if (!last) {
                    Text(
                        "Skip",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.textSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onDone() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            HorizontalPager(
                state = pager,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                beyondViewportPageCount = 1,
            ) { index ->
                val page = pages[index]
                val offset = ((pager.currentPage - index) + pager.currentPageOffsetFraction).absoluteValue
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp)
                        .graphicsLayer {
                            alpha = 1f - (offset * 0.6f).coerceIn(0f, 0.6f)
                            val s = 1f - (offset * 0.08f).coerceIn(0f, 0.08f)
                            scaleX = s; scaleY = s
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    AppImage(
                        page.image,
                        Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .aspectRatio(1f),
                        contentDescription = page.title,
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(page.title, style = MaterialTheme.typography.headlineLarge, color = c.text, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        page.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                pages.indices.forEach { i ->
                    val selected = i == pager.currentPage
                    val w by animateDpAsState(if (selected) 26.dp else 8.dp, label = "dot")
                    val col by animateColorAsState(if (selected) c.primary else c.track, label = "dotc")
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(w)
                            .clip(CircleShape)
                            .background(col)
                    )
                }
            }

            PrimaryButton(
                text = if (last) "Get Started" else "Next",
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                onClick = {
                    if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}
