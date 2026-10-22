package com.goreecloud.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Guard readable, meaningfully differentiated color coding in both modes. */
class MarkdownWorkspaceThemeTest {
    private fun contrast(foreground: Color, background: Color): Float {
        val a = foreground.luminance()
        val b = background.luminance()
        return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
    }

    @Test fun statusAndMetricLabelsHaveAccessibleContrastInBothModes() {
        for (dark in listOf(false, true)) {
            val tones = markdownWorkspaceColors(dark)
            val pairs = listOf(
                tones.copperInk to tones.copperSoft,
                tones.forestInk to tones.forestSoft,
                tones.ochreInk to tones.ochreSoft,
            )
            for ((foreground, background) in pairs) {
                assertTrue("Contrast below 4.5:1 in dark=$dark",
                    contrast(foreground, background) >= 4.5f)
            }
        }
    }

    @Test fun semanticAccentTextRemainsReadableOnEveryWorkspaceSurface() {
        for (dark in listOf(false, true)) {
            val tones = markdownWorkspaceColors(dark)
            val surfaces = listOf(tones.canvas, tones.panel, tones.editor)
            val accents = listOf(tones.copper, tones.forest, tones.ochre)
            for (foreground in accents) {
                for (background in surfaces) {
                    assertTrue(
                        "Workspace accent contrast below 4.5:1 in dark=$dark",
                        contrast(foreground, background) >= 4.5f,
                    )
                }
            }
        }
    }

    @Test fun contextualSemanticGroupsRemainDistinct() {
        for (dark in listOf(false, true)) {
            val tones = markdownWorkspaceColors(dark)
            assertNotEquals(tones.copperSoft, tones.forestSoft)
            assertNotEquals(tones.forestSoft, tones.ochreSoft)
            assertNotEquals(tones.ochreSoft, tones.copperSoft)
            assertNotEquals(tones.canvas, tones.panel)
        }
    }
    @Test fun mockupNewDocumentIsUnsavedUntilProviderVerifiesIt() {
        assertEquals(
            MarkdownVisualStatus.UNSAVED,
            markdownDocumentVisualStatus(false, false, false, false, null),
        )
        assertEquals(
            MarkdownVisualStatus.SAVED,
            markdownDocumentVisualStatus(false, false, true, false, "Save verified."),
        )
    }

    @Test fun unresolvedRecoveryNeverDisplaysGreenSaved() {
        val pending = listOf(
            "The recovery draft matches the saved document. No save is needed. The private recovery copy remains until verified cleanup.",
            "Save verified. A redundant private recovery draft could not be removed.",
            "Saved earlier edit; newer changes remain. Private recovery cleanup is pending.",
            "Save unverified or failed. A private recovery draft may remain.",
        )
        for (message in pending) {
            assertEquals(
                "Misleading Saved state for message: $message",
                MarkdownVisualStatus.ATTENTION,
                markdownDocumentVisualStatus(false, false, true, false, message),
            )
        }
        assertEquals(
            MarkdownVisualStatus.ATTENTION,
            markdownDocumentVisualStatus(false, false, true, true, null),
        )
    }

    @Test fun workingAndUnsavedStatusesRemainTextualAndDeterministic() {
        assertEquals(
            MarkdownVisualStatus.WORKING,
            markdownDocumentVisualStatus(true, true, true, true, null),
        )
        assertEquals(
            MarkdownVisualStatus.UNSAVED,
            markdownDocumentVisualStatus(true, false, true, false, "Saved earlier edit; newer changes remain."),
        )
        assertEquals(
            "Unsaved",
            markdownDocumentVisualStatus(true, false, true, false, null).label,
        )
    }

    @Test fun dismissingRecoveryNoticeDoesNotClearUnderlyingAttention() {
        val state = EditorState(
            text = "saved source",
            persistedText = "saved source",
            recoveryDraft = "older private source",
            recoveryPending = true,
            notice = "A private recovery draft is available.",
        )
        // Dismissal changes the UI message and may hide the dialog, but it
        // cannot certify deletion of the separate private recovery file.
        val dismissed = state.copy(recoveryDraft = null, notice = null)
        assertTrue(dismissed.recoveryPending)
        assertEquals(
            MarkdownVisualStatus.ATTENTION,
            markdownDocumentVisualStatus(
                dirty = dismissed.dirty,
                busy = dismissed.busy,
                hasFile = true,
                recoveryPending = dismissed.recoveryPending,
                notice = dismissed.notice,
            ),
        )
        val cleaned = dismissed.copy(recoveryPending = false)
        assertEquals(
            MarkdownVisualStatus.SAVED,
            markdownDocumentVisualStatus(
                dirty = cleaned.dirty,
                busy = cleaned.busy,
                hasFile = true,
                recoveryPending = cleaned.recoveryPending,
                notice = cleaned.notice,
            ),
        )
    }


