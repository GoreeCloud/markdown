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

    @Test fun malformedUtf16IsRejectedBeforeSaving() {
        assertThrows(CharacterCodingException::class.java) {
            StrictMarkdownUtf8.encode("bad\uD800")
        }
        assertThrows(CharacterCodingException::class.java) {
            StrictMarkdownUtf8.encode("\uDC00")
        }
    }

    @Test fun supplementaryUnicodeRoundTrips() {
        val content = "Emoji 😀✨"
        assertEquals(content, StrictMarkdownUtf8.decode(StrictMarkdownUtf8.encode(content)))
    }

    @Test fun exactByteLimitAcceptedAndMultibyteOverflowRejected() {
        assertEquals(MAX_DOCUMENT_BYTES, StrictMarkdownUtf8.encode("x".repeat(MAX_DOCUMENT_BYTES)).size)
        assertThrows(IllegalArgumentException::class.java) {
            StrictMarkdownUtf8.encode("é".repeat(MAX_DOCUMENT_BYTES / 2 + 1))
        }
    }
}
