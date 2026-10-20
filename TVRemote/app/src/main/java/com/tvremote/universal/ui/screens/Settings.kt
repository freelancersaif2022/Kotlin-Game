package com.tvremote.universal.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StayCurrentPortrait
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvremote.universal.App
import com.tvremote.universal.BuildConfig
import com.tvremote.universal.R
import com.tvremote.universal.core.LocalNetwork
import com.tvremote.universal.core.WakeOnLan
import com.tvremote.universal.data.DiscoveredTv
import com.tvremote.universal.data.Guides
import com.tvremote.universal.data.ThemeMode
import com.tvremote.universal.data.TvPlatform
import com.tvremote.universal.tv.TvDiscovery
import com.tvremote.universal.ui.components.AppBar
import com.tvremote.universal.ui.components.AppCard
import com.tvremote.universal.ui.components.AppDialog
import com.tvremote.universal.ui.components.AppSnackbarHost
import com.tvremote.universal.ui.components.IconTile
import com.tvremote.universal.ui.components.NumberedStep
import com.tvremote.universal.ui.components.PairingAnimation
import com.tvremote.universal.ui.components.PlatformBadge
import com.tvremote.universal.ui.components.PrimaryButton
import com.tvremote.universal.ui.components.SecondaryButton
import com.tvremote.universal.ui.components.SectionTitle
import com.tvremote.universal.ui.components.SnackKind
import com.tvremote.universal.ui.components.StatusPill
import com.tvremote.universal.ui.components.TvGlyph
import com.tvremote.universal.ui.components.brandBrush
import com.tvremote.universal.ui.components.goldBrush
import com.tvremote.universal.ui.components.show
import com.tvremote.universal.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

// ===================================================================================== settings

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPremium: () -> Unit,
    onHelp: () -> Unit,
    onManageTvs: () -> Unit,
    onPrivacy: () -> Unit,
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
    val purchased by container.premium.purchased.collectAsStateWithLifecycle()
    val plan by container.premium.plan.collectAsStateWithLifecycle()
    val passActive by container.premium.passActive.collectAsStateWithLifecycle()
    val rewardUntil by container.premium.rewardUntil.collectAsStateWithLifecycle()
    val privacyRequired by container.ads.privacyOptionsRequired.collectAsStateWithLifecycle()
    val devices by container.tvStore.devices.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme

    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(scheme.background)) {
        AppBar(title = "Settings", onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            if (premium) {
                PremiumActiveBanner(
                    subtitle = when {
                        purchased && plan != null -> "${plan!!.title} plan · all features unlocked"
                        purchased -> "All features unlocked"
                        passActive -> "Free pass until ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(rewardUntil))}"
                        else -> "All features unlocked"
                    },
                    onClick = onPremium,
                )
            } else {
                PremiumTeaser(onPremium)
            }
            Spacer(Modifier.height(20.dp))

            SectionTitle("Appearance")
            AppCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeChip("Light", Icons.Rounded.LightMode, theme == ThemeMode.LIGHT, Modifier.weight(1f)) { settings.setTheme(ThemeMode.LIGHT) }
                    ThemeChip("Dark", Icons.Rounded.DarkMode, theme == ThemeMode.DARK, Modifier.weight(1f)) { settings.setTheme(ThemeMode.DARK) }
                    ThemeChip("System", Icons.Rounded.BrightnessMedium, theme == ThemeMode.SYSTEM, Modifier.weight(1f)) { settings.setTheme(ThemeMode.SYSTEM) }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("Remote")
            AppCard(Modifier.fillMaxWidth()) {
                Column {
                    SwitchRow(Icons.Rounded.Vibration, "Haptic feedback", "Vibrate on every button press", haptics, settings::setHaptics)
                    RowDivider()
                    SwitchRow(Icons.Rounded.StayCurrentPortrait, "Keep screen on", "While the remote is open", keepOn, settings::setKeepScreenOn)
                    RowDivider()
                    SwitchRow(Icons.Rounded.VolumeUp, "Phone volume buttons", "Control the TV volume with your phone's buttons", volumeKeys, settings::setVolumeKeys)
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("TVs & help")
            AppCard(Modifier.fillMaxWidth()) {
                Column {
                    NavRow(Icons.Rounded.Devices, "My TVs", "${devices.size} paired · rename, re-pair or remove", onClick = onManageTvs)
                    RowDivider()
                    NavRow(Icons.AutoMirrored.Rounded.HelpOutline, "Help & guides", "Pairing and power-on for every brand", onClick = onHelp)
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("About")
            AppCard(Modifier.fillMaxWidth()) {
                Column {
                    NavRow(Icons.Rounded.Star, "Rate Universal Remote", "Love it? A 5-star review helps a lot", tint = AppTheme.extra.gold) {
                        openUrl(context, "market://details?id=${context.packageName}", "https://play.google.com/store/apps/details?id=${context.packageName}")
                    }
                    RowDivider()
                    NavRow(Icons.Rounded.Share, "Share with friends", "Send the app to family and friends") {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Control any smart TV from your phone with Universal Remote: https://play.google.com/store/apps/details?id=${context.packageName}")
                        }
                        runCatching { context.startActivity(Intent.createChooser(send, "Share Universal Remote")) }
                    }
                    RowDivider()
                    NavRow(Icons.Rounded.PrivacyTip, "Privacy policy", "How your data is handled", onClick = onPrivacy)
                    if (privacyRequired && activity != null) {
                        RowDivider()
                        NavRow(Icons.Rounded.Tune, "Privacy choices", "Manage ad consent") { container.ads.showPrivacyOptions(activity) }
                    }
                    RowDivider()
                    InfoRow(Icons.Rounded.Info, "Version", BuildConfig.VERSION_NAME)
                }
            }
            if (container.billing.isMock) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(scheme.primary.copy(alpha = 0.08f)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Science, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Billing test mode is on — purchases are simulated.", style = MaterialTheme.typography.bodySmall, color = scheme.primary)
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.app_art), contentDescription = null, modifier = Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)))
                Spacer(Modifier.width(8.dp))
                Text("Universal Remote ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun PremiumActiveBanner(subtitle: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, ambientColor = Color(0xFFF5B400), spotColor = Color(0xFFF5B400))
            .clip(shape)
            .background(goldBrush)
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(50.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Premium Active", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.92f))
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = Color.White)
    }
}

private fun openUrl(context: Context, url: String, fallback: String? = null) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        if (fallback != null) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallback)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}

