package com.bornomala.keyboard.ime

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.bornomala.keyboard.R
import com.bornomala.keyboard.layout.Key
import com.bornomala.keyboard.layout.KeyCode
import com.bornomala.keyboard.layout.KeyboardLayout
import com.bornomala.keyboard.theme.KeyboardTheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Canvas-drawn keyboard. Everything is drawn directly (no child views) so typing stays fast and
 * smooth on low-end phones. Supports multi-touch rollover, sliding between keys, long-press
 * popups, key repeat, space-bar cursor swipe and backspace swipe-to-delete-words.
 */
@SuppressLint("ViewConstructor")
class KeyboardView(context: Context, private val overlay: KeyPopupOverlay) : View(context) {

    interface Listener {
        fun onKeyDown(key: Key)
        fun onKey(key: Key, popupText: String?)
        /** Returns true if the long press was consumed (no key output on release). */
        fun onLongPress(key: Key): Boolean
        fun onCursorMove(steps: Int)
        fun onDeleteWords(count: Int)
    }

    var listener: Listener? = null

    private val density = resources.displayMetrics.density
    private val handler = Handler(Looper.getMainLooper())

    var theme: KeyboardTheme? = null
        set(value) {
            field = value
            iconCache.clear()
            invalidate()
        }

    var layout: KeyboardLayout? = null
        set(value) {
            field = value
            cancelAllPointers()
            requestLayout()
            layoutKeys()
            invalidate()
        }

    /** 0 = off, 1 = shifted once, 2 = caps lock. */
    var shiftState = 0
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var enterIcon = R.drawable.ic_key_enter
        set(value) {
            field = value
            invalidate()
        }

    var spaceLabel = ""
        set(value) {
            field = value
            invalidate()
        }

    var rowHeightPx = (54 * density).toInt()
    var showHints = true
    var showPreview = true
    var longPressDelay = 300L
    var spaceSwipe = true
    var deleteSwipe = true

