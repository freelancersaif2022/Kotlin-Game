package com.tvremote.universal

import android.app.Application
import com.tvremote.universal.ads.AdsManager
import com.tvremote.universal.billing.BillingManager
import com.tvremote.universal.billing.PremiumManager
import com.tvremote.universal.core.CertPins
import com.tvremote.universal.core.ClientIdentity
import com.tvremote.universal.data.SettingsStore
import com.tvremote.universal.data.TvStore
import com.tvremote.universal.tv.TvController
import com.tvremote.universal.tv.TvDiscovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
        // Pre-generate the RSA identity so the first pairing is instant.
        container.scope.launch(Dispatchers.Default) { ClientIdentity.get(this@App) }
        container.billing.start()
    }

    companion object {
        lateinit var instance: App
            private set
        val container: AppContainer get() = instance.container
    }
}

class AppContainer(val app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val settings = SettingsStore(app)
    val tvStore = TvStore(app)
    val certPins = CertPins(app)
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    val discovery by lazy { TvDiscovery(app, http) }
    val billing = BillingManager(app, scope)
    val premium = PremiumManager(settings, billing, scope)
    val ads = AdsManager(app, premium, scope)
}

/** The controller of the remote currently on screen (used by the phone's volume keys). */
object RemoteSession {
    @Volatile var active: TvController? = null
    @Volatile var onVolumeKey: ((up: Boolean) -> Unit)? = null
}
