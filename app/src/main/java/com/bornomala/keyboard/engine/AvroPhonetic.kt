package com.bornomala.keyboard.engine

import java.text.Normalizer

/**
 * Avro-style phonetic transliteration (English letters to Bangla).
 *
 * The engine follows the classic Avro phonetic algorithm: the input is case-folded except for
 * case-sensitive letters, then the longest matching pattern is replaced at every position. A
 * pattern may carry rules that inspect the neighbouring input characters (vowel / consonant /
 * punctuation / exact text) to choose between, for example, a vowel sign (কার) and an
 * independent vowel.
 */
object AvroPhonetic {

    private const val VOWELS = "aeiou"
    private const val CONSONANTS = "bcdfghjklmnpqrstvwxyz"
    private const val CASE_SENSITIVE = "oiudgjnrstyz"

    private enum class Scope { VOWEL, CONSONANT, PUNCTUATION, EXACT }

    private class Cond(val prefix: Boolean, val scope: Scope, val negate: Boolean, val value: String)
    private class Rule(val replace: String, val conds: List<Cond>)
    private class Pattern(val find: String, val replace: String, val rules: List<Rule>)

    /** Condition DSL: "<" = prefix, ">" = suffix, then optional "!" and the scope or `=text`. */
    private fun cond(spec: String): Cond {
        val prefix = spec[0] == '<'
        var rest = spec.substring(1)
        val negate = rest.startsWith("!")
        if (negate) rest = rest.substring(1)
        return when {
            rest.startsWith("=") -> Cond(prefix, Scope.EXACT, negate, rest.substring(1))
            rest == "vowel" -> Cond(prefix, Scope.VOWEL, negate, "")
            rest == "consonant" -> Cond(prefix, Scope.CONSONANT, negate, "")
            rest == "punctuation" -> Cond(prefix, Scope.PUNCTUATION, negate, "")
            else -> error("bad condition $spec")
        }
    }

    private fun rule(replace: String, vararg conds: String) = Rule(replace, conds.map(::cond))

    private val patternsByFirst: Map<Char, List<Pattern>>

