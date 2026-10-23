package com.saif.pymaster

import android.app.Application
import com.saif.pymaster.data.Content
import com.saif.pymaster.data.Repository
import com.saif.pymaster.notify.DailyQuizWorker
import com.saif.pymaster.notify.Notifier
import com.saif.pymaster.python.PythonRunner

class PyMasterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Repository.get(this)
        Content.init(this)
        Notifier.createChannels(this)
        DailyQuizWorker.schedule(this)
        PythonRunner.warmUp(this)
    }
}
