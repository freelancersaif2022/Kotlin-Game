package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Pool

class Particle : Pool.Poolable {
    lateinit var region: TextureRegion
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var gravity = 0f
    var drag = 0f
    var rot = 0f
    var vrot = 0f
    var size0 = 10f
    var size1 = 10f
    var alpha0 = 1f
    var alpha1 = 0f
    var life = 1f
    var age = 0f
    var additive = false
    var delay = 0f
    var stretch = 1f
    var popIn = 0f
    val color = Color(Color.WHITE)

    override fun reset() {
        vx = 0f; vy = 0f; gravity = 0f; drag = 0f; rot = 0f; vrot = 0f
        size0 = 10f; size1 = 10f; alpha0 = 1f; alpha1 = 0f; life = 1f; age = 0f
        additive = false; delay = 0f; stretch = 1f; popIn = 0f
        color.set(Color.WHITE)
    }
}

/** Lightweight sprite particle system (normal + additive layers). */
class Particles {
    private val pool = object : Pool<Particle>(256) {
        override fun newObject() = Particle()
    }
    private val live = ArrayList<Particle>()

    fun spawn(region: TextureRegion, x: Float, y: Float): Particle {
        val p = pool.obtain()
        p.region = region
        p.x = x
        p.y = y
        live += p
        return p
    }

    fun update(dt: Float) {
        val it = live.iterator()
        while (it.hasNext()) {
            val p = it.next()
            if (p.delay > 0f) {
                p.delay -= dt
                continue
            }
            p.age += dt
            if (p.age >= p.life) {
                it.remove()
                pool.free(p)
                continue
            }
            p.vy -= p.gravity * dt
            val d = 1f / (1f + p.drag * dt)
            p.vx *= d
            p.vy *= d
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rot += p.vrot * dt
        }
    }

