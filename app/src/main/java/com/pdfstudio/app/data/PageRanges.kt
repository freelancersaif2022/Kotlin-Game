package com.pdfstudio.app.data

/** Parses and formats page selections such as "1-3, 5, 8". Pages are 1-based in text, 0-based in lists. */
object PageRanges {
    sealed interface Result {
        data object Empty : Result
        data class Valid(val pages: List<Int>) : Result
        data class Invalid(val message: String) : Result
    }

    private val separators = Regex("[,;\\s]+")

    fun parse(input: String, pageCount: Int): Result {
        val tokens = input.split(separators).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return Result.Empty
        val pages = LinkedHashSet<Int>()
        for (token in tokens) {
            val parts = token.split('-')
            when {
                parts.size == 1 -> {
                    val n = parts[0].toIntOrNull() ?: return Result.Invalid("\"$token\" is not a page number")
                    if (n !in 1..pageCount) return Result.Invalid("Page $n doesn't exist (1–$pageCount)")
                    pages += n - 1
                }
                parts.size == 2 -> {
                    if (parts[1].isEmpty()) return Result.Invalid("Finish the range \"$token\"")
                    val a = parts[0].toIntOrNull() ?: return Result.Invalid("\"$token\" is not a valid range")
                    val b = parts[1].toIntOrNull() ?: return Result.Invalid("\"$token\" is not a valid range")
                    if (a !in 1..pageCount || b !in 1..pageCount) {
                        return Result.Invalid("Range $a-$b is outside 1–$pageCount")
                    }
                    val range = if (a <= b) a..b else a downTo b
                    range.forEach { pages += it - 1 }
                }
                else -> return Result.Invalid("\"$token\" is not a valid range")
            }
        }
        return Result.Valid(pages.toList())
    }

    /** Formats a set of 0-based pages compactly, e.g. {0,1,2,4} -> "1-3, 5". */
    fun format(pages: Collection<Int>): String {
        val sorted = pages.toSortedSet().toList()
        if (sorted.isEmpty()) return ""
        val out = StringBuilder()
        var start = sorted[0]
        var prev = start
        fun flush() {
            if (out.isNotEmpty()) out.append(", ")
            out.append(start + 1)
            if (prev > start) out.append('-').append(prev + 1)
        }
        for (i in 1 until sorted.size) {
            val p = sorted[i]
            if (p == prev + 1) {
                prev = p
            } else {
                flush()
                start = p
                prev = p
            }
        }
        flush()
        return out.toString()
    }
}
