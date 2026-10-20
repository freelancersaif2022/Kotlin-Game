package com.flashshare.app.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var v = bytes / 1024.0
    var i = 0
    while (v >= 1024 && i < units.size - 1) { v /= 1024; i++ }
    return if (v >= 100) "%.0f %s".format(v, units[i]) else "%.1f %s".format(v, units[i])
}

fun formatSpeed(bytesPerSec: Double): String {
    val mb = bytesPerSec / (1024 * 1024)
    return when {
        mb >= 10 -> "%.0f MB/s".format(mb)
        mb >= 1 -> "%.1f MB/s".format(mb)
        else -> "%.0f KB/s".format(bytesPerSec / 1024)
    }
}

fun formatEta(sec: Long): String = when {
    sec < 0 -> "--"
    sec < 60 -> "${sec}s"
    sec < 3600 -> "${sec / 60}m ${sec % 60}s"
    else -> "${sec / 3600}h ${(sec % 3600) / 60}m"
}

fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return if (s < 60) "${s}s" else "${s / 60}:%02d".format(s % 60)
}

fun formatDate(ms: Long): String = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(ms))

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Shows a professional in-app snackbar (falls back to nothing if no screen is visible). */
@Suppress("UnusedReceiverParameter")
fun Context.toast(msg: String, kind: com.flashshare.app.ui.components.SnackKind? = null) {
    if (kind != null) com.flashshare.app.ui.components.Snack.show(msg, kind) else com.flashshare.app.ui.components.Snack.show(msg)
}

/** Opens a received / local file with the right app (APKs go to the package installer). */
fun Context.openFile(uriString: String?, mime: String) {
    if (uriString == null) { toast("File not available"); return }
    var uri = Uri.parse(uriString)
    if (uri.scheme == "file") uri = FileProvider.getUriForFile(this, "$packageName.files", File(uri.path!!))
    val isApk = mime == "application/vnd.android.package-archive"
    if (isApk && Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
        toast("Allow FlashShare to install apps, then tap again")
        runCatching {
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        return
    }
    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        runCatching { startActivity(Intent.createChooser(intent.setDataAndType(uri, "*/*"), "Open with").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { toast("No app can open this file") }
    } catch (e: Exception) {
        toast("Couldn't open file")
    }
}

fun Context.shareFiles(uris: List<Uri>, mime: String = "*/*") {
    if (uris.isEmpty()) return
    val fixed = uris.map { if (it.scheme == "file") FileProvider.getUriForFile(this, "$packageName.files", File(it.path!!)) else it }
    val intent = if (fixed.size == 1) {
        Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, fixed[0])
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).setType(mime).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(fixed))
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { startActivity(Intent.createChooser(intent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

fun Context.openUrl(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { toast("No browser found") }
}
