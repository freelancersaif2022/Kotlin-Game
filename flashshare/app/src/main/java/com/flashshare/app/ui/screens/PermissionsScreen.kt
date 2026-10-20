package com.flashshare.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.flashshare.app.ui.components.AppCard
import com.flashshare.app.ui.components.AppScaffold
import com.flashshare.app.ui.components.GradientButton
import com.flashshare.app.ui.components.GlossyIcon
import com.flashshare.app.ui.components.IconBrushes
import com.flashshare.app.ui.theme.Brand
import com.flashshare.app.ui.theme.LocalExtra
import com.flashshare.app.util.Need
import com.flashshare.app.util.PermFlow
import com.flashshare.app.util.Perms
import com.flashshare.app.util.findActivity

private fun needIcon(n: Need): ImageVector = when (n) {
    Need.NEARBY -> Icons.Rounded.WifiTethering
    Need.LOCATION -> Icons.Rounded.LocationOn
    Need.CAMERA -> Icons.Rounded.QrCodeScanner
    Need.MEDIA -> Icons.Rounded.PhotoLibrary
    Need.ALL_FILES -> Icons.Rounded.FolderOpen
    Need.CONTACTS -> Icons.Rounded.Contacts
    Need.NOTIFICATIONS -> Icons.Rounded.NotificationsActive
    Need.WIFI_ON -> Icons.Rounded.Wifi
    Need.LOCATION_ON -> Icons.Rounded.MyLocation
}

@Composable
fun PermissionsScreen(flow: PermFlow, onBack: () -> Unit, onGranted: () -> Unit) {
    val ctx = LocalContext.current
    val needs = remember(flow) { Perms.needsFor(flow) }
    var tick by remember { mutableIntStateOf(0) }
    val status = remember(tick) { needs.associateWith { Perms.isGranted(ctx, it) } }
    var asked by rememberSaveable { mutableStateOf(setOf<String>()) }
    val allRequired = needs.filterNot { it.optional }.all { status[it] == true }
    var autoContinued by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }

    fun openAppSettings() {
        ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun request(need: Need) {
        when (need) {
            Need.WIFI_ON -> if (Build.VERSION.SDK_INT >= 29) {
                ctx.startActivity(Intent(Settings.Panel.ACTION_WIFI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } else {
                @Suppress("DEPRECATION")
                ctx.applicationContext.getSystemService(WifiManager::class.java).isWifiEnabled = true
                tick++
            }
            Need.LOCATION_ON -> ctx.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Need.ALL_FILES -> requestAllFiles(ctx)
            else -> {
                val perms = Perms.permissionsFor(need)
                val activity = ctx.findActivity()
                // Asked before and Android won't show the dialog again → only Settings can grant it.
                val blocked = need.name in asked && activity != null &&
                    perms.none { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
                if (blocked) openAppSettings() else {
                    asked = asked + need.name
                    launcher.launch(perms)
                }
            }
        }
    }

    LaunchedEffect(allRequired) {
        if (allRequired && !autoContinued) {
            autoContinued = true
            onGranted()
        }
    }

    AppScaffold(title = "", onBack = onBack) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val t = rememberInfiniteTransition(label = "shield")
                        val s by t.animateFloat(0.94f, 1.04f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "s")
                        GlossyIcon(Icons.Rounded.Shield, Brand.main, 88.dp, Modifier.scale(s))
                        Spacer(Modifier.height(16.dp))
                        Text("Quick setup", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "FlashShare connects phones directly over Wi-Fi. These permissions are only used for that – your files never touch the internet.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                        )
                    }
                }
                items(needs, key = { it.name }) { n ->
                    PermRow(n, status[n] == true, blocked = n.name in asked && status[n] != true && !n.special) { request(n) }
                }
                if (Perms.mediaPartial(ctx) && Need.MEDIA in needs) {
                    item {
                        Text(
                            "You allowed only some photos. Tap “Photos, videos & music” again to allow all for the full gallery.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            val firstMissing = needs.firstOrNull { status[it] != true && !it.optional }
            GradientButton(
                text = when {
                    allRequired -> "Continue"
                    firstMissing == Need.WIFI_ON -> "Turn on Wi-Fi"
                    firstMissing == Need.LOCATION_ON -> "Turn on Location"
                    firstMissing == Need.ALL_FILES -> "Allow all files access"
                    else -> "Allow access"
                },
                icon = if (allRequired) Icons.Rounded.Check else Icons.Rounded.LockOpen,
                onClick = {
                    if (allRequired) onGranted()
                    else {
                        val runtime = needs.filter { status[it] != true && Perms.permissionsFor(it).isNotEmpty() }
                        val unblocked = runtime.filter { it.name !in asked }
                        when {
                            unblocked.isNotEmpty() -> {
                                asked = asked + unblocked.map { it.name }
                                launcher.launch(unblocked.flatMap { Perms.permissionsFor(it).toList() }.distinct().toTypedArray())
                            }
                            firstMissing != null -> request(firstMissing)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun PermRow(need: Need, granted: Boolean, blocked: Boolean, onClick: () -> Unit) {
    val extra = LocalExtra.current
    val iconBg by animateColorAsState(if (granted) extra.success.copy(alpha = 0.14f) else MaterialTheme.colorScheme.primaryContainer, label = "bg")
    AppCard(Modifier.fillMaxWidth(), onClick = if (granted) null else onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            GlossyIcon(needIcon(need), if (granted) IconBrushes.mint else Brand.main, 46.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(need.title, style = MaterialTheme.typography.titleSmall)
                    if (need.optional) {
                        Spacer(Modifier.width(6.dp))
                        Text("Optional", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(need.why, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(10.dp))
            if (granted) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(extra.success), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            } else {
                FilledTonalButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 14.dp)) {
                    Text(if (blocked) "Settings" else if (need == Need.WIFI_ON || need == Need.LOCATION_ON) "Turn on" else "Allow", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
