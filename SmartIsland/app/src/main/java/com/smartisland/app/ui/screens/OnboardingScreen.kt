package com.smartisland.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartisland.app.data.IslandSettings
import com.smartisland.app.island.IslandActivity
import com.smartisland.app.ui.appContainer
import com.smartisland.app.ui.components.Demo
import com.smartisland.app.ui.components.IconBadge
import com.smartisland.app.ui.components.PhoneTopPreview
import com.smartisland.app.ui.components.PrimaryButton
import com.smartisland.app.ui.components.pressable
import com.smartisland.app.ui.components.rememberShowcase
import com.smartisland.app.ui.navigation.AppNavigator
import com.smartisland.app.ui.theme.Palette
import kotlinx.coroutines.launch

private val previewSettings = IslandSettings(glow = true)

private data class Page(val title: String, val body: String, val features: List<Triple<ImageVector, Color, String>>)

private val pages = listOf(
    Page(
        "Meet your island",
        "A living, breathing island at the top of your screen that morphs to show what matters right now.",
        listOf(
            Triple(Icons.Rounded.MusicNote, Color(0xFFFF375F), "Music with album art and controls"),
            Triple(Icons.Rounded.Call, Palette.Success, "Answer or decline calls"),
            Triple(Icons.Rounded.BatteryChargingFull, Palette.Success, "Charging animation"),
        ),
    ),
    Page(
        "17 live widgets",
        "Ultra-clean home screen widgets that update in real time: clocks, battery, media, stats and more.",
        listOf(
            Triple(Icons.Rounded.Widgets, Palette.Primary, "Clocks, calendar and world time"),
            Triple(Icons.Rounded.BatteryChargingFull, Palette.Success, "Battery ring and charging pulse"),
            Triple(Icons.Rounded.Notifications, Palette.Secondary, "Now playing and quick toggles"),
        ),
    ),
    Page(
        "Software power & volume",
        "Worn-out buttons? Lock, power menu, screenshots and a precise 0–100% volume control are one tap away.",
        listOf(
            Triple(Icons.Rounded.PowerSettingsNew, Palette.Error, "Power menu and screenshot"),
            Triple(Icons.Rounded.Lock, Palette.Primary, "One-tap lock screen"),
            Triple(Icons.Rounded.Tune, Palette.Warning, "Volume sliders for every stream"),
        ),
    ),
)

@Composable
fun OnboardingScreen(nav: AppNavigator) {
    val container = appContainer()
    var showSetup by remember { mutableStateOf(false) }

    fun finish() {
        container.settings.update { it.copy(onboardingDone = true) }
        nav.finishOnboarding()
    }

    AnimatedContent(showSetup, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "onboarding") { setup ->
        if (setup) {
            PermissionsScreen(nav, onDone = ::finish)
        } else {
            Intro(onContinue = { showSetup = true }, onSkip = ::finish)
        }
    }
}

@Composable
private fun Intro(onContinue: () -> Unit, onSkip: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val showcase = rememberShowcase()
    val last = pager.currentPage == pages.lastIndex

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Palette.HeroGradient)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // The phone preview scales with the screen so the page text always has room on small phones.
        val previewHeight = (maxHeight * 0.30f).coerceIn(170.dp, 250.dp)
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    "Skip",
                    style = MaterialTheme.typography.labelLarge,
                    color = Palette.TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .pressable(onSkip)
                        .padding(8.dp),
                )
            }

            Box(Modifier.padding(horizontal = 20.dp)) {
                val media = remember { Demo.media() }
                val activity: IslandActivity = when (pager.currentPage) {
                    0 -> showcase.first
                    1 -> media
                    else -> Demo.volume()
                }
                PhoneTopPreview(
                    previewSettings,
                    activity,
                    expanded = if (pager.currentPage == 0) showcase.second else pager.currentPage == 1,
                    height = previewHeight,
                )
            }

            HorizontalPager(pager, Modifier.weight(1f)) { index ->
                val page = pages[index]
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 26.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(page.title, style = MaterialTheme.typography.headlineMedium, color = Palette.TextPrimary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text(page.body, style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    page.features.forEach { (icon, color, text) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconBadge(icon, color, 36.dp)
                            Spacer(Modifier.width(14.dp))
                            Text(text, style = MaterialTheme.typography.bodyLarge, color = Palette.TextPrimary, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(pages.size) { i ->
                    val w by animateDpAsState(if (i == pager.currentPage) 22.dp else 7.dp, label = "dot")
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = w, height = 7.dp)
                            .clip(CircleShape)
                            .background(if (i == pager.currentPage) Palette.Primary else Palette.SurfaceHigh),
                    )
                }
            }
            PrimaryButton(
                if (last) "Set up Smart Island" else "Next",
                {
                    if (last) onContinue() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}
