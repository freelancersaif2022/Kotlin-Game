package com.twinspace.app

import android.app.Application
import com.twinspace.app.admin.SpaceSetup
import com.twinspace.app.core.IconCache
import com.twinspace.app.core.SpaceManager
import com.twinspace.app.data.SpacePrefs

class TwinSpaceApplication : Application() {
    lateinit var prefs: SpacePrefs
        private set
    lateinit var space: SpaceManager
        private set
    lateinit var icons: IconCache
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = SpacePrefs(this)
        icons = IconCache(this)
        space = SpaceManager(this)
        space.configureComponentsForThisSide()
        // Inside the space, make sure the link to the personal side is intact every time we start.
        if (space.isInsideSpace) SpaceSetup.finish(this, null)
    }
}
