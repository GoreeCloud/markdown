package com.goreecloud.markdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Presentation-only identity and status surface. Do not infer provider durability
 * from dirty=false alone while a private recovery draft or failure remains.
 * Labels always accompany semantic color.
 */
@Composable
internal fun MarkdownWorkspaceHeader(
    fileName: String,
    dirty: Boolean,
    busy: Boolean,
    hasFile: Boolean,
    recoveryPending: Boolean,
    notice: String?,
    lines: Int,
    words: Int,
    characters: Int,
    compact: Boolean,
    tones: MarkdownWorkspaceColors,
) {
    val visualStatus = markdownDocumentVisualStatus(
        dirty = dirty,
        busy = busy,
        hasFile = hasFile,
        recoveryPending = recoveryPending,
        notice = notice,
    )
    val status = visualStatus.label
    val (surface, ink) = when (visualStatus) {
        MarkdownVisualStatus.WORKING, MarkdownVisualStatus.ATTENTION ->
            tones.ochreSoft to tones.ochreInk
        MarkdownVisualStatus.UNSAVED -> tones.copperSoft to tones.copperInk
        MarkdownVisualStatus.SAVED -> tones.forestSoft to tones.forestInk
    }
    if (compact) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                fileName,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(status, style = MaterialTheme.typography.labelLarge, color = ink)
        }
        return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = tones.panel,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "GOREECLOUD  /  WRITING",
                        color = tones.copper,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        "Markdown",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                Surface(color = surface, shape = RoundedCornerShape(50.dp)) {
                    Text(
                        status,
                        color = ink,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    )
                }
            }
            Text(
                fileName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (LocalDensity.current.fontScale >= 1.35f) {
                Text(
                    "$lines lines  •  $words words  •  $characters characters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MarkdownMetric(lines, "Lines", tones.forestSoft, tones.forestInk, Modifier.weight(1f))
                    MarkdownMetric(words, "Words", tones.ochreSoft, tones.ochreInk, Modifier.weight(1f))
                    MarkdownMetric(characters, "Chars", tones.copperSoft, tones.copperInk, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MarkdownMetric(
    count: Int,
    label: String,
    background: Color,
    foreground: Color,
    modifier: Modifier,
) {
    Surface(modifier = modifier, color = background, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)) {
            Text(count.toString(), color = foreground, style = MaterialTheme.typography.titleMedium)
            Text(label, color = foreground, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** UI-neutral document status. The provider, not this function, verifies durability. */
internal enum class MarkdownVisualStatus(val label: String) {
    WORKING("Working"), ATTENTION("Needs attention"), UNSAVED("Unsaved"), SAVED("Saved")
}

/** Fail closed on ambiguous recovery and provider messages, never claim a clean save. */
internal fun markdownDocumentVisualStatus(
    dirty: Boolean,
    busy: Boolean,
    hasFile: Boolean,
    recoveryPending: Boolean,
    notice: String?,
): MarkdownVisualStatus {
    val unresolved = recoveryPending || notice?.let {
        it.startsWith("Save unverified", ignoreCase = true) ||
            it.startsWith("Open failed", ignoreCase = true) ||
            it.contains("could not", ignoreCase = true) ||
            it.contains("cleanup is pending", ignoreCase = true) ||
            it.contains("recovery copy remains", ignoreCase = true) ||
            it.contains("recovery draft remains", ignoreCase = true)
    } == true
    return when {
        busy -> MarkdownVisualStatus.WORKING
        unresolved -> MarkdownVisualStatus.ATTENTION
        dirty || !hasFile -> MarkdownVisualStatus.UNSAVED
        else -> MarkdownVisualStatus.SAVED
    }
}
