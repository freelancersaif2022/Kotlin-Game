import 'dart:async';
import 'dart:io';
import 'dart:math' as math;

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:sensors_plus/sensors_plus.dart';

/// 3D vector helper.
class V3 {
  const V3(this.x, this.y, this.z);
  final double x, y, z;
  V3 operator +(V3 o) => V3(x + o.x, y + o.y, z + o.z);
  V3 operator -(V3 o) => V3(x - o.x, y - o.y, z - o.z);
  V3 operator *(double s) => V3(x * s, y * s, z * s);
  double dot(V3 o) => x * o.x + y * o.y + z * o.z;
  V3 cross(V3 o) => V3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
  double get length => math.sqrt(dot(this));
  V3 get normalized {
    final l = length;
    return l == 0 ? this : this * (1 / l);
  }
}

/// World axes expressed in device coordinates
/// (device: x right, y top of screen, z out of the screen).
class DeviceOrientation3D {
  const DeviceOrientation3D(this.east, this.north, this.up);
  final V3 east, north, up;

  /// World direction (true bearing / altitude, degrees) -> device vector.
  V3 toDevice(double bearingDeg, double altDeg) {
    final b = bearingDeg * math.pi / 180, a = altDeg * math.pi / 180;
    final e = math.cos(a) * math.sin(b),
        n = math.cos(a) * math.cos(b),
        u = math.sin(a);
    return east * e + north * n + up * u;
  }

  /// Bearing the back camera points to.
  double get cameraBearing => _bearingOf(-east.z, -north.z);

  /// Camera elevation above the horizon (degrees).
  double get cameraPitch => math.asin((-up.z).clamp(-1.0, 1.0)) * 180 / math.pi;

  /// Heading for a compass: top edge when flat, camera when upright.
  double get compassHeading =>
      up.z.abs() > 0.55 ? _bearingOf(east.y, north.y) : cameraBearing;

  bool get isFlat => up.z.abs() > 0.55;

  static double _bearingOf(double e, double n) =>
      (math.atan2(e, n) * 180 / math.pi + 360) % 360;
}

/// Fuses accelerometer + magnetometer into a stable orientation, corrected
/// to TRUE north with the local magnetic declination.
class OrientationTracker extends ChangeNotifier {
  OrientationTracker({required this.lat, required this.lng});

  final double lat, lng;
  static const _channel = MethodChannel('golden_hour/geomag');

  StreamSubscription<AccelerometerEvent>? _acc;
  StreamSubscription<MagnetometerEvent>? _mag;
  V3? _g, _m;
  double declination = 0;
  DeviceOrientation3D? orientation;
  bool available = true;

  /// Rough magnetic interference indicator (µT far from ~25–65).
  bool needsCalibration = false;

  Future<void> start() async {
    if (kIsWeb) return;
    try {
      if (Platform.isAndroid) {
        declination =
            await _channel.invokeMethod<double>('declination', {
              'lat': lat,
              'lng': lng,
              'time': DateTime.now().millisecondsSinceEpoch,
            }) ??
            0;
      }
    } catch (_) {
      declination = 0;
    }
    const period = Duration(milliseconds: 20);
    _acc = accelerometerEventStream(samplingPeriod: period).listen((e) {
      _g = _lowPass(_g, V3(e.x, e.y, e.z), 0.12);
      _update();
    }, onError: (_) => available = false);
    _mag = magnetometerEventStream(samplingPeriod: period).listen((e) {
      final v = V3(e.x, e.y, e.z);
      final strength = v.length;
      needsCalibration = strength < 15 || strength > 85;
      _m = _lowPass(_m, v, 0.1);
      _update();
    }, onError: (_) => available = false);
  }

  static V3 _lowPass(V3? prev, V3 next, double a) =>
      prev == null ? next : prev + (next - prev) * a;

  void _update() {
    final g = _g, m = _m;
    if (g == null || m == null) return;
    final up = g.normalized;
    final eastMag = m.cross(up).normalized;
    if (eastMag.length < 0.5) return;
    final northMag = up.cross(eastMag).normalized;
    // Rotate magnetic axes around "up" by the declination -> true north.
    final d = declination * math.pi / 180;
    final cd = math.cos(d), sd = math.sin(d);
    final north = northMag * cd + eastMag * (-sd);
    final east = eastMag * cd + northMag * sd;
    orientation = DeviceOrientation3D(east, north, up);
    notifyListeners();
  }

  @override
  void dispose() {
    _acc?.cancel();
    _mag?.cancel();
    super.dispose();
  }
}
