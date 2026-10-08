package com.goreecloud.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarkdownSearchTest {
    @Test
    fun findsNextCaseInsensitively() {
        assertEquals(11, MarkdownSearch.findNext("Alpha beta ALPHA", "alpha", 1))
    }

    @Test
    fun wrapsToFirstMatchWhenSearchingPastEnd() {
        assertEquals(0, MarkdownSearch.findNext("Alpha beta", "alpha", 1000))
    }

    @Test
    fun emptyQueryOrMissingMatchReturnsNull() {
        assertNull(MarkdownSearch.findNext("Alpha", ""))
        assertNull(MarkdownSearch.findNext("Alpha", "omega"))
    }
}
