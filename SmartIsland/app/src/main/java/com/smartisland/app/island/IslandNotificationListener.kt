package com.smartisland.app.island

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.smartisland.app.system.Bitmaps
import com.smartisland.app.system.systemService

/**
 * Feeds calls, media sessions and (premium) notification alerts into [IslandEngine].
 *
 * Calls are read from the dialer's call-style notification, so answering/declining reuses the dialer's
 * own actions and the app needs no phone permissions at all.
 */
class IslandNotificationListener : NotificationListenerService() {

    private var media: MediaTracker? = null
    private var callKey: String? = null
    private var lastCallSbn: StatusBarNotification? = null
    /** Wall-clock time a call connected, kept stable across notification re-posts. */
    private var callConnectedAt = 0L
    private val lastNoticeAt = HashMap<String, Long>()
    private val main = Handler(Looper.getMainLooper())
    private val audio by lazy { systemService<AudioManager>() }
    private var callAvatar: android.graphics.Bitmap? = null

    /**
     * Many dialers answer a call without re-posting the notification in a recognizable way, so while a
     * call is shown we watch the audio mode: RINGTONE means ringing, IN_CALL / IN_COMMUNICATION means
     * the call connected and the timer must run.
     */
    private val callWatcher = object : Runnable {
        override fun run() {
            val sbn = lastCallSbn ?: return
            handleCall(sbn)
            main.postDelayed(this, 1000)
        }
    }

    /** When the phone's audio entered a call; lets the timer survive notification changes. */
    private var audioCallStart = 0L

    /**
     * Calls are also detected from the audio mode, so outgoing SIM calls and VoIP calls (Messenger,
     * WhatsApp...) show the call icon and timer even when the calling app's notification isn't
     * recognizable. Android 12+ reports mode changes; older versions are polled cheaply.
     */
    private val modePoller = object : Runnable {
        override fun run() {
            onAudioMode(runCatching { audio.mode }.getOrDefault(AudioManager.MODE_NORMAL))
            main.postDelayed(this, 1200)
        }
    }
    private var modeListener: Any? = null

