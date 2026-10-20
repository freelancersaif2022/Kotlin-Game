package com.tvremote.universal.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StayCurrentPortrait
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvremote.universal.App
import com.tvremote.universal.AppConfig
import com.tvremote.universal.BuildConfig
import com.tvremote.universal.core.LocalNetwork
import com.tvremote.universal.core.WakeOnLan
import com.tvremote.universal.data.DiscoveredTv
import com.tvremote.universal.data.Guides
import com.tvremote.universal.data.ThemeMode
import com.tvremote.universal.data.TvPlatform
import com.tvremote.universal.ui.components.AppBar
import com.tvremote.universal.ui.components.AppCard
import com.tvremote.universal.ui.components.NumberedStep
import com.tvremote.universal.ui.components.PlatformBadge
import com.tvremote.universal.ui.components.PrimaryButton
import com.tvremote.universal.ui.components.SecondaryButton
import com.tvremote.universal.ui.components.SectionTitle
import com.tvremote.universal.ui.components.TvGlyph
import com.tvremote.universal.ui.theme.AppTheme
import kotlinx.coroutines.launch

// ===================================================================================== settings

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPremium: () -> Unit,
    onHelp: () -> Unit,
    onManageTvs: () -> Unit,
) {
    val container = App.container
    val settings = container.settings
    val context = LocalContext.current
    val activity = LocalActivity.current
    val theme by settings.themeMode.collectAsStateWithLifecycle()
    val haptics by settings.haptics.collectAsStateWithLifecycle()
    val keepOn by settings.keepScreenOn.collectAsStateWithLifecycle()
    val volumeKeys by settings.volumeKeys.collectAsStateWithLifecycle()
    val premium by container.premium.isPremium.collectAsStateWithLifecycle()
    val privacyRequired by container.ads.privacyOptionsRequired.collectAsStateWithLifecycle()

    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppBar(title = "Settings", onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            if (!premium) {
                PremiumTeaser(onPremium)
                Spacer(Modifier.height(18.dp))
            }

            SectionTitle("Appearance")
            AppCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeChip("Light", Icons.Rounded.LightMode, theme == ThemeMode.LIGHT, Modifier.weight(1f)) { settings.setTheme(ThemeMode.LIGHT) }
                    ThemeChip("Dark", Icons.Rounded.DarkMode, theme == ThemeMode.DARK, Modifier.weight(1f)) { settings.setTheme(ThemeMode.DARK) }
                    ThemeChip("System", Icons.Rounded.BrightnessMedium, theme == ThemeMode.SYSTEM, Modifier.weight(1f)) { settings.setTheme(ThemeMode.SYSTEM) }
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionTitle("Remote")
            AppCard(Modifier.fillMaxWidth()) {
                Column {
                    SwitchRow(Icons.Rounded.Vibration, "Haptic feedback", "Vibrate on every button press", haptics, settings::setHaptics)
                    SwitchRow(Icons.Rounded.StayCurrentPortrait, "Keep screen on", "While the remote is open", keepOn, settings::setKeepScreenOn)
                    SwitchRow(Icons.Rounded.VolumeUp, "Phone volume buttons", "Control the TV volume with your phone's buttons", volumeKeys, settings::setVolumeKeys)
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionTitle("TVs & help")
            AppCard(Modifier.fillMaxWidth()) {
                Column {
                    NavRow(Icons.Rounded.Devices, "My TVs", "Rename, re-pair or remove TVs", onManageTvs)
                    NavRow(Icons.AutoMirrored.Rounded.HelpOutline, "Pairing & power-on guides", "Step-by-step help for every brand", onHelp)
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionTitle("About")
            AppCard(Modifier.fillMaxWidth()) {
                Column {
                    NavRow(Icons.Rounded.Star, "Rate TV Remote", "Love it? A 5-star review helps a lot") {
                        openUrl(context, "market://details?id=${context.packageName}", "https://play.google.com/store/apps/details?id=${context.packageName}")
                    }
                    NavRow(Icons.Rounded.Share, "Share with friends", null) {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Control any smart TV from your phone: https://play.google.com/store/apps/details?id=${context.packageName}")
                        }
                        runCatching { context.startActivity(Intent.createChooser(send, "Share TV Remote")) }
                    }
                    NavRow(Icons.Rounded.Email, "Contact support", AppConfig.SUPPORT_EMAIL) {
                        openUrl(context, "mailto:${AppConfig.SUPPORT_EMAIL}?subject=${Uri.encode("TV Remote support")}")
                    }
                    NavRow(Icons.Rounded.PrivacyTip, "Privacy policy", null) { openUrl(context, AppConfig.PRIVACY_POLICY_URL) }
                    if (privacyRequired && activity != null) {
                        NavRow(Icons.Rounded.Tune, "Privacy choices", "Manage ad consent") { container.ads.showPrivacyOptions(activity) }
                    }
                    InfoRow(Icons.Rounded.Info, "Version", BuildConfig.VERSION_NAME)
                }
            }
            Spacer(Modifier.height(24.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

private fun openUrl(context: Context, url: String, fallback: String? = null) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        if (fallback != null) runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallback)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

@Composable
private fun ThemeChip(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) scheme.primaryContainer else Color.Transparent)
            .border(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) scheme.primary else scheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) scheme.onPrimaryContainer else scheme.onSurface)
    }
}

@Composable
private fun RowIcon(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary) {
    Box(
        Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
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
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        RowIcon(icon)
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ===================================================================================== help

@Composable
fun HelpScreen(initial: TvPlatform?, onBack: () -> Unit) {
    var expanded by remember { mutableStateOf(initial) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppBar(title = "Help & guides", subtitle = "Pairing, power on and troubleshooting", onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RowIcon(Icons.Rounded.WifiTethering)
                        Spacer(Modifier.width(12.dp))
                        Text("Before you start", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    NumberedStep(1, "Your phone and TV must be on the same Wi-Fi network (not a guest network).")
                    NumberedStep(2, "Turn off VPN apps on your phone while using the remote.")
                    NumberedStep(3, "The TV must be turned on the first time you pair it.")
                    NumberedStep(4, "Supported: most smart TVs from 2016 and newer — Android TV / Google TV, Samsung Tizen, LG webOS, Roku, Vizio SmartCast and Fire TV.")
                }
            }
            Spacer(Modifier.height(18.dp))
            SectionTitle("Choose your TV")
            TvPlatform.entries.forEach { p ->
                PlatformGuideCard(p, expanded == p) { expanded = if (expanded == p) null else p }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun PlatformGuideCard(platform: TvPlatform, expanded: Boolean, onToggle: () -> Unit) {
    val guide = remember(platform) { Guides.of(platform) }
    AppCard(Modifier.fillMaxWidth()) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TvGlyph(platform, size = 42.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(platform.title, style = MaterialTheme.typography.titleSmall)
                    Text(platform.brands, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    GuideSection("How to pair", guide.pairing)
                    GuideSection("Turn the TV on from your phone", guide.powerOn)
                    GuideSection("Troubleshooting", guide.troubleshooting)
                    Text("Find the TV's IP address: ${guide.findIp}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun GuideSection(title: String, steps: List<String>) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
    steps.forEachIndexed { i, s -> NumberedStep(i + 1, s) }
    Spacer(Modifier.height(6.dp))
}

// ===================================================================================== device settings

@Composable
fun DeviceSettingsScreen(
    deviceId: String,
    onBack: () -> Unit,
    onRepair: (DiscoveredTv) -> Unit,
    onDeleted: () -> Unit,
    onHelp: (TvPlatform) -> Unit,
) {
    val container = App.container
    val devices by container.tvStore.devices.collectAsStateWithLifecycle()
    val tv = devices.firstOrNull { it.id == deviceId }
    if (tv == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onBack() }
        return
    }
    var name by remember(tv.id) { mutableStateOf(tv.name) }
    var host by remember(tv.id) { mutableStateOf(tv.host) }
    var mac by remember(tv.id) { mutableStateOf(tv.mac) }
    var confirmDelete by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val hostValid = LocalNetwork.isValidIpv4(host)
    val macValid = mac.isBlank() || WakeOnLan.normalizeMac(mac) != null
    val changed = name.trim() != tv.name || host.trim() != tv.host || (WakeOnLan.normalizeMac(mac) ?: mac.trim()) != tv.mac

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().imePadding()) {
            AppBar(title = "TV settings", subtitle = tv.name, onBack = onBack)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                AppCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        TvGlyph(tv.platform, size = 48.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tv.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PlatformBadge(tv.platform)
                                if (tv.model.isNotBlank()) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(tv.model, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                SectionTitle("Details")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("TV name") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = host,
                    onValueChange = { v -> host = v.filter { it.isDigit() || it == '.' }.take(15) },
                    label = { Text("IP address") },
                    singleLine = true,
                    isError = !hostValid,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = mac,
                    onValueChange = { v -> mac = v.uppercase().filter { it.isLetterOrDigit() || it == ':' || it == '-' }.take(17) },
                    label = { Text("MAC address (for Power on)") },
                    placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                    singleLine = true,
                    isError = !macValid,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (tv.platform == TvPlatform.SAMSUNG || tv.platform == TvPlatform.LG || tv.platform == TvPlatform.ROKU) {
                        "Filled in automatically while the TV is connected. You can also find it on the TV under network settings."
                    } else {
                        "Find it on the TV: ${Guides.of(tv.platform).findIp} (look for MAC / Wi-Fi MAC / Ethernet MAC)."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    "Save changes",
                    enabled = changed && hostValid && macValid && name.isNotBlank(),
                    onClick = {
                        container.tvStore.update(tv.id) {
                            it.copy(
                                name = name.trim(),
                                host = host.trim(),
                                mac = WakeOnLan.normalizeMac(mac) ?: "",
                            )
                        }
                        scope.launch { snackbar.showSnackbar("Saved") }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(22.dp))
                SectionTitle("Actions")
                SecondaryButton(
                    "Pair again",
                    icon = Icons.Rounded.Link,
                    onClick = {
                        container.certPins.clearHost(tv.host)
                        onRepair(DiscoveredTv(tv.platform, tv.host, tv.name, tv.model, tv.mac))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                SecondaryButton(
                    "Power-on setup guide",
                    icon = Icons.Rounded.PowerSettingsNew,
                    onClick = { onHelp(tv.platform) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Remove this TV", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(24.dp))
                Spacer(Modifier.navigationBarsPadding())
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove ${tv.name}?") },
            text = { Text("You'll need to pair this TV again to control it.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    container.tvStore.remove(tv.id)
                    container.certPins.clearHost(tv.host)
                    onDeleted()
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            containerColor = AppTheme.extra.panel,
        )
    }
}
