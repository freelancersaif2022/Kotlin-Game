package com.vaultly.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.vaultly.app.ads.Consent
import com.vaultly.app.ui.nav.VaultlyNav
import com.vaultly.app.ui.theme.VaultlyTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    /** Item to open when the app is launched from a reminder notification. */
    private val pendingItem = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as VaultlyApp
        Consent.gather(this) { app.ads.init() }
        readIntent(intent)
        setContent {
            VaultlyTheme {
                VaultlyNav(app, pendingItem)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(intent: Intent?) {
        val id = intent?.getLongExtra(EXTRA_ITEM_ID, -1L) ?: -1L
        if (id > 0) pendingItem.value = id
        intent?.removeExtra(EXTRA_ITEM_ID)
    }

    companion object {
        const val EXTRA_ITEM_ID = "item_id"
    }
}
