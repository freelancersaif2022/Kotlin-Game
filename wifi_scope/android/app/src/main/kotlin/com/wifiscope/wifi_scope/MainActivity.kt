package com.wifiscope.wifi_scope

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val channelName = "wifiscope/wifi"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "getConnection" -> result.success(readConnection())
                    "openWifiSettings" -> {
                        openWifiSettings()
                        result.success(true)
                    }
                    "openLocationSettings" -> {
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        result.success(true)
                    }
                    "isLocationEnabled" -> {
                        result.success(isLocationEnabled())
                    }
                    else -> result.notImplemented()
                }
            }
    }

    private fun isLocationEnabled(): Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    private fun openWifiSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        startActivity(intent)
    }

    // WifiManager.connectionInfo is deprecated but remains the only synchronous way to read the
    // live RSSI and link speed of the current connection, which the survey samples several times
    // per second.
    @Suppress("DEPRECATION")
    private fun readConnection(): Map<String, Any?> {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wm.isWifiEnabled) return mapOf("wifiEnabled" to false, "connected" to false)

        val info: WifiInfo = wm.connectionInfo
            ?: return mapOf("wifiEnabled" to true, "connected" to false)
        val connected = info.networkId != -1 && info.rssi > -127
        val ssid = info.ssid?.removeSurrounding("\"")?.takeUnless { it == "<unknown ssid>" }

        val map = mutableMapOf<String, Any?>(
            "wifiEnabled" to true,
            "connected" to connected,
            "ssid" to ssid,
            "bssid" to info.bssid?.takeUnless { it == "02:00:00:00:00:00" },
            "rssi" to info.rssi,
            "linkSpeed" to info.linkSpeed,
            "frequency" to info.frequency,
            "ip" to formatIp(info.ipAddress),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            map["txLinkSpeed"] = info.txLinkSpeedMbps
            map["rxLinkSpeed"] = info.rxLinkSpeedMbps
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            map["standard"] = info.wifiStandard
            map["maxTxSpeed"] = info.maxSupportedTxLinkSpeedMbps
        }
        return map
    }

    private fun formatIp(ip: Int): String? {
        if (ip == 0) return null
        return "${ip and 0xff}.${ip shr 8 and 0xff}.${ip shr 16 and 0xff}.${ip shr 24 and 0xff}"
    }
}
