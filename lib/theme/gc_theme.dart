import 'package:flutter/material.dart';

/// Native mapping of GoreeCloud UI's Canvas / Surface / Lens semantic roles.
/// Source authority: https://github.com/GoreeCloud/gc-ui/tree/main/tokens
class GcColors {
  const GcColors({
    required this.canvas,
    required this.surface,
    required this.raised,
    required this.tonal,
    required this.text,
    required this.muted,
    required this.outline,
    required this.lens,
    required this.brand,
    required this.onBrand,
    required this.brandContainer,
  });

  final Color canvas, surface, raised, tonal, text, muted, outline, lens;
  final Color brand, onBrand, brandContainer;

  static const light = GcColors(
    canvas: Color(0xFFF2F1FA),
    surface: Color(0xFFFBFAFF),
    raised: Color(0xFFFFFFFF),
    tonal: Color(0xFFECEAF5),
    text: Color(0xFF202239),
    muted: Color(0xFF6B6F85),
    outline: Color(0xFFDCDCE9),
    lens: Color(0xFFFEFDFF),
    brand: Color(0xFF5145D9),
    onBrand: Color(0xFFFFFFFF),
    brandContainer: Color(0xFFE8E6FF),
  );

  static const dark = GcColors(
    canvas: Color(0xFF111320),
    surface: Color(0xFF1A1D2B),
    raised: Color(0xFF25293A),
    tonal: Color(0xFF2B2C42),
    text: Color(0xFFF5F3FF),
    muted: Color(0xFFB3B4C7),
    outline: Color(0xFF393D51),
    lens: Color(0xFF24283A),
    brand: Color(0xFFB6ACFF),
    onBrand: Color(0xFF24185B),
    brandContainer: Color(0xFF39306E),
  );

  static GcColors of(BuildContext context) =>
      Theme.of(context).brightness == Brightness.dark ? dark : light;
}

ThemeData goreeTheme(Brightness brightness) {
  final c = brightness == Brightness.dark ? GcColors.dark : GcColors.light;
  final base = ThemeData(
    useMaterial3: true,
    brightness: brightness,
    colorScheme: ColorScheme.fromSeed(
      seedColor: c.brand,
      brightness: brightness,
      surface: c.surface,
    ),
  );
  return base.copyWith(
    scaffoldBackgroundColor: c.canvas,
    cardColor: c.surface,
    dividerColor: c.outline,
    iconTheme: IconThemeData(color: c.muted, size: 21),
    textTheme: base.textTheme.apply(bodyColor: c.text, displayColor: c.text),
    tooltipTheme: TooltipThemeData(
      waitDuration: const Duration(milliseconds: 450),
      decoration: BoxDecoration(
        color: c.raised,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: c.outline),
      ),
      textStyle: TextStyle(color: c.text, fontSize: 12),
    ),
    snackBarTheme: SnackBarThemeData(
      behavior: SnackBarBehavior.floating,
      backgroundColor: c.raised,
      contentTextStyle: TextStyle(color: c.text),
    ),
  );
}
