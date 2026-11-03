import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:goreecloud_markdown/main.dart';
import 'package:goreecloud_markdown/screens/workspace_screen.dart';

void main() {
  testWidgets('workspace opens with editable Markdown', (tester) async {
    await tester.pumpWidget(const GoreeCloudMarkdownApp());
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('markdown-editor')), findsOneWidget);
    expect(find.text('Welcome.md'), findsOneWidget);
  });

  testWidgets('preview switch is accessible on a compact screen', (tester) async {
    tester.view.physicalSize = const Size(420, 880);
    tester.view.devicePixelRatio = 1;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });
    await tester.pumpWidget(const GoreeCloudMarkdownApp());
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('Preview'));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('markdown-preview')), findsOneWidget);
  });

  testWidgets('compact preview honours disabled animations', (tester) async {
    tester.view.physicalSize = const Size(420, 880);
    tester.view.devicePixelRatio = 1;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });

    await tester.pumpWidget(MaterialApp(
      home: MediaQuery(
        data: const MediaQueryData(disableAnimations: true),
        child: WorkspaceScreen(onToggleTheme: () {}),
      ),
    ));
    await tester.pumpAndSettle();
    final switcher = tester.widget<AnimatedSwitcher>(find.byType(AnimatedSwitcher));
    expect(switcher.duration, Duration.zero);

    await tester.tap(find.byTooltip('Preview'));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('markdown-preview')), findsOneWidget);
  });
}
