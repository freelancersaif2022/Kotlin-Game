package com.zyropay.app.ui.screens.card

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zyropay.app.R
import com.zyropay.app.data.AppRepository
import com.zyropay.app.data.CardDesign
import com.zyropay.app.data.CardKind
import com.zyropay.app.data.Fmt
import com.zyropay.app.data.KycStatus
import com.zyropay.app.data.MockData
import com.zyropay.app.navigation.Routes
import com.zyropay.app.navigation.back
import com.zyropay.app.ui.components.ButtonStyle
import com.zyropay.app.ui.components.CoinIcon
import com.zyropay.app.ui.components.DetailRow
import com.zyropay.app.ui.components.DividerLine
import com.zyropay.app.ui.components.InfoBanner
import com.zyropay.app.ui.components.KeyHint
import com.zyropay.app.ui.components.ListRow
import com.zyropay.app.ui.components.LoadingDialog
import com.zyropay.app.ui.components.LocalMessenger
import com.zyropay.app.ui.components.NumericKeypad
import com.zyropay.app.ui.components.PasscodeDots
import com.zyropay.app.ui.components.PaymentCardView
import com.zyropay.app.ui.components.Pill
import com.zyropay.app.ui.components.StepIndicator
import com.zyropay.app.ui.components.SurfaceCard
import com.zyropay.app.ui.components.SwitchRow
import com.zyropay.app.ui.components.WarningBanner
import com.zyropay.app.ui.components.ZButton
import com.zyropay.app.ui.components.ZDialog
import com.zyropay.app.ui.components.ZIcon
import com.zyropay.app.ui.components.ZImage
import com.zyropay.app.ui.components.ZScreen
import com.zyropay.app.ui.components.ZTextField
import com.zyropay.app.ui.components.appendAmount
import com.zyropay.app.ui.components.pressable
import com.zyropay.app.ui.components.rememberAuthGate
import com.zyropay.app.ui.screens.wallet.CoinPickerSheet
import com.zyropay.app.ui.theme.Z
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ------------------------------------------------------------------ apply

