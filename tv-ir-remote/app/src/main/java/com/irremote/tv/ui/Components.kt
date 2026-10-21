package com.irremote.tv.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.irremote.tv.R
import kotlin.math.min

/** Screen header: optional back arrow, title + subtitle, optional trailing action icons. */
fun Context.topBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: List<Pair<Int, (View) -> Unit>> = emptyList(),
): LinearLayout = horizontal {
    setPadding(dp(8), dp(10), dp(8), dp(10))
    minimumHeight = dp(64)
    if (onBack != null) {
        addView(iconButton(R.drawable.ic_arrow_left, "Back") { onBack() })
    } else {
        addView(View(context), lp(dp(12), 1))
    }
    addView(vertical {
        setPadding(dp(4), 0, dp(4), 0)
        addView(text(title, 20f, Palette.TEXT, Fonts.bold).apply {
            maxLines = 1; ellipsize = TextUtils.TruncateAt.END; tag = "title"
        })
        if (subtitle != null) {
            addView(text(subtitle, 13f, Palette.TEXT_2).apply {
                maxLines = 1; ellipsize = TextUtils.TruncateAt.END; tag = "subtitle"
            }, lp(WRAP, WRAP).margins(t = dp(3)))
        }
    }, lp(0, WRAP, 1f))
    for ((res, action) in actions) addView(iconButton(res, null, action))
}

fun Context.iconButton(res: Int, description: String?, onClick: (View) -> Unit): View =
    FrameLayout(this).apply {
        background = ripple(roundRect(0x00000000, dp(22f)), 0x22FFFFFF)
        contentDescription = description
        isClickable = true
        setOnClickListener(onClick)
        addView(icon(res, Palette.TEXT, 24).apply { layoutParams = frameLp(dp(24), dp(24), Gravity.CENTER) })
        layoutParams = lp(dp(44), dp(44))
    }

/** Big gradient call-to-action button. */
fun Context.primaryButton(label: String, iconRes: Int = 0, onClick: () -> Unit): View = horizontal {
    gravity = Gravity.CENTER
    background = ripple(gradient(dp(18f), Palette.ACCENT, Palette.ACCENT_2, orientation = GradientDrawable.Orientation.LEFT_RIGHT), 0x44FFFFFF)
    elevation = dp(6f)
    isClickable = true
    setOnClickListener { onClick() }
    pressScale()
    if (iconRes != 0) addView(icon(iconRes, 0xFFFFFFFF.toInt(), 22), lp(dp(22), dp(22)).margins(r = dp(10)))
    addView(text(label, 16f, 0xFFFFFFFF.toInt(), Fonts.bold))
    layoutParams = lp(MATCH, dp(56))
}

fun Context.secondaryButton(label: String, iconRes: Int = 0, color: Int = Palette.TEXT, onClick: () -> Unit): View = horizontal {
    gravity = Gravity.CENTER
    background = ripple(roundRect(Palette.SURFACE_2, dp(18f), Palette.STROKE, dp(1)))
    isClickable = true
    setOnClickListener { onClick() }
    pressScale()
    if (iconRes != 0) addView(icon(iconRes, color, 20), lp(dp(20), dp(20)).margins(r = dp(8)))
    addView(text(label, 15f, color, Fonts.medium))
    layoutParams = lp(MATCH, dp(52))
}

fun Context.card(radius: Float = dp(22f)): LinearLayout = vertical {
    background = roundRect(Palette.SURFACE, radius, Palette.STROKE, dp(1))
    setPadding(dp(18), dp(18), dp(18), dp(18))
}

fun Context.sectionTitle(value: String): TextView = text(value.uppercase(), 12f, Palette.TEXT_3, Fonts.bold).apply {
    letterSpacing = 0.12f
}

/** A rounded square badge showing a brand's initial on a colour derived from its name. */
fun Context.brandBadge(name: String, sizeDp: Int = 46): TextView =
    text("", sizeDp * 0.42f, 0xFFFFFFFF.toInt(), Fonts.black).apply {
        gravity = Gravity.CENTER
        layoutParams = lp(dp(sizeDp), dp(sizeDp))
        bindBadge(this, name, sizeDp)
    }

