package com.goreecloud.markdown

/**
 * One-based logical source position for the footer in GoreeCloud Markdown.
 * Columns count Unicode code points, matching source character statistics.
 * Visual tab stops and grapheme clusters may occupy different screen widths.
 */
internal data class MarkdownCaretLocation(val line: Int, val column: Int) {
    companion object {
        fun at(text: String, selectionEnd: Int): MarkdownCaretLocation {
            val offset = selectionEnd.coerceIn(0, text.length)
            var line = 1
            var lineStart = 0
            for (index in 0 until offset) {
                if (text[index] == '\n') {
                    line++
                    lineStart = index + 1
                }
            }
            val codePoints = Character.codePointCount(text, lineStart, offset - lineStart)
            return MarkdownCaretLocation(line = line, column = codePoints + 1)
        }
    }
}
