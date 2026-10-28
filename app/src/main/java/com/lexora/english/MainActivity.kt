package com.lexora.english

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexora.english.data.AppRepository
import com.lexora.english.data.Notifier
import com.lexora.english.ui.LexoraRoot
import com.lexora.english.ui.theme.LexoraTheme
import com.lexora.english.util.Speaker

class MainActivity : ComponentActivity() {

    /** Set when the app is opened from a notification so the notification centre is shown. */
    private var openNotifications by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        Speaker.init(this)
        openNotifications = intent?.getBooleanExtra(Notifier.EXTRA_OPEN_NOTIFICATIONS, false) == true

        val repo = AppRepository.get(this)
        setContent {
            val state by repo.state.collectAsStateWithLifecycle()
            LexoraTheme(themeMode = state.settings.themeMode, fontScale = state.settings.fontScale) {
                LexoraRoot(
                    state = state,
                    openNotifications = openNotifications,
                    onNotificationsOpened = { openNotifications = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(Notifier.EXTRA_OPEN_NOTIFICATIONS, false)) openNotifications = true
    }

    override fun onResume() {
        super.onResume()
        val repo = AppRepository.get(this)
        if (repo.current.profile != null) repo.ensureDailyQuiz(postSystemNotification = false)
    }
}
