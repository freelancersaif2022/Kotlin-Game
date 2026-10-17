package com.bramble.dash.progression

import com.bramble.dash.config.Bosses
import com.bramble.dash.config.Worlds
import com.bramble.dash.save.MissionState
import com.bramble.dash.save.SaveData

enum class MissionType(val singleRun: Boolean) {
    RUN_METRES(true), COINS_IN_RUN(true), SCORE_IN_RUN(true),
    COINS_TOTAL(false), SLIDES_UNDER(false), ENEMIES(false), POWERUPS(false), GEMS(false), STARS(false),
    BOSS(false), VISIT(false), CHESTS(false),
}

class MissionDef(val id: String, val type: MissionType, val target: Int, val reward: Reward, val text: String, val param: String = "")

object Missions {
    const val ACTIVE = 3

    val pool: List<MissionDef> = buildList {
        add(MissionDef("run300", MissionType.RUN_METRES, 300, Reward(coins = 100), "Run 300 m in one run"))
        add(MissionDef("coins100", MissionType.COINS_TOTAL, 100, Reward(coins = 80), "Collect 100 coins"))
        add(MissionDef("slide5", MissionType.SLIDES_UNDER, 5, Reward(coins = 120), "Slide under 5 hazards"))
        add(MissionDef("enemy5", MissionType.ENEMIES, 5, Reward(coins = 150), "Defeat 5 enemies"))
        add(MissionDef("power3", MissionType.POWERUPS, 3, Reward(coins = 150), "Activate 3 power-ups"))
        add(MissionDef("boss_jungle_guardian", MissionType.BOSS, 1, Reward(coins = 300, gems = 2), "Defeat the Giant Jungle Guardian", "jungle_guardian"))
        add(MissionDef("run800", MissionType.RUN_METRES, 800, Reward(coins = 250), "Run 800 m in one run"))
        add(MissionDef("visit_forest", MissionType.VISIT, 1, Reward(coins = 200), "Visit the Sunny Green Forest", "forest"))
        add(MissionDef("coinsrun150", MissionType.COINS_IN_RUN, 150, Reward(coins = 200), "Collect 150 coins in one run"))
        add(MissionDef("gems5", MissionType.GEMS, 5, Reward(coins = 250), "Collect 5 gems"))
        add(MissionDef("stars5", MissionType.STARS, 5, Reward(coins = 200), "Collect 5 stars"))
        add(MissionDef("slide25", MissionType.SLIDES_UNDER, 25, Reward(coins = 300), "Slide under 25 hazards"))
        add(MissionDef("boss_scorpion_king", MissionType.BOSS, 1, Reward(coins = 400, gems = 3), "Defeat the Desert Scorpion King", "scorpion_king"))
        add(MissionDef("visit_desert", MissionType.VISIT, 1, Reward(coins = 250), "Visit the Hot Desert", "desert"))
        add(MissionDef("enemy25", MissionType.ENEMIES, 25, Reward(coins = 400), "Defeat 25 enemies"))
        add(MissionDef("score10k", MissionType.SCORE_IN_RUN, 10000, Reward(coins = 400), "Score 10,000 in one run"))
        add(MissionDef("chest2", MissionType.CHESTS, 2, Reward(gems = 3), "Open 2 treasure chests"))
        add(MissionDef("coins1000", MissionType.COINS_TOTAL, 1000, Reward(coins = 500), "Collect 1,000 coins"))
        add(MissionDef("boss_ice_titan", MissionType.BOSS, 1, Reward(coins = 500, gems = 4), "Defeat the Ice Monster Titan", "ice_titan"))
        add(MissionDef("visit_snow", MissionType.VISIT, 1, Reward(coins = 300), "Visit the Snowy Mountain", "snow"))
        add(MissionDef("run1500", MissionType.RUN_METRES, 1500, Reward(coins = 500, gems = 2), "Run 1,500 m in one run"))
        add(MissionDef("power20", MissionType.POWERUPS, 20, Reward(coins = 500), "Activate 20 power-ups"))
        add(MissionDef("visit_haunted", MissionType.VISIT, 1, Reward(coins = 350), "Visit the Dark Haunted Forest", "haunted"))
        add(MissionDef("boss_shadow_dragon", MissionType.BOSS, 1, Reward(coins = 600, gems = 5), "Defeat the Shadow Dragon", "shadow_dragon"))
        add(MissionDef("visit_city", MissionType.VISIT, 1, Reward(coins = 400), "Visit the Futuristic City", "city"))
        add(MissionDef("boss_robot_destroyer", MissionType.BOSS, 1, Reward(coins = 700, gems = 5), "Defeat the Mechanical Robot Destroyer", "robot_destroyer"))
        add(MissionDef("enemy75", MissionType.ENEMIES, 75, Reward(coins = 800), "Defeat 75 enemies"))
        add(MissionDef("visit_volcano", MissionType.VISIT, 1, Reward(coins = 450), "Visit the Volcanic Lava World", "volcano"))
        add(MissionDef("coinsrun500", MissionType.COINS_IN_RUN, 500, Reward(coins = 800), "Collect 500 coins in one run"))
        add(MissionDef("visit_fantasy", MissionType.VISIT, 1, Reward(coins = 500, gems = 3), "Visit the Magical Fantasy Kingdom", "fantasy"))
        add(MissionDef("run3000", MissionType.RUN_METRES, 3000, Reward(coins = 1000, gems = 5), "Run 3,000 m in one run"))
        add(MissionDef("slide100", MissionType.SLIDES_UNDER, 100, Reward(coins = 1000), "Slide under 100 hazards"))
        add(MissionDef("score50k", MissionType.SCORE_IN_RUN, 50000, Reward(coins = 1500, gems = 5), "Score 50,000 in one run"))
    }

