package com.pulse.messenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pulse.messenger.PulseApp
import com.pulse.messenger.data.PulseRepository
import com.pulse.messenger.data.Settings
import com.pulse.messenger.data.SettingsRepository
import com.pulse.messenger.ui.components.LocalSnackbar
import com.pulse.messenger.ui.components.PulseSnackbarHost
import com.pulse.messenger.ui.components.SnackbarController
import com.pulse.messenger.ui.navigation.PulseNavHost
import com.pulse.messenger.ui.settings.AppLockScreen

val LocalRepo = staticCompositionLocalOf<PulseRepository> { error("No repository") }
val LocalSettingsRepo = staticCompositionLocalOf<SettingsRepository> { error("No settings") }

@Composable
fun PulseRoot(settings: Settings) {
    val app = PulseApp.instance
    val hostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snackbar = remember(hostState, scope) { SnackbarController(hostState, scope) }
    var unlocked by rememberSaveable { mutableStateOf(false) }

    CompositionLocalProvider(
        LocalRepo provides app.repository,
        LocalSettingsRepo provides app.settings,
        LocalSnackbar provides snackbar,
    ) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            PulseNavHost(startOnboarding = !settings.onboarded)
            PulseSnackbarHost(
                hostState,
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 76.dp),
            )
            if (settings.onboarded && settings.appLock && settings.appLockPin.isNotEmpty() && !unlocked) {
                AppLockScreen(pin = settings.appLockPin, onUnlock = { unlocked = true })
            }
        }
    }
}
