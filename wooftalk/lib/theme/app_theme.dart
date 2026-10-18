import 'package:flutter/material.dart';

class AppColors {
  static const ink = Color(0xFF2B2140);
  static const inkSoft = Color(0xFF6E6489);
  static const cream = Color(0xFFFFF6E9);
  static const blush = Color(0xFFFFE4EE);
  static const sky = Color(0xFFE6F0FF);
  static const orange = Color(0xFFFF8A00);
  static const coral = Color(0xFFFF4E7A);
  static const purple = Color(0xFF7B5CFF);
  static const violet = Color(0xFF5B3DF5);
  static const mint = Color(0xFF22C99A);
  static const gold = Color(0xFFFFC837);
  static const goldDeep = Color(0xFFFF8008);
  static const danger = Color(0xFFFF4D5E);
  static const card = Colors.white;

  static const primaryGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [orange, coral],
  );
  static const purpleGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [Color(0xFF9B7BFF), violet],
  );
  static const goldGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [gold, goldDeep],
  );
  static const recordGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [Color(0xFFFF6B6B), Color(0xFFE8335A)],
  );
}

class AppTheme {
  static const display = 'Fredoka';
  static const body = 'Nunito';

  static ThemeData get light {
    final base = ThemeData(
      useMaterial3: true,
      fontFamily: body,
      colorScheme: ColorScheme.fromSeed(
        seedColor: AppColors.orange,
        primary: AppColors.orange,
        secondary: AppColors.purple,
        surface: AppColors.cream,
      ),
      scaffoldBackgroundColor: AppColors.cream,
      splashFactory: InkSparkle.splashFactory,
    );
    return base.copyWith(
      textTheme: base.textTheme.apply(
        bodyColor: AppColors.ink,
        displayColor: AppColors.ink,
      ),
      pageTransitionsTheme: const PageTransitionsTheme(
        builders: {
          TargetPlatform.android: FadeForwardsPageTransitionsBuilder(),
        },
      ),
    );
  }

  static TextStyle title(double size, {Color color = AppColors.ink}) =>
      TextStyle(
        fontFamily: display,
        fontWeight: FontWeight.w700,
        fontSize: size,
        color: color,
        height: 1.1,
      );

  static TextStyle label(
    double size, {
    Color color = AppColors.ink,
    FontWeight weight = FontWeight.w700,
  }) => TextStyle(
    fontFamily: body,
    fontWeight: weight,
    fontSize: size,
    color: color,
  );

  static List<BoxShadow> softShadow([Color color = const Color(0xFF7B5CFF)]) =>
      [
        BoxShadow(
          color: color.withValues(alpha: 0.18),
          blurRadius: 24,
          offset: const Offset(0, 12),
        ),
        BoxShadow(
          color: Colors.black.withValues(alpha: 0.04),
          blurRadius: 6,
          offset: const Offset(0, 2),
        ),
      ];
}
