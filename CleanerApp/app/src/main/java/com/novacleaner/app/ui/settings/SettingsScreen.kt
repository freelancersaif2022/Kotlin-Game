package com.novacleaner.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novacleaner.app.BuildConfig
import com.novacleaner.app.R
import com.novacleaner.app.ads.Consent
import com.novacleaner.app.core.AppGraph
import com.novacleaner.app.core.AppPermission
import com.novacleaner.app.core.Settings
import com.novacleaner.app.service.NetSpeedService
import com.novacleaner.app.ui.components.GlossyIcon
import com.novacleaner.app.ui.components.NovaCard
import com.novacleaner.app.ui.components.ToolScaffold
import com.novacleaner.app.ui.components.bounceClick
import com.novacleaner.app.ui.components.openPermissionSettings
import com.novacleaner.app.ui.components.rememberPermissionRequester
import com.novacleaner.app.ui.components.rememberPermissionState
import com.novacleaner.app.ui.navigation.LocalNavigator
import com.novacleaner.app.ui.navigation.Tool
import com.novacleaner.app.ui.navigation.findActivity
import com.novacleaner.app.ui.theme.Gold
import com.novacleaner.app.ui.theme.GoldDeep
import com.novacleaner.app.worker.ReminderWorker
import kotlinx.coroutines.launch

private fun Context.openExternal(intent: Intent): Boolean {
    AppGraph.appOpen.skipNextResume()
    return runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
}

@Composable
fun SettingsScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by AppGraph.prefs.settings.collectAsStateWithLifecycle(initialValue = Settings())
    val premium by AppGraph.premium.collectAsStateWithLifecycle()
    var themeDialog by remember { mutableStateOf(false) }
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    val supportEmail = stringResource(R.string.support_email)

    ToolScaffold(title = "Settings", onBack = navigator::back, showBanner = !premium) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            PremiumCard(premium, navigator::openPremium)

            Section("Appearance")
            Group {
                Item(
                    Icons.Rounded.DarkMode, Tool.DUPLICATES.colors, "Theme",
                    listOf("System default", "Light", "Dark")[settings.themeMode.coerceIn(0, 2)],
                ) { themeDialog = true }
            }

            Section("Features")
            Group {
                ToggleItem(
                    Icons.Rounded.Notifications, Tool.DATA_USAGE.colors, "Cleanup reminders",
                    "A gentle nudge when you haven't cleaned in a while", settings.remindersEnabled,
                ) { on ->
                    scope.launch { AppGraph.prefs.setRemindersEnabled(on) }
                    ReminderWorker.schedule(context, on)
                }
                Divider()
                ToggleItem(
                    Icons.Rounded.NotificationsActive, Tool.SPEED.colors, "Network speed meter",
                    "Live speed in the status bar", settings.speedMonitorEnabled,
                ) { on ->
                    scope.launch { AppGraph.prefs.setSpeedMonitorEnabled(on) }
                    if (on) NetSpeedService.start(context) else NetSpeedService.stop(context)
                }
            }

            Section("Permissions")
            Group {
                PermissionItem(AppPermission.STORAGE, Icons.Rounded.FolderOpen, Tool.LARGE_FILES.colors, "All files access")
                Divider()
                PermissionItem(AppPermission.USAGE, Icons.Rounded.BarChart, Tool.APPS.colors, "Usage access")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Divider()
                    PermissionItem(AppPermission.NOTIFICATIONS, Icons.Rounded.Notifications, Tool.SPEED.colors, "Notifications")
                }
            }

            Section("Privacy")
            Group {
                if (!premium && Consent.isPrivacyOptionsRequired(context)) {
                    Item(Icons.Rounded.PrivacyTip, Tool.JUNK.colors, "Ad privacy choices", "Manage how ads use your data") {
                        context.findActivity()?.let { Consent.showPrivacyOptions(it) }
                    }
                    Divider()
                }
                Item(Icons.Rounded.Policy, Tool.APK.colors, "Privacy policy", null) {
                    context.openExternal(Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl)))
                }
            }

            Section("About")
            Group {
                Item(Icons.Rounded.Star, listOf(Gold, GoldDeep), "Rate Nova Cleaner", "Enjoying the app? Leave a review") {
                    val pkg = context.packageName
                    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
                    if (!context.openExternal(market)) {
                        context.openExternal(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
                    }
                }
                Divider()
                Item(Icons.Rounded.Share, Tool.SCREENSHOTS.colors, "Share with friends", null) {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "I keep my phone clean with Nova Cleaner: https://play.google.com/store/apps/details?id=${context.packageName}")
                    }
                    context.openExternal(Intent.createChooser(send, "Share Nova Cleaner"))
                }
                Divider()
                Item(Icons.Rounded.Email, Tool.DATA_USAGE.colors, "Contact support", supportEmail) {
                    context.openExternal(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$supportEmail")).putExtra(Intent.EXTRA_SUBJECT, "Nova Cleaner feedback"))
                }
                Divider()
                Item(Icons.Rounded.Restore, Tool.BATTERY.colors, "Restore purchase", null) { AppGraph.billing.restore() }
                Divider()
                Item(Icons.Rounded.Info, Tool.APPS.colors, "Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", showChevron = false) {}
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (themeDialog) {
        AlertDialog(
            onDismissRequest = { themeDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Theme") },
            text = {
                Column {
                    listOf("System default", "Light", "Dark").forEachIndexed { i, label ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                                scope.launch { AppGraph.prefs.setThemeMode(i) }
                                themeDialog = false
                            }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = settings.themeMode == i,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { themeDialog = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun PremiumCard(premium: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF2A1F00), Color(0xFF4A3300))))
            .border(1.dp, Brush.linearGradient(listOf(Gold, GoldDeep.copy(alpha = 0.3f))), shape)
            .bounceClick(enabled = !premium, onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlossyIcon(if (premium) Icons.Rounded.Verified else Icons.Rounded.WorkspacePremium, listOf(Gold, GoldDeep), size = 50.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(if (premium) "Ad-free unlocked" else "Remove all ads", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                if (premium) "Thank you for your support!" else "One-time purchase • lifetime",
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f),
            )
        }
        if (!premium) Icon(Icons.Rounded.ChevronRight, null, tint = Color.White)
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 22.dp, bottom = 8.dp),
    )
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    NovaCard(Modifier.fillMaxWidth(), padding = PaddingValues(vertical = 4.dp)) { content() }
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(start = 70.dp), color = MaterialTheme.colorScheme.outline)

@Composable
private fun Item(
    icon: ImageVector,
    colors: List<Color>,
    title: String,
    subtitle: String?,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlossyIcon(icon, colors, size = 38.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when {
            trailing != null -> trailing()
            showChevron -> Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ToggleItem(icon: ImageVector, colors: List<Color>, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Item(icon, colors, title, subtitle, trailing = {
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
        )
    }) { onChange(!checked) }
}

@Composable
private fun PermissionItem(permission: AppPermission, icon: ImageVector, colors: List<Color>, title: String) {
    val context = LocalContext.current
    val granted by rememberPermissionState(permission)
    val request = rememberPermissionRequester(permission)
    Item(icon, colors, title, null, trailing = {
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (granted) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(
                if (granted) "Allowed" else "Allow",
                style = MaterialTheme.typography.labelMedium,
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }) { if (granted) openPermissionSettings(context, permission) else request() }
}