fun Context.bindBadge(view: TextView, name: String, sizeDp: Int) {
    val hues = intArrayOf(
        0xFF4C7DFF.toInt(), 0xFF7B4DFF.toInt(), 0xFF00B3A4.toInt(), 0xFFFF7A45.toInt(),
        0xFFE94B8A.toInt(), 0xFF2FB36B.toInt(), 0xFFE0A100.toInt(), 0xFF3AA0FF.toInt(),
    )
    val c = hues[(name.hashCode() and 0x7FFFFFFF) % hues.size]
    view.text = name.firstOrNull()?.uppercase() ?: "?"
    view.background = gradient(dp(sizeDp * 0.32f), c, blend(c, 0xFF000000.toInt(), 0.35f))
}

/** Circular progress indicator with a gradient sweep, used by Auto Search. */
class ProgressRing(context: Context) : View(context) {
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Palette.SURFACE_3 }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val pulse = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val oval = RectF()
    private var pulseT = 0f
    private val pulseAnim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1400
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { pulseT = it.animatedValue as Float; invalidate() }
    }

    var progress = 0f
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }

    var running = false
        set(v) {
            field = v
            if (v) pulseAnim.start() else { pulseAnim.cancel(); pulseT = 0f; invalidate() }
        }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnim.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        val stroke = dp(12f)
        val r = min(width, height) / 2f - stroke - dp(14f)
        val cx = width / 2f
        val cy = height / 2f
        if (running) {
            pulse.strokeWidth = dp(2f)
            pulse.color = withAlpha(Palette.ACCENT, (110 * (1f - pulseT)).toInt())
            canvas.drawCircle(cx, cy, r + stroke / 2 + dp(14f) * pulseT, pulse)
        }
        track.strokeWidth = stroke
        arc.strokeWidth = stroke
        canvas.drawCircle(cx, cy, r, track)
        arc.shader = SweepGradient(cx, cy, intArrayOf(Palette.ACCENT, Palette.ACCENT_2, Palette.ACCENT), null)
        oval.set(cx - r, cy - r, cx + r, cy + r)
        val save = canvas.save()
        canvas.rotate(-90f, cx, cy)
        if (progress > 0f) canvas.drawArc(oval, 0f, 360f * progress, false, arc)
        canvas.restoreToCount(save)
    }
}

/** App logo drawn from the launcher artwork, with optional animated IR pulses for the splash screen. */
class LogoView(context: Context, private val animated: Boolean) : View(context) {
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val fg = context.getDrawable(R.drawable.ic_launcher_foreground)!!
    private val rect = RectF()
    private var t = 0f
    private val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1600
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { t = it.animatedValue as Float; invalidate() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (animated) anim.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        anim.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        val size = min(width, height) * (if (animated) 0.56f else 1f)
        val cx = width / 2f
        val cy = height / 2f
        if (animated) {
            ring.strokeWidth = dp(2f)
            for (i in 0..1) {
                val p = (t + i * 0.5f) % 1f
                ring.color = withAlpha(Palette.ACCENT, (120 * (1f - p)).toInt())
                canvas.drawCircle(cx, cy, size * 0.55f + (min(width, height) / 2f - size * 0.55f) * p, ring)
            }
        }
        rect.set(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2)
        bg.shader = LinearGradient(rect.left, rect.bottom, rect.right, rect.top, 0xFF3D7BFF.toInt(), 0xFF7B4DFF.toInt(), Shader.TileMode.CLAMP)
        val r = size * 0.27f
        canvas.drawRoundRect(rect, r, r, bg)
        // the 108-unit adaptive-icon artwork, with the 90-unit visible area mapped onto the badge
        val scale = size / 90f
        val left = (cx - 54 * scale).toInt()
        val top = (cy - 54 * scale).toInt()
        fg.setBounds(left, top, left + (108 * scale).toInt(), top + (108 * scale).toInt())
        fg.draw(canvas)
    }
}
