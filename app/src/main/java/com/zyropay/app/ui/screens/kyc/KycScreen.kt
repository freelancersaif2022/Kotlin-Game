package com.zyropay.app.ui.screens.kyc

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zyropay.app.R
import com.zyropay.app.data.AppRepository
import com.zyropay.app.data.KycStatus
import com.zyropay.app.navigation.Routes
import com.zyropay.app.navigation.back
import com.zyropay.app.ui.components.BrandSpinner
import com.zyropay.app.ui.components.ButtonStyle
import com.zyropay.app.ui.components.DetailRow
import com.zyropay.app.ui.components.IconCircle
import com.zyropay.app.ui.components.InfoBanner
import com.zyropay.app.ui.components.LocalMessenger
import com.zyropay.app.ui.components.SearchField
import com.zyropay.app.ui.components.SheetOption
import com.zyropay.app.ui.components.StepIndicator
import com.zyropay.app.ui.components.SurfaceCard
import com.zyropay.app.ui.components.ZBottomSheet
import com.zyropay.app.ui.components.ZButton
import com.zyropay.app.ui.components.ZIcon
import com.zyropay.app.ui.components.ZImage
import com.zyropay.app.ui.components.ZScreen
import com.zyropay.app.ui.components.ZTextField
import com.zyropay.app.ui.components.pressable
import com.zyropay.app.ui.theme.Z
import kotlinx.coroutines.delay

private val countries = listOf(
    "Hong Kong", "Singapore", "United Arab Emirates", "United Kingdom", "Bangladesh", "India", "Malaysia", "Thailand",
    "Philippines", "Indonesia", "Vietnam", "Japan", "South Korea", "Australia", "Canada", "Germany", "France", "Spain",
    "Italy", "Brazil", "Mexico", "Turkey", "Nigeria", "Kenya", "South Africa", "Pakistan", "Saudi Arabia", "Qatar",
)

