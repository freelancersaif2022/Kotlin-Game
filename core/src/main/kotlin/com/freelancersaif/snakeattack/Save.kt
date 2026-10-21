package com.freelancersaif.snakeattack

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

/** Persistent player progress and settings. */
object Save {
    private val prefs: Preferences by lazy { Gdx.app.getPreferences("snake_attack") }

    var level: Int
        get() = prefs.getInteger("level", 1)
        set(v) = prefs.putInteger("level", v).flush()

    var coins: Int
        get() = prefs.getInteger("coins", 50)
        set(v) = prefs.putInteger("coins", v).flush()

    var extraSlot: Boolean
        get() = prefs.getBoolean("extraSlot", false)
        set(v) = prefs.putBoolean("extraSlot", v).flush()

    var sound: Boolean
        get() = prefs.getBoolean("sound", true)
        set(v) = prefs.putBoolean("sound", v).flush()

    var music: Boolean
        get() = prefs.getBoolean("music", true)
        set(v) = prefs.putBoolean("music", v).flush()

    var vibration: Boolean
        get() = prefs.getBoolean("vibration", true)
        set(v) = prefs.putBoolean("vibration", v).flush()

    fun stars(level: Int) = prefs.getInteger("stars_$level", 0)

    fun setStars(level: Int, stars: Int) {
        if (stars > stars(level)) prefs.putInteger("stars_$level", stars).flush()
    }

    private var lastVibrate = 0L

    /** Rate-limited so rapid hits don't flood the vibrator service (which can cause frame hitches). */
    fun vibrate(ms: Int) {
        if (!vibration) return
        val now = com.badlogic.gdx.utils.TimeUtils.millis()
        if (now - lastVibrate < 90L) return
        lastVibrate = now
        Gdx.input.vibrate(ms)
    }
}
