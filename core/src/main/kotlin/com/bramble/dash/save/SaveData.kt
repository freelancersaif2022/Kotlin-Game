package com.bramble.dash.save

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import com.bramble.dash.config.Bosses
import com.bramble.dash.config.Characters
import com.bramble.dash.config.Worlds
import java.io.StringWriter

class MissionState(var id: String, var progress: Int = 0, var claimed: Boolean = false)

class LeaderboardEntry(val score: Int, val distance: Int, val character: String, val world: String, val day: Long)

class Settings {
    var musicVolume = 0.6f
    var sfxVolume = 0.8f
    var muted = false
    var vibration = true
    /** 0 low, 1 medium, 2 high */
    var quality = 2
    var leftHanded = false
    var swipeControls = true
}

class Stats {
    var runs = 0
    var totalMetres = 0L
    var coinsEarned = 0L
    var enemiesDefeated = 0
    var bossesDefeated = 0
    var powerupsUsed = 0
    var slidesUnder = 0
    var gemsCollected = 0
    var chestsOpened = 0
    var starsCollected = 0
    var revives = 0
    var playSeconds = 0L
}

/** Everything that persists between launches. Versioned, validated on load. */
class SaveData {
    var version = CURRENT_VERSION
    var coins = 0
    var gems = 0
    var keys = 0
    var tokens = 0
    var chests = 0
    val unlockedCharacters = linkedSetOf("max")
    val unlockedWorlds = linkedSetOf("jungle")
    var selectedCharacter = "max"
    var selectedWorld = "jungle"
    val upgrades = LinkedHashMap<String, Int>()
    var bestScore = 0
    var bestMetres = 0
    val bestMetresByWorld = LinkedHashMap<String, Int>()
    val worldsCleared = linkedSetOf<String>()      // worlds whose gate boss was defeated
    val bossesBeaten = linkedSetOf<String>()       // boss ids ever defeated
    val visitedWorlds = linkedSetOf<String>()
    val stats = Stats()
    val missions = ArrayList<MissionState>()
    var missionsCompleted = 0
    val achievements = LinkedHashMap<String, Int>() // 1 = unlocked, 2 = reward claimed
    var dailyLastClaimDay = -1L
    var dailyStreak = 0                             // index of the next reward (0..6)
    val settings = Settings()
    var tutorialDone = false
    val leaderboard = ArrayList<LeaderboardEntry>()

    fun upgrade(id: String) = upgrades[id] ?: 0

    fun toJson(): String {
        val sw = StringWriter()
        val w = JsonWriter(sw)
        w.setOutputType(JsonWriter.OutputType.json)
        w.`object`()
        w.set("version", version)
        w.set("coins", coins); w.set("gems", gems); w.set("keys", keys); w.set("tokens", tokens); w.set("chests", chests)
        w.name("unlockedCharacters").array(); unlockedCharacters.forEach { w.value(it) }; w.pop()
        w.name("unlockedWorlds").array(); unlockedWorlds.forEach { w.value(it) }; w.pop()
        w.set("selectedCharacter", selectedCharacter); w.set("selectedWorld", selectedWorld)
        w.name("upgrades").`object`(); upgrades.forEach { (k, v) -> w.set(k, v) }; w.pop()
        w.set("bestScore", bestScore); w.set("bestMetres", bestMetres)
        w.name("bestMetresByWorld").`object`(); bestMetresByWorld.forEach { (k, v) -> w.set(k, v) }; w.pop()
        w.name("worldsCleared").array(); worldsCleared.forEach { w.value(it) }; w.pop()
        w.name("bossesBeaten").array(); bossesBeaten.forEach { w.value(it) }; w.pop()
        w.name("visitedWorlds").array(); visitedWorlds.forEach { w.value(it) }; w.pop()
        w.name("stats").`object`()
        with(stats) {
            w.set("runs", runs); w.set("totalMetres", totalMetres); w.set("coinsEarned", coinsEarned)
            w.set("enemiesDefeated", enemiesDefeated); w.set("bossesDefeated", bossesDefeated); w.set("powerupsUsed", powerupsUsed)
            w.set("slidesUnder", slidesUnder); w.set("gemsCollected", gemsCollected); w.set("chestsOpened", chestsOpened)
            w.set("starsCollected", starsCollected); w.set("revives", revives); w.set("playSeconds", playSeconds)
        }
        w.pop()
        w.name("missions").array()
        missions.forEach { w.`object`(); w.set("id", it.id); w.set("progress", it.progress); w.set("claimed", it.claimed); w.pop() }
        w.pop()
        w.set("missionsCompleted", missionsCompleted)
        w.name("achievements").`object`(); achievements.forEach { (k, v) -> w.set(k, v) }; w.pop()
        w.set("dailyLastClaimDay", dailyLastClaimDay); w.set("dailyStreak", dailyStreak)
        w.name("settings").`object`()
        with(settings) {
            w.set("musicVolume", musicVolume); w.set("sfxVolume", sfxVolume); w.set("muted", muted); w.set("vibration", vibration)
            w.set("quality", quality); w.set("leftHanded", leftHanded); w.set("swipeControls", swipeControls)
        }
        w.pop()
        w.set("tutorialDone", tutorialDone)
        w.name("leaderboard").array()
        leaderboard.forEach {
            w.`object`(); w.set("score", it.score); w.set("distance", it.distance); w.set("character", it.character)
            w.set("world", it.world); w.set("day", it.day); w.pop()
        }
        w.pop()
        w.pop()
        w.close()
        return sw.toString()
    }

