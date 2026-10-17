package com.bramble.dash.game

import com.badlogic.gdx.math.Rectangle
import com.bramble.dash.config.EnemyBehavior
import com.bramble.dash.config.ObstacleKind
import com.bramble.dash.config.Tuning

/**
 * A simple reactive bot that plays using only the public input methods. Tests use it to show
 * that generated patterns and boss volleys are survivable with ordinary jump/slide timing.
 */
class AutoPilot {
    var jumps = 0
    var slides = 0
    private var holdSlideUntil = 0f
    private var jumpCooldown = 0f
    private val tmp = Rectangle()
    var debug = ""

    private class Threat { var ttc = 0f; var passTime = 0f; var bottom = 0f; var top = 0f; var set = false }
    private val overhead = Threat()
    private val low = Threat()

    fun think(w: GameWorld) {
        val p = w.player
        if (w.state != RunState.RUNNING && w.state != RunState.BOSS && w.state != RunState.BOSS_WARNING && w.state != RunState.BOSS_DEFEATED) return
        if (w.canAttack) w.pressAttack()
        if (jumpCooldown > 0f) jumpCooldown -= Tuning.STEP
        val speed = w.speed.coerceAtLeast(1f)
        val front = p.box.x + p.box.width
        val ground = Tuning.GROUND_Y
        val slideTop = w.slideHeight()
        overhead.set = false; low.set = false

        fun consider(b: Rectangle, closing: Float) {
            if (b.x + b.width < p.box.x - 2f) return
            val dx = b.x - front
            if (dx > speed * 1.3f) return
            val ttc = (dx / closing).coerceAtLeast(0f)
            val bottom = b.y - ground
            val target = if (bottom > slideTop + 1f) { if (bottom > w.standHeight() + 60f) return; overhead } else low
            if (!target.set || ttc < target.ttc) {
                target.set = true; target.ttc = ttc; target.passTime = (b.width + p.box.width + (b.x - front).coerceAtMost(0f)) / closing
                target.bottom = bottom; target.top = b.y + b.height - ground
            }
        }
        w.obstacles.each { o ->
            if (o.dead) return@each
            when (o.def.kind) {
                ObstacleKind.SOLID, ObstacleKind.DAMAGE, ObstacleKind.OSCILLATOR, ObstacleKind.HANGING, ObstacleKind.TIMED, ObstacleKind.BREAKABLE ->
                    consider(o.box, speed)
                ObstacleKind.ROLLER -> consider(o.box, speed + if (o.triggered) o.rollSpeed else 0f)
                ObstacleKind.FALLING -> if (o.triggered) { tmp.set(o.box); tmp.y = ground; consider(tmp, speed) }
                else -> {}
            }
        }
        w.enemies.each { e ->
            if (!e.alive) return@each
            if (e.def.behavior == EnemyBehavior.HOPPER) { tmp.set(e.box); tmp.y = e.baseY + (e.box.y - e.y); tmp.height += 40f; consider(tmp, speed + if (e.fired) e.chargeSpeed else 0f); return@each }
            if (e.def.behavior == EnemyBehavior.DANGLER) { tmp.set(e.box); tmp.y = e.box.y - (e.y - e.baseY); consider(tmp, speed); return@each }
            val moving = e.fired && (e.def.behavior == EnemyBehavior.CHARGER || e.def.behavior == EnemyBehavior.SWOOPER)
            if (e.solid || e.def.behavior == EnemyBehavior.PHASER) consider(e.box, speed + if (moving) e.chargeSpeed else 0f)
        }
        w.projectiles.each { pr -> if (pr.alive && !pr.friendly) consider(pr.box, (speed - pr.vx).coerceAtLeast(1f)) }

        debug = "over=${overhead.set}/${overhead.ttc}/${overhead.bottom} low=${low.set}/${low.ttc}/${low.top} slideTop=$slideTop"
        // overhead hazards: be sliding when they arrive and until they pass
        val underNow = overhead.set && overhead.ttc <= 0.001f
        if (overhead.set && overhead.ttc < 0.14f) {
            if (p.grounded) {
                if (!p.sliding || p.slideTimer < overhead.ttc + overhead.passTime + 0.03f) { w.pressSlide(); slides++ }
                holdSlideUntil = w.runTime + overhead.ttc + overhead.passTime
            } else if (p.vy < 200f) w.pressSlide()
        } else if (overhead.set && overhead.ttc < 0.45f && !p.grounded && p.vy < 0f) {
            w.pressSlide()
        }
        // ground hazards: jump so the arc clears the top while passing
        if (low.set && !underNow) {
            val h = low.top + 6f
            val v = w.move.jumpVelocity
            val tUp = if (h >= w.move.apex) w.move.riseTime else (v - kotlin.math.sqrt(v * v - 2f * Tuning.GRAVITY * h)) / Tuning.GRAVITY
            val above = w.move.timeAbove(h)
            val margin = ((above - low.passTime) / 2f).coerceAtLeast(0f)
            val overheadSoon = overhead.set && overhead.ttc < low.ttc + low.passTime + 0.1f
            if (low.ttc <= tUp + margin && (p.grounded || p.coyote > 0f) && jumpCooldown <= 0f && !overheadSoon) {
                w.pressJump(); w.releaseJumpLater = true; jumps++; jumpCooldown = 0.15f
            }
        }
        if (p.sliding && w.runTime < holdSlideUntil && p.slideTimer < 0.05f) w.pressSlide()
    }
}
