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
import com.irremote.tv.R
import com.irremote.tv.ir.CodeSet
import com.irremote.tv.ir.IrDatabase
import com.irremote.tv.ir.Key
import com.irremote.tv.ui.Fonts
import com.irremote.tv.ui.MATCH
import com.irremote.tv.ui.Palette
import com.irremote.tv.ui.ProgressRing
import com.irremote.tv.ui.WRAP
import com.irremote.tv.ui.card
import com.irremote.tv.ui.dp
import com.irremote.tv.ui.frameLp
import com.irremote.tv.ui.horizontal
import com.irremote.tv.ui.iconButton
import com.irremote.tv.ui.lp
import com.irremote.tv.ui.margins
import com.irremote.tv.ui.primaryButton
import com.irremote.tv.ui.roundRect
import com.irremote.tv.ui.secondaryButton
import com.irremote.tv.ui.text
import com.irremote.tv.ui.topBar
import com.irremote.tv.ui.vertical
import com.irremote.tv.ui.withAlpha

/**
 * Sends every distinct POWER code in the library, one after another. The user taps Stop as soon
 * as the TV turns off; the last code (or a neighbour, if they reacted late) is then confirmed and saved.
 */
class AutoSearchScreen(activity: MainActivity) : Screen(activity) {
    private val codes: List<CodeSet> = IrDatabase.get(activity).powerSearchList
    private val handler = Handler(Looper.getMainLooper())
    private var index = 0
    private var running = false
    private var found = false

    private lateinit var ring: ProgressRing
    private lateinit var countText: TextView
    private lateinit var brandText: TextView
    private lateinit var stateText: TextView
    private lateinit var controls: FrameLayout

