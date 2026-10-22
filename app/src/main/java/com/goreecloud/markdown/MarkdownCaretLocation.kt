package com.goreecloud.markdown

/**
 * One-based logical source coordinates, counting Unicode code points per line.
 * Tabs and grapheme clusters can occupy different display widths.
 */
internal data class MarkdownCaretLocation(val line: Int, val column: Int) {
    companion object {
        fun at(text: String, selectionEnd: Int): MarkdownCaretLocation =
            MarkdownCaretIndex(text).at(selectionEnd)
    }
}

/**
 * Index UTF-16 line starts once per document revision so cursor-only movement
 * avoids rescanning the entire document prefix. No provider data is persisted.
 */
internal class MarkdownCaretIndex(private val text: String) {
    private val lineStarts: IntArray = run {
        var lines = 1
        for (ch in text) if (ch == '\n') lines++
        val starts = IntArray(lines)
        var index = 1
        for (offset in text.indices) {
            if (text[offset] == '\n') starts[index++] = offset + 1
        }
        starts
    }

    fun at(selectionEnd: Int): MarkdownCaretLocation {
        val end = selectionEnd.coerceIn(0, text.length)
        val search = lineStarts.binarySearch(end)
        val lineIndex = if (search >= 0) search else -search - 2
        val start = lineStarts[lineIndex]
        return MarkdownCaretLocation(
            line = lineIndex + 1,
            column = Character.codePointCount(text, start, end) + 1,
        )
    }
}
