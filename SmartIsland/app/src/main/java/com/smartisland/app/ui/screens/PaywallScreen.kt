package com.smartisland.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.VerticalSplit
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartisland.app.billing.PurchaseResult
import com.smartisland.app.billing.SubscriptionProduct
import com.smartisland.app.data.IslandSettings
import com.smartisland.app.data.Plan
import com.smartisland.app.ui.appContainer
import com.smartisland.app.ui.components.AppDialog
import com.smartisland.app.ui.components.LocalSnack
import com.smartisland.app.ui.components.PhoneTopPreview
import com.smartisland.app.ui.components.PrimaryButton
import com.smartisland.app.ui.components.ProgressDialog
import com.smartisland.app.ui.components.ScreenTopBar
import com.smartisland.app.ui.components.SnackType
import com.smartisland.app.ui.components.pressable
import com.smartisland.app.ui.components.rememberShowcase
import com.smartisland.app.ui.findActivity
import com.smartisland.app.ui.navigation.AppNavigator
import com.smartisland.app.ui.theme.Palette
import kotlinx.coroutines.launch

private data class Perk(val icon: ImageVector, val color: Color, val title: String, val detail: String)

private val perks = listOf(
    Perk(Icons.Rounded.Widgets, Palette.Primary, "All 12 widgets", "Charging Pulse, World Clock, Now Playing, Stopwatch and more"),
    Perk(Icons.Rounded.Notifications, Palette.Secondary, "Notification alerts", "Messages pop out of the island like on iPhone"),
    Perk(Icons.AutoMirrored.Rounded.VolumeUp, Palette.Primary, "Island volume HUD", "Volume keys show a sleek 0–100% bar"),
    Perk(Icons.Rounded.VerticalSplit, Palette.Warning, "Edge panel", "Floating power & volume buttons on any screen"),
    Perk(Icons.Rounded.AutoAwesome, Palette.Cyan, "Styles", "Accent colors and a soft glow"),
)

