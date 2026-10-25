package com.twinspace.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.UserManager
import android.widget.Toast
import com.twinspace.app.core.ShortcutHelper

/** Invisible trampoline behind home-screen shortcuts: opens the clone and gets out of the way. */
class ShortcutLauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TwinSpaceApplication
        val pkg = intent.getStringExtra(ShortcutHelper.EXTRA_PACKAGE)
        val serial = intent.getLongExtra(ShortcutHelper.EXTRA_USER_SERIAL, -1L)
        val user = getSystemService(UserManager::class.java)!!.getUserForSerialNumber(serial)
            ?: app.space.spaceUser()

        when {
            pkg == null || user == null -> toast("The space isn't available right now.")
            pkg in app.prefs.settings.value.frozen -> startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(MainActivity.EXTRA_UNFREEZE, pkg),
            )
            !app.space.launch(pkg, user, intent.sourceBounds) ->
                toast("This clone isn't in your space any more. Open Twin Space to clone it again.")
        }
        finish()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
