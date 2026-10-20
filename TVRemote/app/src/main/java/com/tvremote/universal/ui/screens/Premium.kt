package com.tvremote.universal.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvremote.universal.App
import com.tvremote.universal.billing.Plan
import com.tvremote.universal.billing.PremiumManager
import com.tvremote.universal.ui.components.AppCard
import com.tvremote.universal.ui.components.AppDialog
import com.tvremote.universal.ui.components.AppSnackbarHost
import com.tvremote.universal.ui.components.PrimaryButton
import com.tvremote.universal.ui.components.SecondaryButton
import com.tvremote.universal.ui.components.SnackKind
import com.tvremote.universal.ui.components.goldBrush
import com.tvremote.universal.ui.components.heroBrush
import com.tvremote.universal.ui.components.show
import com.tvremote.universal.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import kotlin.math.cos
import kotlin.math.sin

private val orbitIcons = listOf(
    Icons.Rounded.Block,
    Icons.Rounded.Devices,
    Icons.Rounded.Keyboard,
    Icons.Rounded.TouchApp,
    Icons.Rounded.Apps,
    Icons.Rounded.PowerSettingsNew,
)

private val featureNames = listOf("No ads", "Unlimited TVs", "Keyboard", "Touchpad", "App shortcuts", "Power on")

