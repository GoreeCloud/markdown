package com.goreecloud.markdown

internal data class MarkdownHeading(
    val level: Int,
    val title: String,
    val start: Int,
    val endExclusive: Int,
)

/** Lightweight ATX heading navigation. Ignores fenced code and keeps UTF-16 offsets. */
internal object MarkdownOutline {
    fun headings(markdown: String): List<MarkdownHeading> {
        val result = mutableListOf<MarkdownHeading>()
        var cursor = 0
        var fenceCharacter: Char? = null
        var fenceWidth = 0
        while (cursor < markdown.length) {
            val newline = markdown.indexOf('\n', cursor)
            val end = if (newline < 0) markdown.length else newline
            val line = markdown.substring(cursor, end).removeSuffix("\r")
            val indentation = line.takeWhile { it == ' ' }.length
            if (indentation <= 3) {
                val body = line.substring(indentation)
                val character = body.firstOrNull()
                val runLength = if (character == '`' || character == '~') {
                    body.takeWhile { it == character }.length
                } else 0
                if (fenceCharacter == null && runLength >= 3) {
                    fenceCharacter = character
                    fenceWidth = runLength
                } else if (fenceCharacter == character && runLength >= fenceWidth &&
                    body.drop(runLength).isBlank()
                ) {
                    fenceCharacter = null
                    fenceWidth = 0
                } else if (fenceCharacter == null) {
                    val marks = body.takeWhile { it == '#' }.length
                    if (marks in 1..6 && body.length > marks && body[marks].isWhitespace()) {
                        val title = body.drop(marks).trim().trimEnd('#').trim()
                        if (title.isNotEmpty()) {
                            result.add(MarkdownHeading(marks, title, cursor, cursor + line.length))
                        }
                    }
                }
            }
            cursor = if (newline < 0) markdown.length else newline + 1
        }
        return result
    }
}
