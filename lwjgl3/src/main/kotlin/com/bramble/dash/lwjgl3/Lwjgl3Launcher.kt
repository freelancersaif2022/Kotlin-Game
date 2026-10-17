package com.bramble.dash.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.bramble.dash.BrambleDash
import com.bramble.dash.LaunchOptions
import com.bramble.dash.save.MemoryStore
import com.bramble.dash.save.PreferencesStore
import com.bramble.dash.save.SaveData
import com.bramble.dash.save.SaveManager

/**
 * Desktop launcher for testing. Keyboard: Space/Up = jump (hold for jetpack), Down = slide,
 * F = energy blast during boss fights, P/Esc = pause.
 * Options: --size 1600x720, --autoplay, --world <id>, --character <id>,
 *          --screenshots <dir> (tour every screen with a demo save, then exit).
 */
fun main(args: Array<String>) {
    val opts = LaunchOptions()
    var width = 1280
    var height = 720
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--screenshots" -> opts.screenshotDir = args[++i]
            "--autoplay" -> opts.autoPlay = true
            "--run-only" -> opts.tourRunOnly = true
            "--world" -> opts.startWorld = args[++i]
            "--character" -> opts.startCharacter = args[++i]
            "--size" -> args[++i].split("x").let { width = it[0].toInt(); height = it[1].toInt() }
        }
        i++
    }
    val storeFactory = if (opts.screenshotDir != null) {
        {
            // demo profile in memory only; the player's real save is never touched by the tour
            val store = MemoryStore()
            val d = SaveData()
            d.coins = 4200; d.gems = 14; d.keys = 1; d.chests = 2; d.tokens = 6
            d.tutorialDone = true
            d.validate()
            store.map[SaveManager.KEY] = d.toJson()
            store
        }
    } else {
        { PreferencesStore(SaveManager.PREFS_NAME) }
    }
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Bramble Dash")
        setWindowedMode(width, height)
        useVsync(true)
        setForegroundFPS(60)
        setWindowIcon("icons/icon_128.png", "icons/icon_64.png", "icons/icon_32.png")
    }
    Lwjgl3Application(BrambleDash(opts, storeFactory), config)
}
