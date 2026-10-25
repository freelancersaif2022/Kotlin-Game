package com.bornomala.keyboard.engine

import kotlin.math.ln

/**
 * A frequency-ordered word list with a sorted index over an arbitrary key (the word itself or its
 * phonetic skeleton). Index position `i` in [words] is the frequency rank (0 = most frequent).
 */
class SortedIndex(private val keys: Array<String>) {

    private val order: IntArray = keys.indices
        .sortedWith { a, b ->
            val c = keys[a].compareTo(keys[b])
            if (c != 0) c else a - b
        }
        .toIntArray()

    private fun lowerBound(key: String): Int {
        var lo = 0
        var hi = order.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (keys[order[mid]] < key) lo = mid + 1 else hi = mid
        }
        return lo
    }

    /** Ranks whose key equals [key], most frequent first. */
    fun exact(key: String, limit: Int = Int.MAX_VALUE): List<Int> {
        val out = ArrayList<Int>()
        var i = lowerBound(key)
        while (i < order.size && keys[order[i]] == key && out.size < limit) {
            out += order[i]
            i++
        }
        return out
    }

    /** The [limit] most frequent ranks whose key starts with [prefix] (but is not equal to it). */
    fun prefix(prefix: String, limit: Int, scanLimit: Int = 20000): List<Int> {
        if (prefix.isEmpty()) return emptyList()
        val best = IntArray(limit) { Int.MAX_VALUE }
        var count = 0
        var i = lowerBound(prefix)
        var scanned = 0
        while (i < order.size && scanned < scanLimit) {
            val k = keys[order[i]]
            if (!k.startsWith(prefix)) break
            if (k.length > prefix.length) {
                val rank = order[i]
                // Insert into the small sorted "best" array.
                if (count < limit || rank < best[limit - 1]) {
                    var j = minOf(count, limit - 1)
                    while (j > 0 && best[j - 1] > rank) {
                        best[j] = best[j - 1]
                        j--
                    }
                    best[j] = rank
                    if (count < limit) count++
                }
            }
            i++
            scanned++
        }
        return best.take(count)
    }

    fun contains(key: String): Boolean {
        val i = lowerBound(key)
        return i < order.size && keys[order[i]] == key
    }

    fun rankOf(key: String): Int {
        val i = lowerBound(key)
        return if (i < order.size && keys[order[i]] == key) order[i] else -1
    }
}

class BanglaLexicon(val words: Array<String>) {
    private val byWord = SortedIndex(words)
    private val bySkeleton = SortedIndex(Array(words.size) { PhoneticSkeleton.fromBangla(words[it]) })

    fun contains(word: String) = byWord.contains(word)
    fun rankOf(word: String) = byWord.rankOf(word)

    fun skeletonExact(skeleton: String, limit: Int): List<String> =
        bySkeleton.exact(skeleton, limit).map { words[it] }

    fun skeletonPrefix(skeleton: String, limit: Int): List<String> =
        bySkeleton.prefix(skeleton, limit).map { words[it] }

    fun completions(prefix: String, limit: Int): List<String> =
        byWord.prefix(prefix, limit).map { words[it] }
}

class EnglishLexicon(val words: Array<String>) {
    private val byWord = SortedIndex(words)

    fun contains(word: String) = byWord.contains(word)
    fun rankOf(word: String) = byWord.rankOf(word)

    fun completions(prefix: String, limit: Int): List<String> =
        byWord.prefix(prefix, limit).map { words[it] }

    /** Known words one edit (insert, delete, substitute, transpose) away from [word], best first. */
    fun corrections(word: String, limit: Int): List<String> {
        if (word.length < 2 || word.length > 20) return emptyList()
        val found = HashMap<String, Double>()
        fun check(candidate: String, cost: Double) {
            val rank = byWord.rankOf(candidate)
            if (rank >= 0) {
                // Lower is better: rarer words and less likely typos cost more.
                val score = ln(rank + 10.0) + cost
                val old = found[candidate]
                if (old == null || score < old) found[candidate] = score
            }
        }
        val alphabet = "abcdefghijklmnopqrstuvwxyz'"
        val sb = StringBuilder()
        for (i in 0..word.length) {
            for (c in alphabet) {
                sb.setLength(0)
                sb.append(word, 0, i).append(c).append(word, i, word.length)
                val doubled = (i > 0 && word[i - 1] == c) || (i < word.length && word[i] == c)
                check(sb.toString(), if (doubled || c == '\'') COST_DOUBLE else COST_INSERT_DELETE)
            }
            if (i < word.length) {
                val deleted = word.removeRange(i, i + 1)
                val undoubled = (i > 0 && word[i - 1] == word[i])
                check(deleted, if (undoubled) COST_DOUBLE else COST_INSERT_DELETE)
                for (c in alphabet) {
                    if (c == word[i]) continue
                    sb.setLength(0)
                    sb.append(word, 0, i).append(c).append(word, i + 1, word.length)
                    check(sb.toString(), if (KeyProximity.isNear(word[i], c)) COST_NEAR else COST_FAR)
                }
                if (i + 1 < word.length) {
                    val chars = word.toCharArray()
                    val t = chars[i]; chars[i] = chars[i + 1]; chars[i + 1] = t
                    check(String(chars), COST_TRANSPOSE)
                }
            }
        }
        found.remove(word)
        return found.entries.sortedBy { it.value }.take(limit).map { it.key }
    }

    private companion object {
        const val COST_DOUBLE = 0.0
        const val COST_TRANSPOSE = 0.3
        const val COST_NEAR = 1.2
        const val COST_INSERT_DELETE = 1.5
        const val COST_FAR = 3.0
    }
}

/** QWERTY neighbourhood used to rank typo corrections. */
object KeyProximity {
    private val rows = arrayOf("qwertyuiop", "asdfghjkl", "zxcvbnm")

    private fun pos(c: Char): Pair<Int, Int>? {
        for ((r, row) in rows.withIndex()) {
            val i = row.indexOf(c)
            if (i >= 0) return r to i
        }
        return null
    }

    fun isNear(a: Char, b: Char): Boolean {
        val pa = pos(a) ?: return false
        val pb = pos(b) ?: return false
        return kotlin.math.abs(pa.first - pb.first) <= 1 && kotlin.math.abs(pa.second - pb.second) <= 1
    }
}
