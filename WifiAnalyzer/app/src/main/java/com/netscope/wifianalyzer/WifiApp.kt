package com.netscope.wifianalyzer

import android.app.Application
import com.netscope.wifianalyzer.widget.WidgetUpdater
import com.netscope.wifianalyzer.work.IntruderWorker
import com.netscope.wifianalyzer.work.Notifications
import com.netscope.wifianalyzer.work.WidgetRefreshWorker

class WifiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        Notifications.createChannels(this)
        if (AppGraph.prefs.intruderEnabled.value) IntruderWorker.schedule(this)
        IntruderWorker.watchWifi(this)
        WidgetRefreshWorker.schedule(this)
        AppGraph.premium.addChangeListener { WidgetUpdater.updateAll(this) }
    }
}
