package com.irremote.tv.screens

import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import com.irremote.tv.MainActivity
import com.irremote.tv.R
import com.irremote.tv.ir.Brand
import com.irremote.tv.ir.IrDatabase
import com.irremote.tv.ui.Fonts
import com.irremote.tv.ui.MATCH
import com.irremote.tv.ui.Palette
import com.irremote.tv.ui.WRAP
import com.irremote.tv.ui.bindBadge
import com.irremote.tv.ui.brandBadge
import com.irremote.tv.ui.dp
import com.irremote.tv.ui.frameLp
import com.irremote.tv.ui.gradient
import com.irremote.tv.ui.horizontal
import com.irremote.tv.ui.icon
import com.irremote.tv.ui.lp
import com.irremote.tv.ui.margins
import com.irremote.tv.ui.pressScale
import com.irremote.tv.ui.ripple
import com.irremote.tv.ui.roundRect
import com.irremote.tv.ui.sectionTitle
import com.irremote.tv.ui.text
import com.irremote.tv.ui.topBar
import com.irremote.tv.ui.vertical

/** Brand picker with live search, popular brands, and the Auto Search entry point. */
class BrandScreen(activity: MainActivity) : Screen(activity) {
    private val db = IrDatabase.get(activity)
    private var filtered: List<Brand> = db.brands
    private lateinit var header: View
    private lateinit var headerContent: View
    private lateinit var emptyView: TextView
    private lateinit var listView: ListView
    private lateinit var search: EditText

    private val adapter = object : BaseAdapter() {
        override fun getCount() = filtered.size
        override fun getItem(position: Int) = filtered[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = (convertView?.takeIf { it.tag == ROW_TAG } as? LinearLayout) ?: brandRow()
            bindRow(row, filtered[position])
            return row
        }
    }

