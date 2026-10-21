package com.netscope.wifianalyzer.net

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.max

data class AccessPoint(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val channel: Int,
    val centerChannel: Int,
    val widthMhz: Int,
    val band: String,
    val security: String,
    val standard: String?,
    val isConnected: Boolean,
) {
    val hidden: Boolean get() = ssid.isBlank()
    val label: String get() = ssid.ifBlank { "Hidden network" }
}

data class ChannelAdvice(val band: String, val bestChannel: Int, val currentChannel: Int?, val crowdByChannel: Map<Int, Int>)

class WifiScanner(private val context: Context) {
    private val wm = context.applicationContext.getSystemService(WifiManager::class.java)

    /** Asks Android for a fresh scan. Returns false when the system throttled the request. */
    suspend fun scan(): Boolean {
        val manager = wm ?: return false
        val ok = withTimeoutOrNull(8_000) {
            suspendCancellableCoroutine { cont ->
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(c: Context?, intent: Intent?) {
                        runCatching { context.unregisterReceiver(this) }
                        val updated = intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false) ?: false
                        if (cont.isActive) cont.resume(updated)
                    }
                }
                ContextCompat.registerReceiver(
                    context, receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED,
                )
                @Suppress("DEPRECATION")
                val started = runCatching { manager.startScan() }.getOrDefault(false)
                if (!started) {
                    runCatching { context.unregisterReceiver(receiver) }
                    if (cont.isActive) cont.resume(false)
                }
                cont.invokeOnCancellation { runCatching { context.unregisterReceiver(receiver) } }
            }
        }
        return ok == true
    }

    @Suppress("DEPRECATION")
    fun results(): List<AccessPoint> {
        val manager = wm ?: return emptyList()
        val connectedBssid = runCatching { manager.connectionInfo?.bssid }.getOrNull()
        val raw: List<ScanResult> = runCatching { manager.scanResults }.getOrNull().orEmpty()
        return raw.map { r ->
            val width = when (r.channelWidth) {
                ScanResult.CHANNEL_WIDTH_40MHZ -> 40
                ScanResult.CHANNEL_WIDTH_80MHZ -> 80
                ScanResult.CHANNEL_WIDTH_160MHZ, ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> 160
                5 -> 320
                else -> 20
            }
            val center = if (width > 20 && r.centerFreq0 > 0) WifiUtils.frequencyToChannel(r.centerFreq0) else WifiUtils.frequencyToChannel(r.frequency)
            AccessPoint(
                ssid = r.SSID.orEmpty(),
                bssid = r.BSSID.orEmpty(),
                rssi = r.level,
                frequency = r.frequency,
                channel = WifiUtils.frequencyToChannel(r.frequency),
                centerChannel = center,
                widthMhz = width,
                band = WifiUtils.bandName(r.frequency),
                security = WifiUtils.securityFromCapabilities(r.capabilities.orEmpty()),
                standard = if (Build.VERSION.SDK_INT >= 30) WifiUtils.standardName(r.wifiStandard) else null,
                isConnected = connectedBssid != null && connectedBssid == r.BSSID,
            )
        }.sortedByDescending { it.rssi }
    }

    companion object {
        private val CANDIDATES_24 = listOf(1, 6, 11)
        private val CANDIDATES_5 = listOf(36, 40, 44, 48, 149, 153, 157, 161, 165)

        private fun weight(rssi: Int): Double = max(0, rssi + 100).toDouble()

        /** Picks the least congested channel, weighting stronger neighbours more. */
        fun advise(aps: List<AccessPoint>, band: String): ChannelAdvice {
            val inBand = aps.filter { it.band == band }
            val current = inBand.firstOrNull { it.isConnected }?.channel
            val candidates = if (band == "2.4 GHz") CANDIDATES_24 else if (band == "5 GHz") CANDIDATES_5 else
                inBand.map { it.channel }.distinct().ifEmpty { listOf(5) }
            val crowd = mutableMapOf<Int, Int>()
            inBand.forEach { crowd[it.channel] = (crowd[it.channel] ?: 0) + 1 }
            val scores = candidates.associateWith { c ->
                inBand.filterNot { it.isConnected }.sumOf { ap ->
                    val span = if (band == "2.4 GHz") 4 else ap.widthMhz / 10
                    val dist = abs(ap.centerChannel - c)
                    val overlap = if (band == "2.4 GHz") (1.0 - dist / 5.0).coerceAtLeast(0.0)
                    else if (dist <= span) 1.0 else 0.0
                    overlap * weight(ap.rssi)
                }
            }
            val best = scores.minByOrNull { it.value }?.key ?: candidates.first()
            return ChannelAdvice(band, best, current, crowd)
        }
    }
}