@Composable
fun CardApplyScreen(nav: NavController) {
    val c = Z.colors
    val user by AppRepository.user.collectAsState()
    val balances by AppRepository.balances.collectAsState()
    val gate = rememberAuthGate()
    val scope = rememberCoroutineScope()
    val messenger = LocalMessenger.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var kind by rememberSaveable { mutableStateOf(CardKind.VIRTUAL) }
    var design by rememberSaveable { mutableStateOf(CardDesign.VIOLET) }
    var name by rememberSaveable { mutableStateOf(user.fullName) }
    var line1 by rememberSaveable { mutableStateOf(user.address) }
    var city by rememberSaveable { mutableStateOf("") }
    var postcode by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf(user.phone) }
    var processing by remember { mutableStateOf(false) }
    val steps = if (kind == CardKind.PHYSICAL) 4 else 3
    val fee = AppRepository.cardFee(kind)
    val usdt = balances["USDT"] ?: 0.0

    if (user.kycStatus != KycStatus.VERIFIED) {
        ZScreen("Apply for card", onBack = { nav.back() }) {
            com.zyropay.app.ui.components.EmptyState(
                R.drawable.ill_kyc_intro, "Verification required",
                "To comply with regulations, please verify your identity before applying for a Zyro Pay card.",
                action = "Verify identity", onAction = { nav.navigate(Routes.KYC) },
            )
        }
        return
    }
    BackHandler(step > 0) { step-- }
    val isReview = (kind == CardKind.VIRTUAL && step == 2) || (kind == CardKind.PHYSICAL && step == 3)
    ZScreen("Apply for card", onBack = { if (step > 0) step-- else nav.back() }, bottomBar = {
        when {
            isReview -> ZButton("Pay ${Fmt.plain(fee, 2)} USDT & apply", onClick = {
                if (usdt < fee) {
                    messenger.error("Insufficient USDT", "You need ${Fmt.plain(fee, 2)} USDT. Deposit to continue.")
                    return@ZButton
                }
                gate.request("Confirm card application") {
                    processing = true
                    scope.launch {
                        delay(1800)
                        val address = if (kind == CardKind.PHYSICAL) "$line1, $city $postcode" else null
                        val id = AppRepository.applyCard(kind, design, address)
                        processing = false
                        nav.navigate(Routes.result(if (kind == CardKind.VIRTUAL) "card_virtual" else "card_physical", id)) { popUpTo(Routes.MAIN) }
                    }
                }
            }, modifier = Modifier.fillMaxWidth())
            kind == CardKind.PHYSICAL && step == 2 -> ZButton("Continue", onClick = { step++ }, modifier = Modifier.fillMaxWidth(), enabled = name.isNotBlank() && line1.isNotBlank() && city.isNotBlank() && phone.isNotBlank())
            else -> ZButton("Continue", onClick = { step++ }, modifier = Modifier.fillMaxWidth())
        }
    }) {
        StepIndicator(step, steps)
        Spacer(Modifier.height(20.dp))
        AnimatedContent(step, transitionSpec = {
            if (targetState > initialState) (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
            else (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
        }, label = "applyStep") { s ->
            Column {
                when {
                    s == 0 -> {
                        Text("Choose your card", style = Z.type.h2, color = c.textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text("Both cards spend from the same crypto balance.", style = Z.type.bodySmall, color = c.textSecondary)
                        Spacer(Modifier.height(16.dp))
                        CardTypeOption(
                            CardKind.VIRTUAL, kind == CardKind.VIRTUAL, R.drawable.card_virtual_violet, "Virtual card", "10 USDT · Issued instantly",
                            listOf("Online & mobile wallet payments", "Ready in seconds", "Up to 5 virtual cards"),
                        ) { kind = CardKind.VIRTUAL; design = CardDesign.VIOLET }
                        Spacer(Modifier.height(12.dp))
                        CardTypeOption(
                            CardKind.PHYSICAL, kind == CardKind.PHYSICAL, R.drawable.card_physical_black, "Physical card", "100 USDT · Free worldwide shipping",
                            listOf("In-store, contactless & ATM", "Premium metal finish", "Delivered in 7–14 days"),
                        ) { kind = CardKind.PHYSICAL; design = CardDesign.BLACK }
                    }
                    s == 1 -> {
                        Text("Pick a design", style = Z.type.h2, color = c.textPrimary)
                        Spacer(Modifier.height(16.dp))
                        val preview = com.zyropay.app.data.PaymentCard(
                            "preview", kind, design, name.ifBlank { user.fullName.ifBlank { "YOUR NAME" } }.uppercase(), "4893150000004821", "12/30", "000", 0.0,
                        )
                        PaymentCardView(preview, Modifier.fillMaxWidth().padding(horizontal = 8.dp))
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CardDesign.entries.filter { it.kind == kind }.forEach { d ->
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        Modifier.fillMaxWidth().aspectRatio(1.585f).clip(RoundedCornerShape(10.dp))
                                            .border(2.dp, if (d == design) c.primary else Color.Transparent, RoundedCornerShape(10.dp))
                                            .pressable { design = d }.padding(3.dp).clip(RoundedCornerShape(8.dp)),
                                    ) { ZImage(d.art, Modifier.fillMaxSize(), ContentScale.Crop) }
                                    Spacer(Modifier.height(6.dp))
                                    Text(d.label, style = Z.type.caption, color = if (d == design) c.textPrimary else c.textSecondary, maxLines = 1)
                                }
                            }
                        }
                        if (kind == CardKind.PHYSICAL && design == CardDesign.GOLD) {
                            Spacer(Modifier.height(12.dp))
                            InfoBanner("Royal Gold is available to VIP 2+ members. You qualify!", icon = R.drawable.ic_crown, color = c.warning, background = c.warningSoft)
                        }
                    }
                    kind == CardKind.PHYSICAL && s == 2 -> {
                        Text("Shipping address", style = Z.type.h2, color = c.textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text("We ship worldwide with tracking via DHL Express.", style = Z.type.bodySmall, color = c.textSecondary)
                        Spacer(Modifier.height(16.dp))
                        ZTextField(name, { name = it }, label = "Full name", placeholder = "As shown on ID", leadingIcon = R.drawable.ic_user)
                        Spacer(Modifier.height(12.dp))
                        ZTextField(line1, { line1 = it }, label = "Address", placeholder = "Street, building, unit", leadingIcon = R.drawable.ic_location)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ZTextField(city, { city = it }, Modifier.weight(1f), label = "City", placeholder = "City")
                            ZTextField(postcode, { postcode = it }, Modifier.weight(1f), label = "Postcode", placeholder = "Optional")
                        }
                        Spacer(Modifier.height(12.dp))
                        ZTextField(phone, { phone = it }, label = "Phone for courier", placeholder = "+852 5123 4567", leadingIcon = R.drawable.ic_phone, keyboardType = KeyboardType.Phone)
                    }
                    else -> {
                        Text("Review & confirm", style = Z.type.h2, color = c.textPrimary)
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(width = 88.dp, height = 56.dp).clip(RoundedCornerShape(8.dp))) { ZImage(design.art, Modifier.fillMaxSize(), ContentScale.Crop) }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(if (kind == CardKind.VIRTUAL) "Virtual card" else "Physical card", style = Z.type.title, color = c.textPrimary)
                                Text(design.label, style = Z.type.bodySmall, color = c.textSecondary)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        SurfaceCard(Modifier.fillMaxWidth()) {
                            DetailRow("Card fee", "${Fmt.plain(fee, 2)} USDT")
                            DetailRow("Monthly fee", "Free", valueColor = c.success)
                            if (kind == CardKind.PHYSICAL) {
                                DetailRow("Shipping", "Free (DHL Express)", valueColor = c.success)
                                DetailRow("Deliver to", "$name\n$line1, $city $postcode")
                            }
                            DividerLine(Modifier.padding(vertical = 6.dp))
                            DetailRow("Pay with", "USDT balance")
                            DetailRow("Available", Fmt.coin(usdt, "USDT"), valueColor = if (usdt >= fee) c.textPrimary else c.error)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("By applying you agree to the Cardholder Agreement and Fee Schedule.", style = Z.type.caption, color = c.textTertiary)
                    }
                }
            }
        }
    }
    if (processing) LoadingDialog(if (kind == CardKind.VIRTUAL) "Issuing your card…" else "Placing your order…")
}

