package com.zyropay.app.ui.screens.pay

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zyropay.app.R
import com.zyropay.app.data.AppRepository
import com.zyropay.app.data.CardStatus
import com.zyropay.app.data.Fmt
import com.zyropay.app.data.MockData
import com.zyropay.app.navigation.Routes
import com.zyropay.app.navigation.back
import com.zyropay.app.ui.components.CardThumb
import com.zyropay.app.ui.components.CoinIcon
import com.zyropay.app.ui.components.IconButtonZ
import com.zyropay.app.ui.components.LoadingDialog
import com.zyropay.app.ui.components.LocalMessenger
import com.zyropay.app.ui.components.NumericKeypad
import com.zyropay.app.ui.components.SheetOption
import com.zyropay.app.ui.components.ZBottomSheet
import com.zyropay.app.ui.components.ZButton
import com.zyropay.app.ui.components.ZIcon
import com.zyropay.app.ui.components.ZImage
import com.zyropay.app.ui.components.ZScreen
import com.zyropay.app.ui.components.appendAmount
import com.zyropay.app.ui.components.pressable
import com.zyropay.app.ui.components.rememberAuthGate
import com.zyropay.app.ui.theme.Z
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun ScanScreen(nav: NavController) {
    val messenger = LocalMessenger.current
    var torch by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(true) }
    val t = rememberInfiniteTransition(label = "scan")
    val line by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse), label = "line")
    val pulse by t.animateFloat(0.96f, 1.02f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse")
    LaunchedEffect(Unit) {
        delay(3200)
        if (scanning) {
            scanning = false
            messenger.success("QR code recognised", "Zyro Pay merchant detected")
            nav.navigate(Routes.payMerchant(Random.nextInt(MockData.merchants.size))) { popUpTo(Routes.SCAN) { inclusive = true } }
        }
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF07080C))) {
        // simulated camera feed
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.radialGradient(listOf(Color(0xFF2A2D3A), Color(0xFF0B0C12)), center = Offset(size.width / 2, size.height * 0.42f), radius = size.width))
            if (torch) drawRect(Brush.radialGradient(listOf(Color(0x55FFF4D0), Color.Transparent), center = Offset(size.width / 2, size.height * 0.42f), radius = size.width * 0.8f))
        }
        // dim overlay with transparent window
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            val side = size.width * 0.68f
            val left = (size.width - side) / 2
            val top = size.height * 0.42f - side / 2
            drawRect(Color.Black.copy(alpha = 0.55f))
            drawRoundRect(Color.Transparent, Offset(left, top), Size(side, side), CornerRadius(28.dp.toPx()), blendMode = BlendMode.Clear)
            val s = side * pulse
            val l = (size.width - s) / 2
            val tp = size.height * 0.42f - s / 2
            val len = 34.dp.toPx()
            val stroke = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
            val col = Color(0xFF8F5BFF)
            // corners
            drawLine(col, Offset(l, tp + len), Offset(l, tp + 14), stroke.width, StrokeCap.Round)
            drawArc(col, 180f, 90f, false, Offset(l, tp), Size(28f, 28f), style = stroke)
            drawLine(col, Offset(l + 14, tp), Offset(l + len, tp), stroke.width, StrokeCap.Round)
            drawLine(col, Offset(l + s - len, tp), Offset(l + s - 14, tp), stroke.width, StrokeCap.Round)
            drawArc(col, 270f, 90f, false, Offset(l + s - 28, tp), Size(28f, 28f), style = stroke)
            drawLine(col, Offset(l + s, tp + 14), Offset(l + s, tp + len), stroke.width, StrokeCap.Round)
            drawLine(col, Offset(l, tp + s - len), Offset(l, tp + s - 14), stroke.width, StrokeCap.Round)
            drawArc(col, 90f, 90f, false, Offset(l, tp + s - 28), Size(28f, 28f), style = stroke)
            drawLine(col, Offset(l + 14, tp + s), Offset(l + len, tp + s), stroke.width, StrokeCap.Round)
            drawLine(col, Offset(l + s - len, tp + s), Offset(l + s - 14, tp + s), stroke.width, StrokeCap.Round)
            drawArc(col, 0f, 90f, false, Offset(l + s - 28, tp + s - 28), Size(28f, 28f), style = stroke)
            drawLine(col, Offset(l + s, tp + s - 14), Offset(l + s, tp + s - len), stroke.width, StrokeCap.Round)
            // scan line
            val y = top + 16 + (side - 32) * line
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0x6600D1FF)), startY = y - 60, endY = y), Offset(left + 12, y - 60), Size(side - 24, 60f))
            drawLine(Color(0xFF00D1FF), Offset(left + 16, y), Offset(left + side - 16, y), 2.dp.toPx(), StrokeCap.Round)
        }
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButtonZ(R.drawable.ic_close, onClick = { scanning = false; nav.back() }, tint = Color.White, background = Color.White.copy(alpha = 0.12f))
            Text("Scan to pay", style = Z.type.title, color = Color.White, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            IconButtonZ(R.drawable.ic_gallery, onClick = {
                scanning = false
                messenger.info("Reading image…", "Detecting QR code from gallery")
                nav.navigate(Routes.payMerchant(Random.nextInt(MockData.merchants.size))) { popUpTo(Routes.SCAN) { inclusive = true } }
            }, tint = Color.White, background = Color.White.copy(alpha = 0.12f))
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Align the QR code within the frame", style = Z.type.bodySmall, color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.height(4.dp))
            Text("Supports Zyro Pay, crypto addresses & merchant QR", style = Z.type.caption, color = Color.White.copy(alpha = 0.5f))
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                ScanAction(if (torch) "Light off" else "Flashlight", R.drawable.ic_sparkle, torch) { torch = !torch }
                ScanAction("My QR", R.drawable.ic_qr_code, false) { scanning = false; nav.navigate(Routes.RECEIVE) { popUpTo(Routes.SCAN) { inclusive = true } } }
                ScanAction("Pay merchant", R.drawable.ic_merchant, false) {
                    scanning = false
                    nav.navigate(Routes.payMerchant(0)) { popUpTo(Routes.SCAN) { inclusive = true } }
                }
            }
        }
    }
}

