import 'dart:math' as math;
import 'dart:ui';

import 'theme.dart';

/// Signal-strength helpers shared by every screen.
abstract final class Signal {
  /// Maps RSSI (dBm) to a 0–100 quality score. −50 dBm or better is 100%,
  /// −100 dBm is 0%: the same linear scale Android uses for its bars.
  static int quality(int rssi) => (2 * (rssi + 100)).clamp(0, 100);

  static String label(int rssi) {
    if (rssi >= -55) return 'Excellent';
    if (rssi >= -65) return 'Good';
    if (rssi >= -72) return 'Fair';
    if (rssi >= -80) return 'Weak';
    return 'Very weak';
  }

  static Color color(int rssi) {
    if (rssi >= -55) return AppColors.excellent;
    if (rssi >= -65) return AppColors.good;
    if (rssi >= -72) return AppColors.fair;
    if (rssi >= -80) return AppColors.weak;
    return AppColors.bad;
  }

  /// Continuous colour ramp for the heatmap, from −40 dBm (green) to
  /// −90 dBm (red).
  static Color ramp(double rssi) {
    const stops = [-90.0, -80.0, -72.0, -65.0, -55.0, -40.0];
    const colors = [
      AppColors.bad,
      AppColors.weak,
      AppColors.fair,
      AppColors.good,
      AppColors.excellent,
      AppColors.excellent,
    ];
    if (rssi <= stops.first) return colors.first;
    for (var i = 1; i < stops.length; i++) {
      if (rssi <= stops[i]) {
        final t = (rssi - stops[i - 1]) / (stops[i] - stops[i - 1]);
        return Color.lerp(colors[i - 1], colors[i], t)!;
      }
    }
    return colors.last;
  }

  /// What the user can realistically do at this signal level.
  static String usage(int rssi) {
    if (rssi >= -55) return '4K streaming, gaming and video calls run smoothly';
    if (rssi >= -65) return 'HD streaming and video calls work well';
    if (rssi >= -72) return 'Browsing and SD video are fine; calls may stutter';
    if (rssi >= -80) return 'Basic browsing only; expect slow loads';
    return 'Unreliable: move closer to the router';
  }

  /// Rough distance to the access point using the indoor log-distance path
  /// loss model: RSSI = P0 − 10·n·log10(d). Walls and bodies change the real
  /// value, so the UI always labels this as an estimate.
  static double estimateDistanceMeters(int rssi, int frequencyMhz) {
    final is24 = frequencyMhz < 3000;
    final p0 = is24 ? -38.0 : -45.0; // typical RSSI at 1 m
    const n = 3.0; // indoor residential path-loss exponent
    final d = math.pow(10, (p0 - rssi) / (10 * n)).toDouble();
    return d.clamp(0.5, 80.0);
  }

  static String band(int frequencyMhz) {
    if (frequencyMhz >= 5925) return '6 GHz';
    if (frequencyMhz >= 4900) return '5 GHz';
    return '2.4 GHz';
  }

  static int channel(int frequencyMhz) {
    if (frequencyMhz == 2484) return 14;
    if (frequencyMhz >= 2412 && frequencyMhz < 2484) return (frequencyMhz - 2407) ~/ 5;
    if (frequencyMhz >= 5925) return (frequencyMhz - 5950) ~/ 5;
    if (frequencyMhz >= 4900) return (frequencyMhz - 5000) ~/ 5;
    return 0;
  }

  static String standardName(int? code) => switch (code) {
        1 => '802.11a/b/g',
        4 => 'Wi-Fi 4 (802.11n)',
        5 => 'Wi-Fi 5 (802.11ac)',
        6 => 'Wi-Fi 6 (802.11ax)',
        7 => 'WiGig (802.11ad)',
        8 => 'Wi-Fi 7 (802.11be)',
        _ => 'Unknown',
      };
}
