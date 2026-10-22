package com.smokemaster.game

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.sin

class SplashScreen(g: Game) : Screen(g) {
    private var t = 0f
    private var shown = 0f
    private var leaving = false
    private val smokePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val smoke = Particles(60, smokePaint)
    private val puffs = arrayOf(g.assets["smoke_puff_1"], g.assets["smoke_puff_2"], g.assets["smoke_puff_3"])
    private var spawn = 0f
    private val barBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCC1A0703.toInt() }
    private val barStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 5f; color = 0xFFE7B54A.toInt()
    }
    private val barFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private var barShaderW = -1f
    private val r = RectF()

    override fun update(dt: Float) {
        t += dt
        shown = approach(shown, g.loadProgress, 6f, dt)
        spawn -= dt
        while (spawn <= 0f) {
            spawn += 0.16f
            smoke.spawn(RANDOM.nextInt(3), rnd(-100f, g.vw + 100f), g.vh + 120f, rnd(-25f, 25f), rnd(-140f, -80f),
                rnd(6f, 9f), rnd(250f, 400f), rnd(600f, 900f), rnd(0.10f, 0.2f), spin = rnd(-12f, 12f))
        }
        smoke.update(dt)
        if (!leaving && g.loaded && t > 2.6f && shown > 0.97f) {
            leaving = true
            g.go(LobbyScreen(g))
        }
    }

    override fun draw(c: Canvas) {
        g.drawCover(c, g.assets["splash_bg"], 1.0f + 0.06f * Ease.inOutSine(t / 5f))
        g.dim(c, 0.25f)
        smoke.draw(c, puffs)

        // logo
        val p = clamp01((t - 0.2f) / 0.9f)
        if (p > 0f) {
            val s = Ease.outElastic(p)
            val bob = sin(t * 2f) * 8f * p
            g.drawCentered(c, g.assets["game_logo"], g.vw / 2, g.vh * 0.40f + bob, 920f * s, clamp01(p * 3))
        }

        // loading bar
        val a = clamp01((t - 0.6f) / 0.4f)
        if (a > 0f) {
            val w = 600f
            val y = g.vh * 0.70f
            val x0 = g.vw / 2 - w / 2
            if (barShaderW != g.vw) {
                barShaderW = g.vw
                barFill.shader = LinearGradient(x0, 0f, x0 + w, 0f,
                    intArrayOf(0xFFFFE07A.toInt(), 0xFFF0A020.toInt(), 0xFFFF5A1F.toInt()), null, Shader.TileMode.CLAMP)
            }
            barBg.alpha = (a * 204).toInt()
            barStroke.alpha = (a * 255).toInt()
            barFill.alpha = (a * 255).toInt()
            r.set(x0, y - 16f, x0 + w, y + 16f)
            c.drawRoundRect(r, 16f, 16f, barBg)
            r.set(x0 + 4f, y - 12f, x0 + 4f + (w - 8f) * shown, y + 12f)
            if (r.width() > 1f) c.drawRoundRect(r, 12f, 12f, barFill)
            r.set(x0, y - 16f, x0 + w, y + 16f)
            c.drawRoundRect(r, 16f, 16f, barStroke)
            g.text.draw(c, "LOADING  ${(shown * 100).toInt()}%", g.vw / 2, y + 85f, 44f, alpha = a)
        }
        g.text.draw(c, "Smoking is injurious to health. This game is fiction.", g.vw / 2,
            g.vh - 70f - g.safeBottom, 30f, color = 0xFFF3E3C0.toInt(), alpha = 0.75f * a)
    }

    override fun back() = true
}
