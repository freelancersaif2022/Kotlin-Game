import 'package:flutter/cupertino.dart' show CupertinoPageTransitionsBuilder;
import 'package:flutter/material.dart';

abstract final class GH {
  static const gold = Color(0xFFFFB25B);
  static const goldLight = Color(0xFFFFD99A);
  static const amber = Color(0xFFFF8A3D);
  static const rose = Color(0xFFFF6F7D);
  static const blue = Color(0xFF7FA8FF);
  static const blueDeep = Color(0xFF3B5BDB);
  static const ink = Color(0xFF07080F);
  static const night = Color(0xFF0C0F1C);
  static const textHi = Color(0xFFF6F3EE);
  static const textMid = Color(0xB3F6F3EE);
  static const textLo = Color(0x73F6F3EE);

  static const goldGradient = LinearGradient(
    colors: [Color(0xFFFFE3A3), Color(0xFFFFB25B), Color(0xFFFF7A45)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const tabular = [FontFeature.tabularFigures()];

  static ThemeData theme() {
    const scheme = ColorScheme.dark(
      primary: gold,
      secondary: blue,
      surface: night,
      onSurface: textHi,
      onPrimary: ink,
    );
    final base = ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: ink,
      splashFactory: InkSparkle.splashFactory,
    );
    return base.copyWith(
      textTheme: base.textTheme.apply(bodyColor: textHi, displayColor: textHi),
      switchTheme: SwitchThemeData(
        thumbColor: WidgetStateProperty.resolveWith(
          (s) => s.contains(WidgetState.selected) ? ink : textMid,
        ),
        trackColor: WidgetStateProperty.resolveWith(
          (s) => s.contains(WidgetState.selected) ? gold : Colors.white12,
        ),
        trackOutlineColor: const WidgetStatePropertyAll(Colors.transparent),
      ),
      sliderTheme: const SliderThemeData(
        activeTrackColor: gold,
        thumbColor: goldLight,
        inactiveTrackColor: Colors.white12,
        overlayColor: Color(0x22FFB25B),
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        backgroundColor: const Color(0xFF1B1E2C),
        contentTextStyle: const TextStyle(color: textHi),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      ),
      pageTransitionsTheme: const PageTransitionsTheme(
        builders: {
          TargetPlatform.android: FadeForwardsPageTransitionsBuilder(),
          TargetPlatform.iOS: CupertinoPageTransitionsBuilder(),
        },
      ),
    );
  }

  // Typography -------------------------------------------------------------

  static const display = TextStyle(
    fontSize: 64,
    fontWeight: FontWeight.w200,
    letterSpacing: -2,
    height: 1,
    color: textHi,
    fontFeatures: tabular,
  );

  static const title = TextStyle(
    fontSize: 22,
    fontWeight: FontWeight.w600,
    letterSpacing: -0.3,
  );
  static const label = TextStyle(
    fontSize: 11,
    fontWeight: FontWeight.w700,
    letterSpacing: 1.8,
    color: textMid,
  );
  static const body = TextStyle(fontSize: 15, color: textMid, height: 1.35);
  static const value = TextStyle(
    fontSize: 20,
    fontWeight: FontWeight.w500,
    color: textHi,
    fontFeatures: tabular,
  );
}
