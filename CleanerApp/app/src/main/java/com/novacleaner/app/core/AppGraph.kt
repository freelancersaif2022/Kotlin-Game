package com.novacleaner.app.core

import android.app.Application
import com.novacleaner.app.ads.AdsManager
import com.novacleaner.app.ads.AppOpenAdManager
import com.novacleaner.app.billing.BillingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Tiny manual service locator – keeps the app free of a DI framework (smaller APK, faster start). */
object AppGraph {
    lateinit var app: Application
        private set
    lateinit var prefs: Prefs
        private set
    lateinit var ads: AdsManager
        private set
    lateinit var appOpen: AppOpenAdManager
        private set
    lateinit var billing: BillingManager
        private set
    lateinit var paywall: PaywallManager
        private set

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _premium = MutableStateFlow(false)
    val premium: StateFlow<Boolean> = _premium.asStateFlow()

    fun init(application: Application) {
        app = application
        prefs = Prefs(application)
        ads = AdsManager(application) { _premium.value }
        appOpen = AppOpenAdManager(application, ads) { _premium.value }
        ads.onReady = { appOpen.load() }
        billing = BillingManager(application, scope) { owned -> setPremium(owned) }
        paywall = PaywallManager(prefs) { _premium.value }

        scope.launch {
            prefs.settings.collect { _premium.value = it.premium }
        }
        billing.connect()
    }

    private fun setPremium(owned: Boolean) {
        if (_premium.value == owned) return
        _premium.value = owned
        if (owned) ads.onPremiumActivated()
        scope.launch { prefs.setPremium(owned) }
    }
}