    init {
        val list = ArrayList<Pattern>(400)
        fun p(find: String, replace: String, vararg rules: Rule) {
            list += Pattern(find, replace, rules.toList())
        }

        // Vowels that have both a vowel sign (কার) and an independent form.
        fun vowel(find: String, kar: String, full: String) = p(
            find, kar,
            rule(full, "<!consonant", ">!=`"),
            rule(full, "<punctuation", ">!=`"),
        )
        vowel("i", "ি", "ই")
        vowel("I", "ী", "ঈ")
        vowel("ee", "ী", "ঈ")
        vowel("u", "ু", "উ")
        vowel("U", "ূ", "ঊ")
        vowel("oo", "ু", "উ")
        vowel("e", "ে", "এ")
        vowel("OI", "ৈ", "ঐ")
        vowel("OU", "ৌ", "ঔ")
        vowel("O", "ো", "ও")
        vowel("rri", "ৃ", "ঋ")
        p(
            "a", "া",
            rule("আ", "<punctuation", ">!=`"),
            rule("য়া", "<!consonant", "<!=a", ">!=`"),
            rule("আ", "<=a", ">!=`"),
        )
        p(
            "o", "",
            rule("ও", "<vowel", "<!=o"),
            rule("অ", "<vowel", "<=o"),
            rule("অ", "<punctuation"),
        )

        // Consonants and conjuncts.
        val plain = arrayOf(
            "bdh" to "ব্ধ", "bhl" to "ভ্ল", "bj" to "ব্জ", "bd" to "ব্দ", "bb" to "ব্ব", "bl" to "ব্ল",
            "bh" to "ভ", "vl" to "ভ্ল", "b" to "ব", "v" to "ভ",
            "cNG" to "চ্ঞ", "cch" to "চ্ছ", "cc" to "চ্চ", "ch" to "ছ", "c" to "চ",
            "dhn" to "ধ্ন", "dhm" to "ধ্ম", "dgh" to "দ্ঘ", "ddh" to "দ্ধ", "dbh" to "দ্ভ", "dv" to "দ্ভ",
            "dm" to "দ্ম", "DD" to "ড্ড", "Dh" to "ঢ", "dh" to "ধ", "dg" to "দ্গ", "dd" to "দ্দ",
            "D" to "ড", "d" to "দ",
            "fl" to "ফ্ল", "f" to "ফ",
            "ghn" to "ঘ্ন", "Ghn" to "ঘ্ন", "gdh" to "গ্ধ", "gN" to "গ্ণ", "GN" to "গ্ণ", "gn" to "গ্ন",
            "Gn" to "গ্ন", "gm" to "গ্ম", "Gm" to "গ্ম", "gl" to "গ্ল", "Gl" to "গ্ল", "gg" to "জ্ঞ",
            "GG" to "জ্ঞ", "Gg" to "জ্ঞ", "gG" to "জ্ঞ", "gh" to "ঘ", "Gh" to "ঘ", "g" to "গ", "G" to "গ",
            "hN" to "হ্ণ", "hn" to "হ্ন", "hm" to "হ্ম", "hl" to "হ্ল", "h" to "হ",
            "jjh" to "জ্ঝ", "jNG" to "জ্ঞ", "jh" to "ঝ", "jj" to "জ্জ", "j" to "জ", "J" to "জ",
            "kkhN" to "ক্ষ্ণ", "kShN" to "ক্ষ্ণ", "kkhm" to "ক্ষ্ম", "kShm" to "ক্ষ্ম", "kxN" to "ক্ষ্ণ",
            "kxm" to "ক্ষ্ম", "kkh" to "ক্ষ", "kSh" to "ক্ষ", "ksh" to "কশ", "kx" to "ক্ষ", "kk" to "ক্ক",
            "kT" to "ক্ট", "kt" to "ক্ত", "kl" to "ক্ল", "ks" to "ক্স", "kh" to "খ", "k" to "ক",
            "lbh" to "ল্ভ", "ldh" to "ল্ধ", "lkh" to "লখ", "lgh" to "লঘ", "lph" to "লফ", "lk" to "ল্ক",
            "lg" to "ল্গ", "lT" to "ল্ট", "lD" to "ল্ড", "lp" to "ল্প", "lv" to "ল্ভ", "lm" to "ল্ম",
            "ll" to "ল্ল", "lb" to "ল্ব", "l" to "ল",
            "mth" to "ম্থ", "mph" to "ম্ফ", "mbh" to "ম্ভ", "mpl" to "মপ্ল", "mn" to "ম্ন", "mp" to "ম্প",
            "mv" to "ম্ভ", "mm" to "ম্ম", "ml" to "ম্ল", "mb" to "ম্ব", "mf" to "ম্ফ", "m" to "ম",
            "NgkSh" to "ঙ্ক্ষ", "Ngkkh" to "ঙ্ক্ষ", "NGch" to "ঞ্ছ", "Nggh" to "ঙ্ঘ", "Ngkh" to "ঙ্খ",
            "NGjh" to "ঞ্ঝ", "ngOU" to "ঙ্গৌ", "ngOI" to "ঙ্গৈ", "Ngkx" to "ঙ্ক্ষ", "NGc" to "ঞ্চ",
            "nch" to "ঞ্ছ", "njh" to "ঞ্ঝ", "ngh" to "ঙ্ঘ", "Ngk" to "ঙ্ক", "Ngx" to "ঙ্ষ", "Ngg" to "ঙ্গ",
            "Ngm" to "ঙ্ম", "NGj" to "ঞ্জ", "ndh" to "ন্ধ", "nTh" to "ন্ঠ", "NTh" to "ণ্ঠ", "nth" to "ন্থ",
            "nkh" to "ঙ্খ", "ngo" to "ঙ্গ", "nga" to "ঙ্গা", "ngi" to "ঙ্গি", "ngI" to "ঙ্গী",
            "ngu" to "ঙ্গু", "ngU" to "ঙ্গূ", "nge" to "ঙ্গে", "ngO" to "ঙ্গো", "NDh" to "ণ্ঢ",
            "nsh" to "নশ", "Ngr" to "ঙর", "NGr" to "ঞর", "ngr" to "ংর", "nj" to "ঞ্জ", "Ng" to "ঙ",
            "NG" to "ঞ", "nk" to "ঙ্ক", "ng" to "ং", "nn" to "ন্ন", "NN" to "ণ্ণ", "Nn" to "ণ্ন",
            "nm" to "ন্ম", "Nm" to "ণ্ম", "nd" to "ন্দ", "nT" to "ন্ট", "NT" to "ণ্ট", "nD" to "ন্ড",
            "ND" to "ণ্ড", "nt" to "ন্ত", "ns" to "ন্স", "nc" to "ঞ্চ", "n" to "ন", "N" to "ণ",
            "phl" to "ফ্ল", "pT" to "প্ট", "pt" to "প্ত", "pn" to "প্ন", "pp" to "প্প", "pl" to "প্ল",
            "ps" to "প্স", "ph" to "ফ", "p" to "প",
            "q" to "ক",
            "rZ" to "র\u200D্য", "Rg" to "ড়্গ", "Rh" to "ঢ়", "R" to "ড়",
            "shch" to "শ্ছ", "ShTh" to "ষ্ঠ", "Shph" to "ষ্ফ", "sch" to "স্ছ", "skl" to "স্ক্ল",
            "skh" to "স্খ", "sth" to "স্থ", "sph" to "স্ফ", "shc" to "শ্চ", "sht" to "শ্ত", "shn" to "শ্ন",
            "shm" to "শ্ম", "shl" to "শ্ল", "Shk" to "ষ্ক", "ShT" to "ষ্ট", "ShN" to "ষ্ণ", "Shp" to "ষ্প",
            "Shf" to "ষ্ফ", "Shm" to "ষ্ম", "spl" to "স্প্ল", "sk" to "স্ক", "Sh" to "ষ", "sh" to "শ",
            "sT" to "স্ট", "st" to "স্ত", "sp" to "স্প", "sf" to "স্ফ", "sm" to "স্ম", "sn" to "স্ন",
            "sl" to "স্ল", "ss" to "স্স", "S" to "শ", "s" to "স",
            "tth" to "ত্থ", "t``" to "ৎ", "TT" to "ট্ট", "Tm" to "ট্ম", "Th" to "ঠ", "tn" to "ত্ন",
            "tm" to "ত্ম", "th" to "থ", "tt" to "ত্ত", "T" to "ট", "t" to "ত",
            "Y" to "য়", "z" to "য", "Z" to "্য",
            // Signs and punctuation.
            "^`" to "^", "^" to "ঁ", ":`" to ":", ":" to "ঃ", ".`" to ".", "." to "।", "$" to "৳",
            ",," to "্\u200C", "`" to "",
            "0" to "০", "1" to "১", "2" to "২", "3" to "৩", "4" to "৪",
            "5" to "৫", "6" to "৬", "7" to "৭", "8" to "৮", "9" to "৯",
        )
        for ((f, r) in plain) p(f, r)

        p(
            "rr", "রর",
            rule("র্", "<!consonant", ">!vowel", ">!=r", ">!punctuation"),
            rule("্রর", "<consonant", "<!=r"),
        )
        p("r", "র", rule("্র", "<consonant", "<!=r"))
        p("y", "য়", rule("্য", "<consonant"), rule("ইয়", "<punctuation"))
        p("wa", "ওয়া", rule("্বা", "<consonant"))
        p("w", "ও", rule("ওয়", "<punctuation", ">vowel"), rule("্ব", "<consonant"))
        p("x", "ক্স", rule("এক্স", "<punctuation"))

        patternsByFirst = list
            .sortedByDescending { it.find.length }
            .groupBy { it.find[0] }
    }

