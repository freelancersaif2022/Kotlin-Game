import 'dart:math' as math;
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';

import '../../astro/extras.dart' show angleDiff;
import 'atmosphere.dart';
import 'shaders.dart';

/// Moon parameters for the sky scene.
class SkyMoon {
  const SkyMoon({
    required this.bearing,
    required this.altitude,
    required this.phase,
    this.tilt = 0,
  });
  final double bearing;
  final double altitude;
  final double phase;
  final double tilt;
}

/// Ultra-realistic animated sky: physically based atmosphere, drifting
/// sun-lit clouds, twinkling stars, the real moon, layered mountains with
/// aerial perspective and a subtle lens flare.
class SkyView extends StatefulWidget {
  const SkyView({
    super.key,
    required this.sunBearing,
    required this.sunAltitude,
    required this.viewBearing,
    this.moon,
    this.horizon = 0.62,
    this.hFov = 110,
    this.cloudCover = 0.45,
    this.animate = true,
    this.time,
  });

  final double sunBearing;
  final double sunAltitude;
  final double viewBearing;
  final SkyMoon? moon;

  /// Horizon position from the top (0..1).
  final double horizon;
  final double hFov;
  final double cloudCover;
  final bool animate;

  /// Fixed animation time (for screenshots / tests).
  final double? time;

  @override
  State<SkyView> createState() => _SkyViewState();
}

class _SkyViewState extends State<SkyView> with SingleTickerProviderStateMixin {
  late final Ticker _ticker;
  final _time = ValueNotifier<double>(0);
  final _cache = _SkyCache();

  @override
  void initState() {
    super.initState();
    _time.value = widget.time ?? 0;
    _ticker = createTicker(
      (elapsed) => _time.value = elapsed.inMicroseconds / 1e6 + 40,
    );
    if (widget.animate && widget.time == null) _ticker.start();
  }

  @override
  void dispose() {
    _ticker.dispose();
    _time.dispose();
    _cache.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final palette = _cache.palette(widget.sunAltitude);
    final geo = _SkyGeometry(widget);
    return Stack(
      fit: StackFit.expand,
      children: [
        RepaintBoundary(
          child: CustomPaint(painter: _SkyPainter(widget, geo, _cache)),
        ),
        RepaintBoundary(
          child: CustomPaint(painter: _StarsPainter(geo, palette, _time)),
        ),
        if (widget.moon != null && widget.moon!.altitude > -1)
          RepaintBoundary(
            child: CustomPaint(
              painter: _MoonPainter(geo, widget.moon!, palette),
            ),
          ),
        RepaintBoundary(
          child: CustomPaint(
            painter: _CloudPainter(geo, palette, _time, widget.cloudCover),
          ),
        ),
        RepaintBoundary(
          child: CustomPaint(
            painter: _FlarePainter(geo, palette, widget.sunAltitude),
          ),
        ),
        RepaintBoundary(
          child: CustomPaint(painter: _MountainPainter(geo, palette)),
        ),
      ],
    );
  }
}

/// Shared projection math for every layer.
class _SkyGeometry {
  _SkyGeometry(SkyView w)
    : viewBearing = w.viewBearing,
      hFov = w.hFov,
      horizon = w.horizon,
      sunBearing = w.sunBearing,
      sunAltitude = w.sunAltitude,
      vFov = 90 / w.horizon;

  final double viewBearing, hFov, horizon, vFov, sunBearing, sunAltitude;

  /// Screen position (0..1) of a direction.
  Offset project(double bearing, double altitude) {
    var d = (bearing - viewBearing) % 360;
    if (d > 180) d -= 360;
    return Offset(0.5 + d / hFov, horizon - altitude / vFov);
  }

  Offset get sunUv => project(sunBearing, sunAltitude);
}

// ---------------------------------------------------------------------------
// Atmosphere (expensive shader rendered at reduced resolution and cached)
// ---------------------------------------------------------------------------

class _SkyCache {
  ui.Image? image;
  String? _key;
  SkyPalette? _palette;
  String? _paletteKey;

  SkyPalette palette(double alt) {
    final key = alt.toStringAsFixed(2);
    if (key != _paletteKey) {
      _palette = SkyPalette.compute(alt);
      _paletteKey = key;
    }
    return _palette!;
  }

