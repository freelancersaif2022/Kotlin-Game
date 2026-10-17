package com.bramble.dash.progression

import com.bramble.dash.config.Bosses
import com.bramble.dash.config.Characters
import com.bramble.dash.config.Worlds
import com.bramble.dash.save.SaveData

class AchievementDef(val id: String, val title: String, val text: String, val reward: Reward, val icon: String, val check: (SaveData) -> Boolean)

object Achievements {
    val all: List<AchievementDef> = buildList {
        add(AchievementDef("first_run", "First Steps", "Finish your first run", Reward(coins = 100), "ui/icons/star") { it.stats.runs >= 1 })
        add(AchievementDef("km1", "Trailblazer", "Run 1,000 m in a single run", Reward(coins = 300), "ui/icons/speed") { it.bestMetres >= 1000 })
        add(AchievementDef("km3", "Marathoner", "Run 3,000 m in a single run", Reward(gems = 5), "ui/icons/speed") { it.bestMetres >= 3000 })
        add(AchievementDef("total20k", "Globetrotter", "Run 20,000 m in total", Reward(gems = 5), "ui/icons/speed") { it.stats.totalMetres >= 20000 })
        add(AchievementDef("coins5k", "Treasure Hunter", "Earn 5,000 coins in total", Reward(gems = 3), "ui/icons/silver") { it.stats.coinsEarned >= 5000 })
        add(AchievementDef("coins50k", "Tycoon", "Earn 50,000 coins in total", Reward(gems = 10), "ui/icons/gold") { it.stats.coinsEarned >= 50000 })
        add(AchievementDef("enemies50", "Stomper", "Defeat 50 enemies", Reward(coins = 500), "ui/icons/star") { it.stats.enemiesDefeated >= 50 })
        add(AchievementDef("slides50", "Limbo Master", "Slide under 50 hazards", Reward(coins = 500), "ui/icons/shield") { it.stats.slidesUnder >= 50 })
        add(AchievementDef("power50", "Power Surge", "Activate 50 power-ups", Reward(coins = 600), "ui/icons/jetpack") { it.stats.powerupsUsed >= 50 })
        for (b in Bosses.all) add(AchievementDef("boss_" + b.id, b.name.substringAfterLast(' ') + " Slayer", "Defeat the ${b.name}", Reward(coins = 500, gems = 3), "ui/icons/heart") { b.id in it.bossesBeaten })
        add(AchievementDef("all_worlds", "World Traveller", "Visit all eight worlds", Reward(gems = 15), "ui/icons/key") { it.visitedWorlds.size >= Worlds.all.size })
        add(AchievementDef("all_chars", "Full Squad", "Unlock all five runners", Reward(gems = 15), "ui/icons/star") { it.unlockedCharacters.size >= Characters.all.size })
        add(AchievementDef("upgrade_max", "Maxed Out", "Fully upgrade any upgrade", Reward(gems = 5), "ui/icons/magnet") { d -> Upgrade.values().any { d.upgrade(it.id) >= it.maxLevel } })
        add(AchievementDef("chests5", "Key Keeper", "Open 5 treasure chests", Reward(gems = 5), "ui/icons/key") { it.stats.chestsOpened >= 5 })
        add(AchievementDef("revive", "Second Wind", "Revive with an extra life", Reward(coins = 300), "ui/icons/heart") { it.stats.revives >= 1 })
    }

    /** Marks newly met achievements as unlocked (reward claimable once). Returns their ids. */
    fun evaluate(d: SaveData): List<String> {
        val newly = ArrayList<String>()
        for (a in all) if (a.id !in d.achievements && a.check(d)) { d.achievements[a.id] = 1; newly += a.id }
        return newly
    }

    fun claim(d: SaveData, id: String): Reward? {
        if (d.achievements[id] != 1) return null
        val r = all.first { it.id == id }.reward
        r.grant(d)
        d.achievements[id] = 2
        return r
    }
}
