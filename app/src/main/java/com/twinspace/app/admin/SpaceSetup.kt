package com.twinspace.app.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.PersistableBundle
import androidx.core.content.edit
import com.twinspace.app.R
import com.twinspace.app.core.SpaceContract

/**
 * Runs inside the newly created profile and turns it into a ready-to-use space.
 * Safe to call more than once: Android may deliver both the broadcast and the activity callbacks.
 * Called with a null intent to repair an interrupted setup without touching user choices.
 */
object SpaceSetup {
    private const val PREFS = "space_side"
    private const val KEY_TOKEN = "token"

    fun admin(context: Context) = ComponentName(context, SpaceAdminReceiver::class.java)

    fun finish(context: Context, intent: Intent?) {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)!!
        if (!dpm.isProfileOwnerApp(context.packageName)) return
        val admin = admin(context)

        val extras = intent?.adminExtras()
        extras?.getString(SpaceContract.ADMIN_EXTRA_TOKEN)?.takeIf { it.isNotBlank() }?.let { saveToken(context, it) }
        val name = extras?.getString(SpaceContract.ADMIN_EXTRA_NAME)?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.space_default_name)

        // Only name the space during provisioning; later repairs must keep the user's chosen name.
        if (intent != null) runCatching { dpm.setProfileName(admin, name) }

        // Let the personal side reach our gateway in here.
        runCatching {
            dpm.addCrossProfileIntentFilter(
                admin,
                IntentFilter(SpaceContract.ACTION_SPACE_OP).apply { addCategory(Intent.CATEGORY_DEFAULT) },
                DevicePolicyManager.FLAG_PARENT_CAN_ACCESS_MANAGED,
            )
        }

        // The copy of Twin Space inside the space works silently; hide its launcher icon.
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context.packageName, "${context.packageName}.Launcher"),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )

        runCatching { dpm.setProfileEnabled(admin) }
    }

    @Suppress("DEPRECATION")
    private fun Intent.adminExtras(): PersistableBundle? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE, PersistableBundle::class.java)
        } else {
            getParcelableExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE)
        }

    private fun saveToken(context: Context, token: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit(commit = true) { putString(KEY_TOKEN, token) }

    fun token(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)
}
