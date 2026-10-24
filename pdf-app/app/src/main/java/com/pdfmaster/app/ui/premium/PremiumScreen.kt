package com.pdfmaster.app.ui.premium

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfmaster.app.R
import com.pdfmaster.app.billing.BillingEvent
import com.pdfmaster.app.billing.Plan
import com.pdfmaster.app.billing.PlanId
import com.pdfmaster.app.ui.LocalAppDeps
import com.pdfmaster.app.ui.components.PrimaryButton
import com.pdfmaster.app.ui.components.bounceClick
import com.pdfmaster.app.ui.navigation.LocalNavigator
import com.pdfmaster.app.ui.rememberActivity
import com.pdfmaster.app.ui.settings.PRIVACY_URL
import com.pdfmaster.app.ui.settings.TERMS_URL
import com.pdfmaster.app.ui.theme.AppTheme
import com.pdfmaster.app.ui.theme.Brand
import com.pdfmaster.app.util.FileActions
import com.pdfmaster.app.util.Format
import kotlinx.coroutines.delay

private data class Feature(val icon: ImageVector, val title: String, val body: String)

private val features = listOf(
    Feature(Icons.Rounded.Block, "No ads, ever", "A clean, distraction-free workspace"),
    Feature(Icons.Rounded.TextFields, "OCR text recognition", "Turn scans and photos into editable text"),
    Feature(Icons.Rounded.Lock, "Lock & unlock PDFs", "AES-256 password protection"),
    Feature(Icons.Rounded.AllInclusive, "Unlimited everything", "No daily limits on merge, split or compress"),
    Feature(Icons.Rounded.Draw, "Pro editing", "Watermarks, extreme compression and more"),
)

