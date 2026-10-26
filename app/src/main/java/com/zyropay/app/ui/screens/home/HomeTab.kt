package com.zyropay.app.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zyropay.app.R
import com.zyropay.app.data.AppRepository
import com.zyropay.app.data.CardStatus
import com.zyropay.app.data.Fmt
import com.zyropay.app.data.KycStatus
import com.zyropay.app.navigation.Routes
import com.zyropay.app.ui.components.Avatar
import com.zyropay.app.ui.components.BadgeDot
import com.zyropay.app.ui.components.ButtonSize
import com.zyropay.app.ui.components.ButtonStyle
import com.zyropay.app.ui.components.CardThumb
import com.zyropay.app.ui.components.CoinPriceRow
import com.zyropay.app.ui.components.EmptyState
import com.zyropay.app.ui.components.IconButtonZ
import com.zyropay.app.ui.components.IconCircle
import com.zyropay.app.ui.components.LocalMessenger
import com.zyropay.app.ui.components.SectionHeader
import com.zyropay.app.ui.components.SurfaceCard
import com.zyropay.app.ui.components.TxRow
import com.zyropay.app.ui.components.ZButton
import com.zyropay.app.ui.components.ZIcon
import com.zyropay.app.ui.components.ZImage
import com.zyropay.app.ui.components.maskIf
import com.zyropay.app.ui.components.pressable
import com.zyropay.app.ui.screens.main.Tab
import com.zyropay.app.ui.theme.Z
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

private data class Promo(val image: Int, val title: String, val body: String, val route: String)