@Composable
fun PremiumScreen(onBack: () -> Unit) {
    val container = App.container
    val billing = container.billing
    val activity = LocalActivity.current
    val context = LocalContext.current
    val offers by billing.offers.collectAsStateWithLifecycle()
    val billingEvent by billing.events.collectAsStateWithLifecycle()
    val isPremium by container.premium.isPremium.collectAsStateWithLifecycle()
    val purchased by container.premium.purchased.collectAsStateWithLifecycle()
    val plan by container.premium.plan.collectAsStateWithLifecycle()
    val passActive by container.premium.passActive.collectAsStateWithLifecycle()
    val rewardUntil by container.premium.rewardUntil.collectAsStateWithLifecycle()
    val rewardProgress by container.premium.rewardProgress.collectAsStateWithLifecycle()
    val rewardedReady by container.ads.rewardedReady.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(Plan.YEARLY) }
    var confirmMock by remember { mutableStateOf<Plan?>(null) }
    val scheme = MaterialTheme.colorScheme
    val extra = AppTheme.extra

    LaunchedEffect(Unit) { container.ads.loadRewarded() }
    LaunchedEffect(billingEvent) {
        billingEvent?.let {
            billing.consumeEvent()
            snackbar.show(it, if (it.startsWith("Welcome")) SnackKind.PREMIUM else SnackKind.INFO)
        }
    }

    fun buy() {
        if (billing.isMock) {
            confirmMock = selected
            return
        }
        val ok = activity != null && billing.launch(activity, selected)
        if (!ok) scope.launch { snackbar.show("Google Play isn't available right now. Please try again in a moment.", SnackKind.ERROR) }
    }

    Box(Modifier.fillMaxSize().background(scheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // ------------------------------------------------------------ hero
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                    .background(heroBrush())
                    .statusBarsPadding()
                    .padding(bottom = 26.dp),
            ) {
                Box(
                    Modifier.fillMaxWidth().height(300.dp)
                        .background(Brush.radialGradient(listOf(Color(0xFFFFC53D).copy(alpha = 0.22f), Color.Transparent))),
                )
                IconButton(onClick = onBack, modifier = Modifier.padding(6.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                }
                Column(
                    Modifier.fillMaxWidth().padding(top = 28.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BadgeOrbit()
                    Spacer(Modifier.height(10.dp))
                    Text("UNIVERSAL REMOTE", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f), letterSpacing = 3.sp)
                    Text(
                        "Premium",
                        style = TextStyle(brush = goldBrush, fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Every feature unlocked. No ads, ever. One remote for every TV in your home.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        featureNames.take(3).forEach { FeatureChip(it) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        featureNames.drop(3).forEach { FeatureChip(it) }
                    }
                }
            }

            Column(Modifier.padding(16.dp)) {
                if (isPremium) {
                    ActiveCard(purchased = purchased, planTitle = plan?.title, passActive = passActive, until = rewardUntil)
                    Spacer(Modifier.height(16.dp))
                    if (purchased && billing.isMock) {
                        SecondaryButton(
                            "Reset test purchase",
                            icon = Icons.Rounded.Science,
                            onClick = {
                                billing.resetMockPurchase()
                                scope.launch { snackbar.show("Test purchase removed", SnackKind.INFO) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else if (purchased && plan != Plan.LIFETIME) {
                        SecondaryButton(
                            "Manage subscription",
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions?package=${context.packageName}")),
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                if (!purchased) {
                    // -------------------------------------------------------- 24 h pass
                    if (!passActive) {
                        AppCard(
                            Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, extra.gold.copy(alpha = 0.6f)),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(goldBrush),
                                        contentAlignment = Alignment.Center,
                                    ) { Icon(Icons.Rounded.OndemandVideo, contentDescription = null, tint = Color.White) }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Get Premium free for 24 hours", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                                        Text(
                                            "Watch ${PremiumManager.REQUIRED_ADS} short videos to unlock everything for a day.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = scheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    repeat(PremiumManager.REQUIRED_ADS) { i ->
                                        val done = i < rewardProgress
                                        Row(
                                            Modifier
                                                .weight(1f)
                                                .height(30.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (done) goldBrush else Brush.linearGradient(listOf(scheme.surfaceVariant, scheme.surfaceVariant))),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                        ) {
                                            if (done) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "Video ${i + 1}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (done) Color.White else scheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                                PrimaryButton(
                                    text = if (rewardedReady) "Watch video ${rewardProgress + 1} of ${PremiumManager.REQUIRED_ADS}" else "Loading video…",
                                    icon = Icons.Rounded.OndemandVideo,
                                    brush = goldBrush,
                                    enabled = rewardedReady,
                                    onClick = {
                                        val act = activity ?: return@PrimaryButton
                                        container.ads.showRewarded(
                                            act,
                                            onReward = {
                                                val granted = container.premium.onRewardEarned()
                                                scope.launch {
                                                    if (granted) snackbar.show("Premium unlocked for 24 hours. Enjoy!", SnackKind.PREMIUM)
                                                    else snackbar.show(
                                                        "Nice! ${PremiumManager.REQUIRED_ADS - container.premium.rewardProgress.value} more to go.",
                                                        SnackKind.SUCCESS,
                                                    )
                                                }
                                            },
                                            onUnavailable = {
                                                scope.launch { snackbar.show("No video available right now. Please try again shortly.", SnackKind.ERROR) }
                                            },
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        Spacer(Modifier.height(22.dp))
                    }

                    // -------------------------------------------------------- plans
                    Text("Choose your plan", style = MaterialTheme.typography.titleLarge, color = scheme.onBackground, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
                    Plan.entries.forEach { p ->
                        val offer = offers[p]
                        PlanCard(
                            title = p.title,
                            subtitle = when {
                                offer?.trialText != null -> "${offer.trialText}, then ${p.period}"
                                p == Plan.YEARLY -> "Just ${yearlyMonthly(offer?.price ?: p.defaultPrice)} / month · ${p.period}"
                                else -> p.period.replaceFirstChar { it.uppercase() }
                            },
                            price = offer?.price ?: p.defaultPrice,
                            selected = selected == p,
                            badge = when (p) {
                                Plan.YEARLY -> "BEST VALUE"
                                Plan.LIFETIME -> "PAY ONCE"
                                else -> null
                            },
                            onClick = { selected = p },
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    val trial = offers[selected]?.takeIf { it.hasFreeTrial }
                    PrimaryButton(
                        text = if (trial != null) "Start ${trial.trialText ?: "free trial"}" else "Continue",
                        icon = Icons.Rounded.WorkspacePremium,
                        onClick = { buy() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (selected == Plan.LIFETIME) "One-time payment. No subscription, yours forever."
                        else "Subscription renews automatically until cancelled. Cancel anytime in Google Play › Payments & subscriptions › Subscriptions, at least 24 hours before renewal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (billing.isMock) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(scheme.primary.copy(alpha = 0.08f)).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Science, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Test mode: purchases are simulated and nothing is charged.", style = MaterialTheme.typography.bodySmall, color = scheme.primary)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = {
                        scope.launch { snackbar.show("Checking your purchases…", SnackKind.INFO) }
                        billing.restore { found ->
                            scope.launch {
                                if (found) snackbar.show("Purchase restored. Premium is active.", SnackKind.PREMIUM)
                                else snackbar.show("No previous purchases found on this Google account.", SnackKind.INFO)
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Restore purchases")
                }
                Spacer(Modifier.height(12.dp))
                Spacer(Modifier.navigationBarsPadding())
            }
        }
        AppSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }

    confirmMock?.let { p ->
        AppDialog(
            onDismiss = { confirmMock = null },
            icon = Icons.Rounded.WorkspacePremium,
            iconColor = extra.gold,
            title = "Confirm test purchase",
            message = "${p.title} plan · ${offers[p]?.price ?: p.defaultPrice} ${if (p == Plan.LIFETIME) "" else p.period}\n\nTest mode — no money will be charged.",
            confirmText = "Buy (test)",
            onConfirm = {
                confirmMock = null
                billing.completeMockPurchase(p)
            },
        )
    }
}

private fun yearlyMonthly(price: String): String {
    val number = price.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: return price
    val symbol = price.takeWhile { !it.isDigit() }
    return symbol + "%.2f".format(number / 12)
}

@Composable
private fun FeatureChip(text: String) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.12f)).padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFFFFC53D), modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

/** Gold premium badge with the premium features orbiting around it. */
@Composable
private fun BadgeOrbit() {
    val t = rememberInfiniteTransition(label = "orbit")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "angle")
    Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(200.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape))
        Box(Modifier.size(150.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)))
        orbitIcons.forEachIndexed { i, icon ->
            val a = Math.toRadians((angle + i * 60f).toDouble())
            val r = 92f
            Box(
                Modifier
                    .offset(x = (r * cos(a)).dp, y = (r * sin(a)).dp)
                    .size(38.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF15276E))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                OrbitIcon(icon)
            }
        }
        Box(
            Modifier
                .size(96.dp)
                .shadow(28.dp, CircleShape, ambientColor = Color(0xFFFFC53D), spotColor = Color(0xFFFFC53D))
                .clip(CircleShape)
                .background(goldBrush)
                .border(3.dp, Color.White.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.WorkspacePremium, contentDescription = "Premium", tint = Color.White, modifier = Modifier.size(54.dp).rotate(0f))
        }
    }
}

@Composable
private fun OrbitIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
}

@Composable
private fun ActiveCard(purchased: Boolean, planTitle: String?, passActive: Boolean, until: Long) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, ambientColor = Color(0xFFF5B400), spotColor = Color(0xFFF5B400))
            .clip(shape)
            .background(goldBrush)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(50.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp)) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text("Premium Active", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(
                when {
                    purchased && planTitle != null -> "$planTitle plan · thank you for your support!"
                    purchased -> "Thank you for your support!"
                    passActive -> "Free pass until ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(until))}"
                    else -> "All features unlocked"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.92f),
            )
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    subtitle: String,
    price: String,
    selected: Boolean,
    badge: String?,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val extra = AppTheme.extra
    val shape = RoundedCornerShape(22.dp)
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(if (selected) 10.dp else 2.dp, shape, ambientColor = extra.glow, spotColor = extra.glow)
                .clip(shape)
                .background(if (selected) scheme.primary.copy(alpha = if (extra.isDark) 0.18f else 0.07f) else extra.panel)
                .background(extra.panel.copy(alpha = if (selected) 0.6f else 0f))
                .border(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else extra.panelBorder, shape)
                .clickable(onClick = onClick)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (selected) scheme.primary else scheme.outline, CircleShape)
                    .background(if (selected) scheme.primary else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Text(price, style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
        }
        if (badge != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 18.dp)
                    .offset(y = (-10).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(goldBrush)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(badge, style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }
    }
}
