import 'dart:math';

import 'package:flutter/services.dart';

/// Pure selection-aware Markdown transformations. Never rewrite unrelated text.
class MarkdownEditing {
  static TextEditingValue wrap(
    TextEditingValue value,
    String before,
    String after,
  ) {
    final range = _range(value);
    final selected = value.text.substring(range.$1, range.$2);
    final updated = value.text.replaceRange(
      range.$1,
      range.$2,
      '$before$selected$after',
    );
    final start = range.$1 + before.length;
    return TextEditingValue(
      text: updated,
      selection: selected.isEmpty
          ? TextSelection.collapsed(offset: start)
          : TextSelection(baseOffset: start, extentOffset: start + selected.length),
    );
  }

  static TextEditingValue prefixLine(TextEditingValue value, String prefix) {
    final range = _range(value);
    final lineStart = value.text.lastIndexOf('\n', max(0, range.$1 - 1)) + 1;
    final updated = value.text.replaceRange(lineStart, lineStart, prefix);
    return TextEditingValue(
      text: updated,
      selection: TextSelection(
        baseOffset: range.$1 + prefix.length,
        extentOffset: range.$2 + prefix.length,
      ),
    );
  }

  static (int, int) _range(TextEditingValue value) {
    final selection = value.selection;
    if (!selection.isValid) {
      return (value.text.length, value.text.length);
    }
    return (
      min(selection.start, value.text.length),
      min(selection.end, value.text.length),
    );
  }

  /// Locate a plain-text match without modifying the editor buffer.
  ///
  /// [start] is a UTF-16 selection offset. Matches wrap at the document end
  /// (or beginning for reverse navigation). An empty query is not a match.
  static TextRange? findMatch(
    String text,
    String query, {
    int start = 0,
    bool backwards = false,
  }) {
    if (text.isEmpty || query.isEmpty) return null;
    final haystack = text.toLowerCase();
    final needle = query.toLowerCase();
    if (needle.isEmpty || needle.length > haystack.length) return null;
    final cursor = start < 0
        ? 0
        : (start > haystack.length ? haystack.length : start);
    final index = backwards
        ? (() {
            final previous = cursor == 0
                ? -1
                : haystack.lastIndexOf(needle, cursor - 1);
            return previous >= 0 ? previous : haystack.lastIndexOf(needle);
          })()
        : (() {
            final next = haystack.indexOf(needle, cursor);
            return next >= 0 ? next : haystack.indexOf(needle);
          })();
    if (index < 0) return null;
    return TextRange(start: index, end: index + needle.length);
  }

  static int wordCount(String text) {
    final trimmed = text.trim();
    return trimmed.isEmpty ? 0 : trimmed.split(RegExp(r'\s+')).length;
  }

  static int readingMinutes(String text) =>
      max(1, (wordCount(text) / 220).ceil());
}
