package com.zyropay.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zyropay.app.BuildConfig
import com.zyropay.app.R
import com.zyropay.app.data.AppRepository
import com.zyropay.app.data.KycStatus
import com.zyropay.app.data.ThemeMode
import com.zyropay.app.navigation.Routes
import com.zyropay.app.navigation.navigateClear
import com.zyropay.app.ui.components.Avatar
import com.zyropay.app.ui.components.ButtonStyle
import com.zyropay.app.ui.components.DividerLine
import com.zyropay.app.ui.components.KycPill
import com.zyropay.app.ui.components.ListRow
import com.zyropay.app.ui.components.LocalMessenger
import com.zyropay.app.ui.components.Pill
import com.zyropay.app.ui.components.SurfaceCard
import com.zyropay.app.ui.components.SwitchRow
import com.zyropay.app.ui.components.ZButton
import com.zyropay.app.ui.components.ZDialog
import com.zyropay.app.ui.components.ZIcon
import com.zyropay.app.ui.components.ZImage
import com.zyropay.app.ui.components.rememberCopier
import com.zyropay.app.ui.components.shareText
import com.zyropay.app.ui.theme.Z

@Composable
fun ProfileTab(nav: NavController) {
    val c = Z.colors
    val user by AppRepository.user.collectAsState()
    val prefs by AppRepository.prefs.collectAsState()
    val security by AppRepository.security.collectAsState()
    val copy = rememberCopier()
    val messenger = LocalMessenger.current
    val context = LocalContext.current
    var logout by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item(key = "header") {
            Box(Modifier.fillMaxWidth()) {
                ZImage(if (c.isDark) R.drawable.bg_profile_header_dark else R.drawable.bg_profile_header_light, Modifier.fillMaxWidth().height(210.dp), ContentScale.Crop)
                Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.clickable(interactionSource = null, indication = null) { nav.navigate(Routes.PERSONAL_INFO) }) {
                            Avatar(user.avatar, size = 68.dp, ring = true)
                            Box(
                                Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).size(22.dp).clip(CircleShape).background(c.surface).padding(2.dp).clip(CircleShape).background(c.primary),
                                contentAlignment = Alignment.Center,
                            ) { ZIcon(R.drawable.ic_edit, size = 11.dp, tint = Color.White) }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(user.nickname.ifBlank { "Zyro user" }, style = Z.type.h2, color = c.textPrimary)
                            Spacer(Modifier.height(4.dp))
                            Row(
                                Modifier.clip(RoundedCornerShape(6.dp)).clickable { copy("UID", user.uid) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("UID ${user.uid}", style = Z.type.caption, color = c.textSecondary)
                                Spacer(Modifier.width(4.dp))
                                ZIcon(R.drawable.ic_copy, size = 13.dp, tint = c.textSecondary)
                            }
                            Spacer(Modifier.height(6.dp))
                            Row {
                                KycPill(user.kycStatus)
                                Spacer(Modifier.width(6.dp))
                                Pill("VIP ${user.vipLevel}", color = Color(0xFFB7791F), background = Color(0x33E8B84A), icon = R.drawable.ic_crown)
                            }
                        }
                    }
                }
            }
        }
        if (user.kycStatus != KycStatus.VERIFIED) {
            item(key = "kyc") {
                SurfaceCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), onClick = { nav.navigate(Routes.KYC) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ZImage(R.drawable.ill_kyc_intro, Modifier.size(56.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (user.kycStatus == KycStatus.PENDING) "Verification under review" else "Complete verification", style = Z.type.title, color = c.textPrimary)
                            Text(if (user.kycStatus == KycStatus.PENDING) "We'll notify you once approved" else "Unlock cards, withdrawals and higher limits", style = Z.type.caption, color = c.textSecondary)
                        }
                        ZIcon(R.drawable.ic_chevron_right, size = 20.dp, tint = c.textTertiary)
                    }
                }
            }
        }
        item(key = "rewards") {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                PromoTile("Rewards", "Cashback & tasks", R.drawable.ill_rewards_trophy, Modifier.weight(1f)) { nav.navigate(Routes.REWARDS) }
                Spacer(Modifier.width(10.dp))
                PromoTile("Invite friends", "Earn 5 USDT each", R.drawable.ill_referral_gift, Modifier.weight(1f)) { nav.navigate(Routes.REFERRAL) }
            }
        }
        item(key = "account") {
            Section("Account") {
                ListRow("Personal information", icon = R.drawable.ic_user, onClick = { nav.navigate(Routes.PERSONAL_INFO) })
                DividerLine(inset = 66.dp)
                ListRow("Identity verification", icon = R.drawable.ic_user_verify, trailing = { KycPill(user.kycStatus); ZIcon(R.drawable.ic_chevron_right, size = 18.dp, tint = c.textTertiary) }, onClick = { nav.navigate(Routes.KYC) })
                DividerLine(inset = 66.dp)
                ListRow("Security center", icon = R.drawable.ic_shield_check, value = if (security.twoFaEnabled) "Strong" else "Medium", valueColor = if (security.twoFaEnabled) c.success else c.warning, onClick = { nav.navigate(Routes.SECURITY) })
                DividerLine(inset = 66.dp)
                ListRow("Transaction history", icon = R.drawable.ic_history, onClick = { nav.navigate(Routes.transactions()) })
            }
        }
        item(key = "prefs") {
            Section("Preferences") {
                SwitchRow("Dark mode", prefs.themeMode == ThemeMode.DARK, { on ->
                    AppRepository.updatePrefs { it.copy(themeMode = if (on) ThemeMode.DARK else ThemeMode.LIGHT) }
                }, icon = R.drawable.ic_moon)
                DividerLine(inset = 66.dp)
                ListRow("Appearance", icon = R.drawable.ic_sun, value = prefs.themeMode.name.lowercase().replaceFirstChar { it.uppercase() }, onClick = { nav.navigate(Routes.SETTINGS) })
                DividerLine(inset = 66.dp)
                ListRow("Language", icon = R.drawable.ic_globe_language, value = prefs.language, onClick = { nav.navigate(Routes.LANGUAGE) })
                DividerLine(inset = 66.dp)
                ListRow("Display currency", icon = R.drawable.ic_percent, value = prefs.currency, onClick = { nav.navigate(Routes.CURRENCY) })
                DividerLine(inset = 66.dp)
                ListRow("Notifications", icon = R.drawable.ic_bell, onClick = { nav.navigate(Routes.NOTIFICATION_SETTINGS) })
            }
        }
        item(key = "support") {
            Section("Support") {
                ListRow("Help center", icon = R.drawable.ic_faq, onClick = { nav.navigate(Routes.HELP) })
                DividerLine(inset = 66.dp)
                ListRow("Live chat", icon = R.drawable.ic_support_chat, subtitle = "Average reply < 2 min", onClick = { nav.navigate(Routes.CHAT) })
                DividerLine(inset = 66.dp)
                ListRow("Rate Zyro Pay", icon = R.drawable.ic_rate, onClick = { messenger.success("Thank you! ⭐", "Your feedback helps us improve.") })
                DividerLine(inset = 66.dp)
                ListRow("Share app", icon = R.drawable.ic_share, onClick = { context.shareText("Spend crypto like cash with Zyro Pay! Use my code ${user.referralCode} to get 5 USDT.") })
                DividerLine(inset = 66.dp)
                ListRow("About Zyro Pay", icon = R.drawable.ic_about, value = "v${BuildConfig.VERSION_NAME}", onClick = { nav.navigate(Routes.ABOUT) })
            }
        }
        item(key = "logout") {
            ZButton("Log out", onClick = { logout = true }, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth(), style = ButtonStyle.Outline, icon = R.drawable.ic_logout)
        }
    }
    if (logout) {
        ZDialog(
            onDismiss = { logout = false }, title = "Log out?",
            message = "You'll need your password to log in again. Your passcode and biometrics will be reset on this device.",
            confirmText = "Log out", destructive = true, illustration = R.drawable.ill_logout,
            onConfirm = {
                logout = false
                AppRepository.logout()
                messenger.info("Logged out", "See you soon!")
                nav.navigateClear(Routes.WELCOME)
            },
        )
    }
}

@Composable
fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(title, style = Z.type.label.copy(fontWeight = FontWeight.SemiBold), color = Z.colors.textSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        SurfaceCard(Modifier.fillMaxWidth(), padding = 0.dp) { content() }
    }
}

@Composable
private fun PromoTile(title: String, subtitle: String, image: Int, modifier: Modifier, onClick: () -> Unit) {
    val c = Z.colors
    SurfaceCard(modifier, padding = 12.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = Z.type.bodySmallMedium.copy(fontWeight = FontWeight.SemiBold), color = c.textPrimary)
                Text(subtitle, style = Z.type.caption, color = c.textSecondary, maxLines = 2)
            }
            ZImage(image, Modifier.size(48.dp))
        }
    }
}
