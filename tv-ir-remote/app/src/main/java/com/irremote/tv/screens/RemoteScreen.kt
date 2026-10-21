package com.irremote.tv.screens

import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import com.irremote.tv.MainActivity
import com.irremote.tv.MainActivity.MenuItem
import com.irremote.tv.R
import com.irremote.tv.data.SavedRemote
import com.irremote.tv.ir.CodeSet
import com.irremote.tv.ir.IrDatabase
import com.irremote.tv.ir.Key
import com.irremote.tv.ui.DPadView
import com.irremote.tv.ui.Fonts
import com.irremote.tv.ui.KeyButton
import com.irremote.tv.ui.KeyButton.Style
import com.irremote.tv.ui.KeyListener
import com.irremote.tv.ui.LedView
import com.irremote.tv.ui.MATCH
import com.irremote.tv.ui.Palette
import com.irremote.tv.ui.RockerView
import com.irremote.tv.ui.WRAP
import com.irremote.tv.ui.dp
import com.irremote.tv.ui.frameLp
import com.irremote.tv.ui.horizontal
import com.irremote.tv.ui.lp
import com.irremote.tv.ui.margins
import com.irremote.tv.ui.text
import com.irremote.tv.ui.topBar
import com.irremote.tv.ui.vertical

/** The remote control itself, laid out like a premium physical TV remote. */
class RemoteScreen(activity: MainActivity, private val remoteId: Long) : Screen(activity), KeyListener {
    private var remote: SavedRemote? = activity.store.find(remoteId)
    private val set: CodeSet? get() = remote?.codeSet
    private var led: LedView? = null
    private var warnedNoIr = false

