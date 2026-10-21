package com.netscope.wifianalyzer.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.netscope.wifianalyzer.ui.components.AppIcons
import com.netscope.wifianalyzer.AppConfig
import com.netscope.wifianalyzer.AppGraph
import com.netscope.wifianalyzer.billing.PlanInfo
import com.netscope.wifianalyzer.ui.components.GradientButton
import com.netscope.wifianalyzer.ui.components.LocalPremiumActions
import com.netscope.wifianalyzer.ui.components.LocalSnackbar
import com.netscope.wifianalyzer.ui.components.SnackType
import com.netscope.wifianalyzer.ui.components.findActivity
import com.netscope.wifianalyzer.ui.components.formatRemaining
import com.netscope.wifianalyzer.ui.components.rememberPopIn
import com.netscope.wifianalyzer.ui.components.rememberPremiumState
import com.netscope.wifianalyzer.ui.navigation.Routes
import com.netscope.wifianalyzer.ui.navigation.navigateTab
import com.netscope.wifianalyzer.ui.theme.AppColors
import com.netscope.wifianalyzer.ui.theme.Palette
import java.text.NumberFormat
import java.util.Currency
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class Perk(val icon: ImageVector, val title: String, val color: Color)

private val perks = listOf(
    Perk(Icons.Rounded.Block, "No ads, ever", Palette.Rose),
    Perk(Icons.Rounded.Devices, "See every device on your Wi-Fi", Palette.Lilac),
    Perk(Icons.Rounded.Security, "Intruder alerts for new devices", Palette.Mint),
    Perk(Icons.Rounded.Tune, "Channel analyzer & best channel", Palette.Orange),
    Perk(Icons.Rounded.GridOn, "Room-by-room signal heatmap", Palette.Pink),
    Perk(Icons.Rounded.Insights, "Full history, charts & CSV export", Palette.Blue),
    Perk(Icons.Rounded.DataUsage, "Data usage per app", Palette.Gold),
    Perk(Icons.Rounded.Router, "One-tap router admin", Palette.Violet),
    Perk(Icons.Rounded.Widgets, "Home screen widgets", Palette.Cyan),
)

private fun formatMicros(micros: Long, currency: String): String = runCatching {
    NumberFormat.getCurrencyInstance().apply { this.currency = Currency.getInstance(currency) }.format(micros / 1_000_000.0)
}.getOrDefault("")

