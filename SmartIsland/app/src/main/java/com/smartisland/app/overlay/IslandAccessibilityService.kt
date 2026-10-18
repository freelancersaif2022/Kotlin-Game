package com.smartisland.app.overlay

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.smartisland.app.container
import com.smartisland.app.data.IslandSettings
import com.smartisland.app.data.PremiumState
import com.smartisland.app.island.IslandActivity
import com.smartisland.app.island.IslandEngine
import com.smartisland.app.system.BatterySnapshot
import com.smartisland.app.system.Permissions
import com.smartisland.app.system.SystemActions
import com.smartisland.app.system.VolumeController
import com.smartisland.app.system.VolumeStream
import com.smartisland.app.system.systemService
import com.smartisland.app.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The always-on engine: hosts the island and edge panel overlays, performs power-key actions, owns the
 * optional volume-key takeover and keeps battery/stats widgets live.
 */
class IslandAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: IslandAccessibilityService? = null
            private set

        private val LOW_LEVELS = setOf(20, 10, 5)
        private const val VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var volume: VolumeController
    private var island: IslandOverlay? = null
    private var edge: EdgePanelOverlay? = null
    private var lastBattery: BatterySnapshot? = null
    private var tickerJob: Job? = null
    private var landscape = false
    private var filteringKeys = false
    private var receiversRegistered = false

    private val settingsRepo get() = container.settings

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        volume = VolumeController(this)
        landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        island = IslandOverlay(this, settingsRepo.settings)
        edge = EdgePanelOverlay(this, { volume.percent(VolumeStream.MEDIA) }, ::onEdgeAction)
        IslandEngine.setServiceRunning(true)

        scope.launch {
            combine(settingsRepo.settings, container.premium.state) { s, p -> s to p }
                .collect { (s, p) -> runCatching { apply(s, p) } }
        }
        registerReceivers()
        registerTorchCallback()
        startTicker()
        WidgetUpdater.updateAll(this)
        Permissions.returnToAppIfExpected(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        // System UI (shade, volume panel) and keyboards don't change which app is in front.
        if (pkg == "com.android.systemui" || event.className?.contains("InputMethod") == true) return
        appInFront = pkg == packageName
        updateEdgeHandle()
    }

    private var appInFront = false

    private fun updateEdgeHandle() {
        edge?.darkHandle?.value = appInFront && !settingsRepo.current.darkMode
    }

    override fun onInterrupt() = Unit

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        landscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        apply(settingsRepo.current, container.premium.current)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        teardown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    private fun teardown() {
        if (instance == null) return
        instance = null
        IslandEngine.setServiceRunning(false)
        island?.hide()
        edge?.hide()
        island = null
        edge = null
        if (receiversRegistered) {
            runCatching { unregisterReceiver(systemReceiver) }
            receiversRegistered = false
        }
        runCatching { systemService<CameraManager>().unregisterTorchCallback(torchCallback) }
        scope.cancel()
        WidgetUpdater.updateAll(applicationContext)
    }

    // Settings --------------------------------------------------------------------------------

    private fun apply(s: IslandSettings, p: PremiumState) {
        val island = island ?: return
        val hidden = landscape && s.hideInLandscape
        if (s.enabled) {
            island.show()
            island.applyGeometry(s)
            island.setVisible(!hidden)
        } else {
            island.hide()
        }

        if (s.edgePanelEnabled && p.isPremium) {
            edge?.show(s.edgePanelRight)
            updateEdgeHandle()
            edge?.setVisible(!hidden)
        } else {
            edge?.hide()
        }

        setKeyFiltering(s.enabled && s.replaceVolumePanel && p.isPremium)
    }

    /** Only asks for key events while the volume HUD is enabled, so keys are untouched otherwise. */
    private fun setKeyFiltering(enabled: Boolean) {
        if (enabled == filteringKeys) return
        val info = serviceInfo ?: return
        info.flags = if (enabled) {
            info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        } else {
            info.flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS.inv()
        }
        serviceInfo = info
        filteringKeys = enabled
    }

    // Volume keys -----------------------------------------------------------------------------

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!filteringKeys) return false
        val up = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> true
            KeyEvent.KEYCODE_VOLUME_DOWN -> false
            else -> return false
        }
        // Calls and ringing keep the system behaviour (silencing the ringer, in-call volume).
        if (volume.isInCall) return false
        if (event.action == KeyEvent.ACTION_DOWN) {
            volume.step(VolumeStream.MEDIA, up)
            showVolumeHud()
        }
        return true
    }

    fun showVolumeHud() {
        IslandEngine.showTransient(
            IslandActivity.Volume(volume.percent(VolumeStream.MEDIA), volume.isMuted(VolumeStream.MEDIA)),
            durationMs = 1600L,
        )
    }

    /** True when the island is on screen and can show the volume HUD instead of the system panel. */
    val canShowHud: Boolean get() = island?.isShowing == true && !landscape

    private fun onEdgeAction(action: EdgeAction) {
        when (action) {
            EdgeAction.VOLUME_UP, EdgeAction.VOLUME_DOWN -> {
                val up = action == EdgeAction.VOLUME_UP
                volume.step(VolumeStream.MEDIA, up, showSystemUi = !canShowHud)
                if (canShowHud) showVolumeHud()
                WidgetUpdater.updateVolume(this)
            }
            EdgeAction.LOCK -> SystemActions.lockScreen()
            EdgeAction.POWER -> SystemActions.powerMenu()
            EdgeAction.SCREENSHOT -> scope.launch {
                delay(450) // Let the panel animate away so it is not captured.
                SystemActions.screenshot()
            }
        }
    }

    // System events ---------------------------------------------------------------------------

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> onBattery(intent)
                AudioManager.RINGER_MODE_CHANGED_ACTION -> if (!isInitialStickyBroadcast) {
                    IslandEngine.showTransient(
                        IslandActivity.Ringer(intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL)),
                    )
                    WidgetUpdater.updateToggles(context)
                }
                VOLUME_CHANGED -> WidgetUpdater.updateVolume(context)
                Intent.ACTION_SCREEN_ON -> startTicker()
                Intent.ACTION_SCREEN_OFF -> {
                    tickerJob?.cancel()
                    IslandEngine.dismissTransient()
                }
            }
        }
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(VOLUME_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(systemReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(systemReceiver, filter)
        }
        receiversRegistered = true
    }

    private fun onBattery(intent: Intent) {
        val snap = BatterySnapshot.from(this, intent)
        val prev = lastBattery
        lastBattery = snap
        if (prev == null) return WidgetUpdater.updateBattery(this)

        if (snap.isCharging != prev.isCharging) {
            showCharging(snap)
            if (snap.isCharging) {
                // Charge current takes a moment to ramp up; re-sample to detect fast charging.
                scope.launch {
                    delay(1800)
                    val later = BatterySnapshot.from(this@IslandAccessibilityService)
                    if (later.isCharging && IslandEngine.current.value is IslandActivity.Charging) showCharging(later, 2200L)
                }
            }
        } else if (!snap.isCharging && snap.level < prev.level && snap.level in LOW_LEVELS) {
            IslandEngine.showTransient(IslandActivity.LowBattery(snap.level), 4000L)
        }
        if (snap.level != prev.level || snap.isCharging != prev.isCharging) WidgetUpdater.updateBattery(this)
    }

    private fun showCharging(s: BatterySnapshot, duration: Long = 3400L) {
        IslandEngine.showTransient(IslandActivity.Charging(s.level, s.isCharging, s.isFast, s.watts), duration)
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (SystemActions.torchOn == enabled) return
            SystemActions.torchOn = enabled
            WidgetUpdater.updateToggles(this@IslandAccessibilityService)
        }
    }

    private fun registerTorchCallback() {
        runCatching { systemService<CameraManager>().registerTorchCallback(torchCallback, null) }
    }

    /** Keeps the RAM/storage and charging widgets fresh while the screen is on. */
    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                WidgetUpdater.updateStats(this@IslandAccessibilityService)
                if (lastBattery?.isCharging == true) WidgetUpdater.updateCharging(this@IslandAccessibilityService)
                delay(60_000)
            }
        }
    }
}
