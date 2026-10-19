package com.eyescroll.app.service

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Display
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.eyescroll.app.EyeScrollApp
import com.eyescroll.app.R
import com.eyescroll.app.Readiness
import com.eyescroll.app.data.AppSettings
import com.eyescroll.app.data.SettingsStore
import com.eyescroll.app.tracking.FaceFrame
import com.eyescroll.app.tracking.FaceTracker
import com.eyescroll.app.tracking.GazeController
import com.eyescroll.app.tracking.TrackingBus
import com.eyescroll.app.tracking.TrackingPhase
import com.eyescroll.app.tracking.TrackingStatus
import com.eyescroll.app.ui.MainActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Camera-on-demand foreground service. The camera runs only while this service runs; stopping it
 * (bubble, tile, notification, app, auto-off, screen off) releases the camera immediately.
 */
class EyeTrackingService : LifecycleService(), FaceTracker.Listener {

    private lateinit var store: SettingsStore
    private val main = Handler(Looper.getMainLooper())
    private var tracker: FaceTracker? = null
    @Volatile private var controller: GazeController? = null
    private val jobs = mutableListOf<Job>()

    @Volatile private var lastActivityMs = 0L
    @Volatile private var lastFaceMs = 0L
    private var fpsWindowStart = 0L
    private var fpsFrames = 0
    @Volatile private var fps = 0

    private var screenReceiver: BroadcastReceiver? = null
    private var displayListener: DisplayManager.DisplayListener? = null

