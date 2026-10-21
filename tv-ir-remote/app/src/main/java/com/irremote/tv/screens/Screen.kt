package com.irremote.tv.screens

import android.view.View
import com.irremote.tv.MainActivity
import com.irremote.tv.ui.dp

/** A lightweight view controller hosted by [MainActivity]'s stack. */
abstract class Screen(val activity: MainActivity) {
    var view: View? = null
        private set

    fun createView(): View = build().also { view = it }

    protected abstract fun build(): View

    /** Called when the screen becomes the visible top of the stack. */
    open fun onShow() {}

    /** Called when the screen stops being visible (covered, popped, or app paused). */
    open fun onHide() {}

    open fun onDestroy() {}

    /** Return true to consume the back press. */
    open fun onBack(): Boolean = false

    protected fun dp(v: Int): Int = activity.dp(v)
    protected fun dp(v: Float): Float = activity.dp(v)
}
