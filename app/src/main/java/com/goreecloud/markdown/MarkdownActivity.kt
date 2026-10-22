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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
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
    /** Remains true after a warning is dismissed until recovery cleanup is verified. */
    val recoveryPending: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
) {
    val dirty get() = text != persistedText
}

internal class EditorViewModel : ViewModel() {
    private val mutable = MutableStateFlow(EditorState())
    private val editHistory = MarkdownUndoHistory()
    val state = mutable.asStateFlow()

    fun edit(text: String) {
        val current = mutable.value
        if (current.busy || current.text == text) return
        editHistory.record(current.text, text)
        mutable.update { it.copy(text = text, notice = null,
            canUndo = editHistory.canUndo, canRedo = editHistory.canRedo) }
    }
    fun reset() {
        editHistory.reset()
        mutable.value = EditorState()
    }
    fun undo(): String? {
        val current = mutable.value
        if (current.busy) return null
        val restored = editHistory.undo(current.text)
        mutable.update { it.copy(text = restored ?: it.text, notice = null,
            canUndo = editHistory.canUndo, canRedo = editHistory.canRedo) }
        return restored
    }
    fun redo(): String? {
        val current = mutable.value
        if (current.busy) return null
        val restored = editHistory.redo(current.text)
        mutable.update { it.copy(text = restored ?: it.text, notice = null,
            canUndo = editHistory.canUndo, canRedo = editHistory.canRedo) }
        return restored
    }
    fun dismissNotice() = mutable.update { it.copy(notice = null) }
    fun announce(message: String) = mutable.update { it.copy(notice = message) }
    fun restoreDraft() {
        val before = mutable.value
        if (before.busy) return
        val draft = before.recoveryDraft ?: return
        edit(draft)
        mutable.update { current ->
            current.copy(
                recoveryDraft = null,
                notice = if (current.dirty) {
                    "Private recovery draft restored. Save to keep these changes."
                } else {
                    "The recovery draft matches the saved document. No save is needed. " +
                        "The private recovery copy remains until verified cleanup."
                },
            )
        }
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
                editHistory.reset()
                mutable.value = EditorState(
                    uri = uri, fileName = result.first,
                    text = result.second, persistedText = result.second,
                    recoveryDraft = result.third.draft,
                    recoveryPending = result.third.readFailed || result.third.draft != null,
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
                        recoveryPending = outcome.recoveryCleanupPending,
                        notice = SaveNotice.message(
                            newerEditsRemain = it.text != snapshot.text,
                            recoveryCleanupPending = outcome.recoveryCleanupPending,
                        ),
                    )
                }
            } catch (error: Exception) {
                mutable.update {
                    it.copy(
                        busy = false,
                        recoveryPending = true,
                        notice = "Save unverified or failed. A private recovery draft may remain: " +
                            (error.message ?: "unknown error"),
                    )
                }
            }
        }
    }
}

