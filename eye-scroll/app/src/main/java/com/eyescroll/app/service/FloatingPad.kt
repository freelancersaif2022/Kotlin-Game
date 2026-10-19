package com.eyescroll.app.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.eyescroll.app.data.AppSettings
import com.eyescroll.app.data.ScrollStyle
import com.eyescroll.app.data.SettingsStore
import com.eyescroll.app.scroll.ScrollDirection
import com.eyescroll.app.scroll.ScrollMath
import kotlin.math.abs
import kotlin.math.hypot

/**
 * The floating scroll pad.
 *  - move your finger slightly up/down and release  -> smooth scroll (further = longer scroll)
 *  - move and keep holding                          -> keeps scrolling until you let go
 *  - long-press without moving, then drag           -> move the pad (snaps to the edge)
 */
class FloatingPad(private val service: ScrollAccessibilityService, private val store: SettingsStore) {

    private val wm = service.getSystemService(WindowManager::class.java)
    private val density = service.resources.displayMetrics.density
    private val view = PadView(service)
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT,
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private var attached = false
    private var settings: AppSettings = store.current
    private var snapAnimator: ValueAnimator? = null

    // touch state
    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var moving = false
    private var direction: ScrollDirection? = null
    private var peakDy = 0f
    private var lastDy = 0f
    private var holdRunning = false
    private var holdScrolled = false
    private var holdUnsupported = false

    private val longPress = Runnable {
        moving = true
        view.moving = true
        haptic(HapticFeedbackConstants.LONG_PRESS, force = true)
    }

    private val holdTick = object : Runnable {
        override fun run() {
            val dir = direction ?: return
            if (!holdRunning) {
                holdRunning = true
                service.beginHold()
                view.holding = true
            }
            if (service.holdStep(dir)) {
                holdScrolled = true
                view.flash(dir)
                haptic(HapticFeedbackConstants.CLOCK_TICK)
                view.postDelayed(this, ScrollMath.holdIntervalMs(lastDy, thresholdPx(), travelPx(), settings.holdSpeed))
            } else {
                // This screen has no scrollable view (e.g. a game or canvas): fall back to the
                // swipe gesture when the finger is released.
                holdUnsupported = true
                stopHold()
            }
        }
    }

    fun show() {
        if (attached) return
        setupTouch()
        layoutForSize()
        val (w, h) = screen()
        params.x = if (settings.padX >= 0) settings.padX.coerceIn(0, w - view.windowW) else w - view.windowW
        params.y = if (settings.padY >= 0) settings.padY.coerceIn(0, h - view.windowH) else (h * 0.52f).toInt()
        try {
            wm.addView(view, params)
            attached = true
        } catch (_: Exception) {
        }
    }

    fun hide() {
        snapAnimator?.cancel()
        view.removeCallbacks(longPress)
        view.removeCallbacks(holdTick)
        if (holdRunning) stopHold()
        if (attached) {
            runCatching { wm.removeView(view) }
            attached = false
        }
    }

    fun applySettings(s: AppSettings) {
        val sizeChanged = s.padSize != settings.padSize
        val positionReset = s.padX < 0 && settings.padX >= 0
        settings = s
        view.idleAlpha = s.padOpacity
        if (sizeChanged) layoutForSize()
        if (positionReset || sizeChanged) {
            val (w, h) = screen()
            if (positionReset) {
                params.x = w - view.windowW
                params.y = (h * 0.52f).toInt()
            } else {
                params.x = params.x.coerceIn(0, w - view.windowW)
                params.y = params.y.coerceIn(0, h - view.windowH)
            }
            if (attached) runCatching { wm.updateViewLayout(view, params) }
        }
        view.invalidate()
    }

    private fun layoutForSize() {
        view.setPadSize(settings.padSize.widthDp * density, settings.padSize.heightDp * density)
        params.width = view.windowW
        params.height = view.windowH
    }

    private fun screen(): Pair<Int, Int> {
        val m = service.resources.displayMetrics
        return m.widthPixels to m.heightPixels
    }

    private fun thresholdPx() = settings.triggerDp * density
    private fun travelPx() = view.travel

    private fun padOnRight(): Boolean = params.x + view.windowW / 2 > screen().first / 2

    private fun haptic(type: Int, force: Boolean = false) {
        if (settings.haptics || force) view.performHapticFeedback(type)
    }