@Composable
fun PremiumScreen(nav: NavHostController) {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val actions = LocalPremiumActions.current
    val plans by AppGraph.billing.plans.collectAsState()
    val premium by rememberPremiumState()
    var selected by remember { mutableStateOf(AppConfig.PRODUCT_YEARLY) }

    val monthly = plans[AppConfig.PRODUCT_MONTHLY]
    val yearly = plans[AppConfig.PRODUCT_YEARLY]
    val savings = if (monthly != null && yearly != null && monthly.priceMicros > 0) {
        (100 - yearly.priceMicros * 100.0 / (monthly.priceMicros * 12)).roundToInt().coerceAtLeast(0)
    } else {
        37
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PaywallBackdrop()
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth()) {
                IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Rounded.Close, "Close", tint = AppColors.ext.muted) }
            }
            PremiumHero()
            if (premium.isPremium) {
                ActivePremium(
                    subscribed = premium.subscribed,
                    plan = premium.plan,
                    remainingMs = premium.rewardRemainingMs,
                    onManage = { openSubscriptions(context) },
                    onExplore = { nav.popBackStack(); nav.navigateTab(Routes.TOOLS) },
                )
            } else {
            Spacer(Modifier.height(18.dp))
            Text("WiFi Analyzer Premium", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                "Every pro tool unlocked. Zero ads. Faster, safer Wi-Fi.",
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.ext.muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, AppColors.ext.cardBorder, RoundedCornerShape(22.dp))
                    .padding(vertical = 8.dp, horizontal = 16.dp),
            ) {
                perks.forEachIndexed { i, p ->
                    val pop = rememberPopIn(120 + i * 70)
                    Row(
                        Modifier
                            .padding(vertical = 7.dp)
                            .graphicsLayer {
                                alpha = pop
                                translationX = (1f - pop) * 40f
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(p.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(p.icon, null, tint = p.color, modifier = Modifier.size(17.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Text(p.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.CheckCircle, null, tint = AppColors.ext.success, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            if (premium.subscribed) {
                Text("You're Premium. Thank you for your support!", style = MaterialTheme.typography.titleMedium, color = AppColors.ext.gold)
                Spacer(Modifier.height(14.dp))
                GradientButton(
                    "Manage subscription",
                    onClick = { openSubscriptions(context) },
                    brush = AppColors.ext.premiumGradient,
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                PlanCard(
                    title = "Yearly",
                    price = yearly?.formattedPrice ?: AppConfig.FALLBACK_YEARLY_PRICE,
                    period = "/ year",
                    detail = yearly?.let { "Just ${formatMicros(it.priceMicros / 12, it.currency)} per month" } ?: "Just $1.25 per month",
                    strike = monthly?.let { formatMicros(it.priceMicros * 12, it.currency) } ?: "$23.88",
                    badge = "BEST VALUE • SAVE $savings%",
                    trialDays = yearly?.freeTrialDays ?: 0,
                    selected = selected == AppConfig.PRODUCT_YEARLY,
                ) { selected = AppConfig.PRODUCT_YEARLY }
                Spacer(Modifier.height(12.dp))
                PlanCard(
                    title = "Monthly",
                    price = monthly?.formattedPrice ?: AppConfig.FALLBACK_MONTHLY_PRICE,
                    period = "/ month",
                    detail = "Flexible, cancel anytime",
                    strike = null,
                    badge = null,
                    trialDays = monthly?.freeTrialDays ?: 0,
                    selected = selected == AppConfig.PRODUCT_MONTHLY,
                ) { selected = AppConfig.PRODUCT_MONTHLY }
                Spacer(Modifier.height(20.dp))
                val plan: PlanInfo? = plans[selected]
                GradientButton(
                    if ((plan?.freeTrialDays ?: 0) > 0) "Start ${plan?.freeTrialDays}-day free trial" else "Continue",
                    onClick = {
                        val activity = context.findActivity()
                        if (activity == null) {
                            snackbar.show("Something went wrong. Please try again.", SnackType.Error)
                        } else {
                            AppGraph.billing.launch(activity, selected)
                        }
                    },
                    icon = AppIcons.Crown,
                    brush = AppColors.ext.premiumGradient,
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    height = 58.dp,
                )
                Spacer(Modifier.height(12.dp))
                if (!premium.rewardActive) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { actions.openRewardUnlock() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.PlayCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Or watch ${AppConfig.REWARD_ADS_REQUIRED} ads for 12 hours free",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Payment is charged to your Google Play account. Subscriptions renew automatically unless cancelled at least 24 hours before the end of the current period. Manage or cancel anytime in Google Play.",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.ext.subtle,
                    textAlign = TextAlign.Center,
                )
            }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FooterLink("Restore") { AppGraph.billing.refreshPurchases(reportRestore = true) }
                FooterLink("Terms") { openUrl(context, AppConfig.TERMS_URL) }
                FooterLink("Privacy") { nav.navigate(Routes.PRIVACY) }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ActivePremium(
    subscribed: Boolean,
    plan: String?,
    remainingMs: Long,
    onManage: () -> Unit,
    onExplore: () -> Unit,
) {
    Spacer(Modifier.height(22.dp))
    Text(if (subscribed) "You're Premium" else "Premium is active", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text(
        if (subscribed) "Every Pro tool is unlocked and ads are gone. Thank you for your support!"
        else "Every Pro tool is unlocked and ads are off for the time shown below.",
        style = MaterialTheme.typography.bodyLarge,
        color = AppColors.ext.muted,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.ext.gold.copy(alpha = 0.08f))
            .border(1.dp, AppColors.ext.gold.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (subscribed) "Current plan" else "Time remaining",
            style = MaterialTheme.typography.labelLarge,
            color = AppColors.ext.muted,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                !subscribed -> formatRemaining(remainingMs)
                plan == AppConfig.PRODUCT_YEARLY -> "Yearly"
                plan == AppConfig.PRODUCT_MONTHLY -> "Monthly"
                else -> "Premium"
            },
            style = MaterialTheme.typography.headlineSmall,
            color = AppColors.ext.gold,
            fontWeight = FontWeight.Bold,
        )
        if (!subscribed) {
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { (remainingMs.toFloat() / AppConfig.REWARD_DURATION_MS).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = AppColors.ext.gold,
                trackColor = AppColors.ext.gold.copy(alpha = 0.15f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
    }
    Spacer(Modifier.height(20.dp))
    GradientButton("Explore Pro tools", onClick = onExplore, modifier = Modifier.fillMaxWidth())
    if (subscribed) {
        Spacer(Modifier.height(10.dp))
        GradientButton(
            "Manage subscription",
            onClick = onManage,
            brush = AppColors.ext.premiumGradient,
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FooterLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = AppColors.ext.muted,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun PaywallBackdrop() {
    val t = rememberInfiniteTransition(label = "backdrop")
    val a by t.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "a")
    Canvas(Modifier.fillMaxWidth().height(420.dp)) {
        val c1 = Offset(size.width * (0.3f + 0.15f * cos(a)), size.height * (0.3f + 0.1f * sin(a)))
        val c2 = Offset(size.width * (0.75f - 0.12f * sin(a)), size.height * (0.4f + 0.12f * cos(a)))
        drawCircle(Brush.radialGradient(listOf(Palette.Gold.copy(alpha = 0.28f), Color.Transparent), c1, size.width * 0.55f), size.width * 0.55f, c1)
        drawCircle(Brush.radialGradient(listOf(Palette.Pink.copy(alpha = 0.22f), Color.Transparent), c2, size.width * 0.5f), size.width * 0.5f, c2)
    }
}

@Composable
private fun PremiumHero() {
    val t = rememberInfiniteTransition(label = "hero")
    val float by t.animateFloat(-6f, 6f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "float")
    val shine by t.animateFloat(-1f, 2f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "shine")
    val pop = rememberPopIn()
    Box(
        Modifier
            .graphicsLayer {
                translationY = float * density
                scaleX = 0.6f + 0.4f * pop
                scaleY = 0.6f + 0.4f * pop
            }
            .size(116.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(AppColors.ext.premiumGradient),
        contentAlignment = Alignment.Center,
    ) {
        Icon(AppIcons.Crown, null, tint = Color.White, modifier = Modifier.size(64.dp))
        Canvas(Modifier.fillMaxSize()) {
            val x = size.width * shine
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                    start = Offset(x - size.width * 0.3f, 0f),
                    end = Offset(x + size.width * 0.3f, size.height),
                ),
            )
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    period: String,
    detail: String,
    strike: String?,
    badge: String?,
    trialDays: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val border by animateColorAsState(if (selected) AppColors.ext.gold else AppColors.ext.cardBorder, label = "planBorder")
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = if (badge != null) 10.dp else 0.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (selected) AppColors.ext.gold.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface)
                .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                null,
                tint = if (selected) AppColors.ext.gold else AppColors.ext.subtle,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (trialDays > 0) "$trialDays days free, then $detail".replace("Just", "just") else detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.ext.muted,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (strike != null) {
                    Text(strike, style = MaterialTheme.typography.labelSmall, color = AppColors.ext.subtle, textDecoration = TextDecoration.LineThrough)
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(price, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(period, style = MaterialTheme.typography.labelSmall, color = AppColors.ext.muted, modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
                }
            }
        }
        if (badge != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-16).dp)
                    .clip(CircleShape)
                    .background(AppColors.ext.premiumGradient)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(badge, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
            }
        }
    }
}
