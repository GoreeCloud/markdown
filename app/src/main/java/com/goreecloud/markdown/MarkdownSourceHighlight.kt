package com.goreecloud.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

internal enum class MarkdownSourceKind { HEADING, FENCE, CODE, QUOTE, LIST, LINK }
internal data class MarkdownSourceToken(
    val start: Int,
    val endExclusive: Int,
    val kind: MarkdownSourceKind,
)

/**
 * A bounded, line-oriented Markdown source scanner.
 * The document text is never modified. All offsets use UTF-16 indexing, as
 * required by TextFieldValue selection and Compose's Identity offset mapping.
 */
internal object MarkdownSourceSyntax {
    private const val MAX_TOKENS = 20000

    fun tokens(source: String): List<MarkdownSourceToken> {
        val result = ArrayList<MarkdownSourceToken>()
        var start = 0
        var inFence = false
        var fenceCharacter = ' '
        while (start < source.length && result.size < MAX_TOKENS) {
            val newline = source.indexOf('\n', start)
            val end = if (newline < 0) source.length else newline
            var contentStart = start
            while (contentStart < end && contentStart - start < 3 &&
                source[contentStart] == ' ') contentStart++
            val fence = contentStart + 2 < end &&
                (source[contentStart] == '`' || source[contentStart] == '~') &&
                source[contentStart + 1] == source[contentStart] &&
                source[contentStart + 2] == source[contentStart]
            if (fence && (!inFence || fenceCharacter == source[contentStart])) {
                result.add(MarkdownSourceToken(contentStart, end, MarkdownSourceKind.FENCE))
                if (inFence) inFence = false
                else {
                    inFence = true
                    fenceCharacter = source[contentStart]
                }
            } else if (inFence) {
                if (contentStart < end) result.add(MarkdownSourceToken(contentStart, end, MarkdownSourceKind.CODE))
            } else {
                val heading = headingMarkerEnd(source, contentStart, end)
                if (heading != null) {
                    result.add(MarkdownSourceToken(contentStart, end, MarkdownSourceKind.HEADING))
                } else {
                    val prefix = prefixToken(source, contentStart, end)
                    if (prefix != null) result.add(prefix)
                    scanInline(source, contentStart, end, result)
                }
            }
            start = if (newline < 0) source.length else newline + 1
        }
        return result
    }

    private fun headingMarkerEnd(text: String, start: Int, end: Int): Int? {
        var i = start
        while (i < end && i - start < 6 && text[i] == '#') i++
        return if (i > start && i < end && text[i] == ' ') i else null
    }

    private fun prefixToken(text: String, start: Int, end: Int): MarkdownSourceToken? {
        if (start >= end) return null
        if (text[start] == '>') {
            var i = start
            while (i < end && (text[i] == '>' || text[i] == ' ')) i++
            return MarkdownSourceToken(start, i, MarkdownSourceKind.QUOTE)
        }
        if (text[start] in listOf('-', '*', '+') &&
            start + 1 < end && text[start + 1] == ' ') {
            return MarkdownSourceToken(start, start + 1, MarkdownSourceKind.LIST)
        }
        var i = start
        while (i < end && i - start < 9 && text[i] in '0'..'9') i++
        if (i > start && i + 1 < end && text[i] == '.' && text[i + 1] == ' ') {
            return MarkdownSourceToken(start, i + 1, MarkdownSourceKind.LIST)
        }
        return null
    }

    private fun scanInline(
        text: String, start: Int, end: Int, result: MutableList<MarkdownSourceToken>,
    ) {
        var i = start
        while (i < end && result.size < MAX_TOKENS) {
            when (text[i]) {
                '`' -> {
                    val closing = text.indexOf('`', i + 1)
                    if (closing > i + 1 && closing < end) {
                        result.add(MarkdownSourceToken(i, closing + 1, MarkdownSourceKind.CODE))
                        i = closing + 1
                    } else i++
                }
                '[' -> {
                    val close = text.indexOf(']', i + 1)
                    if (close > i + 1 && close + 1 < end && text[close + 1] == '(') {
                        val closeUrl = text.indexOf(')', close + 2)
                        if (closeUrl in (close + 3)..<end) {
                            result.add(MarkdownSourceToken(i, closeUrl + 1, MarkdownSourceKind.LINK))
                            i = closeUrl + 1
                        } else i++
                    } else i++
                }
                else -> i++
            }
        }
    }
}

/** Visual-only transformation: never changes source bytes, selection or IME offsets. */
internal class MarkdownSourceHighlight(
    private val colors: MarkdownWorkspaceColors,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        for (token in MarkdownSourceSyntax.tokens(text.text)) {
            val color: Color = when (token.kind) {
                MarkdownSourceKind.HEADING, MarkdownSourceKind.LINK -> colors.copper
                MarkdownSourceKind.FENCE, MarkdownSourceKind.CODE -> colors.ochre
                MarkdownSourceKind.QUOTE, MarkdownSourceKind.LIST -> colors.forest
            }
            builder.addStyle(
                SpanStyle(
                    color = color,
                    fontWeight = if (token.kind == MarkdownSourceKind.HEADING) FontWeight.Bold else null,
                ),
                token.start, token.endExclusive,
            )
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}
