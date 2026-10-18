import 'package:flutter_test/flutter_test.dart';
import 'package:golden_hour/astro/day_light.dart';
import 'package:golden_hour/astro/extras.dart';
import 'package:golden_hour/astro/sun_calc.dart';
import 'package:golden_hour/screens/moon_screen.dart';

// Run with TZ=UTC so local DateTimes equal UTC.
void main() {
  void near(DateTime? actual, DateTime expected, {int minutes = 2}) {
    expect(actual, isNotNull);
    final diff = actual!.toUtc().difference(expected).inSeconds.abs();
    expect(
      diff,
      lessThanOrEqualTo(minutes * 60),
      reason: 'got ${actual.toUtc()} expected $expected',
    );
  }

  test('London summer solstice sunrise / sunset', () {
    final d = DayLight.forDate(DateTime(2024, 6, 21), 51.5074, -0.1278);
    near(d.sunrise, DateTime.utc(2024, 6, 21, 3, 43));
    near(d.sunset, DateTime.utc(2024, 6, 21, 20, 21));
  });

  test('New York winter solstice sunrise / sunset', () {
    final d = DayLight.forDate(DateTime(2024, 12, 21), 40.7128, -74.0060);
    near(d.sunrise, DateTime.utc(2024, 12, 21, 12, 17));
    near(d.sunset, DateTime.utc(2024, 12, 21, 21, 32));
  });

  test('golden and blue hour windows are ordered', () {
    final d = DayLight.forDate(DateTime(2024, 9, 10), 24.86, 67.0); // Karachi
    final bm = d.blueHourMorning!, gm = d.goldenHourMorning!;
    final ge = d.goldenHourEvening!, be = d.blueHourEvening!;
    expect(bm.end, gm.start);
    expect(gm.start.isBefore(d.sunrise!), isTrue);
    expect(gm.end.isAfter(d.sunrise!), isTrue);
    expect(ge.start.isBefore(d.sunset!), isTrue);
    expect(ge.end, be.start);
    expect(gm.length.inMinutes, inInclusiveRange(25, 60));
  });

  test('polar night has no sunrise', () {
    final d = DayLight.forDate(
      DateTime(2024, 12, 21),
      78.22,
      15.65,
    ); // Svalbard
    expect(d.sunrise, isNull);
    expect(d.goldenHourEvening, isNull);
  });

  test('sun bearing at sunset is in the west', () {
    final d = DayLight.forDate(DateTime(2024, 6, 21), 51.5, 0);
    final b = sunPosition(d.sunset!, 51.5, 0).bearing;
    expect(b, inInclusiveRange(300, 320)); // ~310° NW at summer solstice
  });

  test('full moon Jan 2024 (25 Jan 17:54 UTC)', () {
    final full = MoonScreen.nextPhase(DateTime.utc(2024, 1, 20), 0.5);
    expect(
      full.difference(DateTime.utc(2024, 1, 25, 17, 54)).inHours.abs(),
      lessThanOrEqualTo(8),
    );
  });

  test('status countdown points to next golden hour', () {
    final s = LightStatus.at(DateTime(2024, 6, 21, 12), 51.5, -0.12);
    expect(s.target, isNotNull);
    expect(s.headline, contains('golden hour'));
    expect(s.target!.hour, inInclusiveRange(18, 20));
  });

  test('alignment finder finds Manhattanhenge (late May / mid July)', () {
    final r = findSunAlignments(
      lat: 40.758,
      lng: -73.985,
      targetBearing: 299,
      from: DateTime(2024, 1, 1),
      tolerance: 0.4,
    ).where((m) => !m.isSunrise).toList();
    expect(r.any((m) => m.time.month == 5 && m.time.day >= 26), isTrue);
    expect(
      r.any((m) => m.time.month == 7 && m.time.day >= 9 && m.time.day <= 14),
      isTrue,
    );
  });

  test('Milky Way visible in summer, not in December (mid-latitude)', () {
    expect(milkyWayForNight(DateTime(2024, 7, 5), 36.0, -112.0), isNotNull);
    expect(milkyWayForNight(DateTime(2024, 12, 15), 36.0, -112.0), isNull);
  });
}
