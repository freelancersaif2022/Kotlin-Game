package com.eyescroll.app.service

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.eyescroll.app.data.SettingsStore
import com.eyescroll.app.tracking.GazeZone
import com.eyescroll.app.tracking.ScrollDirection
import com.eyescroll.app.tracking.TrackingPhase
import com.eyescroll.app.tracking.TrackingStatus
import kotlin.math.hypot

/**
 * The floating on/off dot. Tap = camera on/off, drag = move (snaps to the screen edge),
 * long-press = open EyeScroll. While tracking, the little eye inside follows your gaze and a ring
 * fills up just before a scroll fires.
 */
class FloatingBubble(private val service: AccessibilityService, private val store: SettingsStore) {

    var onTap: () -> Unit = {}
    var onLongPress: () -> Unit = {}

    private val wm = service.getSystemService(WindowManager::class.java)
    private val view = BubbleView(service)
    private val sizePx = dp(service, 64f).toInt()
    private val params = WindowManager.LayoutParams(
        sizePx,
        sizePx,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }
    private var attached = false
    private var snapAnimator: ValueAnimator? = null

    fun show() {
        if (attached) return
        val (w, h) = screen()
        val s = store.current
        params.x = if (s.bubbleX >= 0) s.bubbleX.coerceIn(0, w - sizePx) else w - sizePx
        params.y = if (s.bubbleY >= 0) s.bubbleY.coerceIn(0, h - sizePx) else (h * 0.38f).toInt()
        setupTouch()
        try {
            wm.addView(view, params)
            attached = true
            view.start()
        } catch (_: Exception) {
        }
    }

    fun hide() {
        snapAnimator?.cancel()
        view.stop()
        if (attached) {
            runCatching { wm.removeView(view) }
            attached = false
        }
    }

    fun render(status: TrackingStatus) = view.setStatus(status)

    private fun screen(): Pair<Int, Int> {
        val m = service.resources.displayMetrics
        return m.widthPixels to m.heightPixels
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouch() {
        val slop = ViewConfiguration.get(service).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        var longPressed = false
        val longPress = Runnable {
            longPressed = true
            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            onLongPress()
        }

        view.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    snapAnimator?.cancel()
                    downX = e.rawX
                    downY = e.rawY
                    startX = params.x
                    startY = params.y
                    dragging = false
                    longPressed = false
                    view.setPressedState(true)
                    view.postDelayed(longPress, 550)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (!dragging && hypot(dx, dy) > slop) {
                        dragging = true
                        view.removeCallbacks(longPress)
                    }
                    if (dragging) {
                        val (w, h) = screen()
                        params.x = (startX + dx).toInt().coerceIn(0, w - sizePx)
                        params.y = (startY + dy).toInt().coerceIn(0, h - sizePx)
                        if (attached) wm.updateViewLayout(view, params)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    view.removeCallbacks(longPress)
                    view.setPressedState(false)
                    if (dragging) {
                        snapToEdge()
                    } else if (!longPressed) {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        onTap()
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    view.removeCallbacks(longPress)
                    view.setPressedState(false)
                }
            }
            true
        }
    }

    private fun snapToEdge() {
        val (w, _) = screen()
        val target = if (params.x + sizePx / 2 < w / 2) 0 else w - sizePx
        snapAnimator = ValueAnimator.ofInt(params.x, target).apply {
            duration = 260
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener {
                params.x = it.animatedValue as Int
                if (attached) runCatching { wm.updateViewLayout(view, params) }
            }
            start()
        }
        store.update { it.copy(bubbleX = target, bubbleY = params.y) }
    }

    companion object {
        fun dp(context: Context, v: Float): Float = v * context.resources.displayMetrics.density
    }
}

private class BubbleView(context: Context) : View(context) {

