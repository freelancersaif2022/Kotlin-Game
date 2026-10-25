package com.bornomala.keyboard.ime

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.bornomala.keyboard.R
import com.bornomala.keyboard.theme.KeyboardTheme

/** Lists recent clipboard items; tap to paste, pin to keep, or delete. */
@SuppressLint("ViewConstructor")
class ClipboardPanel(
    context: Context,
    private val history: ClipboardHistory,
    private val listener: Listener,
) : LinearLayout(context) {

    interface Listener {
        fun onPasteClip(text: String)
        fun onClipboardClose()
    }

    private val density = resources.displayMetrics.density
    private val header = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val list = LinearLayout(context).apply { orientation = VERTICAL }
    private var theme: KeyboardTheme? = null

    init {
        orientation = VERTICAL
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, (44 * density).toInt()))
        addView(ScrollView(context).apply { addView(list) }, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun dp(v: Float) = (v * density).toInt()

    fun applyTheme(t: KeyboardTheme) {
        theme = t
        header.removeAllViews()
        header.addView(PanelUi.textButton(context, "ABC", t) { listener.onClipboardClose() },
            LayoutParams(dp(56f), LayoutParams.MATCH_PARENT).also { it.setMargins(dp(4f), dp(4f), dp(4f), dp(4f)) })
        header.addView(TextView(context).apply {
            text = context.getString(R.string.clipboard_title)
            setTextColor(t.keyText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(dp(8f), 0, 0, 0)
        }, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        header.addView(PanelUi.iconButton(context, R.drawable.ic_tb_delete, context.getString(R.string.clipboard_clear), t) {
            history.clear()
            refresh()
        }, LayoutParams(dp(48f), LayoutParams.MATCH_PARENT).also { it.setMargins(dp(4f), dp(4f), dp(4f), dp(4f)) })
        refresh()
    }

    fun refresh() {
        val t = theme ?: return
        list.removeAllViews()
        val items = history.all()
        if (items.isEmpty()) {
            list.addView(TextView(context).apply {
                text = context.getString(if (history.enabled) R.string.clipboard_empty else R.string.clipboard_disabled)
                setTextColor(t.keyHint)
                gravity = Gravity.CENTER
                setPadding(dp(16f), dp(32f), dp(16f), dp(32f))
            }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            return
        }
        for (item in items) {
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = PanelUi.keyBackground(context, t, t.key)
            }
            row.addView(TextView(context).apply {
                text = item.text
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                setTextColor(t.keyText)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setPadding(dp(12f), dp(10f), dp(8f), dp(10f))
                setOnClickListener { listener.onPasteClip(item.text) }
            }, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            row.addView(smallIcon(R.drawable.ic_tb_pin, if (item.pinned) t.accent else t.keyHint, "Pin") {
                history.togglePin(item)
                refresh()
            })
            row.addView(smallIcon(R.drawable.ic_tb_close, t.keyHint, "Delete") {
                history.remove(item)
                refresh()
            })
            list.addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                it.setMargins(dp(6f), dp(3f), dp(6f), dp(3f))
            })
        }
    }

    private fun smallIcon(res: Int, color: Int, description: String, onClick: () -> Unit) = ImageView(context).apply {
        setImageResource(res)
        setColorFilter(color)
        contentDescription = description
        val pad = dp(10f)
        setPadding(pad, pad, pad, pad)
        layoutParams = LayoutParams(dp(44f), dp(44f))
        setOnClickListener { onClick() }
    }
}
