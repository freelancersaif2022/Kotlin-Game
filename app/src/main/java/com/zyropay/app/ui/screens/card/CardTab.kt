package com.zyropay.app.ui.screens.card

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.navigation.NavController
import com.zyropay.app.R
import com.zyropay.app.data.AppRepository
import com.zyropay.app.data.CardDesign
import com.zyropay.app.data.CardStatus
import com.zyropay.app.data.Fmt
import com.zyropay.app.data.KycStatus
import com.zyropay.app.data.PaymentCard
import com.zyropay.app.navigation.Routes
import com.zyropay.app.ui.components.ButtonStyle
import com.zyropay.app.ui.components.DetailRow
import com.zyropay.app.ui.components.DividerLine
import com.zyropay.app.ui.components.IconButtonZ
import com.zyropay.app.ui.components.ListRow
import com.zyropay.app.ui.components.LocalMessenger
import com.zyropay.app.ui.components.PaymentCardView
import com.zyropay.app.ui.components.ProgressBar
import com.zyropay.app.ui.components.QuickAction
import com.zyropay.app.ui.components.SectionHeader
import com.zyropay.app.ui.components.SurfaceCard
import com.zyropay.app.ui.components.SwitchRow
import com.zyropay.app.ui.components.TxRow
import com.zyropay.app.ui.components.ZBottomSheet
import com.zyropay.app.ui.components.ZButton
import com.zyropay.app.ui.components.ZDialog
import com.zyropay.app.ui.components.ZImage
import com.zyropay.app.ui.components.maskIf
import com.zyropay.app.ui.components.rememberAuthGate
import com.zyropay.app.ui.components.rememberCopier
import com.zyropay.app.ui.theme.Z
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

