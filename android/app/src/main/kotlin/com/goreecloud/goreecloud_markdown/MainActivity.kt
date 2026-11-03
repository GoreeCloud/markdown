package com.goreecloud.goreecloud_markdown

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * Android SAF integration: provider-issued URI permissions only.
 * No broad media/storage permission, OAuth token, or filesystem-path scraping.
 */
class MainActivity : FlutterActivity() {
    private val channelName = "goreecloud/markdown/documents"
    private val openRequest = 4701
    private val createRequest = 4702
    private val maxBytes = 4 * 1024 * 1024

    private lateinit var channel: MethodChannel
    private var pending: MethodChannel.Result? = null
    private var pendingText: String? = null
    private var initialConsumed = false

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
        channel.setMethodCallHandler { call, result ->
            try {
                when (call.method) {
                    "open" -> openPicker(result)
                    "saveAs" -> createPicker(
                        call.argument<String>("name") ?: "Untitled.md",
                        call.argument<String>("text") ?: "",
                        result
                    )
                    "write" -> {
                        val uri = Uri.parse(call.argument<String>("uri") ?: "")
                        val text = call.argument<String>("text") ?: ""
                        writeDocument(uri, text)
                        result.success(null)
                    }
                    "initialDocument" -> {
                        if (initialConsumed) {
                            result.success(null)
                        } else {
                            initialConsumed = true
                            result.success(readSharedIntent(intent))
                        }
                    }
                    else -> result.notImplemented()
                }
            } catch (e: Exception) {
                result.error("DOCUMENT_ERROR", e.message ?: "Document operation failed", null)
            }
        }
    }

    private fun openPicker(result: MethodChannel.Result) {
        if (pending != null) {
            result.error("BUSY", "A document picker is already open", null)
            return
        }
        pending = result
        val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "text/*", "application/octet-stream", "application/markdown"
            ))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try {
            startActivityForResult(picker, openRequest)
        } catch (e: Exception) {
            pending = null
            result.error("PICKER_ERROR", e.message, null)
        }
    }

    private fun createPicker(name: String, text: String, result: MethodChannel.Result) {
        if (pending != null) {
            result.error("BUSY", "A document picker is already open", null)
            return
        }
        validateBytes(text)
        pending = result
        pendingText = text
        val safeName = name.substringAfterLast('/').substringAfterLast('\\')
            .ifBlank { "Untitled.md" }.take(120)
        val picker = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/markdown"
            putExtra(Intent.EXTRA_TITLE, safeName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try {
            startActivityForResult(picker, createRequest)
        } catch (e: Exception) {
            pending = null
            pendingText = null
            result.error("PICKER_ERROR", e.message, null)
        }
    }

    @Deprecated("Legacy Activity result API is required by FlutterActivity.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != openRequest && requestCode != createRequest) return
        val receiver = pending ?: return
        pending = null
        val textToSave = pendingText
        pendingText = null
        if (resultCode != Activity.RESULT_OK || data?.data == null) {
            receiver.success(null)
            return
        }
        try {
            val uri = data.data!!
            val access = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            try {
                if (access != 0) {
                    contentResolver.takePersistableUriPermission(uri, access)
                }
            } catch (_: SecurityException) {
                // Some document providers grant access only for this session.
            }
            val writable = requestCode == createRequest ||
                (access and Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0
            if (requestCode == createRequest) {
                writeDocument(uri, textToSave ?: "")
            }
            receiver.success(readDocument(uri, writable))
        } catch (e: Exception) {
            receiver.error("DOCUMENT_ERROR", e.message ?: "Cannot open document", null)
        }
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)
        setIntent(newIntent)
        if (!::channel.isInitialized) return
        try {
            val document = readSharedIntent(newIntent)
            if (document != null) channel.invokeMethod("externalDocument", document)
        } catch (_: Exception) {
            // Do not crash the app on malformed or inaccessible external URIs.
        }
    }

    private fun readSharedIntent(incoming: Intent?): Map<String, Any>? {
        if (incoming?.action != Intent.ACTION_VIEW && incoming?.action != Intent.ACTION_EDIT) {
            return null
        }
        val uri = incoming.data ?: return null
        val writable = (incoming.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0
        return readDocument(uri, writable)
    }

    private fun readDocument(uri: Uri, writable: Boolean): Map<String, Any> {
        require(uri.scheme == "content") { "Only content-provider documents are accepted" }
        val output = ByteArrayOutputStream()
        val input = contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Provider did not return readable content")
        input.use { stream ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                if (output.size() + count > maxBytes) {
                    throw IllegalArgumentException("Document exceeds the 4 MiB editing limit")
                }
                output.write(buffer, 0, count)
            }
        }
        val text = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(output.toByteArray())).toString()
        var filename = "Document.md"
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) filename = it.getString(index) ?: filename
            }
        }
        return mapOf("uri" to uri.toString(), "name" to filename,
            "text" to text, "writable" to writable)
    }

    private fun validateBytes(text: String) {
        require(text.toByteArray(StandardCharsets.UTF_8).size <= maxBytes) {
            "Document exceeds the 4 MiB editing limit"
        }
    }

    private fun writeDocument(uri: Uri, text: String) {
        require(uri.scheme == "content") { "Only content-provider documents are accepted" }
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= maxBytes) { "Document exceeds the 4 MiB editing limit" }
        val output = contentResolver.openOutputStream(uri, "wt")
            ?: throw IllegalArgumentException("Provider does not support writing")
        output.use { it.write(bytes); it.flush() }
    }
}