    private fun isVowel(c: Char) = VOWELS.indexOf(c.lowercaseChar()) >= 0
    private fun isConsonant(c: Char) = CONSONANTS.indexOf(c.lowercaseChar()) >= 0
    private fun isPunctuation(c: Char) = !isVowel(c) && !isConsonant(c)

    private fun fix(input: String): String {
        val sb = StringBuilder(input.length)
        for (c in input) sb.append(if (CASE_SENSITIVE.indexOf(c.lowercaseChar()) >= 0) c else c.lowercaseChar())
        return sb.toString()
    }

    private fun matches(c: Cond, s: String, start: Int, end: Int): Boolean {
        val chk = if (c.prefix) start - 1 else end
        val inRange = chk >= 0 && chk < s.length
        val result = when (c.scope) {
            Scope.VOWEL -> inRange && isVowel(s[chk])
            Scope.CONSONANT -> inRange && isConsonant(s[chk])
            Scope.PUNCTUATION -> !inRange || isPunctuation(s[chk])
            Scope.EXACT -> if (c.prefix) {
                start >= c.value.length && s.regionMatches(start - c.value.length, c.value, 0, c.value.length)
            } else {
                s.regionMatches(end, c.value, 0, c.value.length)
            }
        }
        return result != c.negate
    }

    /** Transliterates [input] (English letters) into Bangla. */
    fun parse(input: String): String {
        if (input.isEmpty()) return ""
        val s = fix(input)
        val out = StringBuilder(s.length * 2)
        var cur = 0
        while (cur < s.length) {
            val candidates = patternsByFirst[s[cur]]
            var matched = false
            if (candidates != null) {
                for (pattern in candidates) {
                    val end = cur + pattern.find.length
                    if (end > s.length || !s.regionMatches(cur, pattern.find, 0, pattern.find.length)) continue
                    var replace = pattern.replace
                    for (rule in pattern.rules) {
                        if (rule.conds.all { matches(it, s, cur, end) }) {
                            replace = rule.replace
                            break
                        }
                    }
                    out.append(replace)
                    cur = end
                    matched = true
                    break
                }
            }
            if (!matched) {
                out.append(s[cur])
                cur++
            }
        }
        return Normalizer.normalize(out, Normalizer.Form.NFC)
    }

    /** Characters that are part of a phonetic word while it is being composed. */
    fun isWordChar(c: Char, composing: Boolean): Boolean =
        c in 'a'..'z' || c in 'A'..'Z' || c == '`' || c == '^' || (composing && c == ':')
}
