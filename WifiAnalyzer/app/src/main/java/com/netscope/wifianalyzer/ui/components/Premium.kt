package com.netscope.wifianalyzer.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.netscope.wifianalyzer.AppConfig
import com.netscope.wifianalyzer.AppGraph
import com.netscope.wifianalyzer.data.PremiumState
import com.netscope.wifianalyzer.ui.theme.AppColors
import com.netscope.wifianalyzer.ui.theme.Palette

class PremiumActions(val openRewardUnlock: () -> Unit, val openPaywall: () -> Unit)

val LocalPremiumActions = staticCompositionLocalOf<PremiumActions> { error("No premium actions") }

@Composable
fun rememberPremiumState(): State<PremiumState> = AppGraph.premium.state.collectAsState()

fun formatRemaining(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format(java.util.Locale.US, "%dh %02dm %02ds", h, m, s) else String.format(java.util.Locale.US, "%dm %02ds", m, s)
}

/**
 * Shows [content] for premium users. Everyone else sees a blurred preview
 * with ways to unlock: 3 rewarded ads (12 hours) or a subscription.
 */
@Composable
fun PremiumGate(
    feature: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    preview: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val premium by rememberPremiumState()
    if (premium.isPremium) {
        content()
        return
    }
    Box(modifier.fillMaxSize()) {
        if (preview != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(14.dp) else Modifier)
                    .alpha(if (Build.VERSION.SDK_INT >= 31) 0.55f else 0.18f),
            ) { preview() }
        }
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            LockedFeatureCard(feature, description, icon)
        }
    }
}

