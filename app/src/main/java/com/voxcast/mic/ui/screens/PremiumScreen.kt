package com.voxcast.mic.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Restore
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voxcast.mic.data.PremiumManager
import com.voxcast.mic.ui.components.GhostButton
import com.voxcast.mic.ui.components.GradientButton
import com.voxcast.mic.ui.components.IconBadge
import com.voxcast.mic.ui.components.Messenger
import com.voxcast.mic.ui.components.RoundIconButton
import com.voxcast.mic.ui.components.VoxBackground
import com.voxcast.mic.ui.components.VoxDialog
import com.voxcast.mic.ui.components.glass
import com.voxcast.mic.ui.components.gradientTint
import com.voxcast.mic.ui.icons.VoxIcons
import com.voxcast.mic.ui.theme.VoxColors
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Perk(val icon: ImageVector, val title: String, val body: String, val brush: Brush)

@Composable
fun PremiumScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val app = context.voxApp
    val settings by app.settings.settings.collectAsStateWithLifecycle()
    val premium = settings.premium
    val scope = rememberCoroutineScope()
    var processing by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }

    val perks = listOf(
        Perk(VoxIcons.NoAds, "Zero ads", "No banner, interstitial or native ads anywhere", Brush.linearGradient(listOf(VoxColors.Error, Color(0xFFB8324A)))),
        Perk(VoxIcons.Studio, "Studio 48 kHz", "Broadcast-grade full-band audio quality", Brush.linearGradient(listOf(VoxColors.Cyan, VoxColors.CyanDeep))),
        Perk(VoxIcons.Boost, "250% speaker boost", "Louder playback for halls and outdoors", Brush.linearGradient(listOf(VoxColors.Violet, Color(0xFF4B3FD1)))),
        Perk(VoxIcons.Crown, "PRO badge", "Support development of new features", VoxColors.GoldGradient),
    )

    VoxBackground(accent = VoxColors.Gold, secondary = VoxColors.Violet) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.padding(top = 36.dp)) { RoundIconButton(Icons.Rounded.Close, "Close", onClose) }
                }
                CrownHero()
                Spacer(Modifier.height(10.dp))
                Text(
                    "VoxCast Premium",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    modifier = Modifier.gradientTint(VoxColors.GoldGradient),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (premium.isEntitled) "You're a Premium member. Thank you!" else "The cleanest, loudest, ad-free wireless mic.",
                    style = MaterialTheme.typography.bodyLarge, color = VoxColors.TextDim, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))

                perks.forEachIndexed { i, perk -> PerkRow(perk, i) ; Spacer(Modifier.height(10.dp)) }

                Spacer(Modifier.height(12.dp))
                PlanCard(active = premium.isEntitled)
                Spacer(Modifier.height(10.dp))
                if (premium.isEntitled) {
                    val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(premium.renewsAt))
                    Text(
                        if (premium.autoRenew) "Renews on $date" else "Cancelled · Premium active until $date",
                        style = MaterialTheme.typography.bodyMedium, color = if (premium.autoRenew) VoxColors.Success else VoxColors.Warning,
                    )
                }
                Spacer(Modifier.height(18.dp))
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, VoxColors.Background)))
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!premium.isEntitled) {
                    GradientButton(
                        text = "Subscribe · ${PremiumManager.PRICE} / ${PremiumManager.PERIOD_LABEL}",
                        onClick = {
                            processing = true
                            scope.launch {
                                val result = app.premium.subscribe()
                                processing = false
                                if (result is PremiumManager.Result.Success) success = true
                            }
                        },
                        brush = VoxColors.GoldGradient,
                        contentColor = Color(0xFF2A1E05),
                        icon = VoxIcons.Crown,
                        loading = processing,
                        height = 60.dp,
                    )
                    Spacer(Modifier.height(10.dp))
                    GhostButton(
                        if (restoring) "Restoring…" else "Restore purchase",
                        onClick = {
                            if (restoring) return@GhostButton
                            restoring = true
                            scope.launch {
                                when (val r = app.premium.restore()) {
                                    PremiumManager.Result.Success -> Messenger.success("Premium restored")
                                    is PremiumManager.Result.Failure -> Messenger.info("Nothing to restore", r.message)
                                }
                                restoring = false
                            }
                        },
                        icon = Icons.Rounded.Restore,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else if (premium.autoRenew) {
                    GhostButton("Cancel subscription", onClick = { confirmCancel = true }, modifier = Modifier.fillMaxWidth(), color = VoxColors.TextDim)
                } else {
                    GradientButton(
                        "Resume subscription",
                        onClick = { processing = true; scope.launch { app.premium.subscribe(); processing = false; success = true } },
                        brush = VoxColors.GoldGradient, contentColor = Color(0xFF2A1E05), loading = processing,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Test mode: purchases are simulated, no real payment is taken. Cancel anytime.",
                    style = MaterialTheme.typography.bodySmall, color = VoxColors.TextFaint, textAlign = TextAlign.Center,
                )
            }
        }
    }

    VoxDialog(
        visible = processing,
        onDismiss = {},
        dismissible = false,
        icon = VoxIcons.Crown,
        accent = VoxColors.Gold,
        iconBrush = VoxColors.GoldGradient,
        title = "Confirming purchase",
        message = "Contacting the store securely…",
        confirmText = null,
    ) {
        androidx.compose.material3.LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)),
            color = VoxColors.Gold,
            trackColor = Color.White.copy(alpha = 0.08f),
        )
    }

    VoxDialog(
        visible = success,
        onDismiss = { success = false; onClose() },
        icon = Icons.Rounded.CheckCircle,
        accent = VoxColors.Gold,
        iconBrush = VoxColors.GoldGradient,
        title = "Welcome to Premium!",
        message = "Ads are gone, Studio 48 kHz and 250% boost are unlocked. Enjoy VoxCast.",
        confirmText = "Let's go",
        confirmBrush = VoxColors.GoldGradient,
        onConfirm = { success = false; onClose() },
    ) {
        Confetti()
    }

    VoxDialog(
        visible = confirmCancel,
        onDismiss = { confirmCancel = false },
        icon = VoxIcons.Crown,
        accent = VoxColors.Warning,
        title = "Cancel Premium?",
        message = "You keep Premium until the end of the current month. Ads return after that.",
        confirmText = "Cancel subscription",
        confirmBrush = VoxColors.LiveGradient,
        onConfirm = {
            confirmCancel = false
            scope.launch { app.premium.cancel(); Messenger.info("Subscription cancelled", "Premium stays active until the period ends") }
        },
        dismissText = "Keep Premium",
    )
}

