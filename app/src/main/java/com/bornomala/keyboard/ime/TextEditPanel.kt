package com.bornomala.keyboard.ime

import android.annotation.SuppressLint
import android.content.Context
import android.widget.LinearLayout
import com.bornomala.keyboard.R
import com.bornomala.keyboard.theme.KeyboardTheme

/** Cursor movement, selection and clipboard actions. */
@SuppressLint("ViewConstructor")
class TextEditPanel(context: Context, private val listener: Listener) : LinearLayout(context) {

    enum class Action { LEFT, RIGHT, UP, DOWN, HOME, END, SELECT, SELECT_ALL, COPY, CUT, PASTE, DELETE, CLOSE }

    interface Listener {
        fun onEditAction(action: Action, selecting: Boolean)
    }

    private val density = resources.displayMetrics.density
    var selecting = false
        private set
    private var theme: KeyboardTheme? = null

    init {
        orientation = VERTICAL
    }

    fun reset() {
        selecting = false
        theme?.let { build(it) }
    }

    fun applyTheme(t: KeyboardTheme) {
        theme = t
        build(t)
    }

    private fun build(t: KeyboardTheme) {
        removeAllViews()
        val rows = listOf(
            listOf(
                Triple(R.drawable.ic_tb_select_all, "Select all", Action.SELECT_ALL),
                Triple(R.drawable.ic_key_arrow_up, "", Action.UP),
                Triple(R.drawable.ic_tb_copy, "Copy", Action.COPY),
                Triple(R.drawable.ic_tb_cut, "Cut", Action.CUT),
            ),
            listOf(
                Triple(R.drawable.ic_key_arrow_left, "", Action.LEFT),
                Triple(R.drawable.ic_tb_text_edit, if (selecting) "Selecting" else "Select", Action.SELECT),
                Triple(R.drawable.ic_key_arrow_right, "", Action.RIGHT),
                Triple(R.drawable.ic_tb_paste, "Paste", Action.PASTE),
            ),
            listOf(
                Triple(R.drawable.ic_key_home, "Start", Action.HOME),
                Triple(R.drawable.ic_key_arrow_down, "", Action.DOWN),
                Triple(R.drawable.ic_key_end, "End", Action.END),
                Triple(R.drawable.ic_key_backspace, "Delete", Action.DELETE),
            ),
            listOf(Triple(R.drawable.ic_tb_close, "Back to keyboard", Action.CLOSE)),
        )
        val m = (3 * density).toInt()
        for (row in rows) {
            val line = LinearLayout(context).apply { orientation = HORIZONTAL }
            for ((icon, label, action) in row) {
                val color = when {
                    action == Action.SELECT && selecting -> t.keyPressed
                    action == Action.UP || action == Action.DOWN || action == Action.LEFT || action == Action.RIGHT -> t.key
                    else -> t.functionalKey
                }
                val b = PanelUi.labeledButton(context, icon, label, t, color) {
                    if (action == Action.SELECT) {
                        selecting = !selecting
                        build(t)
                    }
                    listener.onEditAction(action, selecting)
                }
                if (action == Action.DELETE) PanelUi.repeatOnHold(b) { listener.onEditAction(action, selecting) }
                line.addView(b, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).also { it.setMargins(m, m, m, m) })
            }
            addView(line, LayoutParams(LayoutParams.MATCH_PARENT, 0, if (row.size == 1) 0.7f else 1f))
        }
    }
}
