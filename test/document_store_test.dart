import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:goreecloud_markdown/services/document_store.dart';

void main() {
  test('size limit permits small documents', () {
    expect(() => verifyTextSize('# Hello'), returnsNormally);
  });

  test('size limit measures bytes rather than UTF-16 characters', () {
    final tooLarge = '💜' * (maxDocumentBytes ~/ 4 + 1);
    expect(() => verifyTextSize(tooLarge), throwsFormatException);
  });

  test('bounded Linux read preserves exact UTF-8 bytes and empty files', () async {
    final folder = await Directory.systemTemp.createTemp('gc-md-bounded-open-');
    try {
      final file = File('${folder.path}/note.md');
      final source = '# GoreeCloud 📋\nLine two';
      final bytes = Uint8List.fromList(utf8.encode(source));
      await file.writeAsBytes(bytes, flush: true);
      expect(await readBoundedDocumentBytes(file), bytes);
      await file.writeAsBytes([], flush: true);
      expect(await readBoundedDocumentBytes(file), isEmpty);
    } finally {
      await folder.delete(recursive: true);
    }
  });

  test('bounded Linux read rejects oversized files without loading them', () async {
    final folder = await Directory.systemTemp.createTemp('gc-md-over-limit-');
    try {
      final file = File('${folder.path}/too-large.md');
      await file.writeAsBytes(Uint8List(maxDocumentBytes + 1), flush: true);
      await expectLater(readBoundedDocumentBytes(file), throwsFormatException);
    } finally {
      await folder.delete(recursive: true);
    }
  });

  test('document immutable update preserves its provider identity', () {
    const original = OpenDocument(
      name: 'notes.md',
      text: 'a',
      identity: 'content://provider/document/1',
      origin: 'Android document provider',
      writable: false,
    );
    final modified = original.withText('b');
    expect(modified.text, 'b');
    expect(modified.identity, original.identity);
    expect(modified.writable, isFalse);
  });
}
