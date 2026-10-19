package com.eyescroll.app

import android.app.Application
import com.eyescroll.app.data.SettingsStore

class EyeScrollApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsStore.get(this)
    }
}
