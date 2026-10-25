package com.bornomala.keyboard.engine

/**
 * Reduces both Bangla words and loosely typed English (phonetic) input to a shared, lossy
 * "sound skeleton" so that casual spellings find the right dictionary word: aspirated and
 * unaspirated consonants merge (ত/থ → t), the three sibilants merge (শ/ষ/স → s), long and short
 * vowels merge and the inherent vowel "o" is dropped. Repeated letters collapse.
 *
 * Example: "kotha", "kota" and কথা all become "kta"; "shobai" and সবাই become "sbai".
 */
object PhoneticSkeleton {

    private const val HASANTA = '\u09CD'
    private const val NUKTA = '\u09BC'

    fun fromEnglish(input: String): String {
        val sb = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val c = input[i]
            val lc = c.lowercaseChar()
            val next = input.getOrNull(i + 1)?.lowercaseChar()
            when {
                lc == 'p' && next == 'h' -> { sb.append('f'); i += 2; continue }
                next == 'h' && lc in "kgcjtdbsr" -> { sb.append(lc); i += 2; continue }
                lc == 'e' && next == 'e' -> { sb.append('i'); i += 2; continue }
                lc == 'o' && next == 'o' -> { sb.append('u'); i += 2; continue }
                lc == 'o' -> {}
                lc == 'v' || lc == 'w' -> sb.append('b')
                lc == 'q' -> sb.append('k')
                lc == 'x' -> sb.append("ks")
                c == 'Z' -> sb.append('y')
                lc == 'z' -> sb.append('j')
                lc in 'a'..'z' -> sb.append(lc)
                else -> {}
            }
            i++
        }
        return collapse(sb)
    }

    fun fromBangla(word: String): String {
        val sb = StringBuilder(word.length)
        var i = 0
        val n = word.length
        while (i < n) {
            val c = word[i]
            val next = if (i + 1 < n) word[i + 1] else '\u0000'
            val prev = if (i > 0) word[i - 1] else '\u0000'
            when (c) {
                'আ', 'া' -> sb.append('a')
                'ই', 'ঈ', 'ি', 'ী', 'ঐ', 'ৈ' -> sb.append('i')
                'উ', 'ঊ', 'ু', 'ূ', 'ঔ', 'ৌ' -> sb.append('u')
                'ঋ', 'ৃ' -> sb.append("ri")
                'এ', 'ে' -> sb.append('e')
                'ক' -> {
                    sb.append('k')
                    // ক্ষ is usually typed as "kh".
                    if (next == HASANTA && i + 2 < n && word[i + 2] == 'ষ') i += 2
                }
                'খ' -> sb.append('k')
                'গ', 'ঘ' -> sb.append('g')
                'ঙ', 'ং' -> sb.append("ng")
                'চ', 'ছ' -> sb.append('c')
                'জ', 'ঝ' -> sb.append('j')
                'য' -> when {
                    next == NUKTA -> { sb.append('y'); i++ }
                    prev == HASANTA -> sb.append('y')
                    else -> sb.append('j')
                }
                '\u09DF' -> sb.append('y')
                'ঞ', 'ণ', 'ন' -> sb.append('n')
                'ট', 'ঠ', 'ত', 'থ', 'ৎ' -> sb.append('t')
                'ড', 'ঢ' -> if (next == NUKTA) { sb.append('r'); i++ } else sb.append('d')
                '\u09DC', '\u09DD', 'র' -> sb.append('r')
                'দ', 'ধ' -> sb.append('d')
                'প' -> sb.append('p')
                'ফ' -> sb.append('f')
                'ব', 'ভ' -> sb.append('b')
                'ম' -> sb.append('m')
                'ল' -> sb.append('l')
                'শ', 'ষ', 'স' -> sb.append('s')
                'হ' -> sb.append('h')
                else -> {} // অ, ও, ো, ঁ, ঃ, ্, joiners and everything else carry no skeleton sound.
            }
            i++
        }
        return collapse(sb)
    }

    private fun collapse(sb: StringBuilder): String {
        if (sb.length < 2) return sb.toString()
        val out = StringBuilder(sb.length)
        var last = '\u0000'
        for (c in sb) {
            if (c != last) out.append(c)
            last = c
        }
        return out.toString()
    }
}
