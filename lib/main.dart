import 'package:flutter/material.dart';

import 'screens/workspace_screen.dart';
import 'theme/gc_theme.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const GoreeCloudMarkdownApp());
}

class GoreeCloudMarkdownApp extends StatefulWidget {
  const GoreeCloudMarkdownApp({super.key});

  @override
  State<GoreeCloudMarkdownApp> createState() => _GoreeCloudMarkdownAppState();
}

class _GoreeCloudMarkdownAppState extends State<GoreeCloudMarkdownApp> {
  ThemeMode _mode = ThemeMode.system;

  @override
  Widget build(BuildContext context) => MaterialApp(
        title: 'GoreeCloud Markdown',
        debugShowCheckedModeBanner: false,
        theme: goreeTheme(Brightness.light),
        darkTheme: goreeTheme(Brightness.dark),
        themeMode: _mode,
        home: WorkspaceScreen(
          onToggleTheme: () => setState(() {
            _mode = _mode == ThemeMode.dark ? ThemeMode.light : ThemeMode.dark;
          }),
        ),
      );
}
