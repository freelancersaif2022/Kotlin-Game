package com.vaultly.app.ui.paywall

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.WorkspacePremium
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
import com.vaultly.app.ui.components.PrimaryButton
import com.vaultly.app.ui.components.findActivity
import com.vaultly.app.ui.theme.Palette
import kotlinx.coroutines.launch

@Composable
fun PaywallScreen(app: VaultlyApp, onClose: () -> Unit) {
    val plans by app.billing.plans.collectAsStateWithLifecycle()
    val connected by app.billing.connected.collectAsStateWithLifecycle()
    val pro by app.prefs.pro.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val proPlans = plans.filter { it.key != PlanKey.REMOVE_ADS }
    var selected by remember { mutableStateOf(PlanKey.YEARLY) }
    val chosen = proPlans.firstOrNull { it.key == selected } ?: proPlans.firstOrNull()

    BackHandler(onBack = onClose)
    LaunchedEffect(Unit) {
        app.billing.connect()
        app.billing.events.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    LaunchedEffect(pro) { if (pro) onClose() }

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1D2C4E), Palette.Ink, Palette.InkDeep))),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, 170.dp.toPx())
            listOf(80, 130, 185, 245).forEachIndexed { i, r ->
                drawCircle(Palette.GoldLight.copy(alpha = 0.16f - i * 0.035f), r.dp.toPx(), c, style = Stroke(1.dp.toPx()))
            }
        }
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Close, "Close", tint = Color.White.copy(alpha = 0.8f)) }
            }
            Spacer(Modifier.height(22.dp))
            Box(
                Modifier.size(92.dp).clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Palette.GoldLight, Palette.Gold, Palette.GoldDeep))),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.WorkspacePremium, null, Modifier.size(50.dp), tint = Palette.Ink) }
            Spacer(Modifier.height(26.dp))
            Text("VAULTLY PRO", style = MaterialTheme.typography.labelSmall, color = Palette.GoldLight)
            Spacer(Modifier.height(8.dp))
            Text(
                "Protect everything\nyou own.",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(26.dp))

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Benefit(Icons.Outlined.AllInclusive, "Unlimited purchases", "No cap on receipts, warranties or photos")
                Benefit(Icons.Outlined.PictureAsPdf, "Unlimited claim packs", "Export polished PDFs any time, no videos")
                Benefit(Icons.Outlined.Block, "Completely ad-free", "A calm, private vault with zero ads")
            }
            Spacer(Modifier.height(28.dp))

            if (proPlans.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.06f)).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (!connected) {
                        CircularProgressIndicator(Modifier.size(26.dp), color = Palette.GoldLight, strokeWidth = 2.5.dp)
                        Spacer(Modifier.height(12.dp))
                        Text("Connecting to Google Play…", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
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
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    proPlans.forEach { plan ->
                        PlanCard(plan, plan.key == chosen?.key) { selected = plan.key }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            PrimaryButton(
                when (chosen?.key) {
                    PlanKey.LIFETIME -> "Unlock Pro forever"
                    null -> "Continue"
                    else -> "Start Vaultly Pro"
                },
                { context.findActivity()?.let { a -> chosen?.let { app.billing.buy(a, it) } } },
                Modifier.fillMaxWidth(),
                enabled = chosen != null,
                gold = true,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                FooterLink("Restore purchases") { scope.launch { app.billing.restore(silent = false) } }
                plans.firstOrNull { it.key == PlanKey.REMOVE_ADS }?.let { plan ->
                    if (!app.prefs.removeAds.value) {
                        Text("  ·  ", color = Color.White.copy(alpha = 0.3f))
                        FooterLink("Remove ads only · ${plan.price}") { context.findActivity()?.let { app.billing.buy(it, plan) } }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Subscriptions renew automatically until cancelled. Cancel any time in Google Play › Subscriptions. " +
                    "Lifetime is a single payment.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
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

@Composable
private fun PlanCard(plan: Plan, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val (title, sub) = when (plan.key) {
        PlanKey.YEARLY -> "Yearly" to (plan.perMonth?.let { "Just $it, billed yearly" } ?: "Billed yearly")
        PlanKey.MONTHLY -> "Monthly" to "Billed monthly, cancel anytime"
        PlanKey.LIFETIME -> "Lifetime" to "Pay once, Pro forever"
        PlanKey.REMOVE_ADS -> "Remove ads" to "One-time"
    }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (selected) Palette.GoldLight.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.05f))
                .border(if (selected) 2.dp else 1.dp, if (selected) Palette.GoldLight else Color.White.copy(alpha = 0.12f), shape)
                .clickable(onClick = onClick)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(22.dp).clip(CircleShape)
                    .background(if (selected) Palette.GoldLight else Color.Transparent)
                    .border(1.5.dp, if (selected) Palette.GoldLight else Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(14.dp), tint = Palette.Ink) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f))
            }
            Text(plan.price, style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
        if (plan.key == PlanKey.YEARLY) {
            Text(
                "BEST VALUE",
                style = MaterialTheme.typography.labelSmall,
                color = Palette.Ink,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp)
                    .offset(y = (-9).dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.linearGradient(listOf(Palette.GoldLight, Palette.Gold)))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun FooterLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = 0.75f),
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(6.dp),
    )
}
