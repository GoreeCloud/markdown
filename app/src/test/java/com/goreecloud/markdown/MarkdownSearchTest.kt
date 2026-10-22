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
    fun previousWrapsAndMatchesCaseInsensitively() {
        assertEquals(11, MarkdownSearch.findPrevious("Alpha beta ALPHA", "alpha", 0))
        assertEquals(0, MarkdownSearch.findPrevious("Alpha beta ALPHA", "alpha", 11))
    }

    @Test
    fun previousDoesNotMatchEmptyQuery() {
        assertNull(MarkdownSearch.findPrevious("Alpha", ""))
        assertNull(MarkdownSearch.findPrevious("Alpha", "unknown"))
    }

    @Test
    fun countsNonOverlappingMatches() {
        assertEquals(2, MarkdownSearch.countMatches("banana BANANA", "ana"))
        assertEquals(0, MarkdownSearch.countMatches("", "ana"))
    }

    @Test
    fun forwardBackwardAndCountAgreeOnOverlaps() {
        val document = "banana"
        assertEquals(1, MarkdownSearch.findNext(document, "ana", 2))
        assertEquals(1, MarkdownSearch.findPrevious(document, "ana", 0))
        assertEquals(1, MarkdownSearch.countMatches(document, "ana"))
    }

    @Test
    fun activeMatchNumberUsesRealNonOverlappingOccurrences() {
        val doc = "Markdown and MARKDOWN again"
        assertEquals(1, MarkdownSearch.matchOrdinal(doc, "markdown", 0))
        assertEquals(2, MarkdownSearch.matchOrdinal(doc, "markdown", 13))
        assertNull(MarkdownSearch.matchOrdinal(doc, "markdown", 1))
    }

    @Test
    fun staleSelectionAndMissingMatchNeverClaimAnOrdinal() {
        assertNull(MarkdownSearch.matchOrdinal("banana", "ana", 3))
        assertEquals(1, MarkdownSearch.matchOrdinal("banana", "ana", 1))
        assertNull(MarkdownSearch.matchOrdinal("x", "", 0))
        assertNull(MarkdownSearch.matchOrdinal("x", "x", null))
        assertNull(MarkdownSearch.matchOrdinal("x", "x", -1))
    }

    @Test
    fun emptyQueryOrMissingMatchReturnsNull() {
        assertNull(MarkdownSearch.findNext("Alpha", ""))
        assertNull(MarkdownSearch.findNext("Alpha", "omega"))
    }
}
