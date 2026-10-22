package com.smokemaster.game

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.sin

/** Animated modal panel built on popup_panel.png. */
abstract class Popup(val g: Game, var pw: Float = 800f) {
    protected var t = 0f
    private var closing = false
    var done = false
        private set
    private var after: (() -> Unit)? = null
    protected val buttons = ArrayList<Button>()
    private val panel = g.assets["popup_panel"]

    val ph get() = pw * panel.height / panel.width
    val cx get() = g.vw / 2
    open val cy get() = g.vh / 2
    val left get() = cx - pw / 2
    val right get() = cx + pw / 2
    val top get() = cy - ph / 2
    val bottom get() = cy + ph / 2

    fun close(then: (() -> Unit)? = null) {
        if (closing) return
        closing = true
        after = then
    }

    open fun update(dt: Float) {
        if (closing) {
            t -= dt * 5f
            if (t <= 0f) {
                t = 0f
                if (!done) {
                    done = true
                    after?.invoke()
                }
            }
        } else {
            t = minOf(1f, t + dt * 3.2f)
        }
        layout()
        for (b in buttons) b.update(dt)
    }

    protected abstract fun layout()
    protected abstract fun content(c: Canvas)

    fun draw(c: Canvas) {
        g.dim(c, 0.62f * clamp01(t))
        val s = if (closing) lerp(0.8f, 1f, t) else Ease.outBack(t)
        if (s <= 0.01f) return
        c.save()
        c.scale(s, s, cx, cy)
        val faded = t < 0.999f
        if (faded) c.saveLayerAlpha(0f, -200f, g.vw, g.vh + 200f, (clamp01(t) * 255).toInt())
        g.drawCentered(c, panel, cx, cy, pw)
        content(c)
        for (b in buttons) b.draw(c, g.paint)
        if (faded) c.restore()
        c.restore()
    }

    fun touch(e: Touch) {
        if (closing || t < 0.9f) return
        when (e.type) {
            Touch.DOWN -> for (b in buttons) if (b.down(e)) break
            Touch.UP -> for (b in buttons) b.up(e)
            else -> for (b in buttons) b.cancel()
        }
    }

    open fun back() { g.click(); close() }

    protected fun closeButton() = Button(g.assets["btn_close"], 118f, 118f) { g.click(); close() }
}

/** Icon + label + on/off switch. */
class ToggleRow(private val g: Game, private val icon: String, private val label: String,
                private val get: () -> Boolean, private val set: (Boolean) -> Unit) {
    var y = 0f
    var left = 0f
    var right = 0f
    val toggle = Button(g.assets["toggle_on"], 180f, 73f) {
        set(!get())
        g.click()
        g.haptics.buzz(15)
    }

    fun layout(left: Float, right: Float, y: Float) {
        this.left = left; this.right = right; this.y = y
        toggle.at(right - 140f, y)
        toggle.bitmap = g.assets[if (get()) "toggle_on" else "toggle_off"]
    }

    fun draw(c: Canvas) {
        g.drawCentered(c, g.assets[icon], left + 130f, y, 92f)
        g.text.draw(c, label, left + 205f, y + 20f, 54f, align = Paint.Align.LEFT)
    }
}

private fun settingsRows(g: Game) = listOf(
    ToggleRow(g, "icon_music", "MUSIC", { g.settings.music }, { g.audio.setMusicEnabled(it) }),
    ToggleRow(g, "icon_sound", "SOUND", { g.settings.sound }, { g.settings.sound = it }),
    ToggleRow(g, "icon_vibration", "VIBRATE", { g.settings.vibration }, { g.settings.vibration = it }),
)

class SettingsPopup(g: Game) : Popup(g) {
    private val rows = settingsRows(g)
    private val close = closeButton()

    init {
        buttons += close
        rows.forEach { buttons += it.toggle }
    }

    override fun layout() {
        close.at(right - 50f, top + 55f)
        rows.forEachIndexed { i, r -> r.layout(left, right, top + 360f + i * 170f) }
    }

    override fun content(c: Canvas) {
        g.text.draw(c, "SETTINGS", cx, top + 190f, 92f)
        rows.forEach { it.draw(c) }
        g.text.draw(c, "Smoke Master  v1.0", cx, bottom - 80f, 34f, color = 0xFFE8C987.toInt(), alpha = 0.8f)
    }
}

class HighScorePopup(g: Game) : Popup(g) {
    private val close = closeButton()

    init { buttons += close }

    override fun layout() { close.at(right - 50f, top + 55f) }

