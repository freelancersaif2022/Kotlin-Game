import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:wifi_scan/wifi_scan.dart';

import '../core/signal.dart';

/// Snapshot of the current Wi-Fi connection.
@immutable
class WifiConnection {
  const WifiConnection({
    required this.wifiEnabled,
    required this.connected,
    this.ssid,
    this.bssid,
    this.rssi = -127,
    this.linkSpeed = 0,
    this.txLinkSpeed,
    this.rxLinkSpeed,
    this.maxTxSpeed,
    this.frequency = 0,
    this.ip,
    this.standard,
  });

  factory WifiConnection.fromMap(Map<Object?, Object?> m) => WifiConnection(
        wifiEnabled: m['wifiEnabled'] as bool? ?? false,
        connected: m['connected'] as bool? ?? false,
        ssid: m['ssid'] as String?,
        bssid: m['bssid'] as String?,
        rssi: m['rssi'] as int? ?? -127,
        linkSpeed: m['linkSpeed'] as int? ?? 0,
        txLinkSpeed: m['txLinkSpeed'] as int?,
        rxLinkSpeed: m['rxLinkSpeed'] as int?,
        maxTxSpeed: m['maxTxSpeed'] as int?,
        frequency: m['frequency'] as int? ?? 0,
        ip: m['ip'] as String?,
        standard: m['standard'] as int?,
      );

  static const disconnected = WifiConnection(wifiEnabled: false, connected: false);

  final bool wifiEnabled;
  final bool connected;
  final String? ssid;
  final String? bssid;
  final int rssi;
  final int linkSpeed;
  final int? txLinkSpeed;
  final int? rxLinkSpeed;
  final int? maxTxSpeed;
  final int frequency;
  final String? ip;
  final int? standard;

  int get quality => Signal.quality(rssi);
  String get band => Signal.band(frequency);
  int get channel => Signal.channel(frequency);
  double get distance => Signal.estimateDistanceMeters(rssi, frequency);
}

enum WifiAccess { ready, permissionDenied, permissionPermanentlyDenied, locationOff, unsupported }

/// Reads live Wi-Fi data from the Android side and keeps a short RSSI
/// history for the dashboard chart. Polls only while someone is listening.
class WifiService extends ChangeNotifier {
  WifiService._();
  static final instance = WifiService._();

  static const _channel = MethodChannel('wifiscope/wifi');
  static const historyLength = 60;

  WifiConnection _connection = WifiConnection.disconnected;
  WifiConnection get connection => _connection;

  final List<int> _history = [];
  List<int> get history => List.unmodifiable(_history);

  WifiAccess _access = WifiAccess.ready;
  WifiAccess get access => _access;

  Timer? _timer;
  int _pollers = 0;

  bool get isSupported => Platform.isAndroid;

  Future<WifiAccess> checkAccess({bool request = false}) async {
    if (!isSupported) return _setAccess(WifiAccess.unsupported);
    var status = await Permission.locationWhenInUse.status;
    if (!status.isGranted && request) {
      status = await Permission.locationWhenInUse.request();
    }
    if (status.isPermanentlyDenied) return _setAccess(WifiAccess.permissionPermanentlyDenied);
    if (!status.isGranted) return _setAccess(WifiAccess.permissionDenied);
    final locationOn = await _channel.invokeMethod<bool>('isLocationEnabled') ?? true;
    if (!locationOn) return _setAccess(WifiAccess.locationOff);
    return _setAccess(WifiAccess.ready);
  }

  WifiAccess _setAccess(WifiAccess a) {
    if (a != _access) {
      _access = a;
      notifyListeners();
    }
    return a;
  }

  Future<WifiConnection> read() async {
    if (!isSupported) return WifiConnection.disconnected;
    try {
      final map = await _channel.invokeMapMethod<Object?, Object?>('getConnection');
      return map == null ? WifiConnection.disconnected : WifiConnection.fromMap(map);
    } on PlatformException {
      return WifiConnection.disconnected;
    }
  }

  /// Averages several readings over [duration] so one noisy sample does not
  /// skew a heatmap point.
  Future<WifiConnection> sampleAveraged({
    Duration duration = const Duration(milliseconds: 2400),
    int samples = 6,
    void Function(double progress)? onProgress,
  }) async {
    final step = duration ~/ samples;
    final readings = <WifiConnection>[];
    for (var i = 0; i < samples; i++) {
      final r = await read();
      if (r.connected) readings.add(r);
      onProgress?.call((i + 1) / samples);
      if (i < samples - 1) await Future<void>.delayed(step);
    }
    if (readings.isEmpty) return WifiConnection.disconnected;
    final sorted = readings.map((r) => r.rssi).toList()..sort();
    // Trimmed mean: drop the single highest and lowest when we have enough.
    final trimmed = sorted.length > 3 ? sorted.sublist(1, sorted.length - 1) : sorted;
    final avg = (trimmed.reduce((a, b) => a + b) / trimmed.length).round();
    final speed = (readings.map((r) => r.linkSpeed).reduce((a, b) => a + b) / readings.length).round();
    final last = readings.last;
    return WifiConnection(
      wifiEnabled: true,
      connected: true,
      ssid: last.ssid,
      bssid: last.bssid,
      rssi: avg,
      linkSpeed: speed,
      frequency: last.frequency,
      standard: last.standard,
    );
  }

  void startPolling() {
    _pollers++;
    if (_timer != null) return;
    _tick();
    _timer = Timer.periodic(const Duration(seconds: 1), (_) => _tick());
  }

  void stopPolling() {
    _pollers = (_pollers - 1).clamp(0, 1 << 20);
    if (_pollers == 0) {
      _timer?.cancel();
      _timer = null;
    }
  }

  Future<void> _tick() async {
    final c = await read();
    _connection = c;
    if (c.connected) {
      _history.add(c.rssi);
      if (_history.length > historyLength) _history.removeAt(0);
    }
    notifyListeners();
  }

  @visibleForTesting
  void debugSet({required WifiConnection connection, List<int>? history, WifiAccess access = WifiAccess.ready}) {
    _connection = connection;
    _access = access;
    if (history != null) {
      _history
        ..clear()
        ..addAll(history);
    }
    notifyListeners();
  }

  Future<void> openWifiSettings() async {
    if (isSupported) await _channel.invokeMethod('openWifiSettings');
  }

  Future<void> openLocationSettings() async {
    if (isSupported) await _channel.invokeMethod('openLocationSettings');
  }

  /// Triggers a scan and returns the freshest results available. Android
  /// throttles foreground apps to 4 scans per 2 minutes, so when a scan is
  /// refused we still return the last cached results and flag [throttled].
  Future<({List<WiFiAccessPoint> results, bool throttled})> scan() async {
    if (!isSupported) return (results: <WiFiAccessPoint>[], throttled: false);
    var throttled = false;
    final can = await WiFiScan.instance.canStartScan(askPermissions: false);
    if (can == CanStartScan.yes) {
      final started = await WiFiScan.instance.startScan();
      if (started) {
        try {
          await WiFiScan.instance.onScannedResultsAvailable.first.timeout(const Duration(seconds: 6));
        } on TimeoutException {
          // Fall through to the cached results.
        }
      } else {
        throttled = true;
      }
    } else {
      throttled = true;
    }
    final canGet = await WiFiScan.instance.canGetScannedResults(askPermissions: false);
    if (canGet != CanGetScannedResults.yes) return (results: <WiFiAccessPoint>[], throttled: throttled);
    final results = await WiFiScan.instance.getScannedResults();
    results.sort((a, b) => b.level.compareTo(a.level));
    return (results: results, throttled: throttled);
  }
}
