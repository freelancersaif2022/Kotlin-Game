package com.eyescroll.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.eyescroll.app.data.SettingsStore
import com.eyescroll.app.tracking.ScrollDirection
import com.eyescroll.app.tracking.TrackingBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Performs the scroll gestures in whatever app is on screen and hosts the floating on/off dot.
 * It does not read window content (canRetrieveWindowContent = false).
 */
class EyeScrollAccessibilityService : AccessibilityService() {

    private var scope: CoroutineScope? = null
    private var bubble: FloatingBubble? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        val store = SettingsStore.get(this)
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = s

        s.launch {
            TrackingBus.scrolls.collect { dir ->
                scroll(dir, store.current.scrollDistance.fraction)
            }
        }
        s.launch {
            combine(store.settings, TrackingBus.status) { settings, status -> settings to status }
                .collect { (settings, status) ->
                    if (settings.showBubble && settings.onboardingDone) {
                        val b = bubble ?: FloatingBubble(this@EyeScrollAccessibilityService, store).also {
                            it.onTap = { toggleTracking() }
                            it.onLongPress = { openApp() }
                            it.show()
                            bubble = it
                        }
                        b.render(status)
                    } else {
                        bubble?.hide()
                        bubble = null
                    }
                }
        }
    }

    private fun toggleTracking() {
        startActivity(
            Intent(this, TrackingLauncherActivity::class.java)
                .setAction(TrackingLauncherActivity.ACTION_TOGGLE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
        )
    }

    private fun openApp() {
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /**
     * Simulates a smooth finger swipe in the middle band of the screen (away from the status bar
     * and navigation gesture areas). Scrolling DOWN means the finger moves UP.
     */
    fun scroll(direction: ScrollDirection, fraction: Float): Boolean {
        val metrics = resources.displayMetrics
        val w = metrics.widthPixels.toFloat()
        val h = metrics.heightPixels.toFloat()
        val x = w * 0.5f
        val half = h * fraction.coerceIn(0.1f, 0.6f) / 2f
        val center = h * 0.52f
        val (fromY, toY) = when (direction) {
            ScrollDirection.DOWN -> (center + half) to (center - half)
            ScrollDirection.UP -> (center - half) to (center + half)
        }
        val path = Path().apply {
            moveTo(x, fromY)
            lineTo(x, toY)
        }
        val duration = (220 + 380 * fraction).toLong()
        // The swipe is followed by a short hold so the list stops where the finger stops
        // (no fling) – that keeps scrolling predictable while reading.
        val swipe = GestureDescription.StrokeDescription(path, 0, duration, true)
        val hold = Path().apply {
            moveTo(x, toY)
            lineTo(x, toY + 1f)
        }
        return dispatchGesture(
            GestureDescription.Builder().addStroke(swipe).build(),
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    val end = swipe.continueStroke(hold, 0, 90, false)
                    dispatchGesture(GestureDescription.Builder().addStroke(end).build(), null, null)
                }
            },
            null,
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        isRunning = false
        bubble?.hide()
        bubble = null
        scope?.cancel()
        scope = null
        // Scrolling is impossible without this service, so release the camera too.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && TrackingBus.status.value.isActive) {
            EyeTrackingService.stop(this)
        }
    }

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