    override fun build(): View {
        val r = remote ?: return activity.vertical {
            addView(context.topBar("Remote not found", onBack = { activity.pop() }))
        }
        activity.store.lastUsed = r.id
        val root = activity.vertical {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Palette.BG_TOP, Palette.BG, Palette.BG))
        }
        root.addView(activity.topBar(r.name, "${r.brand} · IR remote", onBack = { activity.pop() },
            actions = listOf(R.drawable.ic_dots_vertical to { _: View -> menu() })))

        val scroll = ScrollView(activity).apply {
            isVerticalScrollBarEnabled = false
            clipToPadding = false
            setPadding(0, activity.dp(4), 0, activity.dp(28))
        }
        val holder = FrameLayout(activity)
        val screenW = activity.resources.displayMetrics.widthPixels
        val bodyW = minOf(screenW - activity.dp(28), activity.dp(372))
        holder.addView(remoteBody(r, bodyW), frameLp(bodyW, WRAP, Gravity.CENTER_HORIZONTAL))
        scroll.addView(holder)
        root.addView(scroll, lp(MATCH, 0, 1f))
        return root
    }

    private fun remoteBody(r: SavedRemote, width: Int): View = activity.vertical {
        val pad = dp(22)
        setPadding(pad, dp(18), pad, dp(26))
        background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xFF1E232E.toInt(), 0xFF151921.toInt(), 0xFF111419.toInt())).apply {
            cornerRadius = dp(44f)
            setStroke(dp(1), 0xFF2C3242.toInt())
        }
        elevation = dp(14f)
        val inner = width - 2 * pad

        // power · IR LED · input
        addView(activity.horizontal {
            addView(key(Key.POWER, R.drawable.ic_power, style = Style.POWER).apply { iconSize = dp(28) }, lp(dp(60), dp(63)))
            addView(activity.vertical {
                gravity = Gravity.CENTER
                val l = LedView(context)
                led = l
                addView(l, lp(dp(30), dp(30)).apply { gravity = Gravity.CENTER_HORIZONTAL })
                addView(context.text("IR", 9f, Palette.TEXT_3, Fonts.bold).apply { gravity = Gravity.CENTER },
                    lp(MATCH, WRAP))
            }, lp(0, WRAP, 1f))
            addView(key(Key.INPUT, R.drawable.ic_video_input_hdmi, style = Style.ROUND), lp(dp(56), dp(59)))
        })

        // streaming shortcuts
        addView(spacedRow(
            appKey(Key.NETFLIX, "NETFLIX", 0xFFE50914.toInt()),
            appKey(Key.YOUTUBE, "YouTube", 0xFFFF3B3B.toInt()),
            appKey(Key.PRIME, "prime", 0xFF1FA8E1.toInt()),
            appKey(Key.DISNEY, "Disney+", 0xFF5B8CFF.toInt()),
            w = dp(64), h = dp(38),
        ), lp(MATCH, WRAP).margins(t = dp(22)))

        // navigation
        val pad2 = minOf(inner, dp(244))
        addView(DPadView(context).apply { listener = this@RemoteScreen }, lp(pad2, pad2 + dp(4)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(22)
        })
        addView(spacedRow(
            key(Key.BACK, R.drawable.ic_arrow_left, style = Style.ROUND),
            key(Key.HOME, R.drawable.ic_home, style = Style.ROUND),
            key(Key.MENU, R.drawable.ic_menu, style = Style.ROUND),
            w = dp(54), h = dp(57),
        ), lp(MATCH, WRAP).margins(t = dp(14)))

        // volume · mute/guide · channel
        addView(activity.horizontal {
            gravity = Gravity.CENTER
            addView(RockerView(context, "VOL", Key.VOL_UP, Key.VOL_DOWN, R.drawable.ic_plus, R.drawable.ic_minus)
                .apply { listener = this@RemoteScreen }, lp(dp(64), dp(160)))
            addView(activity.vertical {
                gravity = Gravity.CENTER
                addView(key(Key.MUTE, R.drawable.ic_volume_off, style = Style.ROUND), lp(dp(54), dp(57)))
                addView(key(Key.INFO, R.drawable.ic_information_outline, style = Style.ROUND), lp(dp(54), dp(57)).margins(t = dp(16)))
            }, lp(0, WRAP, 1f))
            addView(RockerView(context, "CH", Key.CH_UP, Key.CH_DOWN, R.drawable.ic_chevron_up, R.drawable.ic_chevron_down)
                .apply { listener = this@RemoteScreen }, lp(dp(64), dp(160)))
        }, lp(MATCH, WRAP).margins(t = dp(22), l = dp(6), r = dp(6)))

        // number pad
        addView(divider(), lp(MATCH, dp(1)).margins(t = dp(24), b = dp(18)))
        val digits = listOf(Key.D1, Key.D2, Key.D3, Key.D4, Key.D5, Key.D6, Key.D7, Key.D8, Key.D9)
        for (row in 0 until 3) {
            addView(spacedRow(*Array(3) { c -> key(digits[row * 3 + c], label = digits[row * 3 + c].label) }, w = dp(70), h = dp(49)),
                lp(MATCH, WRAP).margins(b = dp(10)))
        }
        addView(spacedRow(
            key(Key.PRE_CH, R.drawable.ic_history),
            key(Key.D0, label = "0"),
            key(Key.DASH, label = "–"),
            w = dp(70), h = dp(49),
        ), lp(MATCH, WRAP))

        // playback
        addView(divider(), lp(MATCH, dp(1)).margins(t = dp(20), b = dp(18)))
        addView(spacedRow(
            key(Key.REWIND, R.drawable.ic_rewind, style = Style.SMALL),
            key(Key.PLAY, R.drawable.ic_play, style = Style.SMALL),
            key(Key.PAUSE, R.drawable.ic_pause, style = Style.SMALL),
            key(Key.FFWD, R.drawable.ic_fast_forward, style = Style.SMALL),
            w = dp(58), h = dp(42),
        ), lp(MATCH, WRAP).margins(b = dp(10)))
        addView(spacedRow(
            key(Key.PREV, R.drawable.ic_skip_previous, style = Style.SMALL),
            key(Key.STOP, R.drawable.ic_stop, style = Style.SMALL),
            key(Key.RECORD, R.drawable.ic_record, style = Style.SMALL, accent = Palette.POWER),
            key(Key.NEXT, R.drawable.ic_skip_next, style = Style.SMALL),
            w = dp(58), h = dp(42),
        ), lp(MATCH, WRAP))

        // colour keys
        addView(spacedRow(
            key(Key.RED, style = Style.COLOR, accent = 0xFFFF4D5E.toInt()),
            key(Key.GREEN, style = Style.COLOR, accent = 0xFF34D27B.toInt()),
            key(Key.YELLOW, style = Style.COLOR, accent = 0xFFFFC93D.toInt()),
            key(Key.BLUE, style = Style.COLOR, accent = 0xFF4C8DFF.toInt()),
            w = dp(58), h = dp(30),
        ), lp(MATCH, WRAP).margins(t = dp(18)))

        // every other key this TV supports
        val extras = listOf(
            Key.EXIT to R.drawable.ic_exit_to_app, Key.SETTINGS to R.drawable.ic_cog,
            Key.OPTIONS to R.drawable.ic_dots_horizontal, Key.GUIDE to R.drawable.ic_television_guide,
            Key.SUBTITLE to R.drawable.ic_closed_caption, Key.ASPECT to R.drawable.ic_aspect_ratio,
            Key.TEXT to R.drawable.ic_card_text_outline, Key.LIST to R.drawable.ic_format_list_bulleted,
            Key.FAV to R.drawable.ic_star, Key.AUDIO to R.drawable.ic_translate,
            Key.PICTURE to R.drawable.ic_image_outline, Key.SOUND to R.drawable.ic_speaker,
            Key.SLEEP to R.drawable.ic_power_sleep, Key.APPS to R.drawable.ic_apps,
            Key.PLAY_PAUSE to R.drawable.ic_play_pause,
            Key.POWER_ON to R.drawable.ic_power, Key.POWER_OFF to R.drawable.ic_power,
        ).filter { (k, _) -> r.codeSet.codes.containsKey(k) }
        if (extras.isNotEmpty()) {
            addView(divider(), lp(MATCH, dp(1)).margins(t = dp(22), b = dp(16)))
            addView(context.text("MORE", 11f, Palette.TEXT_3, Fonts.bold).apply { letterSpacing = 0.25f; gravity = Gravity.CENTER },
                lp(MATCH, WRAP).margins(b = dp(12)))
            addView(GridLayout(context).apply {
                columnCount = 4
                for ((i, pair) in extras.withIndex()) {
                    val (k, icon) = pair
                    val cell = activity.vertical {
                        gravity = Gravity.CENTER_HORIZONTAL
                        val accent = when (k) { Key.POWER_ON -> Palette.SUCCESS; Key.POWER_OFF -> Palette.POWER; else -> Palette.ACCENT }
                        addView(key(k, icon, style = Style.SMALL, accent = accent).apply {
                            if (k == Key.POWER_ON || k == Key.POWER_OFF) iconSize = dp(18)
                        }, lp(dp(58), dp(42)))
                        addView(context.text(k.label, 10.5f, Palette.TEXT_2).apply { maxLines = 1 }, lp(WRAP, WRAP).margins(t = dp(5)))
                    }
                    addView(cell, GridLayout.LayoutParams(GridLayout.spec(i / 4), GridLayout.spec(i % 4, 1f)).also {
                        it.width = 0
                        it.setMargins(0, 0, 0, dp(12))
                    })
                }
            }, lp(MATCH, WRAP))
        }

        // brand engraving at the bottom of the remote
        addView(context.text(r.brand.uppercase(), 13f, 0xFF4A5266.toInt(), Fonts.black).apply {
            letterSpacing = 0.35f
            gravity = Gravity.CENTER
        }, lp(MATCH, WRAP).margins(t = dp(20)))
    }

    private fun key(
        k: Key,
        icon: Int = 0,
        label: String? = null,
        style: Style = Style.NORMAL,
        accent: Int = Palette.ACCENT,
    ): KeyButton = KeyButton(activity, k, icon, label, style, repeatable = false, accentColor = accent).also { it.listener = this }

    private fun appKey(k: Key, label: String, color: Int): KeyButton =
        KeyButton(activity, k, label = label, keyStyle = Style.APP, accentColor = color).also { it.listener = this }

    /** Places [views] evenly across the remote's width, each at a fixed size. */
    private fun spacedRow(vararg views: View, w: Int, h: Int): LinearLayout = activity.horizontal {
        for (v in views) {
            addView(FrameLayout(context).apply { addView(v, frameLp(w, h, Gravity.CENTER)) }, lp(0, h, 1f))
        }
    }

    private fun divider(): View = View(activity).apply { setBackgroundColor(0xFF232836.toInt()) }

    // ------------------------------------------------------------- KeyListener

    override fun onKey(key: Key, repeat: Boolean) {
        val signal = set?.signalFor(key) ?: return
        if (!activity.ir.hasEmitter && !warnedNoIr) {
            warnedNoIr = true
            activity.toast("This phone has no IR blaster", R.drawable.ic_alert_circle_outline)
        }
        activity.ir.send(signal, repeat)
    }

    override fun isAvailable(key: Key): Boolean = set?.has(key) == true

    override fun onUnavailable(key: Key) = activity.toast("${key.label} isn't available for this TV code")

    override val hapticsEnabled: Boolean get() = activity.store.vibration

    override fun onShow() {
        activity.ir.onActivity = { on -> led?.setActive(on) }
    }

    override fun onHide() {
        activity.ir.onActivity = null
        led?.setActive(false)
    }

    // ------------------------------------------------------------- menu

    private fun menu() {
        val r = remote ?: return
        val vib = activity.store.vibration
        val brand = IrDatabase.get(activity).brand(r.brand)
        activity.showMenu(r.name, listOf(
            MenuItem("Change code / re-match", R.drawable.ic_refresh, subtitle = "Find a code that fits your TV better") {
                if (brand != null) activity.push(MatchScreen(activity, brand.name)) else activity.push(AutoSearchScreen(activity))
            },
            MenuItem("Rename", R.drawable.ic_pencil_outline) {
                activity.prompt("Rename remote", r.name, "Save") { name ->
                    activity.store.rename(r.id, name)
                    remote = activity.store.find(r.id)
                    view?.findViewWithTag<android.widget.TextView>("title")?.text = name
                }
            },
            MenuItem("Vibrate on key press", R.drawable.ic_vibrate, checked = vib) { activity.store.vibration = !vib },
            MenuItem("Delete remote", R.drawable.ic_delete_outline, color = Palette.POWER) {
                activity.confirm("Delete remote?", "\"${r.name}\" will be removed from this phone.", "Delete", destructive = true) {
                    activity.store.delete(r.id)
                    activity.pop()
                }
            },
        ))
    }
}