@Composable
private fun RowDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 68.dp).height(1.dp).background(AppTheme.extra.panelBorder))
}

@Composable
private fun ThemeChip(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) brandBrush() else androidx.compose.ui.graphics.SolidColor(Color.Transparent))
            .border(if (selected) 0.dp else 1.dp, if (selected) Color.Transparent else AppTheme.extra.panelBorder, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Color.White else scheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) Color.White else scheme.onSurface)
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = scheme.primary, uncheckedTrackColor = scheme.surfaceVariant),
        )
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, subtitle: String?, tint: Color = MaterialTheme.colorScheme.primary, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, tint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = scheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon)
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
    }
}

// ===================================================================================== help

@Composable
fun HelpScreen(initial: TvPlatform?, onBack: () -> Unit) {
    var expanded by remember { mutableStateOf(initial) }
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(scheme.background)) {
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
                        IconTile(Icons.Rounded.WifiTethering)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Before you start", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                            Text("4 quick checks that solve most problems", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    NumberedStep(1, "Phone and TV are on the same Wi-Fi network (not a guest network).")
                    NumberedStep(2, "VPN apps on the phone are turned off while using the remote.")
                    NumberedStep(3, "The TV is turned on the first time you pair it.")
                    NumberedStep(4, "Supported: most smart TVs from 2016 onward — Android TV / Google TV, Samsung Tizen, LG webOS, Roku, Vizio SmartCast and Fire TV.")
                }
            }
            Spacer(Modifier.height(20.dp))
            SectionTitle("Choose your TV for a step-by-step guide")
            TvPlatform.entries.forEach { p ->
                PlatformGuideCard(p, expanded == p) { expanded = if (expanded == p) null else p }
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun PlatformGuideCard(platform: TvPlatform, expanded: Boolean, onToggle: () -> Unit) {
    val guide = remember(platform) { Guides.of(platform) }
    val scheme = MaterialTheme.colorScheme
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chev")
    var tab by remember { mutableStateOf(0) }
    AppCard(
        Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(if (expanded) 1.5.dp else 1.dp, if (expanded) scheme.primary.copy(alpha = 0.6f) else AppTheme.extra.panelBorder),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TvGlyph(platform, size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(platform.title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                    Text(platform.brands, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 2)
                }
                Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.rotate(rotation))
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(scheme.primary.copy(alpha = 0.06f))
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center,
                    ) { PairingAnimation(platform, tvWidth = 200.dp) }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(scheme.surfaceVariant).padding(4.dp),
                    ) {
                        listOf("Pair", "Power on", "Fix issues").forEachIndexed { i, label ->
                            val sel = tab == i
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(if (sel) brandBrush() else androidx.compose.ui.graphics.SolidColor(Color.Transparent))
                                    .clickable { tab = i },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(label, style = MaterialTheme.typography.labelLarge, color = if (sel) Color.White else scheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    val steps = when (tab) {
                        0 -> guide.pairing
                        1 -> guide.powerOn
                        else -> guide.troubleshooting
                    }
                    steps.forEachIndexed { i, s -> NumberedStep(i + 1, s) }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(scheme.surfaceVariant.copy(alpha = 0.6f)).padding(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(Icons.Rounded.Router, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("TV IP address: ${guide.findIp}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ===================================================================================== privacy policy

private data class PolicySection(val icon: ImageVector, val title: String, val body: String)

private val policy = listOf(
    PolicySection(
        Icons.Rounded.Router,
        "Your TVs stay on your network",
        "Universal Remote finds and controls TVs directly over your local Wi-Fi. TV names, IP addresses, MAC addresses and pairing keys are stored only on this phone and are never uploaded to us.",
    ),
    PolicySection(
        Icons.Rounded.Shield,
        "What we don't collect",
        "We don't run servers that collect your personal data. The app does not read your contacts, photos, files, microphone or precise location.",
    ),
    PolicySection(
        Icons.Rounded.Star,
        "Advertising",
        "The free version shows full-screen and rewarded ads from Google AdMob. Google may use device identifiers (such as the advertising ID), approximate location from your IP address and ad interactions to show and measure ads. Where required (for example in the EEA and UK) we ask for consent first, and you can change it any time in Settings › Privacy choices. Premium removes ads.",
    ),
    PolicySection(
        Icons.Rounded.WorkspacePremium,
        "Purchases",
        "Subscriptions and one-time purchases are processed securely by Google Play. We never see or store your payment details. Subscriptions can be managed or cancelled in Google Play › Payments & subscriptions.",
    ),
    PolicySection(
        Icons.Rounded.Memory,
        "Permissions",
        "Internet and Wi-Fi access are used only to discover and control TVs on your network. Vibration is used for button feedback.",
    ),
    PolicySection(
        Icons.Rounded.Delete,
        "Deleting your data",
        "Remove a TV in TV settings, or uninstall the app to delete everything it stored on this phone.",
    ),
    PolicySection(
        Icons.Rounded.Info,
        "Children & changes",
        "The app is not directed at children under 13. If this policy changes, the updated version will be shown here and on our Play Store listing.",
    ),
)

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(scheme.background)) {
        AppBar(title = "Privacy policy", subtitle = "Last updated October 2026", onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            AppCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.app_art), contentDescription = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Your privacy matters", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                        Text(
                            "Universal Remote is built to work locally. Here's exactly what happens with your data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            policy.forEach { s ->
                AppCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        IconTile(s.icon)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(s.title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                            Spacer(Modifier.height(4.dp))
                            Text(s.body, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
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
        LaunchedEffect(Unit) { onBack() }
        return
    }
    var name by remember(tv.id) { mutableStateOf(tv.name) }
    var host by remember(tv.id) { mutableStateOf(tv.host) }
    var mac by remember(tv.id) { mutableStateOf(tv.mac) }
    var confirmDelete by remember { mutableStateOf(false) }
    var online by remember { mutableStateOf<Boolean?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val extra = AppTheme.extra
    val hostValid = LocalNetwork.isValidIpv4(host)
    val macValid = mac.isBlank() || WakeOnLan.normalizeMac(mac) != null
    val changed = name.trim() != tv.name || host.trim() != tv.host || (WakeOnLan.normalizeMac(mac) ?: mac.trim()) != tv.mac

    LaunchedEffect(tv.host) { online = TvDiscovery.probe(tv.platform, tv.host) }

    Box(Modifier.fillMaxSize().background(scheme.background)) {
        Column(Modifier.fillMaxSize().imePadding()) {
            AppBar(title = "TV settings", subtitle = tv.platform.title, onBack = onBack)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        TvGlyph(tv.platform, size = 76.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(tv.name, style = MaterialTheme.typography.titleLarge, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            PlatformBadge(tv.platform)
                            when (online) {
                                true -> StatusPill("Online", extra.success)
                                false -> StatusPill("Offline", scheme.onSurfaceVariant)
                                null -> StatusPill("Checking", extra.warning)
                            }
                            if (tv.mac.isNotBlank()) StatusPill("Power on ready", scheme.primary)
                        }
                        if (tv.model.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(tv.model, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                SectionTitle("Details")
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it.take(40) },
                            label = { Text("TV name") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = fieldColors(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = host,
                            onValueChange = { v -> host = v.filter { it.isDigit() || it == '.' }.take(15) },
                            label = { Text("IP address") },
                            leadingIcon = { Icon(Icons.Rounded.Lan, contentDescription = null) },
                            singleLine = true,
                            isError = !hostValid,
                            shape = RoundedCornerShape(16.dp),
                            colors = fieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = mac,
                            onValueChange = { v -> mac = v.uppercase().filter { it.isLetterOrDigit() || it == ':' || it == '-' }.take(17) },
                            label = { Text("MAC address · for Power on") },
                            placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                            leadingIcon = { Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null) },
                            singleLine = true,
                            isError = !macValid,
                            supportingText = { if (!macValid) Text("Use 12 hex characters, e.g. AA:BB:CC:DD:EE:FF") },
                            shape = RoundedCornerShape(16.dp),
                            colors = fieldColors(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            if (tv.platform == TvPlatform.SAMSUNG || tv.platform == TvPlatform.LG || tv.platform == TvPlatform.ROKU) {
                                "Filled in automatically while the TV is connected. You can also copy it from the TV's network settings."
                            } else {
                                "Find it on the TV: ${Guides.of(tv.platform).findIp} (look for MAC / Wi-Fi MAC / Ethernet MAC)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        PrimaryButton(
                            "Save changes",
                            icon = Icons.Rounded.Save,
                            enabled = changed && hostValid && macValid && name.isNotBlank(),
                            onClick = {
                                container.tvStore.update(tv.id) {
                                    it.copy(name = name.trim(), host = host.trim(), mac = WakeOnLan.normalizeMac(mac) ?: "")
                                }
                                scope.launch { snackbar.show("Changes saved", SnackKind.SUCCESS) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                SectionTitle("Tools")
                AppCard(Modifier.fillMaxWidth()) {
                    Column {
                        NavRow(Icons.Rounded.PowerSettingsNew, "Test power on", "Send a Wake-on-LAN signal now") {
                            val m = WakeOnLan.normalizeMac(tv.mac)
                            scope.launch {
                                if (m == null) {
                                    snackbar.show("Add the TV's MAC address first, then save.", SnackKind.INFO)
                                } else {
                                    WakeOnLan.send(context, m, tv.host)
                                    snackbar.show("Wake signal sent to ${tv.name}", SnackKind.SUCCESS)
                                }
                            }
                        }
                        RowDivider()
                        NavRow(Icons.Rounded.Link, "Pair again", "Fixes most connection problems") {
                            container.certPins.clearHost(tv.host)
                            onRepair(DiscoveredTv(tv.platform, tv.host, tv.name, tv.model, tv.mac))
                        }
                        RowDivider()
                        NavRow(Icons.Rounded.Build, "Power-on setup guide", "Settings to turn on in your TV") { onHelp(tv.platform) }
                    }
                }

                Spacer(Modifier.height(20.dp))
                SecondaryButton(
                    "Remove this TV",
                    icon = Icons.Rounded.Delete,
                    tint = scheme.error,
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
                Spacer(Modifier.navigationBarsPadding())
            }
        }
        AppSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }

    if (confirmDelete) {
        AppDialog(
            onDismiss = { confirmDelete = false },
            icon = Icons.Rounded.Delete,
            iconColor = scheme.error,
            title = "Remove ${tv.name}?",
            message = "You'll need to pair this TV again to control it.",
            confirmText = "Remove TV",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                container.tvStore.remove(tv.id)
                container.certPins.clearHost(tv.host)
                onDeleted()
            },
        )
    }
}

@Suppress("unused")
private val unusedAlign = TextAlign.Center
