package com.zyropay.app.ui.screens.support

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zyropay.app.BuildConfig
import com.zyropay.app.R
import com.zyropay.app.data.AppRepository
import com.zyropay.app.data.Fmt
import com.zyropay.app.data.MockData
import com.zyropay.app.navigation.Routes
import com.zyropay.app.navigation.back
import com.zyropay.app.ui.components.ChipButton
import com.zyropay.app.ui.components.DividerLine
import com.zyropay.app.ui.components.IconButtonZ
import com.zyropay.app.ui.components.IconCircle
import com.zyropay.app.ui.components.ListRow
import com.zyropay.app.ui.components.LocalMessenger
import com.zyropay.app.ui.components.SearchField
import com.zyropay.app.ui.components.SurfaceCard
import com.zyropay.app.ui.components.ZButton
import com.zyropay.app.ui.components.ZIcon
import com.zyropay.app.ui.components.ZImage
import com.zyropay.app.ui.components.ZScreen
import com.zyropay.app.ui.components.ZTopBar
import com.zyropay.app.ui.theme.Brand
import com.zyropay.app.ui.theme.Z

@Composable
fun HelpCenterScreen(nav: NavController) {
    val c = Z.colors
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val sections = MockData.faqs.filter { category == null || it.first == category }
        .map { (t, qs) -> t to qs.filter { query.isBlank() || it.first.contains(query, true) || it.second.contains(query, true) } }
        .filter { it.second.isNotEmpty() }
    Column(Modifier.fillMaxSize().background(c.background)) {
        ZTopBar("Help center", onBack = { nav.back() })
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text("How can we help?", style = Z.type.h1, color = c.textPrimary)
                    Spacer(Modifier.height(12.dp))
                    SearchField(query, { query = it }, placeholder = "Search for answers")
                    Spacer(Modifier.height(12.dp))
                }
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipButton("All", category == null) { category = null }
                    MockData.faqs.forEach { (t, _) -> ChipButton(t, category == t) { category = t } }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (sections.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No articles found", style = Z.type.title, color = c.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("Try different keywords or chat with us.", style = Z.type.bodySmall, color = c.textSecondary)
                    }
                }
            }
            sections.forEach { (title, qs) ->
                item(key = "t_$title") { Text(title, style = Z.type.label, color = c.textSecondary, modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)) }
                item(key = "s_$title") {
                    SurfaceCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), padding = 0.dp) {
                        qs.forEachIndexed { i, (q, a) ->
                            val open = expanded == q
                            val rot by androidx.compose.animation.core.animateFloatAsState(if (open) 180f else 0f, label = "faq")
                            Column(Modifier.fillMaxWidth().clickable { expanded = if (open) null else q }.padding(16.dp).animateContentSize()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(q, style = Z.type.bodyMedium, color = c.textPrimary, modifier = Modifier.weight(1f))
                                    ZIcon(R.drawable.ic_chevron_down, size = 18.dp, tint = c.textTertiary, modifier = Modifier.graphicsLayer { rotationZ = rot })
                                }
                                if (open) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(a, style = Z.type.bodySmall, color = c.textSecondary)
                                }
                            }
                            if (i < qs.lastIndex) DividerLine(Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
            item {
                SurfaceCard(Modifier.padding(20.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ZImage(R.drawable.ill_support_agent, Modifier.size(64.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Still need help?", style = Z.type.title, color = c.textPrimary)
                            Text("Our team is available 24/7", style = Z.type.caption, color = c.textSecondary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    ZButton("Chat with us", onClick = { nav.navigate(Routes.CHAT) }, modifier = Modifier.fillMaxWidth(), icon = R.drawable.ic_support_chat)
                }
            }
        }
    }
}

@Composable
fun SupportChatScreen(nav: NavController) {
    val c = Z.colors
    val messages by AppRepository.chat.collectAsState()
    val typing by AppRepository.agentTyping.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { AppRepository.startChatIfNeeded() }
    LaunchedEffect(messages.size, typing) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size + if (typing) 1 else 0) }
    val send = {
        if (input.isNotBlank()) {
            AppRepository.sendChat(input.trim())
            input = ""
        }
    }
    Column(Modifier.fillMaxSize().background(c.background).imePadding()) {
        ZTopBar(onBack = { nav.back() }, centerTitle = false, title = "")
        Row(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                ZImage(R.drawable.ill_support_agent, Modifier.size(42.dp).clip(CircleShape).background(c.primarySoft))
                Box(Modifier.align(Alignment.BottomEnd).size(12.dp).clip(CircleShape).background(c.background).padding(2.dp).clip(CircleShape).background(c.success))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Zyro Pay Support", style = Z.type.title, color = c.textPrimary)
                Text(if (typing) "Mia is typing…" else "Online · replies in ~1 min", style = Z.type.caption, color = if (typing) c.primary else c.success)
            }
        }
        DividerLine()
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Today", style = Z.type.caption, color = c.textTertiary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
            items(messages, key = { it.id }) { m ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start) {
                    Column(horizontalAlignment = if (m.fromUser) Alignment.End else Alignment.Start) {
                        Box(
                            Modifier.widthIn(max = 280.dp)
                                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (m.fromUser) 18.dp else 4.dp, bottomEnd = if (m.fromUser) 4.dp else 18.dp))
                                .background(if (m.fromUser) Brand.buttonGradient else SolidColor(c.surface))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) { Text(m.text, style = Z.type.bodySmall, color = if (m.fromUser) Color.White else c.textPrimary) }
                        Spacer(Modifier.height(2.dp))
                        Text(Fmt.time(m.timestamp) + if (m.fromUser) " · Read" else "", style = Z.type.caption, color = c.textTertiary)
                    }
                }
            }
            if (typing) item(key = "typing") { TypingBubble() }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Card lost or stolen", "Deposit not received", "Withdrawal pending", "KYC status", "Fees").forEach { q ->
                ChipButton(q, false) { AppRepository.sendChat(q) }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(c.surface).navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButtonZ(R.drawable.ic_add, onClick = {}, tint = c.textSecondary)
            BasicTextField(
                value = input, onValueChange = { input = it },
                textStyle = Z.type.bodySmall.copy(color = c.textPrimary),
                cursorBrush = SolidColor(c.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(c.surfaceVariant).padding(horizontal = 16.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    Box { if (input.isEmpty()) Text("Type a message…", style = Z.type.bodySmall, color = c.textTertiary); inner() }
                },
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(if (input.isBlank()) c.surfaceVariant else c.primary).clickable(enabled = input.isNotBlank()) { send() },
                contentAlignment = Alignment.Center,
            ) { ZIcon(R.drawable.ic_send, size = 20.dp, tint = if (input.isBlank()) c.textTertiary else Color.White) }
        }
    }
}