    @Test fun failedSaveNeedsAttentionEvenAfterWarningDismissed() {
        val failedSave = EditorState(
            text = "provider's last verified bytes",
            persistedText = "provider's last verified bytes",
            recoveryPending = true,
            notice = "Save unverified or failed. A private recovery draft may remain.",
        )
        val dismissed = failedSave.copy(notice = null)
        assertEquals(
            MarkdownVisualStatus.ATTENTION,
            markdownDocumentVisualStatus(
                dirty = dismissed.dirty,
                busy = dismissed.busy,
                hasFile = true,
                recoveryPending = dismissed.recoveryPending,
                notice = dismissed.notice,
            ),
        )
        assertTrue(dismissed.recoveryPending)
    }

    @Test fun recoveryDialogYieldsToDiscardConfirmationWithoutLosingDraft() {
        assertTrue(RecoveryDialogPresentation.show(
            available = true, discardConfirmationPending = false,
        ))
        assertTrue(!RecoveryDialogPresentation.show(
            available = true, discardConfirmationPending = true,
        ))
        // Canceling the discard confirmation makes the same recovery prompt
        // visible again: no recovery bytes or editing state were discarded.
        assertTrue(RecoveryDialogPresentation.show(
            available = true, discardConfirmationPending = false,
        ))
    }

    @Test fun recoveryDialogIsAbsentAfterRecoveryIsResolved() {
        assertTrue(!RecoveryDialogPresentation.show(
            available = false, discardConfirmationPending = false,
        ))
        assertTrue(!RecoveryDialogPresentation.show(
            available = false, discardConfirmationPending = true,
        ))
    }

    @Test fun discardDialogAlwaysOwnsBackNavigationUntilResolved() {
        val allowed = {
            pending: Boolean -> MarkdownModalFocus.allowUnsavedBack(
                dirty = true, busy = false, keyboardVisible = false,
                findOpen = false, outlineOpen = false,
                replaceConfirmationOpen = false, discardConfirmationOpen = pending,
            )
        }
        assertTrue(allowed(false))
        assertTrue(!allowed(true))
        assertTrue(allowed(false))
    }

    @Test fun modalAndKeyboardStatesPreventUnsavedExitHandler() {
        fun allowed(ime: Boolean = false, find: Boolean = false,
                    outline: Boolean = false, replace: Boolean = false) =
            MarkdownModalFocus.allowUnsavedBack(
                dirty = true, busy = false, keyboardVisible = ime,
                findOpen = find, outlineOpen = outline,
                replaceConfirmationOpen = replace, discardConfirmationOpen = false,
            )
        assertTrue(!allowed(ime = true))
        assertTrue(!allowed(find = true))
        assertTrue(!allowed(outline = true))
        assertTrue(!allowed(replace = true))
        assertTrue(!MarkdownModalFocus.allowUnsavedBack(
            dirty = false, busy = false, keyboardVisible = false,
            findOpen = false, outlineOpen = false,
            replaceConfirmationOpen = false, discardConfirmationOpen = false,
        ))
    }

    @Test fun outlineAndReplacementFocusWaitForSheetDismissal() {
        assertTrue(!MarkdownModalFocus.restoreEditorFocus(
            focusPending = true, preview = false, findOpen = true,
        ))
        assertTrue(MarkdownModalFocus.restoreEditorFocus(
            focusPending = true, preview = false, findOpen = false,
        ))
        assertTrue(!MarkdownModalFocus.restoreEditorFocus(
            focusPending = false, preview = false, findOpen = false,
        ))
        assertTrue(!MarkdownModalFocus.restoreEditorFocus(
            focusPending = true, preview = true, findOpen = false,
        ))
    }

}
