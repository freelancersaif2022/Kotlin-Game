package com.bornomala.keyboard.engine

/** Small helpers for Bangla script handling. */
object BanglaText {

    const val HASANTA = '\u09CD'
    const val NUKTA = '\u09BC'
    const val ZWNJ = '\u200C'
    const val ZWJ = '\u200D'

    private val karToVowel = mapOf(
        'া' to 'আ', 'ি' to 'ই', 'ী' to 'ঈ', 'ু' to 'উ', 'ূ' to 'ঊ',
        'ৃ' to 'ঋ', 'ে' to 'এ', 'ৈ' to 'ঐ', 'ো' to 'ও', 'ৌ' to 'ঔ',
    )

    fun isKar(c: Char) = c in karToVowel

    fun independentVowel(kar: Char): Char? = karToVowel[kar]

    fun isConsonant(c: Char): Boolean =
        c in 'ক'..'হ' || c == 'ড়' || c == 'ঢ়' || c == 'য়' || c == 'ৎ'

    /** True for any character belonging to the Bengali Unicode block or a joiner. */
    fun isBangla(c: Char): Boolean = c in 'ঀ'..'৿' || c == ZWNJ || c == ZWJ

    /** Letters and signs that make up a Bangla word (digits and the danda excluded). */
    fun isBanglaWordChar(c: Char): Boolean =
        (c in 'ঀ'..'ৣ' && c != '।' && c != '॥') || c == 'ৰ' || c == 'ৱ' ||
            c == ZWNJ || c == ZWJ

    private const val BANGLA_DIGITS = "০১২৩৪৫৬৭৮৯"

    fun toBanglaDigits(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) sb.append(if (c in '0'..'9') BANGLA_DIGITS[c - '0'] else c)
        return sb.toString()
    }
}
