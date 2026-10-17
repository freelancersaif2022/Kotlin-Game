package com.bramble.dash

import com.bramble.dash.progression.Achievements
import com.bramble.dash.progression.DailyRewards
import com.bramble.dash.progression.Economy
import com.bramble.dash.progression.Missions
import com.bramble.dash.progression.MysteryBox
import com.bramble.dash.progression.Progression
import com.bramble.dash.progression.PurchaseResult
import com.bramble.dash.progression.RunReport
import com.bramble.dash.progression.Upgrade
import com.bramble.dash.save.MemoryStore
import com.bramble.dash.save.SaveData
import com.bramble.dash.save.SaveManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ProgressionTest {
    private fun report(metres: Int = 500, coins: Int = 120, gate: Boolean = false, bosses: List<String> = emptyList(), world: String = "jungle") =
        RunReport(world, "max", metres * 10, metres, coins, 2, 1, 1, 3, 2, 4, 6, 2, bosses, gate, 0, 60f)

    @Test
    fun saveRoundTripAndPersistenceAcrossRestarts() {
        val store = MemoryStore()
        val a = SaveManager(store)
        a.transact { it.coins = 1234; it.unlockedCharacters += "luna"; it.selectedCharacter = "luna"; it.upgrades["health"] = 1; it.settings.musicVolume = 0.2f }
        val b = SaveManager(store)   // simulates an app restart
        assertEquals(1234, b.data.coins)
        assertEquals("luna", b.data.selectedCharacter)
        assertEquals(1, b.data.upgrade("health"))
        assertEquals(0.2f, b.data.settings.musicVolume, 1e-4f)
    }

    @Test
    fun corruptedSaveFallsBackToBackupThenDefaults() {
        val store = MemoryStore()
        val m = SaveManager(store)
        m.transact { it.coins = 50 }
        m.transact { it.coins = 75 }        // previous good copy (50) becomes the backup
        store.map[SaveManager.KEY] = "{not json"
        val r = SaveManager(store)
        assertTrue(r.recoveredFromBackup)
        assertEquals(50, r.data.coins)
        store.map[SaveManager.BACKUP_KEY] = "garbage"
        val d = SaveManager(store)
        assertTrue(d.resetToDefaults)
        assertEquals(0, d.data.coins)
        assertTrue("max" in d.data.unlockedCharacters && "jungle" in d.data.unlockedWorlds)
    }

    @Test
    fun validationRepairsInvalidValues() {
        val json = SaveData().apply { coins = 10 }.toJson()
            .replace("\"coins\":10", "\"coins\":-5")
            .replace("\"selectedCharacter\":\"max\"", "\"selectedCharacter\":\"robo_x\"")
            .replace("\"unlockedCharacters\":[\"max\"]", "\"unlockedCharacters\":[\"max\",\"ghost\"]")
        val d = SaveData.fromJson(json)!!
        assertEquals(0, d.coins)
        assertEquals("max", d.selectedCharacter)    // not owned -> default
        assertFalse("ghost" in d.unlockedCharacters)
        assertNull("future versions are rejected", SaveData.fromJson(json.replace("\"version\":1", "\"version\":99")))
    }

    @Test
    fun purchasesAndUpgrades() {
        val d = SaveData()
        assertEquals(PurchaseResult.INSUFFICIENT_FUNDS, Economy.buyCharacter(d, "luna"))
        d.coins = 1600
        assertEquals(PurchaseResult.OK, Economy.buyCharacter(d, "luna"))
        assertEquals(100, d.coins)
        assertEquals(PurchaseResult.ALREADY_OWNED, Economy.buyCharacter(d, "luna"))
        d.coins = 1_000_000
        repeat(Upgrade.HEALTH.maxLevel) { assertEquals(PurchaseResult.OK, Economy.buyUpgrade(d, Upgrade.HEALTH)) }
        assertEquals(PurchaseResult.MAXED, Economy.buyUpgrade(d, Upgrade.HEALTH))
        assertEquals(2, d.upgrade("health"))
    }

    @Test
    fun mysteryBoxTableIsDocumentedAndBalanced() {
        assertEquals(100, MysteryBox.table.sumOf { it.weight })
        val rng = Random(1)
        var keys = 0
        repeat(20000) { if (MysteryBox.roll(rng).keys > 0) keys++ }
        assertEquals(0.15, keys / 20000.0, 0.01)
    }

    @Test
    fun dailyRewardsCycle() {
        val d = SaveData()
        val day = 20000L
        assertNotNull(DailyRewards.claim(d, day))
        assertNull("cannot claim twice the same day", DailyRewards.claim(d, day))
        assertEquals(DailyRewards.Status.CLAIMED_TODAY, DailyRewards.status(d, day))
        assertEquals(1, DailyRewards.nextIndex(d, day + 1))
        DailyRewards.claim(d, day + 1)
        assertEquals("missing a day restarts the cycle", 0, DailyRewards.nextIndex(d, day + 3))
        assertEquals("clock moved backwards", DailyRewards.Status.CLOCK_BEHIND, DailyRewards.status(d, day - 2))
        assertNull(DailyRewards.claim(d, day - 2))
        val coins = d.coins
        for (i in 0 until 7) DailyRewards.claim(d, day + 3 + i)
        assertEquals("cycle wraps after day 7", 0, d.dailyStreak)
        assertTrue(d.coins > coins)
    }

    @Test
    fun missionsAndAchievementsRewardOnce() {
        val d = SaveData()
        Missions.refill(d)
        assertEquals(3, d.missions.size)
        val done = Missions.applyRun(d, report(metres = 400, coins = 150))
        assertTrue("run300" in done && "coins100" in done)
        val before = d.coins
        assertNotNull(Missions.claim(d, "run300"))
        assertNull("second claim must fail", Missions.claim(d, "run300"))
        assertEquals(before + 100, d.coins)
        assertEquals(3, d.missions.size)
        d.stats.runs = 1
        assertEquals(listOf("first_run"), Achievements.evaluate(d).filter { it == "first_run" })
        assertTrue(Achievements.evaluate(d).isEmpty())
        val c = d.coins
        assertNotNull(Achievements.claim(d, "first_run"))
        assertNull(Achievements.claim(d, "first_run"))
        assertEquals(c + 100, d.coins)
    }

    @Test
    fun runResultsUnlockNextWorldAndLeaderboard() {
        val d = SaveData()
        val o = Progression.applyRun(d, report(metres = 900, gate = true, bosses = listOf("jungle_guardian")), 20000)
        assertEquals("forest", o.worldUnlocked)
        assertTrue("forest" in d.unlockedWorlds && "jungle" in d.worldsCleared)
        assertEquals(1, d.leaderboard.size)
        assertTrue(o.newBestScore && o.newBestMetres)
        for (i in 0 until 15) Progression.applyRun(d, report(metres = 100 + i), 20000)
        assertEquals(SaveData.LEADERBOARD_SIZE, d.leaderboard.size)
        assertTrue(d.leaderboard.zipWithNext().all { (a, b) -> a.score >= b.score })
    }
}
