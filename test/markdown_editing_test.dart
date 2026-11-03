import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:goreecloud_markdown/editor/markdown_editing.dart';

void main() {
  test('wrapping a selected phrase preserves the rest of the document', () {
    const source = TextEditingValue(
      text: 'alpha beta gamma',
      selection: TextSelection(baseOffset: 6, extentOffset: 10),
    );
    final edited = MarkdownEditing.wrap(source, '**', '**');
    expect(edited.text, 'alpha **beta** gamma');
    expect(edited.selection, const TextSelection(baseOffset: 8, extentOffset: 12));
  });

  test('wrapping an empty selection places cursor between markers', () {
    const source = TextEditingValue(
      text: 'hello',
      selection: TextSelection.collapsed(offset: 5),
    );
    final edited = MarkdownEditing.wrap(source, '*', '*');
    expect(edited.text, 'hello**');
    expect(edited.selection.baseOffset, 6);
  });

  test('prefixes only the current line', () {
    const source = TextEditingValue(
      text: 'first\nsecond',
      selection: TextSelection.collapsed(offset: 9),
    );
    expect(MarkdownEditing.prefixLine(source, '## ').text, 'first\n## second');
  });

  test('word count and reading time handle blank input', () {
    expect(MarkdownEditing.wordCount('  \n  '), 0);
    expect(MarkdownEditing.wordCount('Hello  world\nagain'), 3);
    expect(MarkdownEditing.readingMinutes(''), 1);
  });

  test('find navigation wraps forward and backward, ignoring case', () {
    const text = 'One two one';
    expect(MarkdownEditing.findMatch(text, 'ONE'),
        const TextRange(start: 0, end: 3));
    expect(MarkdownEditing.findMatch(text, 'ONE', start: 3),
        const TextRange(start: 8, end: 11));
    expect(MarkdownEditing.findMatch(text, 'one', start: 11),
        const TextRange(start: 0, end: 3));
    expect(MarkdownEditing.findMatch(text, 'one', start: 8, backwards: true),
        const TextRange(start: 0, end: 3));
    expect(MarkdownEditing.findMatch(text, 'one', backwards: true),
        const TextRange(start: 8, end: 11));
  });

  test('find handles no result and invalid cursor positions', () {
    expect(MarkdownEditing.findMatch('abc', ''), isNull);
    expect(MarkdownEditing.findMatch('', 'a'), isNull);
    expect(MarkdownEditing.findMatch('abc', 'not here'), isNull);
    expect(MarkdownEditing.findMatch('abc', 'a', start: -15),
        const TextRange(start: 0, end: 1));
    expect(MarkdownEditing.findMatch('abc', 'a', start: 500),
        const TextRange(start: 0, end: 1));
  });

  test('find offsets match Dart UTF-16 selections for emoji', () {
    expect(MarkdownEditing.findMatch('a😀 b😀', '😀', start: 3),
        const TextRange(start: 5, end: 7));
  });
}
