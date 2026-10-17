package com.bramble.dash

import com.bramble.dash.config.Characters
import com.bramble.dash.config.Tuning
import com.bramble.dash.config.Worlds
import com.bramble.dash.game.AutoPilot
import com.bramble.dash.game.GameWorld
import com.bramble.dash.game.RunState
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plays every world with a reactive bot. A bot that only reacts to hazards in front of it can
 * survive only if every generated combination leaves a dodge window, so deaths here point at
 * unfair patterns (or a too-optimistic bot) with a reproducible seed.
 */
class FairnessTest {
    private fun play(w: GameWorld, metres: Float, maxSeconds: Float = 900f): String? {
        w.autoPilot = AutoPilot()
        w.start()
        var t = 0f
        while (t < maxSeconds) {
            w.step(Tuning.STEP)
            t += Tuning.STEP
            if (w.state == RunState.VICTORY) w.continueAfterBoss()
            if (w.hitsTaken > 0 || w.state == RunState.DYING) {
                return "hit by ${w.lastHitBy} player=${w.player.box} state=${w.state}"
            }
            if (w.metres >= metres) return null
        }
        return "timeout at ${w.metres.toInt()} m"
    }

    @Test
    fun botSurvivesAllWorlds() {
        val failures = ArrayList<String>()
        for (world in Worlds.all) for (seed in 1L..4L) {
            val w = TestSupport.world(world, seed * 7919 + world.id.hashCode())
            play(w, 3200f)?.let { failures += "${world.id} seed=${w.setup.seed}: $it" }
            println("${world.id} seed=${w.setup.seed}: ${w.metres.toInt()} m, bosses=${w.bossEncounters}, score=${w.score.toInt()}, coins=${w.coins}, jumps=${w.autoPilot!!.jumps}, slides=${w.autoPilot!!.slides}, enemies=${w.enemiesDefeated}")
            val problems = w.generator.validate(w.baseSpeed)
            if (problems.isNotEmpty()) failures += "${world.id}: ${problems.first()}"
        }
        println("fairness failures: ${failures.size}\n" + failures.joinToString("\n"))
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun everyCharacterCanFinishFirstBoss() {
        for (c in Characters.all) {
            val w = TestSupport.world(Worlds.all[0], 42L, c)
            val r = play(w, Tuning.FIRST_BOSS_METRES + 500f)
            assertTrue("${c.id}: $r", r == null)
            assertTrue("${c.id} should have beaten the gate boss", w.bossEncounters >= 1)
        }
    }
}
