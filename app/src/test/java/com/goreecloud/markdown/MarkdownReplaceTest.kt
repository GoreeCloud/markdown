package com.goreecloud.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarkdownReplaceTest {
    @Test fun replacesOnlyTheSelectedOccurrence() {
        val oldText = "one ONE one"
        val result = MarkdownReplace.replaceSelected(oldText, "one", "two", 4, 7)
        assertEquals("one two one", result?.text)
        assertEquals(7, result?.caret)
        assertEquals("one ONE one", oldText)
    }

    @Test fun rejectsStaleSelectionAndIncorrectRanges() {
        assertNull(MarkdownReplace.replaceSelected("one other", "one", "two", 4, 7))
        assertNull(MarkdownReplace.replaceSelected("one", "one", "two", -1, 2))
        assertNull(MarkdownReplace.replaceSelected("one", "one", "two", 0, 4))
        assertNull(MarkdownReplace.replaceSelected("one", "one", "two", 2, 1))
    }

    @Test fun neverReplacesWhenNoQueryIsSupplied() {
        assertNull(MarkdownReplace.replaceSelected("one", "", "new", 0, 0))
    }

    @Test fun permitsEmptyReplacementAsExplicitDeletion() {
        val result = MarkdownReplace.replaceSelected("one two", "one", "", 0, 3)
        assertEquals(" two", result?.text)
        assertEquals(0, result?.caret)
    }

    @Test fun limitsUtf8BytesRatherThanOnlyCharacterCount() {
        assertNull(MarkdownReplace.replaceSelected(
            "é", "é", "é".repeat(MAX_DOCUMENT_BYTES / 2 + 1), 0, 1
        ))
    }

    @Test fun rejectsInvalidUtf16WithoutAlteringTheOriginal() {
        val source = "one"
        assertNull(MarkdownReplace.replaceSelected(source, "one", "\uD800", 0, 3))
        assertEquals("one", source)
    }

    @Test fun preservesUtf16CaretOffsetsForEmoji() {
        val result = MarkdownReplace.replaceSelected("one two", "one", "😀", 0, 3)
        assertEquals("😀 two", result?.text)
        assertEquals(2, result?.caret)
    }
}
