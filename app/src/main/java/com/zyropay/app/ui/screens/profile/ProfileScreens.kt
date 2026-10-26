package com.zyropay.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zyropay.app.R
import com.zyropay.app.data.AppRepository
import com.zyropay.app.data.Fmt
import com.zyropay.app.data.KycStatus
import com.zyropay.app.data.MockData
import com.zyropay.app.data.ThemeMode
import com.zyropay.app.navigation.Routes
import com.zyropay.app.navigation.back
import com.zyropay.app.ui.components.Avatar
import com.zyropay.app.ui.components.Biometrics
import com.zyropay.app.ui.components.ButtonSize
import com.zyropay.app.ui.components.ButtonStyle
import com.zyropay.app.ui.components.DetailRow
import com.zyropay.app.ui.components.DividerLine
import com.zyropay.app.ui.components.IconCircle
import com.zyropay.app.ui.components.InfoBanner
import com.zyropay.app.ui.components.KycPill
import com.zyropay.app.ui.components.ListRow
import com.zyropay.app.ui.components.LocalMessenger
import com.zyropay.app.ui.components.OtpInput
import com.zyropay.app.ui.components.PasswordField
import com.zyropay.app.ui.components.PasswordStrength
import com.zyropay.app.ui.components.Pill
import com.zyropay.app.ui.components.ProgressBar
import com.zyropay.app.ui.components.QrCode
import com.zyropay.app.ui.components.SearchField
import com.zyropay.app.ui.components.SheetOption
import com.zyropay.app.ui.components.SurfaceCard
import com.zyropay.app.ui.components.SwitchRow
import com.zyropay.app.ui.components.ZButton
import com.zyropay.app.ui.components.ZDialog
import com.zyropay.app.ui.components.ZIcon
import com.zyropay.app.ui.components.ZImage
import com.zyropay.app.ui.components.ZScreen
import com.zyropay.app.ui.components.ZTextField
import com.zyropay.app.ui.components.isStrongPassword
import com.zyropay.app.ui.components.pressable
import com.zyropay.app.ui.components.rememberAuthGate
import com.zyropay.app.ui.components.rememberCopier
import com.zyropay.app.ui.components.shareText
import com.zyropay.app.ui.theme.Brand
import com.zyropay.app.ui.theme.Z
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ------------------------------------------------------------------ personal info

