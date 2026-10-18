package com.smartisland.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartisland.app.BuildConfig
import com.smartisland.app.data.Plan
import com.smartisland.app.system.OemGuides
import com.smartisland.app.ui.appContainer
import com.smartisland.app.ui.components.AppDialog
import com.smartisland.app.ui.components.Divider
import com.smartisland.app.ui.components.GlassCard
import com.smartisland.app.ui.components.LocalSnack
import com.smartisland.app.ui.components.PrimaryButton
import com.smartisland.app.ui.components.ProgressDialog
import com.smartisland.app.ui.components.SectionHeader
import com.smartisland.app.ui.components.SettingRow
import com.smartisland.app.ui.components.SwitchRow
import com.smartisland.app.ui.components.TabHeader
import com.smartisland.app.ui.components.tabBarPadding
import com.smartisland.app.ui.findActivity
import com.smartisland.app.ui.navigation.AppNavigator
import com.smartisland.app.ui.navigation.Routes
import com.smartisland.app.ui.rememberPermissionStatus
import com.smartisland.app.ui.theme.Palette
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

/** Replace with the hosted privacy policy before publishing (required by Play for these permissions). */
private const val PRIVACY_POLICY_URL = "https://example.com/smart-island/privacy"

@Composable
fun SettingsScreen(nav: AppNavigator) {
    val context = LocalContext.current
    val container = appContainer()
    val snack = LocalSnack.current
    val scope = rememberCoroutineScope()
    val premium by container.premium.state.collectAsStateWithLifecycle()
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val perms = rememberPermissionStatus()
    val oem = remember { OemGuides.detect() }
    var restoring by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .tabBarPadding(),
    ) {
        TabHeader("Settings", "Smart Island")
        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(10.dp))
            PlanCard(
                premium = premium.isPremium,
                planLabel = if (premium.plan == Plan.YEARLY) "Yearly" else "Monthly",
                detail = when {
                    !premium.isPremium && premium.unlockedWidgets.isNotEmpty() ->
                        "${premium.unlockedWidgets.size} widget(s) unlocked forever with rewarded ads."
                    !premium.isPremium -> "Unlock all 17 widgets, alerts, volume HUD, edge panel and styles."
                    premium.autoRenew -> "Renews on ${formatDate(premium.expiresAt)}"
                    else -> "Cancelled · PRO until ${formatDate(premium.expiresAt)}"
                },
                onUpgrade = { nav.go(Routes.PAYWALL) },
            )

            SectionHeader("Appearance")
            GlassCard(padding = 12.dp) {
                SwitchRow(
                    if (settings.darkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                    if (settings.darkMode) Palette.Primary else Palette.Warning,
                    "Dark mode",
                    if (settings.darkMode) "Dark theme across the app" else "Light theme across the app",
                    settings.darkMode,
                    { v -> container.settings.update { it.copy(darkMode = v) } },
                )
            }

            SectionHeader("Setup")
            GlassCard(padding = 12.dp) {
                SettingRow(
                    Icons.Rounded.Security, Palette.Primary, "Permissions",
                    "${perms.grantedCount} of 4 granted" + if (perms.coreReady) "" else " · island needs setup",
                    onClick = { nav.go(Routes.PERMISSIONS) },
                )
                Divider()
                SettingRow(
                    Icons.Rounded.BatterySaver, Palette.Success, "Keep running in background",
                    "Battery & autostart guide for ${oem.brand}",
                    onClick = { nav.go(Routes.BATTERY_GUIDE) },
                )
            }

            SectionHeader("Subscription")
            GlassCard(padding = 12.dp) {
                SettingRow(Icons.Rounded.Restore, Palette.Secondary, "Restore purchases", "Already subscribed on another device?", onClick = {
                    restoring = true
                    scope.launch {
                        val ok = container.billing.restore()
                        restoring = false
                        if (ok) snack.success("Purchases restored") else snack.show("No active subscription found")
                    }
                })
                if (premium.isPremium && premium.autoRenew) {
                    Divider()
                    SettingRow(Icons.Rounded.Cancel, Palette.Error, "Cancel subscription", "Keeps PRO until the end of the period", onClick = { confirmCancel = true })
                }
            }

            SectionHeader("Privacy")
            GlassCard(padding = 12.dp) {
                if (container.ads.privacyOptionsRequired) {
                    SettingRow(Icons.Rounded.Shield, Palette.Primary, "Ad privacy choices", "Change your consent for personalized ads", onClick = {
                        context.findActivity()?.let { container.ads.showPrivacyOptions(it) }
                    })
                    Divider()
                }
                SettingRow(Icons.Rounded.PrivacyTip, Palette.Secondary, "Privacy policy", "Smart Island never uploads your calls, media or notifications", onClick = {
                    openUrl(context, PRIVACY_POLICY_URL)
                })
            }

            SectionHeader("About")
            GlassCard(padding = 12.dp) {
                SettingRow(Icons.Rounded.StarRate, Palette.Gold, "Rate Smart Island", "Enjoying it? A review helps a lot", onClick = {
                    openUrl(context, "market://details?id=${context.packageName}")
                })
                Divider()
                SettingRow(Icons.Rounded.Share, Palette.Success, "Share with friends", null, onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(
                        Intent.EXTRA_TEXT,
                        "Get the island on Android: https://play.google.com/store/apps/details?id=${context.packageName}",
                    )
                    runCatching { context.startActivity(Intent.createChooser(send, "Share Smart Island")) }
                })
                Divider()
                SettingRow(Icons.Rounded.BugReport, Palette.Warning, "Send feedback", null, onClick = {
                    openUrl(context, "mailto:support@example.com?subject=Smart%20Island%20feedback")
                })
                Divider()
                SettingRow(Icons.Rounded.Info, Palette.TextSecondary, "Version", BuildConfig.VERSION_NAME, trailing = null)
            }
        }
    }

    if (restoring) ProgressDialog("Restoring purchases", "Checking Google Play…")

    if (confirmCancel) {
        AppDialog(
            onDismiss = { confirmCancel = false },
            icon = Icons.Rounded.Cancel,
            title = "Cancel subscription?",
            message = "You'll keep PRO until ${formatDate(premium.expiresAt)}. Premium widgets you unlocked with ads stay unlocked.",
            confirmText = "Cancel subscription",
            accent = Palette.Error,
            onConfirm = {
                container.premium.cancelAutoRenew()
                confirmCancel = false
                snack.show("Subscription cancelled")
            },
            dismissText = "Keep PRO",
        )
    }
}

