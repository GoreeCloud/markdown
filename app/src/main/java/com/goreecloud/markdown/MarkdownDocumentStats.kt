package com.goreecloud.markdown

/** Source-text statistics; computed locally without indexing or telemetry. */
internal data class MarkdownDocumentStats(val lines: Int, val words: Int, val characters: Int) {
    companion object {
        fun from(text: String): MarkdownDocumentStats {
            var lines = 1
            var words = 0
            var characters = 0
            var inWord = false
            var offset = 0
            while (offset < text.length) {
                val point = Character.codePointAt(text, offset)
                if (point == '\n'.code) lines++
                val isWordPart = Character.isLetterOrDigit(point) ||
                    Character.getType(point) == Character.NON_SPACING_MARK.toInt() ||
                    Character.getType(point) == Character.COMBINING_SPACING_MARK.toInt()
                if (isWordPart && !inWord) words++
                inWord = isWordPart
                characters++
                offset += Character.charCount(point)
            }
            return MarkdownDocumentStats(lines, words, characters)
        }
    }
}
