package com.twinspace.app.admin

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.twinspace.app.core.SpaceContract
import com.twinspace.app.core.SpaceContract.Op

/**
 * Runs inside the space. Receives requests from the personal-side app (via the system
 * cross-profile forwarder), performs them with profile-owner rights and returns a result.
 *
 * Nothing here runs on its own: every action starts from something the user tapped.
 */
class SpaceGatewayActivity : Activity() {
    private val dpm by lazy { getSystemService(DevicePolicyManager::class.java)!! }
    private val admin by lazy { SpaceSetup.admin(this) }
    private var wasHidden = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            // Recreated while the uninstall dialog is showing; its result is still on the way.
            wasHidden = savedInstanceState.getBoolean(STATE_WAS_HIDDEN)
            return
        }

        if (!dpm.isProfileOwnerApp(packageName)) return reply(false, "Twin Space is not set up on this side.")
        val op = runCatching { Op.valueOf(intent.getStringExtra(SpaceContract.EXTRA_OP).orEmpty()) }.getOrNull()
        if (!isAuthorized(op)) return reply(false, "Request refused.")

        // Repair setup if something interrupted provisioning earlier.
        SpaceSetup.finish(this, null)

        val packages = intent.getStringArrayExtra(SpaceContract.EXTRA_PACKAGES).orEmpty().toList()
        when (op) {
            Op.PING -> reply(true)
            Op.CLONE -> clone(packages)
            Op.FREEZE -> freeze(packages, intent.getBooleanExtra(SpaceContract.EXTRA_HIDDEN, true))
            Op.REMOVE -> remove(packages.firstOrNull())
            Op.RENAME_SPACE -> renameSpace(intent.getStringExtra(SpaceContract.EXTRA_NAME))
            Op.DELETE_SPACE -> deleteSpace()
            null -> reply(false, "Unknown request.")
        }
    }

    /**
     * The forwarder starts us "as the caller" and forwards our result, so the calling package is
     * the personal-side Twin Space. The token shared at creation time covers devices that report
     * otherwise, but never for removing the whole space.
     */
    private fun isAuthorized(op: Op?): Boolean {
        if (callingPackage == packageName) return true
        if (op == Op.DELETE_SPACE) return false
        val expected = SpaceSetup.token(this)
        val given = intent.getStringExtra(SpaceContract.EXTRA_TOKEN)
        return !expected.isNullOrEmpty() && expected == given
    }

    private fun clone(packages: List<String>) {
        val done = ArrayList<String>()
        val failed = ArrayList<String>()
        for (pkg in packages) {
            val ok = runCatching { dpm.installExistingPackage(admin, pkg) }.getOrDefault(false) ||
                runCatching { dpm.enableSystemApp(admin, pkg); isInstalled(pkg) }.getOrDefault(false)
            if (ok) {
                // A clone made earlier and then frozen comes back visible.
                runCatching { dpm.setApplicationHidden(admin, pkg, false) }
                done += pkg
            } else {
                failed += pkg
            }
        }
        setResult(
            RESULT_OK,
            Intent()
                .putStringArrayListExtra(SpaceContract.EXTRA_DONE, done)
                .putStringArrayListExtra(SpaceContract.EXTRA_FAILED, failed),
        )
        finish()
    }

    private fun isInstalled(pkg: String) =
        runCatching { packageManager.getApplicationInfo(pkg, 0); true }.getOrDefault(false)

    private fun freeze(packages: List<String>, hidden: Boolean) {
        val done = ArrayList<String>()
        for (pkg in packages) {
            if (runCatching { dpm.setApplicationHidden(admin, pkg, hidden) }.getOrDefault(false) ||
                runCatching { dpm.isApplicationHidden(admin, pkg) == hidden }.getOrDefault(false)
            ) {
                done += pkg
            }
        }
        setResult(RESULT_OK, Intent().putStringArrayListExtra(SpaceContract.EXTRA_DONE, done))
        finish()
    }

    /** Opens Android's own uninstall confirmation for the clone only. The personal app is untouched. */
    @Suppress("DEPRECATION")
    private fun remove(pkg: String?) {
        if (pkg == null) return reply(false, "Nothing to remove.")
        // A frozen app must be visible for the system uninstaller to find it.
        wasHidden = runCatching { dpm.isApplicationHidden(admin, pkg) }.getOrDefault(false)
        if (wasHidden) runCatching { dpm.setApplicationHidden(admin, pkg, false) }
        val uninstall = Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.fromParts("package", pkg, null))
            .putExtra(Intent.EXTRA_RETURN_RESULT, true)
        runCatching { startActivityForResult(uninstall, REQUEST_UNINSTALL) }
            .onFailure { reply(false, "Android couldn't open the uninstaller.") }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_UNINSTALL) return
        val pkg = intent.getStringArrayExtra(SpaceContract.EXTRA_PACKAGES)?.firstOrNull()
        if (pkg != null && !isInstalled(pkg)) {
            setResult(RESULT_OK, Intent().putStringArrayListExtra(SpaceContract.EXTRA_DONE, arrayListOf(pkg)))
            finish()
        } else {
            // The user cancelled: leave the clone exactly as it was.
            if (wasHidden && pkg != null) runCatching { dpm.setApplicationHidden(admin, pkg, true) }
            reply(false, null)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_WAS_HIDDEN, wasHidden)
    }

    private fun renameSpace(name: String?) {
        if (name.isNullOrBlank()) return reply(false, "Name can't be empty.")
        reply(runCatching { dpm.setProfileName(admin, name.trim()) }.isSuccess)
    }

    /** Removes the whole space and every clone in it. Only reached after the user confirmed twice. */
    private fun deleteSpace() {
        setResult(RESULT_OK)
        finish()
        Handler(Looper.getMainLooper()).postDelayed({ runCatching { dpm.wipeData(0) } }, 300)
    }

    private fun reply(ok: Boolean, error: String? = null) {
        setResult(if (ok) RESULT_OK else RESULT_CANCELED, Intent().putExtra(SpaceContract.EXTRA_ERROR, error))
        finish()
    }

    private companion object {
        const val REQUEST_UNINSTALL = 7
        const val STATE_WAS_HIDDEN = "was_hidden"
    }
}