@Composable
fun KycScreen(nav: NavController) {
    val c = Z.colors
    val user by AppRepository.user.collectAsState()
    val messenger = LocalMessenger.current
    // 0 intro, 1 country+doc, 2 front, 3 back, 4 selfie, 5 personal info, 6 review
    var step by rememberSaveable { mutableIntStateOf(0) }
    var country by rememberSaveable { mutableStateOf(user.country.ifBlank { "Hong Kong" }) }
    var docType by rememberSaveable { mutableStateOf("ID card") }
    var front by rememberSaveable { mutableStateOf(false) }
    var back by rememberSaveable { mutableStateOf(false) }
    var selfie by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf(user.fullName) }
    var dob by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var pickCountry by remember { mutableStateOf(false) }

    if (user.kycStatus == KycStatus.VERIFIED || user.kycStatus == KycStatus.PENDING) {
        KycStatusView(nav, user.kycStatus, user.fullName, user.country)
        return
    }
    val needsBack = docType != "Passport"
    BackHandler(step > 0) { step = if (step == 4 && !needsBack) 2 else step - 1 }
    val dobValid = Regex("""\d{2}/\d{2}/\d{4}""").matches(dob)
    ZScreen("Identity verification", onBack = { if (step > 0) step = if (step == 4 && !needsBack) 2 else step - 1 else nav.back() }, bottomBar = {
        when (step) {
            0 -> ZButton("Start verification", onClick = { step = 1 }, modifier = Modifier.fillMaxWidth())
            1 -> ZButton("Continue", onClick = { step = 2 }, modifier = Modifier.fillMaxWidth())
            2 -> ZButton(if (front) "Continue" else "Take photo", onClick = { if (front) step = if (needsBack) 3 else 4 else front = true }, modifier = Modifier.fillMaxWidth())
            3 -> ZButton(if (back) "Continue" else "Take photo", onClick = { if (back) step = 4 else back = true }, modifier = Modifier.fillMaxWidth())
            4 -> ZButton(if (selfie) "Continue" else "Start face scan", onClick = { if (selfie) step = 5 else selfie = true }, modifier = Modifier.fillMaxWidth())
            5 -> ZButton("Continue", onClick = { step = 6 }, modifier = Modifier.fillMaxWidth(), enabled = name.trim().length >= 3 && dobValid && address.length >= 6)
            else -> ZButton("Submit for review", onClick = {
                AppRepository.submitKyc(name.trim(), country, dob, address.trim())
                messenger.success("Submitted", "Your documents are being reviewed.")
                nav.navigate(Routes.result("kyc")) { popUpTo(Routes.KYC) { inclusive = true } }
            }, modifier = Modifier.fillMaxWidth())
        }
    }) {
        if (step > 0) {
            StepIndicator(step - 1, 6)
            Spacer(Modifier.height(20.dp))
        }
        AnimatedContent(step, transitionSpec = {
            if (targetState > initialState) (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
            else (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
        }, label = "kyc") { s ->
            Column(Modifier.fillMaxWidth()) {
                when (s) {
                    0 -> {
                        ZImage(R.drawable.ill_kyc_intro, Modifier.size(200.dp).align(Alignment.CenterHorizontally))
                        Text("Verify your identity", style = Z.type.h1, color = c.textPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text("We're required by law to verify your identity. It takes about 2 minutes.", style = Z.type.body, color = c.textSecondary)
                        Spacer(Modifier.height(20.dp))
                        listOf(
                            Triple(R.drawable.ic_id_card, "Government-issued ID", "ID card, passport or driver's licence"),
                            Triple(R.drawable.ic_selfie_camera, "Face verification", "A quick selfie to confirm it's you"),
                            Triple(R.drawable.ic_address, "Personal details", "Name, date of birth and address"),
                        ).forEach { (icon, t, sub) ->
                            Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconCircle(icon, size = 42.dp, background = c.primarySoft, tint = c.primary)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(t, style = Z.type.bodyMedium, color = c.textPrimary)
                                    Text(sub, style = Z.type.caption, color = c.textSecondary)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        InfoBanner("Your data is encrypted and handled according to our Privacy Policy. We never share it without your consent.", icon = R.drawable.ic_lock)
                    }
                    1 -> {
                        Text("Select your document", style = Z.type.h2, color = c.textPrimary)
                        Spacer(Modifier.height(16.dp))
                        ZTextField(country, {}, label = "Issuing country / region", readOnly = true, leadingIcon = R.drawable.ic_globe_language,
                            trailing = { ZIcon(R.drawable.ic_chevron_down, size = 18.dp, tint = c.textTertiary) }, onClick = { pickCountry = true })
                        Spacer(Modifier.height(16.dp))
                        Text("Document type", style = Z.type.label, color = c.textSecondary)
                        Spacer(Modifier.height(8.dp))
                        listOf(
                            Triple("ID card", R.drawable.ic_id_card, "Front and back"),
                            Triple("Passport", R.drawable.ic_passport, "Photo page only"),
                            Triple("Driver's licence", R.drawable.ic_card, "Front and back"),
                        ).forEach { (t, icon, sub) ->
                            val sel = docType == t
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(16.dp))
                                    .background(if (sel) c.primarySoft else c.surface)
                                    .border(1.5.dp, if (sel) c.primary else c.outline, RoundedCornerShape(16.dp))
                                    .pressable(scaleDown = 0.985f) { docType = t }.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconCircle(icon, size = 40.dp, background = if (sel) c.surface else c.surfaceVariant, tint = if (sel) c.primary else c.textSecondary)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(t, style = Z.type.bodyMedium, color = c.textPrimary)
                                    Text(sub, style = Z.type.caption, color = c.textSecondary)
                                }
                                if (sel) ZIcon(R.drawable.ic_check_circle, size = 22.dp, tint = c.primary)
                            }
                        }
                    }
                    2, 3 -> CaptureStep(
                        title = if (s == 2) "Front of $docType" else "Back of $docType",
                        hint = "Place your document inside the frame. Make sure all corners are visible and there's no glare.",
                        captured = if (s == 2) front else back,
                        onCaptured = {},
                        illustration = R.drawable.ill_kyc_id_scan,
                        selfie = false,
                    )
                    4 -> CaptureStep(
                        title = "Face verification",
                        hint = "Position your face in the circle, look straight ahead and blink slowly.",
                        captured = selfie, onCaptured = {}, illustration = R.drawable.ill_kyc_selfie, selfie = true,
                    )
                    5 -> {
                        Text("Personal details", style = Z.type.h2, color = c.textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text("Must match the information on your document.", style = Z.type.bodySmall, color = c.textSecondary)
                        Spacer(Modifier.height(16.dp))
                        ZTextField(name, { name = it }, label = "Full legal name", placeholder = "e.g. Chan Tai Man", leadingIcon = R.drawable.ic_user)
                        Spacer(Modifier.height(12.dp))
                        ZTextField(dob, { v -> dob = formatDob(v) }, label = "Date of birth", placeholder = "DD/MM/YYYY", leadingIcon = R.drawable.ic_calendar,
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number, error = if (dob.length == 10 && !dobValid) "Use DD/MM/YYYY" else null)
                        Spacer(Modifier.height(12.dp))
                        ZTextField(address, { address = it }, label = "Residential address", placeholder = "Street, city, postcode", leadingIcon = R.drawable.ic_location, singleLine = false, minHeight = 80.dp)
                    }
                    else -> {
                        Text("Review your information", style = Z.type.h2, color = c.textPrimary)
                        Spacer(Modifier.height(16.dp))
                        SurfaceCard(Modifier.fillMaxWidth()) {
                            DetailRow("Country", country)
                            DetailRow("Document", docType)
                            DetailRow("Full name", name)
                            DetailRow("Date of birth", dob)
                            DetailRow("Address", address)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ID front" to front, "ID back" to (back || !needsBack), "Selfie" to selfie).forEach { (l, ok) ->
                                Row(
                                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (ok) c.successSoft else c.surfaceVariant).padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    ZIcon(R.drawable.ic_check_circle, size = 16.dp, tint = c.success)
                                    Spacer(Modifier.width(6.dp))
                                    Text(l, style = Z.type.caption, color = c.textPrimary)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("By submitting, you confirm the information is accurate and consent to identity checks by our verification partner.", style = Z.type.caption, color = c.textTertiary)
                    }
                }
            }
        }
    }
    if (pickCountry) {
        var q by remember { mutableStateOf("") }
        ZBottomSheet({ pickCountry = false }, title = "Country / region") { close ->
            SearchField(q, { q = it }, Modifier.padding(horizontal = 20.dp), placeholder = "Search")
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                items(countries.filter { q.isBlank() || it.contains(q, true) }) { name ->
                    SheetOption(name, selected = name == country) { close { country = name } }
                }
            }
        }
    }
}

private fun formatDob(input: String): String {
    val d = input.filter { it.isDigit() }.take(8)
    return buildString {
        d.forEachIndexed { i, ch ->
            if (i == 2 || i == 4) append('/')
            append(ch)
        }
    }
}

@Composable
private fun CaptureStep(title: String, hint: String, captured: Boolean, onCaptured: () -> Unit, illustration: Int, selfie: Boolean) {
    val c = Z.colors
    var analysing by remember(captured) { mutableStateOf(captured) }
    LaunchedEffect(captured) { if (captured) { delay(1600); analysing = false } }
    val t = rememberInfiniteTransition(label = "capture")
    val sweep by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse), label = "sweep")
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = Z.type.h2, color = c.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text(hint, style = Z.type.bodySmall, color = c.textSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier.fillMaxWidth().aspectRatio(if (selfie) 1f else 1.4f).clip(RoundedCornerShape(24.dp)).background(Color(0xFF12131A)),
            contentAlignment = Alignment.Center,
        ) {
            ZImage(illustration, Modifier.fillMaxSize(0.8f))
            val accent = if (captured && !analysing) c.success else Color(0xFF8F5BFF)
            Canvas(Modifier.fillMaxSize().padding(18.dp)) {
                if (selfie) {
                    drawCircle(accent, size.minDimension / 2.2f, style = Stroke(3.dp.toPx(), pathEffect = if (captured) null else PathEffect.dashPathEffect(floatArrayOf(18f, 12f))))
                } else {
                    drawRoundRect(accent, style = Stroke(3.dp.toPx(), pathEffect = if (captured) null else PathEffect.dashPathEffect(floatArrayOf(22f, 14f))), cornerRadius = CornerRadius(16.dp.toPx()))
                }
                if (!captured || analysing) {
                    val y = size.height * sweep
                    drawLine(Color(0xFF00D1FF), Offset(0f, y), Offset(size.width, y), 2.dp.toPx())
                }
            }
            if (captured) {
                Box(Modifier.align(Alignment.BottomCenter).padding(14.dp).clip(RoundedCornerShape(50)).background(Color(0xE6FFFFFF)).padding(horizontal = 14.dp, vertical = 7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (analysing) BrandSpinner(Modifier.size(16.dp)) else ZIcon(R.drawable.ic_check_circle, size = 16.dp, tint = c.success)
                        Spacer(Modifier.width(6.dp))
                        Text(if (analysing) "Analysing image…" else if (selfie) "Liveness confirmed" else "Document captured", style = Z.type.label, color = Color(0xFF0B0F19))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf("Good lighting", "No glare", "Not blurry").forEach { tip ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ZIcon(R.drawable.ic_check, size = 14.dp, tint = c.success)
                    Spacer(Modifier.width(4.dp))
                    Text(tip, style = Z.type.caption, color = c.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun KycStatusView(nav: NavController, status: KycStatus, name: String, country: String) {
    val c = Z.colors
    ZScreen("Identity verification", onBack = { nav.back() }, bottomBar = {
        ZButton(if (status == KycStatus.VERIFIED) "Apply for a card" else "Back to home", onClick = {
            if (status == KycStatus.VERIFIED) nav.navigate(Routes.CARD_APPLY) { popUpTo(Routes.KYC) { inclusive = true } } else nav.back()
        }, modifier = Modifier.fillMaxWidth(), style = if (status == KycStatus.VERIFIED) ButtonStyle.Primary else ButtonStyle.Secondary)
    }) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            ZImage(if (status == KycStatus.VERIFIED) R.drawable.ill_kyc_approved else R.drawable.ill_kyc_review, Modifier.size(210.dp))
            Text(if (status == KycStatus.VERIFIED) "You're verified" else "Verification in review", style = Z.type.h1, color = c.textPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                if (status == KycStatus.VERIFIED) "Your identity has been confirmed. Enjoy higher limits and full access to Zyro Pay cards." else "We're checking your documents. This usually takes a few minutes — we'll notify you as soon as it's done.",
                style = Z.type.body, color = c.textSecondary, textAlign = TextAlign.Center,
            )
            if (status == KycStatus.PENDING) {
                Spacer(Modifier.height(16.dp))
                BrandSpinner(Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        SurfaceCard(Modifier.fillMaxWidth()) {
            DetailRow("Level", if (status == KycStatus.VERIFIED) "Level 2 · Advanced" else "Level 1 · Basic")
            DetailRow("Name", name.ifBlank { "—" })
            DetailRow("Country", country.ifBlank { "—" })
            DetailRow("Daily card limit", if (status == KycStatus.VERIFIED) "$20,000" else "$0")
            DetailRow("Daily withdrawal", if (status == KycStatus.VERIFIED) "$1,000,000" else "$0")
        }
    }
}
