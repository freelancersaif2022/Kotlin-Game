package com.voxcast.mic.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.voxcast.mic.net.HostMode
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Single source of truth shared by the UI and [StreamService]. */
object StreamController {

    private val _state = MutableStateFlow<StreamState>(StreamState.Idle)
    val state: StateFlow<StreamState> = _state.asStateFlow()

    private val _level = MutableStateFlow(0f)
    /** Live audio level 0..1 (mic input on the mic phone, playback on the speaker phone). */
    val level: StateFlow<Float> = _level.asStateFlow()

    private val _stats = MutableStateFlow(LinkStats())
    val stats: StateFlow<LinkStats> = _stats.asStateFlow()

    private val _onAir = MutableStateFlow(true)
    /** Whether the microphone is transmitting (false = muted / push-to-talk released). */
    val onAir: StateFlow<Boolean> = _onAir.asStateFlow()

    private val _events = MutableSharedFlow<StreamEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<StreamEvent> = _events.asSharedFlow()

    @Volatile var speakerVolume: Float = 1f

    @Volatile internal var speakerEngine: com.voxcast.mic.audio.SpeakerEngine? = null

    /** Issues a brand-new QR code (new secret token) while waiting for a microphone. */
    fun refreshCode() { speakerEngine?.refreshToken() }

    internal fun update(state: StreamState) { _state.value = state }
    internal fun level(value: Float) { _level.value = value }
    internal fun stats(value: LinkStats) { _stats.value = value }
    internal fun emit(event: StreamEvent) { _events.tryEmit(event) }

    fun setOnAir(value: Boolean) { _onAir.value = value }

    fun startSpeaker(context: Context, mode: HostMode) {
        _state.value = StreamState.Preparing(Role.SPEAKER, if (mode == HostMode.WIFI_DIRECT) "Creating Wi-Fi Direct group" else "Opening speaker")
        val intent = Intent(context, StreamService::class.java)
            .setAction(StreamService.ACTION_START_SPEAKER)
            .putExtra(StreamService.EXTRA_MODE, mode.name)
        ContextCompat.startForegroundService(context, intent)
    }

    fun startMic(context: Context, qrPayload: String, startOnAir: Boolean) {
        _onAir.value = startOnAir
        _state.value = StreamState.MicConnecting("Speaker", "Reading secure code", 0)
        val intent = Intent(context, StreamService::class.java)
            .setAction(StreamService.ACTION_START_MIC)
            .putExtra(StreamService.EXTRA_PAYLOAD, qrPayload)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        if (_state.value is StreamState.Idle) return
        runCatching { context.startService(Intent(context, StreamService::class.java).setAction(StreamService.ACTION_STOP)) }
            .onFailure { _state.value = StreamState.Idle }
    }

    /** Clears a terminal Failed state so screens can retry. */
    fun acknowledgeFailure() {
        if (_state.value is StreamState.Failed) _state.value = StreamState.Idle
    }
}
