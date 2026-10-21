package com.novacleaner.app.ui.premium

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novacleaner.app.billing.BillingManager
import com.novacleaner.app.core.AppGraph
import com.novacleaner.app.ui.components.bounceClick
import com.novacleaner.app.ui.components.sparklePath
import com.novacleaner.app.ui.navigation.LocalNavigator
import com.novacleaner.app.ui.navigation.findActivity
import com.novacleaner.app.ui.theme.Gold
import com.novacleaner.app.ui.theme.GoldDeep
import kotlin.math.sin

private val Ink = Color(0xFF07120F)
private val InkSoft = Color(0xFF0F201B)
private val GoldLight = Color(0xFFFFE6A3)

@Composable
fun PremiumScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val premium by AppGraph.premium.collectAsStateWithLifecycle()
    val price by AppGraph.billing.price.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        AppGraph.billing.connect()
        AppGraph.billing.events.collect { event ->
            val message = when (event) {
                BillingManager.Event.Purchased -> "Thank you! Ads are gone for good."
                BillingManager.Event.Restored -> "Purchase restored – enjoy ad-free Nova Cleaner."
                BillingManager.Event.NothingToRestore -> "No previous purchase found on this Google account."
                BillingManager.Event.Pending -> "Payment pending. Ads will be removed once it completes."
                is BillingManager.Event.Error -> event.message
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(InkSoft, Ink, Color.Black))),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(listOf(Gold.copy(alpha = 0.22f), Color.Transparent), Offset(size.width / 2, size.height * 0.2f), size.width * 0.75f),
                size.width * 0.75f, Offset(size.width / 2, size.height * 0.2f),
            )
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFF00C896).copy(alpha = 0.16f), Color.Transparent), Offset(0f, size.height), size.width),
                size.width, Offset(0f, size.height),
            )
        }
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)).bounceClick { navigator.back() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, "Close", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp)) }
            }
            CrownIllustration(Modifier.size(200.dp))
            AnimatedContent(premium, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "premium") { owned ->
                if (owned) OwnedContent { navigator.back() } else OfferContent(price) {
                    context.findActivity()?.let { AppGraph.billing.purchase(it) }
                }
            }
        }
    }
}

@Composable
private fun OfferContent(price: String?, onBuy: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Remove Ads Forever", style = MaterialTheme.typography.headlineLarge, color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "One payment. No subscription. Every feature stays free – just without the ads.",
            style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(26.dp))
        Benefit(Icons.Rounded.Block, "No more ads", "Banners, full-screen and app-open ads are gone")
        Benefit(Icons.Rounded.RocketLaunch, "Faster, distraction-free", "Clean and manage your phone without interruptions")
        Benefit(Icons.Rounded.AllInclusive, "Lifetime access", "Pay once, keep it forever on your Google account")
        Benefit(Icons.Rounded.Favorite, "Support the developer", "Help us keep Nova Cleaner improving")
        Spacer(Modifier.height(22.dp))

        val shape = RoundedCornerShape(24.dp)
        Box(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(shape)
                    .background(Brush.linearGradient(listOf(Gold.copy(alpha = 0.16f), Gold.copy(alpha = 0.05f))))
                    .border(1.5.dp, Brush.linearGradient(listOf(GoldLight, GoldDeep)), shape)
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Lifetime • Remove Ads", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("Pay once, ad-free forever", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
                    }
                    Text(price ?: "—", style = MaterialTheme.typography.headlineMedium, color = GoldLight)
                }
            }
            Text(
                "BEST VALUE",
                style = MaterialTheme.typography.labelSmall,
                color = Ink,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-18).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(GoldLight, Gold)))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(22.dp))
        val btnShape = RoundedCornerShape(30.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(16.dp, btnShape, spotColor = GoldDeep, ambientColor = GoldDeep)
                .clip(btnShape)
                .background(Brush.horizontalGradient(listOf(GoldLight, Gold, GoldDeep)))
                .bounceClick(onClick = onBuy),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (price != null) "Remove Ads – $price" else "Remove Ads",
                style = MaterialTheme.typography.titleMedium, color = Ink,
            )
        }
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = { AppGraph.billing.restore() }) {
            Text("Restore purchase", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
        }
        Text(
            "One-time payment through Google Play. No recurring charges.",
            style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.45f), textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OwnedContent(onDone: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("You're ad-free!", style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Spacer(Modifier.height(10.dp))
        Text(
            "Thanks for supporting Nova Cleaner. Enjoy a clean experience, forever.",
            style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Box(
            Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(29.dp))
                .background(Brush.horizontalGradient(listOf(GoldLight, Gold, GoldDeep))).bounceClick(onClick = onDone),
            contentAlignment = Alignment.Center,
        ) { Text("Continue", style = MaterialTheme.typography.titleMedium, color = Ink) }
    }
}

@Composable
private fun Benefit(icon: ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Gold.copy(alpha = 0.30f), GoldDeep.copy(alpha = 0.15f)))),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = GoldLight, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
        }
        Icon(Icons.Rounded.Check, null, tint = Gold, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun CrownIllustration(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "crown")
    val float by transition.animateFloat(-5f, 5f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "float")
    val twinkle by transition.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "tw")
    val sparkles = remember { listOf(Offset(0.12f, 0.22f) to 0.06f, Offset(0.86f, 0.18f) to 0.05f, Offset(0.9f, 0.7f) to 0.04f, Offset(0.08f, 0.72f) to 0.035f) }
    Canvas(modifier) {
        val w = size.width * 0.62f
        val h = w * 0.78f
        val left = (size.width - w) / 2
        val top = (size.height - h) / 2 + float.dp.toPx()
        drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = 0.35f), Color.Transparent), center, size.minDimension / 2), size.minDimension / 2)
        val crown = Path().apply {
            moveTo(left + w * 0.08f, top + h * 0.78f)
            lineTo(left, top + h * 0.26f)
            lineTo(left + w * 0.3f, top + h * 0.5f)
            lineTo(left + w * 0.5f, top + h * 0.08f)
            lineTo(left + w * 0.7f, top + h * 0.5f)
            lineTo(left + w, top + h * 0.26f)
            lineTo(left + w * 0.92f, top + h * 0.78f)
            close()
        }
        val goldBrush = Brush.verticalGradient(listOf(GoldLight, Gold, GoldDeep), startY = top, endY = top + h)
        drawPath(crown, goldBrush)
        drawPath(crown, Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), startX = left, endX = left + w * 0.5f))
        drawRoundRect(goldBrush, Offset(left + w * 0.06f, top + h * 0.82f), Size(w * 0.88f, h * 0.16f), androidx.compose.ui.geometry.CornerRadius(h * 0.05f))
        listOf(Offset(left, top + h * 0.26f), Offset(left + w * 0.5f, top + h * 0.08f), Offset(left + w, top + h * 0.26f)).forEach {
            drawCircle(GoldLight, w * 0.055f, it)
        }
        drawCircle(Color(0xFF00C896), w * 0.05f, Offset(left + w * 0.5f, top + h * 0.9f))
        drawCircle(Color(0xFFFF5C8A), w * 0.035f, Offset(left + w * 0.28f, top + h * 0.9f))
        drawCircle(Color(0xFFFF5C8A), w * 0.035f, Offset(left + w * 0.72f, top + h * 0.9f))
        sparkles.forEachIndexed { i, (pos, r) ->
            val s = 0.6f + 0.4f * sin(twinkle + i * 1.7f)
            drawPath(sparklePath(size.width * pos.x, size.height * pos.y, size.minDimension * r * s), GoldLight.copy(alpha = 0.5f + 0.5f * s))
        }
    }
}