    override fun onListenerConnected() {
        media = MediaTracker(applicationContext, ComponentName(this, IslandNotificationListener::class.java)).also { it.start() }
        runCatching { activeNotifications }.getOrNull()?.forEach { handleCall(it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val l = AudioManager.OnModeChangedListener { mode -> main.post { onAudioMode(mode) } }
            runCatching { audio.addOnModeChangedListener(mainExecutor, l) }.onSuccess { modeListener = l }
            onAudioMode(audio.mode)
        }
        if (modeListener == null) main.post(modePoller)
        com.smartisland.app.system.Permissions.returnToAppIfExpected(applicationContext)
    }

    private fun onAudioMode(mode: Int) {
        val inCall = mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION
        if (inCall) {
            if (audioCallStart == 0L) audioCallStart = System.currentTimeMillis()
            if (callKey != null) return
            // Prefer the calling app's own notification (caller name, hang-up button).
            val sbn = runCatching { activeNotifications }.getOrNull()?.firstOrNull {
                it.packageName != packageName && (isCallNotification(it) || isVoipOngoing(it))
            }
            if (sbn != null && handleCall(sbn, force = true)) return
            callKey = AUDIO_CALL_KEY
            IslandEngine.setCall(
                IslandActivity.Call(
                    key = AUDIO_CALL_KEY, caller = "Ongoing call", isIncoming = false, startedAt = audioCallStart,
                    avatar = null, answer = null, decline = null, hangUp = null, open = null,
                ),
            )
        } else {
            audioCallStart = 0L
            if (callKey == AUDIO_CALL_KEY) endCall()
        }
    }

    private fun isVoipOngoing(sbn: StatusBarNotification): Boolean =
        sbn.packageName in VOIP_APPS && (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0

    override fun onListenerDisconnected() {
        media?.stop()
        media = null
        main.removeCallbacks(modePoller)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (modeListener as? AudioManager.OnModeChangedListener)?.let { runCatching { audio.removeOnModeChangedListener(it) } }
        }
        modeListener = null
        endCall()
    }

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun endCall() {
        callKey?.let { IslandEngine.clearCall(it) }
        callKey = null
        lastCallSbn = null
        callAvatar = null
        callConnectedAt = 0L
        main.removeCallbacks(callWatcher)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        if (handleCall(sbn)) return
        if (isMediaNotification(sbn.notification)) {
            media?.rescan()
            return
        }
        handleNotice(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.key == callKey) {
            endCall()
            // Some dialers drop their notification mid-call; fall back to audio detection.
            main.postDelayed({ onAudioMode(runCatching { audio.mode }.getOrDefault(AudioManager.MODE_NORMAL)) }, 600)
        }
    }

    // Calls -----------------------------------------------------------------------------------

    private fun isCallNotification(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        if (n.category == Notification.CATEGORY_CALL) return true
        if (n.extras.getString(Notification.EXTRA_TEMPLATE)?.contains("CallStyle") == true) return true
        return sbn.packageName in DIALERS && (n.flags and Notification.FLAG_ONGOING_EVENT) != 0
    }

    private fun handleCall(sbn: StatusBarNotification, force: Boolean = false): Boolean {
        if (!force && !isCallNotification(sbn) && !(isVoipOngoing(sbn) && audioCallStart > 0L)) return false
        val n = sbn.notification
        val extras = n.extras

        val callType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) extras.getInt(Notification.EXTRA_CALL_TYPE, 0) else 0
        val ongoingFlag = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val usesChronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)
        val mode = runCatching { audio.mode }.getOrDefault(AudioManager.MODE_NORMAL)
        val audioInCall = mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION
        val isIncoming = when {
            callType == CALL_TYPE_ONGOING || usesChronometer -> false
            callType == CALL_TYPE_INCOMING || callType == CALL_TYPE_SCREENING -> !audioInCall
            audioInCall -> false
            mode == AudioManager.MODE_RINGTONE -> true
            else -> n.fullScreenIntent != null || (n.actions?.size ?: 0) >= 2
        }
        if (!ongoingFlag && !isIncoming && !usesChronometer && !audioInCall) return true

        if (sbn.key != callKey) {
            if (callKey != AUDIO_CALL_KEY) callConnectedAt = 0L
            callAvatar = Bitmaps.fromIcon(this, n.getLargeIcon())
        }
        if (!isIncoming && callConnectedAt == 0L) {
            val reported = n.`when`
            val now = System.currentTimeMillis()
            // Trust the dialer's chronometer base when it is sane, otherwise start counting now.
            callConnectedAt = when {
                usesChronometer && reported in (now - 12 * 3600_000L)..now -> reported
                audioCallStart > 0L -> audioCallStart
                else -> now
            }
        }
        if (isIncoming) callConnectedAt = 0L

        val caller = callerName(n) ?: "Unknown"
        val actions = n.actions.orEmpty()
        fun byTitle(vararg words: String): PendingIntent? = actions.firstOrNull { a ->
            val t = a.title?.toString()?.lowercase().orEmpty()
            words.any { it in t }
        }?.actionIntent

        val answer = extraIntent(n, "android.answerIntent") ?: byTitle("answer", "accept", "pick")
            ?: if (isIncoming && actions.size >= 2) actions.last().actionIntent else null
        val decline = extraIntent(n, "android.declineIntent") ?: byTitle("decline", "reject", "dismiss")
            ?: if (isIncoming && actions.isNotEmpty()) actions.first().actionIntent else null
        val hangUp = extraIntent(n, "android.hangUpIntent") ?: byTitle("hang up", "end", "hangup")
            ?: if (!isIncoming && actions.isNotEmpty()) actions.last().actionIntent else null

