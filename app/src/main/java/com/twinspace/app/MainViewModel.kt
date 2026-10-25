package com.twinspace.app

import android.app.Activity
import android.app.Application
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.os.SystemClock
import android.os.UserHandle
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.twinspace.app.core.AppEntry
import com.twinspace.app.core.ShortcutHelper
import com.twinspace.app.core.SpaceContract
import com.twinspace.app.core.SpaceContract.Op
import com.twinspace.app.core.SpaceStatus
import com.twinspace.app.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { Home, AddApps, Settings }

data class UiState(
    val loading: Boolean = true,
    val status: SpaceStatus = SpaceStatus.NONE,
    val canCreate: Boolean = false,
    val settingUp: Boolean = false,
    val cloned: List<AppEntry> = emptyList(),
    val installable: List<AppEntry> = emptyList(),
    val installableLoaded: Boolean = false,
    /** Non-null while a request is running inside the space. */
    val busy: String? = null,
    val locked: Boolean = false,
    val screen: Screen = Screen.Home,
)

sealed interface UiEvent {
    data class Message(val text: String, val actionLabel: String? = null, val action: (() -> Unit)? = null) : UiEvent
    data class StartForResult(val intent: Intent, val kind: Kind) : UiEvent
    data object Authenticate : UiEvent

    enum class Kind { GATEWAY, PROVISION }
}

/** A request sent into the space, remembered until its result comes back. */
private sealed interface PendingOp {
    data class Clone(val entries: List<AppEntry>) : PendingOp
    data class Remove(val entry: AppEntry) : PendingOp
    data class Freeze(val entry: AppEntry, val hide: Boolean) : PendingOp
    data class RenameSpace(val name: String) : PendingOp
    data object DeleteSpace : PendingOp
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TwinSpaceApplication
    private val space = app.space
    private val prefs = app.prefs
    val icons = app.icons

    private val _state = MutableStateFlow(UiState(locked = prefs.settings.value.lockEnabled))
    val state: StateFlow<UiState> = _state.asStateFlow()
    val settings: StateFlow<Settings> = prefs.settings

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var pending: PendingOp? = null
    private var refreshJob: Job? = null
    private var stoppedAt = 0L

    private val launcherCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackageAdded(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackageChanged(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
        override fun onShortcutsChanged(packageName: String, shortcuts: MutableList<ShortcutInfo>, user: UserHandle) = Unit
    }

    private val profileReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refresh()
    }