    /** Clamp and repair anything a corrupted or hand-edited save could contain. */
    fun validate() {
        coins = coins.coerceIn(0, MAX_CURRENCY); gems = gems.coerceIn(0, MAX_CURRENCY)
        keys = keys.coerceIn(0, 999); tokens = tokens.coerceIn(0, 9999); chests = chests.coerceIn(0, 999)
        val charIds = Characters.all.map { it.id }.toSet()
        val worldIds = Worlds.all.map { it.id }.toSet()
        val bossIds = Bosses.all.map { it.id }.toSet()
        unlockedCharacters.retainAll(charIds); unlockedCharacters.add("max")
        unlockedWorlds.retainAll(worldIds); unlockedWorlds.add("jungle")
        worldsCleared.retainAll(worldIds); visitedWorlds.retainAll(worldIds); bossesBeaten.retainAll(bossIds)
        if (selectedCharacter !in unlockedCharacters) selectedCharacter = "max"
        if (selectedWorld !in unlockedWorlds) selectedWorld = "jungle"
        val caps = com.bramble.dash.progression.Upgrade.values().associate { it.id to it.maxLevel }
        upgrades.keys.retainAll(caps.keys)
        for ((k, v) in upgrades.entries) upgrades[k] = v.coerceIn(0, caps.getValue(k))
        bestScore = bestScore.coerceAtLeast(0); bestMetres = bestMetres.coerceAtLeast(0)
        bestMetresByWorld.keys.retainAll(worldIds)
        dailyStreak = dailyStreak.coerceIn(0, 6)
        settings.musicVolume = settings.musicVolume.coerceIn(0f, 1f)
        settings.sfxVolume = settings.sfxVolume.coerceIn(0f, 1f)
        settings.quality = settings.quality.coerceIn(0, 2)
        val validMissions = com.bramble.dash.progression.Missions.pool.map { it.id }.toSet()
        missions.removeAll { it.id !in validMissions }
        val validAch = com.bramble.dash.progression.Achievements.all.map { it.id }.toSet()
        achievements.keys.retainAll(validAch)
        for ((k, v) in achievements.entries) achievements[k] = v.coerceIn(1, 2)
        leaderboard.removeAll { it.score < 0 || it.distance < 0 }
        leaderboard.sortByDescending { it.score }
        while (leaderboard.size > LEADERBOARD_SIZE) leaderboard.removeAt(leaderboard.size - 1)
        // keep progression coherent: a cleared world always unlocks the next one
        Worlds.all.forEachIndexed { i, w -> if (w.id in worldsCleared && i + 1 < Worlds.all.size) unlockedWorlds += Worlds.all[i + 1].id }
    }