@Composable
fun PremiumScreen(source: String) {
    val deps = LocalAppDeps.current
    val nav = LocalNavigator.current
    val activity = rememberActivity()
    val context = LocalContext.current
    val plans by deps.billing.plans.collectAsStateWithLifecycle()
    val premium = rememberPremiumState()
    val reward = rememberRewardLauncher()
    var selected by rememberSaveable { mutableStateOf(PlanId.YEARLY) }
    var showClose by remember { mutableStateOf(source != "onboarding") }

    LaunchedEffect(Unit) {
        if (!showClose) {
            delay(1200)
            showClose = true
        }
    }
    LaunchedEffect(Unit) {
        deps.billing.events.collect { if (it == BillingEvent.Success || it == BillingEvent.Restored) { delay(600); nav.back() } }
    }

    val float = rememberInfiniteTransition(label = "hero")
    val dy by float.animateFloat(-6f, 6f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dy")
    val sparkle by float.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "spark")

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Hero
            Box(
                Modifier.fillMaxWidth().height(330.dp).background(Brush.verticalGradient(listOf(Brand.RedDeep, Brand.Red, Brand.RedBright))),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(Color.White.copy(alpha = 0.07f), radius = size.width * 0.55f, center = Offset(size.width * 0.9f, size.height * 0.05f))
                    drawCircle(Color.White.copy(alpha = 0.05f), radius = size.width * 0.4f, center = Offset(size.width * 0.05f, size.height * 0.85f))
                    for (i in 0 until 7) {
                        val t = (sparkle + i / 7f) % 1f
                        val x = size.width * ((i * 37 % 100) / 100f)
                        val y = size.height * (0.15f + 0.55f * ((i * 53 % 100) / 100f))
                        drawCircle(Color.White.copy(alpha = (1f - t) * 0.6f), radius = 2.dp.toPx() + t * 3.dp.toPx(), center = Offset(x, y - t * 20.dp.toPx()))
                    }
                }
                Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painterResource(R.drawable.img_premium_hero), null,
                        Modifier.height(150.dp).graphicsLayer { translationY = dy * density },
                    )
                    Text("PDF Master Premium", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Unlock every tool. Zero ads. No limits.",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            // Sheet
            Column(
                Modifier
                    .offset(y = (-28).dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
            ) {
                Spacer(Modifier.height(22.dp))
                if (premium.purchased) {
                    PurchasedState { nav.back() }
                } else {
                    features.forEachIndexed { i, f -> FeatureRow(f, i) }
                    Spacer(Modifier.height(16.dp))
                    if (premium.passActive) {
                        PassActiveCard(premium, onExtend = { reward.launch() })
                        Spacer(Modifier.height(16.dp))
                    }
                    plans.forEach { plan ->
                        PlanCard(plan, plan.id == selected) { selected = plan.id }
                        Spacer(Modifier.height(10.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    val plan = plans.first { it.id == selected }
                    ShimmerCta(
                        text = when {
                            plan.id == PlanId.LIFETIME -> "Get Lifetime Access"
                            plan.hasTrial -> "Start Free Trial"
                            else -> "Continue"
                        },
                    ) { deps.billing.purchase(activity, selected) }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when {
                            plan.id == PlanId.LIFETIME -> "One-time payment of ${plan.price}. No subscription."
                            plan.hasTrial -> "Free for 3 days, then ${plan.price}${plan.period}. Cancel anytime in Google Play."
                            else -> "${plan.price}${plan.period}, renews automatically. Cancel anytime in Google Play."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(18.dp))
                    RewardCard(active = premium.passActive, remaining = premium.passRemaining) { reward.launch() }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    FooterLink("Restore") { deps.billing.restore() }
                    FooterDot()
                    FooterLink("Terms") { FileActions.openUrl(context, TERMS_URL) }
                    FooterDot()
                    FooterLink("Privacy") { FileActions.openUrl(context, PRIVACY_URL) }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        AnimatedContent(showClose, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut() }, label = "close", modifier = Modifier.statusBarsPadding().padding(8.dp)) { visible ->
            if (visible) {
                IconButton(onClick = { nav.back() }, modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.18f))) {
                    Icon(Icons.Rounded.Close, "Close", tint = Color.White)
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }
    }
}

@Composable
private fun FeatureRow(feature: Feature, index: Int) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(150L + index * 70L)
        appear.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp).graphicsLayer { alpha = appear.value; translationX = (1f - appear.value) * 30.dp.toPx() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(feature.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(feature.title, style = MaterialTheme.typography.titleSmall)
            Text(feature.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.size(22.dp).clip(CircleShape).background(AppTheme.colors.success.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, null, tint = AppTheme.colors.success, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun PlanCard(plan: Plan, selected: Boolean, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "border")
    val width by animateDpAsState(if (selected) 2.dp else 1.dp, label = "w")
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface)
                .border(width, border, RoundedCornerShape(20.dp))
                .bounceClick(scaleDown = 0.985f, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(22.dp).clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(plan.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(plan.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(plan.price, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(plan.period, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        plan.badge?.let {
            Text(
                it,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-16).dp, y = (-9).dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brand.GoldGradient)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun ShimmerCta(text: String, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "cta")
    val x by transition.animateFloat(-0.4f, 1.4f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "x")
    Box(
        Modifier.drawWithContent {
            drawContent()
            val w = size.width
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.28f), Color.Transparent),
                    start = Offset(w * x - 80f, 0f), end = Offset(w * x + 80f, size.height),
                ),
            )
        },
    ) {
        PrimaryButton(text, onClick, height = 60.dp)
    }
}

@Composable
private fun RewardCard(active: Boolean, remaining: Long, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppTheme.colors.goldSoft)
            .border(1.5.dp, Brand.Gold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .bounceClick(scaleDown = 0.985f, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.img_reward_unlock), null, Modifier.size(64.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (active) "Add 5 more hours" else "Not ready to subscribe?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                if (active) "Pass ends in ${Format.countdown(remaining)}" else "Watch a short video and get Premium free for 5 hours.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = if (active) FontFamily.Monospace else FontFamily.SansSerif,
            )
        }
        Icon(Icons.Rounded.PlayCircle, null, tint = Brand.GoldDeep, modifier = Modifier.size(34.dp))
    }
}

@Composable
private fun PurchasedState(onDone: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.img_success_done), null, Modifier.size(150.dp))
        Text("You're Premium!", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            "Every tool is unlocked and ads are gone. Thanks for supporting PDF Master.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Continue", onDone)
    }
}

@Composable
private fun FooterLink(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun FooterDot() {
    Box(Modifier.size(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline))
}
