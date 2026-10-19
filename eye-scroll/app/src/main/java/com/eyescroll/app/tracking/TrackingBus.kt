package com.eyescroll.app.tracking

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TrackingPhase { OFF, STARTING, TRACKING, NO_FACE, ERROR }

data class TrackingStatus(
    val phase: TrackingPhase = TrackingPhase.OFF,
    val gaze: GazeSnapshot = GazeSnapshot(),
    val fps: Int = 0,
    val message: String? = null,
    val lastScroll: ScrollDirection? = null,
    val lastScrollAt: Long = 0L,
    val scrollCount: Int = 0,
    /** Seconds left before the camera turns itself off, or -1 when auto-off is disabled. */
    val autoOffIn: Int = -1,
) {
    val isActive: Boolean get() = phase == TrackingPhase.STARTING || phase == TrackingPhase.TRACKING || phase == TrackingPhase.NO_FACE
}

/** In-process bus between the tracking service, the accessibility service (scrolling + bubble) and the UI. */
object TrackingBus {
    private val _status = MutableStateFlow(TrackingStatus())
    val status: StateFlow<TrackingStatus> = _status.asStateFlow()

    private val _scrolls = MutableSharedFlow<ScrollDirection>(extraBufferCapacity = 8)
    val scrolls: SharedFlow<ScrollDirection> = _scrolls.asSharedFlow()

    fun update(block: (TrackingStatus) -> TrackingStatus) {
        while (true) {
            val cur = _status.value
            if (_status.compareAndSet(cur, block(cur))) return
        }
    }

    fun set(status: TrackingStatus) {
        _status.value = status
    }

    fun emitScroll(direction: ScrollDirection) {
        _scrolls.tryEmit(direction)
    }
}
