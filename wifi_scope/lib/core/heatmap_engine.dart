import 'dart:async';
import 'dart:math' as math;
import 'dart:ui' as ui;

import 'package:flutter/foundation.dart';

import '../services/storage.dart';
import 'signal.dart';

class _HeatInput {
  _HeatInput(this.xs, this.ys, this.values, this.width, this.height, this.aspect);
  final Float64List xs;
  final Float64List ys;
  final Float64List values;
  final int width;
  final int height;
  final double aspect;
}

/// Renders survey points into a smooth heatmap image.
///
/// Uses inverse-distance weighting (the standard method in professional
/// site-survey tools) on a low-resolution grid, computed off the UI thread,
/// then scaled up with bilinear filtering. Areas far from any measurement
/// fade out instead of being guessed, so the map never shows coverage that
/// was not actually measured.
abstract final class HeatmapEngine {
  static const _maxResolution = 120;

  static Future<ui.Image?> render(List<SurveyPoint> points, double aspect) async {
    if (points.isEmpty) return null;
    final w = aspect >= 1 ? _maxResolution : (_maxResolution * aspect).round().clamp(8, _maxResolution);
    final h = aspect >= 1 ? (_maxResolution / aspect).round().clamp(8, _maxResolution) : _maxResolution;
    final input = _HeatInput(
      Float64List.fromList(points.map((p) => p.x).toList()),
      Float64List.fromList(points.map((p) => p.y).toList()),
      Float64List.fromList(points.map((p) => p.rssi.toDouble()).toList()),
      w,
      h,
      aspect,
    );
    final pixels = await compute(_compute, input);
    final completer = Completer<ui.Image>();
    ui.decodeImageFromPixels(pixels, w, h, ui.PixelFormat.rgba8888, completer.complete);
    return completer.future;
  }

  static Uint8List _compute(_HeatInput d) {
    final out = Uint8List(d.width * d.height * 4);
    final n = d.values.length;
    // Distance is measured in "plan units" so a square room is not skewed.
    final ax = d.aspect >= 1 ? 1.0 : d.aspect;
    final ay = d.aspect >= 1 ? 1 / d.aspect : 1.0;
    // A measurement is trusted up to ~30% of the room diagonal away.
    final reach = 0.30 * math.sqrt(ax * ax + ay * ay);
    for (var py = 0; py < d.height; py++) {
      final y = (py + 0.5) / d.height;
      for (var px = 0; px < d.width; px++) {
        final x = (px + 0.5) / d.width;
        var wSum = 0.0;
        var vSum = 0.0;
        var nearest = double.infinity;
        for (var i = 0; i < n; i++) {
          final dx = (x - d.xs[i]) * ax;
          final dy = (y - d.ys[i]) * ay;
          final dist2 = dx * dx + dy * dy;
          final dist = math.sqrt(dist2);
          if (dist < nearest) nearest = dist;
          final w = 1 / (dist2 + 1e-4);
          wSum += w;
          vSum += w * d.values[i];
        }
        final value = vSum / wSum;
        final c = Signal.ramp(value);
        final fade = (1 - (nearest / reach)).clamp(0.0, 1.0);
        final alpha = (0.78 * math.pow(fade, 0.6)).clamp(0.0, 1.0);
        final o = (py * d.width + px) * 4;
        // Premultiplied alpha, as the engine expects.
        out[o] = (c.r * alpha * 255).round();
        out[o + 1] = (c.g * alpha * 255).round();
        out[o + 2] = (c.b * alpha * 255).round();
        out[o + 3] = (alpha * 255).round();
      }
    }
    return out;
  }
}


/// Coverage analysis behind the "Placement insights" report.
class SurveyInsights {
  SurveyInsights._({
    required this.coverage,
    required this.best,
    required this.worst,
    required this.average,
    required this.deadZones,
    this.suggestedRouter,
    this.suggestedExtender,
  });

  /// Share of measured spots in each class: excellent/good, fair, weak.
  final ({double strong, double fair, double weak}) coverage;
  final SurveyPoint best;
  final SurveyPoint worst;
  final double average;
  final List<SurveyPoint> deadZones;
  final ui.Offset? suggestedRouter;
  final ui.Offset? suggestedExtender;

  int get score {
    final s = coverage.strong * 100 + coverage.fair * 55;
    return s.round().clamp(0, 100);
  }

  static SurveyInsights? analyze(Survey survey) {
    final pts = survey.points;
    if (pts.length < 3) return null;
    final sorted = [...pts]..sort((a, b) => b.rssi.compareTo(a.rssi));
    final strong = pts.where((p) => p.rssi >= -65).length / pts.length;
    final fair = pts.where((p) => p.rssi < -65 && p.rssi >= -72).length / pts.length;
    final weak = 1 - strong - fair;
    final avg = pts.map((p) => p.rssi).reduce((a, b) => a + b) / pts.length;
    final dead = pts.where((p) => p.rssi < -75).toList();

    // Suggested router spot: the signal-weighted centre of the space, pulled
    // toward weak areas. Placing the router there evens out coverage.
    var wx = 0.0, wy = 0.0, wt = 0.0;
    for (final p in pts) {
      final need = (-p.rssi - 40).clamp(5, 60).toDouble();
      wx += p.x * need;
      wy += p.y * need;
      wt += need;
    }
    final centre = ui.Offset(wx / wt, wy / wt);
    final router = survey.router;
    ui.Offset? suggestedRouter;
    if (router == null) {
      suggestedRouter = centre;
    } else if ((router - centre).distance > 0.12) {
      // Move part-way: routers are often tied to a cable outlet.
      suggestedRouter = ui.Offset.lerp(router, centre, 0.6);
    }

    // Extender: halfway between the router and the weakest cluster, which is
    // where it still receives a solid signal to repeat.
    ui.Offset? extender;
    if (dead.isNotEmpty) {
      final dx = dead.map((p) => p.x).reduce((a, b) => a + b) / dead.length;
      final dy = dead.map((p) => p.y).reduce((a, b) => a + b) / dead.length;
      final origin = router ?? sorted.first.offset;
      extender = ui.Offset.lerp(origin, ui.Offset(dx, dy), 0.5);
    }

    return SurveyInsights._(
      coverage: (strong: strong, fair: fair, weak: weak.clamp(0.0, 1.0)),
      best: sorted.first,
      worst: sorted.last,
      average: avg,
      deadZones: dead,
      suggestedRouter: suggestedRouter,
      suggestedExtender: extender,
    );
  }
}