    private val keyRect = RectF()
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.RIGHT }
    private val iconCache = HashMap<Long, Drawable>()

    private val gapX = 3 * density
    private val gapY = 4.5f * density
    private val touchSlop = 10 * density
    private val swipeStep = 14 * density
    private val deleteWordStep = 36 * density

    // ---------------------------------------------------------------- layout

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val rows = layout?.rows?.size ?: 4
        setMeasuredDimension(w, rows * rowHeightPx + paddingTop + paddingBottom)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        layoutKeys()
    }

    private fun layoutKeys() {
        val l = layout ?: return
        val width = width.toFloat()
        if (width <= 0f) return
        var y = paddingTop.toFloat()
        for (row in l.rows) {
            val units = row.leftPad + row.rightPad + row.keys.sumOf { it.width.toDouble() }.toFloat()
            val unit = width / units
            var x = row.leftPad * unit
            for ((i, key) in row.keys.withIndex()) {
                key.x = x
                key.y = y
                key.w = key.width * unit
                key.h = rowHeightPx.toFloat()
                key.vx = key.x
                key.vw = key.w
                // Edge keys absorb the row padding so taps near the screen edge still hit them.
                if (i == 0 && row.leftPad > 0) {
                    key.x = 0f
                    key.w += row.leftPad * unit
                }
                if (i == row.keys.lastIndex && row.rightPad > 0) key.w += row.rightPad * unit
                x += key.width * unit
            }
            y += rowHeightPx
        }
    }

    // ---------------------------------------------------------------- drawing

    private fun icon(res: Int, color: Int): Drawable? {
        val cacheKey = (res.toLong() shl 32) or (color.toLong() and 0xFFFFFFFFL)
        return iconCache.getOrPut(cacheKey) {
            val d = ContextCompat.getDrawable(context, res)?.mutate() ?: return null
            d.setTint(color)
            d
        }
    }

    private fun isShifted() = shiftState > 0

    private fun labelFor(key: Key): String = when (key.code) {
        KeyCode.TEXT -> displayLabel(if (isShifted()) key.shiftLabel else key.label)
        KeyCode.SPACE -> spaceLabel
        else -> key.label
    }

    override fun onDraw(canvas: Canvas) {
        val l = layout ?: return
        val t = theme ?: return
        val radius = t.cornerRadiusDp * density
        borderPaint.color = t.keyBorder
        borderPaint.strokeWidth = density
        shadowPaint.color = if (t.dark) 0x40000000 else 0x22000000
        val pressedKeys = pointers.values.mapNotNullTo(HashSet()) { if (it.mode != Mode.CONSUMED) it.key else null }

        for (row in l.rows) {
            for (key in row.keys) {
                keyRect.set(key.vx + gapX, key.y + gapY / 2, key.vx + key.vw - gapX, key.y + key.h - gapY / 2)

                val pressed = key in pressedKeys
                val isEnter = key.code == KeyCode.ENTER
                val shiftActive = key.code == KeyCode.SHIFT && shiftState > 0
                keyPaint.color = when {
                    pressed -> t.keyPressed
                    isEnter -> t.accent
                    shiftActive -> t.keyPressed
                    key.functional -> t.functionalKey
                    else -> t.key
                }
                if (!t.borders) {
                    keyRect.offset(0f, density)
                    canvas.drawRoundRect(keyRect, radius, radius, shadowPaint)
                    keyRect.offset(0f, -density)
                }
                canvas.drawRoundRect(keyRect, radius, radius, keyPaint)
                if (t.borders) canvas.drawRoundRect(keyRect, radius, radius, borderPaint)

                val fg = if (isEnter && !pressed) t.onAccent else if (shiftActive) t.accent else t.keyText
                val iconRes = when (key.code) {
                    KeyCode.SHIFT -> when (shiftState) {
                        1 -> R.drawable.ic_key_shift_on
                        2 -> R.drawable.ic_key_caps_lock
                        else -> R.drawable.ic_key_shift
                    }
                    KeyCode.ENTER -> enterIcon
                    else -> key.icon
                }
                if (iconRes != 0) {
                    val d = icon(iconRes, fg)
                    if (d != null) {
                        val size = (min(keyRect.height(), keyRect.width()) * 0.5f).toInt()
                        val cx = keyRect.centerX().toInt()
                        val cy = keyRect.centerY().toInt()
                        d.setBounds(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2)
                        d.draw(canvas)
                    }
                    continue
                }

                val label = labelFor(key)
                if (label.isNotEmpty()) {
                    labelPaint.color = if (key.code == KeyCode.SPACE) t.keyHint else fg
                    labelPaint.textSize = when {
                        key.code == KeyCode.SPACE -> key.h * 0.25f
                        key.code != KeyCode.TEXT -> key.h * 0.27f
                        label.length > 2 -> key.h * 0.3f
                        label[0].code in 0x0980..0x09FF -> key.h * 0.42f
                        else -> key.h * 0.4f
                    }
                    val y = keyRect.centerY() - (labelPaint.descent() + labelPaint.ascent()) / 2
                    canvas.drawText(label, keyRect.centerX(), y, labelPaint)
                }
                if (showHints && key.code == KeyCode.TEXT) {
                    val hint = (if (isShifted()) key.shiftPopup else key.popup).firstOrNull()
                    if (hint != null && hint.length <= 2 && hint.isNotBlank() && hint[0].code >= 0x21) {
                        hintPaint.color = t.keyHint
                        hintPaint.textSize = key.h * 0.2f
                        canvas.drawText(displayLabel(hint), keyRect.right - 4 * density, keyRect.top - hintPaint.ascent() + 2 * density, hintPaint)
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- touch handling

    private enum class Mode { NORMAL, POPUP, SPACE_SWIPE, DELETE_SWIPE, REPEATING, LONG_HANDLED, CONSUMED }

    private inner class Pointer(val id: Int, var key: Key?, val downX: Float, val downY: Float) {
        var mode = Mode.NORMAL
        var lastSwipeX = downX
        var popupItems: List<String> = emptyList()
        var popupLeft = 0f
        var popupCellW = 0f
        var popupIndex = 0
        var deleteCount = 0

        val longPress = Runnable { onLongPressTimeout(this) }
        val repeat = object : Runnable {
            override fun run() {
                val k = key ?: return
                mode = Mode.REPEATING
                listener?.onKey(k, null)
                handler.postDelayed(this, REPEAT_INTERVAL)
            }
        }
    }

    private val pointers = LinkedHashMap<Int, Pointer>()

    private fun findKey(x: Float, y: Float): Key? {
        val l = layout ?: return null
        var best: Key? = null
        var bestDist = Float.MAX_VALUE
        for (row in l.rows) for (key in row.keys) {
            if (key.contains(x, y)) return key
            val dx = max(0f, max(key.x - x, x - (key.x + key.w)))
            val dy = max(0f, max(key.y - y, y - (key.y + key.h)))
            val d = dx * dx + dy * dy
            if (d < bestDist) {
                bestDist = d
                best = key
            }
        }
        return if (bestDist < (24 * density) * (24 * density)) best else null
    }

    private fun overlayOffset(): Pair<Float, Float> {
        val a = IntArray(2)
        val b = IntArray(2)
        getLocationInWindow(a)
        overlay.getLocationInWindow(b)
        return (a[0] - b[0]).toFloat() to (a[1] - b[1]).toFloat()
    }

    private fun showPreviewFor(key: Key) {
        if (!showPreview || key.code != KeyCode.TEXT) {
            overlay.hidePreview()
            return
        }
        val (ox, oy) = overlayOffset()
        val w = max(key.vw * 1.1f, key.h * 0.9f)
        val h = key.h * 1.05f
        val left = (key.vx + key.vw / 2 - w / 2).coerceIn(0f, max(0f, width - w))
        val top = key.y - h + key.h * 0.15f
        overlay.showPreview(left + ox, top + oy, w, h, labelFor(key))
    }

    private fun popupFor(key: Key): List<String> = if (isShifted()) key.shiftPopup else key.popup

    private fun onLongPressTimeout(p: Pointer) {
        val key = p.key ?: return
        if (p.mode != Mode.NORMAL) return
        val items = popupFor(key)
        if (key.code == KeyCode.TEXT && items.isNotEmpty()) {
            val all = listOf(if (isShifted()) key.shiftText else key.text).let { base ->
                // The first popup item is selected by default; the key's own character comes last.
                items + base.filter { it !in items }
            }
            val cellW = max(key.vw * 0.95f, 36 * density)
            val totalW = cellW * all.size
            val left = (key.vx + key.vw / 2 - cellW / 2).coerceIn(0f, max(0f, width - totalW))
            p.popupItems = all
            p.popupLeft = left
            p.popupCellW = cellW
            p.mode = Mode.POPUP
            val (ox, oy) = overlayOffset()
            overlay.showPopup(left + ox, key.y - key.h * 0.85f + oy, cellW, key.h, all, 0)
            listener?.onKeyDown(key)
        } else if (listener?.onLongPress(key) == true) {
            p.mode = Mode.LONG_HANDLED
            overlay.hidePreview()
            invalidate()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                onDown(event.getPointerId(idx), event.getX(idx), event.getY(idx))
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) onMove(event.getPointerId(i), event.getX(i), event.getY(i))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val idx = event.actionIndex
                onUp(event.getPointerId(idx))
            }
            MotionEvent.ACTION_CANCEL -> cancelAllPointers()
        }
        return true
    }

    private fun onDown(id: Int, x: Float, y: Float) {
        val key = findKey(x, y) ?: return
        // Rollover: a second finger landing commits the key still held by the first one.
        for (other in pointers.values) {
            val ok = other.key ?: continue
            if (other.mode == Mode.NORMAL && ok.code != KeyCode.SHIFT && ok.code != KeyCode.DELETE) {
                handler.removeCallbacks(other.longPress)
                other.mode = Mode.CONSUMED
                listener?.onKey(ok, null)
            }
        }
        val p = Pointer(id, key, x, y)
        pointers[id] = p
        listener?.onKeyDown(key)
        showPreviewFor(key)
        scheduleLongPress(p)
        invalidate()
    }

    private fun scheduleLongPress(p: Pointer) {
        val key = p.key ?: return
        if (key.repeatable) handler.postDelayed(p.repeat, REPEAT_START)
        else handler.postDelayed(p.longPress, longPressDelay)
    }

    private fun onMove(id: Int, x: Float, y: Float) {
        val p = pointers[id] ?: return
        val key = p.key ?: return
        when (p.mode) {
            Mode.POPUP -> {
                p.popupIndex = ((x - p.popupLeft) / p.popupCellW).toInt().coerceIn(0, p.popupItems.lastIndex)
                overlay.selectPopup(p.popupIndex)
                return
            }
            Mode.SPACE_SWIPE -> {
                val steps = ((x - p.lastSwipeX) / swipeStep).toInt()
                if (steps != 0) {
                    p.lastSwipeX += steps * swipeStep
                    listener?.onCursorMove(steps)
                }
                return
            }
            Mode.DELETE_SWIPE -> {
                p.deleteCount = ((p.downX - x) / deleteWordStep).toInt().coerceAtLeast(0) + 1
                return
            }
            Mode.REPEATING, Mode.LONG_HANDLED, Mode.CONSUMED -> return
            Mode.NORMAL -> {}
        }
        if (key.code == KeyCode.SPACE && spaceSwipe && abs(x - p.downX) > touchSlop * 1.5f) {
            handler.removeCallbacks(p.longPress)
            p.mode = Mode.SPACE_SWIPE
            p.lastSwipeX = x
            overlay.hidePreview()
            return
        }
        if (key.code == KeyCode.DELETE && deleteSwipe && p.downX - x > touchSlop * 2) {
            handler.removeCallbacks(p.repeat)
            p.mode = Mode.DELETE_SWIPE
            p.deleteCount = 1
            return
        }
        if (abs(x - p.downX) > touchSlop || abs(y - p.downY) > touchSlop) {
            val newKey = findKey(x, y)
            if (newKey != null && newKey !== key && key.code != KeyCode.SPACE && key.code != KeyCode.DELETE) {
                handler.removeCallbacks(p.longPress)
                handler.removeCallbacks(p.repeat)
                p.key = newKey
                showPreviewFor(newKey)
                scheduleLongPress(p)
                invalidate()
            }
        }
    }

    private fun onUp(id: Int) {
        val p = pointers.remove(id) ?: return
        handler.removeCallbacks(p.longPress)
        handler.removeCallbacks(p.repeat)
        val key = p.key
        when (p.mode) {
            Mode.POPUP -> {
                overlay.hidePopup()
                if (key != null) listener?.onKey(key, p.popupItems.getOrNull(p.popupIndex))
            }
            Mode.DELETE_SWIPE -> listener?.onDeleteWords(p.deleteCount)
            Mode.NORMAL -> if (key != null) listener?.onKey(key, null)
            else -> {}
        }
        if (pointers.isEmpty()) overlay.hideAll() else pointers.values.lastOrNull()?.key?.let { showPreviewFor(it) }
        invalidate()
    }

    fun cancelAllPointers() {
        for (p in pointers.values) {
            handler.removeCallbacks(p.longPress)
            handler.removeCallbacks(p.repeat)
        }
        pointers.clear()
        overlay.hideAll()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelAllPointers()
    }

    companion object {
        private const val REPEAT_START = 400L
        private const val REPEAT_INTERVAL = 50L

        /** Readable labels for invisible characters. */
        fun displayLabel(s: String): String = when (s) {
            "\u200C" -> "ZWNJ"
            "\u200D" -> "ZWJ"
            "্\u200C" -> "্\u200C"
            else -> s
        }
    }
}
