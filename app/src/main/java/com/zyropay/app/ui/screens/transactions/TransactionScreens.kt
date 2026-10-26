package com.zyropay.app.ui.screens.transactions

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zyropay.app.R
import com.zyropay.app.data.AppNotification
import com.zyropay.app.data.AppRepository
import com.zyropay.app.data.Fmt
import com.zyropay.app.data.MockData
import com.zyropay.app.data.NotificationKind
import com.zyropay.app.data.Transaction
import com.zyropay.app.data.TxStatus
import com.zyropay.app.data.TxType
import com.zyropay.app.navigation.Routes
import com.zyropay.app.navigation.back
import com.zyropay.app.ui.components.ButtonStyle
import com.zyropay.app.ui.components.ChipButton
import com.zyropay.app.ui.components.DetailRow
import com.zyropay.app.ui.components.DividerLine
import com.zyropay.app.ui.components.EmptyState
import com.zyropay.app.ui.components.IconButtonZ
import com.zyropay.app.ui.components.IconCircle
import com.zyropay.app.ui.components.LocalMessenger
import com.zyropay.app.ui.components.SearchField
import com.zyropay.app.ui.components.StatusPill
import com.zyropay.app.ui.components.SurfaceCard
import com.zyropay.app.ui.components.TxLeading
import com.zyropay.app.ui.components.TxRow
import com.zyropay.app.ui.components.UnderlineTabs
import com.zyropay.app.ui.components.ZButton
import com.zyropay.app.ui.components.ZIcon
import com.zyropay.app.ui.components.ZImage
import com.zyropay.app.ui.components.ZScreen
import com.zyropay.app.ui.components.ZTopBar
import com.zyropay.app.ui.components.rememberCopier
import com.zyropay.app.ui.components.shareText
import com.zyropay.app.ui.theme.Z

private val filters = listOf("all" to "All", "card" to "Card", "deposit" to "Deposit", "withdraw" to "Withdraw", "transfer" to "Transfer", "swap" to "Swap", "rewards" to "Rewards")

private fun Transaction.matches(filter: String) = when (filter) {
    "card" -> type == TxType.CARD_PAYMENT || type == TxType.CARD_TOP_UP || type == TxType.CARD_FEE || type == TxType.REFUND || type == TxType.PAY
    "deposit" -> type == TxType.DEPOSIT || type == TxType.P2P_BUY
    "withdraw" -> type == TxType.WITHDRAW || type == TxType.P2P_SELL
    "transfer" -> type == TxType.SEND || type == TxType.RECEIVE
    "swap" -> type == TxType.SWAP
    "rewards" -> type == TxType.CASHBACK || type == TxType.REWARD
    else -> true
}

@Composable
fun TransactionsScreen(nav: NavController, initialFilter: String) {
    val c = Z.colors
    val txs by AppRepository.transactions.collectAsState()
    val prefs by AppRepository.prefs.collectAsState()
    var filter by rememberSaveable { mutableStateOf(initialFilter.ifBlank { "all" }) }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    val list = txs.filter { it.matches(filter) && (query.isBlank() || it.title.contains(query, true) || it.coin.contains(query, true) || it.subtitle.contains(query, true)) }
    val grouped = list.groupBy { Fmt.dayKey(it.timestamp) }
    val monthIn = txs.filter { it.status == TxStatus.SUCCESS && it.isIncoming && System.currentTimeMillis() - it.timestamp < 30 * Fmt.DAY }.sumOf { it.usdValue }
    val monthOut = txs.filter { it.status == TxStatus.SUCCESS && !it.isIncoming && it.type != TxType.SWAP && System.currentTimeMillis() - it.timestamp < 30 * Fmt.DAY }.sumOf { it.usdValue }

    Column(Modifier.fillMaxSize().background(c.background)) {
        ZTopBar("Transactions", onBack = { nav.back() }, actions = {
            IconButtonZ(if (searching) R.drawable.ic_close else R.drawable.ic_search, onClick = { searching = !searching; if (!searching) query = "" })
        })
        if (searching) SearchField(query, { query = it }, Modifier.padding(horizontal = 20.dp, vertical = 4.dp), placeholder = "Search merchant, coin…")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEach { (k, l) -> ChipButton(l, filter == k) { filter = k } }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item(key = "summary") {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SurfaceCard(Modifier.weight(1f), padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconCircle(R.drawable.ic_arrow_down_left, size = 28.dp, iconSize = 15.dp, background = c.successSoft, tint = c.success)
                            Spacer(Modifier.width(8.dp))
                            Text("In · 30d", style = Z.type.caption, color = c.textSecondary)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(Fmt.fiat(monthIn, prefs.currency), style = Z.type.title.copy(fontFeatureSettings = "tnum"), color = c.textPrimary)
                    }
                    SurfaceCard(Modifier.weight(1f), padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconCircle(R.drawable.ic_arrow_up_right, size = 28.dp, iconSize = 15.dp, background = c.errorSoft, tint = c.error)
                            Spacer(Modifier.width(8.dp))
                            Text("Out · 30d", style = Z.type.caption, color = c.textSecondary)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(Fmt.fiat(monthOut, prefs.currency), style = Z.type.title.copy(fontFeatureSettings = "tnum"), color = c.textPrimary)
                    }
                }
            }
            if (list.isEmpty()) {
                item(key = "empty") {
                    EmptyState(R.drawable.ill_empty_transactions, if (query.isNotBlank()) "No results" else "No transactions", if (query.isNotBlank()) "Try a different search term." else "Transactions of this type will show up here.")
                }
            }
            grouped.forEach { (day, items) ->
                item(key = "h$day") {
                    Text(Fmt.dayHeader(day), style = Z.type.label, color = c.textSecondary, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp))
                }
                items(items, key = { it.id }) { t ->
                    TxRow(t, prefs.hideBalance, prefs.currency, Modifier.padding(horizontal = 4.dp)) { nav.navigate(Routes.tx(t.id)) }
                }
            }
        }
    }
}

