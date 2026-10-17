package com.bramble.dash

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.bramble.dash.assets.GameAssets
import com.bramble.dash.audio.AudioService
import com.bramble.dash.save.PreferencesStore
import com.bramble.dash.save.SaveManager
import com.bramble.dash.save.SaveStore
import com.bramble.dash.screens.LoadingScreen
import com.bramble.dash.ui.UiKit

/** Optional platform hooks (desktop debug tools); Android uses the defaults. */
open class LaunchOptions {
    /** Desktop testing: visit every screen and write screenshots into this directory, then exit. */
    open var screenshotDir: String? = null
    /** Desktop testing: let the bot play the run. */
    open var autoPlay = false
    open var startWorld: String? = null
    open var startCharacter: String? = null
    /** Screenshot tour: skip the menu screens and only capture a run. */
    open var tourRunOnly = false
}

class BrambleDash(val options: LaunchOptions = LaunchOptions(), private val storeFactory: () -> SaveStore = { PreferencesStore(SaveManager.PREFS_NAME) }) : Game() {
    lateinit var batch: SpriteBatch
        private set
    lateinit var assets: GameAssets
        private set
    lateinit var save: SaveManager
        private set
    lateinit var audio: AudioService
        private set
    lateinit var ui: UiKit
        private set
    var ready = false
        private set

    /** Local calendar day used by the daily-reward cycle (trusts the device clock). */
    fun today(): Long = epochDayNow()

    companion object {
        /** Local calendar day since 1970-01-01 (java.time needs Android 8, this works on Android 5+). */
        fun epochDayNow(): Long {
            val c = java.util.Calendar.getInstance()
            val utc = java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("UTC"))
            utc.clear()
            utc.set(c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH))
            return utc.timeInMillis / 86_400_000L
        }

        fun formatEpochDay(day: Long): String {
            val utc = java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("UTC"))
            utc.timeInMillis = day * 86_400_000L
            return "%04d-%02d-%02d".format(utc.get(java.util.Calendar.YEAR), utc.get(java.util.Calendar.MONTH) + 1, utc.get(java.util.Calendar.DAY_OF_MONTH))
        }
    }

    override fun create() {
        Gdx.input.setCatchKey(com.badlogic.gdx.Input.Keys.BACK, true)
        batch = SpriteBatch()
        assets = GameAssets()
        assets.loadSpriteData()
        save = SaveManager(storeFactory())
        assets.queueCore()
        setScreen(LoadingScreen(this))
    }

    /** Called by the loading screen once fonts, UI and sounds are resident. */
    fun onCoreLoaded() {
        audio = AudioService({ assets.sound(it) }, { assets.newMusic(it) })
        applyAudioSettings()
        ui = UiKit(assets) { audio }
        ready = true
    }

    fun applyAudioSettings() {
        val s = save.data.settings
        audio.sfxVolume = s.sfxVolume
        audio.musicVolume = s.musicVolume
        audio.muted = s.muted
        audio.applyMusicVolume()
    }

    fun vibrate(ms: Int) {
        if (save.data.settings.vibration) runCatching { Gdx.input.vibrate(ms) }
    }

    /** Switch screens and dispose the previous one. */
    fun go(next: Screen) {
        val old = screen
        setScreen(next)
        old?.dispose()
    }

    private var tour: com.bramble.dash.debug.ScreenshotTour? = null

    override fun render() {
        val dt = Gdx.graphics.deltaTime.coerceAtMost(0.1f)
        if (ready) audio.update(dt)
        super.render()
        options.screenshotDir?.let { dir ->
            if (tour == null) tour = com.bramble.dash.debug.ScreenshotTour(this, java.io.File(dir))
            tour?.update(dt)
        }
    }

    override fun pause() {
        // Android: app backgrounded / interrupted. The current screen pauses gameplay first.
        super.pause()
        if (ready) { audio.suspend(); save.commit() }
    }

    override fun resume() {
        super.resume()
        if (ready) audio.resume()
    }

    override fun dispose() {
        screen?.dispose()
        if (ready) audio.dispose()
        assets.dispose()
        batch.dispose()
    }
}
