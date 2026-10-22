package com.goreecloud.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class MarkdownHistoryCollisionTest {
    @Test fun undoRejectsDifferentDocumentWithIdenticalLegacyHashCode() {
        assertEquals("Aa!".hashCode(), "BB!".hashCode())
        val history = MarkdownUndoHistory()
        history.record("Aa", "Aa!")
        assertNull(history.undo("BB!"))
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test fun redoRejectsDifferentDocumentWithIdenticalLegacyHashCode() {
        assertEquals("Aa".hashCode(), "BB".hashCode())
        val history = MarkdownUndoHistory()
        history.record("Aa", "Aa!")
        assertEquals("Aa", history.undo("Aa!"))
        assertNull(history.redo("BB"))
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
    }
}
