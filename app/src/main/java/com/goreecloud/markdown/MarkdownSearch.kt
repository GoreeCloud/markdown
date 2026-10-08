package com.goreecloud.markdown

/** Read-only, case-insensitive plain text search with wraparound. */
internal object MarkdownSearch {
    /** Search strictly before a cursor, wrapping to the final match. */
    fun findPrevious(document: String, query: String, before: Int = document.length): Int? {
        if (query.isEmpty() || document.isEmpty()) return null
        val cursor = before.coerceIn(0, document.length)
        val previous = if (cursor > 0) {
            document.lastIndexOf(query, startIndex = cursor - 1, ignoreCase = true)
        } else -1
        return (if (previous >= 0) previous else
            document.lastIndexOf(query, startIndex = document.length, ignoreCase = true)
        ).takeIf { it >= 0 }
    }

    /** Count non-overlapping occurrences consistently with forward navigation. */
    fun countMatches(document: String, query: String): Int {
        if (query.isEmpty() || document.isEmpty()) return 0
        var count = 0
        var offset = 0
        while (offset < document.length) {
            val position = document.indexOf(query, startIndex = offset, ignoreCase = true)
            if (position < 0) break
            count++
            offset = position + query.length
        }
        return count
    }

    fun findNext(document: String, query: String, from: Int = 0): Int? {
        if (query.isEmpty() || document.isEmpty()) return null
        val start = from.coerceIn(0, document.length)
        val next = document.indexOf(query, startIndex = start, ignoreCase = true)
        return if (next >= 0) next else document.indexOf(query, ignoreCase = true).takeIf { it >= 0 }
    }
}