    companion object {
        const val CURRENT_VERSION = 1
        const val MAX_CURRENCY = 99_999_999
        const val LEADERBOARD_SIZE = 10

        /** Returns null when the text is not a usable save (caller falls back to backup/defaults). */
        fun fromJson(text: String?): SaveData? {
            if (text.isNullOrBlank()) return null
            return try {
                val r = JsonReader().parse(text) ?: return null
                if (!r.isObject) return null
                val d = SaveData()
                d.version = r.getInt("version", 0)
                if (d.version <= 0 || d.version > CURRENT_VERSION) return null
                d.coins = r.getInt("coins", 0); d.gems = r.getInt("gems", 0); d.keys = r.getInt("keys", 0)
                d.tokens = r.getInt("tokens", 0); d.chests = r.getInt("chests", 0)
                strings(r, "unlockedCharacters", d.unlockedCharacters)
                strings(r, "unlockedWorlds", d.unlockedWorlds)
                d.selectedCharacter = r.getString("selectedCharacter", "max")
                d.selectedWorld = r.getString("selectedWorld", "jungle")
                ints(r, "upgrades", d.upgrades)
                d.bestScore = r.getInt("bestScore", 0); d.bestMetres = r.getInt("bestMetres", 0)
                ints(r, "bestMetresByWorld", d.bestMetresByWorld)
                strings(r, "worldsCleared", d.worldsCleared)
                strings(r, "bossesBeaten", d.bossesBeaten)
                strings(r, "visitedWorlds", d.visitedWorlds)
                r.get("stats")?.let { s ->
                    with(d.stats) {
                        runs = s.getInt("runs", 0); totalMetres = s.getLong("totalMetres", 0); coinsEarned = s.getLong("coinsEarned", 0)
                        enemiesDefeated = s.getInt("enemiesDefeated", 0); bossesDefeated = s.getInt("bossesDefeated", 0)
                        powerupsUsed = s.getInt("powerupsUsed", 0); slidesUnder = s.getInt("slidesUnder", 0)
                        gemsCollected = s.getInt("gemsCollected", 0); chestsOpened = s.getInt("chestsOpened", 0)
                        starsCollected = s.getInt("starsCollected", 0); revives = s.getInt("revives", 0); playSeconds = s.getLong("playSeconds", 0)
                    }
                }
                r.get("missions")?.let { arr ->
                    var m = arr.child
                    while (m != null) {
                        d.missions += MissionState(m.getString("id", ""), m.getInt("progress", 0).coerceAtLeast(0), m.getBoolean("claimed", false))
                        m = m.next
                    }
                }
                d.missionsCompleted = r.getInt("missionsCompleted", 0)
                ints(r, "achievements", d.achievements)
                d.dailyLastClaimDay = r.getLong("dailyLastClaimDay", -1L)
                d.dailyStreak = r.getInt("dailyStreak", 0)
                r.get("settings")?.let { s ->
                    with(d.settings) {
                        musicVolume = s.getFloat("musicVolume", 0.6f); sfxVolume = s.getFloat("sfxVolume", 0.8f)
                        muted = s.getBoolean("muted", false); vibration = s.getBoolean("vibration", true)
                        quality = s.getInt("quality", 2); leftHanded = s.getBoolean("leftHanded", false)
                        swipeControls = s.getBoolean("swipeControls", true)
                    }
                }
                d.tutorialDone = r.getBoolean("tutorialDone", false)
                r.get("leaderboard")?.let { arr ->
                    var e = arr.child
                    while (e != null) {
                        d.leaderboard += LeaderboardEntry(e.getInt("score", 0), e.getInt("distance", 0), e.getString("character", "max"),
                            e.getString("world", "jungle"), e.getLong("day", 0))
                        e = e.next
                    }
                }
                d.validate()
                d
            } catch (e: Exception) {
                null
            }
        }

        private fun strings(r: JsonValue, name: String, out: MutableSet<String>) {
            val arr = r.get(name) ?: return
            if (!arr.isArray) return
            out.clear()
            var c = arr.child
            while (c != null) { if (c.isString) out += c.asString(); c = c.next }
        }

        private fun ints(r: JsonValue, name: String, out: MutableMap<String, Int>) {
            val obj = r.get(name) ?: return
            if (!obj.isObject) return
            var c = obj.child
            while (c != null) { if (c.isNumber) out[c.name] = c.asInt(); c = c.next }
        }
    }
}