  ui.Image? render(Size size, SkyView w, _SkyGeometry geo) {
    final program = Shaders.sky;
    if (program == null || size.isEmpty) return null;
    const scale = 0.5;
    final sw = math.max(8, (size.width * scale).round());
    final sh = math.max(8, (size.height * scale).round());
    final key =
        '$sw x $sh|${w.sunBearing.toStringAsFixed(2)}|${w.sunAltitude.toStringAsFixed(3)}'
        '|${w.viewBearing.toStringAsFixed(2)}|${w.horizon}|${w.hFov}';
    if (key == _key && image != null) return image;

    final p = palette(w.sunAltitude);
    final shader = program.fragmentShader()
      ..setFloat(0, sw.toDouble())
      ..setFloat(1, sh.toDouble())
      ..setFloat(2, geo.viewBearing)
      ..setFloat(3, geo.hFov)
      ..setFloat(4, geo.vFov)
      ..setFloat(5, geo.horizon)
      ..setFloat(6, w.sunBearing)
      ..setFloat(7, w.sunAltitude);
    var i = 8;
    for (final c in [p.zenith, p.horizonSun, p.horizonAway, p.glow]) {
      shader
        ..setFloat(i++, c.r)
        ..setFloat(i++, c.g)
        ..setFloat(i++, c.b);
    }
    final rec = ui.PictureRecorder();
    Canvas(rec).drawRect(
      Rect.fromLTWH(0, 0, sw.toDouble(), sh.toDouble()),
      Paint()..shader = shader,
    );
    final pic = rec.endRecording();
    image?.dispose();
    image = pic.toImageSync(sw, sh);
    pic.dispose();
    shader.dispose();
    _key = key;
    return image;
  }

  void dispose() => image?.dispose();
}

class _SkyPainter extends CustomPainter {
  _SkyPainter(this.w, this.geo, this.cache);
  final SkyView w;
  final _SkyGeometry geo;
  final _SkyCache cache;

  @override
  void paint(Canvas canvas, Size size) {
    final img = cache.render(size, w, geo);
    final rect = Offset.zero & size;
    if (img != null) {
      canvas.drawImageRect(
        img,
        Rect.fromLTWH(0, 0, img.width.toDouble(), img.height.toDouble()),
        rect,
        Paint()..filterQuality = FilterQuality.medium,
      );
      return;
    }
    // Fallback: gradient from the CPU atmosphere model.
    final p = cache.palette(w.sunAltitude);
    canvas.drawRect(
      rect,
      Paint()
        ..shader = LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [p.zenith, p.horizonSun, p.horizonSun],
          stops: [0, geo.horizon, 1],
        ).createShader(rect),
    );
  }

  @override
  bool shouldRepaint(_SkyPainter old) =>
      old.w.sunBearing != w.sunBearing ||
      old.w.sunAltitude != w.sunAltitude ||
      old.w.viewBearing != w.viewBearing ||
      old.w.horizon != w.horizon;
}

// ---------------------------------------------------------------------------
// Stars
// ---------------------------------------------------------------------------

class _StarsPainter extends CustomPainter {
  _StarsPainter(this.geo, this.palette, this.time)
    : super(repaint: palette.night > 0.05 ? time : null);
  final _SkyGeometry geo;
  final SkyPalette palette;
  final ValueNotifier<double> time;

  static final List<(Offset, double, double, Color)> _stars = () {
    final r = math.Random(7);
    const tints = [
      Color(0xFFFFFFFF),
      Color(0xFFCFDcFF),
      Color(0xFFFFF1D6),
      Color(0xFFDDE8FF),
    ];
    return List.generate(320, (_) {
      final mag = math.pow(r.nextDouble(), 3.2).toDouble();
      return (
        Offset(r.nextDouble(), r.nextDouble()),
        0.35 + mag * 1.6,
        r.nextDouble() * math.pi * 2,
        tints[r.nextInt(tints.length)],
      );
    });
  }();

  @override
  void paint(Canvas canvas, Size size) {
    final vis = ((palette.night - 0.15) / 0.85).clamp(0.0, 1.0);
    if (vis <= 0) return;
    final t = time.value;
    final horizonY = geo.horizon * size.height;
    final paint = Paint();
    final glow = Paint()
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 2.2);

