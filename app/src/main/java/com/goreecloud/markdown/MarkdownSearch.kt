package com.goreecloud.markdown

/** Read-only, case-insensitive plain text search with wraparound. */
internal object MarkdownSearch {
    /** Navigate the same non-overlapping matches used by countMatches. */
    fun findPrevious(document: String, query: String, before: Int = document.length): Int? {
        if (query.isEmpty() || document.isEmpty()) return null
        val cursor = before.coerceIn(0, document.length)
        var offset = 0
        var previous: Int? = null
        var last: Int? = null
        while (offset < document.length) {
            val position = document.indexOf(query, startIndex = offset, ignoreCase = true)
            if (position < 0) break
            if (position < cursor) previous = position
            last = position
            offset = position + query.length
        }
        return previous ?: last
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

    /** One-based match number for a known non-overlapping match, or null if stale. */
    fun matchOrdinal(document: String, query: String, selectedStart: Int?): Int? {
        if (selectedStart == null || selectedStart < 0 ||
            query.isEmpty() || document.isEmpty()) return null
        var offset = 0
        var ordinal = 0
        while (offset < document.length) {
            val index = document.indexOf(query, startIndex = offset, ignoreCase = true)
            if (index < 0 || index > selectedStart) return null
            ordinal++
            if (index == selectedStart) return ordinal
            offset = index + query.length
        }
        return null
    }

    fun findNext(document: String, query: String, from: Int = 0): Int? {
        if (query.isEmpty() || document.isEmpty()) return null
        val start = from.coerceIn(0, document.length)
        var offset = 0
        var first: Int? = null
        while (offset < document.length) {
            val position = document.indexOf(query, startIndex = offset, ignoreCase = true)
            if (position < 0) break
            if (first == null) first = position
            if (position >= start) return position
            offset = position + query.length
        }
        return first
    }
}
