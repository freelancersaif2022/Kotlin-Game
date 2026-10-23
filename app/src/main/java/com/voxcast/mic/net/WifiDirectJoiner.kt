package com.voxcast.mic.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Microphone side of Wi-Fi Direct. On Android 10+ the phone joins the speaker's
 * group directly (Android shows a one-tap "Connect" confirmation). Older phones must
 * join the network once from Wi-Fi settings using the name/password shown on screen.
 */
class WifiDirectJoiner(context: Context) {
    private val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var callback: ConnectivityManager.NetworkCallback? = null

    val canJoinAutomatically: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /** Returns the joined network, or null when the phone should use its current connection. */
    suspend fun join(ssid: String, passphrase: String): Result<Network?> {
        if (NetworkUtils.localLinks().any { it.ip.startsWith("192.168.49.") }) return Result.success(null)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return Result.success(null)

        val spec = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(passphrase)
            .build()
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(spec)
            .build()

        val network = withTimeoutOrNull(60_000) {
            suspendCancellableCoroutine<Network?> { cont ->
                val cb = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) { if (cont.isActive) cont.resume(network) }
                    override fun onUnavailable() { if (cont.isActive) cont.resume(null) }
                }
                callback = cb
                runCatching { cm.requestNetwork(request, cb) }.onFailure { if (cont.isActive) cont.resume(null) }
            }
        }
        return if (network != null) Result.success(network)
        else {
            release()
            Result.failure(IllegalStateException("Could not join $ssid. Tap \"Connect\" when Android asks"))
        }
    }

    fun release() {
        callback?.let { runCatching { cm.unregisterNetworkCallback(it) } }
        callback = null
    }
}
