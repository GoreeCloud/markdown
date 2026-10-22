package com.goreecloud.markdown

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BoundedDocumentBytesTest {
    @Test fun exactLimitIsAccepted() {
        val bytes = ByteArray(MAX_DOCUMENT_BYTES) { 0x61 }
        assertArrayEquals(bytes, BoundedDocumentBytes.read(ByteArrayInputStream(bytes)))
    }

    @Test fun oneBytePastLimitIsRejected() {
        val bytes = ByteArray(MAX_DOCUMENT_BYTES + 1) { 0x61 }
        assertThrows(IOException::class.java) {
            BoundedDocumentBytes.read(ByteArrayInputStream(bytes))
        }
    }

    @Test fun shortReadsRemainLossless() {
        val source = byteArrayOf(0x61, 0x62, 0x63, 0x0A)
        val input = object : ByteArrayInputStream(source) {
            override fun read(b: ByteArray, off: Int, len: Int): Int =
                super.read(b, off, minOf(len, 1))
        }
        assertArrayEquals(source, BoundedDocumentBytes.read(input))
    }

    @Test fun unexpectedZeroCountBulkReadFallsBackToSingleByte() {
        val input = object : InputStream() {
            var first = true
            val remaining = ByteArrayInputStream(byteArrayOf(0x42, 0x43))
            override fun read(): Int = remaining.read()
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (first) {
                    first = false
                    return 0
                }
                return remaining.read(buffer, offset, length)
            }
        }
        assertArrayEquals(byteArrayOf(0x42, 0x43), BoundedDocumentBytes.read(input))
    }

    @Test fun zeroBulkReadAlsoEnforcesSizeBoundary() {
        val remaining = ByteArrayInputStream(ByteArray(MAX_DOCUMENT_BYTES + 1))
        val input = object : InputStream() {
            var first = true
            override fun read(): Int = remaining.read()
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (first) { first = false; return 0 }
                return remaining.read(buffer, offset, length)
            }
        }
        assertThrows(IOException::class.java) { BoundedDocumentBytes.read(input) }
    }

    @Test fun streamIOExceptionIsNotSuppressed() {
        val input = object : InputStream() {
            override fun read(): Int = throw IOException("provider failure")
        }
        val thrown = assertThrows(IOException::class.java) { BoundedDocumentBytes.read(input) }
        assertEquals("provider failure", thrown.message)
    }

    @Test fun emptyStreamIsEmpty() {
        assertEquals(0, BoundedDocumentBytes.read(ByteArrayInputStream(byteArrayOf())).size)
    }
}
