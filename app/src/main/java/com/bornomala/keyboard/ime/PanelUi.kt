package com.bornomala.keyboard.ime

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bornomala.keyboard.theme.KeyboardTheme

/** Small helpers that build themed buttons for the emoji, clipboard and editing panels. */
object PanelUi {

    fun keyBackground(context: Context, t: KeyboardTheme, color: Int = t.functionalKey): Drawable {
        val d = context.resources.displayMetrics.density
        val shape = GradientDrawable().apply {
            setColor(color)
            cornerRadius = t.cornerRadiusDp * d
            if (t.borders) setStroke(d.toInt().coerceAtLeast(1), t.keyBorder)
        }
        return RippleDrawable(ColorStateList.valueOf(t.keyPressed), shape, null)
    }

    fun iconButton(context: Context, icon: Int, description: String, t: KeyboardTheme, onClick: () -> Unit): ImageView {
        val d = context.resources.displayMetrics.density
        return ImageView(context).apply {
            setImageResource(icon)
            contentDescription = description
            setColorFilter(t.keyText)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            val pad = (10 * d).toInt()
            setPadding(pad, pad, pad, pad)
            background = keyBackground(context, t)
            setOnClickListener { onClick() }
        }
    }

    fun textButton(context: Context, text: String, t: KeyboardTheme, onClick: () -> Unit): TextView =
        TextView(context).apply {
            this.text = text
            gravity = Gravity.CENTER
            setTextColor(t.keyText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            background = keyBackground(context, t)
            setOnClickListener { onClick() }
        }

    /** A cell with an icon above a short label, used by the text editing panel. */
    fun labeledButton(context: Context, icon: Int, label: String, t: KeyboardTheme, color: Int, onClick: () -> Unit): LinearLayout {
        val d = context.resources.displayMetrics.density
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = keyBackground(context, t, color)
            contentDescription = label
            isClickable = true
            setOnClickListener { onClick() }
            addView(ImageView(context).apply {
                setImageResource(icon)
                setColorFilter(t.keyText)
            }, LinearLayout.LayoutParams((24 * d).toInt(), (24 * d).toInt()))
            if (label.isNotEmpty()) {
                addView(TextView(context).apply {
                    text = label
                    setTextColor(t.keyHint)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    gravity = Gravity.CENTER
                })
            }
        }
    }

    fun margins(context: Context, view: View, dp: Float) {
        val px = (dp * context.resources.displayMetrics.density).toInt()
        (view.layoutParams as? LinearLayout.LayoutParams)?.setMargins(px, px, px, px)
    }

    /** Repeats [action] while the view is held down (used for delete buttons). */
    fun repeatOnHold(view: View, action: () -> Unit) {
        val handler = Handler(Looper.getMainLooper())
        val repeat = object : Runnable {
            override fun run() {
                action()
                handler.postDelayed(this, 60)
            }
        }
        view.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    action()
                    handler.postDelayed(repeat, 400)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    handler.removeCallbacks(repeat)
                }
            }
            true
        }
    }
}
