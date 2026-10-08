package com.goreecloud.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownOutlineTest {
    @Test fun extractsSupportedHeadingsAndOffsets() {
        val source = "# Main\nBody\n  ## Section\r\n#### Deep\n"
        val headings = MarkdownOutline.headings(source)
        assertEquals(listOf(1, 2, 4), headings.map { it.level })
        assertEquals(listOf("Main", "Section", "Deep"), headings.map { it.title })
        assertEquals("# Main", source.substring(headings[0].start, headings[0].endExclusive))
        assertEquals("  ## Section", source.substring(headings[1].start, headings[1].endExclusive))
    }

    @Test fun ignoresHashCharactersInsideCodeFences() {
        val source = "# Visible\n```md\n## Hidden\n```\n~~~\n### Hidden too\n~~~\n## Visible too\n"
        assertEquals(listOf("Visible", "Visible too"),
            MarkdownOutline.headings(source).map { it.title })
    }

    @Test fun rejectsIndentedAndNonHeadingLines() {
        val source = "    # code\n#NoSpace\n####### Invalid\nplain text\n"
        assertTrue(MarkdownOutline.headings(source).isEmpty())
    }

    @Test fun literalHashSuffixesArePreserved() {
        val source = "# C#\n## Function##\n### Heading ###\n# ###\n"
        val headings = MarkdownOutline.headings(source)
        assertEquals(listOf("C#", "Function##", "Heading"), headings.map { it.title })
        assertEquals(listOf(1, 2, 3), headings.map { it.level })
    }

    @Test fun emptyInputHasNoHeadings() {
        assertTrue(MarkdownOutline.headings("").isEmpty())
    }
}
