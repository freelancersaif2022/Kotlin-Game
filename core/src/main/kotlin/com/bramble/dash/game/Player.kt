package com.bramble.dash.game

import com.badlogic.gdx.math.Rectangle
import com.bramble.dash.config.Ability
import com.bramble.dash.config.CharacterDef
import com.bramble.dash.config.PowerUpType
import com.bramble.dash.config.Tuning

enum class PlayerAnim(val anim: String) {
    IDLE("idle"), RUN("running"), JUMP("jumping"), DOUBLE_JUMP("double_jump"), SLIDE("sliding"), FALL("falling"),
    LAND("landing"), HURT("taking_damage"), DEATH("death"), VICTORY("victory")
}

class Player(val character: CharacterDef) {
    var x = 0f
    var y = Tuning.GROUND_Y
    var prevX = 0f
    var prevY = Tuning.GROUND_Y
    var vy = 0f
    var grounded = true
    var groundTop = Tuning.GROUND_Y
    var airJumpsUsed = 0
    var coyote = 0f
    var jumpBuffer = 0f
    var jumpHeld = false
    var slideTimer = 0f
    var slideBuffered = false
    var invulnerable = 0f
    var hurtTimer = 0f
    var landTimer = 0f
    var health = Tuning.BASE_HEALTH
    var maxHealth = Tuning.BASE_HEALTH
    var anim = PlayerAnim.RUN
    var animTime = 0f
    var dead = false
    var slowTimer = 0f          // knockable/quicksand slow-down
    var inQuicksand = false
    val box = Rectangle()

    val sliding get() = slideTimer > 0f
    val ability get() = character.ability

    val slideDuration get() = if (ability == Ability.LONG_SLIDE) Tuning.SLIDE_DURATION * 1.45f else Tuning.SLIDE_DURATION
    val invulnerableTime get() = if (ability == Ability.TOUGH) Tuning.INVULNERABLE_TIME * 1.35f else Tuning.INVULNERABLE_TIME
    val fallGravityScale get() = if (ability == Ability.AIR_CONTROL) 0.72f else 1f
    val jumpScale get() = if (ability == Ability.AIR_CONTROL) 1.06f else 1f

    fun play(a: PlayerAnim) {
        if (anim != a) { anim = a; animTime = 0f }
    }

    /** Collision box: stable stand/slide boxes derived from the pack AABBs (see GameWorld.initPlayerBoxes). */
    fun updateBox(stand: Rectangle, slide: Rectangle) {
        val src = if (sliding && grounded) slide else stand
        box.set(x + src.x, y + src.y, src.width, src.height)
    }
}

/** Active power-up state: timers, shield and extra life. */
class PowerUps(private val durationBonus: Float, private val magnetBonus: Float) {
    val remaining = FloatArray(PowerUpType.values().size)
    val total = FloatArray(PowerUpType.values().size)
    var shield = false
    var extraLife = false
    var grace = 0f              // short invulnerability after jetpack/speed boost ends

    fun duration(t: PowerUpType): Float {
        var d = t.baseDuration * (1f + durationBonus)
        if (t == PowerUpType.COIN_MAGNET) d *= magnetBonus
        return d
    }

    fun active(t: PowerUpType) = remaining[t.ordinal] > 0f

    /** Timed effects refresh to full duration instead of stacking. */
    fun activate(t: PowerUpType) {
        if (!t.timed) return
        val d = duration(t)
        remaining[t.ordinal] = d
        total[t.ordinal] = d
    }

    /** Returns the types that expired during this step. */
    fun update(dt: Float, expired: MutableList<PowerUpType>) {
        if (grace > 0f) grace -= dt
        for (t in PowerUpType.values()) {
            val i = t.ordinal
            if (remaining[i] > 0f) {
                remaining[i] -= dt
                if (remaining[i] <= 0f) { remaining[i] = 0f; expired += t }
            }
        }
    }

    fun clearTimed() { remaining.fill(0f) }
}
