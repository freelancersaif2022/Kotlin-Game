package com.zyropay.app

import android.app.Application
import com.zyropay.app.data.AppRepository

class ZyroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppRepository.init(this)
    }
}
