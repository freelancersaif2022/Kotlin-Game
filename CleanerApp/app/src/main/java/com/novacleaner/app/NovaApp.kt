package com.novacleaner.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.novacleaner.app.core.AppGraph
import com.novacleaner.app.service.Notifications
import com.novacleaner.app.worker.ReminderWorker
import kotlinx.coroutines.launch

class NovaApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        Notifications.createChannels(this)
        AppGraph.scope.launch {
            AppGraph.prefs.incrementLaunchCount()
            val settings = AppGraph.prefs.snapshot()
            ReminderWorker.schedule(this@NovaApp, settings.remindersEnabled)
        }
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(VideoFrameDecoder.Factory()) }
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.2).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("thumbs")).maxSizeBytes(40L * 1024 * 1024).build() }
        .crossfade(true)
        .respectCacheHeaders(false)
        .build()
}