@Composable
fun TransactionDetailScreen(nav: NavController, id: String) {
    val c = Z.colors
    val txs by AppRepository.transactions.collectAsState()
    val cards by AppRepository.cards.collectAsState()
    val prefs by AppRepository.prefs.collectAsState()
    val copy = rememberCopier()
    val context = LocalContext.current
    val t = txs.firstOrNull { it.id == id }
    if (t == null) {
        ZScreen("Details", onBack = { nav.back() }) {
            EmptyState(R.drawable.ill_empty_transactions, "Transaction not found", "It may have been removed.")
        }
        return
    }
    ZScreen("Transaction details", onBack = { nav.back() }, bottomBar = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZButton("Get help", onClick = { nav.navigate(Routes.CHAT) }, modifier = Modifier.weight(1f), style = ButtonStyle.Secondary, icon = R.drawable.ic_support_chat)
            ZButton("Share receipt", onClick = {
                context.shareText("Zyro Pay receipt\n${t.title}\nAmount: ${amountText(t, prefs.currency)}\nStatus: ${t.status.name.lowercase()}\nDate: ${Fmt.dateTime(t.timestamp)}\nRef: ${t.id}")
            }, modifier = Modifier.weight(1f), icon = R.drawable.ic_share)
        }
    }) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TxLeading(t, size = 60.dp)
            Spacer(Modifier.height(12.dp))
            Text(t.title, style = Z.type.bodyMedium, color = c.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(
                amountText(t, prefs.currency),
                style = Z.type.h1.copy(fontFeatureSettings = "tnum"),
                color = if (t.status == TxStatus.FAILED) c.textTertiary else if (t.isIncoming) c.success else c.textPrimary,
            )
            if (t.coin != "USD" && t.type != TxType.SWAP) Text("≈ ${Fmt.fiat(t.usdValue, prefs.currency)}", style = Z.type.bodySmall, color = c.textSecondary)
            Spacer(Modifier.height(10.dp))
            StatusPill(t.status)
        }
        if (t.status != TxStatus.FAILED && (t.type == TxType.WITHDRAW || t.type == TxType.DEPOSIT)) {
            SurfaceCard(Modifier.fillMaxWidth()) { Timeline(t) }
            Spacer(Modifier.height(12.dp))
        }
        if (t.note != null && t.status == TxStatus.FAILED) {
            com.zyropay.app.ui.components.InfoBanner(t.note, icon = R.drawable.ic_warning, color = c.error, background = c.errorSoft, title = "Why did this fail?")
            Spacer(Modifier.height(12.dp))
        }
        SurfaceCard(Modifier.fillMaxWidth()) {
            DetailRow("Type", t.type.label)
            t.category?.let { DetailRow("Category", it.label) }
            if (t.merchant != null) DetailRow("Merchant", "${t.merchant}\n${t.subtitle}")
            t.cardId?.let { cid -> cards.firstOrNull { it.id == cid }?.let { DetailRow("Card", "${it.title} ·· ${it.last4}") } }
            if (t.type == TxType.SWAP && t.toCoin != null && t.toAmount != null) {
                DetailRow("Paid", Fmt.coin(-t.amount, t.coin))
                DetailRow("Received", Fmt.coin(t.toAmount, t.toCoin), valueColor = c.success)
                DetailRow("Rate", "1 ${t.coin} = ${Fmt.plain(t.toAmount / -t.amount, MockData.coin(t.toCoin).decimals)} ${t.toCoin}")
            }
            t.network?.let { DetailRow("Network", it) }
            t.address?.let { DetailRow(if (t.isIncoming) "From" else "To", it, copyable = true, onCopy = { copy("Address", it) }) }
            if (t.fee > 0) DetailRow("Fee", if (t.type == TxType.SWAP) Fmt.coin(t.fee, t.coin) else Fmt.coin(t.fee, t.coin))
            if (t.note != null && t.status != TxStatus.FAILED) DetailRow("Note", t.note)
            DividerLine(Modifier.padding(vertical = 6.dp))
            DetailRow("Date", Fmt.dateTime(t.timestamp))
            DetailRow("Order ID", t.id, copyable = true, onCopy = { copy("Order ID", t.id) })
            t.txHash?.let { DetailRow("TxID", Fmt.shortAddress(it), copyable = true, onCopy = { copy("TxID", it) }) }
        }
        if (t.type == TxType.CARD_PAYMENT) {
            Spacer(Modifier.height(12.dp))
            com.zyropay.app.ui.components.ListRow("Don't recognise this payment?", subtitle = "Freeze your card and report it", icon = R.drawable.ic_warning, iconTint = c.warning, iconBg = c.warningSoft, onClick = { nav.navigate(Routes.CHAT) })
        }
    }
}

