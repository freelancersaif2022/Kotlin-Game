package com.irremote.tv.screens

import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.irremote.tv.MainActivity
import com.irremote.tv.ir.IrDatabase
import com.irremote.tv.ui.Fonts
import com.irremote.tv.ui.LogoView
import com.irremote.tv.ui.MATCH
import com.irremote.tv.ui.Palette
import com.irremote.tv.ui.WRAP
import com.irremote.tv.ui.dp
import com.irremote.tv.ui.frameLp
import com.irremote.tv.ui.lp
import com.irremote.tv.ui.margins
import com.irremote.tv.ui.text
import com.irremote.tv.ui.vertical

class SplashScreen(activity: MainActivity) : Screen(activity) {
    private val handler = Handler(Looper.getMainLooper())

    override fun build(): View = FrameLayout(activity).apply {
        background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Palette.BG_TOP, Palette.BG))
        val column = activity.vertical {
            gravity = Gravity.CENTER_HORIZONTAL
            addView(LogoView(context, animated = true), lp(dp(230), dp(230)))
            addView(context.text("TV Remote IR", 30f, Palette.TEXT, Fonts.black).apply { letterSpacing = 0.01f },
                lp(WRAP, WRAP).margins(t = dp(4)))
            addView(context.text("Universal infrared remote for every TV", 15f, Palette.TEXT_2),
                lp(WRAP, WRAP).margins(t = dp(10)))
        }
        addView(column, frameLp(MATCH, WRAP, Gravity.CENTER))
        addView(context.text("NO WI-FI  •  NO PAIRING  •  WORKS INSTANTLY", 11f, Palette.TEXT_3, Fonts.bold).apply {
            letterSpacing = 0.14f
        }, frameLp(WRAP, WRAP, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(40) })

        column.alpha = 0f
        column.scaleX = 0.92f
        column.scaleY = 0.92f
        column.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(520).start()
    }

    override fun onShow() {
        if (started) return
        started = true
        val start = SystemClock.uptimeMillis()
        Thread {
            IrDatabase.get(activity) // parse the code library while the splash is showing
            activity.ir
            val wait = maxOf(0L, MIN_SPLASH_MS - (SystemClock.uptimeMillis() - start))
            handler.postDelayed({ if (!activity.isFinishing) openApp() }, wait)
        }.start()
    }

    private var started = false

    private fun openApp() {
        val home = HomeScreen(activity)
        val last = activity.store.lastUsed.takeIf { it != 0L }?.let { activity.store.find(it) }
        if (last != null) activity.reset(home, RemoteScreen(activity, last.id)) else activity.reset(home)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
    }

    private companion object {
        const val MIN_SPLASH_MS = 1400L
    }
}