@Composable
private fun TypingBubble() {
    val c = Z.colors
    val t = rememberInfiniteTransition(label = "typing")
    Row(Modifier.clip(RoundedCornerShape(18.dp)).background(c.surface).padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { i ->
            val a by t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(500, delayMillis = i * 150), RepeatMode.Reverse), label = "dot$i")
            Box(Modifier.size(7.dp).graphicsLayer { alpha = a }.clip(CircleShape).background(c.textSecondary))
        }
    }
}

@Composable
fun AboutScreen(nav: NavController) {
    val c = Z.colors
    val messenger = LocalMessenger.current
    var checking by remember { mutableStateOf(false) }
    LaunchedEffect(checking) {
        if (checking) {
            kotlinx.coroutines.delay(1500)
            checking = false
            messenger.success("You're up to date", "Zyro Pay ${BuildConfig.VERSION_NAME} is the latest version.")
        }
    }
    ZScreen("About", onBack = { nav.back() }) {
        Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(84.dp).clip(RoundedCornerShape(24.dp)).background(Brand.heroGradient), contentAlignment = Alignment.Center) {
                ZImage(R.drawable.brand_logo_symbol_white, Modifier.size(44.dp))
            }
            Spacer(Modifier.height(14.dp))
            ZImage(if (c.isDark) R.drawable.brand_wordmark_dark else R.drawable.brand_wordmark, Modifier.height(26.dp))
            Spacer(Modifier.height(6.dp))
            Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = Z.type.caption, color = c.textSecondary)
        }
        SurfaceCard(Modifier.fillMaxWidth(), padding = 0.dp) {
            ListRow("Check for updates", icon = R.drawable.ic_refresh, trailing = { if (checking) com.zyropay.app.ui.components.BrandSpinner(Modifier.size(20.dp)) }, onClick = { checking = true }, chevron = !checking)
            DividerLine(inset = 66.dp)
            ListRow("Terms of Service", icon = R.drawable.ic_terms, onClick = { nav.navigate(Routes.legal("terms")) })
            DividerLine(inset = 66.dp)
            ListRow("Privacy Policy", icon = R.drawable.ic_privacy, onClick = { nav.navigate(Routes.legal("privacy")) })
            DividerLine(inset = 66.dp)
            ListRow("Cardholder Agreement", icon = R.drawable.ic_document, onClick = { nav.navigate(Routes.legal("card")) })
            DividerLine(inset = 66.dp)
            ListRow("Open-source licences", icon = R.drawable.ic_about, onClick = { nav.navigate(Routes.legal("licences")) })
        }
        Spacer(Modifier.height(24.dp))
        Text("© 2026 Zyro Pay Ltd. All rights reserved.\nDigital asset services are subject to local regulations.", style = Z.type.caption, color = c.textTertiary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun LegalScreen(nav: NavController, doc: String) {
    val c = Z.colors
    val (title, sections) = when (doc) {
        "privacy" -> "Privacy Policy" to listOf(
            "1. Information we collect" to "We collect information you provide when creating an account (such as your email, phone number and identity documents), information about your transactions, and technical data about the device you use.",
            "2. How we use your information" to "We use your information to provide and improve our services, verify your identity, prevent fraud and comply with legal obligations including anti-money-laundering requirements.",
            "3. Sharing" to "We share information only with service providers who help us operate (such as card issuing and identity verification partners), and with authorities where required by law. We never sell your personal data.",
            "4. Security" to "Your data is encrypted in transit and at rest. Access is restricted to authorised personnel and monitored continuously.",
            "5. Your rights" to "You may access, correct or delete your personal data, subject to legal retention requirements. Contact privacy@zyropay.app to exercise your rights.",
        )
        "card" -> "Cardholder Agreement" to listOf(
            "1. The card" to "Your Zyro Pay card is a prepaid debit card funded by converting digital assets from your Zyro Pay wallet. It is not a credit card.",
            "2. Fees" to "Card issuance fees, FX fees and ATM fees are set out in the Fee Schedule shown in the app before you confirm any action.",
            "3. Lost or stolen cards" to "Freeze your card immediately in the app and contact support. You are not liable for unauthorised transactions reported promptly.",
            "4. Limits" to "Spending and withdrawal limits depend on your verification level and may be adjusted in the app within the maximums allowed.",
        )
        "licences" -> "Open-source licences" to listOf(
            "Jetpack Compose, AndroidX" to "Apache License 2.0 — Copyright The Android Open Source Project.",
            "Kotlin & kotlinx.coroutines" to "Apache License 2.0 — Copyright JetBrains s.r.o.",
            "ZXing" to "Apache License 2.0 — Copyright ZXing authors.",
            "Inter typeface" to "SIL Open Font License 1.1 — Copyright The Inter Project Authors.",
        )
        else -> "Terms of Service" to listOf(
            "1. Acceptance" to "By creating an account or using Zyro Pay you agree to these Terms. If you do not agree, please do not use the service.",
            "2. Eligibility" to "You must be at least 18 years old and a resident of a supported country. You are responsible for keeping your login credentials and passcode secure.",
            "3. Digital assets" to "Digital asset prices are volatile. You acknowledge that the value of your holdings may go up or down and that past performance is no guarantee of future results.",
            "4. Prohibited use" to "You may not use Zyro Pay for unlawful activity, including money laundering, fraud or sanctions evasion. We may suspend accounts that breach these Terms.",
            "5. Liability" to "To the extent permitted by law, Zyro Pay is not liable for indirect losses, including losses caused by network congestion or third-party failures.",
            "6. Changes" to "We may update these Terms from time to time. We'll notify you in the app of material changes.",
        )
    }
    ZScreen(title, onBack = { nav.back() }) {
        Text("Last updated: 1 October 2026", style = Z.type.caption, color = c.textTertiary)
        Spacer(Modifier.height(12.dp))
        sections.forEach { (h, body) ->
            Text(h, style = Z.type.title, color = c.textPrimary)
            Spacer(Modifier.height(6.dp))
            Text(body, style = Z.type.bodySmall, color = c.textSecondary)
            Spacer(Modifier.height(18.dp))
        }
    }
}
