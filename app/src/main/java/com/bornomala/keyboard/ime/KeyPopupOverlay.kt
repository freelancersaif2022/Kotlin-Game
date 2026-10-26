package com.bornomala.keyboard.ime

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.SystemClock
import android.view.View
import com.bornomala.keyboard.theme.KeyboardTheme
import kotlin.math.abs

/**
 * Transparent layer above the whole keyboard window that draws key previews and long-press
 * popups with short, smooth animations. It never consumes touches; the keyboard view keeps
 * tracking the finger.
 */
@SuppressLint("ViewConstructor")
class KeyPopupOverlay(context: Context) : View(context) {

    private val density = resources.displayMetrics.density
    private var theme: KeyboardTheme? = null
    private val hardwareShadow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density
    }
    private val fallbackShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x30000000 }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val selPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Key preview.
    private var previewText: String? = null
    private val previewRect = RectF()
    private var previewShownAt = 0L
    private var previewAnimate = true
    private var previewHiddenAt = 0L

    // Long-press popup.
    private var popupItems: List<String> = emptyList()
    private val popupRect = RectF()
    private var popupCellW = 0f
    private var popupSelected = 0
    private var popupShownAt = 0L
    private var selX = 0f

    private val tmp = RectF()

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun applyTheme(t: KeyboardTheme) {
        theme = t
        bgPaint.color = t.key or 0xFF000000.toInt()
        if (hardwareShadow) bgPaint.setShadowLayer(10 * density, 0f, 3 * density, if (t.dark) 0x66000000 else 0x38000000)
        borderPaint.color = t.keyBorder
        textPaint.color = t.keyText
        selPaint.color = t.accent
        invalidate()
    }

    fun showPreview(left: Float, top: Float, width: Float, height: Float, text: String) {
        val now = SystemClock.uptimeMillis()
        // While typing quickly a preview is already on screen: switch instantly instead of re-animating.
        previewAnimate = previewText == null && now - previewHiddenAt > FAST_TYPING_MS
        previewRect.set(left, top, left + width, top + height)
        previewText = text
        previewShownAt = now
        invalidate()
    }

    fun hidePreview() {
        if (previewText != null) {
            previewText = null
            previewHiddenAt = SystemClock.uptimeMillis()
            invalidate()
        }
    }

    fun showPopup(left: Float, top: Float, cellW: Float, height: Float, items: List<String>, selected: Int) {
        popupItems = items
        popupCellW = cellW
        popupRect.set(left, top, left + cellW * items.size, top + height)
        popupSelected = selected
        selX = left + selected * cellW
        popupShownAt = SystemClock.uptimeMillis()
        previewText = null
        invalidate()
    }

    fun selectPopup(index: Int) {
        if (index != popupSelected) {
            popupSelected = index
            invalidate()
        }
    }

    fun hidePopup() {
        if (popupItems.isNotEmpty()) {
            popupItems = emptyList()
            invalidate()
        }
    }

    fun hideAll() {
        if (previewText != null) previewHiddenAt = SystemClock.uptimeMillis()
        previewText = null
        popupItems = emptyList()
        invalidate()
    }

    private fun decelerate(t: Float) = 1f - (1f - t) * (1f - t)

    private fun overshoot(t: Float): Float {
        val s = 1.6f
        val x = t - 1f
        return x * x * ((s + 1) * x + s) + 1f
    }

    override fun onDraw(canvas: Canvas) {
        val t = theme ?: return
        val now = SystemClock.uptimeMillis()
        val r = t.cornerRadiusDp * density * 1.3f
        var animating = false

        previewText?.let { text ->
            val p = if (previewAnimate) ((now - previewShownAt) / PREVIEW_IN_MS.toFloat()).coerceIn(0f, 1f) else 1f
            if (p < 1f) animating = true
            val e = decelerate(p)
            val scale = 0.82f + 0.18f * e
            canvas.save()
            // Grow out of the key below the bubble.
            canvas.scale(scale, scale, previewRect.centerX(), previewRect.bottom)
            val alpha = (255 * e).toInt()
            drawBubble(canvas, previewRect, r, alpha)
            textPaint.color = t.keyText
            textPaint.alpha = alpha
            textPaint.textSize = previewRect.height() * 0.52f
            val y = previewRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(text, previewRect.centerX(), y, textPaint)
            textPaint.alpha = 255
            canvas.restore()
        }

        if (popupItems.isNotEmpty()) {
            val p = ((now - popupShownAt) / POPUP_IN_MS.toFloat()).coerceIn(0f, 1f)
            if (p < 1f) animating = true
            val scale = 0.7f + 0.3f * overshoot(p)
            canvas.save()
            canvas.scale(scale, scale, popupRect.centerX(), popupRect.bottom)
            drawBubble(canvas, popupRect, r, (255 * decelerate(p)).toInt())

            // The highlight glides towards the selected cell.
            val target = popupRect.left + popupSelected * popupCellW
            selX += (target - selX) * 0.45f
            if (abs(target - selX) > 0.5f) animating = true else selX = target
            val inset = 3 * density
            tmp.set(selX + inset, popupRect.top + inset, selX + popupCellW - inset, popupRect.bottom - inset)
            canvas.drawRoundRect(tmp, r * 0.8f, r * 0.8f, selPaint)

            for ((i, item) in popupItems.withIndex()) {
                val l = popupRect.left + i * popupCellW
                val label = KeyboardView.displayLabel(item)
                textPaint.color = if (i == popupSelected) t.onAccent else t.keyText
                textPaint.textSize = if (label.length > 3) minOf(popupRect.height() * 0.3f, popupCellW * 0.3f)
                else minOf(popupRect.height() * 0.44f, popupCellW * 0.56f)
                val cx = l + popupCellW / 2
                val y = popupRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(label, cx, y, textPaint)
            }
            canvas.restore()
        }

        if (animating) postInvalidateOnAnimation()
    }

    private fun drawBubble(canvas: Canvas, rect: RectF, r: Float, alpha: Int) {
        if (!hardwareShadow) {
            tmp.set(rect)
            tmp.offset(0f, 2 * density)
            fallbackShadow.alpha = alpha * 0x30 / 255
            canvas.drawRoundRect(tmp, r, r, fallbackShadow)
        }
        bgPaint.alpha = alpha
        canvas.drawRoundRect(rect, r, r, bgPaint)
        bgPaint.alpha = 255
        if (theme?.borders == true || theme?.dark == false) {
            borderPaint.alpha = alpha
            canvas.drawRoundRect(rect, r, r, borderPaint)
        }
    }

    companion object {
        private const val PREVIEW_IN_MS = 90L
        private const val POPUP_IN_MS = 160L
        private const val FAST_TYPING_MS = 120L
    }
}
