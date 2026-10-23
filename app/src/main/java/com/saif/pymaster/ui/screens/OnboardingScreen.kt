package com.saif.pymaster.ui.screens

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material.icons.rounded.RocketLaunch
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saif.pymaster.R
import com.saif.pymaster.ui.components.PrimaryButton
import com.saif.pymaster.ui.theme.Brand
import com.saif.pymaster.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

private data class OnboardPage(val image: Int, val title: String, val text: String, val accent: Color)

private val pages = listOf(
    OnboardPage(
        R.drawable.onboard_1, "Learn Python from A to Z",
        "12 structured modules take you from your very first print() to classes, generators and decorators — with real examples in every lesson.",
        Brand.PythonBlueBright,
    ),
    OnboardPage(
        R.drawable.onboard_2, "Code in a real terminal",
        "Write, edit and run real Python 3 right on your phone. Read errors, fix bugs and experiment like a professional developer.",
        Brand.Cyan,
    ),
    OnboardPage(
        R.drawable.onboard_3, "Daily quizzes & progress",
        "Fresh quiz questions arrive every day. Track your streak, study time and module progress with clear charts.",
        Brand.PythonYellow,
    ),
    OnboardPage(
        R.drawable.onboard_4, "Build 15 projects & get certified",
        "Finish the course to unlock 15 real-world projects. Complete them all to earn your professional PyMaster certificate.",
        Brand.Success,
    ),
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val colors = LocalAppColors.current
    val isLast = pager.currentPage == pages.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${pager.currentPage + 1} / ${pages.size}",
                style = MaterialTheme.typography.labelMedium,
                color = Brand.TextMuted,
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            )
            if (!isLast) {
                TextButton(onClick = onFinish) {
                    Text("Skip", style = MaterialTheme.typography.labelLarge, color = Brand.TextSecondary)
                }
            } else {
                Spacer(Modifier.height(48.dp))
            }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
            val page = pages[index]
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(
                            Brush.radialGradient(
                                listOf(page.accent.copy(alpha = 0.35f), Color.Transparent),
                                center = Offset(size.width / 2, size.height / 2),
                                radius = size.minDimension / 2,
                            )
                        )
                    }
                    Image(painterResource(page.image), page.title, Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    page.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Brand.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    page.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Brand.TextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            pages.forEachIndexed { i, page ->
                val selected = i == pager.currentPage
                val width by animateDpAsState(if (selected) 28.dp else 8.dp, label = "dot")
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(width)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) page.accent else Color(0x33FFFFFF))
                )
            }
        }

        PrimaryButton(
            text = if (isLast) "Get Started" else "Next",
            icon = if (isLast) Icons.Rounded.RocketLaunch else Icons.AutoMirrored.Rounded.ArrowForward,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp),
        ) {
            if (isLast) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
        }
    }
}