@Composable
private fun CrownHero() {
    val t = rememberInfiniteTransition(label = "crown")
    val float by t.animateFloat(-6f, 6f, infiniteRepeatable(tween(2_000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")
    val spin by t.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(9_000, easing = LinearEasing)), label = "spin")
    Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(VoxColors.Gold.copy(alpha = 0.45f), Color.Transparent)), radius = size.minDimension / 2)
            // Orbiting sparkles.
            for (i in 0 until 7) {
                val a = spin + i * (2 * Math.PI / 7).toFloat()
                val r = size.minDimension * (0.36f + 0.06f * sin(a * 2))
                val p = Offset(center.x + cos(a) * r, center.y + sin(a) * r)
                drawCircle(Color.White.copy(alpha = 0.7f), radius = (2.2f + (i % 3)) * density, center = p)
            }
        }
        IconBadge(
            VoxIcons.Crown, VoxColors.GoldGradient, size = 104.dp, glow = VoxColors.Gold, iconTint = Color(0xFF2A1E05),
            modifier = Modifier.graphicsLayer { translationY = float * density },
        )
    }
}

@Composable
private fun PerkRow(perk: Perk, index: Int) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(500, delayMillis = 120 + index * 90, easing = FastOutSlowInEasing)) }
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = anim.value; translationX = (1f - anim.value) * 60f }
            .glass(RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(perk.icon, perk.brush, size = 46.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(perk.title, style = MaterialTheme.typography.titleMedium, color = VoxColors.Text)
            Text(perk.body, style = MaterialTheme.typography.bodySmall, color = VoxColors.TextDim)
        }
        Icon(Icons.Rounded.Check, null, tint = VoxColors.Gold)
    }
}

@Composable
private fun PlanCard(active: Boolean) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF2E2511), Color(0xFF171309))))
            .border(2.dp, VoxColors.GoldGradient, shape)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(VoxColors.GoldGradient),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Check, null, tint = Color(0xFF2A1E05), modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Monthly", style = MaterialTheme.typography.titleLarge, color = VoxColors.Text)
            Text(if (active) "Your current plan" else "Billed monthly · cancel anytime", style = MaterialTheme.typography.bodySmall, color = VoxColors.TextDim)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(PremiumManager.PRICE, style = MaterialTheme.typography.headlineMedium, color = VoxColors.Gold, fontWeight = FontWeight.ExtraBold)
            Text("per ${PremiumManager.PERIOD_LABEL}", style = MaterialTheme.typography.bodySmall, color = VoxColors.TextDim)
        }
    }
}

/** Celebration burst for the success dialog. */
@Composable
private fun Confetti() {
    val progress = remember { Animatable(0f) }
    val pieces = remember {
        List(36) {
            Triple(Random.nextFloat() * 2 * Math.PI.toFloat(), 0.4f + Random.nextFloat() * 0.6f, listOf(VoxColors.Gold, VoxColors.Cyan, VoxColors.Violet, VoxColors.Success, Color.White).random())
        }
    }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1_400, easing = FastOutSlowInEasing)) }
    Canvas(Modifier.fillMaxWidth().height(70.dp)) {
        val p = progress.value
        pieces.forEach { (angle, speed, color) ->
            val dist = size.width * 0.45f * speed * p
            val pos = Offset(center.x + cos(angle) * dist, center.y + sin(angle) * dist * 0.6f + p * p * 30f * density)
            drawCircle(color.copy(alpha = 1f - p * 0.8f), radius = 3.2f * density * (1f - p * 0.4f), center = pos)
        }
    }
}
