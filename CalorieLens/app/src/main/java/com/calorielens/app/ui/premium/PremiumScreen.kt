package com.calorielens.app.ui.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calorielens.app.data.prefs.PremiumPlan
import com.calorielens.app.premium.PlanOffer
import com.calorielens.app.ui.components.AppDialog
import com.calorielens.app.ui.components.ConfettiBurst
import com.calorielens.app.ui.components.GradientButton
import com.calorielens.app.ui.components.IconCircleButton
import com.calorielens.app.ui.components.LoadingDialog
import com.calorielens.app.ui.components.LocalSnackbar
import com.calorielens.app.ui.components.MessageDialog
import com.calorielens.app.ui.components.PremiumEmblem
import com.calorielens.app.ui.components.SnackType
import com.calorielens.app.ui.components.pressScale
import com.calorielens.app.ui.theme.Brand
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

private data class Perk(val icon: ImageVector, val title: String, val subtitle: String)

private val perks = listOf(
    Perk(Icons.Rounded.AllInclusive, "Unlimited food scans", "No daily limit — scan every bite"),
    Perk(Icons.Rounded.Science, "Full micronutrients", "Vitamins A & C, calcium, iron, potassium"),
    Perk(Icons.Rounded.BarChart, "30-day insights", "Monthly trends, consistency & macro balance"),
    Perk(Icons.Rounded.History, "Unlimited history", "Your complete food diary, forever"),
    Perk(Icons.Rounded.Block, "Zero ads", "A clean, distraction-free experience"),
)

private val PaywallBg = Color(0xFF07070A)
private val TextOnDark = Color(0xFFF5F3EE)
private val MutedOnDark = Color(0xFFA19C92)

@Composable
fun PremiumScreen(onClose: () -> Unit, vm: PremiumViewModel = hiltViewModel()) {
    val selected by vm.selected.collectAsStateWithLifecycle()
    val purchase by vm.purchase.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbar.current
    val isPremium = settings?.isPremium == true

    Box(
        Modifier
            .fillMaxSize()
            .background(PaywallBg),
    ) {
        PaywallBackdrop()
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                IconCircleButton(
                    Icons.Rounded.Close, "Close", onClose,
                    size = 40.dp, background = Color.White.copy(alpha = 0.08f), tint = TextOnDark, bordered = false,
                )
            }
            PremiumEmblem(size = 150.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                "CalorieLens PRO",
                style = MaterialTheme.typography.displaySmall.copy(brush = Brand.goldGradient, letterSpacing = 0.5.sp),
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (isPremium) "You're a Pro member. Thanks for your support!" else "Eat smarter with the full power of on-device food AI.",
                style = MaterialTheme.typography.bodyLarge,
                color = MutedOnDark,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(26.dp))

            perks.forEachIndexed { i, perk -> PerkRow(perk, i) }

            Spacer(Modifier.height(22.dp))
            if (isPremium) {
                ActiveMembershipCard(settings?.premiumPlan?.label ?: "Pro", settings?.premiumSince ?: 0L)
                Spacer(Modifier.height(20.dp))
                GradientButton(
                    "Continue", onClose,
                    brush = Brand.goldGradient, contentColor = Color(0xFF2A1A00),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                vm.offers.forEach { offer ->
                    PlanCard(offer, offer.plan == selected) { vm.select(offer.plan) }
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(10.dp))
                GradientButton(
                    text = when (selected) {
                        PremiumPlan.LIFETIME -> "Unlock lifetime access"
                        else -> "Start Premium"
                    },
                    onClick = vm::buy,
                    brush = Brand.goldGradient,
                    contentColor = Color(0xFF2A1A00),
                    shimmer = true,
                    modifier = Modifier.fillMaxWidth(),
                    height = 60.dp,
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Verified, null, tint = Brand.Mint, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cancel anytime · Secure checkout", style = MaterialTheme.typography.bodySmall, color = MutedOnDark)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(
                        "Restore purchase",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextOnDark,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.pressScale(onClick = vm::restore),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Demo build: purchases are simulated on this device and no payment is taken.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedOnDark.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    when (val p = purchase) {
        PurchaseState.Processing -> LoadingDialog("Processing payment", "Confirming your purchase securely…", Brand.Gold)
        PurchaseState.Restoring -> LoadingDialog("Restoring", "Looking for previous purchases…", Brand.Gold)
        is PurchaseState.Success -> SuccessDialog(p.plan) {
            vm.reset()
            snackbar.show("All Pro features are now unlocked.", SnackType.PREMIUM, title = "Welcome to PRO")
            onClose()
        }
        is PurchaseState.RestoreResult -> MessageDialog(
            icon = if (p.found) Icons.Rounded.Check else Icons.Rounded.Search,
            iconColor = if (p.found) Brand.Mint else Brand.Amber,
            title = if (p.found) "Purchase restored" else "Nothing to restore",
            message = if (p.found) "Your Pro membership is active on this device." else "We couldn't find an earlier purchase for this account.",
            confirmText = "OK",
            onConfirm = vm::reset,
            onDismiss = vm::reset,
        )
        PurchaseState.Idle -> Unit
    }
}

@Composable
private fun PaywallBackdrop() {
    val t = rememberInfiniteTransition(label = "paywall")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse), label = "drift")
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(
            Brush.radialGradient(listOf(Brand.Gold.copy(alpha = 0.22f), Color.Transparent), Offset(size.width * (0.3f + 0.4f * drift), size.height * 0.14f), size.width * 0.9f),
            radius = size.width * 0.9f,
            center = Offset(size.width * (0.3f + 0.4f * drift), size.height * 0.14f),
        )
        drawCircle(
            Brush.radialGradient(listOf(Brand.GoldDark.copy(alpha = 0.16f), Color.Transparent), Offset(size.width * (0.8f - 0.5f * drift), size.height * 0.85f), size.width),
            radius = size.width,
            center = Offset(size.width * (0.8f - 0.5f * drift), size.height * 0.85f),
        )
        // Fine star dust
        for (i in 0 until 40) {
            val x = ((i * 73) % 100) / 100f * size.width
            val y = ((i * 37) % 100) / 100f * size.height * 0.55f
            val tw = ((i * 0.13f + drift) % 1f)
            drawCircle(Brand.GoldLight.copy(alpha = 0.08f + 0.25f * tw), 1.2.dp.toPx(), Offset(x, y))
        }
    }
}

