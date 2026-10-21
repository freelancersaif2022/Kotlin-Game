package com.novacleaner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novacleaner.app.core.AppGraph
import com.novacleaner.app.core.Settings
import com.novacleaner.app.ui.navigation.NovaNavHost
import com.novacleaner.app.ui.theme.NovaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by AppGraph.prefs.settings.collectAsStateWithLifecycle(initialValue = Settings())
            NovaTheme(themeMode = settings.themeMode) {
                NovaNavHost()
            }
        }
    }
}
