package com.goreecloud.markdown

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MarkdownActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MarkdownApp() }
    }
}

internal data class EditorState(
    val uri: Uri? = null,
    val fileName: String = "Untitled.md",
    val text: String = "",
    val persistedText: String = "",
    val busy: Boolean = false,
    val notice: String? = null,
    val recoveryDraft: String? = null,
) {
    val dirty get() = text != persistedText
}

internal class EditorViewModel : ViewModel() {
    private val mutable = MutableStateFlow(EditorState())
    val state = mutable.asStateFlow()

    fun edit(text: String) = mutable.update { it.copy(text = text, notice = null) }
    fun reset() = mutable.update { EditorState() }
    fun dismissNotice() = mutable.update { it.copy(notice = null) }
    fun restoreDraft() = mutable.update {
        it.copy(text = it.recoveryDraft ?: it.text, recoveryDraft = null,
            notice = "Private recovery draft restored. Save to verify the document.")
    }
    fun dismissRecovery() = mutable.update { it.copy(recoveryDraft = null) }

    fun open(context: Context, uri: Uri) {
        val appContext = context.applicationContext
        mutable.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val storage = DocumentStorage(appContext)
                    val text = storage.read(uri)
                    Triple(storage.displayName(uri), text, RecoveryReader.readSafely { storage.recoveryDraft(uri) })
                }
                mutable.value = EditorState(
                    uri = uri, fileName = result.first,
                    text = result.second, persistedText = result.second,
                    recoveryDraft = result.third.draft,
                    notice = if (result.third.readFailed)
                        "Document opened. A private recovery draft could not be read; it has not been deleted."
                    else null,
                )
            } catch (error: Exception) {
                mutable.update { it.copy(busy = false, notice = "Open failed: " + (error.message ?: "unknown error")) }
            }
        }
    }

    fun save(context: Context, destination: Uri) {
        val appContext = context.applicationContext
        val snapshot = mutable.value
        if (snapshot.busy) return
        mutable.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            try {
                val (name, outcome) = withContext(Dispatchers.IO) {
                    val storage = DocumentStorage(appContext)
                    val result = storage.save(
                        destination,
                        snapshot.text,
                        expectedPersistedText = snapshot.persistedText.takeIf {
                            destination == snapshot.uri
                        },
                    )
                    storage.displayName(destination) to result
                }
                mutable.update {
                    it.copy(
                        uri = destination,
                        fileName = name,
                        persistedText = snapshot.text,
                        busy = false,
                        notice = SaveNotice.message(
                            newerEditsRemain = it.text != snapshot.text,
                            recoveryCleanupPending = outcome.recoveryCleanupPending,
                        ),
                    )
                }
            } catch (error: Exception) {
                mutable.update {
                    it.copy(busy = false, notice = "Save unverified or failed. A private recovery draft may remain: " +
                        (error.message ?: "unknown error"))
                }
            }
        }
    }
}

