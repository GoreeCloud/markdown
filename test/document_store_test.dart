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
