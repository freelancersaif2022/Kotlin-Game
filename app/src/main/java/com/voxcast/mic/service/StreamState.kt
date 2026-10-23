package com.voxcast.mic.service

import com.voxcast.mic.net.ConnectionInfo

enum class Role { MIC, SPEAKER }

data class LinkStats(
    val rttMs: Int = -1,
    val lossPercent: Float = 0f,
    val kbps: Int = 0,
    val bufferMs: Int = 0,
)

sealed interface StreamState {
    val role: Role?

    data object Idle : StreamState { override val role: Role? = null }

    data class Preparing(override val role: Role, val message: String) : StreamState

    data class SpeakerWaiting(val info: ConnectionInfo) : StreamState { override val role = Role.SPEAKER }

    data class SpeakerLive(
        val info: ConnectionInfo,
        val micName: String,
        val since: Long,
        val talking: Boolean,
    ) : StreamState { override val role = Role.SPEAKER }

    data class MicConnecting(
        val speakerName: String,
        val stage: String,
        val step: Int,
        val ssidToJoinManually: String? = null,
        val passphrase: String? = null,
    ) : StreamState { override val role = Role.MIC }

    data class MicLive(
        val speakerName: String,
        val since: Long,
        val sampleRate: Int,
    ) : StreamState { override val role = Role.MIC }

    data class MicReconnecting(val speakerName: String, val attempt: Int) : StreamState { override val role = Role.MIC }

    data class Failed(override val role: Role, val title: String, val message: String) : StreamState
}

sealed interface StreamEvent {
    data class Info(val text: String) : StreamEvent
    data class Success(val text: String) : StreamEvent
    data class Warning(val text: String) : StreamEvent
    data class Error(val text: String) : StreamEvent
    data class SessionEnded(val role: Role) : StreamEvent
}
