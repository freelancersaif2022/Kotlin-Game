package com.flashshare.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.flashshare.app.ads.AdsManager
import com.flashshare.app.billing.BillingManager
import com.flashshare.app.data.CleanerRepository
import com.flashshare.app.data.HistoryStore
import com.flashshare.app.data.MediaRepository
import com.flashshare.app.data.Prefs
import com.flashshare.app.data.SelectionStore
import com.flashshare.app.net.WifiDirectController
import com.flashshare.app.transfer.TransferManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class FlashShareApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createChannels()
        container.billing.start()
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(VideoFrameDecoder.Factory()) }
        .crossfade(180)
        .respectCacheHeaders(false)
        .build()

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TRANSFER, getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW)
                .apply { setShowBadge(false) }
        )
    }

    companion object {
        const val CHANNEL_TRANSFER = "transfers"
    }
}

/** Manual dependency container – keeps the APK small and start-up fast (no DI framework). */
class AppContainer(val app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val prefs = Prefs(app, scope)
    val history = HistoryStore(app, scope)
    val media = MediaRepository(app)
    val selection = SelectionStore()
    val wifi = WifiDirectController(app)
    val transfers = TransferManager(app, prefs, history, wifi, scope)
    val billing = BillingManager(app, prefs, scope)
    val ads = AdsManager(app)
    val cleaner = CleanerRepository(app)
}

val Context.container: AppContainer get() = (applicationContext as FlashShareApp).container