@Composable
private fun ScanAction(label: String, icon: Int, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.pressable(scaleDown = 0.9f, ripple = false, onClick = onClick)) {
        Box(Modifier.size(54.dp).clip(CircleShape).background(if (active) Color.White else Color.White.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            ZIcon(icon, size = 24.dp, tint = if (active) Color(0xFF0B0F19) else Color.White)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = Z.type.caption, color = Color.White)
    }
}

@Composable
fun PayMerchantScreen(nav: NavController, index: Int) {
    val c = Z.colors
    val merchant = MockData.merchants.getOrElse(index) { MockData.merchants.first() }
    val cards by AppRepository.cards.collectAsState()
    val balances by AppRepository.balances.collectAsState()
    val active = cards.filter { it.status == CardStatus.ACTIVE && !it.frozen }
    val messenger = LocalMessenger.current
    val gate = rememberAuthGate()
    val scope = rememberCoroutineScope()
    var amount by rememberSaveable { mutableStateOf("") }
    // source: card id or "coin:USDT"
    var source by rememberSaveable { mutableStateOf(active.firstOrNull()?.id ?: "coin:USDT") }
    var pick by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    val a = amount.toDoubleOrNull() ?: 0.0
    val card = active.firstOrNull { it.id == source }
    val coin = if (source.startsWith("coin:")) source.removePrefix("coin:") else null
    val available = card?.balance ?: (balances[coin ?: "USDT"] ?: 0.0) * AppRepository.priceOf(coin ?: "USDT")
    val error = if (a > available) "Insufficient balance" else null

    ZScreen("Pay", onBack = { nav.back() }, scrollable = false) {
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))
            ZImage(merchant.icon, Modifier.size(64.dp).clip(CircleShape))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(merchant.name, style = Z.type.h3, color = c.textPrimary)
                Spacer(Modifier.width(4.dp))
                ZIcon(R.drawable.ic_shield_check, size = 16.dp, tint = c.primary)
            }
            Text("${merchant.city} · Verified merchant", style = Z.type.caption, color = c.textSecondary)
            Spacer(Modifier.weight(1f))
            Text(
                "$" + amount.ifEmpty { "0" },
                style = Z.type.amount.copy(fontSize = if (amount.length > 7) 36.sp else 48.sp),
                color = if (error != null) c.error else if (amount.isEmpty()) c.textTertiary else c.textPrimary,
            )
            Text(error ?: "2% cashback · ≈ ${Fmt.fiat(a * 0.02)} back", style = Z.type.bodySmall, color = if (error != null) c.error else c.success)
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).border(1.dp, c.outline.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .clickable { pick = true }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (card != null) CardThumb(card, Modifier.size(width = 40.dp, height = 26.dp)) else CoinIcon(coin ?: "USDT", size = 32.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (card != null) "${card.title} ·· ${card.last4}" else "${coin} wallet", style = Z.type.bodyMedium, color = c.textPrimary)
                    Text("Available ${Fmt.fiat(available)}", style = Z.type.caption, color = c.textSecondary)
                }
                Text("Change", style = Z.type.label, color = c.primary)
            }
            Spacer(Modifier.height(8.dp))
        }
        NumericKeypad(onDigit = { amount = appendAmount(amount, it, 2) }, onDelete = { amount = amount.dropLast(1) }, decimal = true, modifier = Modifier.padding(horizontal = 16.dp), keyHeight = 54.dp)
        ZButton(
            if (a > 0) "Pay ${Fmt.fiat(a)}" else "Pay", onClick = {
                gate.request("Confirm payment") {
                    processing = true
                    scope.launch {
                        delay(1500)
                        val id = AppRepository.payMerchant(merchant, a, card?.id, coin ?: "USDT")
                        processing = false
                        nav.navigate(Routes.result("pay", id)) { popUpTo(Routes.MAIN) }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), enabled = a > 0 && error == null,
        )
    }
    if (pick) {
        ZBottomSheet({ pick = false }, title = "Payment method") { close ->
            active.forEach { cd ->
                SheetOption("${cd.title} ·· ${cd.last4}", subtitle = "Balance ${Fmt.fiat(cd.balance)}", selected = source == cd.id, leading = { CardThumb(cd) }) { close { source = cd.id } }
            }
            listOf("USDT", "USDC").forEach { s ->
                SheetOption("$s wallet", subtitle = "Balance ${Fmt.coin(balances[s] ?: 0.0, s)}", selected = source == "coin:$s", leading = { CoinIcon(s, size = 36.dp) }) { close { source = "coin:$s" } }
            }
            if (active.isEmpty()) {
                Text("No active card. Pay directly from your wallet or apply for a card.", style = Z.type.caption, color = c.textSecondary, modifier = Modifier.padding(20.dp))
            }
        }
    }
    if (processing) LoadingDialog("Processing payment…")
    LaunchedEffect(Unit) { if (cards.any { it.frozen } && active.isEmpty()) messenger.warning("Card frozen", "Unfreeze your card or pay from your wallet.") }
}