    private val density = context.resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f * density }
    private val progress = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        strokeCap = Paint.Cap.ROUND
    }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val eyeStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * density
        strokeJoin = Paint.Join.ROUND
    }
    private val eyeFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.6f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val eyePath = Path()
    private val arrowPath = Path()
    private val arc = RectF()

    private var status = TrackingStatus()
    private var pulse = 0f
    private var pressedScale = 1f
    private var irisOffset = 0f
    private var flashUntil = 0L
    private var flashDir: ScrollDirection? = null
    private var lastCount = 0
    private var animator: ValueAnimator? = null

    fun start() {
        if (animator != null) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1600
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                pulse = it.animatedValue as Float
                if (status.isActive || SystemClock.uptimeMillis() < flashUntil) invalidate()
            }
            start()
        }
    }

    fun stop() {
        animator?.cancel()
        animator = null
    }

    fun setPressedState(pressed: Boolean) {
        pressedScale = if (pressed) 0.9f else 1f
        invalidate()
    }

    fun setStatus(s: TrackingStatus) {
        if (s.scrollCount != lastCount) {
            if (s.scrollCount > lastCount && s.lastScroll != null) {
                flashDir = s.lastScroll
                flashUntil = SystemClock.uptimeMillis() + 420
            }
            lastCount = s.scrollCount
        }
        val target = if (s.phase == TrackingPhase.TRACKING) (s.gaze.gazeY - 0.5f).coerceIn(-0.7f, 0.7f) else 0f
        irisOffset += (target - irisOffset) * 0.35f
        status = s
        invalidate()
    }

    private fun accent(): Int = when (status.phase) {
        TrackingPhase.OFF -> Color.rgb(138, 151, 178)
        TrackingPhase.STARTING -> Color.rgb(255, 181, 71)
        TrackingPhase.TRACKING -> Color.rgb(61, 220, 255)
        TrackingPhase.NO_FACE -> Color.rgb(255, 138, 61)
        TrackingPhase.ERROR -> Color.rgb(255, 92, 122)
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = 22f * density * pressedScale
        val color = accent()
        val active = status.isActive

        if (active) {
            val gr = r * (1.18f + 0.22f * pulse)
            glow.shader = RadialGradient(
                cx, cy, gr,
                intArrayOf(withAlpha(color, (110 * (1f - pulse)).toInt()), withAlpha(color, 0)),
                null, Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, gr, glow)
        }

        fill.shader = RadialGradient(
            cx, cy - r * 0.4f, r * 1.4f,
            intArrayOf(Color.argb(240, 28, 38, 64), Color.argb(240, 9, 13, 24)),
            null, Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, r, fill)
        ring.color = withAlpha(color, if (active) 230 else 120)
        canvas.drawCircle(cx, cy, r, ring)

        // dwell progress ring
        val p = status.gaze.dwellProgress
        if (active && p > 0.02f && (status.gaze.zone == GazeZone.UP || status.gaze.zone == GazeZone.DOWN)) {
            progress.color = if (status.gaze.zone == GazeZone.UP) Color.rgb(143, 240, 255) else Color.rgb(164, 140, 255)
            val pr = r + 4f * density
            arc.set(cx - pr, cy - pr, cx + pr, cy + pr)
            canvas.drawArc(arc, -90f, 360f * p, false, progress)
        }

        val now = SystemClock.uptimeMillis()
        val dir = flashDir
        if (dir != null && now < flashUntil) {
            drawArrow(canvas, cx, cy, r, dir, color)
            return
        }

        // eye glyph
        val ew = r * 0.62f
        val eh = r * 0.42f
        eyePath.reset()
        eyePath.moveTo(cx - ew, cy)
        eyePath.quadTo(cx, cy - eh * 1.6f, cx + ew, cy)
        eyePath.quadTo(cx, cy + eh * 1.6f, cx - ew, cy)
        eyePath.close()
        eyeStroke.color = if (active) Color.WHITE else Color.rgb(170, 180, 200)
        canvas.drawPath(eyePath, eyeStroke)
        canvas.save()
        canvas.clipPath(eyePath)
        eyeFill.color = color
        val irisY = cy + irisOffset * eh * 1.1f
        canvas.drawCircle(cx, irisY, eh * 0.78f, eyeFill)
        eyeFill.color = Color.rgb(6, 9, 18)
        canvas.drawCircle(cx, irisY, eh * 0.34f, eyeFill)
        canvas.restore()

        if (!active) {
            // small "off" slash
            eyeStroke.color = withAlpha(Color.rgb(170, 180, 200), 200)
            canvas.drawLine(cx - ew * 0.8f, cy + eh * 1.1f, cx + ew * 0.8f, cy - eh * 1.1f, eyeStroke)
        }
    }

    private fun drawArrow(canvas: Canvas, cx: Float, cy: Float, r: Float, dir: ScrollDirection, color: Int) {
        val s = r * 0.42f
        arrowPath.reset()
        if (dir == ScrollDirection.UP) {
            arrowPath.moveTo(cx - s, cy + s * 0.45f)
            arrowPath.lineTo(cx, cy - s * 0.55f)
            arrowPath.lineTo(cx + s, cy + s * 0.45f)
        } else {
            arrowPath.moveTo(cx - s, cy - s * 0.45f)
            arrowPath.lineTo(cx, cy + s * 0.55f)
            arrowPath.lineTo(cx + s, cy - s * 0.45f)
        }
        arrow.color = color
        canvas.drawPath(arrowPath, arrow)
    }

    private fun withAlpha(c: Int, a: Int) = Color.argb(a.coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c))
}