@Composable
fun CardTab(nav: NavController) {
    val c = Z.colors
    val cards by AppRepository.cards.collectAsState()
    val user by AppRepository.user.collectAsState()
    val prefs by AppRepository.prefs.collectAsState()
    val txs by AppRepository.transactions.collectAsState()
    val messenger = LocalMessenger.current
    val gate = rememberAuthGate()
    var details by remember { mutableStateOf<PaymentCard?>(null) }
    var freezeTarget by remember { mutableStateOf<PaymentCard?>(null) }
    var walletSheet by remember { mutableStateOf(false) }

    if (cards.isEmpty()) {
        NoCardState(nav, user.kycStatus)
        return
    }
    val pager = rememberPagerState { cards.size }
    val card = cards[pager.currentPage.coerceIn(0, cards.lastIndex)]

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "title") {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("My cards", style = Z.type.h1, color = c.textPrimary, modifier = Modifier.weight(1f))
                IconButtonZ(R.drawable.ic_card_add, onClick = { nav.navigate(Routes.CARD_APPLY) })
            }
        }
        item(key = "pager") {
            HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 28.dp), pageSpacing = 12.dp, modifier = Modifier.padding(top = 8.dp)) { i ->
                val offset = ((pager.currentPage - i) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                PaymentCardView(
                    cards[i],
                    Modifier.fillMaxWidth().padding(vertical = 12.dp).graphicsLayer {
                        val s = lerp(1f, 0.9f, offset)
                        scaleX = s; scaleY = s; alpha = lerp(1f, 0.6f, offset)
                    },
                )
            }
        }
        item(key = "dots") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(cards.size) { i ->
                    Box(Modifier.padding(horizontal = 3.dp).size(width = if (i == pager.currentPage) 18.dp else 6.dp, height = 6.dp).clip(RoundedCornerShape(3.dp)).background(if (i == pager.currentPage) c.primary else c.outline))
                }
            }
        }
        item(key = "balance") {
            Column(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${card.title} balance", style = Z.type.bodySmall, color = c.textSecondary)
                Text(maskIf(prefs.hideBalance, Fmt.fiat(card.balance, prefs.currency)), style = Z.type.balance.copy(fontSize = 32.sp), color = c.textPrimary)
            }
        }
        item(key = "actions") {
            val active = card.status == CardStatus.ACTIVE
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                QuickAction("Top up", R.drawable.ic_top_up) {
                    if (!active) messenger.info("Card not active yet", "Your physical card is on its way.")
                    else nav.navigate(Routes.cardTopUp(card.id))
                }
                QuickAction("Details", R.drawable.ic_card_details) {
                    gate.request("View card details") { details = card }
                }
                QuickAction(
                    if (card.frozen) "Unfreeze" else "Freeze",
                    if (card.frozen) R.drawable.ic_card_unfreeze else R.drawable.ic_card_freeze,
                    background = if (card.frozen) c.infoSoft else c.primarySoft,
                    tint = if (card.frozen) c.info else c.onPrimarySoft,
                ) { freezeTarget = card }
                QuickAction("Settings", R.drawable.ic_card_settings) { nav.navigate(Routes.cardSettings(card.id)) }
            }
        }
        item(key = "spend") {
            SurfaceCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Monthly spending", style = Z.type.title, color = c.textPrimary, modifier = Modifier.weight(1f))
                    Text("Limit ${Fmt.fiat(card.monthlyLimit, prefs.currency)}", style = Z.type.caption, color = c.textSecondary)
                }
                Spacer(Modifier.height(10.dp))
                ProgressBar((card.spentMonth / card.monthlyLimit).toFloat())
                Spacer(Modifier.height(8.dp))
                Row {
                    Text("Spent ${Fmt.fiat(card.spentMonth, prefs.currency)}", style = Z.type.caption, color = c.textSecondary, modifier = Modifier.weight(1f))
                    Text("Today ${Fmt.fiat(card.spentToday, prefs.currency)} / ${Fmt.fiat(card.dailyLimit, prefs.currency)}", style = Z.type.caption, color = c.textSecondary)
                }
            }
        }
        item(key = "controls") {
            SurfaceCard(Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth(), padding = 0.dp) {
                SwitchRow("Online payments", card.onlinePayments, { v -> AppRepository.updateCard(card.id) { it.copy(onlinePayments = v) }; messenger.toast(if (v) "Online payments on" else "Online payments off") }, icon = R.drawable.ic_online_payment)
                DividerLine(inset = 66.dp)
                SwitchRow("Contactless", card.contactless, { v -> AppRepository.updateCard(card.id) { it.copy(contactless = v) }; messenger.toast(if (v) "Contactless on" else "Contactless off") }, icon = R.drawable.ic_contactless)
                DividerLine(inset = 66.dp)
                ListRow("Add to mobile wallet", subtitle = "Pay with your phone in-store", icon = R.drawable.ic_apple_wallet_generic, onClick = { walletSheet = true })
            }
        }
        item(key = "txHeader") {
            SectionHeader("Card transactions", Modifier.padding(horizontal = 20.dp).padding(top = 8.dp), "See all") { nav.navigate(Routes.transactions("card")) }
        }
        val list = txs.filter { it.cardId == card.id }.take(10)
        if (list.isEmpty()) {
            item(key = "noTx") {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ZImage(R.drawable.ill_empty_transactions, Modifier.size(120.dp))
                    Text("No card transactions yet", style = Z.type.bodySmall, color = c.textSecondary)
                    Spacer(Modifier.height(12.dp))
                    ZButton("Try a test payment", onClick = { nav.navigate(Routes.SCAN) }, style = ButtonStyle.Secondary, size = com.zyropay.app.ui.components.ButtonSize.Small)
                }
            }
        } else {
            item(key = "txList") {
                SurfaceCard(Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth(), padding = 0.dp) {
                    list.forEach { t -> TxRow(t, prefs.hideBalance, prefs.currency) { nav.navigate(Routes.tx(t.id)) } }
                }
            }
        }
    }

    details?.let { d -> CardDetailsSheet(d) { details = null } }
    freezeTarget?.let { f ->
        ZDialog(
            onDismiss = { freezeTarget = null },
            title = if (f.frozen) "Unfreeze card?" else "Freeze card?",
            message = if (f.frozen) "Your card ending ${f.last4} will be able to make payments again." else "All payments and ATM withdrawals with card ending ${f.last4} will be declined until you unfreeze it.",
            confirmText = if (f.frozen) "Unfreeze" else "Freeze card",
            icon = if (f.frozen) R.drawable.ic_card_unfreeze else R.drawable.ic_card_freeze,
            iconTint = c.info, iconBg = c.infoSoft,
            onConfirm = {
                freezeTarget = null
                gate.request(if (f.frozen) "Unfreeze card" else "Freeze card") {
                    AppRepository.setCardFrozen(f.id, !f.frozen)
                    if (f.frozen) messenger.success("Card unfrozen", "Card ending ${f.last4} is ready to use.")
                    else messenger.info("Card frozen", "Card ending ${f.last4} is temporarily locked.")
                }
            },
        )
    }
    if (walletSheet) {
        var adding by remember { mutableStateOf(false) }
        ZBottomSheet({ walletSheet = false }) { close ->
            Column(Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PaymentCardView(card, Modifier.fillMaxWidth(0.7f), elevation = false)
                Spacer(Modifier.height(16.dp))
                Text("Add to mobile wallet", style = Z.type.h3, color = c.textPrimary)
                Spacer(Modifier.height(6.dp))
                Text("Tap to pay with your phone wherever contactless is accepted. Your card number is never shared with merchants.", style = Z.type.bodySmall, color = c.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                LaunchedEffect(adding) {
                    if (adding) {
                        delay(1600)
                        close { messenger.success("Card added", "Card ending ${card.last4} is ready for contactless payments.") }
                    }
                }
                ZButton("Add card", onClick = { adding = true }, loading = adding, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun NoCardState(nav: NavController, kyc: KycStatus) {
    val c = Z.colors
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Text("Card", style = Z.type.h1, color = c.textPrimary, modifier = Modifier.statusBarsPadding().padding(start = 20.dp, top = 16.dp))
        }
        item {
            val pager = rememberPagerState { 3 }
            LaunchedEffect(Unit) { while (true) { delay(3000); pager.animateScrollToPage((pager.currentPage + 1) % 3) } }
            HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 40.dp), pageSpacing = 12.dp, modifier = Modifier.padding(vertical = 20.dp)) { i ->
                val d = listOf(CardDesign.VIOLET, CardDesign.BLACK, CardDesign.AURORA)[i]
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))) {
                    ZImage(d.art, Modifier.fillMaxWidth().aspectRatio(1.585f), ContentScale.Crop)
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("One card for all your crypto", style = Z.type.h2, color = c.textPrimary)
                Spacer(Modifier.height(8.dp))
                Text("Spend USDT, USDC, BTC and more anywhere cards are accepted — online, in-store and at ATMs.", style = Z.type.bodySmall, color = c.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                listOf(
                    R.drawable.ic_globe_language to "Accepted at 130M+ merchants in 170+ countries",
                    R.drawable.ic_category_cashback to "Up to 2% cashback on every purchase",
                    R.drawable.ic_shield_check to "Freeze, limits and alerts in real time",
                    R.drawable.ic_apple_wallet_generic to "Works with mobile wallets",
                ).forEach { (icon, text) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        com.zyropay.app.ui.components.IconCircle(icon, size = 36.dp, iconSize = 18.dp, background = c.primarySoft, tint = c.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(text, style = Z.type.bodySmallMedium, color = c.textPrimary)
                    }
                }
                Spacer(Modifier.height(20.dp))
                if (kyc != KycStatus.VERIFIED) {
                    com.zyropay.app.ui.components.InfoBanner(
                        if (kyc == KycStatus.PENDING) "Your verification is under review. You can apply as soon as it's approved." else "Identity verification is required before applying for a card.",
                        title = if (kyc == KycStatus.PENDING) "Verification in review" else "Verify first",
                    )
                    Spacer(Modifier.height(12.dp))
                }
                ZButton(
                    if (kyc == KycStatus.VERIFIED) "Apply for a card" else if (kyc == KycStatus.PENDING) "Check status" else "Verify identity",
                    onClick = { nav.navigate(if (kyc == KycStatus.VERIFIED) Routes.CARD_APPLY else Routes.KYC) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun CardDetailsSheet(card: PaymentCard, onDismiss: () -> Unit) {
    val c = Z.colors
    val copy = rememberCopier()
    var back by remember { mutableStateOf(false) }
    ZBottomSheet(onDismiss, title = "Card details") { _ ->
        Column(Modifier.padding(horizontal = 20.dp)) {
            PaymentCardView(card, Modifier.fillMaxWidth().padding(horizontal = 12.dp).clickable(interactionSource = null, indication = null) { back = !back }, revealed = true, showBack = back, elevation = false)
            Spacer(Modifier.height(10.dp))
            Text(
                if (back) "Tap to see front" else "Tap to flip card",
                style = Z.type.caption, color = c.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(8.dp))
                    .clickable(interactionSource = null, indication = null) { back = !back }.padding(4.dp),
            )
            Spacer(Modifier.height(8.dp))
            SurfaceCard(Modifier.fillMaxWidth(), color = c.surfaceVariant, border = false) {
                DetailRow("Card number", card.formattedNumber, copyable = true, onCopy = { copy("Card number", card.number) })
                DetailRow("Cardholder", card.holder, copyable = true, onCopy = { copy("Name", card.holder) })
                DetailRow("Expiry date", card.expiry, copyable = true, onCopy = { copy("Expiry", card.expiry) })
                DetailRow("CVV", card.cvv, copyable = true, onCopy = { copy("CVV", card.cvv) })
                DetailRow("Billing address", "Flat 12A, 88 Queen's Road Central, Hong Kong")
            }
            Spacer(Modifier.height(12.dp))
            com.zyropay.app.ui.components.WarningBanner("Never share your card details or CVV. Zyro Pay staff will never ask for them.")
            Spacer(Modifier.height(8.dp))
            Text("Details will auto-hide when you close this sheet", style = Z.type.caption.copy(fontWeight = FontWeight.Normal), color = c.textTertiary, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
