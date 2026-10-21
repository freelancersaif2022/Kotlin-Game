package com.netscope.wifianalyzer

import android.app.Application
import com.netscope.wifianalyzer.ads.AdsManager
import com.netscope.wifianalyzer.billing.BillingManager
import com.netscope.wifianalyzer.data.AppPrefs
import com.netscope.wifianalyzer.data.HeatmapRepository
import com.netscope.wifianalyzer.data.HistoryRepository
import com.netscope.wifianalyzer.data.KnownDevicesRepository
import com.netscope.wifianalyzer.data.PremiumManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Tiny service locator; everything here lives as long as the process. */
object AppGraph {
    lateinit var app: Application
        private set
    lateinit var prefs: AppPrefs
        private set
    lateinit var premium: PremiumManager
        private set
    lateinit var billing: BillingManager
        private set
    lateinit var ads: AdsManager
        private set
    lateinit var history: HistoryRepository
        private set
    lateinit var knownDevices: KnownDevicesRepository
        private set
    lateinit var heatmap: HeatmapRepository
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun init(application: Application) {
        app = application
        prefs = AppPrefs(application)
        premium = PremiumManager(prefs, appScope)
        history = HistoryRepository(application)
        knownDevices = KnownDevicesRepository(application)
        heatmap = HeatmapRepository(application)
        billing = BillingManager(application, premium, appScope)
        ads = AdsManager(application, premium, prefs)
    }
}
