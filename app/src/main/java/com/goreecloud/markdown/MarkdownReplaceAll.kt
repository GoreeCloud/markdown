package com.goreecloud.markdown

/** Result of an explicit, in-memory operation; no document provider is written. */
internal data class MarkdownReplaceAllResult(val text: String, val replacements: Int, val caret: Int)

internal object MarkdownReplaceAll {
    /**
     * Replace non-overlapping plain-text matches using the same case-insensitive
     * search semantics as Find. The caller must capture the current document and
     * match count before confirmation and reject a stale document snapshot.
     * Returns null on invalid input, changed match count or unsafe UTF-8 output.
     */
    fun replaceAll(
        document: String,
        query: String,
        replacement: String,
        expectedMatches: Int,
    ): MarkdownReplaceAllResult? {
        if (query.isEmpty() || expectedMatches <= 0 || document.isEmpty()) return null
        if (MarkdownSearch.countMatches(document, query) != expectedMatches) return null
        val outputChars = document.length.toLong() +
            expectedMatches.toLong() * (replacement.length.toLong() - query.length.toLong())
        if (outputChars < 0 || outputChars > MAX_DOCUMENT_BYTES) return null

        val result = StringBuilder(outputChars.toInt())
        var cursor = 0
        var replacements = 0
        var firstCaret = 0
        while (cursor < document.length) {
            val match = document.indexOf(query, startIndex = cursor, ignoreCase = true)
            if (match < 0) break
            result.append(document, cursor, match).append(replacement)
            if (replacements == 0) firstCaret = result.length
            replacements++
            cursor = match + query.length
        }
        if (replacements != expectedMatches) return null
        result.append(document, cursor, document.length)
        val nextText = result.toString()
        try {
            StrictMarkdownUtf8.encode(nextText)
        } catch (_: Exception) {
            return null
        }
        return MarkdownReplaceAllResult(nextText, replacements, firstCaret)
    }
}
