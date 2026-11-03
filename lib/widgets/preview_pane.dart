import 'package:flutter/material.dart';
import 'package:flutter_markdown_plus/flutter_markdown_plus.dart';

import '../theme/gc_theme.dart';

class PreviewPane extends StatelessWidget {
  const PreviewPane({super.key, required this.markdown, this.onExternalLink});

  final String markdown;
  final ValueChanged<String>? onExternalLink;

  @override
  Widget build(BuildContext context) {
    final c = GcColors.of(context);
    final theme = Theme.of(context);
    return Container(
      decoration: BoxDecoration(
        color: c.raised,
        border: Border.all(color: c.outline),
        borderRadius: BorderRadius.circular(26),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        children: [
          Container(
            height: 60,
            padding: const EdgeInsets.symmetric(horizontal: 22),
            color: c.surface,
            child: Row(
              children: [
                Container(
                  height: 28,
                  width: 28,
                  decoration: BoxDecoration(
                    color: c.brandContainer,
                    borderRadius: BorderRadius.circular(9),
                  ),
                  child: Icon(Icons.visibility_outlined, size: 17, color: c.brand),
                ),
                const SizedBox(width: 12),
                const Text('PREVIEW', style: TextStyle(
                  fontSize: 11, fontWeight: FontWeight.w800, letterSpacing: 1.7,
                )),
                const Spacer(),
                Icon(Icons.shield_outlined, color: c.muted, size: 15),
                const SizedBox(width: 5),
                Text('LOCAL ONLY', style: TextStyle(
                  color: c.muted, fontWeight: FontWeight.w700,
                  letterSpacing: 1.1, fontSize: 10,
                )),
              ],
            ),
          ),
          Divider(height: 1, thickness: 1, color: c.outline),
          Expanded(
            child: markdown.trim().isEmpty
                ? Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.auto_awesome_outlined, size: 34, color: c.brand),
                        const SizedBox(height: 14),
                        Text('A beautiful blank canvas',
                          style: TextStyle(color: c.text, fontSize: 20,
                            fontWeight: FontWeight.w700)),
                        const SizedBox(height: 7),
                        Text('Your writing will appear here as you type.',
                          style: TextStyle(color: c.muted)),
                      ],
                    ),
                  )
                : Markdown(
                    key: const Key('markdown-preview'),
                    data: markdown,
                    selectable: true,
                    padding: const EdgeInsets.all(32),
                    onTapLink: (_, href, __) {
                      if (href != null) onExternalLink?.call(href);
                    },
                    imageBuilder: (uri, title, alt) => Semantics(
                      label: 'Image not loaded: ${alt ?? uri.toString()}',
                      child: Container(
                        margin: const EdgeInsets.symmetric(vertical: 8),
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: c.tonal,
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(Icons.image_not_supported_outlined, color: c.muted),
                            const SizedBox(width: 10),
                            Flexible(child: Text(
                              alt?.isNotEmpty == true ? alt! : 'External image blocked',
                              style: TextStyle(color: c.muted),
                            )),
                          ],
                        ),
                      ),
                    ),
                    styleSheet: MarkdownStyleSheet.fromTheme(theme).copyWith(
                      p: TextStyle(color: c.text, height: 1.8, fontSize: 15),
                      h1: TextStyle(color: c.text, fontWeight: FontWeight.w800,
                        fontSize: 33, height: 1.24),
                      h2: TextStyle(color: c.text, fontWeight: FontWeight.w800,
                        fontSize: 25, height: 1.35),
                      h3: TextStyle(color: c.text, fontWeight: FontWeight.w700,
                        fontSize: 20, height: 1.4),
                      a: TextStyle(color: c.brand, decoration: TextDecoration.underline),
                      code: TextStyle(fontFamily: 'monospace', color: c.brand,
                        backgroundColor: c.tonal, fontSize: 13),
                      codeblockDecoration: BoxDecoration(
                        color: c.tonal, borderRadius: BorderRadius.circular(14),
                      ),
                      blockquoteDecoration: BoxDecoration(
                        color: c.tonal,
                        border: Border(left: BorderSide(color: c.brand, width: 3)),
                      ),
                      blockquotePadding: const EdgeInsets.all(15),
                      horizontalRuleDecoration: BoxDecoration(
                        border: Border(top: BorderSide(color: c.outline)),
                      ),
                    ),
                  ),
          ),
        ],
      ),
    );
  }
}
