package com.eyescroll.app.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.eyescroll.app.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Quick-settings tile: show or hide the floating scroll pad. */
class ScrollTileService : TileService() {

    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = s
        s.launch { SettingsStore.get(this@ScrollTileService).settings.collect { refresh(it.padEnabled) } }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        SettingsStore.get(this).update { it.copy(padEnabled = !it.padEnabled, onboardingDone = true) }
    }

    private fun refresh(enabled: Boolean) {
        val tile = qsTile ?: return
        val serviceOn = ScrollAccessibilityService.isRunning
        tile.state = if (enabled && serviceOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                !serviceOn -> "Setup needed"
                enabled -> "Pad on"
                else -> "Pad off"
            }
        }
        tile.updateTile()
    }
}
