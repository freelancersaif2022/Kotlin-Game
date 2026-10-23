package com.voxcast.mic.net

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class P2pGroupInfo(val ssid: String, val passphrase: String, val ownerIp: String)

/**
 * Speaker side of Wi-Fi Direct: creates an autonomous group (a mini hotspot made by the
 * Wi-Fi chip itself, no router or mobile data needed). The microphone joins it as a
 * regular Wi-Fi client using the name and password carried in the QR code.
 */
class WifiDirectHost(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    private var channel: WifiP2pManager.Channel? = null

    val isSupported: Boolean get() = manager != null

    @SuppressLint("MissingPermission")
    suspend fun start(): Result<P2pGroupInfo> {
        val mgr = manager ?: return Result.failure(IllegalStateException("Wi-Fi Direct is not supported on this phone"))
        val ch = mgr.initialize(appContext, Looper.getMainLooper(), null)
            ?: return Result.failure(IllegalStateException("Wi-Fi Direct is unavailable. Turn Wi-Fi on and try again"))
        channel = ch

        // Remove any leftover group from a previous session.
        awaitAction { mgr.removeGroup(ch, it) }
        delay(300)

        val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val config = WifiP2pConfig.Builder()
                .setNetworkName("DIRECT-VX-" + ConnectionInfo.randomToken(4))
                .setPassphrase(ConnectionInfo.randomToken(12))
                .enablePersistentMode(false)
                .build()
            awaitAction { mgr.createGroup(ch, config, it) }
        } else {
            awaitAction { mgr.createGroup(ch, it) }
        }
        if (created != null) {
            return Result.failure(IllegalStateException(reasonText(created)))
        }

        repeat(20) {
            val group = requestGroup(mgr, ch)
            if (group != null && group.isGroupOwner && !group.networkName.isNullOrBlank() && !group.passphrase.isNullOrBlank()) {
                val ip = NetworkUtils.localLinks().firstOrNull { it.kind == LinkKind.WIFI_DIRECT }?.ip
                    ?: NetworkUtils.WIFI_DIRECT_OWNER_IP
                return Result.success(P2pGroupInfo(group.networkName, group.passphrase, ip))
            }
            delay(400)
        }
        return Result.failure(IllegalStateException("Wi-Fi Direct group did not start. Toggle Wi-Fi and retry"))
    }

    fun stop() {
        val mgr = manager ?: return
        val ch = channel ?: return
        runCatching { mgr.removeGroup(ch, null) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) runCatching { ch.close() }
        channel = null
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestGroup(mgr: WifiP2pManager, ch: WifiP2pManager.Channel): WifiP2pGroup? =
        suspendCancellableCoroutine { cont ->
            runCatching { mgr.requestGroupInfo(ch) { g -> if (cont.isActive) cont.resume(g) } }
                .onFailure { if (cont.isActive) cont.resume(null) }
        }

    /** Runs a P2P action; returns null on success or the failure reason code. */
    private suspend fun awaitAction(block: (WifiP2pManager.ActionListener) -> Unit): Int? =
        suspendCancellableCoroutine { cont ->
            val listener = object : WifiP2pManager.ActionListener {
                override fun onSuccess() { if (cont.isActive) cont.resume(null) }
                override fun onFailure(reason: Int) { if (cont.isActive) cont.resume(reason) }
            }
            runCatching { block(listener) }.onFailure { if (cont.isActive) cont.resume(WifiP2pManager.ERROR) }
        }

    private fun reasonText(reason: Int) = when (reason) {
        WifiP2pManager.P2P_UNSUPPORTED -> "Wi-Fi Direct is not supported on this phone"
        WifiP2pManager.BUSY -> "Wi-Fi Direct is busy. Turn Wi-Fi off and on, then retry"
        else -> "Could not start Wi-Fi Direct. Make sure Wi-Fi is on (and Location on older Android)"
    }
}
