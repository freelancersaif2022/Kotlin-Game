package com.bornomala.keyboard.ime

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.bornomala.keyboard.R
import com.bornomala.keyboard.theme.KeyboardTheme

/**
 * Lists recent clipboard items as cards; tap to paste, pin to keep forever, or delete.
 * Unpinned items disappear after 24 hours.
 */
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
    private val scroll = ScrollView(context).apply {
        isVerticalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
        addView(list)
    }
    private var theme: KeyboardTheme? = null

    init {
        orientation = VERTICAL
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, dp(48f)))
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun dp(v: Float) = (v * density).toInt()

    fun applyTheme(t: KeyboardTheme) {
        theme = t
        header.removeAllViews()
        header.addView(PanelUi.textButton(context, "ABC", t) { listener.onClipboardClose() },
            LayoutParams(dp(56f), LayoutParams.MATCH_PARENT).also { it.setMargins(dp(6f), dp(6f), dp(4f), dp(6f)) })
        val titles = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(10f), 0, 0, 0)
            addView(TextView(context).apply {
                text = context.getString(R.string.clipboard_title)
                setTextColor(t.keyText)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                paint.isFakeBoldText = true
            })
            addView(TextView(context).apply {
                text = context.getString(R.string.clipboard_hint)
                setTextColor(t.keyHint)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            })
        }
        header.addView(titles, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        header.addView(PanelUi.iconButton(context, R.drawable.ic_tb_delete, context.getString(R.string.clipboard_clear), t) {
            history.clear()
            refresh()
        }, LayoutParams(dp(48f), LayoutParams.MATCH_PARENT).also { it.setMargins(dp(4f), dp(6f), dp(6f), dp(6f)) })
        refresh()
    }

    fun refresh() {
        val t = theme ?: return
        list.removeAllViews()
        val items = history.all()
        if (items.isEmpty()) {
            list.addView(LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(24f), dp(28f), dp(24f), dp(28f))
                addView(ImageView(context).apply {
                    setImageResource(R.drawable.ic_tb_clipboard)
                    setColorFilter(t.keyHint)
                    alpha = 0.7f
                }, LayoutParams(dp(36f), dp(36f)))
                addView(TextView(context).apply {
                    text = context.getString(if (history.enabled) R.string.clipboard_empty else R.string.clipboard_disabled)
                    setTextColor(t.keyHint)
                    gravity = Gravity.CENTER
                    setPadding(0, dp(10f), 0, 0)
                })
            }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            return
        }
        for ((index, item) in items.withIndex()) {
            val card = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = PanelUi.keyBackground(context, t, t.key)
                elevation = dp(1f).toFloat()
                setOnClickListener { listener.onPasteClip(item.text) }
            }
            val texts = LinearLayout(context).apply {
                orientation = VERTICAL
                setPadding(dp(14f), dp(10f), dp(6f), dp(10f))
            }
            texts.addView(TextView(context).apply {
                text = item.text
                maxLines = 3
                ellipsize = TextUtils.TruncateAt.END
                setTextColor(t.keyText)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            })
            texts.addView(TextView(context).apply {
                text = if (item.pinned) context.getString(R.string.clipboard_pinned) else timeLabel(item.time)
                setTextColor(if (item.pinned) t.accent else t.keyHint)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setPadding(0, dp(3f), 0, 0)
            })
            card.addView(texts, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            card.addView(smallIcon(R.drawable.ic_tb_pin, if (item.pinned) t.accent else t.keyHint,
                context.getString(if (item.pinned) R.string.clipboard_unpin else R.string.clipboard_pin), item.pinned, t) {
                history.togglePin(item)
                refresh()
            })
            card.addView(smallIcon(R.drawable.ic_tb_close, t.keyHint, context.getString(R.string.clipboard_delete), false, t) {
                history.remove(item)
                refresh()
            })
            list.addView(card, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                it.setMargins(dp(8f), dp(4f), dp(8f), dp(4f))
            })
            // Cards slide in quickly, one after another.
            card.alpha = 0f
            card.translationY = dp(8f).toFloat()
            card.animate().alpha(1f).translationY(0f).setStartDelay(minOf(index, 6) * 25L).setDuration(160).start()
        }
    }

    private fun timeLabel(time: Long): String {
        val minutes = ((System.currentTimeMillis() - time) / 60_000).coerceAtLeast(0)
        val left = ((ClipboardHistory.KEEP_MS - (System.currentTimeMillis() - time)) / 3_600_000).coerceAtLeast(0)
        val ago = when {
            minutes < 1 -> context.getString(R.string.time_just_now)
            minutes < 60 -> context.getString(R.string.time_minutes, minutes)
            else -> context.getString(R.string.time_hours, minutes / 60)
        }
        return context.getString(R.string.clipboard_expires, ago, left)
    }

    private fun smallIcon(res: Int, color: Int, description: String, active: Boolean, t: KeyboardTheme, onClick: () -> Unit) =
        ImageView(context).apply {
            setImageResource(res)
            setColorFilter(color)
            contentDescription = description
            val pad = dp(11f)
            setPadding(pad, pad, pad, pad)
            layoutParams = LayoutParams(dp(44f), dp(44f))
            if (active) {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor((t.accent and 0x00FFFFFF) or 0x26000000)
                }
            }
            setOnClickListener { onClick() }
        }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE && changedView === this) scroll.scrollTo(0, 0)
    }
}