    init {
        space.registerCallback(launcherCallback)
        ContextCompat.registerReceiver(
            application,
            profileReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
                addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
                addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
                addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        refresh()
    }

    override fun onCleared() {
        space.unregisterCallback(launcherCallback)
        runCatching { getApplication<Application>().unregisterReceiver(profileReceiver) }
    }

    private fun changed(packageName: String) {
        icons.invalidate(packageName)
        refresh()
    }

    // ---------- Loading ----------

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val frozen = prefs.settings.value.frozen
            val loadInstallable = _state.value.installableLoaded || _state.value.screen == Screen.AddApps
            val result = withContext(Dispatchers.IO) {
                val status = space.status()
                val user = space.spaceUser()
                val cloned = if (user != null) space.clonedApps(user, frozen) else emptyList()
                val installable = if (loadInstallable) space.installableApps(cloned.mapTo(HashSet()) { it.packageName }) else null
                Triple(status, cloned, installable)
            }
            val (status, cloned, installable) = result
            // A clone the user unfroze from somewhere else shows up as visible again.
            cloned.filter { !it.frozen && it.packageName in frozen }.forEach { prefs.setFrozen(it.packageName, false) }
            _state.update {
                it.copy(
                    loading = false,
                    status = status,
                    canCreate = status == SpaceStatus.NONE && space.canCreateSpace(),
                    settingUp = it.settingUp && status != SpaceStatus.ACTIVE,
                    cloned = cloned,
                    installable = installable ?: it.installable,
                    installableLoaded = installable != null || it.installableLoaded,
                )
            }
        }
    }

    fun navigate(screen: Screen) {
        _state.update { it.copy(screen = screen) }
        if (screen == Screen.AddApps) refresh()
    }

    fun displayName(entry: AppEntry): String = settings.value.aliases[entry.packageName] ?: entry.label

    // ---------- Space creation ----------

    fun createSpace() {
        val intent = space.provisioningIntent(prefs.token(), settings.value.spaceName.ifBlank { "Twin Space" })
        send(UiEvent.StartForResult(intent, UiEvent.Kind.PROVISION))
    }

    fun onProvisionResult(resultCode: Int) {
        if (resultCode != Activity.RESULT_OK) {
            message("Space setup was cancelled. Nothing was changed.")
            return
        }
        _state.update { it.copy(settingUp = true) }
        viewModelScope.launch {
            // The space finishes its own setup in the background; watch for it to appear.
            repeat(40) {
                refresh()
                refreshJob?.join()
                if (_state.value.status == SpaceStatus.ACTIVE) {
                    _state.update { s -> s.copy(settingUp = false) }
                    message("Your space is ready. Add the apps you want to clone.")
                    return@launch
                }
                delay(750)
            }
            _state.update { it.copy(settingUp = false) }
        }
    }

    // ---------- App actions ----------

    fun launch(entry: AppEntry, bounds: Rect? = null) {
        if (entry.frozen) {
            message("${displayName(entry)} is frozen", "Unfreeze") { setFrozen(entry, false) }
            return
        }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { space.launch(entry.packageName, entry.user, bounds) }
            if (!ok) message("Couldn't open ${displayName(entry)}.")
        }
    }

    fun openAppInfo(entry: AppEntry) {
        if (!space.openAppInfo(entry)) message("App info isn't available while the clone is frozen.")
    }

    fun rename(entry: AppEntry, alias: String?) {
        val clean = alias?.trim()?.takeIf { it.isNotEmpty() && it != entry.label }
        prefs.setAlias(entry.packageName, clean)
        val label = clean ?: entry.label
        viewModelScope.launch(Dispatchers.IO) {
            ShortcutHelper.refreshIfPinned(getApplication(), entry, label, icons)
        }
        message(if (clean == null) "Name reset to ${entry.label}" else "Renamed to $clean")
    }

    fun addShortcut(entry: AppEntry) {
        val label = displayName(entry)
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { ShortcutHelper.requestPin(getApplication(), entry, label, icons) }
            if (!ok) message("Your launcher doesn't support adding shortcuts.")
        }
    }

    fun clone(entries: List<AppEntry>) {
        if (entries.isEmpty()) return
        val what = if (entries.size == 1) entries.first().label else "${entries.size} apps"
        runInSpace(PendingOp.Clone(entries), "Cloning $what…", Op.CLONE, entries.map { it.packageName })
    }

    fun remove(entry: AppEntry) =
        runInSpace(PendingOp.Remove(entry), "Waiting for confirmation…", Op.REMOVE, listOf(entry.packageName))

    fun setFrozen(entry: AppEntry, hide: Boolean) = runInSpace(
        PendingOp.Freeze(entry, hide),
        if (hide) "Freezing…" else "Unfreezing…",
        Op.FREEZE,
        listOf(entry.packageName),
        hidden = hide,
    )

    fun renameSpace(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        runInSpace(PendingOp.RenameSpace(clean), "Renaming space…", Op.RENAME_SPACE, name = clean)
    }

    fun deleteSpace() = runInSpace(PendingOp.DeleteSpace, "Removing space…", Op.DELETE_SPACE)

    fun setPaused(paused: Boolean) {
        if (space.setPaused(paused)) {
            viewModelScope.launch { delay(600); refresh() }
        } else {
            message(
                if (paused) "Turn off \"Work apps\" in Quick Settings to pause your space."
                else "Turn on \"Work apps\" in Quick Settings, or open any clone to resume.",
            )
        }
    }

    private fun runInSpace(
        op: PendingOp,
        busyText: String,
        kind: Op,
        packages: List<String> = emptyList(),
        hidden: Boolean = false,
        name: String? = null,
    ) {
        if (pending != null) return
        if (_state.value.status == SpaceStatus.PAUSED) {
            message("Your space is paused.", "Resume") { setPaused(false) }
            return
        }
        val intent = space.gatewayIntent(kind, prefs.token(), packages, hidden, name)
        if (intent == null) {
            message("Can't reach your space right now. Make sure it's turned on.")
            refresh()
            return
        }
        pending = op
        _state.update { it.copy(busy = busyText) }
        send(UiEvent.StartForResult(intent, UiEvent.Kind.GATEWAY))
    }

    fun onGatewayResult(resultCode: Int, data: Intent?) {
        val op = pending ?: return
        pending = null
        _state.update { it.copy(busy = null) }
        val ok = resultCode == Activity.RESULT_OK
        val done = data?.getStringArrayListExtra(SpaceContract.EXTRA_DONE).orEmpty()
        val error = data?.getStringExtra(SpaceContract.EXTRA_ERROR)

        when (op) {
            is PendingOp.Clone -> {
                val failed = data?.getStringArrayListExtra(SpaceContract.EXTRA_FAILED).orEmpty()
                when {
                    !ok -> message(error ?: "Cloning didn't finish. Try again.")
                    done.isNotEmpty() && failed.isEmpty() -> {
                        message(if (done.size == 1) "${op.entries.first().label} is ready in your space" else "${done.size} apps cloned")
                        _state.update { it.copy(screen = Screen.Home) }
                    }
                    else -> {
                        val names = op.entries.filter { it.packageName in failed }.joinToString { it.label }
                        message("Couldn't clone $names. Android doesn't allow it for this app.")
                    }
                }
            }
            is PendingOp.Remove -> if (op.entry.packageName in done) {
                prefs.forgetApp(op.entry.packageName)
                message("${displayName(op.entry)} removed from your space")
            } else if (error != null) {
                message(error)
            }
            is PendingOp.Freeze -> if (op.entry.packageName in done) {
                prefs.setFrozen(op.entry.packageName, op.hide)
                message(if (op.hide) "${displayName(op.entry)} frozen. It won't run until you unfreeze it." else "${displayName(op.entry)} unfrozen")
            } else {
                message(error ?: "Couldn't change ${displayName(op.entry)}.")
            }
            is PendingOp.RenameSpace -> if (ok) {
                prefs.setSpaceName(op.name)
                message("Space renamed to ${op.name}")
            } else {
                message(error ?: "Couldn't rename the space.")
            }
            PendingOp.DeleteSpace -> {
                if (ok) {
                    prefs.clearSpaceState()
                    _state.update { it.copy(screen = Screen.Home) }
                    message("Space removed")
                } else {
                    message(error ?: "The space wasn't removed.")
                }
                viewModelScope.launch {
                    repeat(10) { refresh(); refreshJob?.join(); if (_state.value.status == SpaceStatus.NONE) return@launch; delay(800) }
                }
                return
            }
        }
        refresh()
    }

    // ---------- App lock ----------

    fun isDeviceSecure(): Boolean =
        getApplication<Application>().getSystemService(KeyguardManager::class.java)!!.isDeviceSecure

    fun setLockEnabled(enabled: Boolean) {
        if (enabled && !isDeviceSecure()) {
            message("Set a screen lock in Android settings first.")
            return
        }
        prefs.setLockEnabled(enabled)
        message(if (enabled) "App lock is on" else "App lock is off")
    }

    fun onAuthenticated() = _state.update { it.copy(locked = false) }

    fun requestUnlock() = send(UiEvent.Authenticate)

    fun onStop() {
        stoppedAt = SystemClock.elapsedRealtime()
    }

    fun onStart() {
        val away = SystemClock.elapsedRealtime() - stoppedAt
        if (stoppedAt != 0L && pending == null && prefs.settings.value.lockEnabled && away > RELOCK_AFTER_MS) {
            _state.update { it.copy(locked = true) }
        }
        refresh()
    }

    // ---------- helpers ----------

    private fun send(event: UiEvent) {
        _events.trySend(event)
    }

    private fun message(text: String, actionLabel: String? = null, action: (() -> Unit)? = null) =
        send(UiEvent.Message(text, actionLabel, action))

    private companion object {
        const val RELOCK_AFTER_MS = 5_000L
    }
}
