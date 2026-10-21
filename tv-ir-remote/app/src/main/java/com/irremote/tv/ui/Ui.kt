package com.irremote.tv.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** Design tokens shared by every screen. */
object Palette {
    const val BG = 0xFF0A0C11.toInt()
    const val BG_TOP = 0xFF121726.toInt()
    const val SURFACE = 0xFF151922.toInt()
    const val SURFACE_2 = 0xFF1C212C.toInt()
    const val SURFACE_3 = 0xFF252B38.toInt()
    const val STROKE = 0xFF2A3141.toInt()
    const val TEXT = 0xFFF3F5FA.toInt()
    const val TEXT_2 = 0xFFA2ABBC.toInt()
    const val TEXT_3 = 0xFF6B7488.toInt()
    const val ACCENT = 0xFF4C7DFF.toInt()
    const val ACCENT_2 = 0xFF7B4DFF.toInt()
    const val POWER = 0xFFFF3D57.toInt()
    const val POWER_2 = 0xFFD8203D.toInt()
    const val SUCCESS = 0xFF2FD47A.toInt()
    const val WARNING = 0xFFFFB547.toInt()
    const val RIPPLE = 0x33FFFFFF
}

object Fonts {
    val regular: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    val medium: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    val bold: Typeface = Typeface.create("sans-serif", Typeface.BOLD)
    val black: Typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
    val light: Typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
}

fun Context.dp(v: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)
fun Context.dp(v: Int): Int = dp(v.toFloat()).toInt()
fun View.dp(v: Int): Int = context.dp(v)
fun View.dp(v: Float): Float = context.dp(v)

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

fun lp(w: Int, h: Int, weight: Float = 0f) = LinearLayout.LayoutParams(w, h, weight)

fun LinearLayout.LayoutParams.margins(l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0) = apply { setMargins(l, t, r, b) }

fun frameLp(w: Int, h: Int, gravity: Int = Gravity.NO_GRAVITY) = FrameLayout.LayoutParams(w, h, gravity)

fun roundRect(color: Int, radius: Float, strokeColor: Int = 0, strokeWidth: Int = 0): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        if (strokeWidth > 0) setStroke(strokeWidth, strokeColor)
    }

fun gradient(radius: Float, vararg colors: Int, orientation: GradientDrawable.Orientation = GradientDrawable.Orientation.TL_BR) =
    GradientDrawable(orientation, colors).apply { cornerRadius = radius }

/** Background with a touch ripple, clipped to [base]'s shape. */
fun ripple(base: Drawable, rippleColor: Int = Palette.RIPPLE): Drawable =
    RippleDrawable(ColorStateList.valueOf(rippleColor), base, base.constantState?.newDrawable()?.mutate() ?: base)

fun Context.text(
    value: CharSequence,
    size: Float,
    color: Int = Palette.TEXT,
    font: Typeface = Fonts.regular,
): TextView = TextView(this).apply {
    text = value
    setTextColor(color)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    typeface = font
    includeFontPadding = false
}

fun Context.icon(res: Int, tint: Int = Palette.TEXT, sizeDp: Int = 24): ImageView = ImageView(this).apply {
    setImageDrawable(tinted(res, tint))
    scaleType = ImageView.ScaleType.FIT_CENTER
    layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
}

fun Context.tinted(res: Int, tint: Int): Drawable = getDrawable(res)!!.mutate().apply { setTint(tint) }

fun Context.vertical(block: LinearLayout.() -> Unit = {}): LinearLayout =
    LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; block() }

fun Context.horizontal(block: LinearLayout.() -> Unit = {}): LinearLayout =
    LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; block() }

fun View.haptic(enabled: Boolean) {
    if (!enabled) return
    val type = if (Build.VERSION.SDK_INT >= 23) HapticFeedbackConstants.CONTEXT_CLICK else HapticFeedbackConstants.VIRTUAL_KEY
    performHapticFeedback(type, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
}

/** Press-down scale feedback used by cards and pill buttons. */
fun View.pressScale(scale: Float = 0.97f) {
    setOnTouchListener { v, e ->
        when (e.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> v.animate().scaleX(scale).scaleY(scale).setDuration(90).start()
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
        }
        false
    }
}

fun withAlpha(color: Int, alpha: Int): Int = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

fun blend(a: Int, b: Int, t: Float): Int {
    val u = 1f - t
    return Color.argb(
        (Color.alpha(a) * u + Color.alpha(b) * t).toInt(),
        (Color.red(a) * u + Color.red(b) * t).toInt(),
        (Color.green(a) * u + Color.green(b) * t).toInt(),
        (Color.blue(a) * u + Color.blue(b) * t).toInt(),
    )
}
