package com.smartisland.app.overlay

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.smartisland.app.MainActivity
import com.smartisland.app.data.IslandSettings
import com.smartisland.app.island.IslandEngine
import com.smartisland.app.island.MediaCommands
import com.smartisland.app.island.ui.IslandActions
import com.smartisland.app.island.ui.IslandOuterPadding
import com.smartisland.app.island.ui.IslandView
import com.smartisland.app.system.PendingIntents
import com.smartisland.app.system.systemService
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.StateFlow

/**
 * The always-on island, built from two windows:
 *
 * - a non-touchable window that draws the island. It is always as wide as the fully expanded island,
 *   so the island grows, shrinks and morphs inside it without the window ever being resized or
 *   re-centred (resizing is what made it jitter sideways);
 * - a small invisible window that covers only the island and forwards touches to it, so the rest of
 *   the status bar stays fully usable (e.g. pulling down the notification shade).
 */
class IslandOverlay(
    private val context: Context,
    private val settings: StateFlow<IslandSettings>,
) {
    private val wm = context.systemService<WindowManager>()
    private val expanded = mutableStateOf(false)
    private val density = context.resources.displayMetrics.density

    private var lifecycle: OverlayLifecycleOwner? = null
    private var visualRoot: FrameLayout? = null
    private var composeView: ComposeView? = null
    private var touchView: View? = null
    private var visible = true

    /** Island bounds inside the visual window, in px. */
    private var footprint = android.graphics.Rect()

    private val actions = object : IslandActions {
        override val media: MediaCommands? get() = IslandEngine.mediaCommands
        override fun send(intent: PendingIntent?) {
            PendingIntents.send(context, intent)
            expanded.value = false
        }
        override fun dismissTransient() = IslandEngine.dismissTransient()
        override fun openApp() {
            runCatching {
                context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    private val visualParams = baseParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
    ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL }

    private val touchParams = baseParams(
        1,
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
    ).apply { height = 1 }

    private fun baseParams(width: Int, extraFlags: Int) = WindowManager.LayoutParams(
        width,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            extraFlags,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            fitInsetsTypes = 0
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        title = "SmartIsland"
    }

    @Composable
    private fun Content() {
        val s by settings.collectAsState()
        val activity by IslandEngine.current.collectAsState()
        // A new kind of activity always starts collapsed; updates of the same kind keep the state.
        LaunchedEffect(activity::class) { expanded.value = false }
        val islandMax = islandMaxWidth(s, LocalConfiguration.current.screenWidthDp.toFloat()).dp
        // Fixed-width box: the window keeps this width whatever the island is doing.
        Box(Modifier.width(islandMax + IslandOuterPadding * 2), contentAlignment = Alignment.TopCenter) {
            IslandView(
                activity = activity,
                settings = s,
                expanded = expanded.value,
                onExpandedChange = { expanded.value = it },
                actions = actions,
                maxWidth = islandMax,
                onFootprint = ::onFootprint,
            )
        }
    }

    val isShowing: Boolean get() = visualRoot != null

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (visualRoot != null) return
        val owner = OverlayLifecycleOwner().also { it.start() }
        val frame = FrameLayout(context)
        val compose = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent { Content() }
        }
        frame.addView(compose)
        owner.attachTo(frame)
        if (runCatching { wm.addView(frame, visualParams) }.isFailure) {
            owner.destroy()
            return
        }
        visualRoot = frame
        composeView = compose
        lifecycle = owner

        val touch = View(context)
        touch.setOnTouchListener { _, ev ->
            if (ev.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                expanded.value = false
                return@setOnTouchListener false
            }
            // Re-base the event into the visual window and let Compose handle it as if touched directly.
            val dx = footprint.left.toFloat()
            val dy = footprint.top.toFloat()
            ev.offsetLocation(dx, dy)
            runCatching { composeView?.dispatchTouchEvent(ev) }
            ev.offsetLocation(-dx, -dy)
            true
        }
        if (runCatching { wm.addView(touch, touchParams) }.isSuccess) {
            touchView = touch
            syncTouchWindow()
        } else {
            // Fallback: let the drawing window take touches itself.
            visualParams.flags = visualParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            runCatching { wm.updateViewLayout(frame, visualParams) }
        }
    }

    fun hide() {
        visualRoot?.let { runCatching { wm.removeViewImmediate(it) } }
        touchView?.let { runCatching { wm.removeViewImmediate(it) } }
        visualRoot = null
        composeView = null
        touchView = null
        lifecycle?.destroy()
        lifecycle = null
    }

    fun setVisible(show: Boolean) {
        visible = show
        visualRoot?.visibility = if (show) View.VISIBLE else View.GONE
        syncTouchWindow()
    }

    fun applyGeometry(s: IslandSettings) {
        // Same placement as the classic single-window island: centred, nudged by the offsets.
        val x = px(s.xOffsetDp)
        val y = px(s.topOffsetDp - IslandOuterPadding.value)
        if (visualParams.x == x && visualParams.y == y) return
        visualParams.x = x
        visualParams.y = y
        visualRoot?.let { runCatching { wm.updateViewLayout(it, visualParams) } }
        visualRoot?.post { syncTouchWindow() }
    }

    private fun islandMaxWidth(s: IslandSettings, screenWidthDp: Float): Float =
        s.expandedWidthDp.coerceAtMost(screenWidthDp - 16f)

    private fun onFootprint(bounds: Rect) {
        val r = android.graphics.Rect(
            bounds.left.roundToInt(), bounds.top.roundToInt(), bounds.right.roundToInt(), bounds.bottom.roundToInt(),
        )
        if (r == footprint) return
        footprint = r
        // Wait for the window to be positioned, then follow it on screen.
        visualRoot?.post { syncTouchWindow() } ?: syncTouchWindow()
    }

    /** Moves the invisible touch window over the island (or disables it while hidden). */
    private fun syncTouchWindow() {
        val touch = touchView ?: return
        val active = visible && !footprint.isEmpty
        val flags = if (active) {
            touchParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            touchParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        // Real on-screen position of the drawing window, so the touch area matches even if the system
        // clamps or shifts the window.
        val loc = IntArray(2)
        visualRoot?.getLocationOnScreen(loc)
        val x = loc[0] + footprint.left
        val y = loc[1] + footprint.top
        val w = footprint.width().coerceAtLeast(1)
        val h = footprint.height().coerceAtLeast(1)
        if (touchParams.flags == flags && touchParams.x == x && touchParams.y == y && touchParams.width == w && touchParams.height == h) return
        touchParams.flags = flags
        touchParams.x = x
        touchParams.y = y
        touchParams.width = w
        touchParams.height = h
        runCatching { wm.updateViewLayout(touch, touchParams) }
    }

    private fun px(dp: Float): Int = (dp * density).roundToInt()
}
