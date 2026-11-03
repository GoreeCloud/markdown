import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:goreecloud_markdown/widgets/preview_pane.dart';

void main() {
  testWidgets('external Markdown images render only a local placeholder',
      (tester) async {
    await tester.pumpWidget(const MaterialApp(
      home: Scaffold(
        body: PreviewPane(
          markdown: '![Private diagram](https://example.invalid/track.png)',
        ),
      ),
    ));
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.image_not_supported_outlined), findsOneWidget);
    expect(find.text('Private diagram'), findsOneWidget);
    expect(find.byType(Image), findsNothing);
    expect(tester.takeException(), isNull);
  });

  testWidgets('rendering external Markdown links never activates them',
      (tester) async {
    final activated = <String>[];
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: PreviewPane(
          markdown: '[Open website](https://example.invalid/path)',
          onExternalLink: activated.add,
        ),
      ),
    ));
    await tester.pumpAndSettle();

    expect(activated, isEmpty);
    expect(tester.takeException(), isNull);
  });
}
