// Offline astronomy engine.
//
// Sun and moon formulas are a Dart port of the well known "SunCalc" algorithms
// (Vladimir Agafonkin, BSD-2-Clause), which are based on the formulas from
// http://aa.quae.nl/en/reken/zonpositie.html. Accuracy is about 1 minute for
// sun events, which is more than enough for photography planning.

import 'dart:math' as math;

const double _rad = math.pi / 180;
const double _deg = 180 / math.pi;
const double _dayMs = 1000 * 60 * 60 * 24;
const double _j1970 = 2440588;
const double _j2000 = 2451545;
const double _e = _rad * 23.4397; // obliquity of the Earth

double _toJulian(DateTime date) =>
    date.millisecondsSinceEpoch / _dayMs - 0.5 + _j1970;

DateTime? _fromJulian(double j) {
  if (j.isNaN || j.isInfinite) return null;
  return DateTime.fromMillisecondsSinceEpoch(
    ((j + 0.5 - _j1970) * _dayMs).round(),
  );
}

double _toDays(DateTime date) => _toJulian(date) - _j2000;

double _rightAscension(double l, double b) => math.atan2(
  math.sin(l) * math.cos(_e) - math.tan(b) * math.sin(_e),
  math.cos(l),
);

double _declination(double l, double b) => math.asin(
  math.sin(b) * math.cos(_e) + math.cos(b) * math.sin(_e) * math.sin(l),
);

double _azimuth(double h, double phi, double dec) => math.atan2(
  math.sin(h),
  math.cos(h) * math.sin(phi) - math.tan(dec) * math.cos(phi),
);

double _altitude(double h, double phi, double dec) => math.asin(
  math.sin(phi) * math.sin(dec) + math.cos(phi) * math.cos(dec) * math.cos(h),
);

double _siderealTime(double d, double lw) =>
    _rad * (280.16 + 360.9856235 * d) - lw;

double _astroRefraction(double h) {
  if (h < 0) h = 0;
  return 0.0002967 / math.tan(h + 0.00312536 / (h + 0.08901179));
}

double _solarMeanAnomaly(double d) => _rad * (357.5291 + 0.98560028 * d);

double _eclipticLongitude(double m) {
  final c =
      _rad *
      (1.9148 * math.sin(m) +
          0.02 * math.sin(2 * m) +
          0.0003 * math.sin(3 * m));
  const p = _rad * 102.9372; // perihelion of the Earth
  return m + c + p + math.pi;
}

class _EqCoords {
  const _EqCoords(this.ra, this.dec, [this.dist = 0]);
  final double ra;
  final double dec;
  final double dist;
}

_EqCoords _sunCoords(double d) {
  final m = _solarMeanAnomaly(d);
  final l = _eclipticLongitude(m);
  return _EqCoords(_rightAscension(l, 0), _declination(l, 0));
}

_EqCoords _moonCoords(double d) {
  final l = _rad * (218.316 + 13.176396 * d);
  final m = _rad * (134.963 + 13.064993 * d);
  final f = _rad * (93.272 + 13.229350 * d);
  final lon = l + _rad * 6.289 * math.sin(m);
  final lat = _rad * 5.128 * math.sin(f);
  final dist = 385001 - 20905 * math.cos(m);
  return _EqCoords(_rightAscension(lon, lat), _declination(lon, lat), dist);
}

/// Position of a body in the sky, in degrees.
class SkyPosition {
  const SkyPosition({
    required this.altitude,
    required this.bearing,
    this.distanceKm,
  });

  /// Degrees above the horizon (negative = below).
  final double altitude;

  /// Compass bearing in degrees: 0 = north, 90 = east, 180 = south, 270 = west.
  final double bearing;

  final double? distanceKm;
}

double _bearingFromSuncalc(double az) => (az * _deg + 180) % 360;