        val avatar = callAvatar
        callKey = sbn.key
        if (lastCallSbn == null) main.postDelayed(callWatcher, 1000)
        lastCallSbn = sbn
        IslandEngine.setCall(
            IslandActivity.Call(
                key = sbn.key,
                caller = caller,
                isIncoming = isIncoming,
                startedAt = if (isIncoming) 0L else callConnectedAt,
                avatar = avatar,
                answer = answer,
                decline = decline,
                hangUp = hangUp,
                open = n.contentIntent ?: n.fullScreenIntent,
            ),
        )
        return true
    }

    @Suppress("DEPRECATION")
    private fun callerName(n: Notification): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val person = n.extras.getParcelable<android.app.Person>(Notification.EXTRA_CALL_PERSON)
            person?.name?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() }
    }

    @Suppress("DEPRECATION")
    private fun extraIntent(n: Notification, key: String): PendingIntent? =
        runCatching { n.extras.getParcelable<PendingIntent>(key) }.getOrNull()

    // Notifications ---------------------------------------------------------------------------

    private fun isMediaNotification(n: Notification): Boolean =
        n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
            n.category == Notification.CATEGORY_TRANSPORT ||
            n.extras.getString(Notification.EXTRA_TEMPLATE)?.contains("MediaStyle") == true

    private fun handleNotice(sbn: StatusBarNotification) {
        if (!IslandEngine.isPremium) return
        val n = sbn.notification
        if (sbn.isOngoing || !sbn.isClearable) return
        if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        if (n.category == Notification.CATEGORY_PROGRESS || n.category == Notification.CATEGORY_SERVICE) return

        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = (n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: n.extras.getCharSequence(Notification.EXTRA_TEXT))
            ?.toString()?.trim().orEmpty()
        if (title.isEmpty() && text.isEmpty()) return

        // Apps re-post the same notification constantly (progress, edits); only alert once per burst.
        val now = SystemClock.elapsedRealtime()
        val last = lastNoticeAt[sbn.key]
        lastNoticeAt[sbn.key] = now
        if (last != null && now - last < 4000) return
        if (lastNoticeAt.size > 64) lastNoticeAt.entries.removeAll { now - it.value > 60_000 }

        val pm = packageManager
        val appName = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString() }
            .getOrDefault(sbn.packageName)
        val icon = Bitmaps.fromIcon(this, n.getLargeIcon()) ?: Bitmaps.appIcon(this, sbn.packageName)

        IslandEngine.showTransient(
            IslandActivity.Notice(
                key = sbn.key,
                appName = appName,
                title = title.ifEmpty { appName },
                text = text,
                icon = icon,
                open = n.contentIntent,
            ),
            durationMs = 4200L,
        )
    }

    private companion object {
        // Mirrors Notification.CallStyle.CALL_TYPE_* (API 31) so the parser also works on older releases.
        const val CALL_TYPE_INCOMING = 1
        const val CALL_TYPE_ONGOING = 2
        const val CALL_TYPE_SCREENING = 3

        const val AUDIO_CALL_KEY = "audio-call"

        /** Calling apps whose ongoing-call notification may not be tagged as a call. */
        val VOIP_APPS = setOf(
            "com.facebook.orca", "com.facebook.mlite", "com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger",
            "com.instagram.android", "com.skype.raider", "com.viber.voip", "org.thoughtcrime.securesms",
            "com.google.android.apps.tachyon", "us.zoom.videomeetings", "com.microsoft.teams", "jp.naver.line.android",
            "com.discord", "com.snapchat.android", "com.imo.android.imoim", "com.tencent.mm",
        )

        /** Stock and vendor in-call UIs, for dialers that don't tag their notification as a call. */
        val DIALERS = setOf(
            "com.google.android.dialer", "com.android.dialer", "com.android.incallui", "com.android.server.telecom",
            "com.samsung.android.incallui", "com.samsung.android.dialer", "com.oplus.incallui", "com.coloros.incallui",
            "com.android.phone", "com.miui.voip", "com.vivo.incallui", "com.huawei.contacts", "com.hihonor.contacts",
        )
    }
}
