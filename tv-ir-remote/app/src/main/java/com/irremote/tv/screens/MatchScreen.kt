package com.irremote.tv.screens

import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.irremote.tv.MainActivity
import com.irremote.tv.MainActivity.MenuItem
import com.irremote.tv.R
import com.irremote.tv.ir.Brand
import com.irremote.tv.ir.CodeSet
import com.irremote.tv.ir.IrDatabase
import com.irremote.tv.ir.Key
import com.irremote.tv.ui.Fonts
import com.irremote.tv.ui.KeyButton
import com.irremote.tv.ui.KeyListener
import com.irremote.tv.ui.MATCH
import com.irremote.tv.ui.Palette
import com.irremote.tv.ui.WRAP
import com.irremote.tv.ui.card
import com.irremote.tv.ui.dp
import com.irremote.tv.ui.frameLp
import com.irremote.tv.ui.horizontal
import com.irremote.tv.ui.icon
import com.irremote.tv.ui.iconButton
import com.irremote.tv.ui.lp
import com.irremote.tv.ui.margins
import com.irremote.tv.ui.primaryButton
import com.irremote.tv.ui.secondaryButton
import com.irremote.tv.ui.text
import com.irremote.tv.ui.topBar
import com.irremote.tv.ui.vertical
import com.irremote.tv.ui.withAlpha

/**
 * Finds the working code set for one brand: the user presses Power (and other test keys),
 * then confirms or moves on to the next code. Codes can also be picked from a list or
 * stepped through automatically.
 */
class MatchScreen(activity: MainActivity, private val brandName: String) : Screen(activity), KeyListener {
    private val brand: Brand = IrDatabase.get(activity).brand(brandName)!!
    private val sets: List<CodeSet> = brand.sets
    private var index = 0
    private val handler = Handler(Looper.getMainLooper())
    private var autoTry = false
    private var warnedNoIr = false

    private lateinit var subtitle: TextView
    private lateinit var codeTitle: TextView
    private lateinit var codeLabel: TextView
    private lateinit var autoButton: FrameLayout
    private lateinit var testKeys: List<View>
    private lateinit var powerKey: KeyButton

    private val current get() = sets[index]

