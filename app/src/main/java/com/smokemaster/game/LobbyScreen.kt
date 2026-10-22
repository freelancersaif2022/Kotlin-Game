package com.smokemaster.game

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import kotlin.math.min
import kotlin.math.sin

class LobbyScreen(g: Game) : Screen(g) {
    private var t = 0f
    private var popup: Popup? = null
    private val play = Button(g.assets["btn_play"], 540f, 540f * 177 / 476) { g.click(); g.go(PlayScreen(g)) }
    private val scores = Button(g.assets["btn_highscore"], 330f, 330f * 206 / 476) { g.click(); popup = HighScorePopup(g) }
    private val settings = Button(g.assets["btn_settings"], 330f, 330f * 194 / 476) { g.click(); popup = SettingsPopup(g) }
    private val buttons = listOf(play, scores, settings)

    private val smokePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }
    private val smoke = Particles(90, smokePaint)
    private val dust = Particles(40, glowPaint)
    private val puffs = arrayOf(g.assets["smoke_puff_1"], g.assets["smoke_puff_2"], g.assets["smoke_puff_3"])
    private val sparks = arrayOf(g.assets["ember_spark"])
    private val hero = g.assets["character_lobby"]
    private val heroTip: FloatArray
    private var smokeTimer = 0f
    private var dustTimer = 0f
    private val dst = RectF()

    // layout
    private var logoY = 0f
    private var heroTop = 0f
    private var heroH = 0f
    private var tipX = 0f
    private var tipY = 0f

    init {
        val meta = g.assets.spriteMeta["character_lobby"]!!
        // cigarette ember in the original 1024x1920 artwork
        heroTip = floatArrayOf((328f - meta[0]) * meta[2], (377f - meta[1]) * meta[2])
        play.pulse = true
    }

    override fun enter() {
        g.audio.music("music_lobby", 0.5f)
    }

    private fun layout() {
        val logoW = 760f
        val logoH = logoW * 501 / 1000
        logoY = g.safeTop + 40f + logoH / 2
        val smallY = g.vh - g.safeBottom - 125f
        val playY = smallY - 205f
        play.at(g.vw / 2, playY)
        scores.at(g.vw / 2 - 190f, smallY)
        settings.at(g.vw / 2 + 190f, smallY)
        val areaTop = logoY + logoH / 2 - 30f
        val areaBottom = playY - 30f
        heroH = min(areaBottom - areaTop, 1350f)
        heroTop = areaBottom - heroH
        val s = heroH / hero.height
        val heroLeft = g.vw / 2 - hero.width * s / 2
        tipX = heroLeft + heroTip[0] * s
        tipY = heroTop + heroTip[1] * s
    }

    override fun update(dt: Float) {
        t += dt
        layout()
        buttons.forEach { it.update(dt) }

        smokeTimer -= dt
        while (smokeTimer <= 0f) {
            smokeTimer += 0.09f
            smoke.spawn(RANDOM.nextInt(3), tipX + rnd(-3f, 3f), tipY - 4f, rnd(-14f, 14f) + sin(t * 1.3f) * 18f,
                rnd(-85f, -60f), rnd(2.4f, 3.2f), 14f, rnd(110f, 170f), rnd(0.16f, 0.26f), spin = rnd(-40f, 40f))
        }
        dustTimer -= dt
        while (dustTimer <= 0f) {
            dustTimer += 0.25f
            dust.spawn(0, rnd(0f, g.vw), rnd(g.vh * 0.25f, g.vh), rnd(-10f, 10f), rnd(-40f, -15f),
                rnd(3f, 5f), rnd(6f, 12f), rnd(4f, 10f), rnd(0.3f, 0.6f), spin = rnd(-60f, 60f))
        }
        smoke.update(dt)
        dust.update(dt)

        popup?.let {
            it.update(dt)
            if (it.done) popup = null
        }
    }

    override fun draw(c: Canvas) {
        g.drawCover(c, g.assets["lobby_bg"], 1.02f + 0.02f * sin(t * 0.25f))
        dust.draw(c, sparks)

        // hero with subtle breathing
        val s = heroH / hero.height
        val w = hero.width * s
        val bottom = heroTop + heroH
        c.save()
        c.scale(1f + 0.004f * sin(t * 2.1f), 1f + 0.007f * sin(t * 2.1f), g.vw / 2, bottom)
        c.rotate(0.4f * sin(t * 0.7f), g.vw / 2, bottom)
        dst.set(g.vw / 2 - w / 2, heroTop, g.vw / 2 + w / 2, bottom)
        c.drawBitmap(hero, null, dst, g.paint)
        c.restore()

        // ember glow + smoke
        val flick = 0.75f + 0.25f * sin(t * 13f) * sin(t * 7.3f)
        glowPaint.alpha = (200 * flick).toInt()
        val gs = 30f * flick
        dst.set(tipX - gs / 2, tipY - gs / 2, tipX + gs / 2, tipY + gs / 2)
        c.drawBitmap(sparks[0], null, dst, glowPaint)
        glowPaint.alpha = 255
        smoke.draw(c, puffs)

        // logo
        val ls = Ease.outBack(clamp01(t / 0.6f))
        g.drawCentered(c, g.assets["game_logo"], g.vw / 2, logoY + sin(t * 1.8f) * 6f, 760f * ls.coerceAtLeast(0.01f))

        val best = g.settings.best
        if (best > 0) {
            g.text.draw(c, "BEST  $best", g.vw / 2, play.cy - play.h / 2 - 22f, 46f, alpha = clamp01(t * 2))
        }
        buttons.forEach { it.draw(c, g.paint) }
        popup?.draw(c)
    }

    override fun touch(t: Touch) {
        popup?.let { it.touch(t); return }
        when (t.type) {
            Touch.DOWN -> for (b in buttons) if (b.down(t)) break
            Touch.UP -> buttons.forEach { it.up(t) }
            else -> buttons.forEach { it.cancel() }
        }
    }

    override fun back(): Boolean {
        val p = popup ?: return false
        p.back()
        return true
    }
}
