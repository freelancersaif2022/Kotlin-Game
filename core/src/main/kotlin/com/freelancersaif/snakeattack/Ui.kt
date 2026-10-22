package com.freelancersaif.snakeattack

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.Batch

/** Shared drawing helpers for HUDs and popups. */
object Ui {
    val BROWN: Color get() = Palette.BROWN

    fun dim(b: Batch, w: Float, h: Float, alpha: Float) = b.fillRect(-20f, -20f, w + 40f, h + 40f, 0.02f, 0.04f, 0.02f, alpha)

    /** Framed popup with a title ribbon across its top edge, centred horizontally on [cx]. */
    fun popup(b: Batch, cx: Float, cy: Float, w: Float, h: Float, ribbon: String, title: String, scale: Float) {
        if (scale <= 0f) return
        val pw = w * scale
        val ph = h * scale
        b.setColor(0f, 0f, 0f, 0.45f)
        b.drawCentered(Assets.r("glow"), cx, cy - 20f, pw * 1.5f, ph * 1.4f)
        b.color = Color.WHITE
        Assets.framePanel.draw(b, cx - pw / 2, cy - ph / 2, pw, ph)
        val ry = cy + ph / 2 - 6f * scale
        b.drawCentered(Assets.r(ribbon), cx, ry, (w + 60f) * scale)
        b.text(Assets.fontBig, title, cx, ry + 10f * scale, 50f * scale)
    }

    /** Dark rounded pill with an icon on the left and text. */
    fun pill(b: Batch, x: Float, y: Float, w: Float, icon: String, text: String, textColor: Color = Color.WHITE, iconSize: Float = 52f) {
        Assets.pill.draw(b, x, y - 26f, w, 52f)
        b.drawCentered(Assets.r(icon), x + 8f, y, iconSize)
        b.text(Assets.fontBig, text, x + w / 2 + 16f, y + 1f, 30f, textColor)
    }

    fun coins(b: Batch, x: Float, y: Float, w: Float = 160f) = pill(b, x, y, w, "coin", num(Save.coins))

    // small cache so per-frame counters don't allocate new strings
    private val numCache = HashMap<Int, String>()
    fun num(n: Int): String = numCache.getOrPut(n) { n.toString() }
    private var clockSec = -1
    private var clockStr = ""

    /** Hearts pill: lives count and the refill countdown when not full. */
    fun lives(b: Batch, x: Float, y: Float, w: Float = 190f) {
        val l = Save.lives
        Assets.pill.draw(b, x, y - 26f, w, 52f)
        b.drawCentered(Assets.r("heart_full"), x + 10f, y, 58f)
        b.text(Assets.fontBig, num(l), x + 10f, y + 2f, 28f)
        val txt = if (l >= Save.MAX_LIVES) "FULL" else clock(Save.nextLifeIn)
        b.text(Assets.fontBig, txt, x + w / 2 + 22f, y + 1f, 28f, if (l > 0) Color.WHITE else Palette.RED)
    }

    fun clock(ms: Long): String {
        val s = (ms / 1000).toInt()
        if (s == clockSec) return clockStr
        clockSec = s
        val m = s / 60
        val r = s % 60
        clockStr = "$m:${if (r < 10) "0" else ""}$r"
        return clockStr
    }

    /** Rotating light rays (additive). */
    fun rays(b: Batch, time: Float, x: Float, y: Float, scale: Float, color: Color, alpha: Float) {
        if (alpha <= 0f) return
        b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        b.setColor(color.r, color.g, color.b, alpha)
        val ray = Assets.r("ray")
        for (i in 0 until 12) {
            val a = time * 18f + i * 30f
            b.draw(ray, x - 40f * scale, y, 40f * scale, 0f, 80f * scale, 560f * scale, 1f, 1f, a)
        }
        b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        b.color = Color.WHITE
    }

    /** Small slide-in toast at the top of the screen (achievements, rewards). */
    class Toast(val title: String, val text: String, val icon: String) {
        var t = 0f
    }

    fun drawToast(b: Batch, w: Float, top: Float, toast: Toast) {
        val t = toast.t
        val inK = Ease.outBack(clamp01(t / 0.35f))
        val outK = Ease.inCubic(clamp01((t - 2.6f) / 0.3f))
        val y = top - 70f + (1f - inK) * 160f + outK * 160f
        val pw = 560f
        Assets.framePanel.draw(b, w / 2 - pw / 2, y - 55f, pw, 110f)
        b.drawCentered(Assets.r(toast.icon), w / 2 - pw / 2 + 64f, y, 80f)
        b.text(Assets.fontBig, toast.title, w / 2 + 40f, y + 20f, 34f, Palette.DARK_GREEN)
        b.text(Assets.fontPlain, toast.text, w / 2 + 40f, y - 22f, 26f, Palette.BROWN)
    }
}