    override fun build(): View {
        val root = activity.vertical {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Palette.BG_TOP, Palette.BG, Palette.BG))
            addView(context.topBar("Auto Search", "${codes.size} power codes from every brand", onBack = { activity.pop() }))
        }
        val content = activity.vertical {
            setPadding(dp(20), dp(4), dp(20), dp(28))
            addView(steps())
            addView(FrameLayout(context).apply {
                ring = ProgressRing(context)
                addView(ring, frameLp(MATCH, MATCH))
                addView(activity.vertical {
                    gravity = Gravity.CENTER
                    countText = context.text("0 / ${codes.size}", 34f, Palette.TEXT, Fonts.black)
                    brandText = context.text("Ready", 15f, Palette.TEXT_2, Fonts.medium)
                    addView(countText)
                    addView(brandText, lp(WRAP, WRAP).margins(t = dp(6)))
                }, frameLp(WRAP, WRAP, Gravity.CENTER))
            }, lp(MATCH, dp(270)).margins(t = dp(10)))
            stateText = context.text("", 15f, Palette.TEXT_2).apply { gravity = Gravity.CENTER; setLineSpacing(0f, 1.25f) }
            addView(stateText, lp(MATCH, WRAP).margins(b = dp(16), l = dp(8), r = dp(8)))
            controls = FrameLayout(context)
            addView(controls, lp(MATCH, WRAP))
        }
        root.addView(ScrollView(activity).apply {
            isVerticalScrollBarEnabled = false
            addView(content)
        }, lp(MATCH, 0, 1f))
        render()
        return root
    }

    private fun steps(): View = activity.card().apply {
        val items = listOf(
            "Turn your TV on and point your phone at it.",
            "Tap Start. A different power code is sent every ${INTERVAL_MS / 1000.0} s.",
            "Tap \"TV turned off\" the moment your TV switches off.",
        )
        for ((i, s) in items.withIndex()) {
            addView(activity.horizontal {
                gravity = Gravity.TOP
                addView(context.text("${i + 1}", 13f, Palette.ACCENT, Fonts.bold).apply {
                    gravity = Gravity.CENTER
                    background = roundRect(withAlpha(Palette.ACCENT, 36), dp(12f))
                }, lp(dp(24), dp(24)).margins(r = dp(12)))
                addView(context.text(s, 14f, Palette.TEXT_2).apply { setLineSpacing(0f, 1.2f) }, lp(0, WRAP, 1f))
            }, lp(MATCH, WRAP).margins(b = if (i < items.size - 1) dp(12) else 0))
        }
    }

    private fun render() {
        countText.text = "${minOf(index + if (running || found) 1 else 0, codes.size)} / ${codes.size}"
        ring.progress = if (codes.isEmpty()) 0f else (index + if (running || found) 1 else 0).toFloat() / codes.size
        ring.running = running
        val c = codes.getOrNull(index)
        brandText.text = when {
            found && c != null -> "${c.brand} · Code ${index + 1}"
            running && c != null -> c.brand
            index > 0 -> "Paused"
            else -> "Ready"
        }
        stateText.text = when {
            found -> "Press Test to confirm this code turns your TV on or off. If it doesn't, step back or forward."
            running -> "Sending power codes… watch your TV."
            index > 0 -> "Search paused."
            else -> ""
        }
        controls.removeAllViews()
        val box = activity.vertical()
        when {
            found -> {
                box.addView(activity.horizontal {
                    addView(activity.iconButton(R.drawable.ic_chevron_left, "Previous code") { step(-1) },
                        LinearLayout.LayoutParams(dp(56), dp(56)))
                    addView(activity.secondaryButton("Test Power", R.drawable.ic_power, Palette.POWER) { send() }, lp(0, dp(56), 1f).margins(l = dp(8), r = dp(8)))
                    addView(activity.iconButton(R.drawable.ic_chevron_right, "Next code") { step(1) },
                        LinearLayout.LayoutParams(dp(56), dp(56)))
                })
                box.addView(activity.primaryButton("It works — save remote", R.drawable.ic_check) { save() }, lp(MATCH, dp(56)).margins(t = dp(12)))
                box.addView(activity.secondaryButton("Continue searching", R.drawable.ic_play) { found = false; index = minOf(index + 1, codes.size - 1); start() },
                    lp(MATCH, dp(52)).margins(t = dp(10)))
            }
            running -> {
                box.addView(activity.primaryButton("TV turned off — Stop", R.drawable.ic_stop) { stop(found = true) })
                box.addView(activity.secondaryButton("Pause") { stop(found = false) }, lp(MATCH, dp(52)).margins(t = dp(10)))
            }
            else -> {
                box.addView(activity.primaryButton(if (index > 0) "Resume search" else "Start search", R.drawable.ic_play) { start() })
                if (index > 0) box.addView(activity.secondaryButton("Restart from the first code", R.drawable.ic_refresh) {
                    index = 0; render()
                }, lp(MATCH, dp(52)).margins(t = dp(10)))
            }
        }
        controls.addView(box, frameLp(MATCH, WRAP))
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            send()
            render()
            handler.postDelayed({
                if (!running) return@postDelayed
                if (index >= codes.size - 1) {
                    stop(found = false)
                    activity.toast("Reached the last code. Tap Restart to search again.")
                } else {
                    index++
                    handler.post(this)
                }
            }, INTERVAL_MS)
        }
    }

    private fun start() {
        if (codes.isEmpty()) return
        if (!activity.ir.hasEmitter) activity.toast("This phone has no IR blaster", R.drawable.ic_alert_circle_outline)
        running = true
        found = false
        render()
        handler.post(tick)
    }

    private fun stop(found: Boolean) {
        running = false
        handler.removeCallbacksAndMessages(null)
        this.found = found
        render()
    }

    private fun step(delta: Int) {
        index = (index + delta).coerceIn(0, codes.size - 1)
        render()
        send()
    }

    private fun send() {
        val signal = codes.getOrNull(index)?.signalFor(Key.POWER) ?: return
        activity.ir.send(signal)
    }

    private fun save() {
        val set = codes[index]
        activity.prompt("Name this remote", "${set.brand} TV", "Save remote") { name ->
            val saved = activity.store.add(name, set)
            activity.toast("Remote saved", R.drawable.ic_check_circle)
            activity.reset(HomeScreen(activity), RemoteScreen(activity, saved.id))
        }
    }

    override fun onHide() {
        if (running) stop(found = false)
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
    }

    private companion object {
        const val INTERVAL_MS = 1800L
    }
}
