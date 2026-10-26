package com.zyropay.app.ui.screens.wallet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zyropay.app.R
import com.zyropay.app.data.AppRepository
import com.zyropay.app.data.Coin
import com.zyropay.app.data.Fmt
import com.zyropay.app.data.KycStatus
import com.zyropay.app.data.MockData
import com.zyropay.app.data.Network
import com.zyropay.app.navigation.Routes
import com.zyropay.app.navigation.back
import com.zyropay.app.ui.components.ButtonSize
import com.zyropay.app.ui.components.ButtonStyle
import com.zyropay.app.ui.components.CoinIcon
import com.zyropay.app.ui.components.DetailRow
import com.zyropay.app.ui.components.DividerLine
import com.zyropay.app.ui.components.IconButtonZ
import com.zyropay.app.ui.components.IconCircle
import com.zyropay.app.ui.components.InfoBanner
import com.zyropay.app.ui.components.LoadingDialog
import com.zyropay.app.ui.components.LocalMessenger
import com.zyropay.app.ui.components.PriceChart
import com.zyropay.app.ui.components.QrCode
import com.zyropay.app.ui.components.SearchField
import com.zyropay.app.ui.components.SegmentedTabs
import com.zyropay.app.ui.components.SheetOption
import com.zyropay.app.ui.components.SurfaceCard
import com.zyropay.app.ui.components.TxRow
import com.zyropay.app.ui.components.WarningBanner
import com.zyropay.app.ui.components.ZBottomSheet
import com.zyropay.app.ui.components.ZButton
import com.zyropay.app.ui.components.ZDialog
import com.zyropay.app.ui.components.ZIcon
import com.zyropay.app.ui.components.ZImage
import com.zyropay.app.ui.components.ZScreen
import com.zyropay.app.ui.components.ZTextField
import com.zyropay.app.ui.components.ZTopBar
import com.zyropay.app.ui.components.ChipButton
import com.zyropay.app.ui.components.maskIf
import com.zyropay.app.ui.components.pressable
import com.zyropay.app.ui.components.rememberAuthGate
import com.zyropay.app.ui.components.rememberCopier
import com.zyropay.app.ui.components.rememberPaster
import com.zyropay.app.ui.components.sanitizeAmount
import com.zyropay.app.ui.components.shareText
import com.zyropay.app.ui.components.toAmount
import com.zyropay.app.ui.theme.Z
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ------------------------------------------------------------------ select coin

