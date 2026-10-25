package com.bornomala.keyboard.ime

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import com.bornomala.keyboard.theme.KeyboardTheme

/**
 * Transparent layer above the whole keyboard window that draws key previews and long-press
 * popups. It never consumes touches; the keyboard view keeps tracking the finger.
 */
@SuppressLint("ViewConstructor")
class KeyPopupOverlay(context: Context) : View(context) {

    private val density = resources.displayMetrics.density
    private var theme: KeyboardTheme? = null

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33000000 }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val selPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var previewText: String? = null
    private val previewRect = RectF()

    private var popupItems: List<String> = emptyList()
    private val popupRect = RectF()
    private var popupCellW = 0f
    private var popupSelected = 0
    private val tmp = RectF()

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun applyTheme(t: KeyboardTheme) {
        theme = t
        bgPaint.color = t.key or 0xFF000000.toInt()
        borderPaint.color = t.keyBorder
        textPaint.color = t.keyText
        selPaint.color = t.accent
        invalidate()
    }

    fun showPreview(left: Float, top: Float, width: Float, height: Float, text: String) {
        previewRect.set(left, top, left + width, top + height)
        previewText = text
        invalidate()
    }

    fun hidePreview() {
        if (previewText != null) {
            previewText = null
            invalidate()
        }
    }

    fun showPopup(left: Float, top: Float, cellW: Float, height: Float, items: List<String>, selected: Int) {
        popupItems = items
        popupCellW = cellW
        popupRect.set(left, top, left + cellW * items.size, top + height)
        popupSelected = selected
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
        previewText = null
        popupItems = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val t = theme ?: return
        val r = t.cornerRadiusDp * density
        previewText?.let { text ->
            drawBubble(canvas, previewRect, r)
            textPaint.color = t.keyText
            textPaint.textSize = previewRect.height() * 0.5f
            val y = previewRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(text, previewRect.centerX(), y, textPaint)
        }
        if (popupItems.isNotEmpty()) {
            drawBubble(canvas, popupRect, r)
            textPaint.textSize = minOf(popupRect.height() * 0.42f, popupCellW * 0.55f)
            for ((i, item) in popupItems.withIndex()) {
                val l = popupRect.left + i * popupCellW
                tmp.set(l + 2 * density, popupRect.top + 2 * density, l + popupCellW - 2 * density, popupRect.bottom - 2 * density)
                if (i == popupSelected) {
                    canvas.drawRoundRect(tmp, r, r, selPaint)
                    textPaint.color = t.onAccent
                } else {
                    textPaint.color = t.keyText
                }
                val y = tmp.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
                val label = KeyboardView.displayLabel(item)
                textPaint.textSize = if (label.length > 3) minOf(popupRect.height() * 0.3f, popupCellW * 0.3f)
                else minOf(popupRect.height() * 0.42f, popupCellW * 0.55f)
                canvas.drawText(label, tmp.centerX(), y, textPaint)
            }
        }
    }

    private fun drawBubble(canvas: Canvas, rect: RectF, r: Float) {
        tmp.set(rect)
        tmp.offset(0f, 2 * density)
        canvas.drawRoundRect(tmp, r, r, shadowPaint)
        canvas.drawRoundRect(rect, r, r, bgPaint)
        canvas.drawRoundRect(rect, r, r, borderPaint)
    }
}