    fun draw(batch: Batch) {
        drawLayer(batch, false)
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        drawLayer(batch, true)
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun drawLayer(batch: Batch, additive: Boolean) {
        val old = batch.packedColor
        for (p in live) {
            if (p.additive != additive || p.delay > 0f) continue
            val t = p.age / p.life
            var size = MathUtils.lerp(p.size0, p.size1, t)
            if (p.popIn > 0f && p.age < p.popIn) size *= Ease.outBack(p.age / p.popIn)
            val a = MathUtils.lerp(p.alpha0, p.alpha1, t)
            batch.setColor(p.color.r, p.color.g, p.color.b, p.color.a * a)
            val h = size * p.region.regionHeight / p.region.regionWidth
            batch.draw(p.region, p.x - size / 2, p.y - h / 2, size / 2, h / 2, size, h, p.stretch, 1f, p.rot)
        }
        batch.packedColor = old
    }

    fun clear() {
        live.forEach { pool.free(it) }
        live.clear()
    }

    // ------------------------------------------------------------ presets

    fun burst(x: Float, y: Float, color: Color, count: Int = 14, speed: Float = 420f, size: Float = 26f) {
        repeat(count) {
            val a = MathUtils.random(360f)
            val s = MathUtils.random(0.35f, 1f) * speed
            spawn(Assets.r("circle"), x, y).apply {
                vx = MathUtils.cosDeg(a) * s
                vy = MathUtils.sinDeg(a) * s + 120f
                gravity = 900f
                drag = 1.5f
                size0 = size * MathUtils.random(0.6f, 1.2f)
                size1 = 0f
                alpha0 = 1f
                alpha1 = 1f
                life = MathUtils.random(0.45f, 0.8f)
                this.color.set(color)
            }
        }
    }

    fun scales(x: Float, y: Float, region: TextureRegion, count: Int = 7) {
        repeat(count) {
            val a = MathUtils.random(360f)
            val s = MathUtils.random(220f, 520f)
            spawn(region, x, y).apply {
                vx = MathUtils.cosDeg(a) * s
                vy = MathUtils.sinDeg(a) * s + 200f
                gravity = 1300f
                rot = MathUtils.random(360f)
                vrot = MathUtils.random(-720f, 720f)
                size0 = MathUtils.random(16f, 28f)
                size1 = size0 * 0.6f
                alpha0 = 1f
                alpha1 = 0f
                life = MathUtils.random(0.5f, 0.9f)
            }
        }
    }

    fun sparkle(x: Float, y: Float, count: Int = 6, spread: Float = 60f, tint: Color = Color.WHITE) {
        repeat(count) {
            spawn(Assets.r("star_sparkle"), x + MathUtils.random(-spread, spread), y + MathUtils.random(-spread, spread)).apply {
                size0 = MathUtils.random(18f, 40f)
                size1 = 0f
                alpha0 = 1f
                alpha1 = 0.6f
                vrot = MathUtils.random(-180f, 180f)
                life = MathUtils.random(0.4f, 0.8f)
                delay = MathUtils.random(0f, 0.25f)
                popIn = 0.1f
                additive = true
                color.set(tint)
            }
        }
    }

    fun smoke(x: Float, y: Float, size: Float = 70f, count: Int = 3, rise: Float = 60f) {
        repeat(count) {
            val reg = Assets.r(if (MathUtils.randomBoolean()) "smoke_puff_1" else "smoke_puff_2")
            spawn(reg, x + MathUtils.random(-14f, 14f), y + MathUtils.random(-8f, 8f)).apply {
                vx = MathUtils.random(-40f, 40f)
                vy = rise + MathUtils.random(0f, 40f)
                drag = 2f
                size0 = size * MathUtils.random(0.5f, 0.8f)
                size1 = size * MathUtils.random(1.1f, 1.5f)
                rot = MathUtils.random(-20f, 20f)
                vrot = MathUtils.random(-40f, 40f)
                alpha0 = 0.85f
                alpha1 = 0f
                life = MathUtils.random(0.5f, 0.9f)
            }
        }
    }

    fun flash(region: String, x: Float, y: Float, size0: Float, size1: Float, life: Float, rot: Float = 0f, additive: Boolean = true, tint: Color = Color.WHITE): Particle =
        spawn(Assets.r(region), x, y).apply {
            this.size0 = size0
            this.size1 = size1
            this.life = life
            this.rot = rot
            this.additive = additive
            alpha0 = 1f
            alpha1 = 0f
            color.set(tint)
        }

    fun confetti(x: Float, y: Float, width: Float, count: Int = 70) {
        val names = listOf("confetti_red", "confetti_blue", "confetti_green", "confetti_yellow")
        repeat(count) {
            spawn(Assets.r(names.random()), x + MathUtils.random(-width / 2, width / 2), y + MathUtils.random(0f, 120f)).apply {
                vx = MathUtils.random(-160f, 160f)
                vy = MathUtils.random(-80f, 260f)
                gravity = 380f
                drag = 1.2f
                rot = MathUtils.random(360f)
                vrot = MathUtils.random(-540f, 540f)
                size0 = MathUtils.random(26f, 44f)
                size1 = size0
                alpha0 = 1f
                alpha1 = 0.7f
                life = MathUtils.random(2.2f, 3.4f)
                delay = MathUtils.random(0f, 0.6f)
                stretch = MathUtils.random(0.5f, 1f)
            }
        }
    }

    fun coinsFly(fromX: Float, fromY: Float, count: Int) {
        repeat(count) {
            spawn(Assets.r("coin"), fromX, fromY).apply {
                val a = MathUtils.random(40f, 140f)
                val s = MathUtils.random(300f, 600f)
                vx = MathUtils.cosDeg(a) * s
                vy = MathUtils.sinDeg(a) * s
                gravity = 1100f
                size0 = 34f
                size1 = 30f
                alpha0 = 1f
                alpha1 = 1f
                life = 0.9f
                delay = it * 0.03f
                vrot = MathUtils.random(-200f, 200f)
            }
        }
    }
}
