package com.bornomala.keyboard.engine

/**
 * Smart composition rules for the Bangla National (Jatiyo) layout.
 *
 * - A vowel sign typed after hasanta becomes the independent vowel (্ + া → আ), as in the
 *   standard Jatiyo layout.
 * - A vowel sign typed where no consonant precedes it (word start, after another vowel) becomes
 *   the independent vowel, so users never end up with a dangling কার.
 * - অ + া → আ.
 */
object NationalComposer {

    /** Result of composing: delete [deleteBefore] chars before the cursor, then insert [text]. */
    data class Edit(val deleteBefore: Int, val text: String)

    fun compose(previous: Char?, typed: String): Edit {
        if (typed.length != 1) return Edit(0, typed)
        val c = typed[0]
        val vowel = BanglaText.independentVowel(c) ?: return Edit(0, typed)
        return when {
            previous == BanglaText.HASANTA -> Edit(1, vowel.toString())
            previous == 'অ' && c == 'া' -> Edit(1, "আ")
            previous == null || !(BanglaText.isConsonant(previous) || previous == BanglaText.NUKTA) ->
                Edit(0, vowel.toString())
            else -> Edit(0, typed)
        }
    }
}
