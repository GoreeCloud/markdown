package com.goreecloud.markdown

/** Read-only, case-insensitive plain text search with wraparound. */
internal object MarkdownSearch {
    fun findNext(document: String, query: String, from: Int = 0): Int? {
        if (query.isEmpty() || document.isEmpty()) return null
        val start = from.coerceIn(0, document.length)
        val next = document.indexOf(query, startIndex = start, ignoreCase = true)
        return if (next >= 0) next else document.indexOf(query, ignoreCase = true).takeIf { it >= 0 }
    }
}