/// Sun position for [date] at [lat]/[lng].
SkyPosition sunPosition(DateTime date, double lat, double lng) {
  final lw = _rad * -lng;
  final phi = _rad * lat;
  final d = _toDays(date);
  final c = _sunCoords(d);
  final h = _siderealTime(d, lw) - c.ra;
  return SkyPosition(
    altitude: _altitude(h, phi, c.dec) * _deg,
    bearing: _bearingFromSuncalc(_azimuth(h, phi, c.dec)),
  );
}

/// Moon position for [date] at [lat]/[lng] (refraction corrected).
SkyPosition moonPosition(DateTime date, double lat, double lng) {
  final lw = _rad * -lng;
  final phi = _rad * lat;
  final d = _toDays(date);
  final c = _moonCoords(d);
  final hAngle = _siderealTime(d, lw) - c.ra;
  var h = _altitude(hAngle, phi, c.dec);
  h += _astroRefraction(h);
  return SkyPosition(
    altitude: h * _deg,
    bearing: _bearingFromSuncalc(_azimuth(hAngle, phi, c.dec)),
    distanceKm: c.dist,
  );
}

/// Position of a fixed star / deep sky object given its RA/Dec in degrees.
SkyPosition fixedObjectPosition(
  DateTime date,
  double lat,
  double lng, {
  required double raDeg,
  required double decDeg,
}) {
  final lw = _rad * -lng;
  final phi = _rad * lat;
  final d = _toDays(date);
  final dec = decDeg * _rad;
  final h = _siderealTime(d, lw) - raDeg * _rad;
  return SkyPosition(
    altitude: _altitude(h, phi, dec) * _deg,
    bearing: _bearingFromSuncalc(_azimuth(h, phi, dec)),
  );
}

// ---------------------------------------------------------------------------
// Sun event times
// ---------------------------------------------------------------------------

const double _j0 = 0.0009;

double _julianCycle(double d, double lw) =>
    (d - _j0 - lw / (2 * math.pi)).roundToDouble();

double _approxTransit(double ht, double lw, double n) =>
    _j0 + (ht + lw) / (2 * math.pi) + n;

double _solarTransitJ(double ds, double m, double l) =>
    _j2000 + ds + 0.0053 * math.sin(m) - 0.0069 * math.sin(2 * l);

double _hourAngle(double h, double phi, double d) => math.acos(
  (math.sin(h) - math.sin(phi) * math.sin(d)) / (math.cos(phi) * math.cos(d)),
);

/// Rise (morning) and set (evening) moments for a given sun altitude.
class SunCrossing {
  const SunCrossing(this.rising, this.setting);

  /// Null when the sun never reaches that altitude that day (polar regions).
  final DateTime? rising;
  final DateTime? setting;
}

/// Calculates the moments the sun crosses each altitude in [anglesDeg]
/// on the solar day closest to [date].
class SunTimes {
  SunTimes._(this.solarNoon, this.nadir, this._crossings);

  final DateTime solarNoon;
  final DateTime nadir;
  final Map<double, SunCrossing> _crossings;

  SunCrossing at(double angleDeg) => _crossings[angleDeg]!;

  static SunTimes compute(
    DateTime date,
    double lat,
    double lng,
    Iterable<double> anglesDeg,
  ) {
    final lw = _rad * -lng;
    final phi = _rad * lat;
    final d = _toDays(date);
    final n = _julianCycle(d, lw);
    final ds = _approxTransit(0, lw, n);
    final m = _solarMeanAnomaly(ds);
    final l = _eclipticLongitude(m);
    final dec = _declination(l, 0);
    final jNoon = _solarTransitJ(ds, m, l);

    final crossings = <double, SunCrossing>{};
    for (final angle in anglesDeg) {
      final w = _hourAngle(angle * _rad, phi, dec);
      if (w.isNaN) {
        crossings[angle] = const SunCrossing(null, null);
        continue;
      }
      final a = _approxTransit(w, lw, n);
      final jSet = _solarTransitJ(a, m, l);
      final jRise = jNoon - (jSet - jNoon);
      crossings[angle] = SunCrossing(_fromJulian(jRise), _fromJulian(jSet));
    }
    return SunTimes._(
      _fromJulian(jNoon)!,
      _fromJulian(jNoon - 0.5)!,
      crossings,
    );
  }
}

