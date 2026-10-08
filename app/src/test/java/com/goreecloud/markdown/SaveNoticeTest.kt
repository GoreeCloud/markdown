package com.goreecloud.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveNoticeTest {
    @Test fun cleanSaveHasSuccessMessage() {
        assertEquals("Save verified.", SaveNotice.message(false, false))
    }

    @Test fun newerEditsStayUnsaved() {
        val message = SaveNotice.message(true, false)
        assertTrue(message.contains("newer changes remain"))
    }

    @Test fun cleanupWarningDoesNotMisreportSaveFailure() {
        val message = SaveNotice.message(false, true)
        assertTrue(message.startsWith("Save verified."))
        assertTrue(message.contains("could not be removed"))
    }

    @Test fun cleanupWarningAndNewEditsAreBothReported() {
        val message = SaveNotice.message(true, true)
        assertTrue(message.contains("newer changes remain"))
        assertTrue(message.contains("cleanup is pending"))
    }
}
