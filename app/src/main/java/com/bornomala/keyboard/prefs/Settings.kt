package com.bornomala.keyboard.prefs

import android.content.Context
import android.content.SharedPreferences

/** Preference keys and defaults, shared by the keyboard service and the settings app. */
object Keys {
    const val ENABLED_ENGLISH = "enabled_english"
    const val ENABLED_BANGLA = "enabled_bangla"
    const val BANGLA_LAYOUT = "bangla_layout"
    const val LAST_LANGUAGE = "last_language"
    const val THEME = "theme"
    const val KEY_BORDERS = "key_borders"
    const val NUMBER_ROW = "number_row"
    const val SHOW_HINTS = "show_hints"
    const val KEY_POPUP = "key_popup"
    const val LONG_PRESS_DELAY = "long_press_delay"
    const val KEYBOARD_HEIGHT = "keyboard_height"
    const val ONE_HANDED = "one_handed"
    const val BANGLA_DIGITS = "bangla_digits"
    const val SUGGESTIONS = "suggestions"
    const val AUTO_CAP = "auto_cap"
    const val DOUBLE_SPACE_PERIOD = "double_space_period"
    const val AUTOCORRECT_ENGLISH = "autocorrect_english"
    const val AUTOCORRECT_BANGLA = "autocorrect_bangla"
    const val LEARN_WORDS = "learn_words"
    const val AUTO_SPACE = "auto_space"
    const val VIBRATE = "vibrate"
    const val VIBRATE_MS = "vibrate_ms"
    const val SOUND = "sound"
    const val SOUND_VOLUME = "sound_volume"
    const val SPACE_SWIPE = "space_swipe"
    const val DELETE_SWIPE = "delete_swipe"
    const val SPACE_LONG_PRESS_PICKER = "space_long_press_picker"
    const val CLIPBOARD_HISTORY = "clipboard_history"
    const val EMOJI_RECENT = "emoji_recent"
}

object BanglaLayout {
    const val PHONETIC = "phonetic"
    const val NATIONAL = "national"
}

object OneHanded {
    const val OFF = "off"
    const val LEFT = "left"
    const val RIGHT = "right"
}

/** An immutable snapshot of all keyboard settings. */
data class KeyboardSettings(
    val englishEnabled: Boolean,
    val banglaEnabled: Boolean,
    val banglaLayout: String,
    val lastLanguage: String,
    val theme: String,
    val keyBorders: Boolean,
    val numberRow: Boolean,
    val showHints: Boolean,
    val keyPopup: Boolean,
    val longPressDelay: Int,
    val keyboardHeight: Int,
    val oneHanded: String,
    val banglaDigits: Boolean,
    val suggestions: Boolean,
    val autoCap: Boolean,
    val doubleSpacePeriod: Boolean,
    val autocorrectEnglish: Boolean,
    val autocorrectBangla: Boolean,
    val learnWords: Boolean,
    val autoSpace: Boolean,
    val vibrate: Boolean,
    val vibrateMs: Int,
    val sound: Boolean,
    val soundVolume: Int,
    val spaceSwipe: Boolean,
    val deleteSwipe: Boolean,
    val spaceLongPressPicker: Boolean,
    val clipboardHistory: Boolean,
) {
    companion object {
        fun from(p: SharedPreferences) = KeyboardSettings(
            englishEnabled = p.getBoolean(Keys.ENABLED_ENGLISH, true),
            banglaEnabled = p.getBoolean(Keys.ENABLED_BANGLA, true),
            banglaLayout = p.getString(Keys.BANGLA_LAYOUT, BanglaLayout.PHONETIC) ?: BanglaLayout.PHONETIC,
            lastLanguage = p.getString(Keys.LAST_LANGUAGE, "bn") ?: "bn",
            theme = p.getString(Keys.THEME, "green") ?: "green",
            keyBorders = p.getBoolean(Keys.KEY_BORDERS, false),
            numberRow = p.getBoolean(Keys.NUMBER_ROW, false),
            showHints = p.getBoolean(Keys.SHOW_HINTS, true),
            keyPopup = p.getBoolean(Keys.KEY_POPUP, true),
            longPressDelay = p.getInt(Keys.LONG_PRESS_DELAY, 300),
            keyboardHeight = p.getInt(Keys.KEYBOARD_HEIGHT, 100),
            oneHanded = p.getString(Keys.ONE_HANDED, OneHanded.OFF) ?: OneHanded.OFF,
            banglaDigits = p.getBoolean(Keys.BANGLA_DIGITS, true),
            suggestions = p.getBoolean(Keys.SUGGESTIONS, true),
            autoCap = p.getBoolean(Keys.AUTO_CAP, true),
            doubleSpacePeriod = p.getBoolean(Keys.DOUBLE_SPACE_PERIOD, true),
            autocorrectEnglish = p.getBoolean(Keys.AUTOCORRECT_ENGLISH, true),
            autocorrectBangla = p.getBoolean(Keys.AUTOCORRECT_BANGLA, true),
            learnWords = p.getBoolean(Keys.LEARN_WORDS, true),
            autoSpace = p.getBoolean(Keys.AUTO_SPACE, true),
            vibrate = p.getBoolean(Keys.VIBRATE, true),
            vibrateMs = p.getInt(Keys.VIBRATE_MS, 15),
            sound = p.getBoolean(Keys.SOUND, false),
            soundVolume = p.getInt(Keys.SOUND_VOLUME, 30),
            spaceSwipe = p.getBoolean(Keys.SPACE_SWIPE, true),
            deleteSwipe = p.getBoolean(Keys.DELETE_SWIPE, true),
            spaceLongPressPicker = p.getBoolean(Keys.SPACE_LONG_PRESS_PICKER, true),
            clipboardHistory = p.getBoolean(Keys.CLIPBOARD_HISTORY, true),
        )
    }
}

object Prefs {
    const val FILE = "bornomala_settings"

    fun get(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