    private fun stopHold() {
        view.removeCallbacks(holdTick)
        if (holdRunning) {
            holdRunning = false
            service.endHold()
        }
        view.holding = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouch() {
        val slop = ViewConfiguration.get(service).scaledTouchSlop.toFloat()
        view.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    snapAnimator?.cancel()
                    downRawX = e.rawX
                    downRawY = e.rawY
                    startX = params.x
                    startY = params.y
                    moving = false
                    direction = null
                    peakDy = 0f
                    lastDy = 0f
                    holdScrolled = false
                    holdUnsupported = false
                    view.touching = true
                    view.postDelayed(longPress, LONG_PRESS_MS)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downRawX
                    val dy = e.rawY - downRawY
                    if (moving) {
                        val (w, h) = screen()
                        params.x = (startX + dx).toInt().coerceIn(0, w - view.windowW)
                        params.y = (startY + dy).toInt().coerceIn(0, h - view.windowH)
                        if (attached) wm.updateViewLayout(view, params)
                    } else {
                        if (hypot(dx, dy) > slop) view.removeCallbacks(longPress)
                        lastDy = dy
                        if (abs(dy) > abs(peakDy)) peakDy = dy
                        view.setThumb(dy.coerceIn(-view.travel, view.travel))
                        val next = ScrollMath.direction(dy, thresholdPx(), settings.style == ScrollStyle.NATURAL)
                        if (next != direction) {
                            direction = next
                            view.activeFingerUp = if (next == null) null else dy < 0
                            stopHold()
                            if (next != null) {
                                haptic(HapticFeedbackConstants.CLOCK_TICK)
                                if (!holdUnsupported) view.postDelayed(holdTick, HOLD_DELAY_MS)
                            }
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.removeCallbacks(longPress)
                    val wasHolding = holdRunning
                    stopHold()
                    view.touching = false
                    if (moving) {
                        moving = false
                        view.moving = false
                        snapToEdge()
                    } else if (e.actionMasked == MotionEvent.ACTION_UP) {
                        val dir = direction
                        if (dir != null && !holdScrolled) {
                            // Use the furthest point reached in this direction for the distance.
                            val dist = if ((peakDy < 0) == (lastDy < 0)) peakDy else lastDy
                            val fraction = ScrollMath.swipeFraction(dist, thresholdPx(), travelPx(), settings.maxDistance)
                            if (service.swipeScroll(dir, fraction, settings.speed.durationMs, padOnRight())) {
                                view.flash(dir)
                            }
                        } else if (dir == null && !wasHolding && abs(lastDy) < slop && SystemClock.uptimeMillis() - e.downTime < 250) {
                            view.pulse()
                        }
                    }
                    direction = null
                    view.activeFingerUp = null
                    view.releaseThumb()
                }
            }
            true
        }
    }

    private fun snapToEdge() {
        val (w, _) = screen()
        val target = if (params.x + view.windowW / 2 < w / 2) 0 else w - view.windowW
        snapAnimator = ValueAnimator.ofInt(params.x, target).apply {
            duration = 280
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener {
                params.x = it.animatedValue as Int
                if (attached) runCatching { wm.updateViewLayout(view, params) }
            }
            start()
        }
        store.update { it.copy(padX = target, padY = params.y) }
    }

    companion object {
        private const val LONG_PRESS_MS = 450L
        private const val HOLD_DELAY_MS = 420L
    }
}

private class PadView(context: Context) : View(context) {

    private val density = context.resources.displayMetrics.density
    private val margin = 10f * density
    private var padW = 48f * density
    private var padH = 128f * density

    val windowW: Int get() = (padW + margin * 2).toInt()
    val windowH: Int get() = (padH + margin * 2).toInt()
    val travel: Float get() = padH / 2f - padW * 0.36f - 4f * density

    var idleAlpha = 0.9f
        set(value) {
            field = value
            updateAlpha()
        }
    var touching = false
        set(value) {
            field = value
            updateAlpha()
            invalidate()
        }
    var moving = false
        set(value) {
            field = value
            invalidate()
        }
    var holding = false
        set(value) {
            field = value
            if (value) startPulseLoop() else invalidate()
        }
    /** null = no direction yet; true = finger moved up past the threshold; false = down. */
    var activeFingerUp: Boolean? = null
        set(value) {
            field = value
            invalidate()
        }

    private var thumb = 0f
    private var thumbAnimator: ValueAnimator? = null
    private var flashUntil = 0L
    private var flashTop = false
    private var pulseStart = 0L