private val promos = listOf(
    Promo(R.drawable.banner_cashback, "2% cashback", "On every card purchase, paid in USDT", Routes.REWARDS),
    Promo(R.drawable.banner_referral, "Invite & earn", "Get 5 USDT for every friend who joins", Routes.REFERRAL),
    Promo(R.drawable.banner_physical_card, "Metal card", "Order the Obsidian physical card", Routes.CARD_APPLY),
    Promo(R.drawable.banner_zero_fee, "0% swap fee", "Swap stablecoins free this month", Routes.swap("USDT")),
    Promo(R.drawable.banner_travel, "Travel smarter", "Low FX fees in 170+ countries", Routes.CARD_APPLY),
    Promo(R.drawable.banner_security, "Stay protected", "Enable 2FA in 1 minute", Routes.SECURITY),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTab(nav: NavController, onSwitchTab: (Tab) -> Unit) {
    val c = Z.colors
    val user by AppRepository.user.collectAsState()
    val prefs by AppRepository.prefs.collectAsState()
    val balances by AppRepository.balances.collectAsState()
    val prices by AppRepository.prices.collectAsState()
    val cards by AppRepository.cards.collectAsState()
    val txs by AppRepository.transactions.collectAsState()
    val notifications by AppRepository.notifications.collectAsState()
    val messenger = LocalMessenger.current
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val total = balances.entries.sumOf { (k, v) -> v * (prices[k]?.usd ?: 0.0) } + cards.sumOf { it.balance }
    val change = balances.entries.sumOf { (k, v) -> v * (prices[k]?.usd ?: 0.0) * (prices[k]?.change24h ?: 0.0) / 100 }
    val unread = notifications.count { !it.read }
    val hidden = prefs.hideBalance

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            refreshing = true
            scope.launch { delay(1200); refreshing = false; messenger.toast("Balances updated") }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
            item(key = "header") {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(user.avatar, size = 40.dp, modifier = Modifier.pressable { onSwitchTab(Tab.ME) })
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(greeting(), style = Z.type.caption, color = c.textSecondary)
                        Text(user.nickname.ifBlank { "Welcome" }, style = Z.type.title, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButtonZ(R.drawable.ic_headset, onClick = { nav.navigate(Routes.CHAT) })
                    IconButtonZ(R.drawable.ic_scan_qr, onClick = { nav.navigate(Routes.SCAN) })
                    Box {
                        IconButtonZ(R.drawable.ic_bell, onClick = { nav.navigate(Routes.NOTIFICATIONS) })
                        BadgeDot(Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp), unread)
                    }
                }
            }
            item(key = "hero") {
                BalanceHero(
                    total = total, change = change, hidden = hidden, currency = prefs.currency,
                    btc = total / (prices["BTC"]?.usd ?: 67_000.0),
                    onToggle = { AppRepository.updatePrefs { it.copy(hideBalance = !it.hideBalance) } },
                    onDeposit = { nav.navigate(Routes.selectCoin("deposit")) },
                    onSend = { nav.navigate(Routes.selectCoin("send")) },
                    onSwap = { nav.navigate(Routes.swap()) },
                    onWithdraw = { nav.navigate(Routes.selectCoin("withdraw")) },
                )
            }
            item(key = "shortcuts") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Shortcut(R.drawable.ic_top_up, "Top up card") {
                        val card = cards.firstOrNull { it.status == CardStatus.ACTIVE }
                        if (card != null) nav.navigate(Routes.cardTopUp(card.id)) else onSwitchTab(Tab.CARD)
                    }
                    Shortcut(R.drawable.ic_p2p, "P2P trade") { nav.navigate(Routes.P2P) }
                    Shortcut(R.drawable.ic_receive, "Receive") { nav.navigate(Routes.RECEIVE) }
                    Shortcut(R.drawable.ic_gift, "Rewards") { nav.navigate(Routes.REWARDS) }
                }
            }
            if (user.kycStatus != KycStatus.VERIFIED) {
                item(key = "kyc") { KycBanner(user.kycStatus) { nav.navigate(Routes.KYC) } }
            }
            item(key = "card") {
                val card = cards.firstOrNull()
                SurfaceCard(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth(), onClick = { onSwitchTab(Tab.CARD) }) {
                    if (card != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CardThumb(card)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${card.title} ·· ${card.last4}", style = Z.type.caption, color = c.textSecondary)
                                Text(maskIf(hidden, Fmt.fiat(card.balance, prefs.currency)), style = Z.type.number.copy(fontSize = 18.sp), color = c.textPrimary)
                            }
                            ZButton("Top up", onClick = { nav.navigate(Routes.cardTopUp(card.id)) }, size = ButtonSize.Small, style = ButtonStyle.Secondary, icon = R.drawable.ic_add)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Get your Zyro Pay card", style = Z.type.title, color = c.textPrimary)
                                Spacer(Modifier.height(4.dp))
                                Text("Spend crypto at 130M+ merchants. Virtual card in seconds.", style = Z.type.caption, color = c.textSecondary)
                                Spacer(Modifier.height(12.dp))
                                ZButton("Apply now", onClick = { nav.navigate(Routes.CARD_APPLY) }, size = ButtonSize.Small)
                            }
                            ZImage(R.drawable.ill_card_apply, Modifier.size(96.dp))
                        }
                    }
                }
            }
            item(key = "promos") { PromoCarousel { nav.navigate(it) } }
            item(key = "marketsHeader") {
                SectionHeader("Markets", Modifier.padding(horizontal = 20.dp).padding(top = 12.dp), "All assets") { onSwitchTab(Tab.WALLET) }
            }
            item(key = "markets") {
                SurfaceCard(Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth(), padding = 0.dp) {
                    listOf("BTC", "ETH", "SOL", "BNB", "TON").forEach { sym ->
                        CoinPriceRow(sym, prices[sym]) { nav.navigate(Routes.coin(sym)) }
                    }
                }
            }
            item(key = "txHeader") {
                SectionHeader("Recent activity", Modifier.padding(horizontal = 20.dp).padding(top = 16.dp), "View all") { nav.navigate(Routes.transactions()) }
            }
            val recent = txs.take(6)
            if (recent.isEmpty()) {
                item(key = "emptyTx") { EmptyState(R.drawable.ill_empty_transactions, "No transactions yet", "Your deposits, card payments and transfers will appear here.") }
            } else {
                item(key = "txCard") {
                    SurfaceCard(Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth(), padding = 0.dp) {
                        recent.forEach { t -> TxRow(t, hidden, prefs.currency) { nav.navigate(Routes.tx(t.id)) } }
                    }
                }
            }
            item(key = "footer") {
                Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    ZIcon(R.drawable.ic_shield_check, size = 14.dp, tint = c.textTertiary)
                    Spacer(Modifier.width(6.dp))
                    Text("Funds secured with institutional-grade custody", style = Z.type.caption, color = c.textTertiary)
                }
            }
        }
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning ☀️"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night 🌙"
}

