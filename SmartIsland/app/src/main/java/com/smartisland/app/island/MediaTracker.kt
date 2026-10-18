package com.smartisland.app.island

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.smartisland.app.system.Bitmaps
import com.smartisland.app.system.PendingIntents
import com.smartisland.app.system.systemService
import com.smartisland.app.widget.WidgetUpdater

/**
 * Follows the most relevant active media session. Requires notification-listener access, which is what
 * grants [MediaSessionManager.getActiveSessions].
 */
class MediaTracker(
    private val context: Context,
    private val listenerComponent: ComponentName,
) : MediaCommands {

    private val main = Handler(Looper.getMainLooper())
    private val sessionManager = context.systemService<MediaSessionManager>()
    private var controller: MediaController? = null
    private var lastArtKey: String? = null
    private var cachedArt: android.graphics.Bitmap? = null
    private var cachedAccent: Int = DEFAULT_ACCENT

    private val hidePaused = Runnable { if (controller?.playbackState?.state != PlaybackState.STATE_PLAYING) publish(null) }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        pick(list.orEmpty())
    }

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
        override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
        override fun onSessionDestroyed() {
            detach()
            pick(activeSessions())
        }
    }

    fun start() {
        runCatching { sessionManager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, main) }
        pick(activeSessions())
        IslandEngine.mediaCommands = this
    }

    fun stop() {
        runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsListener) }
        detach()
        main.removeCallbacks(hidePaused)
        if (IslandEngine.mediaCommands === this) IslandEngine.mediaCommands = null
        publish(null)
    }

    /** Re-evaluates sessions; called when a media notification is posted by an app we did not see yet. */
    fun rescan() = pick(activeSessions())

    private fun activeSessions(): List<MediaController> =
        runCatching { sessionManager.getActiveSessions(listenerComponent) }.getOrDefault(emptyList())

    private fun pick(sessions: List<MediaController>) {
        val best = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull { it.sessionToken == controller?.sessionToken }
            ?: sessions.firstOrNull { it.metadata != null }
        if (best?.sessionToken != controller?.sessionToken) {
            detach()
            controller = best
            best?.registerCallback(callback, main)
        }
        refresh()
    }

    private fun detach() {
        controller?.unregisterCallback(callback)
        controller = null
    }

    private fun refresh() {
        val c = controller ?: return publish(null)
        val meta = c.metadata ?: return publish(null)
        val state = c.playbackState
        val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        if (title.isNullOrBlank()) return publish(null)

        val playing = state?.state == PlaybackState.STATE_PLAYING || state?.state == PlaybackState.STATE_BUFFERING
        val artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: appLabel(c.packageName)

        val artKey = "${c.packageName}|$title|$artist"
        if (artKey != lastArtKey) {
            lastArtKey = artKey
            val raw = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: meta.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            cachedArt = Bitmaps.shrink(raw) ?: Bitmaps.appIcon(context, c.packageName)
            cachedAccent = Bitmaps.accentOf(cachedArt, DEFAULT_ACCENT)
        }

        main.removeCallbacks(hidePaused)
        if (!playing) main.postDelayed(hidePaused, PAUSED_GRACE_MS)

        publish(
            IslandActivity.Media(
                title = title,
                artist = artist,
                art = cachedArt,
                accent = cachedAccent,
                isPlaying = playing,
                positionMs = state?.position ?: 0L,
                durationMs = meta.getLong(MediaMetadata.METADATA_KEY_DURATION),
                speed = state?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
                sampledAt = state?.lastPositionUpdateTime?.takeIf { it > 0 } ?: SystemClock.elapsedRealtime(),
                packageName = c.packageName,
            ),
        )
    }

    private fun publish(media: IslandActivity.Media?) {
        val prev = IslandEngine.mediaState.value
        IslandEngine.setMedia(media)
        if (prev?.title != media?.title || prev?.isPlaying != media?.isPlaying) {
            WidgetUpdater.updateMedia(context)
        }
    }

    private fun appLabel(pkg: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault("")

    // MediaCommands ---------------------------------------------------------------------------

    override fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
        else c.transportControls.play()
    }

    override fun next() {
        controller?.transportControls?.skipToNext()
    }

    override fun previous() {
        controller?.transportControls?.skipToPrevious()
    }

    override fun seekTo(positionMs: Long) {
        controller?.transportControls?.seekTo(positionMs)
    }

    override fun open() {
        val c = controller ?: return
        val pi = c.sessionActivity
        if (pi != null) {
            PendingIntents.send(context, pi)
        } else {
            context.packageManager.getLaunchIntentForPackage(c.packageName)
                ?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                ?.let { runCatching { context.startActivity(it) } }
        }
    }

    private companion object {
        const val PAUSED_GRACE_MS = 45_000L
        const val DEFAULT_ACCENT = 0xFFFF375F.toInt()
    }
}
