package com.bornomala.keyboard.theme

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import androidx.annotation.DrawableRes
import com.bornomala.keyboard.R
import com.bornomala.keyboard.prefs.CustomThemeSettings
import java.io.File

/** Colours and decoration used to draw the keyboard. */
data class KeyboardTheme(
    val id: String,
    val name: String,
    val background: Int,
    val key: Int,
    val keyPressed: Int,
    val functionalKey: Int,
    val keyText: Int,
    val keyHint: Int,
    val accent: Int,
    val keyBorder: Int,
    val strip: Int,
    val borders: Boolean,
    val cornerRadiusDp: Float,
    @DrawableRes val backgroundImage: Int = 0,
    val dark: Boolean = false,
    /** Absolute path of the user's own background photo (custom theme). */
    val backgroundFile: String? = null,
    /** Vertical position of the photo: 0 = top, 1 = bottom. */
    val imageAlign: Float = 0.5f,
    /** Darkening applied over the photo, 0–90 %. */
    val imageDim: Int = 0,
) {
    /** Text colour drawn on top of [accent]. */
    val onAccent: Int
        get() = if (luminance(accent) > 0.55) Color.BLACK else Color.WHITE

    companion object {
        fun luminance(c: Int): Double =
            (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) / 255.0
    }
}

object KeyboardThemes {

    private fun c(hex: Long) = hex.toInt()

    val LIGHT = KeyboardTheme(
        "light", "Light", c(0xFFF7F8FA), c(0xFFFFFFFF), c(0xFFDCEFE4), c(0xFFE5EAE7),
        c(0xFF17231D), c(0xFF53675B), c(0xFF1B8A5A), c(0xFFD9E1DC), c(0xFFF7F8FA), true, 8f,
    )
    val DARK = KeyboardTheme(
        "dark", "Dark", c(0xFF121417), c(0xFF262B29), c(0xFF355443), c(0xFF1D3528),
        c(0xFFF2F7F3), c(0xFFA8BAB0), c(0xFF6AD6A0), c(0xFF3D4942), c(0xFF121417), false, 8f, dark = true,
    )
    val AMOLED = KeyboardTheme(
        "amoled", "AMOLED Black", c(0xFF000000), c(0xFF101412), c(0xFF254333), c(0xFF14281D),
        c(0xFFF4F8F5), c(0xFFA8BAAE), c(0xFF6AD6A0), c(0xFF26362C), c(0xFF000000), false, 8f, dark = true,
    )
    val GREEN = KeyboardTheme(
        "green", "Green", c(0xFFE0EFE5), c(0xFFFAFFFB), c(0xFFB4DBC3), c(0xFFC0DFC9),
        c(0xFF102E1C), c(0xFF4E6957), c(0xFF1B8A5A), c(0xFFBCD5C4), c(0xFFE0EFE5), false, 8f,
    )
    val OCEAN = KeyboardTheme(
        "ocean", "Ocean Blue", c(0xFFE3EEF7), c(0xFFFBFDFF), c(0xFFB7D5ED), c(0xFFC4DCEE),
        c(0xFF173047), c(0xFF526D82), c(0xFF246A9E), c(0xFFBCCFDF), c(0xFFE3EEF7), false, 8f,
    )
    val ROSE = KeyboardTheme(
        "rose", "Rose", c(0xFFF7E8EB), c(0xFFFFFBFC), c(0xFFEDBCC6), c(0xFFEECBD3),
        c(0xFF45212A), c(0xFF825E67), c(0xFFA3314D), c(0xFFDEC2CA), c(0xFFF7E8EB), false, 8f,
    )

    // Image themes keep keys mostly opaque so labels stay readable on the decorative backgrounds.
    private val IMAGE_THEMES = listOf(
        GREEN.copy(id = "img_green", name = "Meadow", key = c(0xE6FAFFFB), functionalKey = c(0xD9C0DFC9),
            strip = c(0x00000000), backgroundImage = R.drawable.theme_bg_gradient_green),
        OCEAN.copy(id = "img_blue", name = "Sky", key = c(0xE6FBFDFF), functionalKey = c(0xD9C4DCEE),
            strip = c(0x00000000), backgroundImage = R.drawable.theme_bg_gradient_blue),
        ROSE.copy(id = "img_sunset", name = "Sunset", key = c(0xE6FFFBFC), functionalKey = c(0xD9EECBD3),
            accent = c(0xFFC2502E), strip = c(0x00000000), backgroundImage = R.drawable.theme_bg_gradient_sunset),
        GREEN.copy(id = "img_nakshi", name = "Nakshi", key = c(0xEBFAFFFB), functionalKey = c(0xE0C0DFC9),
            strip = c(0x00000000), backgroundImage = R.drawable.theme_bg_nakshi_pattern),
        DARK.copy(id = "img_dark", name = "Midnight", key = c(0xD9262B29), functionalKey = c(0xD91D3528),
            strip = c(0x00000000), backgroundImage = R.drawable.theme_bg_dark_abstract),
        LIGHT.copy(id = "img_paper", name = "Paper", key = c(0xEBFFFFFF), functionalKey = c(0xE0E5EAE7),
            borders = false, strip = c(0x00000000), backgroundImage = R.drawable.theme_bg_light_paper),
    )

