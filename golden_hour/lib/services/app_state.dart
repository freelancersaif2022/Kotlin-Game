import 'dart:convert';

import 'package:flutter/widgets.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// A place the user plans for. Coordinates only — everything works offline.
class Place {
  const Place({
    required this.name,
    required this.lat,
    required this.lng,
    this.isGps = false,
  });

  final String name;
  final double lat;
  final double lng;
  final bool isGps;

  Map<String, dynamic> toJson() => {
    'name': name,
    'lat': lat,
    'lng': lng,
    'gps': isGps,
  };

  factory Place.fromJson(Map<String, dynamic> j) => Place(
    name: j['name'] as String,
    lat: (j['lat'] as num).toDouble(),
    lng: (j['lng'] as num).toDouble(),
    isGps: j['gps'] as bool? ?? false,
  );

  String get coords =>
      '${lat.abs().toStringAsFixed(3)}°${lat >= 0 ? 'N' : 'S'}  '
      '${lng.abs().toStringAsFixed(3)}°${lng >= 0 ? 'E' : 'W'}';
}

/// Features that can be unlocked for 24 hours with a rewarded ad.
enum RewardFeature {
  anyDate('Any-date planning'),
  shadowCalculator('Shadow calculator'),
  extraAlert('Extra alert today');

  const RewardFeature(this.label);
  final String label;
}

class AlertSettings {
  const AlertSettings({
    this.enabled = true,
    this.minutesBefore = 20,
    this.morningGolden = false,
    this.eveningGolden = true,
    this.blueHour = false,
  });

  final bool enabled;
  final int minutesBefore;
  final bool morningGolden;
  final bool eveningGolden;
  final bool blueHour;

  AlertSettings copyWith({
    bool? enabled,
    int? minutesBefore,
    bool? morningGolden,
    bool? eveningGolden,
    bool? blueHour,
  }) => AlertSettings(
    enabled: enabled ?? this.enabled,
    minutesBefore: minutesBefore ?? this.minutesBefore,
    morningGolden: morningGolden ?? this.morningGolden,
    eveningGolden: eveningGolden ?? this.eveningGolden,
    blueHour: blueHour ?? this.blueHour,
  );

  Map<String, dynamic> toJson() => {
    'enabled': enabled,
    'minutes': minutesBefore,
    'morning': morningGolden,
    'evening': eveningGolden,
    'blue': blueHour,
  };

  factory AlertSettings.fromJson(Map<String, dynamic> j) => AlertSettings(
    enabled: j['enabled'] as bool? ?? true,
    minutesBefore: j['minutes'] as int? ?? 20,
    morningGolden: j['morning'] as bool? ?? false,
    eveningGolden: j['evening'] as bool? ?? true,
    blueHour: j['blue'] as bool? ?? false,
  );
}

/// Single source of truth for the app. Persists to SharedPreferences.
class AppState extends ChangeNotifier {
  AppState._(this._prefs);

  static const freePlaceLimit = 2;
  static const _kPlaces = 'places';
  static const _kSelected = 'selected_place';
  static const _kGps = 'gps_place';
  static const _kPro = 'is_pro';
  static const _kProVerified = 'pro_verified_at';
  static const _kUnlocks = 'reward_unlocks';
  static const _kAlerts = 'alerts';
  static const _kOnboarded = 'onboarded';
  static const _kLaunches = 'launch_count';

  final SharedPreferences _prefs;

  static Future<AppState> load() async {
    final prefs = await SharedPreferences.getInstance();
    final s = AppState._(prefs);
    s._read();
    await prefs.setInt(_kLaunches, s.launchCount + 1);
    s.launchCount += 1;
    return s;
  }

  Place? gpsPlace;
  List<Place> savedPlaces = [];

  /// -1 = GPS place, otherwise index into [savedPlaces].
  int selectedIndex = -1;
  bool isPro = false;
  DateTime? proVerifiedAt;
  Map<RewardFeature, DateTime> _unlocks = {};
  AlertSettings alerts = const AlertSettings();
  bool onboarded = false;
  int launchCount = 0;

