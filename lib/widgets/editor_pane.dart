import 'package:flutter/material.dart';

import '../theme/gc_theme.dart';

class EditorPane extends StatelessWidget {
  const EditorPane({
    super.key,
    required this.controller,
    required this.focusNode,
  });

  final TextEditingController controller;
  final FocusNode focusNode;

  @override
  Widget build(BuildContext context) {
    final c = GcColors.of(context);
    return Container(
      decoration: BoxDecoration(
        color: c.surface,
        borderRadius: BorderRadius.circular(26),
        border: Border.all(color: c.outline),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        children: [
          _Heading(colors: c),
          Divider(height: 1, thickness: 1, color: c.outline),
          Expanded(
            child: TextField(
              key: const Key('markdown-editor'),
              controller: controller,
              focusNode: focusNode,
              expands: true,
              maxLines: null,
              minLines: null,
              textAlignVertical: TextAlignVertical.top,
              keyboardType: TextInputType.multiline,
              style: TextStyle(
                fontFamily: 'monospace',
                fontSize: 14,
                height: 1.85,
                letterSpacing: 0.15,
                color: c.text,
              ),
              cursorColor: c.brand,
              decoration: const InputDecoration(
                hintText: 'Your next idea begins here…',
                hintStyle: TextStyle(fontFamily: 'monospace'),
                border: InputBorder.none,
                contentPadding: EdgeInsets.all(28),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Heading extends StatelessWidget {
  const _Heading({required this.colors});
  final GcColors colors;

  @override
  Widget build(BuildContext context) => Container(
        height: 60,
        padding: const EdgeInsets.symmetric(horizontal: 22),
        color: colors.raised,
        child: Row(
          children: [
            Container(
              height: 28,
              width: 28,
              decoration: BoxDecoration(
                color: colors.brandContainer,
                borderRadius: BorderRadius.circular(9),
              ),
              child: Icon(Icons.code_rounded, color: colors.brand, size: 17),
            ),
            const SizedBox(width: 12),
            const Text('EDITOR', style: TextStyle(
              fontSize: 11, fontWeight: FontWeight.w800, letterSpacing: 1.7,
            )),
            const Spacer(),
            Text('MARKDOWN', style: TextStyle(
              fontSize: 10, fontWeight: FontWeight.w700, letterSpacing: 1.4,
              color: colors.muted,
            )),
          ],
        ),
      );
}
