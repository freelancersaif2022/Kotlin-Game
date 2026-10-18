import 'dart:ui';

/// Photographic sky colour keys by sun altitude (degrees).
/// Each key: zenith, horizon toward the sun, horizon away from the sun,
/// sun glow. Values are taken from real sky photography and interpolated.
const _keys = <(double, Color, Color, Color, Color)>[
  (
    -18,
    Color(0xFF04060D),
    Color(0xFF070A14),
    Color(0xFF05070F),
    Color(0xFF05070F),
  ),
  (
    -13,
    Color(0xFF080E24),
    Color(0xFF181A33),
    Color(0xFF0C1230),
    Color(0xFF1E1830),
  ),
  (
    -9,
    Color(0xFF0F1A40),
    Color(0xFF3A2E4E),
    Color(0xFF1A2550),
    Color(0xFF52324A),
  ),
  (
    -6,
    Color(0xFF172A60),
    Color(0xFF7C4A5A),
    Color(0xFF2C3D74),
    Color(0xFF9E4C48),
  ),
  (
    -4,
    Color(0xFF1E3772),
    Color(0xFFC0603F),
    Color(0xFF4D5A8E),
    Color(0xFFE0603A),
  ),
  (
    -2,
    Color(0xFF26457F),
    Color(0xFFEE7A3E),
    Color(0xFF86799E),
    Color(0xFFFF7638),
  ),
  (
    0,
    Color(0xFF2E528D),
    Color(0xFFFF9248),
    Color(0xFFB896A8),
    Color(0xFFFF8A40),
  ),
  (
    3,
    Color(0xFF3762A2),
    Color(0xFFFFB46C),
    Color(0xFFBCB4C3),
    Color(0xFFFFB25E),
  ),
  (
    8,
    Color(0xFF3D70B6),
    Color(0xFFF4D7AE),
    Color(0xFFAFC4DA),
    Color(0xFFFFD49A),
  ),
  (
    20,
    Color(0xFF3A79C8),
    Color(0xFFC9DEF0),
    Color(0xFF9EC2E6),
    Color(0xFFFFF0D2),
  ),
  (
    60,
    Color(0xFF2E6DC2),
    Color(0xFFAACDF0),
    Color(0xFF90B8E6),
    Color(0xFFFFF7E6),
  ),
];

/// Direct sunlight colour (for lighting clouds), linear-ish 0..1 RGB.
const _sunKeys = <(double, List<double>)>[
  (-12, [0.02, 0.02, 0.05]),
  (-8, [0.18, 0.08, 0.16]),
  (-5, [0.55, 0.20, 0.25]),
  (-2, [0.95, 0.38, 0.24]),
  (0, [1.0, 0.52, 0.26]),
  (2, [1.0, 0.66, 0.36]),
  (6, [1.0, 0.84, 0.60]),
  (15, [1.0, 0.95, 0.85]),
  (40, [1.0, 0.98, 0.94]),
];

(int, double) _seg(List<double> alts, double alt) {
  if (alt <= alts.first) return (0, 0);
  if (alt >= alts.last) return (alts.length - 2, 1);
  for (var i = 0; i < alts.length - 1; i++) {
    if (alt <= alts[i + 1]) {
      final t = (alt - alts[i]) / (alts[i + 1] - alts[i]);
      return (i, t * t * (3 - 2 * t));
    }
  }
  return (alts.length - 2, 1);
}

/// Colours describing the current scene for one sun altitude.
class SkyPalette {
  SkyPalette._({
    required this.zenith,
    required this.horizonSun,
    required this.horizonAway,
    required this.glow,
    required this.sunLight,
    required this.night,
  });

  final Color zenith;
  final Color horizonSun;
  final Color horizonAway;
  final Color glow;

  /// Direct sunlight (0..1 RGB).
  final List<double> sunLight;

  /// 0 = full day, 1 = full night.
  final double night;

  static SkyPalette compute(double sunAlt) {
    final (i, t) = _seg([for (final k in _keys) k.$1], sunAlt);
    final a = _keys[i], b = _keys[i + 1];
    final (j, u) = _seg([for (final k in _sunKeys) k.$1], sunAlt);
    final sa = _sunKeys[j].$2, sb = _sunKeys[j + 1].$2;
    final n = ((-sunAlt - 2) / 12).clamp(0.0, 1.0);
    return SkyPalette._(
      zenith: Color.lerp(a.$2, b.$2, t)!,
      horizonSun: Color.lerp(a.$3, b.$3, t)!,
      horizonAway: Color.lerp(a.$4, b.$4, t)!,
      glow: Color.lerp(a.$5, b.$5, t)!,
      sunLight: [for (var c = 0; c < 3; c++) sa[c] + (sb[c] - sa[c]) * u],
      night: n * n * (3 - 2 * n),
    );
  }
}