    // Faint Milky Way band.
    final band = Path()
      ..moveTo(size.width * -0.1, horizonY * 0.95)
      ..quadraticBezierTo(
        size.width * 0.45,
        horizonY * 0.15,
        size.width * 1.1,
        horizonY * 0.05,
      )
      ..lineTo(size.width * 1.1, horizonY * 0.28)
      ..quadraticBezierTo(
        size.width * 0.5,
        horizonY * 0.42,
        size.width * -0.1,
        horizonY * 1.0,
      )
      ..close();
    canvas.drawPath(
      band,
      Paint()
        ..color = const Color(0xFFB8C4FF).withValues(alpha: 0.07 * vis)
        ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 28),
    );

    for (final (pos, r, phase, tint) in _stars) {
      final p = Offset(pos.dx * size.width, pos.dy * horizonY);
      // Stars dim near the horizon (extinction).
      final ext = (p.dy / horizonY);
      final twinkle = 0.75 + 0.25 * math.sin(t * (1.3 + r) + phase);
      final a = (vis * twinkle * (1 - ext * ext * 0.85)).clamp(0.0, 1.0);
      paint.color = tint.withValues(alpha: a);
      canvas.drawCircle(p, r, paint);
      if (r > 1.4) {
        glow.color = tint.withValues(alpha: a * 0.35);
        canvas.drawCircle(p, r * 2.4, glow);
      }
    }
  }

  @override
  bool shouldRepaint(_StarsPainter old) => old.palette != palette;
}

// ---------------------------------------------------------------------------
// Moon
// ---------------------------------------------------------------------------

class _MoonPainter extends CustomPainter {
  _MoonPainter(this.geo, this.moon, this.palette);
  final _SkyGeometry geo;
  final SkyMoon moon;
  final SkyPalette palette;

  @override
  void paint(Canvas canvas, Size size) {
    final uv = geo.project(moon.bearing, moon.altitude);
    if (uv.dx < -0.1 || uv.dx > 1.1) return;
    final c = Offset(uv.dx * size.width, uv.dy * size.height);
    final radius = size.width * 0.038;
    final dayFade = 0.35 + 0.65 * palette.night;

    canvas.saveLayer(
      null,
      Paint()..color = Colors.white.withValues(alpha: dayFade),
    );
    // Halo
    canvas.drawCircle(
      c,
      radius * 4,
      Paint()
        ..shader = RadialGradient(
          colors: [
            const Color(0xFFDDE6FF).withValues(alpha: 0.22 * palette.night),
            Colors.transparent,
          ],
        ).createShader(Rect.fromCircle(center: c, radius: radius * 4)),
    );
    final program = Shaders.moon;
    final rect = Rect.fromCircle(center: c, radius: radius * 1.35);
    if (program != null) {
      final s = program.fragmentShader()
        ..setFloat(0, rect.width)
        ..setFloat(1, rect.height)
        ..setFloat(2, moon.phase)
        ..setFloat(3, moon.tilt)
        ..setFloat(4, 0.12);
      canvas.save();
      canvas.translate(rect.left, rect.top);
      canvas.drawRect(Offset.zero & rect.size, Paint()..shader = s);
      canvas.restore();
      s.dispose();
    } else {
      canvas.drawCircle(c, radius, Paint()..color = const Color(0xFFEDEFF5));
    }
    canvas.restore();
  }

  @override
  bool shouldRepaint(_MoonPainter old) =>
      old.moon.bearing != moon.bearing ||
      old.moon.altitude != moon.altitude ||
      old.palette != palette;
}

// ---------------------------------------------------------------------------
// Clouds
// ---------------------------------------------------------------------------

class _CloudPainter extends CustomPainter {
  _CloudPainter(this.geo, this.palette, this.time, this.cover)
    : super(repaint: time);
  final _SkyGeometry geo;
  final SkyPalette palette;
  final ValueNotifier<double> time;
  final double cover;

