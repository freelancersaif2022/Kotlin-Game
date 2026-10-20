package com.flashshare.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

/** What a flow needs before it can start. */
enum class PermFlow { SEND, RECEIVE, JOIN, CLONE_SEND, FILES, ALL }

enum class Need(val title: String, val why: String) {
    NEARBY("Nearby devices", "Creates the direct 5 GHz Wi-Fi link between phones – no internet used."),
    LOCATION("Location access", "Android 12 and older need it to use Wi-Fi Direct. We never track you."),
    CAMERA("Camera", "Scan the QR code on the other phone to connect instantly."),
    MEDIA("Photos, videos & music", "Pick what to send and save what you receive."),
    CONTACTS("Contacts", "Copy your contacts to your new phone with Phone Clone."),
    NOTIFICATIONS("Notifications", "Shows live transfer progress when the app is in the background."),
    WIFI_ON("Wi-Fi turned on", "Wi-Fi must be on for the ultra-fast direct link (you don't need a network)."),
    LOCATION_ON("Location turned on", "Android 12 and older need Location on to find nearby phones.");

    val optional: Boolean get() = this == NOTIFICATIONS
}

object Perms {
    fun permissionsFor(need: Need): Array<String> = when (need) {
        Need.NEARBY -> if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES) else emptyArray()
        Need.LOCATION -> if (Build.VERSION.SDK_INT < 33) arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) else emptyArray()
        Need.CAMERA -> arrayOf(Manifest.permission.CAMERA)
        Need.MEDIA -> when {
            Build.VERSION.SDK_INT >= 34 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
            Build.VERSION.SDK_INT >= 29 -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        Need.CONTACTS -> arrayOf(Manifest.permission.READ_CONTACTS)
        Need.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray()
        Need.WIFI_ON, Need.LOCATION_ON -> emptyArray()
    }

    fun needsFor(flow: PermFlow): List<Need> {
        val base = buildList {
            if (Build.VERSION.SDK_INT >= 33) add(Need.NEARBY) else add(Need.LOCATION)
            add(Need.WIFI_ON)
            if (Build.VERSION.SDK_INT < 33) add(Need.LOCATION_ON)
        }
        return when (flow) {
            PermFlow.SEND -> base + listOf(Need.CAMERA, Need.MEDIA, Need.NOTIFICATIONS)
            PermFlow.RECEIVE -> base + listOfNotNull(if (Build.VERSION.SDK_INT <= 28) Need.MEDIA else null, Need.NOTIFICATIONS)
            PermFlow.JOIN -> base + listOfNotNull(Need.CAMERA, if (Build.VERSION.SDK_INT <= 28) Need.MEDIA else null, Need.NOTIFICATIONS)
            PermFlow.CLONE_SEND -> base + listOf(Need.CAMERA, Need.MEDIA, Need.CONTACTS, Need.NOTIFICATIONS)
            PermFlow.FILES -> listOf(Need.MEDIA)
            PermFlow.ALL -> base + listOf(Need.CAMERA, Need.MEDIA, Need.CONTACTS, Need.NOTIFICATIONS)
        }.filter { it == Need.WIFI_ON || it == Need.LOCATION_ON || permissionsFor(it).isNotEmpty() }
    }

    fun isGranted(ctx: Context, need: Need): Boolean = when (need) {
        Need.WIFI_ON -> ctx.applicationContext.getSystemService(WifiManager::class.java).isWifiEnabled
        Need.LOCATION_ON -> Build.VERSION.SDK_INT >= 33 ||
            LocationManagerCompat.isLocationEnabled(ctx.getSystemService(LocationManager::class.java))
        Need.MEDIA -> mediaGranted(ctx)
        Need.LOCATION -> granted(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
        else -> permissionsFor(need).all { granted(ctx, it) }
    }

    /** Android 14 "selected photos only" counts as granted (we show a hint to allow all). */
    fun mediaGranted(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 34 && granted(ctx, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)) return true
        return when {
            Build.VERSION.SDK_INT >= 33 -> granted(ctx, Manifest.permission.READ_MEDIA_IMAGES) || granted(ctx, Manifest.permission.READ_MEDIA_VIDEO) || granted(ctx, Manifest.permission.READ_MEDIA_AUDIO)
            else -> granted(ctx, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun mediaPartial(ctx: Context): Boolean = Build.VERSION.SDK_INT >= 34 &&
        granted(ctx, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) && !granted(ctx, Manifest.permission.READ_MEDIA_IMAGES)

    fun allGranted(ctx: Context, flow: PermFlow): Boolean = needsFor(flow).filterNot { it.optional }.all { isGranted(ctx, it) }

    private fun granted(ctx: Context, p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
}