@Composable
private fun PerkRow(perk: Perk, index: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(80L * index + 150); visible = true }
    AnimatedVisibility(visible, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 }) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF2B2416), Color(0xFF18140D))))
                    .border(1.dp, Brand.Gold.copy(alpha = 0.3f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(perk.icon, null, tint = Brand.Gold, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(perk.title, style = MaterialTheme.typography.titleMedium, color = TextOnDark)
                Text(perk.subtitle, style = MaterialTheme.typography.bodySmall, color = MutedOnDark)
            }
            Icon(Icons.Rounded.Check, null, tint = Brand.Mint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PlanCard(offer: PlanOffer, selected: Boolean, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) Brand.Gold else Color.White.copy(alpha = 0.1f), tween(250), label = "planBorder")
    val shape = RoundedCornerShape(20.dp)
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .pressScale(scaleDown = 0.98f, onClick = onClick)
                .clip(shape)
                .background(if (selected) Color(0xFF1E1910) else Color(0xFF111114))
                .border(if (selected) 2.dp else 1.dp, border, shape)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (selected) Brand.Gold else Color.White.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(Brand.goldGradient))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(offer.plan.label, style = MaterialTheme.typography.titleMedium, color = TextOnDark)
                offer.perMonth?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MutedOnDark) }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(offer.price, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = TextOnDark)
                Text(offer.period, style = MaterialTheme.typography.bodySmall, color = MutedOnDark)
            }
        }
        offer.badge?.let { badge ->
            Text(
                badge,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = Color(0xFF2A1A00),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-16).dp, y = (-9).dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brand.goldGradient)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ActiveMembershipCard(plan: String, since: Long) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF2B2416), Color(0xFF15120C))))
            .border(1.dp, Brush.linearGradient(listOf(Brand.GoldLight, Brand.GoldDark)), shape)
            .padding(20.dp),
    ) {
        Text("ACTIVE MEMBERSHIP", style = MaterialTheme.typography.labelSmall, color = Brand.Gold)
        Spacer(Modifier.height(6.dp))
        Text("$plan plan", style = MaterialTheme.typography.headlineSmall, color = TextOnDark)
        if (since > 0) {
            Text(
                "Member since ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(since))}",
                style = MaterialTheme.typography.bodySmall,
                color = MutedOnDark,
            )
        }
    }
}

@Composable
private fun SuccessDialog(plan: PremiumPlan, onContinue: () -> Unit) {
    AppDialog(onDismiss = onContinue) {
        Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
            ConfettiBurst(Modifier.fillMaxSize())
            val pop = remember { Animatable(0.3f) }
            LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
            Box(Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
                PremiumEmblem(size = 140.dp)
            }
        }
        Text("Welcome to PRO", style = MaterialTheme.typography.headlineMedium.copy(brush = Brand.goldGradient), fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your ${plan.label.lowercase()} plan is active. Unlimited scans, full nutrients, deeper insights and no ads.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(22.dp))
        GradientButton(
            "Let's go", onContinue,
            brush = Brand.goldGradient, contentColor = Color(0xFF2A1A00), shimmer = true,
            modifier = Modifier.fillMaxWidth(), height = 54.dp,
        )
    }
}
