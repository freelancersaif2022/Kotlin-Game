package com.smokemaster.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
fun clamp01(v: Float) = v.coerceIn(0f, 1f)
fun smoothstep(e0: Float, e1: Float, x: Float): Float {
    val t = clamp01((x - e0) / (e1 - e0))
    return t * t * (3 - 2 * t)
}

/** Moves [current] towards [target] by an exponential smoothing step. */
fun approach(current: Float, target: Float, speed: Float, dt: Float): Float {
    val k = 1f - kotlin.math.exp(-speed * dt)
    return current + (target - current) * k
}

object Ease {
    fun outCubic(t: Float): Float { val u = 1 - clamp01(t); return 1 - u * u * u }
    fun inCubic(t: Float): Float { val u = clamp01(t); return u * u * u }
    fun inOutSine(t: Float): Float = (-(cos(PI * clamp01(t)) - 1) / 2).toFloat()
    fun outBack(t: Float, s: Float = 1.70158f): Float {
        val u = clamp01(t) - 1
        return u * u * ((s + 1) * u + s) + 1
    }
    fun outElastic(t: Float): Float {
        val u = clamp01(t)
        if (u == 0f || u == 1f) return u
        return (2.0.pow(-10.0 * u) * sin((u * 10 - 0.75) * (2 * PI / 3)) + 1).toFloat()
    }
}

val RANDOM = java.util.Random()
fun rnd(a: Float, b: Float) = a + RANDOM.nextFloat() * (b - a)

/** Outlined, shadowed text with an optional gold gradient fill. */
class TextPainter {
    val face: Typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = face }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = face
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private val gold = LinearGradient(
        0f, 0f, 0f, 1f,
        intArrayOf(0xFFFFF6C2.toInt(), 0xFFFFD54A.toInt(), 0xFFE39A16.toInt(), 0xFFFFE07A.toInt()),
        floatArrayOf(0f, 0.45f, 0.78f, 1f), Shader.TileMode.CLAMP,
    )
    private val m = Matrix()

    fun width(text: String, size: Float): Float {
        fill.textSize = size
        return fill.measureText(text)
    }

    fun draw(
        c: Canvas, text: String, x: Float, y: Float, size: Float,
        color: Int = GOLD, align: Paint.Align = Paint.Align.CENTER, alpha: Float = 1f,
        strokeColor: Int = 0xFF2A0C04.toInt(), strokeScale: Float = 0.16f,
    ) {
        if (alpha <= 0.01f) return
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        fill.textSize = size
        stroke.textSize = size
        fill.textAlign = align
        stroke.textAlign = align
        stroke.strokeWidth = size * strokeScale
        // drop shadow
        stroke.color = 0x99000000.toInt()
        stroke.alpha = (a * 0.6f).toInt()
        c.drawText(text, x, y + size * 0.07f, stroke)
        stroke.color = strokeColor
        stroke.alpha = a
        c.drawText(text, x, y, stroke)
        if (color == GOLD) {
            m.setScale(1f, size * 0.8f)
            m.postTranslate(0f, y - size * 0.72f)
            gold.setLocalMatrix(m)
            fill.shader = gold
            fill.color = Color.WHITE
        } else {
            fill.shader = null
            fill.color = color
        }
        fill.alpha = a
        c.drawText(text, x, y, fill)
        fill.shader = null
    }

    companion object {
        const val GOLD = 0x01FFD54A
    }
}

/** Image button with press feedback, optional idle pulse and disabled state. */
class Button(
    var bitmap: Bitmap,
    var w: Float,
    var h: Float,
    private val fireOnDown: Boolean = false,
    val onClick: () -> Unit,
) {
    var cx = 0f
    var cy = 0f
    var enabled = true
    var visible = true
    var pulse = false
    private var pointer = -1
    private var press = 0f
    private var pulseT = 0f
    private val dst = RectF()

    fun at(x: Float, y: Float): Button { cx = x; cy = y; return this }

    fun hit(x: Float, y: Float) = visible && abs(x - cx) <= w * 0.55f && abs(y - cy) <= h * 0.6f

    fun down(t: Touch): Boolean {
        if (!hit(t.x, t.y)) return false
        pointer = t.id
        press = 1f
        if (fireOnDown) onClick()
        return true
    }

    fun up(t: Touch): Boolean {
        if (t.id != pointer) return false
        pointer = -1
        if (!fireOnDown && hit(t.x, t.y)) onClick()
        return true
    }

    fun cancel() { pointer = -1 }

    fun update(dt: Float) {
        if (pointer < 0) press = approach(press, 0f, 14f, dt)
        pulseT += dt
    }

    fun draw(c: Canvas, paint: Paint, alpha: Float = 1f) {
        if (!visible || alpha <= 0f) return
        var s = 1f - 0.09f * press
        if (pulse && enabled) s *= 1f + 0.06f * sin(pulseT * 6.5f)
        val hw = w * s / 2
        val hh = h * s / 2
        dst.set(cx - hw, cy - hh, cx + hw, cy + hh)
        paint.alpha = (alpha * (if (enabled) 255 else 150)).toInt()
        paint.colorFilter = if (enabled) null else GREY
        c.drawBitmap(bitmap, null, dst, paint)
        paint.colorFilter = null
        paint.alpha = 255
    }

    companion object {
        val GREY = ColorMatrixColorFilter(ColorMatrix().apply {
            setSaturation(0.15f)
            postConcat(ColorMatrix(floatArrayOf(0.7f, 0f, 0f, 0f, 0f, 0f, 0.7f, 0f, 0f, 0f, 0f, 0f, 0.7f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)))
        })
    }
}

/** Floating "+25" style score/feedback labels. */
class FloatingTexts(private val text: TextPainter) {
    private class Item(var s: String = "", var x: Float = 0f, var y: Float = 0f, var age: Float = 0f,
                       var life: Float = 1f, var size: Float = 60f, var color: Int = 0)

    private val items = Array(24) { Item() }
    private var next = 0
    private var active = 0

    fun add(s: String, x: Float, y: Float, size: Float = 64f, color: Int = TextPainter.GOLD, life: Float = 1.1f) {
        val it = items[next]
        next = (next + 1) % items.size
        it.s = s; it.x = x; it.y = y; it.age = 0f; it.life = life; it.size = size; it.color = color
        active = items.size
    }

    fun update(dt: Float) {
        if (active == 0) return
        var any = 0
        for (it in items) {
            if (it.age < it.life) {
                it.age += dt
                it.y -= dt * 120f * (1f - it.age / it.life)
                any++
            }
        }
        active = any
    }

    fun draw(c: Canvas) {
        if (active == 0) return
        for (it in items) {
            if (it.age >= it.life) continue
            val p = it.age / it.life
            val pop = if (p < 0.15f) Ease.outBack(p / 0.15f) else 1f
            val a = if (p > 0.7f) 1f - (p - 0.7f) / 0.3f else 1f
            text.draw(c, it.s, it.x, it.y, it.size * (0.6f + 0.4f * pop), it.color, alpha = a)
        }
    }

    fun clear() { items.forEach { it.age = it.life } ; active = 0 }
}
