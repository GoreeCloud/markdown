package com.goreecloud.markdown

/** Text replacement is an explicit editor action; it does not write to a provider. */
internal data class MarkdownReplacement(val text: String, val caret: Int)

internal object MarkdownReplace {
    /**
     * Replace only a user-selected search match, never another occurrence.
     * All offsets use Kotlin/Android UTF-16 indices. Stale or invalid selections
     * fail closed, protecting text when the document changed after searching.
     */
    fun replaceSelected(
        document: String,
        query: String,
        replacement: String,
        selectionStart: Int,
        selectionEnd: Int,
    ): MarkdownReplacement? {
        if (query.isEmpty() || selectionStart < 0 || selectionEnd < selectionStart) return null
        if (selectionEnd > document.length || selectionEnd - selectionStart != query.length) return null
        if (!document.regionMatches(selectionStart, query, 0, query.length, ignoreCase = true)) return null
        val resultLength = document.length.toLong() - query.length + replacement.length
        if (resultLength > MAX_DOCUMENT_BYTES) return null
        val nextText = document.replaceRange(selectionStart, selectionEnd, replacement)
        // Validate the actual UTF-8 representation before accepting an edit.
        // Never silently replace invalid Unicode or allow an oversized save.
        try {
            StrictMarkdownUtf8.encode(nextText)
        } catch (_: Exception) {
            return null
        }
        return MarkdownReplacement(nextText, selectionStart + replacement.length)
    }
}
