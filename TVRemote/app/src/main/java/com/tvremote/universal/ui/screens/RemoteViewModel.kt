package com.tvremote.universal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvremote.universal.App
import com.tvremote.universal.RemoteSession
import com.tvremote.universal.core.WakeOnLan
import com.tvremote.universal.data.RemoteKey
import com.tvremote.universal.data.StreamingApp
import com.tvremote.universal.data.TvDevice
import com.tvremote.universal.data.TvPlatform
import com.tvremote.universal.tv.Capabilities
import com.tvremote.universal.tv.ConnectionState
import com.tvremote.universal.tv.ControllerFactory
import com.tvremote.universal.tv.FailReason
import com.tvremote.universal.tv.TvController
import com.tvremote.universal.tv.TvDiscovery
import com.tvremote.universal.ui.components.SnackKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RemoteViewModel(private val deviceId: String) : ViewModel() {
    private val c = App.container

    val device: StateFlow<TvDevice?> = c.tvStore.devices
        .map { list -> list.firstOrNull { it.id == deviceId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, c.tvStore.get(deviceId))

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _capabilities = MutableStateFlow<Capabilities?>(null)
    val capabilities: StateFlow<Capabilities?> = _capabilities.asStateFlow()

    private val _waking = MutableStateFlow(false)
    val waking: StateFlow<Boolean> = _waking.asStateFlow()

    private val _events = MutableSharedFlow<Pair<String, SnackKind>>(extraBufferCapacity = 4)
    val events: SharedFlow<Pair<String, SnackKind>> = _events.asSharedFlow()

    private var controller: TvController? = null
    private var stateJob: Job? = null
    private var queueJob: Job? = null
    private var connectJob: Job? = null
    private var foreground = false
    private var retries = 0

    /** Key presses are executed strictly in order, off the main thread. */
    private val queue = Channel<suspend (TvController) -> Unit>(64, BufferOverflow.DROP_OLDEST)

    init {
        c.settings.setLastDevice(deviceId)
        c.tvStore.update(deviceId) { it.copy(lastUsed = System.currentTimeMillis()) }
        ensureController()
    }

    private fun ensureController() {
        val stored = c.tvStore.get(deviceId) ?: return
        val current = controller
        if (current != null && current.device.host == stored.host && current.device.credentials == stored.credentials) return
        current?.close()
        stateJob?.cancel()
        queueJob?.cancel()
        val ctrl = ControllerFactory.create(stored, c) { updated ->
            c.tvStore.update(deviceId) { s ->
                s.copy(
                    credentials = updated.credentials,
                    mac = s.mac.ifBlank { updated.mac },
                    model = s.model.ifBlank { updated.model },
                )
            }
        }
        controller = ctrl
        _capabilities.value = ctrl.capabilities
        stateJob = viewModelScope.launch {
            ctrl.state.collect { s ->
                _state.value = s
                onStateChanged(s)
            }
        }
        queueJob = viewModelScope.launch(Dispatchers.IO) {
            for (action in queue) runCatching { action(ctrl) }
        }
        if (foreground) RemoteSession.active = ctrl
    }

    fun onForeground() {
        foreground = true
        ensureController()
        RemoteSession.active = controller
        RemoteSession.onVolumeKey = { up -> press(if (up) RemoteKey.VOLUME_UP else RemoteKey.VOLUME_DOWN) }
        val s = _state.value
        if (s !is ConnectionState.Connected && s !is ConnectionState.Connecting) connect()
    }

    fun onBackground() {
        foreground = false
        if (RemoteSession.active === controller) {
            RemoteSession.onVolumeKey = null
            RemoteSession.active = null
        }
        connectJob?.cancel()
        controller?.disconnect()
    }

    fun connect() {
        val ctrl = controller ?: return
        connectJob?.cancel()
        connectJob = viewModelScope.launch { ctrl.connect() }
    }

    private fun onStateChanged(s: ConnectionState) {
        if (s is ConnectionState.Connected) retries = 0
        if (s is ConnectionState.Failed && s.reason == FailReason.UNREACHABLE && foreground && !_waking.value && retries < 3) {
            retries++
            val attempt = retries
            viewModelScope.launch {
                delay(1500L * attempt)
                if (foreground && !_waking.value && _state.value is ConnectionState.Failed) connect()
            }
        }
    }

    private fun enqueue(action: suspend (TvController) -> Unit) {
        if (_state.value !is ConnectionState.Connected) {
            _events.tryEmit("Not connected to your TV yet — reconnecting…" to SnackKind.INFO)
            if (_state.value is ConnectionState.Failed || _state.value is ConnectionState.Idle) connect()
            return
        }
        queue.trySend(action)
    }

    fun press(key: RemoteKey) = enqueue { it.sendKey(key) }
    fun sendText(text: String) = enqueue { it.sendText(text) }
    fun backspace() = enqueue { it.sendBackspace() }
    fun enter() = enqueue { it.sendEnter() }
    fun launch(app: StreamingApp) = enqueue { it.launchApp(app) }

    val canWake: Boolean
        get() {
            val d = device.value ?: return false
            return d.mac.isNotBlank() || d.platform == TvPlatform.ROKU || d.platform == TvPlatform.VIZIO
        }

    /** Power key. Returns false when turning the TV on requires Premium. */
    fun power(premium: Boolean): Boolean {
        val s = _state.value
        if (s is ConnectionState.Connected) {
            queue.trySend { it.sendKey(RemoteKey.POWER) }
            return true
        }
        if (!premium) return false
        wake()
        return true
    }

    fun wake() {
        val ctrl = controller ?: return
        val d = device.value ?: return
        if (_waking.value) return
        if (!canWake) {
            _events.tryEmit("Add your TV's MAC address in TV settings so the app can turn it on." to SnackKind.INFO)
            return
        }
        viewModelScope.launch {
            _waking.value = true
            connectJob?.cancel()
            _events.tryEmit("Waking up ${d.name}…" to SnackKind.INFO)
            val deadline = System.currentTimeMillis() + 40_000
            while (System.currentTimeMillis() < deadline) {
                withContext(Dispatchers.IO) {
                    if (d.mac.isNotBlank()) WakeOnLan.send(c.app, d.mac, d.host)
                    runCatching { ctrl.wake() }
                }
                delay(1500)
                // Only try a full connection once the TV's control port answers again.
                if (TvDiscovery.probe(d.platform, d.host)) {
                    ctrl.connect()
                    if (ctrl.state.value is ConnectionState.Connected) break
                }
            }
            val s = ctrl.state.value
            if (s is ConnectionState.Connected && !s.tvOn) runCatching { ctrl.sendKey(RemoteKey.POWER) }
            _waking.value = false
            if (s is ConnectionState.Connected) {
                _events.tryEmit("${d.name} is on" to SnackKind.SUCCESS)
            } else {
                _events.tryEmit("Your TV didn't wake up. Check the power-on setting for your TV in Help." to SnackKind.ERROR)
            }
        }
    }

    override fun onCleared() {
        if (RemoteSession.active === controller) {
            RemoteSession.active = null
            RemoteSession.onVolumeKey = null
        }
        controller?.close()
        queue.close()
    }
}