    override fun build(): View {
        val root = activity.vertical {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Palette.BG_TOP, Palette.BG, Palette.BG))
        }
        val bar = activity.topBar(brand.name, "", onBack = { activity.pop() },
            actions = listOf(R.drawable.ic_format_list_bulleted to { _: View -> chooseCode() }))
        subtitle = bar.findViewWithTag("subtitle") ?: activity.text("", 13f).also { (bar.getChildAt(1) as LinearLayout).addView(it) }
        root.addView(bar)

        val content = activity.vertical {
            setPadding(dp(20), dp(4), dp(20), dp(28))
            addView(instructions())
            addView(powerTester(), lp(MATCH, WRAP).margins(t = dp(14)))
            addView(codeNavigator(), lp(MATCH, WRAP).margins(t = dp(14)))
            addView(context.text("Did your TV respond?", 17f, Palette.TEXT, Fonts.bold).apply { gravity = Gravity.CENTER },
                lp(MATCH, WRAP).margins(t = dp(24), b = dp(14)))
            addView(activity.horizontal {
                addView(activity.secondaryButton("No, next code", R.drawable.ic_arrow_right) { next(fromUser = true) }, lp(0, dp(56), 1f).margins(r = dp(6)))
                addView(activity.primaryButton("Yes, it works", R.drawable.ic_check) { confirmWorking() }, lp(0, dp(56), 1f).margins(l = dp(6)))
            })
            autoButton = FrameLayout(context)
            addView(autoButton, lp(MATCH, dp(52)).margins(t = dp(12)))
            addView(context.text(
                "Tip: if Power doesn't work, try Volume or Mute too — some TVs ignore Power while already on.",
                12.5f, Palette.TEXT_3,
            ).apply { gravity = Gravity.CENTER; setLineSpacing(0f, 1.2f) }, lp(MATCH, WRAP).margins(t = dp(16), l = dp(8), r = dp(8)))
        }
        root.addView(ScrollView(activity).apply {
            isVerticalScrollBarEnabled = false
            addView(content)
        }, lp(MATCH, 0, 1f))
        renderAutoButton()
        updateCode()
        return root
    }

    private fun instructions(): View = activity.card().apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(activity.horizontal {
            addView(context.icon(R.drawable.ic_remote_tv, Palette.ACCENT, 26))
            addView(context.icon(R.drawable.ic_access_point, withAlpha(Palette.ACCENT, 160), 18).apply { rotation = 90f },
                lp(dp(18), dp(18)).margins(l = dp(4), r = dp(4)))
            addView(context.icon(R.drawable.ic_television, Palette.TEXT, 28))
        }, lp(WRAP, WRAP).margins(r = dp(16)))
        addView(context.text("Point the top of your phone at the TV, then press Power.", 14f, Palette.TEXT_2).apply {
            setLineSpacing(0f, 1.25f)
        }, lp(0, WRAP, 1f))
    }

    private fun powerTester(): View = activity.card().apply {
        gravity = Gravity.CENTER_HORIZONTAL
        powerKey = KeyButton(context, Key.POWER, R.drawable.ic_power, keyStyle = KeyButton.Style.POWER).apply {
            listener = this@MatchScreen
            iconSize = dp(40)
        }
        addView(powerKey, lp(dp(104), dp(108)))
        addView(context.text("Test Power", 15f, Palette.TEXT, Fonts.bold), lp(WRAP, WRAP).margins(t = dp(10)))
        val row = activity.horizontal { gravity = Gravity.CENTER }
        val keys = listOf(
            KeyButton(context, Key.VOL_DOWN, R.drawable.ic_volume_minus, keyStyle = KeyButton.Style.ROUND),
            KeyButton(context, Key.VOL_UP, R.drawable.ic_volume_plus, keyStyle = KeyButton.Style.ROUND),
            KeyButton(context, Key.MUTE, R.drawable.ic_volume_off, keyStyle = KeyButton.Style.ROUND),
            KeyButton(context, Key.CH_UP, label = "CH+", keyStyle = KeyButton.Style.ROUND),
            KeyButton(context, Key.INPUT, R.drawable.ic_video_input_hdmi, keyStyle = KeyButton.Style.ROUND),
        )
        for (k in keys) {
            k.listener = this@MatchScreen
            row.addView(k, lp(dp(54), dp(57)).margins(l = dp(4), r = dp(4)))
        }
        testKeys = keys
        addView(row, lp(MATCH, WRAP).margins(t = dp(18)))
        addView(context.text("More test buttons", 12f, Palette.TEXT_3), lp(WRAP, WRAP).margins(t = dp(8)))
    }

    private fun codeNavigator(): View = activity.horizontal {
        background = com.irremote.tv.ui.roundRect(Palette.SURFACE, dp(20f), Palette.STROKE, dp(1))
        setPadding(dp(6), dp(8), dp(6), dp(8))
        addView(activity.iconButton(R.drawable.ic_chevron_left, "Previous code") { previous() })
        addView(activity.vertical {
            gravity = Gravity.CENTER
            isClickable = true
            setOnClickListener { chooseCode() }
            codeTitle = context.text("", 18f, Palette.TEXT, Fonts.bold)
            codeLabel = context.text("", 12.5f, Palette.TEXT_2).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }
            addView(codeTitle)
            addView(codeLabel, lp(WRAP, WRAP).margins(t = dp(4)))
        }, lp(0, WRAP, 1f))
        addView(activity.iconButton(R.drawable.ic_chevron_right, "Next code") { next(fromUser = true) })
    }

    private fun updateCode() {
        subtitle.text = if (brand.universal) "Universal codes" else "${sets.size} code set${if (sets.size == 1) "" else "s"} for this brand"
        codeTitle.text = "Code ${index + 1} of ${sets.size}"
        val s = current
        codeLabel.text = if (brand.universal) "${s.brand} · ${s.label}" else s.label
        powerKey.invalidate()
        testKeys.forEach { it.invalidate() }
    }

    private fun next(fromUser: Boolean) {
        if (index >= sets.size - 1) {
            stopAuto()
            activity.showMenu("That was the last code", listOf(
                MenuItem("Start again from code 1", R.drawable.ic_refresh) { index = 0; updateCode() },
                MenuItem("Try Auto Search (all brands)", R.drawable.ic_magnify_scan) { activity.push(AutoSearchScreen(activity)) },
                MenuItem("Use Generic / Other codes", R.drawable.ic_format_list_bulleted) {
                    activity.push(MatchScreen(activity, "Generic / Other"))
                },
            ))
            return
        }
        index++
        updateCode()
        if (fromUser) activity.toast("Code ${index + 1} — press Power to test", R.drawable.ic_power)
    }

    private fun previous() {
        if (index == 0) return
        index--
        updateCode()
    }

    private fun chooseCode() {
        stopAuto()
        val items = sets.mapIndexed { i, s ->
            MenuItem(
                "Code ${i + 1}" + if (brand.universal) " · ${s.brand}" else "",
                if (i == index) R.drawable.ic_check_circle else R.drawable.ic_remote_tv,
                subtitle = "${s.label} · ${s.codes.size} buttons",
                color = if (i == index) Palette.ACCENT else Palette.TEXT,
            ) { index = i; updateCode() }
        }
        activity.showMenu("Select code", items)
    }

    // ------------------------------------------------------------- auto-try

    private val autoStep = object : Runnable {
        override fun run() {
            if (!autoTry) return
            sendPower()
            handler.postDelayed({
                if (!autoTry) return@postDelayed
                if (index >= sets.size - 1) next(false) else { next(false); handler.post(this) }
            }, AUTO_INTERVAL_MS)
        }
    }

    private fun renderAutoButton() {
        autoButton.removeAllViews()
        autoButton.addView(
            if (autoTry) activity.secondaryButton("Stop — my TV responded", R.drawable.ic_stop, Palette.POWER) { stopAuto(); confirmWorking() }
            else activity.secondaryButton("Auto-try codes (Power every 2.5 s)", R.drawable.ic_play) { startAuto() },
            frameLp(MATCH, MATCH),
        )
    }

    private fun startAuto() {
        autoTry = true
        renderAutoButton()
        activity.toast("Watch your TV — tap Stop when it turns off", R.drawable.ic_television)
        handler.post(autoStep)
    }

    private fun stopAuto() {
        if (!autoTry) return
        autoTry = false
        handler.removeCallbacksAndMessages(null)
        renderAutoButton()
    }

    // ------------------------------------------------------------- saving

    private fun confirmWorking() {
        stopAuto()
        val set = current
        val name = if (brand.universal) "${brand.name} TV" else "${set.brand} TV"
        activity.prompt("Name this remote", name, "Save remote") { chosen ->
            val saved = activity.store.add(chosen, set)
            activity.toast("Remote saved", R.drawable.ic_check_circle)
            activity.reset(HomeScreen(activity), RemoteScreen(activity, saved.id))
        }
    }

    // ------------------------------------------------------------- sending

    private fun sendPower() = onKey(Key.POWER, false)

    override fun onKey(key: Key, repeat: Boolean) {
        val signal = current.signalFor(key) ?: return
        if (!activity.ir.hasEmitter && !warnedNoIr) {
            warnedNoIr = true
            activity.toast("This phone has no IR blaster", R.drawable.ic_alert_circle_outline)
        }
        activity.ir.send(signal, repeat)
    }

    override fun isAvailable(key: Key) = current.has(key)

    override fun onUnavailable(key: Key) = activity.toast("${key.label} isn't in this code set")

    override val hapticsEnabled get() = activity.store.vibration

    override fun onHide() = stopAuto()

    override fun onDestroy() {
        autoTry = false
        handler.removeCallbacksAndMessages(null)
    }

    private companion object {
        const val AUTO_INTERVAL_MS = 2500L
    }
}
