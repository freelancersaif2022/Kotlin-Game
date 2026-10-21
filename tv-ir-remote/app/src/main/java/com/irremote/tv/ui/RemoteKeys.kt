package com.irremote.tv.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.irremote.tv.R
import com.irremote.tv.ir.Key
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/** Receives presses from every remote control widget. */
interface KeyListener {
    fun onKey(key: Key, repeat: Boolean)
    fun isAvailable(key: Key): Boolean
    fun onUnavailable(key: Key)
    val hapticsEnabled: Boolean
}

/**
 * Shared press handling: press animation, haptics and auto-repeat while held
 * (used for volume, channel and arrow keys like a physical remote).
 */
abstract class PressableView(context: Context) : View(context) {
    var listener: KeyListener? = null
    protected var pressT = 0f
    private var anim: ValueAnimator? = null
    private var repeatKey: Key? = null
    private var repeating = false

    private val repeatRunnable = object : Runnable {
        override fun run() {
            val k = repeatKey ?: return
            listener?.onKey(k, repeating)
            repeating = true
            postDelayed(this, REPEAT_MS)
        }
    }

    protected fun animatePress(down: Boolean) {
        anim?.cancel()
        anim = ValueAnimator.ofFloat(pressT, if (down) 1f else 0f).apply {
            duration = if (down) 70 else 220
            interpolator = DecelerateInterpolator()
            addUpdateListener { pressT = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    /** Starts a press on [key]; returns false if it is not available for this TV. */
    protected fun beginPress(key: Key, repeatable: Boolean): Boolean {
        val l = listener ?: return false
        if (!l.isAvailable(key)) {
            l.onUnavailable(key)
            return false
        }
        haptic(l.hapticsEnabled)
        animatePress(true)
        l.onKey(key, false)
        if (repeatable) {
            repeatKey = key
            repeating = true
            postDelayed(repeatRunnable, REPEAT_DELAY_MS)
        }
        return true
    }

    protected fun endPress() {
        removeCallbacks(repeatRunnable)
        repeatKey = null
        repeating = false
        animatePress(false)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(repeatRunnable)
        anim?.cancel()
    }

    companion object {
        const val REPEAT_DELAY_MS = 420L
        const val REPEAT_MS = 130L
    }
}

/** A single physical-looking remote button with an icon or a text label. */
class KeyButton(
    context: Context,
    val key: Key,
    private val iconRes: Int = 0,
    private val label: String? = null,
    private val keyStyle: Style = Style.NORMAL,
    private val repeatable: Boolean = false,
    private val accentColor: Int = Palette.ACCENT,
) : PressableView(context) {

    enum class Style { NORMAL, ROUND, POWER, COLOR, APP, SMALL }

    private val body = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = dp(1f) }
    private val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x88000000.toInt() }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = if (keyStyle == Style.APP) Fonts.black else Fonts.medium
    }
    private val icon: Drawable? = if (iconRes != 0) context.getDrawable(iconRes)?.mutate() else null
    private val rect = RectF()
    private var shaderH = -1

    var iconSize = dp(if (keyStyle == Style.SMALL) 18 else 22)
    var textSizePx = dp(if (keyStyle == Style.APP) 11.5f else 15f)

    private val available get() = listener?.isAvailable(key) ?: true

    init {
        isClickable = true
        contentDescription = key.label
    }

    private fun radius(): Float = when (keyStyle) {
        Style.ROUND, Style.POWER -> min(rect.width(), rect.height()) / 2f
        Style.COLOR -> rect.height() / 2f
        else -> dp(16f)
    }

    override fun onDraw(canvas: Canvas) {
        val sh = dp(2.5f)
        rect.set(dp(1f), dp(1f), width - dp(1f), height - sh - dp(1f))
        if (shaderH != height) {
            shaderH = height
            val (top, bottom) = when (keyStyle) {
                Style.POWER -> Palette.POWER to Palette.POWER_2
                else -> 0xFF2B3140.toInt() to 0xFF1B1F29.toInt()
            }
            body.shader = LinearGradient(0f, rect.top, 0f, rect.bottom, top, bottom, Shader.TileMode.CLAMP)
            edge.shader = LinearGradient(
                0f, rect.top, 0f, rect.bottom,
                if (keyStyle == Style.POWER) 0x66FFFFFF else 0x30FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP,
            )
        }
        val r = radius()
        val alphaMul = if (available) 1f else 0.32f
        val save = canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(), (255 * alphaMul).toInt())
        val sink = sh * pressT

