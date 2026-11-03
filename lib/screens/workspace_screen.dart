import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../editor/markdown_editing.dart';
import '../services/document_store.dart';
import '../theme/gc_theme.dart';
import '../widgets/editor_pane.dart';
import '../widgets/preview_pane.dart';

const welcomeMarkdown = '''
# Give your ideas room to breathe.

Welcome to **GoreeCloud Markdown** — a calm, focused home for your words.

> A little more clarity. A lot more possibility.

## Everything you need, nothing in the way.

- [x] Write in clean, familiar Markdown
- [x] Watch your words come to life
- [x] Keep your documents where *you* choose
- [ ] Make something wonderful today

### Your words stay yours.

Open a document with **Open**, or create a fresh canvas with **New**.
On Android, choose a file from a system document provider.
On Linux, browse local or mounted folders.

**Ctrl+S** saves · **Ctrl+O** opens · **Ctrl+N** starts fresh.
''';

enum _Decision { save, discard, cancel }

class WorkspaceScreen extends StatefulWidget {
  const WorkspaceScreen({super.key, required this.onToggleTheme});
  final VoidCallback onToggleTheme;

  @override
  State<WorkspaceScreen> createState() => _WorkspaceScreenState();
}

class _WorkspaceScreenState extends State<WorkspaceScreen> {
  final _controller = TextEditingController(text: welcomeMarkdown);
  final _editorFocus = FocusNode();
  late final DocumentStore _store = DocumentStore.current();
  OpenDocument? _document;
  String _saved = welcomeMarkdown;
  String _preview = welcomeMarkdown;
  bool _demo = true;
  bool _previewMode = false;
  bool _saving = false;
  Timer? _debounce;

  bool get _dirty => _controller.text != _saved;
  String get _name => _document?.name ?? (_demo ? 'Welcome.md' : 'Untitled.md');

  @override
  void initState() {
    super.initState();
    _controller.addListener(_onChange);
    _store.setExternalOpenHandler(_receive);
    WidgetsBinding.instance.addPostFrameCallback((_) => _initial());
  }

  Future<void> _initial() async {
    try {
      final doc = await _store.initialDocument();
      if (doc != null && mounted) await _receive(doc);
    } catch (e) {
      _notice('Could not open shared file: $e');
    }
  }

