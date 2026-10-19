package com.eyescroll.app.scroll

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PadStatus(
    /** Accessibility service connected (pad can be shown and scroll). */
    val serviceRunning: Boolean = false,
    val padVisible: Boolean = false,
    val scrollCount: Int = 0,
    val lastScroll: ScrollDirection? = null,
    val holding: Boolean = false,
)

/** In-process state shared between the accessibility service and the UI. */
object PadBus {
    private val _status = MutableStateFlow(PadStatus())
    val status: StateFlow<PadStatus> = _status.asStateFlow()

    fun update(block: (PadStatus) -> PadStatus) {
        while (true) {
            val cur = _status.value
            if (_status.compareAndSet(cur, block(cur))) return
        }
    }
}
