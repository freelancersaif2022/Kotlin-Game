package com.irremote.tv.screens

import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import com.irremote.tv.MainActivity
import com.irremote.tv.MainActivity.MenuItem
import com.irremote.tv.R
import com.irremote.tv.data.SavedRemote
import com.irremote.tv.ir.IrDatabase
import com.irremote.tv.ui.Fonts
import com.irremote.tv.ui.LogoView
import com.irremote.tv.ui.MATCH
import com.irremote.tv.ui.Palette
import com.irremote.tv.ui.WRAP
import com.irremote.tv.ui.brandBadge
import com.irremote.tv.ui.dp
import com.irremote.tv.ui.frameLp
import com.irremote.tv.ui.horizontal
import com.irremote.tv.ui.icon
import com.irremote.tv.ui.iconButton
import com.irremote.tv.ui.lp
import com.irremote.tv.ui.margins
import com.irremote.tv.ui.pressScale
import com.irremote.tv.ui.primaryButton
import com.irremote.tv.ui.ripple
import com.irremote.tv.ui.roundRect
import com.irremote.tv.ui.sectionTitle
import com.irremote.tv.ui.text
import com.irremote.tv.ui.vertical
import com.irremote.tv.ui.withAlpha

class HomeScreen(activity: MainActivity) : Screen(activity) {
    private lateinit var list: LinearLayout

