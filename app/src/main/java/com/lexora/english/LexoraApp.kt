package com.lexora.english

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.lexora.english.data.AppRepository
import com.lexora.english.data.DailyQuizWorker
import com.lexora.english.data.Notifier
import com.lexora.english.util.Speaker

class LexoraApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        val repo = AppRepository.get(this)
        Notifier.createChannels(this)
        Speaker.rate = repo.current.settings.speechRate
        DailyQuizWorker.schedule(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
            .diskCache(null as DiskCache?)
            .crossfade(true)
            .allowRgb565(false)
            .respectCacheHeaders(false)
            .build()
}