private fun amountText(t: Transaction, currency: String): String = when {
    t.type == TxType.SWAP && t.toAmount != null && t.toCoin != null -> Fmt.coin(t.toAmount, t.toCoin, signed = true)
    t.coin == "USD" -> Fmt.fiat(t.amount, currency, signed = true)
    else -> Fmt.coin(t.amount, t.coin, signed = true)
}

@Composable
private fun Timeline(t: Transaction) {
    val c = Z.colors
    val steps = if (t.type == TxType.DEPOSIT) listOf("Deposit detected", "Confirming on network", "Credited to wallet") else listOf("Withdrawal submitted", "Processing", "Sent to network")
    val done = if (t.status == TxStatus.SUCCESS) 3 else 2
    steps.forEachIndexed { i, s ->
        Row(verticalAlignment = Alignment.Top) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(20.dp).clip(CircleShape).background(if (i < done) c.success else c.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (i < done) ZIcon(R.drawable.ic_check, size = 12.dp, tint = androidx.compose.ui.graphics.Color.White)
                    else if (i == done) com.zyropay.app.ui.components.BrandSpinner(Modifier.size(14.dp))
                }
                if (i < steps.lastIndex) Box(Modifier.width(2.dp).height(26.dp).background(if (i < done - 1) c.success else c.outline))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(s, style = Z.type.bodySmallMedium, color = if (i < done) c.textPrimary else c.textTertiary)
                if (i == 0) Text(Fmt.dateTime(t.timestamp), style = Z.type.caption, color = c.textTertiary)
            }
        }
    }
}

// ------------------------------------------------------------------ notifications

@Composable
fun NotificationsScreen(nav: NavController) {
    val c = Z.colors
    val notifications by AppRepository.notifications.collectAsState()
    val messenger = LocalMessenger.current
    var tab by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    val kinds = listOf(null, NotificationKind.TRANSACTION, NotificationKind.SECURITY, NotificationKind.SYSTEM, NotificationKind.PROMO)
    val list = notifications.filter { kinds[tab] == null || it.kind == kinds[tab] }
    Column(Modifier.fillMaxSize().background(c.background)) {
        ZTopBar("Notifications", onBack = { nav.back() }, actions = {
            IconButtonZ(R.drawable.ic_check_circle, onClick = {
                if (notifications.any { !it.read }) {
                    AppRepository.markAllNotificationsRead(); messenger.toast("All marked as read")
                } else messenger.toast("You're all caught up")
            })
            IconButtonZ(R.drawable.ic_settings, onClick = { nav.navigate(Routes.NOTIFICATION_SETTINGS) })
        })
        Box(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 6.dp)) {
            UnderlineTabs(listOf("All", "Transactions", "Security", "System", "Promotions"), tab) { tab = it }
        }
        if (list.isEmpty()) {
            EmptyState(R.drawable.ill_empty_notifications, "No notifications", "We'll let you know when something important happens.", Modifier.padding(top = 48.dp))
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp, top = 4.dp)) {
                items(list, key = { it.id }) { n ->
                    NotificationRow(n) {
                        AppRepository.markNotificationRead(n.id)
                        when {
                            n.txId != null -> nav.navigate(Routes.tx(n.txId))
                            n.kind == NotificationKind.SECURITY -> nav.navigate(Routes.SECURITY)
                            n.kind == NotificationKind.PROMO -> nav.navigate(Routes.REWARDS)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: AppNotification, onClick: () -> Unit) {
    val c = Z.colors
    val (icon, bg, tint) = when (n.kind) {
        NotificationKind.TRANSACTION -> Triple(R.drawable.ic_transfer, c.successSoft, c.success)
        NotificationKind.SECURITY -> Triple(R.drawable.ic_shield, c.warningSoft, c.warning)
        NotificationKind.SYSTEM -> Triple(R.drawable.ic_info, c.infoSoft, c.info)
        NotificationKind.PROMO -> Triple(R.drawable.ic_gift, c.primarySoft, c.primary)
    }
    Row(
        Modifier.fillMaxWidth().background(if (n.read) c.background else c.primarySoft.copy(alpha = if (c.isDark) 0.35f else 0.45f))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        IconCircle(icon, size = 40.dp, iconSize = 20.dp, background = bg, tint = tint)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(n.title, style = Z.type.bodySmallMedium.copy(fontWeight = if (n.read) androidx.compose.ui.text.font.FontWeight.Medium else androidx.compose.ui.text.font.FontWeight.SemiBold), color = c.textPrimary, modifier = Modifier.weight(1f), maxLines = 1)
                Text(Fmt.relative(n.timestamp), style = Z.type.caption, color = c.textTertiary)
            }
            Spacer(Modifier.height(3.dp))
            Text(n.body, style = Z.type.caption, color = c.textSecondary, maxLines = 3)
        }
        if (!n.read) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.padding(top = 6.dp).size(8.dp).clip(CircleShape).background(c.primary))
        }
    }
}