@Composable
private fun BalanceHero(
    total: Double, change: Double, hidden: Boolean, currency: String, btc: Double,
    onToggle: () -> Unit, onDeposit: () -> Unit, onSend: () -> Unit, onSwap: () -> Unit, onWithdraw: () -> Unit,
) {
    val animated by animateFloatAsState(total.toFloat(), tween(900), label = "balance")
    val base = total - change
    val pct = if (base > 0) change / base * 100 else 0.0
    Box(
        Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.linearGradient(listOf(Color(0xFF4A2FE0), Color(0xFF6A45F7), Color(0xFF8F5BFF)), Offset.Zero, Offset(size.width, size.height)))
            drawCircle(Brush.radialGradient(listOf(Color(0x6600D1FF), Color.Transparent), Offset(size.width * 0.95f, 0f), size.width * 0.6f), size.width * 0.6f, Offset(size.width * 0.95f, 0f))
            drawCircle(Color.White.copy(alpha = 0.06f), size.width * 0.35f, Offset(size.width * 0.05f, size.height * 1.05f))
        }
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onToggle).padding(vertical = 2.dp)) {
                Text("Total balance ($currency)", style = Z.type.bodySmall, color = Color.White.copy(alpha = 0.82f))
                Spacer(Modifier.width(6.dp))
                ZIcon(if (hidden) R.drawable.ic_eye_off else R.drawable.ic_eye, size = 16.dp, tint = Color.White.copy(alpha = 0.82f))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (hidden) "••••••••" else Fmt.fiat(animated.toDouble(), currency),
                style = Z.type.balance,
                color = Color.White,
                maxLines = 1,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (hidden) "≈ •••• BTC" else "≈ ${Fmt.plain(btc, 6)} BTC", style = Z.type.caption, color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.width(10.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ZIcon(if (change >= 0) R.drawable.ic_trending_up else R.drawable.ic_trending_down, size = 13.dp, tint = if (change >= 0) Color(0xFF7CF5C2) else Color(0xFFFFB4AE))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (hidden) "•••" else "${Fmt.fiat(change, currency, signed = true)} (${Fmt.percent(pct)}) 24h",
                        style = Z.type.captionMedium, color = Color.White,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HeroAction(R.drawable.ic_deposit, "Deposit", onDeposit)
                HeroAction(R.drawable.ic_send, "Send", onSend)
                HeroAction(R.drawable.ic_swap, "Swap", onSwap)
                HeroAction(R.drawable.ic_withdraw, "Withdraw", onWithdraw)
            }
        }
    }
}

@Composable
private fun HeroAction(icon: Int, label: String, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).pressable(scaleDown = 0.9f, haptic = true, ripple = false, onClick = onClick).padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(50.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            ZIcon(icon, size = 22.dp, tint = Color.White)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = Z.type.captionMedium, color = Color.White)
    }
}

@Composable
private fun Shortcut(icon: Int, label: String, onClick: () -> Unit) {
    val c = Z.colors
    Column(
        Modifier.width(76.dp).clip(RoundedCornerShape(14.dp)).pressable(scaleDown = 0.92f, onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconCircle(icon, size = 46.dp, iconSize = 22.dp, background = c.surface, tint = c.primary, modifier = Modifier)
        Spacer(Modifier.height(6.dp))
        Text(label, style = Z.type.caption.copy(fontWeight = FontWeight.Medium), color = c.textPrimary, maxLines = 1)
    }
}

@Composable
private fun KycBanner(status: KycStatus, onClick: () -> Unit) {
    val c = Z.colors
    val pending = status == KycStatus.PENDING
    Row(
        Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (pending) c.warningSoft else c.primarySoft)
            .pressable(scaleDown = 0.985f, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ZImage(if (pending) R.drawable.ill_kyc_review else R.drawable.ill_kyc_intro, Modifier.size(54.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (pending) "Verification in review" else "Verify your identity", style = Z.type.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = c.textPrimary)
            Text(
                if (pending) "Usually takes a few minutes. We'll notify you." else "Unlock your card, higher limits and withdrawals. Takes 2 minutes.",
                style = Z.type.caption, color = c.textSecondary,
            )
        }
        ZIcon(R.drawable.ic_chevron_right, size = 20.dp, tint = c.textSecondary)
    }
}

@Composable
private fun PromoCarousel(onOpen: (String) -> Unit) {
    val c = Z.colors
    val pager = rememberPagerState { promos.size }
    LaunchedEffect(pager) {
        while (true) {
            delay(4500)
            if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.currentPage + 1) % promos.size, animationSpec = tween(600))
        }
    }
    Column(Modifier.padding(top = 12.dp)) {
        HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 20.dp), pageSpacing = 12.dp) { i ->
            val p = promos[i]
            Box(
                Modifier.fillMaxWidth().aspectRatio(1600f / 640f).clip(RoundedCornerShape(20.dp))
                    .pressable(scaleDown = 0.985f) { onOpen(p.route) },
            ) {
                ZImage(p.image, Modifier.fillMaxSize(), ContentScale.Crop)
                Column(Modifier.fillMaxWidth(0.58f).align(Alignment.CenterStart).padding(start = 18.dp)) {
                    Text(p.title, style = Z.type.h3, color = Color(0xFF0B0F19))
                    Spacer(Modifier.height(4.dp))
                    Text(p.body, style = Z.type.caption, color = Color(0xFF3D4456), maxLines = 2)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF0B0F19)).padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Learn more", style = Z.type.captionMedium, color = Color.White)
                        ZIcon(R.drawable.ic_chevron_right, size = 12.dp, tint = Color.White)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
            repeat(promos.size) { i ->
                val active = i == pager.currentPage
                Box(Modifier.padding(horizontal = 2.5.dp).size(width = if (active) 16.dp else 6.dp, height = 6.dp).clip(RoundedCornerShape(3.dp)).background(if (active) c.primary else c.outline))
            }
        }
    }
}
