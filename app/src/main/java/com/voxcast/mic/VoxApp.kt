package com.voxcast.mic

import android.app.Application
import com.voxcast.mic.ads.AdsManager
import com.voxcast.mic.data.PremiumManager
import com.voxcast.mic.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class VoxApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var settings: SettingsRepository
        private set
    lateinit var premium: PremiumManager
        private set
    lateinit var ads: AdsManager
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this, appScope)
        premium = PremiumManager(settings)
        ads = AdsManager(this, appScope, settings)
        ads.initialize()
    }
}
