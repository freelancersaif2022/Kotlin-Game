package com.bornomala.keyboard.ime

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bornomala.keyboard.R
import com.bornomala.keyboard.prefs.Keys
import com.bornomala.keyboard.prefs.Prefs
import com.bornomala.keyboard.theme.KeyboardTheme

/** Emoji and kaomoji picker shown in place of the keys. */
@SuppressLint("ViewConstructor")
class EmojiPanel(context: Context, private val listener: Listener) : LinearLayout(context) {

    interface Listener {
        fun onEmoji(text: String)
        fun onEmojiDelete()
        fun onEmojiClose()
    }

    private class Category(val id: String, val icon: Int, val description: String, var items: List<String>)

    private val density = resources.displayMetrics.density
    private val prefs = Prefs.get(context)
    private val categories = listOf(
        Category("recent", R.drawable.ic_emoji_recent, "Recent", emptyList()),
        Category("smileys", R.drawable.ic_emoji_smileys, "Smileys", emptyList()),
        Category("people", R.drawable.ic_emoji_people, "People", emptyList()),
        Category("animals", R.drawable.ic_emoji_animals, "Animals and nature", emptyList()),
        Category("food", R.drawable.ic_emoji_food, "Food and drink", emptyList()),
        Category("travel", R.drawable.ic_emoji_travel, "Travel and places", emptyList()),
        Category("activities", R.drawable.ic_emoji_activities, "Activities", emptyList()),
        Category("objects", R.drawable.ic_emoji_objects, "Objects", emptyList()),
        Category("symbols", R.drawable.ic_emoji_symbols, "Symbols", emptyList()),
        Category("flags", R.drawable.ic_emoji_flags, "Flags", emptyList()),
        Category("kaomoji", R.drawable.ic_emoji_kaomoji, "Kaomoji", KAOMOJI),
    )
    private var current = 1
    private var theme: KeyboardTheme? = null