@Composable
private fun MarkdownApp(vm: EditorViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    var showPreview by remember { mutableStateOf(false) }
    var showFind by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var nextFindOffset by remember { mutableIntStateOf(0) }
    var foundStart by remember { mutableStateOf<Int?>(null) }
    var findAttempted by remember { mutableStateOf(false) }
    var editorField by remember { mutableStateOf(TextFieldValue(state.text)) }
    val editorFocus = remember { FocusRequester() }
    var pendingDiscard by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Preserve IME composition and cursor selection during ordinary typing.
    // Update the field only when a new document or a recovered draft replaces
    // its text outside the text input callback.
    LaunchedEffect(state.text) {
        if (editorField.text != state.text) {
            editorField = editorField.copy(
                text = state.text,
                selection = TextRange(editorField.selection.end.coerceIn(0, state.text.length)),
                composition = null,
            )
        }
    }
    LaunchedEffect(foundStart, showPreview) {
        if (foundStart != null && !showPreview) editorFocus.requestFocus()
    }

    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
            } catch (_: SecurityException) {
                // Some providers offer session-only access. Save will surface write failures.
            }
            vm.open(context, uri)
        }
    }
    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri -> if (uri != null) vm.save(context, uri) }

    fun confirmDiscard(action: () -> Unit) {
        if (state.dirty) pendingDiscard = action else action()
    }

    BackHandler(enabled = state.dirty && !state.busy) {
        pendingDiscard = { (context as? Activity)?.finish() }
    }

    val palette = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFF9CC3FF), surface = Color(0xFF1C1F24))
    } else {
        lightColorScheme(primary = Color(0xFF245EB9), surface = Color(0xFFF8F9FC))
    }
    MaterialTheme(colorScheme = palette) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Markdown", style = MaterialTheme.typography.headlineMedium)
                Text(state.fileName + if (state.dirty) " • Unsaved changes" else "",
                    style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = { confirmDiscard { vm.reset(); showPreview = false } },
                        enabled = !state.busy) { Text("New") }
                    TextButton(onClick = { confirmDiscard { openDocument.launch(arrayOf("*/*")) } },
                        enabled = !state.busy) { Text("Open") }
                    TextButton(onClick = { createDocument.launch(state.fileName) },
                        enabled = !state.busy) { Text("Save as") }
                    Button(onClick = {
                        state.uri?.let { vm.save(context, it) }
                            ?: createDocument.launch(state.fileName)
                    }, enabled = !state.busy && (state.dirty || state.uri == null)) {
                        Text(if (state.busy) "Working…" else "Save")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !showPreview, onClick = { showPreview = false }, label = { Text("Edit") })
                    FilterChip(selected = showPreview, onClick = { showPreview = true }, label = { Text("Preview") })
                    TextButton(onClick = {
                        showFind = !showFind
                        foundStart = null
                        findAttempted = false
                    }) { Text(if (showFind) "Close find" else "Find") }
                }
                if (showFind) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = findQuery,
                            onValueChange = {
                                findQuery = it
                                nextFindOffset = 0
                                foundStart = null
                                findAttempted = false
                            },
                            label = { Text("Find in document") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            enabled = !state.busy && findQuery.isNotEmpty(),
                            onClick = {
                                val start = MarkdownSearch.findNext(
                                    state.text, findQuery, nextFindOffset
                                )
                                foundStart = start
                                findAttempted = true
                                if (start != null) {
                                    editorField = editorField.copy(
                                        selection = TextRange(start, start + findQuery.length)
                                    )
                                    nextFindOffset = start + findQuery.length
                                    showPreview = false
                                }
                            },
                        ) { Text("Next") }
                    }
                    if (findAttempted) {
                        Text(
                            foundStart?.let { index ->
                                "Match on line ${1 + state.text.take(index).count { it == '\n' }}"
                            } ?: "No matches",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                }
                state.notice?.let { notice ->
                    Text(notice, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    TextButton(onClick = vm::dismissNotice) { Text("Dismiss message") }
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (showPreview) {
                        val markwon = remember(context) { Markwon.create(context) }
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            AndroidView(
                                factory = { TextView(it).apply {
                                    setTextIsSelectable(true)
                                    textSize = 16f
                                    setPadding(8, 12, 8, 12)
                                } },
                                update = { view -> markwon.setMarkdown(view, state.text) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = editorField,
                            onValueChange = { next ->
                                editorField = next
                                if (next.text != state.text) {
                                    foundStart = null
                                    findAttempted = false
                                    nextFindOffset = 0
                                    vm.edit(next.text)
                                }
                            },
                            label = { Text("Markdown source") },
                            modifier = Modifier.fillMaxSize().focusRequester(editorFocus),
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                            enabled = !state.busy,
                        )
                    }
                }
            }
        }
        if (pendingDiscard != null) {
            AlertDialog(
                onDismissRequest = { pendingDiscard = null },
                title = { Text("Discard unsaved changes?") },
                text = { Text("Your current edits have not been saved to the selected file.") },
                confirmButton = {
                    TextButton(onClick = {
                        val action = pendingDiscard
                        pendingDiscard = null
                        action?.invoke()
                    }) { Text("Discard") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDiscard = null }) { Text("Keep editing") }
                },
            )
        }
        if (state.recoveryDraft != null) {
            AlertDialog(
                onDismissRequest = vm::dismissRecovery,
                title = { Text("Recovery draft available") },
                text = { Text("A private local draft from an earlier unverified save is available. Restore it into the editor?") },
                confirmButton = {
                    TextButton(onClick = vm::restoreDraft) { Text("Restore draft") }
                },
                dismissButton = {
                    TextButton(onClick = vm::dismissRecovery) { Text("Keep file version") }
                },
            )
        }
    }
}
