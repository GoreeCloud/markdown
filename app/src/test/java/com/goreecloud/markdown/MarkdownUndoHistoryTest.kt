package com.goreecloud.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownUndoHistoryTest {
    @Test fun textEditsUndoAndRedoWithoutProviderOperations() {
        val history = MarkdownUndoHistory()
        history.record("# Heading", "# Heading!")
        assertTrue(history.canUndo)
        assertEquals("# Heading", history.undo("# Heading!"))
        assertTrue(history.canRedo)
        assertEquals("# Heading!", history.redo("# Heading"))
    }

    @Test fun undoTracksReplacementsAndUtf16Emoji() {
        val history = MarkdownUndoHistory()
        history.record("a😀b", "a✔b")
        assertEquals("a😀b", history.undo("a✔b"))
        assertEquals("a✔b", history.redo("a😀b"))
    }

    @Test fun editsAfterUndoInvalidateRedo() {
        val history = MarkdownUndoHistory()
        history.record("a", "ab")
        assertEquals("a", history.undo("ab"))
        history.record("a", "ax")
        assertFalse(history.canRedo)
        assertNull(history.redo("ax"))
        assertEquals("a", history.undo("ax"))
    }

    @Test fun noOpDoesNotCreateHistoryOrClearRedo() {
        val history = MarkdownUndoHistory()
        history.record("one", "two")
        assertEquals("one", history.undo("two"))
        history.record("one", "one")
        assertTrue(history.canRedo)
        assertEquals("two", history.redo("one"))
    }

    @Test fun resetNeverCrossesDocumentBoundaries() {
        val history = MarkdownUndoHistory()
        history.record("document A", "changed A")
        history.reset()
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
        assertNull(history.undo("document B"))
    }

    @Test fun externalMutationFailsClosedAndClearsHistory() {
        val history = MarkdownUndoHistory()
        history.record("abc", "abcd")
        assertNull(history.undo("abcz"))
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test fun boundedStepCountDiscardsOldestChange() {
        val history = MarkdownUndoHistory(maxSteps = 2)
        history.record("", "a")
        history.record("a", "ab")
        history.record("ab", "abc")
        assertEquals("ab", history.undo("abc"))
        assertEquals("a", history.undo("ab"))
        assertNull(history.undo("a"))
    }

    @Test fun oversizedChangeCannotCreateUnboundedHistory() {
        val history = MarkdownUndoHistory(maxStoredChars = 4)
        history.record("one", "two")
        assertTrue(history.canUndo)
        history.record("two", "a very long document")
        assertFalse(history.canUndo)
    }

    @Test fun insertDeleteAndRepeatedTextAreReversible() {
        val history = MarkdownUndoHistory()
        history.record("banana banana", "banana banana!")
        history.record("banana banana!", "banana!")
        assertEquals("banana banana!", history.undo("banana!"))
        assertEquals("banana banana", history.undo("banana banana!"))
        assertEquals("banana banana!", history.redo("banana banana"))
        assertEquals("banana!", history.redo("banana banana!"))
    }
}
