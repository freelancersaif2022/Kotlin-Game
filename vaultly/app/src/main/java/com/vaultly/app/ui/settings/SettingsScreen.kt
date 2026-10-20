package com.vaultly.app.ui.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import com.vaultly.app.BuildConfig
import com.vaultly.app.VaultlyApp
import com.vaultly.app.billing.PlanKey
import com.vaultly.app.ui.components.ProBadge
import com.vaultly.app.ui.components.SectionHeader
import com.vaultly.app.ui.components.findActivity
import com.vaultly.app.ui.home.RoundIcon
import com.vaultly.app.ui.nav.Routes
import com.vaultly.app.ui.nav.openPaywall
import com.vaultly.app.ui.theme.Palette
import com.vaultly.app.ui.theme.VaultlyTheme
import com.vaultly.app.util.Format
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(app: VaultlyApp, nav: NavHostController) {
    val prefs = app.prefs
    val pro by prefs.pro.collectAsStateWithLifecycle()
    val removeAds by prefs.removeAds.collectAsStateWithLifecycle()
    val currency by prefs.currency.collectAsStateWithLifecycle()
    val remindReturn by prefs.remindReturn.collectAsStateWithLifecycle()
    val remindWarranty by prefs.remindWarranty.collectAsStateWithLifecycle()
    val plans by app.billing.plans.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var showCurrency by remember { mutableStateOf(false) }

    fun hasNotificationPermission() = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var notificationsAllowed by remember { mutableStateOf(hasNotificationPermission()) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { notificationsAllowed = hasNotificationPermission() }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = granted
        if (!granted) {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        }
    }
    LaunchedEffect(Unit) {
        app.billing.events.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIcon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", { nav.popBackStack() })
            Spacer(Modifier.width(14.dp))
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ProCard(pro) { nav.openPaywall() }

            Spacer(Modifier.height(8.dp))
            SectionHeader("Reminders")
            Group {
                if (!notificationsAllowed) {
                    LinkRow(Icons.Outlined.NotificationsOff, "Allow notifications", "Needed to remind you before deadlines", accent = true) {
                        if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    Divider()
                }
                SwitchRow(Icons.Outlined.AssignmentTurnedIn, "Return windows", "7, 3 and 1 day before, and on the last day", remindReturn) {
                    prefs.setRemindReturn(it)
                }
                Divider()
                SwitchRow(Icons.Outlined.VerifiedUser, "Warranties", "30 and 7 days before, and on the last day", remindWarranty) {
                    prefs.setRemindWarranty(it)
                }
            }

            Spacer(Modifier.height(8.dp))
            SectionHeader("Preferences")
            Group {
                LinkRow(Icons.Outlined.Payments, "Default currency", "$currency · ${Format.symbol(currency)}") { showCurrency = true }
            }

            Spacer(Modifier.height(8.dp))
            SectionHeader("Purchases")
            Group {
                LinkRow(Icons.Outlined.Restore, "Restore purchases", "Bring back Pro or ad-free on this device") {
                    scope.launch { app.billing.restore(silent = false) }
                }
                if (!pro && !removeAds) {
                    Divider()
                    val plan = plans.firstOrNull { it.key == PlanKey.REMOVE_ADS }
                    LinkRow(Icons.Outlined.Block, "Remove ads", plan?.let { "One-time purchase · ${it.price}" } ?: "One-time purchase") {
                        val activity = context.findActivity()
                        if (plan != null && activity != null) app.billing.buy(activity, plan)
                        else Toast.makeText(context, "Google Play isn't available right now.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            SectionHeader("About")
            Group {
                LinkRow(Icons.Outlined.StarRate, "Rate Vaultly", "It really helps an independent app") {
                    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                    try {
                        context.startActivity(market)
                    } catch (_: ActivityNotFoundException) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")),
                        )
                    }
                }
                Divider()
                LinkRow(Icons.Outlined.Share, "Share Vaultly", "Tell a friend who loses receipts") {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "I keep all my receipts and warranties in Vaultly: https://play.google.com/store/apps/details?id=${context.packageName}",
                        )
                    }
                    context.startActivity(Intent.createChooser(send, "Share Vaultly"))
                }
                Divider()
                LinkRow(Icons.Outlined.Lock, "Privacy", "Your receipts stay on your device") { nav.navigate(Routes.PRIVACY) }
            }
            Text(
                "Vaultly ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = VaultlyTheme.colors.muted,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 18.dp),
            )
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (showCurrency) {
        AlertDialog(
            onDismissRequest = { showCurrency = false },
            title = { Text("Default currency") },
            text = {
                LazyColumn(Modifier.height(360.dp)) {
                    items(Format.currencies) { code ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .clickable { prefs.setCurrency(code); showCurrency = false }
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(code, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(64.dp))
                            Text(Format.symbol(code), color = VaultlyTheme.colors.muted, modifier = Modifier.weight(1f))
                            if (code == currency) Icon(Icons.Outlined.Check, null, tint = VaultlyTheme.colors.gold)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCurrency = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun ProCard(pro: Boolean, onUpgrade: () -> Unit) {
    val colors = VaultlyTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.inkBrush)
            .border(1.dp, Palette.Gold.copy(alpha = 0.3f), shape)
            .clickable(enabled = !pro, onClick = onUpgrade)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(colors.goldBrush),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.WorkspacePremium, null, tint = Palette.Ink) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (pro) "Vaultly Pro" else "Upgrade to Pro", style = MaterialTheme.typography.titleLarge, color = Color.White)
                if (pro) { Spacer(Modifier.width(8.dp)); ProBadge() }
            }
            Text(
                if (pro) "Unlimited vault · No ads · Unlimited claim packs" else "Unlimited items, claim packs and no ads",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
            )
        }
        if (!pro) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Palette.GoldLight)
    }
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(VaultlyTheme.colors.card).border(1.dp, VaultlyTheme.colors.hairline, shape),
    ) { content() }
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(start = 66.dp), color = VaultlyTheme.colors.hairline)