// ---------------------------------------------------------------------------
// Moon illumination & times
// ---------------------------------------------------------------------------

class MoonIllumination {
  const MoonIllumination({
    required this.fraction,
    required this.phase,
    required this.angle,
  });

  /// Illuminated fraction 0..1.
  final double fraction;

  /// 0 = new, 0.25 = first quarter, 0.5 = full, 0.75 = last quarter.
  final double phase;

  /// Midpoint angle of the illuminated limb (radians, east of north).
  final double angle;

  String get phaseName {
    const names = [
      'New Moon',
      'Waxing Crescent',
      'First Quarter',
      'Waxing Gibbous',
      'Full Moon',
      'Waning Gibbous',
      'Last Quarter',
      'Waning Crescent',
    ];
    final index = ((phase * 8) + 0.5).floor() % 8;
    return names[index];
  }
}

MoonIllumination moonIllumination(DateTime date) {
  final d = _toDays(date);
  final s = _sunCoords(d);
  final m = _moonCoords(d);
  const sdist = 149598000.0;
  final phi = math.acos(
    math.sin(s.dec) * math.sin(m.dec) +
        math.cos(s.dec) * math.cos(m.dec) * math.cos(s.ra - m.ra),
  );
  final inc = math.atan2(sdist * math.sin(phi), m.dist - sdist * math.cos(phi));
  final angle = math.atan2(
    math.cos(s.dec) * math.sin(s.ra - m.ra),
    math.sin(s.dec) * math.cos(m.dec) -
        math.cos(s.dec) * math.sin(m.dec) * math.cos(s.ra - m.ra),
  );
  return MoonIllumination(
    fraction: (1 + math.cos(inc)) / 2,
    phase: 0.5 + 0.5 * inc * (angle < 0 ? -1 : 1) / math.pi,
    angle: angle,
  );
}

class MoonTimes {
  const MoonTimes({
    this.rise,
    this.set,
    this.alwaysUp = false,
    this.alwaysDown = false,
  });
  final DateTime? rise;
  final DateTime? set;
  final bool alwaysUp;
  final bool alwaysDown;
}

/// Moonrise / moonset during the local calendar day of [day].
MoonTimes moonTimes(DateTime day, double lat, double lng) {
  final t = DateTime(day.year, day.month, day.day);
  const hc = 0.133; // degrees
  double alt(double hours) =>
      moonPosition(
        t.add(Duration(milliseconds: (hours * 3600000).round())),
        lat,
        lng,
      ).altitude -
      hc;

  var h0 = alt(0);
  double? rise;
  double? set;
  double ye = 0;
  for (var i = 1; i <= 24; i += 2) {
    final h1 = alt(i.toDouble());
    final h2 = alt(i + 1.0);
    final a = (h0 + h2) / 2 - h1;
    final b = (h2 - h0) / 2;
    final xe = -b / (2 * a);
    ye = (a * xe + b) * xe + h1;
    final disc = b * b - 4 * a * h1;
    var roots = 0;
    double x1 = 0, x2 = 0;
    if (disc >= 0) {
      final dx = math.sqrt(disc) / (a.abs() * 2);
      x1 = xe - dx;
      x2 = xe + dx;
      if (x1.abs() <= 1) roots++;
      if (x2.abs() <= 1) roots++;
      if (x1 < -1) x1 = x2;
    }
    if (roots == 1) {
      if (h0 < 0) {
        rise = i + x1;
      } else {
        set = i + x1;
      }
    } else if (roots == 2) {
      rise = i + (ye < 0 ? x2 : x1);
      set = i + (ye < 0 ? x1 : x2);
    }
    if (rise != null && set != null) break;
    h0 = h2;
  }

  DateTime at(double hours) =>
      t.add(Duration(milliseconds: (hours * 3600000).round()));
  return MoonTimes(
    rise: rise == null ? null : at(rise),
    set: set == null ? null : at(set),
    alwaysUp: rise == null && set == null && ye > 0,
    alwaysDown: rise == null && set == null && ye <= 0,
  );
}
