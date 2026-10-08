package com.goreecloud.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.charset.CharacterCodingException

class StrictMarkdownUtf8Test {
    @Test fun unicodeAndNewlinesRoundTrip() {
        val markdown = "# Résumé\n\n- café ☕\n"
        assertEquals(markdown, StrictMarkdownUtf8.decode(StrictMarkdownUtf8.encode(markdown)))
    }

    @Test fun rejectsInvalidUtf8RatherThanSilentlyDestroyingBytes() {
        assertThrows(CharacterCodingException::class.java) {
            StrictMarkdownUtf8.decode(byteArrayOf(0xC3.toByte(), 0x28))
        }
    }

    @Test fun rejectsOversizedDocument() {
        assertThrows(IllegalArgumentException::class.java) {
            StrictMarkdownUtf8.encode("a".repeat(MAX_DOCUMENT_BYTES + 1))
        }
    }
}
