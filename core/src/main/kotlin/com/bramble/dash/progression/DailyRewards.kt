package com.bramble.dash.progression

import com.bramble.dash.save.SaveData

/**
 * Seven-day local reward cycle keyed by the device's local calendar day (epoch day).
 * Limitation (documented in README): it trusts the device clock; moving the clock forward
 * grants the next day early, moving it backwards blocks claiming until the stored day passes.
 */
object DailyRewards {
    val rewards = listOf(
        Reward(coins = 100), Reward(coins = 150), Reward(gems = 2), Reward(coins = 300),
        Reward(keys = 1), Reward(gems = 4), Reward(coins = 600, tokens = 3),
    )

    enum class Status { AVAILABLE, CLAIMED_TODAY, CLOCK_BEHIND }

    fun status(d: SaveData, today: Long): Status = when {
        d.dailyLastClaimDay < 0 || today > d.dailyLastClaimDay -> Status.AVAILABLE
        today == d.dailyLastClaimDay -> Status.CLAIMED_TODAY
        else -> Status.CLOCK_BEHIND
    }

    /** Index of the reward that a claim today would give. Missing a day restarts the cycle. */
    fun nextIndex(d: SaveData, today: Long): Int =
        if (d.dailyLastClaimDay >= 0 && today > d.dailyLastClaimDay + 1) 0 else d.dailyStreak

    fun claim(d: SaveData, today: Long): Reward? {
        if (status(d, today) != Status.AVAILABLE) return null
        val i = nextIndex(d, today)
        val r = rewards[i]
        r.grant(d)
        d.dailyLastClaimDay = today
        d.dailyStreak = (i + 1) % rewards.size
        return r
    }
}
