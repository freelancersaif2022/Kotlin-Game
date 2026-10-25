package com.bornomala.keyboard.layout

import com.bornomala.keyboard.R

object KeyCode {
    const val TEXT = 0
    const val SHIFT = -1
    const val SYMBOLS = -2
    const val ALPHA = -3
    const val ENTER = -4
    const val DELETE = -5
    const val SYMBOLS2 = -6
    const val LANGUAGE = -7
    const val EMOJI = -8
    const val SPACE = -9
}

/** A single key. Geometry is assigned by the keyboard view. */
class Key(
    val code: Int = KeyCode.TEXT,
    val text: String = "",
    val shiftText: String = text,
    val label: String = text,
    val shiftLabel: String = shiftText,
    val popup: List<String> = emptyList(),
    val shiftPopup: List<String> = popup,
    val width: Float = 1f,
    val icon: Int = 0,
    val functional: Boolean = code != KeyCode.TEXT && code != KeyCode.SPACE,
    val repeatable: Boolean = false,
) {
    // Touch area.
    var x = 0f
    var y = 0f
    var w = 0f
    var h = 0f

    // Visible key (edge keys get a wider touch area than they draw).
    var vx = 0f
    var vw = 0f

    fun contains(px: Float, py: Float) = px >= x && px < x + w && py >= y && py < y + h
}

class Row(val keys: List<Key>, val leftPad: Float = 0f, val rightPad: Float = 0f)

class KeyboardLayout(val id: String, val rows: List<Row>)

/** What the bottom row and enter key should look like for the current editor. */
data class BottomRowStyle(
    val bangla: Boolean,
    val email: Boolean = false,
    val url: Boolean = false,
    val showLanguageKey: Boolean = true,
)

object Layouts {

