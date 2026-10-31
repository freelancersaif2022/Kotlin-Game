package com.spectral.ghostdetector

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.spectral.ghostdetector.ads.AdsManager
import com.spectral.ghostdetector.audio.Haptics
import com.spectral.ghostdetector.audio.SoundManager
import com.spectral.ghostdetector.billing.BillingManager
import com.spectral.ghostdetector.data.GhostNames
import com.spectral.ghostdetector.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Application-wide service holder (simple manual DI). */
class GhostApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var prefs: Prefs
        private set
    lateinit var sounds: SoundManager
        private set
    lateinit var haptics: Haptics
        private set
    lateinit var ads: AdsManager
        private set
    lateinit var billing: BillingManager
        private set
    lateinit var premium: StateFlow<Boolean>
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        premium = prefs.premium.stateIn(appScope, SharingStarted.Eagerly, false)
        sounds = SoundManager(this)
        haptics = Haptics(this)
        ads = AdsManager(this, appScope) { premium.value }
        billing = BillingManager(this) { appScope.launch { prefs.setPremium(true) } }

        appScope.launch { prefs.sound.collect { sounds.enabled = it } }
        appScope.launch { prefs.vibration.collect { haptics.enabled = it } }
        appScope.launch(Dispatchers.IO) { GhostNames.load(this@GhostApp) }

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = sounds.onForeground()
            override fun onStop(owner: LifecycleOwner) = sounds.onBackground()
        })
    }
}
