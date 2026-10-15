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
}
