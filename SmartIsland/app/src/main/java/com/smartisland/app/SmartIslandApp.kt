package com.smartisland.app

import android.app.Application
import android.content.Context
import com.smartisland.app.ads.AdsManager
import com.smartisland.app.billing.BillingGateway
import com.smartisland.app.billing.MockBillingGateway
import com.smartisland.app.data.PremiumRepository
import com.smartisland.app.data.SettingsRepository
import com.smartisland.app.island.IslandEngine
import com.smartisland.app.system.CrashLog
import com.smartisland.app.system.VolumeController
import com.smartisland.app.widget.WidgetUpdater
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Manual dependency container: a handful of singletons doesn't justify a DI framework. */
class AppContainer(app: Application) {
    val settings = SettingsRepository(app)
    val premium = PremiumRepository(app)
    val billing: BillingGateway = MockBillingGateway(premium)
    val ads = AdsManager(app)
    val volume = VolumeController(app)
}

class SmartIslandApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = MainScope()

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        container = AppContainer(this)
        IslandEngine.bindConfig(container.settings.settings, container.premium.state)

        // Premium or unlock changes flip widgets between locked and live content.
        appScope.launch {
            container.premium.state
                .map { it.isPremium to it.unlockedWidgets }
                .distinctUntilChanged()
                .drop(1)
                .collect { WidgetUpdater.updateAll(this@SmartIslandApp) }
        }
        // The island toggle widget mirrors the setting.
        appScope.launch {
            container.settings.settings
                .map { it.enabled }
                .distinctUntilChanged()
                .drop(1)
                .collect { WidgetUpdater.updateToggles(this@SmartIslandApp) }
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as SmartIslandApp).container
