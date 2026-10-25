package com.twinspace.app.core

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.CrossProfileApps
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.PersistableBundle
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.twinspace.app.admin.SpaceAdminReceiver

enum class SpaceStatus {
    /** The device can't host a work profile. */
    UNSUPPORTED,

    /** No space yet. */
    NONE,

    /** Another app already owns this device's work profile. */
    FOREIGN,
    ACTIVE,

    /** The user turned work apps off. Clones keep all their data. */
    PAUSED,
}

data class AppEntry(
    val packageName: String,
    val label: String,
    /** Launcher activity; null when the clone is frozen (hidden apps have no launchable entry). */
    val component: ComponentName?,
    val user: UserHandle,
    val isSystem: Boolean,
    val frozen: Boolean = false,
) {
    val key: String get() = "${user.hashCode()}/$packageName"
}

/**
 * Personal-side view of the space: discovers the profile, lists apps on both sides,
 * launches clones and builds the requests that run inside the space.
 */
class SpaceManager(private val context: Context) {
    private val pm: PackageManager = context.packageManager
    private val launcherApps = context.getSystemService(LauncherApps::class.java)!!
    private val userManager = context.getSystemService(UserManager::class.java)!!
    private val crossProfileApps = context.getSystemService(CrossProfileApps::class.java)!!
    private val dpm = context.getSystemService(DevicePolicyManager::class.java)!!
    private val me: UserHandle = Process.myUserHandle()
    private val ownPackage = context.packageName

    val adminComponent = ComponentName(context, SpaceAdminReceiver::class.java)

    val isSupported: Boolean
        get() = pm.hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS)

    /** True when this copy of the app is the one running inside the space. */
    val isInsideSpace: Boolean
        get() = dpm.isProfileOwnerApp(ownPackage)

    /** The profile that holds our space: the profile in which this same app is installed. */
    fun spaceUser(): UserHandle? {
        crossProfileApps.targetUserProfiles.firstOrNull()?.let { return it }
        return userManager.userProfiles.firstOrNull { user ->
            user != me && runCatching { launcherApps.getApplicationInfo(ownPackage, 0, user) != null }
                .getOrDefault(false)
        }
    }

    fun status(): SpaceStatus {
        val space = spaceUser()
        return when {
            space != null -> if (userManager.isQuietModeEnabled(space)) SpaceStatus.PAUSED else SpaceStatus.ACTIVE
            !isSupported -> SpaceStatus.UNSUPPORTED
            userManager.userProfiles.any { it != me } -> SpaceStatus.FOREIGN
            else -> SpaceStatus.NONE
        }
    }

    fun canCreateSpace(): Boolean =
        isSupported && dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)

    fun clonedApps(space: UserHandle, frozen: Set<String>): List<AppEntry> {
        val visible = launcherApps.getActivityList(null, space)
            .asSequence()
            .filter { it.applicationInfo.packageName != ownPackage }
            .distinctBy { it.applicationInfo.packageName }
            .map { it.toEntry(space) }
            .toMutableList()
        val visiblePackages = visible.mapTo(HashSet()) { it.packageName }
        frozen.filterNot { it in visiblePackages }.forEach { pkg ->
            visible += AppEntry(
                packageName = pkg,
                label = personalLabel(pkg) ?: pkg,
                component = null,
                user = space,
                isSystem = false,
                frozen = true,
            )
        }
        return visible.sortedBy { it.label.lowercase() }
    }

    fun installableApps(alreadyCloned: Set<String>): List<AppEntry> =
        launcherApps.getActivityList(null, me)
            .asSequence()
            .filter { it.applicationInfo.packageName != ownPackage }
            .filter { it.applicationInfo.packageName !in alreadyCloned }
            .distinctBy { it.applicationInfo.packageName }
            .map { it.toEntry(me) }
            .sortedBy { it.label.lowercase() }
            .toList()

    private fun LauncherActivityInfo.toEntry(user: UserHandle) = AppEntry(
        packageName = applicationInfo.packageName,
        label = label?.toString()?.takeIf { it.isNotBlank() } ?: applicationInfo.packageName,
        component = componentName,
        user = user,
        isSystem = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
    )

    private fun personalLabel(pkg: String): String? = runCatching {
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrNull()

    /** Finds the clone's current launcher activity (it can change after an app update). */
    fun resolveLaunchable(packageName: String, user: UserHandle): ComponentName? =
        launcherApps.getActivityList(packageName, user).firstOrNull()?.componentName

    fun launch(packageName: String, user: UserHandle, bounds: Rect? = null): Boolean {
        val component = resolveLaunchable(packageName, user) ?: return false
        return runCatching { launcherApps.startMainActivity(component, user, bounds, null); true }
            .getOrDefault(false)
    }

    fun openAppInfo(entry: AppEntry): Boolean {
        val component = entry.component ?: resolveLaunchable(entry.packageName, entry.user)
            ?: return false
        return runCatching { launcherApps.startAppDetailsActivity(component, entry.user, null, null); true }
            .getOrDefault(false)
    }

    /**
     * Asks Android to turn the space on or off. Only some launchers may do this directly;
     * returns false when the user has to use the Work apps toggle instead.
     */
    fun setPaused(paused: Boolean): Boolean {
        val space = spaceUser() ?: return false
        return runCatching { userManager.requestQuietModeEnabled(paused, space) }
            .map { true }
            .getOrDefault(false)
    }

    fun provisioningIntent(token: String, spaceName: String): Intent =
        Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE).apply {
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION, true)
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_EDUCATION_SCREENS, true)
            putExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
                PersistableBundle().apply {
                    putString(SpaceContract.ADMIN_EXTRA_TOKEN, token)
                    putString(SpaceContract.ADMIN_EXTRA_NAME, spaceName)
                },
            )
        }

    /**
     * Builds a request for the space and points it at the system's cross-profile forwarder,
     * so it is delivered to our copy inside the space rather than to ourselves.
     * Returns null when no route exists (e.g. the space is paused or gone).
     */
    fun gatewayIntent(
        op: SpaceContract.Op,
        token: String,
        packages: List<String> = emptyList(),
        hidden: Boolean = false,
        name: String? = null,
    ): Intent? {
        val intent = Intent(SpaceContract.ACTION_SPACE_OP).apply {
            putExtra(SpaceContract.EXTRA_OP, op.name)
            putExtra(SpaceContract.EXTRA_TOKEN, token)
            putExtra(SpaceContract.EXTRA_PACKAGES, packages.toTypedArray())
            putExtra(SpaceContract.EXTRA_HIDDEN, hidden)
            if (name != null) putExtra(SpaceContract.EXTRA_NAME, name)
        }
        // Only trust the system's own forwarder: the request carries our token.
        val forwarder = pm.queryIntentActivities(intent, 0)
            .firstOrNull {
                it.activityInfo.packageName != ownPackage &&
                    it.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
            }
            ?: return null
        return intent.setComponent(
            ComponentName(forwarder.activityInfo.packageName, forwarder.activityInfo.name),
        )
    }

    fun registerCallback(callback: LauncherApps.Callback) = launcherApps.registerCallback(callback)
    fun unregisterCallback(callback: LauncherApps.Callback) = launcherApps.unregisterCallback(callback)
}
