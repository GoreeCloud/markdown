import 'dart:convert';
import 'dart:io';

import 'package:file_selector/file_selector.dart';
import 'package:flutter/services.dart';

const maxDocumentBytes = 4 * 1024 * 1024;

const _editableTextSuffixes = ['.md', '.markdown', '.mdown', '.txt', '.text'];

/// Suggest a safe basename without silently doubling an editable extension.
/// The provider or Linux Save dialog still chooses the final file location.
String suggestedDocumentName(String original) {
  if (RegExp(r'[\x00-\x1f\x7f]').hasMatch(original)) {
    throw const FormatException('Document name contains invalid controls.');
  }
  final basename = original.split(RegExp(r'[/\\]')).last.trim();
  if (basename.isEmpty || basename == '.' || basename == '..') {
    return 'Untitled.md';
  }
  final folded = basename.toLowerCase();
  return _editableTextSuffixes.any(folded.endsWith)
      ? basename
      : '$basename.md';
}

class OpenDocument {
  const OpenDocument({
    required this.name,
    required this.text,
    required this.identity,
    required this.origin,
    required this.writable,
  });

  final String name;
  final String text;
  /// A local filesystem path on Linux or provider content URI on Android.
  final String identity;
  final String origin;
  final bool writable;

  OpenDocument withText(String value) => OpenDocument(
        name: name,
        text: value,
        identity: identity,
        origin: origin,
        writable: writable,
      );
}

typedef ExternalOpenHandler = Future<void> Function(OpenDocument document);

abstract class DocumentStore {
  factory DocumentStore.current() {
    if (Platform.isAndroid) return AndroidDocumentStore();
    if (Platform.isLinux) return LinuxDocumentStore();
    throw UnsupportedError('Only Android and Linux are supported.');
  }

  Future<OpenDocument?> open();
  Future<OpenDocument?> initialDocument();
  Future<OpenDocument?> saveAs(String name, String content);
  Future<void> write(OpenDocument document, String content);
  void setExternalOpenHandler(ExternalOpenHandler handler);
}

void verifyTextSize(String text) {
  if (utf8.encode(text).length > maxDocumentBytes) {
    throw const FormatException('Document exceeds the 4 MiB editing limit.');
  }
}

class LinuxDocumentStore implements DocumentStore {
  static const type = XTypeGroup(
    label: 'Markdown or plain text',
    extensions: ['md', 'markdown', 'mdown', 'txt', 'text'],
  );

  @override
  Future<OpenDocument?> open() async {
    final picked = await openFile(acceptedTypeGroups: [type]);
    if (picked == null) return null;
    final file = File(picked.path);
    if (await file.length() > maxDocumentBytes) {
      throw const FormatException('Document exceeds the 4 MiB editing limit.');
    }
    final bytes = await file.readAsBytes();
    return OpenDocument(
      name: picked.name,
      text: utf8.decode(bytes, allowMalformed: false),
      identity: picked.path,
      origin: 'Local / mounted storage',
      writable: true,
    );
  }

  @override
  Future<OpenDocument?> initialDocument() async => null;

  @override
  Future<OpenDocument?> saveAs(String name, String content) async {
    verifyTextSize(content);
    final location = await getSaveLocation(
      suggestedName: suggestedDocumentName(name),
      acceptedTypeGroups: [type],
    );
    if (location == null) return null;
    await File(location.path).writeAsBytes(utf8.encode(content), flush: true);
    return OpenDocument(
      name: location.path.split(Platform.pathSeparator).last,
      text: content,
      identity: location.path,
      origin: 'Local / mounted storage',
      writable: true,
    );
  }

  @override
  Future<void> write(OpenDocument document, String content) async {
    verifyTextSize(content);
    await File(document.identity).writeAsBytes(utf8.encode(content), flush: true);
  }

  @override
  void setExternalOpenHandler(ExternalOpenHandler handler) {}
}

class AndroidDocumentStore implements DocumentStore {
  AndroidDocumentStore() {
    _channel.setMethodCallHandler((call) async {
      if (call.method == 'externalDocument') {
        final document = _decode(call.arguments);
        final handler = _external;
        if (handler != null) await handler(document);
      }
    });
  }

  static const _channel = MethodChannel('goreecloud/markdown/documents');
  ExternalOpenHandler? _external;

  OpenDocument _decode(dynamic raw) {
    final map = Map<String, dynamic>.from(raw as Map);
    return OpenDocument(
      name: map['name'] as String,
      text: map['text'] as String,
      identity: map['uri'] as String,
      origin: 'Android document provider',
      writable: map['writable'] as bool? ?? false,
    );
  }

  @override
  Future<OpenDocument?> open() async {
    final data = await _channel.invokeMethod<dynamic>('open');
    return data == null ? null : _decode(data);
  }

  @override
  Future<OpenDocument?> initialDocument() async {
    final data = await _channel.invokeMethod<dynamic>('initialDocument');
    return data == null ? null : _decode(data);
  }

  @override
  Future<OpenDocument?> saveAs(String name, String content) async {
    verifyTextSize(content);
    final data = await _channel.invokeMethod<dynamic>(
      'saveAs',
      {'name': suggestedDocumentName(name), 'text': content},
    );
    return data == null ? null : _decode(data);
  }

  @override
  Future<void> write(OpenDocument document, String content) async {
    verifyTextSize(content);
    await _channel.invokeMethod<void>('write', {
      'uri': document.identity,
      'text': content,
    });
  }

  @override
  void setExternalOpenHandler(ExternalOpenHandler handler) {
    _external = handler;
  }
}
