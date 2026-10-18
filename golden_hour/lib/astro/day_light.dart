import 'sun_calc.dart';

/// Sun altitude thresholds (degrees) used by photographers.
abstract final class SunAngle {
  static const double astronomical = -18;
  static const double nautical = -12;
  static const double civil = -6; // blue hour starts / ends
  static const double blueGolden = -4; // blue hour <-> golden hour
  static const double horizon = -0.833; // sunrise / sunset (refraction + disc)
  static const double goldenTop = 6; // golden hour ends / starts

  static const all = [
    astronomical,
    nautical,
    civil,
    blueGolden,
    horizon,
    goldenTop,
  ];
}

enum LightPhase {
  night('Night'),
  astronomicalTwilight('Astronomical Twilight'),
  nauticalTwilight('Nautical Twilight'),
  blueHour('Blue Hour'),
  goldenHour('Golden Hour'),
  day('Daylight');

  const LightPhase(this.label);
  final String label;

  static LightPhase fromAltitude(double alt) {
    if (alt < SunAngle.astronomical) return night;
    if (alt < SunAngle.nautical) return astronomicalTwilight;
    if (alt < SunAngle.civil) return nauticalTwilight;
    if (alt < SunAngle.blueGolden) return blueHour;
    if (alt < SunAngle.goldenTop) return goldenHour;
    return day;
  }
}

/// A time window like "evening golden hour".
class LightWindow {
  const LightWindow(this.start, this.end);
  final DateTime start;
  final DateTime end;

  Duration get length => end.difference(start);
  bool contains(DateTime t) => !t.isBefore(start) && t.isBefore(end);
}

enum DayEventKind {
  astronomicalDawn,
  nauticalDawn,
  blueHourMorning,
  goldenHourMorning,
  sunrise,
  goldenHourMorningEnd,
  solarNoon,
  goldenHourEvening,
  sunset,
  blueHourEvening,
  blueHourEveningEnd,
  nauticalDusk,
  astronomicalDusk,
}

class DayEvent {
  const DayEvent(this.kind, this.time, this.title, this.subtitle);
  final DayEventKind kind;
  final DateTime time;
  final String title;
  final String subtitle;
}

/// All light information for one local calendar day at one place.
class DayLight {
  DayLight._({
    required this.date,
    required this.lat,
    required this.lng,
    required this.times,
  });

  factory DayLight.forDate(DateTime date, double lat, double lng) {
    final noon = DateTime(date.year, date.month, date.day, 12);
    return DayLight._(
      date: DateTime(date.year, date.month, date.day),
      lat: lat,
      lng: lng,
      times: SunTimes.compute(noon, lat, lng, SunAngle.all),
    );
  }

  final DateTime date;
  final double lat;
  final double lng;
  final SunTimes times;

  DateTime? get sunrise => times.at(SunAngle.horizon).rising;
  DateTime? get sunset => times.at(SunAngle.horizon).setting;
  DateTime get solarNoon => times.solarNoon;

  LightWindow? _window(DateTime? a, DateTime? b) =>
      (a == null || b == null) ? null : LightWindow(a, b);

  LightWindow? get blueHourMorning => _window(
    times.at(SunAngle.civil).rising,
    times.at(SunAngle.blueGolden).rising,
  );
  LightWindow? get goldenHourMorning => _window(
    times.at(SunAngle.blueGolden).rising,
    times.at(SunAngle.goldenTop).rising,
  );
  LightWindow? get goldenHourEvening => _window(
    times.at(SunAngle.goldenTop).setting,
    times.at(SunAngle.blueGolden).setting,
  );
  LightWindow? get blueHourEvening => _window(
    times.at(SunAngle.blueGolden).setting,
    times.at(SunAngle.civil).setting,
  );

  Duration? get dayLength {
    final r = sunrise, s = sunset;
    if (r == null || s == null) return null;
    return s.difference(r);
  }

  /// Noon sun altitude, useful for "polar night" / "midnight sun" messages.
  double get noonAltitude => sunPosition(solarNoon, lat, lng).altitude;

