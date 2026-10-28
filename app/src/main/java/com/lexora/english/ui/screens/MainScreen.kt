package com.lexora.english.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lexora.english.R
import com.lexora.english.data.AppRepository
import com.lexora.english.data.AppState
import com.lexora.english.ui.Nav
import com.lexora.english.ui.components.AppImage
import com.lexora.english.ui.components.LinearProgress
import com.lexora.english.ui.components.PrimaryButton
import com.lexora.english.ui.theme.Brand
import com.lexora.english.ui.theme.Lex
import com.lexora.english.util.rememberHaptics

val LocalTutorialTargets = staticCompositionLocalOf<SnapshotStateMap<String, Rect>?> { null }

/** Registers this element's on-screen bounds so the first-run tutorial can spotlight it. */
fun Modifier.tutorialTarget(key: String): Modifier = composed {
    val targets = LocalTutorialTargets.current
    if (targets == null) this else this.onGloballyPositioned { targets[key] = it.boundsInWindow() }
}

private data class Tab(val title: String, val icon: ImageVector, val key: String)

private val tabs = listOf(
    Tab("Home", Icons.Rounded.Home, "nav_home"),
    Tab("Learn", Icons.AutoMirrored.Rounded.MenuBook, "nav_learn"),
    Tab("Practice", Icons.Rounded.EditNote, "nav_practice"),
    Tab("Settings", Icons.Rounded.Settings, "nav_settings"),
)

@Composable
fun MainScreen(state: AppState, nav: Nav) {
    val c = Lex.colors
    val repo = AppRepository.get(LocalContext.current)
    val haptics = rememberHaptics()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val targets = remember { mutableStateMapOf<String, Rect>() }
    val homeList = rememberLazyListState()
    val learnList = rememberLazyListState()
    val practiceList = rememberLazyListState()
    val settingsList = rememberLazyListState()
    val showTutorial = !state.tutorialDone && state.profile != null

    BackHandler(enabled = tab != 0) { tab = 0 }

    CompositionLocalProvider(LocalTutorialTargets provides targets) {
        Box(
            Modifier
                .fillMaxSize()
                .background(c.background)
        ) {
            Column(Modifier.fillMaxSize()) {
                Crossfade(
                    targetState = tab,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    animationSpec = tween(220),
                    label = "tabs",
                ) { t ->
                    when (t) {
                        0 -> HomeScreen(state, nav, homeList, onOpenTab = { tab = it })
                        1 -> LearnScreen(state, nav, learnList)
                        2 -> PracticeScreen(state, nav, practiceList)
                        else -> SettingsScreen(state, nav, settingsList)
                    }
                }
                BottomBar(tab) {
                    if (it != tab) haptics.tick()
                    tab = it
                }
            }
            if (showTutorial) {
                HomeTutorial(
                    name = state.profile?.name?.substringBefore(' ').orEmpty(),
                    targets = targets,
                    onStepShown = { if (tab != 0) tab = 0 },
                    onFinish = { repo.setTutorialDone(true) },
                )
            }
        }
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val c = Lex.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.navBar)
    ) {
        HorizontalDivider(color = c.border, thickness = 1.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            tabs.forEachIndexed { index, tab ->
                val sel = index == selected
                val bg by animateColorAsState(if (sel) c.primarySoft else Color.Transparent, tween(250), label = "nb")
                val fg by animateColorAsState(if (sel) c.primary else c.textMuted, tween(250), label = "nf")
                Column(
                    Modifier
                        .weight(1f)
                        .tutorialTarget(tab.key)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(index) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(bg)
                            .padding(horizontal = 18.dp, vertical = 5.dp)
                    ) {
                        Icon(tab.icon, tab.title, tint = fg, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(tab.title, style = MaterialTheme.typography.labelMedium, color = fg)
                }
            }
        }
    }
}

private data class TutorialStep(val target: String?, val title: String, val body: String)