/** Free = clean card with a gold upgrade button; PRO = gold card with an active seal. */
@Composable
private fun PlanCard(premium: Boolean, planLabel: String, detail: String, onUpgrade: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(if (premium) 18.dp else 0.dp, shape, ambientColor = Palette.Gold, spotColor = Palette.GoldDeep)
            .clip(shape)
            .background(if (premium) Palette.GoldCardGradient else SolidColor(Palette.Surface))
            .border(1.dp, if (premium) Palette.Gold.copy(alpha = 0.7f) else Palette.Outline, shape)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (premium) Palette.GoldGradient else SolidColor(Palette.Gold.copy(alpha = 0.16f))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = if (premium) Palette.OnGold else Palette.GoldDeep, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (premium) "Smart Island PRO" else "Free plan",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (premium) Color.White else Palette.TextPrimary,
                )
                Text(
                    if (premium) "All features unlocked" else "Basic island & 6 widgets",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (premium) Palette.Gold else Palette.TextSecondary,
                )
            }
            if (premium) {
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(Palette.GoldGradient)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Check, null, tint = Palette.OnGold, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(planLabel, style = MaterialTheme.typography.labelMedium, color = Palette.OnGold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(detail, style = MaterialTheme.typography.bodySmall, color = if (premium) Color.White.copy(alpha = 0.85f) else Palette.TextSecondary)
        if (!premium) {
            Spacer(Modifier.height(14.dp))
            PrimaryButton("Upgrade to PRO", onUpgrade, Modifier.fillMaxWidth(), icon = Icons.Rounded.WorkspacePremium, gold = true)
        }
    }
}

private fun formatDate(ms: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(ms))

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
