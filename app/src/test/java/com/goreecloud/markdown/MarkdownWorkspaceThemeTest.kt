package com.goreecloud.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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

    @Test fun contextualSemanticGroupsRemainDistinct() {
        for (dark in listOf(false, true)) {
            val tones = markdownWorkspaceColors(dark)
            assertNotEquals(tones.copperSoft, tones.forestSoft)
            assertNotEquals(tones.forestSoft, tones.ochreSoft)
            assertNotEquals(tones.ochreSoft, tones.copperSoft)
            assertNotEquals(tones.canvas, tones.panel)
        }
    }
}
