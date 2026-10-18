import 'dart:math' as math;

import 'day_light.dart';
import 'sun_calc.dart';

// ---------------------------------------------------------------------------
// Sun alignment finder ("Manhattanhenge" style)
// ---------------------------------------------------------------------------

class AlignmentMatch {
  const AlignmentMatch(this.time, this.bearing, this.isSunrise);
  final DateTime time;
  final double bearing;
  final bool isSunrise;
}

double angleDiff(double a, double b) {
  final d = ((a - b) % 360 + 540) % 360 - 180;
  return d.abs();
}

/// Days in the next [days] when the sun rises or sets along [targetBearing]
/// (within [tolerance] degrees). The sun is measured 1° above the horizon,
/// the moment the full disc glows in the street.
List<AlignmentMatch> findSunAlignments({
  required double lat,
  required double lng,
  required double targetBearing,
  DateTime? from,
  int days = 366,
  double tolerance = 0.6,
}) {
  final start = from ?? DateTime.now();
  final result = <AlignmentMatch>[];
  for (var i = 0; i < days; i++) {
    final day = DateTime(start.year, start.month, start.day + i, 12);
    final t = SunTimes.compute(day, lat, lng, const [1.0]).at(1.0);
    for (final (time, rising) in [(t.rising, true), (t.setting, false)]) {
      if (time == null) continue;
      final b = sunPosition(time, lat, lng).bearing;
      if (angleDiff(b, targetBearing) <= tolerance) {
        result.add(AlignmentMatch(time, b, rising));
      }
    }
  }
  result.sort((a, b) => a.time.compareTo(b.time));
  return result;
}

// ---------------------------------------------------------------------------
// Milky Way core visibility
// ---------------------------------------------------------------------------

/// Galactic centre (Sgr A*), J2000.
const double _gcRa = 266.40499;
const double _gcDec = -28.93617;

class MilkyWayWindow {
  const MilkyWayWindow({
    required this.start,
    required this.end,
    required this.bestTime,
    required this.bestAltitude,
    required this.bestBearing,
    required this.moonFraction,
  });
  final DateTime start;
  final DateTime end;
  final DateTime bestTime;
  final double bestAltitude;
  final double bestBearing;
  final double moonFraction;

  String get quality {
    if (moonFraction < 0.25) return 'Excellent';
    if (moonFraction < 0.5) return 'Good';
    if (moonFraction < 0.8) return 'Fair';
    return 'Poor (bright moon)';
  }
}

/// Night of [date] (evening of that day to the next morning): when is the
/// galactic core at least 5° up while the sky is astronomically dark?
MilkyWayWindow? milkyWayForNight(DateTime date, double lat, double lng) {
  final start = DateTime(date.year, date.month, date.day, 12);
  DateTime? first, last, best;
  var bestAlt = -90.0, bestBearing = 0.0;
  for (var m = 0; m <= 24 * 60; m += 10) {
    final t = start.add(Duration(minutes: m));
    if (sunPosition(t, lat, lng).altitude > SunAngle.astronomical) continue;
    final gc = fixedObjectPosition(t, lat, lng, raDeg: _gcRa, decDeg: _gcDec);
    if (gc.altitude < 5) continue;
    first ??= t;
    last = t;
    if (gc.altitude > bestAlt) {
      bestAlt = gc.altitude;
      bestBearing = gc.bearing;
      best = t;
    }
  }
  if (first == null || best == null) return null;
  return MilkyWayWindow(
    start: first,
    end: last!,
    bestTime: best,
    bestAltitude: bestAlt,
    bestBearing: bestBearing,
    moonFraction: moonIllumination(best).fraction,
  );
}

// ---------------------------------------------------------------------------
// Shadows
// ---------------------------------------------------------------------------

class ShadowInfo {
  const ShadowInfo({
    required this.length,
    required this.bearing,
    required this.sunAltitude,
  });

  /// Null when the sun is below the horizon.
  final double? length;
  final double bearing;
  final double sunAltitude;
}

ShadowInfo shadowAt(
  DateTime time,
  double lat,
  double lng,
  double objectHeight,
) {
  final p = sunPosition(time, lat, lng);
  final len = p.altitude <= 0.5
      ? null
      : objectHeight / math.tan(p.altitude * math.pi / 180);
  return ShadowInfo(
    length: len,
    bearing: (p.bearing + 180) % 360,
    sunAltitude: p.altitude,
  );
}

// ---------------------------------------------------------------------------
// Photo tips
// ---------------------------------------------------------------------------

class PhotoTip {
  const PhotoTip(
    this.iso,
    this.shutter,
    this.aperture,
    this.whiteBalance,
    this.advice,
  );
  final String iso;
  final String shutter;
  final String aperture;
  final String whiteBalance;
  final String advice;
}

PhotoTip photoTipFor(LightPhase phase, double sunAltitude) {
  switch (phase) {
    case LightPhase.day:
      return const PhotoTip(
        '100',
        '1/500 s',
        'f/8',
        '5500 K',
        'Hard light. Look for shade, use a polarizer, or shoot backlit subjects.',
      );
    case LightPhase.goldenHour:
      return sunAltitude > 0
          ? const PhotoTip(
              '100–200',
              '1/250 s',
              'f/2.8–f/5.6',
              '5000 K',
              'Put the sun behind your subject for rim light. Expose for the highlights.',
            )
          : const PhotoTip(
              '200–400',
              '1/60 s',
              'f/2.8',
              '4500 K',
              'Sun is just below the horizon: shoot the glowing sky and silhouettes.',
            );
    case LightPhase.blueHour:
      return const PhotoTip(
        '100–400',
        '1–4 s (tripod)',
        'f/8',
        '4000 K',
        'Perfect for city lights and long exposures. Balance sky and artificial light.',
      );
    case LightPhase.nauticalTwilight:
      return const PhotoTip(
        '800',
        '8–15 s (tripod)',
        'f/4',
        '3800 K',
        'Last colour in the sky. Use a tripod and a 2 s timer.',
      );
    case LightPhase.astronomicalTwilight:
    case LightPhase.night:
      return const PhotoTip(
        '3200',
        '15–20 s',
        'f/1.8–f/2.8',
        '3800 K',
        'Night sky: focus manually on a bright star, use the 500 rule to avoid trails.',
      );
  }
}