    private val DIGITS_EN = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    private val DIGITS_BN = listOf("১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "০")

    private fun digits(bangla: Boolean) = if (bangla) DIGITS_BN else DIGITS_EN

    private fun letter(c: Char, hint: String? = null, extra: List<String> = emptyList()): Key {
        val s = c.toString()
        val popup = listOfNotNull(hint) + extra
        return Key(text = s, shiftText = s.uppercase(), popup = popup, shiftPopup = popup.map { it.uppercase() })
    }

    private val ENGLISH_EXTRAS = mapOf(
        'e' to listOf("é", "è", "ê", "ë"),
        'a' to listOf("á", "à", "â", "ä", "å"),
        'o' to listOf("ó", "ò", "ô", "ö"),
        'u' to listOf("ú", "ù", "û", "ü"),
        'i' to listOf("í", "ì", "î", "ï"),
        'n' to listOf("ñ"),
        'c' to listOf("ç"),
        's' to listOf("ß"),
    )

    private fun shiftKey() = Key(code = KeyCode.SHIFT, width = 1.5f, icon = R.drawable.ic_key_shift)
    private fun deleteKey() = Key(code = KeyCode.DELETE, width = 1.5f, icon = R.drawable.ic_key_backspace, repeatable = true)

    private fun numberRow(bangla: Boolean): Row {
        val d = digits(bangla)
        val alt = digits(!bangla)
        return Row(d.mapIndexed { i, s -> Key(text = s, popup = listOf(alt[i])) })
    }

    /** QWERTY used by English and Bangla phonetic. */
    fun qwerty(numberRow: Boolean, style: BottomRowStyle, phonetic: Boolean): KeyboardLayout {
        val d = digits(style.bangla && phonetic)
        val rows = ArrayList<Row>()
        if (numberRow) rows += numberRow(style.bangla && phonetic)
        rows += Row("qwertyuiop".mapIndexed { i, c ->
            letter(c, if (numberRow) null else d[i], if (phonetic) emptyList() else ENGLISH_EXTRAS[c].orEmpty())
        })
        val row2Hints = listOf("@", "#", "&", "*", "-", "+", "=", "(", ")")
        rows += Row(
            "asdfghjkl".mapIndexed { i, c ->
                letter(c, if (numberRow) null else row2Hints[i], if (phonetic) emptyList() else ENGLISH_EXTRAS[c].orEmpty())
            },
            leftPad = 0.5f, rightPad = 0.5f,
        )
        val row3Hints = listOf("_", "\"", "'", ":", ";", "!", "?")
        rows += Row(listOf(shiftKey()) + "zxcvbnm".mapIndexed { i, c ->
            letter(c, if (numberRow) null else row3Hints[i], if (phonetic) emptyList() else ENGLISH_EXTRAS[c].orEmpty())
        } + deleteKey())
        rows += bottomRow(style, symbolsLabel = if (style.bangla) "?১২৩" else "?123")
        return KeyboardLayout(if (phonetic) "phonetic" else "english", rows)
    }

    /** Bangla National (Jatiyo) layout as arranged on Ridmik's touch keyboard. */
    fun national(numberRow: Boolean, style: BottomRowStyle): KeyboardLayout {
        fun k(n: String, s: String, popup: List<String> = emptyList(), shiftPopup: List<String> = popup) =
            Key(text = n, shiftText = s, popup = popup, shiftPopup = shiftPopup)

        val rows = ArrayList<Row>()
        if (numberRow) rows += numberRow(true)
        val d = if (numberRow) List(10) { null } else DIGITS_BN.map { it }
        fun hint(i: Int, more: List<String> = emptyList()) = listOfNotNull(d[i]) + more
        rows += Row(listOf(
            k("ঙ", "ং", hint(0, listOf("ঁ")), hint(0, listOf("ঃ", "ঁ"))),
            k("য", "য়", hint(1)),
            k("ড", "ঢ", hint(2)),
            k("প", "ফ", hint(3)),
            k("ট", "ঠ", hint(4)),
            k("চ", "ছ", hint(5)),
            k("জ", "ঝ", hint(6)),
            k("হ", "ঞ", hint(7)),
            k("গ", "ঘ", hint(8)),
            k("ড়", "ঢ়", hint(9)),
        ))
        rows += Row(listOf(
            k("ৃ", "ঋ", listOf("ঋ")),
            k("ু", "ূ", listOf("উ"), listOf("ঊ")),
            k("ি", "ী", listOf("ই"), listOf("ঈ")),
            k("া", "অ", listOf("আ"), listOf("আ")),
            k("্", "।", listOf("্\u200C", "\u200C", "\u200D"), listOf("॥", ".")),
            k("ব", "ভ"),
            k("ক", "খ", listOf("ক্ষ")),
            k("ত", "থ", listOf("ৎ"), listOf("ৎ")),
            k("দ", "ধ"),
        ), leftPad = 0.5f, rightPad = 0.5f)
        rows += Row(listOf(shiftKey()) + listOf(
            k("্র", "্য", listOf("র্"), listOf("র\u200D্য")),
            k("ো", "ৌ", listOf("ও"), listOf("ঔ")),
            k("ে", "ৈ", listOf("এ"), listOf("ঐ")),
            k("র", "ল"),
            k("ন", "ণ"),
            k("স", "ষ"),
            k("ম", "শ", listOf("ঃ", "ঁ", "ৎ"), listOf("ঃ", "ঁ")),
        ) + deleteKey())
        rows += bottomRow(style, symbolsLabel = "?১২৩")
        return KeyboardLayout("national", rows)
    }

    private fun bottomRow(style: BottomRowStyle, symbolsLabel: String, alphaLabel: String? = null): Row {
        val keys = ArrayList<Key>()
        keys += if (alphaLabel != null) {
            Key(code = KeyCode.ALPHA, label = alphaLabel, width = 1.5f)
        } else {
            Key(code = KeyCode.SYMBOLS, label = symbolsLabel, width = 1.5f)
        }
        if (style.showLanguageKey) keys += Key(code = KeyCode.LANGUAGE, icon = R.drawable.ic_key_language)
        else keys += Key(code = KeyCode.EMOJI, icon = R.drawable.ic_key_emoji)
        when {
            style.email -> keys += Key(text = "@", popup = listOf(",", ";"))
            style.url -> keys += Key(text = "/", popup = listOf(".com", ".net", ".org", ".bd", ":"))
            else -> keys += Key(text = ",", popup = listOf(";", ":", "'", "\"", "!", "?"))
        }
        keys += Key(code = KeyCode.SPACE, text = " ", width = 4f)
        keys += if (style.bangla && !style.email && !style.url) {
            Key(text = "।", popup = listOf(".", "?", "!", ",", "'", "\"", "-", ":", ";"))
        } else {
            Key(text = ".", popup = listOf("?", "!", ",", "'", "\"", "-", ":", ";", "…"))
        }
        keys += Key(code = KeyCode.ENTER, width = 1.5f, icon = R.drawable.ic_key_enter)
        return Row(keys)
    }

    fun symbols(style: BottomRowStyle): KeyboardLayout {
        val d = digits(style.bangla)
        val currency = if (style.bangla) "৳" else "$"
        val rows = listOf(
            Row(d.mapIndexed { i, s -> Key(text = s, popup = listOf(digits(!style.bangla)[i])) }),
            Row(listOf("@", "#", currency, "_", "&", "-", "+", "(", ")", "/").map { s ->
                Key(text = s, popup = when (s) {
                    "৳", "$" -> listOf("৳", "$", "€", "£", "¥", "₹", "¢")
                    "-" -> listOf("–", "—", "·")
                    "(" -> listOf("[", "{", "<")
                    ")" -> listOf("]", "}", ">")
                    else -> emptyList()
                })
            }),
            Row(listOf(Key(code = KeyCode.SYMBOLS2, label = "=\\<", width = 1.5f)) +
                listOf("*", "\"", "'", ":", ";", "!", "?").map { s ->
                    Key(text = s, popup = when (s) {
                        "\"" -> listOf("“", "”", "„", "«", "»")
                        "'" -> listOf("‘", "’", "‚", "‹", "›")
                        "!" -> listOf("¡")
                        "?" -> listOf("¿")
                        "*" -> listOf("†", "‡", "★")
                        else -> emptyList()
                    })
                } + deleteKey()),
            bottomRow(style, symbolsLabel = "", alphaLabel = if (style.bangla) "অআক" else "ABC"),
        )
        return KeyboardLayout("symbols", rows)
    }

    fun symbols2(style: BottomRowStyle): KeyboardLayout {
        val rows = listOf(
            Row(listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆").map { Key(text = it) }),
            Row(listOf("£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\").map { Key(text = it) }),
            Row(listOf(Key(code = KeyCode.SYMBOLS, label = if (style.bangla) "?১২৩" else "?123", width = 1.5f)) +
                listOf("%", "©", "®", "™", "✓", "[", "]").map { s ->
                    Key(text = s, popup = if (s == "%") listOf("‰") else emptyList())
                } + deleteKey()),
            bottomRow(style, symbolsLabel = "", alphaLabel = if (style.bangla) "অআক" else "ABC"),
        )
        return KeyboardLayout("symbols2", rows)
    }

    fun numpad(phone: Boolean, bangla: Boolean): KeyboardLayout {
        val d = digits(bangla)
        fun n(i: Int, popup: List<String> = emptyList()) = Key(text = d[i], popup = popup, width = 1f)
        val rows = listOf(
            Row(listOf(n(0), n(1), n(2), Key(text = "-", popup = listOf("+", "(", ")", "/"), functional = true))),
            Row(listOf(n(3), n(4), n(5), Key(code = KeyCode.SPACE, text = " ", functional = true))),
            Row(listOf(n(6), n(7, if (phone) listOf("*") else emptyList()), n(8, if (phone) listOf("#") else emptyList()),
                Key(code = KeyCode.DELETE, icon = R.drawable.ic_key_backspace, repeatable = true))),
            Row(listOf(
                if (phone) Key(text = "+", popup = listOf("*", "#", "(", ")", "N", ";", ","), functional = true)
                else Key(text = ",", popup = listOf("+", "*", "#", "/"), functional = true),
                n(9),
                Key(text = ".", popup = listOf(",", ":"), functional = true),
                Key(code = KeyCode.ENTER, icon = R.drawable.ic_key_enter),
            )),
        )
        return KeyboardLayout("numpad", rows)
    }
}