  @override
  void paint(Canvas canvas, Size size) {
    final program = Shaders.clouds;
    if (program == null || cover <= 0) return;
    final sun = geo.sunUv;
    final sl = palette.sunLight;
    // Warm boost when the sun is low: clouds glow orange / pink.
    final low =
        (1 - (geo.sunAltitude / 12).clamp(0.0, 1.0)) *
        (geo.sunAltitude > -6 ? 1 : 0);
    final sunCol = [
      (sl[0] * (1.5 + low * 0.6)).clamp(0.0, 2.0),
      (sl[1] * (1.45 + low * 0.2)).clamp(0.0, 2.0),
      (sl[2] * 1.4).clamp(0.0, 2.0),
    ];
    final amb = Color.lerp(palette.zenith, palette.horizonAway, 0.5)!;
    final s = program.fragmentShader()
      ..setFloat(0, size.width)
      ..setFloat(1, size.height)
      ..setFloat(2, time.value)
      ..setFloat(3, geo.horizon)
      ..setFloat(4, sun.dx)
      ..setFloat(5, sun.dy)
      ..setFloat(6, sunCol[0].toDouble())
      ..setFloat(7, sunCol[1].toDouble())
      ..setFloat(8, sunCol[2].toDouble())
      ..setFloat(9, amb.r)
      ..setFloat(10, amb.g)
      ..setFloat(11, amb.b)
      ..setFloat(12, cover)
      ..setFloat(13, palette.night);
    canvas.drawRect(Offset.zero & size, Paint()..shader = s);
    s.dispose();
  }

  @override
  bool shouldRepaint(_CloudPainter old) =>
      old.palette != palette || old.cover != cover;
}

// ---------------------------------------------------------------------------
// Lens flare + sun bloom
// ---------------------------------------------------------------------------

class _FlarePainter extends CustomPainter {
  _FlarePainter(this.geo, this.palette, this.sunAlt);
  final _SkyGeometry geo;
  final SkyPalette palette;
  final double sunAlt;

  @override
  void paint(Canvas canvas, Size size) {
    if (sunAlt < -1.2) return;
    final uv = geo.sunUv;
    if (uv.dx < -0.2 || uv.dx > 1.2 || uv.dy < -0.2) return;
    final c = Offset(uv.dx * size.width, uv.dy * size.height);
    final sl = palette.sunLight;
    final warm = Color.from(
      alpha: 1,
      red: (sl[0] * 1.2).clamp(0.0, 1.0),
      green: (sl[1] * 1.1).clamp(0.0, 1.0),
      blue: (sl[2] * 0.95).clamp(0.0, 1.0),
    );
    final strength = ((sunAlt + 1.2) / 4).clamp(0.0, 1.0);
    final add = Paint()..blendMode = BlendMode.plus;

    // Bloom
    final bloomR = size.width * 0.55;
    add.shader = RadialGradient(
      colors: [
        warm.withValues(alpha: 0.55 * strength),
        warm.withValues(alpha: 0.12 * strength),
        Colors.transparent,
      ],
      stops: const [0, 0.25, 1],
    ).createShader(Rect.fromCircle(center: c, radius: bloomR));
    canvas.drawCircle(c, bloomR, add);

    // Ghosts along the optical axis.
    final centre = Offset(size.width / 2, size.height * 0.45);
    final axis = centre - c;
    const ghosts = [
      (0.55, 0.020, 0.10),
      (0.85, 0.045, 0.06),
      (1.25, 0.012, 0.12),
      (1.6, 0.07, 0.035),
    ];
    for (final (t, r, a) in ghosts) {
      final p = c + axis * t;
      final rad = size.width * r * 2.2;
      add.shader = RadialGradient(
        colors: [
          Color.lerp(
            warm,
            const Color(0xFF9FD3FF),
            t / 2,
          )!.withValues(alpha: a * strength),
          Colors.transparent,
        ],
      ).createShader(Rect.fromCircle(center: p, radius: rad));
      canvas.drawCircle(p, rad, add);
    }

    // Anamorphic streak
    final streak = Rect.fromCenter(
      center: c,
      width: size.width * 1.4,
      height: 3,
    );
    add.shader = LinearGradient(
      colors: [
        Colors.transparent,
        warm.withValues(alpha: 0.35 * strength),
        Colors.transparent,
      ],
    ).createShader(streak);
    canvas.drawRect(
      streak,
      add..maskFilter = const MaskFilter.blur(BlurStyle.normal, 1.5),
    );
  }

  @override
  bool shouldRepaint(_FlarePainter old) =>
      old.sunAlt != sunAlt ||
      old.geo.sunBearing != geo.sunBearing ||
      old.geo.viewBearing != geo.viewBearing;
}

// ---------------------------------------------------------------------------
// Mountains with aerial perspective
// ---------------------------------------------------------------------------

class _MountainPainter extends CustomPainter {
  _MountainPainter(this.geo, this.palette);
  final _SkyGeometry geo;
  final SkyPalette palette;