private data class MarkdownReplaceAllRequest(
    val snapshot: String,
    val query: String,
    val replacement: String,
    val matches: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkdownApp(vm: EditorViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    // Retain only bounded UI state across activity recreation, never the entire document
    // text or a potentially large replacement value in an Android saved-state Bundle.
    var showPreview by rememberSaveable { mutableStateOf(false) }
    var showFind by rememberSaveable { mutableStateOf(false) }
    var showOutline by rememberSaveable { mutableStateOf(false) }
    var pendingHeadingFocus by remember { mutableStateOf(false) }
    var findQuery by rememberSaveable { mutableStateOf("") }
    var replaceWith by remember { mutableStateOf("") }
    var nextFindOffset by rememberSaveable { mutableIntStateOf(0) }
    var foundStart by rememberSaveable { mutableStateOf<Int?>(null) }
    var findAttempted by rememberSaveable { mutableStateOf(false) }
    var pendingReplaceAll by remember { mutableStateOf<MarkdownReplaceAllRequest?>(null) }
    var caretStart by rememberSaveable { mutableIntStateOf(0) }
    var caretEnd by rememberSaveable { mutableIntStateOf(0) }
    var editorField by remember {
        mutableStateOf(
            TextFieldValue(
                text = state.text,
                selection = TextRange(
                    caretStart.coerceIn(0, state.text.length),
                    caretEnd.coerceIn(0, state.text.length),
                ),
            )
        )
    }
    LaunchedEffect(editorField.selection) {
        caretStart = editorField.selection.start
        caretEnd = editorField.selection.end
    }
    val editorFocus = remember { FocusRequester() }
    val matchCount = remember(state.text, findQuery) {
        MarkdownSearch.countMatches(state.text, findQuery)
    }
    val documentStats = remember(state.text) { MarkdownDocumentStats.from(state.text) }
    val caretIndex = remember(state.text) { MarkdownCaretIndex(state.text) }
    val caretLocation = remember(caretIndex, editorField.selection.end) {
        caretIndex.at(editorField.selection.end)
    }
    var pendingDiscard by remember { mutableStateOf<(() -> Unit)?>(null) }
    // Let the IME consume Back first instead of prompting to discard while typing.
    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

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
    // A modal Find sheet owns focus; never pull IME focus into the editor beneath it.
    LaunchedEffect(foundStart, showPreview, showFind) {
        if (foundStart != null && !showPreview && !showFind) editorFocus.requestFocus()
    }
    LaunchedEffect(pendingHeadingFocus, showPreview) {
        if (pendingHeadingFocus && !showPreview) {
            editorFocus.requestFocus()
            pendingHeadingFocus = false
        }
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

    BackHandler(enabled = state.dirty && !state.busy && !isKeyboardVisible && !showFind && !showOutline && pendingReplaceAll == null) {
        pendingDiscard = { (context as? Activity)?.finish() }
    }

    MarkdownWorkspaceTheme { tones ->
        Surface(modifier = Modifier.fillMaxSize(), color = tones.canvas) {
            Column(
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                    .imePadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MarkdownWorkspaceHeader(
                    fileName = state.fileName,
                    dirty = state.dirty,
                    busy = state.busy,
                    hasFile = state.uri != null,
                    recoveryPending = state.recoveryPending,
                    notice = state.notice,
                    lines = documentStats.lines,
                    words = documentStats.words,
                    characters = documentStats.characters,
                    compact = isKeyboardVisible || showFind,
                    tones = tones,
                )
                // Hide secondary navigation while the keyboard occupies the screen;
                // keep the editor and both history controls reachable.
                if (!isKeyboardVisible) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
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
                    }, enabled = !state.busy && (state.dirty || state.uri == null),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tones.forestSoft,
                            contentColor = tones.forestInk,
                        ),
                    ) {
                        Text(if (state.busy) "Working…" else "Save")
                    }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FilterChip(
                        selected = !showPreview,
                        onClick = { showPreview = false },
                        label = { Text("Edit") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tones.forestSoft,
                            selectedLabelColor = tones.forestInk,
                        ),
                    )
                    FilterChip(
                        selected = showPreview,
                        onClick = { showPreview = true },
                        label = { Text("Preview") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tones.copperSoft,
                            selectedLabelColor = tones.copperInk,
                        ),
                    )
                    TextButton(onClick = {
                        showFind = !showFind
                        foundStart = null
                        findAttempted = false
                    }) { Text(if (showFind) "Close find" else "Find") }
                    TextButton(onClick = { showOutline = true }, enabled = !state.busy) {
                        Text("Outline")
                    }
                }
                }
                // Keep history actions visible at all times. The navigation row
                // above can scroll horizontally on compact devices.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = {
                        val previous = vm.undo()
                        if (previous != null) {
                            editorField = editorField.copy(
                                text = previous,
                                selection = TextRange(editorField.selection.start.coerceIn(0, previous.length)),
                                composition = null,
                            )
                            foundStart = null
                            findAttempted = false
                            nextFindOffset = 0
                            showPreview = false
                            pendingHeadingFocus = true
                        }
                    }, enabled = !state.busy && state.canUndo,
                        modifier = Modifier.weight(1f)) { Text("Undo") }
                    OutlinedButton(onClick = {
                        val next = vm.redo()
                        if (next != null) {
                            editorField = editorField.copy(
                                text = next,
                                selection = TextRange(editorField.selection.start.coerceIn(0, next.length)),
                                composition = null,
                            )
                            foundStart = null
                            findAttempted = false
                            nextFindOffset = 0
                            showPreview = false
                            pendingHeadingFocus = true
                        }
                    }, enabled = !state.busy && state.canRedo,
                        modifier = Modifier.weight(1f)) { Text("Redo") }
                }
                if (showFind) {
                    ModalBottomSheet(
                        onDismissRequest = { showFind = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        containerColor = tones.panel,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .heightIn(max = 520.dp)
                                .verticalScroll(rememberScrollState())
                                .imePadding()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                Text("Find & replace", style = MaterialTheme.typography.titleLarge)
                                TextButton(onClick = { showFind = false }) { Text("Close") }
                            }
                    OutlinedTextField(
                        value = findQuery,
                        onValueChange = {
                            findQuery = it.take(256)
                            nextFindOffset = 0
                            foundStart = null
                            findAttempted = false
                        },
                        label = { Text("Find in document") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val activeMatch = remember(state.text, findQuery, foundStart) {
                        MarkdownSearch.matchOrdinal(state.text, findQuery, foundStart)
                    }
                    Text(
                        when {
                            findQuery.isEmpty() -> "Enter a term to search"
                            matchCount == 0 -> "No matches"
                            activeMatch != null -> "$activeMatch of $matchCount matches"
                            else -> "$matchCount matches"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = tones.copper,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            enabled = !state.busy && findQuery.isNotEmpty(),
                            onClick = {
                                val index = MarkdownSearch.findPrevious(
                                    state.text, findQuery, foundStart ?: editorField.selection.start,
                                )
                                foundStart = index
                                findAttempted = true
                                if (index != null) {
                                    editorField = editorField.copy(
                                        selection = TextRange(index, index + findQuery.length)
                                    )
                                    nextFindOffset = index + findQuery.length
                                    showPreview = false
                                }
                            },
                        ) { Text("Previous") }
                        TextButton(
                            enabled = !state.busy && findQuery.isNotEmpty(),
                            onClick = {
                                val index = MarkdownSearch.findNext(
                                    state.text, findQuery, nextFindOffset
                                )
                                foundStart = index
                                findAttempted = true
                                if (index != null) {
                                    editorField = editorField.copy(
                                        selection = TextRange(index, index + findQuery.length)
                                    )
                                    nextFindOffset = index + findQuery.length
                                    showPreview = false
                                }
                            },
                        ) { Text("Next") }
                    }
                    OutlinedTextField(
                        value = replaceWith,
                        onValueChange = { replaceWith = it },
                        label = { Text("Replace selected match with") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        enabled = !state.busy && findQuery.isNotEmpty() &&
                            foundStart != null && editorField.text == state.text &&
                            editorField.selection.start == foundStart &&
                            editorField.selection.end == (foundStart ?: 0) + findQuery.length,
                        onClick = {
                            val updated = MarkdownReplace.replaceSelected(
                                state.text,
                                findQuery,
                                replaceWith,
                                editorField.selection.start,
                                editorField.selection.end,
                            )
                            when {
                                updated == null ->
                                    vm.announce("Replacement was not applied; reselect a valid match or shorten the replacement.")
                                updated.text == state.text ->
                                    vm.announce("Selected match already uses that text. No changes were made.")
                                else -> {
                                    editorField = editorField.copy(
                                        text = updated.text,
                                        selection = TextRange(updated.caret),
                                        composition = null,
                                    )
                                    foundStart = null
                                    findAttempted = false
                                    nextFindOffset = updated.caret
                                    showPreview = false
                                    vm.edit(updated.text)
                                    vm.announce("Selected match replaced. Save to keep this change.")
                                }
                            }
                        },
                    ) { Text("Replace selected") }
                    TextButton(
                        enabled = !state.busy && findQuery.isNotEmpty() && matchCount > 0,
                        onClick = {
                            pendingReplaceAll = MarkdownReplaceAllRequest(
                                snapshot = state.text,
                                query = findQuery,
                                replacement = replaceWith,
                                matches = matchCount,
                            )
                        },
                    ) { Text("Replace all…") }
                    Text(
                        "Review matches before replacing all",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                        }
                    }
                }
                state.notice?.let { notice ->
                    val attention = notice.startsWith("Save unverified") ||
                        notice.startsWith("Open failed") ||
                        notice.contains("could not", ignoreCase = true) ||
                        notice.contains("cleanup is pending", ignoreCase = true) ||
                        notice.contains("newer changes remain", ignoreCase = true)
                    val verified = notice == "Save verified." && !state.dirty &&
                        !state.recoveryPending
                    val background = when {
                        attention -> tones.ochreSoft
                        verified -> tones.forestSoft
                        else -> tones.copperSoft
                    }
                    val foreground = when {
                        attention -> tones.ochreInk
                        verified -> tones.forestInk
                        else -> tones.copperInk
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = background,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                notice,
                                style = MaterialTheme.typography.bodySmall,
                                color = foreground,
                                modifier = Modifier.weight(1f).heightIn(max = 96.dp)
                                    .verticalScroll(rememberScrollState())
                                    .semantics { liveRegion = LiveRegionMode.Polite },
                            )
                            TextButton(onClick = vm::dismissNotice) {
                                Text("Dismiss", color = foreground)
                            }
                        }
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    color = tones.editor,
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 1.dp,
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                    if (showPreview) {
                        val markwon = remember(context) { Markwon.create(context) }
                        // Preview is an Android TextView under a Compose theme; explicitly
                        // adopt the active foreground color for readable dark-mode text.
                        val previewTextColor = MaterialTheme.colorScheme.onSurface.toArgb()
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            AndroidView(
                                factory = { TextView(it).apply {
                                    setTextIsSelectable(true)
                                    textSize = 16f
                                    setPadding(8, 12, 8, 12)
                                } },
                                update = { view ->
                                    view.setTextColor(previewTextColor)
                                    markwon.setMarkdown(view, state.text)
                                },
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
                // Source-derived footer: never use the presentation mockup's hard-coded caret.
                if (!showPreview && !isKeyboardVisible) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "${documentStats.lines} lines · ${documentStats.words} words · ${documentStats.characters} chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "Ln ${caretLocation.line}, Col ${caretLocation.column}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        if (showOutline) {
            val headings = remember(state.text) { MarkdownOutline.headings(state.text) }
            AlertDialog(
                onDismissRequest = { showOutline = false },
                title = { Text("Document outline") },
                text = {
                    if (headings.isEmpty()) {
                        Text("No Markdown headings found.")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            items(headings, key = { it.start }) { heading ->
                                TextButton(
                                    onClick = {
                                        editorField = editorField.copy(
                                            selection = TextRange(heading.start, heading.endExclusive)
                                        )
                                        showPreview = false
                                        showOutline = false
                                        pendingHeadingFocus = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Level ${heading.level}: ${heading.title}")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showOutline = false }) { Text("Close") }
                },
            )
        }
        pendingReplaceAll?.let { request ->
            AlertDialog(
                onDismissRequest = { pendingReplaceAll = null },
                title = { Text("Replace all matches?") },
                text = {
                    Text(
                        "Replace " + request.matches + " non-overlapping matches in the editor? " +
                            "Nothing is written to the document provider until Save. " +
                            "Undo is session-only and subject to its size limit."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        pendingReplaceAll = null
                        val updated = if (!state.busy && state.text == request.snapshot) {
                            MarkdownReplaceAll.replaceAll(
                                state.text, request.query, request.replacement, request.matches
                            )
                        } else null
                        when {
                            updated == null ->
                                vm.announce("Replace all was not applied: source changed, invalid text, or size limit exceeded.")
                            updated.text == state.text ->
                                vm.announce("Matches already use that replacement. No changes were made.")
                            else -> {
                                editorField = editorField.copy(
                                    text = updated.text,
                                    selection = TextRange(updated.caret),
                                    composition = null,
                                )
                                foundStart = null
                                findAttempted = false
                                nextFindOffset = updated.caret
                                showPreview = false
                                vm.edit(updated.text)
                                pendingHeadingFocus = true
                                vm.announce(updated.replacements.toString() +
                                    " matches replaced in the editor. Save to keep these changes.")
                            }
                        }
                    }) { Text("Replace all") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingReplaceAll = null }) { Text("Cancel") }
                },
            )
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
                    TextButton(onClick = {
                        confirmDiscard { vm.restoreDraft(); showPreview = false }
                    }) { Text("Restore draft") }
                },
                dismissButton = {
                    TextButton(onClick = vm::dismissRecovery) { Text("Keep file version") }
                },
            )
        }
    }
}
