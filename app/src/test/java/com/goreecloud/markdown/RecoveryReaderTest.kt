package com.goreecloud.markdown

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryReaderTest {
    @Test fun readableDraftReturned() {
        val result = RecoveryReader.readSafely { "# draft" }
        assertEquals("# draft", result.draft)
        assertFalse(result.readFailed)
    }
    @Test fun absentDraftIsFine() {
        val result = RecoveryReader.readSafely { null }
        assertNull(result.draft)
        assertFalse(result.readFailed)
    }
    @Test fun brokenDraftIsReportedWithoutCrashing() {
        val result = RecoveryReader.readSafely { throw IOException("bad draft") }
        assertNull(result.draft)
        assertTrue(result.readFailed)
    }
}