// ------------------------------------------------------------------ results

@Composable
fun ResultScreen(nav: NavController, kind: String, ref: String) {
    val c = Z.colors
    val txs by AppRepository.transactions.collectAsState()
    val prefs by AppRepository.prefs.collectAsState()
    val t = txs.firstOrNull { it.id == ref }
    val pending = t?.status == TxStatus.PENDING || kind == "kyc" || kind == "card_physical"
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f)) }
    val (title, subtitle) = when (kind) {
        "withdraw" -> (if (pending) "Withdrawal submitted" else "Withdrawal completed") to "Your funds are on the way. You'll be notified once confirmed on-chain."
        "send" -> "Transfer successful" to "The recipient has received the funds instantly."
        "swap" -> "Swap completed" to "Your new balance is available immediately."
        "topup" -> "Card topped up" to "Your card balance is ready to spend."
        "pay" -> "Payment successful" to "You earned 2% cashback on this purchase."
        "p2p" -> "Order completed" to "Crypto has been released to your wallet."
        "card_virtual" -> "Your card is ready!" to "Your virtual card has been issued. Top up to start spending online and in mobile wallets."
        "card_physical" -> "Card ordered" to "Your physical card is being produced and will ship within 3 business days. Track it from the Card tab."
        "kyc" -> "Verification submitted" to "We're reviewing your documents. This usually takes just a few minutes."
        else -> "Success" to ""
    }
    val illustration = when {
        kind == "card_virtual" -> R.drawable.ill_card_apply
        kind == "card_physical" -> R.drawable.ill_card_shipping
        kind == "kyc" -> R.drawable.ill_kyc_review
        pending -> R.drawable.ill_pending_clock
        else -> R.drawable.ill_success_check
    }
    androidx.activity.compose.BackHandler { nav.navigate(Routes.MAIN) { popUpTo(Routes.MAIN) { inclusive = false }; launchSingleTop = true } }
    Column(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.5f))
        ZImage(illustration, Modifier.size(200.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value })
        Spacer(Modifier.height(12.dp))
        Text(title, style = Z.type.h1, color = c.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = Z.type.body, color = c.textSecondary, textAlign = TextAlign.Center)
        if (t != null) {
            Spacer(Modifier.height(20.dp))
            SurfaceCard(Modifier.fillMaxWidth()) {
                DetailRow("Amount", amountText(t, prefs.currency), bold = true)
                DetailRow("Status", when (t.status) { TxStatus.SUCCESS -> "Completed"; TxStatus.PENDING -> "Processing"; else -> "Failed" }, valueColor = when (t.status) { TxStatus.SUCCESS -> c.success; TxStatus.PENDING -> c.warning; else -> c.error })
                DetailRow("Date", Fmt.dateTime(t.timestamp))
                DetailRow("Reference", t.id)
            }
        }
        Spacer(Modifier.weight(1f))
        if (t != null) {
            ZButton("View details", onClick = { nav.navigate(Routes.tx(t.id)) { popUpTo(Routes.MAIN) } }, modifier = Modifier.fillMaxWidth(), style = ButtonStyle.Secondary)
            Spacer(Modifier.height(10.dp))
        }
        ZButton("Done", onClick = { nav.navigate(Routes.MAIN) { popUpTo(Routes.MAIN) { inclusive = false }; launchSingleTop = true } }, modifier = Modifier.fillMaxWidth())
    }
}
