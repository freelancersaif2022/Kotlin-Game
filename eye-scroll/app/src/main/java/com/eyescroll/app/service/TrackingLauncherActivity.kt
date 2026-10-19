package com.eyescroll.app.service

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.eyescroll.app.Readiness
import com.eyescroll.app.tracking.TrackingBus
import com.eyescroll.app.ui.MainActivity

/**
 * Invisible, instant activity used by the floating dot and the quick-settings tile.
 * Android only lets a camera foreground service start while the app is in the foreground, so the
 * dot/tile open this for a split second, start the service, and close again.
 */
class TrackingLauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val action = intent?.action ?: ACTION_TOGGLE
        val active = TrackingBus.status.value.isActive
        when {
            action == ACTION_STOP || (action == ACTION_TOGGLE && active) -> EyeTrackingService.stop(this)
            !Readiness.canStartTracking(this) -> startActivity(
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            else -> EyeTrackingService.start(this)
        }
        finish()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.eyescroll.app.TOGGLE"
        const val ACTION_START = "com.eyescroll.app.START"
        const val ACTION_STOP = "com.eyescroll.app.STOP_TRACKING"
    }
}