@Composable
fun PersonalInfoScreen(nav: NavController) {
    val c = Z.colors
    val user by AppRepository.user.collectAsState()
    val messenger = LocalMessenger.current
    val copy = rememberCopier()
    var nickname by remember { mutableStateOf(user.nickname) }
    var avatar by remember { mutableIntStateOf(user.avatar) }
    var pickAvatar by remember { mutableStateOf(false) }
    val changed = nickname != user.nickname || avatar != user.avatar
    ZScreen("Personal information", onBack = { nav.back() }, bottomBar = {
        ZButton("Save changes", onClick = {
            AppRepository.updateUser { it.copy(nickname = nickname.trim(), avatar = avatar) }
            messenger.success("Profile updated")
            nav.back()
        }, modifier = Modifier.fillMaxWidth(), enabled = changed && nickname.isNotBlank())
    }) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.pressable { pickAvatar = true }) {
                Avatar(avatar, size = 92.dp)
                Box(Modifier.align(Alignment.BottomEnd).size(28.dp).clip(CircleShape).background(c.primary), contentAlignment = Alignment.Center) {
                    ZIcon(R.drawable.ic_camera, size = 15.dp, tint = Color.White)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Tap to change avatar", style = Z.type.caption, color = c.textTertiary)
        }
        ZTextField(nickname, { nickname = it.take(24) }, label = "Nickname", placeholder = "Your display name", leadingIcon = R.drawable.ic_user)
        Spacer(Modifier.height(20.dp))
        SurfaceCard(Modifier.fillMaxWidth()) {
            DetailRow("UID", user.uid, copyable = true, onCopy = { copy("UID", user.uid) })
            DetailRow("Legal name", user.fullName.ifBlank { "Not verified" }, valueColor = if (user.fullName.isBlank()) c.textTertiary else c.textPrimary)
            DetailRow("Email", user.email.ifBlank { "Not linked" }, valueColor = if (user.email.isBlank()) c.textTertiary else c.textPrimary)
            DetailRow("Phone", user.phone.ifBlank { "Not linked" }, valueColor = if (user.phone.isBlank()) c.textTertiary else c.textPrimary)
            DetailRow("Country / region", user.country.ifBlank { "—" })
            DetailRow("Member since", Fmt.monthYear(user.joinedAt))
            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Verification", style = Z.type.bodySmall, color = c.textSecondary, modifier = Modifier.weight(1f))
                KycPill(user.kycStatus)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Legal name and country come from your verified ID and can't be edited.", style = Z.type.caption, color = c.textTertiary)
    }
    if (pickAvatar) {
        com.zyropay.app.ui.components.ZBottomSheet({ pickAvatar = false }, title = "Choose avatar") { close ->
            Column(Modifier.padding(horizontal = 20.dp)) {
                MockData.avatars.indices.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { i ->
                            Box(
                                Modifier.size(68.dp).clip(CircleShape).border(3.dp, if (i == avatar) c.primary else Color.Transparent, CircleShape)
                                    .pressable { close { avatar = i } }.padding(5.dp),
                            ) { Avatar(i, size = 58.dp) }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ------------------------------------------------------------------ security center

@Composable
fun SecurityScreen(nav: NavController) {
    val c = Z.colors
    val security by AppRepository.security.collectAsState()
    val user by AppRepository.user.collectAsState()
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    val gate = rememberAuthGate()
    val score = listOf(security.passcode.isNotEmpty(), security.biometricEnabled, security.twoFaEnabled, security.antiPhishingCode.isNotEmpty(), user.kycStatus == KycStatus.VERIFIED).count { it }
    ZScreen("Security center", onBack = { nav.back() }) {
        SurfaceCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Security level", style = Z.type.bodySmall, color = c.textSecondary)
                    Text(when { score >= 5 -> "Excellent"; score >= 4 -> "Strong"; score >= 3 -> "Medium"; else -> "Weak" }, style = Z.type.h2, color = when { score >= 4 -> c.success; score >= 3 -> c.warning; else -> c.error })
                }
                ZImage(R.drawable.ill_security_setup, Modifier.size(72.dp))
            }
            Spacer(Modifier.height(10.dp))
            ProgressBar(score / 5f, color = when { score >= 4 -> c.success; score >= 3 -> c.warning; else -> c.error })
            Spacer(Modifier.height(8.dp))
            Text("$score of 5 protections enabled", style = Z.type.caption, color = c.textSecondary)
        }
        Spacer(Modifier.height(16.dp))
        Text("Login & unlock", style = Z.type.label, color = c.textSecondary)
        Spacer(Modifier.height(8.dp))
        SurfaceCard(Modifier.fillMaxWidth(), padding = 0.dp) {
            ListRow("Change passcode", icon = R.drawable.ic_passcode, onClick = { nav.navigate(Routes.passcode("change")) })
            DividerLine(inset = 66.dp)
            SwitchRow("Biometric unlock", security.biometricEnabled, { on ->
                if (on) {
                    if (!Biometrics.available(context)) messenger.warning("Not available", "Set up a fingerprint or face unlock in your phone settings first.")
                    else Biometrics.authenticate(context, "Enable biometrics", "Confirm to enable", onSuccess = { AppRepository.setBiometric(true); messenger.success("Biometrics enabled") })
                } else gate.request("Disable biometrics") { AppRepository.setBiometric(false); messenger.toast("Biometrics disabled") }
            }, icon = R.drawable.ic_fingerprint, subtitle = "Fingerprint or face recognition")
            DividerLine(inset = 66.dp)
            ListRow("Login password", icon = R.drawable.ic_key, onClick = { nav.navigate(Routes.CHANGE_PASSWORD) })
        }
        Spacer(Modifier.height(16.dp))
        Text("Advanced protection", style = Z.type.label, color = c.textSecondary)
        Spacer(Modifier.height(8.dp))
        SurfaceCard(Modifier.fillMaxWidth(), padding = 0.dp) {
            ListRow("Google Authenticator (2FA)", icon = R.drawable.ic_two_fa, value = if (security.twoFaEnabled) "On" else "Off", valueColor = if (security.twoFaEnabled) c.success else c.textTertiary, onClick = { nav.navigate(Routes.TWO_FA) })
            DividerLine(inset = 66.dp)
            ListRow("Anti-phishing code", icon = R.drawable.ic_shield, value = if (security.antiPhishingCode.isNotEmpty()) "Set" else "Not set", onClick = { nav.navigate(Routes.ANTI_PHISHING) })
            DividerLine(inset = 66.dp)
            SwitchRow("Withdrawal whitelist", security.withdrawWhitelist, { v -> gate.request("Update whitelist") { AppRepository.setWhitelist(v); messenger.toast(if (v) "Whitelist enabled" else "Whitelist disabled") } }, icon = R.drawable.ic_lock, subtitle = "Only withdraw to saved addresses")
            DividerLine(inset = 66.dp)
            ListRow("Device management", icon = R.drawable.ic_device, onClick = { nav.navigate(Routes.DEVICES) })
        }
        Spacer(Modifier.height(16.dp))
        SurfaceCard(Modifier.fillMaxWidth(), padding = 0.dp) {
            ListRow("Freeze account", subtitle = "Instantly lock all activity if you suspect fraud", icon = R.drawable.ic_warning, iconTint = c.error, iconBg = c.errorSoft, titleColor = c.error, onClick = {
                messenger.warning("Contact support to freeze", "For your safety, account freezes are confirmed by our security team.")
                nav.navigate(Routes.CHAT)
            })
        }
    }
}

@Composable
fun ChangePasswordScreen(nav: NavController) {
    val messenger = LocalMessenger.current
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    ZScreen("Change password", onBack = { nav.back() }, bottomBar = {
        ZButton("Update password", onClick = {
            if (AppRepository.security.value.password.isNotEmpty() && AppRepository.security.value.password != current) {
                error = "Current password is incorrect"; return@ZButton
            }
            loading = true
            scope.launch {
                delay(1000)
                AppRepository.changePassword(new)
                loading = false
                messenger.success("Password changed", "Withdrawals are paused for 24 hours for your safety.")
                nav.back()
            }
        }, modifier = Modifier.fillMaxWidth(), loading = loading, enabled = current.isNotEmpty() && isStrongPassword(new) && new == confirm)
    }) {
        PasswordField(current, { current = it; error = null }, label = "Current password", placeholder = "Enter current password", error = error, imeAction = androidx.compose.ui.text.input.ImeAction.Next)
        Spacer(Modifier.height(16.dp))
        PasswordField(new, { new = it }, label = "New password", placeholder = "Create new password", imeAction = androidx.compose.ui.text.input.ImeAction.Next)
        Spacer(Modifier.height(10.dp))
        PasswordStrength(new)
        Spacer(Modifier.height(16.dp))
        PasswordField(confirm, { confirm = it }, label = "Confirm new password", placeholder = "Re-enter new password", error = if (confirm.isNotEmpty() && confirm != new) "Passwords do not match" else null)
    }
}

@Composable
fun TwoFaScreen(nav: NavController) {
    val c = Z.colors
    val security by AppRepository.security.collectAsState()
    val user by AppRepository.user.collectAsState()
    val messenger = LocalMessenger.current
    val copy = rememberCopier()
    val gate = rememberAuthGate()
    val secret = remember { security.twoFaSecret.ifBlank { (1..16).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"[kotlin.random.Random.nextInt(32)] }.joinToString("") } }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    if (security.twoFaEnabled) {
        ZScreen("Two-factor authentication", onBack = { nav.back() }) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                ZImage(R.drawable.ill_security_setup, Modifier.size(170.dp))
                Text("2FA is enabled", style = Z.type.h2, color = c.textPrimary)
                Spacer(Modifier.height(6.dp))
                Text("Google Authenticator protects your logins and withdrawals.", style = Z.type.bodySmall, color = c.textSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                ZButton("Disable 2FA", onClick = { gate.request("Disable 2FA") { AppRepository.setTwoFa(false); messenger.warning("2FA disabled", "Your account is less secure now.") } }, style = ButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
            }
        }
        return
    }
    LaunchedEffect(code) {
        if (code.length == 6) {
            delay(800)
            if (code.toSet().size > 1) {
                AppRepository.setTwoFa(true, secret)
                messenger.success("2FA enabled", "Your account security is now strong.")
                nav.back()
            } else {
                error = true; messenger.error("Invalid code", "Enter the current 6-digit code from your authenticator app."); delay(500); code = ""; error = false
            }
        }
    }
    ZScreen("Enable 2FA", onBack = { nav.back() }) {
        StepTitle(1, "Install an authenticator app", "Google Authenticator, Microsoft Authenticator or Authy.")
        StepTitle(2, "Scan this QR code or enter the key", null)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(132.dp).clip(RoundedCornerShape(14.dp)).background(Color.White).border(1.dp, c.outline, RoundedCornerShape(14.dp)).padding(8.dp)) {
                QrCode("otpauth://totp/ZyroPay:${user.email.ifBlank { user.uid }}?secret=$secret&issuer=ZyroPay", Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Setup key", style = Z.type.caption, color = c.textSecondary)
                Text(secret.chunked(4).joinToString(" "), style = Z.type.bodySmallMedium.copy(fontFeatureSettings = "tnum"), color = c.textPrimary)
                Spacer(Modifier.height(8.dp))
                ZButton("Copy key", onClick = { copy("Key", secret) }, size = ButtonSize.Small, style = ButtonStyle.Secondary, icon = R.drawable.ic_copy)
            }
        }
        Spacer(Modifier.height(12.dp))
        com.zyropay.app.ui.components.WarningBanner("Save this key somewhere safe. You'll need it to recover 2FA if you lose your phone.")
        Spacer(Modifier.height(16.dp))
        StepTitle(3, "Enter the 6-digit code", null)
        Spacer(Modifier.height(8.dp))
        OtpInput(code, { code = it }, error = error)
    }
}

@Composable
private fun StepTitle(n: Int, title: String, subtitle: String?) {
    val c = Z.colors
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(c.primarySoft), contentAlignment = Alignment.Center) {
            Text("$n", style = Z.type.captionMedium, color = c.primary)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = Z.type.bodyMedium, color = c.textPrimary)
            if (subtitle != null) Text(subtitle, style = Z.type.caption, color = c.textSecondary)
        }
    }
}

