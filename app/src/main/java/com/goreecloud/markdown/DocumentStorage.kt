package com.goreecloud.markdown

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Uses only an explicitly user-selected document URI.
 * External document providers may not offer atomic replace semantics.
 */
internal class DocumentStorage(private val context: Context) {
    fun read(uri: Uri): String {
        val source = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open the selected document.")
        source.use { input ->
            val bytes = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                if (count == 0) continue
                if (bytes.size() + count > MAX_DOCUMENT_BYTES) {
                    throw IOException("Document exceeds the 2 MiB development safety limit.")
                }
                bytes.write(buffer, 0, count)
            }
            return StrictMarkdownUtf8.decode(bytes.toByteArray())
        }
    }

    fun displayName(uri: Uri): String {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (col >= 0) cursor.getString(col)?.take(120) else null
                    } else null
                } ?: "Untitled.md"
        } catch (_: Exception) {
            "Untitled.md"
        }
    }

    fun recoveryDraft(uri: Uri): String? {
        val file = recoveryFile(uri)
        if (!file.isFile) return null
        return StrictMarkdownUtf8.decode(file.readBytes().also {
            if (it.size > MAX_DOCUMENT_BYTES) throw IOException("Recovery draft exceeds the safety limit.")
        })
    }

    /**
     * Writes a private, fsynced draft first. Only deletes that draft if a provider
     * write and byte-for-byte readback both succeed. The draft is intentionally
     * kept on failure for possible recovery on a subsequent Open.
     */
    fun save(uri: Uri, text: String, expectedPersistedText: String? = null) {
        val contents = StrictMarkdownUtf8.encode(text)
        val rescue = recoveryFile(uri)
        rescue.parentFile?.mkdirs()
        // Stage on the same private filesystem before replacing a previous rescue
        // draft. A crash during staging must not truncate the older recovery copy.
        val staging = File(rescue.parentFile, "${rescue.name}.pending")
        FileOutputStream(staging).use {
            it.write(contents)
            it.fd.sync()
        }
        if (!staging.renameTo(rescue)) {
            throw IOException("Unable to prepare a private recovery draft; the provider was not written.")
        }

        // Detect edits made by another application since the document was opened
        // or last saved. The SAF provider may still race between this read and the
        // subsequent write; this is conflict detection, not atomic replacement.
        if (expectedPersistedText != null) {
            DocumentConflictGuard.verify(expectedPersistedText, read(uri))
        }
        val destination = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Cannot write the selected document.")
        destination.use {
            it.write(contents)
            it.flush()
        }
        if (read(uri) != text) {
            throw IOException("The document provider did not return the saved content; draft retained.")
        }
        if (!rescue.delete()) {
            // The provider copy is verified, but a redundant private draft remains.
            throw IOException("Saved document verified, but private recovery cleanup failed.")
        }
    }

    private fun recoveryFile(uri: Uri): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(uri.toString().toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(File(context.filesDir, "recovery"), "$digest.draft")
    }
}

internal const val MAX_DOCUMENT_BYTES = 2 * 1024 * 1024

internal object StrictMarkdownUtf8 {
    fun encode(text: String): ByteArray {
        require(text.length <= MAX_DOCUMENT_BYTES) { "Document exceeds the 2 MiB development safety limit." }
        val buffer = StandardCharsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .encode(java.nio.CharBuffer.wrap(text))
        require(buffer.remaining() <= MAX_DOCUMENT_BYTES) { "Document exceeds the 2 MiB development safety limit." }
        return ByteArray(buffer.remaining()).also { buffer.get(it) }
    }

    fun decode(bytes: ByteArray): String {
        require(bytes.size <= MAX_DOCUMENT_BYTES) { "Document exceeds the 2 MiB development safety limit." }
        return StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    }
}

/** Pure pre-write comparison to prevent silent overwrites of observed changes. */
internal object DocumentConflictGuard {
    fun verify(expectedPersistedText: String, currentProviderText: String) {
        if (expectedPersistedText != currentProviderText) {
            throw IOException(
                "The document changed outside GoreeCloud Markdown. " +
                    "It was not overwritten; use Save as to preserve both versions."
            )
        }
    }
}
