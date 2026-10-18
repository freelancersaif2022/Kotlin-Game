package com.smartisland.app.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.smartisland.app.system.systemService

/**
 * A wrap-content accessibility overlay window that hosts Compose content. Accessibility overlays draw
 * above the status bar and need no "draw over other apps" permission.
 */
class OverlayWindow(
    private val context: Context,
    private val gravity: Int,
    private val onOutsideTouch: () -> Unit = {},
    private val content: @Composable () -> Unit,
) {
    private val wm = context.systemService<WindowManager>()
    private var lifecycle: OverlayLifecycleOwner? = null
    private var root: FrameLayout? = null

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT,
    ).apply {
        this.gravity = this@OverlayWindow.gravity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            fitInsetsTypes = 0
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        title = "SmartIslandOverlay"
    }

    val isShowing: Boolean get() = root != null

    fun show() {
        if (root != null) return
        val owner = OverlayLifecycleOwner().also { it.start() }
        val frame = OutsideTouchFrame(context, onOutsideTouch)
        val compose = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent(content)
        }
        frame.addView(compose)
        owner.attachTo(frame)
        if (runCatching { wm.addView(frame, params) }.isSuccess) {
            root = frame
            lifecycle = owner
        } else {
            owner.destroy()
        }
    }

    fun hide() {
        root?.let { runCatching { wm.removeViewImmediate(it) } }
        root = null
        lifecycle?.destroy()
        lifecycle = null
    }

    fun setVisible(visible: Boolean) {
        root?.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
    }

    fun move(x: Int, y: Int) {
        if (params.x == x && params.y == y) return
        params.x = x
        params.y = y
        root?.let { runCatching { wm.updateViewLayout(it, params) } }
    }

    @SuppressLint("ViewConstructor")
    private class OutsideTouchFrame(context: Context, private val onOutside: () -> Unit) : FrameLayout(context) {
        override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
            if (ev.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                onOutside()
                return false
            }
            return super.dispatchTouchEvent(ev)
        }
    }
}