@Composable
fun DevicesScreen(nav: NavController) {
    val c = Z.colors
    val devices by AppRepository.devices.collectAsState()
    val messenger = LocalMessenger.current
    var remove by remember { mutableStateOf<String?>(null) }
    ZScreen("Device management", onBack = { nav.back() }) {
        Text("Devices that have logged in to your account in the last 30 days.", style = Z.type.bodySmall, color = c.textSecondary)
        Spacer(Modifier.height(16.dp))
        devices.forEach { d ->
            SurfaceCard(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconCircle(R.drawable.ic_device, background = if (d.current) c.primarySoft else c.surfaceVariant, tint = if (d.current) c.primary else c.textSecondary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(d.name, style = Z.type.bodyMedium, color = c.textPrimary)
                            if (d.current) { Spacer(Modifier.width(6.dp)); Pill("Current") }
                        }
                        Text(d.location, style = Z.type.caption, color = c.textSecondary)
                        Text(if (d.current) "Active now" else "Last active ${Fmt.relative(d.lastActive)}", style = Z.type.caption, color = c.textTertiary)
                    }
                    if (!d.current) ZButton("Remove", onClick = { remove = d.id }, size = ButtonSize.Small, style = ButtonStyle.Outline)
                }
            }
        }
    }
    remove?.let { id ->
        ZDialog(onDismiss = { remove = null }, title = "Remove device?", message = "This device will be logged out immediately and will need to verify again to sign in.",
            confirmText = "Remove", destructive = true, icon = R.drawable.ic_device, iconTint = c.error, iconBg = c.errorSoft,
            onConfirm = { AppRepository.removeDevice(id); remove = null; messenger.success("Device removed", "The device has been logged out.") })
    }
}

