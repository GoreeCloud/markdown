package com.goreecloud.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarkdownReplaceAllTest {
    @Test fun replacesNonOverlappingMatchesCaseInsensitively() {
        val result = MarkdownReplaceAll.replaceAll("one ONE one", "one", "😀", 3)
        assertEquals("😀 😀 😀", result?.text)
        assertEquals(3, result?.replacements)
        assertEquals(2, result?.caret)
    }

    @Test fun matchesFindsNonOverlappingSemantics() {
        assertEquals("bXnX", MarkdownReplaceAll.replaceAll("bananana", "ana", "X", 2)?.text)
        assertEquals(" ", MarkdownReplaceAll.replaceAll("a a", "a", "", 2)?.text)
    }

    @Test fun rejectsEmptySearchAndStaleMatchCount() {
        assertNull(MarkdownReplaceAll.replaceAll("one", "", "two", 1))
        assertNull(MarkdownReplaceAll.replaceAll("one", "one", "two", 2))
        assertNull(MarkdownReplaceAll.replaceAll("one", "one", "two", 0))
        assertNull(MarkdownReplaceAll.replaceAll("", "one", "two", 1))
    }

    @Test fun rejectsInvalidUtf16WithoutChangingOriginalText() {
        val original = "one"
        assertNull(MarkdownReplaceAll.replaceAll(original, "one", "\uD800", 1))
        assertEquals("one", original)
    }

    @Test fun rejectsOversizedUtf8OutputBeforeProviderWrite() {
        val oversized = "😀".repeat(MAX_DOCUMENT_BYTES / 4)
        assertNull(MarkdownReplaceAll.replaceAll("aa", "a", oversized, 2))
    }

    @Test fun preservesDocumentWhenReplacementEqualsExistingText() {
        assertEquals("one", MarkdownReplaceAll.replaceAll("one", "one", "one", 1)?.text)
    }

    @Test fun handlesEndOfDocumentAndEmojiOffsets() {
        val result = MarkdownReplaceAll.replaceAll("one", "one", "😀", 1)
        assertEquals("😀", result?.text)
        assertEquals(2, result?.caret)
    }
}
