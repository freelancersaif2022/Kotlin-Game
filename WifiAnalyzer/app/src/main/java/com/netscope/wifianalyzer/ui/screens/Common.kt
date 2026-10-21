package com.netscope.wifianalyzer.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.netscope.wifianalyzer.net.WifiSnapshot
import com.netscope.wifianalyzer.net.WifiUtils
import com.netscope.wifianalyzer.ui.components.AppCard
import com.netscope.wifianalyzer.ui.components.IconBadge
import com.netscope.wifianalyzer.ui.components.LocalSnackbar
import com.netscope.wifianalyzer.ui.components.SnackType
import com.netscope.wifianalyzer.ui.components.SoftButton
import com.netscope.wifianalyzer.ui.components.findActivity
import com.netscope.wifianalyzer.ui.theme.AppColors
import com.netscope.wifianalyzer.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class NetState(
    val snap: WifiSnapshot,
    val hasLocation: Boolean,
    val locationOn: Boolean,
    val wifiOn: Boolean,
) {
    val needsLocation: Boolean get() = snap.wifiConnected && (!hasLocation || !locationOn)
}

private fun readNetState(context: Context) = NetState(
    snap = WifiUtils.snapshot(context),
    hasLocation = WifiUtils.hasLocationPermission(context),
    locationOn = WifiUtils.isLocationEnabled(context),
    wifiOn = WifiUtils.isWifiEnabled(context),
)

/** Live Wi-Fi state, polled while the screen is visible. */
@Composable
fun rememberNetState(intervalMs: Long = 2_000): State<NetState> {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val initial = remember { readNetState(context) }
    return produceState(initial, intervalMs) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = withContext(Dispatchers.IO) { readNetState(context) }
                delay(intervalMs)
            }
        }
    }
}

val LOCATION_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

/** Requests location; explains and links to Settings if it was permanently denied. */
@Composable
fun rememberLocationRequest(onGranted: () -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val launcher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
            if (res[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                snackbar.show("Location access granted", SnackType.Success)
                onGranted()
            } else {
                val activity = context.findActivity()
                val permanentlyDenied = activity != null &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                if (permanentlyDenied) {
                    snackbar.show("Allow \"Precise location\" in Settings to see Wi-Fi names", SnackType.Error, "Settings") {
                        openAppSettings(context)
                    }
                } else {
                    snackbar.show("Precise location is needed to read Wi-Fi names and nearby networks", SnackType.Info)
                }
            }
        }
    return remember(launcher) {
        {
            if (WifiUtils.hasLocationPermission(context)) {
                if (!WifiUtils.isLocationEnabled(context)) openLocationSettings(context) else onGranted()
            } else {
                launcher.launch(LOCATION_PERMISSIONS)
            }
        }
    }
}

fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

fun openLocationSettings(context: Context) {
    runCatching { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

fun openWifiSettings(context: Context) {
    runCatching { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

fun openUrl(context: Context, url: String, dark: Boolean = true) {
    try {
        val params = CustomTabColorSchemeParams.Builder().setToolbarColor(if (dark) 0xFF0B1124.toInt() else 0xFFFFFFFF.toInt()).build()
        CustomTabsIntent.Builder().setDefaultColorSchemeParams(params).setShowTitle(true).build()
            .launchUrl(context, Uri.parse(url))
    } catch (e: Exception) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
        }
    }
}

fun shareText(context: Context, text: String, title: String = "Share") {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    runCatching { context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

fun openPlayStore(context: Context) {
    val pkg = context.packageName
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        openUrl(context, "https://play.google.com/store/apps/details?id=$pkg")
    }
}

fun openSubscriptions(context: Context) {
    openUrl(context, "https://play.google.com/store/account/subscriptions?package=${context.packageName}")
}

/** Card shown when Android needs location to reveal the Wi-Fi name / scan results. */
@Composable
fun LocationNeededCard(net: NetState, modifier: Modifier = Modifier, onGranted: () -> Unit = {}) {
    val request = rememberLocationRequest(onGranted)
    val context = LocalContext.current
    if (!net.wifiOn) {
        AppCard(modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.WifiOff, Palette.Rose)
                Spacer(Modifier.width(14.dp))
                Text(
                    "Wi-Fi is turned off. Turn it on to analyze your network.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            SoftButton("Open Wi-Fi settings", onClick = { openWifiSettings(context) }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
        }
        return
    }
    if (net.hasLocation && net.locationOn) return
    AppCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.LocationOn, Palette.Gold)
            Spacer(Modifier.width(14.dp))
            Text(
                if (!net.hasLocation) {
                    "Android requires precise location permission to show your Wi-Fi name and nearby networks. Your location is never stored or shared."
                } else {
                    "Turn on device location so Android can show your Wi-Fi name and scan nearby networks."
                },
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.ext.muted,
                modifier = Modifier.weight(1f),
            )
        }
        SoftButton(
            if (!net.hasLocation) "Allow location access" else "Turn on location",
            onClick = request,
            tint = Palette.Gold,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        )
    }
}