@Composable
fun AntiPhishingScreen(nav: NavController) {
    val security by AppRepository.security.collectAsState()
    val messenger = LocalMessenger.current
    val gate = rememberAuthGate()
    var code by remember { mutableStateOf(security.antiPhishingCode) }
    val valid = code.length in 4..20 && code.all { it.isLetterOrDigit() || it == '_' }
    ZScreen("Anti-phishing code", onBack = { nav.back() }, bottomBar = {
        ZButton("Save code", onClick = { gate.request("Set anti-phishing code") { AppRepository.setAntiPhishing(code); messenger.success("Anti-phishing code set", "All genuine Zyro Pay emails will now include it."); nav.back() } }, modifier = Modifier.fillMaxWidth(), enabled = valid && code != security.antiPhishingCode)
    }) {
        ZImage(R.drawable.ill_security_setup, Modifier.size(150.dp).align(Alignment.CenterHorizontally))
        Text("Recognise real Zyro Pay emails", style = Z.type.h3, color = Z.colors.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text("Once set, this code will appear in every email we send you. If an email doesn't contain it, it's a phishing attempt.", style = Z.type.bodySmall, color = Z.colors.textSecondary)
        Spacer(Modifier.height(20.dp))
        ZTextField(code, { code = it.take(20) }, label = "Your code", placeholder = "4–20 letters or numbers", error = if (code.isNotEmpty() && !valid) "Use 4–20 letters, numbers or _" else null, leadingIcon = R.drawable.ic_shield)
    }
}

// ------------------------------------------------------------------ settings

@Composable
fun SettingsScreen(nav: NavController) {
    val c = Z.colors
    val prefs by AppRepository.prefs.collectAsState()
    val messenger = LocalMessenger.current
    var clearing by remember { mutableStateOf(false) }
    ZScreen("Appearance & settings", onBack = { nav.back() }) {
        Text("Theme", style = Z.type.label, color = c.textSecondary)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ThemeOption("Light", ThemeMode.LIGHT, prefs.themeMode, Modifier.weight(1f))
            ThemeOption("Dark", ThemeMode.DARK, prefs.themeMode, Modifier.weight(1f))
            ThemeOption("System", ThemeMode.SYSTEM, prefs.themeMode, Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        SurfaceCard(Modifier.fillMaxWidth(), padding = 0.dp) {
            SwitchRow("Hide balances", prefs.hideBalance, { v -> AppRepository.updatePrefs { it.copy(hideBalance = v) } }, icon = R.drawable.ic_eye_off, subtitle = "Mask amounts on home and wallet")
            DividerLine(inset = 66.dp)
            SwitchRow("Haptic feedback", prefs.haptics, { v -> AppRepository.updatePrefs { it.copy(haptics = v) } }, icon = R.drawable.ic_device)
            DividerLine(inset = 66.dp)
            ListRow("Language", icon = R.drawable.ic_globe_language, value = prefs.language, onClick = { nav.navigate(Routes.LANGUAGE) })
            DividerLine(inset = 66.dp)
            ListRow("Display currency", icon = R.drawable.ic_percent, value = prefs.currency, onClick = { nav.navigate(Routes.CURRENCY) })
            DividerLine(inset = 66.dp)
            ListRow("Clear cache", icon = R.drawable.ic_trash, value = "12.4 MB", onClick = { clearing = true })
        }
    }
    if (clearing) {
        ZDialog(onDismiss = { clearing = false }, title = "Clear cache?", message = "Temporary images and data will be removed. Your account and settings are not affected.",
            confirmText = "Clear", icon = R.drawable.ic_trash, onConfirm = { clearing = false; messenger.success("Cache cleared", "12.4 MB freed") })
    }
}

@Composable
private fun ThemeOption(label: String, mode: ThemeMode, current: ThemeMode, modifier: Modifier) {
    val c = Z.colors
    val selected = mode == current
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).border(2.dp, if (selected) c.primary else c.outline, RoundedCornerShape(16.dp))
            .pressable { AppRepository.updatePrefs { it.copy(themeMode = mode) } }.padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val (bg, card, line) = when (mode) {
            ThemeMode.LIGHT -> Triple(Color(0xFFF7F8FA), Color.White, Color(0xFFE4E7EC))
            ThemeMode.DARK -> Triple(Color(0xFF0A0B10), Color(0xFF1C1F2B), Color(0xFF2A2E3D))
            ThemeMode.SYSTEM -> Triple(Color(0xFF8C90A0), Color(0xFFDADDE6), Color(0xFF5D6679))
        }
        Column(Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(10.dp)).background(bg).padding(8.dp)) {
            Box(Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(5.dp)).background(Brand.heroGradient))
            Spacer(Modifier.height(6.dp))
            repeat(3) {
                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(3.dp)).background(card))
                Spacer(Modifier.height(4.dp))
            }
            Box(Modifier.fillMaxWidth(0.6f).height(4.dp).background(line))
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(16.dp).clip(CircleShape).border(1.5.dp, if (selected) c.primary else c.textTertiary, CircleShape).padding(3.dp).clip(CircleShape).background(if (selected) c.primary else Color.Transparent))
            Spacer(Modifier.width(6.dp))
            Text(label, style = Z.type.label, color = c.textPrimary)
        }
    }
}