@Composable
fun PaywallScreen(nav: AppNavigator) {
    val container = appContainer()
    val context = LocalContext.current
    val snack = LocalSnack.current
    val scope = rememberCoroutineScope()
    val billing = container.billing
    val premium by container.premium.state.collectAsStateWithLifecycle()
    val showcase = rememberShowcase()

    var selected by remember { mutableStateOf(billing.products.first()) }
    var confirm by remember { mutableStateOf<SubscriptionProduct?>(null) }
    var processing by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.HeroGradient),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 190.dp),
        ) {
            ScreenTopBar("", onBack = nav::back)
            Column(Modifier.padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PhoneTopPreview(IslandSettings(glow = true), showcase.first, showcase.second, height = 200.dp)
                Spacer(Modifier.height(22.dp))
                Box(
                    Modifier
                        .size(58.dp)
                        .shadow(16.dp, CircleShape, ambientColor = Palette.Gold, spotColor = Palette.GoldDeep)
                        .clip(CircleShape)
                        .background(Palette.GoldGradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.WorkspacePremium, null, tint = Palette.OnGold, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("Smart Island PRO", style = MaterialTheme.typography.headlineMedium, color = Palette.TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(
                    "The complete island experience, every widget and pro controls.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(22.dp))
                // Monthly first, then the yearly best-value plan (selected by default).
                billing.products.sortedBy { it.plan == Plan.YEARLY }.forEach { product ->
                    PlanCard(product, product == selected) { selected = product }
                    Spacer(Modifier.height(14.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "EVERYTHING IN PRO",
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.TextSecondary,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Palette.Surface)
                        .border(1.dp, Palette.Outline, RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    perks.forEach { PerkRow(it) }
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Palette.Background.copy(alpha = 0.96f))
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (premium.isPremium) {
                PrimaryButton("You're PRO ✓", nav::back, Modifier.fillMaxWidth(), color = Palette.Success)
            } else {
                PrimaryButton(
                    if (selected.trialDays > 0) "Start ${selected.trialDays}-day free trial" else "Subscribe for ${selected.price}/${selected.period}",
                    { confirm = selected },
                    Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                buildString {
                    if (selected.trialDays > 0) append("Free for ${selected.trialDays} days, then ")
                    append("${selected.price}/${selected.period}. Auto-renews until cancelled in Google Play.")
                },
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextTertiary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Restore purchases",
                style = MaterialTheme.typography.labelLarge,
                color = Palette.PrimarySoft,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .pressable({
                        processing = true
                        scope.launch {
                            val ok = billing.restore()
                            processing = false
                            if (ok) {
                                snack.success("Purchases restored")
                                nav.back()
                            } else {
                                snack.show("No active subscription found")
                            }
                        }
                    })
                    .padding(6.dp),
            )
        }
    }

    confirm?.let { product ->
        AppDialog(
            onDismiss = { confirm = null },
            icon = Icons.Rounded.WorkspacePremium,
            title = "Confirm your plan",
            message = buildString {
                if (product.trialDays > 0) append("Enjoy ${product.trialDays} days free, then ") else append("You'll be charged ")
                append("${product.price} per ${product.period}. Cancel anytime in Google Play.")
                if (billing.isMock) append("\n\nTest mode: no money is charged.")
            },
            confirmText = if (product.trialDays > 0) "Start free trial" else "Subscribe now",
            confirmIcon = Icons.Rounded.WorkspacePremium,
            confirmGold = true,
            accent = Palette.GoldDeep,
            header = { PlanSummary(product) },
            onConfirm = {
                confirm = null
                val activity = context.findActivity() ?: return@AppDialog
                processing = true
                scope.launch {
                    val result = billing.purchase(activity, product)
                    processing = false
                    when (result) {
                        PurchaseResult.Success -> {
                            snack.show("Welcome to Smart Island PRO 🎉", SnackType.PREMIUM)
                            nav.back()
                        }
                        PurchaseResult.Cancelled -> snack.show("Purchase cancelled")
                        is PurchaseResult.Error -> snack.error(result.message)
                    }
                }
            },
            dismissText = "Not now",
        )
    }

    if (processing) ProgressDialog("Processing", "Talking to Google Play…")
}

/** Gold plan summary shown at the top of the purchase confirmation. */
@Composable
private fun PlanSummary(product: SubscriptionProduct) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Palette.GoldCardGradient)
            .border(1.dp, Palette.Gold.copy(alpha = 0.7f), shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Palette.GoldGradient),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.WorkspacePremium, null, tint = Palette.OnGold, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Smart Island PRO", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text("${product.title} plan", style = MaterialTheme.typography.bodySmall, color = Palette.Gold)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(product.price, style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("per ${product.period}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun PerkRow(perk: Perk) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(perk.color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(perk.icon, null, tint = perk.color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(perk.title, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
            Text(perk.detail, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
        }
        Icon(Icons.Rounded.Check, null, tint = Palette.Success, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun PlanCard(product: SubscriptionProduct, selected: Boolean, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) Palette.Primary else Palette.Outline, label = "plan")
    val bg by animateColorAsState(if (selected) Palette.Primary.copy(alpha = 0.12f) else Palette.Surface, label = "planBg")
    val shape = RoundedCornerShape(22.dp)
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(bg)
                .border(if (selected) 2.dp else 1.dp, border, shape)
                .pressable(onClick)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) Palette.Primary else Color.Transparent)
                    .border(2.dp, if (selected) Palette.Primary else Palette.TextTertiary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(product.title, style = MaterialTheme.typography.titleMedium, color = Palette.TextPrimary)
                Text(
                    if (product.trialDays > 0) "${product.trialDays}-day free trial · ${product.pricePerMonth}" else product.pricePerMonth,
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(product.price, style = MaterialTheme.typography.titleLarge, color = Palette.TextPrimary)
                Text("per ${product.period}", style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)
            }
        }
        product.badge?.let { badge ->
            Text(
                badge,
                style = MaterialTheme.typography.labelSmall,
                color = Palette.OnGold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-16).dp, y = (-9).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Palette.GoldGradient)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}
