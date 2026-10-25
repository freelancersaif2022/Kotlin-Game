package com.bornomala.keyboard.engine

import java.io.BufferedReader
import java.io.Writer

/**
 * Words the user typed or picked, plus remembered phonetic choices ("ami" → "আমি").
 * All data stays on the device. Thread-safe.
 */
class UserDictionary {

    private val counts = HashMap<String, Int>()
    private val phoneticChoices = HashMap<String, String>()

    @Volatile
    var dirty = false
        private set

    @Synchronized
    fun learn(word: String, weight: Int = 1) {
        if (word.isBlank() || word.length > 40) return
        counts[word] = (counts[word] ?: 0) + weight
        if (counts.size > MAX_WORDS) trim()
        dirty = true
    }

    @Synchronized
    fun rememberPhonetic(input: String, word: String) {
        if (input.isEmpty() || word.isEmpty()) return
        phoneticChoices[input] = word
        if (phoneticChoices.size > MAX_WORDS) phoneticChoices.remove(phoneticChoices.keys.first())
        dirty = true
    }

    @Synchronized
    fun phoneticChoice(input: String): String? = phoneticChoices[input]

    @Synchronized
    fun count(word: String): Int = counts[word] ?: 0

    /** Learned words that start with [prefix] and are trusted (picked or typed repeatedly). */
    @Synchronized
    fun completions(prefix: String, limit: Int, filter: (String) -> Boolean = { true }): List<String> =
        counts.entries
            .filter { it.value >= TRUSTED && it.key.length > prefix.length && it.key.startsWith(prefix) && filter(it.key) }
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }

    @Synchronized
    fun matching(limit: Int, predicate: (String) -> Boolean): List<String> =
        counts.entries
            .filter { it.value >= TRUSTED && predicate(it.key) }
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }

    @Synchronized
    fun allWords(): List<Pair<String, Int>> = counts.entries.sortedByDescending { it.value }.map { it.key to it.value }

    @Synchronized
    fun remove(word: String) {
        counts.remove(word)
        phoneticChoices.entries.removeAll { it.value == word }
        dirty = true
    }

    @Synchronized
    fun clear() {
        counts.clear()
        phoneticChoices.clear()
        dirty = true
    }

    private fun trim() {
        val keep = counts.entries.sortedByDescending { it.value }.take(MAX_WORDS * 3 / 4).map { it.key to it.value }
        counts.clear()
        counts.putAll(keep)
    }

    /** Writes all data; [markSaved] clears the dirty flag (false for exports). */
    @Synchronized
    fun write(writer: Writer, markSaved: Boolean = true) {
        for ((w, c) in counts) writer.write("w\t$w\t$c\n")
        for ((k, v) in phoneticChoices) writer.write("p\t$k\t$v\n")
        writer.flush()
        if (markSaved) dirty = false
    }

    @Synchronized
    fun read(reader: BufferedReader, merge: Boolean = false) {
        if (!merge) {
            counts.clear()
            phoneticChoices.clear()
        }
        reader.forEachLine { line ->
            val parts = line.split('\t')
            if (parts.size != 3) return@forEachLine
            when (parts[0]) {
                "w" -> parts[2].toIntOrNull()?.let { counts[parts[1]] = maxOf(counts[parts[1]] ?: 0, it) }
                "p" -> phoneticChoices[parts[1]] = parts[2]
            }
        }
        dirty = merge
    }

    companion object {
        const val TRUSTED = 2
        private const val MAX_WORDS = 5000
    }
}
