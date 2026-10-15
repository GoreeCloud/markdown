package com.goreecloud.markdown

import java.util.ArrayDeque
import java.security.MessageDigest

/**
 * Session-only, text-based undo/redo. Stores changed spans, not full documents.
 * No provider writes, persistence, or network access.
 */
internal class MarkdownUndoHistory(
    private val maxSteps: Int = 96,
    private val maxStoredChars: Int = 1_048_576,
) {
    private data class Change(
        val offset: Int,
        val removed: String,
        val inserted: String,
        val beforeLength: Int,
        val beforeHash: ByteArray,
        val afterLength: Int,
        val afterHash: ByteArray,
    ) {
        val cost get() = removed.length + inserted.length
    }

    private val past = ArrayDeque<Change>()
    private val future = ArrayDeque<Change>()
    private var storedChars = 0

    val canUndo get() = past.isNotEmpty()
    val canRedo get() = future.isNotEmpty()

    fun reset() {
        past.clear()
        future.clear()
        storedChars = 0
    }

    fun record(before: String, after: String) {
        if (before == after) return
        while (future.isNotEmpty()) storedChars -= future.removeLast().cost

        val prefixLimit = minOf(before.length, after.length)
        var prefix = 0
        while (prefix < prefixLimit && before[prefix] == after[prefix]) prefix++

        var suffix = 0
        val suffixLimit = minOf(before.length - prefix, after.length - prefix)
        while (suffix < suffixLimit &&
            before[before.length - suffix - 1] == after[after.length - suffix - 1]) {
            suffix++
        }

        val change = Change(
            offset = prefix,
            removed = before.substring(prefix, before.length - suffix),
            inserted = after.substring(prefix, after.length - suffix),
            beforeLength = before.length,
            beforeHash = fingerprint(before),
            afterLength = after.length,
            afterHash = fingerprint(after),
        )
        if (maxSteps <= 0 || change.cost > maxStoredChars) {
            reset()
            return
        }
        past.addLast(change)
        storedChars += change.cost
        while (past.size + future.size > maxSteps || storedChars > maxStoredChars) {
            storedChars -= past.removeFirst().cost
        }
    }

    fun undo(current: String): String? {
        val change = past.peekLast() ?: return null
        val restored = applyChange(current, change, reverse = true) ?: run {
            reset()
            return null
        }
        past.removeLast()
        future.addLast(change)
        return restored
    }

    fun redo(current: String): String? {
        val change = future.peekLast() ?: return null
        val restored = applyChange(current, change, reverse = false) ?: run {
            reset()
            return null
        }
        future.removeLast()
        past.addLast(change)
        return restored
    }

    /** Collision-resistant fingerprint of exact UTF-16 code units, including surrogate pairs. */
    private fun fingerprint(text: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        // Hash the same little-endian UTF-16 bytes in chunks instead of invoking
        // MessageDigest twice per code unit on the main editor edit path.
        val bytes = ByteArray(8192)
        var filled = 0
        for (unit in text) {
            if (filled == bytes.size) {
                digest.update(bytes, 0, filled)
                filled = 0
            }
            bytes[filled++] = (unit.code and 0xff).toByte()
            bytes[filled++] = (unit.code ushr 8).toByte()
        }
        if (filled > 0) digest.update(bytes, 0, filled)
        return digest.digest()
    }

    private fun applyChange(current: String, change: Change, reverse: Boolean): String? {
        val expectedLength = if (reverse) change.afterLength else change.beforeLength
        val expectedHash = if (reverse) change.afterHash else change.beforeHash
        val expected = if (reverse) change.inserted else change.removed
        val replacement = if (reverse) change.removed else change.inserted
        val offset = change.offset
        if (current.length != expectedLength || !fingerprint(current).contentEquals(expectedHash) ||
            offset < 0 || offset > current.length - expected.length ||
            !current.regionMatches(offset, expected, 0, expected.length)) return null
        val result = current.replaceRange(offset, offset + expected.length, replacement)
        val resultLength = if (reverse) change.beforeLength else change.afterLength
        val resultHash = if (reverse) change.beforeHash else change.afterHash
        return result.takeIf { it.length == resultLength && fingerprint(it).contentEquals(resultHash) }
    }
}