  static double _ridge(double x, int seed, double rough) {
    var v = 0.0, amp = 1.0, freq = 1.0, norm = 0.0;
    for (var o = 0; o < 6; o++) {
      v += amp * _vnoise(x * freq * 3 + seed * 17.3, seed + o);
      norm += amp;
      amp *= rough;
      freq *= 2.1;
    }
    return v / norm;
  }

  static double _vnoise(double x, int seed) {
    final i = x.floor();
    final f = x - i;
    double h(int n) {
      final s = math.sin((n * 127.1 + seed * 311.7)) * 43758.5453;
      return s - s.floorToDouble();
    }

    final u = f * f * (3 - 2 * f);
    return h(i) * (1 - u) + h(i + 1) * u;
  }

  @override
  void paint(Canvas canvas, Size size) {
    final hy = geo.horizon * size.height;
    final dark = Color.lerp(
      const Color(0xFF0B0D16),
      const Color(0xFF05060B),
      palette.night,
    )!;
    final hazeSun = palette.horizonSun;
    final hazeAway = palette.horizonAway;
    // Parallax: ridges shift slightly with the view bearing.
    final shift = geo.viewBearing / 360;

    final layers = [
      (0.012, 0.075, 0.45, 3, 0.42, 0.25),
      (0.055, 0.085, 0.62, 11, 0.50, 0.55),
      (0.115, 0.095, 0.82, 23, 0.55, 0.9),
      (0.20, 0.07, 0.95, 37, 0.5, 1.4),
    ];
    for (final (base, amp, darkness, seed, rough, par) in layers) {
      final path = Path()..moveTo(0, size.height);
      const steps = 120;
      for (var i = 0; i <= steps; i++) {
        final x = i / steps;
        final r = _ridge(x * (1.2 + par * 0.4) + shift * par, seed, rough);
        final y =
            hy +
            base * size.height -
            math.pow(r, 1.6) * amp * size.height * 1.9;
        path.lineTo(x * size.width, y);
      }
      path
        ..lineTo(size.width, size.height)
        ..close();

      final sunSide = geo.sunUv.dx.clamp(0.0, 1.0);
      final haze = Color.lerp(hazeAway, hazeSun, 0.6)!;
      final top = Color.lerp(haze, dark, darkness)!;
      final bottom = Color.lerp(haze, dark, math.min(1.0, darkness + 0.12))!;
      final rect = Rect.fromLTRB(
        0,
        hy - amp * size.height * 2,
        size.width,
        size.height,
      );
      canvas.drawPath(
        path,
        Paint()
          ..shader = LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [top, bottom],
          ).createShader(rect),
      );

      // Sun-side rim light on the farthest ridges when the sun is low.
      if (darkness < 0.7 && geo.sunAltitude > -3 && geo.sunAltitude < 15) {
        final rim = Paint()
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.2
          ..blendMode = BlendMode.plus
          ..shader = RadialGradient(
            center: Alignment(sunSide * 2 - 1, -1),
            radius: 1.1,
            colors: [
              hazeSun.withValues(alpha: 0.5 * (1 - darkness)),
              Colors.transparent,
            ],
          ).createShader(Offset.zero & size);
        canvas.drawPath(path, rim);
      }
    }

    // Ground fog for depth + legibility of the UI.
    final fogRect = Rect.fromLTRB(0, hy, size.width, size.height);
    canvas.drawRect(
      fogRect,
      Paint()
        ..shader = LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            Colors.transparent,
            dark.withValues(alpha: 0.65),
            dark.withValues(alpha: 0.92),
          ],
          stops: const [0, 0.55, 1],
        ).createShader(fogRect),
    );
  }

  @override
  bool shouldRepaint(_MountainPainter old) =>
      old.palette != palette || old.geo.viewBearing != geo.viewBearing;
}

/// Chooses a pleasing direction to face: sunrise in the morning, sunset in
/// the afternoon (where the golden hour happens).
double homeViewBearing(
  double sunBearing,
  double? sunriseBearing,
  double? sunsetBearing,
  bool afternoon,
) {
  final target = afternoon ? sunsetBearing : sunriseBearing;
  if (target == null) return sunBearing;
  // Keep the sun on screen when it's near the target direction.
  if (angleDiff(target, sunBearing) >= 40) return target;
  final a = target * math.pi / 180, b = sunBearing * math.pi / 180;
  final mean =
      math.atan2(math.sin(a) + math.sin(b), math.cos(a) + math.cos(b)) *
      180 /
      math.pi;
  return (mean + 360) % 360;
}