    override fun content(c: Canvas) {
        g.drawCentered(c, g.assets["trophy"], cx, top + 175f, 190f)
        g.text.draw(c, "HIGH SCORES", cx, top + 340f, 80f)
        val scores = g.settings.highScores
        if (scores.isEmpty()) {
            g.text.draw(c, "NO SCORES YET", cx, top + 560f, 56f, color = 0xFFF3E3C0.toInt())
            g.text.draw(c, "Play a round!", cx, top + 640f, 44f, color = 0xFFE8C987.toInt())
            return
        }
        scores.forEachIndexed { i, s ->
            val y = top + 450f + i * 95f
            val col = if (i == 0) TextPainter.GOLD else 0xFFF3E3C0.toInt()
            g.text.draw(c, "${i + 1}.", left + 150f, y, 60f, col, Paint.Align.LEFT)
            g.text.draw(c, s.toString(), right - 150f, y, 60f, col, Paint.Align.RIGHT)
        }
    }
}

class PausePopup(g: Game, private val onResume: () -> Unit, private val onHome: () -> Unit) : Popup(g) {
    private val rows = settingsRows(g)
    private val resume = Button(g.assets["btn_resume"], 300f, 108f) { g.click(); close(onResume) }
    private val home = Button(g.assets["btn_home"], 300f, 106f) { g.click(); close(onHome) }

    init {
        buttons += resume
        buttons += home
        rows.forEach { buttons += it.toggle }
    }

    override fun layout() {
        rows.forEachIndexed { i, r -> r.layout(left, right, top + 340f + i * 150f) }
        resume.at(cx - 175f, bottom - 170f)
        home.at(cx + 175f, bottom - 170f)
    }

    override fun content(c: Canvas) {
        g.text.draw(c, "PAUSED", cx, top + 190f, 96f)
        rows.forEach { it.draw(c) }
    }

    override fun back() { g.click(); close(onResume) }
}

class GameOverPopup(
    g: Game,
    private val score: Int,
    private val best: Int,
    private val newBest: Boolean,
    private val cigarettes: Int,
    private val puffs: Int,
    private val onAgain: () -> Unit,
    private val onHome: () -> Unit,
) : Popup(g, 860f) {
    private val again = Button(g.assets["btn_play_again"], 380f, 127f) { g.click(); close(onAgain) }
    private val home = Button(g.assets["btn_home"], 290f, 102f) { g.click(); close(onHome) }
    private var age = 0f
    private var shown = 0
    private var dinged = false
    private var scoreStr = "0"

    override val cy get() = g.vh / 2 + 70f

    init {
        buttons += again
        buttons += home
        again.pulse = true
    }

    override fun update(dt: Float) {
        super.update(dt)
        age += dt
        val p = clamp01((age - 0.45f) / 1.1f)
        val v = (score * Ease.outCubic(p)).toInt()
        if (v != shown) {
            shown = v
            scoreStr = v.toString()
        }
        if (p >= 1f && !dinged) {
            dinged = true
            g.audio.play(if (newBest) "new_record" else "ding")
        }
    }

    override fun layout() {
        again.at(cx - 175f, top + 905f)
        home.at(cx + 205f, top + 905f)
    }

    override fun content(c: Canvas) {
        // battered hero
        val img = g.assets["char_gameover"]
        val h = 450f
        val w = h * img.width / img.height
        val wob = sin(age * 2.2f) * 1.5f
        c.save()
        c.rotate(wob, cx, top + 590f)
        g.drawCentered(c, img, cx, top + 370f, w)
        c.restore()

        // title over the top edge
        val ts = Ease.outBack(clamp01((age - 0.1f) / 0.5f))
        g.drawCentered(c, g.assets["gameover_title"], cx, top + 30f, 800f * ts.coerceAtLeast(0.01f))

        g.text.draw(c, "SCORE", cx, top + 650f, 46f, color = 0xFFF3E3C0.toInt())
        g.text.draw(c, scoreStr, cx, top + 740f, 100f)
        g.text.draw(c, "BEST  $best", cx, top + 800f, 44f, color = 0xFFF3E3C0.toInt())
        g.text.draw(c, "CIGARETTES $cigarettes   •   PUFFS $puffs", cx, top + 845f, 32f, color = 0xFFE8C987.toInt())

        if (newBest && age > 1.5f) {
            val k = Ease.outBack(clamp01((age - 1.5f) / 0.4f))
            c.save()
            c.rotate(-12f, right - 170f, top + 230f)
            g.text.draw(c, "NEW BEST!", right - 170f, top + 230f, 64f * k * (1f + 0.05f * sin(age * 8f)),
                color = 0xFFFF4A2A.toInt(), strokeColor = 0xFFFFF1C0.toInt())
            c.restore()
        }
    }

    override fun back() { g.click(); close(onHome) }
}