@Composable
private fun RowIcon(icon: ImageVector, accent: Boolean = false) {
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(11.dp))
            .background(if (accent) VaultlyTheme.colors.gold.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(19.dp), tint = if (accent) VaultlyTheme.colors.gold else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, subtitle: String, accent: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon, accent)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = VaultlyTheme.colors.muted)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = VaultlyTheme.colors.muted)
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = VaultlyTheme.colors.muted)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = VaultlyTheme.colors.gold,
                checkedThumbColor = Color.White,
                checkedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIcon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
            Spacer(Modifier.width(14.dp))
            Text("Privacy", style = MaterialTheme.typography.headlineSmall)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PrivacyBlock(
                Icons.Outlined.Lock, "Your vault stays on your phone",
                "Receipts, photos and purchase details are stored only in Vaultly's private storage on this device. " +
                    "They are included in your phone's own Android backup if you have it turned on.",
            )
            PrivacyBlock(
                Icons.Outlined.NotificationsActive, "On-device reading",
                "Text on receipts is read with Google ML Kit running on your device. Images are not uploaded for scanning.",
            )
            PrivacyBlock(
                Icons.Outlined.Payments, "Ads and purchases",
                "The free version shows ads from Google AdMob, which may use your device's advertising ID. " +
                    "Purchases are handled by Google Play; Vaultly never sees your payment details. Going Pro or removing ads turns ads off.",
            )
            PrivacyBlock(
                Icons.Outlined.Share, "You decide what to share",
                "A claim pack PDF is only created when you ask for it, and only goes where you send it.",
            )
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }
}

@Composable
private fun PrivacyBlock(icon: ImageVector, title: String, body: String) {
    Row {
        RowIcon(icon, accent = true)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
