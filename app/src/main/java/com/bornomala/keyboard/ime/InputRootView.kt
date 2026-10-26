package com.bornomala.keyboard.ime

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.FrameLayout

/**
 * Root of the keyboard window. Its height is exactly the height of [content] (suggestion strip +
 * keys + navigation inset); the background and popup layers are then sized to match.
 *
 * A plain FrameLayout would let MATCH_PARENT layers (background image, popup overlay) grow to
 * the full screen height, which pushes the keys to the top of the screen on many phones.
 */
@SuppressLint("ViewConstructor")
class InputRootView(context: Context, private val content: View) : FrameLayout(context) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        content.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )
        val height = content.measuredHeight
        val exactW = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val exactH = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child !== content && child.visibility != GONE) child.measure(exactW, exactH)
        }
        setMeasuredDimension(width, height)
    }
}
