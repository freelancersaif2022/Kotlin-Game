package com.irremote.tv

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import com.irremote.tv.data.RemoteStore
import com.irremote.tv.ir.IrTransmitter
import com.irremote.tv.screens.Screen
import com.irremote.tv.screens.SplashScreen
import com.irremote.tv.ui.Fonts
import com.irremote.tv.ui.MATCH
import com.irremote.tv.ui.Palette
import com.irremote.tv.ui.WRAP
import com.irremote.tv.ui.dp
import com.irremote.tv.ui.frameLp
import com.irremote.tv.ui.horizontal
import com.irremote.tv.ui.icon
import com.irremote.tv.ui.lp
import com.irremote.tv.ui.margins
import com.irremote.tv.ui.primaryButton
import com.irremote.tv.ui.ripple
import com.irremote.tv.ui.roundRect
import com.irremote.tv.ui.secondaryButton
import com.irremote.tv.ui.text
import com.irremote.tv.ui.vertical

/**
 * Single-activity host. Screens are plain view controllers kept on a stack, which keeps
 * navigation instant and the APK tiny (no AndroidX needed).
 */
class MainActivity : Activity() {

    private lateinit var root: FrameLayout
    private lateinit var content: FrameLayout
    private val stack = ArrayList<Screen>()
    private var sheet: View? = null
    private var toastView: View? = null
    private val handler = Handler(Looper.getMainLooper())