    private val map = pool.associateBy { it.id }
    fun def(id: String) = map.getValue(id)

    init {
        // every boss and world has a reachable mission
        check(Bosses.all.all { b -> pool.any { it.type == MissionType.BOSS && it.param == b.id } })
        check(Worlds.all.drop(1).all { w -> pool.any { it.type == MissionType.VISIT && it.param == w.id } })
    }

    fun refill(d: SaveData) {
        var next = d.missionsCompleted + d.missions.size
        while (d.missions.size < ACTIVE && next < pool.size) {
            val id = pool[next].id
            if (d.missions.none { it.id == id }) d.missions += MissionState(id)
            next++
        }
    }

    fun isComplete(m: MissionState) = m.progress >= def(m.id).target

    /** Grants the reward exactly once and replaces the mission. */
    fun claim(d: SaveData, id: String): Reward? {
        val m = d.missions.firstOrNull { it.id == id } ?: return null
        if (m.claimed || !isComplete(m)) return null
        m.claimed = true
        val r = def(id).reward
        r.grant(d)
        d.missions.remove(m)
        d.missionsCompleted += 1
        refill(d)
        return r
    }

    /** Applies run results. Returns ids of missions that became complete during this update. */
    fun applyRun(d: SaveData, run: RunReport): List<String> {
        refill(d)
        val newly = ArrayList<String>()
        for (m in d.missions) {
            val def = def(m.id)
            val was = isComplete(m)
            val value = when (def.type) {
                MissionType.RUN_METRES -> run.metres
                MissionType.COINS_IN_RUN -> run.coins
                MissionType.SCORE_IN_RUN -> run.score
                MissionType.COINS_TOTAL -> run.coins
                MissionType.SLIDES_UNDER -> run.slidesUnder
                MissionType.ENEMIES -> run.enemiesDefeated
                MissionType.POWERUPS -> run.powerupsUsed
                MissionType.GEMS -> run.gems
                MissionType.STARS -> run.stars
                MissionType.BOSS -> if (def.param in run.bossesDefeated) 1 else 0
                MissionType.VISIT -> if (run.worldId == def.param) 1 else 0
                MissionType.CHESTS -> 0
            }
            m.progress = if (def.type.singleRun || def.type == MissionType.BOSS || def.type == MissionType.VISIT) maxOf(m.progress, value)
            else m.progress + value
            m.progress = m.progress.coerceAtMost(def.target)
            if (!was && isComplete(m)) newly += m.id
        }
        return newly
    }

    /** Chest openings happen outside runs. */
    fun onChestOpened(d: SaveData): List<String> {
        val newly = ArrayList<String>()
        for (m in d.missions) {
            val def = def(m.id)
            if (def.type == MissionType.CHESTS && !isComplete(m)) {
                m.progress = (m.progress + 1).coerceAtMost(def.target)
                if (isComplete(m)) newly += m.id
            }
        }
        return newly
    }
}