@Composable
fun SelectCoinScreen(nav: NavController, purpose: String) {
    val balances by AppRepository.balances.collectAsState()
    val prices by AppRepository.prices.collectAsState()
    val prefs by AppRepository.prefs.collectAsState()
    var query by remember { mutableStateOf("") }
    val title = when (purpose) {
        "deposit" -> "Select coin to deposit"
        "withdraw" -> "Select coin to withdraw"
        "send" -> "Select coin to send"
        else -> "Select coin"
    }
    val coins = MockData.coins.filter { query.isBlank() || it.symbol.contains(query, true) || it.name.contains(query, true) }
        .sortedByDescending { if (purpose == "deposit") 0.0 else (balances[it.symbol] ?: 0.0) * (prices[it.symbol]?.usd ?: 0.0) }
    ZScreen(title, onBack = { nav.back() }, scrollable = false) {
        SearchField(query, { query = it }, Modifier.padding(horizontal = 20.dp), placeholder = "Search coin name")
        Spacer(Modifier.height(8.dp))
        if (purpose == "deposit") {
            Text("Popular", style = Z.type.label, color = Z.colors.textSecondary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("USDT", "USDC", "BTC", "ETH").forEach { s ->
                    ChipButton(s, false, icon = null) { nav.navigate(Routes.deposit(s)) { popUpTo(Routes.SELECT_COIN) { inclusive = true } } }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(coins, key = { it.symbol }) { coin ->
                val bal = balances[coin.symbol] ?: 0.0
                Row(
                    Modifier.fillMaxWidth().clickable {
                        val route = when (purpose) {
                            "deposit" -> Routes.deposit(coin.symbol)
                            "withdraw" -> Routes.withdraw(coin.symbol)
                            "send" -> Routes.send(coin.symbol)
                            else -> Routes.coin(coin.symbol)
                        }
                        nav.navigate(route) { popUpTo(Routes.SELECT_COIN) { inclusive = true } }
                    }.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoinIcon(coin.symbol, size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(coin.symbol, style = Z.type.bodyMedium, color = Z.colors.textPrimary)
                        Text(coin.name, style = Z.type.caption, color = Z.colors.textSecondary)
                    }
                    if (purpose != "deposit") {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(maskIf(prefs.hideBalance, Fmt.coin(bal, coin.symbol, withSymbol = false)), style = Z.type.number, color = Z.colors.textPrimary)
                            Text(maskIf(prefs.hideBalance, "≈ " + Fmt.fiat(bal * (prices[coin.symbol]?.usd ?: 0.0), prefs.currency)), style = Z.type.caption, color = Z.colors.textSecondary)
                        }
                    } else {
                        Text("${coin.networks.size} network${if (coin.networks.size > 1) "s" else ""}", style = Z.type.caption, color = Z.colors.textTertiary)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ coin detail

@Composable
fun CoinDetailScreen(nav: NavController, symbol: String) {
    val c = Z.colors
    val coin = MockData.coin(symbol)
    val prices by AppRepository.prices.collectAsState()
    val balances by AppRepository.balances.collectAsState()
    val prefs by AppRepository.prefs.collectAsState()
    val txs by AppRepository.transactions.collectAsState()
    val price = prices[symbol]
    var range by rememberSaveable { mutableIntStateOf(1) }
    var scrub by remember { mutableStateOf<Int?>(null) }
    val series = remember(price?.history, range) {
        val h = price?.history ?: emptyList()
        when (range) {
            0 -> h.takeLast(16)
            1 -> h
            else -> {
                // synthesize longer ranges from the live series
                val factor = if (range == 2) 1.0 else 2.4
                val r = kotlin.random.Random(symbol.hashCode() + range)
                var v = (h.firstOrNull() ?: coin.basePrice.toFloat()) * (1 - 0.06f * factor.toFloat())
                List(60) { v *= (1 + ((r.nextDouble() - 0.46) * 0.03 * factor).toFloat()); v } + h.takeLast(10)
            }
        }
    }
    val shown = scrub?.let { series.getOrNull(it)?.toDouble() } ?: (price?.usd ?: coin.basePrice)
    val bal = balances[symbol] ?: 0.0
    val up = (price?.change24h ?: 0.0) >= 0
    ZScreen(coin.name, onBack = { nav.back() }, actions = {
        IconButtonZ(R.drawable.ic_star, onClick = { }, tint = c.textSecondary)
    }, bottomBar = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZButton("Deposit", onClick = { nav.navigate(Routes.deposit(symbol)) }, modifier = Modifier.weight(1f), style = ButtonStyle.Secondary)
            ZButton("Withdraw", onClick = { nav.navigate(Routes.withdraw(symbol)) }, modifier = Modifier.weight(1f), style = ButtonStyle.Secondary)
            ZButton("Swap", onClick = { nav.navigate(Routes.swap(symbol)) }, modifier = Modifier.weight(1f))
        }
    }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoinIcon(symbol, size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Text("${coin.symbol}/USD", style = Z.type.bodyMedium, color = c.textSecondary)
        }
        Spacer(Modifier.height(12.dp))
        Text(Fmt.price(shown), style = Z.type.h1.copy(fontFeatureSettings = "tnum"), color = c.textPrimary)
        Text(
            if (scrub != null) "Drag to explore" else "${Fmt.percent(price?.change24h ?: 0.0)} · 24h",
            style = Z.type.bodySmallMedium, color = if (scrub != null) c.textTertiary else if (up) c.success else c.error,
        )
        Spacer(Modifier.height(16.dp))
        PriceChart(series, if (up) c.success else c.error, Modifier.fillMaxWidth().height(200.dp)) { scrub = it }
        Spacer(Modifier.height(12.dp))
        SegmentedTabs(listOf("1H", "24H", "1W", "1M"), range) { range = it }
        Spacer(Modifier.height(20.dp))
        SurfaceCard(Modifier.fillMaxWidth()) {
            Text("My balance", style = Z.type.caption, color = c.textSecondary)
            Spacer(Modifier.height(4.dp))
            Text(maskIf(prefs.hideBalance, Fmt.coin(bal, symbol)), style = Z.type.h2, color = c.textPrimary)
            Text(maskIf(prefs.hideBalance, "≈ " + Fmt.fiat(bal * (price?.usd ?: 0.0), prefs.currency)), style = Z.type.bodySmall, color = c.textSecondary)
        }
        Spacer(Modifier.height(16.dp))
        SurfaceCard(Modifier.fillMaxWidth(), padding = 16.dp) {
            Text("Market stats", style = Z.type.title, color = c.textPrimary)
            Spacer(Modifier.height(6.dp))
            val p = price?.usd ?: coin.basePrice
            DetailRow("24h high", Fmt.price(p * 1.021))
            DetailRow("24h low", Fmt.price(p * 0.976))
            DetailRow("Market cap", "$" + Fmt.compact(p * when (symbol) { "BTC" -> 19_700_000.0; "ETH" -> 120_000_000.0; "USDT" -> 118_000_000_000.0; "USDC" -> 34_000_000_000.0; else -> 450_000_000.0 }))
            DetailRow("24h volume", "$" + Fmt.compact(p * 1_250_000.0))
            DetailRow("Supported networks", coin.networks.joinToString { it.short })
        }
        Spacer(Modifier.height(16.dp))
        Text("Transactions", style = Z.type.title, color = c.textPrimary)
        Spacer(Modifier.height(6.dp))
        val list = txs.filter { it.coin == symbol || it.toCoin == symbol }.take(8)
        if (list.isEmpty()) {
            Text("No $symbol transactions yet", style = Z.type.bodySmall, color = c.textTertiary, modifier = Modifier.padding(vertical = 16.dp))
        } else {
            SurfaceCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                list.forEach { t -> TxRow(t, prefs.hideBalance, prefs.currency) { nav.navigate(Routes.tx(t.id)) } }
            }
        }
    }
}

// ------------------------------------------------------------------ network picker

@Composable
fun NetworkSheet(coin: Coin, selected: Network?, deposit: Boolean, onDismiss: () -> Unit, onPick: (Network) -> Unit) {
    ZBottomSheet(onDismiss, title = "Select network") { close ->
        InfoBanner(
            "Make sure the network matches the one used by the ${if (deposit) "sending" else "receiving"} platform, or assets may be lost.",
            Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))
        coin.networks.forEach { n ->
            SheetOption(
                n.name,
                subtitle = if (deposit) "Min. deposit ${Fmt.plain(n.minDeposit, coin.decimals)} ${coin.symbol} · ${n.confirmations} confirmations · ${n.eta}" else "Fee ${Fmt.plain(n.fee, coin.decimals)} ${coin.symbol} · Arrival ${n.eta}",
                selected = n.id == selected?.id,
                leading = { ZImage(n.icon, Modifier.size(32.dp).clip(CircleShape)) },
            ) { close { onPick(n) } }
        }
    }
}

@Composable
private fun NetworkField(network: Network?, onClick: () -> Unit) {
    ZTextField(
        value = network?.name ?: "", onValueChange = {}, label = "Network", placeholder = "Select network", readOnly = true,
        leading = if (network != null) {
            { ZImage(network.icon, Modifier.size(22.dp).clip(CircleShape)) }
        } else null,
        trailing = { ZIcon(R.drawable.ic_chevron_down, size = 18.dp, tint = Z.colors.textTertiary) },
        onClick = onClick,
    )
}

// ------------------------------------------------------------------ deposit

@Composable
fun DepositScreen(nav: NavController, symbol: String) {
    val c = Z.colors
    val coin = MockData.coin(symbol)
    val context = LocalContext.current
    val copy = rememberCopier()
    val messenger = LocalMessenger.current
    var network by remember { mutableStateOf<Network?>(null) }
    var pick by remember { mutableStateOf(true) }
    var simulate by remember { mutableStateOf(false) }
    ZScreen("Deposit ${coin.symbol}", onBack = { nav.back() }, actions = {
        IconButtonZ(R.drawable.ic_history, onClick = { nav.navigate(Routes.transactions("deposit")) })
    }, bottomBar = if (network != null) {
        {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZButton("Share", onClick = { context.shareText("My ${coin.symbol} (${network!!.short}) deposit address on Zyro Pay:\n${AppRepository.depositAddress(symbol, network!!.id)}") }, modifier = Modifier.weight(1f), style = ButtonStyle.Secondary, icon = R.drawable.ic_share)
                ZButton("Copy address", onClick = { copy("Address", AppRepository.depositAddress(symbol, network!!.id)) }, modifier = Modifier.weight(1f), icon = R.drawable.ic_copy)
            }
        }
    } else null) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surfaceVariant).clickable { nav.navigate(Routes.selectCoin("deposit")) { popUpTo(Routes.DEPOSIT) { inclusive = true } } }.padding(14.dp)) {
            CoinIcon(symbol, size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Text("${coin.symbol}  ", style = Z.type.bodyMedium, color = c.textPrimary)
            Text(coin.name, style = Z.type.bodySmall, color = c.textSecondary, modifier = Modifier.weight(1f))
            ZIcon(R.drawable.ic_chevron_down, size = 18.dp, tint = c.textTertiary)
        }
        Spacer(Modifier.height(14.dp))
        NetworkField(network) { pick = true }
        Spacer(Modifier.height(16.dp))
        AnimatedContent(network, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "depositNet") { net ->
            if (net == null) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    ZImage(R.drawable.ill_deposit_coins, Modifier.size(180.dp))
                    Text("Select a network to generate your deposit address", style = Z.type.bodySmall, color = c.textSecondary, textAlign = TextAlign.Center)
                }
            } else {
                val address = AppRepository.depositAddress(symbol, net.id)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.size(232.dp).clip(RoundedCornerShape(20.dp)).background(Color.White).border(1.dp, c.outline, RoundedCornerShape(20.dp)).padding(16.dp)) {
                        QrCode(address, Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.height(16.dp))
                    SurfaceCard(Modifier.fillMaxWidth()) {
                        Text("${coin.symbol} deposit address (${net.short})", style = Z.type.caption, color = c.textSecondary)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(address, style = Z.type.bodySmallMedium.copy(fontFeatureSettings = "tnum"), color = c.textPrimary, modifier = Modifier.weight(1f))
                            IconButtonZ(R.drawable.ic_copy, onClick = { copy("Address", address) }, tint = c.primary)
                        }
                        if (net.id == "TON") {
                            DividerLine(Modifier.padding(vertical = 8.dp))
                            DetailRow("Memo / Comment", AppRepository.user.value.uid, copyable = true, onCopy = { copy("Memo", AppRepository.user.value.uid) })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    SurfaceCard(Modifier.fillMaxWidth()) {
                        DetailRow("Minimum deposit", "${Fmt.plain(net.minDeposit, coin.decimals)} ${coin.symbol}")
                        DetailRow("Credited after", "${net.confirmations} network confirmations")
                        DetailRow("Expected arrival", net.eta)
                    }
                    Spacer(Modifier.height(12.dp))
                    WarningBanner("Only send ${coin.symbol} via ${net.name} to this address. Sending other assets or using another network will result in permanent loss.")
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Simulate an incoming deposit (demo)",
                        style = Z.type.bodySmallMedium, color = c.primary,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { simulate = true }.padding(8.dp),
                    )
                }
            }
        }
    }
    if (pick) NetworkSheet(coin, network, deposit = true, onDismiss = { pick = false }) { network = it }
    if (simulate && network != null) {
        var amount by remember { mutableStateOf(if (coin.stable) "500" else if (symbol == "BTC") "0.01" else "1") }
        ZBottomSheet({ simulate = false }, title = "Simulate deposit") { close ->
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("This demo creates an incoming on-chain deposit that confirms in a few seconds.", style = Z.type.bodySmall, color = c.textSecondary)
                Spacer(Modifier.height(12.dp))
                ZTextField(amount, { amount = sanitizeAmount(it, coin.decimals) }, label = "Amount (${coin.symbol})", keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
                Spacer(Modifier.height(16.dp))
                ZButton("Send test deposit", onClick = {
                    val a = amount.toAmount()
                    if (a <= 0) return@ZButton
                    close {
                        val id = AppRepository.simulateDeposit(symbol, network!!.id, a)
                        messenger.info("Deposit detected", "${Fmt.coin(a, symbol)} is confirming on ${network!!.short}.")
                        nav.navigate(Routes.tx(id))
                    }
                }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ------------------------------------------------------------------ receive (internal)

@Composable
fun ReceiveScreen(nav: NavController) {
    val c = Z.colors
    val user by AppRepository.user.collectAsState()
    val copy = rememberCopier()
    val context = LocalContext.current
    val payload = "zyropay://pay?uid=${user.uid}"
    ZScreen("Receive", onBack = { nav.back() }) {
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(com.zyropay.app.ui.theme.Brand.heroGradient).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            com.zyropay.app.ui.components.Avatar(user.avatar, size = 56.dp, ring = true)
            Spacer(Modifier.height(8.dp))
            Text(user.nickname, style = Z.type.h3, color = Color.White)
            Text("UID ${user.uid}", style = Z.type.caption, color = Color.White.copy(alpha = 0.75f))
            Spacer(Modifier.height(16.dp))
            Box(Modifier.size(220.dp).clip(RoundedCornerShape(20.dp)).background(Color.White).padding(14.dp)) {
                QrCode(payload, Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(14.dp))
            Text("Scan with Zyro Pay to send me crypto instantly with zero fees", style = Z.type.caption, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        SurfaceCard(Modifier.fillMaxWidth()) {
            DetailRow("Zyro Pay UID", user.uid, copyable = true, onCopy = { copy("UID", user.uid) })
            if (user.email.isNotEmpty()) DetailRow("Email", user.email, copyable = true, onCopy = { copy("Email", user.email) })
            if (user.phone.isNotEmpty()) DetailRow("Phone", user.phone, copyable = true, onCopy = { copy("Phone", user.phone) })
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZButton("Share", onClick = { context.shareText("Send me crypto on Zyro Pay — UID ${user.uid}") }, modifier = Modifier.weight(1f), style = ButtonStyle.Secondary, icon = R.drawable.ic_share)
            ZButton("On-chain deposit", onClick = { nav.navigate(Routes.selectCoin("deposit")) }, modifier = Modifier.weight(1f), icon = R.drawable.ic_deposit)
        }
    }
}

// ------------------------------------------------------------------ withdraw

@Composable
fun WithdrawScreen(nav: NavController, symbol: String) {
    val c = Z.colors
    val coin = MockData.coin(symbol)
    val messenger = LocalMessenger.current
    val paste = rememberPaster()
    val focus = LocalFocusManager.current
    val gate = rememberAuthGate()
    val scope = rememberCoroutineScope()
    val balances by AppRepository.balances.collectAsState()
    val user by AppRepository.user.collectAsState()
    val balance = balances[symbol] ?: 0.0
    var address by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var memo by rememberSaveable { mutableStateOf("") }
    var network by remember { mutableStateOf(coin.networks.first()) }
    var pick by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    var kycDialog by remember { mutableStateOf(false) }

    val a = amount.toAmount()
    val fee = network.fee
    val receive = (a - fee).coerceAtLeast(0.0)
    val addressError = when {
        address.isEmpty() -> null
        !validAddress(address, network) -> "Invalid ${network.short} address format"
        else -> null
    }
    val amountError = when {
        amount.isEmpty() -> null
        a > balance -> "Insufficient balance"
        a < network.minWithdraw -> "Minimum withdrawal is ${Fmt.plain(network.minWithdraw, coin.decimals)} ${coin.symbol}"
        else -> null
    }
    val valid = address.isNotEmpty() && addressError == null && amount.isNotEmpty() && amountError == null

    ZScreen("Withdraw ${coin.symbol}", onBack = { nav.back() }, bottomBar = {
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Receive amount", style = Z.type.caption, color = c.textSecondary)
                Text("${Fmt.plain(receive, coin.decimals)} ${coin.symbol}", style = Z.type.title, color = c.textPrimary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Network fee", style = Z.type.caption, color = c.textSecondary)
                Text("${Fmt.plain(fee, coin.decimals)} ${coin.symbol}", style = Z.type.bodySmallMedium, color = c.textPrimary)
            }
        }
        ZButton("Withdraw", onClick = {
            focus.clearFocus()
            if (user.kycStatus != KycStatus.VERIFIED) kycDialog = true else confirm = true
        }, modifier = Modifier.fillMaxWidth(), enabled = valid)
    }) {
        ZTextField(
            address, { address = it.trim() }, label = "Address", placeholder = "Long-press to paste", error = addressError, singleLine = false, minHeight = 54.dp,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Paste", style = Z.type.label, color = c.primary, modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable {
                        val p = paste()
                        if (p.isBlank()) messenger.toast("Clipboard is empty") else address = p.trim()
                    }.padding(4.dp))
                    Spacer(Modifier.width(6.dp))
                    ZIcon(R.drawable.ic_scan_qr, size = 20.dp, tint = c.textSecondary, modifier = Modifier.clip(CircleShape).clickable {
                        address = sampleAddress(network)
                        messenger.success("QR scanned", "Address filled from QR code.")
                    })
                }
            },
        )
        Spacer(Modifier.height(16.dp))
        NetworkField(network) { pick = true }
        if (network.id == "TON") {
            Spacer(Modifier.height(16.dp))
            ZTextField(memo, { memo = it }, label = "Memo (optional)", placeholder = "Required by some exchanges")
        }
        Spacer(Modifier.height(16.dp))
        ZTextField(
            amount, { amount = sanitizeAmount(it, coin.decimals) }, label = "Amount", placeholder = "Minimum ${Fmt.plain(network.minWithdraw, coin.decimals)}",
            error = amountError, keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(coin.symbol, style = Z.type.bodySmallMedium, color = c.textSecondary)
                    Spacer(Modifier.width(10.dp))
                    Text("MAX", style = Z.type.label, color = c.primary, modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { amount = Fmt.plain(balance, coin.decimals).replace(",", "") }.padding(4.dp))
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        Row {
            Text("Available", style = Z.type.caption, color = c.textSecondary, modifier = Modifier.weight(1f))
            Text(Fmt.coin(balance, symbol), style = Z.type.captionMedium, color = c.textPrimary)
        }
        Spacer(Modifier.height(20.dp))
        InfoBanner("Withdrawals to external addresses are irreversible. Double-check the address and network before confirming.", title = "Before you withdraw")
    }

    if (pick) NetworkSheet(coin, network, deposit = false, onDismiss = { pick = false }) { network = it; address = "" }
    if (kycDialog) {
        ZDialog(
            onDismiss = { kycDialog = false }, title = "Verification required",
            message = "Complete identity verification to unlock withdrawals and higher limits. It only takes 2 minutes.",
            confirmText = "Verify now", onConfirm = { kycDialog = false; nav.navigate(Routes.KYC) }, illustration = R.drawable.ill_kyc_intro,
        )
    }
    if (confirm) {
        ZBottomSheet({ confirm = false }, title = "Confirm withdrawal") { close ->
            Column(Modifier.padding(horizontal = 20.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    CoinIcon(symbol, size = 48.dp, network = network.icon)
                    Spacer(Modifier.height(10.dp))
                    Text("-${Fmt.plain(a, coin.decimals)} ${coin.symbol}", style = Z.type.h1.copy(fontFeatureSettings = "tnum"), color = c.textPrimary)
                    Text("≈ ${Fmt.fiat(a * AppRepository.priceOf(symbol))}", style = Z.type.bodySmall, color = c.textSecondary)
                }
                Spacer(Modifier.height(16.dp))
                DetailRow("To address", address)
                DetailRow("Network", network.name)
                DetailRow("Network fee", "${Fmt.plain(fee, coin.decimals)} ${coin.symbol}")
                DetailRow("You will receive", "${Fmt.plain(receive, coin.decimals)} ${coin.symbol}", bold = true)
                DetailRow("Estimated arrival", network.eta)
                Spacer(Modifier.height(16.dp))
                ZButton("Confirm", onClick = {
                    close {
                        gate.request("Confirm withdrawal") {
                            processing = true
                            scope.launch {
                                delay(1400)
                                val id = AppRepository.withdraw(symbol, network.id, address, receive, fee, memo.ifBlank { null })
                                processing = false
                                nav.navigate(Routes.result("withdraw", id)) { popUpTo(Routes.MAIN) }
                            }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (processing) LoadingDialog("Submitting withdrawal…")
}

private fun validAddress(a: String, n: Network): Boolean = when (n.addressPrefix) {
    "T" -> a.startsWith("T") && a.length == 34
    "0x" -> a.startsWith("0x") && a.length == 42 && a.drop(2).all { it.isLetterOrDigit() }
    "bc1q" -> (a.startsWith("bc1") || a.startsWith("1") || a.startsWith("3")) && a.length in 26..62
    "UQ" -> (a.startsWith("UQ") || a.startsWith("EQ")) && a.length == 48
    else -> a.length in 32..44
}

private fun sampleAddress(n: Network): String = when (n.addressPrefix) {
    "T" -> "TQn9Y2khEsLJW1ChVWFMSMeRDow5KcbLSE"
    "0x" -> "0x71C7656EC7ab88b098defB751B7401B5f6d8976F"
    "bc1q" -> "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh"
    "UQ" -> "UQBvW8Z5huBkMJYdnfAEM5JqTNkuWX3diqYENkWsIL0XggGG"
    else -> "7EcDhSYGxXyscszYEp35KHN8vvw3svAuLKTzXwCFLtV"
}

// ------------------------------------------------------------------ send (internal transfer)

@Composable
fun SendScreen(nav: NavController, symbol: String) {
    val c = Z.colors
    val coin = MockData.coin(symbol)
    val gate = rememberAuthGate()
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val balances by AppRepository.balances.collectAsState()
    val balance = balances[symbol] ?: 0.0
    var recipient by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    var lookup by remember { mutableStateOf<String?>(null) }
    var lookingUp by remember { mutableStateOf(false) }
    val a = amount.toAmount()
    val amountError = if (amount.isNotEmpty() && a > balance) "Insufficient balance" else null

    LaunchedEffect(recipient) {
        lookup = null
        if (recipient.length >= 6) {
            lookingUp = true
            delay(700)
            lookingUp = false
            lookup = when {
                "@" in recipient -> recipient.substringBefore("@").replaceFirstChar { it.uppercase() } + " K."
                recipient.all { it.isDigit() } && recipient.length >= 8 -> "Zyro user ••" + recipient.takeLast(3)
                recipient.length >= 8 -> "Verified user"
                else -> null
            }
        }
    }
    val contacts = listOf("Sarah K." to "sarah.k@gmail.com", "Leo Chan" to "18820431", "Ana P." to "ana.p@outlook.com", "Omar R." to "50318842")

    ZScreen("Send ${coin.symbol}", onBack = { nav.back() }, bottomBar = {
        ZButton("Continue", onClick = { focus.clearFocus(); confirm = true }, modifier = Modifier.fillMaxWidth(), enabled = lookup != null && a > 0 && amountError == null)
    }) {
        InfoBanner("Send instantly to any Zyro Pay user by email, phone or UID. Internal transfers are free and arrive in seconds.", icon = R.drawable.ic_sparkle)
        Spacer(Modifier.height(16.dp))
        ZTextField(
            recipient, { recipient = it.trim() }, label = "Recipient", placeholder = "Email / Phone / Zyro Pay UID", leadingIcon = R.drawable.ic_user,
            helper = when {
                lookingUp -> "Looking up user…"
                lookup != null -> "✓ $lookup"
                else -> null
            },
        )
        Spacer(Modifier.height(10.dp))
        Text("Recent", style = Z.type.label, color = c.textSecondary)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            contacts.forEachIndexed { i, (name, id) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.pressable { recipient = id }) {
                    com.zyropay.app.ui.components.Avatar(i + 2, size = 46.dp)
                    Spacer(Modifier.height(4.dp))
                    Text(name, style = Z.type.caption, color = c.textPrimary, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        ZTextField(
            amount, { amount = sanitizeAmount(it, coin.decimals) }, label = "Amount", placeholder = "0.00", error = amountError,
            keyboardType = KeyboardType.Decimal, leading = { CoinIcon(symbol, size = 24.dp) },
            trailing = {
                Text("MAX", style = Z.type.label, color = c.primary, modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { amount = Fmt.plain(balance, coin.decimals).replace(",", "") }.padding(4.dp))
            },
        )
        Spacer(Modifier.height(8.dp))
        Row {
            Text("Available: ${Fmt.coin(balance, symbol)}", style = Z.type.caption, color = c.textSecondary, modifier = Modifier.weight(1f))
            Text("≈ ${Fmt.fiat(a * AppRepository.priceOf(symbol))}", style = Z.type.caption, color = c.textSecondary)
        }
        Spacer(Modifier.height(16.dp))
        ZTextField(note, { note = it.take(60) }, label = "Note (optional)", placeholder = "What's it for?", imeAction = ImeAction.Done)
    }
    if (confirm) {
        ZBottomSheet({ confirm = false }, title = "Confirm transfer") { close ->
            Column(Modifier.padding(horizontal = 20.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${Fmt.plain(a, coin.decimals)} ${coin.symbol}", style = Z.type.h1.copy(fontFeatureSettings = "tnum"), color = c.textPrimary)
                    Text("≈ ${Fmt.fiat(a * AppRepository.priceOf(symbol))}", style = Z.type.bodySmall, color = c.textSecondary)
                }
                Spacer(Modifier.height(14.dp))
                DetailRow("To", "${lookup ?: ""}\n$recipient")
                DetailRow("Fee", "Free", valueColor = c.success)
                if (note.isNotBlank()) DetailRow("Note", note)
                DetailRow("Arrival", "Instant")
                Spacer(Modifier.height(16.dp))
                ZButton("Send now", onClick = {
                    close {
                        gate.request("Confirm transfer") {
                            processing = true
                            scope.launch {
                                delay(1200)
                                val id = AppRepository.sendInternal(symbol, lookup ?: recipient, a, note.ifBlank { null })
                                processing = false
                                nav.navigate(Routes.result("send", id)) { popUpTo(Routes.MAIN) }
                            }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (processing) LoadingDialog("Sending…")
}

// ------------------------------------------------------------------ swap

@Composable
fun CoinPickerSheet(title: String, exclude: String?, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val balances by AppRepository.balances.collectAsState()
    ZBottomSheet(onDismiss, title = title) { close ->
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
            items(MockData.coins.filter { it.symbol != exclude }, key = { it.symbol }) { coin ->
                SheetOption(
                    coin.symbol, subtitle = coin.name + " · Balance " + Fmt.coin(balances[coin.symbol] ?: 0.0, coin.symbol, withSymbol = false),
                    leading = { CoinIcon(coin.symbol, size = 36.dp) },
                ) { close { onPick(coin.symbol) } }
            }
        }
    }
}

@Composable
fun SwapScreen(nav: NavController, initialFrom: String) {
    val c = Z.colors
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val balances by AppRepository.balances.collectAsState()
    AppRepository.prices.collectAsState().value
    var from by rememberSaveable { mutableStateOf(initialFrom) }
    var to by rememberSaveable { mutableStateOf(if (initialFrom == "BTC") "USDT" else if (initialFrom == "USDT") "BTC" else "USDT") }
    var amount by rememberSaveable { mutableStateOf("") }
    var picker by remember { mutableStateOf<Int?>(null) }
    var confirm by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    var flips by remember { mutableIntStateOf(0) }
    var countdown by remember { mutableIntStateOf(15) }
    LaunchedEffect(from, to) {
        countdown = 15
        while (true) {
            delay(1000)
            countdown = if (countdown <= 1) 15 else countdown - 1
        }
    }
    val rotation by animateFloatAsState(flips * 180f, spring(dampingRatio = 0.6f), label = "flip")
    val fromCoin = MockData.coin(from)
    val toCoin = MockData.coin(to)
    val balance = balances[from] ?: 0.0
    val a = amount.toAmount()
    val rate = AppRepository.swapRate(from, to)
    val out = a * rate
    val error = when {
        amount.isEmpty() -> null
        a > balance -> "Insufficient $from balance"
        a * AppRepository.priceOf(from) < 1 -> "Minimum swap is $1"
        else -> null
    }

    ZScreen("Swap", onBack = { nav.back() }, actions = {
        IconButtonZ(R.drawable.ic_history, onClick = { nav.navigate(Routes.transactions("swap")) })
    }, bottomBar = {
        ZButton("Preview swap", onClick = { focus.clearFocus(); confirm = true }, modifier = Modifier.fillMaxWidth(), enabled = a > 0 && error == null)
    }) {
        Spacer(Modifier.height(4.dp))
        Box {
            Column {
                SwapBox("From", from, amount, editable = true, balance = balance, error = error,
                    onAmount = { amount = sanitizeAmount(it, fromCoin.decimals) },
                    onPick = { picker = 0 },
                    onMax = { amount = Fmt.plain(balance, fromCoin.decimals).replace(",", "") },
                    percents = { p -> amount = Fmt.plain(balance * p, fromCoin.decimals).replace(",", "") })
                Spacer(Modifier.height(8.dp))
                SwapBox("To (estimated)", to, if (a > 0) Fmt.plain(out, toCoin.decimals).replace(",", "") else "", editable = false, balance = balances[to] ?: 0.0, error = null,
                    onAmount = {}, onPick = { picker = 1 }, onMax = {}, percents = null)
            }
            Box(
                Modifier.align(Alignment.Center).size(44.dp).clip(CircleShape).background(c.background).padding(4.dp).clip(CircleShape).background(c.primary)
                    .pressable(scaleDown = 0.88f, haptic = true) { val t = from; from = to; to = t; amount = ""; flips++ },
                contentAlignment = Alignment.Center,
            ) { ZIcon(R.drawable.ic_swap, size = 20.dp, tint = Color.White, modifier = Modifier.graphicsLayer { rotationZ = rotation }) }
        }
        Spacer(Modifier.height(16.dp))
        SurfaceCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rate", style = Z.type.bodySmall, color = c.textSecondary, modifier = Modifier.weight(1f))
                Text("1 $from ≈ ${Fmt.plain(rate, toCoin.decimals.coerceAtLeast(2))} $to", style = Z.type.bodySmallMedium, color = c.textPrimary)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Quote refreshes in", style = Z.type.bodySmall, color = c.textSecondary, modifier = Modifier.weight(1f))
                com.zyropay.app.ui.components.Pill("${countdown}s", icon = R.drawable.ic_refresh)
            }
            Spacer(Modifier.height(10.dp))
            Row {
                Text("Fee", style = Z.type.bodySmall, color = c.textSecondary, modifier = Modifier.weight(1f))
                Text(if (fromCoin.stable && toCoin.stable) "0% (promo)" else "0.2% included", style = Z.type.bodySmallMedium, color = c.success)
            }
        }
        Spacer(Modifier.height(12.dp))
        InfoBanner("Swaps are executed instantly at the quoted rate. Final amount may vary slightly due to market movement.")
    }
    picker?.let { side ->
        CoinPickerSheet(if (side == 0) "Swap from" else "Swap to", exclude = if (side == 0) to else from, onDismiss = { picker = null }) {
            if (side == 0) from = it else to = it
            amount = ""
        }
    }
    if (confirm) {
        ZBottomSheet({ confirm = false }, title = "Confirm swap") { close ->
            Column(Modifier.padding(horizontal = 20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CoinIcon(from, size = 44.dp)
                        Spacer(Modifier.height(6.dp))
                        Text("${Fmt.plain(a, fromCoin.decimals)} $from", style = Z.type.title, color = c.textPrimary)
                    }
                    ZIcon(R.drawable.ic_arrow_right, size = 22.dp, tint = c.textTertiary)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CoinIcon(to, size = 44.dp)
                        Spacer(Modifier.height(6.dp))
                        Text("${Fmt.plain(out, toCoin.decimals)} $to", style = Z.type.title, color = c.success)
                    }
                }
                Spacer(Modifier.height(16.dp))
                DetailRow("Rate", "1 $from = ${Fmt.plain(rate, toCoin.decimals.coerceAtLeast(2))} $to")
                DetailRow("Value", Fmt.fiat(a * AppRepository.priceOf(from)))
                DetailRow("Fee", if (fromCoin.stable && toCoin.stable) "Free" else "0.2%")
                Spacer(Modifier.height(16.dp))
                ZButton("Confirm swap", onClick = {
                    close {
                        processing = true
                        scope.launch {
                            delay(1300)
                            val id = AppRepository.swap(from, to, a)
                            processing = false
                            nav.navigate(Routes.result("swap", id)) { popUpTo(Routes.MAIN) }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (processing) LoadingDialog("Swapping…")
}

@Composable
private fun SwapBox(
    label: String, symbol: String, amount: String, editable: Boolean, balance: Double, error: String?,
    onAmount: (String) -> Unit, onPick: () -> Unit, onMax: () -> Unit, percents: ((Double) -> Unit)?,
) {
    val c = Z.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface)
            .border(1.dp, if (error != null) c.error else c.outline.copy(alpha = 0.6f), RoundedCornerShape(20.dp)).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = Z.type.caption, color = c.textSecondary, modifier = Modifier.weight(1f))
            Text("Balance: ${Fmt.coin(balance, symbol, withSymbol = false)}", style = Z.type.caption, color = c.textSecondary)
            if (editable) {
                Spacer(Modifier.width(6.dp))
                Text("MAX", style = Z.type.label, color = c.primary, modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onMax).padding(2.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(c.surfaceVariant).clickable(onClick = onPick).padding(start = 6.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoinIcon(symbol, size = 26.dp)
                Spacer(Modifier.width(6.dp))
                Text(symbol, style = Z.type.title, color = c.textPrimary)
                ZIcon(R.drawable.ic_chevron_down, size = 16.dp, tint = c.textSecondary)
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                if (editable) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = amount, onValueChange = onAmount, singleLine = true,
                        textStyle = Z.type.h2.copy(color = c.textPrimary, textAlign = TextAlign.End, fontFeatureSettings = "tnum"),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(c.primary),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterEnd) {
                                if (amount.isEmpty()) Text("0.00", style = Z.type.h2, color = c.textTertiary)
                                inner()
                            }
                        },
                    )
                } else {
                    Text(amount.ifEmpty { "0.00" }, style = Z.type.h2.copy(fontFeatureSettings = "tnum"), color = if (amount.isEmpty()) c.textTertiary else c.textPrimary, maxLines = 1)
                }
            }
        }
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Text(error, style = Z.type.caption, color = c.error)
        }
        if (percents != null) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.25 to "25%", 0.5 to "50%", 0.75 to "75%", 1.0 to "100%").forEach { (p, l) ->
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(c.surfaceVariant).clickable { percents(p) }.padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(l, style = Z.type.label, color = c.textSecondary) }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ P2P

@Composable
fun P2PScreen(nav: NavController) {
    val c = Z.colors
    var buy by rememberSaveable { mutableStateOf(true) }
    var coin by rememberSaveable { mutableStateOf("USDT") }
    var fiat by rememberSaveable { mutableStateOf(AppRepository.prefs.value.currency.let { if (it == "USD") "HKD" else it }) }
    var pickFiat by remember { mutableStateOf(false) }
    val offers = remember(buy, coin, fiat) { MockData.p2pOffers(buy, fiat, coin) }
    Column(Modifier.fillMaxSize().background(c.background)) {
        ZTopBar("P2P Trading", onBack = { nav.back() }, actions = {
            IconButtonZ(R.drawable.ic_help, onClick = { nav.navigate(Routes.HELP) })
        })
        Column(Modifier.padding(horizontal = 20.dp)) {
            SegmentedTabs(listOf("Buy", "Sell"), if (buy) 0 else 1) { buy = it == 0 }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf("USDT", "USDC", "BTC", "ETH").forEach { s ->
                    ChipButton(s, coin == s, Modifier.padding(end = 8.dp)) { coin = s }
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.clip(RoundedCornerShape(10.dp)).clickable { pickFiat = true }.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ZImage(MockData.fiat(fiat).flag, Modifier.size(18.dp).clip(CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text(fiat, style = Z.type.label, color = c.textPrimary)
                    ZIcon(R.drawable.ic_chevron_down, size = 14.dp, tint = c.textSecondary)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
            items(offers, key = { it.id }) { o ->
                SurfaceCard(Modifier.fillMaxWidth().padding(vertical = 5.dp), padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.zyropay.app.ui.components.Avatar(o.avatar, size = 30.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(o.trader, style = Z.type.bodySmallMedium, color = c.textPrimary)
                        if (o.verified) {
                            Spacer(Modifier.width(4.dp))
                            ZIcon(R.drawable.ic_shield_check, size = 14.dp, tint = c.primary)
                        }
                        Spacer(Modifier.weight(1f))
                        Text("${o.orders} orders · ${"%.1f".format(o.completion)}%", style = Z.type.caption, color = c.textSecondary)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text("${MockData.fiat(fiat).symbol}${Fmt.plain(o.price, 2)}", style = Z.type.h3.copy(fontFeatureSettings = "tnum"), color = c.textPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text("Available ${Fmt.plain(o.available, MockData.coin(coin).decimals)} $coin", style = Z.type.caption, color = c.textSecondary)
                            Text("Limit ${MockData.fiat(fiat).symbol}${Fmt.compact(o.minLimit)} – ${Fmt.compact(o.maxLimit)}", style = Z.type.caption, color = c.textSecondary)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                o.methods.forEach { m -> com.zyropay.app.ui.components.Pill(m, color = c.textSecondary, background = c.surfaceVariant) }
                            }
                        }
                        ZButton(if (buy) "Buy" else "Sell", onClick = { nav.navigate(Routes.p2pOrder(o.id, buy, coin, fiat)) }, size = ButtonSize.Small, style = if (buy) ButtonStyle.Primary else ButtonStyle.Danger)
                    }
                }
            }
        }
    }
    if (pickFiat) {
        ZBottomSheet({ pickFiat = false }, title = "Select currency") { close ->
            LazyColumn(Modifier.heightIn(max = 460.dp)) {
                items(MockData.fiats, key = { it.code }) { f ->
                    SheetOption(f.code, subtitle = f.name, selected = f.code == fiat, leading = { ZImage(f.flag, Modifier.size(30.dp).clip(CircleShape)) }) { close { fiat = f.code } }
                }
            }
        }
    }
}

@Composable
fun P2POrderScreen(nav: NavController, offerId: String, buy: Boolean, symbol: String, fiat: String) {
    val c = Z.colors
    val offer = remember { MockData.p2pOffers(buy, fiat, symbol).firstOrNull { it.id == offerId } ?: MockData.p2pOffers(buy, fiat, symbol).first() }
    val f = MockData.fiat(fiat)
    val coin = MockData.coin(symbol)
    val gate = rememberAuthGate()
    val scope = rememberCoroutineScope()
    val copy = rememberCopier()
    var amountFiat by rememberSaveable { mutableStateOf("") }
    var stage by rememberSaveable { mutableIntStateOf(0) } // 0 enter, 1 pay, 2 releasing
    var seconds by remember { mutableIntStateOf(15 * 60) }
    val fiatValue = amountFiat.toAmount()
    val coinAmount = fiatValue / offer.price
    val err = when {
        amountFiat.isEmpty() -> null
        fiatValue < offer.minLimit -> "Minimum ${f.symbol}${Fmt.money(offer.minLimit)}"
        fiatValue > offer.maxLimit -> "Maximum ${f.symbol}${Fmt.money(offer.maxLimit)}"
        !buy && coinAmount > AppRepository.balanceOf(symbol) -> "Insufficient $symbol balance"
        else -> null
    }
    LaunchedEffect(stage) {
        if (stage == 1) while (seconds > 0) { delay(1000); seconds-- }
        if (stage == 2) {
            delay(3500)
            val id = AppRepository.p2pTrade(offer, buy, symbol, coinAmount)
            nav.navigate(Routes.result("p2p", id)) { popUpTo(Routes.P2P) { inclusive = true } }
        }
    }
    ZScreen(if (buy) "Buy $symbol" else "Sell $symbol", onBack = { nav.back() }, bottomBar = {
        when (stage) {
            0 -> ZButton(if (buy) "Buy $symbol" else "Sell $symbol", onClick = { stage = 1 }, modifier = Modifier.fillMaxWidth(), enabled = fiatValue > 0 && err == null)
            1 -> ZButton(if (buy) "I have paid" else "Confirm payment received", onClick = {
                gate.request("Confirm order") { stage = 2 }
            }, modifier = Modifier.fillMaxWidth())
            else -> ZButton("Waiting for release…", onClick = {}, modifier = Modifier.fillMaxWidth(), loading = true)
        }
    }) {
        SurfaceCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.zyropay.app.ui.components.Avatar(offer.avatar, size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(offer.trader, style = Z.type.bodyMedium, color = c.textPrimary)
                    Text("${offer.orders} orders · ${"%.1f".format(offer.completion)}% · ~${offer.responseMins} min", style = Z.type.caption, color = c.textSecondary)
                }
            }
            DividerLine(Modifier.padding(vertical = 12.dp))
            DetailRow("Price", "${f.symbol}${Fmt.plain(offer.price, 2)} / $symbol")
            DetailRow("Payment method", offer.methods.joinToString())
        }
        Spacer(Modifier.height(16.dp))
        AnimatedContent(stage, label = "p2pStage") { s ->
            Column {
                when (s) {
                    0 -> {
                        ZTextField(
                            amountFiat, { amountFiat = sanitizeAmount(it, 2) }, label = if (buy) "I want to pay" else "I want to receive",
                            placeholder = "${Fmt.compact(offer.minLimit)} – ${Fmt.compact(offer.maxLimit)}", error = err, keyboardType = KeyboardType.Decimal,
                            leading = { ZImage(f.flag, Modifier.size(22.dp).clip(CircleShape)) },
                            trailing = { Text(fiat, style = Z.type.bodySmallMedium, color = c.textSecondary) },
                        )
                        Spacer(Modifier.height(12.dp))
                        SurfaceCard(Modifier.fillMaxWidth()) {
                            DetailRow(if (buy) "You will receive" else "You will sell", "${Fmt.plain(coinAmount, coin.decimals)} $symbol", bold = true)
                            DetailRow("Fee", "0 (taker fee free)", valueColor = c.success)
                        }
                        Spacer(Modifier.height(12.dp))
                        InfoBanner("Your $symbol is held safely in escrow until both sides confirm. Never release crypto before receiving payment.", icon = R.drawable.ic_shield_check, color = c.success, background = c.successSoft)
                    }
                    else -> {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (s == 1) "Pay within" else "Seller is releasing crypto", style = Z.type.bodySmall, color = c.textSecondary)
                            Text(if (s == 1) "%02d:%02d".format(seconds / 60, seconds % 60) else "Usually < 5 min", style = Z.type.h1, color = if (s == 1) c.warning else c.primary)
                        }
                        Spacer(Modifier.height(14.dp))
                        SurfaceCard(Modifier.fillMaxWidth()) {
                            Text(if (buy) "Transfer to seller" else "Buyer will pay you", style = Z.type.title, color = c.textPrimary)
                            Spacer(Modifier.height(6.dp))
                            DetailRow("Amount", "${f.symbol}${Fmt.money(fiatValue)}", bold = true, copyable = true, onCopy = { copy("Amount", Fmt.money(fiatValue)) })
                            DetailRow("Method", offer.methods.first())
                            DetailRow("Account name", offer.trader.uppercase().replace("_", " "), copyable = true, onCopy = { copy("Name", offer.trader) })
                            DetailRow("Account no.", "0" + (offer.orders * 7919L % 1_000_000_000L).toString().padStart(9, '4'), copyable = true, onCopy = { copy("Account", "0123456789") })
                            DetailRow("Reference", "ZP" + offer.id.hashCode().toString().takeLast(6).replace("-", "7"), copyable = true, onCopy = { copy("Reference", offer.id) })
                        }
                        Spacer(Modifier.height(12.dp))
                        WarningBanner("Only pay from an account in your own name. Do not mention crypto in the transfer note.")
                    }
                }
            }
        }
    }
}