@Composable
fun LanguageScreen(nav: NavController) {
    val prefs by AppRepository.prefs.collectAsState()
    val messenger = LocalMessenger.current
    var query by remember { mutableStateOf("") }
    ZScreen("Language", onBack = { nav.back() }, scrollable = false) {
        SearchField(query, { query = it }, Modifier.padding(horizontal = 20.dp), placeholder = "Search language")
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(MockData.languages.filter { query.isBlank() || it.first.contains(query, true) || it.second.contains(query, true) }, key = { it.first }) { (native, english) ->
                SheetOption(native, subtitle = english, selected = prefs.language == native) {
                    AppRepository.updatePrefs { it.copy(language = native) }
                    if (native != "English") messenger.info("Language set to $native", "Full translation is rolling out — some screens remain in English.")
                    nav.back()
                }
            }
        }
    }
}

@Composable
fun CurrencyScreen(nav: NavController) {
    val prefs by AppRepository.prefs.collectAsState()
    val messenger = LocalMessenger.current
    ZScreen("Display currency", onBack = { nav.back() }, scrollable = false) {
        Text("Balances and prices will be shown in this currency.", style = Z.type.bodySmall, color = Z.colors.textSecondary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(MockData.fiats, key = { it.code }) { f ->
                SheetOption(f.code, subtitle = f.name, selected = prefs.currency == f.code, leading = { ZImage(f.flag, Modifier.size(32.dp).clip(CircleShape)) }) {
                    AppRepository.updatePrefs { it.copy(currency = f.code) }
                    messenger.toast("Currency set to ${f.code}")
                    nav.back()
                }
            }
        }
    }
}