@Composable
fun LockedFeatureCard(feature: String, description: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val actions = LocalPremiumActions.current
    val premium by rememberPremiumState()
    val pulse = rememberInfiniteTransition(label = "lock")
    val glow by pulse.animateFloat(0.85f, 1.08f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "glow")
    AppCard(modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(22.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(86.dp)
                        .graphicsLayer {
                            scaleX = glow
                            scaleY = glow
                        }
                        .clip(CircleShape)
                        .background(Palette.Gold.copy(alpha = 0.14f)),
                )
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(AppColors.ext.premiumGradient),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp)) }
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Lock, null, tint = AppColors.ext.gold, modifier = Modifier.size(13.dp)) }
            }
            Spacer(Modifier.height(14.dp))
            ProBadge(text = "PREMIUM")
            Spacer(Modifier.height(10.dp))
            Text(feature, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = AppColors.ext.muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            GradientButton(
                "Unlock free for 12 hours",
                onClick = actions.openRewardUnlock,
                icon = Icons.Rounded.PlayCircle,
                modifier = Modifier.fillMaxWidth(),
            )
            if (premium.rewardAdsWatched > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "${premium.rewardAdsWatched} of ${AppConfig.REWARD_ADS_REQUIRED} ads watched",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(10.dp))
            GradientButton(
                "Go Premium • No ads",
                onClick = actions.openPaywall,
                icon = Icons.Rounded.WorkspacePremium,
                brush = AppColors.ext.premiumGradient,
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Checks premium; if locked, opens the unlock dialog instead of running [action]. */
@Composable
fun rememberPremiumAction(): (() -> Unit) -> Unit {
    val actions = LocalPremiumActions.current
    return remember(actions) {
        { action -> if (AppGraph.premium.isPremium) action() else actions.openRewardUnlock() }
    }
}

private enum class RewardStep { Ready, Loading, Unlocked, Unavailable }

/** "Watch 3 ads, get 12 hours of Premium" flow. */
@Composable
fun RewardUnlockDialog(onDismiss: () -> Unit, onGoPremium: () -> Unit) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val premium by rememberPremiumState()
    val snackbar = LocalSnackbar.current
    var step by remember { mutableStateOf(RewardStep.Ready) }
    val watched = premium.rewardAdsWatched
    val required = AppConfig.REWARD_ADS_REQUIRED

    fun watchNext() {
        if (activity == null) return
        step = RewardStep.Loading
        AppGraph.ads.ensureRewarded { ready ->
            if (!ready) {
                step = RewardStep.Unavailable
                return@ensureRewarded
            }
            AppGraph.ads.showRewarded(
                activity,
                onReward = {
                    val unlocked = AppGraph.premium.onRewardEarned()
                    if (unlocked) step = RewardStep.Unlocked
                },
                onClosed = { earned ->
                    if (step != RewardStep.Unlocked) {
                        step = RewardStep.Ready
                        if (!earned) snackbar.show("Watch the full ad to earn your reward", SnackType.Info)
                    }
                },
            )
        }
    }

    if (step == RewardStep.Unlocked) {
        Box(Modifier.fillMaxSize()) {
            AppDialog(
                onDismiss = onDismiss,
                icon = Icons.Rounded.WorkspacePremium,
                iconBrush = AppColors.ext.premiumGradient,
                title = "Premium unlocked!",
                message = "Enjoy every Pro feature with no ads for the next 12 hours.",
                confirmText = "Start exploring",
                onConfirm = onDismiss,
            ) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Time left: ${formatRemaining(premium.rewardRemainingMs)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            ConfettiBurst()
        }
        return
    }

    AppDialog(
        onDismiss = onDismiss,
        icon = Icons.Rounded.PlayCircle,
        iconBrush = AppColors.ext.primaryGradient,
        title = "Unlock Premium free",
        message = "Watch $required short video ads to unlock every Pro feature for 12 hours. No payment needed.",
        dismissText = "Maybe later",
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(required) { i ->
                val done = i < watched
                val active = i == watched
                val fill by animateFloatAsState(if (done) 1f else 0f, tween(500), label = "seg$i")
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (done) Palette.Mint.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                2.dp,
                                when {
                                    done -> Palette.Mint
                                    active -> MaterialTheme.colorScheme.primary
                                    else -> Color.Transparent
                                },
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedContent(done, transitionSpec = { (scaleIn() + fadeIn()) togetherWith fadeOut() }, label = "check$i") { d ->
                            if (d) {
                                Icon(Icons.Rounded.Check, null, tint = Palette.Mint)
                            } else {
                                Text("${i + 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Box(Modifier.fillMaxWidth(fill).height(4.dp).background(Palette.Mint))
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        when (step) {
            RewardStep.Loading -> Row(
                Modifier.fillMaxWidth().height(56.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
                Spacer(Modifier.width(12.dp))
                Text("Loading ad…", style = MaterialTheme.typography.titleSmall)
            }
            RewardStep.Unavailable -> {
                Text(
                    "No ads are available right now. Please try again in a minute, or go Premium to unlock instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.ext.warning,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                GradientButton("Try again", onClick = { watchNext() }, modifier = Modifier.fillMaxWidth())
            }
            else -> GradientButton(
                "Watch ad ${watched + 1} of $required",
                onClick = { watchNext() },
                icon = Icons.Rounded.PlayCircle,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(10.dp))
        GradientButton(
            "Go Premium instead",
            onClick = onGoPremium,
            icon = Icons.Rounded.WorkspacePremium,
            brush = AppColors.ext.premiumGradient,
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth(),
            height = 50.dp,
        )
    }
}

@Composable
fun PremiumStatusCardCompact(onUnlock: () -> Unit, onPaywall: () -> Unit) {
    AppCard(
        Modifier.fillMaxWidth(),
        brush = androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(Palette.Gold.copy(alpha = 0.16f), Palette.Pink.copy(alpha = 0.12f)),
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(AppColors.ext.premiumGradient),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.WorkspacePremium, null, tint = Color.White) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("6 Pro tools are locked", style = MaterialTheme.typography.titleSmall)
                Text("Unlock all free for 12h or remove ads forever", style = MaterialTheme.typography.bodySmall, color = AppColors.ext.muted)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SoftButton("Watch 3 ads", onClick = onUnlock, icon = Icons.Rounded.PlayCircle, modifier = Modifier.weight(1f), height = 44.dp)
            SoftButton("Go Premium", onClick = onPaywall, icon = Icons.Rounded.WorkspacePremium, tint = AppColors.ext.gold, modifier = Modifier.weight(1f), height = 44.dp)
        }
    }
}