    /** Theme ids offered in settings, in display order. */
    val ALL_IDS = listOf("custom", "system", "dynamic", "green", "light", "dark", "amoled", "ocean", "rose") +
        IMAGE_THEMES.map { it.id }

    private fun isNight(context: Context) =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    fun displayName(id: String): String = when (id) {
        "system" -> "Follow system"
        "dynamic" -> "Material You"
        "custom" -> "My photo"
        else -> (listOf(LIGHT, DARK, AMOLED, GREEN, OCEAN, ROSE) + IMAGE_THEMES).firstOrNull { it.id == id }?.name ?: id
    }

    const val CUSTOM_FILE = "custom_keyboard_bg.webp"

    fun customFile(context: Context) = File(context.filesDir, CUSTOM_FILE)

    /** The user's photo theme; keys are tinted glass so the photo shows through. */
    fun custom(context: Context, s: CustomThemeSettings): KeyboardTheme {
        val f = customFile(context)
        val base = if (s.darkKeys) DARK else LIGHT
        val alpha = (s.keyOpacity.coerceIn(0, 100) * 255 / 100) shl 24
        fun glass(c: Int) = (c and 0x00FFFFFF) or alpha
        return base.copy(
            id = "custom", name = "My photo",
            key = glass(base.key),
            functionalKey = glass(base.functionalKey),
            keyPressed = base.keyPressed,
            strip = 0,
            borders = false,
            backgroundFile = if (f.exists()) f.absolutePath else null,
            imageAlign = s.align.coerceIn(0, 100) / 100f,
            imageDim = s.dim,
        )
    }

    fun resolve(context: Context, id: String, keyBorders: Boolean, customSettings: CustomThemeSettings? = null): KeyboardTheme {
        val base = when (id) {
            "custom" -> custom(context, customSettings ?: CustomThemeSettings(0, 20, 70, 50, true))
            "system" -> if (isNight(context)) DARK else LIGHT
            "dynamic" -> dynamic(context) ?: if (isNight(context)) DARK else GREEN
            "light" -> LIGHT
            "dark" -> DARK
            "amoled" -> AMOLED
            "ocean" -> OCEAN
            "rose" -> ROSE
            "green" -> GREEN
            else -> IMAGE_THEMES.firstOrNull { it.id == id } ?: GREEN
        }
        return if (keyBorders) base.copy(borders = true) else base
    }

    private fun dynamic(context: Context): KeyboardTheme? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        val r = context.resources
        fun col(id: Int) = r.getColor(id, context.theme)
        val night = isNight(context)
        return if (night) {
            KeyboardTheme(
                "dynamic", "Material You",
                background = col(android.R.color.system_neutral1_900),
                key = col(android.R.color.system_neutral1_800),
                keyPressed = col(android.R.color.system_accent1_700),
                functionalKey = col(android.R.color.system_accent2_800),
                keyText = col(android.R.color.system_neutral1_50),
                keyHint = col(android.R.color.system_neutral2_300),
                accent = col(android.R.color.system_accent1_200),
                keyBorder = col(android.R.color.system_neutral2_700),
                strip = col(android.R.color.system_neutral1_900),
                borders = false, cornerRadiusDp = 8f, dark = true,
            )
        } else {
            KeyboardTheme(
                "dynamic", "Material You",
                background = col(android.R.color.system_neutral1_50),
                key = col(android.R.color.system_neutral1_10),
                keyPressed = col(android.R.color.system_accent1_100),
                functionalKey = col(android.R.color.system_accent2_100),
                keyText = col(android.R.color.system_neutral1_900),
                keyHint = col(android.R.color.system_neutral2_600),
                accent = col(android.R.color.system_accent1_600),
                keyBorder = col(android.R.color.system_neutral2_200),
                strip = col(android.R.color.system_neutral1_50),
                borders = false, cornerRadiusDp = 8f,
            )
        }
    }
}
