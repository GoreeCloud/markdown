package com.goreecloud.markdown

import org.junit.Assert.assertEquals
import org.junit.Test

/** The footer position is derived from actual source caret, never mockup data. */
class MarkdownCaretLocationTest {
    @Test fun emptyAndDocumentStartUseOneBasedCoordinates() {
        assertEquals(MarkdownCaretLocation(1, 1), MarkdownCaretLocation.at("", 0))
        assertEquals(MarkdownCaretLocation(1, 1), MarkdownCaretLocation.at("abc", 0))
    }

    @Test fun newlineStartsNextLineAtColumnOne() {
        val source = "title\nbody\n"
        assertEquals(MarkdownCaretLocation(1, 6), MarkdownCaretLocation.at(source, 5))
        assertEquals(MarkdownCaretLocation(2, 1), MarkdownCaretLocation.at(source, 6))
        assertEquals(MarkdownCaretLocation(2, 5), MarkdownCaretLocation.at(source, 10))
        assertEquals(MarkdownCaretLocation(3, 1), MarkdownCaretLocation.at(source, source.length))
    }

    @Test fun emojiAndCombiningMarksUseCodePoints() {
        val source = "🙂a\nCafe\u0301"
        assertEquals(MarkdownCaretLocation(1, 2), MarkdownCaretLocation.at(source, 2))
        assertEquals(MarkdownCaretLocation(1, 3), MarkdownCaretLocation.at(source, 3))
        assertEquals(MarkdownCaretLocation(2, 6), MarkdownCaretLocation.at(source, source.length))
    }

    @Test fun invalidOffsetsAreClampedWithoutCrashing() {
        assertEquals(MarkdownCaretLocation(1, 1), MarkdownCaretLocation.at("abc", -100))
        assertEquals(MarkdownCaretLocation(1, 4), MarkdownCaretLocation.at("abc", 100))
    }

    @Test fun CRLFCountsOnlyLinefeedAsNextSourceLine() {
        assertEquals(MarkdownCaretLocation(2, 1), MarkdownCaretLocation.at("a\r\nb", 3))
        assertEquals(MarkdownCaretLocation(2, 2), MarkdownCaretLocation.at("a\r\nb", 4))
    }
}
