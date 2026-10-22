package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/** Lucky wheel: one free spin per day, extra spins cost coins. */
class SpinScreen(game: SnakeAttackGame) : MetaScreen(game, "LUCKY SPIN", "ribbon_purple") {
    private var angle = 0f
    private var speed = 0f
    private var spinning = false
    private var lastTick = 0
    private var result: SpinWheel.Slice? = null
    private var resultTime = 0f
    private var pointerKick = 0f
    private val spinBtn = Button(Assets.r("btn_green"), 380f, "", 44f) { spin() }
    private var cx = 0f
    private var cy = 0f
    private val radius = 250f

    init {
        buttons += spinBtn
        spinBtn.pulse = true
        refreshLabel()
    }

    private fun refreshLabel() {
        spinBtn.label = if (Save.freeSpinReady) "FREE SPIN" else "SPIN  ${SpinWheel.EXTRA_SPIN_PRICE}"
        spinBtn.icon = if (Save.freeSpinReady) null else Assets.r("coin")
        spinBtn.iconSize = 44f
    }

    override fun layout() {
        super.layout()
        cx = W / 2
        cy = H / 2 + 80f
        spinBtn.x = W / 2
        spinBtn.y = 200f
    }

    private fun spin() {
        if (spinning) return
        if (Save.freeSpinReady) {
            Save.useFreeSpin()
        } else if (Save.coins >= SpinWheel.EXTRA_SPIN_PRICE) {
            Save.coins -= SpinWheel.EXTRA_SPIN_PRICE
        } else {
            Assets.sfx("error", 0.7f)
            toast("Not enough coins", "Win levels to earn more", "coin")
            return
        }
        spinning = true
        result = null
        speed = MathUtils.random(720f, 1080f)
        spinBtn.enabled = false
        Assets.sfx("swoosh_in", 0.7f)
    }

    /** Index of the slice under the pointer (top of the wheel). */
    private fun sliceAtPointer(): Int {
        // the wheel turns clockwise by [angle]; slice i is centred (i + 0.5) * 45° clockwise from the top
        val a = (((-angle) % 360f) + 360f) % 360f
        return floor(a / 45f).toInt() % 8
    }

    override fun tick(dt: Float) {
        pointerKick = maxOf(0f, pointerKick - dt * 6f)
        if (spinning) {
            angle += speed * dt
            speed *= 1f / (1f + dt * 0.9f)
            speed -= 30f * dt
            val tick = floor(angle / 45f).toInt()
            if (tick != lastTick) {
                lastTick = tick
                Assets.sfx("spin_tick", 0.6f, 1f + min(speed, 900f) / 2000f, 0.02f)
                pointerKick = 1f
            }
            if (speed <= 8f) {
                spinning = false
                val s = SpinWheel.slices[sliceAtPointer()]
                result = s
                resultTime = 0f
                Save.coins += s.coins
                s.booster?.let { Save.addBooster(it, 1) }
                Assets.sfx("reward", 0.9f)
                particles.sparkle(cx, cy + radius, 14, 80f, Palette.GOLD)
                particles.confetti(W / 2, H + 40f, W, 50)
                if (s.coins > 0) particles.coinsFly(cx, cy, 10)
                spinBtn.enabled = true
                refreshLabel()
            }
        }
        resultTime += dt
    }

    override fun content() {
        val b = batch
        Ui.rays(b, time, cx, cy, 1.1f, Palette.RAYS_WIN, 0.25f)
        b.drawCentered(Assets.r("ui_spin_wheel_stand"), cx, cy - radius - 30f, 300f)
        b.drawCentered(Assets.r("ui_spin_wheel_8_slices"), cx, cy, radius * 2.15f, rotation = -angle)
        // slice prizes ride on the wheel (slice i spans [i*45, i*45+45) clockwise from the pointer)
        for (i in 0 until 8) {
            val s = SpinWheel.slices[i]
            val a = 90f - angle - (i + 0.5f) * 45f
            val x = cx + MathUtils.cosDeg(a) * radius * 0.62f
            val y = cy + MathUtils.sinDeg(a) * radius * 0.62f
            b.drawCentered(Assets.r(s.icon), x, y, if (s.booster != null) 74f else 56f, rotation = a - 90f)
            if (s.label.isNotEmpty()) {
                val tx = cx + MathUtils.cosDeg(a) * radius * 0.88f
                val ty = cy + MathUtils.sinDeg(a) * radius * 0.88f
                b.text(Assets.fontBig, s.label, tx, ty, 30f)
            }
        }
        // rim lights blink
        for (k in 0 until 16) {
            val a = k * 22.5f
            val on = ((time * 6f).toInt() + k) % 2 == 0
            b.drawCentered(Assets.r(if (on) "ui_wheel_light_on" else "ui_wheel_light_off"),
                cx + MathUtils.cosDeg(a) * radius * 1.04f, cy + MathUtils.sinDeg(a) * radius * 1.04f, 30f)
        }
        b.drawCentered(Assets.r("ui_spin_wheel_center_button"), cx, cy, 120f)
        b.drawCentered(Assets.r("star"), cx, cy, 70f, rotation = sin(time * 2f) * 10f)
        b.drawCentered(Assets.r("ui_spin_wheel_pointer"), cx, cy + radius + 26f, 80f, rotation = pointerKick * -14f)
        spinBtn.draw(b)
        result?.let { r ->
            val k = Ease.outBack(min(1f, resultTime * 3f))
            val fade = 1f - clamp01((resultTime - 2.6f) * 2f)
            if (fade <= 0f) return
            b.setColor(1f, 1f, 1f, fade)
            b.drawCentered(Assets.r("ui_glow_burst"), cx, cy, 360f * k, rotation = time * 30f)
            b.drawCentered(Assets.r(r.icon), cx, cy, 150f * k)
            b.color = Color.WHITE
            val text = if (r.booster != null) "+1 ${r.booster.title}" else "+${r.coins} COINS"
            b.text(Assets.fontBig, text, cx, cy - 120f, 52f * k, Palette.GOLD, alpha = fade)
        }
        if (!spinning && abs(speed) < 9f && result == null && Save.freeSpinReady) {
            b.text(Assets.fontBig, "1 FREE SPIN EVERY DAY", W / 2, 300f, 30f)
        }
    }
}