    override fun onCreate() {
        super.onCreate()
        store = SettingsStore.get(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopTracking(null)
            return START_NOT_STICKY
        }
        if (tracker != null) return START_NOT_STICKY

        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA else 0,
            )
        } catch (e: Exception) {
            fail("Android blocked the camera from starting. Open EyeScroll and tap Start.")
            return START_NOT_STICKY
        }

        if (!Readiness.hasCamera(this)) {
            fail("Camera permission is needed. Open EyeScroll to allow it.")
            return START_NOT_STICKY
        }
        val settings = store.current
        val model = store.calibration.value?.model(settings.mode)
        if (model == null || !model.isUsable) {
            fail("Please calibrate first (10 seconds).")
            return START_NOT_STICKY
        }

        controller = GazeController(model, settings.toConfig())
        val now = SystemClock.uptimeMillis()
        lastActivityMs = now
        lastFaceMs = now
        fpsWindowStart = now
        TrackingBus.set(TrackingStatus(phase = TrackingPhase.STARTING, autoOffIn = settings.autoOffSeconds.takeIf { it > 0 } ?: -1))

        tracker = FaceTracker(this, this, settings.targetFps, this).also {
            it.start(rotation = currentRotation())
        }
        registerScreenOff()
        registerRotation()

        jobs += lifecycleScope.launch {
            store.settings.collect { s ->
                controller?.config = s.toConfig()
                tracker?.targetFps = s.targetFps
            }
        }
        jobs += lifecycleScope.launch {
            while (isActive) {
                delay(1000)
                tick()
            }
        }
        return START_NOT_STICKY
    }

    private fun AppSettings.toConfig() = GazeController.Config(sensitivity, dwellMs, repeatMs)

    // ---- FaceTracker.Listener (background thread) ----

    override fun onFrame(frame: FaceFrame) {
        val c = controller ?: return
        val now = SystemClock.uptimeMillis()
        val (snapshot, fired) = c.process(frame, now)

        fpsFrames++
        if (now - fpsWindowStart >= 1000) {
            fps = (fpsFrames * 1000 / (now - fpsWindowStart)).toInt()
            fpsFrames = 0
            fpsWindowStart = now
        }
        if (frame.faceFound) lastFaceMs = now
        if (fired != null) {
            lastActivityMs = now
            TrackingBus.emitScroll(fired)
            if (store.current.vibrate) vibrate()
        }
        val hint = hintFor(frame)
        TrackingBus.update {
            it.copy(
                phase = if (frame.faceFound) TrackingPhase.TRACKING else TrackingPhase.NO_FACE,
                gaze = snapshot,
                fps = fps,
                message = hint,
                lastScroll = fired ?: it.lastScroll,
                lastScrollAt = if (fired != null) System.currentTimeMillis() else it.lastScrollAt,
                scrollCount = it.scrollCount + if (fired != null) 1 else 0,
            )
        }
    }

    override fun onError(message: String) {
        main.post { stopTracking(message, error = true) }
    }

    private fun hintFor(frame: FaceFrame): String? = when {
        !frame.faceFound -> "Face not visible – hold the phone in front of your face"
        frame.brightness in 0f..0.16f -> "It's quite dark – more light improves accuracy"
        frame.eyeWidthPx in 1f..16f -> "Bring the phone a little closer"
        !EyeScrollAccessibilityService.isRunning -> "Turn on EyeScroll in Accessibility settings to scroll"
        else -> null
    }

    // ---- housekeeping ----

    private fun tick() {
        if (tracker == null) return
        val s = store.current
        val now = SystemClock.uptimeMillis()
        if (now - lastFaceMs > NO_FACE_TIMEOUT_MS) {
            stopTracking("Turned off – no face seen for ${NO_FACE_TIMEOUT_MS / 1000} s")
            return
        }
        if (s.autoOffSeconds > 0) {
            val left = s.autoOffSeconds - ((now - lastActivityMs) / 1000).toInt()
            if (left <= 0) {
                stopTracking("Turned off automatically after ${s.autoOffSeconds} s without scrolling")
                return
            }
            TrackingBus.update { it.copy(autoOffIn = left) }
        } else {
            TrackingBus.update { it.copy(autoOffIn = -1) }
        }
    }

    private fun stopTracking(message: String?, error: Boolean = false) {
        jobs.forEach { it.cancel() }
        jobs.clear()
        tracker?.close()
        tracker = null
        controller = null
        unregisterScreenOff()
        unregisterRotation()
        TrackingBus.set(TrackingStatus(phase = if (error) TrackingPhase.ERROR else TrackingPhase.OFF, message = message))
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun fail(message: String) {
        TrackingBus.set(TrackingStatus(phase = TrackingPhase.ERROR, message = message))
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (tracker != null) {
            tracker?.close()
            tracker = null
            unregisterScreenOff()
            unregisterRotation()
            TrackingBus.set(TrackingStatus(phase = TrackingPhase.OFF))
        }
        super.onDestroy()
    }

    private fun registerScreenOff() {
        val r = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_OFF) stopTracking("Turned off – screen went off")
            }
        }
        ContextCompat.registerReceiver(this, r, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        screenReceiver = r
    }

    private fun unregisterScreenOff() {
        screenReceiver?.let { runCatching { unregisterReceiver(it) } }
        screenReceiver = null
    }

    private fun currentRotation(): Int =
        getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: 0

    private fun registerRotation() {
        val dm = getSystemService(DisplayManager::class.java)
        val l = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
            override fun onDisplayChanged(displayId: Int) {
                if (displayId == Display.DEFAULT_DISPLAY) tracker?.setRotation(currentRotation())
            }
        }
        dm.registerDisplayListener(l, main)
        displayListener = l
    }

    private fun unregisterRotation() {
        displayListener?.let { getSystemService(DisplayManager::class.java).unregisterDisplayListener(it) }
        displayListener = null
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
        vibrator?.vibrate(VibrationEffect.createOneShot(14, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, EyeTrackingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, EyeScrollApp.CHANNEL_TRACKING)
            .setSmallIcon(R.drawable.ic_stat_eye)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setColor(0xFF3DDCFF.toInt())
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(open)
            .addAction(0, getString(R.string.notification_stop), stop)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.eyescroll.app.STOP"
        private const val NOTIFICATION_ID = 42
        private const val NO_FACE_TIMEOUT_MS = 20_000L

        /** Call only while the app is in the foreground (activity visible) – see [TrackingLauncherActivity]. */
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, EyeTrackingService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, EyeTrackingService::class.java))
        }
    }
}
