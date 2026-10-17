package com.bramble.dash.game

import com.badlogic.gdx.math.Rectangle
import com.bramble.dash.config.BossDef
import com.bramble.dash.config.Lane

enum class BossState(val anim: String) {
    ENTER("movement"), IDLE("idle"), PRIMARY("primary_attack"), SPECIAL("special_attack"), DAMAGE("damage"), DEATH("death")
}

/** A scheduled boss shot: fires at [time] (boss-local clock) in a lane the player must dodge. */
class BossShot(val time: Float, val lane: Action, val fx: String)

class Boss(val def: BossDef, val maxHp: Int, val encounter: Int) {
    var hp = maxHp
    var x = 0f
    var y = 0f
    var screenX = 0f            // offset from the camera's left edge
    var state = BossState.ENTER
    var stateTime = 0f
    var clock = 0f
    var flash = 0f
    var phase = 1
    var phaseBreak = 0f         // short pause when switching to phase 2
    var nextVolleyAt = 2.2f
    var volleys = 0
    val queue = ArrayList<BossShot>()
    val box = Rectangle()
    var defeated = false

    fun set(s: BossState) { if (state != s) { state = s; stateTime = 0f } }

    fun laneOf(l: Lane) = if (l == Lane.HIGH) Action.SLIDE else Action.JUMP

    /**
     * Builds the next volley. Shots are spaced by at least [minGap] seconds in arrival time (they all
     * travel the same distance at the same speed, so firing gaps equal arrival gaps).
     */
    fun planVolley(minGap: (Action, Action) -> Float, rngFloat: () -> Float) {
        val primary = laneOf(def.primaryLane)
        val special = laneOf(def.specialLane)
        val lanes: List<Action> = if (phase == 1) {
            when (volleys % 3) { 0 -> listOf(primary); 1 -> listOf(special); else -> listOf(special, primary) }
        } else {
            when (volleys % 3) {
                0 -> listOf(primary, special, primary)
                1 -> listOf(special, special, primary)
                else -> listOf(primary, primary, special, primary)
            }
        }
        val fx = if (lanes.size > 1) def.specialFx else def.primaryFx
        var t = clock + TELEGRAPH
        var prev: Action? = null
        for (lane in lanes) {
            if (prev != null) {
                val slack = if (phase == 1) 0.45f else 0.18f
                t += minGap(prev, lane) + slack + rngFloat() * 0.15f
            }
            queue += BossShot(t, lane, fx)
            prev = lane
        }
        set(if (lanes.size > 1) BossState.SPECIAL else BossState.PRIMARY)
        volleys++
        val rest = if (phase == 1) 1.5f + rngFloat() * 0.6f else 0.95f + rngFloat() * 0.4f
        nextVolleyAt = t + rest
    }

    companion object {
        const val TELEGRAPH = 0.75f
    }
}