  List<DayEvent> get events {
    final list = <DayEvent>[];
    void add(DayEventKind k, DateTime? t, String title, String sub) {
      if (t != null) list.add(DayEvent(k, t, title, sub));
    }

    add(
      DayEventKind.astronomicalDawn,
      times.at(SunAngle.astronomical).rising,
      'Astronomical Dawn',
      'First trace of light',
    );
    add(
      DayEventKind.nauticalDawn,
      times.at(SunAngle.nautical).rising,
      'Nautical Dawn',
      'Horizon becomes visible',
    );
    add(
      DayEventKind.blueHourMorning,
      times.at(SunAngle.civil).rising,
      'Blue Hour',
      'Deep blue, soft light',
    );
    add(
      DayEventKind.goldenHourMorning,
      times.at(SunAngle.blueGolden).rising,
      'Golden Hour',
      'Warm, low-angle light',
    );
    add(DayEventKind.sunrise, sunrise, 'Sunrise', 'Sun breaks the horizon');
    add(
      DayEventKind.goldenHourMorningEnd,
      times.at(SunAngle.goldenTop).rising,
      'Golden Hour Ends',
      'Light turns harsh',
    );
    add(DayEventKind.solarNoon, solarNoon, 'Solar Noon', 'Sun at its highest');
    add(
      DayEventKind.goldenHourEvening,
      times.at(SunAngle.goldenTop).setting,
      'Golden Hour',
      'Warm, low-angle light',
    );
    add(DayEventKind.sunset, sunset, 'Sunset', 'Sun touches the horizon');
    add(
      DayEventKind.blueHourEvening,
      times.at(SunAngle.blueGolden).setting,
      'Blue Hour',
      'Deep blue, city lights',
    );
    add(
      DayEventKind.blueHourEveningEnd,
      times.at(SunAngle.civil).setting,
      'Civil Dusk',
      'Blue hour fades',
    );
    add(
      DayEventKind.nauticalDusk,
      times.at(SunAngle.nautical).setting,
      'Nautical Dusk',
      'Horizon disappears',
    );
    add(
      DayEventKind.astronomicalDusk,
      times.at(SunAngle.astronomical).setting,
      'Astronomical Dusk',
      'True darkness begins',
    );
    list.sort((a, b) => a.time.compareTo(b.time));
    return list;
  }

  /// Golden + blue windows of the day in order.
  List<(String, LightWindow, LightPhase)> get photoWindows => [
    if (blueHourMorning != null)
      ('Morning Blue Hour', blueHourMorning!, LightPhase.blueHour),
    if (goldenHourMorning != null)
      ('Morning Golden Hour', goldenHourMorning!, LightPhase.goldenHour),
    if (goldenHourEvening != null)
      ('Evening Golden Hour', goldenHourEvening!, LightPhase.goldenHour),
    if (blueHourEvening != null)
      ('Evening Blue Hour', blueHourEvening!, LightPhase.blueHour),
  ];
}

/// What the big countdown on the home screen should say.
class LightStatus {
  const LightStatus({
    required this.phase,
    required this.headline,
    required this.target,
    required this.sunAltitude,
    required this.sunBearing,
    this.activeWindow,
  });

  final LightPhase phase;
  final String headline;
  final DateTime? target;
  final double sunAltitude;
  final double sunBearing;
  final LightWindow? activeWindow;

  static LightStatus at(DateTime now, double lat, double lng) {
    final pos = sunPosition(now, lat, lng);
    final phase = LightPhase.fromAltitude(pos.altitude);
    final today = DayLight.forDate(now, lat, lng);
    final tomorrow = DayLight.forDate(
      now.add(const Duration(days: 1)),
      lat,
      lng,
    );

    final windows = [...today.photoWindows, ...tomorrow.photoWindows];

    for (final (name, w, _) in windows) {
      if (w.contains(now)) {
        return LightStatus(
          phase: phase,
          headline: '${name.split(' ').skip(1).join(' ')} ends in',
          target: w.end,
          sunAltitude: pos.altitude,
          sunBearing: pos.bearing,
          activeWindow: w,
        );
      }
    }

    for (final (name, w, p) in windows) {
      if (p == LightPhase.goldenHour && w.start.isAfter(now)) {
        return LightStatus(
          phase: phase,
          headline: '${name.split(' ').first} golden hour in',
          target: w.start,
          sunAltitude: pos.altitude,
          sunBearing: pos.bearing,
        );
      }
    }

    return LightStatus(
      phase: phase,
      headline: pos.altitude > 0
          ? 'The sun stays up today'
          : 'The sun stays down today',
      target: null,
      sunAltitude: pos.altitude,
      sunBearing: pos.bearing,
    );
  }
}
