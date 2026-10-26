package com.bornomala.keyboard.ime

import android.content.Intent
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.SystemClock
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import com.bornomala.keyboard.R
import com.bornomala.keyboard.engine.AvroPhonetic
import com.bornomala.keyboard.engine.BanglaText
import com.bornomala.keyboard.engine.NationalComposer
import com.bornomala.keyboard.engine.Suggestions
import com.bornomala.keyboard.layout.BottomRowStyle
import com.bornomala.keyboard.layout.Key
import com.bornomala.keyboard.layout.KeyCode
import com.bornomala.keyboard.layout.KeyboardLayout
import com.bornomala.keyboard.layout.Layouts
import com.bornomala.keyboard.prefs.BanglaLayout
import com.bornomala.keyboard.prefs.KeyboardSettings
import com.bornomala.keyboard.prefs.Keys
import com.bornomala.keyboard.prefs.OneHanded
import com.bornomala.keyboard.prefs.Prefs
import com.bornomala.keyboard.theme.KeyboardTheme
import com.bornomala.keyboard.theme.KeyboardThemes
import com.bornomala.keyboard.ui.MainActivity
import java.text.BreakIterator
import java.text.Normalizer

/** The Bornomala input method: English, Bangla phonetic (Avro style) and Bangla National. */
class BornomalaIME : InputMethodService(),
    KeyboardView.Listener,
    CandidateBar.Listener,
    EmojiPanel.Listener,
    ClipboardPanel.Listener,
    TextEditPanel.Listener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private enum class Mode { ALPHA, SYMBOLS, SYMBOLS2, NUMPAD }
    private enum class Panel { KEYS, EMOJI, CLIPBOARD, TEXT_EDIT }

    private lateinit var prefs: SharedPreferences
    private lateinit var settings: KeyboardSettings
    private lateinit var feedback: Feedback
    private lateinit var clipboardHistory: ClipboardHistory
    private var theme: KeyboardTheme = KeyboardThemes.GREEN

    // Views (created in onCreateInputView).
    private var root: InputRootView? = null
    private lateinit var backgroundView: KeyboardBackgroundView
    private lateinit var column: LinearLayout
    private lateinit var overlay: KeyPopupOverlay
    private lateinit var candidateBar: CandidateBar
    private lateinit var keyboardView: KeyboardView
    private lateinit var content: FrameLayout
    private lateinit var sideLeft: LinearLayout
    private lateinit var sideRight: LinearLayout
    private lateinit var emojiPanel: EmojiPanel
    private lateinit var clipboardPanel: ClipboardPanel
    private lateinit var textEditPanel: TextEditPanel

    // Keyboard state.
    private var bangla = true
    private var mode = Mode.ALPHA
    private var panel = Panel.KEYS
    private var shift = 0
    private var autoShifted = false
    private var lastShiftTap = 0L

    // Editor state.
    private var forceEnglish = false
    private var password = false
    private var suggestionsAllowed = true
    private var learningAllowed = true
    private var isPhone = false
    private var bottomStyle = BottomRowStyle(bangla = false)
    private var enterAction = EditorInfo.IME_ACTION_NONE

    // Word being composed: raw English for phonetic, the word itself otherwise.
    private val typed = StringBuilder()
    private var suggestions = Suggestions.EMPTY
    private var lastSpaceTime = 0L
    private var autoSpaced = false
    private var revert: Pair<String, String>? = null

    private val useBangla get() = bangla && !forceEnglish
    private val phonetic get() = useBangla && mode == Mode.ALPHA && settings.banglaLayout != BanglaLayout.NATIONAL
    private val national get() = useBangla && mode == Mode.ALPHA && settings.banglaLayout == BanglaLayout.NATIONAL

    // ------------------------------------------------------------------ lifecycle

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs.get(this)
        settings = KeyboardSettings.from(prefs)
        feedback = Feedback(this)
        clipboardHistory = ClipboardHistory(this).also { it.start() }
        bangla = settings.lastLanguage == "bn"
        prefs.registerOnSharedPreferenceChangeListener(this)
        Dictionaries.ensureLoaded(this)
    }

    override fun onDestroy() {
        clipboardHistory.stop()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        Dictionaries.saveUser(this)
        super.onDestroy()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        val ctx = this
        overlay = KeyPopupOverlay(ctx)
        keyboardView = KeyboardView(ctx, overlay).also { it.listener = this }
        candidateBar = CandidateBar(ctx, this)
        emojiPanel = EmojiPanel(ctx, this)
        clipboardPanel = ClipboardPanel(ctx, clipboardHistory, this)
        textEditPanel = TextEditPanel(ctx, this)

        content = FrameLayout(ctx).apply {
            addView(keyboardView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(emojiPanel, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0))
            addView(clipboardPanel, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0))
            addView(textEditPanel, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0))
        }
        sideLeft = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        sideRight = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val keysRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(sideLeft, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.14f))
            addView(content, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(sideRight, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.14f))
        }
        column = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(candidateBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)))
            addView(keysRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        backgroundView = KeyboardBackgroundView(ctx)
        // InputRootView sizes the window to the keys only, so the keyboard always sits at the bottom.
        val r = InputRootView(ctx, column).apply {
            addView(backgroundView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            addView(column, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(this@BornomalaIME.overlay, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        // Keep the keys above the gesture navigation area on edge-to-edge devices.
        r.setOnApplyWindowInsetsListener { _, insets ->
            val bottom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetBottom
            }
            column.setPadding(0, 0, 0, bottom)
            insets
        }
        root = r
        applySettings()
        updateLayout()
        return r
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        settings = KeyboardSettings.from(prefs)
        if (root != null) applySettings()

        val cls = info.inputType and InputType.TYPE_MASK_CLASS
        val variation = info.inputType and InputType.TYPE_MASK_VARIATION
        isPhone = cls == InputType.TYPE_CLASS_PHONE
        mode = when (cls) {
            InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE, InputType.TYPE_CLASS_DATETIME -> Mode.NUMPAD
            else -> Mode.ALPHA
        }
        password = (cls == InputType.TYPE_CLASS_TEXT && variation in PASSWORD_VARIATIONS) ||
            (cls == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        val email = cls == InputType.TYPE_CLASS_TEXT &&
            (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS || variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)
        val url = cls == InputType.TYPE_CLASS_TEXT && variation == InputType.TYPE_TEXT_VARIATION_URI
        forceEnglish = password || email || url
        bangla = when {
            !settings.englishEnabled -> true
            !settings.banglaEnabled -> false
            else -> settings.lastLanguage == "bn"
        }
        val noSuggestionsFlag = (info.inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
        suggestionsAllowed = settings.suggestions && !password && cls == InputType.TYPE_CLASS_TEXT && !noSuggestionsFlag
        learningAllowed = settings.learnWords && !password &&
            (info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) == 0
        bottomStyle = BottomRowStyle(
            bangla = false,
            email = email,
            url = url,
            showLanguageKey = settings.englishEnabled && settings.banglaEnabled && !forceEnglish,
        )
        enterAction = if ((info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
            EditorInfo.IME_ACTION_NONE
        } else {
            info.imeOptions and EditorInfo.IME_MASK_ACTION
        }

        typed.setLength(0)
        suggestions = Suggestions.EMPTY
        autoSpaced = false
        revert = null
        shift = 0
        autoShifted = false
        if (root != null) {
            showPanel(Panel.KEYS)
            candidateBar.clear()
            keyboardView.enterIcon = enterIcon(enterAction)
            updateLayout()
            updateShiftAuto()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        finishComposing()
        if (root != null) keyboardView.cancelAllPointers()
        Dictionaries.saveUser(this)
        super.onFinishInputView(finishingInput)
    }

    override fun onFinishInput() {
        finishComposing()
        super.onFinishInput()
    }

    private fun finishComposing() {
        if (typed.isNotEmpty()) {
            currentInputConnection?.finishComposingText()
            typed.setLength(0)
            suggestions = Suggestions.EMPTY
            if (root != null) candidateBar.clear()
        }
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (typed.isNotEmpty() && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            // The user moved the cursor away from the word being typed.
            finishComposing()
        }
        if (typed.isEmpty() && root != null) updateShiftAuto()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == Keys.LAST_LANGUAGE || key == Keys.EMOJI_RECENT) return
        settings = KeyboardSettings.from(prefs)
        if (root != null) {
            applySettings()
            updateLayout()
        }
    }

    // ------------------------------------------------------------------ appearance

    private fun applySettings() {
        theme = KeyboardThemes.resolve(this, settings.theme, settings.keyBorders, settings.custom)
        backgroundView.apply(theme)
        overlay.applyTheme(theme)
        candidateBar.applyTheme(theme)
        emojiPanel.applyTheme(theme)
        clipboardPanel.applyTheme(theme)
        textEditPanel.applyTheme(theme)
        keyboardView.theme = theme
        keyboardView.rowHeightPx = (dp(54) * settings.keyboardHeight / 100f).toInt()
        keyboardView.showHints = settings.showHints
        keyboardView.showPreview = settings.keyPopup
        keyboardView.longPressDelay = settings.longPressDelay.toLong()
        keyboardView.spaceSwipe = settings.spaceSwipe
        keyboardView.deleteSwipe = settings.deleteSwipe
        feedback.vibrate = settings.vibrate
        feedback.vibrateMs = settings.vibrateMs
        feedback.sound = settings.sound
        feedback.soundVolume = settings.soundVolume / 100f
        clipboardHistory.enabled = settings.clipboardHistory
        candidateBar.setToolbarVisible(CandidateBar.ToolbarAction.LAYOUT_SWITCH, settings.banglaEnabled)
        applyOneHanded()
        updatePanelHeights()
    }

    private fun applyOneHanded() {
        val mode = settings.oneHanded
        candidateBar.setOneHandedIcon(mode != OneHanded.LEFT)
        sideLeft.visibility = if (mode == OneHanded.RIGHT) View.VISIBLE else View.GONE
        sideRight.visibility = if (mode == OneHanded.LEFT) View.VISIBLE else View.GONE
        for (side in listOf(sideLeft, sideRight)) {
            side.removeAllViews()
            if (side.visibility != View.VISIBLE) continue
            side.gravity = android.view.Gravity.CENTER
            val toOther = PanelUi.iconButton(this,
                if (mode == OneHanded.RIGHT) R.drawable.ic_tb_one_hand_left else R.drawable.ic_tb_one_hand_right,
                getString(R.string.one_handed_switch), theme) {
                prefs.edit().putString(Keys.ONE_HANDED, if (mode == OneHanded.RIGHT) OneHanded.LEFT else OneHanded.RIGHT).apply()
            }
            val exit = PanelUi.iconButton(this, R.drawable.ic_tb_expand, getString(R.string.one_handed_exit), theme) {
                prefs.edit().putString(Keys.ONE_HANDED, OneHanded.OFF).apply()
            }
            for (b in listOf(toOther, exit)) {
                side.addView(b, LinearLayout.LayoutParams(dp(44), dp(44)).also { it.setMargins(0, dp(6), 0, dp(6)) })
                b.background = null
            }
        }
    }

    private fun keyRows(): Int = if (settings.numberRow) 5 else 4

    private fun updatePanelHeights() {
        val h = keyboardView.rowHeightPx * keyRows()
        for (v in listOf(emojiPanel, clipboardPanel, textEditPanel)) {
            val lp = v.layoutParams
            if (lp.height != h) {
                lp.height = h
                v.layoutParams = lp
            }
        }
    }

    private fun currentLayout(): KeyboardLayout {
        val style = bottomStyle.copy(bangla = useBangla)
        val digitsStyle = style.copy(bangla = useBangla && settings.banglaDigits)
        return when (mode) {
            Mode.NUMPAD -> Layouts.numpad(isPhone, false)
            Mode.SYMBOLS -> Layouts.symbols(digitsStyle)
            Mode.SYMBOLS2 -> Layouts.symbols2(digitsStyle)
            Mode.ALPHA -> when {
                national -> Layouts.national(settings.numberRow, style)
                useBangla -> Layouts.qwerty(settings.numberRow, style, phonetic = true)
                else -> Layouts.qwerty(settings.numberRow, style, phonetic = false)
            }
        }
    }

    private fun updateLayout() {
        if (root == null) return
        keyboardView.layout = currentLayout()
        keyboardView.shiftState = shift
        keyboardView.spaceLabel = when {
            !useBangla -> getString(R.string.space_english)
            settings.banglaLayout == BanglaLayout.NATIONAL -> getString(R.string.space_national)
            else -> getString(R.string.space_phonetic)
        }
        keyboardView.enterIcon = enterIcon(enterAction)
        candidateBar.setToolbarVisible(CandidateBar.ToolbarAction.LAYOUT_SWITCH, settings.banglaEnabled && !forceEnglish)
    }

    private fun enterIcon(action: Int) = when (action) {
        EditorInfo.IME_ACTION_SEARCH -> R.drawable.ic_key_search
        EditorInfo.IME_ACTION_SEND -> R.drawable.ic_key_send
        EditorInfo.IME_ACTION_GO -> R.drawable.ic_key_go
        EditorInfo.IME_ACTION_NEXT -> R.drawable.ic_key_next
        EditorInfo.IME_ACTION_PREVIOUS -> R.drawable.ic_key_previous
        EditorInfo.IME_ACTION_DONE -> R.drawable.ic_key_done
        else -> R.drawable.ic_key_enter
    }

    private fun showPanel(p: Panel) {
        if (p != Panel.KEYS) commitTyped(auto = false)
        panel = p
        keyboardView.visibility = if (p == Panel.KEYS) View.VISIBLE else View.GONE
        emojiPanel.visibility = if (p == Panel.EMOJI) View.VISIBLE else View.GONE
        clipboardPanel.visibility = if (p == Panel.CLIPBOARD) View.VISIBLE else View.GONE
        textEditPanel.visibility = if (p == Panel.TEXT_EDIT) View.VISIBLE else View.GONE
        val shown = when (p) {
            Panel.KEYS -> keyboardView
            Panel.EMOJI -> emojiPanel
            Panel.CLIPBOARD -> clipboardPanel
            Panel.TEXT_EDIT -> textEditPanel
        }
        shown.alpha = 0f
        shown.animate().alpha(1f).setDuration(150).start()
        when (p) {
            Panel.EMOJI -> emojiPanel.show()
            Panel.CLIPBOARD -> clipboardPanel.refresh()
            Panel.TEXT_EDIT -> textEditPanel.reset()
            Panel.KEYS -> keyboardView.cancelAllPointers()
        }
        updatePanelHeights()
    }

    // ------------------------------------------------------------------ key handling

    override fun onKeyDown(key: Key) = feedback.onKey(key)

    override fun onKey(key: Key, popupText: String?) {
        if (key.code != KeyCode.DELETE) revert = null
        when {
            popupText != null -> handleText(popupText)
            key.code == KeyCode.TEXT -> handleText(if (shift > 0) key.shiftText else key.text)
            key.code == KeyCode.SPACE -> handleSpace()
            key.code == KeyCode.DELETE -> handleDelete()
            key.code == KeyCode.ENTER -> handleEnter()
            key.code == KeyCode.SHIFT -> handleShift()
            key.code == KeyCode.SYMBOLS -> setMode(Mode.SYMBOLS)
            key.code == KeyCode.SYMBOLS2 -> setMode(Mode.SYMBOLS2)
            key.code == KeyCode.ALPHA -> setMode(Mode.ALPHA)
            key.code == KeyCode.LANGUAGE -> switchLanguage()
            key.code == KeyCode.EMOJI -> showPanel(Panel.EMOJI)
        }
    }

    override fun onLongPress(key: Key): Boolean = when (key.code) {
        KeyCode.SPACE -> {
            if (settings.spaceLongPressPicker) {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
                true
            } else false
        }
        KeyCode.LANGUAGE -> {
            showPanel(Panel.EMOJI)
            true
        }
        KeyCode.SHIFT -> {
            shift = 2
            autoShifted = false
            keyboardView.shiftState = shift
            true
        }
        KeyCode.SYMBOLS, KeyCode.ALPHA -> {
            showPanel(Panel.EMOJI)
            true
        }
        else -> false
    }

    private fun setMode(m: Mode) {
        commitTyped(auto = true)
        mode = m
        shift = 0
        autoShifted = false
        updateLayout()
        updateShiftAuto()
    }

    private fun isWordInput(text: String): Boolean = when {
        mode != Mode.ALPHA -> false
        phonetic -> text.all { AvroPhonetic.isWordChar(it, typed.isNotEmpty()) }
        national -> text.all { BanglaText.isBanglaWordChar(it) }
        else -> suggestionsAllowed && (text.all { it.isLetter() } || (typed.isNotEmpty() && text == "'"))
    }

    private fun handleText(text: String) {
        val ic = currentInputConnection ?: return
        if (text.isEmpty()) return
        if (isWordInput(text)) {
            appendTyped(text)
        } else {
            ic.beginBatchEdit()
            commitTyped(auto = true)
            val out = if (useBangla) nfc(text) else text
            if (autoSpaced && text.length == 1 && text[0] in SPACE_SWALLOWING_PUNCTUATION) {
                val before = ic.getTextBeforeCursor(1, 0)
                if (before?.toString() == " ") ic.deleteSurroundingText(1, 0)
                ic.commitText("$out ", 1)
            } else {
                ic.commitText(out, 1)
                autoSpaced = false
            }
            ic.endBatchEdit()
        }
        if (shift == 1) {
            shift = 0
            autoShifted = false
            keyboardView.shiftState = 0
        }
        if (typed.isEmpty()) updateShiftAuto()
    }

    private fun appendTyped(text: String) {
        val ic = currentInputConnection ?: return
        autoSpaced = false
        if (national) {
            val prev = typed.lastOrNull() ?: ic.getTextBeforeCursor(1, 0)?.lastOrNull()
            val edit = NationalComposer.compose(prev, text)
            if (edit.deleteBefore > 0) {
                if (typed.length >= edit.deleteBefore) typed.setLength(typed.length - edit.deleteBefore)
                else ic.deleteSurroundingText(edit.deleteBefore, 0)
            }
            typed.append(edit.text)
        } else {
            typed.append(text)
        }
        showComposing()
    }

    private fun composingText(): String = if (phonetic) AvroPhonetic.parse(typed.toString()) else typed.toString()

    private fun showComposing() {
        val ic = currentInputConnection ?: return
        ic.setComposingText(composingText(), 1)
        updateSuggestions()
    }

    private fun updateSuggestions() {
        val raw = typed.toString()
        val engine = Dictionaries.engine
        suggestions = when {
            raw.isEmpty() -> Suggestions.EMPTY
            phonetic -> engine.phonetic(raw, settings.autocorrectBangla && settings.suggestions)
            national -> if (settings.suggestions) engine.national(raw) else Suggestions.EMPTY
            suggestionsAllowed -> engine.english(raw, settings.autocorrectEnglish)
            else -> Suggestions.EMPTY
        }
        val show = suggestions.words.isNotEmpty() && (phonetic || settings.suggestions)
        candidateBar.setSuggestions(if (show) suggestions.words else emptyList(), suggestions.defaultIndex)
    }

    /**
     * Commits the composing word. With [auto] the default suggestion (which may be an
     * auto-correction) is used, otherwise exactly what is shown. [choice] overrides both.
     */
    private fun commitTyped(auto: Boolean, choice: String? = null, picked: Boolean = false) {
        if (typed.isEmpty()) return
        val ic = currentInputConnection
        val raw = typed.toString()
        val shown = composingText()
        val word = choice ?: if (auto) suggestions.default ?: shown else shown
        val out = if (useBangla || word.any { BanglaText.isBangla(it) }) nfc(word) else word
        ic?.commitText(out, 1)
        if (learningAllowed) {
            val user = Dictionaries.user
            user.learn(out, if (picked) 2 else 1)
            if (phonetic && picked) user.rememberPhonetic(raw, out)
        }
        revert = if (!picked && out != nfc(shown)) out to shown else null
        typed.setLength(0)
        suggestions = Suggestions.EMPTY
        candidateBar.clear()
    }

    private fun handleSpace() {
        val ic = currentInputConnection ?: return
        val hadWord = typed.isNotEmpty()
        ic.beginBatchEdit()
        if (hadWord) commitTyped(auto = true)
        val now = SystemClock.uptimeMillis()
        if (!hadWord && autoSpaced) {
            // A space was already added after the picked suggestion.
            autoSpaced = false
            ic.endBatchEdit()
            return
        }
        if (!hadWord && settings.doubleSpacePeriod && mode == Mode.ALPHA && now - lastSpaceTime < DOUBLE_SPACE_MS) {
            val before = ic.getTextBeforeCursor(2, 0)
            if (before != null && before.length == 2 && before[1] == ' ' && !before[0].isWhitespace() &&
                before[0] !in ".,!?।;:"
            ) {
                ic.deleteSurroundingText(1, 0)
                ic.commitText(if (useBangla) "। " else ". ", 1)
                ic.endBatchEdit()
                lastSpaceTime = 0
                updateShiftAuto()
                return
            }
        }
        ic.commitText(" ", 1)
        ic.endBatchEdit()
        lastSpaceTime = now
        autoSpaced = false
        updateShiftAuto()
    }

    private fun handleDelete() {
        val ic = currentInputConnection ?: return
        autoSpaced = false
        if (typed.isNotEmpty()) {
            typed.setLength(typed.length - 1)
            if (typed.isEmpty()) {
                ic.commitText("", 1)
                suggestions = Suggestions.EMPTY
                candidateBar.clear()
                updateShiftAuto()
            } else {
                showComposing()
            }
            return
        }
        revert?.let { (committed, original) ->
            revert = null
            val before = ic.getTextBeforeCursor(committed.length + 1, 0)?.toString()
            if (before == "$committed ") {
                ic.beginBatchEdit()
                ic.deleteSurroundingText(committed.length + 1, 0)
                ic.commitText(original, 1)
                ic.endBatchEdit()
                return
            }
        }
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
            return
        }
        val before = ic.getTextBeforeCursor(16, 0)
        if (before.isNullOrEmpty()) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            return
        }
        ic.deleteSurroundingText(lastGraphemeLength(before), 0)
    }

    private fun lastGraphemeLength(s: CharSequence): Int {
        val last = s[s.length - 1]
        if (!Character.isSurrogate(last) && last != '\uFE0F' && last != '\u20E3') return 1
        return try {
            val bi = BreakIterator.getCharacterInstance()
            bi.setText(s.toString())
            val start = bi.preceding(s.length)
            if (start == BreakIterator.DONE) 1 else s.length - start
        } catch (e: Exception) {
            if (Character.isLowSurrogate(last) && s.length >= 2) 2 else 1
        }
    }

    private fun handleEnter() {
        val ic = currentInputConnection ?: return
        commitTyped(auto = true)
        autoSpaced = false
        val info = currentInputEditorInfo
        when {
            enterAction != EditorInfo.IME_ACTION_NONE && enterAction != EditorInfo.IME_ACTION_UNSPECIFIED ->
                ic.performEditorAction(enterAction)
            info == null || (info.inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_NULL ->
                sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            else -> ic.commitText("\n", 1)
        }
        updateShiftAuto()
    }

    private fun handleShift() {
        if (mode != Mode.ALPHA) return
        val now = SystemClock.uptimeMillis()
        shift = when (shift) {
            0 -> 1
            1 -> if (!autoShifted && now - lastShiftTap < DOUBLE_TAP_MS) 2 else 0
            else -> 0
        }
        autoShifted = false
        lastShiftTap = now
        keyboardView.shiftState = shift
    }

    private fun updateShiftAuto() {
        if (root == null || mode != Mode.ALPHA || shift == 2) return
        val ic = currentInputConnection
        val info = currentInputEditorInfo
        val want = settings.autoCap && !useBangla && !forceEnglish && typed.isEmpty() && ic != null && info != null &&
            ic.getCursorCapsMode(info.inputType) != 0
        if (want) {
            shift = 1
            autoShifted = true
        } else if (autoShifted) {
            shift = 0
            autoShifted = false
        }
        keyboardView.shiftState = shift
    }

    private fun switchLanguage() {
        if (forceEnglish || !(settings.englishEnabled && settings.banglaEnabled)) return
        commitTyped(auto = false)
        bangla = !bangla
        prefs.edit().putString(Keys.LAST_LANGUAGE, if (bangla) "bn" else "en").apply()
        mode = Mode.ALPHA
        shift = 0
        autoShifted = false
        updateLayout()
        updateShiftAuto()
    }

    override fun onCursorMove(steps: Int) {
        commitTyped(auto = false)
        val code = if (steps < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        repeat(kotlin.math.abs(steps)) { sendDownUpKeyEvents(code) }
    }

    override fun onDeleteWords(count: Int) {
        val ic = currentInputConnection ?: return
        commitTyped(auto = false)
        val before = ic.getTextBeforeCursor(500, 0)?.toString() ?: return
        var i = before.length
        repeat(count) {
            while (i > 0 && before[i - 1].isWhitespace()) i--
            while (i > 0 && !before[i - 1].isWhitespace()) i--
        }
        if (i < before.length) ic.deleteSurroundingText(before.length - i, 0)
        updateShiftAuto()
    }

    // ------------------------------------------------------------------ suggestion strip

    override fun onCandidate(index: Int) {
        val word = suggestions.words.getOrNull(index) ?: return
        val ic = currentInputConnection ?: return
        feedback.onKey(null)
        ic.beginBatchEdit()
        commitTyped(auto = false, choice = word, picked = true)
        if (settings.autoSpace) {
            ic.commitText(" ", 1)
            autoSpaced = true
        }
        ic.endBatchEdit()
        updateShiftAuto()
    }

    override fun onCandidateLongPress(word: String) {
        if (Dictionaries.user.count(word) > 0) {
            Dictionaries.user.remove(word)
            Toast.makeText(this, getString(R.string.removed_word, word), Toast.LENGTH_SHORT).show()
            if (typed.isNotEmpty()) updateSuggestions()
        }
    }

    override fun onToolbarAction(action: CandidateBar.ToolbarAction) {
        feedback.onKey(null)
        when (action) {
            CandidateBar.ToolbarAction.EMOJI -> showPanel(if (panel == Panel.EMOJI) Panel.KEYS else Panel.EMOJI)
            CandidateBar.ToolbarAction.CLIPBOARD -> showPanel(if (panel == Panel.CLIPBOARD) Panel.KEYS else Panel.CLIPBOARD)
            CandidateBar.ToolbarAction.TEXT_EDIT -> showPanel(if (panel == Panel.TEXT_EDIT) Panel.KEYS else Panel.TEXT_EDIT)
            CandidateBar.ToolbarAction.LAYOUT_SWITCH -> {
                commitTyped(auto = false)
                val next = if (settings.banglaLayout == BanglaLayout.NATIONAL) BanglaLayout.PHONETIC else BanglaLayout.NATIONAL
                if (!bangla) {
                    bangla = true
                    prefs.edit().putString(Keys.LAST_LANGUAGE, "bn").apply()
                }
                mode = Mode.ALPHA
                prefs.edit().putString(Keys.BANGLA_LAYOUT, next).apply()
                Toast.makeText(this,
                    if (next == BanglaLayout.NATIONAL) R.string.layout_national else R.string.layout_phonetic,
                    Toast.LENGTH_SHORT).show()
            }
            CandidateBar.ToolbarAction.ONE_HANDED -> prefs.edit().putString(Keys.ONE_HANDED,
                if (settings.oneHanded == OneHanded.OFF) OneHanded.RIGHT else OneHanded.OFF).apply()
            CandidateBar.ToolbarAction.THEME -> openSettings("themes")
            CandidateBar.ToolbarAction.SETTINGS -> openSettings(null)
            CandidateBar.ToolbarAction.HIDE -> requestHideSelf(0)
        }
    }

    private fun openSettings(screen: String?) {
        commitTyped(auto = false)
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (screen != null) intent.putExtra(MainActivity.EXTRA_SCREEN, screen)
        startActivity(intent)
        requestHideSelf(0)
    }

    // ------------------------------------------------------------------ panels

    override fun onEmoji(text: String) {
        feedback.onKey(null)
        currentInputConnection?.commitText(text, 1)
    }

    override fun onEmojiDelete() {
        feedback.onKey(null)
        handleDelete()
    }

    override fun onEmojiClose() = showPanel(Panel.KEYS)

    override fun onPasteClip(text: String) {
        feedback.onKey(null)
        currentInputConnection?.commitText(text, 1)
        showPanel(Panel.KEYS)
    }

    override fun onClipboardClose() = showPanel(Panel.KEYS)

    override fun onEditAction(action: TextEditPanel.Action, selecting: Boolean) {
        val ic = currentInputConnection ?: return
        feedback.onKey(null)
        when (action) {
            TextEditPanel.Action.LEFT -> sendKey(KeyEvent.KEYCODE_DPAD_LEFT, selecting)
            TextEditPanel.Action.RIGHT -> sendKey(KeyEvent.KEYCODE_DPAD_RIGHT, selecting)
            TextEditPanel.Action.UP -> sendKey(KeyEvent.KEYCODE_DPAD_UP, selecting)
            TextEditPanel.Action.DOWN -> sendKey(KeyEvent.KEYCODE_DPAD_DOWN, selecting)
            TextEditPanel.Action.HOME -> sendKey(KeyEvent.KEYCODE_MOVE_HOME, selecting)
            TextEditPanel.Action.END -> sendKey(KeyEvent.KEYCODE_MOVE_END, selecting)
            TextEditPanel.Action.SELECT -> {}
            TextEditPanel.Action.SELECT_ALL -> ic.performContextMenuAction(android.R.id.selectAll)
            TextEditPanel.Action.COPY -> ic.performContextMenuAction(android.R.id.copy)
            TextEditPanel.Action.CUT -> ic.performContextMenuAction(android.R.id.cut)
            TextEditPanel.Action.PASTE -> ic.performContextMenuAction(android.R.id.paste)
            TextEditPanel.Action.DELETE -> handleDelete()
            TextEditPanel.Action.CLOSE -> showPanel(Panel.KEYS)
        }
    }

    private fun sendKey(code: Int, shifted: Boolean) {
        val ic = currentInputConnection ?: return
        val meta = if (shifted) KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON else 0
        val now = SystemClock.uptimeMillis()
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0, meta))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0, meta))
    }

    private fun nfc(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFC)

    companion object {
        private const val DOUBLE_SPACE_MS = 450L
        private const val DOUBLE_TAP_MS = 350L
        private const val SPACE_SWALLOWING_PUNCTUATION = ".,?!:;।"
        private val PASSWORD_VARIATIONS = setOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
        )
    }
}