  void _read() {
    final gps = _prefs.getString(_kGps);
    gpsPlace = gps == null
        ? null
        : Place.fromJson(jsonDecode(gps) as Map<String, dynamic>);
    savedPlaces = (_prefs.getStringList(_kPlaces) ?? [])
        .map((e) => Place.fromJson(jsonDecode(e) as Map<String, dynamic>))
        .toList();
    selectedIndex = _prefs.getInt(_kSelected) ?? -1;
    if (selectedIndex >= savedPlaces.length) selectedIndex = -1;
    isPro = _prefs.getBool(_kPro) ?? false;
    final v = _prefs.getInt(_kProVerified);
    proVerifiedAt = v == null ? null : DateTime.fromMillisecondsSinceEpoch(v);
    final unlocks = _prefs.getString(_kUnlocks);
    if (unlocks != null) {
      final map = jsonDecode(unlocks) as Map<String, dynamic>;
      _unlocks = {
        for (final e in map.entries)
          if (RewardFeature.values.any((f) => f.name == e.key))
            RewardFeature.values.byName(e.key):
                DateTime.fromMillisecondsSinceEpoch(e.value as int),
      };
    }
    final a = _prefs.getString(_kAlerts);
    if (a != null) {
      alerts = AlertSettings.fromJson(jsonDecode(a) as Map<String, dynamic>);
    }
    onboarded = _prefs.getBool(_kOnboarded) ?? false;
    launchCount = _prefs.getInt(_kLaunches) ?? 0;
  }

  /// The place everything is currently calculated for.
  Place? get place =>
      selectedIndex >= 0 ? savedPlaces[selectedIndex] : gpsPlace;

  int get placeLimit => isPro ? 999 : freePlaceLimit;
  bool get canAddPlace => savedPlaces.length < placeLimit;

  Future<void> setGpsPlace(Place p) async {
    gpsPlace = p;
    await _prefs.setString(_kGps, jsonEncode(p.toJson()));
    notifyListeners();
  }

  Future<void> addPlace(Place p) async {
    savedPlaces = [...savedPlaces, p];
    await _savePlaces();
    await selectPlace(savedPlaces.length - 1);
  }

  Future<void> removePlace(int index) async {
    savedPlaces = [...savedPlaces]..removeAt(index);
    if (selectedIndex == index) {
      selectedIndex = -1;
    } else if (selectedIndex > index) {
      selectedIndex--;
    }
    await _prefs.setInt(_kSelected, selectedIndex);
    await _savePlaces();
    notifyListeners();
  }

  Future<void> selectPlace(int index) async {
    selectedIndex = index;
    await _prefs.setInt(_kSelected, index);
    notifyListeners();
  }

  Future<void> _savePlaces() => _prefs.setStringList(
    _kPlaces,
    savedPlaces.map((p) => jsonEncode(p.toJson())).toList(),
  );

  // --- Pro & rewarded unlocks ----------------------------------------------

  Future<void> setPro(bool value, {bool verified = false}) async {
    isPro = value;
    await _prefs.setBool(_kPro, value);
    if (verified) {
      proVerifiedAt = DateTime.now();
      await _prefs.setInt(_kProVerified, proVerifiedAt!.millisecondsSinceEpoch);
    }
    notifyListeners();
  }

  bool hasAccess(RewardFeature f) {
    if (isPro) return true;
    final until = _unlocks[f];
    return until != null && until.isAfter(DateTime.now());
  }

  Duration? unlockRemaining(RewardFeature f) {
    final until = _unlocks[f];
    if (until == null) return null;
    final d = until.difference(DateTime.now());
    return d.isNegative ? null : d;
  }

  Future<void> grantReward(
    RewardFeature f, {
    Duration duration = const Duration(hours: 24),
  }) async {
    _unlocks[f] = DateTime.now().add(duration);
    await _prefs.setString(
      _kUnlocks,
      jsonEncode({
        for (final e in _unlocks.entries)
          e.key.name: e.value.millisecondsSinceEpoch,
      }),
    );
    notifyListeners();
  }

  // --- Alerts & onboarding -------------------------------------------------

  Future<void> setAlerts(AlertSettings a) async {
    alerts = a;
    await _prefs.setString(_kAlerts, jsonEncode(a.toJson()));
    notifyListeners();
  }

  Future<void> completeOnboarding() async {
    onboarded = true;
    await _prefs.setBool(_kOnboarded, true);
    notifyListeners();
  }
}

/// Makes [AppState] available to the widget tree.
class AppScope extends InheritedNotifier<AppState> {
  const AppScope({super.key, required AppState state, required super.child})
    : super(notifier: state);

  static AppState of(BuildContext context) =>
      context.dependOnInheritedWidgetOfExactType<AppScope>()!.notifier!;

  static AppState read(BuildContext context) =>
      context.getInheritedWidgetOfExactType<AppScope>()!.notifier!;
}
