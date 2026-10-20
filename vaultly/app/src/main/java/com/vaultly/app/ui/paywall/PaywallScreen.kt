package com.vaultly.app.ui.paywall

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vaultly.app.VaultlyApp
import com.vaultly.app.billing.Plan
import com.vaultly.app.billing.PlanKey
import com.vaultly.app.ui.components.ClaimScene
import com.vaultly.app.ui.components.NoAdsScene
import com.vaultly.app.ui.components.PrimaryButton
import com.vaultly.app.ui.components.UnlimitedScene
import com.vaultly.app.ui.components.findActivity
import com.vaultly.app.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Showcase(val title: String, val body: String, val scene: @Composable () -> Unit)

private val showcase = listOf(
    Showcase("Unlimited vault", "Keep every receipt and warranty – no 15-item cap.") { UnlimitedScene() },
    Showcase("Claim packs, any time", "Export PDF proof for returns and claims instantly.") { ClaimScene() },
    Showcase("Calm and ad-free", "No interruptions. Just your vault.") { NoAdsScene() },
)

@Composable
fun PaywallScreen(app: VaultlyApp, onClose: () -> Unit) {
    val plans by app.billing.plans.collectAsStateWithLifecycle()
    val connected by app.billing.connected.collectAsStateWithLifecycle()
    val purchasing by app.billing.purchasing.collectAsStateWithLifecycle()
    val pro by app.prefs.pro.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val subPlans = plans.filter { it.key == PlanKey.YEARLY || it.key == PlanKey.MONTHLY }
    var selected by rememberSaveable { mutableStateOf(PlanKey.YEARLY) }
    val chosen = subPlans.firstOrNull { it.key == selected } ?: subPlans.firstOrNull()
    val wasPro = remember { pro }
    var celebrate by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = onClose)
    LaunchedEffect(Unit) { app.billing.connect() }
    LaunchedEffect(pro) { if (pro && !wasPro) celebrate = true }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1D2C4E), Palette.Ink, Palette.InkDeep)))) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Close, "Close", tint = Color.White.copy(alpha = 0.8f)) }
            }

            // 1. How Pro works – animated
            ShowcasePager()

            // 2. Title
            Spacer(Modifier.height(14.dp))
            Text("VAULTLY PRO", style = MaterialTheme.typography.labelSmall, color = Palette.GoldLight)
            Spacer(Modifier.height(8.dp))
            Text(
                "Protect everything\nyou own.",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))

            // 3. Plans
            if (subPlans.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.06f)).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (!connected) {
                        CircularProgressIndicator(Modifier.size(26.dp), color = Palette.GoldLight, strokeWidth = 2.5.dp)
                        Spacer(Modifier.height(12.dp))
                        Text("Loading plans…", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
                    } else {
                        Text(
                            "Plans aren't available right now. Make sure Vaultly was installed from Google Play and you're signed in.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    subPlans.forEach { plan -> PlanRow(plan, plan.key == chosen?.key) { selected = plan.key } }
                }
            }

            // 4. Continue
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                if (purchasing) "Processing…" else "Continue",
                { context.findActivity()?.let { a -> chosen?.let { app.billing.buy(a, it) } } },
                Modifier.fillMaxWidth(),
                enabled = chosen != null,
                loading = purchasing,
                gold = true,
            )
            if (chosen?.isMock == true) {
                Spacer(Modifier.height(8.dp))
                Text("Test build: purchases are simulated, no charge.", style = MaterialTheme.typography.bodySmall, color = Palette.GoldLight.copy(alpha = 0.7f))
            }

            // 5. Restore
            Spacer(Modifier.height(6.dp))
            Text(
                "Restore purchase",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { scope.launch { app.billing.restore(silent = false) } }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )

            // 6. Benefits
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Benefit(Icons.Outlined.AllInclusive, "Unlimited purchases", "No cap on receipts, warranties or photos")
                Benefit(Icons.Outlined.PictureAsPdf, "Unlimited claim packs", "Export polished PDFs any time, no videos")
                Benefit(Icons.Outlined.Block, "Completely ad-free", "A calm, private vault with zero ads")
            }

            // 7. Terms
            Spacer(Modifier.height(18.dp))
            Text(
                "Subscriptions renew automatically at the price shown until cancelled. Cancel any time in Google Play › " +
                    "Payments & subscriptions › Subscriptions, at least 24 hours before the renewal date.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
        }

        AnimatedVisibility(celebrate, enter = fadeIn(tween(250)), exit = fadeOut(tween(250))) {
            ProCelebration(onDone = { celebrate = false; onClose() })
        }
    }
}

@Composable
private fun ShowcasePager() {
    val pager = rememberPagerState { showcase.size }
    LaunchedEffect(pager) {
        while (true) {
            delay(3600)
            pager.animateScrollToPage((pager.currentPage + 1) % showcase.size)
        }
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(300.dp)) {
            listOf(70, 110, 150).forEachIndexed { i, r ->
                drawCircle(Palette.GoldLight.copy(alpha = 0.14f - i * 0.04f), r.dp.toPx(), Offset(size.width / 2, size.height / 2), style = Stroke(1.dp.toPx()))
            }
        }
        HorizontalPager(pager, Modifier.fillMaxWidth()) { i ->
            val item = showcase[i]
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                item.scene()
                Text(item.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text(item.body, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.Center) {
        repeat(showcase.size) { i ->
            val c by animateColorAsState(if (pager.currentPage == i) Palette.GoldLight else Color.White.copy(alpha = 0.22f), label = "dot")
            Box(Modifier.padding(horizontal = 3.dp).size(if (pager.currentPage == i) 18.dp else 6.dp, 6.dp).clip(CircleShape).background(c))
        }
    }
}

@Composable
private fun PlanRow(plan: Plan, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val (title, sub) = when (plan.key) {
        PlanKey.YEARLY -> "Yearly" to (plan.perMonth?.let { "Only $it · billed yearly" } ?: "Billed once a year")
        else -> "Monthly" to "Billed monthly · cancel anytime"
    }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (selected) Palette.GoldLight.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.05f))
                .border(if (selected) 2.dp else 1.dp, if (selected) Palette.GoldLight else Color.White.copy(alpha = 0.14f), shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(24.dp).clip(CircleShape)
                    .background(if (selected) Palette.GoldLight else Color.Transparent)
                    .border(1.5.dp, if (selected) Palette.GoldLight else Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(15.dp), tint = Palette.Ink) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(plan.price, style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(if (plan.key == PlanKey.YEARLY) "per year" else "per month", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
            }
        }
        if (plan.key == PlanKey.YEARLY) {
            Row(Modifier.align(Alignment.TopEnd).padding(end = 18.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge("BEST VALUE", Brush.linearGradient(listOf(Palette.GoldLight, Palette.Gold)), Palette.Ink)
                plan.saving?.let { Badge(it.uppercase(), Brush.linearGradient(listOf(Color(0xFF3CC28C), Color(0xFF2A9D6E))), Color.White) }
            }
        }
    }
}

@Composable
private fun Badge(text: String, brush: Brush, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .offset(y = (-10).dp)
            .clip(RoundedCornerShape(6.dp))
            .background(brush)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun Benefit(icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, Palette.GoldLight.copy(alpha = 0.2f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(21.dp), tint = Palette.GoldLight) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(body, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f))
        }
    }
}
