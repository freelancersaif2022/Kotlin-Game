package com.bornomala.keyboard.engine

import java.io.InputStream

/** Candidates for the word being composed. [defaultIndex] is committed by space / punctuation. */
data class Suggestions(val words: List<String>, val defaultIndex: Int) {
    val default: String? get() = words.getOrNull(defaultIndex)

    companion object {
        val EMPTY = Suggestions(emptyList(), 0)
    }
}

class SuggestionEngine(val user: UserDictionary) {

    @Volatile
    var bangla: BanglaLexicon? = null
        private set

    @Volatile
    var english: EnglishLexicon? = null
        private set

    fun loadBangla(stream: InputStream) {
        bangla = BanglaLexicon(readWords(stream))
    }

    fun loadEnglish(stream: InputStream) {
        english = EnglishLexicon(readWords(stream))
    }

    private fun readWords(stream: InputStream): Array<String> =
        stream.bufferedReader(Charsets.UTF_8).useLines { lines -> lines.filter { it.isNotEmpty() }.toList().toTypedArray() }

    /** Bangla phonetic: [input] is the raw English the user typed. */
    fun phonetic(input: String, autoCorrect: Boolean, limit: Int = 12): Suggestions {
        if (input.isEmpty()) return Suggestions.EMPTY
        val translit = AvroPhonetic.parse(input)
        val learned = user.phoneticChoice(input)
        val lex = bangla
        val skeleton = PhoneticSkeleton.fromEnglish(input)

        var exact: List<String> = emptyList()
        var partial: List<String> = emptyList()
        if (lex != null && skeleton.isNotEmpty()) {
            exact = lex.skeletonExact(skeleton, 8).sortedByDescending { user.count(it) }
            partial = lex.skeletonPrefix(skeleton, 6)
        }
        val corrected = exact.firstOrNull()?.takeIf { top ->
            autoCorrect && learned == null && lex != null && top != translit &&
                user.count(translit) < UserDictionary.TRUSTED &&
                muchMoreCommon(lex.rankOf(top), lex.rankOf(translit), BANGLA_RATIO)
        }

        val list = LinkedHashSet<String>()
        learned?.let { list += it }
        corrected?.let { list += it }
        list += translit
        list += exact
        list += partial
        if (input.length > 1 && input.all { it in 'a'..'z' || it in 'A'..'Z' }) list += input
        return Suggestions(list.take(limit), 0)
    }

    /** Bangla National layout: [word] is the Bangla text typed so far. */
    fun national(word: String, limit: Int = 12): Suggestions {
        if (word.isEmpty()) return Suggestions.EMPTY
        val list = LinkedHashSet<String>()
        list += word
        list += user.completions(word, 3) { w -> w.any { BanglaText.isBangla(it) } }
        bangla?.let { list += it.completions(word, limit) }
        return Suggestions(list.take(limit), 0)
    }

    /** English: [typed] keeps the user's capitalisation. */
    fun english(typed: String, autoCorrect: Boolean, limit: Int = 10): Suggestions {
        if (typed.isEmpty()) return Suggestions.EMPTY
        val lower = typed.lowercase()
        val lex = english
        val list = LinkedHashSet<String>()

        val trusted = user.count(typed) >= UserDictionary.TRUSTED || user.count(lower) >= UserDictionary.TRUSTED
        var best: String? = null
        if (lower == "i") best = "I"
        if (lex != null && !trusted && typed.length >= 2) {
            val typedRank = lex.rankOf(lower)
            val corrections = lex.corrections(lower, 3)
            // A missing apostrophe ("dont", "im") is always fixed; otherwise only clearly better words win.
            val apostrophe = corrections.firstOrNull { it.replace("'", "") == lower }
            val top = apostrophe ?: corrections.firstOrNull()
            if (autoCorrect && top != null && (typed.length >= 3 || apostrophe != null) &&
                (apostrophe != null || muchMoreCommon(lex.rankOf(top), typedRank, ENGLISH_RATIO))
            ) {
                best = matchCase(top, typed)
            }
            corrections.forEach { list += matchCase(it, typed) }
        }

        val ordered = LinkedHashSet<String>()
        best?.let { ordered += it }
        ordered += typed
        ordered += list
        user.completions(lower, 3) { w -> w.all { it.code < 0x80 } }.forEach { ordered += matchCase(it, typed) }
        lex?.completions(lower, 8)?.forEach { ordered += matchCase(it, typed) }
        return Suggestions(ordered.take(limit), 0)
    }

    /** True when the word at [bestRank] is far more frequent than the one at [typedRank] (-1 = unknown). */
    private fun muchMoreCommon(bestRank: Int, typedRank: Int, ratio: Int): Boolean =
        bestRank >= 0 && (typedRank < 0 || bestRank.toLong() * ratio < typedRank)

    private fun matchCase(word: String, typed: String): String = when {
        typed.length > 1 && typed.all { !it.isLetter() || it.isUpperCase() } -> word.uppercase()
        typed.first().isUpperCase() -> word.replaceFirstChar { it.uppercaseChar() }
        else -> word
    }

    private companion object {
        // Phonetic transliterations are noisier than English typing, so Bangla corrects more eagerly.
        const val BANGLA_RATIO = 20
        const val ENGLISH_RATIO = 50
    }
}