    val store by lazy { RemoteStore(this) }
    val ir by lazy { IrTransmitter.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this).apply { setBackgroundColor(Palette.BG) }
        content = FrameLayout(this)
        root.addView(content, frameLp(MATCH, MATCH))
        setContentView(root)
        push(SplashScreen(this), animate = false)
    }

    val current: Screen? get() = stack.lastOrNull()

    fun push(screen: Screen, animate: Boolean = true) {
        val prev = stack.lastOrNull()
        stack.add(screen)
        val v = screen.createView()
        content.addView(v, frameLp(MATCH, MATCH))
        if (animate && prev != null) {
            v.alpha = 0f
            v.translationX = dp(36f)
            v.animate().alpha(1f).translationX(0f).setDuration(220).withEndAction { prev.view?.visibility = View.GONE }.start()
        } else {
            prev?.view?.visibility = View.GONE
        }
        prev?.onHide()
        screen.onShow()
    }

    fun pop() {
        if (stack.size <= 1) {
            finish()
            return
        }
        val top = stack.removeAt(stack.size - 1)
        top.onHide()
        top.onDestroy()
        val below = stack.last()
        below.view?.visibility = View.VISIBLE
        below.onShow()
        val v = top.view ?: return
        v.animate().alpha(0f).translationX(dp(36f)).setDuration(180).withEndAction { content.removeView(v) }.start()
    }

    /** Clears the stack and shows [screens] (last one on top). */
    fun reset(vararg screens: Screen) {
        dismissSheet()
        for (s in stack) {
            s.onHide()
            s.onDestroy()
        }
        stack.clear()
        content.removeAllViews()
        for ((i, s) in screens.withIndex()) {
            stack.add(s)
            val v = s.createView()
            content.addView(v, frameLp(MATCH, MATCH))
            if (i < screens.size - 1) v.visibility = View.GONE
        }
        screens.lastOrNull()?.let { top ->
            top.view?.alpha = 0f
            top.view?.animate()?.alpha(1f)?.setDuration(260)?.start()
            top.onShow()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (sheet != null) {
            dismissSheet()
            return
        }
        if (current?.onBack() == true) return
        pop()
    }

    override fun onResume() {
        super.onResume()
        current?.onShow()
    }

    override fun onPause() {
        super.onPause()
        current?.onHide()
    }

    override fun onDestroy() {
        super.onDestroy()
        for (s in stack) s.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    // ---------------------------------------------------------------- toast

    fun toast(message: String, iconRes: Int = 0) {
        toastView?.let { root.removeView(it) }
        val v = horizontal {
            background = roundRect(0xFF262C3A.toInt(), dp(16f), Palette.STROKE, dp(1))
            setPadding(dp(16), dp(13), dp(18), dp(13))
            elevation = dp(10f)
            if (iconRes != 0) addView(icon(iconRes, Palette.TEXT, 20), lp(dp(20), dp(20)).margins(r = dp(10)))
            addView(text(message, 14f, Palette.TEXT, Fonts.medium))
        }
        root.addView(v, frameLp(WRAP, WRAP, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            setMargins(dp(24), 0, dp(24), dp(96))
        })
        toastView = v
        v.alpha = 0f
        v.translationY = dp(16f)
        v.animate().alpha(1f).translationY(0f).setDuration(180).start()
        handler.removeCallbacksAndMessages(TOAST_TOKEN)
        handler.postAtTime({
            v.animate().alpha(0f).setDuration(200).withEndAction { root.removeView(v); if (toastView === v) toastView = null }.start()
        }, TOAST_TOKEN, android.os.SystemClock.uptimeMillis() + 2200)
    }

    // ---------------------------------------------------------------- bottom sheets

    /** Shows [body] in a bottom sheet over the current screen. */
    fun showSheet(title: String?, body: View) {
        dismissSheet()
        val scrim = FrameLayout(this).apply {
            setBackgroundColor(0x99000000.toInt())
            isClickable = true
            setOnClickListener { dismissSheet() }
        }
        val panel = vertical {
            background = roundRect(Palette.SURFACE, dp(28f), Palette.STROKE, dp(1))
            setPadding(dp(20), dp(10), dp(20), dp(24))
            isClickable = true
            addView(View(context).apply { background = roundRect(Palette.SURFACE_3, dp(3f)) },
                lp(dp(40), dp(5)).also { it.gravity = Gravity.CENTER_HORIZONTAL; it.bottomMargin = dp(14) })
            if (title != null) addView(text(title, 19f, Palette.TEXT, Fonts.bold), lp(MATCH, WRAP).margins(b = dp(14), l = dp(4)))
            addView(ScrollView(context).apply {
                isVerticalScrollBarEnabled = false
                addView(body)
            }, lp(MATCH, WRAP))
        }
        scrim.addView(panel, frameLp(MATCH, WRAP, Gravity.BOTTOM).apply { setMargins(dp(8), dp(80), dp(8), dp(8)) })
        root.addView(scrim, frameLp(MATCH, MATCH))
        sheet = scrim
        scrim.alpha = 0f
        scrim.animate().alpha(1f).setDuration(160).start()
        panel.translationY = dp(300f)
        panel.animate().translationY(0f).setDuration(240).setInterpolator(android.view.animation.DecelerateInterpolator(2f)).start()
    }

    fun dismissSheet() {
        val s = sheet ?: return
        sheet = null
        hideKeyboard(s)
        s.animate().alpha(0f).setDuration(150).withEndAction { root.removeView(s) }.start()
    }

    /** A list of tappable rows in a sheet. */
    fun showMenu(title: String?, items: List<MenuItem>) {
        val list = vertical()
        for (item in items) {
            list.addView(horizontal {
                setPadding(dp(14), dp(14), dp(14), dp(14))
                background = ripple(roundRect(0x00000000, dp(14f)), 0x22FFFFFF)
                isClickable = true
                setOnClickListener {
                    dismissSheet()
                    item.action()
                }
                if (item.icon != 0) addView(icon(item.icon, item.color, 22), lp(dp(22), dp(22)).margins(r = dp(16)))
                addView(vertical {
                    addView(text(item.title, 16f, item.color, Fonts.medium))
                    if (item.subtitle != null) addView(text(item.subtitle, 13f, Palette.TEXT_2), lp(WRAP, WRAP).margins(t = dp(4)))
                }, lp(0, WRAP, 1f))
                if (item.checked != null) {
                    addView(icon(if (item.checked) R.drawable.ic_check_circle else R.drawable.ic_close,
                        if (item.checked) Palette.SUCCESS else Palette.TEXT_3, 22))
                }
            }, lp(MATCH, WRAP))
        }
        showSheet(title, list)
    }

    class MenuItem(
        val title: String,
        val icon: Int = 0,
        val subtitle: String? = null,
        val color: Int = Palette.TEXT,
        val checked: Boolean? = null,
        val action: () -> Unit,
    )

    fun confirm(title: String, message: String, confirmLabel: String, destructive: Boolean = false, onConfirm: () -> Unit) {
        val body = vertical {
            addView(text(message, 15f, Palette.TEXT_2).apply { setLineSpacing(0f, 1.25f) }, lp(MATCH, WRAP).margins(l = dp(4), b = dp(20)))
            addView(if (destructive) {
                secondaryButton(confirmLabel, R.drawable.ic_delete_outline, Palette.POWER) { dismissSheet(); onConfirm() }
            } else {
                primaryButton(confirmLabel) { dismissSheet(); onConfirm() }
            })
            addView(secondaryButton("Cancel") { dismissSheet() }, lp(MATCH, dp(52)).margins(t = dp(10)))
        }
        showSheet(title, body)
    }

    fun prompt(title: String, initial: String, confirmLabel: String, onDone: (String) -> Unit) {
        val input = EditText(this).apply {
            setText(initial)
            setSelection(initial.length)
            setTextColor(Palette.TEXT)
            setHintTextColor(Palette.TEXT_3)
            hint = "Name"
            textSize = 17f
            typeface = Fonts.medium
            setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            imeOptions = EditorInfo.IME_ACTION_DONE
            background = roundRect(Palette.SURFACE_2, dp(16f), Palette.ACCENT, dp(1))
            setPadding(dp(16), 0, dp(16), 0)
        }
        fun submit() {
            val v = input.text.toString().trim()
            if (v.isEmpty()) return
            dismissSheet()
            onDone(v)
        }
        input.setOnEditorActionListener { _, _, _ -> submit(); true }
        val body = vertical {
            addView(input, lp(MATCH, dp(56)).margins(b = dp(16)))
            addView(primaryButton(confirmLabel) { submit() })
        }
        showSheet(title, body)
        input.requestFocus()
        handler.postDelayed({
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }, 250)
    }

    fun hideKeyboard(v: View) {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0)
    }

    fun infoText(value: String): TextView = text(value, 15f, Palette.TEXT_2).apply { setLineSpacing(0f, 1.3f) }

    private companion object {
        val TOAST_TOKEN = Any()
    }
}