@Composable
fun NotificationSettingsScreen(nav: NavController) {
    val prefs by AppRepository.prefs.collectAsState()
    ZScreen("Notification settings", onBack = { nav.back() }) {
        SurfaceCard(Modifier.fillMaxWidth(), padding = 0.dp) {
            SwitchRow("Transactions", prefs.pushTransactions, { v -> AppRepository.updatePrefs { it.copy(pushTransactions = v) } }, subtitle = "Card payments, deposits, withdrawals", icon = R.drawable.ic_transfer)
            DividerLine(inset = 66.dp)
            SwitchRow("Security alerts", prefs.pushSecurity, { v -> AppRepository.updatePrefs { it.copy(pushSecurity = v) } }, subtitle = "Logins, password and device changes", icon = R.drawable.ic_shield)
            DividerLine(inset = 66.dp)
            SwitchRow("Promotions", prefs.pushPromotions, { v -> AppRepository.updatePrefs { it.copy(pushPromotions = v) } }, subtitle = "Rewards, cashback and offers", icon = R.drawable.ic_gift)
            DividerLine(inset = 66.dp)
            SwitchRow("Email notifications", prefs.emailNotifications, { v -> AppRepository.updatePrefs { it.copy(emailNotifications = v) } }, subtitle = "Monthly statements and receipts", icon = R.drawable.ic_email)
        }
        Spacer(Modifier.height(12.dp))
        InfoBanner("Security alerts are always sent by email even if push notifications are off.")
    }
}

