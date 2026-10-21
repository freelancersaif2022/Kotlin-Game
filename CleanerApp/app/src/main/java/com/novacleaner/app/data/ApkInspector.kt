package com.novacleaner.app.data

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

enum class ApkStatus(val label: String) {
    INSTALLED("Installed"),
    OUTDATED("Older than installed"),
    NEWER("Newer than installed"),
    NOT_INSTALLED("Not installed"),
    DAMAGED("Damaged package"),
}

data class ApkMeta(val label: String, val packageName: String, val versionName: String?, val status: ApkStatus)

object ApkInspector {

    @Suppress("DEPRECATION")
    private fun PackageInfo.code(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) longVersionCode else versionCode.toLong()

    fun inspect(pm: PackageManager, path: String): ApkMeta? = runCatching {
        val info = pm.getPackageArchiveInfo(path, 0) ?: return null
        val appInfo = info.applicationInfo
        appInfo?.sourceDir = path
        appInfo?.publicSourceDir = path
        val label = appInfo?.loadLabel(pm)?.toString() ?: info.packageName
        val installed = runCatching { pm.getPackageInfo(info.packageName, 0) }.getOrNull()
        val status = when {
            installed == null -> ApkStatus.NOT_INSTALLED
            installed.code() == info.code() -> ApkStatus.INSTALLED
            installed.code() > info.code() -> ApkStatus.OUTDATED
            else -> ApkStatus.NEWER
        }
        ApkMeta(label, info.packageName, info.versionName, status)
    }.getOrNull()
}