@Composable
private fun HomeTutorial(
    name: String,
    targets: Map<String, Rect>,
    onStepShown: () -> Unit,
    onFinish: () -> Unit,
) {
    val c = Lex.colors
    val haptics = rememberHaptics()
    val steps = remember(name) {
        listOf(
            TutorialStep(null, "Welcome${if (name.isNotEmpty()) ", $name" else ""}! 👋", "Let me show you around Lexora in 30 seconds."),
            TutorialStep("progress", "Your progress", "See your course progress here. Tap Start to begin — Lexora always remembers where you stopped."),
            TutorialStep("daily", "Daily challenge", "5 new questions arrive every day. Answer them to keep your streak alive and earn XP."),
            TutorialStep("bell", "Notifications", "Lesson, project and daily quiz updates are saved here so you never miss anything."),
            TutorialStep("nav_learn", "Learn", "Your full A to Z course with charts, lessons, quizzes and the 15 real-life projects."),
            TutorialStep("nav_practice", "Practice", "Tests, writing feedback, speaking, listening and flashcards — practise like a pro."),
            TutorialStep("nav_settings", "Settings", "Edit your profile, switch to dark mode, change font size, vibration and more."),
        )
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var overlayHeight by remember { mutableFloatStateOf(0f) }
    val step = steps[index]
    val density = LocalDensity.current
    val screenHeightPx = if (overlayHeight > 0f) overlayHeight else with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val pad = with(density) { 8.dp.toPx() }

    val raw = step.target?.let { targets[it] }?.translate(-origin.x, -origin.y)
    val target = raw?.let { Rect(it.left - pad, it.top - pad, it.right + pad, it.bottom + pad) }
    val animated by animateRectAsState(target ?: Rect(0f, 0f, 0f, 0f), tween(380), label = "spot")

    fun next() {
        haptics.tick()
        if (index < steps.lastIndex) {
            index++
            onStepShown()
        } else onFinish()
    }

    AnimatedVisibility(visible = true, enter = fadeIn(tween(300)), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    origin = it.positionInWindow()
                    overlayHeight = it.size.height.toFloat()
                }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { next() }
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            ) {
                drawRect(c.scrim)
                if (target != null) {
                    val r = CornerRadius(22.dp.toPx())
                    drawRoundRect(Color.Transparent, animated.topLeft, animated.size, r, blendMode = BlendMode.Clear)
                    drawRoundRect(Color.White.copy(alpha = 0.9f), animated.topLeft, Size(animated.width, animated.height), r, style = Stroke(2.dp.toPx()))
                }
            }

            val card: @Composable () -> Unit = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(c.card)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (step.target == null) {
                        AppImage(R.drawable.tutorial_welcome, Modifier.size(150.dp), crossfade = false)
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(step.title, style = MaterialTheme.typography.headlineSmall, color = c.text, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(step.body, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    LinearProgress((index + 1f) / steps.size, Modifier.width(120.dp), height = 4.dp, color = Brand.Blue)
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (index < steps.lastIndex) {
                            Text(
                                "Skip",
                                style = MaterialTheme.typography.labelLarge,
                                color = c.textMuted,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onFinish() }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        PrimaryButton(
                            if (index == steps.lastIndex) "Let's go!" else "Next",
                            onClick = { next() },
                            modifier = Modifier.width(140.dp),
                            height = 46.dp,
                        )
                    }
                }
            }

            when {
                target == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { card() }
                target.center.y < screenHeightPx / 2 -> {
                    val top = with(density) { (target.bottom + 16.dp.toPx()).toDp() }
                    Box(Modifier.fillMaxWidth().padding(top = top)) { card() }
                }
                else -> {
                    val fromBottom = with(density) { (screenHeightPx - target.top + 16.dp.toPx()).toDp() }
                    Box(Modifier.fillMaxSize().padding(bottom = fromBottom.coerceAtLeast(0.dp)), contentAlignment = Alignment.BottomCenter) { card() }
                }
            }
        }
    }
}