    private val grid = GridView(context).apply {
        stretchMode = GridView.STRETCH_COLUMN_WIDTH
        isVerticalScrollBarEnabled = false
        selector = android.graphics.drawable.ColorDrawable(0)
        overScrollMode = OVER_SCROLL_NEVER
    }
    private val adapter = Adapter()
    private val tabs = LinearLayout(context).apply { orientation = HORIZONTAL }
    private val tabViews = ArrayList<ImageView>()
    private val bottomBar = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    init {
        orientation = VERTICAL
        grid.adapter = adapter
        grid.setOnItemClickListener { _, _, position, _ ->
            val item = adapter.getItem(position)
            addRecent(item)
            listener.onEmoji(item)
        }
        addView(grid, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        addView(bottomBar, LayoutParams(LayoutParams.MATCH_PARENT, (44 * density).toInt()))
        current = if (recent().isEmpty()) 1 else 0
        load(context)
    }

    fun applyTheme(t: KeyboardTheme) {
        theme = t
        bottomBar.removeAllViews()
        tabs.removeAllViews()
        // applyTheme runs again on every theme change; detach the tab strip from the old scroller.
        (tabs.parent as? ViewGroup)?.removeView(tabs)
        tabViews.clear()
        val abc = PanelUi.textButton(context, "ABC", t) { listener.onEmojiClose() }
        bottomBar.addView(abc, LayoutParams((56 * density).toInt(), LayoutParams.MATCH_PARENT).also {
            it.setMargins((4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt())
        })
        val scroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(tabs)
        }
        bottomBar.addView(scroll, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        for ((i, c) in categories.withIndex()) {
            val iv = ImageView(context).apply {
                setImageResource(c.icon)
                contentDescription = c.description
                val pad = (10 * density).toInt()
                setPadding(pad, pad, pad, pad)
                setOnClickListener { select(i) }
            }
            tabViews += iv
            tabs.addView(iv, LayoutParams((44 * density).toInt(), LayoutParams.MATCH_PARENT))
        }
        val del = PanelUi.iconButton(context, R.drawable.ic_key_backspace, "Delete", t) {}
        PanelUi.repeatOnHold(del) { listener.onEmojiDelete() }
        bottomBar.addView(del, LayoutParams((56 * density).toInt(), LayoutParams.MATCH_PARENT).also {
            it.setMargins((4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt())
        })
        updateTabs()
        adapter.notifyDataSetChanged()
    }

    fun show() {
        if (current == 0) categories[0].items = recent()
        select(if (current == 0 && categories[0].items.isEmpty()) 1 else current)
    }

    private fun select(index: Int) {
        current = index
        if (index == 0) categories[0].items = recent()
        val kaomoji = categories[index].id == "kaomoji"
        grid.numColumns = if (kaomoji) 3 else GridView.AUTO_FIT
        grid.columnWidth = (46 * density).toInt()
        adapter.notifyDataSetChanged()
        grid.setSelection(0)
        updateTabs()
    }

    private fun updateTabs() {
        val t = theme ?: return
        for ((i, iv) in tabViews.withIndex()) iv.setColorFilter(if (i == current) t.accent else t.keyHint)
    }

    private fun recent(): List<String> =
        prefs.getString(Keys.EMOJI_RECENT, "")!!.split('\u0001').filter { it.isNotEmpty() }

    private fun addRecent(item: String) {
        val list = recent().toMutableList()
        list.remove(item)
        list.add(0, item)
        prefs.edit().putString(Keys.EMOJI_RECENT, list.take(MAX_RECENT).joinToString("\u0001")).apply()
    }

    private fun load(context: Context) {
        val app = context.applicationContext
        val main = Handler(Looper.getMainLooper())
        Thread {
            val loaded = loadEmoji(app)
            main.post {
                for (c in categories) loaded[c.id]?.let { c.items = it }
                adapter.notifyDataSetChanged()
            }
        }.apply { priority = Thread.MIN_PRIORITY }.start()
    }

    private inner class Adapter : BaseAdapter() {
        override fun getCount() = categories[current].items.size
        override fun getItem(position: Int) = categories[current].items[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val tv = (convertView as? TextView) ?: TextView(context).apply {
                gravity = Gravity.CENTER
                maxLines = 1
                layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (46 * density).toInt())
            }
            val kaomoji = categories[current].id == "kaomoji"
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (kaomoji) 14f else 26f)
            theme?.let { tv.setTextColor(it.keyText) }
            tv.text = getItem(position)
            return tv
        }
    }

    companion object {
        private const val MAX_RECENT = 40

        @Volatile
        private var cache: Map<String, List<String>>? = null

        private fun loadEmoji(context: Context): Map<String, List<String>> {
            cache?.let { return it }
            val paint = Paint()
            val result = HashMap<String, List<String>>()
            try {
                context.assets.open("emoji/emoji.txt").bufferedReader().useLines { lines ->
                    for (line in lines) {
                        val tab = line.indexOf('\t')
                        if (tab < 0) continue
                        val id = line.substring(0, tab)
                        result[id] = line.substring(tab + 1).split(' ').filter { it.isNotEmpty() && paint.hasGlyph(it) }
                    }
                }
            } catch (e: Exception) {
                // Missing data simply leaves the categories empty.
            }
            cache = result
            return result
        }

        private val KAOMOJI = listOf(
            "(◕‿◕)", "(｡♥‿♥｡)", "(╯°□°）╯︵ ┻━┻", "¯\\_(ツ)_/¯", "(ಥ﹏ಥ)", "(☞ﾟヮﾟ)☞", "ʕ•ᴥ•ʔ", "(＾▽＾)",
            "(≧▽≦)", "(ㆆ_ㆆ)", "(•_•)", "( ͡° ͜ʖ ͡°)", "(T_T)", "(^_^)", "(>_<)", "(o_O)", "(¬‿¬)", "(✿◠‿◠)",
            "(づ｡◕‿‿◕｡)づ", "ヽ(•‿•)ノ", "(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧", "(⌐■_■)", "(•‿•)", "(^ω^)", "(◠‿◠✿)", "٩(◕‿◕｡)۶",
            "(っ˘ω˘ς )", "(*^‿^*)", "(｡•́︿•̀｡)", "(ノಠ益ಠ)ノ", "(°ロ°)☝", "(─‿‿─)", "(●´ω｀●)", "(~˘▾˘)~",
        )
    }
}
