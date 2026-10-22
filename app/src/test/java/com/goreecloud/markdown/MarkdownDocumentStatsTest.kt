package com.goreecloud.markdown

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownDocumentStatsTest {
    @Test fun emptyDocumentHasOneEditorLine() {
        assertEquals(MarkdownDocumentStats(1, 0, 0), MarkdownDocumentStats.from(""))
    }

    @Test fun newlineAndUnicodeCodepoints() {
        assertEquals(
            MarkdownDocumentStats(3, 3, 21),
            MarkdownDocumentStats.from("# Café 😀\nSecond-line\n"),
        )
    }

    @Test fun combiningMarksBelongToWords() {
        assertEquals(
            MarkdownDocumentStats(1, 2, 8),
            MarkdownDocumentStats.from("Cafe\u0301 au"),
        )
    }

    @Test fun punctuationDoesNotCountAsAWord() {
        assertEquals(MarkdownDocumentStats(1, 0, 4), MarkdownDocumentStats.from("---!"))
    }
}
