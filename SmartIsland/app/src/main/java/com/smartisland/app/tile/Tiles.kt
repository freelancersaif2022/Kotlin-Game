package com.smartisland.app.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.smartisland.app.MainActivity
import com.smartisland.app.container
import com.smartisland.app.system.ActionResult
import com.smartisland.app.system.SystemActions

/** Quick Settings tile that locks the screen: a one-tap power-button replacement. */
class LockTileService : TileService() {

    override fun onStartListening() {
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        if (SystemActions.lockScreen() == ActionResult.NEEDS_ACCESSIBILITY) openApp(this, "permissions")
    }
}

/** Quick Settings tile that toggles the island. */
class IslandTileService : TileService() {

    override fun onStartListening() = refresh()

    override fun onClick() {
        container.settings.update { it.copy(enabled = !it.enabled) }
        refresh()
    }

    private fun refresh() {
        qsTile?.apply {
            state = if (container.settings.current.enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }
}

@SuppressLint("StartActivityAndCollapseDeprecated")
private fun openApp(service: TileService, route: String) {
    val intent = Intent(service, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra(MainActivity.EXTRA_ROUTE, route)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val pi = PendingIntent.getActivity(service, 7, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        service.startActivityAndCollapse(pi)
    } else {
        @Suppress("DEPRECATION")
        service.startActivityAndCollapse(intent)
    }
}