    override fun build(): View = FrameLayout(activity).apply {
        background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Palette.BG_TOP, Palette.BG, Palette.BG))
        val scroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            isFillViewport = true
            clipToPadding = false
            setPadding(0, 0, 0, dp(110))
        }
        val column = activity.vertical {
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(header())
            addView(statusCard(), lp(MATCH, WRAP).margins(t = dp(18)))
            list = activity.vertical()
            addView(list, lp(MATCH, WRAP).margins(t = dp(26)))
        }
        scroll.addView(column)
        addView(scroll, frameLp(MATCH, MATCH))

        val bottom = FrameLayout(context).apply {
            background = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(Palette.BG, Palette.BG, 0x000A0C11))
            setPadding(dp(20), dp(26), dp(20), dp(20))
            addView(activity.primaryButton("Add TV Remote", R.drawable.ic_plus) { activity.push(BrandScreen(activity)) })
        }
        addView(bottom, frameLp(MATCH, WRAP, Gravity.BOTTOM))
    }

    private fun header(): View = activity.horizontal {
        setPadding(0, dp(12), 0, 0)
        addView(LogoView(context, animated = false), lp(dp(44), dp(44)).margins(r = dp(14)))
        addView(activity.vertical {
            addView(context.text("My Remotes", 26f, Palette.TEXT, Fonts.black))
            addView(context.text("Infrared control for any TV", 13f, Palette.TEXT_2), lp(WRAP, WRAP).margins(t = dp(4)))
        }, lp(0, WRAP, 1f))
        addView(activity.iconButton(R.drawable.ic_cog, "Settings") { openSettings() })
    }

    private fun statusCard(): View {
        val ok = activity.ir.hasEmitter
        val color = if (ok) Palette.SUCCESS else Palette.WARNING
        return activity.horizontal {
            background = roundRect(withAlpha(color, 22), activity.dp(18f), withAlpha(color, 70), activity.dp(1))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            addView(FrameLayout(context).apply {
                background = roundRect(withAlpha(color, 40), dp(12f))
                addView(context.icon(if (ok) R.drawable.ic_access_point else R.drawable.ic_alert_circle_outline, color, 22),
                    frameLp(dp(22), dp(22), Gravity.CENTER))
            }, lp(dp(40), dp(40)).margins(r = dp(14)))
            addView(activity.vertical {
                addView(context.text(if (ok) "IR blaster ready" else "No IR blaster found", 15f, Palette.TEXT, Fonts.bold))
                addView(context.text(
                    if (ok) "Point the top of your phone at the TV." else "This phone can't send infrared. Use a phone with an IR blaster (Xiaomi, Redmi, POCO, Vivo, Honor…).",
                    13f, Palette.TEXT_2,
                ).apply { setLineSpacing(0f, 1.2f) }, lp(WRAP, WRAP).margins(t = dp(4)))
            }, lp(0, WRAP, 1f))
        }
    }

    override fun onShow() {
        refresh()
    }

    private fun refresh() {
        if (!::list.isInitialized) return
        list.removeAllViews()
        val remotes = activity.store.all()
        if (remotes.isEmpty()) {
            list.addView(emptyState())
            return
        }
        list.addView(activity.sectionTitle("Saved remotes · ${remotes.size}"), lp(WRAP, WRAP).margins(l = dp(4), b = dp(12)))
        for (r in remotes.sortedByDescending { it.id == activity.store.lastUsed }) list.addView(remoteRow(r), lp(MATCH, WRAP).margins(b = dp(12)))
    }

    private fun remoteRow(r: SavedRemote): View = activity.horizontal {
        background = ripple(roundRect(Palette.SURFACE, dp(20f), Palette.STROKE, dp(1)))
        setPadding(dp(14), dp(14), dp(10), dp(14))
        isClickable = true
        pressScale(0.98f)
        setOnClickListener { activity.push(RemoteScreen(activity, r.id)) }
        setOnLongClickListener { options(r); true }
        addView(context.brandBadge(r.brand), lp(dp(48), dp(48)).margins(r = dp(14)))
        addView(activity.vertical {
            addView(context.text(r.name, 17f, Palette.TEXT, Fonts.bold).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END })
            addView(context.text("${r.brand} · ${r.codeSet.codes.size} buttons", 13f, Palette.TEXT_2).apply {
                maxLines = 1; ellipsize = TextUtils.TruncateAt.END
            }, lp(WRAP, WRAP).margins(t = dp(5)))
        }, lp(0, WRAP, 1f))
        addView(activity.iconButton(R.drawable.ic_dots_vertical, "Options") { options(r) })
    }

    private fun options(r: SavedRemote) {
        activity.showMenu(r.name, listOf(
            MenuItem("Open remote", R.drawable.ic_remote_tv) { activity.push(RemoteScreen(activity, r.id)) },
            MenuItem("Rename", R.drawable.ic_pencil_outline) {
                activity.prompt("Rename remote", r.name, "Save") { activity.store.rename(r.id, it); refresh() }
            },
            MenuItem("Delete", R.drawable.ic_delete_outline, color = Palette.POWER) {
                activity.confirm("Delete remote?", "\"${r.name}\" will be removed from this phone.", "Delete", destructive = true) {
                    activity.store.delete(r.id)
                    refresh()
                }
            },
        ))
    }

    private fun emptyState(): View = activity.vertical {
        gravity = Gravity.CENTER_HORIZONTAL
        background = roundRect(Palette.SURFACE, dp(24f), Palette.STROKE, dp(1))
        setPadding(dp(24), dp(30), dp(24), dp(30))
        addView(FrameLayout(context).apply {
            background = roundRect(Palette.SURFACE_2, dp(30f))
            addView(context.icon(R.drawable.ic_remote_tv, Palette.ACCENT, 44), frameLp(dp(44), dp(44), Gravity.CENTER))
        }, lp(dp(88), dp(88)))
        addView(context.text("No remotes yet", 19f, Palette.TEXT, Fonts.bold), lp(WRAP, WRAP).margins(t = dp(18)))
        addView(context.text("Add your TV in a few seconds. Pick its brand, press Power, and you're done.", 14f, Palette.TEXT_2).apply {
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.25f)
        }, lp(WRAP, WRAP).margins(t = dp(8)))
        addView(steps(), lp(MATCH, WRAP).margins(t = dp(22)))
    }

    private fun steps(): View = activity.vertical {
        val items = listOf(
            R.drawable.ic_television to "Choose your TV brand",
            R.drawable.ic_power to "Press Power and check that the TV responds",
            R.drawable.ic_check to "Save it and use every button",
        )
        for ((i, item) in items.withIndex()) {
            addView(activity.horizontal {
                addView(context.text("${i + 1}", 13f, Palette.ACCENT, Fonts.bold).apply {
                    gravity = Gravity.CENTER
                    background = roundRect(withAlpha(Palette.ACCENT, 36), dp(13f))
                }, lp(dp(26), dp(26)).margins(r = dp(12)))
                addView(context.text(item.second, 14f, Palette.TEXT), lp(0, WRAP, 1f))
                addView(context.icon(item.first, Palette.TEXT_3, 20))
            }, lp(MATCH, WRAP).margins(b = dp(12)))
        }
    }

    private fun openSettings() {
        val db = IrDatabase.get(activity)
        val vib = activity.store.vibration
        activity.showMenu("Settings", listOf(
            MenuItem("Vibrate on key press", R.drawable.ic_vibrate, checked = vib) {
                activity.store.vibration = !vib
                activity.toast(if (!vib) "Vibration on" else "Vibration off", R.drawable.ic_vibrate)
            },
            MenuItem("Code library", R.drawable.ic_format_list_bulleted,
                subtitle = "${db.brands.size} brands · ${db.allSets.size} code sets · ${db.powerSearchList.size} power codes") {},
            MenuItem("About", R.drawable.ic_information_outline,
                subtitle = "IR codes from Flipper-IRDB (CC0). Icons: Material Design Icons (Apache 2.0).") {},
        ))
    }
}