@Composable
private fun CardTypeOption(kind: CardKind, selected: Boolean, art: Int, title: String, subtitle: String, perks: List<String>, onClick: () -> Unit) {
    val c = Z.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (selected) c.primarySoft else c.surface)
            .border(1.5.dp, if (selected) c.primary else c.outline, RoundedCornerShape(18.dp)).pressable(scaleDown = 0.985f, onClick = onClick).padding(14.dp),
    ) {
        Box(Modifier.size(width = 84.dp, height = 53.dp).clip(RoundedCornerShape(8.dp))) { ZImage(art, Modifier.fillMaxSize(), ContentScale.Crop) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = Z.type.title, color = c.textPrimary, modifier = Modifier.weight(1f))
                if (kind == CardKind.PHYSICAL) Pill("Popular", color = c.warning, background = c.warningSoft)
            }
            Text(subtitle, style = Z.type.caption, color = c.primary)
            Spacer(Modifier.height(6.dp))
            perks.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
                    ZIcon(R.drawable.ic_check, size = 13.dp, tint = c.success)
                    Spacer(Modifier.width(6.dp))
                    Text(p, style = Z.type.caption, color = c.textSecondary)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ top up

@Composable
fun CardTopUpScreen(nav: NavController, cardId: String) {
    val c = Z.colors
    val cards by AppRepository.cards.collectAsState()
    val balances by AppRepository.balances.collectAsState()
    val card = cards.firstOrNull { it.id == cardId } ?: return
    val gate = rememberAuthGate()
    val scope = rememberCoroutineScope()
    var coin by rememberSaveable { mutableStateOf("USDT") }
    var amount by rememberSaveable { mutableStateOf("") }
    var pick by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    val coinInfo = MockData.coin(coin)
    val balance = balances[coin] ?: 0.0
    val a = amount.toDoubleOrNull() ?: 0.0 // amount entered in USD
    val price = AppRepository.priceOf(coin)
    val coinNeeded = a / price
    val error = when {
        a == 0.0 -> null
        coinNeeded > balance -> "Insufficient $coin balance"
        a < 10 -> "Minimum top-up is $10"
        else -> null
    }
    ZScreen("Top up card", onBack = { nav.back() }, scrollable = false) {
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(c.surfaceVariant).padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                com.zyropay.app.ui.components.CardThumb(card, Modifier.size(width = 34.dp, height = 22.dp))
                Spacer(Modifier.width(8.dp))
                Text("${card.title} ·· ${card.last4}", style = Z.type.label, color = c.textPrimary)
            }
            Spacer(Modifier.weight(1f))
            Text("Enter amount (USD)", style = Z.type.bodySmall, color = c.textSecondary)
            Spacer(Modifier.height(6.dp))
            Text(
                "$" + amount.ifEmpty { "0" },
                style = Z.type.amount.copy(fontSize = if (amount.length > 7) 36.sp else 44.sp),
                color = if (error != null) c.error else if (amount.isEmpty()) c.textTertiary else c.textPrimary,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(error ?: "≈ ${Fmt.plain(coinNeeded, coinInfo.decimals)} $coin", style = Z.type.bodySmall, color = if (error != null) c.error else c.textSecondary)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(50, 100, 200, 500).forEach { v ->
                    Box(Modifier.clip(RoundedCornerShape(50)).background(c.surfaceVariant).clickable { amount = v.toString() }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                        Text("$$v", style = Z.type.label, color = c.textPrimary)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).border(1.dp, c.outline.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .clickable { pick = true }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoinIcon(coin, size = 36.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Pay with $coin", style = Z.type.bodyMedium, color = c.textPrimary)
                    Text("Balance ${Fmt.coin(balance, coin)}", style = Z.type.caption, color = c.textSecondary)
                }
                Text("Max", style = Z.type.label, color = c.primary, modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { amount = ((balance * price).toLong()).toString() }.padding(6.dp))
                ZIcon(R.drawable.ic_chevron_down, size = 18.dp, tint = c.textTertiary)
            }
            Spacer(Modifier.height(8.dp))
        }
        NumericKeypad(
            onDigit = { amount = appendAmount(amount, it, 2) },
            onDelete = { amount = amount.dropLast(1) },
            decimal = true,
            modifier = Modifier.padding(horizontal = 16.dp),
            keyHeight = 54.dp,
        )
        ZButton(
            "Top up", onClick = {
                gate.request("Confirm top-up") {
                    processing = true
                    scope.launch {
                        delay(1300)
                        val id = AppRepository.topUpCard(card.id, coin, coinNeeded)
                        processing = false
                        nav.navigate(Routes.result("topup", id)) { popUpTo(Routes.MAIN) }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), enabled = a >= 10 && error == null,
        )
    }
    if (pick) CoinPickerSheet("Pay with", exclude = null, onDismiss = { pick = false }) { coin = it }
    if (processing) LoadingDialog("Topping up…")
}

// ------------------------------------------------------------------ settings

@Composable
fun CardSettingsScreen(nav: NavController, cardId: String) {
    val c = Z.colors
    val cards by AppRepository.cards.collectAsState()
    val card = cards.firstOrNull { it.id == cardId }
    val messenger = LocalMessenger.current
    val gate = rememberAuthGate()
    var confirmCancel by remember { mutableStateOf(false) }
    var confirmReplace by remember { mutableStateOf(false) }
    if (card == null) {
        LaunchedEffect(Unit) { nav.back() }
        return
    }
    ZScreen("Card settings", onBack = { nav.back() }) {
        PaymentCardView(card, Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp))
        Spacer(Modifier.height(16.dp))
        Text("Security", style = Z.type.label, color = c.textSecondary)
        Spacer(Modifier.height(8.dp))
        SurfaceCard(Modifier.fillMaxWidth(), padding = 0.dp) {
            SwitchRow("Freeze card", card.frozen, { v -> gate.request(if (v) "Freeze card" else "Unfreeze card") { AppRepository.setCardFrozen(card.id, v); messenger.toast(if (v) "Card frozen" else "Card unfrozen") } }, icon = R.drawable.ic_card_freeze, subtitle = "Temporarily block all transactions")
            DividerLine(inset = 66.dp)
            ListRow("Change card PIN", icon = R.drawable.ic_card_pin, subtitle = "Used for ATMs and chip & PIN", onClick = { nav.navigate(Routes.cardPin(card.id)) })
            DividerLine(inset = 66.dp)
            ListRow("Spending limits", icon = R.drawable.ic_card_limit, value = Fmt.compact(card.dailyLimit) + "/day", onClick = { nav.navigate(Routes.cardLimits(card.id)) })
        }
        Spacer(Modifier.height(20.dp))
        Text("Payment channels", style = Z.type.label, color = c.textSecondary)
        Spacer(Modifier.height(8.dp))
        SurfaceCard(Modifier.fillMaxWidth(), padding = 0.dp) {
            SwitchRow("Online payments", card.onlinePayments, { v -> AppRepository.updateCard(card.id) { it.copy(onlinePayments = v) } }, icon = R.drawable.ic_online_payment)
            DividerLine(inset = 66.dp)
            SwitchRow("Contactless payments", card.contactless, { v -> AppRepository.updateCard(card.id) { it.copy(contactless = v) } }, icon = R.drawable.ic_contactless)
            DividerLine(inset = 66.dp)
            SwitchRow("ATM withdrawals", card.atmWithdrawals, { v -> AppRepository.updateCard(card.id) { it.copy(atmWithdrawals = v) } }, icon = R.drawable.ic_atm, enabled = card.kind == CardKind.PHYSICAL, subtitle = if (card.kind == CardKind.VIRTUAL) "Physical cards only" else null)
        }
        Spacer(Modifier.height(20.dp))
        Text("Manage", style = Z.type.label, color = c.textSecondary)
        Spacer(Modifier.height(8.dp))
        SurfaceCard(Modifier.fillMaxWidth(), padding = 0.dp) {
            ListRow("Replace card", icon = R.drawable.ic_replace_card, subtitle = "Lost, stolen or damaged", onClick = { confirmReplace = true })
            DividerLine(inset = 66.dp)
            ListRow("Cancel card", icon = R.drawable.ic_trash, titleColor = c.error, iconTint = c.error, iconBg = c.errorSoft, onClick = { confirmCancel = true })
        }
        Spacer(Modifier.height(12.dp))
        DetailRow("Card type", card.title + " · " + card.design.label)
        DetailRow("Issued", Fmt.monthYear(card.createdAt))
    }
    if (confirmReplace) {
        ZDialog(
            onDismiss = { confirmReplace = false }, title = "Replace this card?",
            message = "Your current card ending ${card.last4} will be cancelled immediately and a new card number will be issued. Your balance moves to the new card.",
            confirmText = "Replace card", icon = R.drawable.ic_replace_card,
            onConfirm = { confirmReplace = false; gate.request("Replace card") { AppRepository.replaceCard(card.id); messenger.success("Card replaced", "Your new card details are ready.") } },
        )
    }
    if (confirmCancel) {
        ZDialog(
            onDismiss = { confirmCancel = false }, title = "Cancel card permanently?",
            message = "This can't be undone. Remaining balance of ${Fmt.fiat(card.balance)} will be returned to your USDT wallet.",
            confirmText = "Cancel card", destructive = true, icon = R.drawable.ic_warning, iconTint = c.error, iconBg = c.errorSoft,
            onConfirm = {
                confirmCancel = false
                gate.request("Cancel card") {
                    AppRepository.cancelCard(card.id)
                    messenger.info("Card cancelled", "Balance returned to your wallet.")
                    nav.back()
                }
            },
        )
    }
}

@Composable
fun CardLimitsScreen(nav: NavController, cardId: String) {
    val c = Z.colors
    val card = AppRepository.cards.collectAsState().value.firstOrNull { it.id == cardId } ?: return
    val messenger = LocalMessenger.current
    var daily by remember { mutableDoubleStateOf(card.dailyLimit) }
    var monthly by remember { mutableDoubleStateOf(card.monthlyLimit) }
    ZScreen("Spending limits", onBack = { nav.back() }, bottomBar = {
        ZButton("Save limits", onClick = {
            AppRepository.updateCard(card.id) { it.copy(dailyLimit = daily, monthlyLimit = monthly) }
            messenger.success("Limits updated", "New limits apply immediately.")
            nav.back()
        }, modifier = Modifier.fillMaxWidth(), enabled = daily != card.dailyLimit || monthly != card.monthlyLimit)
    }) {
        LimitSlider("Daily limit", daily, 100.0, 20_000.0, card.spentToday) { daily = it; if (monthly < it) monthly = it }
        Spacer(Modifier.height(20.dp))
        LimitSlider("Monthly limit", monthly, 1_000.0, 200_000.0, card.spentMonth) { monthly = it.coerceAtLeast(daily) }
        Spacer(Modifier.height(20.dp))
        InfoBanner("Your maximum limits depend on your verification level. Complete advanced verification for limits up to $1,000,000 per month.")
    }
}

@Composable
private fun LimitSlider(label: String, value: Double, min: Double, max: Double, spent: Double, onChange: (Double) -> Unit) {
    val c = Z.colors
    SurfaceCard(Modifier.fillMaxWidth()) {
        Text(label, style = Z.type.bodySmall, color = c.textSecondary)
        Text(Fmt.fiat(value), style = Z.type.h2.copy(fontFeatureSettings = "tnum"), color = c.textPrimary)
        Slider(
            value = value.toFloat(), onValueChange = { onChange((it / 100).toInt() * 100.0) },
            valueRange = min.toFloat()..max.toFloat(),
            colors = SliderDefaults.colors(thumbColor = c.primary, activeTrackColor = c.primary, inactiveTrackColor = c.surfaceVariant),
        )
        Row {
            Text(Fmt.fiat(min), style = Z.type.caption, color = c.textTertiary, modifier = Modifier.weight(1f))
            Text(Fmt.fiat(max), style = Z.type.caption, color = c.textTertiary)
        }
        Spacer(Modifier.height(6.dp))
        Text("Used ${Fmt.fiat(spent)}", style = Z.type.captionMedium, color = c.textSecondary)
    }
}

@Composable
fun CardPinScreen(nav: NavController, cardId: String) {
    val messenger = LocalMessenger.current
    var step by remember { mutableIntStateOf(0) }
    var first by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var shake by remember { mutableIntStateOf(0) }
    LaunchedEffect(code) {
        if (code.length == 4) {
            delay(150)
            if (step == 0) {
                if (code.toSet().size == 1 || code == "1234") { error = "Choose a less obvious PIN"; shake++; delay(350); code = "" }
                else { first = code; code = ""; step = 1 }
            } else if (code == first) {
                messenger.success("PIN updated", "Use your new PIN at ATMs and terminals.")
                nav.back()
            } else {
                error = "PINs don't match"; shake++; delay(350); code = ""; step = 0
            }
        }
    }
    Column(Modifier.fillMaxSize().background(Z.colors.background)) {
        com.zyropay.app.ui.components.ZTopBar("Change PIN", onBack = { nav.back() })
        Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ZImage(R.drawable.ill_security_setup, Modifier.size(130.dp))
            Spacer(Modifier.height(10.dp))
            Text(if (step == 0) "Enter new 4-digit PIN" else "Confirm new PIN", style = Z.type.h3, color = Z.colors.textPrimary)
            Spacer(Modifier.height(6.dp))
            Text(error ?: "Your PIN is used for ATM withdrawals and chip & PIN payments", style = Z.type.bodySmall, color = if (error != null) Z.colors.error else Z.colors.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            PasscodeDots(code.length, length = 4, shakeKey = shake, error = error != null && code.length == 4)
        }
        NumericKeypad(onDigit = { if (code.length < 4) { code += it; error = null } }, onDelete = { code = code.dropLast(1) }, modifier = Modifier.padding(horizontal = 24.dp))
        com.zyropay.app.ui.components.KeyHint("", Modifier.height(24.dp))
        Spacer(Modifier.height(8.dp))
    }
}
