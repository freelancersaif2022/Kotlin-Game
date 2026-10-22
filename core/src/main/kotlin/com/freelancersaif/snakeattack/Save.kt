package com.freelancersaif.snakeattack

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.utils.TimeUtils

enum class Booster(val key: String, val icon: String, val title: String) {
    BOMB("bomb", "booster_bomb", "Bomb"),
    FREEZE("freeze", "booster_freeze", "Freeze"),
    RAINBOW("rainbow", "booster_rainbow", "Rainbow"),
    SHUFFLE("shuffle", "booster_shuffle", "Shuffle"),
}

/** Persistent player progress, economy and settings. */
object Save {
    private val prefs: Preferences by lazy { Gdx.app.getPreferences("snake_attack") }


    var level: Int
        get() = prefs.getInteger("level", 1)
        set(v) = prefs.putInteger("level", v).flush()

    var coins: Int
        get() = prefs.getInteger("coins", 200)
        set(v) = prefs.putInteger("coins", v.coerceAtLeast(0)).flush()

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
        if (stars > stars(level)) {
            prefs.putInteger("totalStars", totalStars + stars - stars(level))
            prefs.putInteger("stars_$level", stars).flush()
        }
    }

    val totalStars get() = prefs.getInteger("totalStars", 0)

    // --------------------------------------------------------------- boosters

    fun boosters(b: Booster) = prefs.getInteger("boost_${b.key}", 2)

    fun addBooster(b: Booster, n: Int) = prefs.putInteger("boost_${b.key}", (boosters(b) + n).coerceAtLeast(0)).flush()

    // ------------------------------------------------------------ daily / spin

    val today get() = (TimeUtils.millis() / 86_400_000L).toInt()

    /** Index (0..6) of the reward that can be claimed today, or -1 when already claimed. */
    val dailyIndex: Int
        get() {
            val last = prefs.getInteger("dailyDay", -10)
            if (last == today) return -1
            val streak = prefs.getInteger("dailyStreak", 0)
            return if (last == today - 1) streak % 7 else 0
        }

    /** Day number (0..6) last claimed, used to show claimed cards. */
    val dailyClaimedCount: Int
        get() {
            val last = prefs.getInteger("dailyDay", -10)
            val streak = prefs.getInteger("dailyStreak", 0)
            return when {
                last == today -> (streak - 1) % 7 + 1
                last == today - 1 -> streak % 7
                else -> 0
            }
        }

    fun claimDaily() {
        val idx = dailyIndex
        if (idx < 0) return
        prefs.putInteger("dailyStreak", idx + 1).putInteger("dailyDay", today).flush()
    }

    val freeSpinReady get() = prefs.getInteger("spinDay", -1) != today

    fun useFreeSpin() = prefs.putInteger("spinDay", today).flush()

    // ------------------------------------------------------------------- shop

    fun owns(item: String) = item == "body_default" || item == "skin_auto" || item == "hat_none" ||
        prefs.getBoolean("own_$item", false)

    fun buy(item: String) {
        prefs.putBoolean("own_$item", true).putInteger("purchases", purchases + 1).flush()
    }

    val purchases get() = prefs.getInteger("purchases", 0)

    var outfit: String
        get() = prefs.getString("outfit", "body_default")
        set(v) = prefs.putString("outfit", v).flush()

    /** Equipped hat region name, or "" for none. */
    var hat: String
        get() = prefs.getString("hat", "")
        set(v) = prefs.putString("hat", v).flush()

    var dragonSkin: String
        get() = prefs.getString("skin", "skin_auto")
        set(v) = prefs.putString("skin", v).flush()

    // ------------------------------------------------- stats / achievements

    fun stat(name: String) = prefs.getInteger("stat_$name", 0)

    fun addStat(name: String, n: Int = 1) = prefs.putInteger("stat_$name", stat(name) + n).flush()

    fun maxStat(name: String, v: Int) {
        if (v > stat(name)) prefs.putInteger("stat_$name", v).flush()
    }

    fun achievementClaimed(id: String) = prefs.getBoolean("ach_$id", false)

    fun claimAchievement(id: String) = prefs.putBoolean("ach_$id", true).flush()

    fun achievementNotified(id: String) = prefs.getBoolean("achn_$id", false)

    fun setAchievementNotified(id: String) = prefs.putBoolean("achn_$id", true).flush()

    fun storySeen(i: Int) = prefs.getBoolean("story_$i", false)

    fun setStorySeen(i: Int) = prefs.putBoolean("story_$i", true).flush()

    // ------------------------------------------------------------- vibration

    private var lastVibrate = 0L

    /** Rate-limited so rapid hits don't flood the vibrator service (which can cause frame hitches). */
    fun vibrate(ms: Int) {
        if (!vibration) return
        val now = TimeUtils.millis()
        if (now - lastVibrate < 90L) return
        lastVibrate = now
        Gdx.input.vibrate(ms)
    }
}
