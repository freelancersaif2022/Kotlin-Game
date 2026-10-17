package com.bramble.dash.progression

import com.bramble.dash.config.Worlds
import com.bramble.dash.save.LeaderboardEntry
import com.bramble.dash.save.SaveData

/** Immutable summary produced once by the simulation when a run ends. */
class RunReport(
    val worldId: String,
    val characterId: String,
    val score: Int,
    val metres: Int,
    val coins: Int,
    val gems: Int,
    val keys: Int,
    val chests: Int,
    val tokens: Int,
    val stars: Int,
    val enemiesDefeated: Int,
    val slidesUnder: Int,
    val powerupsUsed: Int,
    val bossesDefeated: List<String>,
    val gateBossDefeated: Boolean,
    val revives: Int,
    val seconds: Float,
)

class RunOutcome(
    val newBestScore: Boolean,
    val newBestMetres: Boolean,
    val missionsCompleted: List<String>,
    val achievementsUnlocked: List<String>,
    val worldUnlocked: String?,
)

object Progression {
    /** Apply a finished run to the save. The caller guarantees this is invoked once per run. */
    fun applyRun(d: SaveData, r: RunReport, today: Long): RunOutcome {
        d.coins += r.coins; d.gems += r.gems; d.keys += r.keys; d.chests += r.chests; d.tokens += r.tokens
        with(d.stats) {
            runs += 1; totalMetres += r.metres; coinsEarned += r.coins; enemiesDefeated += r.enemiesDefeated
            bossesDefeated += r.bossesDefeated.size; powerupsUsed += r.powerupsUsed; slidesUnder += r.slidesUnder
            gemsCollected += r.gems; starsCollected += r.stars; revives += r.revives; playSeconds += r.seconds.toLong()
        }
        val newBestScore = r.score > d.bestScore
        val newBestMetres = r.metres > d.bestMetres
        if (newBestScore) d.bestScore = r.score
        if (newBestMetres) d.bestMetres = r.metres
        if (r.metres > (d.bestMetresByWorld[r.worldId] ?: 0)) d.bestMetresByWorld[r.worldId] = r.metres
        d.visitedWorlds += r.worldId
        d.bossesBeaten += r.bossesDefeated
        var unlocked: String? = null
        if (r.gateBossDefeated) {
            d.worldsCleared += r.worldId
            val i = Worlds.indexOf(r.worldId)
            if (i + 1 < Worlds.all.size) {
                val next = Worlds.all[i + 1].id
                if (d.unlockedWorlds.add(next)) unlocked = next
            }
        }
        if (r.score > 0) {
            d.leaderboard += LeaderboardEntry(r.score, r.metres, r.characterId, r.worldId, today)
            d.leaderboard.sortByDescending { it.score }
            while (d.leaderboard.size > SaveData.LEADERBOARD_SIZE) d.leaderboard.removeAt(d.leaderboard.size - 1)
        }
        val missions = Missions.applyRun(d, r)
        val ach = Achievements.evaluate(d)
        return RunOutcome(newBestScore, newBestMetres, missions, ach, unlocked)
    }
}
