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

  test('Save As retains supported Markdown and text suffixes', () {
    for (final extension in ['md', 'markdown', 'mdown', 'txt', 'text']) {
      expect(suggestedDocumentName('Draft.' + extension), 'Draft.' + extension);
      expect(suggestedDocumentName('Draft.' + extension.toUpperCase()),
          'Draft.' + extension.toUpperCase());
    }
    expect(suggestedDocumentName('notes'), 'notes.md');
    expect(suggestedDocumentName('notes.csv'), 'notes.csv.md');
    expect(suggestedDocumentName('Résumé.MD'), 'Résumé.MD');
  });

  test('Save As strips path components and handles empty names safely', () {
    expect(suggestedDocumentName('folder/notes.txt'), 'notes.txt');
    expect(suggestedDocumentName(r'folder\notes.markdown'), 'notes.markdown');
    expect(suggestedDocumentName('  '), 'Untitled.md');
    expect(suggestedDocumentName('.'), 'Untitled.md');
    expect(suggestedDocumentName('..'), 'Untitled.md');
    expect(suggestedDocumentName('folder/'), 'Untitled.md');
  });

  test('Save As rejects invisible filename control characters', () {
    for (final name in ['bad\u0000.md', 'bad\nname.md', 'bad\tname.md']) {
      expect(() => suggestedDocumentName(name), throwsFormatException);
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
