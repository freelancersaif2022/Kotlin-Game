package com.eyescroll.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import android.view.accessibility.AccessibilityWindowInfo
import com.eyescroll.app.data.SettingsStore
import com.eyescroll.app.scroll.PadBus
import com.eyescroll.app.scroll.ScrollDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Hosts the floating scroll pad and performs the scrolling:
 *  - swipe-and-release on the pad -> a smooth injected swipe gesture in the app below
 *  - swipe-and-hold -> repeated accessibility scroll actions on the main scrollable view
 *    (injected gestures can't run while a real finger is on the screen, scroll actions can)
 */
class ScrollAccessibilityService : AccessibilityService() {

    private var scope: CoroutineScope? = null
    private var pad: FloatingPad? = null
    private var holdNode: AccessibilityNodeInfo? = null
    private val bounds = Rect()

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        PadBus.update { it.copy(serviceRunning = true) }
        val store = SettingsStore.get(this)
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = s
        s.launch {
            store.settings.collect { settings ->
                if (settings.padEnabled && settings.onboardingDone) {
                    val p = pad ?: FloatingPad(this@ScrollAccessibilityService, store).also {
                        it.show()
                        pad = it
                    }
                    p.applySettings(settings)
                    PadBus.update { it.copy(padVisible = true) }
                } else {
                    pad?.hide()
                    pad = null
                    PadBus.update { it.copy(padVisible = false) }
                }
            }
        }
    }

    // ---- swipe gesture (finger already lifted) ----

    /**
     * Smooth swipe in the middle band of the screen, away from the pad, status bar and
     * gesture-navigation areas. A short hold at the end stops the list exactly there (no fling).
     */
    fun swipeScroll(direction: ScrollDirection, fraction: Float, durationMs: Long, padOnRight: Boolean): Boolean {
        val m = resources.displayMetrics
        val w = m.widthPixels.toFloat()
        val h = m.heightPixels.toFloat()
        val x = w * if (padOnRight) 0.42f else 0.58f
        val half = h * fraction.coerceIn(0.05f, 0.8f) / 2f
        val center = h * 0.5f
        val (fromY, toY) = when (direction) {
            ScrollDirection.DOWN -> (center + half) to (center - half)
            ScrollDirection.UP -> (center - half) to (center + half)
        }
        val path = Path().apply {
            moveTo(x, fromY)
            lineTo(x, toY)
        }
        val swipe = GestureDescription.StrokeDescription(path, 0, durationMs.coerceIn(80, 1000), true)
        val hold = Path().apply {
            moveTo(x, toY)
            lineTo(x, toY + 1f)
        }
        val ok = dispatchGesture(
            GestureDescription.Builder().addStroke(swipe).build(),
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    runCatching {
                        val end = swipe.continueStroke(hold, 0, 60, false)
                        dispatchGesture(GestureDescription.Builder().addStroke(end).build(), null, null)
                    }
                }
            },
            null,
        )
        if (ok) countScroll(direction)
        return ok
    }

    // ---- hold scrolling (finger still on the pad) ----

    fun beginHold() {
        holdNode = null
        PadBus.update { it.copy(holding = true) }
    }

    /** One scroll step on the main scrollable view. Returns false if this screen has none. */
    fun holdStep(direction: ScrollDirection): Boolean {
        val cached = holdNode?.takeIf { runCatching { it.refresh() }.getOrDefault(false) }
        val node = cached ?: findScrollable(direction)?.also { holdNode = it } ?: return false
        val ok = performScroll(node, direction)
        if (!ok) {
            // The cached view may be at its end or gone; look again once.
            val fresh = findScrollable(direction) ?: return false
            holdNode = fresh
            if (!performScroll(fresh, direction)) return false
        }
        countScroll(direction)
        return true
    }

    fun endHold() {
        holdNode = null
        PadBus.update { it.copy(holding = false) }
    }

    private fun performScroll(node: AccessibilityNodeInfo, direction: ScrollDirection): Boolean {
        val directional = if (direction == ScrollDirection.DOWN) AccessibilityAction.ACTION_SCROLL_DOWN else AccessibilityAction.ACTION_SCROLL_UP
        if (node.actionList.any { it.id == directional.id } && node.performAction(directional.id)) return true
        val generic = if (direction == ScrollDirection.DOWN) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return node.performAction(generic)
    }

    /** Biggest visible scrollable view, preferring vertical lists over horizontal pagers. */
    private fun findScrollable(direction: ScrollDirection): AccessibilityNodeInfo? {
        val roots = runCatching {
            windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }.mapNotNull { it.root }
        }.getOrDefault(emptyList()).ifEmpty { listOfNotNull(rootInActiveWindow) }

        val wantId = if (direction == ScrollDirection.DOWN) AccessibilityAction.ACTION_SCROLL_DOWN.id else AccessibilityAction.ACTION_SCROLL_UP.id
        val genericId = if (direction == ScrollDirection.DOWN) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        var best: AccessibilityNodeInfo? = null
        var bestScore = 0.0
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        roots.forEach { queue.addLast(it) }
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val n = queue.removeFirst()
            visited++
            if (n.isScrollable && n.isVisibleToUser) {
                n.getBoundsInScreen(bounds)
                val area = bounds.width().toDouble().coerceAtLeast(0.0) * bounds.height().toDouble().coerceAtLeast(0.0)
                val ids = n.actionList.map { it.id }
                val vertical = ids.contains(AccessibilityAction.ACTION_SCROLL_DOWN.id) || ids.contains(AccessibilityAction.ACTION_SCROLL_UP.id)
                val horizontalOnly = !vertical && (ids.contains(AccessibilityAction.ACTION_SCROLL_LEFT.id) || ids.contains(AccessibilityAction.ACTION_SCROLL_RIGHT.id))
                val canGo = ids.contains(wantId) || ids.contains(genericId)
                var score = area * when {
                    vertical -> 2.0
                    horizontalOnly -> 0.2
                    else -> 1.0
                }
                if (!canGo) score *= 0.1
                if (score > bestScore) {
                    bestScore = score
                    best = n
                }
            }
            for (i in 0 until n.childCount) n.getChild(i)?.let { queue.addLast(it) }
        }
        return best
    }

    private fun countScroll(direction: ScrollDirection) {
        PadBus.update { it.copy(scrollCount = it.scrollCount + 1, lastScroll = direction) }
    }

    fun openApp() {
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        isRunning = false
        pad?.hide()
        pad = null
        holdNode = null
        scope?.cancel()
        scope = null
        PadBus.update { it.copy(serviceRunning = false, padVisible = false, holding = false) }
    }

    companion object {
        private const val MAX_NODES = 1500

        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
