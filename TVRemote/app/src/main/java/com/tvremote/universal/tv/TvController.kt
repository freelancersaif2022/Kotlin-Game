package com.tvremote.universal.tv

import com.tvremote.universal.data.RemoteKey
import com.tvremote.universal.data.StreamingApp
import com.tvremote.universal.data.TvDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

enum class FailReason { UNREACHABLE, NEEDS_PAIRING, CERT_CHANGED, OTHER }

sealed interface ConnectionState {
    data object Idle : ConnectionState
    /** [hint] is shown when the TV is waiting for the user, e.g. "Press Allow on your TV". */
    data class Connecting(val hint: String? = null) : ConnectionState
    data class Connected(val tvOn: Boolean = true) : ConnectionState
    data class Failed(val reason: FailReason, val message: String) : ConnectionState
}

data class Capabilities(
    val keys: Set<RemoteKey>,
    val text: Boolean,
    val backspace: Boolean,
    val apps: Set<StreamingApp>,
) {
    val numbers: Boolean get() = RemoteKey.NUMBERS.all { it in keys }
}

/** One live connection to one TV. */
interface TvController {
    val device: TvDevice
    val state: StateFlow<ConnectionState>
    val capabilities: Capabilities

    /** Connects and returns when connected or failed. Safe to call again to reconnect. */
    suspend fun connect()

    suspend fun sendKey(key: RemoteKey)
    suspend fun sendText(text: String) {}
    suspend fun sendBackspace() {}
    suspend fun sendEnter() = sendKey(RemoteKey.OK)
    suspend fun launchApp(app: StreamingApp) {}

    /** Platform specific wake attempt while not connected (in addition to Wake-on-LAN). */
    suspend fun wake() {}

    fun disconnect()
    fun close()
}

/** Shared plumbing for controllers. [onDeviceChanged] persists tokens, MAC addresses, etc. */
abstract class BaseController(
    initial: TvDevice,
    private val onDeviceChanged: (TvDevice) -> Unit,
) : TvController {

    @Volatile final override var device: TvDevice = initial
        private set

    protected val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    protected fun updateDevice(transform: (TvDevice) -> TvDevice) {
        val updated = transform(device)
        if (updated != device) {
            device = updated
            onDeviceChanged(updated)
        }
    }

    protected fun updateCredentials(vararg pairs: Pair<String, String>) =
        updateDevice { it.copy(credentials = it.credentials + pairs) }

    protected val isConnected get() = _state.value is ConnectionState.Connected

    protected fun fail(reason: FailReason, message: String) {
        _state.value = ConnectionState.Failed(reason, message)
    }

    protected suspend fun awaitSettled(timeoutMs: Long): ConnectionState =
        withTimeoutOrNull(timeoutMs) {
            state.first { it is ConnectionState.Connected || it is ConnectionState.Failed }
        } ?: _state.value

    override fun close() {
        disconnect()
        scope.cancel()
    }
}

object Messages {
    const val UNREACHABLE = "Can't reach your TV. Make sure it's turned on and on the same Wi-Fi as this phone."
    const val LOST = "Connection to the TV was lost."
    const val CERT_CHANGED = "Your TV's security certificate changed. Pair the TV again to continue."
}
