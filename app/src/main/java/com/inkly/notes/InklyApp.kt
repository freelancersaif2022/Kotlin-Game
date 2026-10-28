package com.inkly.notes

import android.app.Application
import android.content.Context
import com.inkly.notes.data.AppPrefs
import com.inkly.notes.data.NoteDatabase
import com.inkly.notes.data.NoteRepository
import com.inkly.notes.drive.BackupManager
import com.inkly.notes.drive.BackupScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class InklyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        val state = container.prefs.state.value
        if (state.driveConnected && state.autoBackup) BackupScheduler.schedule(this)
    }
}

class AppContainer(app: Application) {
    /** Outlives screens – used to finish saves when an editor closes. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val prefs = AppPrefs(app)
    val repo = NoteRepository(NoteDatabase.build(app))
    val backup = BackupManager(app, repo, prefs)
}

val Context.appContainer: AppContainer get() = (applicationContext as InklyApp).container
