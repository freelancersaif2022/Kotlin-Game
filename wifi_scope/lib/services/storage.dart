import 'dart:convert';
import 'dart:io';
import 'dart:ui';

import 'package:flutter/foundation.dart';
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

class SurveyPoint {
  SurveyPoint({required this.x, required this.y, required this.rssi, required this.linkSpeed});

  factory SurveyPoint.fromJson(Map<String, dynamic> j) => SurveyPoint(
        x: (j['x'] as num).toDouble(),
        y: (j['y'] as num).toDouble(),
        rssi: j['rssi'] as int,
        linkSpeed: j['link'] as int? ?? 0,
      );

  /// Position normalised to 0–1 across the floor plan.
  final double x;
  final double y;
  final int rssi;
  final int linkSpeed;

  Offset get offset => Offset(x, y);

  Map<String, dynamic> toJson() => {'x': x, 'y': y, 'rssi': rssi, 'link': linkSpeed};
}

class Survey {
  Survey({
    required this.id,
    required this.name,
    required this.createdAt,
    required this.widthMeters,
    required this.heightMeters,
    this.floorPlanPath,
    this.ssid,
    List<SurveyPoint>? points,
    this.router,
    this.insightsUnlocked = false,
  }) : points = points ?? [];

  factory Survey.fromJson(Map<String, dynamic> j) => Survey(
        id: j['id'] as String,
        name: j['name'] as String,
        createdAt: DateTime.parse(j['createdAt'] as String),
        widthMeters: (j['w'] as num).toDouble(),
        heightMeters: (j['h'] as num).toDouble(),
        floorPlanPath: j['plan'] as String?,
        ssid: j['ssid'] as String?,
        points: (j['points'] as List).map((p) => SurveyPoint.fromJson(p as Map<String, dynamic>)).toList(),
        router: j['router'] == null
            ? null
            : Offset((j['router']['x'] as num).toDouble(), (j['router']['y'] as num).toDouble()),
        insightsUnlocked: j['insights'] as bool? ?? false,
      );

  final String id;
  String name;
  final DateTime createdAt;
  final double widthMeters;
  final double heightMeters;
  String? floorPlanPath;
  String? ssid;
  final List<SurveyPoint> points;
  Offset? router;
  bool insightsUnlocked;

  double get aspect => widthMeters / heightMeters;

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'createdAt': createdAt.toIso8601String(),
        'w': widthMeters,
        'h': heightMeters,
        'plan': floorPlanPath,
        'ssid': ssid,
        'points': points.map((p) => p.toJson()).toList(),
        'router': router == null ? null : {'x': router!.dx, 'y': router!.dy},
        'insights': insightsUnlocked,
      };
}

class SpeedResult {
  SpeedResult({
    required this.at,
    required this.download,
    required this.upload,
    required this.ping,
    required this.jitter,
    this.ssid,
    this.rssi,
  });

  factory SpeedResult.fromJson(Map<String, dynamic> j) => SpeedResult(
        at: DateTime.parse(j['at'] as String),
        download: (j['down'] as num).toDouble(),
        upload: (j['up'] as num).toDouble(),
        ping: (j['ping'] as num).toDouble(),
        jitter: (j['jitter'] as num).toDouble(),
        ssid: j['ssid'] as String?,
        rssi: j['rssi'] as int?,
      );

  final DateTime at;
  final double download;
  final double upload;
  final double ping;
  final double jitter;
  final String? ssid;
  final int? rssi;

  Map<String, dynamic> toJson() => {
        'at': at.toIso8601String(),
        'down': download,
        'up': upload,
        'ping': ping,
        'jitter': jitter,
        'ssid': ssid,
        'rssi': rssi,
      };
}

/// Local persistence. Everything stays on the device.
class Storage extends ChangeNotifier {
  Storage._();
  static final instance = Storage._();

  static const _surveysKey = 'surveys_v1';
  static const _speedKey = 'speed_history_v1';
  static const _onboardedKey = 'onboarded_v1';
  static const _channelUnlockKey = 'channel_unlock_until';
  static const maxSpeedHistory = 50;

  late SharedPreferences _prefs;
  final List<Survey> surveys = [];
  final List<SpeedResult> speedHistory = [];

  Future<void> init() async {
    _prefs = await SharedPreferences.getInstance();
    surveys
      ..clear()
      ..addAll(_decodeList(_prefs.getString(_surveysKey), Survey.fromJson));
    speedHistory
      ..clear()
      ..addAll(_decodeList(_prefs.getString(_speedKey), SpeedResult.fromJson));
  }

  static List<T> _decodeList<T>(String? raw, T Function(Map<String, dynamic>) f) {
    if (raw == null) return [];
    try {
      return (jsonDecode(raw) as List).map((e) => f(e as Map<String, dynamic>)).toList();
    } on Object {
      return [];
    }
  }

  bool get onboarded => _prefs.getBool(_onboardedKey) ?? false;
  Future<void> setOnboarded() => _prefs.setBool(_onboardedKey, true);

  bool get channelAdvisorUnlocked {
    final until = _prefs.getInt(_channelUnlockKey) ?? 0;
    return DateTime.now().millisecondsSinceEpoch < until;
  }

  Future<void> unlockChannelAdvisor() async {
    await _prefs.setInt(
      _channelUnlockKey,
      DateTime.now().add(const Duration(hours: 24)).millisecondsSinceEpoch,
    );
    notifyListeners();
  }

  Future<void> saveSurvey(Survey s) async {
    final i = surveys.indexWhere((e) => e.id == s.id);
    if (i >= 0) {
      surveys[i] = s;
    } else {
      surveys.insert(0, s);
    }
    await _prefs.setString(_surveysKey, jsonEncode(surveys.map((e) => e.toJson()).toList()));
    notifyListeners();
  }

  Future<void> deleteSurvey(Survey s) async {
    surveys.removeWhere((e) => e.id == s.id);
    final plan = s.floorPlanPath;
    if (plan != null) {
      try {
        await File(plan).delete();
      } on Object {
        // Already gone.
      }
    }
    await _prefs.setString(_surveysKey, jsonEncode(surveys.map((e) => e.toJson()).toList()));
    notifyListeners();
  }

  Future<void> addSpeedResult(SpeedResult r) async {
    speedHistory.insert(0, r);
    if (speedHistory.length > maxSpeedHistory) speedHistory.removeLast();
    await _prefs.setString(_speedKey, jsonEncode(speedHistory.map((e) => e.toJson()).toList()));
    notifyListeners();
  }

  Future<void> clearSpeedHistory() async {
    speedHistory.clear();
    await _prefs.remove(_speedKey);
    notifyListeners();
  }

  /// Copies a picked floor plan image into app storage so it survives the
  /// picker's temp files being cleaned up.
  Future<String> importFloorPlan(String sourcePath, String surveyId) async {
    final dir = await getApplicationDocumentsDirectory();
    final ext = sourcePath.contains('.') ? sourcePath.substring(sourcePath.lastIndexOf('.')) : '.jpg';
    final dest = File('${dir.path}/plan_$surveyId$ext');
    await File(sourcePath).copy(dest.path);
    return dest.path;
  }
}
