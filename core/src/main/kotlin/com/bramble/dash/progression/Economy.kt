package com.bramble.dash.progression

import com.bramble.dash.config.Characters
import com.bramble.dash.config.Tuning
import com.bramble.dash.save.SaveData
import kotlin.random.Random

enum class Upgrade(val id: String, val title: String, val prices: IntArray, val iconRegion: String) {
    POWER_DURATION("power_duration", "Power-up Duration", intArrayOf(300, 700, 1300, 2200, 3600), "powerups/speed_boost/hud"),
    MAGNET_RANGE("magnet_range", "Magnet Range", intArrayOf(250, 600, 1100, 1900, 3000), "powerups/coin_magnet/hud"),
    SHIELD_START("shield_start", "Starting Shield", intArrayOf(800, 1800, 3500), "powerups/protective_shield/hud"),
    HEALTH("health", "Max Health", intArrayOf(2500, 6000), "powerups/health_recovery/hud"),
    SCORE_BONUS("score_bonus", "Score Bonus", intArrayOf(400, 900, 1600, 2600, 4000), "powerups/score_multiplier/hud"),
    ;
    val maxLevel get() = prices.size

    /** Human readable effect at a level (0 = not bought). */
    fun describe(level: Int): String = when (this) {
        POWER_DURATION -> "+${(level * Tuning.PU_DURATION_UPGRADE_STEP * 100).toInt()}% power-up time"
        MAGNET_RANGE -> "${(Tuning.MAGNET_BASE_RANGE + level * Tuning.MAGNET_RANGE_STEP).toInt()} px magnet radius"
        SHIELD_START -> "${shieldChance(level)}% chance to start with a shield"
        HEALTH -> "${Tuning.BASE_HEALTH + level} hearts at start"
        SCORE_BONUS -> "+${level * 5}% score"
    }

    companion object {
        fun shieldChance(level: Int) = intArrayOf(0, 35, 70, 100)[level.coerceIn(0, 3)]
    }
}

enum class PurchaseResult { OK, INSUFFICIENT_FUNDS, ALREADY_OWNED, MAXED, LOCKED }

/** Shop rules. All mutations go through [com.bramble.dash.save.SaveManager.transact] by the caller. */
object Economy {
    fun upgradePrice(d: SaveData, u: Upgrade): Int? = d.upgrade(u.id).let { if (it >= u.maxLevel) null else u.prices[it] }

    fun buyUpgrade(d: SaveData, u: Upgrade): PurchaseResult {
        val price = upgradePrice(d, u) ?: return PurchaseResult.MAXED
        if (d.coins < price) return PurchaseResult.INSUFFICIENT_FUNDS
        d.coins -= price
        d.upgrades[u.id] = d.upgrade(u.id) + 1
        return PurchaseResult.OK
    }

    fun buyCharacter(d: SaveData, id: String): PurchaseResult {
        val c = Characters.byId(id)
        if (c.id in d.unlockedCharacters) return PurchaseResult.ALREADY_OWNED
        if (d.coins < c.price) return PurchaseResult.INSUFFICIENT_FUNDS
        d.coins -= c.price
        d.unlockedCharacters += c.id
        d.selectedCharacter = c.id
        return PurchaseResult.OK
    }

    fun buyKey(d: SaveData): PurchaseResult {
        if (d.gems < Tuning.GEMS_PER_KEY) return PurchaseResult.INSUFFICIENT_FUNDS
        d.gems -= Tuning.GEMS_PER_KEY; d.keys += 1
        return PurchaseResult.OK
    }

    fun exchangeGem(d: SaveData): PurchaseResult {
        if (d.gems < 1) return PurchaseResult.INSUFFICIENT_FUNDS
        d.gems -= 1; d.coins += Tuning.COINS_PER_GEM_EXCHANGE
        return PurchaseResult.OK
    }

    /** Pay for a mystery box with gems or bonus tokens, then roll it. */
    fun buyMysteryBox(d: SaveData, withTokens: Boolean, rng: Random): Pair<PurchaseResult, Reward?> {
        if (withTokens) {
            if (d.tokens < Tuning.TOKENS_PER_BOX) return PurchaseResult.INSUFFICIENT_FUNDS to null
            d.tokens -= Tuning.TOKENS_PER_BOX
        } else {
            if (d.gems < Tuning.GEMS_PER_BOX) return PurchaseResult.INSUFFICIENT_FUNDS to null
            d.gems -= Tuning.GEMS_PER_BOX
        }
        val r = MysteryBox.roll(rng)
        r.grant(d)
        return PurchaseResult.OK to r
    }

    /** Open a stored treasure chest with a key. */
    fun openChest(d: SaveData, rng: Random): Pair<PurchaseResult, Reward?> {
        if (d.chests <= 0) return PurchaseResult.LOCKED to null
        if (d.keys <= 0) return PurchaseResult.INSUFFICIENT_FUNDS to null
        d.chests -= 1; d.keys -= 1
        d.stats.chestsOpened += 1
        val r = Reward(coins = 150 + rng.nextInt(0, 251), gems = 1 + rng.nextInt(0, 3))
        r.grant(d)
        return PurchaseResult.OK to r
    }
}

class Reward(val coins: Int = 0, val gems: Int = 0, val keys: Int = 0, val tokens: Int = 0) {
    fun grant(d: SaveData) {
        d.coins += coins; d.gems += gems; d.keys += keys; d.tokens += tokens
        d.stats.coinsEarned += coins
    }

    fun describe(): String = listOfNotNull(
        if (coins > 0) "$coins coins" else null,
        if (gems > 0) "$gems gem" + (if (gems > 1) "s" else "") else null,
        if (keys > 0) "$keys key" + (if (keys > 1) "s" else "") else null,
        if (tokens > 0) "$tokens bonus tokens" else null,
    ).joinToString(" + ").ifEmpty { "nothing" }
}

/**
 * Mystery box reward table (weights sum to 100, i.e. percentages):
 *  45  100-300 coins
 *  20  2-5 gems
 *  15  1 key
 *  10  3 bonus tokens
 *   7  800 coins (jackpot)
 *   3  15 gems (rare jackpot)
 */
object MysteryBox {
    class Entry(val weight: Int, val make: (Random) -> Reward)

    val table = listOf(
        Entry(45) { Reward(coins = 100 + it.nextInt(0, 201)) },
        Entry(20) { Reward(gems = 2 + it.nextInt(0, 4)) },
        Entry(15) { Reward(keys = 1) },
        Entry(10) { Reward(tokens = 3) },
        Entry(7) { Reward(coins = 800) },
        Entry(3) { Reward(gems = 15) },
    )

    fun roll(rng: Random): Reward {
        var pick = rng.nextInt(table.sumOf { it.weight })
        for (e in table) {
            if (pick < e.weight) return e.make(rng)
            pick -= e.weight
        }
        return table.last().make(rng)
    }
}