  void _onChange() {
    if (!mounted) return;
    setState(() {});
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 150), () {
      if (mounted) setState(() => _preview = _controller.text);
    });
  }

  void _replace(OpenDocument? doc, String text) {
    _debounce?.cancel();
    setState(() {
      _document = doc;
      _saved = text;
      _preview = text;
      _demo = false;
      _previewMode = false;
      _controller.value = TextEditingValue(
        text: text, selection: TextSelection.collapsed(offset: text.length),
      );
    });
  }

  Future<bool> _guard() async {
    if (!_dirty) return true;
    final choice = await showDialog<_Decision>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Unsaved changes'),
        content: const Text('Save your edits before replacing this document?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, _Decision.cancel),
            child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(ctx, _Decision.discard),
            child: const Text('Discard')),
          FilledButton(onPressed: () => Navigator.pop(ctx, _Decision.save),
            child: const Text('Save')),
        ],
      ),
    );
    if (choice == _Decision.save) return _save();
    return choice == _Decision.discard;
  }

  Future<void> _receive(OpenDocument doc) async {
    if (!mounted || !await _guard() || !mounted) return;
    _replace(doc, doc.text);
    _notice('Opened ${doc.name}');
  }

  Future<void> _new() async {
    if (!await _guard() || !mounted) return;
    _replace(null, '');
    _editorFocus.requestFocus();
  }

  Future<void> _open() async {
    if (!await _guard() || !mounted) return;
    try {
      final doc = await _store.open();
      if (doc != null && mounted) {
        _replace(doc, doc.text);
        _notice('Opened ${doc.name}');
      }
    } catch (e) {
      _notice('Open failed: $e');
    }
  }

  Future<bool> _save({bool saveAs = false}) async {
    if (_saving) return false;
    final text = _controller.text;
    setState(() => _saving = true);
    try {
      if (saveAs || _document == null || !_document!.writable) {
        final doc = await _store.saveAs(_name, text);
        if (doc == null || !mounted) return false;
        setState(() {
          _document = doc;
          _demo = false;
          _saved = text;
        });
      } else {
        await _store.write(_document!, text);
        if (!mounted) return false;
        setState(() => _saved = text);
      }
      _notice('Saved $_name');
      return true;
    } catch (e) {
      _notice('Save failed: $e');
      return false;
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  void _format(TextEditingValue Function(TextEditingValue) fn) {
    _controller.value = fn(_controller.value);
    _editorFocus.requestFocus();
  }

  void _notice(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _controller.removeListener(_onChange);
    _controller.dispose();
    _editorFocus.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final c = GcColors.of(context);
    return CallbackShortcuts(
      bindings: {
        const SingleActivator(LogicalKeyboardKey.keyS, control: true): () => _save(),
        const SingleActivator(LogicalKeyboardKey.keyS, control: true, shift: true):
            () => _save(saveAs: true),
        const SingleActivator(LogicalKeyboardKey.keyO, control: true): _open,
        const SingleActivator(LogicalKeyboardKey.keyN, control: true): _new,
        const SingleActivator(LogicalKeyboardKey.keyB, control: true):
            () => _format((v) => MarkdownEditing.wrap(v, '**', '**')),
        const SingleActivator(LogicalKeyboardKey.keyI, control: true):
            () => _format((v) => MarkdownEditing.wrap(v, '*', '*')),
      },
      child: Focus(
        autofocus: true,
        child: Scaffold(
          body: SafeArea(
            child: LayoutBuilder(builder: (context, size) {
              final rail = size.maxWidth >= 1000;
              final split = size.maxWidth >= 1180;
              final compact = size.maxWidth < 650;
              return Padding(
                padding: EdgeInsets.all(compact ? 12 : 20),
                child: Row(
                  children: [
                    if (rail) ...[
                      SizedBox(width: 228, child: _sidebar(c)),
                      const SizedBox(width: 18),
                    ],
                    Expanded(child: Column(children: [
                      _topbar(c, compact),
                      const SizedBox(height: 14),
                      _toolbar(c, split),
                      const SizedBox(height: 14),
                      Expanded(
                        child: split
                            ? Row(children: [
                                Expanded(child: EditorPane(
                                  controller: _controller, focusNode: _editorFocus,
                                )),
                                const SizedBox(width: 16),
                                Expanded(child: _previewPane()),
                              ])
                            : AnimatedSwitcher(
                                duration: const Duration(milliseconds: 220),
                                child: _previewMode
                                    ? SizedBox.expand(
                                        key: const ValueKey('preview'),
                                        child: _previewPane(),
                                      )
                                    : EditorPane(
                                        key: const ValueKey('editor'),
                                        controller: _controller,
                                        focusNode: _editorFocus,
                                      ),
                              ),
                      ),
                      const SizedBox(height: 12),
                      _status(c),
                    ])),
                  ],
                ),
              );
            }),
          ),
        ),
      ),
    );
  }

  Widget _previewPane() => PreviewPane(
        markdown: _preview,
        onExternalLink: (_) => _notice('Links are disabled in safe preview.'),
      );

  Widget _topbar(GcColors c, bool compact) => Container(
        padding: EdgeInsets.symmetric(horizontal: compact ? 8 : 16, vertical: 9),
        decoration: BoxDecoration(
          color: c.lens,
          border: Border.all(color: c.outline),
          borderRadius: BorderRadius.circular(24),
          boxShadow: [
            BoxShadow(color: Colors.black.withValues(alpha: 0.055),
              blurRadius: 24, offset: const Offset(0, 8)),
          ],
        ),
        child: Row(children: [
          if (compact) ...[_mark(38), const SizedBox(width: 9)],
          Expanded(child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(_name, maxLines: 1, overflow: TextOverflow.ellipsis,
                style: TextStyle(color: c.text, fontSize: compact ? 14 : 18,
                  fontWeight: FontWeight.w800)),
              const SizedBox(height: 4),
              Row(children: [
                Container(width: 7, height: 7, decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: _dirty ? const Color(0xFFDE9859) : const Color(0xFF54AC91))),
                const SizedBox(width: 6),
                Flexible(child: Text(
                  _dirty ? 'Unsaved changes' : _document?.origin ?? 'Private workspace',
                  maxLines: 1, overflow: TextOverflow.ellipsis,
                  style: TextStyle(color: c.muted, fontSize: 11),
                )),
              ]),
            ],
          )),
          IconButton(tooltip: 'New (Ctrl+N)', onPressed: _new,
            icon: const Icon(Icons.add_rounded)),
          IconButton(tooltip: 'Open (Ctrl+O)', onPressed: _open,
            icon: const Icon(Icons.folder_open_outlined)),
          if (!compact)
            IconButton(tooltip: 'Save as (Ctrl+Shift+S)',
              onPressed: _saving ? null : () => _save(saveAs: true),
              icon: const Icon(Icons.drive_file_rename_outline_rounded)),
          if (compact)
            IconButton(tooltip: 'Save (Ctrl+S)',
              onPressed: _saving ? null : () => _save(),
              icon: const Icon(Icons.save_outlined))
          else
            FilledButton.icon(
              onPressed: _saving ? null : () => _save(),
              icon: const Icon(Icons.save_outlined, size: 18),
              label: const Text('Save'),
              style: FilledButton.styleFrom(
                backgroundColor: c.brand, foregroundColor: c.onBrand,
                minimumSize: const Size(92, 44)),
            ),
          IconButton(tooltip: 'Toggle light and dark theme',
            onPressed: widget.onToggleTheme,
            icon: const Icon(Icons.brightness_6_outlined)),
        ]),
      );

  Widget _toolbar(GcColors c, bool split) => Container(
        height: 60,
        padding: const EdgeInsets.symmetric(horizontal: 8),
        decoration: BoxDecoration(
          color: c.surface, border: Border.all(color: c.outline),
          borderRadius: BorderRadius.circular(20),
        ),
        child: Row(children: [
          Expanded(child: ListView(scrollDirection: Axis.horizontal, children: [
            _tool(Icons.format_bold_rounded, 'Bold',
              () => _format((v) => MarkdownEditing.wrap(v, '**', '**'))),
            _tool(Icons.format_italic_rounded, 'Italic',
              () => _format((v) => MarkdownEditing.wrap(v, '*', '*'))),
            _tool(Icons.title_rounded, 'Heading',
              () => _format((v) => MarkdownEditing.prefixLine(v, '## '))),
            _tool(Icons.format_list_bulleted_rounded, 'List',
              () => _format((v) => MarkdownEditing.prefixLine(v, '- '))),
            _tool(Icons.check_box_outlined, 'Task',
              () => _format((v) => MarkdownEditing.prefixLine(v, '- [ ] '))),
            _tool(Icons.format_quote_rounded, 'Quote',
              () => _format((v) => MarkdownEditing.prefixLine(v, '> '))),
            _tool(Icons.code_rounded, 'Code',
              () => _format((v) => MarkdownEditing.wrap(v, String.fromCharCode(96),
                String.fromCharCode(96)))),
            _tool(Icons.link_rounded, 'Link',
              () => _format((v) => MarkdownEditing.wrap(v, '[',
                '](https://example.com)'))),
          ])),
          if (!split) ...[
            Container(height: 28, width: 1, color: c.outline),
            const SizedBox(width: 6),
            _view(c, Icons.edit_note_rounded, !_previewMode, 'Edit', () {
              setState(() => _previewMode = false);
            }),
            _view(c, Icons.visibility_outlined, _previewMode, 'Preview', () {
              _debounce?.cancel();
              setState(() {
                _preview = _controller.text;
                _previewMode = true;
              });
            }),
          ],
        ]),
      );

  Widget _tool(IconData icon, String text, VoidCallback callback) => Padding(
        padding: const EdgeInsets.symmetric(horizontal: 2, vertical: 7),
        child: IconButton(tooltip: text, icon: Icon(icon, size: 19),
          onPressed: callback),
      );

  Widget _view(GcColors c, IconData icon, bool active, String tooltip,
      VoidCallback callback) => IconButton(
        tooltip: tooltip,
        onPressed: callback,
        style: IconButton.styleFrom(
          backgroundColor: active ? c.brandContainer : Colors.transparent,
          foregroundColor: active ? c.brand : c.muted,
        ),
        icon: Icon(icon, size: 19),
      );

  Widget _status(GcColors c) => Padding(
        padding: const EdgeInsets.symmetric(horizontal: 10),
        child: Row(children: [
          Icon(Icons.lock_outline_rounded, size: 14, color: c.muted),
          const SizedBox(width: 6),
          Expanded(child: Text('Local-first · No account needed',
            overflow: TextOverflow.ellipsis,
            style: TextStyle(fontSize: 11, color: c.muted))),
          Text('${MarkdownEditing.wordCount(_controller.text)} words  ·  ${_controller.text.split('\n').length} lines',
            style: TextStyle(color: c.muted, fontSize: 11)),
        ]),
      );

  Widget _mark(double size) => Container(
        height: size, width: size,
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            colors: [Color(0xFF8075F0), Color(0xFF5145D9)],
            begin: Alignment.topLeft, end: Alignment.bottomRight),
          borderRadius: BorderRadius.circular(size * 0.32),
        ),
        child: Icon(Icons.auto_awesome_rounded, color: Colors.white,
          size: size * 0.5),
      );

  Widget _sidebar(GcColors c) => Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: c.lens, border: Border.all(color: c.outline),
          borderRadius: BorderRadius.circular(26)),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          _mark(52),
          const SizedBox(height: 18),
          Text('GoreeCloud', style: TextStyle(
            fontSize: 21, fontWeight: FontWeight.w900, color: c.text,
            letterSpacing: -0.6)),
          Text('MARKDOWN STUDIO', style: TextStyle(
            fontSize: 10, fontWeight: FontWeight.w800,
            letterSpacing: 1.9, color: c.brand)),
          const SizedBox(height: 34),
          Text('YOUR WORKSPACE', style: TextStyle(
            color: c.muted, fontSize: 10, letterSpacing: 1.5,
            fontWeight: FontWeight.w800)),
          const SizedBox(height: 12),
          _side(Icons.note_add_outlined, 'Fresh document', _new),
          _side(Icons.folder_open_outlined, 'Open a file', _open),
          _side(Icons.save_outlined, 'Save your work', () => _save()),
          const SizedBox(height: 24),
          Divider(color: c.outline),
          const SizedBox(height: 20),
          Text('A SPACE TO CREATE', style: TextStyle(
            color: c.muted, fontSize: 10, letterSpacing: 1.5,
            fontWeight: FontWeight.w800)),
          const SizedBox(height: 14),
          Text('Thoughtful writing, beautiful reading.',
            style: TextStyle(color: c.text, fontSize: 17,
              fontWeight: FontWeight.w700, height: 1.4)),
          const SizedBox(height: 9),
          Text('Every word belongs to you. Open from your device or a connected file provider.',
            style: TextStyle(color: c.muted, height: 1.6, fontSize: 12)),
          const Spacer(),
          Container(
            padding: const EdgeInsets.all(15),
            decoration: BoxDecoration(color: c.brandContainer,
              borderRadius: BorderRadius.circular(18)),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Icon(Icons.keyboard_command_key_rounded, color: c.brand),
              const SizedBox(height: 8),
              Text('A little shortcut', style: TextStyle(
                color: c.text, fontWeight: FontWeight.w800, fontSize: 13)),
              const SizedBox(height: 5),
              Text('Ctrl+S saves your work.',
                style: TextStyle(color: c.muted, fontSize: 11)),
            ]),
          ),
        ]),
      );

  Widget _side(IconData icon, String text, VoidCallback callback) =>
      Padding(
        padding: const EdgeInsets.only(bottom: 6),
        child: TextButton.icon(
          onPressed: callback,
          icon: Icon(icon, size: 19), label: Text(text),
          style: TextButton.styleFrom(
            alignment: Alignment.centerLeft,
            minimumSize: const Size.fromHeight(46),
            padding: const EdgeInsets.symmetric(horizontal: 12),
          ),
        ),
      );
}
