package com.bornomala.keyboard.ime

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bornomala.keyboard.R
import com.bornomala.keyboard.theme.KeyboardTheme

/** The strip above the keys: word suggestions while typing, a toolbar otherwise. */
@SuppressLint("ViewConstructor")
class CandidateBar(context: Context, private val listener: Listener) : FrameLayout(context) {

    interface Listener {
        fun onCandidate(index: Int)
        fun onCandidateLongPress(word: String)
        fun onToolbarAction(action: ToolbarAction)
    }

    enum class ToolbarAction(val icon: Int, val description: String) {
        EMOJI(R.drawable.ic_key_emoji, "Emoji"),
        CLIPBOARD(R.drawable.ic_tb_clipboard, "Clipboard"),
        TEXT_EDIT(R.drawable.ic_tb_text_edit, "Text editing"),
        LAYOUT_SWITCH(R.drawable.ic_tb_layout_switch, "Switch Bangla layout"),
        ONE_HANDED(R.drawable.ic_tb_one_hand_right, "One-handed mode"),
        THEME(R.drawable.ic_tb_theme, "Themes"),
        SETTINGS(R.drawable.ic_tb_settings, "Settings"),
        HIDE(R.drawable.ic_key_keyboard_hide, "Hide keyboard"),
    }

    private val density = resources.displayMetrics.density
    private val toolbar = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private val scroll = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        isFillViewport = true
        overScrollMode = OVER_SCROLL_NEVER
    }
    private val candidateRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val toolbarButtons = HashMap<ToolbarAction, ImageView>()
    private var theme: KeyboardTheme? = null
    private var words: List<String> = emptyList()

    init {
        scroll.addView(candidateRow, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(toolbar, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        for (action in ToolbarAction.entries) {
            val b = ImageView(context).apply {
                setImageResource(action.icon)
                contentDescription = action.description
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                val pad = (11 * density).toInt()
                setPadding(pad, pad, pad, pad)
                setOnClickListener { listener.onToolbarAction(action) }
            }
            toolbarButtons[action] = b
            toolbar.addView(b, LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }
        scroll.visibility = GONE
    }

    fun setToolbarVisible(action: ToolbarAction, visible: Boolean) {
        toolbarButtons[action]?.visibility = if (visible) VISIBLE else GONE
    }

    fun setOneHandedIcon(right: Boolean) {
        toolbarButtons[ToolbarAction.ONE_HANDED]?.setImageResource(
            if (right) R.drawable.ic_tb_one_hand_right else R.drawable.ic_tb_one_hand_left,
        )
    }

    fun applyTheme(t: KeyboardTheme) {
        theme = t
        setBackgroundColor(t.strip)
        for (b in toolbarButtons.values) {
            b.setColorFilter(t.keyText)
            b.background = ripple(t)
        }
        render()
    }

    private fun ripple(t: KeyboardTheme) = RippleDrawable(
        ColorStateList.valueOf(t.keyPressed), null,
        GradientDrawable().apply { setColor(0xFFFFFFFF.toInt()); cornerRadius = 8 * density },
    )

    fun setSuggestions(list: List<String>, defaultIndex: Int) {
        words = list
        this.defaultIndex = defaultIndex
        render()
    }

    private var defaultIndex = 0

    fun clear() = setSuggestions(emptyList(), 0)

    val showingSuggestions get() = words.isNotEmpty()

    private fun render() {
        val t = theme ?: return
        if (words.isEmpty()) {
            scroll.visibility = GONE
            toolbar.visibility = VISIBLE
            return
        }
        toolbar.visibility = GONE
        scroll.visibility = VISIBLE
        // Reuse existing views to keep typing allocation-free.
        while (candidateRow.childCount < words.size * 2) {
            val tv = TextView(context).apply {
                gravity = Gravity.CENTER
                maxLines = 1
                minWidth = (56 * density).toInt()
                setPadding((14 * density).toInt(), 0, (14 * density).toInt(), 0)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            }
            val divider = View(context)
            candidateRow.addView(tv, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
            candidateRow.addView(divider, LinearLayout.LayoutParams((1 * density).toInt(), (20 * density).toInt()).apply {
                gravity = Gravity.CENTER_VERTICAL
            })
        }
        for (i in 0 until candidateRow.childCount / 2) {
            val tv = candidateRow.getChildAt(i * 2) as TextView
            val divider = candidateRow.getChildAt(i * 2 + 1)
            if (i < words.size) {
                val word = words[i]
                tv.visibility = VISIBLE
                divider.visibility = if (i < words.size - 1) VISIBLE else GONE
                divider.setBackgroundColor(t.keyBorder)
                tv.text = word
                tv.setTextColor(if (i == defaultIndex) t.accent else t.keyText)
                tv.typeface = if (i == defaultIndex) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                tv.background = ripple(t)
                tv.setOnClickListener { listener.onCandidate(i) }
                tv.setOnLongClickListener {
                    listener.onCandidateLongPress(word)
                    true
                }
            } else {
                tv.visibility = GONE
                divider.visibility = GONE
            }
        }
        scroll.scrollTo(0, 0)
    }

    companion object {
        fun iconTint(context: Context, res: Int, color: Int) =
            ContextCompat.getDrawable(context, res)?.mutate()?.apply { setTint(color) }
    }
}
