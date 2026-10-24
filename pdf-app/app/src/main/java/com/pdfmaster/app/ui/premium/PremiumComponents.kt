package com.pdfmaster.app.ui.premium

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfmaster.app.R
import com.pdfmaster.app.premium.PremiumManager
import com.pdfmaster.app.premium.PremiumState
import com.pdfmaster.app.ui.LocalAppDeps
import com.pdfmaster.app.ui.components.LoadingDialog
import com.pdfmaster.app.ui.components.LocalSnackbar
import com.pdfmaster.app.ui.components.PrimaryButton
import com.pdfmaster.app.ui.components.SnackType
import com.pdfmaster.app.ui.components.bounceClick
import com.pdfmaster.app.ui.navigation.LocalNavigator
import com.pdfmaster.app.ui.rememberActivity
import com.pdfmaster.app.ui.theme.Brand
import com.pdfmaster.app.util.Format
import kotlinx.coroutines.launch

@Composable
fun rememberPremiumState(): PremiumState {
    val state by LocalAppDeps.current.premium.state.collectAsStateWithLifecycle()
    return state
}

/** Top-bar chip: "PRO" upsell, live pass countdown, or a Premium badge. */
@Composable
fun PremiumChip(modifier: Modifier = Modifier) {
    val state = rememberPremiumState()
    val nav = LocalNavigator.current
    val pulse = rememberInfiniteTransition(label = "pulse")
    val glow by pulse.animateFloat(1f, 1.06f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    AnimatedContent(
        targetState = when {
            state.purchased -> 2
            state.passActive -> 1
            else -> 0
        },
        transitionSpec = { (fadeIn() + slideInVertically { it / 2 }) togetherWith (fadeOut() + slideOutVertically { -it / 2 }) },
        label = "chip",
        modifier = modifier,
    ) { mode ->
        Row(
            Modifier
                .graphicsLayer { if (mode == 0) { scaleX = glow; scaleY = glow } }
                .clip(RoundedCornerShape(50))
                .background(Brand.GoldGradient)
                .bounceClick { nav.premium("chip") }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (mode == 1) Icons.Rounded.Timer else Icons.Rounded.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                when (mode) {
                    2 -> "PREMIUM"
                    1 -> Format.countdown(state.passRemaining)
                    else -> "PRO"
                },
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = if (mode == 1) FontFamily.Monospace else FontFamily.SansSerif,
            )
        }
    }
}

/** Circular countdown showing what's left of the 5-hour pass. */
@Composable
fun PassTimerRing(state: PremiumState, size: Dp = 76.dp, stroke: Dp = 7.dp, dark: Boolean = false) {
    val progress by animateFloatAsState(state.passProgress, tween(900), label = "pass")
    val track = if (dark) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val s = stroke.toPx()
            val arc = Size(this.size.width - s, this.size.height - s)
            drawArc(track, 0f, 360f, false, Offset(s / 2, s / 2), arc, style = Stroke(s))
            drawArc(Brush.sweepGradient(listOf(Brand.Gold, Brand.GoldDeep, Brand.Gold)), -90f, 360f * progress, false, Offset(s / 2, s / 2), arc, style = Stroke(s, cap = StrokeCap.Round))
        }
        Icon(Icons.Rounded.WorkspacePremium, null, tint = Brand.Gold, modifier = Modifier.size(size * 0.38f))
    }
}

/** Card shown while a pass is running, with a live HH:MM:SS countdown. */
@Composable
fun PassActiveCard(state: PremiumState, modifier: Modifier = Modifier, onExtend: (() -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2A1A0A), Color(0xFF1B1D29))))
            .border(1.dp, Brand.Gold.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PassTimerRing(state, dark = true)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("Premium Pass active", color = Brand.Gold, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                Format.countdown(state.passRemaining),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
            )
            Text("All Pro tools unlocked · No ads", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
        }
        if (onExtend != null) {
            TextButton(onClick = onExtend) { Text("+5h", color = Brand.Gold, fontWeight = FontWeight.ExtraBold) }
        }
    }
}

/** Launches a rewarded ad and grants the 5-hour pass, with loading and result feedback. */
@Stable
class RewardLauncher internal constructor(private val start: (onGranted: () -> Unit) -> Unit) {
    fun launch(onGranted: () -> Unit = {}) = start(onGranted)
}

@Composable
fun rememberRewardLauncher(): RewardLauncher {
    val deps = LocalAppDeps.current
    val activity = rememberActivity()
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    if (loading) LoadingDialog("Loading your reward…")
    return remember(activity) {
        RewardLauncher { onGranted ->
            if (loading) return@RewardLauncher
            loading = true
            scope.launch {
                val shown = deps.ads.showRewarded(activity) { earned ->
                    if (earned) {
                        scope.launch {
                            deps.premium.grantRewardPass()
                            onGranted()
                        }
                    } else {
                        snackbar.show("Watch the full video to unlock Premium for 5 hours.", SnackType.WARNING)
                    }
                }
                loading = false
                if (!shown) {
                    snackbar.show("No video is available right now. Please try again in a minute.", SnackType.WARNING, title = "Ad not ready")
                }
            }
        }
    }
}

@Stable
class PremiumGate internal constructor(private val open: (feature: String, onAllowed: () -> Unit) -> Unit, private val manager: PremiumManager) {
    /** Runs [onAllowed] immediately for Premium users, otherwise offers the reward pass or an upgrade. */
    fun require(feature: String, onAllowed: () -> Unit) {
        if (manager.isPremium) onAllowed() else open(feature, onAllowed)
    }
}

@Composable
fun rememberPremiumGate(): PremiumGate {
    val deps = LocalAppDeps.current
    var request by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val reward = rememberRewardLauncher()
    val nav = LocalNavigator.current
    request?.let { (feature, onAllowed) ->
        PremiumGateSheet(
            feature = feature,
            onDismiss = { request = null },
            onWatchAd = {
                request = null
                reward.launch(onAllowed)
            },
            onUpgrade = {
                request = null
                nav.premium("gate")
            },
        )
    }
    return remember { PremiumGate({ f, cb -> request = f to cb }, deps.premium) }
}

@Composable
fun PremiumGateSheet(feature: String, onDismiss: () -> Unit, onWatchAd: () -> Unit, onUpgrade: () -> Unit, title: String? = null, message: String? = null) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surface, dragHandle = null) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outline))
            Spacer(Modifier.height(12.dp))
            Image(painterResource(R.drawable.img_reward_unlock), null, Modifier.size(150.dp))
            Text(title ?: "Unlock $feature", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                message ?: "$feature is a Premium tool. Watch a short video to unlock every Pro tool for 5 hours, or go Premium for unlimited access.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Watch Ad · Free for 5 Hours", onWatchAd, icon = Icons.Rounded.PlayCircle, brush = Brand.GoldGradient)
            Spacer(Modifier.height(10.dp))
            PrimaryButton("Upgrade to Premium", onUpgrade, icon = Icons.Rounded.WorkspacePremium)
            TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 4.dp)) {
                Text("Not now", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FreeLimitSheet(onDismiss: () -> Unit, onWatchAd: () -> Unit, onUpgrade: () -> Unit) {
    PremiumGateSheet(
        feature = "",
        title = "Daily free limit reached",
        message = "You've used all ${PremiumManager.FREE_DAILY_LIMIT} free tasks for today. Watch a short video for 5 hours of unlimited access, or go Premium.",
        onDismiss = onDismiss, onWatchAd = onWatchAd, onUpgrade = onUpgrade,
    )
}
