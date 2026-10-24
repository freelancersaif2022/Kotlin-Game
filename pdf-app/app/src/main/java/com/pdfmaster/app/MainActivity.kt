package com.pdfmaster.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pdfmaster.app.ads.AdsManager
import com.pdfmaster.app.billing.BillingManager
import com.pdfmaster.app.data.prefs.AppPreferences
import com.pdfmaster.app.data.prefs.ThemeMode
import com.pdfmaster.app.data.repo.FileRepository
import com.pdfmaster.app.pdf.ThumbnailLoader
import com.pdfmaster.app.premium.PremiumManager
import com.pdfmaster.app.ui.AppDeps
import com.pdfmaster.app.ui.AppRoot
import com.pdfmaster.app.ui.LocalAppDeps
import com.pdfmaster.app.ui.theme.PdfMasterTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var ads: AdsManager
    @Inject lateinit var premium: PremiumManager
    @Inject lateinit var billing: BillingManager
    @Inject lateinit var prefs: AppPreferences
    @Inject lateinit var files: FileRepository
    @Inject lateinit var thumbnails: ThumbnailLoader

    /** A PDF handed to us by another app, waiting until the UI is ready to open it. */
    private val incoming = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        ads.gatherConsent(this)
        billing.connect()

        setContent {
            val theme by prefs.themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val deps = remember { AppDeps(ads, premium, billing, prefs, files, thumbnails) }
            PdfMasterTheme(theme) {
                CompositionLocalProvider(LocalAppDeps provides deps) {
                    AppRoot(incoming = incoming, onIncomingHandled = { incoming.value = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val uri = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            else -> null
        }
        if (uri != null) incoming.value = uri
    }
}