    private val body = Paint(Paint.ANTI_ALIAS_FLAG)
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f * density }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val chevron = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.4f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f * density }
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val rect = RectF()
    private val path = Path()

    private val cyan = Color.rgb(61, 220, 255)
    private val violet = Color.rgb(124, 92, 255)

    fun setPadSize(w: Float, h: Float) {
        padW = w
        padH = h
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(windowW, windowH)
    }

    private fun updateAlpha() {
        alpha = if (touching || moving) 1f else idleAlpha.coerceIn(0.25f, 1f)
    }

    fun setThumb(v: Float) {
        thumbAnimator?.cancel()
        thumb = v
        invalidate()
    }

    fun releaseThumb() {
        thumbAnimator?.cancel()
        thumbAnimator = ValueAnimator.ofFloat(thumb, 0f).apply {
            duration = 260
            interpolator = OvershootInterpolator(2.2f)
            addUpdateListener {
                thumb = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun flash(dir: ScrollDirection) {
        flashTop = activeFingerUp ?: (dir == ScrollDirection.UP)
        flashUntil = SystemClock.uptimeMillis() + 320
        invalidate()
        postDelayed({ invalidate() }, 340)
    }

    fun pulse() {
        pulseStart = SystemClock.uptimeMillis()
        startPulseLoop()
    }

    private fun startPulseLoop() {
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.uptimeMillis()
        val left = margin
        val top = margin
        val cx = left + padW / 2f
        val cy = top + padH / 2f
        val radius = padW / 2f
        val active = activeFingerUp != null
        val accent = when (activeFingerUp) {
            true -> cyan
            false -> violet
            null -> Color.rgb(150, 165, 195)
        }

        // outer glow while touching
        if (touching || moving || holding) {
            val pulse = if (holding) (now % 900) / 900f else 0f
            glow.shader = RadialGradient(
                cx, cy, padH * (0.62f + 0.06f * pulse),
                intArrayOf(withAlpha(if (moving) Color.rgb(255, 181, 71) else accent, 90), withAlpha(accent, 0)),
                null, Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, padH * 0.68f, glow)
        }

        // glass capsule
        rect.set(left, top, left + padW, top + padH)
        body.shader = LinearGradient(
            0f, top, 0f, top + padH,
            intArrayOf(Color.argb(235, 26, 36, 62), Color.argb(235, 10, 14, 26)),
            null, Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(rect, radius, radius, body)
        border.shader = LinearGradient(
            left, top, left + padW, top + padH,
            intArrayOf(withAlpha(cyan, if (touching) 230 else 140), withAlpha(violet, if (touching) 230 else 140)),
            null, Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(rect, radius, radius, border)

        // centre track
        track.strokeWidth = 3f * density
        track.color = Color.argb(40, 255, 255, 255)
        canvas.drawLine(cx, top + padW * 0.62f, cx, top + padH - padW * 0.62f, track)
        if (thumb != 0f) {
            track.color = withAlpha(accent, 170)
            canvas.drawLine(cx, cy, cx, cy + thumb, track)
        }

        // chevrons
        val cw = padW * 0.2f
        val flashing = now < flashUntil
        val upLit = activeFingerUp == true || (flashing && flashTopChevron())
        val downLit = activeFingerUp == false || (flashing && !flashTopChevron())
        drawChevron(canvas, cx, top + padW * 0.42f, cw, up = true, color = if (upLit) cyan else Color.argb(150, 200, 210, 230))
        drawChevron(canvas, cx, top + padH - padW * 0.42f, cw, up = false, color = if (downLit) violet else Color.argb(150, 200, 210, 230))

        // thumb (follows the finger)
        val tr = padW * 0.3f
        val ty = cy + thumb
        thumbPaint.shader = RadialGradient(
            cx - tr * 0.3f, ty - tr * 0.3f, tr * 1.3f,
            intArrayOf(Color.WHITE, if (active) accent else Color.rgb(170, 185, 215)),
            null, Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, ty, tr, thumbPaint)
        if (holding) {
            val p = (now % 700) / 700f
            ring.color = withAlpha(accent, (200 * (1f - p)).toInt())
            canvas.drawCircle(cx, ty, tr * (1.1f + 0.5f * p), ring)
        }

        // tap feedback ring
        val sincePulse = now - pulseStart
        if (sincePulse < 450) {
            val p = sincePulse / 450f
            ring.color = withAlpha(cyan, (180 * (1f - p)).toInt())
            canvas.drawCircle(cx, cy, tr * (1f + p), ring)
        }

        if (holding || sincePulse < 450 || flashing) postInvalidateOnAnimation()
    }

    /** The lit chevron after a scroll shows where the finger went. */
    private fun flashTopChevron(): Boolean = flashTop

    private fun drawChevron(canvas: Canvas, cx: Float, cy: Float, w: Float, up: Boolean, color: Int) {
        val h = w * 0.6f
        path.reset()
        if (up) {
            path.moveTo(cx - w, cy + h / 2)
            path.lineTo(cx, cy - h / 2)
            path.lineTo(cx + w, cy + h / 2)
        } else {
            path.moveTo(cx - w, cy - h / 2)
            path.lineTo(cx, cy + h / 2)
            path.lineTo(cx + w, cy - h / 2)
        }
        chevron.color = color
        canvas.drawPath(path, chevron)
    }

    private fun withAlpha(c: Int, a: Int) = Color.argb(a.coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c))
}
