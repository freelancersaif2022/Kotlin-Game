package com.flashshare.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.flashshare.app.ui.AppNav
import com.flashshare.app.ui.theme.FlashShareTheme
import com.flashshare.app.data.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val c = container
        splash.setKeepOnScreenCondition { c.prefs.state.value == null }
        c.ads.init(this)
        handleShareIntent(intent)
        setContent {
            val settings by c.prefs.state.collectAsStateWithLifecycle()
            FlashShareTheme(settings?.theme ?: ThemeMode.LIGHT) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    settings?.let { AppNav(it) }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    /** "Share → FlashShare" from the gallery or any other app. */
    private fun handleShareIntent(intent: Intent?) {
        intent ?: return
        val uris: List<Uri> = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            Intent.ACTION_SEND_MULTIPLE -> IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            else -> return
        }
        if (uris.isEmpty()) return
        lifecycleScope.launch {
            val items = container.media.fromDocumentUris(uris)
            if (items.isNotEmpty()) {
                container.selection.set(items)
                pendingShare.value = true
            }
        }
    }

    companion object {
        /** Set when files arrived through the system share sheet – Home jumps straight to scanning. */
        val pendingShare = MutableStateFlow(false)
        val isAtLeastT = Build.VERSION.SDK_INT >= 33
    }
}
