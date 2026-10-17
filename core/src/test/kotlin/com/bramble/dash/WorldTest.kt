package com.bramble.dash

import com.bramble.dash.config.PowerUpType
import com.bramble.dash.config.Tuning
import com.bramble.dash.config.Worlds
import com.bramble.dash.game.Action
import com.bramble.dash.game.AutoPilot
import com.bramble.dash.game.GameWorld
import com.bramble.dash.game.RunState
import com.bramble.dash.game.WorldListener
import com.bramble.dash.game.each
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldTest {
    private fun running(seed: Long = 5L): GameWorld = TestSupport.world(Worlds.all[0], seed).also { it.start() }
    private fun advance(w: GameWorld, seconds: Float) { var t = 0f; while (t < seconds) { w.step(Tuning.STEP); t += Tuning.STEP } }

    @Test
    fun pauseFreezesSimulationAndResumeDoesNotJump() {
        val w = running()
        w.invincibleCheat = true
        advance(w, 2f)
        val x = w.player.x; val score = w.score; val time = w.runTime
        w.pause()
        repeat(300) { w.update(1f / 60f) }          // 5 s of frames while paused
        assertEquals(RunState.PAUSED, w.state)
        assertEquals(x, w.player.x, 0f); assertEquals(score, w.score, 0f); assertEquals(time, w.runTime, 0f)
        w.resume()
        w.update(0.1f)                               // a long first frame is clamped and bounded
        assertTrue(w.player.x - x < w.speed * 0.11f)
    }

    @Test
    fun slidingShrinksTheColliderAndJumpingLeavesTheGround() {
        val w = running()
        w.invincibleCheat = true
        advance(w, 0.5f)
        val standH = w.player.box.height
        w.pressSlide(); w.step(Tuning.STEP)
        assertTrue(w.player.sliding)
        assertTrue(w.player.box.height < standH * 0.7f)
        assertTrue(w.player.box.y + w.player.box.height < Tuning.GROUND_Y + GameWorld.HIGH_LANE_BOTTOM)
        w.pressJump(); w.step(Tuning.STEP)
        assertFalse(w.player.grounded)
        // double jump works once in the air
        advance(w, 0.2f)
        val vyBefore = w.player.vy
        w.pressJump(); w.step(Tuning.STEP)
        assertTrue(w.player.vy > vyBefore)
    }

    @Test
    fun coyoteTimeAndJumpBuffer() {
        val w = running()
        w.invincibleCheat = true
        advance(w, 0.5f)
        // buffered: press shortly before landing
        w.pressJump(); w.releaseJumpLater = true; w.step(Tuning.STEP)
        while (w.player.vy > -200f) w.step(Tuning.STEP)
        while (w.player.y > Tuning.GROUND_Y + 25f) w.step(Tuning.STEP)
        w.pressJump(); w.step(Tuning.STEP)  // still airborne: consumed as the air jump
        assertTrue(w.player.vy > 0f)
    }

    @Test
    fun shieldAbsorbsExactlyOneHitAndExtraLifeOffersARevive() {
        val events = ArrayList<RunState>()
        val w = TestSupport.world(Worlds.all[0], 9L, listener = object : WorldListener { override fun stateChanged(from: RunState, to: RunState) { events += to } })
        w.start()
        w.activatePowerUp(PowerUpType.PROTECTIVE_SHIELD)
        w.activatePowerUp(PowerUpType.EXTRA_LIFE)
        assertTrue(w.powerUps.shield && w.powerUps.extraLife)
        w.debugKill(keepExtraLife = true)
        advance(w, 2f)
        assertEquals(RunState.REVIVE_OFFER, w.state)
        assertTrue(w.revive())
        assertFalse(w.powerUps.extraLife)
        assertTrue(w.player.health >= 1)
        assertTrue(w.player.invulnerable > 0f)
        assertEquals(RunState.RUNNING, w.state)
        assertFalse("only one revive", w.revive())
    }

    @Test
    fun timedPowerUpsExpireAndJetpackRestoresGravity() {
        val w = running()
        w.invincibleCheat = true
        w.activatePowerUp(PowerUpType.JETPACK)
        w.player.jumpHeld = true
        advance(w, 2f)
        assertTrue("jetpack climbs: y=${w.player.y} vy=${w.player.vy}", w.player.y > Tuning.GROUND_Y + 100f)
        w.player.jumpHeld = false
        advance(w, PowerUpType.JETPACK.baseDuration + 2.5f)
        assertFalse(w.powerUps.active(PowerUpType.JETPACK))
        assertTrue("lands safely after the jetpack ends: y=${w.player.y}", w.player.grounded)
        w.activatePowerUp(PowerUpType.DOUBLE_COINS)
        assertTrue(w.powerUps.active(PowerUpType.DOUBLE_COINS))
        advance(w, PowerUpType.DOUBLE_COINS.baseDuration + 0.1f)
        assertFalse(w.powerUps.active(PowerUpType.DOUBLE_COINS))
    }

    @Test
    fun bossFightUsesBothLanesHasTwoPhasesAndRewardsOnce() {
        for (world in Worlds.all) {
            val w = TestSupport.world(world, 77L)
            w.autoPilot = AutoPilot()
            w.start()
            w.debugBossSoon()
            val lanes = HashSet<Action>()
            var sawPhase2 = false
            var t = 0f
            while (w.state != RunState.VICTORY && t < 240f) {
                w.step(Tuning.STEP); t += Tuning.STEP
                w.projectiles.each { if (!it.friendly) lanes += it.lane }
                if ((w.boss?.phase ?: 1) == 2) sawPhase2 = true
            }
            assertEquals("${world.id}: boss not defeated", RunState.VICTORY, w.state)
            assertTrue("${world.id}: boss must use jump and slide lanes, saw $lanes", Action.JUMP in lanes && Action.SLIDE in lanes)
            assertTrue(sawPhase2)
            assertEquals("${world.id}: bot should not be hit", 0, w.hitsTaken)
            val coins = w.coins
            w.continueAfterBoss()
            advance(w, 1f)
            assertTrue(w.coins >= coins)
            val r1 = w.quit()
            assertSame("the run report is built once", r1, w.finish())
            assertTrue(r1.gateBossDefeated)
        }
    }

    @Test
    fun seededGenerationIsReproducibleAndFair() {
        fun beats(seed: Long): List<String> {
            val w = TestSupport.world(Worlds.byId("city"), seed)
            w.invincibleCheat = true
            w.start()
            advance(w, 40f)
            assertTrue(w.generator.validate(w.baseSpeed).isEmpty())
            return w.generator.beats.map { "${it.label}@${it.x.toInt()}" }
        }
        assertEquals(beats(1234L), beats(1234L))
        assertFalse(beats(1234L) == beats(4321L))
    }

    @Test
    fun noHazardSpawnsOnScreen() {
        val w = running(31L)
        w.invincibleCheat = true
        var t = 0f
        val seen = HashSet<Any>()
        while (t < 90f) {
            w.step(Tuning.STEP); t += Tuning.STEP
            w.obstacles.each { o -> if (seen.add(o)) assertTrue("spawned on screen at ${o.x}", o.x > w.cameraX + w.viewWidth || o.def.kind == com.bramble.dash.config.ObstacleKind.FALLING) }
            w.enemies.each { e -> if (seen.add(e)) assertTrue(e.x > w.cameraX + w.viewWidth) }
        }
        assertNotNull(w.generator)
    }
}
