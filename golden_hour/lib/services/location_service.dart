import 'package:geolocator/geolocator.dart';

import 'app_state.dart';

enum LocationResult { ok, serviceOff, denied, deniedForever, error }

/// GPS works without internet — no geocoding, coordinates only.
class LocationService {
  static Future<(LocationResult, Place?)> current() async {
    try {
      if (!await Geolocator.isLocationServiceEnabled()) {
        final last = await Geolocator.getLastKnownPosition();
        return (
          LocationResult.serviceOff,
          last == null ? null : _toPlace(last),
        );
      }
      var perm = await Geolocator.checkPermission();
      if (perm == LocationPermission.denied) {
        perm = await Geolocator.requestPermission();
      }
      if (perm == LocationPermission.denied) {
        return (LocationResult.denied, null);
      }
      if (perm == LocationPermission.deniedForever) {
        return (LocationResult.deniedForever, null);
      }

      final last = await Geolocator.getLastKnownPosition();
      try {
        final pos = await Geolocator.getCurrentPosition(
          locationSettings: const LocationSettings(
            accuracy: LocationAccuracy.low,
            timeLimit: Duration(seconds: 12),
          ),
        );
        return (LocationResult.ok, _toPlace(pos));
      } catch (_) {
        if (last != null) return (LocationResult.ok, _toPlace(last));
        rethrow;
      }
    } catch (_) {
      return (LocationResult.error, null);
    }
  }

  static Place _toPlace(Position p) => Place(
    name: 'Current location',
    lat: p.latitude,
    lng: p.longitude,
    isGps: true,
  );

  static Future<void> openSettings() => Geolocator.openAppSettings();
}