    override fun build(): View = activity.vertical {
        background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Palette.BG_TOP, Palette.BG, Palette.BG))
        addView(context.topBar("Choose TV brand", "${db.brands.size} brands supported", onBack = { activity.pop() }))
        addView(searchField(), lp(MATCH, dp(54)).margins(l = dp(20), r = dp(20), b = dp(8)))

        headerContent = activity.vertical {
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(autoSearchCard())
            addView(context.sectionTitle("Popular"), lp(WRAP, WRAP).margins(t = dp(24), b = dp(12), l = dp(4)))
            addView(popularGrid())
            addView(context.sectionTitle("All brands"), lp(WRAP, WRAP).margins(t = dp(22), b = dp(6), l = dp(4)))
        }
        // the list header stays in place; only its content collapses while searching
        header = FrameLayout(context).apply { addView(headerContent) }
        emptyView = context.text("No brand matches. Try Auto Search, or pick \"Generic / Other\".", 14f, Palette.TEXT_2).apply {
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(32), dp(32), dp(32))
            visibility = View.GONE
        }
        listView = ListView(context).apply {
            divider = null
            selector = android.graphics.drawable.ColorDrawable(0)
            isVerticalScrollBarEnabled = false
            clipToPadding = false
            setPadding(0, 0, 0, dp(24))
            addHeaderView(header, null, false)
            this.adapter = this@BrandScreen.adapter
            setOnScrollListener(object : AbsListView.OnScrollListener {
                override fun onScrollStateChanged(view: AbsListView, state: Int) {
                    if (state == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) activity.hideKeyboard(view)
                }
                override fun onScroll(view: AbsListView, first: Int, visible: Int, total: Int) {}
            })
        }
        addView(emptyView, lp(MATCH, WRAP))
        addView(listView, lp(MATCH, 0, 1f))
    }

    private fun searchField(): View = activity.horizontal {
        background = roundRect(Palette.SURFACE, dp(18f), Palette.STROKE, dp(1))
        setPadding(dp(16), 0, dp(6), 0)
        addView(context.icon(R.drawable.ic_magnify, Palette.TEXT_2, 22), lp(dp(22), dp(22)).margins(r = dp(10)))
        search = EditText(context).apply {
            hint = "Search brand, e.g. Samsung"
            setHintTextColor(Palette.TEXT_3)
            setTextColor(Palette.TEXT)
            textSize = 16f
            typeface = Fonts.regular
            background = null
            setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_FILTER
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setPadding(0, 0, 0, 0)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) = applyFilter(s?.toString().orEmpty())
            })
            setOnEditorActionListener { v, _, _ -> activity.hideKeyboard(v); true }
        }
        addView(search, lp(0, MATCH, 1f))
        val clear = FrameLayout(context).apply {
            addView(context.icon(R.drawable.ic_close, Palette.TEXT_2, 20), frameLp(dp(20), dp(20), Gravity.CENTER))
            background = ripple(roundRect(0, dp(20f)), 0x22FFFFFF)
            visibility = View.GONE
            tag = "clear"
            setOnClickListener { search.setText("") }
        }
        addView(clear, lp(dp(40), dp(40)))
    }

    private fun applyFilter(q: String) {
        val query = q.trim().lowercase()
        filtered = if (query.isEmpty()) db.brands else db.brands
            .filter { it.name.lowercase().contains(query) }
            .sortedBy { if (it.name.lowercase().startsWith(query)) 0 else 1 }
        headerContent.visibility = if (query.isEmpty()) View.VISIBLE else View.GONE
        (search.parent as View).findViewWithTag<View>("clear")?.visibility = if (q.isEmpty()) View.GONE else View.VISIBLE
        emptyView.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        adapter.notifyDataSetChanged()
        listView.setSelection(0)
    }

    private fun autoSearchCard(): View = activity.horizontal {
        background = ripple(gradient(dp(22f), 0xFF2A3D8F.toInt(), 0xFF3B2A86.toInt()), 0x33FFFFFF)
        setPadding(dp(18), dp(18), dp(14), dp(18))
        isClickable = true
        pressScale(0.98f)
        setOnClickListener { activity.push(AutoSearchScreen(activity)) }
        addView(FrameLayout(context).apply {
            background = roundRect(0x33FFFFFF, dp(16f))
            addView(context.icon(R.drawable.ic_magnify_scan, 0xFFFFFFFF.toInt(), 26), frameLp(dp(26), dp(26), Gravity.CENTER))
        }, lp(dp(52), dp(52)).margins(r = dp(16)))
        addView(activity.vertical {
            addView(context.text("Auto Search", 17f, 0xFFFFFFFF.toInt(), Fonts.bold))
            addView(context.text("Don't know the brand? Try every power code automatically.", 13f, 0xCCFFFFFF.toInt()).apply {
                setLineSpacing(0f, 1.2f)
            }, lp(WRAP, WRAP).margins(t = dp(5)))
        }, lp(0, WRAP, 1f))
        addView(context.icon(R.drawable.ic_chevron_right, 0xCCFFFFFF.toInt(), 24))
    }

    private fun popularGrid(): View = GridLayout(activity).apply {
        columnCount = 3
        val popular = IrDatabase.POPULAR.mapNotNull { db.brand(it) }
        for ((i, b) in popular.withIndex()) {
            val cell = activity.vertical {
                gravity = Gravity.CENTER
                background = ripple(roundRect(Palette.SURFACE, dp(18f), Palette.STROKE, dp(1)))
                setPadding(dp(6), dp(14), dp(6), dp(12))
                isClickable = true
                pressScale()
                setOnClickListener { open(b) }
                addView(context.brandBadge(b.name, 38))
                addView(context.text(b.name, 13f, Palette.TEXT, Fonts.medium).apply { maxLines = 1 }, lp(WRAP, WRAP).margins(t = dp(8)))
            }
            addView(cell, GridLayout.LayoutParams(GridLayout.spec(i / 3), GridLayout.spec(i % 3, 1f)).also {
                it.width = 0
                it.setMargins(dp(4), dp(4), dp(4), dp(4))
            })
        }
    }

    private fun brandRow(): LinearLayout = activity.horizontal {
        layoutParams = AbsListView.LayoutParams(MATCH, dp(68))
        tag = ROW_TAG
        setPadding(dp(24), 0, dp(20), 0)
        background = ripple(android.graphics.drawable.ColorDrawable(0), 0x22FFFFFF)
        addView(context.brandBadge("?", 40), lp(dp(40), dp(40)).margins(r = dp(16)))
        addView(activity.vertical {
            addView(context.text("", 16f, Palette.TEXT, Fonts.medium))
            addView(context.text("", 12.5f, Palette.TEXT_2), lp(WRAP, WRAP).margins(t = dp(4)))
        }, lp(0, WRAP, 1f))
        addView(context.icon(R.drawable.ic_chevron_right, Palette.TEXT_3, 22))
    }

    private fun bindRow(row: LinearLayout, b: Brand) {
        activity.bindBadge(row.getChildAt(0) as TextView, b.name, 40)
        val texts = row.getChildAt(1) as LinearLayout
        (texts.getChildAt(0) as TextView).text = b.name
        (texts.getChildAt(1) as TextView).text =
            if (b.universal) "Universal codes · tries all ${b.sets.size} code sets"
            else "${b.sets.size} code set${if (b.sets.size == 1) "" else "s"}"
        row.setOnClickListener { open(b) }
    }

    private fun open(b: Brand) {
        activity.hideKeyboard(search)
        activity.push(MatchScreen(activity, b.name))
    }

    private companion object {
        const val ROW_TAG = "brand_row"
    }
}