// ------------------------------------------------------------------ rewards & referral

@Composable
fun RewardsScreen(nav: NavController) {
    val c = Z.colors
    val user by AppRepository.user.collectAsState()
    val txs by AppRepository.transactions.collectAsState()
    val cards by AppRepository.cards.collectAsState()
    val security by AppRepository.security.collectAsState()
    val messenger = LocalMessenger.current
    val earned = txs.filter { it.type == com.zyropay.app.data.TxType.CASHBACK || it.type == com.zyropay.app.data.TxType.REWARD }.sumOf { it.usdValue }
    val spent = txs.filter { it.type == com.zyropay.app.data.TxType.CARD_PAYMENT }.sumOf { it.usdValue }
    ZScreen("Rewards", onBack = { nav.back() }) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))) {
            ZImage(R.drawable.bg_rewards_header, Modifier.matchParentSize(), ContentScale.Crop)
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Total rewards earned", style = Z.type.bodySmall, color = Color.White.copy(alpha = 0.85f))
                    Text(Fmt.fiat(earned), style = Z.type.h1.copy(fontFeatureSettings = "tnum"), color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Pill("VIP ${user.vipLevel} · ${if (user.vipLevel >= 2) "2%" else "1%"} cashback", color = Color.White, background = Color.White.copy(alpha = 0.2f), icon = R.drawable.ic_crown)
                }
                ZImage(R.drawable.ill_rewards_trophy, Modifier.size(96.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("VIP progress", style = Z.type.title, color = c.textPrimary)
        Spacer(Modifier.height(8.dp))
        SurfaceCard(Modifier.fillMaxWidth()) {
            val target = if (user.vipLevel >= 2) 10_000.0 else 1_000.0
            Row {
                Text("VIP ${user.vipLevel}", style = Z.type.bodySmallMedium, color = c.textPrimary, modifier = Modifier.weight(1f))
                Text("VIP ${user.vipLevel + 1}", style = Z.type.bodySmallMedium, color = c.textSecondary)
            }
            Spacer(Modifier.height(8.dp))
            ProgressBar((spent / target).toFloat(), color = Brand.Gold)
            Spacer(Modifier.height(8.dp))
            Text("Spend ${Fmt.fiat((target - spent).coerceAtLeast(0.0))} more this month to reach VIP ${user.vipLevel + 1} and unlock up to 3% cashback.", style = Z.type.caption, color = c.textSecondary)
        }
        Spacer(Modifier.height(20.dp))
        Text("Tasks", style = Z.type.title, color = c.textPrimary)
        Spacer(Modifier.height(8.dp))
        val tasks = listOf(
            Triple("Verify your identity", "+5 USDT", user.kycStatus == KycStatus.VERIFIED) to Routes.KYC,
            Triple("Get your first card", "+5 USDT", cards.isNotEmpty()) to Routes.CARD_APPLY,
            Triple("Enable 2FA", "+2 USDT", security.twoFaEnabled) to Routes.TWO_FA,
            Triple("Make 3 card payments", "+10 USDT", txs.count { it.type == com.zyropay.app.data.TxType.CARD_PAYMENT } >= 3) to Routes.SCAN,
            Triple("Invite a friend", "+5 USDT", false) to Routes.REFERRAL,
        )
        SurfaceCard(Modifier.fillMaxWidth(), padding = 0.dp) {
            tasks.forEachIndexed { i, (task, route) ->
                val (title, reward, done) = task
                ListRow(
                    title, subtitle = reward, icon = if (done) R.drawable.ic_check_circle else R.drawable.ic_star,
                    iconTint = if (done) c.success else c.warning, iconBg = if (done) c.successSoft else c.warningSoft,
                    trailing = {
                        if (done) Text("Done", style = Z.type.label, color = c.success)
                        else ZButton("Go", onClick = { nav.navigate(route) }, size = ButtonSize.Small, style = ButtonStyle.Secondary)
                    },
                    onClick = { if (done) messenger.toast("Already completed") else nav.navigate(route) },
                )
                if (i < tasks.lastIndex) DividerLine(inset = 66.dp)
            }
        }
        Spacer(Modifier.height(20.dp))
        InfoBanner("Cashback is paid in USDT instantly after each eligible card purchase. Rewards terms apply.")
    }
}

@Composable
fun ReferralScreen(nav: NavController) {
    val c = Z.colors
    val user by AppRepository.user.collectAsState()
    val referrals by AppRepository.referrals.collectAsState()
    val copy = rememberCopier()
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    val link = "https://zyropay.app/r/${user.referralCode}"
    ZScreen("Invite friends", onBack = { nav.back() }, bottomBar = {
        ZButton("Invite friends", onClick = { context.shareText("Join me on Zyro Pay and spend crypto anywhere 🌍 Sign up with my code ${user.referralCode} and we both get 5 USDT: $link") }, modifier = Modifier.fillMaxWidth(), icon = R.drawable.ic_share)
    }) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            ZImage(R.drawable.ill_referral_gift, Modifier.size(170.dp))
            Text("Invite friends, earn 5 USDT", style = Z.type.h2, color = c.textPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text("You and your friend both get 5 USDT once they verify their identity. No limits.", style = Z.type.bodySmall, color = c.textSecondary, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(20.dp))
        SurfaceCard(Modifier.fillMaxWidth()) {
            Text("Your referral code", style = Z.type.caption, color = c.textSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(user.referralCode, style = Z.type.h2.copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified), color = c.primary, modifier = Modifier.weight(1f))
                ZButton("Copy", onClick = { copy("Referral code", user.referralCode) }, size = ButtonSize.Small, style = ButtonStyle.Secondary, icon = R.drawable.ic_copy)
            }
            DividerLine(Modifier.padding(vertical = 10.dp))
            Text("Referral link", style = Z.type.caption, color = c.textSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(link, style = Z.type.bodySmallMedium, color = c.textPrimary, modifier = Modifier.weight(1f), maxLines = 1)
                ZIcon(R.drawable.ic_link, size = 18.dp, tint = c.primary, modifier = Modifier.clip(CircleShape).clickable { copy("Link", link) })
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SurfaceCard(Modifier.weight(1f)) {
                Text("Friends joined", style = Z.type.caption, color = c.textSecondary)
                Text("$referrals", style = Z.type.h2, color = c.textPrimary)
            }
            SurfaceCard(Modifier.weight(1f)) {
                Text("Earned", style = Z.type.caption, color = c.textSecondary)
                Text("${referrals * 5} USDT", style = Z.type.h2, color = c.success)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("How it works", style = Z.type.title, color = c.textPrimary)
        Spacer(Modifier.height(4.dp))
        StepTitle(1, "Share your code or link", "Send it to friends via any app.")
        StepTitle(2, "Friend signs up & verifies", "They enter your code during sign-up.")
        StepTitle(3, "You both get 5 USDT", "Rewards land in your wallet instantly.")
        Spacer(Modifier.height(8.dp))
        Text(
            "Simulate a friend joining (demo)", style = Z.type.bodySmallMedium, color = c.primary,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { AppRepository.simulateReferral(); messenger.success("You earned 5 USDT 🎁", "A friend just joined with your code.") }.padding(8.dp),
        )
    }
}
