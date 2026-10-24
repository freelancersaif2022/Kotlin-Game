package com.pdfmaster.app.util

object PageRanges {
    /**
     * Parses "1-3, 5, 8-6" into zero-based page groups. Returns null when the text is invalid
     * or refers to pages outside 1..[pageCount].
     */
    fun parse(text: String, pageCount: Int): List<List<Int>>? {
        val parts = text.split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null
        return parts.map { part ->
            val bounds = part.split('-', '–').map { it.trim() }
            when (bounds.size) {
                1 -> {
                    val n = bounds[0].toIntOrNull() ?: return null
                    if (n !in 1..pageCount) return null
                    listOf(n - 1)
                }
                2 -> {
                    val a = bounds[0].toIntOrNull() ?: return null
                    val b = bounds[1].toIntOrNull() ?: return null
                    if (a !in 1..pageCount || b !in 1..pageCount) return null
                    if (a <= b) (a - 1..b - 1).toList() else (a - 1 downTo b - 1).toList()
                }
                else -> return null
            }
        }
    }
}
