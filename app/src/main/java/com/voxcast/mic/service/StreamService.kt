package com.voxcast.mic.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.voxcast.mic.MainActivity
import com.voxcast.mic.R
import com.voxcast.mic.VoxApp
import com.voxcast.mic.audio.MicEngine
import com.voxcast.mic.audio.SpeakerEngine
import com.voxcast.mic.net.HostMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Keeps a session alive while the screen is off or the app is in the background,
 * and holds Wi-Fi in low-latency mode so audio never stutters from power saving.
 */
class StreamService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var speaker: SpeakerEngine? = null
    private var mic: MicEngine? = null
    private var engineJob: Job? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var role: Role? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        scope.launch {
            StreamController.state.collect { state ->
                when (state) {
                    is StreamState.Failed, StreamState.Idle -> if (role != null) withContext(Dispatchers.Main) { shutdown() }
                    else -> role?.let { notify(it, state) }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SPEAKER -> {
                releaseEngines()
                val mode = runCatching { HostMode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: "") }.getOrDefault(HostMode.LAN)
                if (!goForeground(Role.SPEAKER)) return START_NOT_STICKY
                acquireLocks()
                val app = application as VoxApp
                val engine = SpeakerEngine(this, scope, app.settings.settings.value.deviceName)
                speaker = engine
                StreamController.speakerEngine = engine
                StreamController.speakerVolume = app.settings.settings.value.speakerBoost
                engineJob = scope.launch { engine.start(mode) }
            }
            ACTION_START_MIC -> {
                releaseEngines()
                val payload = intent.getStringExtra(EXTRA_PAYLOAD).orEmpty()
                if (!goForeground(Role.MIC)) return START_NOT_STICKY
                acquireLocks()
                val app = application as VoxApp
                val engine = MicEngine(this, scope, app.settings.settings)
                mic = engine
                engineJob = scope.launch { engine.start(payload) }
            }
            ACTION_STOP -> {
                val endedRole = role
                StreamController.update(StreamState.Idle)
                shutdown()
                endedRole?.let { StreamController.emit(StreamEvent.SessionEnded(it)) }
            }
            else -> if (role == null) stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun goForeground(newRole: Role): Boolean {
        role = newRole
        val type = when (newRole) {
            Role.MIC -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
            Role.SPEAKER -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        }
        return try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(newRole, StreamController.state.value), type)
            true
        } catch (e: Exception) {
            role = null
            StreamController.update(
                StreamState.Failed(newRole, "Couldn't start", "Keep VoxCast open on screen and grant the permissions, then try again.")
            )
            stopSelf()
            false
        }
    }

    /** Stops engines off the main thread; closing sockets and joining audio threads can block briefly. */
    private fun releaseEngines() {
        engineJob?.cancel()
        engineJob = null
        val oldSpeaker = speaker
        val oldMic = mic
        speaker = null
        mic = null
        StreamController.speakerEngine = null
        if (oldSpeaker != null || oldMic != null) {
            Thread({ oldSpeaker?.stop(); oldMic?.stop() }, "vox-release").start()
        }
    }

    private fun shutdown() {
        role = null
        releaseEngines()
        releaseLocks()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        releaseEngines()
        releaseLocks()
        if (StreamController.state.value !is StreamState.Failed) StreamController.update(StreamState.Idle)
        scope.cancel()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun acquireLocks() {
        if (wifiLock == null) {
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            else WifiManager.WIFI_MODE_FULL_HIGH_PERF
            wifiLock = wm.createWifiLock(mode, "VoxCast:wifi").apply { setReferenceCounted(false); acquire() }
        }
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VoxCast:stream").apply {
                setReferenceCounted(false)
                acquire(6 * 60 * 60 * 1000L)
            }
        }
    }

    private fun releaseLocks() {
        runCatching { wifiLock?.release() }
        runCatching { wakeLock?.release() }
        wifiLock = null
        wakeLock = null
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, "Live session", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shows while VoxCast is streaming audio"
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    private fun notify(role: Role, state: StreamState) {
        val nm = getSystemService(NotificationManager::class.java)
        runCatching { nm.notify(NOTIFICATION_ID, buildNotification(role, state)) }
    }

    private fun buildNotification(role: Role, state: StreamState): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, StreamService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val (title, text) = when (state) {
            is StreamState.SpeakerWaiting -> "Speaker ready" to "Scan the QR code with your microphone phone"
            is StreamState.SpeakerLive -> "Speaker live" to "Playing ${state.micName}"
            is StreamState.MicLive -> "Microphone live" to "Streaming to ${state.speakerName}"
            is StreamState.MicConnecting -> "Connecting" to state.stage
            is StreamState.MicReconnecting -> "Reconnecting" to "Trying to reach ${state.speakerName}"
            is StreamState.Preparing -> "Starting" to state.message
            else -> "VoxCast" to "Session active"
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(if (role == Role.MIC) R.drawable.ic_stat_mic else R.drawable.ic_stat_speaker)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(open)
            .addAction(0, "End session", stop)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        const val ACTION_START_SPEAKER = "com.voxcast.mic.START_SPEAKER"
        const val ACTION_START_MIC = "com.voxcast.mic.START_MIC"
        const val ACTION_STOP = "com.voxcast.mic.STOP"
        const val EXTRA_MODE = "mode"
        const val EXTRA_PAYLOAD = "payload"
        private const val CHANNEL_ID = "vox_live"
        private const val NOTIFICATION_ID = 42
    }
}
