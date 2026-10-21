package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class Cannon(val color: GemColor, var ammo: Int) {
    var appear = 0f
    var cooldown = 0.25f
    val recoil = Spring(380f, 14f)
    val squash = Spring(300f, 10f, 1f)
    var leaving = 0f
    var reserved = 0   // shots in flight
    var aim = 0f
}

class Slot(val index: Int, var locked: Boolean) {
    var x = 0f
    var y = 0f
    var cannon: Cannon? = null
    var reserved = false
    val pop = Spring(260f, 10f, 1f)
    var shake = 0f

    val free get() = !locked && cannon == null && !reserved
}

class Projectile(val cannon: Cannon, val target: Segment, x: Float, y: Float) {
    val from = Vector2(x, y)
    val pos = Vector2(x, y)
    var t = 0f
    var dur = 0.4f
    var trailTimer = 0f
    var angle = 90f
}

class Combat(private val particles: Particles) {
    val slots = ArrayList<Slot>()
    val projectiles = ArrayList<Projectile>()
    private var time = 0f
    var slotSize = 104f

    fun setup(count: Int, lockedFrom: Int) {
        slots.clear()
        for (i in 0 until count) slots += Slot(i, i >= lockedFrom)
    }

    fun layout(cx: Float, y: Float, spacing: Float) {
        val w = spacing * (slots.size - 1)
        slots.forEachIndexed { i, s ->
            s.x = cx - w / 2 + i * spacing
            s.y = y
        }
    }

    fun freeSlot(): Slot? = slots.firstOrNull { it.free }

    fun slotAt(x: Float, y: Float): Slot? = slots.firstOrNull { Vector2.dst(it.x, it.y, x, y) < slotSize * 0.55f }

    fun busy() = slots.any { it.cannon != null || it.reserved } || projectiles.isNotEmpty()

    fun place(slot: Slot, color: GemColor, ammo: Int) {
        slot.reserved = false
        slot.cannon = Cannon(color, ammo)
        slot.pop.value = 0.6f
        slot.pop.kick(5f)
        particles.smoke(slot.x, slot.y - 10f, 60f, 4, 20f)
        particles.sparkle(slot.x, slot.y + 20f, 4, 40f)
        Assets.sfx("place", 0.8f)
    }

    /**
     * Fires cannons at visible segments of their colour, nearest to the head first.
     * [onHit] is called when a fireball lands.
     */
    fun update(dt: Float, snake: Snake?, viewRight: Float, canFire: Boolean, onHit: (Projectile) -> Unit) {
        time += dt
        for (s in slots) {
            s.pop.update(dt)
            s.shake = max(0f, s.shake - dt)
            val c = s.cannon ?: continue
            c.appear = min(1f, c.appear + dt * 4f)
            c.recoil.update(dt)
            c.squash.update(dt)
            if (c.ammo <= 0 && c.reserved == 0) {
                c.leaving += dt * 3.5f
                if (c.leaving >= 1f) {
                    particles.smoke(s.x, s.y, 70f, 4, 30f)
                    s.cannon = null
                    s.pop.kick(4f)
                }
                continue
            }
            if (snake == null || !canFire || c.ammo <= 0) continue
            c.cooldown -= dt
            if (c.cooldown > 0f || c.appear < 1f) continue
            val target = snake.segments.firstOrNull {
                it.color == c.color && !it.claimed && !it.dead && it.pos.x < viewRight - 20f
            } ?: continue
            target.claimed = true
            c.ammo--
            c.reserved++
            c.cooldown = 0.32f
            c.recoil.kick(-900f)
            c.squash.value = 1.25f
            val mx = s.x
            val my = s.y + slotSize * 0.42f
            val p = Projectile(c, target, mx, my)
            p.dur = 0.28f + Vector2.dst(mx, my, target.pos.x, target.pos.y) / 1500f
            projectiles += p
            particles.flash("muzzle_flash", mx, my + 26f, 70f, 110f, 0.16f, rot = 90f, tint = c.color.tint.cpy().lerp(Color.WHITE, 0.5f))
            particles.smoke(mx, my + 10f, 40f, 2, 90f)
            Assets.sfx("shoot", 0.55f, 1.1f, 0.12f)
            Save.vibrate(8)
        }

        val it = projectiles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.t += dt / p.dur
            val e = min(1f, p.t)
            val tx = p.target.pos.x
            val ty = p.target.pos.y
            val cx = (p.from.x + tx) / 2
            val cy = max(p.from.y, ty) + 160f
            val u = 1 - e
            val nx = u * u * p.from.x + 2 * u * e * cx + e * e * tx
            val ny = u * u * p.from.y + 2 * u * e * cy + e * e * ty
            p.angle = MathUtils.atan2(ny - p.pos.y, nx - p.pos.x) * MathUtils.radiansToDegrees
            p.pos.set(nx, ny)
            p.trailTimer -= dt
            if (p.trailTimer <= 0f) {
                p.trailTimer = 0.016f
                particles.spawn(Assets.r("glow"), nx, ny).apply {
                    size0 = 34f; size1 = 6f; life = 0.25f; additive = true
                    color.set(p.cannon.color.tint).lerp(Color.ORANGE, 0.35f)
                }
            }
            if (p.t >= 1f) {
                it.remove()
                p.cannon.reserved--
                onHit(p)
            }
        }
    }

    fun draw(batch: Batch) {
        for (s in slots) {
            val ps = s.pop.value
            val shake = if (s.shake > 0f) sin(time * 70f) * 6f * s.shake * 3f else 0f
            val reg = if (s.locked) Assets.r("slot_locked") else Assets.r("slot_empty")
            batch.drawCentered(reg, s.x + shake, s.y, slotSize * ps)
            if (s.locked) {
                batch.text(Assets.fontSmall, "150", s.x + 10f, s.y - slotSize * 0.5f + 2f, 22f, Color.valueOf("ffe066"))
                batch.drawCentered(Assets.r("coin"), s.x - 26f, s.y - slotSize * 0.5f + 2f, 22f)
            }
        }
        for (s in slots) {
            val c = s.cannon ?: continue
            val reg = Assets.cannon(c.color)
            val app = Ease.outBack(c.appear)
            val leave = 1f - Ease.inCubic(c.leaving)
            val sq = c.squash.value
            val w = slotSize * 0.92f * app * leave
            val bob = sin(time * 4f + s.index) * 2f
            batch.drawAnchored(reg, s.x, s.y - slotSize * 0.36f + c.recoil.value * 0.02f + bob, w, 0.5f, 0f,
                scaleX = 2f - sq, scaleY = sq)
            // ammo badge
            if (c.ammo > 0 && c.leaving <= 0f) {
                val bx = s.x + slotSize * 0.32f
                val by = s.y - slotSize * 0.28f
                batch.color = c.color.tint
                batch.drawCentered(Assets.r("badge"), bx, by, 34f * app)
                batch.color = Color.WHITE
                batch.text(Assets.fontSmall, c.ammo.toString(), bx, by + 1f, 22f * app)
            }
        }
    }

    fun drawProjectiles(batch: Batch) {
        for (p in projectiles) {
            batch.color = Color.WHITE
            val reg = Assets.r("fireball")
            // the fireball art points right, flames trailing left
            batch.drawCentered(reg, p.pos.x, p.pos.y, 58f, rotation = p.angle)
        }
    }

    fun clear() {
        projectiles.clear()
    }
}
