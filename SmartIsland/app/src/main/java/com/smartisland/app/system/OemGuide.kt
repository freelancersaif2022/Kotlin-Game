package com.smartisland.app.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

enum class Oem(val brand: String) {
    XIAOMI("Xiaomi / Redmi / POCO"),
    SAMSUNG("Samsung"),
    OPPO("OPPO / Realme / OnePlus"),
    VIVO("vivo / iQOO"),
    HUAWEI("Huawei / Honor"),
    ASUS("ASUS"),
    OTHER("Android"),
}

data class OemGuide(
    val oem: Oem,
    val steps: List<String>,
    val autostartIntents: List<Intent>,
    val batteryIntents: List<Intent>,
)

/**
 * Vendor skins kill background services aggressively. These are the screens users must visit to keep
 * the island alive; each list is tried in order since component names move between ROM versions.
 */
object OemGuides {

    fun detect(): Oem {
        val m = (Build.MANUFACTURER + " " + Build.BRAND).lowercase()
        return when {
            listOf("xiaomi", "redmi", "poco").any { it in m } -> Oem.XIAOMI
            "samsung" in m -> Oem.SAMSUNG
            listOf("oppo", "realme", "oneplus").any { it in m } -> Oem.OPPO
            listOf("vivo", "iqoo").any { it in m } -> Oem.VIVO
            listOf("huawei", "honor").any { it in m } -> Oem.HUAWEI
            "asus" in m -> Oem.ASUS
            else -> Oem.OTHER
        }
    }

    fun forDevice(context: Context): OemGuide = guide(detect(), context)

    fun guide(oem: Oem, context: Context): OemGuide {
        val pkg = context.packageName
        fun cmp(p: String, c: String) = Intent().setComponent(ComponentName(p, c))
        return when (oem) {
            Oem.XIAOMI -> OemGuide(
                oem,
                listOf(
                    "Open Autostart and turn ON Smart Island.",
                    "Battery saver → Smart Island → choose \"No restrictions\".",
                    "Other permissions → allow \"Display pop-up windows while running in the background\" and \"Show on lock screen\".",
                    "Open Recents, long-press Smart Island and tap the lock icon.",
                ),
                listOf(
                    cmp("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                    cmp("com.miui.securitycenter", "com.miui.powercenter.PowerSettings"),
                ),
                listOf(
                    Intent("miui.intent.action.HIDDEN_APPS_CONFIG_ACTIVITY")
                        .putExtra("package_name", pkg).putExtra("package_label", "Smart Island"),
                    cmp("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")
                        .putExtra("package_name", pkg).putExtra("package_label", "Smart Island"),
                    Intent("miui.intent.action.APP_PERM_EDITOR").putExtra("extra_pkgname", pkg),
                ),
            )
            Oem.SAMSUNG -> OemGuide(
                oem,
                listOf(
                    "Settings → Battery → Background usage limits.",
                    "Make sure Smart Island is NOT in \"Sleeping apps\" or \"Deep sleeping apps\".",
                    "Add Smart Island to \"Never sleeping apps\".",
                    "App info → Battery → choose \"Unrestricted\".",
                ),
                listOf(
                    cmp("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
                    cmp("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.battery.BatteryActivity"),
                ),
                listOf(cmp("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.usage.CheckableAppListActivity")),
            )
            Oem.OPPO -> OemGuide(
                oem,
                listOf(
                    "Open Startup manager / Auto launch and enable Smart Island.",
                    "App info → Battery usage → allow background activity and auto launch.",
                    "Turn off \"Optimize battery use\" for Smart Island.",
                    "Lock Smart Island in Recents (pull the card down).",
                ),
                listOf(
                    cmp("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                    cmp("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
                    cmp("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
                    cmp("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
                ),
                listOf(cmp("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity")),
            )
            Oem.VIVO -> OemGuide(
                oem,
                listOf(
                    "i Manager → App manager → Autostart → enable Smart Island.",
                    "Settings → Battery → Background power consumption → allow Smart Island.",
                    "Lock Smart Island in Recents.",
                ),
                listOf(
                    cmp("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                    cmp("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
                    cmp("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
                ),
                listOf(cmp("com.vivo.abe", "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity")),
            )
            Oem.HUAWEI -> OemGuide(
                oem,
                listOf(
                    "Phone Manager → App launch → Smart Island → switch to \"Manage manually\".",
                    "Enable Auto-launch, Secondary launch and Run in background.",
                    "Lock Smart Island in Recents.",
                ),
                listOf(
                    cmp("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                    cmp("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
                    cmp("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                ),
                emptyList(),
            )
            Oem.ASUS -> OemGuide(
                oem,
                listOf(
                    "Mobile Manager → Auto-start manager → allow Smart Island.",
                    "Turn off battery optimization for Smart Island.",
                ),
                listOf(
                    cmp("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity"),
                    cmp("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity"),
                ),
                emptyList(),
            )
            Oem.OTHER -> OemGuide(
                oem,
                listOf(
                    "Settings → Apps → Smart Island → Battery → choose \"Unrestricted\".",
                    "Keep the accessibility service turned on.",
                    "Avoid clearing Smart Island from Recents with a cleaner app.",
                ),
                emptyList(),
                emptyList(),
            )
        }
    }
}