        // drop shadow, then the key cap which "sinks" into it while pressed
        canvas.drawRoundRect(rect.left, rect.top + sh, rect.right, rect.bottom + sh, r, r, shadow)
        rect.offset(0f, sink)
        canvas.drawRoundRect(rect, r, r, body)
        canvas.drawRoundRect(rect, r, r, edge)
        if (pressT > 0f) {
            val c = if (keyStyle == Style.POWER) 0xFFFFFFFF.toInt() else accentColor
            glow.color = withAlpha(c, (70 * pressT).toInt())
            canvas.drawRoundRect(rect, r, r, glow)
        }

        val cx = rect.centerX()
        val cy = rect.centerY()
        val content = when (keyStyle) {
            Style.POWER -> 0xFFFFFFFF.toInt()
            Style.APP -> accentColor
            else -> blend(Palette.TEXT, 0xFFFFFFFF.toInt(), pressT)
        }
        if (keyStyle == Style.COLOR) {
            val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
            val bw = rect.width() * 0.5f
            val bh = dp(5f)
            canvas.drawRoundRect(cx - bw / 2, cy - bh / 2, cx + bw / 2, cy + bh / 2, bh / 2, bh / 2, bar)
        } else if (icon != null) {
            val s = iconSize
            icon.setTint(content)
            icon.setBounds((cx - s / 2).toInt(), (cy - s / 2).toInt(), (cx + s / 2).toInt(), (cy + s / 2).toInt())
            icon.draw(canvas)
        } else if (label != null) {
            textPaint.color = content
            textPaint.textSize = textSizePx
            val fm = textPaint.fontMetrics
            canvas.drawText(label, cx, cy - (fm.ascent + fm.descent) / 2, textPaint)
        }
        canvas.restoreToCount(save)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> beginPress(key, repeatable)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                endPress()
                if (e.actionMasked == MotionEvent.ACTION_UP) performClick()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

/** Circular navigation pad: four arrows around a central OK key. */
class DPadView(context: Context) : PressableView(context) {
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = dp(1f) }
    private val center = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = dp(1f) }
    private val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x99000000.toInt() }
    private val highlight = Paint(Paint.ANTI_ALIAS_FLAG)
    private val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x40000000; strokeWidth = dp(1.5f) }
    private val okText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; typeface = Fonts.bold; textSize = dp(17f)
    }
    private val chevrons = mapOf(
        Key.UP to context.getDrawable(R.drawable.ic_chevron_up)!!.mutate(),
        Key.DOWN to context.getDrawable(R.drawable.ic_chevron_down)!!.mutate(),
        Key.LEFT to context.getDrawable(R.drawable.ic_chevron_left)!!.mutate(),
        Key.RIGHT to context.getDrawable(R.drawable.ic_chevron_right)!!.mutate(),
    )
    private val path = Path()
    private val oval = RectF()
    private var active: Key? = null
    private var sized = -1

    init {
        isClickable = true
        contentDescription = "Navigation pad"
    }

    private val outerR get() = min(width, height) / 2f - dp(4f)
    private val innerR get() = outerR * 0.42f

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f - dp(1.5f)
        val rO = outerR
        val rI = innerR
        if (sized != width) {
            sized = width
            ring.shader = RadialGradient(cx, cy - rO * 0.4f, rO * 1.5f, 0xFF2E3443.toInt(), 0xFF181C25.toInt(), Shader.TileMode.CLAMP)
            ringEdge.shader = LinearGradient(0f, cy - rO, 0f, cy + rO, 0x38FFFFFF, 0x05FFFFFF, Shader.TileMode.CLAMP)
            center.shader = LinearGradient(0f, cy - rI, 0f, cy + rI, 0xFF343B4C.toInt(), 0xFF1F2430.toInt(), Shader.TileMode.CLAMP)
            centerEdge.shader = LinearGradient(0f, cy - rI, 0f, cy + rI, 0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx, cy + dp(3f), rO, shadow)
        canvas.drawCircle(cx, cy, rO, ring)
        canvas.drawCircle(cx, cy, rO, ringEdge)

        // diagonal grooves between the four arrow zones
        for (a in listOf(45.0, 135.0, 225.0, 315.0)) {
            val rad = Math.toRadians(a)
            val c = Math.cos(rad).toFloat()
            val s = Math.sin(rad).toFloat()
            canvas.drawLine(cx + c * (rI + dp(6f)), cy + s * (rI + dp(6f)), cx + c * (rO - dp(10f)), cy + s * (rO - dp(10f)), divider)
        }

        val pressed = active
        if (pressed != null && pressed != Key.OK && pressT > 0f) {
            val start = when (pressed) { Key.RIGHT -> -45f; Key.DOWN -> 45f; Key.LEFT -> 135f; else -> 225f }
            highlight.color = withAlpha(Palette.ACCENT, (90 * pressT).toInt())
            path.reset()
            oval.set(cx - rO, cy - rO, cx + rO, cy + rO)
            path.arcTo(oval, start, 90f)
            oval.set(cx - rI, cy - rI, cx + rI, cy + rI)
            path.arcTo(oval, start + 90f, -90f)
            path.close()
            canvas.drawPath(path, highlight)
        }

        val mid = (rO + rI) / 2f
        val s = dp(30f).toInt()
        for ((k, d) in chevrons) {
            val (x, y) = when (k) {
                Key.UP -> cx to cy - mid
                Key.DOWN -> cx to cy + mid
                Key.LEFT -> cx - mid to cy
                else -> cx + mid to cy
            }
            val ok = listener?.isAvailable(k) ?: true
            d.setTint(if (pressed == k) blend(Palette.TEXT_2, 0xFFFFFFFF.toInt(), pressT) else if (ok) Palette.TEXT_2 else Palette.TEXT_3)
            d.setBounds((x - s / 2).toInt(), (y - s / 2).toInt(), (x + s / 2).toInt(), (y + s / 2).toInt())
            d.draw(canvas)
        }

        val okPress = if (pressed == Key.OK) pressT else 0f
        canvas.drawCircle(cx, cy + dp(3f), rI, shadow)
        canvas.drawCircle(cx, cy + dp(2f) * okPress, rI, center)
        canvas.drawCircle(cx, cy + dp(2f) * okPress, rI, centerEdge)
        if (okPress > 0f) {
            highlight.color = withAlpha(Palette.ACCENT, (80 * okPress).toInt())
            canvas.drawCircle(cx, cy + dp(2f) * okPress, rI, highlight)
        }
        val okAvailable = listener?.isAvailable(Key.OK) ?: true
        okText.color = if (okAvailable) blend(Palette.TEXT, 0xFFFFFFFF.toInt(), okPress) else Palette.TEXT_3
        val fm = okText.fontMetrics
        canvas.drawText("OK", cx, cy + dp(2f) * okPress - (fm.ascent + fm.descent) / 2, okText)
    }

    private fun zoneAt(x: Float, y: Float): Key? {
        val dx = x - width / 2f
        val dy = y - height / 2f
        val d = hypot(dx, dy)
        if (d > outerR + dp(8f)) return null
        if (d < innerR) return Key.OK
        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
        return when {
            angle >= -45 && angle < 45 -> Key.RIGHT
            angle >= 45 && angle < 135 -> Key.DOWN
            angle >= -135 && angle < -45 -> Key.UP
            else -> Key.LEFT
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val k = zoneAt(e.x, e.y) ?: return false
                active = k
                beginPress(k, repeatable = k != Key.OK)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                endPress()
                if (e.actionMasked == MotionEvent.ACTION_UP) performClick()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

/** Vertical rocker (volume or channel): top half sends [upKey], bottom half [downKey]. */
class RockerView(
    context: Context,
    private val title: String,
    private val upKey: Key,
    private val downKey: Key,
    upIcon: Int,
    downIcon: Int,
) : PressableView(context) {
    private val body = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = dp(1f) }
    private val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x99000000.toInt() }
    private val highlight = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; typeface = Fonts.bold; textSize = dp(11f); letterSpacing = 0.12f
        color = Palette.TEXT_3
    }
    private val up = context.getDrawable(upIcon)!!.mutate()
    private val down = context.getDrawable(downIcon)!!.mutate()
    private val rect = RectF()
    private var active: Key? = null
    private var sized = -1

    init {
        isClickable = true
        contentDescription = title
    }

    override fun onDraw(canvas: Canvas) {
        rect.set(dp(2f), dp(2f), width - dp(2f), height - dp(5f))
        val r = rect.width() / 2f
        if (sized != height) {
            sized = height
            body.shader = LinearGradient(0f, rect.top, 0f, rect.bottom, 0xFF2B3140.toInt(), 0xFF1A1E28.toInt(), Shader.TileMode.CLAMP)
            edge.shader = LinearGradient(0f, rect.top, 0f, rect.bottom, 0x38FFFFFF, 0x05FFFFFF, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(rect.left, rect.top + dp(3f), rect.right, rect.bottom + dp(3f), r, r, shadow)
        canvas.drawRoundRect(rect, r, r, body)
        canvas.drawRoundRect(rect, r, r, edge)

        val pressed = active
        if (pressed != null && pressT > 0f) {
            highlight.color = withAlpha(Palette.ACCENT, (85 * pressT).toInt())
            val save = canvas.save()
            if (pressed == upKey) canvas.clipRect(rect.left, rect.top, rect.right, rect.centerY())
            else canvas.clipRect(rect.left, rect.centerY(), rect.right, rect.bottom)
            canvas.drawRoundRect(rect, r, r, highlight)
            canvas.restoreToCount(save)
        }

        val s = dp(26f).toInt()
        val cx = rect.centerX()
        val upY = rect.top + rect.height() * 0.2f
        val downY = rect.bottom - rect.height() * 0.2f
        val l = listener
        up.setTint(if (l == null || l.isAvailable(upKey)) Palette.TEXT else Palette.TEXT_3)
        down.setTint(if (l == null || l.isAvailable(downKey)) Palette.TEXT else Palette.TEXT_3)
        up.setBounds((cx - s / 2).toInt(), (upY - s / 2).toInt(), (cx + s / 2).toInt(), (upY + s / 2).toInt())
        down.setBounds((cx - s / 2).toInt(), (downY - s / 2).toInt(), (cx + s / 2).toInt(), (downY + s / 2).toInt())
        up.draw(canvas)
        down.draw(canvas)
        val fm = titlePaint.fontMetrics
        canvas.drawText(title, cx, rect.centerY() - (fm.ascent + fm.descent) / 2, titlePaint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val k = if (e.y < height / 2f) upKey else downKey
                active = k
                beginPress(k, repeatable = true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                endPress()
                if (e.actionMasked == MotionEvent.ACTION_UP) performClick()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

/** The small IR LED at the top of the remote; glows while a signal is being sent. */
class LedView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var level = 0f
    private var anim: ValueAnimator? = null

    fun setActive(on: Boolean) {
        anim?.cancel()
        anim = ValueAnimator.ofFloat(level, if (on) 1f else 0f).apply {
            duration = if (on) 40 else 260
            addUpdateListener { level = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = dp(3.5f)
        if (level > 0f) {
            paint.shader = RadialGradient(cx, cy, r * 4f, withAlpha(Palette.POWER, (150 * level).toInt()), 0x00FF3D57, Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, r * 4f, paint)
            paint.shader = null
        }
        paint.color = blend(0xFF3A1820.toInt(), 0xFFFF5A6E.toInt(), level)
        canvas.drawCircle(cx, cy, r, paint)
    }
}
