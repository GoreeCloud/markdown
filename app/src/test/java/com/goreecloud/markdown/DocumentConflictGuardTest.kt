package com.goreecloud.markdown

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DocumentConflictGuardTest {
    @Test
    fun sameContentAllowsSavePreflight() {
        DocumentConflictGuard.verify("# Heading\nBody", "# Heading\nBody")
    }

    @Test
    fun externalEditIsBlockedBeforeProviderWrite() {
        try {
            DocumentConflictGuard.verify("Original", "Externally updated")
            fail("Expected changed document to be rejected")
        } catch (error: IOException) {
            assertTrue(error.message.orEmpty().contains("not overwritten"))
        }
    }

    @Test
    fun changedLineEndingsAlsoCountAsConflict() {
        try {
            DocumentConflictGuard.verify("One\nTwo", "One\r\nTwo")
            fail("Expected different bytes-as-text to be rejected")
        } catch (expected: IOException) {
            assertTrue(expected.message.orEmpty().contains("changed outside"))
        }
    }
    @Test
    fun identicalRecoveryBytesAllowSaveRetry() {
        val previous = "Unsaved 🔐".toByteArray(Charsets.UTF_8)
        RecoveryDraftOverwriteGuard.verify(previous, previous.clone())
    }

    @Test
    fun differentRecoveryBytesArePreserved() {
        val previous = "Older unsaved text".toByteArray(Charsets.UTF_8)
        val candidate = "New edits".toByteArray(Charsets.UTF_8)
        try {
            RecoveryDraftOverwriteGuard.verify(previous, candidate)
            fail("Expected recovery conflict")
        } catch (error: IOException) {
            assertTrue(error.message.orEmpty().contains("not overwritten"))
            assertTrue(error.message.orEmpty().contains("Save as"))
        }
        assertEquals("Older unsaved text", previous.toString(Charsets.UTF_8))
    }

    @Test
    fun evenSmallUnicodeDifferencesRefuseRecoveryReplacement() {
        try {
            RecoveryDraftOverwriteGuard.verify(
                "café".toByteArray(Charsets.UTF_8),
                "cafe".toByteArray(Charsets.UTF_8),
            )
            fail("Distinct recovery bytes must not be silently overwritten")
        } catch (expected: IOException) {
            assertTrue(expected.message.orEmpty().contains("recovery"))
        }
    }

    @Test
    fun emptyPreviousDraftDoesNotEqualNonemptyEdits() {
        try {
            RecoveryDraftOverwriteGuard.verify(byteArrayOf(), "new".toByteArray(Charsets.UTF_8))
            fail("Empty recovery is still a separate document version")
        } catch (_: IOException) {
            // Existing file is preserved even when empty.
        }
    }

}
