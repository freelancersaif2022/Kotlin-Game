package com.tvremote.universal.ui.screens

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvremote.universal.App
import com.tvremote.universal.billing.Plan
import com.tvremote.universal.billing.PlanOffer
import com.tvremote.universal.billing.PremiumManager
import com.tvremote.universal.ui.components.AppCard
import com.tvremote.universal.ui.components.PrimaryButton
import com.tvremote.universal.ui.components.SecondaryButton
import com.tvremote.universal.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private data class Feature(val icon: ImageVector, val title: String, val body: String)

private val features = listOf(
    Feature(Icons.Rounded.Block, "No ads", "A clean, distraction-free remote"),
    Feature(Icons.Rounded.Devices, "Unlimited TVs", "Pair every TV in your home"),
    Feature(Icons.Rounded.Keyboard, "Keyboard", "Type searches and passwords fast"),
    Feature(Icons.Rounded.TouchApp, "Touchpad", "Swipe to navigate, tap to select"),
    Feature(Icons.Rounded.Apps, "App shortcuts", "Open YouTube, Netflix & more instantly"),
    Feature(Icons.Rounded.PowerSettingsNew, "Power on", "Turn your TV on from the app"),
)

@Composable
fun PremiumScreen(onBack: () -> Unit) {
    val container = App.container
    val activity = LocalActivity.current
    val offers by container.billing.offers.collectAsStateWithLifecycle()
    val billingEvent by container.billing.events.collectAsStateWithLifecycle()
    val isPremium by container.premium.isPremium.collectAsStateWithLifecycle()
    val purchased by container.premium.purchased.collectAsStateWithLifecycle()
    val rewardUntil by container.premium.rewardUntil.collectAsStateWithLifecycle()
    val rewardProgress by container.premium.rewardProgress.collectAsStateWithLifecycle()
    val rewardedReady by container.ads.rewardedReady.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(Plan.YEARLY) }
    val extra = AppTheme.extra

    LaunchedEffect(Unit) { container.ads.loadRewarded() }
    LaunchedEffect(billingEvent) {
        billingEvent?.let {
            container.billing.consumeEvent()
            snackbar.showSnackbar(it)
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // ---------------------------------------------------------------- hero
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                    .background(Brush.linearGradient(listOf(extra.heroStart, extra.heroEnd)))
                    .statusBarsPadding()
                    .padding(bottom = 28.dp),
            ) {
                IconButton(onClick = onBack, modifier = Modifier.padding(6.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                }
                Column(
                    Modifier.fillMaxWidth().padding(top = 44.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(84.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = extra.gold, modifier = Modifier.size(48.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("TV Remote Premium", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "The complete remote experience — no ads, every feature.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Column(Modifier.padding(16.dp)) {
                if (isPremium) {
                    AppCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = extra.success, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Premium is active", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (purchased) "Thank you for supporting TV Remote!"
                                    else "Free Premium until ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(rewardUntil))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ------------------------------------------------------------ features
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        features.forEach { f ->
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(f.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(f.title, style = MaterialTheme.typography.titleSmall)
                                    Text(f.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = extra.success)
                            }
                        }
                    }
                }

                if (!purchased) {
                    // -------------------------------------------------------- plans
                    Spacer(Modifier.height(20.dp))
                    Text("Choose your plan", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
                    PlanCard(Plan.YEARLY, offers[Plan.YEARLY], "Yearly", "per year", selected == Plan.YEARLY, badge = "BEST VALUE") { selected = Plan.YEARLY }
                    Spacer(Modifier.height(10.dp))
                    PlanCard(Plan.MONTHLY, offers[Plan.MONTHLY], "Monthly", "per month", selected == Plan.MONTHLY) { selected = Plan.MONTHLY }
                    Spacer(Modifier.height(10.dp))
                    PlanCard(Plan.LIFETIME, offers[Plan.LIFETIME], "Lifetime", "pay once, yours forever", selected == Plan.LIFETIME) { selected = Plan.LIFETIME }
                    Spacer(Modifier.height(16.dp))
                    val trial = offers[selected]?.takeIf { it.hasFreeTrial }
                    PrimaryButton(
                        text = if (trial != null) "Start ${trial.trialText ?: "free trial"}" else "Continue",
                        onClick = {
                            val ok = activity != null && container.billing.launch(activity, selected)
                            if (!ok) scope.launch { snackbar.showSnackbar("Google Play isn't available right now. Please try again in a moment.") }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        if (selected == Plan.LIFETIME) "One-time payment. No subscription."
                        else "Renews automatically. Cancel anytime in Google Play › Subscriptions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )

                    // -------------------------------------------------------- rewarded
                    Spacer(Modifier.height(22.dp))
                    AppCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(extra.gold.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.OndemandVideo, contentDescription = null, tint = extra.gold)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Get Premium free for 24 hours", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Watch ${PremiumManager.REQUIRED_ADS} short videos to unlock every feature for a day.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                repeat(PremiumManager.REQUIRED_ADS) { i ->
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .clip(CircleShape)
                                            .background(if (i < rewardProgress) extra.gold else MaterialTheme.colorScheme.surfaceVariant),
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            SecondaryButton(
                                text = if (rewardedReady) "Watch video ${rewardProgress + 1} of ${PremiumManager.REQUIRED_ADS}" else "Loading video…",
                                icon = Icons.Rounded.OndemandVideo,
                                onClick = {
                                    val act = activity ?: return@SecondaryButton
                                    container.ads.showRewarded(
                                        act,
                                        onReward = {
                                            val granted = container.premium.onRewardEarned()
                                            scope.launch {
                                                snackbar.showSnackbar(
                                                    if (granted) "Premium unlocked for 24 hours. Enjoy!"
                                                    else "Nice! ${PremiumManager.REQUIRED_ADS - container.premium.rewardProgress.value} more to go.",
                                                )
                                            }
                                        },
                                        onUnavailable = {
                                            scope.launch { snackbar.showSnackbar("No video available right now. Please try again shortly.") }
                                        },
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = {
                        container.billing.refreshPurchases()
                        scope.launch { snackbar.showSnackbar("Checking your purchases…") }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("Restore purchases") }
                Spacer(Modifier.height(12.dp))
                Spacer(Modifier.navigationBarsPadding())
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp))
    }
}

@Composable
private fun PlanCard(
    @Suppress("UNUSED_PARAMETER") plan: Plan,
    offer: PlanOffer?,
    title: String,
    period: String,
    selected: Boolean,
    badge: String? = null,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (selected) scheme.primaryContainer.copy(alpha = 0.6f) else scheme.surface)
                .border(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant, shape)
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (selected) scheme.primary else scheme.outline, CircleShape)
                    .background(if (selected) scheme.primary else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    offer?.trialText?.let { "$it, then $period" } ?: period.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Text(offer?.price ?: "—", style = MaterialTheme.typography.titleMedium)
        }
        if (badge != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp)
                    .offset(y = (-10).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(AppTheme.extra.gold, Color(0xFFFF8A00))))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(badge, style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }
    }
}
